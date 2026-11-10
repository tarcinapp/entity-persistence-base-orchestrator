package com.tarcinapp.epbo.processors.entityreactions;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PATCH /entity-reactions (operationId: updateEntityReactions).
 *
 * Bulk PATCH — PATCH semantics: only _slug injected if _name present.
 */
@ApplicationScoped
public class UpdateEntityReactionsPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.PATCH, RecordType.ENTITY_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
