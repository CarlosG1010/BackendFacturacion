package system.solutions.com.api_sunat.entity;

import lombok.*;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "marcas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_marcas_tenant_nombre", columnNames = {"tenant_id", "nombre"}) // CORREGIDO: columnNames
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Marca {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;
}
