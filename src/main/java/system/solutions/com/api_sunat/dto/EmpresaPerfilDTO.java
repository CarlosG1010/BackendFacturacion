package system.solutions.com.api_sunat.dto;

import system.solutions.com.api_sunat.entity.Tenant;

import java.util.UUID;

public record EmpresaPerfilDTO(
        UUID id,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String correo,
        String telefono,
        String regimenTributario,
        String estadoSunat,
        String condicionSunat,
        String logoUrl
) {
    public static EmpresaPerfilDTO from(Tenant tenant) {
        return new EmpresaPerfilDTO(
                tenant.getId(),
                tenant.getRuc(),
                tenant.getRazonSocial(),
                tenant.getNombreComercial(),
                tenant.getDireccionFiscal(),
                tenant.getCorreo(),
                tenant.getTelefono(),
                tenant.getRegimenTributario(),
                tenant.getEstadoSunat(),
                tenant.getCondicionSunat(),
                tenant.getLogoPath() == null || tenant.getLogoPath().isBlank()
                        ? null : "/api/empresa/mi-perfil/logo"
        );
    }
}
