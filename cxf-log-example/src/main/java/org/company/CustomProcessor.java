package org.company;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public final class CustomProcessor implements Processor {

    public void process(Exchange exchange) {
        exchange.getOut().setBody(exchange.getIn().getBody());
    }
}