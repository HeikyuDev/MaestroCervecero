package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IPaisRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.ProvinciaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ubicacion.MapperProvincia;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProvinciaServicioImpl implements IProvinciaServicio {

    // Inyecto los repositorios gracias a LOMBOK
    private final IProvinciaRepository provinciaRepository;
    private final IPaisRepository paisRepository;
    private final ILocalidadRepository localidadRepository;

    /**
     * Recupera una página de provincias activas registradas en el sistema.
     * <p>
     * Las provincias dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ProvinciaResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProvinciaResponseDTO> buscarTodos(Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
        return provinciaRepository.findAll(pageable).map(MapperProvincia::toDTO);
    }

    /**
     * Busca y retorna una provincia específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la provincia buscada.
     * @return Objeto {@link ProvinciaResponseDTO} con la información de la provincia encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna provincia activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public ProvinciaResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperProvincia.toDTO(provinciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La provincia no existe")));
    }

    /**
     * Registra una nueva provincia en el sistema.
     * <p>
     * Valida que el país seleccionado exista y que el nombre no esté duplicado
     * (case-insensitive) entre las provincias activas de ese mismo país. Registra el evento
     * en la auditoría.
     * </p>
     *
     * @param provinciaFormDTO Objeto DTO que contiene los datos de creación de la provincia.
     * @return {@link ProvinciaResponseDTO} representativo de la provincia guardada en la base de datos.
     * @throws RecursoNoEncontradoException Si no existe un país activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una provincia activa del mismo país.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PROVINCIA)
    public ProvinciaResponseDTO altaProvincia(ProvinciaFormDTO provinciaFormDTO) {
        // 1. Verificar que el país seleccionado exista
        PaisEntity paisEntity = paisRepository.findById(provinciaFormDTO.getIdPais())
                .orElseThrow(() -> new RecursoNoEncontradoException("El país no existe"));

        // 2. Validar si el nombre ya está registrado en otra provincia del mismo país (case-insensitive)
        if (provinciaRepository.existsByNombreIgnoreCaseAndPaisId(provinciaFormDTO.getNombre(), provinciaFormDTO.getIdPais())) {
            throw new RecursoDuplicadoException("Ya existe una provincia con el nombre '" + provinciaFormDTO.getNombre() + "' para el país seleccionado");
        }

        // 3. Creo la entidad que se va a almacenar en la base de datos
        ProvinciaEntity provinciaEntity = ProvinciaEntity.builder()
                .nombre(provinciaFormDTO.getNombre())
                .pais(paisEntity)
                .estado(Estado.ACTIVO)
                .build();

        // 4. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperProvincia.toDTO(provinciaRepository.save(provinciaEntity));
    }

    /**
     * Actualiza la información de una provincia existente en la base de datos.
     * <p>
     * Localiza la provincia por su ID, verifica que el país seleccionado exista y que el nuevo
     * nombre no colisione con el de otra provincia activa del mismo país (case-insensitive),
     * permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria de la provincia a modificar.
     * @param provinciaFormDTO DTO con la información actualizada.
     * @return {@link ProvinciaResponseDTO} representativo de la provincia con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza una provincia activa por el ID proporcionado, o si no existe un país activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra provincia activa del mismo país.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PROVINCIA)
    public ProvinciaResponseDTO modificarProvincia(Long id, ProvinciaFormDTO provinciaFormDTO) {
        // 1. Localizar la provincia existente. Si no existe, se dispara RecursoNoEncontradoException
        ProvinciaEntity provinciaEntity = provinciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La provincia no existe"));

        // 2. Verificar que el país seleccionado exista
        PaisEntity paisEntity = paisRepository.findById(provinciaFormDTO.getIdPais())
                .orElseThrow(() -> new RecursoNoEncontradoException("El país no existe"));

        // 3. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (provinciaRepository.existsByNombreIgnoreCaseAndPaisIdAndIdNot(provinciaFormDTO.getNombre(), provinciaFormDTO.getIdPais(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + provinciaFormDTO.getNombre() + "' ya está en uso por otra provincia del país seleccionado");
        }

        // 4. Aplico los cambios sobre la entidad administrada por persistencia
        provinciaEntity.setNombre(provinciaFormDTO.getNombre());
        provinciaEntity.setPais(paisEntity);

        // 5. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperProvincia.toDTO(provinciaRepository.save(provinciaEntity));
    }

    /**
     * Procesa la baja lógica de una provincia existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la provincia con {@link Estado#BAJA} y
     * persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan
     * de encontrarla.
     * </p>
     *
     * @param id Identificador clave primaria de la provincia a dar de baja.
     * @return {@link ProvinciaResponseDTO} con los datos de la provincia ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la provincia con el ID especificado no existe o ya fue dada de baja.
     * @throws ReglaNegocioException Si la provincia tiene al menos una localidad activa asociada.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PROVINCIA)
    public ProvinciaResponseDTO bajaProvincia(Long id) {
        // 1. Buscamos la provincia. Si no existe, se dispara RecursoNoEncontradoException
        ProvinciaEntity provinciaEntity = provinciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la provincia con ID: " + id));

        // 2. Validar que la provincia no tenga localidades activas asociadas
        if (localidadRepository.existsByProvinciaId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la provincia porque tiene localidades activas asociadas");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        provinciaEntity.setEstado(Estado.BAJA);
        provinciaRepository.save(provinciaEntity);

        // 4. Retornamos el DTO de la provincia dada de baja
        return MapperProvincia.toDTO(provinciaEntity);
    }
}
