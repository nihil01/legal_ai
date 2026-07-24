package az.legalai.document.service;

import az.legalai.document.domain.DocumentType;
import java.net.URI;
import java.time.LocalDate;
import java.util.Locale;

public record UploadCommand(
        String title,
        DocumentType documentType,
        String sourceUrl,
        LocalDate adoptionDate,
        LocalDate effectiveDate,
        String language) {
    public UploadCommand {
        title = normalizeTitle(title);
        sourceUrl = normalizeSourceUrl(sourceUrl);
        language = normalizeLanguage(language);
    }

    private static String normalizeTitle(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 500) {
            throw new DocumentValidationException("Название документа слишком длинное");
        }
        return normalized;
    }

    private static String normalizeSourceUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 2048) {
            throw new DocumentValidationException("URL источника слишком длинный");
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!(scheme.equals("http") || scheme.equals("https"))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new DocumentValidationException(
                        "URL источника должен использовать HTTP или HTTPS");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new DocumentValidationException("URL источника имеет неверный формат");
        }
    }

    private static String normalizeLanguage(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z]{2,8}(?:-[a-z0-9]{1,8})*")) {
            throw new DocumentValidationException("Неверный код языка");
        }
        return normalized;
    }
}
