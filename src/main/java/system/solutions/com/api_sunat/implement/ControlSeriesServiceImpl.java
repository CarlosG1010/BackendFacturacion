package system.solutions.com.api_sunat.implement;

//import com.sistema.facturacion.model.ControlSeries;
//import com.sistema.facturacion.repository.ControlSeriesRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.entity.ControlSeries;
import system.solutions.com.api_sunat.repository.ControlSeriesRepository;
import system.solutions.com.api_sunat.service.ControlSeriesService;

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
        Long siguienteNumero = control.getUltimoNumero() + 1;
        control.setUltimoNumero(siguienteNumero);

        // Guardamos el cambio en la base de datos manteniendo el bloqueo hasta que termine la transacción principal
        controlSeriesRepository.save(control);

        return siguienteNumero;
    }
}
