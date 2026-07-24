package az.legalai.job;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_processing_jobs")
public class DocumentProcessingJob {
    @Id private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private int attempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected DocumentProcessingJob() {}

    public DocumentProcessingJob(UUID id, UUID documentId) {
        this.id = id;
        this.documentId = documentId;
        status = JobStatus.PENDING;
        attempts = 0;
    }

    @PrePersist
    void create() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public JobStatus getStatus() {
        return status;
    }
}
