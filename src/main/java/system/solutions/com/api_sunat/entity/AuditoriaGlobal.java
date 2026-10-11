package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria_global", indexes = {
        @Index(name = "idx_auditoria_gl_fecha", columnList = "fecha_hora")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString

public class AuditoriaGlobal {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "rol", length = 30, nullable = false)
    private String rol;

    @Column(name = "tenant_id")
    private UUID tenantId; // Empresa afectada (opcional)

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_auditoria_global_tenant"))
    private Tenant tenant;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "ip_direccion", length = 45, nullable = false)
    private String ipDireccion;

    @Column(name = "accion", length = 50, nullable = false)
    private String accion;

    @Column(name = "modulo", length = 50, nullable = false)
    private String modulo;

    @Column(name = "registro_afectado_id", length = 50)
    private String registroAfectadoId;

}
