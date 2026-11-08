package io.tarcinapp.epbo.processors.listreactions;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * Post-processor for POST /list-reactions/{id}/children (operationId: createChildListReaction).
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
public class CreateChildListReactionPostProcessor implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        // TODO: implement post-processing logic for createChildListReaction
    }
}
