package com.tarcinapp.epbo.processors.entityreactions;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PATCH /entity-reactions/{id} (operationId: updateEntityReactionById).
 *
 * Partial update — only _slug injected if _name present. No config-driven defaults.
 */
@ApplicationScoped
public class UpdateEntityReactionByIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.PATCH, RecordType.ENTITY_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
