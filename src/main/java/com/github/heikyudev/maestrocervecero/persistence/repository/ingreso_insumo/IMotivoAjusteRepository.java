package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los motivos de ajuste ({@link MotivoAjusteEntity}).
 * <p>
 * La entidad no utiliza {@code @SoftDelete}: el filtrado de motivos de ajuste dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}. Los
 * métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se sobrescriben
 * con esa misma condición para que todo el código existente que ya los invoca (sin cambios) siga
 * viendo únicamente motivos de ajuste activos.
 * </p>
 */
@Repository
public interface IMotivoAjusteRepository extends JpaRepository<MotivoAjusteEntity, Long> {

    /**
     * Busca un motivo de ajuste activo por su ID.
     *
     * @param id El ID del motivo de ajuste a buscar.
     * @return Un Optional que contiene el motivo de ajuste si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT ma FROM MotivoAjusteEntity ma WHERE ma.id = :id AND ma.estado = 'ACTIVO'")
    Optional<MotivoAjusteEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de motivos de ajuste activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de motivos de ajuste activos.
     */
    @Override
    @Query(value = "SELECT ma FROM MotivoAjusteEntity ma WHERE ma.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(ma) FROM MotivoAjusteEntity ma WHERE ma.estado = 'ACTIVO'")
    Page<MotivoAjusteEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe un motivo de ajuste activo con el nombre dado, ignorando mayúsculas y
     * minúsculas.
     *
     * @param nombre El nombre del motivo de ajuste a buscar.
     * @return {@code true} si ya existe un motivo de ajuste activo con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END FROM MotivoAjusteEntity ma WHERE UPPER(ma.nombre) = UPPER(:nombre) AND ma.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe un motivo de ajuste activo con el nombre dado, ignorando mayúsculas y
     * minúsculas, excluyendo de la búsqueda al motivo de ajuste con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual sin que la
     * validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre del motivo de ajuste a buscar.
     * @param id El ID del motivo de ajuste a excluir de la verificación.
     * @return {@code true} si otro motivo de ajuste activo ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(ma) > 0 THEN true ELSE false END FROM MotivoAjusteEntity ma WHERE UPPER(ma.nombre) = UPPER(:nombre) AND ma.id <> :id AND ma.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
