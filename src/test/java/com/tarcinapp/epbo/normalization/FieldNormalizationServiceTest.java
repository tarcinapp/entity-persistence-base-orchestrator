package com.tarcinapp.epbo.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FieldNormalizationService}.
 *
 * This test class directly instantiates the services (no CDI container)
 * by building a minimal {@link Config} shim backed by a {@link Map}.
 */
class FieldNormalizationServiceTest {

    /** Minimal config map — mutate per test. */
    private final Map<String, String> configMap = new HashMap<>();

    private FieldNormalizationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        NormalizationConfigService configService = new NormalizationConfigService() {
            // Override config-reading to use a map-backed config
            @Override
            public java.util.Optional<String> getDefaultKind(RecordType type) {
                String key = type.token() + "_DEFAULT_KIND";
                return java.util.Optional.ofNullable(configMap.get(key)).filter(s -> !s.isBlank());
            }

            @Override
            public java.util.Optional<String> getEffectiveVisibility(RecordType type, String resolvedKind) {
                if (resolvedKind != null) {
                    String perKindKey = type.token() + "_DEFAULT_VISIBILITY_FOR_" + kindToEnvKey(resolvedKind);
                    String perKind = configMap.get(perKindKey);
                    if (perKind != null && !perKind.isBlank()) return java.util.Optional.of(perKind);
                }
                String baseKey = type.token() + "_DEFAULT_VISIBILITY";
                return java.util.Optional.ofNullable(configMap.get(baseKey)).filter(s -> !s.isBlank());
            }

            @Override
            public boolean isAutoApprove(RecordType type, String resolvedKind) {
                if (resolvedKind != null) {
                    String perKindKey = type.token() + "_AUTO_APPROVE_FOR_" + kindToEnvKey(resolvedKind);
                    String perKind = configMap.get(perKindKey);
                    if (perKind != null && !perKind.isBlank()) return Boolean.parseBoolean(perKind);
                }
                String baseKey = type.token() + "_AUTO_APPROVE";
                return Boolean.parseBoolean(configMap.getOrDefault(baseKey, "false"));
            }
        };

        SlugService slugService = new SlugService();

        service = new FieldNormalizationService();
        service.configService = configService;
        service.slugService = slugService;
        service.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class CreateOperation {

        @Test
        void injects_kind_default_when_absent() {
            configMap.put("ENTITY_DEFAULT_KIND", "book");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("book", payload.get("_kind").asText());
        }

        @Test
        void does_not_overwrite_existing_kind() {
            configMap.put("ENTITY_DEFAULT_KIND", "book");
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_kind", "article");
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("article", payload.get("_kind").asText());
        }

        @Test
        void injects_visibility_default() {
            configMap.put("ENTITY_DEFAULT_VISIBILITY", "protected");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("protected", payload.get("_visibility").asText());
        }

        @Test
        void per_kind_visibility_overrides_base() {
            configMap.put("ENTITY_DEFAULT_VISIBILITY", "private");
            configMap.put("ENTITY_DEFAULT_VISIBILITY_FOR_BOOK", "public");
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_kind", "book");
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("public", payload.get("_visibility").asText());
        }

        @Test
        void injects_validFromDateTime_when_autoApprove_true() {
            configMap.put("ENTITY_AUTO_APPROVE", "true");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertNotNull(payload.get("_validFromDateTime"));
            assertFalse(payload.get("_validFromDateTime").isNull());
        }

        @Test
        void does_not_inject_validFromDateTime_when_autoApprove_false() {
            configMap.put("ENTITY_AUTO_APPROVE", "false");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            // Field should be absent (not null) when autoApprove is false
            assertNull(payload.get("_validFromDateTime"));
        }

        @Test
        void coerces_validUntilDateTime_to_null_when_absent() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertNotNull(payload.get("_validUntilDateTime"));
            assertTrue(payload.get("_validUntilDateTime").isNull());
        }

        @Test
        void does_not_overwrite_existing_validUntilDateTime() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_validUntilDateTime", "2030-01-01T00:00:00Z");
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("2030-01-01T00:00:00Z", payload.get("_validUntilDateTime").asText());
        }

        @Test
        void computes_slug_from_name() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_name", "My Great Book");
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("my-great-book", payload.get("_slug").asText());
        }

        @Test
        void does_not_overwrite_existing_slug() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_name", "My Great Book");
            payload.put("_slug", "custom-slug");
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertEquals("custom-slug", payload.get("_slug").asText());
        }

        @Test
        void does_not_compute_slug_without_name() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertNull(payload.get("_slug"));
        }

        @Test
        void relation_create_does_not_inject_visibility() {
            configMap.put("ENTITY_DEFAULT_VISIBILITY", "protected");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.RELATION);
            // _visibility must not be injected for RELATION
            assertNull(payload.get("_visibility"));
        }

        @Test
        void relation_create_does_not_compute_slug() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_name", "some relation name");
            service.normalize(payload, Operation.CREATE, RecordType.RELATION);
            // _slug is not applicable to RELATION
            assertNull(payload.get("_slug"));
        }

        @Test
        void does_not_inject_audit_fields_when_already_present() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_createdDateTime", "2024-01-01T00:00:00Z");
            payload.put("_lastUpdatedDateTime", "2024-01-01T00:00:00Z");
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            // EPG-provided values must be preserved
            assertEquals("2024-01-01T00:00:00Z", payload.get("_createdDateTime").asText());
            assertEquals("2024-01-01T00:00:00Z", payload.get("_lastUpdatedDateTime").asText());
        }

        @Test
        void never_injects_createdBy_or_lastUpdatedBy() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertNull(payload.get("_createdBy"), "_createdBy must not be injected by EPBO");
            assertNull(payload.get("_lastUpdatedBy"), "_lastUpdatedBy must not be injected by EPBO");
        }

        @Test
        void never_injects_count_fields() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertNull(payload.get("_ownerUsersCount"), "_ownerUsersCount must not be injected by EPBO");
            assertNull(payload.get("_ownerGroupsCount"), "_ownerGroupsCount must not be injected by EPBO");
            assertNull(payload.get("_viewerUsersCount"), "_viewerUsersCount must not be injected by EPBO");
            assertNull(payload.get("_viewerGroupsCount"), "_viewerGroupsCount must not be injected by EPBO");
            assertNull(payload.get("_parentsCount"), "_parentsCount must not be injected by EPBO");
        }

        @Test
        void never_injects_idempotencyKey() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.CREATE, RecordType.ENTITY);
            assertNull(payload.get("_idempotencyKey"), "_idempotencyKey must not be injected by EPBO");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPLACE (PUT) tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class ReplaceOperation {

        @Test
        void replace_injects_all_same_as_create() {
            configMap.put("ENTITY_DEFAULT_KIND", "book");
            configMap.put("ENTITY_DEFAULT_VISIBILITY", "public");
            configMap.put("ENTITY_AUTO_APPROVE", "true");

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_name", "Replace Test");
            service.normalize(payload, Operation.REPLACE, RecordType.ENTITY);

            assertEquals("book",        payload.get("_kind").asText());
            assertEquals("public",      payload.get("_visibility").asText());
            assertFalse(payload.get("_validFromDateTime").isNull());
            assertTrue(payload.get("_validUntilDateTime").isNull());
            assertEquals("replace-test", payload.get("_slug").asText());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    class PatchOperation {

        @Test
        void patch_injects_slug_when_name_present() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_name", "Updated Name");
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertEquals("updated-name", payload.get("_slug").asText());
        }

        @Test
        void patch_does_not_inject_kind_default() {
            configMap.put("ENTITY_DEFAULT_KIND", "book");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertNull(payload.get("_kind"), "PATCH must not inject _kind default");
        }

        @Test
        void patch_does_not_inject_visibility() {
            configMap.put("ENTITY_DEFAULT_VISIBILITY", "protected");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertNull(payload.get("_visibility"), "PATCH must not inject _visibility");
        }

        @Test
        void patch_does_not_inject_validFromDateTime() {
            configMap.put("ENTITY_AUTO_APPROVE", "true");
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertNull(payload.get("_validFromDateTime"), "PATCH must not inject _validFromDateTime");
        }

        @Test
        void patch_does_not_coerce_validUntilDateTime() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertNull(payload.get("_validUntilDateTime"), "PATCH must not coerce _validUntilDateTime");
        }

        @Test
        void patch_does_not_inject_slug_when_name_absent() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_someOtherField", "value");
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertNull(payload.get("_slug"), "PATCH must not inject _slug when _name absent");
        }

        @Test
        void patch_on_relation_does_not_compute_slug() {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("_name", "hypothetical relation name");
            service.normalize(payload, Operation.PATCH, RecordType.RELATION);
            assertNull(payload.get("_slug"), "PATCH on RELATION must never inject _slug");
        }

        @Test
        void patch_never_injects_audit_owned_by_epg() {
            ObjectNode payload = objectMapper.createObjectNode();
            service.normalize(payload, Operation.PATCH, RecordType.ENTITY);
            assertNull(payload.get("_createdBy"),      "_createdBy must not be injected by EPBO");
            assertNull(payload.get("_lastUpdatedBy"),  "_lastUpdatedBy must not be injected by EPBO");
        }
    }
}
