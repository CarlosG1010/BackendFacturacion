package system.solutions.com.api_sunat.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class DetalleRequestDTO {
    private UUID productoId;
    private BigDecimal cantidad;
    private String tipoAfectacionIgv = "10";
    private boolean gratuito;
}
