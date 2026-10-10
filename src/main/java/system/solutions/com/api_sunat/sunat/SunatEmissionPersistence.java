package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.repository.ComprobanteRepository;
import system.solutions.com.api_sunat.repository.TenantRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SunatEmissionPersistence {

    private final ComprobanteRepository comprobanteRepository;
    private final TenantRepository tenantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(UUID comprobanteId) {
        return comprobanteRepository.cambiarEstadoSiCoincide(comprobanteId, "PENDIENTE_ENVIO", "PROCESANDO") == 1;
    }

    @Transactional(readOnly = true)
    public EmissionData load(UUID comprobanteId) {
        Comprobante comprobante = comprobanteRepository.findWithEmissionDataById(comprobanteId)
                .orElseThrow(() -> new SunatIntegrationException("No se encontró el comprobante para enviarlo a SUNAT."));
        Tenant tenant = tenantRepository.findById(comprobante.getTenantId())
                .orElseThrow(() -> new SunatIntegrationException("No se encontró la empresa del comprobante."));
        return new EmissionData(comprobante, tenant);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSending(UUID id, String hash, String xmlPath) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new SunatIntegrationException("No se encontró el comprobante al guardar el XML firmado."));
        comprobante.setHashTributario(hash);
        comprobante.setXmlFirmadoPath(xmlPath);
        comprobante.setEstadoSunat("ENVIANDO");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID id, SunatReceipt receipt, String cdrPath) {
        Comprobante comprobante = comprobanteRepository.findById(id)
                .orElseThrow(() -> new SunatIntegrationException("No se encontró el comprobante al guardar el CDR."));
        comprobante.setCodigoRespuestaSunat(receipt.responseCode());
        comprobante.setMensajeRespuestaSunat(trim(receipt.description() + (receipt.notes().isBlank()
                ? "" : " | Observaciones: " + receipt.notes()), 2000));
        comprobante.setCdrPath(cdrPath);
        comprobante.setFechaEnvioSunat(LocalDateTime.now());
        comprobante.setEstadoSunat("0".equals(receipt.responseCode()) ? "ACEPTADO" : "RECHAZADO");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(UUID id, String message) {
        comprobanteRepository.findById(id).ifPresent(comprobante -> {
            comprobante.setEstadoSunat("ERROR_ENVIO");
            comprobante.setMensajeRespuestaSunat(trim(message, 2000));
        });
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record EmissionData(Comprobante comprobante, Tenant tenant) {
    }
}
