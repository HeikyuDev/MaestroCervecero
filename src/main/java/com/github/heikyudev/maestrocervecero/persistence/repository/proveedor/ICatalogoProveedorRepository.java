package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los ítems de catálogo de proveedor ({@link CatalogoProveedorEntity}).
 * <p>
 * {@link CatalogoProveedorEntity} no tiene un CRUD propio: su ciclo de vida se gestiona
 * íntegramente desde {@code ProveedorServicioImpl}. Este repositorio existe únicamente como
 * infraestructura interna para poder dar de baja lógica (soft delete) los ítems reemplazados
 * al modificar el catálogo de un proveedor; no se expone a través de ninguna interfaz de
 * servicio propia.
 * </p>
 */
@Repository
public interface ICatalogoProveedorRepository extends JpaRepository<CatalogoProveedorEntity, Long> {

    /**
     * Verifica si existe algún ítem de catálogo activo asociado a la presentación comercial
     * indicada.
     * <p>
     * Se utiliza para impedir la baja de una presentación comercial que todavía es ofrecida
     * por al menos un proveedor.
     * </p>
     *
     * @param idPresentacionComercial El ID de la presentación comercial a verificar.
     * @return {@code true} si existe al menos un ítem de catálogo activo asociado a esa presentación comercial, {@code false} en caso contrario.
     */
    boolean existsByPresentacionComercialId(Long idPresentacionComercial);
}
