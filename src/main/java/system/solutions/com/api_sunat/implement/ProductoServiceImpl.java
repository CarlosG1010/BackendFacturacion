package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.repository.ProductoRepository;
import system.solutions.com.api_sunat.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService{

    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public Producto registrarProducto(Producto producto) {
        producto.setEstado("ACTIVO");
        return productoRepository.save(producto);
    }

    @Override
    @Transactional
    public Producto actualizarProducto(UUID id, Producto productoDetalles) {
        Producto prod = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        prod.setDescripcion(productoDetalles.getDescripcion());
        prod.setPrecioUnitario(productoDetalles.getPrecioUnitario());
        prod.setCategoria(productoDetalles.getCategoria());
        prod.setMarca(productoDetalles.getMarca());
        return productoRepository.save(prod);
    }

    @Override
    @Transactional
    public void desactivarProducto(UUID id) {
        Producto prod = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        prod.setEstado("INACTIVO");
        productoRepository.save(prod);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> buscarPorDescripcion(String descripcion) {
        return productoRepository.findByDescripcionContainingIgnoreCaseAndEstado(descripcion, "ACTIVO");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> obtenerAlertaStockBajo(BigDecimal limite) {
        return productoRepository.findByStockActualLessThanEqual(limite);
    }
}
