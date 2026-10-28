package com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

/**
 * Interfaz que define los servicios de consulta relacionados con los lotes de insumo físicos.
 */
public interface ILoteInsumoServicio {

    /**
     * Busca los lotes de insumo de un insumo determinado que todavía tienen cantidad disponible,
     * para que el operario elija entre ellos al registrar un consumo directo.
     * <p>
     * Ordenados por fecha de vencimiento ascendente (FEFO), el mismo criterio que usa la reserva
     * automática al iniciar un lote.
     * </p>
     *
     * @param idInsumo El ID del insumo, elegido por el usuario entre los devueltos por
     *                 {@code filtrarInsumosRequeridos}.
     * @return Los lotes de insumo de ese insumo con cantidad disponible mayor a cero, en formato DTO.
     */
    List<LoteInsumoResponseDTO> filtrarLotesInsumoDisponibles(Long idInsumo);

    /**
     * Filtra, paginados, los lotes de insumo con saldo disponible mayor a 0 de los insumos indicados (los de la etapa),
     * para elegir el lote de un consumo directo. Cada lote informa si está vencido (vence antes de hoy) y su fecha de
     * vencimiento.
     * <p>
     * El orden es FEFO y lo da la consulta: primero los vigentes por fecha de vencimiento ascendente (el que vence hoy
     * todavía es vigente), luego los que no tienen fecha de vencimiento y al final los vencidos, también por fecha; ante
     * igual fecha, por ID. El orden del {@code pageable} se ignora. El nombre del insumo y la identificación se buscan
     * por coincidencia parcial, sin distinguir mayúsculas, y un texto en blanco equivale a no filtrar.
     * </p>
     *
     * @param idsInsumo Los IDs de los insumos de la etapa; si es nulo o vacío no hay lotes que ofrecer.
     * @param nombreInsumo Texto a buscar dentro del nombre del insumo, o {@code null} para no filtrar por él.
     * @param identificacion Texto a buscar dentro de la identificación del lote del proveedor, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación (solo se usa el tamaño y el número de página).
     * @return Una página de lotes de insumo.
     */
    Page<LoteInsumoResponseDTO> filtrarLotesInsumoDisponibles(Collection<Long> idsInsumo, String nombreInsumo, String identificacion, Pageable pageable);

    /**
     * Filtra, paginados, los lotes de un insumo sin importar su saldo (también los agotados), para elegir el lote existente
     * al registrar un ingreso. Cada lote informa su vencimiento, si está vencido y su cantidad actual.
     * <p>
     * El orden lo da la consulta y es por fecha de vencimiento ascendente y, ante igual fecha, por ID; el orden del
     * {@code pageable} se ignora. La identificación se busca por coincidencia parcial, sin distinguir mayúsculas, y un
     * texto en blanco equivale a no filtrar.
     * </p>
     *
     * @param idInsumo El ID del insumo; si es nulo no hay lotes que ofrecer.
     * @param identificacion Texto a buscar dentro de la identificación del lote del proveedor, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación (solo se usa el tamaño y el número de página).
     * @return Una página de lotes de insumo.
     */
    Page<LoteInsumoResponseDTO> filtrarLotesInsumoPorInsumo(Long idInsumo, String identificacion, Pageable pageable);

    /**
     * Busca un lote de insumo por su ID.
     *
     * @param id El ID del lote de insumo.
     * @return El lote de insumo.
     * @throws RecursoNoEncontradoException Si no existe un lote de insumo con el ID especificado.
     */
    LoteInsumoResponseDTO buscarPorId(Long id);
}
