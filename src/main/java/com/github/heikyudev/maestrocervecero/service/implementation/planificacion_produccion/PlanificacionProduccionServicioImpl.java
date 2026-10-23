package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.SugerenciaFechaFinalizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperPlanificacionProduccion;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperReceta;
import com.github.heikyudev.maestrocervecero.util.method.MetodosVersionado;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlanificacionProduccionServicioImpl implements IPlanificacionProduccionServicio {

    // Constantes de la estimación de la fecha de finalización (ver sugerirFechaFinalizacion)
    private static final int MINUTOS_POR_DIA = 1440;
    private static final int DIAS_MARGEN_LIMPIEZA_FERMENTADOR = 2;
    private static final String MENSAJE_RANGO_PLANIFICACION_EXCEDIDO = "La planificación no puede durar más de 5 años (1.825 días) entre las fechas de inicio y de finalización estimadas";
    private static final String MENSAJE_FECHA_INICIO_ANTERIOR_A_HOY = "La fecha de inicio estimada no puede ser anterior a la fecha actual";

    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IRecetaRepository recetaRepository;
    private final ILoteRepository loteRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IConfiguracionProduccionRepository configuracionProduccionRepository;

    /**
     * Recupera una página de planificaciones de producción registradas en el sistema, filtradas
     * opcionalmente por el ID de la receta contenedora de la versión utilizada, por estado y/o
     * por fecha de inicio estimada (los tres por coincidencia exacta). Un parámetro nulo no
     * restringe por ese criterio.
     * <p>
     * Esta entidad no tiene baja lógica: no existe el concepto de "planificación inactiva", por
     * lo que no se aplica ningún filtro adicional de estado activo/baja.
     * </p>
     *
     * @param idReceta ID de la receta contenedora cuya versión se usó, o {@code null} para no filtrar por ella.
     * @param estado Estado exacto a filtrar, o {@code null} para no filtrar por estado.
     * @param fechaInicio Fecha de inicio estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link PlanificacionProduccionResponseDTO} correspondientes, cada uno con sus litros producidos (0.0 si no tiene envasados registrados).
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PlanificacionProduccionResponseDTO> filtrarPlanificacionesProduccion(Long idReceta, EstadoSolicitud estado, LocalDate fechaInicio, Pageable pageable) {
        Page<PlanificacionProduccionEntity> pagina = planificacionProduccionRepository.filtrarPlanificacionesProduccion(idReceta, estado, fechaInicio, pageable);

        // Una sola consulta de agregación para todas las planificaciones de la página (evita una consulta por fila)
        List<Long> ids = pagina.getContent().stream().map(PlanificacionProduccionEntity::getId).toList();
        Map<Long, Double> litrosProducidosPorId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] fila : envasadoLoteRepository.sumarLitrosProducidosPorPlanificacion(ids)) {
                litrosProducidosPorId.put(((Number) fila[0]).longValue(), ((Number) fila[1]).doubleValue());
            }
        }

        return pagina.map(p -> MapperPlanificacionProduccion.toDTO(p, litrosProducidosPorId.getOrDefault(p.getId(), 0.0)));
    }

    /**
     * Busca y retorna una planificación de producción específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la planificación de producción buscada.
     * @return Objeto {@link PlanificacionProduccionResponseDTO} con la información de la planificación de producción encontrada, incluidos sus litros producidos.
     * @throws RecursoNoEncontradoException Si no existe ninguna planificación de producción activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public PlanificacionProduccionResponseDTO buscarPorId(Long id) {
        PlanificacionProduccionEntity planificacion = planificacionProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + id));
        return MapperPlanificacionProduccion.toDTO(planificacion, envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(id));
    }

    /**
     * Obtiene la versión de receta exacta que usa una planificación de producción (la asociada al registrarla,
     * no la versión vigente de la receta), junto con su posición entre las versiones de su receta.
     * <p>
     * Se resuelve con una única consulta de la planificación que trae la versión y su receta, sin pasar por
     * {@code IRecetaRepository}: su búsqueda por ID solo encuentra recetas activas y una receta dada de baja
     * debe seguir viéndose. El estado de la planificación no condiciona la consulta ni se calculan sus litros
     * producidos, porque la pantalla no los muestra.
     * </p>
     * <p>
     * El número de versión no es una columna: es la posición de la versión entre las de su receta ordenadas
     * por ID (el orden de registro, 1 es la primera).
     * </p>
     *
     * @param idPlanificacion Identificador de la planificación de producción.
     * @return {@link VersionRecetaDePlanificacionResponseDTO} con la versión completa, su número, la cantidad de versiones de la receta y el estado de la receta.
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public VersionRecetaDePlanificacionResponseDTO buscarVersionRecetaDePlanificacion(Long idPlanificacion) {
        PlanificacionProduccionEntity planificacion = planificacionProduccionRepository.buscarConVersionReceta(idPlanificacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacion));

        VersionRecetaEntity versionReceta = planificacion.getVersionReceta();
        RecetaEntity receta = versionReceta.getReceta();

        // Posición de la versión por orden de registro (ID ascendente), sin depender del orden de la lista de la entidad
        long numeroVersion = receta.getVersiones().stream()
                .filter(v -> v.getId() < versionReceta.getId())
                .count() + 1;

        return VersionRecetaDePlanificacionResponseDTO.builder()
                .idPlanificacion(planificacion.getId())
                .idReceta(receta.getId())
                .estadoReceta(receta.getEstado())
                .numeroVersion((int) numeroVersion)
                .cantidadVersiones(receta.getVersiones().size())
                .version(MapperReceta.toDTO(versionReceta))
                .build();
    }

    /**
     * Registra una nueva planificación de producción en el sistema.
     * <p>
     * El usuario elige una receta, no una versión: las versiones son un detalle interno para
     * preservar la trazabilidad de producción. Es responsabilidad de este método resolver cuál
     * es la última versión activa de la receta indicada y asociar esa versión puntual a la planificación.
     * </p>
     *
     * @param planificacionProduccionFormDTO Objeto DTO que contiene los datos de creación de la planificación de producción.
     * @return {@link PlanificacionProduccionResponseDTO} representativo de la planificación de producción guardada en la base de datos, en estado {@code PENDIENTE}.
     * @throws ReglaNegocioException Si la cantidad a producir no es un número finito mayor a 0 o supera el máximo técnico, si la fecha de inicio estimada es anterior a la fecha actual (hoy es válido), si la fecha de finalización estimada es anterior a la fecha de inicio estimada, o si entre ambas pasan más de 1.825 días (5 años).
     * @throws RecursoNoEncontradoException Si la receta referenciada no existe, o si no tiene ninguna versión activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PLANIFICACION_PRODUCCION)
    public PlanificacionProduccionResponseDTO registrarPlanificacionProduccion(PlanificacionProduccionFormDTO planificacionProduccionFormDTO) {
        // 1. Validar la cantidad a producir
        if (planificacionProduccionFormDTO.getCantidadAProducir() == null) {
            throw new ReglaNegocioException("La cantidad a producir es obligatoria");
        }
        if (planificacionProduccionFormDTO.getCantidadAProducir() <= 0) {
            throw new ReglaNegocioException("La cantidad a producir debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(planificacionProduccionFormDTO.getCantidadAProducir(),
                LimitesTecnicos.CANTIDAD_A_PRODUCIR_MAXIMA_LITROS, "La cantidad a producir", "litros");

        // 2. Validar que se hayan informado ambas fechas
        LocalDate fechaInicioEstimada = planificacionProduccionFormDTO.getFechaInicioEstimada();
        LocalDate fechaFinalizacionEstimada = planificacionProduccionFormDTO.getFechaFinalizacionEstimada();
        if (fechaInicioEstimada == null) {
            throw new ReglaNegocioException("La fecha de inicio estimada es obligatoria");
        }
        if (fechaFinalizacionEstimada == null) {
            throw new ReglaNegocioException("La fecha de finalización estimada es obligatoria");
        }

        // 3. Validar que la fecha de inicio estimada no sea anterior a la fecha actual (hoy es válido)
        if (fechaInicioEstimada.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException(MENSAJE_FECHA_INICIO_ANTERIOR_A_HOY);
        }

        // 4. Validar que la fecha de finalización estimada no sea anterior a la fecha de inicio estimada
        if (fechaFinalizacionEstimada.isBefore(fechaInicioEstimada)) {
            throw new ReglaNegocioException("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada");
        }

        // 4 bis. Validar que el rango entre las fechas de inicio y de finalización estimadas no supere el máximo permitido
        if (ChronoUnit.DAYS.between(fechaInicioEstimada, fechaFinalizacionEstimada) > LimitesTecnicos.RANGO_MAXIMO_PLANIFICACION_DIAS) {
            throw new ReglaNegocioException(MENSAJE_RANGO_PLANIFICACION_EXCEDIDO);
        }

        // 5. Resolver la última versión activa de la receta seleccionada
        VersionRecetaEntity versionRecetaEntity = obtenerVersionActiva(planificacionProduccionFormDTO.getIdReceta());

        // 6. Crear la entidad de la planificación de producción a partir del DTO de formulario
        PlanificacionProduccionEntity planificacionProduccionEntity = PlanificacionProduccionEntity.builder()
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionEstimada(fechaFinalizacionEstimada)
                .cantidadAProducir(planificacionProduccionFormDTO.getCantidadAProducir())
                .estado(EstadoSolicitud.PENDIENTE)
                .versionReceta(versionRecetaEntity)
                .build();

        // 7. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.save(planificacionProduccionEntity));
    }

    /**
     * Anula una planificación de producción existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre planificaciones en estado {@code PENDIENTE}:
     * una planificación ya finalizada o ya anulada no puede volver a anularse.
     * </p>
     *
     * @param id Identificador clave primaria de la planificación de producción a anular.
     * @param anulacionFormDTO DTO que contiene el motivo de la anulación.
     * @return {@link PlanificacionProduccionResponseDTO} representativo de la planificación de producción anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera los 255 caracteres, si la planificación no se encuentra en estado {@code PENDIENTE}, si tiene al menos un lote en estado {@code PENDIENTE} o {@code EN_EJECUCION} asociado, o si tiene al menos un lote en estado {@code FINALIZADO} asociado.
     * @throws RecursoNoEncontradoException Si la planificación de producción con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.PLANIFICACION_PRODUCCION)
    public PlanificacionProduccionResponseDTO anularPlanificacionProduccion(Long id, AnulacionPlanificacionProduccionFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        String motivoAnulacion = NormalizacionTexto.nombrePropio(anulacionFormDTO.getMotivoAnulacion());
        if (motivoAnulacion == null || motivoAnulacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoAnulacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de anulación");

        // 2. Localizar la planificación de producción, bloqueándola para escritura (un lote no puede registrarse sobre ella mientras
        //    se anula o finaliza). Si no existe, se dispara RecursoNoEncontradoException
        PlanificacionProduccionEntity planificacionProduccionEntity = planificacionProduccionRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + id));

        // 3. Validar que la planificación se encuentre en estado PENDIENTE
        if (planificacionProduccionEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden anular planificaciones de producción en estado Pendiente");
        }

        // 4. Validar que no tenga lotes pendientes o en ejecución: primero hay que cancelarlos
        if (loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(id, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))) {
            throw new ReglaNegocioException("No se puede anular la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado");
        }

        // 5. Validar que no tenga ningún lote finalizado: la anulación es para errores de carga,
        //    no para producción que ya ocurrió
        if (loteRepository.existsByPlanificacionProduccion_IdAndEstado(id, EstadoLote.FINALIZADO)) {
            throw new ReglaNegocioException("No se puede anular la planificación de producción porque tiene al menos un lote en estado Finalizado asociado");
        }

        // 6. Aplicar la anulación sobre la entidad administrada por persistencia
        planificacionProduccionEntity.setMotivoAnulacion(motivoAnulacion);
        planificacionProduccionEntity.setFechaAnulacion(LocalDateTime.now());
        planificacionProduccionEntity.setEstado(EstadoSolicitud.ANULADA);

        // 7. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.save(planificacionProduccionEntity));
    }

    /**
     * Finaliza de forma forzada una planificación de producción existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre planificaciones en estado {@code PENDIENTE}:
     * una planificación ya finalizada o ya anulada no puede volver a finalizarse. La finalización normal
     * (litros producidos mayor o igual a la cantidad solicitada, sin lotes pendientes ni en ejecución) ocurre
     * sola al finalizar o cancelar el último lote de la planificación, no mediante este método.
     * </p>
     *
     * @param id Identificador clave primaria de la planificación de producción a finalizar.
     * @param finalizacionFormDTO DTO que contiene el motivo de la finalización forzada.
     * @return {@link PlanificacionProduccionResponseDTO} representativo de la planificación de producción finalizada.
     * @throws ReglaNegocioException Si el motivo de finalización no fue informado o supera los 255 caracteres, si la planificación no se encuentra en estado {@code PENDIENTE}, si tiene al menos un lote en estado {@code PENDIENTE} o {@code EN_EJECUCION} asociado, o si no tiene ningún lote en estado {@code FINALIZADO} asociado.
     * @throws RecursoNoEncontradoException Si la planificación de producción con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.FINALIZAR, conceptoAuditoria = ConceptoAuditoria.PLANIFICACION_PRODUCCION)
    public PlanificacionProduccionResponseDTO finalizarPlanificacionProduccion(Long id, FinalizacionForzadaPlanificacionProduccionFormDTO finalizacionFormDTO) {
        // 1. Validar que se haya informado el motivo de finalización
        String motivoFinalizacion = NormalizacionTexto.nombrePropio(finalizacionFormDTO.getMotivoFinalizacion());
        if (motivoFinalizacion == null || motivoFinalizacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de finalización es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoFinalizacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de finalización");

        // 2. Localizar la planificación de producción, bloqueándola para escritura (un lote no puede registrarse sobre ella mientras
        //    se anula o finaliza). Si no existe, se dispara RecursoNoEncontradoException
        PlanificacionProduccionEntity planificacionProduccionEntity = planificacionProduccionRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + id));

        // 3. Validar que la planificación se encuentre en estado PENDIENTE
        if (planificacionProduccionEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden finalizar planificaciones de producción en estado Pendiente");
        }

        // 4. Validar que no tenga lotes pendientes o en ejecución: primero hay que cancelarlos
        if (loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(id, List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION))) {
            throw new ReglaNegocioException("No se puede finalizar la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado");
        }

        // 5. Validar que tenga al menos un lote finalizado: la finalización forzada cierra una
        //    planificación que sí llegó a producir algo pero no completó la cantidad solicitada;
        //    si no produjo nada, corresponde anularla en lugar de finalizarla
        if (!loteRepository.existsByPlanificacionProduccion_IdAndEstado(id, EstadoLote.FINALIZADO)) {
            throw new ReglaNegocioException("No se puede finalizar la planificación de producción porque no tiene ningún lote en estado Finalizado asociado: corresponde anularla en su lugar");
        }

        // 6. Aplicar la finalización forzada sobre la entidad administrada por persistencia
        planificacionProduccionEntity.setMotivoFinalizacion(motivoFinalizacion);
        planificacionProduccionEntity.setFechaFinalizacion(LocalDateTime.now());
        planificacionProduccionEntity.setEstado(EstadoSolicitud.FINALIZADA);

        // 7. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.save(planificacionProduccionEntity));
    }

    /**
     * Sugiere una fecha de finalización para una planificación que todavía no se registró,
     * simulando la producción secuencial de todos sus lotes en un único fermentador.
     * <p>
     * Es solo una estimación: no persiste nada. La cantidad de lotes es la cantidad a producir
     * dividida por la capacidad de lote estándar (redondeada hacia arriba); cada lote dura el día de
     * brewing (maceración + hervido, redondeado hacia arriba a días completos) más la fermentación y
     * la maduración de la última versión activa de la receta; y cada lote comienza dos días después
     * de que termina el anterior (el día intermedio es la limpieza del fermentador).
     * </p>
     *
     * @param idReceta ID de la receta contenedora cuya última versión activa se usa para la simulación.
     * @param cantidadAProducir Cantidad total a producir, en litros; debe ser mayor a 0.
     * @param fechaInicioEstimada Fecha en que comenzaría el primer lote; obligatoria.
     * @return {@link SugerenciaFechaFinalizacionResponseDTO} con la fecha de finalización sugerida y los datos de la simulación.
     * @throws ReglaNegocioException Si la cantidad a producir es nula, no es finita, no es mayor a 0 o supera el máximo técnico, si la fecha de inicio estimada es nula o anterior a la fecha actual (hoy es válido), si la capacidad de lote estándar configurada no es mayor a 0, o si la simulación supera los 1.825 días (5 años) entre el inicio y la finalización sugerida.
     * @throws RecursoNoEncontradoException Si la receta no existe (o está dada de baja), si no tiene una versión activa, o si no se encuentra la configuración de producción.
     */
    @Override
    @Transactional(readOnly = true)
    public SugerenciaFechaFinalizacionResponseDTO sugerirFechaFinalizacion(Long idReceta, Double cantidadAProducir, LocalDate fechaInicioEstimada) {
        // 1. Validar la cantidad a producir
        if (cantidadAProducir == null) {
            throw new ReglaNegocioException("La cantidad a producir es obligatoria");
        }
        if (cantidadAProducir <= 0) {
            throw new ReglaNegocioException("La cantidad a producir debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACero(cantidadAProducir, "La cantidad a producir");

        // 2. Validar que se haya informado la fecha de inicio estimada
        if (fechaInicioEstimada == null) {
            throw new ReglaNegocioException("La fecha de inicio estimada es obligatoria");
        }

        // 3. Validar que la fecha de inicio estimada no sea anterior a la fecha actual (hoy es válido)
        if (fechaInicioEstimada.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException(MENSAJE_FECHA_INICIO_ANTERIOR_A_HOY);
        }

        // 4. Resolver la última versión activa de la receta seleccionada
        VersionRecetaEntity versionRecetaEntity = obtenerVersionActiva(idReceta);

        // 5. Localizar la configuración de producción vigente
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));

        // 6. Validar la capacidad de lote estándar (defensivo: la configuración ya lo exige al guardarse)
        Double capacidadLoteEstandar = configuracion.getCapacidadLoteEstandar();
        if (capacidadLoteEstandar == null) {
            throw new ReglaNegocioException("La capacidad de lote estándar configurada es obligatoria");
        }
        if (capacidadLoteEstandar <= 0) {
            throw new ReglaNegocioException("La capacidad de lote estándar configurada debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACero(capacidadLoteEstandar, "La capacidad de lote estándar configurada");

        // 7. Cantidad de lotes: división decimal exacta redondeada hacia arriba. No se usa Math.ceil
        //    sobre la división de doubles porque el error de punto flotante puede inflar un cociente
        //    exacto (ej. 600 / 300) y sumar un lote de más; con BigDecimal los múltiplos exactos dan el entero justo.
        BigDecimal lotes = BigDecimal.valueOf(cantidadAProducir)
                .divide(BigDecimal.valueOf(capacidadLoteEstandar), 0, RoundingMode.CEILING);
        if (lotes.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new ReglaNegocioException("La cantidad a producir es demasiado grande para la capacidad de lote estándar configurada");
        }
        int cantidadLotes = lotes.intValue();

        // 7 bis. Validar el techo técnico de la cantidad a producir (después de la cantidad de lotes: una
        //        cantidad absurda frente a la capacidad se informa como tal, con su mensaje propio)
        ValidacionesNumericas.validarMaximo(cantidadAProducir, LimitesTecnicos.CANTIDAD_A_PRODUCIR_MAXIMA_LITROS, "La cantidad a producir", "litros");

        // 8. Duración de cada lote en días: día de brewing (maceración + hervido, en minutos, hacia arriba
        //    a días completos; división entera con redondeo hacia arriba) + fermentación + maduración
        long minutosBrewDay = (long) Objects.requireNonNullElse(versionRecetaEntity.getDuracionMaceracion(), 0)
                + Objects.requireNonNullElse(versionRecetaEntity.getDuracionHervido(), 0);
        long diasBrewDay = (minutosBrewDay + MINUTOS_POR_DIA - 1) / MINUTOS_POR_DIA;
        long duracionLoteDias = diasBrewDay
                + Objects.requireNonNullElse(versionRecetaEntity.getDuracionFermentacion(), 0)
                + Objects.requireNonNullElse(versionRecetaEntity.getDuracionMaduracion(), 0);

        // 9. Fecha de finalización: lotes secuenciales en un único fermentador. El lote k termina
        //    duracionLoteDias después de su inicio y el siguiente empieza dos días después (el día
        //    intermedio es la limpieza), por lo que el último termina en: inicio + N * duración + (N - 1) * margen.
        //    Los días se calculan con aritmética long y se comparan contra el rango máximo antes de
        //    construir la fecha, para que una simulación absurda no desborde el LocalDate
        long diasTotales = cantidadLotes * duracionLoteDias + (long) (cantidadLotes - 1) * DIAS_MARGEN_LIMPIEZA_FERMENTADOR;
        if (diasTotales > LimitesTecnicos.RANGO_MAXIMO_PLANIFICACION_DIAS) {
            throw new ReglaNegocioException("La simulación supera los 5 años (" + cantidadLotes + " lotes de " + duracionLoteDias
                    + " días). Reduzca la cantidad a producir o aumente la capacidad de lote estándar en la configuración");
        }
        LocalDate fechaFinalizacionSugerida = fechaInicioEstimada.plusDays(diasTotales);

        // 10. Armar la sugerencia (nada se persiste)
        return SugerenciaFechaFinalizacionResponseDTO.builder()
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionSugerida(fechaFinalizacionSugerida)
                .cantidadLotes(cantidadLotes)
                .capacidadLoteEstandar(capacidadLoteEstandar)
                .duracionLoteDias(duracionLoteDias)
                .build();
    }

    /**
     * Resuelve la última versión activa ({@code esUltimaVersion = true}) de la receta indicada.
     *
     * @param idReceta Identificador de la receta cuya última versión activa se quiere obtener.
     * @return Entidad de la última versión activa de la receta.
     * @throws RecursoNoEncontradoException Si la receta no existe, o si no tiene ninguna versión activa.
     */
    private VersionRecetaEntity obtenerVersionActiva(Long idReceta) {
        RecetaEntity recetaEntity = recetaRepository.findById(idReceta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + idReceta));

        return MetodosVersionado.obtenerVersionActiva(recetaEntity.getVersiones(),
                () -> new RecursoNoEncontradoException("No se encontró una versión activa para la receta con ID: " + idReceta));
    }
}
