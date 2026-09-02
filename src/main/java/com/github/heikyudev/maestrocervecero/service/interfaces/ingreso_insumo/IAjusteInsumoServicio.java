package com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularAjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.AjusteInsumoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de ajustes de insumo.
 * <p>
 * A diferencia de otros módulos, no existe la baja lógica para este registro: un ajuste de
 * insumo solo puede pasar de {@code REGISTRADO} a {@code ANULADO}, y ambos estados permanecen
 * visibles en las búsquedas.
 * </p>
 */
public interface IAjusteInsumoServicio {

    /**
     * Obtiene una página de ajustes de insumo registrados en el sistema.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de ajustes de insumo en formato DTO.
     */
    Page<AjusteInsumoResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un ajuste de insumo por su ID.
     *
     * @param id El ID del ajuste de insumo.
     * @return El ajuste de insumo correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún ajuste de insumo con el ID especificado.
     */
    AjusteInsumoResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo ajuste de insumo.
     * <p>
     * Si el motivo de ajuste es de tipo INGRESO, la cantidad se suma a la cantidad disponible
     * del lote de insumo; si es de tipo EGRESO, se resta.
     * </p>
     *
     * @param ajusteInsumoFormDTO Los datos del ajuste a registrar.
     * @return El ajuste de insumo registrado.
     * @throws RecursoNoEncontradoException Si el lote de insumo o el motivo de ajuste referenciados no existen.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero, o si no hay suficiente cantidad disponible en el lote para un ajuste de tipo EGRESO.
     */
    AjusteInsumoResponseDTO registrarAjusteInsumo(AjusteInsumoFormDTO ajusteInsumoFormDTO);

    /**
     * Anula un ajuste de insumo existente en el sistema.
     *
     * @param id El ID del ajuste de insumo a anular.
     * @param anularAjusteInsumoFormDTO Los datos de la anulación (motivo).
     * @return El ajuste de insumo anulado.
     * @throws RecursoNoEncontradoException Si el ajuste de insumo con el ID especificado no existe.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el ajuste no se encuentra en estado {@code REGISTRADO}, o si revertir un ajuste de tipo INGRESO dejaría cantidad disponible negativa en el lote de insumo.
     */
    AjusteInsumoResponseDTO anularAjusteInsumo(Long id, AnularAjusteInsumoFormDTO anularAjusteInsumoFormDTO);
}
