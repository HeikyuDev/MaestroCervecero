package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import org.springframework.data.domain.Pageable;

/**
 * Estadísticas de compras. Son consultas de solo lectura: ningún método modifica datos. Cada reporte valida el período
 * con {@link PeriodoEstadistica#validar} y devuelve la estructura única {@link ReporteEstadisticoResponseDTO}. La fecha
 * de emisión de una orden de compra es su fecha de alta, y su monto es la suma de cantidad por costo unitario de sus
 * ítems.
 * <p>
 * Precondiciones de todos los métodos: el período de los criterios tiene sus dos extremos, la fecha hasta no es
 * anterior a la fecha desde y el período no supera el rango máximo de las estadísticas.
 * </p>
 */
public interface IEstadisticaComprasServicio {

    /**
     * Genera el reporte "Compras": cuánto se compró y a qué proveedores. El universo son las órdenes de compra emitidas
     * en el período, en cualquier estado.
     * <p>
     * Postcondiciones: devuelve los criterios Proveedor y Estado; cuatro indicadores: monto comprado (de las órdenes no
     * anuladas), órdenes emitidas, monto promedio por orden no anulada y órdenes anuladas (porcentaje de las emitidas);
     * el gráfico del monto por proveedor (los ocho mayores más "Otros"); y la página pedida de la tabla (orden, fecha de
     * emisión, proveedor, estado, ítems y monto), de la más reciente a la más antigua, con el total sin anuladas.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeCompras(CriteriosReporteCompras criterios, Pageable pageable);

    /**
     * Genera el reporte "Proveedores": qué proveedores entregan lo pedido y en fecha. El universo son las órdenes no
     * anuladas emitidas en el período, agrupadas por proveedor; con un tipo de insumo, solo cuentan las órdenes con algún
     * ítem de ese tipo y, de ellas, esos ítems.
     * <p>
     * Postcondiciones: una orden está completa si de cada ítem se recibió (ingresos registrados, convertidos a
     * presentaciones) al menos lo pedido; está a tiempo si además su último ingreso es de una fecha no posterior a su
     * fecha de entrega estimada; su plazo de entrega son los días desde la emisión hasta el último ingreso. Devuelve los
     * criterios Proveedor y Tipo de insumo; cuatro indicadores: entregas a tiempo (porcentaje de las órdenes completas),
     * plazo promedio de entrega (de las órdenes completas), recibido sobre lo pedido (presentaciones recibidas, sin pasar
     * de lo pedido por ítem, sobre las pedidas) y cierres manuales (órdenes finalizadas a mano, con motivo); el gráfico
     * del porcentaje de entregas a tiempo por proveedor (los ocho con más órdenes completas); y la página pedida del
     * ranking por monto (proveedor, órdenes, monto, a tiempo, plazo promedio y recibido), con la fila de totales. Los
     * porcentajes y promedios sin órdenes sobre las que calcularse se muestran con un guion.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeProveedores(CriteriosReporteProveedores criterios, Pageable pageable);

    /**
     * Genera el reporte "Precios": cómo variaron los precios de los insumos comprados. El universo son los ítems de las
     * órdenes no anuladas emitidas en el período.
     * <p>
     * Postcondiciones: el precio de un ítem es por unidad de medida del insumo (costo de la presentación sobre la
     * cantidad de insumo que trae); el promedio se pondera por la cantidad comprada; la variación de un insumo es su
     * último precio contra el primero, por fecha de emisión (un guion si tuvo una sola compra). Devuelve los criterios
     * Tipo de insumo y Proveedor; cuatro indicadores: insumos comprados, insumos con suba, mayor suba y mayor baja (con
     * el insumo en la aclaración); el gráfico de la variación por insumo (las ocho mayores en valor absoluto, con signo);
     * y la página pedida de la tabla en orden alfabético (insumo con su unidad, precio mínimo, máximo y promedio, primer y
     * último precio y variación).
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDePrecios(CriteriosReportePrecios criterios, Pageable pageable);

    /**
     * Genera el reporte "Recepción": qué se recibió en el período y qué falta recibir. El universo son los ingresos
     * registrados con fecha de ingreso en el período; lo pendiente son las órdenes {@code PENDIENTE} de hoy.
     * <p>
     * Postcondiciones: el valor de un ingreso es su cantidad recibida por su costo unitario. Devuelve los criterios
     * Proveedor y Tipo de ingreso; cuatro indicadores: ingresos registrados, valor recibido, órdenes con saldo pendiente
     * y órdenes con entrega vencida (pendientes con entrega estimada anterior a hoy; estos dos, del proveedor indicado y
     * sin depender del período); el gráfico del valor recibido por proveedor (los ocho mayores más "Otros"; los ingresos
     * directos como "Sin orden de compra"); y la página pedida de la tabla (fecha, insumo, cantidad con su unidad, valor,
     * tipo y "Ver" la orden), del ingreso más reciente al más antiguo, con el total del valor.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeRecepcion(CriteriosReporteRecepcion criterios, Pageable pageable);
}
