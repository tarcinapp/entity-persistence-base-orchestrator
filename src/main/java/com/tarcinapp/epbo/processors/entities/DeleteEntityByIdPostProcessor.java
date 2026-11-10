package com.tarcinapp.epbo.processors.entities;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Post-processor for DELETE /entities/{id} (operationId: deleteEntityById).
 *
 * This bean is executed AFTER the backend has responded.
 *
 *
 * <p>Useful exchange accessors:</p>
 * <pre>
 *   String responseBody = exchange.getMessage().getBody(String.class);
 *   int statusCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
 * </pre>
 */
@ApplicationScoped
public class DeleteEntityByIdPostProcessor implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        // TODO: implement post-processing logic for deleteEntityById
    }
}
