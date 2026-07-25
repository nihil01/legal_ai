package az.legalai.document.repository;

import az.legalai.document.domain.LegalDocument;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {
    Optional<LegalDocument> findByChecksumAndExternalSourceIsNull(String checksum);

    Optional<LegalDocument> findByExternalSourceAndExternalIdAndExternalVersionId(
            String externalSource, String externalId, String externalVersionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LegalDocument> findFirstByExternalSourceAndExternalIdOrderByVersionNumberDesc(
            String externalSource, String externalId);
}
