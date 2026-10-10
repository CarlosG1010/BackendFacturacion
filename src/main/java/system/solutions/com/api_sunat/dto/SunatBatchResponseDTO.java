package system.solutions.com.api_sunat.dto;

import system.solutions.com.api_sunat.entity.SunatBatch;

import java.time.LocalDate;
import java.util.UUID;

public record SunatBatchResponseDTO(
        UUID id,
        String tipo,
        LocalDate fecha,
        String estado,
        String ticket,
        String codigoRespuesta,
        String mensaje
) {
    public static SunatBatchResponseDTO from(SunatBatch batch) {
        return new SunatBatchResponseDTO(
                batch.getId(), batch.getTipoLote(), batch.getFechaDocumentos(), batch.getEstado(),
                batch.getTicketSunat(), batch.getCodigoRespuesta(), batch.getMensajeRespuesta());
    }
}
