package com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos para la gestión de ingresos de insumo.
 * <p>
 * Un ingreso puede originarse de dos formas: como recepción de un ítem de una orden de compra
 * ({@link IngresoInsumoPorCompraFormDTO}) o de forma directa, sin orden previa
 * ({@link IngresoInsumoDirectoFormDTO}). No es un CRUD: no admite modificación, solo registro
 * inicial (en sus dos variantes) y anulación.
 * </p>
 */
public interface IIngresoInsumoServicio {

    /**
     * Obtiene una página de ingresos de insumo.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de ingresos de insumo en formato DTO.
     */
    Page<IngresoInsumoResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un ingreso de insumo por su ID.
     *
     * @param id El ID del ingreso de insumo.
     * @return El ingreso de insumo correspondiente al ID.
     */
    IngresoInsumoResponseDTO buscarPorId(Long id);

    /**
     * Registra un ingreso de insumo como recepción de un ítem de una orden de compra.
     *
     * @param ingresoInsumoPorCompraFormDTO Los datos del ingreso a registrar.
     * @return El ingreso de insumo registrado.
     */
    IngresoInsumoResponseDTO registrarIngresoInsumoPorCompra(IngresoInsumoPorCompraFormDTO ingresoInsumoPorCompraFormDTO);

    /**
     * Registra un ingreso de insumo sin una orden de compra previa (stock inicial, donación, etc.).
     *
     * @param ingresoInsumoDirectoFormDTO Los datos del ingreso a registrar.
     * @return El ingreso de insumo registrado.
     */
    IngresoInsumoResponseDTO registrarIngresoInsumoDirecto(IngresoInsumoDirectoFormDTO ingresoInsumoDirectoFormDTO);

    /**
     * Anula un ingreso de insumo existente.
     *
     * @param id El ID del ingreso de insumo a anular.
     * @param anularIngresoInsumoFormDTO Los datos de la anulación (motivo).
     * @return El ingreso de insumo anulado.
     */
    IngresoInsumoResponseDTO anularIngresoInsumo(Long id, AnularIngresoInsumoFormDTO anularIngresoInsumoFormDTO);
}
