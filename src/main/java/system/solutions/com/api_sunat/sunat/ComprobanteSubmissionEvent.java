package system.solutions.com.api_sunat.sunat;

import java.util.UUID;

public record ComprobanteSubmissionEvent(UUID comprobanteId, UUID tenantId) {
}
