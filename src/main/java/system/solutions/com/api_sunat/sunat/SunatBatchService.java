package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.dto.SunatBatchResponseDTO;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.SunatBatch;
import system.solutions.com.api_sunat.repository.ComprobanteRepository;
import system.solutions.com.api_sunat.repository.SunatBatchRepository;
import system.solutions.com.api_sunat.repository.TenantRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SunatBatchService {

    private final ComprobanteRepository comprobanteRepository;
    private final SunatBatchRepository batchRepository;
    private final TenantRepository tenantRepository;
    private final SunatProperties properties;

    @Transactional
    public SunatBatchResponseDTO crearResumen(LocalDate fecha) {
        validarTenant();
        if (fecha == null || fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del resumen no puede ser futura.");
        }
        List<Comprobante> boletas = comprobanteRepository.lockBoletasPendientesResumen(
                fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        if (boletas.isEmpty()) {
            throw new IllegalArgumentException("No hay boletas aceptadas pendientes de resumen para la fecha indicada.");
        }
        SunatBatch batch = crearBatch("RESUMEN", fecha, boletas);
        boletas.forEach(comprobante -> comprobante.setLoteResumenId(batch.getId()));
        return SunatBatchResponseDTO.from(batchRepository.save(batch));
    }

    @Transactional
    public SunatBatchResponseDTO crearBaja(List<UUID> ids, String motivo) {
        validarTenant();
        if (ids == null || ids.isEmpty() || ids.size() > 500 || new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("La solicitud debe incluir entre 1 y 500 comprobantes únicos.");
        }
        if (motivo == null || motivo.isBlank() || motivo.length() > 500) {
            throw new IllegalArgumentException("El motivo de baja es obligatorio y admite hasta 500 caracteres.");
        }
        List<Comprobante> documents = comprobanteRepository.lockByIds(ids);
        if (documents.size() != ids.size()) {
            throw new IllegalArgumentException("Uno o más comprobantes no existen en la empresa autenticada.");
        }
        LocalDate today = LocalDate.now();
        LocalDate documentDate = null;
        for (Comprobante comprobante : documents) {
            if (!List.of("01", "07", "08").contains(comprobante.getTipoComprobante())
                    || !"ACEPTADO".equals(comprobante.getEstadoSunat())) {
                throw new IllegalArgumentException("Solo se pueden dar de baja facturas y notas aceptadas por SUNAT.");
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(comprobante.getFechaEmision().toLocalDate(), today);
            if (days < 0 || days > properties.getMaxBajaDias()) {
                throw new IllegalArgumentException("El comprobante está fuera del plazo configurado para solicitar la baja.");
            }
            if (documentDate == null) {
                documentDate = comprobante.getFechaEmision().toLocalDate();
            } else if (!documentDate.equals(comprobante.getFechaEmision().toLocalDate())) {
                throw new IllegalArgumentException("Una comunicación de baja solo puede agrupar comprobantes de una misma fecha.");
            }
            if (comprobante.getLoteBajaId() != null || "ACEPTADA".equals(comprobante.getEstadoAnulacion())) {
                throw new IllegalArgumentException("El comprobante ya tiene una comunicación de baja asociada.");
            }
        }
        SunatBatch batch = crearBatch("BAJA", documentDate, documents);
        for (Comprobante comprobante : documents) {
            comprobante.setLoteBajaId(batch.getId());
            comprobante.setEstadoAnulacion("PENDIENTE_ENVIO");
        }
        batch.setMensajeRespuesta(motivo);
        return SunatBatchResponseDTO.from(batchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public SunatBatchResponseDTO obtener(UUID id) {
        validarTenant();
        SunatBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el lote solicitado."));
        return SunatBatchResponseDTO.from(batch);
    }

    private SunatBatch crearBatch(String tipo, LocalDate fecha, List<Comprobante> documents) {
        UUID tenantId = TenantContext.getCurrentTenant();
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new IllegalStateException("No se encontró la empresa autenticada."));
        SunatBatch batch = new SunatBatch();
        batch.setTipoLote(tipo);
        batch.setFechaDocumentos(fecha);
        batch.setCorrelativo(batchRepository.maxCorrelativo(tipo, fecha) + 1);
        batch.setDocumentosJson(documents.stream().map(comprobante -> "\"" + comprobante.getId() + "\"")
                .collect(java.util.stream.Collectors.joining(",", "[", "]")));
        batch.setNombreArchivo(tipo);
        batch.setEstado("PENDIENTE_ENVIO");
        batch.setCreadoEn(LocalDateTime.now());
        return batch;
    }

    private void validarTenant() {
        if (TenantContext.getCurrentTenant() == null) {
            throw new IllegalStateException("Se requiere el tenant autenticado para operar lotes SUNAT.");
        }
    }
}
