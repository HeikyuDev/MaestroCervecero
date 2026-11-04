package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MantenimientoEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMantenimientoEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionMantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMantenimientoEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MantenimientoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMantenimientoEquipamiento;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosCicloVidaEquipamiento;
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
    private final IEquipamientoRepository equipamientoRepository;
    private final IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

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
     * El mantenimiento tiene una única fecha y hora y deja al equipamiento en estado
     * operativo {@code DISPONIBLE}. Solo se registra sobre un equipamiento ya en mantenimiento, ya sea por una
     * falla o por haber alcanzado el máximo de usos: no existe el mantenimiento voluntario sobre un equipamiento
     * disponible.
     * </p>
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el equipamiento, antes de leer su última
     * operación o su última participación en un lote. Es el único lock de la operación.
     * </p>
     *
     * @param mantenimientoEquipamientoFormDTO Los datos del mantenimiento a registrar.
     * @return El mantenimiento de equipamiento registrado.
     * @throws ReglaNegocioException Si la fecha no fue informada o es futura, si las observaciones no fueron informadas o superan el máximo, si el equipamiento está dado de baja o no se encuentra en estado operativo {@code EN_MANTENIMIENTO}, o si la fecha no es posterior a la última operación registrada ni al último uso del equipamiento en un lote.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_EQUIPAMIENTO)
    public MantenimientoEquipamientoResponseDTO registrarMantenimientoEquipamiento(MantenimientoEquipamientoFormDTO mantenimientoEquipamientoFormDTO) {
        // 1. Validar la fecha de la operación
        MetodosCicloVidaEquipamiento.validarFecha(mantenimientoEquipamientoFormDTO.getFecha());

        // 2. Validar las observaciones
        String observaciones = MetodosCicloVidaEquipamiento.validarObservaciones(mantenimientoEquipamientoFormDTO.getObservaciones());

        // 3. Localizar el equipamiento, bloqueado para escritura, antes de leer cualquier otro dato
        Long idEquipamiento = mantenimientoEquipamientoFormDTO.getIdEquipamiento();
        EquipamientoEntity equipamientoEntity = MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(
                equipamientoRepository.buscarPorIdParaOperacionCicloVida(idEquipamiento), idEquipamiento);

        // 4. Validar que el equipamiento se encuentre en estado operativo EN_MANTENIMIENTO
        MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(equipamientoEntity.getEstadoOperativo(), EstadoOperativo.EN_MANTENIMIENTO, "un mantenimiento");

        // 5. Validar que la fecha sea posterior a la de la última operación registrada y al fin de la última
        //    participación del equipamiento en un lote
        MetodosCicloVidaEquipamiento.validarFechaPosteriorALaActividadAnterior(
                mantenimientoEquipamientoFormDTO.getFecha(),
                operacionCicloVidaEquipamientoRepository.buscarFechaUltimaOperacionRegistrada(idEquipamiento).orElse(null),
                etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(idEquipamiento).orElse(null));

        // 6. Cambiar el estado operativo del equipamiento a DISPONIBLE y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.DISPONIBLE);
        equipamientoRepository.save(equipamientoEntity);

        // 7. Construir y persistir el mantenimiento, y retornar el DTO de respuesta correspondiente
        MantenimientoEquipamientoEntity mantenimientoEquipamientoEntity = MantenimientoEquipamientoEntity.builder()
                .fecha(mantenimientoEquipamientoFormDTO.getFecha())
                .observaciones(observaciones)
                .equipamiento(equipamientoEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperMantenimientoEquipamiento.toDTO(mantenimientoEquipamientoRepository.save(mantenimientoEquipamientoEntity));
    }

    /**
     * Anula un mantenimiento de equipamiento existente.
     * <p>
     * Solo procede si es la última operación registrada del equipamiento (la de mayor fecha, y a igual fecha la de mayor ID, entre sus
     * limpiezas, fallas y mantenimientos), si el mantenimiento es posterior al fin de la última participación del
     * equipamiento en un lote y si el equipamiento asociado se encuentra en estado operativo
     * {@code DISPONIBLE}; el equipamiento vuelve a {@code EN_MANTENIMIENTO}.
     * </p>
     * <p>
     * Orden de locks: igual que al registrar, se bloquea primero el equipamiento (el ID se resuelve con una consulta
     * escalar, sin cargar el mantenimiento) y recién después se lee el mantenimiento y se verifica que sea el último.
     * Es el único lock de la operación.
     * </p>
     *
     * @param id El ID del mantenimiento de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El mantenimiento de equipamiento anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si el equipamiento está dado de baja, si el mantenimiento no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del equipamiento, si el equipamiento participó en un lote posterior al mantenimiento, o si el equipamiento asociado no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el mantenimiento de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_EQUIPAMIENTO)
    public MantenimientoEquipamientoResponseDTO anularMantenimientoEquipamiento(Long id, AnulacionMantenimientoEquipamientoFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVidaEquipamiento.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el equipamiento del mantenimiento sin cargarlo y bloquearlo para escritura. Si el
        //    mantenimiento no existe, se dispara RecursoNoEncontradoException
        Long idEquipamiento = operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de equipamiento con ID: " + id));
        EquipamientoEntity equipamientoEntity = MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(
                equipamientoRepository.buscarPorIdParaOperacionCicloVida(idEquipamiento), idEquipamiento);

        // 3. Localizar el mantenimiento, ya con el equipamiento bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        MantenimientoEquipamientoEntity mantenimientoEquipamientoEntity = mantenimientoEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de equipamiento con ID: " + id));

        // 4. Validar que el mantenimiento se encuentre en estado REGISTRADO
        if (mantenimientoEquipamientoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular mantenimientos de equipamiento en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del equipamiento (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVidaEquipamiento.validarEsLaUltimaOperacion(
                mantenimientoEquipamientoEntity.getId(),
                operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(idEquipamiento).orElse(null));

        // 6. Validar que el mantenimiento sea posterior al fin de la última participación del equipamiento en un lote
        MetodosCicloVidaEquipamiento.validarPosteriorAlUltimoLote(
                mantenimientoEquipamientoEntity.getFecha(),
                etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(idEquipamiento).orElse(null));

        // 7. Validar que el equipamiento se encuentre en estado operativo DISPONIBLE, para garantizar que la
        //    transición de vuelta a EN_MANTENIMIENTO sea coherente
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede anular un mantenimiento cuyo equipamiento asociado se encuentre en estado operativo Disponible");
        }

        // 8. Restablecer el estado operativo del equipamiento a EN_MANTENIMIENTO y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.EN_MANTENIMIENTO);
        equipamientoRepository.save(equipamientoEntity);

        // 9. Aplicar la anulación sobre el mantenimiento y persistirlo
        mantenimientoEquipamientoEntity.setEstado(EstadoTransaccion.ANULADO);
        mantenimientoEquipamientoEntity.setFechaAnulacion(LocalDateTime.now());
        mantenimientoEquipamientoEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperMantenimientoEquipamiento.toDTO(mantenimientoEquipamientoRepository.save(mantenimientoEquipamientoEntity));
    }
}
