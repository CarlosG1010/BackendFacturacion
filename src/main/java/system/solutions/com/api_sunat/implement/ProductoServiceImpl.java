package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.repository.CategoriaRepository;
import system.solutions.com.api_sunat.repository.MarcaRepository;
import system.solutions.com.api_sunat.repository.ProductoRepository;
import system.solutions.com.api_sunat.service.ProductoService;
import system.solutions.com.api_sunat.util.RequestInput;
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
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    @Override
    @Transactional
    public Producto registrarProducto(Producto producto) {
        if (producto == null || TenantContext.getCurrentTenant() == null) {
            throw new IllegalArgumentException("Se requiere producto y empresa autenticada.");
        }
        validarDatos(producto);
        producto.setId(UUID.randomUUID());
        producto.setTenantId(TenantContext.getCurrentTenant());
        producto.setEstado("ACTIVO");
        return productoRepository.save(producto);
    }

    @Override
    @Transactional
    public Producto actualizarProducto(UUID id, Producto productoDetalles) {
        if (productoDetalles == null) {
            throw new IllegalArgumentException("Los datos del producto son obligatorios.");
        }
        Producto prod = productoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado."));
        validarDatos(productoDetalles);
        prod.setDescripcion(productoDetalles.getDescripcion());
        prod.setPrecioUnitario(productoDetalles.getPrecioUnitario());
        prod.setCategoria(categoria(productoDetalles));
        prod.setMarca(marca(productoDetalles));
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
        String query = RequestInput.filterText(descripcion, "descripcion", 100);
        return productoRepository.findByDescripcionContainingIgnoreCaseAndEstado(query, "ACTIVO");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> obtenerAlertaStockBajo(BigDecimal limite) {
        if (limite == null || limite.signum() < 0 || limite.precision() > 14 || limite.scale() > 4) {
            throw new IllegalArgumentException("El límite de stock debe ser no negativo y tener hasta 4 decimales.");
        }
        return productoRepository.findByStockActualLessThanEqual(limite);
    }

    private void validarDatos(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("Los datos del producto son obligatorios.");
        }
        producto.setCodigoInterno(RequestInput.requiredText(producto.getCodigoInterno(), "codigoInterno", 50));
        if (!producto.getCodigoInterno().matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("El código interno solo admite letras, números, punto, guion y guion bajo.");
        }
        producto.setDescripcion(RequestInput.requiredText(producto.getDescripcion(), "descripcion", 255));
        producto.setUnidadMedida(RequestInput.requiredText(producto.getUnidadMedida(), "unidadMedida", 10)
                .toUpperCase(java.util.Locale.ROOT));
        if (producto.getPrecioUnitario() == null || producto.getPrecioUnitario().signum() < 0
                || producto.getPrecioUnitario().precision() > 14 || producto.getPrecioUnitario().scale() > 4) {
            throw new IllegalArgumentException("El precio unitario debe ser no negativo y tener hasta 4 decimales.");
        }
        if (producto.getStockActual() == null || producto.getStockActual().signum() < 0
                || producto.getStockActual().precision() > 12 || producto.getStockActual().scale() > 4) {
            throw new IllegalArgumentException("El stock debe ser no negativo y tener hasta 4 decimales.");
        }
        producto.setCategoria(categoria(producto));
        producto.setMarca(marca(producto));
    }

    private system.solutions.com.api_sunat.entity.Categoria categoria(Producto producto) {
        if (producto.getCategoria() == null) return null;
        if (producto.getCategoria().getId() == null) {
            throw new IllegalArgumentException("El identificador de categoría es obligatorio.");
        }
        return categoriaRepository.findById(producto.getCategoria().getId())
                .orElseThrow(() -> new IllegalArgumentException("La categoría no existe en la empresa autenticada."));
    }

    private system.solutions.com.api_sunat.entity.Marca marca(Producto producto) {
        if (producto.getMarca() == null) return null;
        if (producto.getMarca().getId() == null) {
            throw new IllegalArgumentException("El identificador de marca es obligatorio.");
        }
        return marcaRepository.findById(producto.getMarca().getId())
                .orElseThrow(() -> new IllegalArgumentException("La marca no existe en la empresa autenticada."));
    }
}
