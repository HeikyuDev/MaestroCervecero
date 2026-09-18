package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de los lotes ({@link LoteEntity}).
 * <p>
 * A diferencia de otras entidades del sistema, {@code LoteEntity} no maneja un
 * ciclo de vida ACTIVO/BAJA: su campo {@code estado} representa el estado
 * productivo del lote (PENDIENTE, EN_EJECUCION, FINALIZADO, ANULADO), no una baja
 * lógica. Por eso no se sobrescriben {@code findById}/{@code findAll} con ningún
 * filtro adicional.
 * </p>
 */
@Repository
public interface ILoteRepository extends JpaRepository<LoteEntity, Long> {

    /**
     * Busca, entre los lotes que todavía ocupan un lugar en el cronograma del fermentador dado
     * (no anulados ni finalizados), la fecha de finalización estimada más lejana ya planificada.
     * Sirve para encolar un nuevo lote detrás del último que ya tiene reservado ese fermentador.
     *
     * @param fermentador   El fermentador cuyo cronograma se quiere consultar.
     * @param estadosVigentes Los estados de lote que se consideran "todavía ocupando" el fermentador.
     * @return La fecha de finalización estimada más lejana, o vacío si el fermentador no tiene ningún lote planificado.
     */
    @Query("SELECT MAX(l.fechaFinalizacionEstimada) FROM LoteEntity l JOIN l.etapas e " +
            "WHERE e.equipamiento = :fermentador AND l.estado IN :estadosVigentes")
    Optional<LocalDate> buscarUltimaFechaFinalizacionEstimadaPorFermentador(
            @Param("fermentador") FermentadorEntity fermentador,
            @Param("estadosVigentes") List<EstadoLote> estadosVigentes);

    /**
     * Filtra los lotes registrados, opcionalmente por receta, identificador interno (coincidencia
     * parcial, sin distinguir mayúsculas/minúsculas), estado, etapa actualmente en curso y volumen
     * objetivo (ambos por coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param idReceta El ID de la receta (a través de la versión de receta vigente del lote) a filtrar, o {@code null} para no filtrar por ella.
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estado El estado del lote a filtrar, o {@code null} para no filtrar por él.
     * @param etapaActual El tipo de etapa actualmente EN_CURSO a filtrar, o {@code null} para no filtrar por ella.
     * @param volumenObjetivo El volumen objetivo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de lotes que cumplen los criterios indicados.
     */
    @Query(value = "SELECT l FROM LoteEntity l WHERE "
            + "(:idReceta IS NULL OR l.planificacionProduccion.versionReceta.receta.id = :idReceta) "
            + "AND (:identificadorInterno IS NULL OR UPPER(l.identificadorInterno) LIKE UPPER(CONCAT('%', :identificadorInterno, '%'))) "
            + "AND (:estado IS NULL OR l.estado = :estado) "
            + "AND (:etapaActual IS NULL OR EXISTS (SELECT 1 FROM EtapaLoteEntity el WHERE el.lote = l AND el.estado = 'EN_CURSO' AND el.etapa = :etapaActual)) "
            + "AND (:volumenObjetivo IS NULL OR l.volumenObjetivo = :volumenObjetivo)",
            countQuery = "SELECT COUNT(l) FROM LoteEntity l WHERE "
                    + "(:idReceta IS NULL OR l.planificacionProduccion.versionReceta.receta.id = :idReceta) "
                    + "AND (:identificadorInterno IS NULL OR UPPER(l.identificadorInterno) LIKE UPPER(CONCAT('%', :identificadorInterno, '%'))) "
                    + "AND (:estado IS NULL OR l.estado = :estado) "
                    + "AND (:etapaActual IS NULL OR EXISTS (SELECT 1 FROM EtapaLoteEntity el WHERE el.lote = l AND el.estado = 'EN_CURSO' AND el.etapa = :etapaActual)) "
                    + "AND (:volumenObjetivo IS NULL OR l.volumenObjetivo = :volumenObjetivo)")
    Page<LoteEntity> filtrarLotes(@Param("idReceta") Long idReceta,
                                   @Param("identificadorInterno") String identificadorInterno,
                                   @Param("estado") EstadoLote estado,
                                   @Param("etapaActual") TipoEtapa etapaActual,
                                   @Param("volumenObjetivo") Double volumenObjetivo,
                                   Pageable pageable);
}
