package az.legalai.ingestion.extractor;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DocumentExtractorRegistry {
    private final List<DocumentTextExtractor> extractors;

    public DocumentExtractorRegistry(List<DocumentTextExtractor> e) {
        extractors = e;
    }

    public DocumentTextExtractor get(String mime, String filename) {
        return extractors.stream()
                .filter(e -> e.supports(mime, filename))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Нет extractor для " + mime + " / " + filename));
    }
}
