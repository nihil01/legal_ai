package az.legalai.eqanun.scheduler;

import az.legalai.eqanun.service.EqanunLawSyncService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@DisallowConcurrentExecution
public class EqanunLawSyncJob implements Job {
    private EqanunLawSyncService syncService;

    @Autowired
    public void setSyncService(EqanunLawSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (syncService == null) {
            throw new JobExecutionException("E-qanun sync service was not injected");
        }
        try {
            syncService.synchronize();
        } catch (RuntimeException exception) {
            throw new JobExecutionException("E-qanun synchronization failed", exception, false);
        }
    }
}
