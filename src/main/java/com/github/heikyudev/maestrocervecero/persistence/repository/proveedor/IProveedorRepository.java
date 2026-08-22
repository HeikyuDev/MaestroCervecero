package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los proveedores ({@link ProveedorEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de proveedores dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}. Los
 * métodos heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se sobrescriben
 * con esa misma condición para que todo el código existente que ya los invoca (sin cambios) siga
 * viendo únicamente proveedores activos.
 * </p>
 */
@Repository
public interface IProveedorRepository extends JpaRepository<ProveedorEntity, Long> {

    /**
     * Busca un proveedor activo por su ID.
     *
     * @param id El ID del proveedor a buscar.
     * @return Un Optional que contiene el proveedor si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT p FROM ProveedorEntity p WHERE p.id = :id AND p.estado = 'ACTIVO'")
    Optional<ProveedorEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de proveedores activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de proveedores activos.
     */
    @Override
    @Query(value = "SELECT p FROM ProveedorEntity p WHERE p.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(p) FROM ProveedorEntity p WHERE p.estado = 'ACTIVO'")
    Page<ProveedorEntity> findAll(Pageable pageable);
}
