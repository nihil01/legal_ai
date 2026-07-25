package az.legalai.job;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DocumentProcessingJobStore {
    private final JdbcTemplate jdbc;
    private final int lockTimeoutSeconds;

    public DocumentProcessingJobStore(
            JdbcTemplate jdbc,
            @Value("${app.processing.lock-timeout-seconds:600}") int lockTimeoutSeconds) {
        if (lockTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("Processing lock timeout must be positive");
        }
        this.jdbc = jdbc;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    @Transactional
    public Optional<JobClaim> claim(String worker, int maxAttempts) {
        failExhaustedJobs(maxAttempts);
        UUID lockToken = UUID.randomUUID();
        List<JobClaim> rows =
                jdbc.query(
                        """
                        WITH candidate AS (
                            SELECT id
                            FROM document_processing_jobs
                            WHERE attempts < ? AND (
                                (
                                    status = 'PENDING'
                                    AND (next_attempt_at IS NULL OR next_attempt_at <= now())
                                ) OR (
                                    status = 'PROCESSING'
                                    AND COALESCE(locked_at, updated_at) < now() - make_interval(secs => ?)
                                )
                            )
                            ORDER BY created_at
                            FOR UPDATE SKIP LOCKED
                            LIMIT 1
                        )
                        UPDATE document_processing_jobs j
                        SET status = 'PROCESSING',
                            attempts = attempts + 1,
                            locked_at = now(),
                            locked_by = ?,
                            lock_token = ?,
                            updated_at = now()
                        FROM candidate c
                        WHERE j.id = c.id
                        RETURNING j.id, j.document_id, j.attempts
                        """,
                        (resultSet, rowNumber) ->
                                new JobClaim(
                                        resultSet.getObject(1, UUID.class),
                                        resultSet.getObject(2, UUID.class),
                                        resultSet.getInt(3),
                                        worker,
                                        lockToken),
                        maxAttempts,
                        lockTimeoutSeconds,
                        worker,
                        lockToken);
        return rows.stream().findFirst();
    }

    private void failExhaustedJobs(int maxAttempts) {
        String message = "Maximum processing attempts exceeded";
        jdbc.update(
                """
                WITH candidates AS (
                    SELECT id
                    FROM document_processing_jobs
                    WHERE attempts >= ? AND (
                        status = 'PENDING' OR (
                            status = 'PROCESSING'
                            AND COALESCE(locked_at, updated_at) < now() - make_interval(secs => ?)
                        )
                    )
                    FOR UPDATE SKIP LOCKED
                ), exhausted AS (
                    UPDATE document_processing_jobs j
                    SET status = 'FAILED', error_message = ?, locked_at = null,
                        locked_by = null, lock_token = null, updated_at = now()
                    FROM candidates c
                    WHERE j.id = c.id
                    RETURNING j.document_id
                )
                UPDATE legal_documents d
                SET status = 'FAILED', processing_error = ?, updated_at = now()
                FROM exhausted e
                WHERE d.id = e.document_id
                """,
                maxAttempts,
                lockTimeoutSeconds,
                message,
                message);
    }

    public boolean heartbeat(JobClaim job) {
        return jdbc.update(
                        """
                        UPDATE document_processing_jobs
                        SET locked_at = now(), updated_at = now()
                        WHERE id = ? AND status = 'PROCESSING' AND locked_by = ? AND lock_token = ?
                        """,
                        job.id(),
                        job.worker(),
                        job.lockToken())
                == 1;
    }

    @Transactional
    public boolean complete(JobClaim job) {
        int updated =
                jdbc.update(
                        """
                        UPDATE document_processing_jobs
                        SET status = 'COMPLETED', locked_at = null, locked_by = null,
                            lock_token = null, error_message = null, updated_at = now()
                        WHERE id = ? AND status = 'PROCESSING' AND locked_by = ? AND lock_token = ?
                        """,
                        job.id(),
                        job.worker(),
                        job.lockToken());
        if (updated != 1) return false;
        CompletionTarget target =
                jdbc.queryForObject(
                        """
                        SELECT external_source, external_id, version_number, effective_date
                        FROM legal_documents
                        WHERE id = ?
                        """,
                        (row, number) ->
                                new CompletionTarget(
                                        row.getString(1),
                                        row.getString(2),
                                        row.getInt(3),
                                        row.getObject(4, java.time.LocalDate.class)),
                        job.documentId());
        if (target != null && target.externalSource() != null) {
            activateExternalVersion(job.documentId(), target);
        } else {
            jdbc.update(
                    """
                    UPDATE legal_documents
                    SET status = 'COMPLETED', processing_error = null, updated_at = now()
                    WHERE id = ?
                    """,
                    job.documentId());
        }
        return true;
    }

    @Transactional
    public JobFailureOutcome fail(JobClaim job, String error, int maxAttempts) {
        String message = truncateError(error);
        if (job.attempts() >= maxAttempts) {
            int updated =
                    jdbc.update(
                            """
                            UPDATE document_processing_jobs
                            SET status = 'FAILED', error_message = ?, locked_at = null,
                                locked_by = null, lock_token = null, updated_at = now()
                            WHERE id = ? AND status = 'PROCESSING' AND locked_by = ? AND lock_token = ?
                            """,
                            message,
                            job.id(),
                            job.worker(),
                            job.lockToken());
            if (updated != 1) return JobFailureOutcome.LEASE_LOST;
            updateDocumentStatus(job.documentId(), "FAILED", message);
            return JobFailureOutcome.TERMINAL_FAILED;
        }

        int updated =
                jdbc.update(
                        """
                        UPDATE document_processing_jobs
                        SET status = 'PENDING', error_message = ?,
                            next_attempt_at = now() + interval '30 seconds',
                            locked_at = null, locked_by = null, lock_token = null, updated_at = now()
                        WHERE id = ? AND status = 'PROCESSING' AND locked_by = ? AND lock_token = ?
                        """,
                        message,
                        job.id(),
                        job.worker(),
                        job.lockToken());
        if (updated != 1) return JobFailureOutcome.LEASE_LOST;
        updateDocumentStatus(job.documentId(), "UPLOADED", message);
        return JobFailureOutcome.RETRY_SCHEDULED;
    }

    public void enqueue(UUID documentId) {
        jdbc.update(
                """
                INSERT INTO document_processing_jobs(
                    id, document_id, status, attempts, created_at, updated_at
                ) VALUES (?, ?, 'PENDING', 0, now(), now())
                """,
                UUID.randomUUID(),
                documentId);
    }

    private void activateExternalVersion(UUID documentId, CompletionTarget target) {
        jdbc.query(
                """
                SELECT id
                FROM legal_documents
                WHERE external_source = ? AND external_id = ?
                ORDER BY version_number, id
                FOR UPDATE
                """,
                (row, number) -> row.getObject(1, UUID.class),
                target.externalSource(),
                target.externalId());
        Integer newerCompleted =
                jdbc.queryForObject(
                        """
                        SELECT count(*)
                        FROM legal_documents
                        WHERE external_source = ? AND external_id = ?
                          AND status = 'COMPLETED' AND version_number > ?
                        """,
                        Integer.class,
                        target.externalSource(),
                        target.externalId(),
                        target.versionNumber());
        if (newerCompleted != null && newerCompleted > 0) {
            jdbc.update(
                    """
                    UPDATE legal_documents d
                    SET status = 'COMPLETED', processing_error = null, is_current = FALSE,
                        valid_to = (
                            SELECT min(newer.effective_date)
                            FROM legal_documents newer
                            WHERE newer.external_source = ? AND newer.external_id = ?
                              AND newer.status = 'COMPLETED'
                              AND newer.version_number > ?
                        ),
                        updated_at = now()
                    WHERE d.id = ?
                    """,
                    target.externalSource(),
                    target.externalId(),
                    target.versionNumber(),
                    documentId);
            return;
        }
        jdbc.update(
                """
                UPDATE legal_documents
                SET is_current = FALSE, valid_to = ?, updated_at = now()
                WHERE external_source = ? AND external_id = ?
                  AND is_current = TRUE AND id <> ?
                """,
                target.effectiveDate(),
                target.externalSource(),
                target.externalId(),
                documentId);
        jdbc.update(
                """
                UPDATE legal_documents
                SET status = 'COMPLETED', processing_error = null,
                    is_current = TRUE, valid_to = null, updated_at = now()
                WHERE id = ?
                """,
                documentId);
    }

    private void updateDocumentStatus(UUID documentId, String status, String error) {
        jdbc.update(
                """
                UPDATE legal_documents
                SET status = ?, processing_error = ?, updated_at = now()
                WHERE id = ?
                """,
                status,
                error,
                documentId);
    }

    private String truncateError(String error) {
        if (error == null) return "Unknown error";
        return error.substring(0, Math.min(error.length(), 4000));
    }

    private record CompletionTarget(
            String externalSource,
            String externalId,
            int versionNumber,
            java.time.LocalDate effectiveDate) {}
}
