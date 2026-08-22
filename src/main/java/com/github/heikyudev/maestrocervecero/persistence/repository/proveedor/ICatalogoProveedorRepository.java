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
     * Verifica si existe algún ítem de catálogo de proveedor asociado a la presentación
     * comercial indicada.
     * <p>
     * Se utiliza para impedir la baja de una presentación comercial que todavía está referenciada
     * por el catálogo de algún proveedor.
     * </p>
     *
     * @param presentacionComercialId El ID de la presentación comercial a verificar.
     * @return {@code true} si existe al menos un ítem de catálogo asociado a esa presentación comercial, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CatalogoProveedorEntity c WHERE c.presentacionComercial.id = :presentacionComercialId")
    boolean existsByPresentacionComercialId(@Param("presentacionComercialId") Long presentacionComercialId);
}
