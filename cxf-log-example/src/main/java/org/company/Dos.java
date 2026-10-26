package org.company;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.time.LocalTime.now;

public class Dos {

    private static final String URL =
            "http://localhost:8080/example/cxf/ExampleService_v1";

    private static final String SOAP_REQUEST = """
            <soapenv:Envelope
                xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                xmlns:v1="http://ws.schema/example/v1">
               <soapenv:Header/>
               <soapenv:Body>
                  <v1:plutoRequest>
                     <v1:requestNumber>4</v1:requestNumber>
                  </v1:plutoRequest>
               </soapenv:Body>
            </soapenv:Envelope>
            """;

    public static void main(String[] args) throws Exception {

        int totalRequests = args.length > 0
                ? Integer.parseInt(args[0])
                : 1;

        int concurrency = args.length > 1
                ? Integer.parseInt(args[1])
                : 1;

        System.out.println("========================================");
        System.out.println("SOAP LOAD TEST");
        System.out.println("========================================");
        System.out.println("URL         : " + URL);
        System.out.println("Requests    : " + totalRequests);
        System.out.println("Concurrency : " + concurrency);
        System.out.println();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrency);

        List<Future<Result>> futures = new ArrayList<>();

        long totalStart = System.nanoTime();

        for (int i = 1; i <= totalRequests; i++) {

            final int requestId = i;

            futures.add(
                    executor.submit(
//                            () -> callService(client, requestId)
                            () -> callAndResetService(requestId)
                    )
            );
        }

        int successful = 0;
        int failed = 0;

        List<Long> latencies = new ArrayList<>();

        for (Future<Result> future : futures) {

            Result result = future.get();

            latencies.add(result.elapsedMs());

            if (result.success()) {
                successful++;
                System.out.printf(
                        "[OK]    #%5d HTTP=%d time=%.3fs%n",
                        result.id(),
                        result.statusCode(),
                        result.elapsedMs() / 1000.0
                );
            } else {
                failed++;
                System.out.printf(
                        "[ERROR] #%5d HTTP=%s time=%.3fs error=%s%n",
                        result.id(),
                        result.statusCode(),
                        result.elapsedMs() / 1000.0,
                        result.error()
                );
                // STAMPA LA RISPOSTA PARZIALE SE PRESENTE
                if (result.responseBody() != null && !result.responseBody().isEmpty()) {
                    System.out.println("        -> Dati parziali intercettati sul socket: "
                            + result.responseBody().replace("\n", " ").trim());
                }
            }
        }

        executor.shutdown();

        long totalElapsedNs = System.nanoTime() - totalStart;

        double totalElapsedSeconds =
                totalElapsedNs / 1_000_000_000.0;

        System.out.println();
        System.out.println("========================================");
        System.out.println("RESULT");
        System.out.println("========================================");

        System.out.println("Total requests : " + totalRequests);
        System.out.println("Successful     : " + successful);
        System.out.println("Failed         : " + failed);

        System.out.printf(
                "Total time     : %.3fs%n",
                totalElapsedSeconds
        );

        System.out.printf(
                "Throughput     : %.2f req/s%n",
                totalRequests / totalElapsedSeconds
        );

        if (!latencies.isEmpty()) {

            long min = latencies.stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0);

            long max = latencies.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0);

            double avg = latencies.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);

            System.out.printf(
                    "Min latency    : %.3fs%n",
                    min / 1000.0
            );

            System.out.printf(
                    "Max latency    : %.3fs%n",
                    max / 1000.0
            );

            System.out.printf(
                    "Avg latency    : %.3fs%n",
                    avg / 1000.0
            );
        }
    }

//    private static Result callService(
//            HttpClient client,
//            int requestId) {
//
//        long start = System.nanoTime();
//
//        try {
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(URL))
//                    .timeout(Duration.ofSeconds(30))
//                    .header(
//                            "Content-Type",
//                            "text/xml; charset=utf-8"
//                    )
//                    .header(
//                            "Accept",
//                            "text/xml"
//                    )
//                    .POST(
//                            HttpRequest.BodyPublishers.ofString(
//                                    SOAP_REQUEST
//                            )
//                    )
//                    .build();
//
//            HttpResponse<String> response =
//                    client.send(
//                            request,
//                            HttpResponse.BodyHandlers.ofString()
//                    );
//
//            long elapsedMs =
//                    (System.nanoTime() - start) / 1_000_000;
//
//            boolean success =
//                    response.statusCode() >= 200
//                            && response.statusCode() < 300;
//
//            return new Result(
//                    requestId,
//                    response.statusCode(),
//                    elapsedMs,
//                    success,
//                    success ? null : response.body()
//            );
//
//        } catch (Exception e) {
//
//            long elapsedMs =
//                    (System.nanoTime() - start) / 1_000_000;
//
//            return new Result(
//                    requestId,
//                    null,
//                    elapsedMs,
//                    false,
//                    e.toString()
//            );
//        }
//    }
//
    record Result(
            int id,
            Integer statusCode,
            long elapsedMs,
            boolean success,
            String responseBody,
            String error
    ) {
    }
    private static Result callAndResetService(int requestId) {
        long start = System.nanoTime();
        String targetUrl = URL; // Usa la stringa http://localhost:8080...

        try {
            java.net.URL url = new java.net.URL(targetUrl);
            String host = url.getHost();
            int port = url.getPort() == -1 ? 80 : url.getPort();
            String path = url.getPath();

            // 1. Apriamo il socket TCP grezzo verso il server Apache CXF
            Socket socket = new Socket(host, port);

            // 2. Prepariamo la richiesta HTTP/1.1 manuale
            String httpRequest = "POST " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + ":" + port + "\r\n" +
                    "Content-Type: text/xml; charset=utf-8\r\n" +
                    "Content-Length: " + SOAP_REQUEST.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                    "Accept: text/xml\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    SOAP_REQUEST;

            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            // 3. Spariamo la richiesta SOAP al server
            os.write(httpRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();

            // 4. Leggiamo solo i primissimi byte della risposta (es. solo fino a "HTTP/1.1 200")
            // per essere sicuri che il server abbia iniziato a rispondere ed elaborare...
            byte[] buffer = new byte[1000000];
            int read = is.read(buffer);


            System.out.println("Soglia di 200KB superata. Eseguo il RESET BRUTALE.");


            // 5. IL TRUCCO DEL RESET BRUTALE:
            // Configura il socket per sparare un TCP RST ed eliminare i buffer immediatamente.
            socket.setSoLinger(true, 0);

            // Chiudiamo il socket a metà. Il server Apache CXF riceverà istantaneamente
            // una IOException (Connection reset by peer) sul suo CachedOutputStream!
            socket.close();

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            return new Result(requestId, 200, elapsedMs, false, new String(buffer, 0, read), "Simulated TCP RST (Connection Reset)");

        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            return new Result(requestId, null, elapsedMs, false, null, "Failed to simulate: " + e.toString());
        }
    }

private static Result callService(HttpClient client, int requestId) {
    long start = System.nanoTime();

    // Buffer dinamico in memoria per accumulare i byte man mano che arrivano dal socket
    java.io.ByteArrayOutputStream partialBuffer = new java.io.ByteArrayOutputStream();
    Integer statusCode = null;
    String exceptionMessage = null;

    try {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("Accept", "text/xml")
                .POST(HttpRequest.BodyPublishers.ofString(SOAP_REQUEST))
                .build();

        // FONDAMENTALE: Chiediamo a Java di darci l'InputStream nativo non appena riceve gli header
        HttpResponse<java.io.InputStream> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );

        statusCode = response.statusCode();

        // Leggiamo lo stream di rete byte per byte (o a blocchi)
        try (java.io.InputStream is = response.body()) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Il ciclo continua finché ci sono dati o finché la connessione non si rompe
            while ((bytesRead = is.read(buffer)) != -1) {
                partialBuffer.write(buffer, 0, bytesRead);
            }
        }

    } catch (Exception e) {
        // Se si rompe qui (es. Connection Reset a metà lettura),
        // catturiamo l'eccezione ma NON perdiamo partialBuffer!
        exceptionMessage = e.toString();
    }

    long elapsedMs = (System.nanoTime() - start) / 1_000_000;

    // Convertiamo i byte accumulati (completi o parziali che siano) in stringa
    String rawContentReceived = partialBuffer.toString(java.nio.charset.StandardCharsets.UTF_8);

    boolean success = exceptionMessage == null
            && statusCode != null
            && statusCode >= 200
            && statusCode < 300;

    return new Result(
            requestId,
            statusCode,
            elapsedMs,
            success,
            rawContentReceived, // <--- Qui dentro avrai il XML troncato o parziale inviato da CXF!
            exceptionMessage
    );
}
}

