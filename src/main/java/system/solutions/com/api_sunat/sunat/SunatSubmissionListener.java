package system.solutions.com.api_sunat.sunat;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import system.solutions.com.api_sunat.config.TenantContext;

@Component
@RequiredArgsConstructor
public class SunatSubmissionListener {

    private final SunatEmissionWorker emissionWorker;

    @Async("sunatExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void submit(ComprobanteSubmissionEvent event) {
        TenantContext.setCurrentTenant(event.tenantId());
        try {
            emissionWorker.enviar(event.comprobanteId());
        } finally {
            TenantContext.clear();
        }
    }
}
