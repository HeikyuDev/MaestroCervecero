package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/***
 * Repositorio de JPA para la entidad {@link EquipamientoEntity}.
 * <p>
 * El proposito de este repositorio es el de poder realizar consultas para saber
 * Si otro equipamiento tiene el mismo identificador interno, ya que este debe ser unico entre los equipamientos activos.
 * </p>
 */
@Repository
public interface IEquipamientoRepository extends JpaRepository<EquipamientoEntity, Long> {

    boolean existsByIdentificadorInternoIgnoreCase(String identificadorInterno);

    boolean existsByIdentificadorInternoIgnoreCaseAndIdNot(String identificadorInterno, Long id);
}
