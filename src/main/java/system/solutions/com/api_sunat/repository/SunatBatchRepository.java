package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import system.solutions.com.api_sunat.entity.SunatBatch;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SunatBatchRepository extends JpaRepository<SunatBatch, UUID> {

    List<SunatBatch> findTop20ByEstadoInOrderByCreadoEnAsc(List<String> states);

    List<SunatBatch> findTop20ByTipoLoteAndEstadoOrderByCreadoEnAsc(String tipoLote, String estado);

    List<SunatBatch> findTop20ByEstadoOrderByCreadoEnAsc(String estado);

    @Query("SELECT COALESCE(MAX(b.correlativo), 0) FROM SunatBatch b " +
            "WHERE b.tipoLote = :type AND b.fechaDocumentos = :date")
    int maxCorrelativo(@Param("type") String type, @Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE SunatBatch b SET b.estado = :next WHERE b.id = :id AND b.estado = :expected")
    int claim(@Param("id") UUID id, @Param("expected") String expected, @Param("next") String next);
}
