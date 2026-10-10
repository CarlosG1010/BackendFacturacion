package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.Usuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    // Para el Login del sistema
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = "tenant")
    Optional<Usuario> findWithTenantByEmail(String email);

    // Soporta RF-TG-008: Listar usuarios globales (donde tenant_id es NULL)
    List<Usuario> findByTenantIsNull();

    // Soporta RF-TE-004: Listar usuarios de una empresa específica (Filtrado automático por @TenantId si estás en contexto tenant)
    List<Usuario> findByTenantId(UUID tenantId);

    Optional<Usuario> findByIdAndTenantId(UUID id, UUID tenantId);

}
