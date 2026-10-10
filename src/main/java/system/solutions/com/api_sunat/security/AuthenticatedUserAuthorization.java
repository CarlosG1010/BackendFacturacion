package system.solutions.com.api_sunat.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.repository.UsuarioRepository;

import java.util.UUID;

@Component
public class AuthenticatedUserAuthorization {

    private final UsuarioRepository usuarioRepository;

    public AuthenticatedUserAuthorization(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public boolean belongsToCurrentTenant(Authentication authentication, UUID tenantUserId) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUser user)
                || user.tenantId() == null
                || tenantUserId == null) {
            return false;
        }
        return usuarioRepository.findByIdAndTenantId(tenantUserId, user.tenantId())
                .map(usuario -> usuario.getTenant() != null
                        && ("VENDEDOR".equals(usuario.getRol()) || "CONTADOR".equals(usuario.getRol())))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isCurrentTenant(Authentication authentication, UUID requestedTenantId) {
        return authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.tenantId() != null
                && user.tenantId().equals(requestedTenantId);
    }
}
