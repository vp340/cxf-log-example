package org.company;

import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import schema.ws.example.v1.PlutoRequest;
import schema.ws.example.v1.PlutoResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public final class CustomProcessor2 implements Processor {

    public void process(Exchange exchange) throws IOException {

        exchange.getMessage().getHeaders().clear();

        byte[] fileBytes;

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("big_file.txt")) {

            if (is == null) {
                throw new FileNotFoundException("big_file.txt not found in classpath");
            }

            fileBytes = is.readAllBytes();
        }

        PlutoRequest request = exchange.getMessage().getBody(PlutoRequest.class);

        request.setPippo(Arrays.toString(fileBytes));

        exchange.getMessage().setBody(request);
    }
}