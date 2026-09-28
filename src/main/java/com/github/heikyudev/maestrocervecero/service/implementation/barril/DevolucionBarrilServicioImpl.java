package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DevolucionBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IDevolucionBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DevolucionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperDevolucionBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DevolucionBarrilServicioImpl implements IDevolucionBarrilServicio {

    private final IDevolucionBarrilRepository devolucionBarrilRepository;
    private final IFallaBarrilRepository fallaBarrilRepository;
    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Filtra las devoluciones de barril, opcionalmente por estado, barril y/o rango de fecha de
     * devolución.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una devolución anulada sigue siendo
     * un registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDevolucionDesde Límite inferior (inclusive) del rango de fecha de devolución, o {@code null} para no acotarlo.
     * @param fechaDevolucionHasta Límite superior (inclusive) del rango de fecha de devolución, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link DevolucionBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DevolucionBarrilResponseDTO> filtrarDevolucionesBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaDevolucionDesde, LocalDateTime fechaDevolucionHasta, Pageable pageable) {
        return devolucionBarrilRepository.filtrarDevolucionesBarril(estado, idBarril, fechaDevolucionDesde, fechaDevolucionHasta, pageable)
                .map(MapperDevolucionBarril::toDTO);
    }

    /**
     * Busca y retorna una devolución de barril mediante su identificador único.
     *
     * @param id El ID de la devolución de barril.
     * @return La devolución de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna devolución de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public DevolucionBarrilResponseDTO buscarPorId(Long id) {
        return MapperDevolucionBarril.toDTO(devolucionBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la devolución de barril con ID: " + id)));
    }

    /**
     * Registra una nueva devolución de barril.
     * <p>
     * Deja al barril afectado en estado operativo {@code EN_LIMPIEZA}: todo barril que vuelve de
     * afuera pasa por limpieza, por motivos sanitarios, sin importar su contenido restante.
     * </p>
     * <p>
     * {@code DevolucionBarrilEntity} no tiene una relación directa con el despacho al que
     * corresponde (ver la discusión de 3FN que llevó a este diseño): la regla "el despacho debe
     * existir y estar registrado" queda satisfecha estructuralmente por exigir que el barril se
     * encuentre en estado operativo {@code DESPACHADO}, ya que solo un despacho REGISTRADO puede
     * haber dejado al barril en ese estado.
     * </p>
     *
     * @param devolucionBarrilFormDTO Los datos de la devolución a registrar.
     * @return La devolución de barril registrada.
     * @throws ReglaNegocioException Si la fecha de devolución o las observaciones no fueron informadas, si la fecha de devolución es posterior a la fecha y hora actual, si no es posterior a la última operación registrada del barril, o si el barril no se encuentra en estado operativo {@code DESPACHADO}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.DEVOLUCION_BARRIL)
    public DevolucionBarrilResponseDTO registrarDevolucionBarril(DevolucionBarrilFormDTO devolucionBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de devolución
        if (devolucionBarrilFormDTO.getFechaDevolucion() == null) {
            throw new ReglaNegocioException("La fecha de devolución es obligatoria");
        }

        // 2. Validar que la fecha de devolución no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(devolucionBarrilFormDTO.getFechaDevolucion());

        // 3. Validar que se hayan informado las observaciones
        if (devolucionBarrilFormDTO.getObservaciones() == null || devolucionBarrilFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 4. Localizar el barril, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idBarril = devolucionBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        // 5. Validar que el barril se encuentre en estado operativo DESPACHADO (implica que existe
        //    un despacho REGISTRADO sobre él, ver nota de diseño en el javadoc de este método)
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("Solo se puede registrar una devolución sobre un barril en estado operativo DESPACHADO");
        }

        // 6. Validar que la fecha de devolución sea posterior a la última operación registrada
        //    sobre este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                devolucionBarrilFormDTO.getFechaDevolucion(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 7. Cambiar el estado operativo del barril a EN_LIMPIEZA y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_LIMPIEZA);
        barrilRepository.save(barrilEntity);

        // 8. Construir y persistir la devolución, y retornar el DTO de respuesta correspondiente
        DevolucionBarrilEntity devolucionBarrilEntity = DevolucionBarrilEntity.builder()
                .fecha(devolucionBarrilFormDTO.getFechaDevolucion())
                .observaciones(devolucionBarrilFormDTO.getObservaciones())
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperDevolucionBarril.toDTO(devolucionBarrilRepository.save(devolucionBarrilEntity));
    }

    /**
     * Anula una devolución de barril existente.
     * <p>
     * Restablece al barril afectado a estado operativo {@code DESPACHADO}: solo procede si ese
     * barril todavía se encuentra en {@code EN_LIMPIEZA}, para garantizar que la transición de
     * vuelta sea coherente.
     * </p>
     *
     * @param id El ID de la devolución de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La devolución de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la devolución no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si la devolución de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.DEVOLUCION_BARRIL)
    public DevolucionBarrilResponseDTO anularDevolucionBarril(Long id, AnulacionDevolucionBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la devolución de barril. Si no existe, se dispara RecursoNoEncontradoException
        DevolucionBarrilEntity devolucionBarrilEntity = devolucionBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la devolución de barril con ID: " + id));

        // 3. Validar que la devolución se encuentre en estado REGISTRADO
        if (devolucionBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular devoluciones de barril en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el barril, comparando
        //    contra las 5 tablas del ciclo de vida (evita anular un registro viejo cuando una
        //    operación posterior ya dejó al barril en un estado distinto)
        Long idBarril = devolucionBarrilEntity.getBarril().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                devolucionBarrilEntity.getFecha(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null));

        // 5. Localizar el barril asociado, bloqueado para escritura, y validar que se encuentre
        //    en estado operativo EN_LIMPIEZA, para garantizar que la transición de vuelta a
        //    DESPACHADO sea coherente
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(devolucionBarrilEntity.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + devolucionBarrilEntity.getBarril().getId()));
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_LIMPIEZA) {
            throw new ReglaNegocioException("Solo se puede anular una devolución cuyo barril asociado se encuentre en estado operativo EN_LIMPIEZA");
        }

        // 6. Restablecer el estado operativo del barril a DESPACHADO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DESPACHADO);
        barrilRepository.save(barrilEntity);

        // 7. Aplicar la anulación sobre la devolución y persistirla
        devolucionBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        devolucionBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        devolucionBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperDevolucionBarril.toDTO(devolucionBarrilRepository.save(devolucionBarrilEntity));
    }
}
