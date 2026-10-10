package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.AuditoriaEmpresa;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaEmpresaRepository extends JpaRepository<AuditoriaEmpresa, UUID>{

    // Soporta RF-TE-022: Consultar auditoría de la empresa por rango de fecha
    List<AuditoriaEmpresa> findByFechaHoraBetweenOrderByFechaHoraDesc(LocalDateTime inicio, LocalDateTime fin);

    // Filtrar auditorías de una acción o módulo específico internamente
    List<AuditoriaEmpresa> findByModuloAndFechaHoraBetween(String modulo, LocalDateTime inicio, LocalDateTime fin);

}
