package system.solutions.com.api_sunat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.pdf.PdfGeneratorService;
import system.solutions.com.api_sunat.pdf.PrintFormat;
import system.solutions.com.api_sunat.repository.ComprobanteRepository;
import system.solutions.com.api_sunat.repository.TenantRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComprobantePdfService {

    private final ComprobanteRepository comprobanteRepository;
    private final TenantRepository tenantRepository;
    private final PdfGeneratorService pdfGeneratorService;

    @Transactional(readOnly = true)
    public byte[] generar(UUID comprobanteId, PrintFormat format) {
        UUID currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == null) {
            throw new IllegalStateException("No hay un tenant autenticado para generar el PDF.");
        }
        Comprobante comprobante = comprobanteRepository.findWithEmissionDataById(comprobanteId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el comprobante solicitado."));
        if (!currentTenant.equals(comprobante.getTenantId())) {
            throw new IllegalArgumentException("No se encontró el comprobante solicitado.");
        }
        Tenant tenant = tenantRepository.findById(currentTenant)
                .orElseThrow(() -> new IllegalStateException("No se encontraron los datos de la empresa emisora."));
        return pdfGeneratorService.generar(comprobante, tenant, format);
    }
}
