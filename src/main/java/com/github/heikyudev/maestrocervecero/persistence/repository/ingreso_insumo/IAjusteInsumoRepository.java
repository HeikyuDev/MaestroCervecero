package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio JPA de los ajustes de insumo ({@link AjusteInsumoEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un ajuste anulado sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IAjusteInsumoRepository extends JpaRepository<AjusteInsumoEntity, Long> {

    /**
     * Filtra los ajustes de insumo, opcionalmente por lote de insumo, tipo de ajuste, estado
     * transaccional y/o rango de fecha de registro. Un parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un ajuste anulado sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param idLoteInsumo El ID del lote de insumo a filtrar, o {@code null} para no filtrar por él.
     * @param tipoAjuste El tipo (del motivo de ajuste) a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDesde Límite inferior (inclusive) del rango de fecha de registro, o {@code null} para no acotarlo.
     * @param fechaHasta Límite superior (inclusive) del rango de fecha de registro, o {@code null} para no acotarlo.
     * @param pageable La configuración de paginación.
     * @return Una página de ajustes de insumo que cumplen los criterios indicados.
     */
    @Query(value = "SELECT a FROM AjusteInsumoEntity a WHERE "
            + "(:idLoteInsumo IS NULL OR a.loteInsumo.id = :idLoteInsumo) "
            + "AND (:tipoAjuste IS NULL OR a.motivoAjuste.tipoAjuste = :tipoAjuste) "
            + "AND (:estado IS NULL OR a.estado = :estado) "
            + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR a.createdDate >= :fechaDesde) "
            + "AND (CAST(:fechaHasta AS LocalDateTime) IS NULL OR a.createdDate <= :fechaHasta)",
            countQuery = "SELECT COUNT(a) FROM AjusteInsumoEntity a WHERE "
                    + "(:idLoteInsumo IS NULL OR a.loteInsumo.id = :idLoteInsumo) "
                    + "AND (:tipoAjuste IS NULL OR a.motivoAjuste.tipoAjuste = :tipoAjuste) "
                    + "AND (:estado IS NULL OR a.estado = :estado) "
                    + "AND (CAST(:fechaDesde AS LocalDateTime) IS NULL OR a.createdDate >= :fechaDesde) "
                    + "AND (CAST(:fechaHasta AS LocalDateTime) IS NULL OR a.createdDate <= :fechaHasta)")
    Page<AjusteInsumoEntity> filtrarAjustesInsumo(@Param("idLoteInsumo") Long idLoteInsumo,
                                                   @Param("tipoAjuste") TipoAjuste tipoAjuste,
                                                   @Param("estado") EstadoTransaccion estado,
                                                   @Param("fechaDesde") LocalDateTime fechaDesde,
                                                   @Param("fechaHasta") LocalDateTime fechaHasta,
                                                   Pageable pageable);

    /**
     * Verifica si un motivo de ajuste tiene al menos un ajuste de insumo asociado, en cualquier estado (REGISTRADO o ANULADO).
     * <p>
     * Se usa para fijar el tipo (INGRESO/EGRESO) de un motivo ya usado y para impedir su baja: los ajustes anulados
     * también conservan la referencia al motivo.
     * </p>
     *
     * @param idMotivoAjuste El ID del motivo de ajuste a verificar.
     * @return {@code true} si existe al menos un ajuste de insumo con ese motivo, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AjusteInsumoEntity a " +
            "WHERE a.motivoAjuste.id = :idMotivoAjuste")
    boolean existsByMotivoAjusteId(@Param("idMotivoAjuste") Long idMotivoAjuste);

    /**
     * Busca un ajuste de insumo por su ID y bloquea su fila hasta el fin de la transacción
     * ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Se usa al anular el ajuste: dos anulaciones simultáneas del mismo ajuste se ejecutan una a una, y la segunda
     * encuentra el ajuste ya {@code ANULADO} en lugar de revertir dos veces su cantidad en el lote.
     * </p>
     *
     * @param id El ID del ajuste de insumo.
     * @return Un Optional que contiene el ajuste bloqueado, o vacío si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AjusteInsumoEntity a WHERE a.id = :id")
    Optional<AjusteInsumoEntity> buscarPorIdParaAnular(@Param("id") Long id);
}
