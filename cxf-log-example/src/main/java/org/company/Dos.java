package org.company;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
                : 1000;

        int concurrency = args.length > 1
                ? Integer.parseInt(args[1])
                : 100;

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
                            () -> callService(client, requestId)
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

    private static Result callService(
            HttpClient client,
            int requestId) {

        long start = System.nanoTime();

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                            "Content-Type",
                            "text/xml; charset=utf-8"
                    )
                    .header(
                            "Accept",
                            "text/xml"
                    )
                    .POST(
                            HttpRequest.BodyPublishers.ofString(
                                    SOAP_REQUEST
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            long elapsedMs =
                    (System.nanoTime() - start) / 1_000_000;

            boolean success =
                    response.statusCode() >= 200
                            && response.statusCode() < 300;

            return new Result(
                    requestId,
                    response.statusCode(),
                    elapsedMs,
                    success,
                    success ? null : response.body()
            );

        } catch (Exception e) {

            long elapsedMs =
                    (System.nanoTime() - start) / 1_000_000;

            return new Result(
                    requestId,
                    null,
                    elapsedMs,
                    false,
                    e.toString()
            );
        }
    }

    record Result(
            int id,
            Integer statusCode,
            long elapsedMs,
            boolean success,
            String error
    ) {
    }
}

