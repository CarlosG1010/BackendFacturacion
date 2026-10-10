package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;

@Entity
@Table(name = "control_series", uniqueConstraints = {
        @UniqueConstraint(name = "uk_series_tenant_tipo", columnNames = {"tenant_id", "tipo_comprobante", "serie"}) // CORREGIDO: columnNames
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class ControlSeries {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "tipo_comprobante", length = 2, nullable = false)
    private String tipoComprobante; // 01=Factura, 03=Boleta, 07=NC, 08=ND

    @Column(name = "serie", length = 4, nullable = false)
    private String serie;

    @Column(name = "ultimo_numero", nullable = false)
    private Long ultimoNumero = 0L;
}
