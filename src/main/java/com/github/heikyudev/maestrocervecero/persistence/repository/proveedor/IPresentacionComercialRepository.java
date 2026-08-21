package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las presentaciones comerciales ({@link PresentacionComercialEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de presentaciones comerciales
 * dadas de baja se realiza explícitamente en cada consulta mediante la condición
 * {@code estado = 'ACTIVO'}. Los métodos heredados de {@link JpaRepository} ({@code findById},
 * {@code findAll}) se sobrescriben con esa misma condición para que todo el código existente
 * que ya los invoca (sin cambios) siga viendo únicamente presentaciones comerciales activas.
 * </p>
 */
@Repository
public interface IPresentacionComercialRepository extends JpaRepository<PresentacionComercialEntity, Long> {

    /**
     * Busca una presentación comercial activa por su ID.
     *
     * @param id El ID de la presentación comercial a buscar.
     * @return Un Optional que contiene la presentación comercial si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT pc FROM PresentacionComercialEntity pc WHERE pc.id = :id AND pc.estado = 'ACTIVO'")
    Optional<PresentacionComercialEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de presentaciones comerciales activas.
     * @param pageable La configuración de paginación.
     * @return Una página de presentaciones comerciales activas.
     */
    @Override
    @Query(value = "SELECT pc FROM PresentacionComercialEntity pc WHERE pc.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(pc) FROM PresentacionComercialEntity pc WHERE pc.estado = 'ACTIVO'")
    Page<PresentacionComercialEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe una presentación comercial activa con el nombre dado, ignorando
     * mayúsculas y minúsculas.
     *
     * @param nombre El nombre de la presentación comercial a buscar.
     * @return {@code true} si ya existe una presentación comercial activa con ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM PresentacionComercialEntity pc WHERE UPPER(pc.nombre) = UPPER(:nombre) AND pc.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    /**
     * Verifica si existe una presentación comercial activa con el nombre dado, ignorando
     * mayúsculas y minúsculas, excluyendo de la búsqueda a la presentación con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio nombre actual
     * sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param nombre El nombre de la presentación comercial a buscar.
     * @param id El ID de la presentación comercial a excluir de la verificación.
     * @return {@code true} si otra presentación comercial activa ya posee ese nombre, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(pc) > 0 THEN true ELSE false END FROM PresentacionComercialEntity pc WHERE UPPER(pc.nombre) = UPPER(:nombre) AND pc.id <> :id AND pc.estado = 'ACTIVO'")
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);
}
