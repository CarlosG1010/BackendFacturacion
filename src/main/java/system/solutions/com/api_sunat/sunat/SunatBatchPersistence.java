package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.SunatBatch;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.repository.ComprobanteRepository;
import system.solutions.com.api_sunat.repository.SunatBatchRepository;
import system.solutions.com.api_sunat.repository.TenantRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SunatBatchPersistence {

    private final SunatBatchRepository batchRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final TenantRepository tenantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(UUID batchId, String expected, String next) {
        return batchRepository.claim(batchId, expected, next) == 1;
    }

    @Transactional(readOnly = true)
    public BatchData load(UUID batchId) {
        SunatBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new SunatIntegrationException("No se encontró el lote SUNAT."));
        List<Comprobante> documents = documents(batch);
        if (documents.isEmpty()) {
            throw new SunatIntegrationException("El lote no tiene comprobantes asociados.");
        }
        Tenant tenant = tenantRepository.findById(batch.getTenantId())
                .orElseThrow(() -> new SunatIntegrationException("No se encontró el tenant del lote."));
        return new BatchData(batch, documents, tenant);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markTicket(UUID id, String fileName, String xmlPath, String ticket) {
        SunatBatch batch = required(id);
        batch.setNombreArchivo(fileName);
        batch.setXmlFirmadoPath(xmlPath);
        batch.setTicketSunat(ticket);
        batch.setEstado("CONSULTA_PENDIENTE");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryTicket(UUID id) {
        SunatBatch batch = required(id);
        batch.setEstado("CONSULTA_PENDIENTE");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID id, String code, String message, String cdrPath, boolean accepted) {
        SunatBatch batch = required(id);
        batch.setCodigoRespuesta(code);
        batch.setMensajeRespuesta(trim(message, 2000));
        batch.setCdrPath(cdrPath);
        batch.setFechaRespuesta(LocalDateTime.now());
        batch.setEstado(accepted ? "ACEPTADO" : "RECHAZADO");
        if ("BAJA".equals(batch.getTipoLote())) {
            documents(batch).forEach(comprobante -> comprobante.setEstadoAnulacion(accepted ? "ACEPTADA" : "RECHAZADA"));
        } else if ("RESUMEN".equals(batch.getTipoLote())) {
            documents(batch).forEach(comprobante -> {
                comprobante.setEstadoSunat(accepted ? "ACEPTADO" : "RECHAZADO");
                comprobante.setCodigoRespuestaSunat(code);
                comprobante.setMensajeRespuestaSunat(trim(message, 2000));
                comprobante.setTicketSunat(batch.getTicketSunat());
                comprobante.setCdrPath(cdrPath);
                comprobante.setFechaEnvioSunat(LocalDateTime.now());
            });
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID id, String message) {
        SunatBatch batch = batchRepository.findById(id).orElse(null);
        if (batch == null) {
            return;
        }
        batch.setEstado("ERROR");
        batch.setMensajeRespuesta(trim(message, 2000));
        batch.setFechaRespuesta(LocalDateTime.now());
        if ("BAJA".equals(batch.getTipoLote())) {
            documents(batch).forEach(comprobante -> comprobante.setEstadoAnulacion("ERROR"));
        } else if ("RESUMEN".equals(batch.getTipoLote())) {
            documents(batch).forEach(comprobante -> {
                comprobante.setEstadoSunat("ERROR_ENVIO");
                comprobante.setMensajeRespuestaSunat(trim(message, 2000));
            });
        }
    }

    private List<Comprobante> documents(SunatBatch batch) {
        return switch (batch.getTipoLote()) {
            case "RESUMEN" -> comprobanteRepository.findWithClientByLoteResumen_IdOrderBySerieAscNumeroAsc(batch.getId());
            case "BAJA" -> comprobanteRepository.findWithClientByLoteBaja_IdOrderBySerieAscNumeroAsc(batch.getId());
            default -> throw new SunatIntegrationException("Tipo de lote SUNAT no soportado.");
        };
    }

    private SunatBatch required(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new SunatIntegrationException("No se encontró el lote SUNAT."));
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record BatchData(SunatBatch batch, List<Comprobante> documents, Tenant tenant) {}
}
