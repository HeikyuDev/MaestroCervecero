package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.ILimpiezaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionFallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFallaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FallaEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperFallaEquipamiento;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FallaEquipamientoServicioImpl implements IFallaEquipamientoServicio {

    private final IFallaEquipamientoRepository fallaEquipamientoRepository;
    private final IMantenimientoEquipamientoRepository mantenimientoEquipamientoRepository;
    private final ILimpiezaEquipamientoRepository limpiezaEquipamientoRepository;
    private final IEquipamientoRepository equipamientoRepository;

    /**
     * Filtra las fallas de equipamiento, opcionalmente por estado, rango de fecha de falla y/o
     * tipo concreto de equipamiento.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una falla anulada sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param idEquipamiento El ID del equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaFallaDesde Límite inferior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param fechaFallaHasta Límite superior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param tipoEquipamiento El tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FallaEquipamientoResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FallaEquipamientoResponseDTO> filtrarFallasEquipamiento(Long idEquipamiento, EstadoTransaccion estado, LocalDateTime fechaFallaDesde, LocalDateTime fechaFallaHasta, TipoEquipamiento tipoEquipamiento, Pageable pageable) {
        Class<? extends EquipamientoEntity> tipoClase = tipoEquipamiento != null ? tipoEquipamiento.getEntityClass() : null;
        return fallaEquipamientoRepository.filtrarFallasEquipamiento(idEquipamiento, estado, fechaFallaDesde, fechaFallaHasta, tipoClase, pageable)
                .map(MapperFallaEquipamiento::toDTO);
    }

    /**
     * Busca y retorna una falla de equipamiento mediante su identificador único.
     *
     * @param id El ID de la falla de equipamiento.
     * @return La falla de equipamiento correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna falla de equipamiento con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FallaEquipamientoResponseDTO buscarPorId(Long id) {
        return MapperFallaEquipamiento.toDTO(fallaEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de equipamiento con ID: " + id)));
    }

    /**
     * Registra una nueva falla de equipamiento.
     * <p>
     * Deja al equipamiento afectado en estado operativo {@code EN_MANTENIMIENTO}: mientras la
     * falla siga registrada, el equipamiento no puede volver a asignarse a una operación de
     * producción.
     * </p>
     *
     * @param fallaEquipamientoFormDTO Los datos de la falla a registrar.
     * @return La falla de equipamiento registrada.
     * @throws ReglaNegocioException Si la fecha de falla no fue informada, si las observaciones no fueron informadas, o si el equipamiento no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FALLA_EQUIPAMIENTO)
    public FallaEquipamientoResponseDTO registrarFallaEquipamiento(FallaEquipamientoFormDTO fallaEquipamientoFormDTO) {
        // 1. Validar que se haya informado la fecha de falla
        if (fallaEquipamientoFormDTO.getFechaFalla() == null) {
            throw new ReglaNegocioException("La fecha de falla es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (fallaEquipamientoFormDTO.getObservaciones() == null || fallaEquipamientoFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Localizar el equipamiento, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        EquipamientoEntity equipamientoEntity = equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(fallaEquipamientoFormDTO.getIdEquipamiento())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + fallaEquipamientoFormDTO.getIdEquipamiento()));

        // 4. Validar que el equipamiento se encuentre en estado operativo DISPONIBLE
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede registrar una falla sobre un equipamiento en estado operativo DISPONIBLE");
        }

        // 5. Cambiar el estado operativo del equipamiento a EN_MANTENIMIENTO y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.EN_MANTENIMIENTO);
        equipamientoRepository.save(equipamientoEntity);

        // 6. Construir y persistir la falla, y retornar el DTO de respuesta correspondiente
        FallaEquipamientoEntity fallaEquipamientoEntity = FallaEquipamientoEntity.builder()
                .fecha(fallaEquipamientoFormDTO.getFechaFalla())
                .observaciones(fallaEquipamientoFormDTO.getObservaciones())
                .equipamiento(equipamientoEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperFallaEquipamiento.toDTO(fallaEquipamientoRepository.save(fallaEquipamientoEntity));
    }

    /**
     * Anula una falla de equipamiento existente.
     * <p>
     * Restablece al equipamiento afectado a estado operativo {@code DISPONIBLE}: solo procede si
     * ese equipamiento todavía se encuentra en {@code EN_MANTENIMIENTO}, para garantizar que la
     * transición de vuelta sea coherente.
     * </p>
     *
     * @param id El ID de la falla de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La falla de equipamiento anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la falla no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el equipamiento, o si el equipamiento asociado no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si la falla de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.FALLA_EQUIPAMIENTO)
    public FallaEquipamientoResponseDTO anularFallaEquipamiento(Long id, AnulacionFallaEquipamientoFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la falla de equipamiento. Si no existe, se dispara RecursoNoEncontradoException
        FallaEquipamientoEntity fallaEquipamientoEntity = fallaEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de equipamiento con ID: " + id));

        // 3. Validar que la falla se encuentre en estado REGISTRADO
        if (fallaEquipamientoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular fallas de equipamiento en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el equipamiento,
        //    comparando contra las 3 tablas del ciclo de vida (evita anular un registro viejo
        //    cuando una operación posterior ya dejó al equipamiento en un estado distinto)
        Long idEquipamiento = fallaEquipamientoEntity.getEquipamiento().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                fallaEquipamientoEntity.getFecha(),
                "equipamiento",
                fallaEquipamientoRepository.buscarFechaUltimaFallaRegistrada(idEquipamiento).orElse(null),
                mantenimientoEquipamientoRepository.buscarFechaUltimoMantenimientoRegistrado(idEquipamiento).orElse(null),
                limpiezaEquipamientoRepository.buscarFechaUltimaLimpiezaRegistrada(idEquipamiento).orElse(null));

        // 5. Localizar el equipamiento asociado, bloqueado para escritura, y validar que se
        //    encuentre en estado operativo EN_MANTENIMIENTO, para garantizar que la transición
        //    de vuelta a DISPONIBLE sea coherente
        EquipamientoEntity equipamientoEntity = equipamientoRepository.buscarPorIdParaCambiarEstadoOperativo(fallaEquipamientoEntity.getEquipamiento().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + fallaEquipamientoEntity.getEquipamiento().getId()));
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede anular una falla cuyo equipamiento asociado se encuentre en estado operativo EN_MANTENIMIENTO");
        }

        // 6. Restablecer el estado operativo del equipamiento a DISPONIBLE y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.DISPONIBLE);
        equipamientoRepository.save(equipamientoEntity);

        // 7. Aplicar la anulación sobre la falla y persistirla
        fallaEquipamientoEntity.setEstado(EstadoTransaccion.ANULADO);
        fallaEquipamientoEntity.setFechaAnulacion(LocalDateTime.now());
        fallaEquipamientoEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperFallaEquipamiento.toDTO(fallaEquipamientoRepository.save(fallaEquipamientoEntity));
    }
}
