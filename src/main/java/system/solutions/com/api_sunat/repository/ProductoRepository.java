package system.solutions.com.api_sunat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.Producto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, UUID>{

    // Soporta RF-TE-011: Búsqueda por código interno
    Optional<Producto> findByCodigoInterno(String codigoInterno);

    // Soporta RF-TE-011: Búsqueda inteligente por descripción
    List<Producto> findByDescripcionContainingIgnoreCaseAndEstado(String descripcion, String estado);

    // Soporta RF-TE-021: Reporte de Alerta de Stock Bajo
    List<Producto> findByStockActualLessThanEqual(BigDecimal limite);

    // Optimización transaccional: Descontar stock atómicamente al facturar para evitar condiciones de carrera
    @Modifying
    @Query("UPDATE Producto p SET p.stockActual = p.stockActual - :cantidad " +
            "WHERE p.id = :id AND p.stockActual >= :cantidad AND p.estado = 'ACTIVO'")
    int disminuirStock(@Param("id") UUID id, @Param("cantidad") BigDecimal cantidad);

}
