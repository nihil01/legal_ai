package az.legalai.ai.transcription;

public interface SpeechToTextService {
    String transcribe(SpeechTranscriptionRequest request);
}
