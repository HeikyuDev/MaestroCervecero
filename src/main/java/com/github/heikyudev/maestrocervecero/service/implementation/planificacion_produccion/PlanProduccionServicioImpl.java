package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.LotePlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.lote.EstimacionLote;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.GeneradorPlanes.Escenario;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.GeneradorPlanes.FermentadorLibre;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.GeneradorPlanes.LoteDefinido;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.GeneradorPlanes.LotePlanificado;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.LotePlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanesProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionConLitrosAPlanificarResponseDTO;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperPlanProduccion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Implementación de {@link IPlanProduccionServicio}.
 * <p>
 * Orquesta la generación: carga la planificación, la configuración y los equipos activos, arma el escenario y delega
 * el cálculo en {@link GeneradorPlanes}, que es puro. La confirmación registra cada lote con
 * {@link ILoteServicio#registrarLote}, que es otro bean: su transacción se une a la de la confirmación y cualquier
 * rechazo revierte todos los lotes del plan.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PlanProduccionServicioImpl implements IPlanProduccionServicio {

    private static final String MENSAJE_PLANIFICACION_NO_PENDIENTE = "La planificación de producción debe encontrarse en estado Pendiente para generar o confirmar un plan de producción";

    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final ILoteRepository loteRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IConfiguracionProduccionRepository configuracionProduccionRepository;
    private final IFermentadorRepository fermentadorRepository;
    private final IMolinoRepository molinoRepository;
    private final IMaceradorRepository maceradorRepository;
    private final IOllaHervorRepository ollaHervorRepository;
    private final IEscaladoInsumoServicio escaladoInsumoServicio;
    private final ILoteServicio loteServicio;
    private final IEquipamientoRepository equipamientoRepository;

    /** Resultado de resolver el escenario de una generación: el generador listo o el motivo por el que no hay planes. */
    private record ResolucionEscenario(GeneradorPlanes generador, String motivoNoDisponible) {
    }

    /** Equipos activos resueltos de un lote del formulario. */
    private record EquiposDeLote(FermentadorEntity fermentador, MolinoEntity molino, MaceradorEntity macerador, OllaHervorEntity ollaHervor) {
    }

    /**
     * Genera los planes secuencial y concurrente de una planificación. Ver {@link IPlanProduccionServicio#generarPlanes(Long)}.
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @return Los dos planes de la planificación (disponibles o con el motivo por el que no lo están).
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public PlanesProduccionResponseDTO generarPlanes(Long idPlanificacion) {
        // 1. Recuperar la planificación y la configuración, y calcular los litros que le faltan planificar
        PlanificacionProduccionEntity planificacion = buscarPlanificacion(idPlanificacion);
        ConfiguracionProduccionEntity configuracion = buscarConfiguracion();
        double litrosAPlanificar = Math.max(0.0, calcularLitrosAPlanificar(planificacion));

        // 2. Resolver el escenario: la planificación admite un plan y hay equipos para generarlo
        ResolucionEscenario resolucion = resolverEscenario(planificacion, configuracion, litrosAPlanificar);

        // 3. Generar cada plan (o informar por qué no hay planes)
        PlanProduccionResponseDTO secuencial;
        PlanProduccionResponseDTO concurrente;
        if (resolucion.motivoNoDisponible() != null) {
            secuencial = GeneradorPlanes.noDisponible(ModalidadPlanProduccion.SECUENCIAL, resolucion.motivoNoDisponible(), litrosAPlanificar);
            concurrente = GeneradorPlanes.noDisponible(ModalidadPlanProduccion.CONCURRENTE, resolucion.motivoNoDisponible(), litrosAPlanificar);
        } else {
            secuencial = resolucion.generador().generarSecuencial();
            concurrente = resolucion.generador().generarConcurrente();
        }

        return PlanesProduccionResponseDTO.builder()
                .idPlanificacion(idPlanificacion)
                .litrosAPlanificar(litrosAPlanificar)
                .excedenteMaximoLitros(configuracion.getExcedenteMaximoLitros())
                .secuencial(secuencial)
                .concurrente(concurrente)
                .build();
    }

    /**
     * Indica por qué una planificación no admite planes de producción. Ver
     * {@link IPlanProduccionServicio#buscarMotivoSinLitrosAPlanificar(Long)}.
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @return El motivo, o vacío si la planificación todavía tiene litros por planificar.
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<String> buscarMotivoSinLitrosAPlanificar(Long idPlanificacion) {
        PlanificacionProduccionEntity planificacion = buscarPlanificacion(idPlanificacion);
        if (planificacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            return Optional.of(MENSAJE_PLANIFICACION_NO_PENDIENTE);
        }
        if (calcularLitrosAPlanificar(planificacion) <= 0) {
            return Optional.of(mensajePlanificacionCubierta(planificacion));
        }
        return Optional.empty();
    }

    /**
     * Obtiene las planificaciones Pendiente con litros sin planificar. Ver
     * {@link IPlanProduccionServicio#buscarPlanificacionesConLitrosAPlanificar()}.
     * <p>
     * Los litros a planificar se calculan con el mismo método que la generación y la confirmación
     * ({@code calcularLitrosAPlanificar}), de modo que el aviso y los planes nunca difieren.
     * </p>
     *
     * @return Las planificaciones con litros sin planificar, de la más urgente a la menos urgente.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PlanificacionConLitrosAPlanificarResponseDTO> buscarPlanificacionesConLitrosAPlanificar() {
        List<PlanificacionConLitrosAPlanificarResponseDTO> resultado = new ArrayList<>();
        for (PlanificacionProduccionEntity planificacion : planificacionProduccionRepository.buscarPendientesConVersionReceta()) {
            double litrosAPlanificar = calcularLitrosAPlanificar(planificacion);
            if (litrosAPlanificar > 0) {
                resultado.add(MapperPlanProduccion.toPlanificacionConLitrosAPlanificarDTO(planificacion, litrosAPlanificar));
            }
        }
        return resultado;
    }

    /**
     * Recalcula un plan con los lotes indicados, en el orden dado. Ver
     * {@link IPlanProduccionServicio#recalcularPlan(Long, PlanProduccionFormDTO)}.
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @param planProduccionFormDTO La modalidad y los lotes del plan.
     * @return El plan recalculado, con sus avisos.
     * @throws ReglaNegocioException Si el plan o alguno de sus lotes no tiene los datos obligatorios, si supera el máximo de lotes o si algún equipo del plan fue dado de baja después de generarlo.
     * @throws RecursoNoEncontradoException Si no existe la planificación o alguno de los equipos activos indicados.
     */
    @Override
    @Transactional(readOnly = true)
    public PlanProduccionResponseDTO recalcularPlan(Long idPlanificacion, PlanProduccionFormDTO planProduccionFormDTO) {
        // 1. Validar los datos del plan antes de consultar nada
        validarFormulario(planProduccionFormDTO);

        // 2. Recuperar la planificación, la configuración y los litros que le faltan planificar
        PlanificacionProduccionEntity planificacion = buscarPlanificacion(idPlanificacion);
        ConfiguracionProduccionEntity configuracion = buscarConfiguracion();
        double litrosAPlanificar = Math.max(0.0, calcularLitrosAPlanificar(planificacion));
        VersionRecetaEntity versionReceta = planificacion.getVersionReceta();

        List<String> avisos = new ArrayList<>();
        if (planificacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            avisos.add(MENSAJE_PLANIFICACION_NO_PENDIENTE);
        } else if (litrosAPlanificar <= 0) {
            avisos.add(mensajePlanificacionCubierta(planificacion));
        }

        // 3. Resolver los equipos de cada lote y avisar los límites de capacidad que el registro rechazaría
        List<LotePlanProduccionFormDTO> lotesForm = planProduccionFormDTO.getLotes();
        List<LoteDefinido> lotes = new ArrayList<>();
        for (int i = 0; i < lotesForm.size(); i++) {
            validarEquiposNoDadosDeBaja(i, lotesForm.get(i));
            EquiposDeLote equipos = resolverEquipos(i, lotesForm.get(i));
            double volumen = Redondeo.redondear(lotesForm.get(i).getVolumenObjetivo());
            double masaMalta = calcularMasaMalta(volumen, equipos.macerador(), versionReceta);
            avisos.addAll(avisosDeCapacidad(prefijoDeLote(i), volumen, masaMalta, equipos, versionReceta));
            lotes.add(new LoteDefinido(equipos.fermentador(), volumen, equipos.molino(), equipos.macerador(), equipos.ollaHervor(), masaMalta));
        }

        // 4. Encadenar las fechas de los lotes en el orden dado, en el cronograma de su fermentador
        List<LotePlanificado> planificados = encadenarFechas(lotes, planificacion, configuracion);
        List<LotePlanProduccionResponseDTO> lotesDTO = IntStream.range(0, planificados.size())
                .mapToObj(i -> GeneradorPlanes.aLoteDTO(i + 1, planificados.get(i)))
                .toList();

        // 5. Avisar si el plan termina después de la fecha de finalización estimada de la planificación (no impide confirmarlo)
        GeneradorPlanes.avisoDeFinalizacionTardia(lotesDTO.stream().map(LotePlanProduccionResponseDTO::getFechaFinalizacionEstimada)
                .max(LocalDate::compareTo).orElse(null), planificacion.getFechaFinalizacionEstimada()).ifPresent(avisos::add);

        // 6. Agregar como avisos lo que la confirmación rechazaría del volumen total del plan
        if (litrosAPlanificar > 0) {
            avisos.addAll(GeneradorPlanes.problemasDelVolumenTotal(lotes.stream().map(LoteDefinido::volumen).toList(),
                    litrosAPlanificar, configuracion.getExcedenteMaximoLitros()));
        }

        return GeneradorPlanes.armarPlan(planProduccionFormDTO.getModalidad(), lotesDTO, litrosAPlanificar, avisos);
    }

    /**
     * Confirma un plan registrando todos sus lotes en una única transacción. Ver
     * {@link IPlanProduccionServicio#confirmarPlan(Long, PlanProduccionFormDTO)}.
     * <p>
     * Antes de registrar, bloquea para escritura todos los equipos distintos del plan en el orden canónico: los
     * fermentadores, luego los molinos, los maceradores y las ollas de hervor, cada grupo por ID ascendente. Con los
     * equipos bloqueados valida que ninguno haya sido dado de baja después de generar el plan. Así dos
     * confirmaciones concurrentes adquieren los bloqueos siempre en el mismo orden y no se esperan
     * mutuamente. Después registra los lotes en el orden del plan, de modo que sus identificadores internos siguen ese
     * orden; el registro de cada lote vuelve a bloquear su fermentador, su cola de lotes pendientes y sus demás equipos
     * (los equipos ya están bloqueados por esta transacción, así que esos bloqueos no esperan). Bloquear la cola en el orden del plan no abre un interbloqueo: toda operación sobre la
     * cola de un fermentador bloquea antes ese fermentador, que esta transacción ya tiene.
     * </p>
     * <p>
     * El orden no puede romper la validación del registro que rechaza un lote cuando los anteriores ya cubren la
     * cantidad a producir: antes se valida que, sin el lote de menor volumen, los demás no cubren los litros a
     * planificar, por lo que la suma de los lotes previos a cualquiera de ellos (en cualquier orden) tampoco los cubre.
     * </p>
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @param planProduccionFormDTO La modalidad y los lotes del plan elegido.
     * @return Los lotes registrados, en el orden del plan.
     * @throws ReglaNegocioException Si el plan no cumple alguna regla, si algún equipo del plan fue dado de baja después de generarlo o si el registro de algún lote la rechaza.
     * @throws RecursoNoEncontradoException Si no existe la planificación o alguno de los equipos indicados.
     */
    @Override
    @Transactional
    public List<LoteResponseDTO> confirmarPlan(Long idPlanificacion, PlanProduccionFormDTO planProduccionFormDTO) {
        // 1. Validar los datos del plan antes de consultar nada
        validarFormulario(planProduccionFormDTO);

        // 2. Validar que la planificación exista y esté Pendiente (se bloquea para escritura, primer nivel del orden
        //    canónico de bloqueo: el registro de cada lote vuelve a bloquearla dentro de la misma transacción)
        PlanificacionProduccionEntity planificacion = planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacion));
        if (planificacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException(MENSAJE_PLANIFICACION_NO_PENDIENTE);
        }

        // 3. Validar que la planificación todavía tenga litros por planificar
        double litrosAPlanificar = calcularLitrosAPlanificar(planificacion);
        if (litrosAPlanificar <= 0) {
            throw new ReglaNegocioException(mensajePlanificacionCubierta(planificacion));
        }

        // 4. Validar el volumen total del plan: cubre los litros, no supera el excedente máximo y no le sobra ningún lote
        List<LotePlanProduccionFormDTO> lotes = planProduccionFormDTO.getLotes();
        List<Double> volumenes = lotes.stream().map(lote -> Redondeo.redondear(lote.getVolumenObjetivo())).toList();
        List<String> problemas = GeneradorPlanes.problemasDelVolumenTotal(volumenes, litrosAPlanificar, buscarConfiguracion().getExcedenteMaximoLitros());
        if (!problemas.isEmpty()) {
            throw new ReglaNegocioException(problemas.get(0));
        }

        // 5. Bloquear los equipos distintos de todo el plan en el orden canónico: primero los fermentadores, luego los
        //    molinos, los maceradores y las ollas de hervor, cada grupo por ID ascendente (el mismo orden por tipo que
        //    usan el inicio y la cancelación de un lote). Así dos confirmaciones concurrentes, o una confirmación y una
        //    operación de lote, nunca se esperan mutuamente. El resultado no se usa aquí: el registro de cada lote vuelve
        //    a buscar con bloqueo sus equipos (ya bloqueados por esta transacción) y rechaza el que no esté activo
        bloquearPorIdAscendente(lotes, LotePlanProduccionFormDTO::getIdFermentador, fermentadorRepository::buscarPorIdParaCambiarEstadoOperativo);
        bloquearPorIdAscendente(lotes, LotePlanProduccionFormDTO::getIdMolino, molinoRepository::buscarPorIdParaCambiarEstadoOperativo);
        bloquearPorIdAscendente(lotes, LotePlanProduccionFormDTO::getIdMacerador, maceradorRepository::buscarPorIdParaCambiarEstadoOperativo);
        bloquearPorIdAscendente(lotes, LotePlanProduccionFormDTO::getIdOllaHervor, ollaHervorRepository::buscarPorIdParaCambiarEstadoOperativo);

        // 6. Validar que ningún equipo del plan haya sido dado de baja después de generarlo: el plan se debe generar de
        //    nuevo. Con los equipos ya bloqueados, una baja no puede confirmarse entre esta validación y el registro
        for (int indice = 0; indice < lotes.size(); indice++) {
            validarEquiposNoDadosDeBaja(indice, lotes.get(indice));
        }

        // 7. Registrar cada lote en el orden del plan con el registro de lotes (otro bean: su transacción se une a esta).
        //    Un rechazo se informa con el número del lote en el plan, conserva la causa y revierte los lotes ya registrados
        List<LoteResponseDTO> registrados = new ArrayList<>();
        for (int indice = 0; indice < lotes.size(); indice++) {
            try {
                registrados.add(loteServicio.registrarLote(MapperPlanProduccion.toLoteFormDTO(idPlanificacion, lotes.get(indice))));
            } catch (ReglaNegocioException e) {
                throw new ReglaNegocioException(prefijoDeLote(indice) + e.getMessage(), e);
            } catch (RecursoNoEncontradoException e) {
                throw new RecursoNoEncontradoException(prefijoDeLote(indice) + e.getMessage(), e);
            }
        }
        return registrados;
    }

    // ==================== Escenario ====================

    /**
     * Resuelve el escenario de la generación: si la planificación no está Pendiente o no le quedan litros por planificar
     * no hay planes; si no, carga los equipos activos y, de cada fermentador, la fecha de fin del lote detrás del cual el
     * registro ubicaría un lote de la planificación según su prioridad, y arma el generador (que a su vez informa si
     * faltan equipos o capacidad).
     */
    private ResolucionEscenario resolverEscenario(PlanificacionProduccionEntity planificacion, ConfiguracionProduccionEntity configuracion,
                                                  double litrosAPlanificar) {
        if (planificacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            return new ResolucionEscenario(null, MENSAJE_PLANIFICACION_NO_PENDIENTE);
        }
        if (litrosAPlanificar <= 0) {
            return new ResolucionEscenario(null, mensajePlanificacionCubierta(planificacion));
        }

        VersionRecetaEntity versionReceta = planificacion.getVersionReceta();
        List<FermentadorEntity> activos = fermentadorRepository.buscarActivos();
        Map<Long, LocalDate> finLoteAnterior = buscarFinDelLoteAnteriorEnCola(planificacion, activos.stream().map(FermentadorEntity::getId).toList());
        List<FermentadorLibre> fermentadores = activos.stream()
                .map(fermentador -> new FermentadorLibre(fermentador, finLoteAnterior.get(fermentador.getId())))
                .toList();
        Escenario escenario = new Escenario(versionReceta, planificacion.getFechaInicioEstimada(), planificacion.getFechaFinalizacionEstimada(), LocalDate.now(),
                configuracion.getVelocidadEstandarEnvasado(), litrosAPlanificar, configuracion.getExcedenteMaximoLitros(), fermentadores,
                molinoRepository.buscarActivos(), maceradorRepository.buscarActivos(), ollaHervorRepository.buscarActivos(),
                (volumen, macerador) -> calcularMasaMalta(volumen, macerador, versionReceta));
        GeneradorPlanes generador = new GeneradorPlanes(escenario);
        return new ResolucionEscenario(generador, generador.motivoNoDisponible());
    }

    /**
     * Rechaza un lote del plan si alguno de sus equipos fue dado de baja después de generar el plan. Un equipo
     * inexistente no se informa aquí: lo rechaza la búsqueda de equipos activos que sigue.
     *
     * @throws ReglaNegocioException Si algún equipo del lote está dado de baja (el mensaje indica el número de lote).
     */
    private void validarEquiposNoDadosDeBaja(int indice, LotePlanProduccionFormDTO lote) {
        for (Long idEquipo : List.of(lote.getIdFermentador(), lote.getIdMolino(), lote.getIdMacerador(), lote.getIdOllaHervor())) {
            equipamientoRepository.findById(idEquipo)
                    .map(equipo -> (EquipamientoEntity) Hibernate.unproxy(equipo))
                    .filter(equipo -> equipo.getEstado() == Estado.BAJA)
                    .ifPresent(equipo -> {
                        throw new ReglaNegocioException(prefijoDeLote(indice) + sujetoDe(equipo) + " " + equipo.getIdentificadorInterno()
                                + (equipo instanceof OllaHervorEntity ? " fue dada" : " fue dado") + " de baja, genere el plan nuevamente");
                    });
        }
    }

    /** Bloquea para escritura, por ID ascendente, los equipos distintos que indica el extractor en los lotes del plan. */
    private static void bloquearPorIdAscendente(List<LotePlanProduccionFormDTO> lotes, Function<LotePlanProduccionFormDTO, Long> idDelEquipo,
                                                Consumer<Long> bloqueo) {
        lotes.stream().map(idDelEquipo).distinct().sorted().forEach(bloqueo);
    }

    /** El sujeto con el que los mensajes nombran al equipo según su tipo ("El molino", "La olla de hervor"...). */
    private static String sujetoDe(EquipamientoEntity equipo) {
        return switch (TipoEquipamiento.deEquipamiento(equipo)) {
            case MOLINO -> "El molino";
            case MACERADOR -> "El macerador";
            case OLLA_HERVOR -> "La olla de hervor";
            case FERMENTADOR -> "El fermentador";
        };
    }

    /**
     * Resuelve los equipos activos de un lote del formulario.
     *
     * @throws RecursoNoEncontradoException Si algún equipo no existe o no está activo (el mensaje indica el número de lote).
     */
    private EquiposDeLote resolverEquipos(int indice, LotePlanProduccionFormDTO lote) {
        String prefijo = prefijoDeLote(indice);
        FermentadorEntity fermentador = fermentadorRepository.findById(lote.getIdFermentador())
                .orElseThrow(() -> new RecursoNoEncontradoException(prefijo + "No se encontró el fermentador con ID: " + lote.getIdFermentador()));
        MolinoEntity molino = molinoRepository.findById(lote.getIdMolino())
                .orElseThrow(() -> new RecursoNoEncontradoException(prefijo + "No se encontró el molino con ID: " + lote.getIdMolino()));
        MaceradorEntity macerador = maceradorRepository.findById(lote.getIdMacerador())
                .orElseThrow(() -> new RecursoNoEncontradoException(prefijo + "No se encontró el macerador con ID: " + lote.getIdMacerador()));
        OllaHervorEntity ollaHervor = ollaHervorRepository.findById(lote.getIdOllaHervor())
                .orElseThrow(() -> new RecursoNoEncontradoException(prefijo + "No se encontró la olla de hervor con ID: " + lote.getIdOllaHervor()));
        return new EquiposDeLote(fermentador, molino, macerador, ollaHervor);
    }

    /**
     * Encadena las fechas de los lotes en el orden dado: el primer lote del plan en cada fermentador parte del lote
     * detrás del cual el registro lo ubica según la prioridad de la planificación, y los siguientes, del lote anterior
     * del plan en ese fermentador.
     */
    private List<LotePlanificado> encadenarFechas(List<LoteDefinido> lotes, PlanificacionProduccionEntity planificacion,
                                                  ConfiguracionProduccionEntity configuracion) {
        List<Long> idsFermentadores = lotes.stream().map(lote -> lote.fermentador().getId()).distinct().toList();
        return GeneradorPlanes.encadenarFechas(lotes, buscarFinDelLoteAnteriorEnCola(planificacion, idsFermentadores),
                planificacion.getFechaInicioEstimada(), LocalDate.now(), planificacion.getVersionReceta(), configuracion.getVelocidadEstandarEnvasado());
    }

    // ==================== Validaciones ====================

    /**
     * Valida los datos obligatorios del plan y de cada lote, y el máximo de lotes. No consulta nada.
     *
     * @throws ReglaNegocioException Si falta la modalidad, si el plan no tiene lotes o supera el máximo, o si a un lote
     *                               le falta el volumen objetivo (o no es mayor a 0) o algún equipo.
     */
    private static void validarFormulario(PlanProduccionFormDTO planProduccionFormDTO) {
        if (planProduccionFormDTO.getModalidad() == null) {
            throw new ReglaNegocioException("La modalidad del plan es obligatoria");
        }
        List<LotePlanProduccionFormDTO> lotes = planProduccionFormDTO.getLotes();
        if (lotes == null || lotes.isEmpty()) {
            throw new ReglaNegocioException("El plan debe tener al menos un lote");
        }
        if (lotes.size() > LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES) {
            throw new ReglaNegocioException("El plan no puede tener más de " + LimitesTecnicos.PLAN_PRODUCCION_MAXIMO_LOTES + " lotes");
        }
        for (int i = 0; i < lotes.size(); i++) {
            LotePlanProduccionFormDTO lote = lotes.get(i);
            String prefijo = prefijoDeLote(i);
            if (lote == null) {
                throw new ReglaNegocioException(prefijo + "Los datos del lote son obligatorios");
            }
            if (lote.getVolumenObjetivo() == null) {
                throw new ReglaNegocioException(prefijo + "El volumen objetivo es obligatorio");
            }
            if (!Double.isFinite(lote.getVolumenObjetivo()) || Redondeo.redondear(lote.getVolumenObjetivo()) <= 0) {
                throw new ReglaNegocioException(prefijo + "El volumen objetivo debe ser mayor a 0");
            }
            validarInformado(lote.getIdFermentador(), prefijo + "El fermentador es obligatorio");
            validarInformado(lote.getIdMolino(), prefijo + "El molino es obligatorio");
            validarInformado(lote.getIdMacerador(), prefijo + "El macerador es obligatorio");
            validarInformado(lote.getIdOllaHervor(), prefijo + "La olla de hervor es obligatoria");
        }
    }

    /**
     * Arma los avisos de capacidad de un lote modificado: los mismos límites que el registro valida para el
     * fermentador, la olla de hervor y el macerador.
     */
    private static List<String> avisosDeCapacidad(String prefijo, double volumen, double masaMalta, EquiposDeLote equipos,
                                                  VersionRecetaEntity versionReceta) {
        List<String> avisos = new ArrayList<>();
        FermentadorEntity fermentador = equipos.fermentador();
        OllaHervorEntity ollaHervor = equipos.ollaHervor();
        MaceradorEntity macerador = equipos.macerador();
        if (EstimacionLote.superaCapacidad(volumen, fermentador.getCapacidadUtil())) {
            avisos.add(prefijo + "El volumen objetivo (" + FormatoCantidad.formatearVolumen(volumen)
                    + " L) supera la capacidad útil del fermentador " + fermentador.getIdentificadorInterno()
                    + " (" + FormatoCantidad.formatearVolumen(fermentador.getCapacidadUtil()) + " L)");
        }
        if (!EstimacionLote.ollaAdmiteVolumen(volumen, ollaHervor, versionReceta)) {
            avisos.add(prefijo + "El volumen pre-hervor calculado ("
                    + FormatoCantidad.formatearVolumen(EstimacionLote.calcularVolumenPreHervor(volumen, ollaHervor, versionReceta))
                    + " L) supera la capacidad útil de la olla de hervor " + ollaHervor.getIdentificadorInterno()
                    + " (" + FormatoCantidad.formatearVolumen(ollaHervor.getCapacidadUtil()) + " L)");
        }
        if (!EstimacionLote.maceradorAdmiteMasaMalta(masaMalta, macerador, versionReceta)) {
            avisos.add(prefijo + "La mezcla de agua y grano ("
                    + FormatoCantidad.formatearVolumen(EstimacionLote.calcularVolumenMezclaMacerador(masaMalta, macerador, versionReceta))
                    + " L) desbordará el macerador " + macerador.getIdentificadorInterno()
                    + " (capacidad útil: " + FormatoCantidad.formatearVolumen(macerador.getCapacidadUtil()) + " L)");
        }
        return avisos;
    }

    private static void validarInformado(Object valor, String mensaje) {
        if (valor == null) {
            throw new ReglaNegocioException(mensaje);
        }
    }

    // ==================== Apoyo ====================

    private PlanificacionProduccionEntity buscarPlanificacion(Long idPlanificacion) {
        return planificacionProduccionRepository.findById(idPlanificacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacion));
    }

    private ConfiguracionProduccionEntity buscarConfiguracion() {
        return configuracionProduccionRepository.findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
    }

    /**
     * Litros que le faltan planificar: la cantidad a producir menos los litros producidos y el volumen objetivo de los
     * lotes vivos de la planificación (puede ser 0 o negativo si ya están cubiertos).
     */
    private double calcularLitrosAPlanificar(PlanificacionProduccionEntity planificacion) {
        return Redondeo.redondear(BigDecimal.valueOf(planificacion.getCantidadAProducir())
                .subtract(BigDecimal.valueOf(litrosComprometidos(planificacion)))
                .doubleValue());
    }

    private double litrosComprometidos(PlanificacionProduccionEntity planificacion) {
        return BigDecimal.valueOf(envasadoLoteRepository.sumarLitrosProducidosDePlanificacion(planificacion.getId()))
                .add(BigDecimal.valueOf(loteRepository.sumarVolumenObjetivoDeLotesVivosDePlanificacion(planificacion.getId())))
                .doubleValue();
    }

    private String mensajePlanificacionCubierta(PlanificacionProduccionEntity planificacion) {
        return "La planificación ya tiene lotes que cubren la cantidad a producir ("
                + ValidacionesNumericas.formatearNumero(litrosComprometidos(planificacion)) + " L comprometidos de "
                + ValidacionesNumericas.formatearNumero(planificacion.getCantidadAProducir()) + " L solicitados)";
    }

    /**
     * Fin del lote detrás del cual el registro ubicaría un lote de la planificación en cada fermentador, con la misma
     * regla de prioridad de {@link ILoteServicio#registrarLote} (ver {@link ILoteServicio#calcularFinDelLoteAnteriorEnCola}).
     * Un fermentador en el que el lote arrancaría libre no figura.
     */
    private Map<Long, LocalDate> buscarFinDelLoteAnteriorEnCola(PlanificacionProduccionEntity planificacion, List<Long> idsFermentadores) {
        if (idsFermentadores.isEmpty()) {
            return Map.of();
        }
        return loteServicio.calcularFinDelLoteAnteriorEnCola(planificacion.getId(), idsFermentadores);
    }

    private double calcularMasaMalta(double volumen, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return escaladoInsumoServicio.calcularRequerimientosMalta(versionReceta, volumen, macerador).stream()
                .mapToDouble(RequerimientoInsumo::cantidadRequerida)
                .sum();
    }

    private static String prefijoDeLote(int indice) {
        return "Lote " + (indice + 1) + " del plan: ";
    }
}
