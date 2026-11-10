package com.tarcinapp.epbo.processors.relations;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /relations (operationId: createRelation).
 *
 * Injects _kind, _validFromDateTime, _validUntilDateTime.
 * _visibility and _slug are not applicable to RELATION records.
 */
@ApplicationScoped
public class CreateRelationPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.RELATION);
        exchange.getMessage().setBody(normalized);
    }
}
