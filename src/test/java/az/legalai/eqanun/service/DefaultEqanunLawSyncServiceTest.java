package az.legalai.eqanun.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.legalai.eqanun.client.EqanunApiClient;
import az.legalai.eqanun.client.EqanunDocumentPayload;
import az.legalai.eqanun.parser.EqanunLawCandidate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultEqanunLawSyncServiceTest {
    @Test
    void importsOnlyChangedCodicesAndContinuesAfterOneCodexFails() {
        EqanunApiClient client = mock(EqanunApiClient.class);
        EqanunLawImporter importer = mock(EqanunLawImporter.class);
        EqanunLawCandidate changed = candidate("46960", "19770");
        EqanunLawCandidate unchanged = candidate("46944", "20001");
        byte[] document = new byte[] {1, 2, 3};
        EqanunDocumentPayload payload =
                new EqanunDocumentPayload(document, "eqanun-46960.doc", "application/msword");

        when(client.findLatestVersion("46960")).thenReturn(Optional.of(changed));
        when(client.findLatestVersion("46944")).thenReturn(Optional.of(unchanged));
        when(client.findLatestVersion("broken")).thenThrow(new IllegalStateException("boom"));
        when(importer.isImported(changed)).thenReturn(false);
        when(importer.isImported(unchanged)).thenReturn(true);
        when(client.downloadCurrentDocument("46960")).thenReturn(payload);
        when(importer.importLaw(changed, payload)).thenReturn(EqanunImportOutcome.IMPORTED);
        Clock clock = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);
        EqanunLawSyncService service =
                new DefaultEqanunLawSyncService(
                        client, importer, List.of("46960", "46944", "broken"), clock);

        EqanunSyncReport report = service.synchronize();

        assertThat(report.discovered()).isEqualTo(2);
        assertThat(report.updated()).isEqualTo(1);
        assertThat(report.unchanged()).isEqualTo(1);
        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.completedAt()).isEqualTo(clock.instant());
        verify(client).downloadCurrentDocument("46960");
        verify(client, never()).downloadCurrentDocument("46944");
        verify(importer).importLaw(changed, payload);
    }

    private EqanunLawCandidate candidate(String codexId, String versionId) {
        return new EqanunLawCandidate(
                codexId,
                versionId,
                "Test Məcəlləsi",
                "https://e-qanun.az/framework/" + codexId,
                LocalDate.of(2026, 4, 21));
    }
}
