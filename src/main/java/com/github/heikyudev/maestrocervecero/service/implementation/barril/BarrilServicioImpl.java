package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
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
import com.github.heikyudev.maestrocervecero.util.method.GeneradorIdentificador;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
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
 * Un barril puede repetir número de serie entre fabricantes distintos: la unicidad del
 * número de serie de un barril es relativa a su fabricante.
 * </p>
 * <p>
 * El identificador interno (BRL-01, BRL-02...) lo genera el sistema al dar de alta con una secuencia de la base de
 * datos y no se modifica. Las secuencias no son transaccionales: si el alta se revierte después de pedir el número, ese
 * número no se reutiliza y queda un hueco en la numeración. Por eso el número se pide como último paso, después de
 * todas las validaciones.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class BarrilServicioImpl implements IBarrilServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos barriles activos del mismo fabricante con el mismo número de serie.
     */
    private static final String INDICE_NUMERO_SERIE = "udx_barril_numero_serie";

    private final IBarrilRepository barrilRepository;
    private final IFabricanteBarrilRepository fabricanteBarrilRepository;
    private final ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;

    /**
     * Recupera una página de barriles activos, filtrados opcionalmente por identificador interno, número de serie,
     * capacidad, estado operativo y/o fabricante.
     * <p>
     * Los barriles dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param numeroSerie Texto a buscar dentro del número de serie, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idFabricante El ID del fabricante a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link BarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BarrilResponseDTO> filtrarBarriles(String identificadorInterno, String numeroSerie, Double capacidad, EstadoOperativoBarril estadoOperativo, Long idFabricante, Pageable pageable) {
        // El identificador interno buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        // El número de serie buscado se normaliza como el guardado: sin espacios de más y conservando las mayúsculas
        String numeroSerieNormalizado = NormalizacionTexto.nombrePropio(numeroSerie);
        if (numeroSerieNormalizado != null && numeroSerieNormalizado.isEmpty()) {
            numeroSerieNormalizado = null;
        }
        return barrilRepository.filtrarBarriles(identificadorInternoNormalizado, numeroSerieNormalizado, capacidad, estadoOperativo, idFabricante, pageable).map(MapperBarril::toDTO);
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
     * Normaliza el número de serie (sin espacios en los extremos ni repetidos, conservando las mayúsculas), valida que sea obligatorio y no supere los 255 caracteres,
     * que la capacidad sea mayor a 0 y no supere {@link LimitesTecnicos#CAPACIDAD_BARRIL_MAXIMA_LITROS} litros, que los usos máximos antes de mantenimiento sean mayores a 0 y no superen {@link LimitesTecnicos#USOS_MAXIMOS_MANTENIMIENTO_MAXIMO}, que el fabricante de barril indicado exista y esté
     * activo, y que ningún otro barril activo de ese mismo fabricante tenga ya el número de serie
     * indicado. Con todo validado, genera el identificador interno (BRL-NN) y guarda el barril.
     * </p>
     *
     * @param barrilFormDTO Objeto DTO que contiene los datos de creación del barril.
     * @return {@link BarrilResponseDTO} representativo del barril guardado en la base de datos.
     * @throws ReglaNegocioException Si el número de serie falta o supera los 255 caracteres, si la capacidad es nula, menor o igual a 0 o supera {@link LimitesTecnicos#CAPACIDAD_BARRIL_MAXIMA_LITROS} litros, o si los usos máximos antes de mantenimiento son nulos, menores o iguales a 0 o superan {@link LimitesTecnicos#USOS_MAXIMOS_MANTENIMIENTO_MAXIMO}.
     * @throws RecursoNoEncontradoException Si no existe un fabricante de barril activo con el ID indicado.
     * @throws RecursoDuplicadoException Si otro barril activo del mismo fabricante ya tiene ese número de serie.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO altaBarril(BarrilFormDTO barrilFormDTO) {
        // 1. Normalizar el número de serie sin modificar el DTO (se conservan las mayúsculas)
        String numeroSerie = NormalizacionTexto.nombrePropio(barrilFormDTO.getNumeroSerie());

        // 2. Validar que el número de serie sea obligatorio y no supere la longitud máxima
        validarNumeroSerie(numeroSerie);

        // 3. Validar que la capacidad sea mayor a 0 y no supere el máximo técnico
        validarCapacidad(barrilFormDTO.getCapacidad());

        // 4. Validar que los usos máximos antes de mantenimiento sean válidos
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());

        // 5. Validar que el fabricante de barril indicado exista y esté activo
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(barrilFormDTO.getIdFabricanteBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + barrilFormDTO.getIdFabricanteBarril()));

        // 6. Validar que no haya otro barril activo del mismo fabricante con el mismo número de serie
        if (barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId(numeroSerie, barrilFormDTO.getIdFabricanteBarril())) {
            throw numeroSerieDuplicado(numeroSerie);
        }

        // 7. Generar el identificador interno: es el último paso antes de guardar, para que un error de validación no consuma un número de la secuencia
        String identificadorInterno = GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_BARRIL, secuenciaIdentificadorRepository.siguienteNumeroBarril());

        // 8. Creo la entidad que se va a almacenar en la base de datos
        BarrilEntity barrilEntity = BarrilEntity.builder()
                .identificadorInterno(identificadorInterno)
                .numeroSerie(numeroSerie)
                .capacidad(barrilFormDTO.getCapacidad())
                .contenidoActual(0.0)
                .estadoOperativo(EstadoOperativoBarril.DISPONIBLE)
                .usosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento())
                .estado(Estado.ACTIVO)
                .fabricante(fabricanteBarrilEntity)
                .build();

        // 9. Guardo la entidad en la base de datos (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y devuelvo el DTO correspondiente
        return MapperBarril.toDTO(GuardadoUnico.guardar(barrilRepository, barrilEntity,
                Map.of(INDICE_NUMERO_SERIE, () -> numeroSerieDuplicado(numeroSerie))));
    }

    /**
     * Actualiza la información de un barril existente en la base de datos.
     * <p>
     * Localiza el barril por su ID bloqueándolo mientras se valida y se escribe (un envasado o despacho simultáneo no puede cambiarlo entre ambos pasos), normaliza el número de serie (sin espacios de más, conservando las mayúsculas), valida que sea obligatorio y no supere los 255 caracteres, que la capacidad sea mayor a 0 y no sea menor al
     * contenido actual del barril, que los usos máximos antes de mantenimiento sean válidos, que
     * el fabricante de barril indicado exista y esté activo (el fabricante puede reasignarse al
     * modificar), y que ningún otro barril activo de ese fabricante tenga ya el número de serie
     * indicado, permitiendo conservar el propio número de serie actual. El identificador interno no se modifica.
     * </p>
     *
     * @param id Identificador clave primaria del barril a modificar.
     * @param barrilFormDTO DTO con la información actualizada.
     * @return {@link BarrilResponseDTO} representativo del barril con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza un barril activo por el ID proporcionado, o si no existe un fabricante de barril activo con el ID indicado.
     * @throws ReglaNegocioException Si el número de serie falta o supera los 255 caracteres, si la capacidad es nula, menor o igual a 0 o supera {@link LimitesTecnicos#CAPACIDAD_BARRIL_MAXIMA_LITROS} litros, si la capacidad es menor al contenido actual del barril, o si los usos máximos antes de mantenimiento son nulos, menores o iguales a 0 o superan {@link LimitesTecnicos#USOS_MAXIMOS_MANTENIMIENTO_MAXIMO}.
     * @throws RecursoDuplicadoException Si otro barril activo del mismo fabricante ya tiene ese número de serie.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.BARRIL)
    public BarrilResponseDTO modificarBarril(Long id, BarrilFormDTO barrilFormDTO) {
        // 1. Normalizar el número de serie sin modificar el DTO (se conservan las mayúsculas)
        String numeroSerie = NormalizacionTexto.nombrePropio(barrilFormDTO.getNumeroSerie());

        // 2. Localizar el barril existente. Si no existe, se dispara RecursoNoEncontradoException
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + id));

        // 3. Validar que el número de serie sea obligatorio y no supere la longitud máxima
        validarNumeroSerie(numeroSerie);

        // 4. Validar que la capacidad sea mayor a 0 y no supere el máximo técnico
        validarCapacidad(barrilFormDTO.getCapacidad());

        // 5. Validar que la nueva capacidad no sea menor al contenido actual del barril: reducirla
        //    por debajo de lo que ya tiene cargado dejaría al barril en un estado imposible
        if (barrilFormDTO.getCapacidad() < barrilEntity.getContenidoActual()) {
            throw new ReglaNegocioException("La capacidad no puede ser menor al contenido actual del barril (" + barrilEntity.getContenidoActual() + " L)");
        }

        // 6. Validar que los usos máximos antes de mantenimiento sean válidos
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());

        // 7. Validar que el fabricante de barril indicado exista y esté activo
        FabricanteBarrilEntity fabricanteBarrilEntity = fabricanteBarrilRepository.findById(barrilFormDTO.getIdFabricanteBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: " + barrilFormDTO.getIdFabricanteBarril()));

        // 8. Validar duplicación excluyendo el propio barril, de modo que conservar el
        //    número de serie actual no falle contra el mismo registro
        if (barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot(numeroSerie, barrilFormDTO.getIdFabricanteBarril(), id)) {
            throw numeroSerieDuplicado(numeroSerie);
        }

        // 9. Aplico los cambios sobre la entidad administrada por persistencia
        barrilEntity.setNumeroSerie(numeroSerie);
        barrilEntity.setCapacidad(barrilFormDTO.getCapacidad());
        barrilEntity.setUsosMaximosAntesMantenimiento(barrilFormDTO.getUsosMaximosAntesMantenimiento());
        barrilEntity.setFabricante(fabricanteBarrilEntity);

        // 10. Persisto la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //     y devuelvo el DTO correspondiente
        return MapperBarril.toDTO(GuardadoUnico.guardar(barrilRepository, barrilEntity,
                Map.of(INDICE_NUMERO_SERIE, () -> numeroSerieDuplicado(numeroSerie))));
    }

    /**
     * Procesa la baja lógica de un barril existente en el sistema.
     * <p>
     * Localiza el barril bloqueándolo mientras se valida y se escribe, de modo que un envasado o despacho simultáneo no cambie su estado operativo entre ambos pasos. En lugar de eliminar el registro, marca al barril con {@link Estado#BAJA} y persiste el
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
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(id)
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
     * Valida que la capacidad sea un número mayor a 0 que no supere {@link LimitesTecnicos#CAPACIDAD_BARRIL_MAXIMA_LITROS} litros.
     *
     * @param capacidad Capacidad a validar.
     * @throws ReglaNegocioException Si la capacidad es nula, no es un número mayor a 0 o supera el máximo.
     */
    private static void validarCapacidad(Double capacidad) {
        if (capacidad == null) {
            throw new ReglaNegocioException("La capacidad es obligatoria");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(capacidad,
                LimitesTecnicos.CAPACIDAD_BARRIL_MAXIMA_LITROS, "La capacidad", "litros");
    }

    /**
     * Valida que el número de serie normalizado sea obligatorio y no supere la longitud máxima.
     *
     * @param numeroSerie Número de serie normalizado.
     * @throws ReglaNegocioException Si el número de serie está vacío o supera los 255 caracteres.
     */
    private static void validarNumeroSerie(String numeroSerie) {
        if (numeroSerie == null || numeroSerie.isEmpty()) {
            throw new ReglaNegocioException("El número de serie es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(numeroSerie, LimitesTecnicos.LONGITUD_MAXIMA_NUMERO_SERIE, "El número de serie");
    }

    private static RecursoDuplicadoException numeroSerieDuplicado(String numeroSerie) {
        return new RecursoDuplicadoException("Ya existe un barril con el número de serie '" + numeroSerie + "' para el fabricante seleccionado", "numeroSerie");
    }
}
