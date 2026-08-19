package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IMaltaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperMalta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaltaServicioImpl implements IMaltaServicio {

    // Inyecto el repositorio gracias a LOMBOK
    private final IMaltaRepository maltaRepository;

    /**
     * Recupera una página de maltas activas registradas en el sistema.
     * <p>
     * Las maltas eliminadas lógicamente son excluidas automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MaltaResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MaltaResponseDTO> buscarTodos(Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
        return maltaRepository.findAll(pageable).map(MapperMalta::toDTO);
    }

    /**
     * Busca y retorna una malta específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la malta buscada.
     * @return Objeto {@link MaltaResponseDTO} con la información de la malta encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna malta activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MaltaResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperMalta.toDTO(maltaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La malta no existe")));
    }

    /**
     * Registra una nueva malta en el sistema.
     * <p>
     * Valida que el rendimiento se encuentre entre 0 y 100 inclusive, que el nombre no esté
     * duplicado (case-insensitive) entre las maltas activas, y asigna la unidad de medida fija
     * de negocio ({@code KILOGRAMO}) antes de persistir. Registra el evento en la auditoría.
     * </p>
     *
     * @param maltaFormDTO Objeto DTO que contiene los datos de creación de la malta.
     * @return {@link MaltaResponseDTO} representativo de la malta guardada en la base de datos.
     * @throws ReglaNegocioException Si el rendimiento no se encuentra entre 0 y 100 inclusive.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a una malta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
    public MaltaResponseDTO altaMalta(MaltaFormDTO maltaFormDTO) {
        // 1. Validar la regla de negocio del rendimiento
        validarRendimiento(maltaFormDTO.getRendimiento());

        // 2. Validar si el nombre ya está registrado en otra malta (case-insensitive)
        if (maltaRepository.existsByNombreIgnoreCase(maltaFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una malta con el nombre '" + maltaFormDTO.getNombre() + "'");
        }

        // 3. Creo la entidad que se va a almacenar en la base de datos.
        //    La unidad de medida es fija por regla de negocio y la asigna el service.
        MaltaEntity maltaEntity = MaltaEntity.builder()
                .nombre(maltaFormDTO.getNombre())
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .tipo(maltaFormDTO.getTipo())
                .rendimiento(maltaFormDTO.getRendimiento())
                .build();

        // 4. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperMalta.toDTO(maltaRepository.save(maltaEntity));
    }

    /**
     * Actualiza la información de una malta existente en la base de datos.
     * <p>
     * Valida el rendimiento antes de consultar la base de datos, luego localiza la malta
     * por su ID y verifica que el nuevo nombre no colisione con el de otra malta activa
     * (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria de la malta a modificar.
     * @param maltaFormDTO DTO con la información actualizada.
     * @return {@link MaltaResponseDTO} representativo de la malta con los cambios aplicados.
     * @throws ReglaNegocioException Si el rendimiento no se encuentra entre 0 y 100 inclusive.
     * @throws RecursoNoEncontradoException Si no se localiza una malta activa por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otra malta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
    public MaltaResponseDTO modificarMalta(Long id, MaltaFormDTO maltaFormDTO) {
        // 1. Validar la regla de negocio del rendimiento antes de consultar la base de datos
        validarRendimiento(maltaFormDTO.getRendimiento());

        // 2. Localizar la malta existente. Si no existe, se dispara RecursoNoEncontradoException
        MaltaEntity maltaEntity = maltaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La malta no existe"));

        // 3. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (maltaRepository.existsByNombreIgnoreCaseAndIdNot(maltaFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + maltaFormDTO.getNombre() + "' ya está en uso por otra malta");
        }

        // 4. Aplico los cambios sobre la entidad administrada por persistencia.
        //    La unidad de medida no se toca: es fija por regla de negocio (KILOGRAMO).
        maltaEntity.setNombre(maltaFormDTO.getNombre());
        maltaEntity.setTipo(maltaFormDTO.getTipo());
        maltaEntity.setRendimiento(maltaFormDTO.getRendimiento());

        // 5. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperMalta.toDTO(maltaRepository.save(maltaEntity));
    }

    /**
     * Procesa la baja lógica de una malta existente en el sistema.
     * <p>
     * Invoca el método de eliminación del repositorio. Como {@link MaltaEntity} hereda la
     * anotación {@code @SoftDelete} de {@code InsumoEntity}, Hibernate ejecuta un UPDATE
     * sobre el flag de borrado en lugar de una eliminación física.
     * </p>
     *
     * @param id Identificador clave primaria de la malta a dar de baja.
     * @return {@link MaltaResponseDTO} con los datos de la malta procesada antes de su inactivación.
     * @throws RecursoNoEncontradoException Si la malta con el ID especificado no existe o ya fue dada de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MALTA)
    public MaltaResponseDTO bajaMalta(Long id) {
        // 1. Buscamos la malta. Si no existe, se dispara RecursoNoEncontradoException
        MaltaEntity maltaEntity = maltaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la malta con ID: " + id));

        // 2. Ejecutamos la baja. Hibernate aplicará automáticamente la anotación de Soft Delete
        // TODO: validar dependencias de Stock/Recetas cuando esos módulos existan
        maltaRepository.delete(maltaEntity);

        // 3. Retornamos el DTO de la malta dada de baja en lugar de null
        return MapperMalta.toDTO(maltaEntity);
    }

    /**
     * Valida la regla de negocio del rendimiento de la malta.
     *
     * @param rendimiento Rendimiento en porcentaje a validar.
     * @throws ReglaNegocioException Si el rendimiento es nulo o no se encuentra entre 0 y 100 inclusive.
     */
    private static void validarRendimiento(Integer rendimiento) {
        if (rendimiento == null || rendimiento < 0 || rendimiento > 100) {
            throw new ReglaNegocioException("El rendimiento debe estar entre 0 y 100 inclusive");
        }
    }
}
