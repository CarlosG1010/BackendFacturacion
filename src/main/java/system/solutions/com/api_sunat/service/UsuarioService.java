package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.Usuario;
import java.util.List;
import java.util.UUID;

public interface UsuarioService {
    Usuario registrarUsuarioGlobal(Usuario usuario);
    Usuario registrarUsuarioEmpresa(Usuario usuario);
    Usuario actualizarUsuario(UUID id, Usuario usuarioDetalles);
    Usuario cambiarEstado(UUID id, String nuevoEstado);
    List<Usuario> listarUsuariosGlobales();
    List<Usuario> listarUsuariosPorEmpresa(UUID tenantId);
    Usuario buscarPorEmail(String email);
}
