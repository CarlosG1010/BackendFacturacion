package system.solutions.com.api_sunat.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "comprobantes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_comprobantes_fiscal", columnNames = {"tenant_id", "tipo_comprobante", "serie", "numero"}) // CORREGIDO: columnNames
}, indexes = {
        @Index(name = "idx_comprobantes_busqueda", columnList = "tenant_id, fecha_emision, tipo_comprobante") // Mantenemos columnList aquí
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"detalles", "cliente", "comprobanteOrigen"})
public class Comprobante {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "tipo_comprobante", length = 2, nullable = false)
    private String tipoComprobante;

    @Column(name = "serie", length = 4, nullable = false)
    private String serie;

    @Column(name = "numero", nullable = false)
    private Long numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comprobantes_cliente"))
    private Cliente cliente;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_origen_id", foreignKey = @ForeignKey(name = "fk_comprobantes_origen"))
    private Comprobante comprobanteOrigen;

    @Column(name = "monto_gravado", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoGravado;

    @Column(name = "monto_exonerado", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoExonerado = BigDecimal.ZERO;

    @Column(name = "monto_inafecto", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoInafecto = BigDecimal.ZERO;

    @Column(name = "monto_gratuito", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoGratuito = BigDecimal.ZERO;

    @Column(name = "monto_igv", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoIgv;

    @Column(name = "monto_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "moneda", length = 3, nullable = false)
    private String moneda = "PEN";

    @Column(name = "tipo_cambio", precision = 14, scale = 6)
    private BigDecimal tipoCambio;

    @Column(name = "estado_sunat", length = 30, nullable = false)
    private String estadoSunat;

    @Column(name = "motivo_nota_codigo", length = 4)
    private String motivoNotaCodigo;

    @Column(name = "motivo_nota", length = 500)
    private String motivoNota;

    @Column(name = "hash_tributario", length = 128)
    private String hashTributario;

    @Column(name = "codigo_respuesta_sunat", length = 20)
    private String codigoRespuestaSunat;

    @Column(name = "mensaje_respuesta_sunat", length = 2000)
    private String mensajeRespuestaSunat;

    @Column(name = "xml_firmado_path", length = 1000)
    private String xmlFirmadoPath;

    @Column(name = "cdr_path", length = 1000)
    private String cdrPath;

    @Column(name = "ticket_sunat", length = 100)
    private String ticketSunat;

    @Column(name = "fecha_envio_sunat")
    private LocalDateTime fechaEnvioSunat;

    @Column(name = "lote_resumen_id")
    private UUID loteResumenId;

    @Column(name = "lote_baja_id")
    private UUID loteBajaId;

    @Column(name = "estado_anulacion", length = 30)
    private String estadoAnulacion;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @JsonManagedReference
    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ComprobanteDetalle> detalles = new ArrayList<>();

    public void addDetalle(ComprobanteDetalle detalle) {
        detalles.add(detalle);
        detalle.setComprobante(this);
    }
}
