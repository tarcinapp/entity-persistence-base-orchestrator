package com.tarcinapp.epbo.normalization;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Reads all EPBO normalization configuration from environment variables per the
 * T-05 approved schema.
 *
 * <p>Naming pattern (type-first, no service prefix):
 * <pre>
 *   {TYPE}_DEFAULT_KIND
 *   {TYPE}_DEFAULT_VISIBILITY[_FOR_{KIND_KEY}]
 *   {TYPE}_AUTO_APPROVE[_FOR_{KIND_KEY}]
 * </pre>
 *
 * <p>Kind encoding: a stored kind value (already in {@code [a-z0-9-]+} form)
 * is encoded as {@code KIND_KEY = kind.toUpperCase().replace('-','_')}.
 *
 * <p>Resolution order for config-driven fields:
 * <ol>
 *   <li>Per-kind override ({TYPE}_{CONCERN}_FOR_{KIND_KEY}) — if set</li>
 *   <li>Base default ({TYPE}_{CONCERN}) — if set</li>
 *   <li>No injection</li>
 * </ol>
 *
 * <p>Auto-approve exception: when {@code {TYPE}_AUTO_APPROVE} is absent, the
 * effective base default is {@code false} (not "no injection"), so that an
 * explicit per-kind {@code true} can still override it.
 */
@ApplicationScoped
public class NormalizationConfigService {

    private static final Logger LOG = Logger.getLogger(NormalizationConfigService.class);

    /** Record-type tokens used in env var construction. */
    public enum RecordType {
        ENTITY, LIST, RELATION, ENTITY_REACTION, LIST_REACTION;

        /** Returns the env-var type prefix (e.g. {@code "ENTITY_REACTION"}). */
        public String token() {
            return this.name(); // enum name IS the token
        }
    }

    private final Config config = ConfigProvider.getConfig();

    // ─────────────────────────────────────────────────────────────────────────
    // _kind default
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the configured default {@code _kind} for the given record type,
     * or {@link Optional#empty()} if not configured.
     */
    public Optional<String> getDefaultKind(RecordType type) {
        String varName = type.token() + "_DEFAULT_KIND";
        Optional<String> value = config.getOptionalValue(varName, String.class);
        value.ifPresent(v -> LOG.tracef("Config %s=%s", varName, v));
        return value.filter(s -> !s.isBlank());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // _visibility default
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the effective {@code _visibility} default for the given record type
     * and resolved kind (after per-kind override lookup), or {@link Optional#empty()}
     * if neither the per-kind var nor the base var is configured.
     *
     * @param type        record type
     * @param resolvedKind the kind value already present in the payload (may be null)
     */
    public Optional<String> getEffectiveVisibility(RecordType type, String resolvedKind) {
        if (resolvedKind != null && !resolvedKind.isBlank()) {
            String perKindVar = type.token() + "_DEFAULT_VISIBILITY_FOR_" + kindToEnvKey(resolvedKind);
            Optional<String> perKind = config.getOptionalValue(perKindVar, String.class)
                    .filter(s -> !s.isBlank());
            if (perKind.isPresent()) {
                LOG.tracef("Config %s=%s (per-kind override)", perKindVar, perKind.get());
                return perKind;
            }
        }
        String baseVar = type.token() + "_DEFAULT_VISIBILITY";
        return config.getOptionalValue(baseVar, String.class).filter(s -> !s.isBlank());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // _validFromDateTime auto-approve
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if auto-approve is enabled for the given type and kind.
     *
     * <p>Resolution: per-kind override wins; falls back to base; effective base
     * default is {@code false} when the base var is unset.
     *
     * @param type        record type
     * @param resolvedKind the kind value already present in the payload (may be null)
     */
    public boolean isAutoApprove(RecordType type, String resolvedKind) {
        if (resolvedKind != null && !resolvedKind.isBlank()) {
            String perKindVar = type.token() + "_AUTO_APPROVE_FOR_" + kindToEnvKey(resolvedKind);
            Optional<String> perKind = config.getOptionalValue(perKindVar, String.class)
                    .filter(s -> !s.isBlank());
            if (perKind.isPresent()) {
                boolean result = Boolean.parseBoolean(perKind.get());
                LOG.tracef("Config %s=%s (per-kind override) → %b", perKindVar, perKind.get(), result);
                return result;
            }
        }
        String baseVar = type.token() + "_AUTO_APPROVE";
        return config.getOptionalValue(baseVar, String.class)
                .filter(s -> !s.isBlank())
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // /system-info snapshot
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of all resolved normalization configuration for every
     * record type. Used by the {@code /system-info} endpoint.
     *
     * <p>The returned map structure mirrors the T-05 §10 JSON shape:
     * <pre>
     * {
     *   "normalization": {
     *     "entity":          { "defaultKind": "...", "defaultVisibility": "...", "autoApprove": false, "perKindOverrides": {...} },
     *     "list":            { ... },
     *     "relation":        { ... },
     *     "entityReaction":  { ... },
     *     "listReaction":    { ... }
     *   }
     * }
     * </pre>
     */
    public Map<String, Object> getSystemInfoSnapshot() {
        Map<String, Object> normalization = new HashMap<>();
        normalization.put("entity",         buildTypeSnapshot(RecordType.ENTITY,           false));
        normalization.put("list",           buildTypeSnapshot(RecordType.LIST,             false));
        normalization.put("relation",       buildTypeSnapshot(RecordType.RELATION,         true));
        normalization.put("entityReaction", buildTypeSnapshot(RecordType.ENTITY_REACTION,  false));
        normalization.put("listReaction",   buildTypeSnapshot(RecordType.LIST_REACTION,    false));

        Map<String, Object> root = new HashMap<>();
        root.put("normalization", normalization);
        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Encodes a kind value to its environment variable key suffix.
     * <pre>
     *   kind.toUpperCase().replace('-', '_')
     * </pre>
     * e.g. {@code "e-book"} → {@code "E_BOOK"}, {@code "book"} → {@code "BOOK"}.
     */
    static String kindToEnvKey(String kind) {
        return kind.toUpperCase().replace('-', '_');
    }

    /**
     * Reverse of {@link #kindToEnvKey}: converts a {@code KIND_KEY} suffix back to
     * the original kind value for use in the {@code /system-info} response.
     */
    static String envKeyToKind(String kindKey) {
        return kindKey.toLowerCase().replace('_', '-');
    }

    private Map<String, Object> buildTypeSnapshot(RecordType type, boolean noVisibility) {
        Map<String, Object> snapshot = new HashMap<>();

        // defaultKind
        getDefaultKind(type).ifPresentOrElse(
                k -> snapshot.put("defaultKind", k),
                () -> snapshot.put("defaultKind", null));

        // defaultVisibility (not applicable to relation)
        if (!noVisibility) {
            getEffectiveVisibility(type, null).ifPresentOrElse(
                    v -> snapshot.put("defaultVisibility", v),
                    () -> snapshot.put("defaultVisibility", null));
        }

        // autoApprove (base, no kind override applied here)
        String baseAutoApproveVar = type.token() + "_AUTO_APPROVE";
        boolean baseAutoApprove = config.getOptionalValue(baseAutoApproveVar, String.class)
                .filter(s -> !s.isBlank())
                .map(Boolean::parseBoolean)
                .orElse(false);
        snapshot.put("autoApprove", baseAutoApprove);

        // perKindOverrides — scan all env vars matching TYPE_*_FOR_* patterns
        Map<String, Object> perKindOverrides = buildPerKindOverrides(type, noVisibility);
        if (!perKindOverrides.isEmpty()) {
            snapshot.put("perKindOverrides", perKindOverrides);
        }

        return snapshot;
    }

    private Map<String, Object> buildPerKindOverrides(RecordType type, boolean noVisibility) {
        Map<String, Map<String, Object>> overridesByKind = new HashMap<>();

        String visibilityPrefix = type.token() + "_DEFAULT_VISIBILITY_FOR_";
        String autoApprovePrefix = type.token() + "_AUTO_APPROVE_FOR_";

        Iterable<String> propertyNames = config.getPropertyNames();
        for (String name : propertyNames) {
            if (!noVisibility && name.startsWith(visibilityPrefix)) {
                String kindKey = name.substring(visibilityPrefix.length());
                String kind = envKeyToKind(kindKey);
                config.getOptionalValue(name, String.class).filter(s -> !s.isBlank()).ifPresent(v -> {
                    overridesByKind.computeIfAbsent(kind, k -> new HashMap<>()).put("defaultVisibility", v);
                });
            } else if (name.startsWith(autoApprovePrefix)) {
                String kindKey = name.substring(autoApprovePrefix.length());
                String kind = envKeyToKind(kindKey);
                config.getOptionalValue(name, String.class).filter(s -> !s.isBlank()).ifPresent(v -> {
                    overridesByKind.computeIfAbsent(kind, k -> new HashMap<>()).put("autoApprove", Boolean.parseBoolean(v));
                });
            }
        }

        return new HashMap<>(overridesByKind);
    }
}
