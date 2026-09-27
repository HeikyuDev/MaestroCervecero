package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionMantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.MantenimientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IMantenimientoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.MantenimientoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperMantenimientoBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MantenimientoBarrilServicioImpl implements IMantenimientoBarrilServicio {

    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final IFallaBarrilRepository fallaBarrilRepository;
    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IDevolucionBarrilRepository devolucionBarrilRepository;
    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Filtra los mantenimientos de barril, opcionalmente por barril, estado y/o rango de fecha de
     * mantenimiento.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un mantenimiento anulado sigue
     * siendo un registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaMantenimientoDesde Límite inferior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param fechaMantenimientoHasta Límite superior (inclusive) del rango de fecha de mantenimiento, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MantenimientoBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MantenimientoBarrilResponseDTO> filtrarMantenimientosBarril(Long idBarril, EstadoTransaccion estado, LocalDateTime fechaMantenimientoDesde, LocalDateTime fechaMantenimientoHasta, Pageable pageable) {
        return mantenimientoBarrilRepository.filtrarMantenimientosBarril(idBarril, estado, fechaMantenimientoDesde, fechaMantenimientoHasta, pageable)
                .map(MapperMantenimientoBarril::toDTO);
    }

    /**
     * Busca y retorna un mantenimiento de barril mediante su identificador único.
     *
     * @param id El ID del mantenimiento de barril.
     * @return El mantenimiento de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún mantenimiento de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MantenimientoBarrilResponseDTO buscarPorId(Long id) {
        return MapperMantenimientoBarril.toDTO(mantenimientoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de barril con ID: " + id)));
    }

    /**
     * Registra un nuevo mantenimiento de barril.
     * <p>
     * Deja al barril afectado en estado operativo {@code DISPONIBLE}: el mantenimiento cierra el
     * ciclo abierto por una falla, devolviendo el barril a producción.
     * </p>
     *
     * @param mantenimientoBarrilFormDTO Los datos del mantenimiento a registrar.
     * @return El mantenimiento de barril registrado.
     * @throws ReglaNegocioException Si la fecha de mantenimiento no fue informada, si las observaciones no fueron informadas, si la fecha de mantenimiento no es posterior a la última operación registrada del ciclo de vida del barril, o si el barril no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_BARRIL)
    public MantenimientoBarrilResponseDTO registrarMantenimientoBarril(MantenimientoBarrilFormDTO mantenimientoBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de mantenimiento
        if (mantenimientoBarrilFormDTO.getFechaMantenimiento() == null) {
            throw new ReglaNegocioException("La fecha de mantenimiento es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (mantenimientoBarrilFormDTO.getObservaciones() == null || mantenimientoBarrilFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Localizar el barril, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idBarril = mantenimientoBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        // 4. Validar que el barril se encuentre en estado operativo EN_MANTENIMIENTO
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede registrar un mantenimiento sobre un barril en estado operativo EN_MANTENIMIENTO");
        }

        // 5. Validar que la fecha de mantenimiento sea posterior a la última operación registrada
        //    sobre este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                mantenimientoBarrilFormDTO.getFechaMantenimiento(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 6. Cambiar el estado operativo del barril a DISPONIBLE y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DISPONIBLE);
        barrilRepository.save(barrilEntity);

        // 7. Construir y persistir el mantenimiento, y retornar el DTO de respuesta correspondiente
        MantenimientoBarrilEntity mantenimientoBarrilEntity = MantenimientoBarrilEntity.builder()
                .fecha(mantenimientoBarrilFormDTO.getFechaMantenimiento())
                .observaciones(mantenimientoBarrilFormDTO.getObservaciones())
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperMantenimientoBarril.toDTO(mantenimientoBarrilRepository.save(mantenimientoBarrilEntity));
    }

    /**
     * Anula un mantenimiento de barril existente.
     * <p>
     * Restablece al barril afectado a estado operativo {@code EN_MANTENIMIENTO}: solo procede si
     * ese barril todavía se encuentra en {@code DISPONIBLE}, para garantizar que la transición de
     * vuelta sea coherente.
     * </p>
     *
     * @param id El ID del mantenimiento de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El mantenimiento de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el mantenimiento no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el mantenimiento de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_BARRIL)
    public MantenimientoBarrilResponseDTO anularMantenimientoBarril(Long id, AnulacionMantenimientoBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar el mantenimiento de barril. Si no existe, se dispara RecursoNoEncontradoException
        MantenimientoBarrilEntity mantenimientoBarrilEntity = mantenimientoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de barril con ID: " + id));

        // 3. Validar que el mantenimiento se encuentre en estado REGISTRADO
        if (mantenimientoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular mantenimientos de barril en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el barril, comparando
        //    contra las 3 tablas del ciclo de vida (evita anular un registro viejo cuando una
        //    operación posterior ya dejó al barril en un estado distinto)
        Long idBarril = mantenimientoBarrilEntity.getBarril().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                mantenimientoBarrilEntity.getFecha(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null));

        // 5. Localizar el barril asociado, bloqueado para escritura, y validar que se encuentre en
        //    estado operativo DISPONIBLE, para garantizar que la transición de vuelta a
        //    EN_MANTENIMIENTO sea coherente
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(mantenimientoBarrilEntity.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + mantenimientoBarrilEntity.getBarril().getId()));
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede anular un mantenimiento cuyo barril asociado se encuentre en estado operativo DISPONIBLE");
        }

        // 6. Restablecer el estado operativo del barril a EN_MANTENIMIENTO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        barrilRepository.save(barrilEntity);

        // 7. Aplicar la anulación sobre el mantenimiento y persistirlo
        mantenimientoBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        mantenimientoBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        mantenimientoBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperMantenimientoBarril.toDTO(mantenimientoBarrilRepository.save(mantenimientoBarrilEntity));
    }
}
