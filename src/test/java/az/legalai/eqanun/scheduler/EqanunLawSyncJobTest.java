package az.legalai.eqanun.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import az.legalai.eqanun.service.EqanunLawSyncService;
import org.junit.jupiter.api.Test;

class EqanunLawSyncJobTest {
    @Test
    void delegatesScheduledExecutionToSyncService() throws Exception {
        EqanunLawSyncService syncService = mock(EqanunLawSyncService.class);
        EqanunLawSyncJob job = new EqanunLawSyncJob();
        job.setSyncService(syncService);

        job.execute(null);

        verify(syncService).synchronize();
    }
}
