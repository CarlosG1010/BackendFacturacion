package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.entity.Categoria;
import system.solutions.com.api_sunat.service.CategoriaService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Categoria> guardar(@RequestBody Categoria categoria) {
        return new ResponseEntity<>(categoriaService.guardar(categoria), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Categoria> actualizar(@PathVariable UUID id, @RequestBody Categoria categoria) {
        return ResponseEntity.ok(categoriaService.actualizar(id, categoria));
    }

    // RF-TE-013: Desactivar categorías
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        categoriaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // RF-TE-013: Listar y filtrar categorías por su nombre
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<List<Categoria>> filtrarPorNombre(@RequestParam(defaultValue = "") String nombre) {
        return ResponseEntity.ok(categoriaService.filtrarPorNombre(nombre));
    }
}
