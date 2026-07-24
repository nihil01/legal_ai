package az.legalai.document.service;

import az.legalai.document.domain.LegalDocument;
import az.legalai.document.repository.LegalDocumentRepository;
import az.legalai.job.DocumentProcessingJob;
import az.legalai.job.DocumentProcessingJobRepository;
import az.legalai.storage.DocumentStorage;
import az.legalai.storage.StoredFile;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentUploadService {
    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    private final DocumentValidator validator;
    private final DocumentStorage storage;
    private final LegalDocumentRepository documents;
    private final DocumentProcessingJobRepository jobs;

    public DocumentUploadService(
            DocumentValidator validator,
            DocumentStorage storage,
            LegalDocumentRepository documents,
            DocumentProcessingJobRepository jobs) {
        this.validator = validator;
        this.storage = storage;
        this.documents = documents;
        this.jobs = jobs;
    }

    @Transactional
    public UUID upload(MultipartFile file, UploadCommand command) {
        ValidatedDocument validated = validator.validate(file);
        if (documents.findByChecksum(validated.checksum()).isPresent())
            throw new DuplicateDocumentException();
        StoredFile stored = storage.store(validated.originalFilename(), validated.bytes());
        registerRollbackCleanup(stored.storageKey());

        UUID id = UUID.randomUUID();
        String title =
                command.title() == null || command.title().isBlank()
                        ? validated.originalFilename()
                        : command.title().trim();
        documents.save(
                new LegalDocument(
                        id,
                        title,
                        validated.originalFilename(),
                        command.documentType(),
                        command.language(),
                        command.sourceUrl(),
                        stored.storageKey(),
                        validated.mimeType(),
                        validated.size(),
                        command.adoptionDate(),
                        command.effectiveDate(),
                        validated.checksum()));
        jobs.save(new DocumentProcessingJob(UUID.randomUUID(), id));
        documents.flush();
        jobs.flush();
        return id;
    }

    private void registerRollbackCleanup(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_ROLLED_BACK) return;
                        try {
                            storage.delete(storageKey);
                        } catch (RuntimeException exception) {
                            log.error(
                                    "Upload transaction rolled back but file cleanup failed: {}",
                                    storageKey,
                                    exception);
                        }
                    }
                });
    }
}
