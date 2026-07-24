package az.legalai.ai.transcription;

public record SpeechTranscriptionRequest(
        String filename, String contentType, byte[] audio, String language) {
    public SpeechTranscriptionRequest {
        if (audio == null || audio.length == 0) {
            throw new IllegalArgumentException("Audio must not be empty");
        }
        filename = filename == null || filename.isBlank() ? "recording.webm" : filename;
        contentType =
                contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType;
        language = language == null ? "" : language.trim();
        audio = audio.clone();
    }

    @Override
    public byte[] audio() {
        return audio.clone();
    }
}
