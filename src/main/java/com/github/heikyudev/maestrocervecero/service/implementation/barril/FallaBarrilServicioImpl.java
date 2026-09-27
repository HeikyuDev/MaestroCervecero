package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FallaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFallaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FallaBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperFallaBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FallaBarrilServicioImpl implements IFallaBarrilServicio {

    private final IFallaBarrilRepository fallaBarrilRepository;
    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IDevolucionBarrilRepository devolucionBarrilRepository;
    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Filtra las fallas de barril, opcionalmente por estado, barril y/o rango de fecha de falla.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: una falla anulada sigue siendo un
     * registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaFallaDesde Límite inferior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param fechaFallaHasta Límite superior (inclusive) del rango de fecha de falla, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FallaBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FallaBarrilResponseDTO> filtrarFallasBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaFallaDesde, LocalDateTime fechaFallaHasta, Pageable pageable) {
        return fallaBarrilRepository.filtrarFallasBarril(estado, idBarril, fechaFallaDesde, fechaFallaHasta, pageable)
                .map(MapperFallaBarril::toDTO);
    }

    /**
     * Busca y retorna una falla de barril mediante su identificador único.
     *
     * @param id El ID de la falla de barril.
     * @return La falla de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna falla de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FallaBarrilResponseDTO buscarPorId(Long id) {
        return MapperFallaBarril.toDTO(fallaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de barril con ID: " + id)));
    }

    /**
     * Registra una nueva falla de barril.
     * <p>
     * Deja al barril afectado en estado operativo {@code EN_MANTENIMIENTO}: mientras la falla
     * siga registrada, el barril no puede volver a asignarse a un envasado.
     * </p>
     *
     * @param fallaBarrilFormDTO Los datos de la falla a registrar.
     * @return La falla de barril registrada.
     * @throws ReglaNegocioException Si la fecha de falla no fue informada, si las observaciones no fueron informadas, si la fecha de falla no es posterior a la última operación registrada del ciclo de vida del barril, o si el barril no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FALLA_BARRIL)
    public FallaBarrilResponseDTO registrarFallaBarril(FallaBarrilFormDTO fallaBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de falla
        if (fallaBarrilFormDTO.getFechaFalla() == null) {
            throw new ReglaNegocioException("La fecha de falla es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (fallaBarrilFormDTO.getObservaciones() == null || fallaBarrilFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Localizar el barril, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idBarril = fallaBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        // 4. Validar que el barril se encuentre en estado operativo DISPONIBLE
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede registrar una falla sobre un barril en estado operativo DISPONIBLE");
        }

        // 5. Validar que la fecha de falla sea posterior a la última operación registrada sobre
        //    este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                fallaBarrilFormDTO.getFechaFalla(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 6. Cambiar el estado operativo del barril a EN_MANTENIMIENTO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        barrilRepository.save(barrilEntity);

        // 7. Construir y persistir la falla, y retornar el DTO de respuesta correspondiente
        FallaBarrilEntity fallaBarrilEntity = FallaBarrilEntity.builder()
                .fecha(fallaBarrilFormDTO.getFechaFalla())
                .observaciones(fallaBarrilFormDTO.getObservaciones())
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperFallaBarril.toDTO(fallaBarrilRepository.save(fallaBarrilEntity));
    }

    /**
     * Anula una falla de barril existente.
     * <p>
     * Restablece al barril afectado a estado operativo {@code DISPONIBLE}: solo procede si ese
     * barril todavía se encuentra en {@code EN_MANTENIMIENTO}, para garantizar que la transición
     * de vuelta sea coherente.
     * </p>
     *
     * @param id El ID de la falla de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La falla de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la falla no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si la falla de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.FALLA_BARRIL)
    public FallaBarrilResponseDTO anularFallaBarril(Long id, AnulacionFallaBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la falla de barril. Si no existe, se dispara RecursoNoEncontradoException
        FallaBarrilEntity fallaBarrilEntity = fallaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de barril con ID: " + id));

        // 3. Validar que la falla se encuentre en estado REGISTRADO
        if (fallaBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular fallas de barril en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el barril, comparando
        //    contra las 3 tablas del ciclo de vida (evita anular un registro viejo cuando una
        //    operación posterior ya dejó al barril en un estado distinto)
        Long idBarril = fallaBarrilEntity.getBarril().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                fallaBarrilEntity.getFecha(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null));

        // 5. Localizar el barril asociado, bloqueado para escritura, y validar que se encuentre
        //    en estado operativo EN_MANTENIMIENTO, para garantizar que la transición de vuelta a
        //    DISPONIBLE sea coherente
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(fallaBarrilEntity.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + fallaBarrilEntity.getBarril().getId()));
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede anular una falla cuyo barril asociado se encuentre en estado operativo EN_MANTENIMIENTO");
        }

        // 6. Restablecer el estado operativo del barril a DISPONIBLE y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DISPONIBLE);
        barrilRepository.save(barrilEntity);

        // 7. Aplicar la anulación sobre la falla y persistirla
        fallaBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        fallaBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        fallaBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperFallaBarril.toDTO(fallaBarrilRepository.save(fallaBarrilEntity));
    }
}
