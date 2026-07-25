package az.legalai.ingestion.pipeline;

import az.legalai.document.domain.DocumentStatus;
import az.legalai.document.repository.*;
import az.legalai.document.service.DocumentStateService;
import az.legalai.embedding.EmbeddingService;
import az.legalai.eqanun.parser.EqanunDocParser;
import az.legalai.eqanun.parser.EqanunLawCandidate;
import az.legalai.eqanun.parser.EqanunParsedLaw;
import az.legalai.ingestion.chunker.*;
import az.legalai.ingestion.cleaner.LegalTextCleaner;
import az.legalai.ingestion.extractor.*;
import az.legalai.ingestion.parser.LegalStructureParser;
import az.legalai.job.JobClaim;
import az.legalai.storage.DocumentStorage;
import java.io.InputStream;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentIngestionPipeline {
    private final LegalDocumentRepository documents;
    private final DocumentStorage storage;
    private final DocumentExtractorRegistry extractors;
    private final LegalTextCleaner cleaner;
    private final LegalStructureParser parser;
    private final EqanunDocParser eqanunParser;
    private final LegalDocumentChunker chunker;
    private final EmbeddingService embeddings;
    private final DocumentChunkStore chunks;
    private final DocumentStateService states;

    public void process(JobClaim job, Runnable leaseHeartbeat) {
        UUID id = job.documentId();
        leaseHeartbeat.run();
        var doc = documents.findById(id).orElseThrow();
        states.update(job, DocumentStatus.PARSING, null);
        leaseHeartbeat.run();
        ExtractedDocument extracted;
        EqanunParsedLaw eqanunParsed = null;
        try (InputStream in = storage.load(doc.getStorageKey())) {
            if ("EQANUN".equals(doc.getExternalSource())) {
                eqanunParsed =
                        eqanunParser.parse(
                                new EqanunLawCandidate(
                                        doc.getExternalId(),
                                        doc.getExternalVersionId(),
                                        doc.getTitle(),
                                        doc.getSourceUrl(),
                                        doc.getEffectiveDate()),
                                doc.getOriginalFilename(),
                                doc.getMimeType(),
                                in);
                extracted = eqanunParsed.extractedDocument();
            } else {
                extracted =
                        extractors.get(doc.getMimeType(), doc.getOriginalFilename()).extract(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Text extraction failed", e);
        }
        leaseHeartbeat.run();
        states.update(job, DocumentStatus.CLEANING, null);
        var cleaned = eqanunParsed == null ? cleaner.clean(extracted.blocks()) : extracted.blocks();
        leaseHeartbeat.run();
        states.update(job, DocumentStatus.STRUCTURE_PARSING, null);
        var root =
                eqanunParsed == null
                        ? parser.parse(doc.getTitle(), cleaned)
                        : eqanunParsed.structure();
        leaseHeartbeat.run();
        states.update(job, DocumentStatus.CHUNKING, null);
        var drafts = chunker.chunk(root);
        if (drafts.isEmpty()) throw new IllegalStateException("Document produced no chunks");
        leaseHeartbeat.run();
        states.update(job, DocumentStatus.EMBEDDING, null);
        List<float[]> vectors = new ArrayList<>();
        int batch = 64;
        for (int i = 0; i < drafts.size(); i += batch) {
            leaseHeartbeat.run();
            var texts =
                    drafts.subList(i, Math.min(i + batch, drafts.size())).stream()
                            .map(ChunkDraft::embeddingContent)
                            .toList();
            vectors.addAll(embeddings.embedBatch(texts));
        }
        leaseHeartbeat.run();
        chunks.replace(job, drafts, vectors);
        leaseHeartbeat.run();
    }
}
