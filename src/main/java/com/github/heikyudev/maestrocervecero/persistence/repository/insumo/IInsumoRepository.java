package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA base de los insumos ({@link InsumoEntity}).
 * <p>
 * Permite buscar cualquier insumo (Malta, Lúpulo o Levadura) por su ID sin conocer de
 * antemano su tipo concreto: Hibernate resuelve la subclase real gracias a la estrategia
 * {@code InheritanceType.JOINED} y a la columna discriminadora {@code tipo_insumo}. Se utiliza
 * en módulos que referencian un insumo de forma polimórfica, como el catálogo de proveedores.
 * </p>
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de insumos dados de baja se
 * realiza explícitamente mediante la condición {@code estado = 'ACTIVO'}.
 * </p>
 */
@Repository
public interface IInsumoRepository extends JpaRepository<InsumoEntity, Long> {

    /**
     * Busca un insumo activo por su ID, cualquiera sea su tipo concreto.
     *
     * @param id El ID del insumo a buscar.
     * @return Un Optional que contiene el insumo si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT i FROM InsumoEntity i WHERE i.id = :id AND i.estado = 'ACTIVO'")
    Optional<InsumoEntity> findById(@Param("id") Long id);
}
