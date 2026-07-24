package az.legalai.ai.transcription;

import az.legalai.ai.AiProviderNotConfiguredException;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SpeechTranscriptionController {
    private final SpeechToTextService service;
    private final long maxAudioBytes;

    public SpeechTranscriptionController(
            SpeechToTextService service,
            @Value("${app.openai.max-audio-bytes:10485760}") long maxAudioBytes) {
        if (maxAudioBytes <= 0) throw new IllegalArgumentException("Audio limit must be positive");
        this.service = service;
        this.maxAudioBytes = maxAudioBytes;
    }

    @PostMapping("/admin/api/transcriptions")
    public ResponseEntity<?> transcribe(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(required = false, defaultValue = "az") String language) {
        if (audio.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Audio recording is empty"));
        }
        if (audio.getSize() > maxAudioBytes) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "Audio recording is too large"));
        }
        String contentType = normalizeContentType(audio.getContentType());
        if (!contentType.startsWith("audio/")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("error", "Unsupported audio format"));
        }
        try {
            String text =
                    service.transcribe(
                            new SpeechTranscriptionRequest(
                                    safeFilename(audio.getOriginalFilename()),
                                    contentType,
                                    audio.getBytes(),
                                    normalizeLanguage(language)));
            return ResponseEntity.ok(Map.of("text", text));
        } catch (AiProviderNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", exception.getMessage()));
        } catch (IOException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot read audio recording"));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Speech-to-text provider request failed"));
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) return "application/octet-stream";
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLanguage(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z]{2,3}") ? normalized : "";
    }

    private String safeFilename(String originalFilename) {
        String filename =
                StringUtils.cleanPath(
                        originalFilename == null ? "recording.webm" : originalFilename);
        filename = filename.replace("\r", "").replace("\n", "");
        if (filename.length() > 255) filename = filename.substring(filename.length() - 255);
        return filename.isBlank() ? "recording.webm" : filename;
    }
}
