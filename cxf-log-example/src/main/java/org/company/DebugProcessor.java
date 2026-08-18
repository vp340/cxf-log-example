package org.company;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.Map;


public final class DebugProcessor implements Processor {

    public void process(Exchange exchange) {
        String phase = exchange.getProperty("debugPhase", String.class);
        Map<String, ?> responseContext =
                exchange.getIn().getHeader("ResponseContext", Map.class);

        Boolean logEnable = null;

        if (responseContext != null) {
            logEnable = (Boolean) responseContext.get("org.apache.cxf.logging.enable");
        }


        System.out.println(phase + " : "+"abracadabra: " + logEnable);    }
}