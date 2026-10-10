package system.solutions.com.api_sunat.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import system.solutions.com.api_sunat.entity.ControlSeries;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ControlSeriesRepository extends JpaRepository<ControlSeries, UUID>{

    // LOCK CRÍTICO por Fila (Pessimistic Write) para evitar que dos vendedores obtengan el mismo número correlativo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ControlSeries c WHERE c.tipoComprobante = :tipo AND c.serie = :serie")
    Optional<ControlSeries> findByTipoComprobanteAndSerieForUpdate(@Param("tipo") String tipoComprobante, @Param("serie") String serie);

}