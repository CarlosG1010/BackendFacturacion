package system.solutions.com.api_sunat.dto;

import system.solutions.com.api_sunat.entity.Comprobante;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ComprobanteResponseDTO(
        UUID id,
        String tipoComprobante,
        String serie,
        Long numero,
        LocalDateTime fechaEmision,
        BigDecimal montoTotal,
        String estadoSunat,
        String hashTributario,
        String codigoRespuestaSunat,
        String mensajeRespuestaSunat,
        String ticketSunat
) {
    public static ComprobanteResponseDTO from(Comprobante comprobante) {
        return new ComprobanteResponseDTO(
                comprobante.getId(),
                comprobante.getTipoComprobante(),
                comprobante.getSerie(),
                comprobante.getNumero(),
                comprobante.getFechaEmision(),
                comprobante.getMontoTotal(),
                comprobante.getEstadoSunat(),
                comprobante.getHashTributario(),
                comprobante.getCodigoRespuestaSunat(),
                comprobante.getMensajeRespuestaSunat(),
                comprobante.getTicketSunat()
        );
    }
}
