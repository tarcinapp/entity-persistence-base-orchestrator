package io.tarcinapp.epbo.processors.lists;

import io.tarcinapp.epbo.normalization.FieldNormalizationService;
import io.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import io.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /lists (operationId: createList).
 *
 * Injects EPBO managed fields (_kind, _visibility, _validFromDateTime,
 * _validUntilDateTime, _slug) before forwarding to EPC.
 */
@ApplicationScoped
public class CreateListPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.LIST);
        exchange.getMessage().setBody(normalized);
    }
}
