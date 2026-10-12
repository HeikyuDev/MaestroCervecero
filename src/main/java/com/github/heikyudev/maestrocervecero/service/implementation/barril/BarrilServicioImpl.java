package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperBarril;
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
 * Implementación del servicio de Barril.
 * <p>
 * Un barril puede repetir identificador entre fabricantes distintos: la unicidad del
 * identificador de un barril es relativa a su fabricante.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class BarrilServicioImpl implements IBarrilServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos barriles activos del mismo fabricante con el mismo identificador.
     */
    private static final String INDICE_IDENTIFICADOR = "udx_barril_identificador";

    private final IBarrilRepository barrilRepository;
    private final IFabricanteBarrilRepository fabricanteBarrilRepository;

    /**
     * Recupera una página de barriles activos, filtrados opcionalmente por identificador,
     * capacidad, estado operativo y/o fabricante.
     * <p>
     * Los barriles dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificador Texto a buscar dentro del identificador, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idFabricante El ID del fabricante a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link BarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BarrilResponseDTO> filtrarBarriles(String identificador, Double capacidad, EstadoOperativoBarril estadoOperativo, Long idFabricante, Pageable pageable) {
        return barrilRepository.filtrarBarriles(identificador, capacidad, estadoOperativo, idFabricante, pageable).map(MapperBarril::toDTO);
    }

    /**
     * Busca y retorna un barril específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del barril buscado.
     * @return Objeto {@link BarrilResponseDTO} con la información del barril encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún barril activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public BarrilResponseDTO buscarPorId(Long id) {
        return MapperBarril.toDTO(barrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id)));
    }

    /**
     * Registra un nuevo barril en el sistema.
     * <p>
     * Normaliza el identificador (en mayúsculas, sin espacios en los extremos ni repetidos), valida que sea obligatorio y no supere los 100 caracteres,
     * que la capacidad sea mayor a 0, que el fabricante de barril indicado exista y esté
     * activo, y que ningún otro barril activo de ese mismo fabricante tenga ya el identificador
     * indicado.
     * </p>
     *
     * @param barrilFormDTO Objeto DTO que contiene los datos de creación del barril.
     * @return {@link BarrilResponseDTO} representativo del barril guardado en la base de datos.
     * @throws ReglaNegocioException Si el identificador falta o supera los 100 caracteres, si la capacidad es nula o menor o igual a 0, o si los usos máximos antes de mantenimiento son nulos o menores o iguales a 0.
     * @throws RecursoNoEncontradoException Si no existe un fabricante de barril activo con el ID indicado.
     * @throws RecursoDuplicadoException Si otro barril activo del mismo fabricante ya tiene ese identificador.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO altaBarril(BarrilFormDTO barrilFormDTO) {
        // 1. Normalizar el identificador sin modificar el DTO
        String identificador = NormalizacionTexto.codigo(barrilFormDTO.getIdentificador());

        // 2. Validar que el identificador sea obligatorio y no supere la longitud máxima
        validarIdentificador(identificador);

        // 3. Validar que la capacidad sea mayor a 0
        validarCapacidad(barrilFormDTO.getCapacidad());

        // 4. Validar que los usos máximos antes de mantenimiento sean válidos
        validarUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());

        // 5. Validar que el fabricante de barril indicado exista y esté activo
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(barrilFormDTO.getIdFabricanteBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + barrilFormDTO.getIdFabricanteBarril()));

        // 6. Validar que no haya otro barril activo del mismo fabricante con el mismo identificador
        if (barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteId(identificador, barrilFormDTO.getIdFabricanteBarril())) {
            throw identificadorDuplicadoEnAlta(identificador);
        }

        // 7. Creo la entidad que se va a almacenar en la base de datos
        BarrilEntity barrilEntity = BarrilEntity.builder()
                .identificador(identificador)
                .capacidad(barrilFormDTO.getCapacidad())
                .contenidoActual(0.0)
                .estadoOperativo(EstadoOperativoBarril.DISPONIBLE)
                .usosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento())
                .estado(Estado.ACTIVO)
                .fabricante(fabricanteBarrilEntity)
                .build();

        // 8. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperBarril.toDTO(GuardadoUnico.guardar(barrilRepository, barrilEntity,
                Map.of(INDICE_IDENTIFICADOR, () -> identificadorDuplicadoEnAlta(identificador))));
    }

    /**
     * Actualiza la información de un barril existente en la base de datos.
     * <p>
     * Localiza el barril por su ID, normaliza el identificador (en mayúsculas), valida que sea obligatorio y no supere los 100 caracteres, que la capacidad sea mayor a 0 y no sea menor al
     * contenido actual del barril, que los usos máximos antes de mantenimiento sean válidos, que
     * el fabricante de barril indicado exista y esté activo (el fabricante puede reasignarse al
     * modificar), y que ningún otro barril activo de ese fabricante tenga ya el identificador
     * indicado, permitiendo conservar el propio identificador actual.
     * </p>
     *
     * @param id Identificador clave primaria del barril a modificar.
     * @param barrilFormDTO DTO con la información actualizada.
     * @return {@link BarrilResponseDTO} representativo del barril con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza un barril activo por el ID proporcionado, o si no existe un fabricante de barril activo con el ID indicado.
     * @throws ReglaNegocioException Si el identificador falta o supera los 100 caracteres, si la capacidad es nula o menor o igual a 0, si la capacidad es menor al contenido actual del barril, o si los usos máximos antes de mantenimiento son nulos o menores o iguales a 0.
     * @throws RecursoDuplicadoException Si otro barril activo del mismo fabricante ya tiene ese identificador.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO modificarBarril(Long id, BarrilFormDTO barrilFormDTO) {
        // 1. Normalizar el identificador sin modificar el DTO
        String identificador = NormalizacionTexto.codigo(barrilFormDTO.getIdentificador());

        // 2. Localizar el barril existente. Si no existe, se dispara RecursoNoEncontradoException
        BarrilEntity barrilEntity = barrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id));

        // 3. Validar que el identificador sea obligatorio y no supere la longitud máxima
        validarIdentificador(identificador);

        // 4. Validar que la capacidad sea mayor a 0
        validarCapacidad(barrilFormDTO.getCapacidad());

        // 5. Validar que la nueva capacidad no sea menor al contenido actual del barril: reducirla
        //    por debajo de lo que ya tiene cargado dejaría al barril en un estado imposible
        if (barrilFormDTO.getCapacidad() < barrilEntity.getContenidoActual()) {
            throw new ReglaNegocioException("La capacidad no puede ser menor al contenido actual del barril (" + barrilEntity.getContenidoActual() + " L)");
        }

        // 6. Validar que los usos máximos antes de mantenimiento sean válidos
        validarUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());

        // 7. Validar que el fabricante de barril indicado exista y esté activo
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(barrilFormDTO.getIdFabricanteBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + barrilFormDTO.getIdFabricanteBarril()));

        // 8. Validar duplicación excluyendo el propio barril, de modo que conservar el
        //    identificador actual no falle contra el mismo registro
        if (barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot(identificador, barrilFormDTO.getIdFabricanteBarril(), id)) {
            throw identificadorDuplicadoEnModificacion(identificador);
        }

        // 9. Aplico los cambios sobre la entidad administrada por persistencia
        barrilEntity.setIdentificador(identificador);
        barrilEntity.setCapacidad(barrilFormDTO.getCapacidad());
        barrilEntity.setUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());
        barrilEntity.setFabricante(fabricanteBarrilEntity);

        // 10. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //     y devuelvo el DTO correspondiente
        return MapperBarril.toDTO(GuardadoUnico.guardar(barrilRepository, barrilEntity,
                Map.of(INDICE_IDENTIFICADOR, () -> identificadorDuplicadoEnModificacion(identificador))));
    }

    /**
     * Procesa la baja lógica de un barril existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al barril con {@link Estado#BAJA} y persiste el
     * cambio. Solo se puede dar de baja un barril disponible: uno con cerveza, despachado, en
     * limpieza o en mantenimiento todavía forma parte de un ciclo de vida en curso, por eso se
     * valida esa condición antes de aplicar la baja.
     * </p>
     *
     * @param id Identificador clave primaria del barril a dar de baja.
     * @return {@link BarrilResponseDTO} con los datos del barril ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el barril con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el barril no se encuentra en estado operativo DISPONIBLE.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO bajaBarril(Long id) {
        // 1. Buscamos el barril. Si no existe, se dispara RecursoNoEncontradoException
        BarrilEntity barrilEntity = barrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id));

        // 2. Validar que el barril se encuentre DISPONIBLE
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DISPONIBLE) {
            throw new ReglaNegocioException("No se puede dar de baja el barril porque no se encuentra disponible");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        barrilEntity.setEstado(Estado.BAJA);

        // 4. Retornamos el DTO del barril dado de baja
        return MapperBarril.toDTO(barrilRepository.save(barrilEntity));
    }

    /**
     * Valida que la capacidad sea mayor a 0.
     *
     * @param capacidad Capacidad a validar.
     * @throws ReglaNegocioException Si la capacidad es nula o menor o igual a 0.
     */
    private static void validarCapacidad(Double capacidad) {
        if (capacidad == null) {
            throw new ReglaNegocioException("La capacidad es obligatoria");
        }
        if (capacidad <= 0) {
            throw new ReglaNegocioException("La capacidad debe ser mayor a 0");
        }
    }

    /**
     * Valida que los usos máximos antes de mantenimiento sean mayores a 0.
     *
     * @param usosMaximosAntesMantenimiento Valor a validar.
     * @throws ReglaNegocioException Si es nulo o menor o igual a 0.
     */
    private static void validarUsosMaximosAntesMantenimiento(Integer usosMaximosAntesMantenimiento) {
        if (usosMaximosAntesMantenimiento == null) {
            throw new ReglaNegocioException("Los usos máximos antes de mantenimiento son obligatorios");
        }
        if (usosMaximosAntesMantenimiento <= 0) {
            throw new ReglaNegocioException("Los usos máximos antes de mantenimiento deben ser mayores a 0");
        }
    }

    /**
     * Valida que el identificador normalizado sea obligatorio y no supere la longitud máxima.
     *
     * @param identificador Identificador normalizado.
     * @throws ReglaNegocioException Si el identificador está vacío o supera los 100 caracteres.
     */
    private static void validarIdentificador(String identificador) {
        if (identificador == null || identificador.isEmpty()) {
            throw new ReglaNegocioException("El identificador es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(identificador, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El identificador");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnAlta(String identificador) {
        return new RecursoDuplicadoException("Ya existe un barril con el identificador '" + identificador + "' para el fabricante seleccionado");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnModificacion(String identificador) {
        return new RecursoDuplicadoException("El identificador '" + identificador + "' ya está en uso por otro barril del fabricante seleccionado");
    }
}
