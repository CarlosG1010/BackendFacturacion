package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "usuarios", indexes = {
        @Index(name = "idx_usuarios_email", columnList = "email"),
        @Index(name = "idx_usuarios_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(exclude = "tenant")

public class Usuario extends AuditableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", foreignKey = @ForeignKey(name = "fk_usuarios_tenant"))
    private Tenant tenant; // NULL para administradores globales y operadores de sistemas

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    @Column(name = "nombre_completo", length = 150, nullable = false)
    private String nombreCompleto;

    @Column(name = "rol", length = 30, nullable = false)
    private String rol; // ADMIN_GLOBAL, OPERADOR_SISTEMAS, ADMIN_EMPRESA, VENDEDOR, CONTADOR

    @Column(name = "estado", length = 20, nullable = false)
    private String estado; // ACTIVO, INACTIVO

}
