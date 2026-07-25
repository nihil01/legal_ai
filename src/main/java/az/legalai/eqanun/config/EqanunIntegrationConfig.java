package az.legalai.eqanun.config;

import az.legalai.eqanun.client.EqanunApiClient;
import az.legalai.eqanun.client.HttpEqanunApiClient;
import az.legalai.eqanun.service.DefaultEqanunLawSyncService;
import az.legalai.eqanun.service.EqanunLawImporter;
import az.legalai.eqanun.service.EqanunLawSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        prefix = "app.eqanun.sync",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class EqanunIntegrationConfig {
    @Bean
    @ConditionalOnMissingBean(EqanunApiClient.class)
    EqanunApiClient eqanunApiClient(
            ObjectMapper objectMapper,
            @Value("${app.eqanun.api-base-url:https://api.e-qanun.az}") String apiBaseUrl,
            @Value("${app.eqanun.public-base-url:https://e-qanun.az/framework/}")
                    String publicBaseUrl,
            @Value("${app.eqanun.allowed-download-hosts:frameworks.e-qanun.az}")
                    String allowedDownloadHosts,
            @Value("${app.eqanun.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${app.eqanun.read-timeout-seconds:60}") long readTimeoutSeconds,
            @Value("${app.eqanun.max-document-bytes:26214400}") long maxDocumentBytes) {
        Duration connectTimeout = positiveDuration(connectTimeoutSeconds, "connect timeout");
        Duration readTimeout = positiveDuration(readTimeoutSeconds, "read timeout");
        HttpClient httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        return new HttpEqanunApiClient(
                httpClient,
                objectMapper,
                URI.create(apiBaseUrl),
                URI.create(publicBaseUrl),
                parseValues(allowedDownloadHosts),
                maxDocumentBytes,
                readTimeout);
    }

    @Bean
    @ConditionalOnMissingBean(EqanunLawSyncService.class)
    EqanunLawSyncService eqanunLawSyncService(
            EqanunApiClient client,
            EqanunLawImporter importer,
            @Value(
                            "${app.eqanun.codex-ids:46960,46944,46947,46945,46959,46943,46948,46940,46941,46942,46946,46950,46951,46952,46953,46955,46956,46957,46958,56187}")
                    String codexIds) {
        List<String> ids = parseList(codexIds);
        if (ids.isEmpty())
            throw new IllegalArgumentException("E-qanun codex id list must not be empty");
        return new DefaultEqanunLawSyncService(client, importer, ids, Clock.systemUTC());
    }

    private Set<String> parseValues(String csv) {
        return parseList(csv).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<String> parseList(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private Duration positiveDuration(long seconds, String name) {
        if (seconds <= 0)
            throw new IllegalArgumentException("E-qanun " + name + " must be positive");
        return Duration.ofSeconds(seconds);
    }
}
