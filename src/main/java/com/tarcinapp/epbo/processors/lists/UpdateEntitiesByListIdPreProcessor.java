package com.tarcinapp.epbo.processors.lists;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PATCH /lists/{id}/entities (operationId: updateEntitiesByListId).
 *
 * Bulk PATCH on relation records — PATCH semantics: _slug not applicable to RELATION.
 * Only _lastUpdatedDateTime secondary injection applies.
 */
@ApplicationScoped
public class UpdateEntitiesByListIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.PATCH, RecordType.RELATION);
        exchange.getMessage().setBody(normalized);
    }
}
