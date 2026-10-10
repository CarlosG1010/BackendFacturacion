package system.solutions.com.api_sunat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.Cliente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    // Soporta RF-TE-007 y RF-TE-025: Buscar cliente por documento dentro del Tenant
    Optional<Cliente> findByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);

    // Buscar clientes por nombre para autocompletar en ventas
    List<Cliente> findByNombreRazonSocialContainingIgnoreCaseAndEstado(String nombre, String estado);

}
