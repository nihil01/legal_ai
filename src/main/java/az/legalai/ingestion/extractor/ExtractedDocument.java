package az.legalai.ingestion.extractor;

import az.legalai.ingestion.cleaner.TextBlock;
import java.util.List;
import java.util.Map;

public record ExtractedDocument(
        String rawText, List<TextBlock> blocks, Map<String, String> metadata) {}
