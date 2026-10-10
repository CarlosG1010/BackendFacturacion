package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.Marca;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, UUID>{

    // Soporta RF-TE-014: Buscar por nombre en la empresa actual
    List<Marca> findByNombreContainingIgnoreCase(String nombre);

}
