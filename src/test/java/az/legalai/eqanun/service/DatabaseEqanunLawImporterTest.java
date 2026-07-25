package az.legalai.eqanun.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.legalai.document.domain.LegalDocument;
import az.legalai.document.repository.LegalDocumentRepository;
import az.legalai.eqanun.client.EqanunDocumentPayload;
import az.legalai.eqanun.parser.EqanunLawCandidate;
import az.legalai.job.DocumentProcessingJobStore;
import az.legalai.storage.DocumentStorage;
import az.legalai.storage.StoredFile;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseEqanunLawImporterTest {
    @Test
    void storesNewCodexWithExternalVersionMetadataAndEnqueuesIngestion() {
        LegalDocumentRepository documents = mock(LegalDocumentRepository.class);
        DocumentStorage storage = mock(DocumentStorage.class);
        DocumentProcessingJobStore jobs = mock(DocumentProcessingJobStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        EqanunLawCandidate candidate = candidate("46960", "19770", LocalDate.of(2026, 4, 21));
        byte[] bytes = new byte[] {1, 2, 3};
        when(documents.findByExternalSourceAndExternalIdAndExternalVersionId(
                        "EQANUN", "46960", "19770"))
                .thenReturn(Optional.empty());
        when(documents.findFirstByExternalSourceAndExternalIdOrderByVersionNumberDesc(
                        "EQANUN", "46960"))
                .thenReturn(Optional.empty());
        when(storage.store("eqanun-46960-v19770.docx", bytes))
                .thenReturn(new StoredFile("stored.doc"));
        EqanunLawImporter importer = new DatabaseEqanunLawImporter(documents, storage, jobs, jdbc);

        EqanunImportOutcome outcome = importer.importLaw(candidate, payload(bytes));

        assertThat(outcome).isEqualTo(EqanunImportOutcome.IMPORTED);
        ArgumentCaptor<LegalDocument> saved = ArgumentCaptor.forClass(LegalDocument.class);
        verify(documents).save(saved.capture());
        LegalDocument document = saved.getValue();
        assertThat(document.getExternalSource()).isEqualTo("EQANUN");
        assertThat(document.getExternalId()).isEqualTo("46960");
        assertThat(document.getExternalVersionId()).isEqualTo("19770");
        assertThat(document.getVersionNumber()).isEqualTo(1);
        assertThat(document.getDocumentGroupId()).isNotNull();
        assertThat(document.getValidFrom()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(document.getOriginalFilename()).isEqualTo("eqanun-46960-v19770.docx");
        assertThat(document.getMimeType())
                .isEqualTo(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(document.isCurrent()).isFalse();
        verify(jobs).enqueue(document.getId());
    }

    @Test
    void importsNewExternalIdentityEvenWhenContentMatchesEarlierVersion() {
        LegalDocumentRepository documents = mock(LegalDocumentRepository.class);
        DocumentStorage storage = mock(DocumentStorage.class);
        DocumentProcessingJobStore jobs = mock(DocumentProcessingJobStore.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID groupId = UUID.randomUUID();
        LegalDocument current =
                LegalDocument.fromEqanun(
                        UUID.randomUUID(),
                        "Old title",
                        "https://e-qanun.az/framework/46960",
                        "old.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "old-storage",
                        10,
                        "787c798e39a5bc1910355bae6d0cd87a36b2e10fd0202a83e3bb6b005da83472",
                        "46960",
                        "19050",
                        LocalDate.of(2026, 3, 3),
                        groupId,
                        1);
        EqanunLawCandidate candidate = candidate("46960", "19770", LocalDate.of(2026, 4, 21));
        byte[] bytes = new byte[] {4, 5, 6};
        when(documents.findByExternalSourceAndExternalIdAndExternalVersionId(
                        "EQANUN", "46960", "19770"))
                .thenReturn(Optional.empty());
        when(documents.findFirstByExternalSourceAndExternalIdOrderByVersionNumberDesc(
                        "EQANUN", "46960"))
                .thenReturn(Optional.of(current));
        when(storage.store("eqanun-46960-v19770.docx", bytes))
                .thenReturn(new StoredFile("new.doc"));
        EqanunLawImporter importer = new DatabaseEqanunLawImporter(documents, storage, jobs, jdbc);

        importer.importLaw(candidate, payload(bytes));

        ArgumentCaptor<LegalDocument> saved = ArgumentCaptor.forClass(LegalDocument.class);
        verify(documents).save(saved.capture());
        LegalDocument next = saved.getValue();
        assertThat(current.isCurrent()).isFalse();
        assertThat(current.getValidTo()).isNull();
        assertThat(next.getDocumentGroupId()).isEqualTo(groupId);
        assertThat(next.getVersionNumber()).isEqualTo(2);
        assertThat(next.getChecksum()).isEqualTo(current.getChecksum());
        assertThat(next.isCurrent()).isFalse();
    }

    private EqanunDocumentPayload payload(byte[] bytes) {
        return new EqanunDocumentPayload(
                bytes,
                "eqanun-46960.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private EqanunLawCandidate candidate(
            String codexId, String versionId, LocalDate effectiveDate) {
        return new EqanunLawCandidate(
                codexId,
                versionId,
                "Test Məcəlləsi",
                "https://e-qanun.az/framework/" + codexId,
                effectiveDate);
    }
}
