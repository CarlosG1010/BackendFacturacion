package system.solutions.com.api_sunat.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import system.solutions.com.api_sunat.dto.ComprobanteRequestDTO;

@Component
public class ComprobanteAuthorization {

    public boolean puedeEmitir(Authentication authentication, ComprobanteRequestDTO request) {
        if (request == null || authentication == null) {
            return false;
        }

        String tipo = request.getTipoComprobante();
        boolean administrador = tieneRol(authentication, "ROLE_ADMIN_EMPRESA");
        if ("01".equals(tipo) || "03".equals(tipo)) {
            return administrador || tieneRol(authentication, "ROLE_VENDEDOR");
        }
        if ("07".equals(tipo) || "08".equals(tipo)) {
            return administrador || tieneRol(authentication, "ROLE_CONTADOR");
        }
        return false;
    }

    private boolean tieneRol(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
