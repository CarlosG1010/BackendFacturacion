package system.solutions.com.api_sunat.sunat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.repository.SunatBatchRepository;
import system.solutions.com.api_sunat.repository.TenantRepository;

import java.util.concurrent.Executor;

@Component
public class SunatBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(SunatBatchScheduler.class);
    private final TenantRepository tenantRepository;
    private final SunatBatchRepository batchRepository;
    private final SunatBatchWorker worker;
    private final Executor executor;

    public SunatBatchScheduler(TenantRepository tenantRepository, SunatBatchRepository batchRepository,
                               SunatBatchWorker worker, @Qualifier("sunatExecutor") Executor executor) {
        this.tenantRepository = tenantRepository;
        this.batchRepository = batchRepository;
        this.worker = worker;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${sunat.batch-poll-ms:10000}")
    public void dispatch() {
        tenantRepository.findAll().forEach(tenant -> {
            try {
                TenantContext.setCurrentTenant(tenant.getId());
                batchRepository.findTop20ByEstadoOrderByCreadoEnAsc("PENDIENTE_ENVIO")
                        .forEach(batch -> dispatch(batch.getId(), tenant.getId(), false));
                batchRepository.findTop20ByEstadoOrderByCreadoEnAsc("CONSULTA_PENDIENTE")
                        .forEach(batch -> dispatch(batch.getId(), tenant.getId(), true));
            } catch (RuntimeException exception) {
                log.error("No se pudieron despachar lotes SUNAT para tenant {}: {}",
                        tenant.getId(), exception.getMessage(), exception);
            } finally {
                TenantContext.clear();
            }
        });
    }

    private void dispatch(java.util.UUID batchId, java.util.UUID tenantId, boolean queryTicket) {
        executor.execute(() -> {
            TenantContext.setCurrentTenant(tenantId);
            try {
                if (queryTicket) worker.consultar(batchId);
                else worker.enviar(batchId);
            } finally {
                TenantContext.clear();
            }
        });
    }
}
