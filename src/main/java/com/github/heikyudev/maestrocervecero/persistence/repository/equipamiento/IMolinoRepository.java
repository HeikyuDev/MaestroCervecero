package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IMolinoRepository extends JpaRepository<MolinoEntity, Long> {

    /**
     * Busca un molino activo por su ID.
     */
    @Override
    @Query("SELECT mo FROM MolinoEntity mo WHERE mo.id = :id AND mo.estado = 'ACTIVO'")
    Optional<MolinoEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de molinos activos, filtrados opcionalmente por identificador interno
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo
     * (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de molinos activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT mo FROM MolinoEntity mo WHERE mo.estado = 'ACTIVO' "
            + "AND (:identificadorInterno IS NULL OR UPPER(mo.identificadorInterno) LIKE UPPER(CONCAT('%', :identificadorInterno, '%'))) "
            + "AND (:estadoOperativo IS NULL OR mo.estadoOperativo = :estadoOperativo)",
            countQuery = "SELECT COUNT(mo) FROM MolinoEntity mo WHERE mo.estado = 'ACTIVO' "
                    + "AND (:identificadorInterno IS NULL OR UPPER(mo.identificadorInterno) LIKE UPPER(CONCAT('%', :identificadorInterno, '%'))) "
                    + "AND (:estadoOperativo IS NULL OR mo.estadoOperativo = :estadoOperativo)")
    Page<MolinoEntity> filtrarMolinos(@Param("identificadorInterno") String identificadorInterno, @Param("estadoOperativo") EstadoOperativo estadoOperativo, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EquipamientoEntity e WHERE e.id = :id AND e.estado = 'ACTIVO'")
    Optional<MolinoEntity> buscarPorIdParaIniciarLote(@Param("id") Long id);
}
