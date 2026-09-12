package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMolino;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MolinoServicioImpl implements IMolinoServicio {

    private final IMolinoRepository molinoRepository;
    private final IEquipamientoRepository equipamientoRepository;

    /**
     * Recupera una página de Molinos activos registrados en el sistema, filtrados opcionalmente
     * por identificador interno (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o
     * estado operativo (coincidencia exacta).
     * <p>
     * Los molinos dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MolinoResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MolinoResponseDTO> filtrarMolinos(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        return molinoRepository.filtrarMolinos(identificadorInterno, estadoOperativo, pageable).map(MapperMolino::toDTO);
    }


    /**
     * Recupera un molino por su ID.
     *
     * @param id ID del molino a buscar.
     * @return {@link MolinoResponseDTO} con los datos del molino encontrado.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public MolinoResponseDTO buscarPorId(Long id) {
        return MapperMolino.toDTO(molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el Molino con ID:" + id)));
    }

    /**
     * Crea un nuevo molino en el sistema.
     *
     * @param molinoFormDTO Datos del molino a crear.
     * @return {@link MolinoResponseDTO} con los datos del molino creado.
     * @throws RecursoDuplicadoException Si ya existe un equipamiento con el mismo identificador interno.
     * @throws ReglaNegocioException      Si el rendimiento de molienda es menor o igual a 0.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO altaMolino(MolinoFormDTO molinoFormDTO) {
        // 1. Validar que el rendimiento de molienda sea mayor a 0.
        validarRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 2. Validar que no haya otro equipamiento registrado con el mismo identificador interno.
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(molinoFormDTO.getIdentificadorInterno())) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + molinoFormDTO.getIdentificadorInterno() + "'");
        }

        // 3. Crear la entidad MolinoEntity.
        MolinoEntity molinoEntity = MolinoEntity.builder().
                identificadorInterno(molinoFormDTO.getIdentificadorInterno()).
                descripcion(molinoFormDTO.getDescripcion()).
                rendimientoMolienda(molinoFormDTO.getRendimientoMolienda()).
                estadoOperativo(EstadoOperativo.DISPONIBLE).
                estado(Estado.ACTIVO).
                build();

        // 4. Guardar la entidad en la base de datos.
        return MapperMolino.toDTO(molinoRepository.save(molinoEntity));
    }

    /**
     * Modifica un molino existente en el sistema.
     *
     * @param id             ID del molino a modificar.
     * @param molinoFormDTO  Datos actualizados del molino.
     * @return {@link MolinoResponseDTO} con los datos del molino modificado.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     * @throws RecursoDuplicadoException    Si otro equipamiento ya tiene el mismo identificador interno.
     * @throws ReglaNegocioException         Si el rendimiento de molienda es menor o igual a 0.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO modificarMolino(Long id, MolinoFormDTO molinoFormDTO) {
        // 1. Validar que el rendimiento de molienda sea mayor a 0.
        validarRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 2. Validar que no haya otro equipamiento registrado con el mismo identificador interno (excluyendo el actual).
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(molinoFormDTO.getIdentificadorInterno(), id)) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + molinoFormDTO.getIdentificadorInterno() + "'");
        }

        // 3. Buscar el molino existente por su ID.
        MolinoEntity molinoEntity = molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el Molino con ID:" + id));

        // TODO: Validar que el molino no esté asociado a lotes Pendientes o en Ejecuciion

        // 4. Actualizar los campos del molinoEntity con los datos del molinoFormDTO.
        molinoEntity.setIdentificadorInterno(molinoFormDTO.getIdentificadorInterno());
        molinoEntity.setDescripcion(molinoFormDTO.getDescripcion());
        molinoEntity.setRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 5. Guardar la entidad actualizada en la base de datos.
        return MapperMolino.toDTO(molinoRepository.save(molinoEntity));
    }

    /**
     * Da de baja un molino existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al molino con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id ID del molino a dar de baja.
     * @return {@link MolinoResponseDTO} con los datos del molino ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO bajaMolino(Long id) {
        MolinoEntity molinoEntity = molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el Molino con ID:" + id));

        // TODO: Validar que el molino no esté asociado a lotes Pendientes o en Ejecuciion

        // 2. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        molinoEntity.setEstado(Estado.BAJA);
        molinoRepository.save(molinoEntity);

        // 3. Retornar el DTO del molino dado de baja
        return MapperMolino.toDTO(molinoEntity);
    }

    /**
     * Valida que el rendimiento de molienda sea mayor a 0.
     *
     * @param rendimientoMolienda Rendimiento de molienda a validar.
     * @throws ReglaNegocioException Si el rendimiento de molienda es menor o igual a 0.
     */
    public static void validarRendimientoMolienda(Double rendimientoMolienda) {
        if (rendimientoMolienda <= 0) {
            throw new ReglaNegocioException("El rendimiento de molienda debe ser mayor a 0.");
        }
    }
}
