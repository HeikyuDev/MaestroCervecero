package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.LocalidadFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ubicacion.MapperLocalidad;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalidadServicioImpl implements ILocalidadServicio {

    // Inyecto los repositorios gracias a LOMBOK
    private final ILocalidadRepository localidadRepository;
    private final IProvinciaRepository provinciaRepository;

    /**
     * Recupera una página de localidades activas registradas en el sistema.
     * <p>
     * Las localidades dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LocalidadResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LocalidadResponseDTO> buscarTodos(Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
        return localidadRepository.findAll(pageable).map(MapperLocalidad::toDTO);
    }

    /**
     * Busca y retorna una localidad específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la localidad buscada.
     * @return Objeto {@link LocalidadResponseDTO} con la información de la localidad encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna localidad activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LocalidadResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperLocalidad.toDTO(localidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La localidad no existe")));
    }

    /**
     * Registra una nueva localidad en el sistema.
     * <p>
     * Valida que la provincia seleccionada exista, que el nombre no esté duplicado
     * (case-insensitive) entre las localidades activas de esa misma provincia, y que el código
     * postal no esté duplicado entre las localidades activas. Registra el evento en la
     * auditoría.
     * </p>
     *
     * @param localidadFormDTO Objeto DTO que contiene los datos de creación de la localidad.
     * @return {@link LocalidadResponseDTO} representativo de la localidad guardada en la base de datos.
     * @throws RecursoNoEncontradoException Si no existe una provincia activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una localidad activa de la misma provincia, o si el código postal ya pertenece a otra localidad activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LOCALIDAD)
    public LocalidadResponseDTO altaLocalidad(LocalidadFormDTO localidadFormDTO) {
        // 1. Verificar que la provincia seleccionada exista
        ProvinciaEntity provinciaEntity = provinciaRepository.findById(localidadFormDTO.getIdProvincia())
                .orElseThrow(() -> new RecursoNoEncontradoException("La provincia no existe"));

        // 2. Validar si el nombre ya está registrado en otra localidad de la misma provincia (case-insensitive)
        if (localidadRepository.existsByNombreIgnoreCaseAndProvinciaId(localidadFormDTO.getNombre(), localidadFormDTO.getIdProvincia())) {
            throw new RecursoDuplicadoException("Ya existe una localidad con el nombre '" + localidadFormDTO.getNombre() + "' para la provincia seleccionada");
        }

        // 3. Validar si el código postal ya está registrado en otra localidad
        if (localidadRepository.existsByCodigoPostal(localidadFormDTO.getCodigoPostal())) {
            throw new RecursoDuplicadoException("Ya existe una localidad con el código postal '" + localidadFormDTO.getCodigoPostal() + "'");
        }

        // 4. Creo la entidad que se va a almacenar en la base de datos
        LocalidadEntity localidadEntity = LocalidadEntity.builder()
                .nombre(localidadFormDTO.getNombre())
                .codigoPostal(localidadFormDTO.getCodigoPostal())
                .provincia(provinciaEntity)
                .estado(Estado.ACTIVO)
                .build();

        // 5. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperLocalidad.toDTO(localidadRepository.save(localidadEntity));
    }

    /**
     * Actualiza la información de una localidad existente en la base de datos.
     * <p>
     * Localiza la localidad por su ID, verifica que la provincia seleccionada exista, que el
     * nuevo nombre no colisione con el de otra localidad activa de la misma provincia
     * (case-insensitive), y que el nuevo código postal no colisione con el de otra localidad
     * activa, permitiendo conservar los propios valores actuales.
     * </p>
     *
     * @param id Identificador clave primaria de la localidad a modificar.
     * @param localidadFormDTO DTO con la información actualizada.
     * @return {@link LocalidadResponseDTO} representativo de la localidad con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza una localidad activa por el ID proporcionado, o si no existe una provincia activa con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra localidad activa de la misma provincia, o si el nuevo código postal ya se encuentra asignado a otra localidad activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOCALIDAD)
    public LocalidadResponseDTO modificarLocalidad(Long id, LocalidadFormDTO localidadFormDTO) {
        // 1. Localizar la localidad existente. Si no existe, se dispara RecursoNoEncontradoException
        LocalidadEntity localidadEntity = localidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La localidad no existe"));

        // 2. Verificar que la provincia seleccionada exista
        ProvinciaEntity provinciaEntity = provinciaRepository.findById(localidadFormDTO.getIdProvincia())
                .orElseThrow(() -> new RecursoNoEncontradoException("La provincia no existe"));

        // 3. Se valida duplicación de nombre excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot(localidadFormDTO.getNombre(), localidadFormDTO.getIdProvincia(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + localidadFormDTO.getNombre() + "' ya está en uso por otra localidad de la provincia seleccionada");
        }

        // 4. Se valida duplicación de código postal excluyendo el propio ID
        if (localidadRepository.existsByCodigoPostalAndIdNot(localidadFormDTO.getCodigoPostal(), id)) {
            throw new RecursoDuplicadoException("El código postal '" + localidadFormDTO.getCodigoPostal() + "' ya está en uso por otra localidad");
        }

        // 5. Aplico los cambios sobre la entidad administrada por persistencia
        localidadEntity.setNombre(localidadFormDTO.getNombre());
        localidadEntity.setCodigoPostal(localidadFormDTO.getCodigoPostal());
        localidadEntity.setProvincia(provinciaEntity);

        // 6. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperLocalidad.toDTO(localidadRepository.save(localidadEntity));
    }

    /**
     * Procesa la baja lógica de una localidad existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la localidad con {@link Estado#BAJA} y
     * persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan
     * de encontrarla.
     * </p>
     *
     * @param id Identificador clave primaria de la localidad a dar de baja.
     * @return {@link LocalidadResponseDTO} con los datos de la localidad ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la localidad con el ID especificado no existe o ya fue dada de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.LOCALIDAD)
    public LocalidadResponseDTO bajaLocalidad(Long id) {
        // 1. Buscamos la localidad. Si no existe, se dispara RecursoNoEncontradoException
        LocalidadEntity localidadEntity = localidadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + id));

        // 2. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        // TODO: verifica que la localidad seleccionada no se encuentre asociada a Proveedores o Clientes en estado activo.
        localidadEntity.setEstado(Estado.BAJA);
        localidadRepository.save(localidadEntity);

        // 3. Retornamos el DTO de la localidad dada de baja
        return MapperLocalidad.toDTO(localidadEntity);
    }
}
