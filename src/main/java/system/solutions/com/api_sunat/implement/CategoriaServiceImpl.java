package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Categoria;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.repository.CategoriaRepository;
import system.solutions.com.api_sunat.service.CategoriaService;
import system.solutions.com.api_sunat.util.RequestInput;
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
        if (categoria == null || TenantContext.getCurrentTenant() == null) {
            throw new IllegalArgumentException("Se requiere categoría y empresa autenticada.");
        }
        categoria.setNombre(RequestInput.requiredText(categoria.getNombre(), "nombre", 100));
        categoria.setId(UUID.randomUUID());
        categoria.setTenantId(TenantContext.getCurrentTenant());
        categoria.setEstado("ACTIVO");
        return categoriaRepository.save(categoria);
    }

    @Override
    @Transactional
    public Categoria actualizar(UUID id, Categoria categoriaDetalles) {
        if (categoriaDetalles == null) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
        Categoria cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoría no encontrada."));
        cat.setNombre(RequestInput.requiredText(categoriaDetalles.getNombre(), "nombre", 100));
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
        return categoriaRepository.findByNombreContainingIgnoreCase(RequestInput.filterText(nombre, "nombre", 100));
    }
}