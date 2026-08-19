package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMacerador;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaceradorServicioImpl implements IMaceradorServicio {

    private final IMaceradorRepository maceradorRepository;
    private final IEquipamientoRepository equipamientoRepository;

    /**
     * Recupera una página de Maceradores activos registrados en el sistema.
     * <p>
     * Los maceradores eliminados lógicamente son excluidos automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MaceradorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MaceradorResponseDTO> buscarTodos(Pageable pageable) {
        return maceradorRepository.findAll(pageable).map(MapperMacerador::toDTO);
    }

    /**
     * Busca y retorna un macerador específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del macerador buscado.
     * @return Objeto {@link MaceradorResponseDTO} con la información del macerador encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún macerador activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MaceradorResponseDTO buscarPorId(Long id) {
        return MapperMacerador.toDTO(maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el Macerador con ID:" + id)));
    }


    /**
     * Registra un nuevo macerador en el sistema.
     * <p>
     * Valida que el identificador interno no esté duplicado (case-insensitive) entre los equipamientos activos,
     * </p>
     *
     * @param maceradorFormDTO Objeto DTO que contiene los datos de creación del macerador.
     * @return {@link MaceradorResponseDTO} representativo del macerador guardado en la base de datos.
     * @throws ReglaNegocioException Si la capacidad util es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si la eficiencia de maceracion no se encuentra entre 40 y 100 inclusive.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a un macerador activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO altaMacerador(MaceradorFormDTO maceradorFormDTO) {

        // 1. Validar que la capacidad util no sea mayor a la capacidad total
        MetodosEquipamiento.validarCapacidadUtil(maceradorFormDTO.getCapacidadTotal(), maceradorFormDTO.getCapacidadUtil());

        // 2. Validar eficiencia de maceracion
        validarEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 3. Validar que el identificador interno no esté duplicado
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(maceradorFormDTO.getIdentificadorInterno())) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + maceradorFormDTO.getIdentificadorInterno() + "'");
        }

        // 4. Creo la entidad que se va a almacenar en la base de datos.
        MaceradorEntity maceradorEntity = MaceradorEntity.builder()
                .identificadorInterno(maceradorFormDTO.getIdentificadorInterno())
                .descripcion(maceradorFormDTO.getDescripcion())
                // Cuando se da de alta un nuevo macerador, su estado operativo inicial es DISPONIBLE.
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(maceradorFormDTO.getCapacidadTotal())
                .capacidadUtil(maceradorFormDTO.getCapacidadUtil())
                .espacioMuerto(maceradorFormDTO.getEspacioMuerto())
                .eficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion())
                .build();

        return MapperMacerador.toDTO(maceradorRepository.save(maceradorEntity));
    }

    /**
     * Actualiza la información de un macerador existente en la base de datos.
     * <p>
     * Valida el porcentaje que la capacidad util no sea mayor a la capacidad total, que la eficiencia de maceracion se encuentre entre 40 y 100 inclusive,
     * luego localiza el macerador por su ID y verifica que el nuevo identificador no colisione con el de otro macerador activo (case-insensitive),
     * permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del macerador a modificar.
     * @param maceradorFormDTO DTO con la información actualizada.
     * @return {@link MaceradorResponseDTO} representativo del macerador con los cambios aplicados.
     * @throws ReglaNegocioException Si la capacidad util es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si la eficiencia de maceracion no se encuentra entre 40 y 100 inclusive.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a un macerador activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO modificarMacerador(Long id,MaceradorFormDTO maceradorFormDTO) {
        // 1. Validar que la capacidad util no sea mayor a la capacidad total
        MetodosEquipamiento.validarCapacidadUtil(maceradorFormDTO.getCapacidadTotal(), maceradorFormDTO.getCapacidadUtil());

        // 2. Validar eficiencia de maceracion
        validarEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 3. Validar que el identificador interno no esté duplicado
        if(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(maceradorFormDTO.getIdentificadorInterno(), id)) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + maceradorFormDTO.getIdentificadorInterno() + "'");
        }

        // 4. Localizar el macerador existente. Si no existe, se dispara RecursoNoEncontradoException
        MaceradorEntity maceradorEntity = maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el Macerador con ID:" + id));

        // TODO: Validar que el Macerador no esté asociado a lotes Pendientes o en Ejecuciion

        // 5. Aplico los cambios sobre la entidad administrada por persistencia.
        maceradorEntity.setIdentificadorInterno(maceradorFormDTO.getIdentificadorInterno());
        maceradorEntity.setDescripcion(maceradorFormDTO.getDescripcion());
        maceradorEntity.setCapacidadTotal(maceradorFormDTO.getCapacidadTotal());
        maceradorEntity.setCapacidadUtil(maceradorFormDTO.getCapacidadUtil());
        maceradorEntity.setEspacioMuerto(maceradorFormDTO.getEspacioMuerto());
        maceradorEntity.setEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 6. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperMacerador.toDTO(maceradorRepository.save(maceradorEntity));
    }

    /**
     * Procesa la baja lógica de un macerador existente en el sistema.
     * <p>
     * Invoca el método de eliminación del repositorio. Como {@link MaceradorEntity} hereda la
     * anotación {@code @SoftDelete} de {@code EquipamientoEntity}, Hibernate ejecuta un UPDATE
     * sobre el flag de borrado en lugar de una eliminación física.
     * </p>
     *
     * @param id Identificador clave primaria del macerador a dar de baja.
     * @return {@link MaceradorResponseDTO} con los datos del macerador procesado antes de su inactivación.
     * @throws RecursoNoEncontradoException Si el macerador con el ID especificado no existe o ya fue dado de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO bajaMacerador(Long id) {

        // 1. Localizar el macerador existente. Si no existe, se dispara RecursoNoEncontradoException
        MaceradorEntity maceradorEntity = maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el Macerador con ID:" + id));

        // TODO: Validar que el Macerador no esté asociado a lotes Pendientes o en Ejecuciion

        // 2. Dar de baja el macerador
        maceradorRepository.delete(maceradorEntity);

        // 3. Retornar el DTO del macerador dado de baja
        return MapperMacerador.toDTO(maceradorEntity);
    }

    /**
     * Valida que la eficiencia de maceración esté entre 40 y 100 inclusive.
     *
     * @param eficienciaMaceracion Eficiencia de maceración del macerador.
     * @throws ReglaNegocioException Si la eficiencia de maceración no está entre 40 y 100 inclusive.
     */
    public static void validarEficienciaMaceracion(Double eficienciaMaceracion) {
        if (eficienciaMaceracion < 40 || eficienciaMaceracion > 100) {
            throw new ReglaNegocioException("La eficiencia de maceración debe estar entre 40 y 100 inclusive.");
        }
    }

}
