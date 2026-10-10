package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.ParametroGlobal;

@Repository
public interface ParametroGlobalRepository extends JpaRepository<ParametroGlobal, String>{
    // Maneja claves únicas como 'IGV_PORCENTAJE', 'SUNAT_ENDPOINT_URL', etc. (RF-TG-014)
}
