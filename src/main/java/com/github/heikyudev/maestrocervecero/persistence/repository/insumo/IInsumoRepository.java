package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA base de los insumos ({@link InsumoEntity}).
 * <p>
 * Permite buscar cualquier insumo (Malta, Lúpulo o Levadura) por su ID sin conocer de
 * antemano su tipo concreto: Hibernate resuelve la subclase real gracias a la estrategia
 * {@code InheritanceType.JOINED} y a la columna discriminadora {@code tipo_insumo}. Se utiliza
 * en módulos que referencian un insumo de forma polimórfica, como el catálogo de proveedores.
 * </p>
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en la entidad: todas las consultas derivadas operan solo sobre insumos activos.
 * </p>
 */
@Repository
public interface IInsumoRepository extends JpaRepository<InsumoEntity, Long> {
}
