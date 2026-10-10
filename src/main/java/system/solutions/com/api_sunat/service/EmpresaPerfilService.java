package system.solutions.com.api_sunat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.dto.EmpresaPerfilDTO;
import system.solutions.com.api_sunat.entity.Tenant;
import system.solutions.com.api_sunat.repository.TenantRepository;
import system.solutions.com.api_sunat.sunat.SunatProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpresaPerfilService {

    private final TenantRepository tenantRepository;
    private final SunatProperties sunatProperties;

    @Transactional(readOnly = true)
    public EmpresaPerfilDTO perfilActual() {
        return EmpresaPerfilDTO.from(obtenerTenantActual());
    }

    @Transactional(readOnly = true)
    public Resource logoActual() {
        Tenant tenant = obtenerTenantActual();
        if (tenant.getLogoPath() == null || tenant.getLogoPath().isBlank()) {
            throw new IllegalArgumentException("La empresa no tiene un logo configurado.");
        }
        try {
            Path root = sunatProperties.getLogoRoot().toRealPath();
            Path configured = Path.of(tenant.getLogoPath());
            Path logo = (configured.isAbsolute() ? configured : root.resolve(configured)).toRealPath();
            if (!logo.startsWith(root) || !Files.isRegularFile(logo)) {
                throw new IllegalStateException("El logo debe estar dentro del directorio autorizado.");
            }
            return new FileSystemResource(logo);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo leer el logo configurado.", exception);
        }
    }

    private Tenant obtenerTenantActual() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("El usuario autenticado no pertenece a una empresa.");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("No se encontraron los datos de la empresa."));
    }
}
