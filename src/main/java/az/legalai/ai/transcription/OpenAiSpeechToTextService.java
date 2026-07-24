package az.legalai.ai.transcription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public final class OpenAiSpeechToTextService implements SpeechToTextService {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public OpenAiSpeechToTextService(
            RestClient client, ObjectMapper mapper, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY is required for speech-to-text");
        }
        this.client = client;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String transcribe(SpeechTranscriptionRequest request) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("model", model);
        if (!request.language().isBlank()) form.add("language", request.language());
        var audioResource =
                new ByteArrayResource(request.audio()) {
                    @Override
                    public String getFilename() {
                        return request.filename();
                    }
                };
        var audioHeaders = new HttpHeaders();
        audioHeaders.setContentType(MediaType.parseMediaType(request.contentType()));
        form.add("file", new HttpEntity<>(audioResource, audioHeaders));

        String body =
                client.post()
                        .uri("/audio/transcriptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(form)
                        .retrieve()
                        .body(String.class);
        try {
            JsonNode root = mapper.readTree(body);
            String text = root.path("text").asText();
            if (text.isBlank()) throw new IllegalStateException("Transcription response is empty");
            return text.trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Speech-to-text API response is invalid", exception);
        }
    }
}
