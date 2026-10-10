package system.solutions.com.api_sunat.sunat;

public record SunatSoapResponse(
        String applicationResponseBase64,
        String ticket,
        String statusCode,
        String statusMessage
) {
}
