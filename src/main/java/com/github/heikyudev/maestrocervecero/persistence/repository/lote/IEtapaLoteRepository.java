package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

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
     * Verifica si el equipamiento indicado tiene por delante, todavía sin arrancar, alguna etapa
     * {@code PENDIENTE} de un lote {@code PENDIENTE} o {@code EN_EJECUCION}.
     * <p>
     * Se utiliza para impedir la modificación o baja de un equipamiento (Molino, Macerador,
     * Olla de Hervor o Fermentador) que ya está comprometido con trabajo futuro. Cubre tanto un
     * lote {@code PENDIENTE} (ninguna de sus etapas arrancó todavía) como un lote ya
     * {@code EN_EJECUCION} que tiene una etapa <em>posterior</em> todavía {@code PENDIENTE} — por
     * ejemplo, el Fermentador reservado para la etapa de Fermentación mientras el lote todavía
     * está en Molienda: ese lote ya está {@code EN_EJECUCION}, pero el Fermentador sigue
     * comprometido con una etapa futura que aún no llegó.
     * </p>
     * <p>
     * No hace falta contemplar la etapa {@code EN_CURSO} acá: mientras una etapa está en curso,
     * el equipamiento que usa queda en estado operativo {@code EN_USO}, y eso ya lo bloquea cada
     * service por separado (ver {@link com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo}).
     * Tampoco etapas {@code FINALIZADA} o {@code CANCELADA}: ya no representan un compromiso
     * futuro sobre el equipamiento.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a verificar.
     * @return {@code true} si existe al menos una etapa pendiente asociada, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EtapaLoteEntity e " +
            "WHERE e.equipamiento.id = :idEquipamiento AND e.estado = 'PENDIENTE' " +
            "AND e.lote.estado IN ('PENDIENTE', 'EN_EJECUCION')")
    boolean existsLotePendienteAsociado(@Param("idEquipamiento") Long idEquipamiento);

    /**
     * Busca la fecha de finalización de la última etapa en la que el equipamiento indicado
     * participó de verdad: etapas {@code FINALIZADA}, o {@code CANCELADA} con {@code fechaInicio}
     * no nula (el lote se canceló mientras esa etapa estaba {@code EN_CURSO}, es decir, el
     * equipamiento sí llegó a usarse).
     * <p>
     * Una etapa {@code CANCELADA} con {@code fechaInicio} nula nunca llegó a {@code EN_CURSO} (el
     * lote se canceló mientras esa etapa seguía {@code PENDIENTE}), así que no representa uso real
     * del equipamiento y se excluye. Una etapa {@code EN_CURSO} tampoco se contempla: mientras está
     * en curso, el equipamiento queda en estado operativo {@code EN_USO}, que ya bloquea el
     * registro de cualquier operación de su ciclo de vida (Falla/Mantenimiento/Limpieza).
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento cuya última participación se quiere buscar.
     * @return La fecha de finalización más reciente entre esas etapas, o {@link Optional#empty()} si el equipamiento nunca participó en ninguna.
     */
    @Query("SELECT MAX(e.fechaFinalizacion) FROM EtapaLoteEntity e " +
            "WHERE e.equipamiento.id = :idEquipamiento " +
            "AND (e.estado = 'FINALIZADA' OR (e.estado = 'CANCELADA' AND e.fechaInicio IS NOT NULL))")
    Optional<LocalDateTime> buscarFechaUltimaParticipacionRegistrada(@Param("idEquipamiento") Long idEquipamiento);
}
