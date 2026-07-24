package az.legalai.config;

import az.legalai.document.service.DocumentValidator;
import az.legalai.embedding.EmbeddingService;
import az.legalai.embedding.LocalHashEmbeddingService;
import az.legalai.embedding.OpenAiEmbeddingService;
import az.legalai.ingestion.chunker.LegalDocumentChunker;
import az.legalai.ingestion.cleaner.LegalTextCleaner;
import az.legalai.ingestion.parser.AzerbaijaniLegalStructureParser;
import az.legalai.ingestion.parser.LegalStructureParser;
import az.legalai.storage.DocumentStorage;
import az.legalai.storage.LocalDocumentStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {
    @Bean
    DocumentValidator documentValidator(@Value("${app.upload.max-bytes:26214400}") long max) {
        return new DocumentValidator(max);
    }

    @Bean
    DocumentStorage documentStorage(
            @Value("${app.storage.path:/data/legal-ai/original-documents}") String path) {
        return new LocalDocumentStorage(Path.of(path));
    }

    @Bean
    LegalTextCleaner legalTextCleaner() {
        return new LegalTextCleaner();
    }

    @Bean
    LegalStructureParser legalStructureParser() {
        return new AzerbaijaniLegalStructureParser();
    }

    @Bean
    LegalDocumentChunker legalDocumentChunker(
            @Value("${app.processing.max-chunk-characters:6000}") int max) {
        return new LegalDocumentChunker(max);
    }

    @Bean
    EmbeddingService embeddingService(
            @Value("${app.embedding.provider:local}") String provider,
            @Value("${app.embedding.dimension:1536}") int dimension,
            @Value("${app.embedding.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.embedding.api-key:}") String apiKey,
            @Value("${app.embedding.model:text-embedding-3-small}") String model,
            @Value("${app.embedding.connect-timeout-seconds:10}") long connectTimeoutSeconds,
            @Value("${app.embedding.read-timeout-seconds:60}") long readTimeoutSeconds,
            ObjectMapper mapper) {
        if ("openai".equalsIgnoreCase(provider)) {
            var requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
            requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
            return new OpenAiEmbeddingService(
                    RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build(),
                    mapper,
                    apiKey,
                    model,
                    dimension);
        }
        return new LocalHashEmbeddingService(dimension);
    }
}
