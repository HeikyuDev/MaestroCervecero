package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOperacionCicloVidaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFallaEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionFallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFallaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FallaEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperFallaEquipamiento;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosCicloVidaEquipamiento;
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
    private final IEquipamientoRepository equipamientoRepository;
    private final IOperacionCicloVidaEquipamientoRepository operacionCicloVidaEquipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

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
     * La falla tiene una única fecha y hora y deja al equipamiento en estado operativo
     * {@code EN_MANTENIMIENTO}, a la espera del mantenimiento correspondiente.
     * </p>
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el equipamiento, antes de leer su última
     * operación o su última participación en un lote. Es el único lock de la operación.
     * </p>
     *
     * @param fallaEquipamientoFormDTO Los datos de la falla a registrar.
     * @return La falla de equipamiento registrada.
     * @throws ReglaNegocioException Si la fecha no fue informada o es futura, si las observaciones no fueron informadas o superan el máximo, si el equipamiento está dado de baja o no se encuentra en estado operativo {@code DISPONIBLE}, o si la fecha no es posterior a la última operación registrada ni al último uso del equipamiento en un lote.
     * @throws RecursoNoEncontradoException Si el equipamiento referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FALLA_EQUIPAMIENTO)
    public FallaEquipamientoResponseDTO registrarFallaEquipamiento(FallaEquipamientoFormDTO fallaEquipamientoFormDTO) {
        // 1. Validar la fecha de la operación
        MetodosCicloVidaEquipamiento.validarFecha(fallaEquipamientoFormDTO.getFecha());

        // 2. Validar las observaciones
        String observaciones = MetodosCicloVidaEquipamiento.validarObservaciones(fallaEquipamientoFormDTO.getObservaciones());

        // 3. Localizar el equipamiento, bloqueado para escritura, antes de leer cualquier otro dato
        Long idEquipamiento = fallaEquipamientoFormDTO.getIdEquipamiento();
        EquipamientoEntity equipamientoEntity = MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(
                equipamientoRepository.buscarPorIdParaOperacionCicloVida(idEquipamiento), idEquipamiento);

        // 4. Validar que el equipamiento se encuentre en estado operativo DISPONIBLE
        MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(equipamientoEntity.getEstadoOperativo(), EstadoOperativo.DISPONIBLE, "una falla");

        // 5. Validar que la fecha sea posterior a la de la última operación registrada y al fin de la última
        //    participación del equipamiento en un lote
        MetodosCicloVidaEquipamiento.validarFechaPosteriorALaActividadAnterior(
                fallaEquipamientoFormDTO.getFecha(),
                operacionCicloVidaEquipamientoRepository.buscarFechaUltimaOperacionRegistrada(idEquipamiento).orElse(null),
                etapaLoteRepository.buscarFechaUltimaParticipacionRegistrada(idEquipamiento).orElse(null));

        // 6. Cambiar el estado operativo del equipamiento a EN_MANTENIMIENTO y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.EN_MANTENIMIENTO);
        equipamientoRepository.save(equipamientoEntity);

        // 7. Construir y persistir la falla, y retornar el DTO de respuesta correspondiente
        FallaEquipamientoEntity fallaEquipamientoEntity = FallaEquipamientoEntity.builder()
                .fecha(fallaEquipamientoFormDTO.getFecha())
                .observaciones(observaciones)
                .equipamiento(equipamientoEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperFallaEquipamiento.toDTO(fallaEquipamientoRepository.save(fallaEquipamientoEntity));
    }

    /**
     * Anula una falla de equipamiento existente.
     * <p>
     * Solo procede si es la última operación registrada del equipamiento (la de mayor fecha, y a igual fecha la de mayor ID, entre sus
     * limpiezas, fallas y mantenimientos) y si el equipamiento asociado se encuentra en estado operativo
     * {@code EN_MANTENIMIENTO}; el equipamiento vuelve a {@code DISPONIBLE}.
     * </p>
     * <p>
     * Orden de locks: igual que al registrar, se bloquea primero el equipamiento (el ID se resuelve con una consulta
     * escalar, sin cargar la falla) y recién después se lee la falla y se verifica que sea la última. Es el único
     * lock de la operación.
     * </p>
     *
     * @param id El ID de la falla de equipamiento a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La falla de equipamiento anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si el equipamiento está dado de baja, si la falla no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del equipamiento, o si el equipamiento asociado no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si la falla de equipamiento con el ID especificado no existe, o si el equipamiento asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.FALLA_EQUIPAMIENTO)
    public FallaEquipamientoResponseDTO anularFallaEquipamiento(Long id, AnulacionFallaEquipamientoFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVidaEquipamiento.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el equipamiento de la falla sin cargarla y bloquearlo para escritura. Si la falla no
        //    existe, se dispara RecursoNoEncontradoException
        Long idEquipamiento = operacionCicloVidaEquipamientoRepository.buscarIdEquipamientoDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de equipamiento con ID: " + id));
        EquipamientoEntity equipamientoEntity = MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(
                equipamientoRepository.buscarPorIdParaOperacionCicloVida(idEquipamiento), idEquipamiento);

        // 3. Localizar la falla, ya con el equipamiento bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        FallaEquipamientoEntity fallaEquipamientoEntity = fallaEquipamientoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de equipamiento con ID: " + id));

        // 4. Validar que la falla se encuentre en estado REGISTRADO
        if (fallaEquipamientoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular fallas de equipamiento en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del equipamiento (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVidaEquipamiento.validarEsLaUltimaOperacion(
                fallaEquipamientoEntity.getId(),
                operacionCicloVidaEquipamientoRepository.buscarIdUltimaOperacionRegistrada(idEquipamiento).orElse(null));

        // 6. Validar que el equipamiento se encuentre en estado operativo EN_MANTENIMIENTO, para garantizar que
        //    la transición de vuelta a DISPONIBLE sea coherente
        if (equipamientoEntity.getEstadoOperativo() != EstadoOperativo.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede anular una falla cuyo equipamiento asociado se encuentre en estado operativo En mantenimiento");
        }

        // 7. Restablecer el estado operativo del equipamiento a DISPONIBLE y persistirlo
        equipamientoEntity.setEstadoOperativo(EstadoOperativo.DISPONIBLE);
        equipamientoRepository.save(equipamientoEntity);

        // 8. Aplicar la anulación sobre la falla y persistirla
        fallaEquipamientoEntity.setEstado(EstadoTransaccion.ANULADO);
        fallaEquipamientoEntity.setFechaAnulacion(LocalDateTime.now());
        fallaEquipamientoEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperFallaEquipamiento.toDTO(fallaEquipamientoRepository.save(fallaEquipamientoEntity));
    }
}
