package system.solutions.com.api_sunat.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class TenantResolver implements CurrentTenantIdentifierResolver<UUID> {

    // UUID por defecto (Fallback) seguro para evitar el bloqueo durante la inicialización de Spring
    private static final UUID DEFAULT_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID current = TenantContext.getCurrentTenant();
        return current != null ? current : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}