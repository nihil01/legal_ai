package az.legalai.job;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobLeaseGuard {
    private final JdbcTemplate jdbc;

    public JobLeaseGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(JobClaim job) {
        var rows =
                jdbc.query(
                        """
                        SELECT 1
                        FROM document_processing_jobs
                        WHERE id = ? AND document_id = ? AND status = 'PROCESSING'
                          AND locked_by = ? AND lock_token = ?
                        FOR UPDATE
                        """,
                        (resultSet, rowNumber) -> resultSet.getInt(1),
                        job.id(),
                        job.documentId(),
                        job.worker(),
                        job.lockToken());
        if (rows.isEmpty()) throw new JobLeaseLostException(job);
    }
}
