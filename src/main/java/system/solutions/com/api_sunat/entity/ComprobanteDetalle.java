package system.solutions.com.api_sunat.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "comprobantes_detalle")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "comprobante")

public class ComprobanteDetalle {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_comprobantes_detalle_tenant"))
    private Tenant tenant;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "comprobante_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    }, foreignKey = @ForeignKey(name = "fk_detalles_comprobante_tenant"))
    private Comprobante comprobante;

    @Column(name = "comprobante_id", nullable = false)
    private UUID comprobanteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(value = {
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "producto_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false)
    }, foreignKey = @ForeignKey(name = "fk_detalles_producto_tenant"))
    private Producto producto;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(name = "cantidad", precision = 12, scale = 4, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", precision = 14, scale = 4, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 14, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "igv", precision = 14, scale = 2, nullable = false)
    private BigDecimal igv;

    @Column(name = "total", precision = 14, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "tipo_afectacion_igv", length = 2, nullable = false)
    private String tipoAfectacionIgv = "10";

    @Column(name = "es_gratuito", nullable = false)
    private boolean gratuito;

}
