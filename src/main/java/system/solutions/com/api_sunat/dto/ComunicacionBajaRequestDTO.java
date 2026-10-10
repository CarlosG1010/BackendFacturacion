package system.solutions.com.api_sunat.dto;

import java.util.List;
import java.util.UUID;

public record ComunicacionBajaRequestDTO(List<UUID> comprobanteIds, String motivo) {
}
