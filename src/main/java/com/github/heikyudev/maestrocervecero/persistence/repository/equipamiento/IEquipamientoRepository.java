package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
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
}
