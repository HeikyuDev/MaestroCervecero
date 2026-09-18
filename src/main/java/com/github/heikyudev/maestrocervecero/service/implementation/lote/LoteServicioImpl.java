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
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
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
    private static final int HORAS_POR_DIA = 24;

    // Estados de lote que todavía ocupan un lugar en el cronograma de un fermentador
    private static final List<EstadoLote> ESTADOS_QUE_OCUPAN_FERMENTADOR = List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION);

    private final IEscaladoInsumoServicio escaladoInsumoServicio;
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
        return loteRepository.filtrarLotes(idReceta, identificadorInterno, estado, etapaActual, volumenObjetivo, pageable)
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

        // 4. Calcular la cantidad requerida de cada insumo, escalada al volumen objetivo del lote,
        //    y asociarla a la etapa donde efectivamente se va a usar: Maceración para la malta,
        //    Fermentación para la levadura, y la etapa configurada en cada detalle para el lúpulo
        //    (HERVIDO para HERVOR/WHIRLPOOL, FERMENTACION o MADURACION para DRY_HOP).
        List<RequerimientoInsumo> requerimientos = escaladoInsumoServicio.calcularRequerimientosTotales(lote);

        // 5. Validar que el stock disponible de cada insumo alcance la cantidad escalada (sumada
        //    entre todas las etapas que lo requieran), antes de reservar nada
        Map<Long, List<LoteInsumoEntity>> stockPorInsumo = obtenerYValidarStockDisponible(requerimientos);

        // 6. Reservar la cantidad escalada de cada insumo aplicando la regla FEFO
        List<ReservaInsumoEntity> reservas = reservarInsumosFEFO(requerimientos, stockPorInsumo);
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
        EtapaLoteEntity etapaMaceracion = lote.obtenerEtapaPorTipo(TipoEtapa.MACERACION);
        avanzarEtapa(etapaMolienda, etapaMaceracion);

        // 5. El molino utilizado pasa a estado EN_LIMPIEZA
        Long idMolino = etapaMolienda.getEquipamiento().getId();
        MolinoEntity molino = molinoRepository.buscarPorIdParaCambiarEstadoOperativo(idMolino)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolino));
        molino.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        molinoRepository.save(molino);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Maceración del lote y da paso al Hervido.
     *
     * @param id El ID del lote cuya Maceración se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Maceración, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarMaceracion(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Maceración
        EtapaLoteEntity etapaMaceracion = obtenerEtapaEnCursoValidando(lote, TipoEtapa.MACERACION);

        // 4. Validar que el consumo de cada insumo requerido de la etapa alcance el porcentaje
        //    mínimo configurado
        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de producción"));
        validarPorcentajeMinimoConsumido(etapaMaceracion, configuracion.getPorcentajeMinimoConsumoParaAvanzarEtapa());

        // 5. Finalizar Maceración e iniciar Hervido
        EtapaLoteEntity etapaHervido = lote.obtenerEtapaPorTipo(TipoEtapa.HERVIDO);
        avanzarEtapa(etapaMaceracion, etapaHervido);

        // 6. El macerador utilizado pasa a estado EN_LIMPIEZA
        Long idMacerador = etapaMaceracion.getEquipamiento().getId();
        MaceradorEntity macerador = maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(idMacerador)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMacerador));
        macerador.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        maceradorRepository.save(macerador);

        // 7. Liberar las reservas de insumo que hayan quedado sin consumir de la etapa de Maceración
        liberarReservasDeLaEtapa(etapaMaceracion);

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Finaliza la etapa de Hervido del lote y da paso a la Fermentación.
     *
     * @param id El ID del lote cuyo Hervido se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Hervido, o si algún insumo requerido
     *                               de la etapa no alcanzó el porcentaje mínimo de consumo
     *                               configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarHervido(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder finalizar una etapa");
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
        OllaHervorEntity ollaHervor = ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(idOllaHervor)
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
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Fermentación, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarFermentacion(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder finalizar una etapa");
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
     * porcentaje mínimo de consumo también aplica acá: es infrecuente que Maduración requiera
     * insumos, pero puede haberlos (por ejemplo, un lúpulo de Dry Hop).
     * </p>
     *
     * @param id El ID del lote cuya Maduración se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Maduración, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarMaduracion(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder finalizar una etapa");
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
     *
     * @param id El ID del lote cuyo Envasado se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra el fermentador asociado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, o si su
     *                               etapa actual (EN_CURSO) no es Envasado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.LOTE)
    public LoteResponseDTO finalizarEnvasado(Long id) {
        // 1. Validar que el lote esté registrado en el sistema
        LoteEntity lote = loteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + id));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder finalizar una etapa");
        }

        // 3. Validar que la etapa actual (EN_CURSO) sea Envasado
        EtapaLoteEntity etapaEnvasado = obtenerEtapaEnCursoValidando(lote, TipoEtapa.ENVASADO);

        // 4. El fermentador utilizado pasa a estado EN_LIMPIEZA (concluyen las tres etapas que lo comparten)
        Long idFermentador = etapaEnvasado.getEquipamiento().getId();
        FermentadorEntity fermentador = fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentador)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentador));
        fermentador.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        fermentadorRepository.save(fermentador);

        // 5. Finalizar la etapa de Envasado: es la última, no hay una siguiente a la que avanzar
        etapaEnvasado.setEstado(EstadoEtapaLote.FINALIZADA);
        etapaEnvasado.setFechaFinalizacion(LocalDateTime.now());

        // 6. Finalizar el lote: todo el proceso productivo quedó completo
        lote.setEstado(EstadoLote.FINALIZADO);
        lote.setFechaFinalizacion(LocalDateTime.now());

        return MapperLote.toDTO(loteRepository.save(lote));
    }

    /**
     * Valida que la cantidad consumida de CADA insumo requerido por una etapa alcance el
     * porcentaje mínimo configurado sobre su cantidad requerida (escalada), para evitar que se
     * avance de etapa por error sin haber registrado consumos, o habiéndolos registrado de forma
     * incompleta.
     * <p>
     * Reutiliza {@link IConsumoInsumoServicio#filtrarInsumosRequeridos(Long)} para obtener, por
     * insumo, cuánto requiere la etapa y cuánto ya se consumió — la misma fuente que usa la
     * pantalla de gestión de consumos, evitando recalcular o duplicar esa lógica acá.
     * </p>
     *
     * @throws ReglaNegocioException Si algún insumo requerido no alcanza el porcentaje mínimo.
     */
    private void validarPorcentajeMinimoConsumido(EtapaLoteEntity etapa, double porcentajeMinimo) {
        List<String> insumosInsuficientes = consumoInsumoServicio.filtrarInsumosRequeridos(etapa.getId()).stream()
                .filter(insumoRequerido -> insumoRequerido.getCantidadConsumida() < insumoRequerido.getCantidadRequerida() * (porcentajeMinimo / 100.0))
                .map(insumoRequerido -> insumoRequerido.getInsumo().getNombre())
                .toList();

        if (!insumosInsuficientes.isEmpty()) {
            throw new ReglaNegocioException("No se puede finalizar la etapa: los siguientes insumos no alcanzaron el "
                    + porcentajeMinimo + "% mínimo de consumo requerido: " + String.join(", ", insumosInsuficientes));
        }
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
     * sobre su {@link LoteInsumoEntity} (bloqueándolo para escritura, para evitar que otra
     * operación concurrente lo modifique al mismo tiempo) y elimina la reserva — una reserva
     * liberada deja de existir, no se conserva con algún indicador de "liberada".
     */
    private void liberarReservas(List<ReservaInsumoEntity> reservas) {
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
            MolinoEntity molino = molinoRepository.buscarPorIdParaCambiarEstadoOperativo(idMolinoFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolinoFinal));
            molino.setEstadoOperativo(nuevoEstadoMolino);
            molinoRepository.save(molino);
        }

        EstadoOperativo nuevoEstadoMacerador = resolverEstadoOperativoAlCancelar(estadoMacerador);
        if (nuevoEstadoMacerador != null) {
            Long idMaceradorFinal = idMacerador;
            MaceradorEntity macerador = maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(idMaceradorFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMaceradorFinal));
            macerador.setEstadoOperativo(nuevoEstadoMacerador);
            maceradorRepository.save(macerador);
        }

        EstadoOperativo nuevoEstadoOllaHervor = resolverEstadoOperativoAlCancelar(estadoOllaHervor);
        if (nuevoEstadoOllaHervor != null) {
            Long idOllaHervorFinal = idOllaHervor;
            OllaHervorEntity ollaHervor = ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(idOllaHervorFinal)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervorFinal));
            ollaHervor.setEstadoOperativo(nuevoEstadoOllaHervor);
            ollaHervorRepository.save(ollaHervor);
        }

        EstadoOperativo nuevoEstadoFermentador = resolverEstadoOperativoAlCancelar(estadoFermentador);
        if (nuevoEstadoFermentador != null) {
            Long idFermentadorFinal = idFermentador;
            FermentadorEntity fermentador = fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentadorFinal)
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
        return escaladoInsumoServicio.calcularRequerimientosMalta(versionReceta, volumenObjetivo, macerador).stream()
                .mapToDouble(RequerimientoInsumo::cantidadRequerida)
                .sum();
    }

    /**
     * Para cada insumo requerido, busca (bloqueando para escritura) sus lotes de insumo
     * ordenados por FEFO y valida que la suma de cantidad disponible entre todos ellos alcance la
     * cantidad requerida SUMADA ENTRE TODAS LAS ETAPAS que lo necesiten (el mismo insumo puede
     * tener requerimientos separados por etapa, pero el stock físico que lo cubre es uno solo,
     * compartido). No reserva nada todavía.
     * <p>
     * Evalúa TODOS los insumos antes de lanzar, en vez de cortar en el primero que falla: así el
     * usuario ve de una sola vez la lista completa de insumos con stock insuficiente, sin tener que
     * corregir uno, reintentar, y recién ahí enterarse del siguiente.
     * </p>
     */
    private Map<Long, List<LoteInsumoEntity>> obtenerYValidarStockDisponible(Collection<RequerimientoInsumo> requerimientos) {
        Map<Long, InsumoEntity> insumoPorId = new LinkedHashMap<>();
        Map<Long, Double> cantidadRequeridaPorInsumo = new LinkedHashMap<>();
        for (RequerimientoInsumo requerimiento : requerimientos) {
            insumoPorId.putIfAbsent(requerimiento.insumo().getId(), requerimiento.insumo());
            cantidadRequeridaPorInsumo.merge(requerimiento.insumo().getId(), requerimiento.cantidadRequerida(), Double::sum);
        }

        Map<Long, List<LoteInsumoEntity>> stockPorInsumo = new LinkedHashMap<>();
        List<String> faltantes = new ArrayList<>();

        for (Map.Entry<Long, Double> entry : cantidadRequeridaPorInsumo.entrySet()) {
            Long idInsumo = entry.getKey();
            double cantidadRequeridaTotal = entry.getValue();
            List<LoteInsumoEntity> lotesDisponibles = loteInsumoRepository
                    .buscarPorInsumoIdOrdenadoPorVencimientoParaReservar(idInsumo);

            double totalDisponible = lotesDisponibles.stream().mapToDouble(LoteInsumoEntity::getCantidadDisponible).sum();
            if (totalDisponible < cantidadRequeridaTotal) {
                faltantes.add("'" + insumoPorId.get(idInsumo).getNombre() + "' (se requieren " + cantidadRequeridaTotal
                        + " y solo hay " + totalDisponible + " disponibles)");
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
     * que contribuye, asociada a la etapa de ESE requerimiento puntual.
     */
    private List<ReservaInsumoEntity> reservarInsumosFEFO(Collection<RequerimientoInsumo> requerimientos, Map<Long, List<LoteInsumoEntity>> stockPorInsumo) {
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
                        .etapaLote(requerimiento.etapa())
                        .loteInsumo(loteInsumo)
                        .cantidadReservada(cantidadAReservar)
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

        MolinoEntity molino = molinoRepository.buscarPorIdParaCambiarEstadoOperativo(idMolinoFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + idMolinoFinal));
        MaceradorEntity macerador = maceradorRepository.buscarPorIdParaCambiarEstadoOperativo(idMaceradorFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + idMaceradorFinal));
        OllaHervorEntity ollaHervor = ollaHervorRepository.buscarPorIdParaCambiarEstadoOperativo(idOllaHervorFinal)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + idOllaHervorFinal));
        FermentadorEntity fermentador = fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentadorFinal)
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

        ConfiguracionProduccionEntity configuracion = configuracionProduccionRepository
                .findById(ConfiguracionProduccionEntity.SINGLETON_ID)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la configuración de planificación de producción"));
        double horasEnvasado = volumenObjetivo / configuracion.getVelocidadEstandarEnvasado();

        long diasBrewDayYEnvasado = (long) Math.ceil((horasMolienda + horasMaceracionYHervido + horasEnvasado) / HORAS_POR_DIA);
        long diasTotales = diasBrewDayYEnvasado + versionReceta.getDuracionFermentacion() + versionReceta.getDuracionMaduracion();

        return fechaInicioEstimada.plusDays(diasTotales);
    }
}
