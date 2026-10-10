package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import system.solutions.com.api_sunat.dto.ComunicacionBajaRequestDTO;
import system.solutions.com.api_sunat.dto.ResumenDiarioRequestDTO;
import system.solutions.com.api_sunat.dto.SunatBatchResponseDTO;
import system.solutions.com.api_sunat.sunat.SunatBatchService;

import java.util.UUID;

@RestController
@RequestMapping("/sunat/lotes")
@RequiredArgsConstructor
public class SunatBatchController {

    private final SunatBatchService batchService;

    @PostMapping("/resumen-diario")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    public SunatBatchResponseDTO crearResumen(@RequestBody ResumenDiarioRequestDTO request) {
        return batchService.crearResumen(request.fecha());
    }

    @PostMapping("/comunicaciones-baja")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.ACCEPTED)
    public SunatBatchResponseDTO crearBaja(@RequestBody ComunicacionBajaRequestDTO request) {
        return batchService.crearBaja(request.comprobanteIds(), request.motivo());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'CONTADOR')")
    public SunatBatchResponseDTO estado(@PathVariable UUID id) {
        return batchService.obtener(id);
    }
}
