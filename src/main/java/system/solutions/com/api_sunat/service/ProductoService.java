package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductoService {

    Producto registrarProducto(Producto producto);
    Producto actualizarProducto(UUID id, Producto productoDetalles);
    void desactivarProducto(UUID id);
    List<Producto> buscarPorDescripcion(String descripcion);
    List<Producto> obtenerAlertaStockBajo(BigDecimal limite);

}
