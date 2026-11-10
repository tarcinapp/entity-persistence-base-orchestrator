package com.tarcinapp.epbo.processors.lists;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /lists/{id}/entities (operationId: createEntityByListId).
 *
 * Creates a ListToEntityRelation (RELATION record type) linking a list and an entity.
 */
@ApplicationScoped
public class CreateEntityByListIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.RELATION);
        exchange.getMessage().setBody(normalized);
    }
}
