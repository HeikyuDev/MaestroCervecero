package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionLimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.LimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ILimpiezaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.LimpiezaBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperLimpiezaBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LimpiezaBarrilServicioImpl implements ILimpiezaBarrilServicio {

    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Filtra las limpiezas de barril, opcionalmente por estado, barril y/o rango de fecha de
     * limpieza.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una limpieza anulada sigue siendo
     * un registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaLimpiezaDesde Límite inferior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param fechaLimpiezaHasta Límite superior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LimpiezaBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LimpiezaBarrilResponseDTO> filtrarLimpiezasBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaLimpiezaDesde, LocalDateTime fechaLimpiezaHasta, Pageable pageable) {
        return limpiezaBarrilRepository.filtrarLimpiezasBarril(estado, idBarril, fechaLimpiezaDesde, fechaLimpiezaHasta, pageable)
                .map(MapperLimpiezaBarril::toDTO);
    }

    /**
     * Busca y retorna una limpieza de barril mediante su identificador único.
     *
     * @param id El ID de la limpieza de barril.
     * @return La limpieza de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna limpieza de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LimpiezaBarrilResponseDTO buscarPorId(Long id) {
        return MapperLimpiezaBarril.toDTO(limpiezaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la limpieza de barril con ID: " + id)));
    }

    /**
     * Registra una nueva limpieza de barril.
     * <p>
     * Evalúa la cantidad de limpiezas registradas desde el último mantenimiento del barril (o
     * desde siempre, si nunca tuvo uno), sumando esta limpieza, y la compara contra
     * {@code usosMaximosAntesMantenimiento}: si la alcanza o la supera, el barril pasa a
     * {@code EN_MANTENIMIENTO} en lugar de {@code DISPONIBLE}. Ese resultado queda grabado en la
     * propia limpieza ({@code estadoOperativoResultante}) para que una eventual anulación pueda
     * validar contra el estado exacto que esta limpieza puntual dejó.
     * </p>
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el barril, antes de leer su última operación,
     * su último envasado o sus usos. Es el único lock de la operación.
     * </p>
     *
     * @param limpiezaBarrilFormDTO Los datos de la limpieza a registrar.
     * @return La limpieza de barril registrada.
     * @throws ReglaNegocioException Si la fecha de limpieza no fue informada o es futura, si las observaciones no fueron informadas o superan el máximo, si el barril no se encuentra en estado operativo {@code EN_LIMPIEZA}, o si la fecha no es posterior a la última operación registrada ni al último envasado del barril.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LIMPIEZA_BARRIL)
    public LimpiezaBarrilResponseDTO registrarLimpiezaBarril(LimpiezaBarrilFormDTO limpiezaBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de limpieza
        if (limpiezaBarrilFormDTO.getFecha() == null) {
            throw new ReglaNegocioException("La fecha y hora de la operación es obligatoria", MetodosCicloVida.CAMPO_FECHA);
        }

        // 2. Validar que la fecha de limpieza no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(limpiezaBarrilFormDTO.getFecha(), MetodosCicloVida.CAMPO_FECHA);

        // 3. Validar las observaciones
        String observaciones = MetodosCicloVida.validarObservaciones(limpiezaBarrilFormDTO.getObservaciones());

        // 4. Localizar el barril, bloqueado para escritura, antes de leer cualquier otro dato
        Long idBarril = limpiezaBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 5. Validar que el barril se encuentre en estado operativo EN_LIMPIEZA
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_LIMPIEZA) {
            throw new ReglaNegocioException("Solo se puede registrar una limpieza sobre un barril en estado operativo En limpieza", MetodosCicloVida.CAMPO_BARRIL);
        }

        // 6. Validar que la fecha de limpieza sea posterior a la última operación registrada sobre
        //    este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                limpiezaBarrilFormDTO.getFecha(),
                "barril",
                MetodosCicloVida.CAMPO_FECHA,
                operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 7. Contar los usos del barril desde su último mantenimiento (o desde siempre, si nunca
        //    tuvo uno), sumando la limpieza que se está registrando, y comparar contra el máximo
        //    de usos antes de mantenimiento para decidir el estado operativo resultante
        LocalDateTime fechaUltimoMantenimiento = mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null);
        long cantidadUsos = limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(idBarril, fechaUltimoMantenimiento) + 1;
        EstadoOperativoBarril estadoOperativoResultante = cantidadUsos >= barrilEntity.getUsosMaximosAntesMantenimiento()
                ? EstadoOperativoBarril.EN_MANTENIMIENTO
                : EstadoOperativoBarril.DISPONIBLE;

        // 8. Aplicar el estado operativo resultante sobre el barril y persistirlo
        barrilEntity.setEstadoOperativo(estadoOperativoResultante);
        barrilRepository.save(barrilEntity);

        // 9. Construir y persistir la limpieza, y retornar el DTO de respuesta correspondiente
        LimpiezaBarrilEntity limpiezaBarrilEntity = LimpiezaBarrilEntity.builder()
                .fecha(limpiezaBarrilFormDTO.getFecha())
                .observaciones(observaciones)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .estadoOperativoResultante(estadoOperativoResultante)
                .build();

        return MapperLimpiezaBarril.toDTO(limpiezaBarrilRepository.save(limpiezaBarrilEntity));
    }

    /**
     * Anula una limpieza de barril existente.
     * <p>
     * Solo procede si es la última operación registrada del barril (la de mayor fecha, y a igual fecha la de mayor ID,
     * entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones), si el barril no está en uso por un lote,
     * si es posterior al último envasado del barril y si sigue exactamente en el estado
     * operativo que dejó esta limpieza puntual ({@code estadoOperativoResultante}), no en cualquiera de los dos
     * estados posibles en general: evita revertir una limpieza vieja cuando el estado actual del barril en realidad lo
     * dejó un mantenimiento u otra limpieza posterior. Restablece al barril a estado operativo {@code EN_LIMPIEZA}.
     * </p>
     * <p>
     * Orden de locks: igual que al registrar, se bloquea primero el barril (el ID se resuelve con una consulta escalar,
     * sin cargar la limpieza) y recién después se lee la limpieza y se verifica que sea la última. Es el único lock de
     * la operación.
     * </p>
     *
     * @param id El ID de la limpieza de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La limpieza de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si la limpieza no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del barril, si el barril está en uso por un lote, si el barril tuvo un envasado posterior, o si el barril asociado no se encuentra en el estado operativo que dejó esta limpieza.
     * @throws RecursoNoEncontradoException Si la limpieza de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.LIMPIEZA_BARRIL)
    public LimpiezaBarrilResponseDTO anularLimpiezaBarril(Long id, AnulacionLimpiezaBarrilFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVida.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el barril de la limpieza sin cargarla y bloquearlo para escritura. Si la limpieza no existe, se
        //    dispara RecursoNoEncontradoException
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la limpieza de barril con ID: " + id));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 3. Localizar la limpieza, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        LimpiezaBarrilEntity limpiezaBarrilEntity = limpiezaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la limpieza de barril con ID: " + id));

        // 4. Validar que la limpieza se encuentre en estado REGISTRADO
        if (limpiezaBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular limpiezas de barril en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVida.validarEsLaUltimaOperacion(
                limpiezaBarrilEntity.getId(),
                operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null));

        // 6. Validar que el barril no esté en uso por un lote
        MetodosCicloVida.validarNoEstaEnUso(barrilEntity.getEstadoOperativo());

        // 7. Validar que la limpieza sea posterior al último envasado del barril
        MetodosCicloVida.validarPosteriorAlUltimoEnvasado(
                limpiezaBarrilEntity.getFecha(),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Validar que el barril se encuentre exactamente en el estado operativo que dejó esta limpieza
        if (barrilEntity.getEstadoOperativo() != limpiezaBarrilEntity.getEstadoOperativoResultante()) {
            throw new ReglaNegocioException("Solo se puede anular una limpieza cuyo barril asociado se encuentre en estado operativo " + limpiezaBarrilEntity.getEstadoOperativoResultante().getEtiqueta());
        }

        // 9. Restablecer el estado operativo del barril a EN_LIMPIEZA y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_LIMPIEZA);
        barrilRepository.save(barrilEntity);

        // 10. Aplicar la anulación sobre la limpieza y persistirla
        limpiezaBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        limpiezaBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        limpiezaBarrilEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperLimpiezaBarril.toDTO(limpiezaBarrilRepository.save(limpiezaBarrilEntity));
    }
}
