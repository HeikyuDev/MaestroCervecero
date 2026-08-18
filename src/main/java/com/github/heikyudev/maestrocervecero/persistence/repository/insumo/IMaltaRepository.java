package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las maltas ({@link MaltaEntity}).
 * <p>
 * El filtrado de registros eliminados lógicamente (soft delete) es aplicado
 * automáticamente por Hibernate gracias a la anotación {@code @SoftDelete} declarada
 * en {@code InsumoEntity}: todas las consultas derivadas operan solo sobre maltas activas.
 * </p>
 */
@Repository
public interface IMaltaRepository extends JpaRepository<MaltaEntity, Long> {

    /**
     * Verifica si existe una malta activa con el nombre dado, ignorando mayúsculas y minúsculas.
     *
     * @param nombre El nombre de la malta a buscar.
     * @return {@code true} si ya existe una malta activa con ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe una malta activa con el nombre dado, ignorando mayúsculas y minúsculas,
     * excluyendo de la búsqueda a la malta con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la malta a buscar.
     * @param id El ID de la malta a excluir de la verificación.
     * @return {@code true} si otra malta activa ya posee ese nombre, {@code false} en caso contrario.
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
