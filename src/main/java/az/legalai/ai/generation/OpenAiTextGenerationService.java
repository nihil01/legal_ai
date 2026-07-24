package az.legalai.ai.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public final class OpenAiTextGenerationService implements TextGenerationService {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public OpenAiTextGenerationService(
            RestClient client, ObjectMapper mapper, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY is required for text generation");
        }
        this.client = client;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(TextGenerationRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (!request.systemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.userPrompt()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", request.temperature());
        payload.put("max_tokens", request.maxTokens());

        String body =
                client.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(payload)
                        .retrieve()
                        .body(String.class);
        try {
            JsonNode root = mapper.readTree(body);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content.isBlank())
                throw new IllegalStateException("Text generation response is empty");
            return content.trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Text generation API response is invalid", exception);
        }
    }
}
