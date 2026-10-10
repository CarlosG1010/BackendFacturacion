package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria_empresa", indexes = {
        @Index(name = "idx_auditoria_emp_tenant", columnList = "tenant_id, fecha_hora")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString

public class AuditoriaEmpresa {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "ip_direccion", length = 45, nullable = false)
    private String ipDireccion;

    @Column(name = "accion", length = 100, nullable = false)
    private String accion;

    @Column(name = "modulo", length = 50, nullable = false)
    private String modulo;

    @Column(name = "registro_afectado_id", length = 50)
    private String registroAfectadoId;

}
