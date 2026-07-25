package az.legalai.ai.transcription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SpeechTranscriptionControllerTest {
    @Test
    void returnsTranscriptForBrowserRecording() throws Exception {
        SpeechToTextService service = mock(SpeechToTextService.class);
        when(service.transcribe(any())).thenReturn("Maddə on");
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(new SpeechTranscriptionController(service, 1024))
                        .build();

        mvc.perform(
                        multipart("/admin/api/transcriptions")
                                .file(
                                        new MockMultipartFile(
                                                "audio",
                                                "recording.webm",
                                                "audio/webm",
                                                new byte[] {1, 2, 3}))
                                .param("language", "az"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Maddə on"));
    }

    @Test
    void rejectsRecordingAboveConfiguredLimit() throws Exception {
        SpeechToTextService service = mock(SpeechToTextService.class);
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(new SpeechTranscriptionController(service, 2))
                        .build();

        mvc.perform(
                        multipart("/admin/api/transcriptions")
                                .file(
                                        new MockMultipartFile(
                                                "audio",
                                                "recording.webm",
                                                "audio/webm",
                                                new byte[] {1, 2, 3})))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("Səs yazısının ölçüsü çox böyükdür"));
    }
}
