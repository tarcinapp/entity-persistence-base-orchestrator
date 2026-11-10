package com.tarcinapp.epbo.processors.entityreactions;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PUT /entity-reactions/{id} (operationId: replaceEntityReactionById).
 *
 * Full replacement — same normalization rules as CREATE.
 */
@ApplicationScoped
public class ReplaceEntityReactionByIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.REPLACE, RecordType.ENTITY_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
