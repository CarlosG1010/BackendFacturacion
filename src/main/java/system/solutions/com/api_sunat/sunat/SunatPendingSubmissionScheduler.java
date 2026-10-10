package system.solutions.com.api_sunat.sunat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.repository.ComprobanteRepository;
import system.solutions.com.api_sunat.repository.TenantRepository;

import java.util.concurrent.Executor;

@Component
public class SunatPendingSubmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SunatPendingSubmissionScheduler.class);
    private final TenantRepository tenantRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final SunatEmissionWorker emissionWorker;
    private final Executor executor;

    public SunatPendingSubmissionScheduler(
            TenantRepository tenantRepository,
            ComprobanteRepository comprobanteRepository,
            SunatEmissionWorker emissionWorker,
            @Qualifier("sunatExecutor") Executor executor) {
        this.tenantRepository = tenantRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.emissionWorker = emissionWorker;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${sunat.queue-poll-ms:5000}")
    public void submitPendingDocuments() {
        tenantRepository.findAll().forEach(tenant -> {
            TenantContext.setCurrentTenant(tenant.getId());
            try {
                comprobanteRepository.findTop20PendingIndividual()
                        .forEach(comprobante -> executor.execute(() -> {
                            TenantContext.setCurrentTenant(tenant.getId());
                            try {
                                emissionWorker.enviar(comprobante.getId());
                            } finally {
                                TenantContext.clear();
                            }
                        }));
            } catch (RuntimeException exception) {
                log.error("No se pudieron despachar comprobantes pendientes del tenant {}: {}",
                        tenant.getId(), exception.getMessage(), exception);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
