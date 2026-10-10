package system.solutions.com.api_sunat.service;

import system.solutions.com.api_sunat.dto.ComprobanteRequestDTO;
import system.solutions.com.api_sunat.entity.Comprobante;
import java.util.UUID;

public interface ComprobanteService {

    /**
     * Procesa y registra la emisión completa de un comprobante electrónico (Factura, Boleta, NC, ND).
     *
     * @param request Datos de la venta enviados por el frontend
     * @return El Comprobante guardado con montos e impuestos liquidados
     */
    Comprobante emitirComprobante(ComprobanteRequestDTO request);

    /**
     * Busca un comprobante por su ID aplicando las restricciones de multi-inquilino de forma interna.
     */
    Comprobante buscarPorId(UUID id);
}
