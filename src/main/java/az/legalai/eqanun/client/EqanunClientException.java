package az.legalai.eqanun.client;

public class EqanunClientException extends RuntimeException {
    public EqanunClientException(String message) {
        super(message);
    }

    public EqanunClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
