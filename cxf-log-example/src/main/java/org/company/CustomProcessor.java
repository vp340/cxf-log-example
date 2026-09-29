package org.company;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import schema.ws.example.v1.PlutoResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

public final class CustomProcessor implements Processor {

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

        PlutoResponse response = new PlutoResponse();
        response.setResponseNumber(0);
        DataHandler dataHandler = new DataHandler(
                new ByteArrayDataSource(fileBytes, "application/octet-stream")
        );
        response.setAllegatoFile(dataHandler);

        exchange.getMessage().setBody(response);
    }
}