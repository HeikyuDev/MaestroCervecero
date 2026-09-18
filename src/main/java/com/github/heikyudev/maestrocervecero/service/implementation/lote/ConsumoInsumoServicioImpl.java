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
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
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
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.insumo.MapperInsumo;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperConsumoInsumo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * Filtra los consumos de insumo de una etapa de lote puntual.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param tipoConsumo El tipo de consumo a filtrar, o {@code null} para no filtrar por él.
     * @param idInsumo El ID del insumo requerido a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ConsumoInsumoResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ConsumoInsumoResponseDTO> filtrarConsumosInsumo(Long idEtapaLote, TipoConsumo tipoConsumo, Long idInsumo, EstadoTransaccion estado, Pageable pageable) {
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
     * @return El consumo registrado.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el lote de insumo referenciados
     *                                      no existen.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si la etapa
     *                               no está EN_CURSO, si el tipo de etapa no admite registrar
     *                               consumos, si el insumo del lote de insumo no corresponde a un
     *                               insumo utilizado en esta etapa según la receta, si no existe
     *                               una reserva de ese lote de insumo para esa etapa, si la
     *                               cantidad consumida no es mayor a cero, o si supera la cantidad
     *                               reservada.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.CONSUMO_INSUMO)
    public ConsumoInsumoResponseDTO registrarConsumoInsumoReservado(Long idEtapaLote, ConsumoInsumoFormDTO consumoInsumoFormDTO) {
        // 1. Validar que la etapa de lote esté registrada en el sistema y habilitada para consumir
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));
        validarEtapaHabilitadaParaConsumo(etapaLote);

        // 2. Validar que el lote de insumo esté registrado en el sistema
        LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaConsumir(consumoInsumoFormDTO.getIdLoteInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + consumoInsumoFormDTO.getIdLoteInsumo()));

        // 3. Validar que el insumo de ese lote de insumo se corresponda con un insumo utilizado en esta etapa según la receta
        validarInsumoRequeridoPorEtapa(etapaLote, loteInsumo.getInsumo());

        // 4. Validar que exista una reserva de ese lote de insumo para esa etapa
        ReservaInsumoEntity reserva = reservaInsumoRepository.buscarPorEtapaLoteIdYLoteInsumoIdParaConsumir(etapaLote.getId(), loteInsumo.getId())
                .orElseThrow(() -> new ReglaNegocioException("No existe una reserva de este lote de insumo para esta etapa; utilice el registro de consumo directo"));

        // 5. Validar que la cantidad consumida sea mayor a cero
        Double cantidadConsumida = consumoInsumoFormDTO.getCantidadConsumida();
        if (cantidadConsumida == null || cantidadConsumida <= 0) {
            throw new ReglaNegocioException("La cantidad consumida debe ser mayor a cero");
        }

        // 6. Validar que la cantidad consumida no supere la cantidad reservada
        if (cantidadConsumida > reserva.getCantidadReservada()) {
            throw new ReglaNegocioException("La cantidad consumida no puede superar la cantidad reservada del lote de insumo");
        }

        // 7. Descontar la reserva y ejecutar el consumo sobre el lote de insumo
        reserva.setCantidadReservada(reserva.getCantidadReservada() - cantidadConsumida);
        reservaInsumoRepository.save(reserva);

        loteInsumo.ejecutarConsumo(cantidadConsumida);
        loteInsumoRepository.save(loteInsumo);

        // 8. Registrar el consumo, congelando el costo PPP vigente del lote de insumo, y persistir
        ConsumoInsumoEntity consumoInsumo = ConsumoInsumoEntity.builder()
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(loteInsumo.getCostoUnitarioPPP())
                .estado(EstadoTransaccion.REGISTRADO)
                .tipoConsumo(TipoConsumo.RESERVADO)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .build();

        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.save(consumoInsumo));
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
     * @return El consumo registrado.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el lote de insumo referenciados
     *                                      no existen.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si la etapa
     *                               no está EN_CURSO, si el tipo de etapa no admite registrar
     *                               consumos, si el insumo del lote de insumo no corresponde a un
     *                               insumo utilizado en esta etapa según la receta, si la cantidad
     *                               consumida no es mayor a cero, o si supera la cantidad
     *                               disponible del lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.CONSUMO_INSUMO)
    public ConsumoInsumoResponseDTO registrarConsumoInsumoDirecto(Long idEtapaLote, ConsumoInsumoFormDTO consumoInsumoFormDTO) {
        // 1. Validar que la etapa de lote esté registrada en el sistema y habilitada para consumir
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));
        validarEtapaHabilitadaParaConsumo(etapaLote);

        // 2. Validar que el lote de insumo esté registrado en el sistema
        LoteInsumoEntity loteInsumo = loteInsumoRepository.buscarPorIdParaConsumir(consumoInsumoFormDTO.getIdLoteInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + consumoInsumoFormDTO.getIdLoteInsumo()));

        // 3. Validar que el insumo de ese lote de insumo se corresponda con un insumo utilizado en esta etapa según la receta
        validarInsumoRequeridoPorEtapa(etapaLote, loteInsumo.getInsumo());

        // 4. Validar que la cantidad consumida sea mayor a cero
        Double cantidadConsumida = consumoInsumoFormDTO.getCantidadConsumida();
        if (cantidadConsumida == null || cantidadConsumida <= 0) {
            throw new ReglaNegocioException("La cantidad consumida debe ser mayor a cero");
        }

        // 5. Validar que la cantidad consumida no supere la cantidad disponible del lote de insumo
        if (cantidadConsumida > loteInsumo.getCantidadDisponible()) {
            throw new ReglaNegocioException("La cantidad consumida no puede superar la cantidad disponible del lote de insumo");
        }

        // 6. Ejecutar el consumo directo sobre el lote de insumo (sin tocar la cantidad reservada)
        loteInsumo.consumirDirecto(cantidadConsumida);
        loteInsumoRepository.save(loteInsumo);

        // 7. Registrar el consumo, congelando el costo PPP vigente del lote de insumo, y persistir
        ConsumoInsumoEntity consumoInsumo = ConsumoInsumoEntity.builder()
                .cantidadConsumida(cantidadConsumida)
                .costoUnitarioPPP(loteInsumo.getCostoUnitarioPPP())
                .estado(EstadoTransaccion.REGISTRADO)
                .tipoConsumo(TipoConsumo.DIRECTO)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .build();

        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.save(consumoInsumo));
    }

    /**
     * Obtiene, para una etapa de lote determinada, cuánto requiere de cada insumo según el
     * escalado de la receta y cuánto de eso ya fue consumido.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se gestionan consumos.
     * @return Un DTO por cada insumo requerido en esa etapa, con su cantidad requerida y consumida.
     * @throws RecursoNoEncontradoException Si no existe una etapa de lote con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public List<InsumoRequeridoResponseDTO> filtrarInsumosRequeridos(Long idEtapaLote) {
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));

        List<RequerimientoInsumo> requerimientos = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaLote);

        Map<Long, Double> cantidadConsumidaPorInsumo = consumoInsumoRepository.findByEtapaLoteIdAndEstado(idEtapaLote, EstadoTransaccion.REGISTRADO).stream()
                .collect(Collectors.groupingBy(consumo -> consumo.getLoteInsumo().getInsumo().getId(), Collectors.summingDouble(ConsumoInsumoEntity::getCantidadConsumida)));

        return requerimientos.stream()
                .map(requerimiento -> InsumoRequeridoResponseDTO.builder()
                        .insumo(MapperInsumo.toDTO(requerimiento.insumo()))
                        .cantidadRequerida(requerimiento.cantidadRequerida())
                        .cantidadConsumida(cantidadConsumidaPorInsumo.getOrDefault(requerimiento.insumo().getId(), 0.0))
                        .build())
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
        if (anularConsumoInsumoFormDTO.getMotivoAnulacion() == null || anularConsumoInsumoFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Validar que el consumo de insumo esté registrado en el sistema
        ConsumoInsumoEntity consumoInsumo = consumoInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el consumo de insumo con ID: " + id));

        // 3. Validar que el consumo se encuentre en estado REGISTRADO
        if (consumoInsumo.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular consumos de insumo en estado REGISTRADO");
        }

        // 4. Validar que el lote asociado se encuentre en estado EN_EJECUCION
        if (consumoInsumo.getEtapaLote().getLote().getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder anular un consumo de insumo");
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
            reserva.setCantidadReservada(reserva.getCantidadReservada() + consumoInsumo.getCantidadConsumida());
            reservaInsumoRepository.save(reserva);
            loteInsumo.revertirConsumo(consumoInsumo.getCantidadConsumida());
        } else {
            loteInsumo.revertirConsumoDirecto(consumoInsumo.getCantidadConsumida());
        }
        loteInsumoRepository.save(loteInsumo);

        // 7. Aplicar la anulación sobre el consumo y persistir
        consumoInsumo.setEstado(EstadoTransaccion.ANULADO);
        consumoInsumo.setFechaAnulacion(LocalDateTime.now());
        consumoInsumo.setMotivoAnulacion(anularConsumoInsumoFormDTO.getMotivoAnulacion());

        return MapperConsumoInsumo.toDTO(consumoInsumoRepository.save(consumoInsumo));
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
            throw new ReglaNegocioException("El lote debe estar en ejecución para registrar consumos de insumo");
        }

        if (etapaLote.getEstado() != EstadoEtapaLote.EN_CURSO) {
            throw new ReglaNegocioException("La etapa debe estar en curso para registrar consumos de insumo");
        }

        TipoEtapa tipoEtapa = etapaLote.getEtapa();
        if (!tipoEtapa.isPermiteRegistrarConsumo()) {
            throw new ReglaNegocioException("La etapa de tipo " + tipoEtapa + " no permite registrar consumos de insumo");
        }
    }

    /**
     * Valida que el insumo indicado sea efectivamente uno de los que la receta vigente del lote
     * requiere en esa etapa puntual: Malta solo en Maceración, Levadura solo en Fermentación, y
     * Lúpulo según la etapa de uso particular de cada línea de detalle (puede requerirse en una
     * etapa y no en otra, aunque el mismo lúpulo aparezca en ambas).
     *
     * @param etapaLote La etapa de lote sobre la que se quiere registrar el consumo.
     * @param insumo El insumo del lote de insumo que se pretende consumir.
     * @throws ReglaNegocioException Si el insumo no corresponde a un insumo utilizado en esa etapa
     *                               según la receta vigente del lote.
     */
    private void validarInsumoRequeridoPorEtapa(EtapaLoteEntity etapaLote, InsumoEntity insumo) {
        VersionRecetaEntity versionReceta = etapaLote.getLote().getPlanificacionProduccion().getVersionReceta();
        TipoEtapa etapaActual = etapaLote.getEtapa();

        boolean esRequerido = switch (insumo) {
            case MaltaEntity malta -> etapaActual == TipoEtapa.MACERACION
                    && versionReceta.getDetallesMalta().stream()
                    .anyMatch(detalle -> detalle.getMalta().getId().equals(malta.getId()));
            case LupuloEntity lupulo -> versionReceta.getDetallesLupulo().stream()
                    .anyMatch(detalle -> detalle.getLupulo().getId().equals(lupulo.getId()) && detalle.getEtapaDeUso() == etapaActual);
            case LevaduraEntity levadura -> etapaActual == TipoEtapa.FERMENTACION
                    && versionReceta.getDetallesLevadura().stream()
                    .anyMatch(detalle -> detalle.getLevadura().getId().equals(levadura.getId()));
            default -> false;
        };

        if (!esRequerido) {
            throw new ReglaNegocioException("El insumo del lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta");
        }
    }
}
