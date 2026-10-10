package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.dto.ComprobanteResponseDTO;
import system.solutions.com.api_sunat.dto.ComprobanteRequestDTO;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.security.AuthenticatedUser;
import system.solutions.com.api_sunat.service.ComprobanteService;
import system.solutions.com.api_sunat.sunat.SunatDocumentStorage;

import java.util.UUID;

@RestController
@RequestMapping("/comprobantes")
@RequiredArgsConstructor
public class ComprobanteController {

    private final ComprobanteService comprobanteService;
    private final SunatDocumentStorage documentStorage;

    // RF-TE-016 / RF-TE-017 / RF-TE-018 / RF-TE-019 / RF-TE-026 / RF-TE-027 / RF-TE-030 / RF-TE-031
    @PostMapping
    @PreAuthorize("@comprobanteAuthorization.puedeEmitir(authentication, #p0)")
    public ResponseEntity<ComprobanteResponseDTO> emitirComprobante(
            @RequestBody ComprobanteRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        request.setUsuarioId(authenticatedUser.id());
        Comprobante comprobante = comprobanteService.emitirComprobante(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ComprobanteResponseDTO.from(comprobante));
    }

    // RF-TE-020 / RF-TE-028 / RF-TE-029
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR', 'CONTADOR')")
    public ResponseEntity<ComprobanteResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(ComprobanteResponseDTO.from(comprobanteService.buscarPorId(id)));
    }

    @GetMapping("/{id}/cdr")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'VENDEDOR', 'CONTADOR')")
    public ResponseEntity<Resource> descargarCdr(@PathVariable UUID id) {
        Comprobante comprobante = comprobanteService.buscarPorId(id);
        String filename = "CDR-" + comprobante.getSerie() + "-" + comprobante.getNumero() + ".zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(documentStorage.cargarCdr(comprobante));
    }
}