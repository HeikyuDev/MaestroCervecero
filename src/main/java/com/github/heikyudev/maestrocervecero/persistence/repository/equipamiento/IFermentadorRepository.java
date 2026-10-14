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
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de fermentadores activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FermentadorEntity f WHERE f.estado = 'ACTIVO' "
            + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo)",
            countQuery = "SELECT COUNT(f) FROM FermentadorEntity f WHERE f.estado = 'ACTIVO' "
                    + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
                    + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo)")
    Page<FermentadorEntity> filtrarFermentadores(@Param("identificadorInterno") String identificadorInterno, @Param("estadoOperativo") EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Obtiene una página de fermentadores activos y de fermentadores dados de baja que tienen al menos un lote (en
     * cualquier estado, incluso si todos fueron cancelados), filtrados opcionalmente por identificador interno
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta). Un
     * parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de fermentadores activos y dados de baja con lotes (cancelados o no) que cumplen los criterios indicados.
     */
    @Query(value = "SELECT f FROM FermentadorEntity f WHERE (f.estado = 'ACTIVO' OR (f.estado = 'BAJA' AND EXISTS ("
            + "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION'))) "
            + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
            + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo)",
            countQuery = "SELECT COUNT(f) FROM FermentadorEntity f WHERE (f.estado = 'ACTIVO' OR (f.estado = 'BAJA' AND EXISTS ("
                    + "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION'))) "
                    + "AND (:identificadorInterno IS NULL OR UPPER(f.identificadorInterno) LIKE UPPER(CONCAT('%', CAST(:identificadorInterno AS string), '%'))) "
                    + "AND (:estadoOperativo IS NULL OR f.estadoOperativo = :estadoOperativo)")
    Page<FermentadorEntity> filtrarFermentadoresConDadosDeBajaConLotes(@Param("identificadorInterno") String identificadorInterno, @Param("estadoOperativo") EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Obtiene los fermentadores dados de baja que tienen al menos un lote que no fue cancelado (el fermentador de un
     * lote es el equipamiento de su etapa de Fermentación), ordenados por identificador interno.
     * <p>
     * Sirve para que el cronograma de producción pueda filtrar por un fermentador que ya no está activo y cuyos lotes
     * siguen en el historial.
     * </p>
     *
     * @return Los fermentadores dados de baja con lotes, o una lista vacía si no hay ninguno.
     */
    @Query("SELECT f FROM FermentadorEntity f WHERE f.estado = 'BAJA' AND EXISTS (" +
            "SELECT 1 FROM EtapaLoteEntity e WHERE e.equipamiento.id = f.id AND e.etapa = 'FERMENTACION' " +
            "AND e.lote.estado <> 'CANCELADO') " +
            "ORDER BY f.identificadorInterno ASC")
    List<FermentadorEntity> buscarDadosDeBajaConLotes();

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
