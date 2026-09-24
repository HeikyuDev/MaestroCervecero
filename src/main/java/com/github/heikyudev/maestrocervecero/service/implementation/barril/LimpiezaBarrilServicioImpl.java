package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
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
    private final IFallaBarrilRepository fallaBarrilRepository;
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
     *
     * @param limpiezaBarrilFormDTO Los datos de la limpieza a registrar.
     * @return La limpieza de barril registrada.
     * @throws ReglaNegocioException Si la fecha de limpieza no fue informada, si las observaciones no fueron informadas, o si el barril no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LIMPIEZA_BARRIL)
    public LimpiezaBarrilResponseDTO registrarLimpiezaBarril(LimpiezaBarrilFormDTO limpiezaBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de limpieza
        if (limpiezaBarrilFormDTO.getFechaLimpieza() == null) {
            throw new ReglaNegocioException("La fecha de limpieza es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (limpiezaBarrilFormDTO.getObservaciones() == null || limpiezaBarrilFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Localizar el barril, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idBarril = limpiezaBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        // 4. Validar que el barril se encuentre en estado operativo EN_LIMPIEZA
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_LIMPIEZA) {
            throw new ReglaNegocioException("Solo se puede registrar una limpieza sobre un barril en estado operativo EN_LIMPIEZA");
        }

        // 5. Contar los usos del barril desde su último mantenimiento (o desde siempre, si nunca
        //    tuvo uno), sumando la limpieza que se está registrando, y comparar contra el máximo
        //    de usos antes de mantenimiento para decidir el estado operativo resultante
        LocalDateTime fechaUltimoMantenimiento = mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null);
        long cantidadUsos = limpiezaBarrilRepository.contarLimpiezasRegistradasDesde(idBarril, fechaUltimoMantenimiento) + 1;
        EstadoOperativoBarril estadoOperativoResultante = cantidadUsos >= barrilEntity.getUsosMaximosAntesMantenimiento()
                ? EstadoOperativoBarril.EN_MANTENIMIENTO
                : EstadoOperativoBarril.DISPONIBLE;

        // 6. Aplicar el estado operativo resultante sobre el barril y persistirlo
        barrilEntity.setEstadoOperativo(estadoOperativoResultante);
        barrilRepository.save(barrilEntity);

        // 7. Construir y persistir la limpieza, y retornar el DTO de respuesta correspondiente
        LimpiezaBarrilEntity limpiezaBarrilEntity = LimpiezaBarrilEntity.builder()
                .fecha(limpiezaBarrilFormDTO.getFechaLimpieza())
                .observaciones(limpiezaBarrilFormDTO.getObservaciones())
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .estadoOperativoResultante(estadoOperativoResultante)
                .build();

        return MapperLimpiezaBarril.toDTO(limpiezaBarrilRepository.save(limpiezaBarrilEntity));
    }

    /**
     * Anula una limpieza de barril existente.
     * <p>
     * Solo procede si el barril asociado sigue exactamente en el estado operativo que dejó esta
     * limpieza puntual ({@code estadoOperativoResultante}), no en cualquiera de los dos estados
     * posibles en general: evita revertir una limpieza vieja cuando el estado actual del barril
     * en realidad lo dejó un mantenimiento u otra limpieza posterior. Restablece al barril a
     * estado operativo {@code EN_LIMPIEZA}.
     * </p>
     *
     * @param id El ID de la limpieza de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La limpieza de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la limpieza no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en el estado operativo que dejó esta limpieza.
     * @throws RecursoNoEncontradoException Si la limpieza de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.LIMPIEZA_BARRIL)
    public LimpiezaBarrilResponseDTO anularLimpiezaBarril(Long id, AnulacionLimpiezaBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la limpieza de barril. Si no existe, se dispara RecursoNoEncontradoException
        LimpiezaBarrilEntity limpiezaBarrilEntity = limpiezaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la limpieza de barril con ID: " + id));

        // 3. Validar que la limpieza se encuentre en estado REGISTRADO
        if (limpiezaBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular limpiezas de barril en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el barril, comparando
        //    contra las 3 tablas del ciclo de vida (evita anular un registro viejo cuando una
        //    operación posterior ya dejó al barril en un estado distinto)
        Long idBarril = limpiezaBarrilEntity.getBarril().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                limpiezaBarrilEntity.getFecha(),
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                "barril");

        // 5. Localizar el barril asociado, bloqueado para escritura, y validar que se encuentre
        //    exactamente en el estado operativo que dejó esta limpieza puntual
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(limpiezaBarrilEntity.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + limpiezaBarrilEntity.getBarril().getId()));
        if (barrilEntity.getEstadoOperativo() != limpiezaBarrilEntity.getEstadoOperativoResultante()) {
            throw new ReglaNegocioException("Solo se puede anular una limpieza cuyo barril asociado se encuentre en estado operativo " + limpiezaBarrilEntity.getEstadoOperativoResultante());
        }

        // 6. Restablecer el estado operativo del barril a EN_LIMPIEZA y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_LIMPIEZA);
        barrilRepository.save(barrilEntity);

        // 7. Aplicar la anulación sobre la limpieza y persistirla
        limpiezaBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        limpiezaBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        limpiezaBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperLimpiezaBarril.toDTO(limpiezaBarrilRepository.save(limpiezaBarrilEntity));
    }
}
