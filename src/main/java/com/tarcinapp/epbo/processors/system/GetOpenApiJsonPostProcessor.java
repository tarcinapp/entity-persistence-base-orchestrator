package com.tarcinapp.epbo.processors.system;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Post-processor for GET /openapi.json (operationId: getOpenApiJson).
 *
 * This bean is executed AFTER the backend call.
 *
 *
 * <p>To override the backend response:</p>
 * <pre>
 *   exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
 *   exchange.getMessage().setBody("{ \"modified\": true }");
 * </pre>
 */
@ApplicationScoped
public class GetOpenApiJsonPostProcessor implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        // TODO: implement post-processing logic for getOpenApiJson
    }
}
