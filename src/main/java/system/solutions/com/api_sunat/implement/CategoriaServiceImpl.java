package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Categoria;
import system.solutions.com.api_sunat.repository.CategoriaRepository;
import system.solutions.com.api_sunat.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService{

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public Categoria guardar(Categoria categoria) {
        categoria.setEstado("ACTIVO");
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public Categoria actualizar(UUID id, Categoria categoriaDetalles) {
        Categoria cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));
        cat.setNombre(categoriaDetalles.getNombre());
        return categoriaRepository.save(cat);
    }

    @Override
    @Transactional
    public void desactivar(UUID id) {
        Categoria cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));
        cat.setEstado("INACTIVO");
        categoriaRepository.save(cat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> filtrarPorNombre(String nombre) {
        return categoriaRepository.findByNombreContainingIgnoreCase(nombre);
    }
}