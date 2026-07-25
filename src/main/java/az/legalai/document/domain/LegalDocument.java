package az.legalai.document.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "legal_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LegalDocument {
    @Id private UUID id;
    private String title;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type")
    private DocumentType documentType;

    private String language;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "external_source")
    private String externalSource;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "external_version_id")
    private String externalVersionId;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "adoption_date")
    private LocalDate adoptionDate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Column(name = "processing_error")
    private String processingError;

    private String checksum;

    @Column(name = "version_number")
    private int versionNumber = 1;

    @Column(name = "document_group_id")
    private UUID documentGroupId;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_current")
    private boolean current = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public LegalDocument(
            UUID id,
            String title,
            String originalFilename,
            DocumentType documentType,
            String language,
            String sourceUrl,
            String storageKey,
            String mimeType,
            long fileSize,
            LocalDate adoptionDate,
            LocalDate effectiveDate,
            String checksum) {
        this.id = id;
        this.title = title;
        this.originalFilename = originalFilename;
        this.documentType = documentType;
        this.language = language;
        this.sourceUrl = sourceUrl;
        this.storageKey = storageKey;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.adoptionDate = adoptionDate;
        this.effectiveDate = effectiveDate;
        this.checksum = checksum;
        this.status = DocumentStatus.UPLOADED;
        this.versionNumber = 1;
        this.current = true;
    }

    public static LegalDocument fromEqanun(
            UUID id,
            String title,
            String sourceUrl,
            String originalFilename,
            String mimeType,
            String storageKey,
            long fileSize,
            String checksum,
            String externalId,
            String externalVersionId,
            LocalDate effectiveDate,
            UUID documentGroupId,
            int versionNumber) {
        LegalDocument document =
                new LegalDocument(
                        id,
                        title,
                        originalFilename,
                        DocumentType.CODE,
                        "az",
                        sourceUrl,
                        storageKey,
                        mimeType,
                        fileSize,
                        null,
                        effectiveDate,
                        checksum);
        document.externalSource = "EQANUN";
        document.externalId = externalId;
        document.externalVersionId = externalVersionId;
        document.documentGroupId = documentGroupId;
        document.versionNumber = versionNumber;
        document.validFrom = effectiveDate;
        document.current = false;
        return document;
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

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public void setProcessingError(String value) {
        processingError = value;
    }
}
