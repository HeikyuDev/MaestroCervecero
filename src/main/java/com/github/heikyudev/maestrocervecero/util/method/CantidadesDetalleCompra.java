package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;

import java.util.List;

/**
 * Clase de utilidades con el cálculo de cantidades recibidas y pendientes de un ítem de una orden de compra.
 * <p>
 * Es la única definición de ese cálculo: la usan tanto el módulo de ingreso de insumos, para validar y cerrar la
 * orden, como el detalle de la orden de compra, para mostrarlo. Las cantidades se devuelven sin redondear; quien
 * las compara o las muestra las redondea con {@link com.github.heikyudev.maestrocervecero.util.validacion.Redondeo}.
 * </p>
 */
public final class CantidadesDetalleCompra {

    private CantidadesDetalleCompra() {
    }

    /**
     * Suma las cantidades recibidas de una lista de ingresos de insumo.
     *
     * @param ingresosRegistrados Ingresos de insumo en estado {@code REGISTRADO} de un ítem de compra.
     * @return Cantidad total recibida, en la unidad de medida base del insumo.
     */
    public static double sumarCantidadRecibida(List<IngresoInsumoEntity> ingresosRegistrados) {
        return ingresosRegistrados.stream()
                .mapToDouble(IngresoInsumoEntity::getCantidadRecibida)
                .sum();
    }

    /**
     * Calcula la cantidad pendiente de entrega de un ítem de compra.
     * <p>
     * {@code detalleCompraEntity.getCantidad()} es la cantidad de unidades de la presentación
     * comercial pedidas (ej. 5 bolsas), no la cantidad de insumo — por eso hay que multiplicarla
     * por el contenido de cada unidad ({@code presentacionComercial.getCantidad()}), convertido
     * primero a la unidad del insumo por si la presentación fue cargada en una unidad distinta
     * (ej. presentación en GRAMO para un insumo que trackea en KILOGRAMO).
     * </p>
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @param cantidadYaRecibida Cantidad ya recibida del ítem, en la unidad de medida base del insumo.
     * @return Cantidad pendiente de entrega, en la unidad de medida base del insumo.
     */
    public static double calcularCantidadPendiente(DetalleCompraEntity detalleCompraEntity, double cantidadYaRecibida) {
        PresentacionComercialEntity presentacionComercial = detalleCompraEntity.getCatalogoProveedor().getPresentacionComercial();
        InsumoEntity insumo = detalleCompraEntity.getCatalogoProveedor().getInsumo();
        double cantidadPorUnidadEnUnidadDelInsumo = insumo.getUnidadDeMedida()
                .desdeGramos(presentacionComercial.getUnidadDeMedida().aGramos(presentacionComercial.getCantidad()));
        double cantidadTotalSolicitada = cantidadPorUnidadEnUnidadDelInsumo * detalleCompraEntity.getCantidad();

        return cantidadTotalSolicitada - cantidadYaRecibida;
    }
}
