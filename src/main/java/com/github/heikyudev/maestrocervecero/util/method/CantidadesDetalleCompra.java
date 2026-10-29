package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;

import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        double cantidadTotalSolicitada = calcularCantidadPorPresentacion(detalleCompraEntity) * detalleCompraEntity.getCantidad();

        return cantidadTotalSolicitada - cantidadYaRecibida;
    }

    /**
     * Calcula cuánto insumo trae cada unidad de la presentación comercial de un ítem, expresado en la unidad de
     * medida del insumo (ej. una bolsa de 25000 g de un insumo que se trackea en KILOGRAMO trae 25).
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @return Cantidad de insumo por unidad de presentación, en la unidad de medida base del insumo.
     */
    public static double calcularCantidadPorPresentacion(DetalleCompraEntity detalleCompraEntity) {
        PresentacionComercialEntity presentacionComercial = detalleCompraEntity.getCatalogoProveedor().getPresentacionComercial();
        InsumoEntity insumo = detalleCompraEntity.getCatalogoProveedor().getInsumo();
        return insumo.getUnidadDeMedida()
                .desdeGramos(presentacionComercial.getUnidadDeMedida().aGramos(presentacionComercial.getCantidad()));
    }

    /**
     * Convierte una cantidad de unidades del ítem a la unidad de medida del insumo: el número de unidades
     * por el contenido de cada una, redondeado a la escala de las cantidades (ej. 2 bolsas de 25 kg de un insumo en
     * kg son 50 kg).
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @param unidades Cantidad de unidades.
     * @return Cantidad de insumo equivalente, en la unidad de medida base del insumo.
     */
    public static double convertirPresentacionesAUnidadDelInsumo(DetalleCompraEntity detalleCompraEntity, int unidades) {
        return Redondeo.redondear(unidades * calcularCantidadPorPresentacion(detalleCompraEntity));
    }

    /**
     * Convierte una cantidad de insumo a unidades del ítem: la cantidad dividida por el contenido de cada
     * presentación, redondeada a la escala de las cantidades para no arrastrar el residuo de la coma flotante.
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @param cantidadEnUnidadDelInsumo Cantidad de insumo, en la unidad de medida base del insumo.
     * @return Cantidad equivalente en unidades.
     * @throws IllegalStateException Si la presentación no trae una cantidad de insumo mayor a cero.
     */
    public static double convertirAPresentaciones(DetalleCompraEntity detalleCompraEntity, double cantidadEnUnidadDelInsumo) {
        double cantidadPorPresentacion = calcularCantidadPorPresentacion(detalleCompraEntity);
        if (!(cantidadPorPresentacion > 0)) {
            throw new IllegalStateException("La presentación comercial del ítem no trae una cantidad de insumo mayor a cero");
        }
        return Redondeo.redondear(cantidadEnUnidadDelInsumo / cantidadPorPresentacion);
    }

    /**
     * Calcula las unidades que restan recibir de un ítem: las pedidas menos las ya recibidas, estas últimas
     * convertidas desde la unidad del insumo. Es la misma cantidad pendiente que {@link #calcularCantidadPendiente},
     * expresada en unidades y redondeada a la escala de las cantidades.
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @param cantidadYaRecibida Cantidad ya recibida del ítem, en la unidad de medida base del insumo.
     * @return Unidades pendientes de entrega.
     */
    public static double calcularPresentacionesPendientes(DetalleCompraEntity detalleCompraEntity, double cantidadYaRecibida) {
        return Redondeo.redondear(detalleCompraEntity.getCantidad() - convertirAPresentaciones(detalleCompraEntity, cantidadYaRecibida));
    }

    /**
     * Calcula el costo por unidad de medida del insumo de un ítem de compra.
     * <p>
     * El costo unitario del ítem es por unidad de presentación (el subtotal de la orden es cantidad de unidades
     * por costo unitario); el ingreso y el costo promedio del lote son por unidad del insumo. Se divide el costo de la
     * presentación por la cantidad de insumo que trae, con 4 decimales y redondeo {@code HALF_UP}.
     * </p>
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @return Costo por unidad de medida del insumo, con 4 decimales.
     * @throws IllegalStateException Si la presentación no trae una cantidad de insumo mayor a cero.
     */
    public static BigDecimal calcularCostoPorUnidadDelInsumo(DetalleCompraEntity detalleCompraEntity) {
        double cantidadPorPresentacion = calcularCantidadPorPresentacion(detalleCompraEntity);
        if (!(cantidadPorPresentacion > 0)) {
            throw new IllegalStateException("La presentación comercial del ítem no trae una cantidad de insumo mayor a cero");
        }
        return detalleCompraEntity.getCostoUnitario()
                .divide(BigDecimal.valueOf(cantidadPorPresentacion), 4, RoundingMode.HALF_UP);
    }
}
