package az.legalai.ingestion.chunker;

import az.legalai.ingestion.parser.SectionType;
import java.util.Map;

public record ChunkDraft(
        int chunkIndex,
        SectionType sectionType,
        String sectionNumber,
        String sectionTitle,
        String articleNumber,
        String clauseNumber,
        String parentPath,
        String content,
        String embeddingContent,
        int tokenCount,
        Map<String, Object> metadata) {}
