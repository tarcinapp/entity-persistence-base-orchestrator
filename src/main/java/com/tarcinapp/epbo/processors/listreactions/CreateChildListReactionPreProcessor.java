package com.tarcinapp.epbo.processors.listreactions;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /list-reactions/{id}/children (operationId: createChildListReaction).
 *
 * Creates a child list reaction — same record type (LIST_REACTION).
 */
@ApplicationScoped
public class CreateChildListReactionPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.LIST_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
