package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * @param identificacionLoteProveedor Identificación del lote asignada por el proveedor.
     * @return Un Optional que contiene el lote si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.insumo.id = :idInsumo AND li.identificacionLoteProveedor = :identificacionLoteProveedor")
    Optional<LoteInsumoEntity> buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(@Param("idInsumo") Long idInsumo, @Param("identificacionLoteProveedor") String identificacionLoteProveedor);

    /**
     * Busca, bloqueándolos para escritura, todos los lotes de insumo de un insumo determinado,
     * ordenados por fecha de vencimiento ascendente (criterio FEFO: First Expired, First Out).
     * <p>
     * Se usa al iniciar un lote de producción, para reservar stock priorizando siempre el
     * insumo que vence antes. El bloqueo evita que dos lotes de producción reserven, al mismo
     * tiempo, el mismo stock físico.
     * </p>
     *
     * @param idInsumo El ID del insumo cuyo stock se quiere reservar.
     * @return Los lotes de insumo de ese insumo, ordenados por fecha de vencimiento ascendente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT li FROM LoteInsumoEntity li WHERE li.insumo.id = :idInsumo ORDER BY li.fechaVencimiento ASC")
    List<LoteInsumoEntity> buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(@Param("idInsumo") Long idInsumo);

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
}
