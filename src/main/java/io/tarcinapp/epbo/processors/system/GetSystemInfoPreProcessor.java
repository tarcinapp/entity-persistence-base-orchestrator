package io.tarcinapp.epbo.processors.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tarcinapp.epbo.normalization.NormalizationConfigService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jboss.logging.Logger;

/**
 * Pre-processor for GET /system-info (operationId: getSystemInfo).
 *
 * Short-circuits the route: returns EPBO's own normalization configuration
 * instead of proxying to EPC. The response follows the shape defined in
 * T-05 §10.
 */
@ApplicationScoped
public class GetSystemInfoPreProcessor implements Processor {

    private static final Logger LOG = Logger.getLogger(GetSystemInfoPreProcessor.class);

    @Inject
    NormalizationConfigService configService;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> snapshot = configService.getSystemInfoSnapshot();
        String json = objectMapper.writeValueAsString(snapshot);

        exchange.getMessage().setHeader("Content-Type", "application/json");
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        exchange.getMessage().setBody(json);
        exchange.setProperty("CamelRouteStop", Boolean.TRUE);

        LOG.debug("EPBO /system-info short-circuited: returning normalization config snapshot");
    }
}
