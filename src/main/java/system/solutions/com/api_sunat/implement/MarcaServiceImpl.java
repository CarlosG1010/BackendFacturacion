package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Marca;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.repository.MarcaRepository;
import system.solutions.com.api_sunat.service.MarcaService;
import system.solutions.com.api_sunat.util.RequestInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarcaServiceImpl implements MarcaService{

    private final MarcaRepository marcaRepository;

    @Override
    @Transactional
    public Marca guardar(Marca marca) {
        if (marca == null || TenantContext.getCurrentTenant() == null) {
            throw new IllegalArgumentException("Se requiere marca y empresa autenticada.");
        }
        marca.setNombre(RequestInput.requiredText(marca.getNombre(), "nombre", 100));
        marca.setId(UUID.randomUUID());
        marca.setTenantId(TenantContext.getCurrentTenant());
        marca.setEstado("ACTIVO");
        return marcaRepository.save(marca);
    }

    @Override
    @Transactional
    public Marca actualizar(UUID id, Marca marcaDetalles) {
        if (marcaDetalles == null) {
            throw new IllegalArgumentException("La marca es obligatoria.");
        }
        Marca m = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada."));
        m.setNombre(RequestInput.requiredText(marcaDetalles.getNombre(), "nombre", 100));
        return marcaRepository.save(m);
    }

    @Override
    @Transactional
    public void desactivar(UUID id) {
        Marca m = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada."));
        m.setEstado("INACTIVO");
        marcaRepository.save(m);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Marca> filtrarPorNombre(String nombre) {
        return marcaRepository.findByNombreContainingIgnoreCase(RequestInput.filterText(nombre, "nombre", 100));
    }
}
