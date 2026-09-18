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

    /**
     * Filtra los proveedores activos, opcionalmente por razón social, nombre comercial, CUIT y/o
     * localidad, contra la versión de cada proveedor marcada como {@code esUltimaVersion = true}
     * (la única que representa sus datos vigentes).
     *
     * @param razonSocial Texto a buscar dentro de la razón social, o {@code null} para no filtrar por ella.
     * @param nombreComercial Texto a buscar dentro del nombre comercial, o {@code null} para no filtrar por él.
     * @param cuit El CUIT exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idLocalidad El ID de la localidad a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de proveedores activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT p FROM ProveedorEntity p JOIN p.versiones v WHERE p.estado = 'ACTIVO' AND v.esUltimaVersion = true "
            + "AND (:razonSocial IS NULL OR UPPER(v.razonSocial) LIKE UPPER(CONCAT('%', :razonSocial, '%'))) "
            + "AND (:nombreComercial IS NULL OR UPPER(v.nombreComercial) LIKE UPPER(CONCAT('%', :nombreComercial, '%'))) "
            + "AND (:cuit IS NULL OR v.cuit = :cuit) "
            + "AND (:idLocalidad IS NULL OR v.localidad.id = :idLocalidad)",
            countQuery = "SELECT COUNT(p) FROM ProveedorEntity p JOIN p.versiones v WHERE p.estado = 'ACTIVO' AND v.esUltimaVersion = true "
                    + "AND (:razonSocial IS NULL OR UPPER(v.razonSocial) LIKE UPPER(CONCAT('%', :razonSocial, '%'))) "
                    + "AND (:nombreComercial IS NULL OR UPPER(v.nombreComercial) LIKE UPPER(CONCAT('%', :nombreComercial, '%'))) "
                    + "AND (:cuit IS NULL OR v.cuit = :cuit) "
                    + "AND (:idLocalidad IS NULL OR v.localidad.id = :idLocalidad)")
    Page<ProveedorEntity> filtrarProveedores(@Param("razonSocial") String razonSocial,
                                              @Param("nombreComercial") String nombreComercial,
                                              @Param("cuit") String cuit,
                                              @Param("idLocalidad") Long idLocalidad,
                                              Pageable pageable);
}
