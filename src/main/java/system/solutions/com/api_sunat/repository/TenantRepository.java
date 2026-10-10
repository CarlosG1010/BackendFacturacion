package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import system.solutions.com.api_sunat.entity.Tenant;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID>{

    // Soporta RF-TG-003: Filtrar por RUC
    Optional<Tenant> findByRuc(String ruc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tenant t WHERE t.id = :id")
    Optional<Tenant> findByIdForUpdate(@Param("id") UUID id);

    // Soporta RF-TG-003: Listar/Filtrar por Razón Social y Estado
    java.util.List<Tenant> findByRazonSocialContainingIgnoreCaseAndEstado(String razonSocial, String estado);

}
