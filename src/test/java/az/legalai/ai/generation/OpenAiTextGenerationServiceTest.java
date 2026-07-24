package az.legalai.ai.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiTextGenerationServiceTest {
    @Test
    void returnsAssistantTextFromChatCompletion() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.example.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(
                        withSuccess(
                                "{\"choices\":[{\"message\":{\"content\":\"Cavab\"}}]}",
                                MediaType.APPLICATION_JSON));

        TextGenerationService service =
                new OpenAiTextGenerationService(
                        builder.build(), new ObjectMapper(), "test-key", "gpt-4.1-mini");

        String result =
                service.generate(
                        new TextGenerationRequest("You are a legal assistant", "Sual", 0.2, 300));

        assertThat(result).isEqualTo("Cavab");
        server.verify();
    }
}
