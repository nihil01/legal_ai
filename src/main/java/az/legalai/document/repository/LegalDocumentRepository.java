package az.legalai.document.repository;

import az.legalai.document.domain.LegalDocument;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {
    Optional<LegalDocument> findByChecksum(String checksum);
}
