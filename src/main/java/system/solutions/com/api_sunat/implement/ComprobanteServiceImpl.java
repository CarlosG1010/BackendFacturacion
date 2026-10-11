package system.solutions.com.api_sunat.implement;

import system.solutions.com.api_sunat.dto.ComprobanteRequestDTO;
import system.solutions.com.api_sunat.dto.DetalleRequestDTO;
import system.solutions.com.api_sunat.entity.Cliente;
import system.solutions.com.api_sunat.entity.Comprobante;
import system.solutions.com.api_sunat.entity.ComprobanteDetalle;
import system.solutions.com.api_sunat.entity.Producto;
import system.solutions.com.api_sunat.repository.ClienteRepository;
import system.solutions.com.api_sunat.repository.ComprobanteRepository;
import system.solutions.com.api_sunat.repository.ProductoRepository;
import system.solutions.com.api_sunat.service.ComprobanteService;
import system.solutions.com.api_sunat.service.ControlSeriesService;
import system.solutions.com.api_sunat.config.TenantContext;
import system.solutions.com.api_sunat.util.RequestInput;
import system.solutions.com.api_sunat.sunat.ComprobanteSubmissionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ComprobanteServiceImpl implements ComprobanteService {

    private final ComprobanteRepository comprobanteRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ControlSeriesService controlSeriesService;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal PORCENTAJE_IGV = new BigDecimal("0.18");

    @Override
    @Transactional
    public Comprobante emitirComprobante(ComprobanteRequestDTO request) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("No se puede emitir un comprobante sin tenant autenticado.");
        }
        validarSolicitud(request);
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente seleccionado no existe."));
        if (!"ACTIVO".equals(cliente.getEstado())) {
            throw new IllegalArgumentException("El cliente seleccionado está inactivo.");
        }

        Comprobante comprobante = new Comprobante();
        comprobante.setTipoComprobante(request.getTipoComprobante());
        comprobante.setSerie(request.getSerie());
        comprobante.setCliente(cliente);
        comprobante.setClienteId(cliente.getId());
        comprobante.setUsuarioId(request.getUsuarioId());
        comprobante.setFechaEmision(LocalDateTime.now());
        comprobante.setEstadoSunat("PENDIENTE_ENVIO");
        comprobante.setMotivoNotaCodigo(request.getMotivoNotaCodigo());
        comprobante.setMotivoNota(request.getMotivoNota());
        String currency = request.getMoneda() == null ? "PEN" : request.getMoneda().toUpperCase(Locale.ROOT);
        comprobante.setMoneda(currency);
        comprobante.setTipoCambio(request.getTipoCambio());

        Map<UUID, ComprobanteDetalle> sourceDetailsByProduct = new HashMap<>();
        Map<UUID, BigDecimal> sourceQuantitiesByProduct = new HashMap<>();
        if (request.getComprobanteOrigenId() != null) {
            Comprobante origen = comprobanteRepository.findWithEmissionDataById(request.getComprobanteOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("El comprobante de origen referenciado no existe."));
            if (!cliente.getId().equals(origen.getCliente().getId())) {
                throw new IllegalArgumentException("El comprobante de origen debe corresponder al mismo cliente.");
            }
            if (!Set.of("01", "03").contains(origen.getTipoComprobante())
                    || !"ACEPTADO".equals(origen.getEstadoSunat())) {
                throw new IllegalArgumentException("La nota debe referenciar una factura o boleta aceptada por SUNAT.");
            }
            origen.getDetalles().forEach(detail -> {
                UUID productId = detail.getProducto().getId();
                ComprobanteDetalle existing = sourceDetailsByProduct.putIfAbsent(productId, detail);
                if (existing != null && existing.getPrecioUnitario().compareTo(detail.getPrecioUnitario()) != 0) {
                    throw new IllegalArgumentException("Las líneas repetidas del producto tienen precios distintos y no se pueden consolidar en una nota.");
                }
                sourceQuantitiesByProduct.merge(productId, detail.getCantidad(), BigDecimal::add);
            });
            comprobante.setComprobanteOrigen(origen);
        }

        Long numeroCorrelativo = controlSeriesService.obtenerSiguienteCorrelativo(
                request.getTipoComprobante(),
                request.getSerie()
        );
        comprobante.setNumero(numeroCorrelativo);

        Set<UUID> productIds = new HashSet<>();
        request.getDetalles().forEach(detail -> productIds.add(detail.getProductoId()));
        Map<UUID, Producto> productsById = new HashMap<>();
        productoRepository.findAllById(productIds).forEach(product -> productsById.put(product.getId(), product));

        boolean venta = "01".equals(request.getTipoComprobante()) || "03".equals(request.getTipoComprobante());
        boolean nota = !venta;
        for (DetalleRequestDTO detail : request.getDetalles()) {
            if (nota && !sourceDetailsByProduct.containsKey(detail.getProductoId())) {
                throw new IllegalArgumentException("El producto de la nota no aparece en el comprobante de origen.");
            }
            if (!nota && !productsById.containsKey(detail.getProductoId())) {
                throw new IllegalArgumentException("Uno de los productos seleccionados no existe.");
            }
        }
        if (venta) {
            for (DetalleRequestDTO detail : request.getDetalles()) {
                Producto product = productsById.get(detail.getProductoId());
                if (product == null) {
                    throw new IllegalArgumentException("Uno de los productos seleccionados no existe.");
                }
                if (!"ACTIVO".equals(product.getEstado())) {
                    throw new IllegalStateException("El producto '" + product.getDescripcion() + "' no se encuentra activo.");
                }
                if (productoRepository.disminuirStock(product.getId(), detail.getCantidad()) == 0) {
                    throw new IllegalStateException("Stock insuficiente para el producto: " + product.getDescripcion());
                }
            }
        }

        BigDecimal totalComprobante = BigDecimal.ZERO;
        BigDecimal totalGravado = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal totalExonerado = BigDecimal.ZERO;
        BigDecimal totalInafecto = BigDecimal.ZERO;
        BigDecimal totalGratuito = BigDecimal.ZERO;
        Map<UUID, BigDecimal> noteQuantities = new HashMap<>();

        for (DetalleRequestDTO detalleDto : request.getDetalles()) {
            ComprobanteDetalle sourceDetail = sourceDetailsByProduct.get(detalleDto.getProductoId());
            Producto producto = sourceDetail == null
                    ? productsById.get(detalleDto.getProductoId())
                    : sourceDetail.getProducto();
            if (producto == null) {
                throw new IllegalArgumentException("Uno de los productos seleccionados no existe.");
            }

            BigDecimal precioEnSoles = sourceDetail == null
                    ? producto.getPrecioUnitario()
                    : sourceDetail.getPrecioUnitario();
            if (sourceDetail != null && !"PEN".equals(comprobante.getMoneda())) {
                throw new IllegalArgumentException("Las notas en moneda extranjera requieren conservar y convertir la moneda del comprobante origen.");
            }
            BigDecimal precioUnitario = "USD".equals(comprobante.getMoneda())
                    ? precioEnSoles.divide(request.getTipoCambio(), 4, RoundingMode.HALF_UP)
                    : precioEnSoles;
            if (sourceDetail != null) {
                BigDecimal cumulativeQuantity = noteQuantities.merge(
                        producto.getId(), detalleDto.getCantidad(), BigDecimal::add);
                if (cumulativeQuantity.compareTo(sourceQuantitiesByProduct.get(producto.getId())) > 0) {
                    throw new IllegalArgumentException("La cantidad de la nota supera el detalle del comprobante de origen.");
                }
            }
            String taxCode = detalleDto.getTipoAfectacionIgv() == null ? "10" : detalleDto.getTipoAfectacionIgv();
            boolean free = detalleDto.isGratuito();
            BigDecimal referenceTotal = precioUnitario.multiply(detalleDto.getCantidad()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalItem = free ? BigDecimal.ZERO : referenceTotal;
            BigDecimal subtotalItem;
            BigDecimal igvItem = BigDecimal.ZERO;
            if ("10".equals(taxCode) || "11".equals(taxCode)) {
                subtotalItem = referenceTotal.divide(BigDecimal.ONE.add(PORCENTAJE_IGV), 2, RoundingMode.HALF_UP);
                if (!free) {
                    igvItem = totalItem.subtract(subtotalItem);
                }
            } else {
                subtotalItem = referenceTotal;
            }

            ComprobanteDetalle detalle = new ComprobanteDetalle();
            detalle.setTenantId(tenantId);
            detalle.setComprobante(comprobante);
            detalle.setComprobanteId(comprobante.getId());
            detalle.setProducto(producto);
            detalle.setProductoId(producto.getId());
            detalle.setCantidad(detalleDto.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotalItem);

            detalle.setIgv(igvItem);
            detalle.setTotal(totalItem);
            detalle.setTipoAfectacionIgv(free ? freeAffectationCode(taxCode) : taxCode);
            detalle.setGratuito(free);
            comprobante.addDetalle(detalle);

            totalComprobante = totalComprobante.add(totalItem);
            if (free) {
                totalGratuito = totalGratuito.add(referenceTotal);
            } else if ("10".equals(taxCode)) {
                totalGravado = totalGravado.add(subtotalItem);
            } else if ("20".equals(taxCode)) {
                totalExonerado = totalExonerado.add(subtotalItem);
            } else if ("30".equals(taxCode)) {
                totalInafecto = totalInafecto.add(subtotalItem);
            } else {
                throw new IllegalArgumentException("Tipo de afectación IGV no soportado.");
            }
            totalIgv = totalIgv.add(igvItem);
        }

        comprobante.setMontoGravado(totalGravado);
        comprobante.setMontoExonerado(totalExonerado);
        comprobante.setMontoInafecto(totalInafecto);
        comprobante.setMontoGratuito(totalGratuito);
        comprobante.setMontoIgv(totalIgv);
        comprobante.setMontoTotal(totalComprobante);

        Comprobante persisted = comprobanteRepository.save(comprobante);
        eventPublisher.publishEvent(new ComprobanteSubmissionEvent(persisted.getId(), tenantId));
        return persisted;
    }

    @Override
    @Transactional(readOnly = true)
    public Comprobante buscarPorId(UUID id) {
        return comprobanteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El comprobante solicitado no existe."));
    }

    private void validarSolicitud(ComprobanteRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud del comprobante es obligatoria.");
        }
        if (request.getTipoComprobante() == null
                || !Set.of("01", "03", "07", "08").contains(request.getTipoComprobante())) {
            throw new IllegalArgumentException("Tipo de comprobante no soportado.");
        }
        String expectedPrefix = "03".equals(request.getTipoComprobante()) ? "B" : "F";
        if (request.getSerie() == null || !request.getSerie().matches("[A-Z0-9]{4}")
                || !request.getSerie().startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("La serie debe tener cuatro caracteres y prefijo "
                    + expectedPrefix + " para este tipo de comprobante.");
        }
        if (request.getClienteId() == null || request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Cliente y al menos un detalle son obligatorios.");
        }
        if (request.getDetalles().size() > 100) {
            throw new IllegalArgumentException("Un comprobante no puede tener más de 100 líneas.");
        }
        for (DetalleRequestDTO detail : request.getDetalles()) {
            if (detail == null || detail.getProductoId() == null || detail.getCantidad() == null
                    || detail.getCantidad().signum() <= 0 || detail.getCantidad().precision() > 12
                    || detail.getCantidad().scale() > 4) {
                throw new IllegalArgumentException("Cada detalle requiere un producto y una cantidad mayor que cero.");
            }
            String affectation = detail.getTipoAfectacionIgv() == null ? "10" : detail.getTipoAfectacionIgv();
            if (!Set.of("10", "20", "30").contains(affectation)) {
                throw new IllegalArgumentException("Afectación IGV válida: 10 gravado, 20 exonerado, 30 inafecto.");
            }
            if (detail.isGratuito() && !"10".equals(affectation)) {
                throw new IllegalArgumentException("En esta versión las operaciones gratuitas se soportan para bienes gravados.");
            }
        }
        String currency = request.getMoneda() == null ? "PEN" : request.getMoneda().toUpperCase(Locale.ROOT);
        if (!Set.of("PEN", "USD").contains(currency)) {
            throw new IllegalArgumentException("La moneda debe ser PEN o USD.");
        }
        if ("PEN".equals(currency) && request.getTipoCambio() != null) {
            throw new IllegalArgumentException("No se debe indicar tipo de cambio para comprobantes PEN.");
        }
        if ("USD".equals(currency) && (request.getTipoCambio() == null || request.getTipoCambio().signum() <= 0
                || request.getTipoCambio().precision() > 14 || request.getTipoCambio().scale() > 4)) {
            throw new IllegalArgumentException("El tipo de cambio PEN por USD es obligatorio, positivo y admite hasta 4 decimales.");
        }
        boolean note = "07".equals(request.getTipoComprobante()) || "08".equals(request.getTipoComprobante());
        if (note != (request.getComprobanteOrigenId() != null)) {
            throw new IllegalArgumentException("Las notas requieren comprobante de origen y los demás tipos no lo admiten.");
        }
        if (note) {
            String reasonCode = RequestInput.requiredText(request.getMotivoNotaCodigo(), "motivoNotaCodigo", 2);
            if (!reasonCode.matches("\\d{2}")) {
                throw new IllegalArgumentException("El código de motivo de la nota debe tener dos dígitos.");
            }
            request.setMotivoNotaCodigo(reasonCode);
            request.setMotivoNota(RequestInput.requiredText(request.getMotivoNota(), "motivoNota", 250));
        } else if (request.getMotivoNotaCodigo() != null || request.getMotivoNota() != null) {
            throw new IllegalArgumentException("El motivo solo se permite en notas de crédito o débito.");
        }
        request.setSerie(RequestInput.requiredText(request.getSerie(), "serie", 4));
    }

    private String freeAffectationCode(String taxableCode) {
        return switch (taxableCode) {
            case "10" -> "11";
            case "20" -> "21";
            case "30" -> "31";
            default -> throw new IllegalArgumentException("Tipo de afectación gratuita no soportado.");
        };
    }
}
