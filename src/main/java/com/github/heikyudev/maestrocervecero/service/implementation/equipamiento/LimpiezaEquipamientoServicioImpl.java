package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionLimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.LimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.ILimpiezaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.LimpiezaEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperLimpiezaEquipamiento;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LimpiezaEquipamientoServicioImpl implements ILimpiezaEquipamientoServicio {

    private final ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    private final IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    private final IFallaEquipamientoRepository fallaEquipamientoRepository;
    private final IEquipamientoRepository equipamientoRepository;

    /**
     * Filtra las limpiezas de equipamiento, opcionalmente por equipamiento, tipo concreto de
     * equipamiento, estado y/o rango de fecha de limpieza.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una limpieza anulada sigue siendo
     * un registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param tipoEquipamiento El tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaLimpiezaDesde Límite inferior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param fechaLimpiezaHasta Límite superior (inclusive) del rango de fecha de limpieza, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link LimpiezaEquipamientoResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<LimpiezaEquipamientoResponseDTO> filtrarLimpiezasEquipamiento(Long idEquipamiento, TipoEquipamiento tipoEquipamiento, EstadoTransaccion estado, LocalDateTime fechaLimpiezaDesde, LocalDateTime fechaLimpiezaHasta, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipoEquipamiento != null ? tipoEquipamiento.getEntityClass() : null;
        return limpiezaEquipamientoRepository.filtrarLimpiezasEquipamiento(idEquipamiento, tipoClase, estado, fechaLimpiezaDesde, fechaLimpiezaHasta, pageable)
                .map(MapperLimpiezaEquipamiento::toDTO);
    }

    /**
     * Busca y retorna una limpieza de equipamiento mediante su identificador único.
     *
     * @param id El ID de la limpieza de equipamiento.
     * @return La limpieza de equipamiento correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna limpieza de equipamiento con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public LimpiezaEquipamientoResponseDTO buscarPorId(Long id) {
        return MapperLimpiezaEquipamiento.toDTO(limpiezaEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la limpieza de equipamiento con ID: " + id)));
    }

    /**
     * Registra una nueva limpieza de equipamiento.
     * <p>
     * Evalúa la cantidad de limpiezas registradas desde el último mantenimiento del equipamiento
     * (o desde siempre, si nunca tuvo uno), sumando esta limpieza, y la compara contra
     * {@code usosMaximosAntesMantenimiento}: si la alcanza o la supera, el equipamiento pasa a
     * {@code EN_MANTENIMIENTO} en lugar de {@code DISPONIBLE}. Ese resultado queda grabado en la
     * propia limpieza ({@code estadoOperativoResultante}) para que una eventual anulación pueda
     * validar contra el estado exacto que esta limpieza puntual dejó.
     * </p>
     *
     * @param limpiezaEquipamientoFormDTO Los datos de la limpieza a registrar.
     * @return La limpieza de equipamiento registrada.
     * @throws ReglaNegocioException Si la fecha de limpieza no fue informada, si las observaciones no fueron informadas, o si el equipamiento no se encuentra en estado operativo {@code EN_LIMPIEZA}.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.LIMPIEZA_EQUIPAMIENTO)
    public LimpiezaEquipamientoResponseDTO registrarLimpiezaEquipamiento(LimpiezaEquipamientoFormDTO limpiezaEquipamientoFormDTO) {
        // 1. Validar que se haya informado la fecha de limpieza
        if (limpiezaEquipamientoFormDTO.getFechaLimpieza() == null) {
            throw new ReglaNegocioException("La fecha de limpieza es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (limpiezaEquipamientoFormDTO.getObservaciones() == null || limpiezaEquipamientoFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Localizar el equipamiento, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idEquipamiento = limpiezaEquipamientoFormDTO.getIdEquipamiento();
        EquipamientoEntity equipamientoEntity = equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(idEquipamiento)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + idEquipamiento));

        // 4. Validar que el equipamiento se encuentre en estado operativo EN_LIMPIEZA
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.EN_LIMPIEZA) {
            throw new ReglaNegocioException("Solo se puede registrar una limpieza sobre un equipamiento en estado operativo EN_LIMPIEZA");
        }

        // 5. Contar los usos del equipamiento desde su último mantenimiento (o desde siempre, si
        //    nunca tuvo uno), sumando la limpieza que se está registrando, y comparar contra el
        //    máximo de usos antes de mantenimiento para decidir el estado operativo resultante
        LocalDateTime fechaUltimoMantenimiento = mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(idEquipamiento).orElse(null);
        long cantidadUsos = limpiezaEquipamientoRepository.contarLimpiezasRegistradasDesde(idEquipamiento, fechaUltimoMantenimiento) + 1;
        EstadoOperativo estadoOperativoResultante = cantidadUsos >= equipamientoEntity.getUsosMaximosAntesMantenimiento()
                ? EstadoOperativo.EN_MANTENIMIENTO
                : EstadoOperativo.DISPONIBLE;

        // 6. Aplicar el estado operativo resultante sobre el equipamiento y persistirlo
        equipamientoEntity.setEstadoOperativo(estadoOperativoResultante);
        equipamientoRepository.save(equipamientoEntity);

        // 7. Construir y persistir la limpieza, y retornar el DTO de respuesta correspondiente
        LimpiezaEquipamientoEntity limpiezaEquipamientoEntity = LimpiezaEquipamientoEntity.builder()
                .fecha(limpiezaEquipamientoFormDTO.getFechaLimpieza())
                .observaciones(limpiezaEquipamientoFormDTO.getObservaciones())
                .equipamiento(equipamientoEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .estadoOperativoResultante(estadoOperativoResultante)
                .build();

        return MapperLimpiezaEquipamiento.toDTO(limpiezaEquipamientoRepository.save(limpiezaEquipamientoEntity));
    }

    /**
     * Anula una limpieza de equipamiento existente.
     * <p>
     * Solo procede si el equipamiento asociado sigue exactamente en el estado operativo que dejó
     * esta limpieza puntual ({@code estadoOperativoResultante}), no en cualquiera de los dos
     * estados posibles en general: evita revertir una limpieza vieja cuando el estado actual del
     * equipamiento en realidad lo dejó un mantenimiento u otra limpieza posterior. Restablece al
     * equipamiento a estado operativo {@code EN_LIMPIEZA}.
     * </p>
     *
     * @param id El ID de la limpieza de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La limpieza de equipamiento anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la limpieza no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el equipamiento, o si el equipamiento asociado no se encuentra en el estado operativo que dejó esta limpieza.
     * @throws RecursoNoEncontradoException Si la limpieza de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.LIMPIEZA_EQUIPAMIENTO)
    public LimpiezaEquipamientoResponseDTO anularLimpiezaEquipamiento(Long id, AnulacionLimpiezaEquipamientoFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la limpieza de equipamiento. Si no existe, se dispara RecursoNoEncontradoException
        LimpiezaEquipamientoEntity limpiezaEquipamientoEntity = limpiezaEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la limpieza de equipamiento con ID: " + id));

        // 3. Validar que la limpieza se encuentre en estado REGISTRADO
        if (limpiezaEquipamientoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular limpiezas de equipamiento en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el equipamiento,
        //    comparando contra las 3 tablas del ciclo de vida (evita anular un registro viejo
        //    cuando una operación posterior ya dejó al equipamiento en un estado distinto)
        Long idEquipamiento = limpiezaEquipamientoEntity.getEquipamiento().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                limpiezaEquipamientoEntity.getFecha(),
                fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(idEquipamiento).orElse(null),
                mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(idEquipamiento).orElse(null),
                limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(idEquipamiento).orElse(null),
                "equipamiento");

        // 5. Localizar el equipamiento asociado, bloqueado para escritura, y validar que se
        //    encuentre exactamente en el estado operativo que dejó esta limpieza puntual
        EquipamientoEntity equipamientoEntity = equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(limpiezaEquipamientoEntity.getEquipamiento().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + limpiezaEquipamientoEntity.getEquipamiento().getId()));
        if (equipamientoEntity.getEstadoOperativo() != limpiezaEquipamientoEntity.getEstadoOperativoResultante()) {
            throw new ReglaNegocioException("Solo se puede anular una limpieza cuyo equipamiento asociado se encuentre en estado operativo " + limpiezaEquipamientoEntity.getEstadoOperativoResultante());
        }

        // 6. Restablecer el estado operativo del equipamiento a EN_LIMPIEZA y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.EN_LIMPIEZA);
        equipamientoRepository.save(equipamientoEntity);

        // 7. Aplicar la anulación sobre la limpieza y persistirla
        limpiezaEquipamientoEntity.setEstado(EstadoTransaccion.ANULADO);
        limpiezaEquipamientoEntity.setFechaAnulacion(LocalDateTime.now());
        limpiezaEquipamientoEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperLimpiezaEquipamiento.toDTO(limpiezaEquipamientoRepository.save(limpiezaEquipamientoEntity));
    }
}
