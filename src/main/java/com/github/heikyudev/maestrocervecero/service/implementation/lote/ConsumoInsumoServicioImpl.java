package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AdicionLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperInsumo;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperConsumoInsumo;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación de {@link IConsumoInsumoServicio}.
 */
@Service
@RequiredArgsConstructor
public class ConsumoInsumoServicioImpl implements IConsumoInsumoServicio {

    private final IConsumoInsumoRepository consumoInsumoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IReservaInsumoRepository reservaInsumoRepository;
    private final IEscaladoInsumoServicio escaladoInsumoServicio;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Filtra los consumos de insumo de una etapa de lote puntual.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param tipoConsumo El tipo de consumo a filtrar, o {@code null} para no filtrar por él.
     * @param idInsumo El ID del insumo requerido a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ConsumoInsumoResponseDTO} correspondientes.
     * @throws ReglaNegocioException Si no se informó la etapa de lote.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ConsumoInsumoResponseDTO> filtrarConsumosInsumo(Long idEtapaLote, TipoConsumo tipoConsumo, Long idInsumo, EstadoTransaccion estado, Pageable pageable) {
        validarEtapaLoteInformada(idEtapaLote);
        return consumoInsumoRepository.filtrarConsumosInsumo(idEtapaLote, tipoConsumo, idInsumo, estado, pageable)
                .map(MapperConsumoInsumo::toDTO);
    }

    /**
     * Busca y retorna un consumo de insumo mediante su identificador único.
     *
     * @param id El ID del consumo.
     * @return El consumo correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un consumo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public ConsumoInsumoResponseDTO buscarPorId(Long id) {
        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el consumo de insumo con ID: " + id)));
    }

    /**
     * Registra el consumo efectivo de un insumo, descontándolo de una reserva de insumo puntual
     * ya existente para esa etapa de lote y ese lote de insumo.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra el consumo (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param consumoInsumoFormDTO Los datos del consumo a registrar.
     * @return El consumo registrado, con sus advertencias: el lote de insumo vencido (una reserva de un lote que
     *         se venció durante la ejecución se mantiene y se puede consumir) y el exceso del total consumido
     *         del insumo en la etapa sobre la cantidad requerida. Las advertencias no impiden el consumo.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el lote de insumo referenciados
     *                                      no existen.
     * @throws ReglaNegocioException Si no se informó el lote de insumo, si la cantidad consumida no se
     *                               informó, no es un número mayor a 0 o supera el máximo técnico
     *                               (estas validaciones se hacen antes de consultar nada), si el lote
     *                               no se encuentra en estado EN_EJECUCION, si la etapa no está
     *                               EN_CURSO, si el tipo de etapa no admite registrar consumos, si el
     *                               insumo del lote de insumo no corresponde a un insumo utilizado en
     *                               esta etapa según la receta, si no existe una reserva de ese lote
     *                               de insumo para esa etapa, o si la cantidad consumida (redondeada
     *                               a 4 decimales) supera la cantidad reservada.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.CONSUMO_INSUMO)
    public ConsumoInsumoResponseDTO registrarConsumoInsumoReservado(Long idEtapaLote, ConsumoInsumoFormDTO consumoInsumoFormDTO) {
        // 1. Validar los datos del formulario antes de consultar nada: el lote de insumo y la cantidad consumida
        Long idLoteInsumo = validarLoteInsumoInformado(consumoInsumoFormDTO.getIdLoteInsumo());
        double cantidadConsumida = validarCantidadConsumida(consumoInsumoFormDTO.getCantidadConsumida());

        // 2. Validar que la etapa de lote esté registrada en el sistema y habilitada para consumir
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));
        validarEtapaHabilitadaParaConsumo(etapaLote);

        // 3. Validar que el lote de insumo esté registrado en el sistema
        LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaConsumir(idLoteInsumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + idLoteInsumo));

        // 4. Validar que el insumo de ese lote de insumo se corresponda con un insumo utilizado en esta etapa según la receta
        validarInsumoRequeridoPorEtapa(etapaLote, loteInsumo.getInsumo());

        // 5. Validar que exista una reserva de ese lote de insumo para esa etapa
        ReservaInsumoEntity reserva = reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(etapaLote.getId(), loteInsumo.getId())
                .orElseThrow(() -> new ReglaNegocioException("No existe una reserva de este lote de insumo para esta etapa; utilice el registro de consumo directo"));

        // 6. Validar que la cantidad consumida no supere la cantidad reservada
        double cantidadReservada = Redondeo.redondear(reserva.getCantidadReservada());
        if (cantidadConsumida > cantidadReservada) {
            throw new ReglaNegocioException("La cantidad consumida no puede superar la cantidad reservada del lote de insumo ("
                    + describirCantidad(cantidadReservada, loteInsumo) + ")");
        }

        // 7. Armar las advertencias (lote vencido y exceso sobre lo requerido), que no impiden el consumo
        List<String> advertencias = construirAdvertencias(etapaLote, loteInsumo, cantidadConsumida);

        // 8. Descontar la reserva y ejecutar el consumo sobre el lote de insumo
        reserva.setCantidadReservada(Redondeo.redondear(cantidadReservada - cantidadConsumida));
        reservaInsumoRepository.save(reserva);

        loteInsumo.ejecutarConsumo(cantidadConsumida);
        loteInsumoRepository.save(loteInsumo);

        // 9. Registrar el consumo, congelando el costo PPP vigente del lote de insumo, y persistir
        ConsumoInsumoEntity consumoInsumo = ConsumoInsumoEntity.builder()
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(loteInsumo.getCostoUnitarioPPP())
                .estado(EstadoTransaccion.REGISTRADO)
                .tipoConsumo(TipoConsumo.RESERVADO)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .build();

        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.save(consumoInsumo), advertencias);
    }

    /**
     * Registra el consumo efectivo de un insumo por fuera de cualquier reserva, descontándolo
     * directamente de la cantidad disponible de un lote de insumo. No requiere que la cantidad
     * reservada de ese insumo para esa etapa esté agotada: el operario puede elegir libremente
     * registrar un consumo directo sobre cualquier lote de insumo de ese insumo requerido por la
     * etapa, en cualquier momento.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra el consumo (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param consumoInsumoFormDTO Los datos del consumo a registrar.
     * @return El consumo registrado, con sus advertencias: el lote de insumo vencido (el consumo directo sobre un
     *         lote vencido se permite) y el exceso del total consumido del insumo en la etapa sobre la cantidad
     *         requerida. Las advertencias no impiden el consumo.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el lote de insumo referenciados
     *                                      no existen.
     * @throws ReglaNegocioException Si no se informó el lote de insumo, si la cantidad consumida no se
     *                               informó, no es un número mayor a 0 o supera el máximo técnico
     *                               (estas validaciones se hacen antes de consultar nada), si el lote
     *                               no se encuentra en estado EN_EJECUCION, si la etapa no está
     *                               EN_CURSO, si el tipo de etapa no admite registrar consumos, si el
     *                               insumo del lote de insumo no corresponde a un insumo utilizado en
     *                               esta etapa según la receta, o si la cantidad consumida (redondeada
     *                               a 4 decimales) supera la cantidad disponible del lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.CONSUMO_INSUMO)
    public ConsumoInsumoResponseDTO registrarConsumoInsumoDirecto(Long idEtapaLote, ConsumoInsumoFormDTO consumoInsumoFormDTO) {
        // 1. Validar los datos del formulario antes de consultar nada: el lote de insumo y la cantidad consumida
        Long idLoteInsumo = validarLoteInsumoInformado(consumoInsumoFormDTO.getIdLoteInsumo());
        double cantidadConsumida = validarCantidadConsumida(consumoInsumoFormDTO.getCantidadConsumida());

        // 2. Validar que la etapa de lote esté registrada en el sistema y habilitada para consumir
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));
        validarEtapaHabilitadaParaConsumo(etapaLote);

        // 3. Validar que el lote de insumo esté registrado en el sistema
        LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaConsumir(idLoteInsumo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + idLoteInsumo));

        // 4. Validar que el insumo de ese lote de insumo se corresponda con un insumo utilizado en esta etapa según la receta
        validarInsumoRequeridoPorEtapa(etapaLote, loteInsumo.getInsumo());

        // 5. Validar que la cantidad consumida no supere la cantidad disponible del lote de insumo
        double cantidadDisponible = loteInsumo.getCantidadDisponible();
        if (cantidadConsumida > cantidadDisponible) {
            throw new ReglaNegocioException("La cantidad consumida no puede superar la cantidad disponible del lote de insumo ("
                    + describirCantidad(cantidadDisponible, loteInsumo) + ")");
        }

        // 6. Armar las advertencias (lote vencido y exceso sobre lo requerido), que no impiden el consumo
        List<String> advertencias = construirAdvertencias(etapaLote, loteInsumo, cantidadConsumida);

        // 7. Ejecutar el consumo directo sobre el lote de insumo (sin tocar la cantidad reservada)
        loteInsumo.consumirDirecto(cantidadConsumida);
        loteInsumoRepository.save(loteInsumo);

        // 8. Registrar el consumo, congelando el costo PPP vigente del lote de insumo, y persistir
        ConsumoInsumoEntity consumoInsumo = ConsumoInsumoEntity.builder()
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(loteInsumo.getCostoUnitarioPPP())
                .estado(EstadoTransaccion.REGISTRADO)
                .tipoConsumo(TipoConsumo.DIRECTO)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .build();

        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.save(consumoInsumo), advertencias);
    }

    /**
     * Obtiene, para una etapa de lote determinada, cuánto requiere de cada insumo según el
     * escalado de la receta y cuánto de eso ya fue consumido.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se gestionan consumos.
     * @return Un DTO por cada insumo requerido en esa etapa, con su cantidad requerida y consumida.
     * @throws RecursoNoEncontradoException Si no existe una etapa de lote con el ID especificado.
     * @throws ReglaNegocioException Si no se informó la etapa de lote.
     */
    /**
     * Obtiene las adiciones de lúpulo de una etapa, cada una con su cantidad y su tiempo de hervor, sin fusionar las del
     * mismo lúpulo.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @return Las adiciones de lúpulo de la etapa; vacía si no usa lúpulo.
     * @throws ReglaNegocioException Si no se informó la etapa de lote.
     * @throws RecursoNoEncontradoException Si la etapa de lote no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AdicionLupuloResponseDTO> filtrarAdicionesLupulo(Long idEtapaLote) {
        validarEtapaLoteInformada(idEtapaLote);
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));

        return escaladoInsumoServicio.calcularAdicionesLupulo(etapaLote).stream()
                .map(adicion -> AdicionLupuloResponseDTO.builder()
                        .nombreInsumo(adicion.insumo().getNombre())
                        .unidadDeMedida(adicion.insumo().getUnidadDeMedida())
                        .uso(adicion.uso())
                        .tiempoDeHervor(adicion.tiempoDeHervor())
                        .cantidad(adicion.cantidad())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsumoRequeridoResponseDTO> filtrarInsumosRequeridos(Long idEtapaLote) {
        validarEtapaLoteInformada(idEtapaLote);
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));

        List<RequerimientoInsumo> requerimientos = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaLote);

        Map<Long, Double> cantidadConsumidaPorInsumo = consumoInsumoRepository.findByEtapaLoteIdAndEstado(idEtapaLote, EstadoTransaccion.REGISTRADO).stream()
                .collect(Collectors.groupingBy(consumo -> consumo.getLoteInsumo().getInsumo().getId(), Collectors.summingDouble(ConsumoInsumoEntity::getCantidadConsumida)));

        // Saldo reservado por insumo: una sola consulta agrupada para toda la etapa (evita una consulta por insumo)
        Map<Long, Double> saldoReservadoPorInsumo = new HashMap<>();
        for (Object[] fila : reservaInsumoRepository.sumarSaldoReservadoPorInsumoDeEtapa(idEtapaLote)) {
            saldoReservadoPorInsumo.put(((Number) fila[0]).longValue(), ((Number) fila[1]).doubleValue());
        }

        return requerimientos.stream()
                .map(requerimiento -> {
                    double cantidadConsumida = cantidadConsumidaPorInsumo.getOrDefault(requerimiento.insumo().getId(), 0.0);
                    return InsumoRequeridoResponseDTO.builder()
                            .insumo(MapperInsumo.toDTO(requerimiento.insumo()))
                            .cantidadRequerida(requerimiento.cantidadRequerida())
                            .cantidadConsumida(cantidadConsumida)
                            .cantidadReservada(Redondeo.redondear(saldoReservadoPorInsumo.getOrDefault(requerimiento.insumo().getId(), 0.0)))
                            .unidadDeMedida(requerimiento.insumo().getUnidadDeMedida())
                            .porcentajeConsumido(calcularPorcentajeConsumido(cantidadConsumida, requerimiento.cantidadRequerida()))
                            .build();
                })
                .toList();
    }

    /**
     * Anula un consumo de insumo existente, devolviendo el stock que había descontado.
     *
     * @param id El ID del consumo de insumo a anular.
     * @param anularConsumoInsumoFormDTO Los datos de la anulación (motivo).
     * @return El consumo de insumo anulado.
     * @throws RecursoNoEncontradoException Si el consumo de insumo, el lote de insumo, o (para uno
     *                                      RESERVADO) la reserva puntual no existen.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el consumo no
     *                               se encuentra en estado REGISTRADO, si el lote asociado no se
     *                               encuentra en estado EN_EJECUCION, o si la etapa no se encuentra
     *                               en estado EN_CURSO.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.CONSUMO_INSUMO)
    public ConsumoInsumoResponseDTO anularConsumoInsumo(Long id, AnularConsumoInsumoFormDTO anularConsumoInsumoFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        String motivoAnulacion = NormalizacionTexto.nombrePropio(anularConsumoInsumoFormDTO.getMotivoAnulacion());
        if (motivoAnulacion == null || motivoAnulacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoAnulacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de anulación");

        // 2. Validar que el consumo de insumo esté registrado en el sistema
        ConsumoInsumoEntity consumoInsumo = consumoInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el consumo de insumo con ID: " + id));

        // 3. Validar que el consumo se encuentre en estado REGISTRADO
        if (consumoInsumo.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular consumos de insumo en estado Registrado");
        }

        // 4. Validar que el lote asociado se encuentre en estado EN_EJECUCION
        if (consumoInsumo.getEtapaLote().getLote().getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder anular un consumo de insumo");
        }

        // 5. Validar que la etapa se encuentre en curso
        if (consumoInsumo.getEtapaLote().getEstado() != EstadoEtapaLote.EN_CURSO) {
            throw new ReglaNegocioException("La etapa debe estar en curso para poder anular un consumo de insumo");
        }

        // 6. Recuperar el lote de insumo bloqueado para escritura (no alcanza con navegar la
        //    relación perezosa del consumo: hay que releerlo con lock, ya que esta anulación va a
        //    mutar su stock) y revertir en él la cantidad consumida
        LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaConsumir(consumoInsumo.getLoteInsumo().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + consumoInsumo.getLoteInsumo().getId()));

        if (consumoInsumo.getTipoConsumo() == TipoConsumo.RESERVADO) {
            // Un consumo reservado descontó, además del lote de insumo, la reserva puntual de esa
            // etapa: hay que devolvérsela también a ella (bloqueada), no solo al lote de insumo
            ReservaInsumoEntity reserva = reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(
                            consumoInsumo.getEtapaLote().getId(), loteInsumo.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la reserva de insumo de esta etapa para este lote de insumo"));
            reserva.setCantidadReservada(Redondeo.redondear(reserva.getCantidadReservada() + consumoInsumo.getCantidadConsumida()));
            reservaInsumoRepository.save(reserva);
            loteInsumo.revertirConsumo(consumoInsumo.getCantidadConsumida());
        } else {
            loteInsumo.revertirConsumoDirecto(consumoInsumo.getCantidadConsumida());
        }
        loteInsumoRepository.save(loteInsumo);

        // 7. Aplicar la anulación sobre el consumo y persistir
        consumoInsumo.setEstado(EstadoTransaccion.ANULADO);
        consumoInsumo.setFechaAnulacion(LocalDateTime.now());
        consumoInsumo.setMotivoAnulacion(motivoAnulacion);

        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.save(consumoInsumo));
    }

    /**
     * Calcula el porcentaje consumido sobre lo requerido, redondeado a 4 decimales y sin tope. Es 0 si lo requerido es 0.
     * Lo usa también el avance hacia el mínimo de consumo de la etapa.
     */
    static double calcularPorcentajeConsumido(double cantidadConsumida, double cantidadRequerida) {
        if (cantidadRequerida <= 0) {
            return 0.0;
        }
        return Redondeo.redondear(cantidadConsumida / cantidadRequerida * 100.0);
    }

    /**
     * Valida que una etapa de lote esté habilitada para registrar consumos de insumo: el lote
     * asociado debe estar EN_EJECUCION, la etapa debe ser la etapa actual del lote (EN_CURSO), y
     * su tipo de etapa debe admitir el registro de consumos (por ejemplo, ENVASADO no admite).
     *
     * @param etapaLote La etapa de lote a validar.
     * @throws ReglaNegocioException Si alguna de las tres condiciones no se cumple.
     */
    private void validarEtapaHabilitadaParaConsumo(EtapaLoteEntity etapaLote) {
        LoteEntity lote = etapaLote.getLote();
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder registrar consumos de insumo");
        }

        if (etapaLote.getEstado() != EstadoEtapaLote.EN_CURSO) {
            throw new ReglaNegocioException("La etapa debe estar en curso para poder registrar consumos de insumo");
        }

        TipoEtapa tipoEtapa = etapaLote.getEtapa();
        if (!tipoEtapa.isPermiteRegistrarConsumo()) {
            throw new ReglaNegocioException("La etapa " + tipoEtapa.getEtiqueta() + " no admite el registro de consumos de insumo");
        }
    }

    /**
     * Valida que el insumo indicado sea efectivamente uno de los que la receta vigente del lote
     * requiere en esa etapa puntual: Malta solo en Molienda, Levadura solo en Fermentación, y
     * Lúpulo según la etapa de uso particular de cada línea de detalle (puede requerirse en una
     * etapa y no en otra, aunque el mismo lúpulo aparezca en ambas).
     * <p>
     * La comparación es por ID y no por el tipo de la entidad: el insumo de un lote de insumo es una
     * relación perezosa, que llega como un proxy de Hibernate de la clase abstracta {@code InsumoEntity}
     * y no es instancia de {@code MaltaEntity}, {@code LupuloEntity} ni {@code LevaduraEntity}. Los ID de
     * los insumos son únicos entre todos los tipos (comparten la tabla raíz), así que el ID alcanza para
     * identificar al insumo en los detalles de la receta.
     * </p>
     *
     * @param etapaLote La etapa de lote sobre la que se quiere registrar el consumo.
     * @param insumo El insumo del lote de insumo que se pretende consumir.
     * @throws ReglaNegocioException Si el insumo no corresponde a un insumo utilizado en esa etapa
     *                               según la receta vigente del lote.
     */
    private void validarInsumoRequeridoPorEtapa(EtapaLoteEntity etapaLote, InsumoEntity insumo) {
        VersionRecetaEntity versionReceta = etapaLote.getLote().getPlanificacionProduccion().getVersionReceta();
        TipoEtapa etapaActual = etapaLote.getEtapa();
        Long idInsumo = insumo.getId();

        boolean esMaltaRequerida = etapaActual == TipoEtapa.MOLIENDA
                && versionReceta.getDetallesMalta().stream()
                .anyMatch(detalle -> idInsumo.equals(detalle.getMalta().getId()));
        boolean esLupuloRequerido = versionReceta.getDetallesLupulo().stream()
                .anyMatch(detalle -> idInsumo.equals(detalle.getLupulo().getId()) && detalle.getEtapaDeUso() == etapaActual);
        boolean esLevaduraRequerida = etapaActual == TipoEtapa.FERMENTACION
                && versionReceta.getDetallesLevadura().stream()
                .anyMatch(detalle -> idInsumo.equals(detalle.getLevadura().getId()));

        if (!esMaltaRequerida && !esLupuloRequerido && !esLevaduraRequerida) {
            throw new ReglaNegocioException("El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        }
    }

    /**
     * Arma las advertencias de un consumo que está por registrarse. Las advertencias no son errores: el
     * consumo se registra igual.
     * <ul>
     *     <li>Lote de insumo vencido: su fecha de vencimiento es anterior a hoy (si vence hoy todavía es válido).</li>
     *     <li>Exceso: el total consumido (registrado) del insumo en la etapa, incluyendo este consumo, supera la
     *     cantidad requerida por el escalado para esa etapa. Ambos lados se redondean a 4 decimales.</li>
     * </ul>
     *
     * @param etapaLote La etapa sobre la que se registra el consumo.
     * @param loteInsumo El lote de insumo del que se descuenta el consumo.
     * @param cantidadConsumida La cantidad que se va a consumir, ya redondeada.
     * @return Las advertencias en el orden vencimiento, exceso; vacía si no hay ninguna.
     */
    private List<String> construirAdvertencias(EtapaLoteEntity etapaLote, LoteInsumoEntity loteInsumo, double cantidadConsumida) {
        List<String> advertencias = new ArrayList<>();

        if (loteInsumo.getFechaVencimiento().isBefore(LocalDate.now())) {
            advertencias.add("Este lote de insumo se encuentra vencido (venció el " + loteInsumo.getFechaVencimiento().format(FORMATO_FECHA) + ")");
        }

        Long idInsumo = loteInsumo.getInsumo().getId();
        double cantidadRequerida = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaLote).stream()
                .filter(requerimiento -> idInsumo.equals(requerimiento.insumo().getId()))
                .mapToDouble(RequerimientoInsumo::cantidadRequerida)
                .sum();
        double cantidadYaConsumida = consumoInsumoRepository.findByEtapaLoteIdAndEstado(etapaLote.getId(), EstadoTransaccion.REGISTRADO).stream()
                .filter(consumo -> idInsumo.equals(consumo.getLoteInsumo().getInsumo().getId()))
                .mapToDouble(ConsumoInsumoEntity::getCantidadConsumida)
                .sum();
        double totalConsumido = Redondeo.redondear(cantidadYaConsumida + cantidadConsumida);

        if (cantidadRequerida > 0 && totalConsumido > Redondeo.redondear(cantidadRequerida)) {
            advertencias.add("El total consumido de " + loteInsumo.getInsumo().getNombre() + " en esta etapa ("
                    + describirCantidad(totalConsumido, loteInsumo) + ") supera la cantidad requerida ("
                    + describirCantidad(Redondeo.redondear(cantidadRequerida), loteInsumo) + ")");
        }

        return advertencias;
    }

    /**
     * Valida que se haya informado la etapa de lote, que los listados de consumos e insumos requeridos
     * necesitan para no mezclar consumos de otras etapas.
     *
     * @throws ReglaNegocioException Si la etapa de lote es nula.
     */
    private void validarEtapaLoteInformada(Long idEtapaLote) {
        if (idEtapaLote == null) {
            throw new ReglaNegocioException("La etapa del lote es obligatoria");
        }
    }

    /**
     * Valida que se haya informado el lote de insumo del que se descuenta el consumo.
     *
     * @return El ID del lote de insumo.
     * @throws ReglaNegocioException Si el ID es nulo.
     */
    private Long validarLoteInsumoInformado(Long idLoteInsumo) {
        if (idLoteInsumo == null) {
            throw new ReglaNegocioException("El lote de insumo es obligatorio");
        }
        return idLoteInsumo;
    }

    /**
     * Valida la cantidad consumida y la devuelve redondeada a la escala de las cantidades
     * ({@link LimitesTecnicos#ESCALA_CANTIDADES} decimales): debe informarse, ser un número finito
     * mayor a 0 (una vez redondeada) y no superar el máximo técnico. No consulta nada.
     *
     * @return La cantidad consumida redondeada.
     * @throws ReglaNegocioException Si la cantidad es nula, no es un número mayor a 0 o supera el máximo técnico.
     */
    private double validarCantidadConsumida(Double cantidadConsumida) {
        if (cantidadConsumida == null) {
            throw new ReglaNegocioException("La cantidad consumida es obligatoria");
        }
        double cantidadRedondeada = Redondeo.redondear(cantidadConsumida);
        if (cantidadRedondeada <= 0) {
            throw new ReglaNegocioException("La cantidad consumida debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(cantidadRedondeada, LimitesTecnicos.CANTIDAD_CONSUMIDA_MAXIMA,
                "La cantidad consumida", "kg o g, según el insumo");
        return cantidadRedondeada;
    }

    /**
     * Describe una cantidad de un insumo con la unidad de medida del insumo del lote de insumo (por ejemplo
     * {@code "12,35 kg"}).
     */
    private static String describirCantidad(double cantidad, LoteInsumoEntity loteInsumo) {
        return ValidacionesNumericas.formatearNumero(cantidad) + " " + loteInsumo.getInsumo().getUnidadDeMedida().getSimbolo();
    }
}
