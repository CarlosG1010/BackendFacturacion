package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;

@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clientes_tenant_doc", columnNames = {"tenant_id", "tipo_documento", "numero_documento"}) // CORREGIDO: columnNames
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
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

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