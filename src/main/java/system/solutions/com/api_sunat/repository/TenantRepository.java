package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import system.solutions.com.api_sunat.entity.Tenant;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID>{

    // Soporta RF-TG-003: Filtrar por RUC
    Optional<Tenant> findByRuc(String ruc);

    // Soporta RF-TG-003: Listar/Filtrar por Razón Social y Estado
    java.util.List<Tenant> findByRazonSocialContainingIgnoreCaseAndEstado(String razonSocial, String estado);

}
