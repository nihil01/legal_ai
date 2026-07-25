package az.legalai.eqanun.service;

import az.legalai.eqanun.client.EqanunApiClient;
import java.time.Clock;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DefaultEqanunLawSyncService implements EqanunLawSyncService {

    private final EqanunApiClient client;
    private final EqanunLawImporter importer;
    private final List<String> codexIds;
    private final Clock clock;

    public DefaultEqanunLawSyncService(
            EqanunApiClient client,
            EqanunLawImporter importer,
            List<String> codexIds,
            Clock clock) {
        this.client = client;
        this.importer = importer;
        this.codexIds = List.copyOf(codexIds);
        this.clock = clock;
    }

    @Override
    public EqanunSyncReport synchronize() {
        int discovered = 0;
        int updated = 0;
        int unchanged = 0;
        int failed = 0;

        for (String codexId : codexIds) {
            try {
                var candidate = client.findLatestVersion(codexId);
                if (candidate.isEmpty()) {
                    failed++;
                    log.warn("E-qanun returned no versions for codex {}", codexId);
                    continue;
                }
                discovered++;
                if (importer.isImported(candidate.get())) {
                    unchanged++;
                    continue;
                }

                var document = client.downloadCurrentDocument(codexId);
                EqanunImportOutcome outcome = importer.importLaw(candidate.get(), document);
                if (outcome == EqanunImportOutcome.IMPORTED) updated++;
                else unchanged++;
            } catch (RuntimeException exception) {
                failed++;
                log.error("Failed to synchronize e-qanun codex {}", codexId, exception);
            }
        }

        String note =
                "E-qanun synchronization completed: %d imported, %d unchanged, %d failed"
                        .formatted(updated, unchanged, failed);
        EqanunSyncReport report =
                new EqanunSyncReport(discovered, updated, unchanged, failed, clock.instant(), note);
        log.info(note);
        return report;
    }
}
