package com.tarcinapp.epbo.processors.entities;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for POST /entities/{id}/children (operationId: createChildEntity).
 *
 * Creates a child entity — same record type as the parent (ENTITY).
 * Injects EPBO managed fields before forwarding to EPC.
 */
@ApplicationScoped
public class CreateChildEntityPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.CREATE, RecordType.ENTITY);
        exchange.getMessage().setBody(normalized);
    }
}
