import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class Wiremock {

    private static WireMockServer wireMockServer;


    public static void main(String[] args) {
        startServer();
    }

    public static void startServer() {
        // 1. Configura e avvia il server sulla porta 8081
        // Nota: Jetty (usato da WireMock) applica nativamente il TCP_NODELAY su localhost
        wireMockServer = new WireMockServer(wireMockConfig().port(8081));
        wireMockServer.start();


        // 2. Associa il client di configurazione all'istanza locale
        WireMock.configureFor("localhost", 8081);

        String soapResponse = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:v1="http://ws.schema/example/v1"
                                  xmlns:xop="http://w3.org">
                    <soapenv:Body>
                        <v1:plutoResponse>
                            <v1:responseNumber>1</v1:responseNumber>
                            <v1:allegatoFile>
                                <!-- Se serve simulare un file (es. MTOM/Base64), puoi inserire qui la stringa codificata -->
                            </v1:allegatoFile>
                        </v1:plutoResponse>
                    </soapenv:Body>
                </soapenv:Envelope>
                """;

        // 3. Definisci lo stub esattamente come da tuo JSON
        stubFor(post(urlEqualTo("/mockExampleBE"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml; charset=UTF-8")
                        .withBody(soapResponse)

                )); // Cerca in src/test/resources/__files/

        System.out.println("=== WireMock Server attivo nativamente su http://localhost:8081 ===");
    }
}
