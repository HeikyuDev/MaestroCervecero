package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los ítems de catálogo de proveedor ({@link CatalogoProveedorEntity}).
 */
@Repository
public interface ICatalogoProveedorRepository extends JpaRepository<CatalogoProveedorEntity, Long> {

    /**
     * Verifica si existe algún ítem de catálogo, perteneciente a la versión vigente de un
     * proveedor activo, asociado a la presentación comercial indicada.
     * <p>
     * Se utiliza para impedir la baja de una presentación comercial que todavía está referenciada
     * por el catálogo de algún proveedor. El catálogo está scopeado a una versión puntual del
     * proveedor (ver {@link com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity}),
     * así que un ítem que solo existe en una versión anterior (ya reemplazada) no cuenta: esa
     * referencia es histórica, no vigente.
     * </p>
     *
     * @param presentacionComercialId El ID de la presentación comercial a verificar.
     * @return {@code true} si existe al menos un ítem de catálogo vigente asociado a esa presentación comercial, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CatalogoProveedorEntity c "
            + "WHERE c.presentacionComercial.id = :presentacionComercialId "
            + "AND c.version.esUltimaVersion = true AND c.version.proveedor.estado = 'ACTIVO'")
    boolean existsByPresentacionComercialId(@Param("presentacionComercialId") Long presentacionComercialId);
}
