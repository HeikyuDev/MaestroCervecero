package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EscaladoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional.ICostoDirectoAdicionalRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEscaladoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.VolumenesAgua;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AguaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperEtapaLote;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperLote;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperLoteCronograma;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.text.Normalizer;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación de {@link ILoteServicio}.
 * <p>
 * Los volúmenes del escalado de agua (documento de dominio "Escalado del Agua") los calcula
 * {@link EscaladoAgua}: al registrar el lote se usan los Pasos 2 y 3 para validar la capacidad de la olla de hervor
 * y del macerador, y {@link #calcularAguaDelLote(Long)} informa el agua de macerado, el agua de lavado y el agua
 * total (Pasos 3 a 5).
 * </p>
 * <h3>Orden canónico de bloqueo</h3>
 * <p>
 * Toda operación que bloquea filas para escritura lo hace en este único orden global (se pueden omitir niveles, pero
 * nunca volver a uno anterior), de modo que dos transacciones nunca se esperen mutuamente:
 * </p>
 * <ol>
 *     <li>planificación de producción;</li>
 *     <li>fermentador, que actúa como exclusión mutua de su cola de lotes pendientes;</li>
 *     <li>cola de lotes pendientes del fermentador, por fecha de inicio estimada e ID;</li>
 *     <li>el lote (por ID);</li>
 *     <li>los equipos del lote: molino, macerador, olla de hervor y fermentador;</li>
 *     <li>los lotes de insumo, siempre por ID ascendente.</li>
 * </ol>
 * <p>
 * {@link OrdenCanonicoDeBloqueo} cubre los cuatro primeros niveles. Como la entidad se lee recién después de
 * bloquear, nunca se opera con datos anteriores al bloqueo; los lotes de insumo, que otras operaciones pueden haber
 * cargado antes en la misma transacción, se vuelven a leer tras bloquearlos ({@code liberarReservas}).
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LoteServicioImpl implements ILoteServicio {

    private static final int HORAS_POR_DIA = 24;

    // Nombre que usa el código del lote cuando el nombre de la versión de receta no deja ningún carácter utilizable
    private static final String NOMBRE_POR_DEFECTO_DEL_CODIGO = "LOTE";

    // Separación entre dos lotes consecutivos de un fermentador: el día fin + 1 es el de limpieza y el siguiente lote
    // arranca como mínimo el día fin + 2
    private static final int DIAS_MARGEN_LIMPIEZA = 2;

    // Días que necesitan los equipos de un lote que estaba en ejecución (finalizado o cancelado) para su limpieza:
    // los pendientes del fermentador no pueden arrancar antes de hoy + 1
    private static final int DIAS_LIMPIEZA_TRAS_LOTE_EN_EJECUCION = 1;

    private static final String MENSAJE_SOLO_MOVER_PENDIENTES = "Solo se pueden mover lotes en estado Pendiente";
    private static final String MENSAJE_LOTE_YA_ES_PRIMERO_DE_LA_COLA = "El lote ya es el primero de la cola pendiente de su fermentador";
    private static final String MENSAJE_LOTE_YA_ES_ULTIMO_DE_LA_COLA = "El lote ya es el último de la cola pendiente de su fermentador";

    // Estados de lote que todavía ocupan un lugar en el cronograma de un fermentador
    private static final List<EstadoLote> ESTADOS_QUE_OCUPAN_FERMENTADOR = List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION);

    private final IEscaladoInsumoServicio escaladoInsumoServicio;
    private final IEscaladoLoteRepository escaladoLoteRepository;
    private final ILoteRepository loteRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IMolinoRepository molinoRepository;
    private final IMaceradorRepository maceradorRepository;
    private final IOllaHervorRepository ollaHervorRepository;
    private final IFermentadorRepository fermentadorRepository;
    private final IRecetaRepository recetaRepository;
    private final IConfiguracionProduccionRepository configuracionProduccionRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IReservaInsumoRepository reservaInsumoRepository;
    private final IConsumoInsumoServicio consumoInsumoServicio;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final ICostoDirectoAdicionalRepository costoDirectoAdicionalRepository;
    private final IPlanificacionProduccionServicio planificacionProduccionServicio;
    private final EntityManager entityManager;
    private final OrdenCanonicoDeBloqueo ordenCanonicoDeBloqueo;

    /**
     * Filtra los lotes registrados, opcionalmente por receta, identificador interno, estado,
     * etapa actualmente en curso y volumen objetivo.
     *
     * @param idReceta El ID de la receta a filtrar, o {@code null} para no filtrar por ella.
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estado El estado del lote a filtrar, o {@code null} para no filtrar por él.
     * @param etapaActual El tipo de etapa actualmente en curso a filtrar, o {@code null} para no filtrar por ella.
     * @param volumenObjetivo El volumen objetivo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LoteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LoteResponseDTO> filtrarLotes(Long idReceta, String identificadorInterno, EstadoLote estado, TipoEtapa etapaActual, Double volumenObjetivo, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        return loteRepository.filtrarLotes(idReceta, identificadorInternoNormalizado, estado, etapaActual, volumenObjetivo, pageable)
                .map(MapperLote::toDTO);
    }

    /**
     * Busca y retorna un lote mediante su identificador único.
     *
     * @param id El ID del lote.
     * @return El lote correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LoteResponseDTO buscarPorId(Long id) {
        return MapperLote.toDTO(loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id)));
    }

    /**
     * Filtra los lotes para el listado de lotes, del último registrado al primero. Ver
     * {@link ILoteServicio#filtrarLotesParaListado(Long, Long, String, EstadoLote, Pageable)}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LoteCronogramaResponseDTO> filtrarLotesParaListado(Long idPlanificacion, Long idFermentador, String identificadorInterno, EstadoLote estado, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }

        // 1. Paginar solo los IDs, ya ordenados por la consulta (el orden del pageable se ignora: siempre es el mismo)
        Pageable pageableSinOrden = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Long> idsDeLaPagina = loteRepository.filtrarIdsDeLotesParaListado(idPlanificacion, idFermentador, identificadorInternoNormalizado, estado, pageableSinOrden);
        if (idsDeLaPagina.isEmpty()) {
            return new PageImpl<>(List.of(), pageableSinOrden, idsDeLaPagina.getTotalElements());
        }

        // 2. Cargar los lotes de la página con sus relaciones en una sola consulta y respetar el orden de los IDs
        Map<Long, LoteEntity> lotesPorId = loteRepository.buscarParaListadoPorIds(idsDeLaPagina.getContent()).stream()
                .collect(Collectors.toMap(LoteEntity::getId, lote -> lote));
        List<LoteCronogramaResponseDTO> contenido = idsDeLaPagina.getContent().stream()
                .map(lotesPorId::get)
                .filter(Objects::nonNull)
                .map(MapperLoteCronograma::toDTO)
                .toList();
        return new PageImpl<>(contenido, pageableSinOrden, idsDeLaPagina.getTotalElements());
    }

    /**
     * Obtiene el detalle de un lote para su pantalla. Ver {@link ILoteServicio#buscarDetallePorId(Long)}.
     */
    @Override
    @Transactional(readOnly = true)
    public LoteDetalleResponseDTO buscarDetallePorId(Long idLote) {
        validarLoteInformado(idLote);
        LoteEntity lote = loteRepository.buscarConEtapasParaDetalle(idLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + idLote));

        // Las etapas siempre en el orden del proceso, sin depender del orden de la lista de la entidad
        List<EtapaLoteEntity> etapas = lote.getEtapas().stream()
                .sorted(Comparator.comparing(EtapaLoteEntity::getEtapa))
                .toList();
        EtapaLoteEntity etapaFermentacion = etapas.stream()
                .filter(etapa -> etapa.getEtapa() == TipoEtapa.FERMENTACION)
                .findFirst()
                .orElse(null);
        Long idPlanificacion = lote.getPlanificacionProduccion().getId();

        return LoteDetalleResponseDTO.builder()
                .id(lote.getId())
                .identificadorInterno(lote.getIdentificadorInterno())
                .estado(lote.getEstado())
                .volumenObjetivo(lote.getVolumenObjetivo())
                .fechaInicioEstimada(lote.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(lote.getFechaFinalizacionEstimada())
                .fechaInicio(lote.getFechaInicio())
                .fechaFinalizacion(lote.getFechaFinalizacion())
                .fechaCancelacion(lote.getFechaCancelacion())
                .motivoCancelacion(lote.getMotivoCancelacion())
                .usuarioCancelacion(lote.getEstado() == EstadoLote.CANCELADO ? lote.getLastModifiedBy() : null)
                .idPlanificacionProduccion(idPlanificacion)
                .versionReceta(planificacionProduccionServicio.buscarVersionRecetaDePlanificacion(idPlanificacion))
                .idFermentador(etapaFermentacion != null ? etapaFermentacion.getEquipamiento().getId() : null)
                .identificadorInternoFermentador(etapaFermentacion != null ? etapaFermentacion.getEquipamiento().getIdentificadorInterno() : null)
                .etapas(etapas.stream().map(MapperEtapaLote::toDTO).toList())
                .etapaPorDefecto(resolverEtapaPorDefecto(lote.getEstado(), etapas))
                .build();
    }

    /**
     * Resuelve la etapa que el detalle abre por defecto según el estado del lote: la etapa en curso si está En
     * ejecución, Molienda si está Pendiente, Envasado si está Finalizado y, si está Cancelado, la primera etapa
     * Cancelada (donde se interrumpió) o, si ninguna lo está, la última etapa iniciada (Molienda si no empezó).
     *
     * @param estado El estado del lote.
     * @param etapasEnOrden Las etapas del lote en el orden del proceso.
     */
    private static TipoEtapa resolverEtapaPorDefecto(EstadoLote estado, List<EtapaLoteEntity> etapasEnOrden) {
        return switch (estado) {
            case PENDIENTE -> TipoEtapa.MOLIENDA;
            case FINALIZADO -> TipoEtapa.ENVASADO;
            case EN_EJECUCION -> etapasEnOrden.stream()
                    .filter(etapa -> etapa.getEstado() == EstadoEtapaLote.EN_CURSO)
                    .map(EtapaLoteEntity::getEtapa)
                    .findFirst()
                    .orElse(TipoEtapa.MOLIENDA);
            case CANCELADO -> etapasEnOrden.stream()
                    .filter(etapa -> etapa.getEstado() == EstadoEtapaLote.CANCELADA)
                    .map(EtapaLoteEntity::getEtapa)
                    .findFirst()
                    .orElseGet(() -> etapasEnOrden.stream()
                            .filter(etapa -> etapa.getFechaInicio() != null)
                            .map(EtapaLoteEntity::getEtapa)
                            .reduce((anterior, siguiente) -> siguiente)
                            .orElse(TipoEtapa.MOLIENDA));
        };
    }

    /**
     * Calcula el avance de una etapa hacia el porcentaje mínimo de consumo. Ver
     * {@link ILoteServicio#calcularAvanceDeEtapa(Long, TipoEtapa)}.
     */
    @Override
    @Transactional(readOnly = true)
    public AvanceEtapaResponseDTO calcularAvanceDeEtapa(Long idLote, TipoEtapa etapa) {
        validarLoteInformado(idLote);
        validarInformado(etapa, "La etapa es obligatoria");

        LoteEntity lote = loteRepository.findById(idLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + idLote));
        EtapaLoteEntity etapaLote = lote.obtenerEtapaPorTipo(etapa);
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        double porcentajeMinimo = configuracion.getPorcentajeMinimoConsumoParaAvanzarEtapa();

        List<AvanceInsumoResponseDTO> insumos = evaluarAvance(consumoInsumoServicio.filtrarInsumosRequeridos(etapaLote.getId()), porcentajeMinimo);
        return AvanceEtapaResponseDTO.builder()
                .porcentajeMinimo(porcentajeMinimo)
                .cumpleMinimo(insumos.stream().allMatch(AvanceInsumoResponseDTO::isCumple))
                .insumos(insumos)
                .build();
    }

    /**
     * Registra un nuevo lote, reservando su lugar en el cronograma de uso de equipamiento.
     *
     * @param loteFormDTO Los datos del lote a registrar.
     * @return El lote registrado.
     * @throws RecursoNoEncontradoException Si la planificación de producción o alguno de los equipamientos referenciados no existe.
     * @throws ReglaNegocioException Si el volumen objetivo no se informó o no es un número mayor a 0, si no se informó
     *                               la planificación de producción o algún equipamiento (estas validaciones se hacen antes
     *                               de consultar nada), si la planificación de producción no está en estado PENDIENTE, si
     *                               el volumen objetivo supera la capacidad útil del fermentador, si algún otro equipo
     *                               seleccionado no tiene capacidad suficiente, o si los lotes de la planificación (litros
     *                               ya producidos por sus lotes finalizados más el volumen objetivo de sus lotes pendientes
     *                               y en ejecución) ya cubren la cantidad a producir.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO registrarLote(LoteFormDTO loteFormDTO) {
        // 1. Validar los datos del formulario antes de consultar nada: el volumen objetivo (redondeado a la escala
        //    de las cantidades) y los IDs de la planificación y de los equipamientos
        double volumenObjetivo = validarVolumenObjetivoInformado(loteFormDTO.getVolumenObjetivo());
        validarInformado(loteFormDTO.getIdPlanificacionProduccion(), "La planificación de producción es obligatoria");
        validarInformado(loteFormDTO.getIdMolino(), "El molino es obligatorio");
        validarInformado(loteFormDTO.getIdMacerador(), "El macerador es obligatorio");
        validarInformado(loteFormDTO.getIdOllaHervor(), "La olla de hervor es obligatoria");
        validarInformado(loteFormDTO.getIdFermentador(), "El fermentador es obligatorio");

        // 2. Validar que la planificación de producción esté registrada y en estado PENDIENTE
        //    (se bloquea para escritura: dos lotes registrados en simultáneo sobre la misma planificación no deben poder pasar ambos)
        PlanificacionProduccionEntity planificacionProduccion = planificacionProduccionRepository.buscarPorIdParaActualizar(loteFormDTO.getIdPlanificacionProduccion())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + loteFormDTO.getIdPlanificacionProduccion()));

        if (planificacionProduccion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("La planificación de producción debe encontrarse en estado Pendiente para poder registrar un lote");
        }

        // 3. Validar que los equipamientos seleccionados estén registrados en el sistema
        MolinoEntity molino = molinoRepository.findById(loteFormDTO.getIdMolino())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + loteFormDTO.getIdMolino()));

        MaceradorEntity macerador = maceradorRepository.findById(loteFormDTO.getIdMacerador())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + loteFormDTO.getIdMacerador()));

        OllaHervorEntity ollaHervor = ollaHervorRepository.findById(loteFormDTO.getIdOllaHervor())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + loteFormDTO.getIdOllaHervor()));

        FermentadorEntity fermentador = fermentadorRepository.findById(loteFormDTO.getIdFermentador())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + loteFormDTO.getIdFermentador()));

        // 4. Validar que el volumen objetivo del lote no supere la capacidad útil del fermentador (Paso 1 del escalado
        //    de agua: verificación del fermentador)
        validarVolumenObjetivoContraFermentador(volumenObjetivo, fermentador);

        VersionRecetaEntity versionReceta = planificacionProduccion.getVersionReceta();
        double masaMaltaEscalada = calcularMasaMaltaEscalada(volumenObjetivo, macerador, versionReceta);

        // 5. Validar el volumen pre-hervor contra la capacidad de la olla de hervor (Paso 2 del escalado de agua)
        double volumenPreHervor = Redondeo.redondear(EscaladoAgua.calcularVolumenPreHervor(volumenObjetivo, ollaHervor, versionReceta));
        if (volumenPreHervor > Redondeo.redondear(ollaHervor.getCapacidadUtil())) {
            throw new ReglaNegocioException("El volumen pre-hervor calculado (" + ValidacionesNumericas.formatearNumero(volumenPreHervor)
                    + " L) supera la capacidad útil de la olla de hervor seleccionada ("
                    + ValidacionesNumericas.formatearNumero(ollaHervor.getCapacidadUtil()) + " L)");
        }

        // 6. Validar el volumen de la mezcla de agua y malta contra la capacidad del macerador (Paso 3 del escalado de agua)
        double volumenMezclaMacerador = Redondeo.redondear(EscaladoAgua.calcularVolumenMezclaMacerador(masaMaltaEscalada, macerador, versionReceta));
        if (volumenMezclaMacerador > Redondeo.redondear(macerador.getCapacidadUtil())) {
            throw new ReglaNegocioException("La mezcla de agua y grano (" + ValidacionesNumericas.formatearNumero(volumenMezclaMacerador)
                    + " L) desbordará el macerador seleccionado (capacidad útil: "
                    + ValidacionesNumericas.formatearNumero(macerador.getCapacidadUtil()) + " L)");
        }

        // 7. Validar que la planificación todavía necesite lotes: lo ya producido por los lotes finalizados
        //    más el volumen objetivo de los lotes vivos (sin contar el lote nuevo) no debe alcanzar la cantidad a producir
        validarPlanificacionNecesitaMasLotes(planificacionProduccion);

        // 8. Generar el identificador interno del lote, incrementando de forma segura el contador de la receta
        String identificadorInterno = generarIdentificadorInterno(versionReceta);

        // 9. Calcular la planificación del lote en el cronograma del fermentador seleccionado (nunca antes de
        //    la fecha de inicio estimada de su planificación de producción ni antes de hoy)
        LocalDate fechaInicioEstimada = calcularFechaInicioEstimada(fermentador, planificacionProduccion);
        LocalDate fechaFinalizacionEstimada = calcularFechaFinalizacionEstimada(fechaInicioEstimada, volumenObjetivo, molino, masaMaltaEscalada, versionReceta);

        // 10. Construir el lote y sus 6 etapas (todas en PENDIENTE), y persistir
        LoteEntity loteEntity = LoteEntity.builder()
                .identificadorInterno(identificadorInterno)
                .volumenObjetivo(volumenObjetivo)
                .estado(EstadoLote.PENDIENTE)
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionEstimada(fechaFinalizacionEstimada)
                .planificacionProduccion(planificacionProduccion)
                .build();

        loteEntity.setEtapas(crearEtapasPendientes(loteEntity, molino, macerador, ollaHervor, fermentador));

        return MapperLote.toDTO(loteRepository.save(loteEntity));
    }

    /**
     * Inicia un lote previamente registrado, dando paso a su ejecución real.
     * <p>
     * Sus fechas estimadas pasan a ser las reales (inicia hoy y conserva su duración estimada) y las
     * fechas estimadas de los lotes pendientes del fermentador se reencadenan a continuación.
     * </p>
     *
     * @param id El ID del lote a iniciar.
     * @return El lote iniciado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado PENDIENTE, si no hay
     *                               stock suficiente de algún insumo, o si algún equipamiento
     *                               requerido no está disponible.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO iniciarLote(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura en el orden canónico
        //    (fermentador, cola pendiente y lote)
        LoteEntity lote = ordenCanonicoDeBloqueo.bloquearLote(id, false);

        // 2. Validar que el lote se encuentre en estado PENDIENTE
        if (lote.getEstado() != EstadoLote.PENDIENTE) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado Pendiente para poder iniciarse");
        }

        // 3. Bloquear y validar que el equipamiento asociado al lote se encuentre DISPONIBLE
        EquipamientoDelLote equipamiento = bloquearYValidarEquipamientoDisponible(lote);

        // 3 bis. Guardar la instantánea del escalado: los parámetros de los equipos recién bloqueados y de los insumos de
        //    la receta. Desde acá todo cálculo del lote (cantidades requeridas, agua, topes de capacidad) usa estos
        //    valores y no los vigentes, así que editar un equipo o un insumo después no cambia las cantidades del lote
        EscaladoLoteEntity escalado = escaladoInsumoServicio.capturarEscalado(lote.getPlanificacionProduccion().getVersionReceta(),
                equipamiento.molino(), equipamiento.macerador(), equipamiento.ollaHervor(), equipamiento.fermentador());
        escaladoLoteRepository.save(escalado);
        lote.setEscaladoLote(escalado);

        // 4. Calcular la cantidad requerida de cada insumo, escalada al volumen objetivo del lote,
        //    y asociarla a la etapa donde efectivamente se va a usar: Molienda para la malta,
        //    Fermentación para la levadura, y la etapa configurada en cada detalle para el lúpulo
        //    (HERVIDO para HERVOR/WHIRLPOOL, FERMENTACION o MADURACION para DRY_HOP).
        List<RequerimientoInsumo> requerimientos = escaladoInsumoServicio.calcularRequerimientosTotales(lote);

        // 5. Validar que el stock disponible de cada insumo alcance la cantidad escalada (sumada
        //    entre todas las etapas que lo requieran), antes de reservar nada
        Map<Long, List<LoteInsumoEntity>> stockPorInsumo = obtenerYValidarStockDisponible(requerimientos);

        // 6. Reservar la cantidad escalada de cada insumo aplicando la regla FEFO
        List<ReservaInsumoEntity> reservas = reservarInsumosFEFO(requerimientos, stockPorInsumo);
        reservaInsumoRepository.saveAll(reservas);

        // 7 y 8. Registrar la fecha y hora de inicio general del lote, y cambiar su estado a EN_EJECUCION.
        //    Sus fechas estimadas pasan a ser las reales: arranca hoy y conserva la duración con la que se planificó.
        long duracionEstimadaEnDias = calcularDuracionEnDias(lote);
        LocalDate hoy = LocalDate.now();
        lote.setFechaInicio(LocalDateTime.now());
        lote.setEstado(EstadoLote.EN_EJECUCION);
        lote.setFechaInicioEstimada(hoy);
        lote.setFechaFinalizacionEstimada(hoy.plusDays(duracionEstimadaEnDias));

        // 9. Asignar a cada equipamiento asociado al lote el estado EN_USO
        equipamiento.molino().setEstadoOperativo(EstadoOperativo.EN_USO);
        equipamiento.macerador().setEstadoOperativo(EstadoOperativo.EN_USO);
        equipamiento.ollaHervor().setEstadoOperativo(EstadoOperativo.EN_USO);
        equipamiento.fermentador().setEstadoOperativo(EstadoOperativo.EN_USO);
        molinoRepository.save(equipamiento.molino());
        maceradorRepository.save(equipamiento.macerador());
        ollaHervorRepository.save(equipamiento.ollaHervor());
        fermentadorRepository.save(equipamiento.fermentador());

        // 10. Marcar la etapa de Molienda como EN_CURSO y registrar su fecha y hora de inicio
        lote.getEtapas().stream()
                .filter(etapa -> etapa.getEtapa() == TipoEtapa.MOLIENDA)
                .findFirst()
                .ifPresent(etapaMolienda -> {
                    etapaMolienda.setEstado(EstadoEtapaLote.EN_CURSO);
                    etapaMolienda.setFechaInicio(LocalDateTime.now());
                });

        // 11. Persistir el lote y volcarlo, para que la cola pendiente del fermentador ya no lo incluya
        LoteEntity loteIniciado = loteRepository.save(lote);
        loteRepository.flush();

        // 12. Reencadenar los lotes pendientes del fermentador a continuación del lote recién iniciado
        reencadenarColaPendienteDeFermentador(equipamiento.fermentador().getId(), false);

        return MapperLote.toDTO(loteIniciado);
    }

    /**
     * Cancela un lote en curso.
     * <p>
     * No revierte los consumos ni las etapas ya FINALIZADA: quedan como registro histórico. Las
     * etapas PENDIENTE o EN_CURSO pasan a CANCELADA (con {@code fechaFinalizacion} igual a la
     * fecha de cancelación del lote), y el equipamiento asociado a cada una se libera según
     * corresponda (ver {@link #actualizarEquipamientoAlCancelar}).
     * </p>
     * <p>
     * Un lote con envasados en estado REGISTRADO no se puede cancelar: primero deben anularse esos
     * envasados. Los lotes cancelados no suman litros producidos a su planificación.
     * </p>
     * <p>
     * Las fechas estimadas de los lotes pendientes del fermentador del lote se reencadenan.
     * </p>
     * <p>
     * Si la planificación de producción del lote ya había alcanzado su cantidad a producir y este era
     * su último lote pendiente o en ejecución, la planificación queda finalizada.
     * </p>
     *
     * @param id El ID del lote a cancelar.
     * @param cancelacionLoteFormDTO Los datos de la cancelación (motivo).
     * @return El lote cancelado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si no se informó el lote, si el motivo de cancelación no fue informado, si el lote no
     *                               se encuentra en estado PENDIENTE o EN_EJECUCION, o si el lote tiene
     *                               envasados en estado REGISTRADO.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO cancelarLote(Long id, CancelacionLoteFormDTO cancelacionLoteFormDTO) {
        validarLoteInformado(id);

        // 1. Validar que se haya informado el motivo de cancelación
        String motivoCancelacion = NormalizacionTexto.nombrePropio(cancelacionLoteFormDTO.getMotivoCancelacion());
        if (motivoCancelacion == null || motivoCancelacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de cancelación es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoCancelacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de cancelación");

        // 2. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura en el orden canónico
        //    (planificación, fermentador, cola pendiente y lote)
        LoteEntity lote = ordenCanonicoDeBloqueo.bloquearLote(id, true);

        // 3. Validar que el lote se encuentre en un estado que permita cancelarlo
        if (lote.getEstado() != EstadoLote.PENDIENTE && lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado Pendiente o En ejecución para poder cancelarse");
        }
        boolean estabaEnEjecucion = lote.getEstado() == EstadoLote.EN_EJECUCION;

        // 4. Validar que el lote no tenga envasados registrados: la cerveza ya está en los barriles, así que primero
        //    deben anularse esos envasados
        if (envasadoLoteRepository.existsByEtapaLote_Lote_IdAndEstado(lote.getId(), EstadoTransaccion.REGISTRADO)) {
            throw new ReglaNegocioException("No se puede cancelar el lote porque tiene envasados registrados, anule los envasados para poder cancelarlo");
        }

        // 5. Liberar todas las reservas de insumo del lote todavía pendientes. Cancelar NO revierte
        //    los consumos ya ejecutados: hoy no existe ninguna forma de consumir un insumo sin pasar
        //    antes por una reserva, así que toda reserva de un lote PENDIENTE/EN_EJECUCION representa
        //    stock que todavía no se consumió.
        liberarReservasDelLote(lote);

        // 6. Si el lote estaba en ejecución, actualizar el estado del equipamiento según el estado de
        //    cada etapa (antes de cancelar las etapas en sí, ya que esta resolución se basa en su estado
        //    PENDIENTE/EN_CURSO/FINALIZADA original): el equipamiento de la etapa EN_CURSO pasa a
        //    EN_LIMPIEZA, el de las etapas PENDIENTE pasa a DISPONIBLE, y el de las etapas ya FINALIZADA
        //    mantiene su estado actual. Un lote PENDIENTE todavía no ocupó ningún equipamiento, así que
        //    cancelarlo no altera el estado operativo de ninguno.
        if (estabaEnEjecucion) {
            actualizarEquipamientoAlCancelar(lote);
        }

        // 7. Cancelar el lote
        LocalDateTime fechaCancelacion = LocalDateTime.now();
        lote.setEstado(EstadoLote.CANCELADO);
        lote.setFechaCancelacion(fechaCancelacion);
        lote.setMotivoCancelacion(motivoCancelacion);

        // 8. Cancelar las etapas que todavía no habían finalizado, como registro histórico del
        //    punto donde se interrumpió el lote. Las etapas ya FINALIZADA no se tocan.
        cancelarEtapasNoFinalizadas(lote, fechaCancelacion);

        LoteEntity loteCancelado = loteRepository.save(lote);
        loteRepository.flush();

        // 9. Reencadenar los lotes pendientes del fermentador del lote cancelado (si el fermentador quedó
        //    en ejecución, la cola arranca como mínimo mañana por el día de limpieza)
        reencadenarColaPendienteDeFermentador(obtenerIdFermentador(lote), estabaEnEjecucion);

        // 10. Si la planificación ya había cubierto su cantidad y este era su último lote vivo, se finaliza
        finalizarPlanificacionSiCorresponde(lote.getPlanificacionProduccion().getId());

        return MapperLote.toDTO(loteCancelado);
    }

    /**
     * Finaliza la etapa de Molienda del lote y da paso a la Maceración.
     * <p>
     * La malta se consume en Molienda, por lo que al finalizarla se exige el porcentaje mínimo de consumo y se
     * liberan las reservas de malta que hayan quedado sin consumir.
     * </p>
     *
     * @param id El ID del lote cuya Molienda se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado EN_EJECUCION, si su
     *                               etapa actual (EN_CURSO) no es Molienda, o si algún insumo requerido de la etapa no
     *                               alcanzó el porcentaje mínimo de consumo configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarMolienda(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura: serializa la finalización
        //    con los consumos, con la cancelación y con un segundo envío del mismo pedido
        LoteEntity lote = loteRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Molienda
        EtapaLoteEntity etapaMolienda = obtenerEtapaEnCursoValidando(lote, TipoEtapa.MOLIENDA);

        // 4. Validar que el consumo de cada insumo requerido de la etapa (la malta) alcance el porcentaje
        //    mínimo configurado
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        validarPorcentajeMinimoConsumido(etapaMolienda, configuracion.getPorcentajeMinimoConsumoParaAvanzarEtapa());

        // 5. Finalizar Molienda e iniciar Maceración
        EtapaLoteEntity etapaMaceracion = lote.obtenerEtapaPorTipo(TipoEtapa.MACERACION);
        avanzarEtapa(etapaMolienda, etapaMaceracion);

        // 6. El molino utilizado pasa a estado EN_LIMPIEZA
        Long idMolino = etapaMolienda.getEquipamiento().getId();
        MolinoEntity molino = desproxy(molinoRepository.buscarPorIdParaCambiarEstadoOperativo(idMolino), MolinoEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolino));
        molino.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        molinoRepository.save(molino);

        // 7. Liberar las reservas de insumo que hayan quedado sin consumir de la etapa de Molienda
        liberarReservasDeLaEtapa(etapaMolienda);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Maceración del lote y da paso al Hervido.
     * <p>
     * La Maceración no requiere insumos (la malta se consume en Molienda), por lo que no se valida el porcentaje
     * mínimo de consumo ni hay reservas que liberar.
     * </p>
     *
     * @param id El ID del lote cuya Maceración se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado EN_EJECUCION, o si su
     *                               etapa actual (EN_CURSO) no es Maceración.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarMaceracion(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura: serializa la finalización
        //    con los consumos, con la cancelación y con un segundo envío del mismo pedido
        LoteEntity lote = loteRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Maceración
        EtapaLoteEntity etapaMaceracion = obtenerEtapaEnCursoValidando(lote, TipoEtapa.MACERACION);

        // 4. Finalizar Maceración e iniciar Hervido
        EtapaLoteEntity etapaHervido = lote.obtenerEtapaPorTipo(TipoEtapa.HERVIDO);
        avanzarEtapa(etapaMaceracion, etapaHervido);

        // 5. El macerador utilizado pasa a estado EN_LIMPIEZA
        Long idMacerador = etapaMaceracion.getEquipamiento().getId();
        MaceradorEntity macerador = desproxy(maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(idMacerador), MaceradorEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMacerador));
        macerador.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        maceradorRepository.save(macerador);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Hervido del lote y da paso a la Fermentación.
     *
     * @param id El ID del lote cuyo Hervido se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Hervido, o si algún insumo requerido
     *                               de la etapa no alcanzó el porcentaje mínimo de consumo
     *                               configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarHervido(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura: serializa la finalización
        //    con los consumos, con la cancelación y con un segundo envío del mismo pedido
        LoteEntity lote = loteRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Hervido
        EtapaLoteEntity etapaHervido = obtenerEtapaEnCursoValidando(lote, TipoEtapa.HERVIDO);

        // 4. Validar que el consumo de cada insumo requerido de la etapa alcance el porcentaje
        //    mínimo configurado
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        validarPorcentajeMinimoConsumido(etapaHervido, configuracion.getPorcentajeMinimoConsumoParaAvanzarEtapa());

        // 5. Finalizar Hervido e iniciar Fermentación
        EtapaLoteEntity etapaFermentacion = lote.obtenerEtapaPorTipo(TipoEtapa.FERMENTACION);
        avanzarEtapa(etapaHervido, etapaFermentacion);

        // 6. La olla de hervor utilizada pasa a estado EN_LIMPIEZA
        Long idOllaHervor = etapaHervido.getEquipamiento().getId();
        OllaHervorEntity ollaHervor = desproxy(ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(idOllaHervor), OllaHervorEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervor));
        ollaHervor.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        ollaHervorRepository.save(ollaHervor);

        // 7. Liberar las reservas de insumo que hayan quedado sin consumir de la etapa de Hervido
        liberarReservasDeLaEtapa(etapaHervido);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Fermentación del lote y da paso a la Maduración.
     * <p>
     * A diferencia de las demás transiciones de etapa, no cambia el estado operativo de ningún
     * equipamiento: el fermentador es compartido por Fermentación, Maduración y Envasado, así que
     * sigue {@code EN_USO} sin interrupción hasta que termine la última de esas tres etapas.
     * </p>
     *
     * @param id El ID del lote cuya Fermentación se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Fermentación, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarFermentacion(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura: serializa la finalización
        //    con los consumos, con la cancelación y con un segundo envío del mismo pedido
        LoteEntity lote = loteRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Fermentación
        EtapaLoteEntity etapaFermentacion = obtenerEtapaEnCursoValidando(lote, TipoEtapa.FERMENTACION);

        // 4. Validar que el consumo de cada insumo requerido de la etapa alcance el porcentaje
        //    mínimo configurado
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        validarPorcentajeMinimoConsumido(etapaFermentacion, configuracion.getPorcentajeMinimoConsumoParaAvanzarEtapa());

        // 5. Finalizar Fermentación e iniciar Maduración (el fermentador, compartido por ambas
        //    etapas, sigue EN_USO sin cambios)
        EtapaLoteEntity etapaMaduracion = lote.obtenerEtapaPorTipo(TipoEtapa.MADURACION);
        avanzarEtapa(etapaFermentacion, etapaMaduracion);

        // 6. Liberar las reservas de insumo que hayan quedado sin consumir de la etapa de Fermentación
        liberarReservasDeLaEtapa(etapaFermentacion);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Maduración del lote y da paso al Envasado.
     * <p>
     * Igual que {@link #finalizarFermentacion(Long)}, no cambia el estado operativo de ningún
     * equipamiento: el fermentador es compartido por Fermentación, Maduración y Envasado, así que
     * sigue {@code EN_USO} sin interrupción hasta que termine el Envasado. La validación del
     * porcentaje mínimo de consumo también aplica aquí: es infrecuente que Maduración requiera
     * insumos, pero puede haberlos (por ejemplo, un lúpulo de Dry Hop).
     * </p>
     *
     * @param id El ID del lote cuya Maduración se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Maduración, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarMaduracion(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura: serializa la finalización
        //    con los consumos, con la cancelación y con un segundo envío del mismo pedido
        LoteEntity lote = loteRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Maduración
        EtapaLoteEntity etapaMaduracion = obtenerEtapaEnCursoValidando(lote, TipoEtapa.MADURACION);

        // 4. Validar que el consumo de cada insumo requerido de la etapa alcance el porcentaje
        //    mínimo configurado (infrecuente que haya alguno, pero puede haber un lúpulo de Dry Hop)
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        validarPorcentajeMinimoConsumido(etapaMaduracion, configuracion.getPorcentajeMinimoConsumoParaAvanzarEtapa());

        // 5. Finalizar Maduración e iniciar Envasado (el fermentador, compartido por ambas etapas,
        //    sigue EN_USO sin cambios)
        EtapaLoteEntity etapaEnvasado = lote.obtenerEtapaPorTipo(TipoEtapa.ENVASADO);
        avanzarEtapa(etapaMaduracion, etapaEnvasado);

        // 6. Liberar las reservas de insumo que hayan quedado sin consumir de la etapa de Maduración
        liberarReservasDeLaEtapa(etapaMaduracion);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Envasado del lote, dando por concluido el proceso productivo completo.
     * <p>
     * Envasado es la última etapa: no hay una siguiente a la que avanzar, así que no se usa
     * {@link #avanzarEtapa(EtapaLoteEntity, EtapaLoteEntity)}. Tampoco se valida porcentaje mínimo
     * de consumo ni se liberan reservas, porque en esta etapa no se registran consumos de insumo
     * (solo envasados).
     * </p>
     * <p>
     * Registra, por cada costo directo adicional activo, un {@link DetalleCostoDirectoEntity} que
     * congela su costo por litro vigente en este momento y calcula el subtotal sobre el total
     * efectivamente envasado (no el volumen objetivo planificado) — así queda un registro
     * histórico de qué costó este lote en particular y cómo se compone ese costo.
     * </p>
     *
     * @param id El ID del lote cuyo Envasado se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra el fermentador asociado.
     * @throws ReglaNegocioException Si no se informó el lote, si el lote no se encuentra en estado EN_EJECUCION, si su
     *                               etapa actual (EN_CURSO) no es Envasado, o si no tiene litros
     *                               envasados registrados.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarEnvasado(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema, bloqueándolo para escritura en el orden canónico
        //    (planificación, fermentador, cola pendiente y lote)
        LoteEntity lote = ordenCanonicoDeBloqueo.bloquearLote(id, true);

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Envasado
        EtapaLoteEntity etapaEnvasado = obtenerEtapaEnCursoValidando(lote, TipoEtapa.ENVASADO);

        // 4. Validar que el lote tenga litros envasados: un lote sin envasados no produjo nada y corresponde cancelarlo
        BigDecimal totalEnvasado = BigDecimal.valueOf(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(etapaEnvasado.getId()));
        if (totalEnvasado.signum() <= 0) {
            throw new ReglaNegocioException("No se puede finalizar el envasado de un lote sin litros envasados, si no se va a producir debe cancelarse el lote");
        }

        // 5. El fermentador utilizado pasa a estado EN_LIMPIEZA (concluyen las tres etapas que lo comparten)
        Long idFermentador = etapaEnvasado.getEquipamiento().getId();
        FermentadorEntity fermentador = desproxy(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentador), FermentadorEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentador));
        fermentador.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        fermentadorRepository.save(fermentador);

        // 6. Finalizar la etapa de Envasado: es la última, no hay una siguiente a la que avanzar
        etapaEnvasado.setEstado(EstadoEtapaLote.FINALIZADA);
        etapaEnvasado.setFechaFinalizacion(LocalDateTime.now());

        // 7. Finalizar el lote: todo el proceso productivo quedó completo
        lote.setEstado(EstadoLote.FINALIZADO);
        lote.setFechaFinalizacion(LocalDateTime.now());

        // 8. Registrar los costos directos adicionales del lote: uno por cada costo directo
        //    adicional activo, aplicando su costo por litro vigente sobre el total efectivamente envasado
        registrarCostosDirectosDelLote(lote, totalEnvasado);

        LoteEntity loteFinalizado = loteRepository.save(lote);
        loteRepository.flush();

        // 9. Reencadenar los lotes pendientes del fermentador: el lote estaba en ejecución y dejó sus equipos
        //    sucios, así que la cola arranca como mínimo mañana
        reencadenarColaPendienteDeFermentador(idFermentador, true);

        // 10. Si con este lote la planificación alcanzó su cantidad y no le quedan lotes vivos, se finaliza
        finalizarPlanificacionSiCorresponde(lote.getPlanificacionProduccion().getId());

        return MapperLote.toDTO(loteFinalizado);
    }

    /**
     * Calcula el agua que la planta debe preparar para un lote (Pasos 3 a 5 del escalado de agua): el agua de
     * macerado, el agua de lavado y el agua total, junto con el volumen pre-hervor. Es de solo lectura y sirve para
     * un lote en cualquier estado.
     *
     * @param id El ID del lote.
     * @return El agua del lote, en litros, con las advertencias del cálculo.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si no se informó el lote, si el equipamiento de la etapa de Maceración o de Hervido
     *                               no es un macerador o una olla de hervor, o si el cálculo produce un valor no válido.
     */
    @Override
    @Transactional(readOnly = true)
    public AguaLoteResponseDTO calcularAguaDelLote(Long id) {
        validarLoteInformado(id);

        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Resolver los parámetros del macerador y de la olla de hervor, y la masa de malta escalada con ellos. Si el lote
        //    ya se inició son los de su instantánea de escalado; si no, los vigentes de sus equipos
        VersionRecetaEntity versionReceta = lote.getPlanificacionProduccion().getVersionReceta();
        EscaladoLoteEntity escalado = lote.getEscaladoLote();
        double espacioMuerto;
        double perdidaPorTrub;
        double evaporacion;
        if (escalado != null) {
            espacioMuerto = escalado.getEspacioMuertoMacerador();
            perdidaPorTrub = escalado.getPerdidaPorTrubOlla();
            evaporacion = escalado.getEvaporacionOlla();
        } else {
            MaceradorEntity macerador = obtenerEquipamientoDeEtapa(lote, TipoEtapa.MACERACION, MaceradorEntity.class, "un macerador asociado");
            OllaHervorEntity ollaHervor = obtenerEquipamientoDeEtapa(lote, TipoEtapa.HERVIDO, OllaHervorEntity.class, "una olla de hervor asociada");
            espacioMuerto = macerador.getEspacioMuerto();
            perdidaPorTrub = ollaHervor.getPerdidaPorTrub();
            evaporacion = ollaHervor.getEvaporacion();
        }
        double masaMalta = escaladoInsumoServicio.calcularRequerimientosMalta(lote).stream()
                .mapToDouble(RequerimientoInsumo::cantidadRequerida)
                .sum();
        if (!Double.isFinite(masaMalta)) {
            throw new ReglaNegocioException("No se puede calcular el agua del lote porque los datos de la receta o de los equipos producen un valor no válido");
        }

        // 3. Calcular los volúmenes de agua
        VolumenesAgua agua = EscaladoAgua.calcular(lote.getVolumenObjetivo(), masaMalta, espacioMuerto, perdidaPorTrub, evaporacion, versionReceta);

        return AguaLoteResponseDTO.builder()
                .volumenObjetivo(lote.getVolumenObjetivo())
                .masaMalta(Redondeo.redondear(masaMalta))
                .volumenPreHervor(agua.volumenPreHervor())
                .aguaMacerado(agua.aguaMacerado())
                .aguaLavado(agua.aguaLavado())
                .aguaTotal(agua.aguaTotal())
                .relacionDeEmpaste(versionReceta.getRelacionDeEmpaste())
                .aguaPorRelacionDeEmpaste(agua.aguaPorRelacionDeEmpaste())
                .espacioMuerto(agua.espacioMuerto())
                .absorcionPorKg(agua.absorcionPorKg())
                .perdidaPorBagazo(agua.perdidaPorBagazo())
                .mostoExtraido(agua.mostoExtraido())
                .advertencias(agua.advertencias())
                .build();
    }

    /**
     * Obtiene el equipamiento de la etapa del tipo indicado, desenvolviéndolo si es un proxy de Hibernate de la clase
     * abstracta {@code EquipamientoEntity} (que no es instancia de su clase concreta).
     *
     * @throws ReglaNegocioException Si el equipamiento de la etapa no es del tipo esperado.
     */
    private <T extends EquipamientoEntity> T obtenerEquipamientoDeEtapa(LoteEntity lote, TipoEtapa tipoEtapa, Class<T> claseEquipamiento, String descripcion) {
        EtapaLoteEntity etapa = lote.obtenerEtapaPorTipo(tipoEtapa);
        Object equipamiento = Hibernate.unproxy(etapa.getEquipamiento());
        if (claseEquipamiento.isInstance(equipamiento)) {
            return claseEquipamiento.cast(equipamiento);
        }
        throw new ReglaNegocioException("La etapa de " + tipoEtapa.getEtiqueta() + " del lote " + lote.getId()
                + " no tiene " + descripcion + " como equipamiento");
    }

    /**
     * Valida que los lotes de la planificación todavía no cubran su cantidad a producir: los litros
     * ya producidos (envasados registrados de lotes finalizados) más el volumen objetivo de los lotes
     * pendientes y en ejecución deben ser menores a la cantidad solicitada.
     *
     * @throws ReglaNegocioException Si los litros comprometidos alcanzan o superan la cantidad a producir.
     */
    private void validarPlanificacionNecesitaMasLotes(PlanificacionProduccionEntity planificacionProduccion) {
        BigDecimal cantidadAProducir = BigDecimal.valueOf(planificacionProduccion.getCantidadAProducir());
        BigDecimal comprometido = BigDecimal.valueOf(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(planificacionProduccion.getId()))
                .add(BigDecimal.valueOf(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(planificacionProduccion.getId())));

        if (comprometido.compareTo(cantidadAProducir) >= 0) {
            throw new ReglaNegocioException("La planificación ya tiene lotes que cubren la cantidad a producir ("
                    + ValidacionesNumericas.formatearNumero(comprometido.doubleValue()) + " L comprometidos de "
                    + ValidacionesNumericas.formatearNumero(cantidadAProducir.doubleValue()) + " L solicitados)");
        }
    }

    /**
     * Finaliza la planificación de producción cuando ya cumplió su objetivo: sigue en estado PENDIENTE,
     * los litros producidos (envasados registrados de lotes finalizados) alcanzan la cantidad a producir
     * y no le quedan lotes pendientes ni en ejecución.
     * <p>
     * Debe invocarse con el lote que originó el cambio ya persistido y volcado ({@code flush}), para
     * que las consultas lo vean. La planificación se bloquea para escritura (ya lo está si el llamador bloqueó el
     * lote con su planificación).
     * </p>
     */
    private void finalizarPlanificacionSiCorresponde(Long idPlanificacionProduccion) {
        PlanificacionProduccionEntity planificacionProduccion = planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacionProduccion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacionProduccion));

        if (planificacionProduccion.getEstado() != EstadoSolicitud.PENDIENTE) {
            return;
        }

        BigDecimal litrosProducidos = BigDecimal.valueOf(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(idPlanificacionProduccion));
        BigDecimal cantidadAProducir = BigDecimal.valueOf(planificacionProduccion.getCantidadAProducir());
        if (litrosProducidos.compareTo(cantidadAProducir) < 0
                || loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(idPlanificacionProduccion, ESTADOS_QUE_OCUPAN_FERMENTADOR)) {
            return;
        }

        planificacionProduccion.setEstado(EstadoSolicitud.FINALIZADA);
        planificacionProduccion.setFechaFinalizacion(LocalDateTime.now());
        planificacionProduccion.setMotivoFinalizacion(null);
        planificacionProduccionRepository.save(planificacionProduccion);
    }

    /**
     * Registra, al finalizar el envasado de un lote, un {@link DetalleCostoDirectoEntity} por
     * cada costo directo adicional activo, congelando su {@code costoPorLitro} vigente en este
     * momento ({@code costoPorLitroAplicado}) y calculando el {@code subtotal} sobre el total
     * efectivamente envasado — no el volumen objetivo planificado — para que quede un registro
     * histórico fiel de qué costó ese lote en particular.
     * <p>
     * Se agregan a {@link LoteEntity#getDetallesCostoDirecto()}: se persisten en cascada junto
     * con el {@code lote} al final de {@link #finalizarEnvasado(Long)}, sin necesidad de un
     * repositorio propio.
     * </p>
     */
    private void registrarCostosDirectosDelLote(LoteEntity lote, BigDecimal totalEnvasado) {
        List<CostoDirectoAdicionalEntity> costosDirectosActivos = costoDirectoAdicionalRepository.findAllActivos();

        for (CostoDirectoAdicionalEntity costoDirectoAdicional : costosDirectosActivos) {
            DetalleCostoDirectoEntity detalleCostoDirecto = DetalleCostoDirectoEntity.builder()
                    .costoPorLitroAplicado(costoDirectoAdicional.getCostoPorLitro())
                    .subtotal(costoDirectoAdicional.getCostoPorLitro().multiply(totalEnvasado).setScale(4, RoundingMode.HALF_UP))
                    .costoDirectoAdicional(costoDirectoAdicional)
                    .lote(lote)
                    .build();
            lote.getDetallesCostoDirecto().add(detalleCostoDirecto);
        }
    }

    /**
     * Valida que la cantidad consumida de CADA insumo requerido por una etapa alcance el
     * porcentaje mínimo configurado sobre su cantidad requerida (escalada), para evitar que se
     * avance de etapa por error sin haber registrado consumos, o habiéndolos registrado de forma
     * incompleta.
     * <p>
     * Reutiliza {@link IConsumoInsumoServicio#filtrarInsumosRequeridos(Long)} para obtener, por
     * insumo, cuánto requiere la etapa y cuánto ya se consumió — la misma fuente que usa la
     * pantalla de gestión de consumos, evitando recalcular o duplicar esa lógica aquí. Ambos lados de
     * la comparación se redondean a 4 decimales.
     * </p>
     *
     * @throws ReglaNegocioException Si algún insumo requerido no alcanza el porcentaje mínimo.
     */
    private void validarPorcentajeMinimoConsumido(EtapaLoteEntity etapa, double porcentajeMinimo) {
        List<String> insumosInsuficientes = evaluarAvance(consumoInsumoServicio.filtrarInsumosRequeridos(etapa.getId()), porcentajeMinimo).stream()
                .filter(avance -> !avance.isCumple())
                .map(AvanceInsumoResponseDTO::getNombreInsumo)
                .toList();

        if (!insumosInsuficientes.isEmpty()) {
            throw new ReglaNegocioException("No se puede finalizar la etapa: los siguientes insumos no alcanzaron el "
                    + ValidacionesNumericas.formatearNumero(porcentajeMinimo) + "% mínimo de consumo requerido: " + String.join(", ", insumosInsuficientes));
        }
    }

    /**
     * Regla única del avance hacia el mínimo de consumo, compartida por la validación de la finalización de la etapa y por
     * {@link #calcularAvanceDeEtapa(Long, TipoEtapa)}: un insumo cumple si su cantidad consumida alcanza el porcentaje
     * mínimo de su cantidad requerida, comparando ambos lados redondeados a 4 decimales. Con mínimo 0 todos cumplen
     * (no se controla el consumo).
     */
    private static List<AvanceInsumoResponseDTO> evaluarAvance(List<InsumoRequeridoResponseDTO> insumosRequeridos, double porcentajeMinimo) {
        return insumosRequeridos.stream()
                .map(insumoRequerido -> AvanceInsumoResponseDTO.builder()
                        .nombreInsumo(insumoRequerido.getInsumo().getNombre())
                        .unidadDeMedida(insumoRequerido.getInsumo().getUnidadDeMedida())
                        .requerida(insumoRequerido.getCantidadRequerida())
                        .consumida(insumoRequerido.getCantidadConsumida())
                        .porcentaje(ConsumoInsumoServicioImpl.calcularPorcentajeConsumido(insumoRequerido.getCantidadConsumida(), insumoRequerido.getCantidadRequerida()))
                        .cumple(Redondeo.redondear(insumoRequerido.getCantidadConsumida())
                                >= Redondeo.redondear(insumoRequerido.getCantidadRequerida() * (porcentajeMinimo / 100.0)))
                        .build())
                .toList();
    }

    /**
     * Busca la etapa actualmente EN_CURSO del lote y valida que sea del tipo esperado.
     *
     * @throws ReglaNegocioException Si el lote no tiene ninguna etapa EN_CURSO, o si la que tiene
     *                               no es del tipo esperado.
     */
    private EtapaLoteEntity obtenerEtapaEnCursoValidando(LoteEntity lote, TipoEtapa tipoEsperado) {
        EtapaLoteEntity etapaActual = lote.getEtapas().stream()
                .filter(etapa -> etapa.getEstado() == EstadoEtapaLote.EN_CURSO)
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("El lote no tiene ninguna etapa en curso"));

        if (etapaActual.getEtapa() != tipoEsperado) {
            throw new ReglaNegocioException("La etapa actual del lote es " + etapaActual.getEtapa().getEtiqueta() + ", no " + tipoEsperado.getEtiqueta());
        }
        return etapaActual;
    }

    /**
     * Finaliza la etapa actual (pasa a FINALIZADA, registra su fecha de fin) e inicia la
     * siguiente (pasa a EN_CURSO, registra su fecha de inicio).
     */
    private void avanzarEtapa(EtapaLoteEntity etapaActual, EtapaLoteEntity etapaSiguiente) {
        etapaActual.setEstado(EstadoEtapaLote.FINALIZADA);
        etapaActual.setFechaFinalizacion(LocalDateTime.now());
        etapaSiguiente.setEstado(EstadoEtapaLote.EN_CURSO);
        etapaSiguiente.setFechaInicio(LocalDateTime.now());
    }

    /**
     * Libera todas las reservas de insumo de un lote, sin importar a cuál de sus etapas esté
     * asociada cada una. Se usa al cancelar un lote.
     */
    private void liberarReservasDelLote(LoteEntity lote) {
        liberarReservas(reservaInsumoRepository.findByEtapaLote_Lote_Id(lote.getId()));
    }

    /**
     * Libera las reservas de insumo de una única etapa de lote (a diferencia de
     * {@link #liberarReservasDelLote(LoteEntity)}, que libera las de TODO el lote): se usa al
     * finalizar una etapa, para devolver a stock lo que haya quedado reservado sin consumir de
     * ella, sin tocar las reservas de las demás etapas del mismo lote.
     */
    private void liberarReservasDeLaEtapa(EtapaLoteEntity etapa) {
        liberarReservas(reservaInsumoRepository.findByEtapaLoteId(etapa.getId()));
    }

    /**
     * Libera un conjunto de reservas de insumo: por cada una, revierte la cantidad reservada
     * sobre su {@link LoteInsumoEntity} y elimina la reserva — una reserva liberada deja de existir, no se
     * conserva con algún indicador de "liberada".
     * <p>
     * Las reservas se procesan por ID de lote de insumo ascendente (orden canónico de bloqueo). Cada lote de insumo se
     * bloquea para escritura y se vuelve a leer ya bloqueado: la transacción pudo haberlo cargado antes (por ejemplo, al
     * validar el consumo mínimo) y, de no releerlo, se escribirían de vuelta cantidades anteriores al bloqueo,
     * pisando lo que otra operación confirmó mientras tanto.
     * </p>
     */
    private void liberarReservas(List<ReservaInsumoEntity> reservas) {
        List<ReservaInsumoEntity> reservasPorLoteInsumo = reservas.stream()
                .sorted(Comparator.comparing(reserva -> reserva.getLoteInsumo().getId()))
                .toList();
        Set<Long> lotesInsumoReleidos = new HashSet<>();
        for (ReservaInsumoEntity reserva : reservasPorLoteInsumo) {
            Long idLoteInsumo = reserva.getLoteInsumo().getId();
            LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaLiberarReserva(idLoteInsumo)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + idLoteInsumo));
            if (lotesInsumoReleidos.add(idLoteInsumo)) {
                entityManager.refresh(loteInsumo);
            }
            loteInsumo.liberarReserva(reserva.getCantidadReservada());
            loteInsumoRepository.save(loteInsumo);
        }

        reservaInsumoRepository.deleteAll(reservas);
    }

    /**
     * Actualiza el estado operativo del equipamiento asociado a las 6 etapas del lote al
     * cancelarlo.
     * <p>
     * El Fermentador es compartido por 3 etapas (Fermentación, Maduración y Envasado), así que se
     * resuelve por equipamiento y no por etapa aislada: si alguna de sus etapas está EN_CURSO, el
     * fermentador pasa a EN_LIMPIEZA sin importar que sus otras etapas todavía estén PENDIENTE — el
     * equipo físico está en uso ahora mismo. Prioridad por equipamiento: EN_CURSO &gt; PENDIENTE &gt;
     * FINALIZADA.
     * </p>
     */
    private void actualizarEquipamientoAlCancelar(LoteEntity lote) {
        Long idMolino = null;
        Long idMacerador = null;
        Long idOllaHervor = null;
        Long idFermentador = null;
        EstadoEtapaLote estadoMolino = null;
        EstadoEtapaLote estadoMacerador = null;
        EstadoEtapaLote estadoOllaHervor = null;
        EstadoEtapaLote estadoFermentador = null;

        for (EtapaLoteEntity etapa : lote.getEtapas()) {
            switch (etapa.getEtapa()) {
                case MOLIENDA -> {
                    idMolino = etapa.getEquipamiento().getId();
                    estadoMolino = etapa.getEstado();
                }
                case MACERACION -> {
                    idMacerador = etapa.getEquipamiento().getId();
                    estadoMacerador = etapa.getEstado();
                }
                case HERVIDO -> {
                    idOllaHervor = etapa.getEquipamiento().getId();
                    estadoOllaHervor = etapa.getEstado();
                }
                case FERMENTACION, MADURACION, ENVASADO -> {
                    idFermentador = etapa.getEquipamiento().getId();
                    estadoFermentador = combinarEstadoEquipamientoCompartido(estadoFermentador, etapa.getEstado());
                }
            }
        }

        Objects.requireNonNull(estadoMolino, "El lote " + lote.getId() + " no tiene una etapa de Molienda");
        Objects.requireNonNull(estadoMacerador, "El lote " + lote.getId() + " no tiene una etapa de Maceración");
        Objects.requireNonNull(estadoOllaHervor, "El lote " + lote.getId() + " no tiene una etapa de Hervido");
        Objects.requireNonNull(estadoFermentador, "El lote " + lote.getId() + " no tiene etapas de Fermentación/Maduración/Envasado");

        EstadoOperativo nuevoEstadoMolino = resolverEstadoOperativoAlCancelar(estadoMolino);
        if (nuevoEstadoMolino != null) {
            Long idMolinoFinal = idMolino;
            MolinoEntity molino = desproxy(molinoRepository.buscarPorIdParaCambiarEstadoOperativo(idMolinoFinal), MolinoEntity.class)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolinoFinal));
            molino.setEstadoOperativo(nuevoEstadoMolino);
            molinoRepository.save(molino);
        }

        EstadoOperativo nuevoEstadoMacerador = resolverEstadoOperativoAlCancelar(estadoMacerador);
        if (nuevoEstadoMacerador != null) {
            Long idMaceradorFinal = idMacerador;
            MaceradorEntity macerador = desproxy(maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(idMaceradorFinal), MaceradorEntity.class)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMaceradorFinal));
            macerador.setEstadoOperativo(nuevoEstadoMacerador);
            maceradorRepository.save(macerador);
        }

        EstadoOperativo nuevoEstadoOllaHervor = resolverEstadoOperativoAlCancelar(estadoOllaHervor);
        if (nuevoEstadoOllaHervor != null) {
            Long idOllaHervorFinal = idOllaHervor;
            OllaHervorEntity ollaHervor = desproxy(ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(idOllaHervorFinal), OllaHervorEntity.class)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervorFinal));
            ollaHervor.setEstadoOperativo(nuevoEstadoOllaHervor);
            ollaHervorRepository.save(ollaHervor);
        }

        EstadoOperativo nuevoEstadoFermentador = resolverEstadoOperativoAlCancelar(estadoFermentador);
        if (nuevoEstadoFermentador != null) {
            Long idFermentadorFinal = idFermentador;
            FermentadorEntity fermentador = desproxy(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentadorFinal), FermentadorEntity.class)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentadorFinal));
            fermentador.setEstadoOperativo(nuevoEstadoFermentador);
            fermentadorRepository.save(fermentador);
        }
    }

    /**
     * Cancela las etapas de un lote que todavía no habían finalizado (PENDIENTE o EN_CURSO) al
     * momento de cancelarlo, dejándolas como registro histórico del punto donde se interrumpió.
     * Las etapas ya FINALIZADA no se tocan.
     * <p>
     * {@code fechaInicio} queda como quedó: {@code null} si la etapa estaba PENDIENTE (nunca se
     * usó el equipamiento), con valor si estaba EN_CURSO (el equipamiento sí se usó) — eso permite
     * distinguir después cuáles de las etapas CANCELADA representan uso real del equipamiento.
     * </p>
     *
     * @param lote El lote que se está cancelando.
     * @param fechaCancelacion La fecha y hora de la cancelación, usada como fecha de finalización de cada etapa cancelada.
     */
    private void cancelarEtapasNoFinalizadas(LoteEntity lote, LocalDateTime fechaCancelacion) {
        for (EtapaLoteEntity etapa : lote.getEtapas()) {
            if (etapa.getEstado() == EstadoEtapaLote.PENDIENTE || etapa.getEstado() == EstadoEtapaLote.EN_CURSO) {
                etapa.setEstado(EstadoEtapaLote.CANCELADA);
                etapa.setFechaFinalizacion(fechaCancelacion);
            }
        }
    }

    /**
     * Combina el estado de dos etapas asociadas al mismo equipamiento compartido (el Fermentador),
     * quedándose con el de mayor prioridad: EN_CURSO &gt; PENDIENTE &gt; FINALIZADA.
     */
    private static EstadoEtapaLote combinarEstadoEquipamientoCompartido(EstadoEtapaLote actual, EstadoEtapaLote nuevo) {
        if (actual == null) {
            return nuevo;
        }
        return prioridadEstadoEtapa(nuevo) > prioridadEstadoEtapa(actual) ? nuevo : actual;
    }

    private static int prioridadEstadoEtapa(EstadoEtapaLote estado) {
        return switch (estado) {
            case EN_CURSO -> 2;
            case PENDIENTE -> 1;
            case FINALIZADA -> 0;
            case CANCELADA -> throw new IllegalStateException("Una etapa CANCELADA no debería llegar a este punto: se evalúa antes de aplicar la cancelación sobre las etapas");
        };
    }

    /**
     * Resuelve a qué {@link EstadoOperativo} debe pasar el equipamiento de una etapa al cancelar el
     * lote, según el estado de esa etapa. Devuelve {@code null} cuando el equipamiento debe
     * mantener su estado actual sin tocarlo (etapa ya FINALIZADA).
     */
    private static EstadoOperativo resolverEstadoOperativoAlCancelar(EstadoEtapaLote estadoEtapa) {
        return switch (estadoEtapa) {
            case EN_CURSO -> EstadoOperativo.EN_LIMPIEZA;
            case PENDIENTE -> EstadoOperativo.DISPONIBLE;
            case FINALIZADA -> null;
            case CANCELADA -> throw new IllegalStateException("Una etapa CANCELADA no debería llegar a este punto: se evalúa antes de aplicar la cancelación sobre las etapas");
        };
    }

    /**
     * Valida que el volumen objetivo del lote se haya informado y sea un número finito mayor a 0, y lo devuelve
     * redondeado a la escala de las cantidades ({@link LimitesTecnicos#ESCALA_CANTIDADES} decimales). No consulta nada.
     *
     * @return El volumen objetivo redondeado.
     * @throws ReglaNegocioException Si el volumen objetivo es nulo o no es un número mayor a 0.
     */
    private double validarVolumenObjetivoInformado(Double volumenObjetivo) {
        if (volumenObjetivo == null) {
            throw new ReglaNegocioException("El volumen objetivo del lote es obligatorio");
        }
        double volumenRedondeado = Redondeo.redondear(volumenObjetivo);
        if (volumenRedondeado <= 0) {
            throw new ReglaNegocioException("El volumen objetivo del lote debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACero(volumenRedondeado, "El volumen objetivo del lote");
        return volumenRedondeado;
    }

    /**
     * Valida que el volumen objetivo del lote (Paso 1 del escalado de agua) no supere la capacidad útil del
     * fermentador seleccionado.
     */
    private void validarVolumenObjetivoContraFermentador(double volumenObjetivo, FermentadorEntity fermentador) {
        if (volumenObjetivo > Redondeo.redondear(fermentador.getCapacidadUtil())) {
            throw new ReglaNegocioException("El volumen objetivo del lote (" + ValidacionesNumericas.formatearNumero(volumenObjetivo)
                    + " L) supera la capacidad útil del fermentador seleccionado ("
                    + ValidacionesNumericas.formatearNumero(fermentador.getCapacidadUtil()) + " L)");
        }
    }

    /**
     * Valida que se haya informado un dato obligatorio del formulario.
     *
     * @param valor   El dato a validar.
     * @param mensaje El mensaje de la excepción si el dato es nulo.
     * @throws ReglaNegocioException Si el dato es nulo.
     */
    private static void validarInformado(Object valor, String mensaje) {
        if (valor == null) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    /**
     * Valida que se haya informado el lote sobre el que se opera.
     *
     * @throws ReglaNegocioException Si el ID del lote es nulo.
     */
    private static void validarLoteInformado(Long idLote) {
        validarInformado(idLote, "El lote es obligatorio");
    }

    /**
     * Calcula la masa total de malta necesaria para alcanzar el OG objetivo de la receta, dado el
     * volumen objetivo del lote y el rendimiento de maceración del macerador seleccionado.
     */
    private double calcularMasaMaltaEscalada(double volumenObjetivo, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return escaladoInsumoServicio.calcularRequerimientosMalta(versionReceta, volumenObjetivo, macerador).stream()
                .mapToDouble(RequerimientoInsumo::cantidadRequerida)
                .sum();
    }

    /**
     * Para cada insumo requerido, busca (bloqueando para escritura) sus lotes de insumo NO VENCIDOS
     * ordenados por FEFO (un lote vencido, con fecha de vencimiento anterior a hoy, no cuenta como stock
     * disponible; si vence hoy todavía es válido) y valida que la suma de cantidad disponible entre todos ellos alcance la
     * cantidad requerida SUMADA ENTRE TODAS LAS ETAPAS que lo necesiten (el mismo insumo puede
     * tener requerimientos separados por etapa, pero el stock físico que lo cubre es uno solo,
     * compartido). No reserva nada todavía.
     * <p>
     * Evalúa TODOS los insumos antes de lanzar, en vez de cortar en el primero que falla: así el
     * usuario ve de una sola vez la lista completa de insumos con stock insuficiente, sin tener que
     * corregir uno, reintentar, y recién ahí enterarse del siguiente.
     * </p>
     * <p>
     * Las cantidades se redondean a 4 decimales antes de compararlas, y el mensaje las muestra con la
     * unidad de medida del insumo.
     * </p>
     */
    private Map<Long, List<LoteInsumoEntity>> obtenerYValidarStockDisponible(Collection<RequerimientoInsumo> requerimientos) {
        Map<Long, InsumoEntity> insumoPorId = new LinkedHashMap<>();
        Map<Long, Double> cantidadRequeridaPorInsumo = new LinkedHashMap<>();
        for (RequerimientoInsumo requerimiento : requerimientos) {
            insumoPorId.putIfAbsent(requerimiento.insumo().getId(), requerimiento.insumo());
            cantidadRequeridaPorInsumo.merge(requerimiento.insumo().getId(), Redondeo.redondear(requerimiento.cantidadRequerida()),
                    (acumulada, nueva) -> Redondeo.redondear(acumulada + nueva));
        }

        // Una sola consulta bloquea las filas de todos los insumos en orden de ID (orden canónico de bloqueo); el orden
        // FEFO (vencimiento ascendente) se aplica después en memoria
        Map<Long, List<LoteInsumoEntity>> candidatosPorInsumo = loteInsumoRepository
                .buscarNoVencidosPorInsumosIdsParaReservar(cantidadRequeridaPorInsumo.keySet(), LocalDate.now()).stream()
                .collect(Collectors.groupingBy(loteInsumo -> loteInsumo.getInsumo().getId()));

        Map<Long, List<LoteInsumoEntity>> stockPorInsumo = new LinkedHashMap<>();
        List<String> faltantes = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : cantidadRequeridaPorInsumo.entrySet()) {
            Long idInsumo = entry.getKey();
            double cantidadRequeridaTotal = entry.getValue();
            List<LoteInsumoEntity> lotesDisponibles = candidatosPorInsumo.getOrDefault(idInsumo, List.of()).stream()
                    .sorted(Comparator.comparing(LoteInsumoEntity::getFechaVencimiento).thenComparing(LoteInsumoEntity::getId))
                    .toList();

            double totalDisponible = Redondeo.redondear(lotesDisponibles.stream().mapToDouble(LoteInsumoEntity::getCantidadDisponible).sum());
            if (totalDisponible < cantidadRequeridaTotal) {
                InsumoEntity insumo = insumoPorId.get(idInsumo);
                String unidad = insumo.getUnidadDeMedida().getSimbolo();
                faltantes.add("'" + insumo.getNombre() + "' (se requieren " + ValidacionesNumericas.formatearNumero(cantidadRequeridaTotal)
                        + " " + unidad + " y solo hay " + ValidacionesNumericas.formatearNumero(totalDisponible) + " " + unidad + " disponibles)");
            }

            stockPorInsumo.put(idInsumo, lotesDisponibles);
        }

        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Stock disponible insuficiente de los siguientes insumos: " + String.join("; ", faltantes));
        }

        return stockPorInsumo;
    }

    /**
     * Reserva la cantidad requerida de cada (insumo, etapa) aplicando FEFO: recorre los lotes de
     * insumo ya ordenados por fecha de vencimiento ascendente (compartidos entre todas las etapas
     * que requieran ese mismo insumo, así que se van agotando de forma acumulativa a medida que se
     * procesa cada requerimiento), tomando de cada uno la cantidad disponible que haga falta hasta
     * cubrir el requerimiento, generando una {@link ReservaInsumoEntity} por cada lote de insumo
     * que contribuye, asociada a la etapa de ESE requerimiento puntual. Las cantidades se redondean a
     * 4 decimales al restar y al guardar cada reserva.
     */
    private List<ReservaInsumoEntity> reservarInsumosFEFO(Collection<RequerimientoInsumo> requerimientos, Map<Long, List<LoteInsumoEntity>> stockPorInsumo) {
        List<ReservaInsumoEntity> reservas = new ArrayList<>();
        for (RequerimientoInsumo requerimiento : requerimientos) {
            double cantidadRestante = Redondeo.redondear(requerimiento.cantidadRequerida());

            for (LoteInsumoEntity loteInsumo : stockPorInsumo.get(requerimiento.insumo().getId())) {
                if (cantidadRestante <= 0) {
                    break;
                }
                double cantidadDisponible = loteInsumo.getCantidadDisponible();
                if (cantidadDisponible <= 0) {
                    continue;
                }

                double cantidadAReservar = Math.min(cantidadRestante, cantidadDisponible);
                loteInsumo.reservar(cantidadAReservar);
                loteInsumoRepository.save(loteInsumo);

                reservas.add(ReservaInsumoEntity.builder()
                        .etapaLote(requerimiento.etapa())
                        .loteInsumo(loteInsumo)
                        .cantidadReservada(cantidadAReservar)
                        .build());
                cantidadRestante = Redondeo.redondear(cantidadRestante - cantidadAReservar);
            }
        }
        return reservas;
    }

    /**
     * Bloquea (con escritura pesimista) y valida disponibilidad de los 4 equipamientos reales
     * asociados a las etapas del lote: Molino, Macerador, Olla de Hervor y el Fermentador
     * (compartido por Fermentación, Maduración y Envasado).
     */
    private EquipamientoDelLote bloquearYValidarEquipamientoDisponible(LoteEntity lote) {
        Long idMolino = null;
        Long idMacerador = null;
        Long idOllaHervor = null;
        Long idFermentador = null;

        for (EtapaLoteEntity etapa : lote.getEtapas()) {
            switch (etapa.getEtapa()) {
                case MOLIENDA -> idMolino = etapa.getEquipamiento().getId();
                case MACERACION -> idMacerador = etapa.getEquipamiento().getId();
                case HERVIDO -> idOllaHervor = etapa.getEquipamiento().getId();
                case FERMENTACION, MADURACION, ENVASADO -> idFermentador = etapa.getEquipamiento().getId();
            }
        }

        final Long idMolinoFinal = idMolino;
        final Long idMaceradorFinal = idMacerador;
        final Long idOllaHervorFinal = idOllaHervor;
        final Long idFermentadorFinal = idFermentador;

        MolinoEntity molino = desproxy(molinoRepository.buscarPorIdParaCambiarEstadoOperativo(idMolinoFinal), MolinoEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolinoFinal));
        MaceradorEntity macerador = desproxy(maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(idMaceradorFinal), MaceradorEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMaceradorFinal));
        OllaHervorEntity ollaHervor = desproxy(ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(idOllaHervorFinal), OllaHervorEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervorFinal));
        FermentadorEntity fermentador = desproxy(fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentadorFinal), FermentadorEntity.class)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentadorFinal));

        validarDisponible(molino, "El molino");
        validarDisponible(macerador, "El macerador");
        validarDisponible(ollaHervor, "La olla de hervor");
        validarDisponible(fermentador, "El fermentador");

        return new EquipamientoDelLote(molino, macerador, ollaHervor, fermentador);
    }

    /**
     * Devuelve la instancia real de una entidad: la consulta bloqueada del equipamiento devuelve el proxy de Hibernate
     * ya cargado en el contexto de persistencia (por la asociación perezosa de la etapa), que no es instancia de la
     * clase concreta y falla al asignarse a ella. Se lee como el tipo base para que el proxy no se convierta antes
     * de desenvolverlo.
     */
    static <T extends EquipamientoEntity> Optional<T> desproxy(Optional<? extends EquipamientoEntity> equipamiento, Class<T> clase) {
        return equipamiento.map(Hibernate::unproxy).map(clase::cast);
    }

    private void validarDisponible(EquipamientoEntity equipamiento, String etiqueta) {
        if (equipamiento.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException(etiqueta + " '" + equipamiento.getIdentificadorInterno()
                    + "' no se encuentra disponible (estado actual: " + equipamiento.getEstadoOperativo().getEtiqueta() + ")");
        }
    }

    /**
     * Los 4 equipamientos reales (ya bloqueados y validados como DISPONIBLE) asociados a un lote.
     */
    private record EquipamientoDelLote(MolinoEntity molino, MaceradorEntity macerador, OllaHervorEntity ollaHervor, FermentadorEntity fermentador) {
    }

    /**
     * Genera el identificador interno del lote (nombre normalizado de la versión de receta de la planificación +
     * "-R" + id de la receta + "-" + número de lote de esa receta, ej. "PILSEN_CLASICA-R7-3"), incrementando de
     * forma segura el contador de lotes de la receta mediante un bloqueo pesimista, para soportar el registro
     * concurrente de múltiples lotes de la misma receta. El código se calcula una sola vez al registrar el lote.
     */
    private String generarIdentificadorInterno(VersionRecetaEntity versionReceta) {
        RecetaEntity receta = recetaRepository.findByIdParaActualizarContador(versionReceta.getReceta().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + versionReceta.getReceta().getId()));

        Long numeroDeLote = receta.getContadorLotes();
        receta.setContadorLotes(numeroDeLote + 1);
        recetaRepository.save(receta);

        return normalizarNombreParaCodigo(versionReceta.getNombre()) + "-R" + receta.getId() + "-" + numeroDeLote;
    }

    /**
     * Normaliza el nombre de la versión de receta para armar el código del lote: sin marcas diacríticas, en
     * mayúsculas (sin depender del idioma del sistema), con toda secuencia de caracteres distintos de A-Z y 0-9
     * reemplazada por un único "_", sin "_" en los extremos y cortado a
     * {@link LimitesTecnicos#LONGITUD_MAXIMA_NOMBRE} caracteres. Si no queda ningún carácter, devuelve "LOTE".
     */
    private static String normalizarNombreParaCodigo(String nombre) {
        if (nombre == null) {
            return NOMBRE_POR_DEFECTO_DEL_CODIGO;
        }
        String sinMarcas = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String normalizado = recortarGuionesBajos(sinMarcas.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_"));
        if (normalizado.length() > LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE) {
            normalizado = recortarGuionesBajos(normalizado.substring(0, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE));
        }
        return normalizado.isEmpty() ? NOMBRE_POR_DEFECTO_DEL_CODIGO : normalizado;
    }

    private static String recortarGuionesBajos(String texto) {
        int desde = 0;
        int hasta = texto.length();
        while (desde < hasta && texto.charAt(desde) == '_') {
            desde++;
        }
        while (hasta > desde && texto.charAt(hasta - 1) == '_') {
            hasta--;
        }
        return texto.substring(desde, hasta);
    }

    /**
     * Crea las 6 etapas del lote en estado PENDIENTE, cada una asociada al equipamiento que le
     * corresponde: Molino para Molienda, Macerador para Maceración, Olla de Hervor para Hervido,
     * y el mismo Fermentador para Fermentación, Maduración y Envasado.
     */
    private List<EtapaLoteEntity> crearEtapasPendientes(LoteEntity loteEntity, MolinoEntity molino, MaceradorEntity macerador, OllaHervorEntity ollaHervor, FermentadorEntity fermentador) {
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(construirEtapaPendiente(TipoEtapa.MOLIENDA, molino, loteEntity));
        etapas.add(construirEtapaPendiente(TipoEtapa.MACERACION, macerador, loteEntity));
        etapas.add(construirEtapaPendiente(TipoEtapa.HERVIDO, ollaHervor, loteEntity));
        etapas.add(construirEtapaPendiente(TipoEtapa.FERMENTACION, fermentador, loteEntity));
        etapas.add(construirEtapaPendiente(TipoEtapa.MADURACION, fermentador, loteEntity));
        etapas.add(construirEtapaPendiente(TipoEtapa.ENVASADO, fermentador, loteEntity));
        return etapas;
    }

    private EtapaLoteEntity construirEtapaPendiente(TipoEtapa etapa, EquipamientoEntity equipamiento, LoteEntity loteEntity) {
        return EtapaLoteEntity.builder()
                .etapa(etapa)
                .estado(EstadoEtapaLote.PENDIENTE)
                .equipamiento(equipamiento)
                .lote(loteEntity)
                .build();
    }

    /**
     * Calcula la fecha de inicio estimada del lote encolándolo detrás del último lote que ya
     * tiene reservado un lugar en el cronograma del fermentador seleccionado (lotes en estado
     * PENDIENTE o EN_EJECUCION): dos días después de su fecha de finalización estimada (el día intermedio es
     * el de limpieza del fermentador), o hoy si el fermentador no tiene ningún lote planificado.
     * <p>
     * Un lote nunca arranca antes de la fecha de inicio estimada de su planificación de producción
     * ni antes de hoy, y un último lote atrasado sigue ocupando el fermentador hoy, así que el resultado es
     * {@code max(max(fin del último lote, hoy) + 2 días, fechaInicioEstimada de la planificación, hoy)}, o
     * {@code max(fechaInicioEstimada de la planificación, hoy)} si no hay lotes vivos.
     * Pueden quedar huecos en la cola del fermentador.
     * </p>
     * <p>
     * El cronograma solo se lleva por fermentador: es el único equipo cuyo tiempo de uso se mide
     * en días/meses (fermentación + maduración + envasado); molino, macerador y olla de hervor se
     * usan en horas dentro del mismo día de brewing y su disponibilidad real se valida recién al
     * iniciar el lote.
     * </p>
     */
    private LocalDate calcularFechaInicioEstimada(FermentadorEntity fermentador, PlanificacionProduccionEntity planificacionProduccion) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaPorCola = loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(fermentador, ESTADOS_QUE_OCUPAN_FERMENTADOR)
                .map(ultimaFechaFinalizacionEstimada -> (ultimaFechaFinalizacionEstimada.isBefore(hoy) ? hoy : ultimaFechaFinalizacionEstimada).plusDays(DIAS_MARGEN_LIMPIEZA))
                .orElse(hoy);
        return maxima(fechaPorCola, planificacionProduccion.getFechaInicioEstimada(), hoy);
    }

    /**
     * Calcula la fecha de finalización estimada del lote sumando, sobre la fecha de inicio
     * estimada, la duración total del proceso: el día de brewing (molienda + maceración +
     * hervido, redondeado hacia arriba a días completos) más la fermentación, la maduración y el
     * envasado.
     */
    private LocalDate calcularFechaFinalizacionEstimada(LocalDate fechaInicioEstimada, double volumenObjetivo, MolinoEntity molino, double masaMaltaEscalada, VersionRecetaEntity versionReceta) {
        double horasMolienda = masaMaltaEscalada / molino.getRendimientoMolienda();
        double horasMaceracionYHervido = (versionReceta.getDuracionMaceracion() + versionReceta.getDuracionHervido()) / 60.0;

        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        double horasEnvasado = volumenObjetivo / configuracion.getVelocidadEstandarEnvasado();

        long diasBrewDayYEnvasado = (long) Math.ceil((horasMolienda + horasMaceracionYHervido + horasEnvasado) / HORAS_POR_DIA);
        long diasTotales = diasBrewDayYEnvasado + versionReceta.getDuracionFermentacion() + versionReceta.getDuracionMaduracion();

        return fechaInicioEstimada.plusDays(diasTotales);
    }

    /**
     * Lista los lotes reales de una planificación de producción, en una versión liviana pensada
     * para el detalle de la planificación y el cronograma.
     * <p>
     * Incluye los lotes en cualquier estado, ordenados por fecha de inicio estimada ascendente y,
     * ante igual fecha, por ID ascendente.
     * </p>
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción cuyos lotes se quieren listar.
     * @return Los lotes de la planificación en formato {@link LoteCronogramaResponseDTO}, o una lista vacía si todavía no tiene lotes.
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LoteCronogramaResponseDTO> listarLotesDePlanificacion(Long idPlanificacionProduccion) {
        // 1. Validar que la planificación de producción exista
        if (!planificacionProduccionRepository.existsById(idPlanificacionProduccion)) {
            throw new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacionProduccion);
        }

        // 2. Recuperar los lotes con sus relaciones
        List<LoteEntity> lotes = loteRepository.buscarLotesDePlanificacionConCronograma(idPlanificacionProduccion);

        // 3. El atraso y el corrimiento dependen de la cola completa de cada fermentador (de todas las planificaciones):
        //    se recuperan los lotes vivos de los fermentadores involucrados, con una sola consulta
        Set<Long> idsFermentadores = lotes.stream()
                .filter(LoteServicioImpl::estaVivo)
                .map(LoteServicioImpl::idFermentadorONulo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<LoteEntity> vivosDeSusFermentadores = idsFermentadores.isEmpty() ? List.of()
                : loteRepository.buscarVivosDeFermentadores(idsFermentadores);

        // 4. Mapear al DTO de cronograma con el atraso y el corrimiento calculados
        return mapearConAtraso(lotes, vivosDeSusFermentadores);
    }

    /**
     * Lista los lotes de todas las planificaciones para el cronograma global de un período, por fermentador.
     * Ver {@link ILoteServicio#listarLotesParaCronograma(Long, LocalDate, LocalDate)}.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LoteCronogramaResponseDTO> listarLotesParaCronograma(Long idFermentador, LocalDate desde, LocalDate hasta) {
        // 1. Validar el período: ambos extremos son obligatorios, no puede estar invertido ni superar el tope
        if (desde == null) {
            throw new ReglaNegocioException("La fecha de inicio del período es obligatoria");
        }
        if (hasta == null) {
            throw new ReglaNegocioException("La fecha de fin del período es obligatoria");
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaNegocioException("La fecha de fin del período no puede ser anterior a la fecha de inicio");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) > LimitesTecnicos.RANGO_MAXIMO_CRONOGRAMA_DIAS) {
            throw new ReglaNegocioException("El período del cronograma no puede superar los "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.RANGO_MAXIMO_CRONOGRAMA_DIAS + 1) + " días (5 años)");
        }

        // 2. Si se pidió un fermentador puntual, validar que exista; un fermentador dado de baja es válido porque sus lotes
        //    siguen en el historial (existsById no filtra por estado, a diferencia de findById)
        if (idFermentador != null && !fermentadorRepository.existsById(idFermentador)) {
            throw new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentador);
        }

        // 3. Recuperar los lotes con sus relaciones; hasta es inclusivo, así que la consulta usa el inicio del día siguiente
        List<LoteEntity> lotes = loteRepository.buscarLotesParaCronograma(idFermentador, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());

        // 4. Los lotes vivos ya vienen completos (la consulta siempre trae todos los PENDIENTE y EN_EJECUCION de cada
        //    fermentador): con ellos se calcula el atraso y el corrimiento de las colas, sin otra consulta
        return mapearConAtraso(lotes, lotes.stream().filter(LoteServicioImpl::estaVivo).toList());
    }

    /**
     * Mapea los lotes al DTO de cronograma agregando el atraso del lote en ejecución y el corrimiento de
     * los pendientes que lo siguen en la cola de su fermentador.
     *
     * @param lotes Los lotes a mostrar.
     * @param vivosDeSusFermentadores Los lotes PENDIENTE y EN_EJECUCION de los fermentadores involucrados, de todas las planificaciones.
     */
    private List<LoteCronogramaResponseDTO> mapearConAtraso(List<LoteEntity> lotes, Collection<LoteEntity> vivosDeSusFermentadores) {
        Map<Long, AtrasoDeLote> atrasos = calcularAtrasos(vivosDeSusFermentadores, LocalDate.now());
        return lotes.stream()
                .map(lote -> {
                    LoteCronogramaResponseDTO dto = MapperLoteCronograma.toDTO(lote);
                    AtrasoDeLote atraso = atrasos.get(lote.getId());
                    return atraso == null ? dto : dto.toBuilder()
                            .diasDeAtraso(atraso.diasDeAtraso())
                            .diasDeDesplazamiento(atraso.diasDeDesplazamiento())
                            .build();
                })
                .toList();
    }

    /**
     * Atraso de un lote en ejecución y corrimiento de un lote pendiente, en días, calculados al consultar.
     */
    private record AtrasoDeLote(int diasDeAtraso, int diasDeDesplazamiento) {
    }

    /**
     * Calcula, por fermentador, cuánto se atrasó su lote EN_EJECUCION y cuánto corre ese atraso a los lotes
     * PENDIENTE de su cola. Es de solo lectura: no modifica ninguna fecha guardada.
     * <p>
     * Un lote en ejecución está atrasado cuando su fecha de finalización estimada es anterior a {@code hoy};
     * su atraso son los días entre esa fecha y hoy. Solo en ese caso se arma el reencadenado virtual de la cola
     * con {@link #encadenarInicios}, igual que el real pero con base {@code max(fin estimado, hoy) + 2 días} e inicio
     * mínimo {@code hoy}; el corrimiento de cada pendiente es la diferencia entre el inicio resultante y el
     * guardado, solo si es positiva (los huecos de la cola absorben parte del atraso). Si algún pendiente de
     * la cola no tiene fechas estimadas válidas, esa cola no se corre.
     * </p>
     *
     * @return El atraso o corrimiento por ID de lote; los lotes sin atraso ni corrimiento no figuran.
     */
    private Map<Long, AtrasoDeLote> calcularAtrasos(Collection<LoteEntity> vivos, LocalDate hoy) {
        Map<Long, List<LoteEntity>> porFermentador = new LinkedHashMap<>();
        for (LoteEntity lote : vivos) {
            Long idFermentador = idFermentadorONulo(lote);
            if (idFermentador != null) {
                porFermentador.computeIfAbsent(idFermentador, k -> new ArrayList<>()).add(lote);
            }
        }

        Map<Long, AtrasoDeLote> atrasos = new LinkedHashMap<>();
        for (List<LoteEntity> cola : porFermentador.values()) {
            LoteEntity enEjecucion = cola.stream().filter(l -> l.getEstado() == EstadoLote.EN_EJECUCION).findFirst().orElse(null);
            if (enEjecucion == null || enEjecucion.getFechaFinalizacionEstimada() == null
                    || !enEjecucion.getFechaFinalizacionEstimada().isBefore(hoy)) {
                continue;
            }
            int diasDeAtraso = (int) ChronoUnit.DAYS.between(enEjecucion.getFechaFinalizacionEstimada(), hoy);
            atrasos.put(enEjecucion.getId(), new AtrasoDeLote(diasDeAtraso, 0));

            List<LoteEntity> pendientes = cola.stream()
                    .filter(l -> l.getEstado() == EstadoLote.PENDIENTE)
                    .sorted(Comparator.comparing(LoteEntity::getFechaInicioEstimada, Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(LoteEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (pendientes.stream().anyMatch(l -> !tieneFechasEstimadasValidas(l))) {
                continue;
            }
            List<Long> duraciones = pendientes.stream().map(this::calcularDuracionEnDias).toList();
            List<LocalDate> iniciosDePlanificacion = pendientes.stream()
                    .map(l -> l.getPlanificacionProduccion().getFechaInicioEstimada())
                    .toList();
            List<LocalDate> inicios = encadenarInicios(duraciones, iniciosDePlanificacion, hoy.plusDays(DIAS_MARGEN_LIMPIEZA), hoy);
            for (int i = 0; i < pendientes.size(); i++) {
                long corrimiento = ChronoUnit.DAYS.between(pendientes.get(i).getFechaInicioEstimada(), inicios.get(i));
                if (corrimiento > 0) {
                    atrasos.put(pendientes.get(i).getId(), new AtrasoDeLote(0, (int) corrimiento));
                }
            }
        }
        return atrasos;
    }

    private static boolean tieneFechasEstimadasValidas(LoteEntity lote) {
        return lote.getFechaInicioEstimada() != null && lote.getFechaFinalizacionEstimada() != null
                && !lote.getFechaFinalizacionEstimada().isBefore(lote.getFechaInicioEstimada());
    }

    private static boolean estaVivo(LoteEntity lote) {
        return lote.getEstado() == EstadoLote.PENDIENTE || lote.getEstado() == EstadoLote.EN_EJECUCION;
    }

    /**
     * ID del fermentador del lote (el equipamiento de su etapa de Fermentación), o {@code null} si no tiene.
     */
    private static Long idFermentadorONulo(LoteEntity lote) {
        return lote.getEtapas().stream()
                .filter(etapa -> etapa.getEtapa() == TipoEtapa.FERMENTACION && etapa.getEquipamiento() != null)
                .map(etapa -> etapa.getEquipamiento().getId())
                .findFirst()
                .orElse(null);
    }

    /**
     * Sube un lote una posición en la cola de pendientes de su fermentador y reencadena las fechas.
     * Ver {@link ILoteServicio#subirLoteEnCola(Long)}.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteCronogramaResponseDTO subirLoteEnCola(Long idLote) {
        return moverLoteEnCola(idLote, -1);
    }

    /**
     * Baja un lote una posición en la cola de pendientes de su fermentador y reencadena las fechas.
     * Ver {@link ILoteServicio#bajarLoteEnCola(Long)}.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteCronogramaResponseDTO bajarLoteEnCola(Long idLote) {
        return moverLoteEnCola(idLote, +1);
    }

    /**
     * Intercambia un lote PENDIENTE con su vecino en la cola de su fermentador y reencadena las
     * fechas de toda la cola.
     *
     * @param idLote El ID del lote a mover.
     * @param desplazamiento {@code -1} para subirlo (adelantarlo), {@code +1} para bajarlo (retrasarlo).
     */
    private LoteCronogramaResponseDTO moverLoteEnCola(Long idLote, int desplazamiento) {
        validarLoteInformado(idLote);

        // 1. Validar que el lote esté registrado en el sistema, bloqueando su fermentador, la cola pendiente de ese
        //    fermentador y el lote en el orden canónico
        LoteEntity lote = ordenCanonicoDeBloqueo.bloquearLote(idLote, false);

        // 2. Validar que el lote se encuentre en estado PENDIENTE
        if (lote.getEstado() != EstadoLote.PENDIENTE) {
            throw new ReglaNegocioException(MENSAJE_SOLO_MOVER_PENDIENTES);
        }

        // 3. Recuperar la cola pendiente del fermentador, ya bloqueada
        Long idFermentador = obtenerIdFermentador(lote);
        List<LoteEntity> colaPendiente = new ArrayList<>(loteRepository.buscarPendientesDeFermentadorParaReordenar(idFermentador));

        // 4. Ubicar el lote en la cola ya bloqueada (si no está, otra operación lo sacó de PENDIENTE después del paso 2)
        int indice = colaPendiente.indexOf(lote);
        if (indice < 0) {
            throw new ReglaNegocioException(MENSAJE_SOLO_MOVER_PENDIENTES);
        }

        // 5. Validar que tenga un vecino hacia el cual moverse
        int indiceVecino = indice + desplazamiento;
        if (indiceVecino < 0) {
            throw new ReglaNegocioException(MENSAJE_LOTE_YA_ES_PRIMERO_DE_LA_COLA);
        }
        if (indiceVecino >= colaPendiente.size()) {
            throw new ReglaNegocioException(MENSAJE_LOTE_YA_ES_ULTIMO_DE_LA_COLA);
        }

        // 6. Intercambiar con el vecino y reencadenar las fechas de toda la cola en el nuevo orden
        Collections.swap(colaPendiente, indice, indiceVecino);
        reencadenarLotesPendientes(idFermentador, colaPendiente, false);

        return MapperLoteCronograma.toDTO(lote);
    }

    /**
     * Resuelve el ID del fermentador de un lote: el equipamiento de su etapa de Fermentación.
     */
    private Long obtenerIdFermentador(LoteEntity lote) {
        return lote.obtenerEtapaPorTipo(TipoEtapa.FERMENTACION).getEquipamiento().getId();
    }

    /**
     * Reencadena las fechas estimadas de los lotes PENDIENTE de un fermentador, tomando la cola en
     * su orden actual (fecha de inicio estimada y, ante igual fecha, ID). Bloquea para escritura
     * los lotes pendientes del fermentador.
     * <p>
     * Debe invocarse con el lote que originó el cambio (iniciado, finalizado o cancelado) ya
     * persistido y volcado ({@code flush}), para que la consulta de la cola no lo incluya.
     * </p>
     */
    private void reencadenarColaPendienteDeFermentador(Long idFermentador, boolean requiereDiaDeLimpieza) {
        reencadenarLotesPendientes(idFermentador, new ArrayList<>(loteRepository.buscarPendientesDeFermentadorParaReordenar(idFermentador)), requiereDiaDeLimpieza);
    }

    /**
     * Reencadena las fechas estimadas de los lotes PENDIENTE de un fermentador según el orden de la
     * lista recibida. Se invoca al reordenar la cola y cada vez que un evento de lote hace derivar
     * el cronograma real (iniciar, finalizar o cancelar un lote).
     * <p>
     * Algoritmo, con {@code hoy = LocalDate.now()}:
     * </p>
     * <ol>
     *     <li>La duración de cada lote, en días, se conserva tal como está guardada:
     *     {@code DAYS.between(fechaInicioEstimada, fechaFinalizacionEstimada)}. Un lote sin
     *     alguna de las dos fechas, o con duración negativa, aborta la operación.</li>
     *     <li>El inicio mínimo de toda la cadena es {@code hoy}, o el día siguiente a hoy
     *     ({@code hoy + DIAS_LIMPIEZA_TRAS_LOTE_EN_EJECUCION}) cuando {@code requiereDiaDeLimpieza} es verdadero: el
     *     lote que acaba de finalizar o de cancelarse estando en ejecución dejó sus equipos sucios y
     *     necesitan su día de limpieza.</li>
     *     <li>La base inicial es {@code max(fechaFinalizacionEstimada, hoy) + DIAS_MARGEN_LIMPIEZA} del lote
     *     EN_EJECUCION del fermentador (si ese lote está atrasado el fermentador sigue ocupado hoy), o el inicio
     *     mínimo si el fermentador no tiene ningún lote en ejecución.</li>
     *     <li>Para cada lote, en el orden recibido:
     *     {@code inicio = max(base, fechaInicioEstimada de su planificación, inicio mínimo)} y
     *     {@code fin = inicio + duración}; la nueva base es {@code fin + 2 días} (el día {@code fin + 1}
     *     es la limpieza del fermentador). Así ningún lote arranca antes de su planificación ni antes
     *     del inicio mínimo, y pueden quedar huecos en la cola.</li>
     *     <li>Solo se guardan los lotes cuyas fechas efectivamente cambiaron.</li>
     * </ol>
     *
     * @param idFermentador El ID del fermentador dueño de la cola.
     * @param lotesPendientesEnOrden Los lotes PENDIENTE del fermentador, en el orden deseado de la cola.
     * @param requiereDiaDeLimpieza {@code true} si el cambio lo originó un lote que estaba en ejecución (finalizado o cancelado).
     * @throws ReglaNegocioException Si algún lote no tiene sus fechas estimadas o su duración es negativa.
     */
    private void reencadenarLotesPendientes(Long idFermentador, List<LoteEntity> lotesPendientesEnOrden, boolean requiereDiaDeLimpieza) {
        LocalDate hoy = LocalDate.now();

        // 1. Calcular y validar la duración de cada lote antes de modificar nada
        List<Long> duracionesEnDias = lotesPendientesEnOrden.stream()
                .map(this::calcularDuracionEnDias)
                .toList();

        // 2. Inicio mínimo de la cadena: hoy, o un día más si el lote que originó el cambio estaba en ejecución
        //    y dejó sus equipos sucios (necesitan su día de limpieza)
        LocalDate inicioMinimo = requiereDiaDeLimpieza ? hoy.plusDays(DIAS_LIMPIEZA_TRAS_LOTE_EN_EJECUCION) : hoy;

        // 3. Base inicial: dos días después de que termine el lote en ejecución de ese fermentador (el intermedio es el de
        //    limpieza; si está atrasado, cuenta desde hoy), o el inicio mínimo
        LocalDate base = loteRepository.buscarLoteEnEjecucionDeFermentador(idFermentador)
                .map(LoteEntity::getFechaFinalizacionEstimada)
                .map(fin -> (fin.isBefore(hoy) ? hoy : fin).plusDays(DIAS_MARGEN_LIMPIEZA))
                .orElse(inicioMinimo);

        // 4. Encadenar los lotes en el orden recibido, respetando la planificación y el inicio mínimo como pisos
        List<LocalDate> inicios = encadenarInicios(duracionesEnDias,
                lotesPendientesEnOrden.stream().map(l -> l.getPlanificacionProduccion().getFechaInicioEstimada()).toList(),
                base, inicioMinimo);
        List<LoteEntity> lotesModificados = new ArrayList<>();
        for (int i = 0; i < lotesPendientesEnOrden.size(); i++) {
            LoteEntity lotePendiente = lotesPendientesEnOrden.get(i);
            LocalDate inicio = inicios.get(i);
            LocalDate fin = inicio.plusDays(duracionesEnDias.get(i));

            if (!inicio.equals(lotePendiente.getFechaInicioEstimada()) || !fin.equals(lotePendiente.getFechaFinalizacionEstimada())) {
                lotePendiente.setFechaInicioEstimada(inicio);
                lotePendiente.setFechaFinalizacionEstimada(fin);
                lotesModificados.add(lotePendiente);
            }
        }

        // 5. Persistir solo los lotes cuyas fechas cambiaron
        if (!lotesModificados.isEmpty()) {
            loteRepository.saveAll(lotesModificados);
        }
    }

    /**
     * Cálculo puro del encadenado de una cola: el inicio de cada lote, en el orden recibido. Lo comparten el
     * reencadenado real ({@link #reencadenarLotesPendientes}) y el virtual del cronograma ({@link #calcularAtrasos}),
     * para que ambos sigan siempre la misma regla.
     * <p>
     * {@code inicio = max(base, inicio estimado de su planificación, inicio mínimo)} y la base del lote
     * siguiente es {@code inicio + duración + 2 días} (el día {@code fin + 1} es la limpieza del fermentador).
     * Una planificación sin fecha de inicio no impone piso.
     * </p>
     *
     * @param duracionesEnDias Duración de cada lote, en días.
     * @param iniciosDePlanificacion Fecha de inicio estimada de la planificación de cada lote.
     * @param base Primer día en que el fermentador está libre.
     * @param inicioMinimo Piso de toda la cadena.
     * @return El inicio de cada lote, en el mismo orden.
     */
    static List<LocalDate> encadenarInicios(List<Long> duracionesEnDias, List<LocalDate> iniciosDePlanificacion,
                                            LocalDate base, LocalDate inicioMinimo) {
        List<LocalDate> inicios = new ArrayList<>(duracionesEnDias.size());
        LocalDate baseDelLote = base;
        for (int i = 0; i < duracionesEnDias.size(); i++) {
            LocalDate inicioDePlanificacion = iniciosDePlanificacion.get(i) == null ? inicioMinimo : iniciosDePlanificacion.get(i);
            LocalDate inicio = maxima(baseDelLote, inicioDePlanificacion, inicioMinimo);
            inicios.add(inicio);
            baseDelLote = inicio.plusDays(duracionesEnDias.get(i)).plusDays(DIAS_MARGEN_LIMPIEZA);
        }
        return inicios;
    }

    private long calcularDuracionEnDias(LoteEntity lote) {
        if (lote.getFechaInicioEstimada() == null || lote.getFechaFinalizacionEstimada() == null) {
            throw new ReglaNegocioException("No se pueden reordenar las fechas de la cola del fermentador porque el lote "
                    + lote.getIdentificadorInterno() + " no tiene sus fechas estimadas de inicio y finalización");
        }
        long duracion = ChronoUnit.DAYS.between(lote.getFechaInicioEstimada(), lote.getFechaFinalizacionEstimada());
        if (duracion < 0) {
            throw new ReglaNegocioException("No se pueden reordenar las fechas de la cola del fermentador porque el lote "
                    + lote.getIdentificadorInterno() + " tiene una fecha de finalización estimada anterior a la de inicio");
        }
        return duracion;
    }

    private static LocalDate maxima(LocalDate primera, LocalDate segunda, LocalDate tercera) {
        return Collections.max(List.of(primera, segunda, tercera));
    }
}
