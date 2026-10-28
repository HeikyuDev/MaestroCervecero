package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de los lotes de insumo ({@link LoteInsumoEntity}).
 */
@Repository
public interface ILoteInsumoRepository extends JpaRepository<LoteInsumoEntity, Long> {

    /**
     * Busca, bloqueándolo para escritura, el lote de insumo de un insumo con una identificación
     * de lote de proveedor determinada.
     * <p>
     * Se usa para decidir, al registrar un ingreso, si corresponde unificar la cantidad recibida
     * en un lote ya existente o crear uno nuevo. El bloqueo evita que dos ingresos concurrentes
     * del mismo insumo y la misma identificación de lote de proveedor (por ejemplo, dos operarios
     * cargando el mismo remito) decidan ambos que el lote no existe y creen cada uno el suyo,
     * duplicando lo que debería ser un único lote físico.
     * </p>
     *
     * @param idInsumo El ID del insumo.
     * @param identificacionLoteProveedor Identificación del lote asignada por el proveedor, ya normalizada como código (mayúsculas y sin espacios de más).
     * @return Un Optional que contiene el lote si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.insumo.id = :idInsumo AND li.identificacionLoteProveedor = :identificacionLoteProveedor")
    Optional<LoteInsumoEntity> buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(@Param("idInsumo") Long idInsumo, @Param("identificacionLoteProveedor") String identificacionLoteProveedor);

    /**
     * Busca, bloqueándolos para escritura, los lotes de insumo no vencidos de un insumo determinado,
     * ordenados por fecha de vencimiento ascendente (criterio FEFO: First Expired, First Out).
     * <p>
     * Un lote de insumo está vencido cuando su fecha de vencimiento es anterior a la fecha indicada: si
     * vence ese mismo día todavía es válido. Los lotes vencidos no cuentan como stock disponible.
     * </p>
     * <p>
     * Se usa al iniciar un lote de producción, para reservar stock priorizando siempre el
     * insumo que vence antes. El bloqueo evita que dos lotes de producción reserven, al mismo
     * tiempo, el mismo stock físico.
     * </p>
     *
     * @param idInsumo El ID del insumo cuyo stock se quiere reservar.
     * @param hoy La fecha de referencia: se excluyen los lotes que vencieron antes de ella.
     * @return Los lotes de insumo no vencidos de ese insumo, ordenados por fecha de vencimiento ascendente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.insumo.id = :idInsumo AND li.fechaVencimiento >= :hoy ORDER BY li.fechaVencimiento ASC")
    List<LoteInsumoEntity> buscarNoVencidosPorInsumoIdOrdenadoPorVencimientoParaReservar(@Param("idInsumo") Long idInsumo, @Param("hoy") LocalDate hoy);

    /**
     * Busca, bloqueándolos para escritura, los lotes de insumo no vencidos de varios insumos a la vez, ordenados por ID.
     * <p>
     * Se usa al iniciar un lote de producción: las filas se bloquean siempre en orden ascendente de ID, el mismo orden con
     * el que cualquier otra operación bloquea varios lotes de insumo, de modo que dos operaciones nunca se esperen
     * mutuamente. El orden FEFO lo aplica luego quien llama.
     * </p>
     *
     * @param idsInsumo Los IDs de los insumos cuyo stock se quiere reservar.
     * @param hoy La fecha de referencia: se excluyen los lotes que vencieron antes de ella.
     * @return Los lotes de insumo no vencidos de esos insumos, ordenados por ID ascendente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.insumo.id IN :idsInsumo AND li.fechaVencimiento >= :hoy ORDER BY li.id ASC")
    List<LoteInsumoEntity> buscarNoVencidosPorInsumosIdsParaReservar(@Param("idsInsumo") Collection<Long> idsInsumo, @Param("hoy") LocalDate hoy);

    /**
     * Busca, bloqueándolo para escritura, un lote de insumo por su ID.
     * <p>
     * Se usa al cancelar un lote, para liberar de forma segura la cantidad reservada sobre el
     * lote de insumo correspondiente a cada reserva, evitando que otra operación concurrente
     * (una nueva reserva, un ingreso, un ajuste) lo modifique al mismo tiempo.
     * </p>
     *
     * @param id El ID del lote de insumo.
     * @return Un Optional que contiene el lote de insumo si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.id = :id")
    Optional<LoteInsumoEntity> buscarPorIdParaLiberarReserva(@Param("id") Long id);

    /**
     * Busca, bloqueándolo para escritura, un lote de insumo por su ID.
     * <p>
     * Se usa al registrar un consumo de insumo (reservado o directo), para evitar que otra
     * operación concurrente (otro consumo, un ajuste) modifique el mismo lote de insumo al mismo
     * tiempo mientras se descuenta su stock.
     * </p>
     *
     * @param id El ID del lote de insumo.
     * @return Un Optional que contiene el lote de insumo si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.id = :id")
    Optional<LoteInsumoEntity> buscarPorIdParaConsumir(@Param("id") Long id);

    /**
     * Busca, bloqueándolo para escritura, un lote de insumo por su ID.
     * <p>
     * Se usa al registrar o anular un ajuste de insumo, para evitar que otra operación
     * concurrente (un consumo, otro ajuste) modifique el mismo lote de insumo al mismo tiempo
     * mientras se ajusta su stock.
     * </p>
     *
     * @param id El ID del lote de insumo.
     * @return Un Optional que contiene el lote de insumo si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.id = :id")
    Optional<LoteInsumoEntity> buscarPorIdParaAjustar(@Param("id") Long id);

    /**
     * Busca, bloqueándolo para escritura, un lote de insumo por su ID.
     * <p>
     * Se usa al anular un ingreso de insumo, para evitar que otra operación concurrente (un
     * consumo, un ajuste, otro ingreso) modifique el mismo lote de insumo al mismo tiempo mientras
     * se descuenta la cantidad anulada de su stock.
     * </p>
     *
     * @param id El ID del lote de insumo.
     * @return Un Optional que contiene el lote de insumo si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.id = :id")
    Optional<LoteInsumoEntity> buscarPorIdParaAnularIngreso(@Param("id") Long id);

    /**
     * Busca los lotes de insumo de un insumo determinado que todavía tienen cantidad disponible
     * (cantidad actual menos la reservada), ordenados por fecha de vencimiento ascendente (FEFO).
     * <p>
     * Se usa para mostrarle al operario, al registrar un consumo directo, entre qué lotes de
     * insumo físico puede elegir. No bloquea: es una consulta de solo lectura para armar una lista
     * en pantalla; el lote que el operario finalmente elija se vuelve a buscar (bloqueado) recién
     * al confirmar el consumo, con {@link #buscarPorIdParaConsumir(Long)}.
     * </p>
     *
     * @param idInsumo El ID del insumo.
     * @return Los lotes de insumo de ese insumo con cantidad disponible mayor a cero.
     */
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.insumo.id = :idInsumo AND (li.cantidadActual - li.cantidadReservada) > 0 ORDER BY li.fechaVencimiento ASC")
    List<LoteInsumoEntity> filtrarLotesInsumoDisponibles(@Param("idInsumo") Long idInsumo);

    /**
     * Verifica si existe algún lote de insumo de un insumo determinado con cantidad actual mayor
     * a la indicada.
     * <p>
     * Se usa para impedir la baja lógica de un insumo que todavía tiene stock físico.
     * </p>
     *
     * @param idInsumo El ID del insumo a verificar.
     * @param cantidadActual Cantidad actual mínima (exclusiva) a considerar.
     * @return {@code true} si existe al menos un lote de ese insumo con cantidad actual mayor a la indicada, {@code false} en caso contrario.
     */
    boolean existsByInsumo_IdAndCantidadActualGreaterThan(Long idInsumo, Double cantidadActual);

    /**
     * Filtra los lotes de insumo con saldo disponible (cantidad actual menos reservada, mayor a 0 al redondear a 4
     * decimales) de los insumos indicados, para el consumo directo. Los criterios de texto nulos no restringen.
     * <p>
     * El orden es FEFO y lo define la consulta, para que la paginación sea estable: primero los vigentes por fecha de
     * vencimiento ascendente (un lote que vence hoy es vigente), luego los que no tienen fecha de vencimiento y al
     * final los vencidos (fecha anterior a {@code hoy}), también por fecha; ante igual fecha, por ID.
     * </p>
     *
     * @param idsInsumo Los IDs de los insumos (no vacío).
     * @param nombreInsumo Texto a buscar dentro del nombre del insumo (ya normalizado), o {@code null}.
     * @param identificacion Texto a buscar dentro de la identificación del lote del proveedor (ya normalizado), o {@code null}.
     * @param hoy La fecha contra la que se decide si un lote está vencido.
     * @param pageable Paginación; el orden lo define la consulta.
     * @return Una página de lotes de insumo con su insumo cargado.
     */
    @Query(value = "SELECT li FROM LoteInsumoEntity li JOIN FETCH li.insumo i WHERE i.id IN :idsInsumo "
            + "AND (li.cantidadActual - li.cantidadReservada) > 0.00005 "
            + "AND (:nombreInsumo IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', CAST(:nombreInsumo AS string), '%'))) "
            + "AND (:identificacion IS NULL OR UPPER(li.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', CAST(:identificacion AS string), '%'))) "
            + "ORDER BY CASE WHEN li.fechaVencimiento IS NULL THEN 1 WHEN li.fechaVencimiento < :hoy THEN 2 ELSE 0 END ASC, "
            + "li.fechaVencimiento ASC, li.id ASC",
            countQuery = "SELECT COUNT(li) FROM LoteInsumoEntity li JOIN li.insumo i WHERE i.id IN :idsInsumo "
                    + "AND (li.cantidadActual - li.cantidadReservada) > 0.00005 "
                    + "AND (:nombreInsumo IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', CAST(:nombreInsumo AS string), '%'))) "
                    + "AND (:identificacion IS NULL OR UPPER(li.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', CAST(:identificacion AS string), '%')))")
    Page<LoteInsumoEntity> filtrarLotesInsumoDisponiblesParaConsumo(@Param("idsInsumo") Collection<Long> idsInsumo,
                                                                     @Param("nombreInsumo") String nombreInsumo,
                                                                     @Param("identificacion") String identificacion,
                                                                     @Param("hoy") LocalDate hoy,
                                                                     Pageable pageable);

    /**
     * Filtra los lotes de un insumo sin importar su saldo (también los agotados), para elegir el lote existente al
     * registrar un ingreso. Un criterio de texto nulo no restringe.
     * <p>
     * El orden lo define la consulta, para que la paginación sea estable: por fecha de vencimiento ascendente y, ante
     * igual fecha, por ID.
     * </p>
     *
     * @param idInsumo El ID del insumo.
     * @param identificacion Texto a buscar dentro de la identificación del lote del proveedor (ya normalizado), o {@code null}.
     * @param pageable Paginación; el orden lo define la consulta.
     * @return Una página de lotes de insumo con su insumo cargado.
     */
    @Query(value = "SELECT li FROM LoteInsumoEntity li JOIN FETCH li.insumo i WHERE i.id = :idInsumo "
            + "AND (:identificacion IS NULL OR UPPER(li.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', CAST(:identificacion AS string), '%'))) "
            + "ORDER BY li.fechaVencimiento ASC, li.id ASC",
            countQuery = "SELECT COUNT(li) FROM LoteInsumoEntity li JOIN li.insumo i WHERE i.id = :idInsumo "
                    + "AND (:identificacion IS NULL OR UPPER(li.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', CAST(:identificacion AS string), '%')))")
    Page<LoteInsumoEntity> filtrarLotesInsumoPorInsumo(@Param("idInsumo") Long idInsumo,
                                                       @Param("identificacion") String identificacion,
                                                       Pageable pageable);
}
