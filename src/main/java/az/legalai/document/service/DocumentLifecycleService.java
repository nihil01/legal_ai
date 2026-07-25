package az.legalai.document.service;

import az.legalai.document.domain.DocumentStatus;
import az.legalai.document.repository.LegalDocumentRepository;
import az.legalai.job.DocumentProcessingJobStore;
import az.legalai.storage.DocumentStorage;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLifecycleService {

    private final LegalDocumentRepository docs;
    private final DocumentProcessingJobStore jobs;
    private final DocumentStorage storage;

    @Transactional
    public void reprocess(UUID id) {
        var d = docs.findById(id).orElseThrow();
        d.setStatus(DocumentStatus.UPLOADED);
        d.setProcessingError(null);
        docs.save(d);
        jobs.enqueue(id);
    }

    @Transactional
    public void delete(UUID id) {
        var document = docs.findById(id).orElseThrow();
        String storageKey = document.getStorageKey();
        docs.delete(document);
        docs.flush();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            storage.delete(storageKey);
                        } catch (RuntimeException exception) {
                            log.error(
                                    "Database row deleted but original file cleanup failed: {}",
                                    storageKey,
                                    exception);
                        }
                    }
                });
    }
}
