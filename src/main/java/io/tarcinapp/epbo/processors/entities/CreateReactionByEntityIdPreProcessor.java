package io.tarcinapp.epbo.processors.entities;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /entities/{id}/reactions (operationId: createReactionByEntityId).
 *
 * Creates an entity reaction linked to the given entity.
 * Record type is ENTITY_REACTION.
 */
@ApplicationScoped
public class CreateReactionByEntityIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.ENTITY_REACTION);
        exchange.getMessage().setBody(normalized);
    }
}
