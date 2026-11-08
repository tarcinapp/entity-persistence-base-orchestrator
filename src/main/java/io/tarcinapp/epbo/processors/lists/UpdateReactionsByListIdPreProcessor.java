package io.tarcinapp.epbo.processors.lists;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PATCH /lists/{id}/reactions (operationId: updateReactionsByListId).
 *
 * Bulk PATCH on list reactions — PATCH semantics: only _slug injected if _name present.
 */
@ApplicationScoped
public class UpdateReactionsByListIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.PATCH, RecordType.LIST_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
