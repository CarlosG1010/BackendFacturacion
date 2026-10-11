package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Usuario;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.repository.TenantRepository;
import system.solutions.com.api_sunat.repository.UsuarioRepository;
import system.solutions.com.api_sunat.service.UsuarioService;
import system.solutions.com.api_sunat.util.RequestInput;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> ROLES_GLOBALES = Set.of("ADMIN_GLOBAL", "OPERADOR_SISTEMAS");
    private static final Set<String> ROLES_SUBORDINADOS = Set.of("VENDEDOR", "CONTADOR");

    @Override
    @Transactional
    public Usuario registrarUsuarioGlobal(Usuario usuario) {
        validarDatosRegistro(usuario);
        validarRol(usuario.getRol(), ROLES_GLOBALES);
        usuario.setId(UUID.randomUUID());
        usuario.setTenant(null);
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuario.setEstado("ACTIVO");
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario registrarUsuarioEmpresa(Usuario usuario) {
        validarDatosRegistro(usuario);
        validarRol(usuario.getRol(), ROLES_SUBORDINADOS);
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("No existe un tenant autenticado para crear el usuario.");
        }
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        usuario.setId(UUID.randomUUID());
        usuario.setTenant(tenant);
        usuario.setPasswordHash(passwordEncoder.encode(usuario.getPasswordHash()));
        usuario.setEstado("ACTIVO");
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario actualizarUsuario(UUID id, Usuario usuarioDetalles) {
        if (usuarioDetalles == null) {
            throw new IllegalArgumentException("Los datos del usuario son obligatorios.");
        }
        Usuario usuario = buscarUsuarioAccesible(id);
        if (TenantContext.getCurrentTenant() != null && !ROLES_SUBORDINADOS.contains(usuario.getRol())) {
            throw new IllegalArgumentException("El administrador de empresa solo puede gestionar vendedores y contadores.");
        }
        usuario.setNombreCompleto(RequestInput.requiredText(
                usuarioDetalles.getNombreCompleto(), "nombreCompleto", 150));
        if (usuarioDetalles.getRol() != null) {
            validarRol(usuarioDetalles.getRol(), usuario.getTenant() == null ? ROLES_GLOBALES : ROLES_SUBORDINADOS);
            usuario.setRol(usuarioDetalles.getRol());
        }
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario cambiarEstado(UUID id, String nuevoEstado) {
        Usuario usuario = buscarUsuarioAccesible(id);
        if (TenantContext.getCurrentTenant() != null && !ROLES_SUBORDINADOS.contains(usuario.getRol())) {
            throw new IllegalArgumentException("El administrador de empresa solo puede gestionar vendedores y contadores.");
        }
        if (!"ACTIVO".equals(nuevoEstado) && !"INACTIVO".equals(nuevoEstado)) {
            throw new IllegalArgumentException("El estado debe ser ACTIVO o INACTIVO.");
        }
        usuario.setEstado(nuevoEstado);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosGlobales() {
        return usuarioRepository.findByTenantIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuariosPorEmpresa(UUID tenantId) {
        UUID currentTenantId = TenantContext.getCurrentTenant();
        if (currentTenantId != null && !currentTenantId.equals(tenantId)) {
            throw new AccessDeniedException("No se puede consultar usuarios de otra empresa.");
        }
        return usuarioRepository.findByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email no registrado."));
    }

    private Usuario buscarUsuarioAccesible(UUID id) {
        UUID tenantId = TenantContext.getCurrentTenant();
        return (tenantId == null
                ? usuarioRepository.findById(id)
                : usuarioRepository.findByIdAndTenantId(id, tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
    }

    private void validarRol(String rol, Set<String> rolesPermitidos) {
        if (rol == null || !rolesPermitidos.contains(rol)) {
            throw new IllegalArgumentException("El rol indicado no está permitido para esta operación.");
        }
    }

    private void validarDatosRegistro(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("Los datos del usuario son obligatorios.");
        }
        String email = RequestInput.requiredText(usuario.getEmail(), "email", 100)
                .toLowerCase(Locale.ROOT);
        if (!email.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+")) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }
        usuario.setEmail(email);
        usuario.setNombreCompleto(RequestInput.requiredText(usuario.getNombreCompleto(), "nombreCompleto", 150));
        String password = usuario.getPasswordHash();
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres y no superar 72 bytes UTF-8.");
        }
    }
}
