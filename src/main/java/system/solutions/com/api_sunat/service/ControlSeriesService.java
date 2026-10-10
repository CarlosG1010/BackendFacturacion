package system.solutions.com.api_sunat.service;

public interface ControlSeriesService {

    /**
     * Obtiene y reserva el siguiente número correlativo de forma segura y concurrente.
     *
     * @param tipoComprobante Tipo de documento (01=Factura, 03=Boleta, etc.)
     * @param serie Serie autorizada (Ej: F001)
     * @return El número correlativo disponible para usar.
     */
    Long obtenerSiguienteCorrelativo(String tipoComprobante, String serie);

}
