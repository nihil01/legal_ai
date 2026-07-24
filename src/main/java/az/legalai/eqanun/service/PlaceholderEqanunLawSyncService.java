package az.legalai.eqanun.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaceholderEqanunLawSyncService implements EqanunLawSyncService {
    private static final Logger log =
            LoggerFactory.getLogger(PlaceholderEqanunLawSyncService.class);

    @Override
    public EqanunSyncReport synchronize() {
        // TODO(eq-anun): Replace this placeholder with the e-qanun DB/API actuality check,
        // download changed .doc files, parse them through EqanunDocParser and enqueue ingestion.
        log.info("E-qanun synchronization trigger executed; source adapter is not configured yet");
        return EqanunSyncReport.notConfigured();
    }
}
