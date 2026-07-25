package az.legalai.document.service;

import az.legalai.document.domain.LegalDocument;
import az.legalai.document.repository.*;
import az.legalai.embedding.EmbeddingService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentQueryService {
    private final LegalDocumentRepository docs;
    private final DocumentChunkStore chunks;
    private final EmbeddingService embeddings;

    public List<LegalDocument> list() {
        return docs.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public LegalDocument get(UUID id) {
        return docs.findById(id).orElseThrow();
    }

    public long chunkCount(UUID id) {
        return chunks.count(id);
    }

    public ChunkPage chunks(UUID id, String query, int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(10, Math.min(pageSize, 100));
        long total = chunks.count(id, query);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        if (totalPages > 0 && safePage >= totalPages) safePage = totalPages - 1;
        int offset = Math.multiplyExact(safePage, safePageSize);
        return new ChunkPage(
                chunks.list(id, query, safePageSize, offset),
                total,
                safePage,
                safePageSize,
                totalPages);
    }

    public List<ChunkView> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        return chunks.search(embeddings.embed(query.trim()), Math.max(1, Math.min(limit, 50)));
    }
}
