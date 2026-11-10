package com.tarcinapp.epbo.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Orchestrates all managed-field injection for EPBO.
 *
 * <p>Field injection order for CREATE and REPLACE (PUT) — hard constraint from T-09 spec:
 * <ol>
 *   <li>{@code _kind} — default from config if absent</li>
 *   <li>{@code _visibility} — default from config (per-kind lookup requires step 1)</li>
 *   <li>{@code _validFromDateTime} — auto-approve (per-kind lookup requires step 1)</li>
 *   <li>{@code _validUntilDateTime} — always coerced to explicit {@code null} when absent</li>
 *   <li>{@code _slug} — computed from {@code _name} when present and {@code _slug} absent;
 *       applies to entity, list, entityReaction, listReaction — NOT relation</li>
 * </ol>
 *
 * <p>For PATCH: only step 5 applies, and only when {@code _name} is present in the
 * PATCH payload and the record type is not {@link NormalizationConfigService.RecordType#RELATION}.
 * Config-driven defaults (steps 1–4) are never injected on PATCH.
 *
 * <p>Fields this service never touches (EPG-owned):
 * {@code _createdBy}, {@code _lastUpdatedBy}, {@code _createdDateTime},
 * {@code _lastUpdatedDateTime}.
 *
 * <p>Fields this service never touches (EPS-owned):
 * {@code _version}, {@code _ownerUsersCount}, {@code _ownerGroupsCount},
 * {@code _viewerUsersCount}, {@code _viewerGroupsCount}, {@code _parentsCount},
 * {@code _idempotencyKey}.
 */
@ApplicationScoped
public class FieldNormalizationService {

    private static final Logger LOG = Logger.getLogger(FieldNormalizationService.class);

    /** Operation types recognised by this service. */
    public enum Operation { CREATE, REPLACE, PATCH }

    @Inject
    NormalizationConfigService configService;

    @Inject
    SlugService slugService;

    @Inject
    ObjectMapper objectMapper;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Injects managed fields into {@code payload} according to the given operation
     * and record type.  Modifies {@code payload} in-place and returns it.
     *
     * @param payload     the parsed JSON body of the incoming request; must be an
     *                    {@link ObjectNode} (i.e. a JSON object, not an array)
     * @param operation   CREATE, REPLACE, or PATCH
     * @param type        the record type (determines which config vars are read)
     * @return the mutated {@code payload} ObjectNode
     */
    public ObjectNode normalize(ObjectNode payload, Operation operation,
                                NormalizationConfigService.RecordType type) {
        if (operation == Operation.PATCH) {
            return normalizePatch(payload, type);
        }
        return normalizeCreateOrReplace(payload, type);
    }

    /**
     * Convenience overload that parses a raw JSON string, normalizes it, and
     * serializes it back to a string.  Returns the original string unchanged on
     * parse failure (defensive — should not happen in a well-formed request).
     */
    public String normalizeJson(String jsonBody, Operation operation,
                                NormalizationConfigService.RecordType type) {
        if (jsonBody == null || jsonBody.isBlank()) {
            return jsonBody;
        }
        try {
            JsonNode node = objectMapper.readTree(jsonBody);
            if (!node.isObject()) {
                LOG.warnf("EPBO normalization skipped: body is not a JSON object (type=%s, op=%s)", type, operation);
                return jsonBody;
            }
            ObjectNode payload = (ObjectNode) node;
            normalize(payload, operation, type);
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            LOG.warnf("EPBO normalization failed to parse JSON body (type=%s, op=%s): %s", type, operation, e.getMessage());
            return jsonBody;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE / REPLACE
    // ─────────────────────────────────────────────────────────────────────────

    private ObjectNode normalizeCreateOrReplace(ObjectNode payload,
                                                NormalizationConfigService.RecordType type) {
        // Step 1 — _kind default
        String resolvedKind = injectKind(payload, type);

        // Step 2 — _visibility default (not applicable to relation)
        if (type != NormalizationConfigService.RecordType.RELATION) {
            injectVisibility(payload, type, resolvedKind);
        }

        // Step 3 — _validFromDateTime auto-approve
        injectValidFromDateTime(payload, type, resolvedKind);

        // Step 4 — _validUntilDateTime null-coercion
        injectValidUntilDateTime(payload);

        // Step 5 — _slug (not applicable to relation)
        if (type != NormalizationConfigService.RecordType.RELATION) {
            injectSlug(payload);
        }

        // Step 6 — _createdDateTime / _lastUpdatedDateTime secondary injection
        injectAuditTimestamps(payload, true);

        return payload;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH
    // ─────────────────────────────────────────────────────────────────────────

    private ObjectNode normalizePatch(ObjectNode payload,
                                      NormalizationConfigService.RecordType type) {
        // PATCH: only _slug (when _name present and type != relation)
        if (type != NormalizationConfigService.RecordType.RELATION) {
            injectSlug(payload);
        }

        // PATCH: secondary _lastUpdatedDateTime injection (EPG is primary)
        injectAuditTimestamps(payload, false);

        return payload;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Individual field injectors
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Injects {@code _kind} default if absent.
     *
     * @return the resolved kind value (from payload or config), or {@code null}
     *         if neither the payload nor config provides a kind value
     */
    private String injectKind(ObjectNode payload,
                               NormalizationConfigService.RecordType type) {
        String existing = textOrNull(payload, "_kind");
        if (existing != null) {
            return existing; // payload already has _kind
        }
        Optional<String> defaultKind = configService.getDefaultKind(type);
        if (defaultKind.isPresent()) {
            payload.put("_kind", defaultKind.get());
            LOG.debugf("EPBO injected _kind=%s (type=%s)", defaultKind.get(), type);
            return defaultKind.get();
        }
        return null;
    }

    /**
     * Injects {@code _visibility} default if absent.
     */
    private void injectVisibility(ObjectNode payload,
                                   NormalizationConfigService.RecordType type,
                                   String resolvedKind) {
        if (payload.has("_visibility") && !payload.get("_visibility").isNull()) {
            return; // payload already provides _visibility
        }
        Optional<String> visibility = configService.getEffectiveVisibility(type, resolvedKind);
        visibility.ifPresent(v -> {
            payload.put("_visibility", v);
            LOG.debugf("EPBO injected _visibility=%s (type=%s, kind=%s)", v, type, resolvedKind);
        });
    }

    /**
     * Injects {@code _validFromDateTime = now} if auto-approve is enabled and
     * the field is absent from the payload.
     */
    private void injectValidFromDateTime(ObjectNode payload,
                                          NormalizationConfigService.RecordType type,
                                          String resolvedKind) {
        if (payload.has("_validFromDateTime") && !payload.get("_validFromDateTime").isNull()) {
            return; // payload already provides _validFromDateTime
        }
        boolean autoApprove = configService.isAutoApprove(type, resolvedKind);
        if (autoApprove) {
            String now = Instant.now().toString();
            payload.put("_validFromDateTime", now);
            LOG.debugf("EPBO injected _validFromDateTime=%s (type=%s, kind=%s, autoApprove=true)", now, type, resolvedKind);
        }
        // If autoApprove=false: leave field absent (do NOT coerce to null)
    }

    /**
     * Always coerces {@code _validUntilDateTime} to explicit JSON {@code null} when absent.
     */
    private void injectValidUntilDateTime(ObjectNode payload) {
        if (!payload.has("_validUntilDateTime")) {
            payload.putNull("_validUntilDateTime");
            LOG.debugf("EPBO injected _validUntilDateTime=null (absent → explicit null)");
        }
    }

    /**
     * Injects {@code _slug} derived from {@code _name} when:
     * <ul>
     *   <li>{@code _name} is present and non-empty in the payload</li>
     *   <li>{@code _slug} is absent from the payload</li>
     * </ul>
     * This method intentionally does NOT check record type — callers are
     * responsible for not calling it for relation records.
     */
    private void injectSlug(ObjectNode payload) {
        String name = textOrNull(payload, "_name");
        if (name == null) {
            return; // _name absent — no slug to derive
        }
        if (payload.has("_slug") && !payload.get("_slug").isNull()) {
            String existingSlug = payload.get("_slug").asText(null);
            if (existingSlug != null && !existingSlug.isBlank()) {
                return; // _slug already explicitly provided
            }
        }
        String slug = slugService.slugify(name);
        if (!slug.isEmpty()) {
            payload.put("_slug", slug);
            LOG.debugf("EPBO injected _slug=%s (from _name=%s)", slug, name);
        }
    }

    /**
     * Secondary injection of {@code _createdDateTime} and {@code _lastUpdatedDateTime}.
     * EPG is the primary injector; EPBO injects as a safety net for cases where EPG
     * did not inject (unauthenticated path, auth disabled, direct internal calls).
     *
     * @param includeCreated {@code true} for CREATE (injects both fields);
     *                       {@code false} for PATCH/REPLACE (injects only {@code _lastUpdatedDateTime})
     */
    private void injectAuditTimestamps(ObjectNode payload, boolean includeCreated) {
        String now = Instant.now().toString();
        if (includeCreated && !payload.has("_createdDateTime")) {
            payload.put("_createdDateTime", now);
            LOG.debugf("EPBO injected _createdDateTime=%s (secondary injection)", now);
        }
        if (!payload.has("_lastUpdatedDateTime")) {
            payload.put("_lastUpdatedDateTime", now);
            LOG.debugf("EPBO injected _lastUpdatedDateTime=%s (secondary injection)", now);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns the text value of a JSON string field, or {@code null} if absent/null/blank. */
    private static String textOrNull(ObjectNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        String v = n.asText(null);
        return (v == null || v.isBlank()) ? null : v;
    }
}
