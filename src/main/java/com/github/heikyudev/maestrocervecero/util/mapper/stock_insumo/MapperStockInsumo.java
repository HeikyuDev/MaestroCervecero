package com.github.heikyudev.maestrocervecero.util.mapper.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.CantidadesStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;

import java.time.LocalDate;
import java.util.Collection;

/**
 * MapperStockInsumo tiene la responsabilidad de calcular las cantidades de stock (disponible, reservada, vencida y
 * total) de un lote de insumo y de un insumo, y de mapear los resultados a los DTO de stock de insumos.
 * <p>
 * Las reglas, con "hoy" recibido como parámetro para que el cálculo sea determinista:
 * </p>
 * <ul>
 *     <li>Un lote está vencido si su fecha de vencimiento es estrictamente anterior a hoy; el que vence hoy es vigente.</li>
 *     <li>Lote vigente: disponible = actual - reservada, reservada = reservada, vencida = 0, total = actual.</li>
 *     <li>Lote vencido: disponible = 0, reservada = 0, vencida = actual (incluye lo que estaba reservado), total = actual.</li>
 *     <li>Insumo: la suma, cantidad por cantidad, de las de sus lotes.</li>
 * </ul>
 * <p>
 * En ambos casos se cumple {@code disponible + reservada + vencida = total}. Todas las cantidades se redondean con
 * {@link Redondeo}.
 * </p>
 */
public class MapperStockInsumo {

    /**
     * Indica si un lote está vencido a una fecha dada.
     *
     * @param fechaVencimiento Fecha de vencimiento del lote.
     * @param hoy Fecha de referencia.
     * @return {@code true} si la fecha de vencimiento es estrictamente anterior a {@code hoy}.
     */
    public static boolean estaVencido(LocalDate fechaVencimiento, LocalDate hoy) {
        return fechaVencimiento.isBefore(hoy);
    }

    /**
     * Calcula las cantidades de stock de un lote de insumo a una fecha dada.
     *
     * @param cantidadActual Cantidad física actual del lote ({@code null} equivale a 0).
     * @param cantidadReservada Cantidad reservada del lote ({@code null} equivale a 0).
     * @param fechaVencimiento Fecha de vencimiento del lote.
     * @param hoy Fecha de referencia para decidir si el lote está vencido.
     * @return Las cantidades disponible, reservada, vencida y total del lote.
     */
    public static CantidadesStockResponseDTO calcularCantidades(Double cantidadActual, Double cantidadReservada,
                                                                 LocalDate fechaVencimiento, LocalDate hoy) {
        double actual = Redondeo.redondear(cantidadActual != null ? cantidadActual : 0.0);
        double reservada = Redondeo.redondear(cantidadReservada != null ? cantidadReservada : 0.0);

        if (estaVencido(fechaVencimiento, hoy)) {
            return cantidades(0.0, 0.0, actual, actual);
        }
        return cantidades(Redondeo.redondear(actual - reservada), reservada, 0.0, actual);
    }

    /**
     * Calcula las cantidades de stock de un lote de insumo a una fecha dada.
     *
     * @param loteInsumoEntity Entidad del lote de insumo.
     * @param hoy Fecha de referencia para decidir si el lote está vencido.
     * @return Las cantidades disponible, reservada, vencida y total del lote.
     */
    public static CantidadesStockResponseDTO calcularCantidades(LoteInsumoEntity loteInsumoEntity, LocalDate hoy) {
        return calcularCantidades(loteInsumoEntity.getCantidadActual(), loteInsumoEntity.getCantidadReservada(),
                loteInsumoEntity.getFechaVencimiento(), hoy);
    }

    /**
     * Suma, cantidad por cantidad, las cantidades de varios lotes para obtener las de su insumo.
     *
     * @param cantidadesPorLote Las cantidades de cada lote; {@code null} o vacía da todo en 0.
     * @return Las cantidades del insumo.
     */
    public static CantidadesStockResponseDTO sumar(Collection<CantidadesStockResponseDTO> cantidadesPorLote) {
        double disponible = 0.0;
        double reservada = 0.0;
        double vencida = 0.0;
        double total = 0.0;

        if (cantidadesPorLote != null) {
            for (CantidadesStockResponseDTO cantidades : cantidadesPorLote) {
                disponible += cantidades.getDisponible();
                reservada += cantidades.getReservada();
                vencida += cantidades.getVencida();
                total += cantidades.getTotal();
            }
        }
        return cantidades(Redondeo.redondear(disponible), Redondeo.redondear(reservada),
                Redondeo.redondear(vencida), Redondeo.redondear(total));
    }

    /**
     * Arma las cantidades de un insumo a partir de las sumas que calculó la base de datos sobre sus lotes.
     *
     * @param disponible Suma de lo disponible de los lotes vigentes.
     * @param reservada Suma de lo reservado de los lotes vigentes.
     * @param vencida Suma de la cantidad actual de los lotes vencidos.
     * @param total Suma de la cantidad actual de todos los lotes.
     * @return Las cantidades del insumo, redondeadas.
     */
    public static CantidadesStockResponseDTO desdeSumas(double disponible, double reservada, double vencida, double total) {
        return cantidades(Redondeo.redondear(disponible), Redondeo.redondear(reservada),
                Redondeo.redondear(vencida), Redondeo.redondear(total));
    }

    /**
     * Mapea un insumo y sus cantidades ya calculadas a {@link StockInsumoResponseDTO}.
     *
     * @param insumoEntity Entidad del insumo (Malta, Lúpulo o Levadura).
     * @param cantidades Cantidades del insumo, sumadas de sus lotes.
     * @return Objeto DTO correspondiente o {@code null} si el insumo es nulo.
     */
    public static StockInsumoResponseDTO toDTO(InsumoEntity insumoEntity, CantidadesStockResponseDTO cantidades) {
        if (insumoEntity == null) {
            return null;
        }

        return StockInsumoResponseDTO.builder()
                .id(insumoEntity.getId())
                .nombre(insumoEntity.getNombre())
                .tipo(tipoDe(insumoEntity))
                .unidadDeMedida(insumoEntity.getUnidadDeMedida())
                .cantidades(cantidades)
                .build();
    }

    /**
     * Mapea un lote de insumo a la fila {@link StockLoteInsumoResponseDTO}, calculando sus cantidades.
     *
     * @param loteInsumoEntity Entidad del lote de insumo.
     * @param hoy Fecha de referencia para decidir si el lote está vencido.
     * @return Objeto DTO correspondiente o {@code null} si el lote es nulo.
     */
    public static StockLoteInsumoResponseDTO toDTO(LoteInsumoEntity loteInsumoEntity, LocalDate hoy) {
        if (loteInsumoEntity == null) {
            return null;
        }

        return StockLoteInsumoResponseDTO.builder()
                .id(loteInsumoEntity.getId())
                .identificacionLoteProveedor(loteInsumoEntity.getIdentificacionLoteProveedor())
                .fechaVencimiento(loteInsumoEntity.getFechaVencimiento())
                .vencido(estaVencido(loteInsumoEntity.getFechaVencimiento(), hoy))
                .cantidades(calcularCantidades(loteInsumoEntity, hoy))
                .build();
    }

    /**
     * Mapea un lote de insumo a la cabecera {@link StockLoteInsumoDetalleResponseDTO}, con los datos de su insumo y
     * sus cantidades calculadas.
     *
     * @param loteInsumoEntity Entidad del lote de insumo.
     * @param hoy Fecha de referencia para decidir si el lote está vencido.
     * @return Objeto DTO correspondiente o {@code null} si el lote es nulo.
     */
    public static StockLoteInsumoDetalleResponseDTO toDetalleDTO(LoteInsumoEntity loteInsumoEntity, LocalDate hoy) {
        if (loteInsumoEntity == null) {
            return null;
        }

        return StockLoteInsumoDetalleResponseDTO.builder()
                .id(loteInsumoEntity.getId())
                .idInsumo(loteInsumoEntity.getInsumo().getId())
                .nombreInsumo(loteInsumoEntity.getInsumo().getNombre())
                .unidadDeMedida(loteInsumoEntity.getInsumo().getUnidadDeMedida())
                .identificacionLoteProveedor(loteInsumoEntity.getIdentificacionLoteProveedor())
                .fechaVencimiento(loteInsumoEntity.getFechaVencimiento())
                .vencido(estaVencido(loteInsumoEntity.getFechaVencimiento(), hoy))
                .cantidades(calcularCantidades(loteInsumoEntity, hoy))
                .build();
    }

    /**
     * Resuelve el tipo concreto de un insumo.
     *
     * @param insumoEntity Entidad del insumo.
     * @return El {@link TipoInsumo} correspondiente a su clase concreta.
     * @throws IllegalArgumentException Si la clase concreta no es Malta, Lúpulo ni Levadura.
     */
    public static TipoInsumo tipoDe(InsumoEntity insumoEntity) {
        if (insumoEntity instanceof MaltaEntity) {
            return TipoInsumo.MALTA;
        }
        if (insumoEntity instanceof LupuloEntity) {
            return TipoInsumo.LUPULO;
        }
        if (insumoEntity instanceof LevaduraEntity) {
            return TipoInsumo.LEVADURA;
        }
        throw new IllegalArgumentException("Tipo de insumo no soportado: " + insumoEntity.getClass().getName());
    }

    private static CantidadesStockResponseDTO cantidades(double disponible, double reservada, double vencida, double total) {
        return CantidadesStockResponseDTO.builder()
                .disponible(disponible)
                .reservada(reservada)
                .vencida(vencida)
                .total(total)
                .build();
    }
}
