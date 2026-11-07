package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.MantenimientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
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
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el barril, antes de leer su última operación y
     * su último envasado. Es el único lock de la operación.
     * </p>
     *
     * @param mantenimientoBarrilFormDTO Los datos del mantenimiento a registrar.
     * @return El mantenimiento de barril registrado.
     * @throws ReglaNegocioException Si la fecha de mantenimiento no fue informada o es futura, si las observaciones no fueron informadas o superan el máximo, si el barril no se encuentra en estado operativo {@code EN_MANTENIMIENTO}, o si la fecha no es posterior a la última operación registrada ni al último envasado del barril.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_BARRIL)
    public MantenimientoBarrilResponseDTO registrarMantenimientoBarril(MantenimientoBarrilFormDTO mantenimientoBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de mantenimiento
        if (mantenimientoBarrilFormDTO.getFecha() == null) {
            throw new ReglaNegocioException("La fecha de mantenimiento es obligatoria", MetodosCicloVida.CAMPO_FECHA);
        }

        // 2. Validar que la fecha de mantenimiento no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(mantenimientoBarrilFormDTO.getFecha(), MetodosCicloVida.CAMPO_FECHA);

        // 3. Validar las observaciones
        String observaciones = MetodosCicloVida.validarObservaciones(mantenimientoBarrilFormDTO.getObservaciones());

        // 4. Localizar el barril, bloqueado para escritura, antes de leer cualquier otro dato
        Long idBarril = mantenimientoBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 5. Validar que el barril se encuentre en estado operativo EN_MANTENIMIENTO
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede registrar un mantenimiento sobre un barril en estado operativo En mantenimiento", MetodosCicloVida.CAMPO_BARRIL);
        }

        // 6. Validar que la fecha de mantenimiento sea posterior a la última operación registrada
        //    sobre este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                mantenimientoBarrilFormDTO.getFecha(),
                "barril",
                MetodosCicloVida.CAMPO_FECHA,
                operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 7. Cambiar el estado operativo del barril a DISPONIBLE y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DISPONIBLE);
        barrilRepository.save(barrilEntity);

        // 8. Construir y persistir el mantenimiento, y retornar el DTO de respuesta correspondiente
        MantenimientoBarrilEntity mantenimientoBarrilEntity = MantenimientoBarrilEntity.builder()
                .fecha(mantenimientoBarrilFormDTO.getFecha())
                .observaciones(observaciones)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperMantenimientoBarril.toDTO(mantenimientoBarrilRepository.save(mantenimientoBarrilEntity));
    }

    /**
     * Anula un mantenimiento de barril existente.
     * <p>
     * Solo procede si es la última operación registrada del barril (la de mayor fecha, y a igual fecha la de mayor ID,
     * entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones), si el barril no está en uso por un lote y
     * si es posterior al último envasado del barril. Restablece al barril afectado a estado
     * operativo {@code EN_MANTENIMIENTO}: solo procede si ese barril todavía se encuentra en {@code DISPONIBLE}, para
     * garantizar que la transición de vuelta sea coherente.
     * </p>
     * <p>
     * Orden de locks: se bloquea primero el barril (el ID se resuelve con una consulta escalar, sin cargar el
     * mantenimiento) y recién después se lee el mantenimiento y se verifica que sea el último. Es el único lock de la
     * operación.
     * </p>
     *
     * @param id El ID del mantenimiento de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El mantenimiento de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si el mantenimiento no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del barril, si el barril está en uso por un lote, si el barril tuvo un envasado posterior, o si el barril asociado no se encuentra en estado operativo {@code DISPONIBLE}.
     * @throws RecursoNoEncontradoException Si el mantenimiento de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.MANTENIMIENTO_BARRIL)
    public MantenimientoBarrilResponseDTO anularMantenimientoBarril(Long id, AnulacionMantenimientoBarrilFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVida.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el barril del mantenimiento sin cargarlo y bloquearlo para escritura. Si el mantenimiento no
        //    existe, se dispara RecursoNoEncontradoException
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de barril con ID: " + id));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 3. Localizar el mantenimiento, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        MantenimientoBarrilEntity mantenimientoBarrilEntity = mantenimientoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el mantenimiento de barril con ID: " + id));

        // 4. Validar que el mantenimiento se encuentre en estado REGISTRADO
        if (mantenimientoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular mantenimientos de barril en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVida.validarEsLaUltimaOperacion(
                mantenimientoBarrilEntity.getId(),
                operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null));

        // 6. Validar que el barril no esté en uso por un lote
        MetodosCicloVida.validarNoEstaEnUso(barrilEntity.getEstadoOperativo());

        // 7. Validar que el mantenimiento sea posterior al último envasado del barril
        MetodosCicloVida.validarPosteriorAlUltimoEnvasado(
                mantenimientoBarrilEntity.getFecha(),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Validar que el barril se encuentre en estado operativo DISPONIBLE, para garantizar que la transición de
        //    vuelta a EN_MANTENIMIENTO sea coherente
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede anular un mantenimiento cuyo barril asociado se encuentre en estado operativo Disponible");
        }

        // 9. Restablecer el estado operativo del barril a EN_MANTENIMIENTO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        barrilRepository.save(barrilEntity);

        // 10. Aplicar la anulación sobre el mantenimiento y persistirlo
        mantenimientoBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        mantenimientoBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        mantenimientoBarrilEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperMantenimientoBarril.toDTO(mantenimientoBarrilRepository.save(mantenimientoBarrilEntity));
    }
}
