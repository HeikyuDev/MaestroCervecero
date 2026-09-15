package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las etapas de lote ({@link EtapaLoteEntity}).
 * <p>
 * Hasta ahora, las etapas de un lote solo se accedían indirectamente a través de
 * {@link com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity#getEtapas()}.
 * Este repositorio existe para poder resolver una etapa puntual por su propio ID (por ejemplo, al
 * registrar una medición, donde el formulario indica directamente sobre qué etapa se mide).
 * </p>
 */
@Repository
public interface IEtapaLoteRepository extends JpaRepository<EtapaLoteEntity, Long> {

    /**
     * Verifica si el equipamiento indicado está asignado a la etapa de algún lote cuyo estado
     * sea {@code PENDIENTE} (todavía no arrancó a producirse).
     * <p>
     * Se utiliza para impedir la modificación o baja de un equipamiento (Molino, Macerador,
     * Olla de Hervor o Fermentador) que ya está comprometido con un lote a punto de arrancar.
     * No contempla lotes {@code EN_EJECUCION}: un lote en ejecución puede tener equipamiento de
     * etapas ya finalizadas completamente libre (ver {@link com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo}
     * del propio equipamiento, que es la fuente de verdad de si está actualmente en uso).
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a verificar.
     * @return {@code true} si existe al menos un lote pendiente asociado, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EtapaLoteEntity e " +
            "WHERE e.equipamiento.id = :idEquipamiento AND e.lote.estado = 'PENDIENTE'")
    boolean existsLotePendienteAsociado(@Param("idEquipamiento") Long idEquipamiento);
}
