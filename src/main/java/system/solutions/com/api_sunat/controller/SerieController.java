package system.solutions.com.api_sunat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import system.solutions.com.api_sunat.dto.SerieRequestDTO;
import system.solutions.com.api_sunat.entity.ControlSeries;
import system.solutions.com.api_sunat.service.ControlSeriesService;

import java.util.List;

@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_EMPRESA')")
public class SerieController {

    private final ControlSeriesService controlSeriesService;

    @PostMapping
    public ResponseEntity<ControlSeries> configurar(@RequestBody SerieRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                controlSeriesService.configurar(
                        request.tipoComprobante(), request.serie(), request.ultimoNumero()));
    }

    @GetMapping
    public ResponseEntity<List<ControlSeries>> listar() {
        return ResponseEntity.ok(controlSeriesService.listar());
    }
}
