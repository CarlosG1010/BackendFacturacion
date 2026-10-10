package system.solutions.com.api_sunat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import system.solutions.com.api_sunat.entity.Comprobante;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, UUID>{

    // Evita duplicidad antes de insertar (Validación del UK Fiscal)
    boolean existsByTipoComprobanteAndSerieAndNumero(String tipoComprobante, String serie, Long numero);

    @EntityGraph(attributePaths = {"cliente", "comprobanteOrigen", "detalles", "detalles.producto"})
    java.util.Optional<Comprobante> findWithEmissionDataById(UUID id);

    @Modifying
    @Query("UPDATE Comprobante c SET c.estadoSunat = :newState WHERE c.id = :id AND c.estadoSunat = :expectedState")
    int cambiarEstadoSiCoincide(@Param("id") UUID id, @Param("expectedState") String expectedState,
                                @Param("newState") String newState);

    @Modifying
    @Query("UPDATE Comprobante c SET c.estadoSunat = 'PROCESANDO' WHERE c.id = :id " +
            "AND c.estadoSunat = 'PENDIENTE_ENVIO' AND c.tipoComprobante <> '03'")
    int claimIndividual(@Param("id") UUID id);

    List<Comprobante> findTop20ByEstadoSunatOrderByFechaEmisionAsc(String estadoSunat);

    @Query("SELECT c FROM Comprobante c WHERE c.estadoSunat = 'PENDIENTE_ENVIO' " +
            "AND c.tipoComprobante <> '03' ORDER BY c.fechaEmision ASC")
    List<Comprobante> findTop20PendingIndividual();

    List<Comprobante> findByFechaEmisionGreaterThanEqualAndFechaEmisionLessThanAndTipoComprobanteAndEstadoSunat(
            LocalDateTime inicio, LocalDateTime fin, String tipoComprobante, String estadoSunat);

    List<Comprobante> findByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = {"cliente", "detalles"})
    List<Comprobante> findWithClientByIdIn(List<UUID> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comprobante c WHERE c.id IN :ids")
    List<Comprobante> lockByIds(@Param("ids") List<UUID> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comprobante c WHERE c.fechaEmision >= :inicio AND c.fechaEmision < :fin " +
            "AND c.tipoComprobante = '03' AND c.estadoSunat = 'PENDIENTE_ENVIO' AND c.loteResumenId IS NULL")
    List<Comprobante> lockBoletasPendientesResumen(@Param("inicio") LocalDateTime inicio,
                                                   @Param("fin") LocalDateTime fin);

    // Soporta RF-TE-020 y RF-TE-028: Consultar comprobantes con filtros avanzados por rango de fechas y tipo
    List<Comprobante> findByFechaEmisionBetweenAndTipoComprobante(LocalDateTime inicio, LocalDateTime fin, String tipoComprobante);

    // Soporta RF-TE-021 y RF-TE-032: Reporte totalizado de ventas agrupado por tipo de comprobante (DQL personalizada)
    @Query("SELECT c.tipoComprobante, SUM(c.montoTotal) FROM Comprobante c " +
            "WHERE c.fechaEmision BETWEEN :inicio AND :fin " +
            "GROUP BY c.tipoComprobante")
    List<Object[]> obtenerResumenVentasPorPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);


}
