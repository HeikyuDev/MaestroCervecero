package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionMantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMantenimientoEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MantenimientoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMantenimientoEquipamiento;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MantenimientoEquipamientoServicioImpl implements IMantenimientoEquipamientoServicio {

    private final IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    private final IFallaEquipamientoRepository fallaEquipamientoRepository;
    private final ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    private final IEquipamientoRepository equipamientoRepository;

    /**
     * Filtra los mantenimientos de equipamiento, opcionalmente por equipamiento, tipo concreto de
     * equipamiento, estado y/o rango de fecha de mantenimiento.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un mantenimiento anulado sigue
     * siendo un registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param tipoEquipamiento El tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMantenimientoDesde Límite inferior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param fechaMantenimientoHasta Límite superior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MantenimientoEquipamientoResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MantenimientoEquipamientoResponseDTO> filtrarMantenimientosEquipamiento(Long idEquipamiento, TipoEquipamiento tipoEquipamiento, EstadoTransaccion estado, LocalDateTime fechaMantenimientoDesde, LocalDateTime fechaMantenimientoHasta, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipoEquipamiento != null ? tipoEquipamiento.getEntityClass() : null;
        return mantenimientoEquipamientoRepository.filtrarMantenimientosEquipamiento(idEquipamiento, tipoClase, estado, fechaMantenimientoDesde, fechaMantenimientoHasta, pageable)
                .map(MapperMantenimientoEquipamiento::toDTO);
    }

    /**
     * Busca y retorna un mantenimiento de equipamiento mediante su identificador único.
     *
     * @param id El ID del mantenimiento de equipamiento.
     * @return El mantenimiento de equipamiento correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún mantenimiento de equipamiento con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MantenimientoEquipamientoResponseDTO buscarPorId(Long id) {
        return MapperMantenimientoEquipamiento.toDTO(mantenimientoEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de equipamiento con ID: " + id)));
    }

    /**
     * Registra un nuevo mantenimiento de equipamiento.
     * <p>
     * Deja al equipamiento afectado en estado operativo {@code DISPONIBLE}: el mantenimiento
     * cierra el ciclo abierto por una falla o por un mantenimiento preventivo, devolviendo el
     * equipamiento a producción.
     * </p>
     *
     * @param mantenimientoEquipamientoFormDTO Los datos del mantenimiento a registrar.
     * @return El mantenimiento de equipamiento registrado.
     * @throws ReglaNegocioException Si la fecha de mantenimiento no fue informada, si las observaciones no fueron informadas, o si el equipamiento no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_EQUIPAMIENTO)
    public MantenimientoEquipamientoResponseDTO registrarMantenimientoEquipamiento(MantenimientoEquipamientoFormDTO mantenimientoEquipamientoFormDTO) {
        // 1. Validar que se haya informado la fecha de mantenimiento
        if (mantenimientoEquipamientoFormDTO.getFechaMantenimiento() == null) {
            throw new ReglaNegocioException("La fecha de mantenimiento es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (mantenimientoEquipamientoFormDTO.getObservaciones() == null || mantenimientoEquipamientoFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Localizar el equipamiento, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        EquipamientoEntity equipamientoEntity = equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(mantenimientoEquipamientoFormDTO.getIdEquipamiento())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + mantenimientoEquipamientoFormDTO.getIdEquipamiento()));

        // 4. Validar que el equipamiento se encuentre en estado operativo EN_MANTENIMIENTO
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede registrar un mantenimiento sobre un equipamiento en estado operativo EN_MANTENIMIENTO");
        }

        // 5. Cambiar el estado operativo del equipamiento a DISPONIBLE y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.DISPONIBLE);
        equipamientoRepository.save(equipamientoEntity);

        // 6. Construir y persistir el mantenimiento, y retornar el DTO de respuesta correspondiente
        MantenimientoEquipamientoEntity mantenimientoEquipamientoEntity = MantenimientoEquipamientoEntity.builder()
                .fecha(mantenimientoEquipamientoFormDTO.getFechaMantenimiento())
                .observaciones(mantenimientoEquipamientoFormDTO.getObservaciones())
                .equipamiento(equipamientoEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperMantenimientoEquipamiento.toDTO(mantenimientoEquipamientoRepository.save(mantenimientoEquipamientoEntity));
    }

    /**
     * Anula un mantenimiento de equipamiento existente.
     * <p>
     * Restablece al equipamiento afectado a estado operativo {@code EN_MANTENIMIENTO}: solo
     * procede si ese equipamiento todavía se encuentra en {@code DISPONIBLE}, para garantizar que
     * la transición de vuelta sea coherente.
     * </p>
     *
     * @param id El ID del mantenimiento de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El mantenimiento de equipamiento anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el mantenimiento no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el equipamiento, o si el equipamiento asociado no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el mantenimiento de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_EQUIPAMIENTO)
    public MantenimientoEquipamientoResponseDTO anularMantenimientoEquipamiento(Long id, AnulacionMantenimientoEquipamientoFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar el mantenimiento de equipamiento. Si no existe, se dispara RecursoNoEncontradoException
        MantenimientoEquipamientoEntity mantenimientoEquipamientoEntity = mantenimientoEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de equipamiento con ID: " + id));

        // 3. Validar que el mantenimiento se encuentre en estado REGISTRADO
        if (mantenimientoEquipamientoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular mantenimientos de equipamiento en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el equipamiento,
        //    comparando contra las 3 tablas del ciclo de vida (evita anular un registro viejo
        //    cuando una operación posterior ya dejó al equipamiento en un estado distinto)
        Long idEquipamiento = mantenimientoEquipamientoEntity.getEquipamiento().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                mantenimientoEquipamientoEntity.getFecha(),
                fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(idEquipamiento).orElse(null),
                mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(idEquipamiento).orElse(null),
                limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(idEquipamiento).orElse(null),
                "equipamiento");

        // 5. Localizar el equipamiento asociado, bloqueado para escritura, y validar que se
        //    encuentre en estado operativo DISPONIBLE, para garantizar que la transición de
        //    vuelta a EN_MANTENIMIENTO sea coherente
        EquipamientoEntity equipamientoEntity = equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(mantenimientoEquipamientoEntity.getEquipamiento().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + mantenimientoEquipamientoEntity.getEquipamiento().getId()));
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede anular un mantenimiento cuyo equipamiento asociado se encuentre en estado operativo DISPONIBLE");
        }

        // 6. Restablecer el estado operativo del equipamiento a EN_MANTENIMIENTO y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.EN_MANTENIMIENTO);
        equipamientoRepository.save(equipamientoEntity);

        // 7. Aplicar la anulación sobre el mantenimiento y persistirlo
        mantenimientoEquipamientoEntity.setEstado(EstadoTransaccion.ANULADO);
        mantenimientoEquipamientoEntity.setFechaAnulacion(LocalDateTime.now());
        mantenimientoEquipamientoEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperMantenimientoEquipamiento.toDTO(mantenimientoEquipamientoRepository.save(mantenimientoEquipamientoEntity));
    }
}
