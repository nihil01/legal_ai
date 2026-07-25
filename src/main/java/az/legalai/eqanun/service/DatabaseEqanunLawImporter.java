package az.legalai.eqanun.service;

import az.legalai.document.domain.LegalDocument;
import az.legalai.document.repository.LegalDocumentRepository;
import az.legalai.eqanun.client.EqanunDocumentPayload;
import az.legalai.eqanun.parser.EqanunLawCandidate;
import az.legalai.job.DocumentProcessingJobStore;
import az.legalai.storage.DocumentStorage;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseEqanunLawImporter implements EqanunLawImporter {
    private static final String SOURCE = "EQANUN";

    private final LegalDocumentRepository documents;
    private final DocumentStorage storage;
    private final DocumentProcessingJobStore jobs;
    private final JdbcTemplate jdbc;

    @Override
    public boolean isImported(EqanunLawCandidate candidate) {
        return documents
                .findByExternalSourceAndExternalIdAndExternalVersionId(
                        SOURCE, candidate.externalId(), candidate.externalVersionId())
                .isPresent();
    }

    @Override
    @Transactional
    public EqanunImportOutcome importLaw(
            EqanunLawCandidate candidate, EqanunDocumentPayload downloadedDocument) {
        byte[] documentBytes = downloadedDocument.bytes();
        lockExternalDocument(candidate.externalId());
        if (isImported(candidate)) return EqanunImportOutcome.UNCHANGED;

        Optional<LegalDocument> latest =
                documents.findFirstByExternalSourceAndExternalIdOrderByVersionNumberDesc(
                        SOURCE, candidate.externalId());
        if (isImported(candidate)) return EqanunImportOutcome.UNCHANGED;

        String checksum = sha256(documentBytes);

        UUID groupId = latest.map(LegalDocument::getDocumentGroupId).orElseGet(UUID::randomUUID);
        int versionNumber = latest.map(document -> document.getVersionNumber() + 1).orElse(1);

        String extension =
                downloadedDocument.mimeType().contains("openxmlformats") ? ".docx" : ".doc";
        String filename =
                "eqanun-%s-v%s%s"
                        .formatted(
                                safeId(candidate.externalId()),
                                safeId(candidate.externalVersionId()),
                                extension);
        var stored = storage.store(filename, documentBytes);
        registerRollbackCleanup(stored.storageKey());

        LegalDocument document =
                LegalDocument.fromEqanun(
                        UUID.randomUUID(),
                        candidate.title(),
                        candidate.sourceUrl(),
                        filename,
                        downloadedDocument.mimeType(),
                        stored.storageKey(),
                        documentBytes.length,
                        checksum,
                        candidate.externalId(),
                        candidate.externalVersionId(),
                        candidate.effectiveDate(),
                        groupId,
                        versionNumber);
        documents.save(document);
        documents.flush();
        jobs.enqueue(document.getId());
        log.info(
                "Imported e-qanun codex {} remote version {} as document {} version {}",
                candidate.externalId(),
                candidate.externalVersionId(),
                document.getId(),
                versionNumber);
        return EqanunImportOutcome.IMPORTED;
    }

    private void lockExternalDocument(String externalId) {
        jdbc.query(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                resultSet -> null,
                SOURCE + ":" + externalId);
    }

    private void registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_ROLLED_BACK) return;
                        try {
                            storage.delete(storageKey);
                        } catch (RuntimeException exception) {
                            log.error(
                                    "E-qanun import rolled back but file cleanup failed: {}",
                                    storageKey,
                                    exception);
                        }
                    }
                });
    }

    private String safeId(String value) {
        String safe = value.replaceAll("[^A-Za-z0-9_-]", "_");
        if (safe.isBlank()) throw new IllegalArgumentException("Invalid e-qanun identifier");
        return safe;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
