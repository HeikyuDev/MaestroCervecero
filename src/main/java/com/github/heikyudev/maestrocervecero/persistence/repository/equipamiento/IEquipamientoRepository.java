package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/***
 * Repositorio de JPA para la entidad {@link EquipamientoEntity}.
 * <p>
 * El proposito de este repositorio es el de poder realizar consultas para saber
 * Si otro equipamiento tiene el mismo identificador interno, ya que este debe ser unico entre los equipamientos activos
 * (cualquiera sea su tipo concreto: Molino, Macerador, Olla de Hervor o Fermentador).
 * </p>
 */
@Repository
public interface IEquipamientoRepository extends JpaRepository<EquipamientoEntity, Long> {

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EquipamientoEntity e WHERE UPPER(e.identificadorInterno) = UPPER(:identificadorInterno) AND e.estado = 'ACTIVO'")
    boolean existsByIdentificadorInternoIgnoreCase(@Param("identificadorInterno") String identificadorInterno);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM EquipamientoEntity e WHERE UPPER(e.identificadorInterno) = UPPER(:identificadorInterno) AND e.id <> :id AND e.estado = 'ACTIVO'")
    boolean existsByIdentificadorInternoIgnoreCaseAndIdNot(@Param("identificadorInterno") String identificadorInterno, @Param("id") Long id);

    /**
     * Busca, entre TODOS los tipos concretos de equipamiento a la vez, una página de equipamientos
     * activos filtrados opcionalmente por identificador interno (coincidencia parcial, sin
     * distinguir mayúsculas/minúsculas), tipo concreto (Molino/Macerador/OllaHervor/Fermentador) y/o
     * estado operativo (ambos por coincidencia exacta). Un parámetro nulo no restringe por ese
     * criterio.
     * <p>
     * Se usa en contextos donde no importa el tipo concreto del equipamiento (por ejemplo, al
     * registrar una limpieza de equipamiento genérica). Para listar únicamente los equipamientos
     * de un tipo determinado con todos sus datos técnicos propios, usar el método
     * {@code filtrarX} del repositorio específico de ese tipo en su lugar.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param tipoClase Clase concreta de equipamiento a filtrar (ver {@link com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento#getEntityClass()}), o {@code null} para no filtrar por tipo.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de equipamientos activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT e FROM EquipamientoEntity e WHERE e.estado = 'ACTIVO' "
            + "AND (:identificadorInterno IS NULL OR UPPER(e.identificadorInterno) LIKE UPPER(CONCAT('%', :identificadorInterno, '%'))) "
            + "AND (:tipoClase IS NULL OR TYPE(e) = :tipoClase) "
            + "AND (:estadoOperativo IS NULL OR e.estadoOperativo = :estadoOperativo)",
            countQuery = "SELECT COUNT(e) FROM EquipamientoEntity e WHERE e.estado = 'ACTIVO' "
                    + "AND (:identificadorInterno IS NULL OR UPPER(e.identificadorInterno) LIKE UPPER(CONCAT('%', :identificadorInterno, '%'))) "
                    + "AND (:tipoClase IS NULL OR TYPE(e) = :tipoClase) "
                    + "AND (:estadoOperativo IS NULL OR e.estadoOperativo = :estadoOperativo)")
    Page<EquipamientoEntity> filtrarEquipamientos(@Param("identificadorInterno") String identificadorInterno,
                                                    @Param("tipoClase") Class<? extends EquipamientoEntity> tipoClase,
                                                    @Param("estadoOperativo") EstadoOperativo estadoOperativo,
                                                    Pageable pageable);
}
