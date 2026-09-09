package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.ConfiguracionPlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IConfiguracionPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
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
import java.util.ArrayList;
import java.util.List;

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
        if (volumenObjetivo == null || volumenObjetivo <= 0) {
            throw new ReglaNegocioException("El volumen objetivo del lote debe ser mayor a cero");
        }
        if (volumenObjetivo > fermentador.getCapacidadUtil()) {
            throw new ReglaNegocioException("El volumen objetivo del lote (" + volumenObjetivo + " L) supera la capacidad útil del fermentador seleccionado (" + fermentador.getCapacidadUtil() + " L)");
        }

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
        List<DetalleMaltaEntity> detallesMalta = versionReceta.getDetallesMalta();
        double cantidadTotalBase = detallesMalta.stream().mapToDouble(DetalleMaltaEntity::getCantidad).sum();

        double extractoPotencialPromedio = detallesMalta.stream()
                .mapToDouble(detalle -> (detalle.getCantidad() / cantidadTotalBase) * (detalle.getMalta().getPotencialExtracto() / 100.0))
                .sum();

        double puntosDensidadObjetivo = (versionReceta.getOgObjetivo() - 1) * 1000 * volumenObjetivo;

        return puntosDensidadObjetivo / (extractoPotencialPromedio * macerador.getEficienciaMaceracion() * REFERENCIA_AZUCAR_PURA_PUNTOS_POR_KG_POR_L);
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
