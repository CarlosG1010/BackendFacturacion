package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.entity.ControlSeries;

import java.util.List;

public interface ControlSeriesService {

    /**
     * Obtiene y reserva el siguiente número correlativo de forma segura y concurrente.
     *
     * @param tipoComprobante Tipo de documento (01=Factura, 03=Boleta, etc.)
     * @param serie Serie autorizada (Ej: F001)
     * @return El número correlativo disponible para usar.
     */
    Long obtenerSiguienteCorrelativo(String tipoComprobante, String serie);

    ControlSeries configurar(String tipoComprobante, String serie, long ultimoNumero);

    List<ControlSeries> listar();

}
