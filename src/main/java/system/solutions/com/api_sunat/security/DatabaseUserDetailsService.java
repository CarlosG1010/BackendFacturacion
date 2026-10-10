package system.solutions.com.api_sunat.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.entity.Usuario;
import system.solutions.com.api_sunat.repository.UsuarioRepository;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final Set<String> GLOBAL_ROLES = Set.of("ADMIN_GLOBAL", "OPERADOR_SISTEMAS");
    private static final Set<String> TENANT_ROLES = Set.of("ADMIN_EMPRESA", "VENDEDOR", "CONTADOR");

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findWithTenantByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas."));

        UUID tenantId = null;
        if (usuario.getTenant() != null) {
            Tenant tenant = usuario.getTenant();
            if (!"ACTIVO".equals(tenant.getEstado())) {
                throw new UsernameNotFoundException("La empresa del usuario está inactiva.");
            }
            tenantId = tenant.getId();
        }

        boolean validRoleScope = tenantId == null
                ? GLOBAL_ROLES.contains(usuario.getRol())
                : TENANT_ROLES.contains(usuario.getRol());
        if (!validRoleScope || !"ACTIVO".equals(usuario.getEstado())) {
            throw new UsernameNotFoundException("La cuenta no está habilitada.");
        }

        return new AuthenticatedUser(
                usuario.getId(),
                tenantId,
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getRol(),
                true
        );
    }
}
