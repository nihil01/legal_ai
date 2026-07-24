package az.legalai.ai.transcription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiSpeechToTextServiceTest {
    @Test
    void returnsTranscriptFromMultipartAudioRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.example.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(
                        withSuccess("{\"text\":\"əmək müqaviləsi\"}", MediaType.APPLICATION_JSON));

        SpeechToTextService service =
                new OpenAiSpeechToTextService(
                        builder.build(), new ObjectMapper(), "test-key", "gpt-4o-mini-transcribe");

        String result =
                service.transcribe(
                        new SpeechTranscriptionRequest(
                                "voice.webm",
                                "audio/webm",
                                "audio".getBytes(StandardCharsets.UTF_8),
                                "az"));

        assertThat(result).isEqualTo("əmək müqaviləsi");
        server.verify();
    }
}
