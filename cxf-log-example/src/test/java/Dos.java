import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dos {
    public static void main(String[] args) throws InterruptedException {
        // Configurazione dei parametri del test
        String url = "http://localhost:8080/example/cxf/ExampleService_v1";
        int parallelThreads = 200;      // Livello di parallelismo (numero di thread simultanei)
        int totalRequests = 1000000;       // Numero totale di richieste da inviare

        String soapBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:v1=\"http://ws.schema/example/v1\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <v1:plutoRequest>\n" +
                "         <v1:requestNumber>4</v1:requestNumber>\n" +
                "         <v1:pippo>Z2ZkZ2RmZw==</v1:pippo>\n" +
                "      </v1:plutoRequest>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        // Inizializzazione del thread pool per gestire la concorrenza
        ExecutorService executor = Executors.newFixedThreadPool(parallelThreads);

        // Configurazione del client HTTP associato all'executor
        HttpClient.Builder builder = HttpClient.newBuilder();

        // Costruzione della richiesta SOAP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/xml; charset=utf-8")
                // Alcuni endpoint SOAP vecchio stile richiedono l'header SOAPAction obbligatorio
                // .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                .build();

        System.out.println("Avvio dello stress test su: " + url);
        System.out.println("Parallelismo: " + parallelThreads + " thread | Richieste totali: " + totalRequests);

        long startTime = System.currentTimeMillis();

        // Sottomissione dei task asincroni
        for (int i = 0; i < totalRequests; i++) {

            final int index = i + 1;
            if (index % 100 == 0) {
                Thread.sleep(500); // Mezzo secondo di respiro per l'OS
            }
            executor.submit(() -> {
                try {
                    HttpClient client = builder.build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    client.close();
                    int statusCode = response.statusCode();
                    String responseBody = response.body();

                    System.out.printf("[Richiesta %d] Status Code: %d\n" , index, statusCode);

                    // Verifica se il body della risposta contiene la stringa "PIPPO"
                    if (responseBody != null && responseBody.contains("Too few bytes returned by the publisher")) {
                        System.err.println("\n" + "!".repeat(60));
                        System.err.println("!!! MEGA ALERT: TROVATO 'PIPPO' NELLA RISPOSTA " + index + " !!!");
                        System.err.println("!".repeat(60) + "\n");
//                        exit(1);
                    } else if (responseBody != null && (responseBody.toLowerCase().contains("timeout") || responseBody.toLowerCase().contains("could not send"))) {
                        System.err.println("\n" + "!".repeat(60));
                        System.err.println("!!! MEGA ALERT: TROVATO 'TIMEOUT' NELLA RISPOSTA " + index + " !!!");
                        System.err.println("!".repeat(60) + "\n");
//                        exit(1);
                    }


                } catch (Exception e) {
                    System.err.printf("[Richiesta %d] Errore: %s\n", index, e.getMessage());
                }
            });
        }

        // Chiusura controllata dell'esecutore al termine delle attività
        executor.shutdown();
        try {
            if (executor.awaitTermination(5, TimeUnit.MINUTES)) {
                long duration = System.currentTimeMillis() - startTime;
                System.out.printf("Test completato in %d ms.\n", duration);
            } else {
                System.err.println("Il test ha superato il tempo massimo di attesa (timeout).");
            }
        } catch (InterruptedException e) {
            System.err.println("Attesa interrotta: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
