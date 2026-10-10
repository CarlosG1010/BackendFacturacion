package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Marca;
import system.solutions.com.api_sunat.repository.MarcaRepository;
import system.solutions.com.api_sunat.service.MarcaService;
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
        marca.setEstado("ACTIVO");
        return marcaRepository.save(marca);
    }

    @Override
    @Transactional
    public Marca actualizar(UUID id, Marca marcaDetalles) {
        Marca m = marcaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Marca no encontrada."));
        m.setNombre(marcaDetalles.getNombre());
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
        return marcaRepository.findByNombreContainingIgnoreCase(nombre);
    }
}
