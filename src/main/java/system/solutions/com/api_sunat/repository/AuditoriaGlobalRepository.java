package system.solutions.com.api_sunat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import system.solutions.com.api_sunat.entity.AuditoriaGlobal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditoriaGlobalRepository extends JpaRepository<AuditoriaGlobal, UUID>{

    // Soporta RF-TG-013: Consultar auditoría global por rango de fechas
    List<AuditoriaGlobal> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);

}
