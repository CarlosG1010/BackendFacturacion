package system.solutions.com.api_sunat.repository;

import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComprobanteDetalleRepository {

    // Recuperar los detalles de una factura específica para impresión o auditoría
    List<ComprobanteDetalle> findByComprobanteId(UUID comprobanteId);

}
