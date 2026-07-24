package az.legalai.config;

import az.legalai.ai.AiProviderNotConfiguredException;
import az.legalai.ai.generation.OpenAiTextGenerationService;
import az.legalai.ai.generation.TextGenerationService;
import az.legalai.ai.transcription.OpenAiSpeechToTextService;
import az.legalai.ai.transcription.SpeechToTextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {
    @Bean
    TextGenerationService textGenerationService(
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.text-model:gpt-4.1-mini}") String model,
            @Value("${app.openai.connect-timeout-seconds:10}") long connectTimeout,
            @Value("${app.openai.read-timeout-seconds:120}") long readTimeout,
            ObjectMapper mapper) {
        if (apiKey == null || apiKey.isBlank()) {
            return request -> {
                throw new AiProviderNotConfiguredException("Text generation");
            };
        }
        return new OpenAiTextGenerationService(
                client(baseUrl, connectTimeout, readTimeout), mapper, apiKey, model);
    }

    @Bean
    SpeechToTextService speechToTextService(
            @Value("${app.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.transcription-model:gpt-4o-mini-transcribe}") String model,
            @Value("${app.openai.connect-timeout-seconds:10}") long connectTimeout,
            @Value("${app.openai.read-timeout-seconds:120}") long readTimeout,
            ObjectMapper mapper) {
        if (apiKey == null || apiKey.isBlank()) {
            return request -> {
                throw new AiProviderNotConfiguredException("Speech-to-text");
            };
        }
        return new OpenAiSpeechToTextService(
                client(baseUrl, connectTimeout, readTimeout), mapper, apiKey, model);
    }

    private RestClient client(String baseUrl, long connectTimeout, long readTimeout) {
        if (connectTimeout <= 0 || readTimeout <= 0) {
            throw new IllegalArgumentException("OpenAI timeouts must be positive");
        }
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
