package com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios de consulta del stock de insumos: listado de insumos con sus cantidades,
 * detalle de un insumo con sus lotes y detalle de un lote de insumo con sus reservas y consumos. Es de solo lectura.
 * <p>
 * Las cantidades (disponible, reservada, vencida y total) se calculan con las reglas de
 * {@code MapperStockInsumo} a la fecha de hoy, que el servicio toma una sola vez por consulta.
 * </p>
 * <p>
 * Consultas de repositorio que requiere la implementación y que hoy no existen:
 * </p>
 * <ul>
 *     <li>Insumos activos con sus cantidades agregadas en una sola consulta (sin N+1), agrupando los lotes por
 *     insumo con sumas condicionales según el vencimiento contra hoy, con filtro por nombre ({@code LIKE} con
 *     escape), por tipo y por situación de stock (condiciones sobre las sumas), ordenados por nombre.</li>
 *     <li>Todos los lotes de un insumo, con o sin stock y vencidos o no, en orden FEFO (fecha de vencimiento
 *     ascendente, luego ID), paginados.</li>
 *     <li>Reservas de insumo por ID de lote de insumo, con la última registrada primero, paginadas.</li>
 *     <li>Consumos de insumo por ID de lote de insumo, con el último registrado primero, paginados.</li>
 * </ul>
 */
public interface IStockInsumoServicio {

    /**
     * Filtra, paginados, los insumos activos con sus cantidades de stock. Un parámetro nulo no restringe por ese
     * criterio.
     * <p>
     * El orden es alfabético ascendente por nombre y lo da la consulta: el orden del {@code pageable} se ignora.
     * El nombre se busca por coincidencia parcial sin distinguir mayúsculas, y un texto en blanco equivale a no filtrar.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre del insumo, o {@code null} para no filtrar por él.
     * @param tipo Tipo concreto de insumo (Malta, Lúpulo o Levadura), o {@code null} para no filtrar por tipo.
     * @param situacionStock Situación de stock del insumo, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación (solo se usa el tamaño y el número de página).
     * @return Una página de insumos activos con sus cantidades.
     */
    Page<StockInsumoResponseDTO> filtrarInsumos(String nombre, TipoInsumo tipo, SituacionStockInsumo situacionStock, Pageable pageable);

    /**
     * Obtiene el resumen de stock de un insumo activo: sus cuatro cantidades, suma de las de todos sus lotes.
     *
     * @param idInsumo El ID del insumo.
     * @return El insumo con sus cantidades.
     * @throws RecursoNoEncontradoException Si no existe un insumo con ese ID o está dado de baja.
     */
    StockInsumoResponseDTO obtenerStockInsumo(Long idInsumo);

    /**
     * Filtra, paginados, todos los lotes de un insumo, incluidos los que no tienen stock y los vencidos.
     * <p>
     * El orden es FEFO y lo da la consulta: por fecha de vencimiento ascendente y, ante igual fecha, por ID.
     * El orden del {@code pageable} se ignora.
     * </p>
     *
     * @param idInsumo El ID del insumo.
     * @param pageable La configuración de paginación (solo se usa el tamaño y el número de página).
     * @return Una página de lotes del insumo con sus cantidades.
     * @throws RecursoNoEncontradoException Si no existe un insumo con ese ID o está dado de baja.
     */
    Page<StockLoteInsumoResponseDTO> filtrarLotesDeInsumo(Long idInsumo, Pageable pageable);

    /**
     * Obtiene la cabecera de un lote de insumo: su insumo, su identificación, su vencimiento y sus cantidades.
     *
     * @param idLoteInsumo El ID del lote de insumo.
     * @return El lote de insumo con sus cantidades.
     * @throws RecursoNoEncontradoException Si no existe un lote de insumo con ese ID o su insumo está dado de baja.
     */
    StockLoteInsumoDetalleResponseDTO obtenerDetalleLoteInsumo(Long idLoteInsumo);

    /**
     * Filtra, paginadas, las reservas hechas sobre un lote de insumo, en cualquier estado (activa, consumida o
     * liberada). Cada reserva informa su etapa y el lote de producción al que pertenece.
     * <p>
     * La última reserva registrada va primero (ID descendente). El orden del {@code pageable} se ignora.
     * </p>
     *
     * @param idLoteInsumo El ID del lote de insumo.
     * @param pageable La configuración de paginación (solo se usa el tamaño y el número de página).
     * @return Una página de reservas del lote de insumo.
     * @throws RecursoNoEncontradoException Si no existe un lote de insumo con ese ID o su insumo está dado de baja.
     */
    Page<ReservaInsumoResponseDTO> filtrarReservasDeLoteInsumo(Long idLoteInsumo, Pageable pageable);

    /**
     * Filtra, paginados, los consumos descontados de un lote de insumo, reservados y directos, registrados y
     * anulados. Cada consumo informa su etapa y el lote de producción al que pertenece.
     * <p>
     * El último consumo registrado va primero (ID descendente). El orden del {@code pageable} se ignora.
     * </p>
     *
     * @param idLoteInsumo El ID del lote de insumo.
     * @param pageable La configuración de paginación (solo se usa el tamaño y el número de página).
     * @return Una página de consumos del lote de insumo.
     * @throws RecursoNoEncontradoException Si no existe un lote de insumo con ese ID o su insumo está dado de baja.
     */
    Page<ConsumoInsumoResponseDTO> filtrarConsumosDeLoteInsumo(Long idLoteInsumo, Pageable pageable);
}
