package system.solutions.com.api_sunat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sunat_lotes", indexes = {
        @Index(name = "idx_sunat_lotes_tenant_estado", columnList = "tenant_id, estado, creado_en")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SunatBatch {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private UUID id = UUID.randomUUID();

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "tipo_lote", length = 20, nullable = false)
    private String tipoLote;

    @Column(name = "fecha_documentos", nullable = false)
    private LocalDate fechaDocumentos;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;

    @Column(name = "documentos_json", columnDefinition = "nvarchar(max)", nullable = false)
    private String documentosJson;

    @Column(name = "nombre_archivo", length = 120, nullable = false)
    private String nombreArchivo;

    @Column(name = "estado", length = 30, nullable = false)
    private String estado;

    @Column(name = "ticket_sunat", length = 100)
    private String ticketSunat;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 2000)
    private String mensajeRespuesta;

    @Column(name = "xml_firmado_path", length = 1000)
    private String xmlFirmadoPath;

    @Column(name = "cdr_path", length = 1000)
    private String cdrPath;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}
