package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IPaisRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.PaisFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ubicacion.MapperPais;
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

@Service
@RequiredArgsConstructor
public class PaisServicioImpl implements IPaisServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos países activos con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_pais_nombre";

    // Inyecto los repositorios gracias a LOMBOK
    private final IPaisRepository paisRepository;
    private final IProvinciaRepository provinciaRepository;

    /**
     * Filtra los países activos, opcionalmente por nombre.
     * <p>
     * Los países dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link PaisResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PaisResponseDTO> filtrarPaises(String nombre, Pageable pageable) {
        // El texto buscado se normaliza igual que el nombre guardado: sin espacios de más
        String nombreNormalizado = NormalizacionTexto.nombrePropio(nombre);
        if (nombreNormalizado != null && nombreNormalizado.isEmpty()) {
            nombreNormalizado = null;
        }
        return paisRepository.filtrarPaises(nombreNormalizado, pageable).map(MapperPais::toDTO);
    }

    /**
     * Busca y retorna un país específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del país buscado.
     * @return Objeto {@link PaisResponseDTO} con la información del país encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún país activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public PaisResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperPais.toDTO(paisRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el país con ID: " + id)));
    }

    /**
     * Registra un nuevo país en el sistema.
     * <p>
     * Normaliza el nombre (sin espacios en los extremos ni repetidos), valida que sea obligatorio y no supere los 100 caracteres, y que no esté
     * duplicado (case-insensitive) entre los países activos. Registra el evento en la auditoría.
     * </p>
     *
     * @param paisFormDTO Objeto DTO que contiene los datos de creación del país.
     * @return {@link PaisResponseDTO} representativo del país guardado en la base de datos.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a un país activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PAIS)
    public PaisResponseDTO altaPais(PaisFormDTO paisFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(paisFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar si el nombre ya está registrado en otro país (case-insensitive)
        if (paisRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 4. Creo la entidad que se va a almacenar en la base de datos
        PaisEntity paisEntity = PaisEntity.builder()
                .nombre(nombre)
                .estado(Estado.ACTIVO)
                .build();

        // 5. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperPais.toDTO(GuardadoUnico.guardar(paisRepository, paisEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Actualiza la información de un país existente en la base de datos.
     * <p>
     * Normaliza y valida el nombre (obligatorio y de longitud máxima), localiza el país por su ID y verifica que el nuevo nombre no colisione con el de otro
     * país activo (case-insensitive), permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del país a modificar.
     * @param paisFormDTO DTO con la información actualizada.
     * @return {@link PaisResponseDTO} representativo del país con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza un país activo por el ID proporcionado.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otro país activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PAIS)
    public PaisResponseDTO modificarPais(Long id, PaisFormDTO paisFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(paisFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Localizar el país existente. Si no existe, se dispara RecursoNoEncontradoException
        PaisEntity paisEntity = paisRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el país con ID: " + id));

        // 4. Se valida duplicación excluyendo el propio ID, de modo que conservar
        //    el nombre actual (incluso con distinto case) no falla contra el mismo registro
        if (paisRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 5. Aplico los cambios sobre la entidad administrada por persistencia
        paisEntity.setNombre(nombre);

        // 6. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperPais.toDTO(GuardadoUnico.guardar(paisRepository, paisEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre))));
    }

    /**
     * Procesa la baja lógica de un país existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al país con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del país a dar de baja.
     * @return {@link PaisResponseDTO} con los datos del país ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el país con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el país tiene al menos una provincia activa asociada.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PAIS)
    public PaisResponseDTO bajaPais(Long id) {
        // 1. Buscamos el país. Si no existe, se dispara RecursoNoEncontradoException
        PaisEntity paisEntity = paisRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el país con ID: " + id));

        // 2. Validar que el país no tenga provincias activas asociadas
        if (provinciaRepository.existsByPaisId(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el país porque tiene provincias activas asociadas");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        paisEntity.setEstado(Estado.BAJA);
        paisRepository.save(paisEntity);

        // 4. Retornamos el DTO del país dado de baja
        return MapperPais.toDTO(paisEntity);
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
        return new RecursoDuplicadoException("Ya existe un país con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otro país");
    }
}
