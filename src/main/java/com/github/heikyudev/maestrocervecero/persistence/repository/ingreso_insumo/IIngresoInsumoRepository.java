package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    /**
     * Filtra los ingresos de insumo, opcionalmente por insumo, identificación del lote del
     * proveedor, estado, tipo de ingreso y/o fecha de ingreso. Un parámetro nulo no restringe por
     * ese criterio.
     * <p>
     * {@code idInsumo} e {@code identificacionLoteProveedor} viven en {@code LoteInsumoEntity}, no
     * en {@code IngresoInsumoEntity}: se resuelven a través de la relación {@code loteInsumo}.
     * {@code estado} no asume {@code REGISTRADO} por defecto como en otros módulos: un ingreso
     * anulado sigue siendo un registro histórico consultable, así que {@code null} muestra ambos
     * estados, igual que {@code findAll}.
     * </p>
     *
     * @param idInsumo El ID del insumo a filtrar, o {@code null} para no filtrar por él.
     * @param identificacionLoteProveedor Texto a buscar dentro de la identificación del lote del proveedor, o {@code null} para no filtrar por ella.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param tipoIngreso El tipo de ingreso exacto a filtrar (COMPRA/DIRECTO), o {@code null} para no filtrar por él.
     * @param fechaIngreso La fecha de ingreso exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de ingresos de insumo que cumplen los criterios indicados.
     */
    @Query(value = "SELECT i FROM IngresoInsumoEntity i WHERE "
            + "(:idInsumo IS NULL OR i.loteInsumo.insumo.id = :idInsumo) "
            + "AND (:identificacionLoteProveedor IS NULL OR UPPER(i.loteInsumo.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', :identificacionLoteProveedor, '%'))) "
            + "AND (:estado IS NULL OR i.estado = :estado) "
            + "AND (:tipoIngreso IS NULL OR i.tipoIngreso = :tipoIngreso) "
            + "AND (:fechaIngreso IS NULL OR i.fechaIngreso = :fechaIngreso)",
            countQuery = "SELECT COUNT(i) FROM IngresoInsumoEntity i WHERE "
                    + "(:idInsumo IS NULL OR i.loteInsumo.insumo.id = :idInsumo) "
                    + "AND (:identificacionLoteProveedor IS NULL OR UPPER(i.loteInsumo.identificacionLoteProveedor) LIKE UPPER(CONCAT('%', :identificacionLoteProveedor, '%'))) "
                    + "AND (:estado IS NULL OR i.estado = :estado) "
                    + "AND (:tipoIngreso IS NULL OR i.tipoIngreso = :tipoIngreso) "
                    + "AND (:fechaIngreso IS NULL OR i.fechaIngreso = :fechaIngreso)")
    Page<IngresoInsumoEntity> filtrarIngresoInsumo(@Param("idInsumo") Long idInsumo,
                                                    @Param("identificacionLoteProveedor") String identificacionLoteProveedor,
                                                    @Param("estado") EstadoTransaccion estado,
                                                    @Param("tipoIngreso") TipoIngreso tipoIngreso,
                                                    @Param("fechaIngreso") LocalDate fechaIngreso,
                                                    Pageable pageable);
}
