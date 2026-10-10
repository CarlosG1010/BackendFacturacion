package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "productos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_productos_tenant_codigo", columnNames = {"tenant_id", "codigo_interno"}) // CORREGIDO: columnNames
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"categoria", "marca"})
public class Producto {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", foreignKey = @ForeignKey(name = "fk_productos_categoria"))
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id", foreignKey = @ForeignKey(name = "fk_productos_marca"))
    private Marca marca;

    @Column(name = "codigo_interno", length = 50, nullable = false)
    private String codigoInterno;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "precio_unitario", precision = 14, scale = 4, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "unidad_medida", length = 10, nullable = false)
    private String unidadMedida;

    @Column(name = "stock_actual", precision = 12, scale = 4, nullable = false)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;
}
