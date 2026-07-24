package az.legalai.document.service;

import az.legalai.document.domain.DocumentStatus;
import az.legalai.job.JobClaim;
import az.legalai.job.JobLeaseGuard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentStateService {
    private final JdbcTemplate jdbc;
    private final JobLeaseGuard leaseGuard;

    public DocumentStateService(JdbcTemplate jdbc, JobLeaseGuard leaseGuard) {
        this.jdbc = jdbc;
        this.leaseGuard = leaseGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update(JobClaim job, DocumentStatus status, String error) {
        leaseGuard.lock(job);
        jdbc.update(
                """
                UPDATE legal_documents
                SET status = ?, processing_error = ?, updated_at = now()
                WHERE id = ?
                """,
                status.name(),
                error,
                job.documentId());
    }
}
