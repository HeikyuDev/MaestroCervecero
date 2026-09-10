package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionPlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperLote;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementación de {@link ILoteServicio}.
 * <p>
 * El cálculo de los volúmenes de escalado de agua (Paso 2 y Paso 3 del documento
 * de dominio "Escalado del Agua") se resuelve acá mismo, ya que hoy solo se
 * utiliza para validar la capacidad de los equipos al registrar el lote. Si en el
 * futuro otra operación (por ejemplo, "Iniciar Lote", que necesita también el
 * agua de lavado y el agua total a preparar) requiere este mismo cálculo, vale la
 * pena extraerlo a una clase de dominio compartida.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LoteServicioImpl implements ILoteServicio {

    // Constantes fijas del sistema (ver docs/Dominio/Escalado/EscaladoDelAgua.md y EscaladoDeMalta.md)
    private static final double DESPLAZAMIENTO_GRANO_L_POR_KG = 0.7;
    private static final double REFERENCIA_AZUCAR_PURA_PUNTOS_POR_KG_POR_L = 384.0;
    private static final int HORAS_POR_DIA = 24;

    // Constantes fijas de la fórmula de Tinseth (ver docs/Dominio/Escalado/EscaladoDelLupulo.md)
    private static final double TINSETH_CONSTANTE_FACTOR_DENSIDAD = 1.65;
    private static final double TINSETH_BASE_FACTOR_DENSIDAD = 0.000125;
    private static final double TINSETH_CONSTANTE_DECAIMIENTO_TIEMPO = 0.04;
    private static final double TINSETH_DIVISOR_FACTOR_TIEMPO = 4.15;

    // Divisor de conversión de puntos de gravedad a grados Plato (ver EscaladoDeLevadura.md)
    private static final double DIVISOR_GRADOS_PLATO = 4.0;

    // Estados de lote que todavía ocupan un lugar en el cronograma de un fermentador
    private static final List<EstadoLote> ESTADOS_QUE_OCUPAN_FERMENTADOR = List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION);

    private final ILoteRepository loteRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IMolinoRepository molinoRepository;
    private final IMaceradorRepository maceradorRepository;
    private final IOllaHervorRepository ollaHervorRepository;
    private final IFermentadorRepository fermentadorRepository;
    private final IRecetaRepository recetaRepository;
    private final IConfiguracionPlanificacionProduccionRepository configuracionPlanificacionProduccionRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IReservaInsumoRepository reservaInsumoRepository;

    /**
     * Recupera una página de lotes registrados en el sistema.
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LoteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LoteResponseDTO> buscarTodos(Pageable pageable) {
        return loteRepository.findAll(pageable).map(MapperLote::toDTO);
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
     * Registra un nuevo lote, reservando su lugar en el cronograma de uso de equipamiento.
     *
     * @param loteFormDTO Los datos del lote a registrar.
     * @return El lote registrado.
     * @throws RecursoNoEncontradoException Si la planificación de producción o alguno de los equipamientos referenciados no existe.
     * @throws ReglaNegocioException Si la planificación de producción no está en estado PENDIENTE, si el volumen objetivo es
     *                               inválido, o si algún equipo seleccionado no tiene capacidad suficiente.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO registrarLote(LoteFormDTO loteFormDTO) {
        // 1. Validar que la planificación de producción esté registrada y en estado PENDIENTE
        PlanificacionProduccionEntity planificacionProduccion = planificacionProduccionRepository.findById(loteFormDTO.getIdPlanificacionProduccion())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + loteFormDTO.getIdPlanificacionProduccion()));

        if (planificacionProduccion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("La planificación de producción debe encontrarse en estado PENDIENTE para poder registrar un lote");
        }

        // 2. Validar que los equipamientos seleccionados estén registrados en el sistema
        MolinoEntity molino = molinoRepository.findById(loteFormDTO.getIdMolino())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + loteFormDTO.getIdMolino()));

        MaceradorEntity macerador = maceradorRepository.findById(loteFormDTO.getIdMacerador())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + loteFormDTO.getIdMacerador()));

        OllaHervorEntity ollaHervor = ollaHervorRepository.findById(loteFormDTO.getIdOllaHervor())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + loteFormDTO.getIdOllaHervor()));

        FermentadorEntity fermentador = fermentadorRepository.findById(loteFormDTO.getIdFermentador())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + loteFormDTO.getIdFermentador()));

        // 3. Validar el volumen objetivo del lote (Paso 1 del escalado de agua: verificación del fermentador)
        Double volumenObjetivo = loteFormDTO.getVolumenObjetivo();
        validarVolumenObjetivo(volumenObjetivo, fermentador);

        VersionRecetaEntity versionReceta = planificacionProduccion.getVersionReceta();
        double masaMaltaEscalada = calcularMasaMaltaEscalada(volumenObjetivo, macerador, versionReceta);

        // 4. Validar el volumen pre-hervor contra la capacidad de la olla de hervor (Paso 2 del escalado de agua)
        double volumenPreHervor = calcularVolumenPreHervor(volumenObjetivo, ollaHervor, versionReceta);
        if (volumenPreHervor > ollaHervor.getCapacidadUtil()) {
            throw new ReglaNegocioException("El volumen pre-hervor calculado (" + volumenPreHervor + " L) supera la capacidad útil de la olla de hervor seleccionada (" + ollaHervor.getCapacidadUtil() + " L)");
        }

        // 5. Validar el volumen de la mezcla de agua y malta contra la capacidad del macerador (Paso 3 del escalado de agua)
        double volumenMezclaMacerador = calcularVolumenMezclaMacerador(masaMaltaEscalada, macerador, versionReceta);
        if (volumenMezclaMacerador > macerador.getCapacidadUtil()) {
            throw new ReglaNegocioException("La mezcla de agua y grano (" + volumenMezclaMacerador + " L) desbordará el macerador seleccionado (capacidad útil: " + macerador.getCapacidadUtil() + " L)");
        }

        // 6. Generar el identificador interno del lote, incrementando de forma segura el contador de la receta
        String identificadorInterno = generarIdentificadorInterno(versionReceta);

        // 7. Calcular la planificación del lote en el cronograma del fermentador seleccionado
        LocalDate fechaInicioEstimada = calcularFechaInicioEstimada(fermentador);
        LocalDate fechaFinalizacionEstimada = calcularFechaFinalizacionEstimada(fechaInicioEstimada, volumenObjetivo, molino, masaMaltaEscalada, versionReceta);

        // 8. Construir el lote y sus 6 etapas (todas en PENDIENTE), y persistir
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
     *
     * @param id El ID del lote a iniciar.
     * @return El lote iniciado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado PENDIENTE, si no hay
     *                               stock suficiente de algún insumo, o si algún equipamiento
     *                               requerido no está disponible.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO iniciarLote(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado PENDIENTE
        if (lote.getEstado() != EstadoLote.PENDIENTE) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado PENDIENTE para poder iniciarse");
        }

        // 3. Bloquear y validar que el equipamiento asociado al lote se encuentre DISPONIBLE
        EquipamientoDelLote equipamiento = bloquearYValidarEquipamientoDisponible(lote);

        // 4. Calcular la cantidad requerida de cada insumo, escalada al volumen objetivo del lote
        VersionRecetaEntity versionReceta = lote.getPlanificacionProduccion().getVersionReceta();
        double volumenObjetivo = lote.getVolumenObjetivo();

        List<RequerimientoInsumo> todosLosRequerimientos = new ArrayList<>();
        todosLosRequerimientos.addAll(calcularRequerimientosMalta(versionReceta, volumenObjetivo, equipamiento.macerador()));
        todosLosRequerimientos.addAll(calcularRequerimientosLupulo(versionReceta, volumenObjetivo));
        todosLosRequerimientos.addAll(calcularRequerimientosLevadura(versionReceta, volumenObjetivo));

        // Este paso es necesario ya unifica los requerimientos de insumo POR INSUMO, evitando
        // tener requerimeintos para un mismo insumo.
        Map<Long, RequerimientoInsumo> requerimientosPorInsumo = new LinkedHashMap<>();
        for (RequerimientoInsumo requerimiento : todosLosRequerimientos) {
            requerimientosPorInsumo.merge(requerimiento.insumo().getId(), requerimiento,
                    (existente, nuevo) -> new RequerimientoInsumo(existente.insumo(), existente.cantidadRequerida() + nuevo.cantidadRequerida()));
        }

        // 5. Validar que el stock disponible de cada insumo alcance la cantidad escalada, antes de
        //    reservar nada
        Map<Long, List<LoteInsumoEntity>> stockPorInsumo = obtenerYValidarStockDisponible(requerimientosPorInsumo.values());

        // 6. Reservar la cantidad escalada de cada insumo aplicando la regla FEFO
        List<ReservaInsumoEntity> reservas = reservarInsumosFEFO(lote, requerimientosPorInsumo.values(), stockPorInsumo);
        reservaInsumoRepository.saveAll(reservas);

        // 7 y 8. Registrar la fecha y hora de inicio general del lote, y cambiar su estado a EN_EJECUCION
        lote.setFechaInicio(LocalDateTime.now());
        lote.setEstado(EstadoLote.EN_EJECUCION);

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

        // 11. Persistir y retornar el lote actualizado
        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Cancela un lote en curso.
     *
     * @param id El ID del lote a cancelar.
     * @param cancelacionLoteFormDTO Los datos de la cancelación (motivo).
     * @return El lote cancelado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el motivo de cancelación no fue informado, o si el lote no
     *                               se encuentra en estado PENDIENTE o EN_EJECUCION.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO cancelarLote(Long id, CancelacionLoteFormDTO cancelacionLoteFormDTO) {
        // 1. Validar que se haya informado el motivo de cancelación
        if (cancelacionLoteFormDTO.getMotivoCancelacion() == null || cancelacionLoteFormDTO.getMotivoCancelacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de cancelación es obligatorio");
        }

        // 2. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 3. Validar que el lote se encuentre en un estado que permita cancelarlo
        if (lote.getEstado() != EstadoLote.PENDIENTE && lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado PENDIENTE o EN_EJECUCION para poder cancelarse");
        }

        // 4. Liberar todas las reservas de insumo del lote todavía pendientes. Cancelar NO revierte
        //    los consumos ya ejecutados: hoy no existe ninguna forma de consumir un insumo sin pasar
        //    antes por una reserva, así que toda reserva de un lote PENDIENTE/EN_EJECUCION representa
        //    stock que todavía no se consumió.
        liberarReservasDelLote(lote);

        // 5. Actualizar el estado del equipamiento según el estado de cada etapa del lote: el
        //    equipamiento de la etapa EN_CURSO pasa a EN_LIMPIEZA, el de las etapas PENDIENTE pasa a
        //    DISPONIBLE, y el de las etapas ya FINALIZADA mantiene su estado actual.
        actualizarEquipamientoAlCancelar(lote);

        // 6. Cancelar el lote
        lote.setEstado(EstadoLote.CANCELADO);
        lote.setFechaCancelacion(LocalDateTime.now());
        lote.setMotivoCancelacion(cancelacionLoteFormDTO.getMotivoCancelacion());

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Molienda del lote y da paso a la Maceración.
     *
     * @param id El ID del lote cuya Molienda se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, o si su
     *                               etapa actual (EN_CURSO) no es Molienda.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarMolienda(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Molienda
        EtapaLoteEntity etapaMolienda = obtenerEtapaEnCursoValidando(lote, TipoEtapa.MOLIENDA);

        // 4. Finalizar Molienda e iniciar Maceración
        EtapaLoteEntity etapaMaceracion = obtenerEtapaPorTipo(lote, TipoEtapa.MACERACION);
        avanzarEtapa(etapaMolienda, etapaMaceracion);

        // 5. El molino utilizado pasa a estado EN_LIMPIEZA
        Long idMolino = etapaMolienda.getEquipamiento().getId();
        MolinoEntity molino = molinoRepository.buscarPorIdParaIniciarLote(idMolino)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolino));
        molino.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        molinoRepository.save(molino);

        return MapperLote.toDTO(loteRepository.save(lote));
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
            throw new ReglaNegocioException("La etapa actual del lote es " + etapaActual.getEtapa() + ", no " + tipoEsperado);
        }
        return etapaActual;
    }

    /**
     * Busca, entre las 6 etapas del lote, la del tipo indicado.
     */
    private EtapaLoteEntity obtenerEtapaPorTipo(LoteEntity lote, TipoEtapa tipo) {
        return lote.getEtapas().stream()
                .filter(etapa -> etapa.getEtapa() == tipo)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El lote no tiene una etapa de " + tipo));
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
     * Libera todas las reservas de insumo de un lote: por cada {@link ReservaInsumoEntity}, revierte
     * la cantidad reservada sobre su {@link LoteInsumoEntity} (bloqueándolo para escritura, para
     * evitar que otra operación concurrente lo modifique al mismo tiempo) y elimina la reserva —
     * una reserva liberada deja de existir, no se conserva con algún indicador de "liberada".
     */
    private void liberarReservasDelLote(LoteEntity lote) {
        List<ReservaInsumoEntity> reservas = reservaInsumoRepository.findByLoteId(lote.getId());

        for (ReservaInsumoEntity reserva : reservas) {
            LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaLiberarReserva(reserva.getLoteInsumo().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + reserva.getLoteInsumo().getId()));
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
            MolinoEntity molino = molinoRepository.buscarPorIdParaIniciarLote(idMolinoFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolinoFinal));
            molino.setEstadoOperativo(nuevoEstadoMolino);
            molinoRepository.save(molino);
        }

        EstadoOperativo nuevoEstadoMacerador = resolverEstadoOperativoAlCancelar(estadoMacerador);
        if (nuevoEstadoMacerador != null) {
            Long idMaceradorFinal = idMacerador;
            MaceradorEntity macerador = maceradorRepository.buscarPorIdParaIniciarLote(idMaceradorFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMaceradorFinal));
            macerador.setEstadoOperativo(nuevoEstadoMacerador);
            maceradorRepository.save(macerador);
        }

        EstadoOperativo nuevoEstadoOllaHervor = resolverEstadoOperativoAlCancelar(estadoOllaHervor);
        if (nuevoEstadoOllaHervor != null) {
            Long idOllaHervorFinal = idOllaHervor;
            OllaHervorEntity ollaHervor = ollaHervorRepository.buscarPorIdParaIniciarLote(idOllaHervorFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervorFinal));
            ollaHervor.setEstadoOperativo(nuevoEstadoOllaHervor);
            ollaHervorRepository.save(ollaHervor);
        }

        EstadoOperativo nuevoEstadoFermentador = resolverEstadoOperativoAlCancelar(estadoFermentador);
        if (nuevoEstadoFermentador != null) {
            Long idFermentadorFinal = idFermentador;
            FermentadorEntity fermentador = fermentadorRepository.buscarPorIdParaIniciarLote(idFermentadorFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentadorFinal));
            fermentador.setEstadoOperativo(nuevoEstadoFermentador);
            fermentadorRepository.save(fermentador);
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
        };
    }

    /**
     * Valida el volumen objetivo del lote (Paso 1 del escalado de agua): debe ser mayor a cero y no
     * superar la capacidad útil del fermentador seleccionado.
     */
    private void validarVolumenObjetivo(Double volumenObjetivo, FermentadorEntity fermentador) {
        if (volumenObjetivo == null || volumenObjetivo <= 0) {
            throw new ReglaNegocioException("El volumen objetivo del lote debe ser mayor a cero");
        }
        if (volumenObjetivo > fermentador.getCapacidadUtil()) {
            throw new ReglaNegocioException("El volumen objetivo del lote (" + volumenObjetivo + " L) supera la capacidad útil del fermentador seleccionado (" + fermentador.getCapacidadUtil() + " L)");
        }
    }

    /**
     * Calcula el volumen que debe reunirse en la olla antes de empezar a hervir (V_pre-hervor),
     * sumando al volumen objetivo la pérdida por trub y la evaporación estimada durante el hervor.
     */
    private double calcularVolumenPreHervor(double volumenObjetivo, OllaHervorEntity ollaHervor, VersionRecetaEntity versionReceta) {
        double horasHervor = versionReceta.getDuracionHervido() / 60.0;
        return volumenObjetivo + ollaHervor.getPerdidaPorTrub() + (ollaHervor.getEvaporacion() * horasHervor);
    }

    /**
     * Calcula el volumen que va a ocupar la mezcla de agua y malta dentro del macerador (V_mezcla_mac),
     * a partir de la masa de malta escalada (ver docs/Dominio/Escalado/EscaladoDeMalta.md), la relación
     * de empaste de la receta, el espacio muerto del macerador y el desplazamiento físico del grano.
     */
    private double calcularVolumenMezclaMacerador(double masaMaltaEscalada, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        double volumenAguaMaceracion = (masaMaltaEscalada * versionReceta.getRelacionDeEmpaste()) + macerador.getEspacioMuerto();
        return volumenAguaMaceracion + (masaMaltaEscalada * DESPLAZAMIENTO_GRANO_L_POR_KG);
    }

    /**
     * Calcula la masa total de malta necesaria para alcanzar el OG objetivo de la receta, dado el
     * volumen objetivo del lote y el rendimiento de maceración del macerador seleccionado.
     */
    private double calcularMasaMaltaEscalada(double volumenObjetivo, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return calcularRequerimientosMalta(versionReceta, volumenObjetivo, macerador).stream()
                .mapToDouble(RequerimientoInsumo::cantidadRequerida)
                .sum();
    }

    /**
     * Calcula, para cada malta de la receta, la masa escalada necesaria para alcanzar el OG
     * objetivo (ver docs/Dominio/Escalado/EscaladoDeMalta.md, Pasos 1 a 5): primero el total de
     * kilos de malta necesarios para el lote completo, y luego el reparto entre las distintas
     * maltas manteniendo la misma proporción definida en la receta.
     */
    private List<RequerimientoInsumo> calcularRequerimientosMalta(VersionRecetaEntity versionReceta, double volumenObjetivo, MaceradorEntity macerador) {
        List<DetalleMaltaEntity> detallesMalta = versionReceta.getDetallesMalta();
        double cantidadTotalBase = detallesMalta.stream().mapToDouble(DetalleMaltaEntity::getCantidad).sum();

        double extractoPotencialPromedio = detallesMalta.stream()
                .mapToDouble(detalle -> (detalle.getCantidad() / cantidadTotalBase) * (detalle.getMalta().getPotencialExtracto() / 100.0))
                .sum();

        double puntosDensidadObjetivo = (versionReceta.getOgObjetivo() - 1) * 1000 * volumenObjetivo;
        double kgMaltaTotal = puntosDensidadObjetivo / (extractoPotencialPromedio * (macerador.getEficienciaMaceracion() / 100.0) * REFERENCIA_AZUCAR_PURA_PUNTOS_POR_KG_POR_L);

        return detallesMalta.stream()
                .map(detalle -> new RequerimientoInsumo(detalle.getMalta(), kgMaltaTotal * (detalle.getCantidad() / cantidadTotalBase)))
                .toList();
    }

    /**
     * Calcula, para cada lúpulo de la receta, la masa escalada necesaria
     * (ver docs/Dominio/Escalado/EscaladoDelLupulo.md): los lúpulos de uso HERVOR se calculan con
     * la fórmula de Tinseth para alcanzar el IBU objetivo (Pasos 1 a 6); los de uso WHIRLPOOL o
     * DRY_HOP, al ser exclusivamente aromáticos, escalan de forma lineal respecto al volumen base
     * de la receta (Paso 7).
     */
    private List<RequerimientoInsumo> calcularRequerimientosLupulo(VersionRecetaEntity versionReceta, double volumenObjetivo) {
        List<DetalleLupuloEntity> detallesLupulo = versionReceta.getDetallesLupulo();
        List<RequerimientoInsumo> requerimientos = new ArrayList<>();

        List<DetalleLupuloEntity> detallesHervor = detallesLupulo.stream()
                .filter(detalle -> detalle.getUso() == UsoLupulo.HERVOR)
                .toList();

        if (!detallesHervor.isEmpty()) {
            double cantidadTotalBaseHervor = detallesHervor.stream().mapToDouble(DetalleLupuloEntity::getCantidad).sum();
            double factorDensidad = TINSETH_CONSTANTE_FACTOR_DENSIDAD * Math.pow(TINSETH_BASE_FACTOR_DENSIDAD, versionReceta.getOgObjetivo() - 1);

            double k = detallesHervor.stream()
                    .mapToDouble(detalle -> {
                        double proporcion = detalle.getCantidad() / cantidadTotalBaseHervor;
                        double factorTiempo = (1 - Math.exp(-TINSETH_CONSTANTE_DECAIMIENTO_TIEMPO * detalle.getTiempoDeHervor())) / TINSETH_DIVISOR_FACTOR_TIEMPO;
                        double utilizacion = factorDensidad * factorTiempo * detalle.getLupulo().getFormato().getFactorCorreccionUtilizacion();
                        return proporcion * (detalle.getLupulo().getAa() / 100.0) * utilizacion;
                    })
                    .sum();

            double gramosTotalHervor = (versionReceta.getIbuObjetivo() * volumenObjetivo) / (1000 * k);

            for (DetalleLupuloEntity detalle : detallesHervor) {
                double proporcion = detalle.getCantidad() / cantidadTotalBaseHervor;
                requerimientos.add(new RequerimientoInsumo(detalle.getLupulo(), gramosTotalHervor * proporcion));
            }
        }

        detallesLupulo.stream()
                .filter(detalle -> detalle.getUso() != UsoLupulo.HERVOR)
                .forEach(detalle -> requerimientos.add(new RequerimientoInsumo(detalle.getLupulo(),
                        detalle.getCantidad() * (volumenObjetivo / versionReceta.getVolumenBase()))));

        return requerimientos;
    }

    /**
     * Calcula, para cada levadura de la receta, la masa escalada necesaria
     * (ver docs/Dominio/Escalado/EscaladoDeLevadura.md): primero la cantidad total de células
     * viables necesarias (según los grados Plato del OG objetivo y la tasa de inoculación
     * promedio ponderada de la mezcla), luego el reparto entre las distintas levaduras, y
     * finalmente la conversión a gramos según la concentración celular propia de cada una.
     */
    private List<RequerimientoInsumo> calcularRequerimientosLevadura(VersionRecetaEntity versionReceta, double volumenObjetivo) {
        List<DetalleLevaduraEntity> detallesLevadura = versionReceta.getDetallesLevadura();
        double cantidadTotalBase = detallesLevadura.stream().mapToDouble(DetalleLevaduraEntity::getCantidad).sum();

        double gradosPlato = ((versionReceta.getOgObjetivo() - 1) * 1000) / DIVISOR_GRADOS_PLATO;
        double tasaPromedio = detallesLevadura.stream()
                .mapToDouble(detalle -> (detalle.getCantidad() / cantidadTotalBase) * detalle.getLevadura().getTipo().getTasaInoculacion())
                .sum();

        double volumenObjetivoMl = volumenObjetivo * 1000;
        double celulasTotalesMillones = volumenObjetivoMl * gradosPlato * tasaPromedio;

        return detallesLevadura.stream()
                .map(detalle -> {
                    double proporcion = detalle.getCantidad() / cantidadTotalBase;
                    double celulasMillones = celulasTotalesMillones * proporcion;
                    double gramos = (celulasMillones * 1_000_000) / detalle.getLevadura().getCantidadCelulasPorGramo();
                    return new RequerimientoInsumo(detalle.getLevadura(), gramos);
                })
                .toList();
    }

    /**
     * Para cada insumo requerido, busca (bloqueando para escritura) sus lotes de insumo
     * ordenados por FEFO y valida que la suma de cantidad disponible entre todos ellos alcance la
     * cantidad requerida. No reserva nada todavía.
     * <p>
     * Evalúa TODOS los insumos antes de lanzar, en vez de cortar en el primero que falla: así el
     * usuario ve de una sola vez la lista completa de insumos con stock insuficiente, sin tener que
     * corregir uno, reintentar, y recién ahí enterarse del siguiente.
     * </p>
     */
    private Map<Long, List<LoteInsumoEntity>> obtenerYValidarStockDisponible(Collection<RequerimientoInsumo> requerimientos) {
        Map<Long, List<LoteInsumoEntity>> stockPorInsumo = new LinkedHashMap<>();
        List<String> faltantes = new ArrayList<>();

        for (RequerimientoInsumo requerimiento : requerimientos) {
            List<LoteInsumoEntity> lotesDisponibles = loteInsumoRepository
                    .buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(requerimiento.insumo().getId());

            double totalDisponible = lotesDisponibles.stream().mapToDouble(LoteInsumoEntity::getCantidadDisponible).sum();
            if (totalDisponible < requerimiento.cantidadRequerida()) {
                faltantes.add("'" + requerimiento.insumo().getNombre() + "' (se requieren " + requerimiento.cantidadRequerida()
                        + " y solo hay " + totalDisponible + " disponibles)");
            }

            stockPorInsumo.put(requerimiento.insumo().getId(), lotesDisponibles);
        }

        if (!faltantes.isEmpty()) {
            throw new ReglaNegocioException("Stock disponible insuficiente de los siguientes insumos: " + String.join("; ", faltantes));
        }

        return stockPorInsumo;
    }

    /**
     * Reserva la cantidad requerida de cada insumo aplicando FEFO: recorre los lotes de insumo ya
     * ordenados por fecha de vencimiento ascendente, tomando de cada uno la cantidad disponible
     * que haga falta hasta cubrir el requerimiento, generando una {@link ReservaInsumoEntity} por
     * cada lote de insumo que contribuye.
     */
    private List<ReservaInsumoEntity> reservarInsumosFEFO(LoteEntity lote, Collection<RequerimientoInsumo> requerimientos, Map<Long, List<LoteInsumoEntity>> stockPorInsumo) {
        List<ReservaInsumoEntity> reservas = new ArrayList<>();
        for (RequerimientoInsumo requerimiento : requerimientos) {
            double cantidadRestante = requerimiento.cantidadRequerida();

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
                        .lote(lote)
                        .loteInsumo(loteInsumo)
                        .cantidadReservada(cantidadAReservar)
                        .costoUnitarioPPP(loteInsumo.getCostoUnitarioPPP())
                        .build());
                cantidadRestante -= cantidadAReservar;
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

        MolinoEntity molino = molinoRepository.buscarPorIdParaIniciarLote(idMolinoFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolinoFinal));
        MaceradorEntity macerador = maceradorRepository.buscarPorIdParaIniciarLote(idMaceradorFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMaceradorFinal));
        OllaHervorEntity ollaHervor = ollaHervorRepository.buscarPorIdParaIniciarLote(idOllaHervorFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervorFinal));
        FermentadorEntity fermentador = fermentadorRepository.buscarPorIdParaIniciarLote(idFermentadorFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentadorFinal));

        validarDisponible(molino, "El molino");
        validarDisponible(macerador, "El macerador");
        validarDisponible(ollaHervor, "La olla de hervor");
        validarDisponible(fermentador, "El fermentador");

        return new EquipamientoDelLote(molino, macerador, ollaHervor, fermentador);
    }

    private void validarDisponible(EquipamientoEntity equipamiento, String etiqueta) {
        if (equipamiento.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException(etiqueta + " '" + equipamiento.getIdentificadorInterno()
                    + "' no se encuentra disponible (estado actual: " + equipamiento.getEstadoOperativo() + ")");
        }
    }

    /**
     * Requerimiento escalado de un insumo puntual (una malta, un lúpulo o una levadura
     * específicos), previo a agregarse por insumo cuando el mismo insumo aparece más de una vez
     * entre los distintos cálculos.
     */
    private record RequerimientoInsumo(InsumoEntity insumo, double cantidadRequerida) {
    }

    /**
     * Los 4 equipamientos reales (ya bloqueados y validados como DISPONIBLE) asociados a un lote.
     */
    private record EquipamientoDelLote(MolinoEntity molino, MaceradorEntity macerador, OllaHervorEntity ollaHervor, FermentadorEntity fermentador) {
    }

    /**
     * Genera el identificador interno del lote (nombre de la receta + número de lote de esa
     * receta), incrementando de forma segura el contador de lotes de la receta mediante un bloqueo
     * pesimista, para soportar el registro concurrente de múltiples lotes de la misma receta.
     */
    private String generarIdentificadorInterno(VersionRecetaEntity versionReceta) {
        RecetaEntity receta = recetaRepository.findByIdParaActualizarContador(versionReceta.getReceta().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + versionReceta.getReceta().getId()));

        Long numeroDeLote = receta.getContadorLotes();
        receta.setContadorLotes(numeroDeLote + 1);
        recetaRepository.save(receta);

        return versionReceta.getNombre() + "-" + numeroDeLote;
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
     * PENDIENTE o EN_EJECUCION). Si el fermentador no tiene ningún lote planificado, el lote
     * arranca hoy.
     * <p>
     * El cronograma solo se lleva por fermentador: es el único equipo cuyo tiempo de uso se mide
     * en días/meses (fermentación + maduración + envasado); molino, macerador y olla de hervor se
     * usan en horas dentro del mismo día de brewing y su disponibilidad real se valida recién al
     * iniciar el lote.
     * </p>
     */
    private LocalDate calcularFechaInicioEstimada(FermentadorEntity fermentador) {
        return loteRepository.buscarUltimaFechaFinalizacionEstimadaPorFermentador(fermentador, ESTADOS_QUE_OCUPAN_FERMENTADOR)
                .map(ultimaFechaFinalizacionEstimada -> ultimaFechaFinalizacionEstimada.plusDays(1))
                .orElse(LocalDate.now());
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

        ConfiguracionPlanificacionProduccionEntity configuracion = configuracionPlanificacionProduccionRepository
                .findById(ConfiguracionPlanificacionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de planificación de producción"));
        double horasEnvasado = volumenObjetivo / configuracion.getVelocidadEstandarEnvasado();

        long diasBrewDayYEnvasado = (long) Math.ceil((horasMolienda + horasMaceracionYHervido + horasEnvasado) / HORAS_POR_DIA);
        long diasTotales = diasBrewDayYEnvasado + versionReceta.getDuracionFermentacion() + versionReceta.getDuracionMaduracion();

        return fechaInicioEstimada.plusDays(diasTotales);
    }
}
