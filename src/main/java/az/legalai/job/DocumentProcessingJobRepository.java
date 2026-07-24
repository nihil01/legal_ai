package az.legalai.job;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentProcessingJobRepository
        extends JpaRepository<DocumentProcessingJob, UUID> {
    void deleteByDocumentId(UUID documentId);
}
