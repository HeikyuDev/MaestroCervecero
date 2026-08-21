package com.github.heikyudev.maestrocervecero.persistence.repository.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los ítems de catálogo de proveedor ({@link CatalogoProveedorEntity}).
 * <p>
 * {@link CatalogoProveedorEntity} no tiene un CRUD propio: su ciclo de vida se gestiona
 * íntegramente desde {@code ProveedorServicioImpl}, en cascada desde {@code ProveedorEntity}.
 * Tampoco se elimina físicamente: sacar un ítem del catálogo pone {@code seleccionado = false}
 * sobre la misma fila, para preservar la referencia de las {@code DetalleCompraEntity}
 * históricas. Este repositorio existe únicamente como infraestructura interna; no se expone a
 * través de ninguna interfaz de servicio propia.
 * </p>
 */
@Repository
public interface ICatalogoProveedorRepository extends JpaRepository<CatalogoProveedorEntity, Long> {

    /**
     * Busca un ítem de catálogo seleccionado por su ID.
     *
     * @param id El ID del ítem de catálogo a buscar.
     * @return Un Optional que contiene el ítem si está seleccionado, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT cp FROM CatalogoProveedorEntity cp WHERE cp.id = :id AND cp.seleccionado = true")
    Optional<CatalogoProveedorEntity> findById(@Param("id") Long id);

    /**
     * Verifica si existe algún ítem de catálogo seleccionado asociado a la presentación
     * comercial indicada, perteneciente a un proveedor activo.
     * <p>
     * Se utiliza para impedir la baja de una presentación comercial que todavía es ofrecida
     * por al menos un proveedor activo.
     * </p>
     *
     * @param idPresentacionComercial El ID de la presentación comercial a verificar.
     * @return {@code true} si existe al menos un ítem de catálogo seleccionado asociado a esa presentación comercial, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END FROM CatalogoProveedorEntity cp WHERE cp.presentacionComercial.id = :idPresentacionComercial AND cp.seleccionado = true AND cp.proveedor.estado = 'ACTIVO'")
    boolean existsByPresentacionComercialId(@Param("idPresentacionComercial") Long idPresentacionComercial);
}
