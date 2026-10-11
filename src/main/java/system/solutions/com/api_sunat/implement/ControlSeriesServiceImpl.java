package system.solutions.com.api_sunat.implement;

//import com.sistema.facturacion.model.ControlSeries;
//import com.sistema.facturacion.repository.ControlSeriesRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.entity.ControlSeries;
import system.solutions.com.api_sunat.repository.ControlSeriesRepository;
import system.solutions.com.api_sunat.service.ControlSeriesService;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ControlSeriesServiceImpl implements ControlSeriesService {

    private final ControlSeriesRepository controlSeriesRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    // REQUIERE ejecutarse en la transacción de la factura. Si la factura falla, el contador vuelve a su estado anterior.
    public Long obtenerSiguienteCorrelativo(String tipoComprobante, String serie) {

        // Ejecuta un SELECT ... FOR UPDATE bloqueando únicamente la fila de la serie solicitada
        ControlSeries control = controlSeriesRepository
                .findByTipoComprobanteAndSerieForUpdate(tipoComprobante, serie)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("La serie %s para el tipo %s no está configurada por el administrador.", serie, tipoComprobante)
                ));

        // Incrementamos el contador de forma aislada
        if (control.getUltimoNumero() == null || control.getUltimoNumero() < 0
                || control.getUltimoNumero() == Long.MAX_VALUE) {
            throw new IllegalStateException("El correlativo de la serie alcanzó su límite permitido.");
        }
        Long siguienteNumero = control.getUltimoNumero() + 1;
        control.setUltimoNumero(siguienteNumero);

        // Guardamos el cambio en la base de datos manteniendo el bloqueo hasta que termine la transacción principal
        controlSeriesRepository.save(control);

        return siguienteNumero;
    }

    @Override
    @Transactional
    public ControlSeries configurar(String tipoComprobante, String serie, long ultimoNumero) {
        var tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("No hay un tenant autenticado para configurar la serie.");
        }
        if (tipoComprobante == null || !Set.of("01", "03", "07", "08").contains(tipoComprobante)) {
            throw new IllegalArgumentException("Tipo de comprobante no soportado para series.");
        }
        String prefix = "03".equals(tipoComprobante) ? "B" : "F";
        if (serie == null || !serie.matches("[A-Z0-9]{4}") || !serie.startsWith(prefix)
                || ultimoNumero < 0 || ultimoNumero == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Serie o correlativo inicial no válido.");
        }
        if (controlSeriesRepository.existsByTipoComprobanteAndSerie(tipoComprobante, serie)) {
            throw new IllegalArgumentException("La serie ya está configurada para este tenant.");
        }
        ControlSeries control = new ControlSeries();
        control.setTipoComprobante(tipoComprobante);
        control.setSerie(serie);
        control.setUltimoNumero(ultimoNumero);
        control.setTenantId(tenantId);
        return controlSeriesRepository.save(control);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ControlSeries> listar() {
        return controlSeriesRepository.findAllByOrderByTipoComprobanteAscSerieAsc();
    }
}
