package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperFermentador;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FermentadorServicioImpl implements IFermentadorServicio {

    private final IFermentadorRepository fermentadorRepository;
    private final IEquipamientoRepository equipamientoRepository;


    /**
     * Recupera una página de Fermentadores activos registrados en el sistema, filtrados
     * opcionalmente por identificador interno (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta).
     * <p>
     * Los fermentadores dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FermentadorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FermentadorResponseDTO> filtrarFermentadores(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        return fermentadorRepository.filtrarFermentadores(identificadorInterno, estadoOperativo, pageable).map(MapperFermentador::toDTO);
    }

    /**
     * Busca y retorna un fermentador específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del fermentador buscado.
     * @return Objeto {@link FermentadorResponseDTO} con la información del fermentador encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún fermentador activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FermentadorResponseDTO buscarPorId(Long id) {
        return MapperFermentador.toDTO(fermentadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + id)));
    }


    /**
     * Crea un nuevo fermentador en el sistema.
     *
     * @param fermentadorFormDTO DTO que contiene los datos del fermentador a crear.
     * @return Objeto {@link FermentadorResponseDTO} con la información del fermentador creado.
     * @throws ReglaNegocioException Si la capacidad util es mayor o igual a la capacidad total.
     * @throws RecursoDuplicadoException Si ya existe un equipamiento con el mismo identificador interno.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FERMENTADOR)
    public FermentadorResponseDTO altaFermentador(FermentadorFormDTO fermentadorFormDTO) {

        // 1. Validar que la capacidad util no sea mayor o igual a la capacidad total.
        MetodosEquipamiento.validarCapacidadUtil(fermentadorFormDTO.getCapacidadTotal(), fermentadorFormDTO.getCapacidadUtil());

        // 2. Validar que no haya otro equipamiento registrado con el mismo identificador interno.
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(fermentadorFormDTO.getIdentificadorInterno())) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + fermentadorFormDTO.getIdentificadorInterno() + "'");
        }

        // 3. Crear la entidad del fermentador a partir del DTO de formulario y establecer su estado operativo como DISPONIBLE.
        FermentadorEntity fermentadorEntity = FermentadorEntity.builder()
                .identificadorInterno(fermentadorFormDTO.getIdentificadorInterno())
                .descripcion(fermentadorFormDTO.getDescripcion())
                .capacidadTotal(fermentadorFormDTO.getCapacidadTotal())
                .capacidadUtil(fermentadorFormDTO.getCapacidadUtil())
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .estado(Estado.ACTIVO)
                .build();

        // 4. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente.
        return MapperFermentador.toDTO(fermentadorRepository.save(fermentadorEntity));
    }

    /**
     * Modifica un fermentador existente en el sistema.
     *
     * @param id                 Identificador único del fermentador a modificar.
     * @param fermentadorFormDTO DTO que contiene los nuevos datos del fermentador.
     * @return Objeto {@link FermentadorResponseDTO} con la información actualizada del fermentador.
     * @throws ReglaNegocioException        Si la capacidad util es mayor o igual a la capacidad total.
     * @throws RecursoNoEncontradoException Si no existe ningún fermentador activo con el ID especificado.
     * @throws RecursoDuplicadoException    Si otro equipamiento ya tiene el mismo identificador interno que se intenta asignar.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.FERMENTADOR)
    public FermentadorResponseDTO modificarFermentador(Long id, FermentadorFormDTO fermentadorFormDTO) {
        // 1. Validar que la capacidad util no sea mayor o igual a la capacidad total.
        MetodosEquipamiento.validarCapacidadUtil(fermentadorFormDTO.getCapacidadTotal(), fermentadorFormDTO.getCapacidadUtil());

        // 2. Validar que no haya otro equipamiento registrado con el mismo identificador interno (excluyendo el actual).
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(fermentadorFormDTO.getIdentificadorInterno(), id)) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + fermentadorFormDTO.getIdentificadorInterno() + "'");
        }

        // 3. Buscar el fermentador existente por su ID.
        FermentadorEntity fermentadorEntity = fermentadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + id));

        // TODO: Validar que el fermentador no esté asociado a lotes Pendientes o en Ejecuciion

        // 4. Actualizar los campos del fermentador con los datos del DTO de formulario.
        fermentadorEntity.setIdentificadorInterno(fermentadorFormDTO.getIdentificadorInterno());
        fermentadorEntity.setDescripcion(fermentadorFormDTO.getDescripcion());
        fermentadorEntity.setCapacidadTotal(fermentadorFormDTO.getCapacidadTotal());
        fermentadorEntity.setCapacidadUtil(fermentadorFormDTO.getCapacidadUtil());

        // 5. Guardar la entidad actualizada en la base de datos y retornar el DTO de respuesta correspondiente.
        return MapperFermentador.toDTO(fermentadorRepository.save(fermentadorEntity));
    }

    /**
     * Procesa la baja lógica de un Fermentador existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al fermentador con {@link Estado#BAJA} y persiste
     * el cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del fermentador a dar de baja.
     * @return {@link FermentadorResponseDTO} con los datos del fermentador ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el fermentador con el ID especificado no existe o ya fue dado de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.FERMENTADOR)
    public FermentadorResponseDTO bajaFermentador(Long id) {

        FermentadorEntity fermentadorEntity = fermentadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + id));

        // TODO: Validar que el fermentador no esté asociado a lotes Pendientes o en Ejecuciion

        // 2. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        fermentadorEntity.setEstado(Estado.BAJA);

        // 3. Retornar el DTO del fermentador dado de baja
        return MapperFermentador.toDTO(fermentadorRepository.save(fermentadorEntity));
    }

}
