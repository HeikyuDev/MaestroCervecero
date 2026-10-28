package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IFermentadorRepository extends JpaRepository<FermentadorEntity, Long> {

    /**
     * Busca un fermentador activo por su ID.
     */
    @Override
    @Query("SELECT f FROM FermentadorEntity f WHERE f.id = :id AND f.estado = 'ACTIVO'")
    Optional<FermentadorEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de fermentadores activos, filtrados opcionalmente por identificador
     * interno (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo
     * (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     * <p>
     * El orden es natural por identificador interno: primero el prefijo (sin distinguir mayúsculas) y luego el número,
     * de modo que {@code 2} va antes que {@code 11} y {@code 11} antes que {@code 100}. Un identificador heredado que no
     * termina en número queda ordenado por su texto (sin distinguir mayúsculas) entre los que comparten ese prefijo. El
     * orden del {@code Pageable} solo desempata.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de fermentadores activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FermentadorEntity f WHERE f.estado = 'ACTIVO' "
            + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo) ORDER BY UPPER(function('regexp_replace', f.identificadorInterno, '[0-9]+$', '')), LENGTH(function('regexp_replace', f.identificadorInterno, '^.*[^0-9]', '')), function('regexp_replace', f.identificadorInterno, '^.*[^0-9]', ''), UPPER(f.identificadorInterno), f.id",
            countQuery = "SELECT COUNT(f) FROM FermentadorEntity f WHERE f.estado = 'ACTIVO' "
                    + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
                    + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo)")
    Page<FermentadorEntity> filtrarFermentadores(@Param("identificadorInterno") String identificadorInterno, @Param("estadoOperativo") EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Obtiene una página de fermentadores activos y de fermentadores dados de baja que tienen al menos un lote (en
     * cualquier estado, incluso si todos fueron cancelados), filtrados opcionalmente por identificador interno
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta). Un
     * parámetro nulo no restringe por ese criterio.
     * <p>
     * El orden es natural por identificador interno: primero el prefijo (sin distinguir mayúsculas) y luego el número,
     * de modo que {@code 2} va antes que {@code 11} y {@code 11} antes que {@code 100}. Un identificador heredado que no
     * termina en número queda ordenado por su texto (sin distinguir mayúsculas) entre los que comparten ese prefijo. El
     * orden del {@code Pageable} solo desempata.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de fermentadores activos y dados de baja con lotes (cancelados o no) que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FermentadorEntity f WHERE (f.estado = 'ACTIVO' OR (f.estado = 'BAJA' AND EXISTS ("
            + "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION'))) "
            + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo) ORDER BY UPPER(function('regexp_replace', f.identificadorInterno, '[0-9]+$', '')), LENGTH(function('regexp_replace', f.identificadorInterno, '^.*[^0-9]', '')), function('regexp_replace', f.identificadorInterno, '^.*[^0-9]', ''), UPPER(f.identificadorInterno), f.id",
            countQuery = "SELECT COUNT(f) FROM FermentadorEntity f WHERE (f.estado = 'ACTIVO' OR (f.estado = 'BAJA' AND EXISTS ("
                    + "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION'))) "
                    + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
                    + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo)")
    Page<FermentadorEntity> filtrarFermentadoresConDadosDeBajaConLotes(@Param("identificadorInterno") String identificadorInterno, @Param("estadoOperativo") EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Obtiene los fermentadores dados de baja que tienen al menos un lote en cualquier estado, incluso si todos fueron
     * cancelados (el fermentador de un lote es el equipamiento de su etapa de Fermentación), ordenados por identificador interno.
     * <p>
     * Sirve para que el cronograma de producción pueda filtrar por un fermentador que ya no está activo y cuyos lotes
     * siguen en el historial.
     * </p>
     * <p>
     * El orden es natural por identificador interno: primero el prefijo (sin distinguir mayúsculas) y luego el número,
     * de modo que {@code 2} va antes que {@code 11} y {@code 11} antes que {@code 100}. Un identificador heredado que no
     * termina en número queda ordenado por su texto (sin distinguir mayúsculas) entre los que comparten ese prefijo. El
     * orden del {@code Pageable} solo desempata.
     * </p>
     *
     * @return Los fermentadores dados de baja con lotes, o una lista vacía si no hay ninguno.
     */
    @Query("SELECT f FROM FermentadorEntity f WHERE f.estado = 'BAJA' AND EXISTS (" +
            "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION') " +
            "ORDER BY UPPER(function('regexp_replace', f.identificadorInterno, '[0-9]+$', '')), LENGTH(function('regexp_replace', f.identificadorInterno, '^.*[^0-9]', '')), function('regexp_replace', f.identificadorInterno, '^.*[^0-9]', ''), UPPER(f.identificadorInterno), f.id")
    List<FermentadorEntity> buscarDadosDeBajaConLotes();

    /**
     * Busca un fermentador por su ID si está activo o si está dado de baja y tiene al menos un lote en cualquier
     * estado (el fermentador de un lote es el equipamiento de su etapa de Fermentación).
     * <p>
     * Sirve para resolver el fermentador elegido en los filtros de la lista de lotes, que también ofrecen los
     * fermentadores dados de baja con historial.
     * </p>
     *
     * @param id El ID del fermentador.
     * @return Un Optional con el fermentador activo o dado de baja con lotes, o vacío en caso contrario.
     */
    @Query("SELECT f FROM FermentadorEntity f WHERE f.id = :id AND (f.estado = 'ACTIVO' OR (f.estado = 'BAJA' AND EXISTS ("
            + "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION')))")
    Optional<FermentadorEntity> buscarActivoODadoDeBajaConLotesPorId(@Param("id") Long id);

    /**
     * Busca, bloqueándolo para escritura, un fermentador por su ID.
     * <p>
     * Se usa cada vez que una operación de lote necesita cambiar su estado operativo (al iniciar
     * un lote, al finalizar la etapa que lo usó, o al cancelar el lote), para evitar que otra
     * operación concurrente lo modifique al mismo tiempo.
     * </p>
     *
     * @param id El ID del fermentador.
     * @return Un Optional que contiene el fermentador si existe y está activo, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EquipamientoEntity e WHERE e.id = :id AND e.estado = 'ACTIVO'")
    Optional<FermentadorEntity> buscarPorIdParaCambiarEstadoOperativo(@Param("id") Long id);
}
