package system.solutions.com.api_sunat.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
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

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalles_comprobante"))
    private Comprobante comprobante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_detalles_producto"))
    private Producto producto;

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
