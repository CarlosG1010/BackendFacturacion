package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.entity.Marca;
import system.solutions.com.api_sunat.service.MarcaService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/marcas")
@RequiredArgsConstructor
public class MarcaController {

    private final MarcaService marcaService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Marca> guardar(@RequestBody Marca marca) {
        return new ResponseEntity<>(marcaService.guardar(marca), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Marca> actualizar(@PathVariable UUID id, @RequestBody Marca marca) {
        return ResponseEntity.ok(marcaService.actualizar(id, marca));
    }

    // RF-TE-014: Desactivar marcas
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        marcaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // RF-TE-014: Consultar marcas
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<List<Marca>> filtrarPorNombre(@RequestParam(defaultValue = "") String nombre) {
        return ResponseEntity.ok(marcaService.filtrarPorNombre(nombre));
    }
}