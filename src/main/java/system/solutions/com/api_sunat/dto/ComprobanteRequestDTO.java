package system.solutions.com.api_sunat.dto;

import lombok.Data; // IMPORTACIÓN CRÍTICA
import java.util.List;
import java.util.UUID;

@Data // ANOTACIÓN CRÍTICA: Genera todos los Getters, Setters y constructores en segundo plano
public class ComprobanteRequestDTO {

    private String tipoComprobante; // "01", "03", etc.
    private String serie;           // "F001"
    private UUID clienteId;
    private UUID usuarioId;
    private UUID comprobanteOrigenId; // Opcional, solo para Notas de Crédito/Débito
    private List<DetalleRequestDTO> detalles;
    private String motivoNotaCodigo;
    private String motivoNota;
    private String moneda = "PEN";
    private java.math.BigDecimal tipoCambio;

}
