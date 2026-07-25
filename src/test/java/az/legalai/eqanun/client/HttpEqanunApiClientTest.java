package az.legalai.eqanun.client;

import static org.assertj.core.api.Assertions.assertThat;

import az.legalai.eqanun.parser.EqanunLawCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpEqanunApiClientTest {
    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void selectsNewestVersionAndDownloadsDocumentFromPlainTextLink() {
        server.createContext(
                "/getVersions",
                exchange -> {
                    byte[] body =
                            """
                            {"data":[
                              {"id":19050,"title":"Test Məcəlləsi","effectDate":"03.03.2026"},
                              {"id":19322,"title":"Test Məcəlləsi","effectDate":"21.04.2026"},
                              {"id":19770,"title":"Test Məcəlləsi","effectDate":"21.04.2026"}
                            ]}
                            """
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        server.createContext(
                "/downloadWord/46960",
                exchange -> {
                    byte[] body =
                            baseUri.resolve("/files/46960.doc")
                                    .toString()
                                    .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });
        byte[] document = new byte[] {'P', 'K', 3, 4, 9, 8, 7};
        server.createContext(
                "/files/46960.doc",
                exchange -> {
                    exchange.sendResponseHeaders(200, document.length);
                    exchange.getResponseBody().write(document);
                    exchange.close();
                });

        EqanunApiClient client =
                new HttpEqanunApiClient(
                        HttpClient.newHttpClient(),
                        new ObjectMapper(),
                        baseUri,
                        URI.create("https://e-qanun.az/framework/"),
                        Set.of("127.0.0.1"),
                        1024,
                        java.time.Duration.ofSeconds(5));

        EqanunLawCandidate latest = client.findLatestVersion("46960").orElseThrow();

        assertThat(latest.externalId()).isEqualTo("46960");
        assertThat(latest.externalVersionId()).isEqualTo("19770");
        assertThat(latest.title()).isEqualTo("Test Məcəlləsi");
        assertThat(latest.effectiveDate()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(latest.sourceUrl()).isEqualTo("https://e-qanun.az/framework/46960");
        EqanunDocumentPayload downloaded = client.downloadCurrentDocument("46960");
        assertThat(downloaded.bytes()).isEqualTo(document);
        assertThat(downloaded.filename()).isEqualTo("eqanun-46960.docx");
        assertThat(downloaded.mimeType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
