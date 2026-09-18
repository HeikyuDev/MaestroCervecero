package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Filtra los ajustes de insumo, opcionalmente por lote de insumo y/o estado transaccional. Un
     * parámetro nulo no restringe por ese criterio.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un ajuste anulado sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados, igual que
     * {@code findAll}.
     * </p>
     *
     * @param idLoteInsumo El ID del lote de insumo a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de ajustes de insumo que cumplen los criterios indicados.
     */
    @Query(value = "SELECT a FROM AjusteInsumoEntity a WHERE "
            + "(:idLoteInsumo IS NULL OR a.loteInsumo.id = :idLoteInsumo) "
            + "AND (:estado IS NULL OR a.estado = :estado)",
            countQuery = "SELECT COUNT(a) FROM AjusteInsumoEntity a WHERE "
                    + "(:idLoteInsumo IS NULL OR a.loteInsumo.id = :idLoteInsumo) "
                    + "AND (:estado IS NULL OR a.estado = :estado)")
    Page<AjusteInsumoEntity> filtrarAjustesInsumo(@Param("idLoteInsumo") Long idLoteInsumo,
                                                   @Param("estado") EstadoTransaccion estado,
                                                   Pageable pageable);
}
