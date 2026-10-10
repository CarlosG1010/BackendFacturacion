package system.solutions.com.api_sunat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenants_ruc", columnList = "ruc"),
        @Index(name = "idx_tenants_estado", columnList = "estado")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString

public class Tenant extends AuditableEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID(); // En producción, mapear a generador UUIDv7

    @Column(name = "ruc", length = 11, unique = true, nullable = false)
    private String ruc;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(name = "nombre_comercial")
    private String nombreComercial;

    @Column(name = "direccion_fiscal", nullable = false)
    private String direccionFiscal;

    @Column(name = "correo", length = 100, nullable = false)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "regimen_tributario", length = 50, nullable = false)
    private String regimenTributario;

    @Column(name = "estado", length = 20, nullable = false)
    private String estado; // ACTIVO, INACTIVO

    @Column(name = "estado_sunat", length = 50)
    private String estadoSunat;

    @Column(name = "condicion_sunat", length = 50)
    private String condicionSunat;

    @Column(name = "certificado_digital_path", length = 500)
    private String certificadoDigitalPath;

    @Column(name = "logo_path", length = 500)
    private String logoPath;
}
