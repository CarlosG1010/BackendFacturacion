package system.solutions.com.api_sunat.sunat;

public class SunatIntegrationException extends RuntimeException {

    public SunatIntegrationException(String message) {
        super(message);
    }

    public SunatIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
