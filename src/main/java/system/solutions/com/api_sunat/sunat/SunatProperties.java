package system.solutions.com.api_sunat.sunat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Component
@ConfigurationProperties(prefix = "sunat")
public class SunatProperties {

    private String ambiente = "BETA";
    private String betaEndpoint = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";
    private String produccionEndpoint = "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService";
    private Path certificateRoot = Path.of("secrets", "certificates");
    private Path logoRoot = Path.of("data", "logos");
    private Path documentRoot = Path.of("data", "sunat");
    private Path schemaDirectory = Path.of("schemas", "sunat");
    private int connectTimeoutSeconds = 10;
    private int responseTimeoutSeconds = 45;
    private int maxBajaDias = 7;
    private Map<UUID, TenantCredentials> tenants = new HashMap<>();

    public String endpoint() {
        return switch (ambiente.toUpperCase(Locale.ROOT)) {
            case "BETA" -> betaEndpoint;
            case "PRODUCCION" -> produccionEndpoint;
            default -> throw new IllegalStateException("El ambiente SUNAT debe ser BETA o PRODUCCION.");
        };
    }

    @Data
    public static class TenantCredentials {
        private String sunatUsername;
        private String sunatPassword;
        private String certificatePassword;
    }
}
