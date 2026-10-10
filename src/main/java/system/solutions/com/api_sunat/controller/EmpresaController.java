package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import system.solutions.com.api_sunat.dto.EmpresaPerfilDTO;
import system.solutions.com.api_sunat.service.EmpresaPerfilService;

import java.nio.file.Files;
import java.time.Duration;

@RestController
@RequestMapping("/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaPerfilService empresaPerfilService;

    @GetMapping("/mi-perfil")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR', 'CONTADOR')")
    public ResponseEntity<EmpresaPerfilDTO> perfil() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(empresaPerfilService.perfilActual());
    }

    @GetMapping("/mi-perfil/logo")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR', 'CONTADOR')")
    public ResponseEntity<Resource> logo() {
        Resource image = empresaPerfilService.logoActual();
        MediaType contentType;
        try {
            String detected = Files.probeContentType(image.getFile().toPath());
            if (detected == null || !detected.startsWith("image/")) {
                throw new IllegalStateException("El logo debe ser una imagen válida.");
            }
            contentType = MediaType.parseMediaType(detected);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo determinar el formato del logo.", exception);
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(image);
    }
}
