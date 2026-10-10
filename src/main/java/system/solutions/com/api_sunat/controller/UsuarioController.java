package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.entity.Usuario;
import system.solutions.com.api_sunat.service.UsuarioService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // RF-TG-005: Registrar usuarios globales (Solo Adm. Global)
    @PostMapping("/global")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<Usuario> registrarUsuarioGlobal(@RequestBody Usuario usuario) {
        return new ResponseEntity<>(usuarioService.registrarUsuarioGlobal(usuario), HttpStatus.CREATED);
    }

    // RF-TE-001: Registrar usuarios de empresa (Adm. Empresa crea Vendedor o Contador)
    @PostMapping("/empresa")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Usuario> registrarUsuarioEmpresa(@RequestBody Usuario usuario) {
        return new ResponseEntity<>(usuarioService.registrarUsuarioEmpresa(usuario), HttpStatus.CREATED);
    }

    // RF-TG-006 / RF-TE-002: Actualizar información de usuarios
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or (hasRole('ADMIN_EMPRESA') and @authenticatedUserAuthorization.belongsToCurrentTenant(authentication, #id))")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable UUID id, @RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, usuario));
    }

    // RF-TG-007 / RF-TE-003: Activar o desactivar usuarios (Cambiar estado)
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or (hasRole('ADMIN_EMPRESA') and @authenticatedUserAuthorization.belongsToCurrentTenant(authentication, #id))")
    public ResponseEntity<Usuario> cambiarEstado(@PathVariable UUID id, @RequestParam String estado) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, estado));
    }

    // RF-TG-008: Visualizar todos los usuarios globales
    @GetMapping("/globales")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<List<Usuario>> listarUsuariosGlobales() {
        return ResponseEntity.ok(usuarioService.listarUsuariosGlobales());
    }

    // RF-TE-004: Visualizar usuarios de la empresa
    @GetMapping("/empresa/{tenantId}")
    @PreAuthorize("hasRole('ADMIN_GLOBAL') or (hasRole('ADMIN_EMPRESA') and @authenticatedUserAuthorization.isCurrentTenant(authentication, #tenantId))")
    public ResponseEntity<List<Usuario>> listarUsuariosPorEmpresa(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(usuarioService.listarUsuariosPorEmpresa(tenantId));
    }
}
