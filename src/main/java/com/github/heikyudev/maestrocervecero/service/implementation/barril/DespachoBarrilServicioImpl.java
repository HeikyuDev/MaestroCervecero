package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
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

    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IFallaBarrilRepository fallaBarrilRepository;
    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IDevolucionBarrilRepository devolucionBarrilRepository;
    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IClienteRepository clienteRepository;
    private final IBarrilRepository barrilRepository;

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
     * Deja al barril afectado en estado operativo {@code DESPACHADO}. La fecha de despacho debe
     * ser posterior a la fecha de la última operación registrada sobre ese barril, contando tanto
     * las 5 operaciones del ciclo de vida (falla, mantenimiento, limpieza, despacho, devolución)
     * como el último envasado de lote que lo llenó: no tiene sentido despachar un barril con una
     * fecha anterior a la del envasado que lo cargó, ni a cualquier otra operación posterior.
     * </p>
     *
     * @param despachoBarrilFormDTO Los datos del despacho a registrar.
     * @return El despacho de barril registrado.
     * @throws ReglaNegocioException Si la fecha de despacho, la fecha estimada de devolución o las observaciones no fueron informadas, si la fecha de despacho es posterior a la fecha y hora actual, si la fecha estimada de devolución es anterior a la fecha de despacho, si la fecha de despacho no es posterior a la última operación registrada del barril, o si el barril no se encuentra en estado operativo {@code CON_CERVEZA}.
     * @throws RecursoNoEncontradoException Si el barril o el cliente referenciados no existen.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.DESPACHO_BARRIL)
    public DespachoBarrilResponseDTO registrarDespachoBarril(DespachoBarrilFormDTO despachoBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de despacho
        if (despachoBarrilFormDTO.getFechaDespacho() == null) {
            throw new ReglaNegocioException("La fecha de despacho es obligatoria");
        }

        // 2. Validar que la fecha de despacho no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(despachoBarrilFormDTO.getFechaDespacho());

        // 3. Validar que se haya informado la fecha estimada de devolución
        if (despachoBarrilFormDTO.getFechaDevolucionEstimada() == null) {
            throw new ReglaNegocioException("La fecha estimada de devolución es obligatoria");
        }

        // 4. Validar que se hayan informado las observaciones
        if (despachoBarrilFormDTO.getObservaciones() == null || despachoBarrilFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 5. Localizar el cliente. Si no existe o no está activo, se dispara RecursoNoEncontradoException
        ClienteEntity clienteEntity = clienteRepository.findById(despachoBarrilFormDTO.getIdCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + despachoBarrilFormDTO.getIdCliente()));

        // 6. Localizar el barril, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idBarril = despachoBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        // 7. Validar que el barril se encuentre en estado operativo CON_CERVEZA
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.CON_CERVEZA) {
            throw new ReglaNegocioException("Solo se puede registrar un despacho sobre un barril en estado operativo CON_CERVEZA");
        }

        // 8. Validar que la fecha estimada de devolución sea posterior o igual a la fecha de despacho
        if (despachoBarrilFormDTO.getFechaDevolucionEstimada().isBefore(despachoBarrilFormDTO.getFechaDespacho().toLocalDate())) {
            throw new ReglaNegocioException("La fecha estimada de devolución debe ser posterior o igual a la fecha de despacho");
        }

        // 9. Validar que la fecha de despacho sea posterior a la última operación registrada sobre
        //    este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                despachoBarrilFormDTO.getFechaDespacho(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 10. Cambiar el estado operativo del barril a DESPACHADO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DESPACHADO);
        barrilRepository.save(barrilEntity);

        // 11. Construir y persistir el despacho, y retornar el DTO de respuesta correspondiente
        DespachoBarrilEntity despachoBarrilEntity = DespachoBarrilEntity.builder()
                .fecha(despachoBarrilFormDTO.getFechaDespacho())
                .observaciones(despachoBarrilFormDTO.getObservaciones())
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
     * Restablece al barril afectado a estado operativo {@code CON_CERVEZA}: solo procede si ese
     * barril todavía se encuentra en {@code DESPACHADO}, para garantizar que la transición de
     * vuelta sea coherente.
     * </p>
     *
     * @param id El ID del despacho de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El despacho de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el despacho no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code DESPACHADO}.
     * @throws RecursoNoEncontradoException Si el despacho de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.DESPACHO_BARRIL)
    public DespachoBarrilResponseDTO anularDespachoBarril(Long id, AnulacionDespachoBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar el despacho de barril. Si no existe, se dispara RecursoNoEncontradoException
        DespachoBarrilEntity despachoBarrilEntity = despachoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + id));

        // 3. Validar que el despacho se encuentre en estado REGISTRADO
        if (despachoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular despachos de barril en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el barril, comparando
        //    contra las 5 tablas del ciclo de vida (evita anular un registro viejo cuando una
        //    operación posterior ya dejó al barril en un estado distinto)
        Long idBarril = despachoBarrilEntity.getBarril().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                despachoBarrilEntity.getFecha(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null));

        // 5. Localizar el barril asociado, bloqueado para escritura, y validar que se encuentre
        //    en estado operativo DESPACHADO, para garantizar que la transición de vuelta a
        //    CON_CERVEZA sea coherente
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(despachoBarrilEntity.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + despachoBarrilEntity.getBarril().getId()));
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("Solo se puede anular un despacho cuyo barril asociado se encuentre en estado operativo DESPACHADO");
        }

        // 6. Restablecer el estado operativo del barril a CON_CERVEZA y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.CON_CERVEZA);
        barrilRepository.save(barrilEntity);

        // 7. Aplicar la anulación sobre el despacho y persistirlo
        despachoBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        despachoBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        despachoBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperDespachoBarril.toDTO(despachoBarrilRepository.save(despachoBarrilEntity));
    }
}
