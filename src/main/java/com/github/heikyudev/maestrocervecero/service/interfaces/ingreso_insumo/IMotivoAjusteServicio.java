package com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de motivos de ajuste.
 * <p>
 * Todas las operaciones actúan únicamente sobre motivos de ajuste activos: los registros con
 * baja lógica (soft delete) no son listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IMotivoAjusteServicio {

    /**
     * Filtra los motivos de ajuste activos, opcionalmente por nombre y/o tipo de ajuste.
     *
     * @param nombre Texto a buscar dentro del nombre del motivo de ajuste, o {@code null} para no filtrar por nombre.
     * @param tipoAjuste Tipo de ajuste exacto a filtrar (INGRESO/EGRESO), o {@code null} para no filtrar por tipo.
     * @param pageable La configuración de paginación.
     * @return Una página de motivos de ajuste activos en formato DTO que cumplen los criterios indicados.
     */
    Page<MotivoAjusteResponseDTO> filtrarMotivosAjuste(String nombre, TipoAjuste tipoAjuste, Pageable pageable);

    /**
     * Obtiene un motivo de ajuste activo por su ID.
     *
     * @param id El ID del motivo de ajuste.
     * @return El motivo de ajuste correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     */
    MotivoAjusteResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo motivo de ajuste.
     *
     * @param motivoAjusteFormDTO Los datos del motivo de ajuste a registrar.
     * @return El motivo de ajuste registrado.
     * @throws RecursoDuplicadoException Si ya existe un motivo de ajuste activo con el mismo nombre (case-insensitive).
     */
    MotivoAjusteResponseDTO altaMotivoAjuste(MotivoAjusteFormDTO motivoAjusteFormDTO);

    /**
     * Modifica un motivo de ajuste existente.
     *
     * @param id El ID del motivo de ajuste a modificar.
     * @param motivoAjusteFormDTO Los nuevos datos del motivo de ajuste.
     * @return El motivo de ajuste modificado.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro motivo de ajuste activo (case-insensitive).
     */
    MotivoAjusteResponseDTO modificarMotivoAjuste(Long id, MotivoAjusteFormDTO motivoAjusteFormDTO);

    /**
     * Elimina lógicamente un motivo de ajuste por su ID.
     *
     * @param id El ID del motivo de ajuste a eliminar.
     * @return El motivo de ajuste eliminado.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     */
    MotivoAjusteResponseDTO bajaMotivoAjuste(Long id);
}
