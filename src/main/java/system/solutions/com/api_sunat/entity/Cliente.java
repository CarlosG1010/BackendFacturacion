package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;

@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_tenant_doc", columnNames = {"tenant_id", "tipo_documento", "numero_documento"}),
        @UniqueConstraint(name = "uk_clientes_tenant_id", columnNames = {"tenant_id", "id"}) // CORREGIDO: columnNames
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Cliente {

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
            foreignKey = @ForeignKey(name = "fk_clientes_tenant"))
    private Tenant tenant;

    @Column(name = "tipo_documento", length = 10, nullable = false)
    private String tipoDocumento; // DNI, RUC, CE

    @Column(name = "numero_documento", length = 20, nullable = false)
    private String numeroDocumento;

    @Column(name = "nombre_razon_social", nullable = false)
    private String nombreRazonSocial;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado; // ACTIVO, INACTIVO
}