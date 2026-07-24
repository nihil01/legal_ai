package az.legalai.ingestion.extractor;

import java.io.InputStream;

public interface DocumentTextExtractor {
    boolean supports(String mimeType, String filename);

    ExtractedDocument extract(InputStream input);
}
