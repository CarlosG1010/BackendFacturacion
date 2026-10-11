package system.solutions.com.api_sunat.entity;

import lombok.*;
import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "marcas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_marcas_tenant_nombre", columnNames = {"tenant_id", "nombre"}),
        @UniqueConstraint(name = "uk_marcas_tenant_id", columnNames = {"tenant_id", "id"}) // CORREGIDO: columnNames
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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_marcas_tenant"))
    private Tenant tenant;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado;
}
