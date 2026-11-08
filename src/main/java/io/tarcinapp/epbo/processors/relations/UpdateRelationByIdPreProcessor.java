package io.tarcinapp.epbo.processors.relations;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PATCH /relations/{id} (operationId: updateRelationById).
 *
 * Partial update — no slug for RELATION. Only _lastUpdatedDateTime secondary injection.
 */
@ApplicationScoped
public class UpdateRelationByIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.PATCH, RecordType.RELATION);
        exchange.getMessage().setBody(normalized);
    }
}
