package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FabricanteBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperFabricanteBarril;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Implementación del servicio de Fabricante de Barril.
 * <p>
 * El fabricante solo indica el modelo de fabricación del barril. Un barril puede repetir identificador entre
 * fabricantes distintos (la unicidad del identificador de un barril es relativa a su fabricante), pero un
 * fabricante no puede repetir nombre con otro fabricante activo.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class FabricanteBarrilServicioImpl implements IFabricanteBarrilServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos fabricantes de barril activos con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_fabricante_barril_nombre";

    private final IFabricanteBarrilRepository fabricanteBarrilRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Recupera una página de fabricantes de barril activos, filtrados opcionalmente por nombre.
     * <p>
     * Los fabricantes dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por nombre.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FabricanteBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FabricanteBarrilResponseDTO> filtrarFabricantesBarril(String nombre, Pageable pageable) {
        return fabricanteBarrilRepository.filtrarFabricantesBarril(nombre, pageable)
                .map(MapperFabricanteBarril::toDTO);
    }

    /**
     * Busca y retorna un fabricante de barril específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del fabricante de barril buscado.
     * @return Objeto {@link FabricanteBarrilResponseDTO} con la información del fabricante encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún fabricante de barril activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FabricanteBarrilResponseDTO buscarPorId(Long id) {
        return MapperFabricanteBarril.toDTO(fabricanteBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + id)));
    }

    /**
     * Registra un nuevo fabricante de barril en el sistema.
     * <p>
     * Normaliza el nombre (sin espacios en los extremos ni repetidos), valida que sea obligatorio y no supere los
     * 100 caracteres, y que no esté duplicado (case-insensitive) entre los fabricantes activos.
     * </p>
     *
     * @param fabricanteBarrilFormDTO Objeto DTO que contiene los datos de creación del fabricante de barril.
     * @return {@link FabricanteBarrilResponseDTO} representativo del fabricante guardado en la base de datos.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a otro fabricante de barril activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FABRICANTE_BARRIL)
    public FabricanteBarrilResponseDTO altaFabricanteBarril(FabricanteBarrilFormDTO fabricanteBarrilFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(fabricanteBarrilFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar que el nombre no esté registrado en otro fabricante activo (case-insensitive)
        if (fabricanteBarrilRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 4. Creo la entidad que se va a almacenar en la base de datos
        FabricanteBarrilEntity fabricanteBarrilEntity = FabricanteBarrilEntity.builder()
                .nombre(nombre)
                .estado(Estado.ACTIVO)
                .build();

        // 5. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperFabricanteBarril.toDTO(GuardadoUnico.guardar(fabricanteBarrilRepository, fabricanteBarrilEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Actualiza la información de un fabricante de barril existente en la base de datos.
     * <p>
     * Normaliza y valida el nombre (obligatorio y de longitud máxima) antes de consultar la base de datos, luego
     * localiza el fabricante por su ID y verifica que el nuevo nombre no colisione con el de otro fabricante
     * activo (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del fabricante de barril a modificar.
     * @param fabricanteBarrilFormDTO DTO con la información actualizada.
     * @return {@link FabricanteBarrilResponseDTO} representativo del fabricante con los cambios aplicados.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres.
     * @throws RecursoNoEncontradoException Si no se localiza un fabricante de barril activo por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro fabricante de barril activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.FABRICANTE_BARRIL)
    public FabricanteBarrilResponseDTO modificarFabricanteBarril(Long id, FabricanteBarrilFormDTO fabricanteBarrilFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(fabricanteBarrilFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima antes de consultar la base de datos
        validarNombre(nombre);

        // 3. Localizar el fabricante existente. Si no existe, se dispara RecursoNoEncontradoException
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + id));

        // 4. Validar el nombre excluyendo el propio ID, de modo que conservar el nombre actual
        //    (incluso con distinto case) no falle contra el mismo registro
        if (fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 5. Aplico los cambios sobre la entidad administrada por persistencia
        fabricanteBarrilEntity.setNombre(nombre);

        // 6. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperFabricanteBarril.toDTO(GuardadoUnico.guardar(fabricanteBarrilRepository, fabricanteBarrilEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre))));
    }

    /**
     * Procesa la baja lógica de un fabricante de barril existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al fabricante con {@link Estado#BAJA} y persiste el
     * cambio. No tiene sentido dar de baja un fabricante mientras siga teniendo barriles activos
     * en el sistema, por eso se valida esa condición antes de aplicar la baja.
     * </p>
     *
     * @param id Identificador clave primaria del fabricante de barril a dar de baja.
     * @return {@link FabricanteBarrilResponseDTO} con los datos del fabricante ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el fabricante de barril con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el fabricante tiene al menos un barril activo asociado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.FABRICANTE_BARRIL)
    public FabricanteBarrilResponseDTO bajaFabricanteBarril(Long id) {
        // 1. Buscamos el fabricante. Si no existe, se dispara RecursoNoEncontradoException
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + id));

        // 2. Validar que el fabricante no tenga barriles activos asociados
        if (barrilRepository.existsByFabricanteId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        fabricanteBarrilEntity.setEstado(Estado.BAJA);
        fabricanteBarrilRepository.save(fabricanteBarrilEntity);

        // 4. Retornamos el DTO del fabricante dado de baja
        return MapperFabricanteBarril.toDTO(fabricanteBarrilEntity);
    }

    /**
     * Valida que el nombre normalizado sea obligatorio y no supere la longitud máxima.
     *
     * @param nombre Nombre normalizado.
     * @throws ReglaNegocioException Si el nombre está vacío o supera los 100 caracteres.
     */
    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            throw new ReglaNegocioException("El nombre es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(nombre, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnAlta(String nombre) {
        return new RecursoDuplicadoException("Ya existe un fabricante de barril con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otro fabricante de barril");
    }
}
