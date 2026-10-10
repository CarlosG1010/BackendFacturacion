package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.service.ProductoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    // RF-TE-009: Registrar productos
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Producto> registrarProducto(@RequestBody Producto producto) {
        return new ResponseEntity<>(productoService.registrarProducto(producto), HttpStatus.CREATED);
    }

    // RF-TE-010: Actualizar productos
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Producto> actualizarProducto(@PathVariable UUID id, @RequestBody Producto producto) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, producto));
    }

    // RF-TE-012: Desactivar productos
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivarProducto(@PathVariable UUID id) {
        productoService.desactivarProducto(id);
        return ResponseEntity.noContent().build();
    }

    // RF-TE-011: Consultar catálogo de productos activos
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<List<Producto>> buscarPorDescripcion(@RequestParam(defaultValue = "") String descripcion) {
        return ResponseEntity.ok(productoService.buscarPorDescripcion(descripcion));
    }

    // RF-TE-021: Generar reportes de alerta / quiebre de stock
    @GetMapping("/reportes/stock-bajo")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<List<Producto>> obtenerAlertaStockBajo(@RequestParam BigDecimal limite) {
        return ResponseEntity.ok(productoService.obtenerAlertaStockBajo(limite));
    }
}
