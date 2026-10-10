package system.solutions.com.api_sunat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.Categoria;
import java.util.List;
import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, UUID>{

    // Soporta RF-TE-013: Buscar por nombre en la empresa actual
    List<Categoria> findByNombreContainingIgnoreCase(String nombre);

}
