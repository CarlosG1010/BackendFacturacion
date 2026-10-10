package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.service.ClienteService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // RF-TE-005 / RF-TE-023: Registrar nuevos clientes
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
    public ResponseEntity<Cliente> registrarCliente(@RequestBody Cliente cliente) {
        return new ResponseEntity<>(clienteService.registrarCliente(cliente), HttpStatus.CREATED);
    }

    // RF-TE-006 / RF-TE-024: Actualizar información de clientes
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable UUID id, @RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, cliente));
    }

    // RF-TE-008: Desactivar clientes (Adm. de Empresa)
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Cliente> cambiarEstado(@PathVariable UUID id, @RequestParam String estado) {
        return ResponseEntity.ok(clienteService.cambiarEstado(id, estado));
    }

    // RF-TE-007 / RF-TE-025: Consultar y listar clientes por filtro de nombre
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
    public ResponseEntity<List<Cliente>> buscarClientes(
            @RequestParam(required = false, defaultValue = "") String nombre,
            @RequestParam(required = false, defaultValue = "ACTIVO") String estado) {
        return ResponseEntity.ok(clienteService.buscarClientes(nombre, estado));
    }

    // Consultar por documento único para agilizar facturación rápida
    @GetMapping("/documento")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR')")
    public ResponseEntity<Cliente> buscarPorDocumento(
            @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento) {
        return ResponseEntity.ok(clienteService.buscarPorDocumento(tipoDocumento, numeroDocumento));
    }
}
