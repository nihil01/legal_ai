package az.legalai.document.repository;

import java.util.UUID;

public record ChunkView(
        UUID id,
        int chunkIndex,
        String sectionType,
        String sectionNumber,
        String sectionTitle,
        String articleNumber,
        String clauseNumber,
        String parentPath,
        String content,
        int tokenCount,
        String metadata,
        Double similarity) {}
