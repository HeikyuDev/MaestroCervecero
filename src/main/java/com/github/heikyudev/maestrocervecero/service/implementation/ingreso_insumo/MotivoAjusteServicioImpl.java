package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IMotivoAjusteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperMotivoAjuste;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MotivoAjusteServicioImpl implements IMotivoAjusteServicio {

    private final IMotivoAjusteRepository motivoAjusteRepository;

    /**
     * Recupera una página de motivos de ajuste activos registrados en el sistema.
     * <p>
     * Los motivos de ajuste dados de baja son excluidos por la condición
     * {@code estado = 'ACTIVO'} aplicada en el repositorio.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MotivoAjusteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MotivoAjusteResponseDTO> buscarTodos(Pageable pageable) {
        return motivoAjusteRepository.findAll(pageable).map(MapperMotivoAjuste::toDTO);
    }

    /**
     * Busca y retorna un motivo de ajuste activo mediante su identificador único.
     *
     * @param id El ID del motivo de ajuste.
     * @return El motivo de ajuste correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún motivo de ajuste activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MotivoAjusteResponseDTO buscarPorId(Long id) {
        return MapperMotivoAjuste.toDTO(motivoAjusteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + id)));
    }

    /**
     * Registra un nuevo motivo de ajuste en el sistema.
     *
     * @param motivoAjusteFormDTO Los datos del motivo de ajuste a registrar.
     * @return El motivo de ajuste registrado.
     * @throws RecursoDuplicadoException Si ya existe un motivo de ajuste activo con el mismo nombre (case-insensitive).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MOTIVO_AJUSTE)
    public MotivoAjusteResponseDTO altaMotivoAjuste(MotivoAjusteFormDTO motivoAjusteFormDTO) {
        // 1. Validar que no exista otro motivo de ajuste con el mismo nombre (case-insensitive)
        if (motivoAjusteRepository.existsByNombreIgnoreCase(motivoAjusteFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe un motivo de ajuste con el nombre '" + motivoAjusteFormDTO.getNombre() + "'");
        }

        // 2. Construir y persistir la entidad, y retornar el DTO de respuesta correspondiente
        MotivoAjusteEntity motivoAjusteEntity = MotivoAjusteEntity.builder()
                .nombre(motivoAjusteFormDTO.getNombre())
                .tipoAjuste(motivoAjusteFormDTO.getTipoAjuste())
                .estado(Estado.ACTIVO)
                .build();

        return MapperMotivoAjuste.toDTO(motivoAjusteRepository.save(motivoAjusteEntity));
    }

    /**
     * Modifica un motivo de ajuste existente en el sistema.
     *
     * @param id El ID del motivo de ajuste a modificar.
     * @param motivoAjusteFormDTO Los nuevos datos del motivo de ajuste.
     * @return El motivo de ajuste modificado.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro motivo de ajuste activo (case-insensitive).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MOTIVO_AJUSTE)
    public MotivoAjusteResponseDTO modificarMotivoAjuste(Long id, MotivoAjusteFormDTO motivoAjusteFormDTO) {
        // 1. Localizar el motivo de ajuste existente. Si no existe, se dispara RecursoNoEncontradoException
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + id));

        // 2. Validar que no exista otro motivo de ajuste con el mismo nombre, excluyendo el propio ID
        if (motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot(motivoAjusteFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + motivoAjusteFormDTO.getNombre() + "' ya está en uso por otro motivo de ajuste");
        }

        // 3. Recién si todas las validaciones pasaron, aplicar los cambios sobre la entidad
        motivoAjusteEntity.setNombre(motivoAjusteFormDTO.getNombre());
        motivoAjusteEntity.setTipoAjuste(motivoAjusteFormDTO.getTipoAjuste());

        // 4. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperMotivoAjuste.toDTO(motivoAjusteRepository.save(motivoAjusteEntity));
    }

    /**
     * Procesa la baja lógica de un motivo de ajuste existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al motivo de ajuste con {@link Estado#BAJA} y
     * persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan de
     * encontrarlo.
     * </p>
     *
     * @param id El ID del motivo de ajuste a dar de baja.
     * @return El motivo de ajuste dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MOTIVO_AJUSTE)
    public MotivoAjusteResponseDTO bajaMotivoAjuste(Long id) {
        // 1. Localizar el motivo de ajuste. Si no existe, se dispara RecursoNoEncontradoException
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + id));

        // 2. Ejecutar la baja lógica y persistir el cambio
        motivoAjusteEntity.setEstado(Estado.BAJA);
        motivoAjusteRepository.save(motivoAjusteEntity);

        // 3. Retornar el DTO del motivo de ajuste dado de baja
        return MapperMotivoAjuste.toDTO(motivoAjusteEntity);
    }
}
