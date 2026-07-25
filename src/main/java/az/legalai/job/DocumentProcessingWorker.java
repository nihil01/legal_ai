package az.legalai.job;

import az.legalai.ingestion.pipeline.DocumentIngestionPipeline;
import java.net.InetAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DocumentProcessingWorker {

    private final DocumentProcessingJobStore jobs;
    private final DocumentIngestionPipeline pipeline;
    private final int maxAttempts;
    private final String worker;

    public DocumentProcessingWorker(
            DocumentProcessingJobStore jobs,
            DocumentIngestionPipeline pipeline,
            @Value("${app.processing.max-attempts:3}") int maxAttempts) {
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Processing max attempts must be positive");
        }
        this.jobs = jobs;
        this.pipeline = pipeline;
        this.maxAttempts = maxAttempts;
        this.worker = hostname();
    }

    @Scheduled(fixedDelayString = "${app.processing.poll-delay-ms:2000}")
    public void poll() {
        jobs.claim(worker, maxAttempts).ifPresent(this::process);
    }

    private void process(JobClaim job) {
        try {
            pipeline.process(job, () -> renewLease(job));
            if (!jobs.complete(job)) throw new JobLeaseLostException(job);
        } catch (JobLeaseLostException exception) {
            log.warn(
                    "Stopped stale worker for document {} because its processing lease was lost",
                    job.documentId());
        } catch (Exception exception) {
            String error =
                    "Sənədin emalı zamanı xəta baş verdi. Təkrar emalı başladın və sistem jurnalını yoxlayın.";
            log.error("Document {} processing failed", job.documentId(), exception);
            JobFailureOutcome outcome = jobs.fail(job, error, maxAttempts);
            if (outcome == JobFailureOutcome.LEASE_LOST) {
                log.warn(
                        "Did not update failed job {} because another worker owns its lease",
                        job.id());
            }
        }
    }

    private void renewLease(JobClaim job) {
        if (!jobs.heartbeat(job)) throw new JobLeaseLostException(job);
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + ProcessHandle.current().pid();
        } catch (Exception exception) {
            return "worker-" + ProcessHandle.current().pid();
        }
    }
}
