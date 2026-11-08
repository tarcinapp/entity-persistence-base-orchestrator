package io.tarcinapp.epbo.processors.lists;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PUT /lists/{id} (operationId: replaceListById).
 *
 * Full replacement — same normalization rules as CREATE.
 */
@ApplicationScoped
public class ReplaceListByIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.REPLACE, RecordType.LIST);
        exchange.getMessage().setBody(normalized);
    }
}
