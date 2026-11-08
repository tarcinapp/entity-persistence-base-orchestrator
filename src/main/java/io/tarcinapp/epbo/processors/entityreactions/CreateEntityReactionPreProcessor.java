package io.tarcinapp.epbo.processors.entityreactions;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /entity-reactions (operationId: createEntityReaction).
 */
@ApplicationScoped
public class CreateEntityReactionPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.ENTITY_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
