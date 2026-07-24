package az.legalai.ai.generation;

public record TextGenerationRequest(
        String systemPrompt, String userPrompt, double temperature, int maxTokens) {
    public TextGenerationRequest {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("User prompt must not be blank");
        }
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }
        systemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        userPrompt = userPrompt.trim();
    }
}
