package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
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
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el barril, antes de leer su última operación y
     * su último envasado. Es el único lock de la operación.
     * </p>
     *
     * @param fallaBarrilFormDTO Los datos de la falla a registrar.
     * @return La falla de barril registrada.
     * @throws ReglaNegocioException Si la fecha de falla no fue informada o es futura, si las observaciones no fueron informadas o superan el máximo, si el barril no se encuentra en estado operativo {@code DISPONIBLE}, o si la fecha no es posterior a la última operación registrada ni al último envasado del barril.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FALLA_BARRIL)
    public FallaBarrilResponseDTO registrarFallaBarril(FallaBarrilFormDTO fallaBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha de falla
        if (fallaBarrilFormDTO.getFecha() == null) {
            throw new ReglaNegocioException("La fecha de falla es obligatoria", MetodosCicloVida.CAMPO_FECHA);
        }

        // 2. Validar que la fecha de falla no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(fallaBarrilFormDTO.getFecha(), MetodosCicloVida.CAMPO_FECHA);

        // 3. Validar las observaciones
        String observaciones = MetodosCicloVida.validarObservaciones(fallaBarrilFormDTO.getObservaciones());

        // 4. Localizar el barril, bloqueado para escritura, antes de leer cualquier otro dato
        Long idBarril = fallaBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 5. Validar que el barril se encuentre en estado operativo DISPONIBLE
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DISPONIBLE) {
            throw new ReglaNegocioException("Solo se puede registrar una falla sobre un barril en estado operativo Disponible", MetodosCicloVida.CAMPO_BARRIL);
        }

        // 6. Validar que la fecha de falla sea posterior a la última operación registrada sobre
        //    este barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                fallaBarrilFormDTO.getFecha(),
                "barril",
                MetodosCicloVida.CAMPO_FECHA,
                operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 7. Cambiar el estado operativo del barril a EN_MANTENIMIENTO y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.EN_MANTENIMIENTO);
        barrilRepository.save(barrilEntity);

        // 8. Construir y persistir la falla, y retornar el DTO de respuesta correspondiente
        FallaBarrilEntity fallaBarrilEntity = FallaBarrilEntity.builder()
                .fecha(fallaBarrilFormDTO.getFecha())
                .observaciones(observaciones)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperFallaBarril.toDTO(fallaBarrilRepository.save(fallaBarrilEntity));
    }

    /**
     * Anula una falla de barril existente.
     * <p>
     * Solo procede si es la última operación registrada del barril (la de mayor fecha, y a igual fecha la de mayor ID,
     * entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones), si el barril no está en uso por un lote y
     * si es posterior al último envasado del barril. Restablece al barril afectado a estado
     * operativo {@code DISPONIBLE}: solo procede si ese barril todavía se encuentra en {@code EN_MANTENIMIENTO}, para
     * garantizar que la transición de vuelta sea coherente.
     * </p>
     * <p>
     * Orden de locks: se bloquea primero el barril (el ID se resuelve con una consulta escalar, sin cargar la falla) y
     * recién después se lee la falla y se verifica que sea la última. Es el único lock de la operación.
     * </p>
     *
     * @param id El ID de la falla de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La falla de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si la falla no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del barril, si el barril está en uso por un lote, si el barril tuvo un envasado posterior, o si el barril asociado no se encuentra en estado operativo {@code EN_MANTENIMIENTO}.
     * @throws RecursoNoEncontradoException Si la falla de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.FALLA_BARRIL)
    public FallaBarrilResponseDTO anularFallaBarril(Long id, AnulacionFallaBarrilFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVida.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el barril de la falla sin cargarla y bloquearlo para escritura. Si la falla no existe, se
        //    dispara RecursoNoEncontradoException
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de barril con ID: " + id));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 3. Localizar la falla, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        FallaBarrilEntity fallaBarrilEntity = fallaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la falla de barril con ID: " + id));

        // 4. Validar que la falla se encuentre en estado REGISTRADO
        if (fallaBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular fallas de barril en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVida.validarEsLaUltimaOperacion(
                fallaBarrilEntity.getId(),
                operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null));

        // 6. Validar que el barril no esté en uso por un lote
        MetodosCicloVida.validarNoEstaEnUso(barrilEntity.getEstadoOperativo());

        // 7. Validar que la falla sea posterior al último envasado del barril
        MetodosCicloVida.validarPosteriorAlUltimoEnvasado(
                fallaBarrilEntity.getFecha(),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Validar que el barril se encuentre en estado operativo EN_MANTENIMIENTO, para garantizar que la
        //    transición de vuelta a DISPONIBLE sea coherente
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.EN_MANTENIMIENTO) {
            throw new ReglaNegocioException("Solo se puede anular una falla cuyo barril asociado se encuentre en estado operativo En mantenimiento");
        }

        // 9. Restablecer el estado operativo del barril a DISPONIBLE y persistirlo
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.DISPONIBLE);
        barrilRepository.save(barrilEntity);

        // 10. Aplicar la anulación sobre la falla y persistirla
        fallaBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        fallaBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        fallaBarrilEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperFallaBarril.toDTO(fallaBarrilRepository.save(fallaBarrilEntity));
    }
}
