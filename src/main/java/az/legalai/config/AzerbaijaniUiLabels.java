package az.legalai.config;

import az.legalai.document.domain.DocumentStatus;
import az.legalai.document.domain.DocumentType;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("uiLabels")
public class AzerbaijaniUiLabels {
    public String status(DocumentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case UPLOADED -> "Yüklənib";
            case PARSING -> "Mətn çıxarılır";
            case CLEANING -> "Mətn təmizlənir";
            case STRUCTURE_PARSING -> "Quruluş müəyyən edilir";
            case CHUNKING -> "Fraqmentlər yaradılır";
            case EMBEDDING -> "Vektorlar yaradılır";
            case COMPLETED -> "Tamamlanıb";
            case FAILED -> "Xəta baş verib";
        };
    }

    public String documentType(DocumentType type) {
        if (type == null) return "Göstərilməyib";
        return switch (type) {
            case LAW -> "Qanun";
            case CODE -> "Məcəllə";
            case DECREE -> "Fərman";
            case REGULATION -> "Əsasnamə";
            case DECISION -> "Qərar";
            case CONTRACT -> "Müqavilə";
            case OTHER -> "Digər";
        };
    }

    public String language(String code) {
        if (code == null || code.isBlank()) return "Göstərilməyib";
        return switch (code.toLowerCase(Locale.ROOT)) {
            case "az" -> "Azərbaycan dili";
            case "ru" -> "Rus dili";
            case "en" -> "İngilis dili";
            default -> code.toUpperCase(Locale.ROOT);
        };
    }
}
