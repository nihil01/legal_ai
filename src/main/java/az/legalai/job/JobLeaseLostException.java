package az.legalai.job;

public class JobLeaseLostException extends RuntimeException {
    public JobLeaseLostException(JobClaim job) {
        super("Processing lease lost for job " + job.id());
    }
}
