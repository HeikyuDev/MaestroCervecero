package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;

import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IDespachoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DespachoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperDespachoBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DespachoBarrilServicioImpl implements IDespachoBarrilServicio {

    /** Nombre del campo del formulario con el cliente al que se despacha el barril. */
    private static final String CAMPO_CLIENTE = "idCliente";
    /** Nombre del campo del formulario con la fecha estimada de devolución. */
    private static final String CAMPO_FECHA_DEVOLUCION_ESTIMADA = "fechaDevolucionEstimada";

    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IClienteRepository clienteRepository;
    private final IBarrilRepository barrilRepository;
    private final ISolicitudBusquedaRepository solicitudBusquedaRepository;

    /**
     * Filtra los despachos de barril, opcionalmente por estado, barril, cliente y/o rango de
     * fecha de despacho.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un despacho anulado sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param idCliente El ID del cliente a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDespachoDesde Límite inferior (inclusive) del rango de fecha de despacho, o {@code null} para no acotarlo.
     * @param fechaDespachoHasta Límite superior (inclusive) del rango de fecha de despacho, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link DespachoBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DespachoBarrilResponseDTO> filtrarDespachosBarril(EstadoTransaccion estado, Long idBarril, Long idCliente, LocalDateTime fechaDespachoDesde, LocalDateTime fechaDespachoHasta, Pageable pageable) {
        return despachoBarrilRepository.filtrarDespachosBarril(estado, idBarril, idCliente, fechaDespachoDesde, fechaDespachoHasta, pageable)
                .map(MapperDespachoBarril::toDTO);
    }

    /**
     * Busca y retorna un despacho de barril mediante su identificador único.
     *
     * @param id El ID del despacho de barril.
     * @return El despacho de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún despacho de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public DespachoBarrilResponseDTO buscarPorId(Long id) {
        return MapperDespachoBarril.toDTO(despachoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + id)));
    }

    /**
     * Registra un nuevo despacho de barril.
     * <p>
     * Deja al barril afectado en estado operativo {@code DESPACHADO}. La fecha y hora del despacho es la del momento
     * del registro, y la fecha estimada de devolución no puede ser anterior a la fecha actual.
     * </p>
     * <p>
     * Orden de locks: el cliente se lee sin bloqueo; lo primero que se bloquea (PESSIMISTIC_WRITE) es el barril. Es el
     * único lock de la operación.
     * </p>
     *
     * @param despachoBarrilFormDTO Los datos del despacho a registrar.
     * @return El despacho de barril registrado.
     * @throws ReglaNegocioException Si la fecha estimada de devolución o las observaciones no fueron informadas, si las observaciones superan el máximo, si el barril no se encuentra en estado operativo {@code CON_CERVEZA}, o si la fecha estimada de devolución es anterior a la fecha actual.
     * @throws RecursoNoEncontradoException Si el barril o el cliente referenciados no existen.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.DESPACHO_BARRIL)
    public DespachoBarrilResponseDTO registrarDespachoBarril(DespachoBarrilFormDTO despachoBarrilFormDTO) {
        // 1. Tomar la fecha y hora actual como fecha de la operación
        LocalDateTime fecha = LocalDateTime.now();

        // 2. Validar que se haya informado la fecha estimada de devolución
        if (despachoBarrilFormDTO.getFechaDevolucionEstimada() == null) {
            throw new ReglaNegocioException("La fecha estimada de devolución es obligatoria", CAMPO_FECHA_DEVOLUCION_ESTIMADA);
        }

        // 3. Validar las observaciones
        String observaciones = MetodosCicloVida.validarObservaciones(despachoBarrilFormDTO.getObservaciones());

        // 4. Localizar el cliente. Si no existe o no está activo, se dispara RecursoNoEncontradoException
        ClienteEntity clienteEntity = clienteRepository.findById(despachoBarrilFormDTO.getIdCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + despachoBarrilFormDTO.getIdCliente(), CAMPO_CLIENTE));

        // 5. Localizar el barril, bloqueado para escritura, antes de leer cualquier otro dato
        Long idBarril = despachoBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 6. Validar que el barril se encuentre en estado operativo CON_CERVEZA
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.CON_CERVEZA) {
            throw new ReglaNegocioException("Solo se puede registrar un despacho sobre un barril en estado operativo Con cerveza", MetodosCicloVida.CAMPO_BARRIL);
        }

        // 7. Validar que la fecha estimada de devolución sea posterior o igual a la fecha actual
        if (despachoBarrilFormDTO.getFechaDevolucionEstimada().isBefore(fecha.toLocalDate())) {
            throw new ReglaNegocioException("La fecha estimada de devolución no puede ser anterior a la fecha actual", CAMPO_FECHA_DEVOLUCION_ESTIMADA);
        }

        // 8. Cambiar el estado operativo del barril a DESPACHADO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DESPACHADO);
        barrilRepository.save(barrilEntity);

        // 9. Construir y persistir el despacho, y retornar el DTO de respuesta correspondiente
        DespachoBarrilEntity despachoBarrilEntity = DespachoBarrilEntity.builder()
                .fecha(fecha)
                .observaciones(observaciones)
                .fechaDevolucionEstimada(despachoBarrilFormDTO.getFechaDevolucionEstimada())
                .cliente(clienteEntity)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperDespachoBarril.toDTO(despachoBarrilRepository.save(despachoBarrilEntity));
    }

    /**
     * Anula un despacho de barril existente.
     * <p>
     * Solo procede si es la última operación registrada del barril (la de mayor fecha, y a igual fecha la de mayor ID,
     * entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones), si el barril no está en uso por un lote y
     * si es posterior al último envasado del barril. Restablece al barril afectado a estado
     * operativo {@code CON_CERVEZA}: solo procede si ese barril todavía se encuentra en {@code DESPACHADO}, para
     * garantizar que la transición de vuelta sea coherente. Si el despacho tiene una solicitud de búsqueda pendiente, la
     * deja {@code CANCELADA} (no cuenta como buscada).
     * </p>
     * <p>
     * Orden de locks: se bloquea primero el barril (el ID se resuelve con una consulta escalar, sin cargar el
     * despacho) y recién después se lee el despacho y se verifica que sea el último. Es el único lock de la operación.
     * </p>
     *
     * @param id El ID del despacho de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El despacho de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si el despacho no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del barril, si el barril está en uso por un lote, si el barril tuvo un envasado posterior, o si el barril asociado no se encuentra en estado operativo {@code DESPACHADO}.
     * @throws RecursoNoEncontradoException Si el despacho de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.DESPACHO_BARRIL)
    public DespachoBarrilResponseDTO anularDespachoBarril(Long id, AnulacionDespachoBarrilFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVida.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el barril del despacho sin cargarlo y bloquearlo para escritura. Si el despacho no existe, se
        //    dispara RecursoNoEncontradoException
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + id));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 3. Localizar el despacho, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        DespachoBarrilEntity despachoBarrilEntity = despachoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + id));

        // 4. Validar que el despacho se encuentre en estado REGISTRADO
        if (despachoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular despachos de barril en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVida.validarEsLaUltimaOperacion(
                despachoBarrilEntity.getId(),
                operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null));

        // 6. Validar que el barril no esté en uso por un lote
        MetodosCicloVida.validarNoEstaEnUso(barrilEntity.getEstadoOperativo());

        // 7. Validar que el despacho sea posterior al último envasado del barril
        MetodosCicloVida.validarPosteriorAlUltimoEnvasado(
                despachoBarrilEntity.getFecha(),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Validar que el barril se encuentre en estado operativo DESPACHADO, para garantizar que la transición de
        //    vuelta a CON_CERVEZA sea coherente
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("Solo se puede anular un despacho cuyo barril asociado se encuentre en estado operativo Despachado");
        }

        // 9. Restablecer el estado operativo del barril a CON_CERVEZA y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.CON_CERVEZA);
        barrilRepository.save(barrilEntity);

        // 10. Aplicar la anulación sobre el despacho y persistirlo
        despachoBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        despachoBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        despachoBarrilEntity.setMotivoAnulacion(motivoAnulacion);

        // 11. Cancelar la solicitud de búsqueda pendiente del despacho, si tiene una: el barril vuelve a la fábrica sin
        //     haber estado en poder del cliente, así que no hay nada que buscar (no cuenta como buscada). El barril ya está
        //     bloqueado, igual que al registrar la solicitud o la devolución
        solicitudBusquedaRepository.findFirstByDespachoBarril_IdAndEstado(id, EstadoSolicitudBusqueda.PENDIENTE).ifPresent(solicitud -> {
            solicitud.setEstado(EstadoSolicitudBusqueda.CANCELADA);
            solicitudBusquedaRepository.save(solicitud);
        });

        return MapperDespachoBarril.toDTO(despachoBarrilRepository.save(despachoBarrilEntity));
    }
}
