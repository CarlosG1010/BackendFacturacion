package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.entity.AuditoriaEmpresa;
import system.solutions.com.api_sunat.entity.AuditoriaGlobal;
import system.solutions.com.api_sunat.service.AuditoriaService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/auditorias")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    // RF-TG-013: Consultar auditoría global de la plataforma (Rango de fechas)
    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN_GLOBAL')")
    public ResponseEntity<List<AuditoriaGlobal>> consultarAuditoriaGlobal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(auditoriaService.consultarAuditoriaGlobal(inicio, fin));
    }

    // RF-TE-022: Consultar auditoría interna de operaciones de la empresa actual
    @GetMapping("/empresa")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<List<AuditoriaEmpresa>> consultarAuditoriaEmpresa(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(auditoriaService.consultarAuditoriaEmpresa(inicio, fin));
    }

}
