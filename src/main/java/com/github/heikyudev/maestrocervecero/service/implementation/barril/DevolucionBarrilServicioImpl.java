package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
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
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
     * afuera pasa por limpieza, por motivos sanitarios. Su contenido se descarta (el cliente pudo
     * manipularlo): el barril queda con contenido 0 y los litros descartados se guardan en la
     * devolución para poder restituirlos al anularla.
     * </p>
     * <p>
     * {@code DevolucionBarrilEntity} no tiene una relación directa con el despacho al que
     * corresponde (ver la discusión de 3FN que llevó a este diseño): la regla "el despacho debe
     * existir y estar registrado" queda satisfecha estructuralmente por exigir que el barril se
     * encuentre en estado operativo {@code DESPACHADO}, ya que solo un despacho REGISTRADO puede
     * haber dejado al barril en ese estado.
     * </p>
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el barril, antes de leer su última operación y
     * su último envasado. Es el único lock de la operación.
     * </p>
     *
     * @param devolucionBarrilFormDTO Los datos de la devolución a registrar.
     * @return La devolución de barril registrada.
     * @throws ReglaNegocioException Si la fecha de devolución o las observaciones no fueron informadas, si la fecha de devolución es posterior a la fecha y hora actual, si las observaciones superan el máximo, si el barril no se encuentra en estado operativo {@code DESPACHADO}, o si la fecha no es posterior a la última operación registrada ni al último envasado del barril.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.DEVOLUCION_BARRIL)
    public DevolucionBarrilResponseDTO registrarDevolucionBarril(DevolucionBarrilFormDTO devolucionBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de devolución
        if (devolucionBarrilFormDTO.getFecha() == null) {
            throw new ReglaNegocioException("La fecha de devolución es obligatoria", MetodosCicloVida.CAMPO_FECHA);
        }

        // 2. Validar que la fecha de devolución no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(devolucionBarrilFormDTO.getFecha(), MetodosCicloVida.CAMPO_FECHA);

        // 3. Validar las observaciones
        String observaciones = MetodosCicloVida.validarObservaciones(devolucionBarrilFormDTO.getObservaciones());

        // 4. Localizar el barril, bloqueado para escritura, antes de leer cualquier otro dato
        Long idBarril = devolucionBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 5. Validar que el barril se encuentre en estado operativo DESPACHADO (implica que existe
        //    un despacho REGISTRADO sobre él, ver nota de diseño en el javadoc de este método)
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("Solo se puede registrar una devolución sobre un barril en estado operativo Despachado", MetodosCicloVida.CAMPO_BARRIL);
        }

        // 6. Validar que la fecha de devolución sea posterior a la última operación registrada
        //    sobre este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                devolucionBarrilFormDTO.getFecha(),
                "barril",
                MetodosCicloVida.CAMPO_FECHA,
                operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 7. Descartar el contenido del barril y cambiar su estado operativo a EN_LIMPIEZA
        double litrosDescartados = barrilEntity.getContenidoActual();
        barrilEntity.setContenidoActual(0.0);
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_LIMPIEZA);
        barrilRepository.save(barrilEntity);

        // 8. Construir y persistir la devolución, y retornar el DTO de respuesta correspondiente
        DevolucionBarrilEntity devolucionBarrilEntity = DevolucionBarrilEntity.builder()
                .fecha(devolucionBarrilFormDTO.getFecha())
                .observaciones(observaciones)
                .litrosDescartados(litrosDescartados)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperDevolucionBarril.toDTO(devolucionBarrilRepository.save(devolucionBarrilEntity));
    }

    /**
     * Anula una devolución de barril existente.
     * <p>
     * Solo procede si es la última operación registrada del barril (la de mayor fecha, y a igual fecha la de mayor ID,
     * entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones), si el barril no está en uso por un lote y
     * si es posterior al último envasado del barril. Restablece al barril afectado a estado
     * operativo {@code DESPACHADO} y le restituye el contenido descartado por la devolución: solo procede si ese
     * barril todavía se encuentra en {@code EN_LIMPIEZA}, para garantizar que la transición de vuelta sea coherente.
     * </p>
     * <p>
     * Orden de locks: se bloquea primero el barril (el ID se resuelve con una consulta escalar, sin cargar la
     * devolución) y recién después se lee la devolución y se verifica que sea la última. Es el único lock de la
     * operación.
     * </p>
     *
     * @param id El ID de la devolución de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La devolución de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si la devolución no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del barril, si el barril está en uso por un lote, si el barril tuvo un envasado posterior, o si el barril asociado no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si la devolución de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.DEVOLUCION_BARRIL)
    public DevolucionBarrilResponseDTO anularDevolucionBarril(Long id, AnulacionDevolucionBarrilFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVida.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el barril de la devolución sin cargarla y bloquearlo para escritura. Si la devolución no
        //    existe, se dispara RecursoNoEncontradoException
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la devolución de barril con ID: " + id));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 3. Localizar la devolución, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        DevolucionBarrilEntity devolucionBarrilEntity = devolucionBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la devolución de barril con ID: " + id));

        // 4. Validar que la devolución se encuentre en estado REGISTRADO
        if (devolucionBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular devoluciones de barril en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVida.validarEsLaUltimaOperacion(
                devolucionBarrilEntity.getId(),
                operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null));

        // 6. Validar que el barril no esté en uso por un lote
        MetodosCicloVida.validarNoEstaEnUso(barrilEntity.getEstadoOperativo());

        // 7. Validar que la devolución sea posterior al último envasado del barril
        MetodosCicloVida.validarPosteriorAlUltimoEnvasado(
                devolucionBarrilEntity.getFecha(),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Validar que el barril se encuentre en estado operativo EN_LIMPIEZA, para garantizar que la transición de
        //    vuelta a DESPACHADO sea coherente
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_LIMPIEZA) {
            throw new ReglaNegocioException("Solo se puede anular una devolución cuyo barril asociado se encuentre en estado operativo En limpieza");
        }

        // 9. Restablecer el estado operativo del barril a DESPACHADO, restituyendo el contenido descartado (se suman al contenido actual del barril)
        barrilEntity.setContenidoActual(barrilEntity.getContenidoActual() + devolucionBarrilEntity.getLitrosDescartados());
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DESPACHADO);
        barrilRepository.save(barrilEntity);

        // 10. Aplicar la anulación sobre la devolución y persistirla
        devolucionBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        devolucionBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        devolucionBarrilEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperDevolucionBarril.toDTO(devolucionBarrilRepository.save(devolucionBarrilEntity));
    }
}
