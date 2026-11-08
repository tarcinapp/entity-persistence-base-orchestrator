package io.tarcinapp.epbo.processors.entities;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PATCH /entities/{id} (operationId: updateEntityById).
 *
 * Partial update — EPBO only injects _slug if _name is present.
 * Config-driven defaults (_kind, _visibility, _validFromDateTime) are NOT injected on PATCH.
 */
@ApplicationScoped
public class UpdateEntityByIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.PATCH, RecordType.ENTITY);
        exchange.getMessage().setBody(normalized);
    }
}
