package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.MapperLupulo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LupuloServicioImpl implements ILupuloServicio {

    // Inyecto el repositorio gracias a LOMBOK
    private final ILupuloRepository lupuloRepository;

    /**
     * Recupera una página de lúpulos activos registrados en el sistema.
     * <p>
     * Los lúpulos eliminados lógicamente son excluidos automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LupuloResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LupuloResponseDTO> findAll(Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
        return lupuloRepository.findAll(pageable).map(MapperLupulo::toDTO);
    }

    /**
     * Busca y retorna un lúpulo específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del lúpulo buscado.
     * @return Objeto {@link LupuloResponseDTO} con la información del lúpulo encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún lúpulo activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LupuloResponseDTO obtenerPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperLupulo.toDTO(lupuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El lúpulo no existe")));
    }

    /**
     * Registra un nuevo lúpulo en el sistema.
     * <p>
     * Valida que el porcentaje de alfa ácidos sea positivo (mayor a 0), que el nombre no esté
     * duplicado (case-insensitive) entre los lúpulos activos, y asigna la unidad de medida fija
     * de negocio ({@code GRAMO}) antes de persistir. Registra el evento en la auditoría.
     * </p>
     *
     * @param lupuloFormDTO Objeto DTO que contiene los datos de creación del lúpulo.
     * @return {@link LupuloResponseDTO} representativo del lúpulo guardado en la base de datos.
     * @throws ReglaNegocioException Si el porcentaje de alfa ácidos no es positivo (mayor a 0).
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a un lúpulo activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LUPULO)
    public LupuloResponseDTO altaLupulo(LupuloFormDTO lupuloFormDTO) {
        // 1. Validar la regla de negocio de los alfa ácidos
        validarAlfaAcidos(lupuloFormDTO.getAa());

        // 2. Validar si el nombre ya está registrado en otro lúpulo (case-insensitive)
        if (lupuloRepository.existsByNombreIgnoreCase(lupuloFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe un lúpulo con el nombre '" + lupuloFormDTO.getNombre() + "'");
        }

        // 3. Creo la entidad que se va a almacenar en la base de datos.
        //    La unidad de medida es fija por regla de negocio y la asigna el service.
        LupuloEntity lupuloEntity = LupuloEntity.builder()
                .nombre(lupuloFormDTO.getNombre())
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .formato(lupuloFormDTO.getFormato())
                .aa(lupuloFormDTO.getAa())
                .build();

        // 4. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperLupulo.toDTO(lupuloRepository.save(lupuloEntity));
    }

    /**
     * Actualiza la información de un lúpulo existente en la base de datos.
     * <p>
     * Valida el porcentaje de alfa ácidos antes de consultar la base de datos, luego localiza
     * el lúpulo por su ID y verifica que el nuevo nombre no colisione con el de otro lúpulo
     * activo (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del lúpulo a modificar.
     * @param lupuloFormDTO DTO con la información actualizada.
     * @return {@link LupuloResponseDTO} representativo del lúpulo con los cambios aplicados.
     * @throws ReglaNegocioException Si el porcentaje de alfa ácidos no es positivo (mayor a 0).
     * @throws RecursoNoEncontradoException Si no se localiza un lúpulo activo por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otro lúpulo activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LUPULO)
    public LupuloResponseDTO modificarLupulo(Long id, LupuloFormDTO lupuloFormDTO) {
        // 1. Validar la regla de negocio de los alfa ácidos antes de consultar la base de datos
        validarAlfaAcidos(lupuloFormDTO.getAa());

        // 2. Localizar el lúpulo existente. Si no existe, se dispara RecursoNoEncontradoException
        LupuloEntity lupuloEntity = lupuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El lúpulo no existe"));

        // 3. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (lupuloRepository.existsByNombreIgnoreCaseAndIdNot(lupuloFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("El nombre '" + lupuloFormDTO.getNombre() + "' ya está en uso por otro lúpulo");
        }

        // 4. Aplico los cambios sobre la entidad administrada por persistencia.
        //    La unidad de medida no se toca: es fija por regla de negocio (GRAMO).
        lupuloEntity.setNombre(lupuloFormDTO.getNombre());
        lupuloEntity.setFormato(lupuloFormDTO.getFormato());
        lupuloEntity.setAa(lupuloFormDTO.getAa());

        // 5. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperLupulo.toDTO(lupuloRepository.save(lupuloEntity));
    }

    /**
     * Procesa la baja lógica de un lúpulo existente en el sistema.
     * <p>
     * Invoca el método de eliminación del repositorio. Como {@link LupuloEntity} hereda la
     * anotación {@code @SoftDelete} de {@code InsumoEntity}, Hibernate ejecuta un UPDATE
     * sobre el flag de borrado en lugar de una eliminación física.
     * </p>
     *
     * @param id Identificador clave primaria del lúpulo a dar de baja.
     * @return {@link LupuloResponseDTO} con los datos del lúpulo procesado antes de su inactivación.
     * @throws RecursoNoEncontradoException Si el lúpulo con el ID especificado no existe o ya fue dado de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.LUPULO)
    public LupuloResponseDTO bajaLupulo(Long id) {
        // 1. Buscamos el lúpulo. Si no existe, se dispara RecursoNoEncontradoException
        LupuloEntity lupuloEntity = lupuloRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lúpulo con ID: " + id));

        // 2. Ejecutamos la baja. Hibernate aplicará automáticamente la anotación de Soft Delete
        // TODO: validar dependencias de Stock/Recetas cuando esos módulos existan
        lupuloRepository.delete(lupuloEntity);

        // 3. Retornamos el DTO del lúpulo dado de baja en lugar de null
        return MapperLupulo.toDTO(lupuloEntity);
    }

    /**
     * Valida la regla de negocio del porcentaje de alfa ácidos del lúpulo.
     *
     * @param aa Porcentaje de alfa ácidos a validar.
     * @throws ReglaNegocioException Si el porcentaje de alfa ácidos es nulo o no es positivo (mayor a 0).
     */
    private static void validarAlfaAcidos(Integer aa) {
        if (aa == null || aa <= 0) {
            throw new ReglaNegocioException("El porcentaje de alfa ácidos debe ser mayor a 0");
        }
    }
}
