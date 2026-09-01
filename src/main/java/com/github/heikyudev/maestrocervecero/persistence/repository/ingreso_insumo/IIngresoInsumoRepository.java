package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA de los ingresos de insumo ({@link IngresoInsumoEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un ingreso anulado sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IIngresoInsumoRepository extends JpaRepository<IngresoInsumoEntity, Long> {

    /**
     * Busca los ingresos de insumo, en el estado indicado, asociados a un ítem de detalle de
     * compra determinado.
     * <p>
     * Se usa para calcular la cantidad ya recibida de ese ítem (sumando {@code cantidadRecibida}
     * de los ingresos en estado {@code REGISTRADO}) y así determinar la cantidad pendiente de
     * entrega.
     * </p>
     *
     * @param idDetalleCompra El ID del ítem de detalle de compra.
     * @param estado Estado de los ingresos a buscar.
     * @return Lista de ingresos de insumo asociados a ese ítem en el estado indicado.
     */
    List<IngresoInsumoEntity> findByDetalleCompraIdAndEstado(Long idDetalleCompra, EstadoTransaccion estado);
}
