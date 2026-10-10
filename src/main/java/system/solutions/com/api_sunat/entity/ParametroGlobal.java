package system.solutions.com.api_sunat.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametros_globales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString

public class ParametroGlobal {

    @Id
    @Column(name = "clave", length = 50, nullable = false)
    @EqualsAndHashCode.Include
    private String clave;

    @Column(name = "valor", columnDefinition = "TEXT", nullable = false)
    private String valor;

    @Column(name = "descripcion")
    private String descripcion;

}
