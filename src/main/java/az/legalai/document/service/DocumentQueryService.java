package az.legalai.document.service;

import az.legalai.document.domain.LegalDocument;
import az.legalai.document.repository.*;
import az.legalai.embedding.EmbeddingService;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class DocumentQueryService {
    private final LegalDocumentRepository docs;
    private final DocumentChunkStore chunks;
    private final EmbeddingService embeddings;

    public DocumentQueryService(
            LegalDocumentRepository d, DocumentChunkStore c, EmbeddingService e) {
        docs = d;
        chunks = c;
        embeddings = e;
    }

    public List<LegalDocument> list() {
        return docs.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public LegalDocument get(UUID id) {
        return docs.findById(id).orElseThrow();
    }

    public long chunkCount(UUID id) {
        return chunks.count(id);
    }

    public List<ChunkView> chunks(UUID id) {
        return chunks.list(id);
    }

    public List<ChunkView> search(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();
        return chunks.search(embeddings.embed(query.trim()), Math.max(1, Math.min(limit, 50)));
    }
}
