package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
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
}
