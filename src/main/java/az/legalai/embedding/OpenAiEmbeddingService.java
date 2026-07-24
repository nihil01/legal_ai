package az.legalai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public final class OpenAiEmbeddingService implements EmbeddingService {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;
    private final int dimension;

    public OpenAiEmbeddingService(
            RestClient client, ObjectMapper mapper, String apiKey, String model, int dimension) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("EMBEDDING_API_KEY is required for openai provider");
        }
        this.client = client;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        String body =
                client.post()
                        .uri("/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + apiKey)
                        .body(Map.of("model", model, "input", texts, "dimensions", dimension))
                        .retrieve()
                        .body(String.class);
        try {
            JsonNode root = mapper.readTree(body);
            List<JsonNode> items = new ArrayList<>();
            root.path("data").forEach(items::add);
            items.sort(Comparator.comparingInt(item -> item.path("index").asInt()));
            List<float[]> result = new ArrayList<>();
            for (JsonNode item : items) {
                JsonNode array = item.path("embedding");
                float[] vector = new float[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    vector[i] = (float) array.get(i).asDouble();
                    if (!Float.isFinite(vector[i])) {
                        throw new IllegalStateException("Embedding contains a non-finite value");
                    }
                }
                if (vector.length != dimension) {
                    throw new IllegalStateException("Embedding dimension mismatch");
                }
                result.add(vector);
            }
            if (result.size() != texts.size()) {
                throw new IllegalStateException("Embedding count mismatch");
            }
            return List.copyOf(result);
        } catch (Exception e) {
            throw new IllegalStateException("Embedding API response is invalid", e);
        }
    }
}
