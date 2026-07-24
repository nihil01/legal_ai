package az.legalai.ai;

public class AiProviderNotConfiguredException extends IllegalStateException {
    public AiProviderNotConfiguredException(String capability) {
        super(capability + " is not configured. Set OPENAI_API_KEY.");
    }
}
