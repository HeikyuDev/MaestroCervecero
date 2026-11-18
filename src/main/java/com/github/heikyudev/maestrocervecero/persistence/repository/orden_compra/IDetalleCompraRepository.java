package com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los ítems de detalle de una orden de compra ({@link DetalleCompraEntity}).
 * <p>
 * Al igual que la orden de compra a la que pertenece, un detalle de compra es un registro
 * transaccional inmutable: no admite baja lógica.
 * </p>
 */
@Repository
public interface IDetalleCompraRepository extends JpaRepository<DetalleCompraEntity, Long> {

    /**
     * Busca, bloqueándolo para escritura, un ítem de detalle de compra por su ID.
     * <p>
     * Se usa al registrar un ingreso de insumo por compra, para evitar que dos ingresos
     * concurrentes sobre el mismo ítem lean la misma cantidad pendiente de entrega antes de que
     * ninguno de los dos persista el suyo, y ambos pasen la validación de cantidad — lo que
     * terminaría recibiendo, en conjunto, más de lo que el ítem tiene solicitado.
     * </p>
     *
     * @param id El ID del ítem de detalle de compra.
     * @return Un Optional que contiene el ítem si existe, o vacío en caso contrario.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DetalleCompraEntity d WHERE d.id = :id")
    Optional<DetalleCompraEntity> buscarPorIdParaIngresar(@Param("id") Long id);

    /**
     * Carga, en una sola consulta, los ítems de las órdenes de compra indicadas con su ítem de catálogo, la presentación
     * comercial y el insumo, que hacen falta para convertir las cantidades recibidas a presentaciones.
     *
     * @param idsOrdenes Los IDs de las órdenes de compra (no vacío).
     * @return Los ítems de esas órdenes con sus relaciones cargadas.
     */
    @Query("SELECT d FROM DetalleCompraEntity d JOIN FETCH d.catalogoProveedor c JOIN FETCH c.presentacionComercial JOIN FETCH c.insumo "
            + "WHERE d.ordenCompra.id IN :idsOrdenes")
    java.util.List<DetalleCompraEntity> buscarConPresentacionDeOrdenes(@Param("idsOrdenes") java.util.Collection<Long> idsOrdenes);

    /**
     * Carga los ítems de las órdenes de compra PENDIENTE cuyo insumo es alguno de los indicados, con su ítem de
     * catálogo, la presentación comercial y el insumo, que hacen falta para calcular cuánto insumo resta recibir.
     *
     * @param idsInsumo Los IDs de los insumos (no vacío).
     * @return Los ítems de las órdenes pendientes de esos insumos.
     */
    @Query("SELECT d FROM DetalleCompraEntity d JOIN FETCH d.catalogoProveedor c JOIN FETCH c.presentacionComercial JOIN FETCH c.insumo i "
            + "WHERE d.ordenCompra.estado = 'PENDIENTE' AND i.id IN :idsInsumo")
    java.util.List<DetalleCompraEntity> buscarDeOrdenesPendientesPorInsumos(@Param("idsInsumo") java.util.Collection<Long> idsInsumo);
}
