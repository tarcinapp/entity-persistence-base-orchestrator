package com.tarcinapp.epbo.processors.entities;

import com.tarcinapp.epbo.normalization.FieldNormalizationService;
import com.tarcinapp.epbo.normalization.FieldNormalizationService.Operation;
import com.tarcinapp.epbo.normalization.NormalizationConfigService.RecordType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Pre-processor for PUT /entities/{id} (operationId: replaceEntityById).
 *
 * Full replacement — same normalization rules as CREATE.
 */
@ApplicationScoped
public class ReplaceEntityByIdPreProcessor implements Processor {

    @Inject
    FieldNormalizationService normalizationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        String normalized = normalizationService.normalizeJson(body, Operation.REPLACE, RecordType.ENTITY);
        exchange.getMessage().setBody(normalized);
    }
}
