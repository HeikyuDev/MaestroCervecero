package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionFraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFraccionamientoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FraccionamientoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperFraccionamientoBarril;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FraccionamientoBarrilServicioImpl implements IFraccionamientoBarrilServicio {

    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IFallaBarrilRepository fallaBarrilRepository;
    private final IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    private final ILimpiezaBarrilRepository limpiezaBarrilRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IDevolucionBarrilRepository devolucionBarrilRepository;
    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Filtra los fraccionamientos de barril, opcionalmente por estado, barril y/o rango de fecha
     * de fraccionamiento.
     * <p>
     * {@code estado} no asume {@code REGISTRADO} por defecto: un fraccionamiento anulado sigue
     * siendo un registro histórico consultable, así que {@code null} muestra ambos estados.
     * </p>
     *
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param idBarril El ID del barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDesde Límite inferior (inclusive) del rango de fecha de fraccionamiento, o {@code null} para no acotarlo.
     * @param fechaHasta Límite superior (inclusive) del rango de fecha de fraccionamiento, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FraccionamientoBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FraccionamientoBarrilResponseDTO> filtrarFraccionamientosBarril(EstadoTransaccion estado, Long idBarril, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        return fraccionamientoBarrilRepository.filtrarFraccionamientosBarril(estado, idBarril, fechaDesde, fechaHasta, pageable)
                .map(MapperFraccionamientoBarril::toDTO);
    }

    /**
     * Busca y retorna un fraccionamiento de barril mediante su identificador único.
     *
     * @param id El ID del fraccionamiento de barril.
     * @return El fraccionamiento de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún fraccionamiento de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FraccionamientoBarrilResponseDTO buscarPorId(Long id) {
        return MapperFraccionamientoBarril.toDTO(fraccionamientoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fraccionamiento de barril con ID: " + id)));
    }

    /**
     * Registra un nuevo fraccionamiento de barril.
     * <p>
     * Descuenta la cantidad extraída del contenido actual del barril. Si el contenido restante
     * llega exactamente a cero, el barril pasa a estado operativo {@code EN_LIMPIEZA}; de lo
     * contrario, se mantiene en {@code CON_CERVEZA}. Ese resultado queda grabado en el propio
     * fraccionamiento ({@code estadoOperativoResultante}) para que una eventual anulación pueda
     * validar contra el estado exacto que este fraccionamiento puntual dejó.
     * </p>
     *
     * @param fraccionamientoBarrilFormDTO Los datos del fraccionamiento a registrar.
     * @return El fraccionamiento de barril registrado.
     * @throws ReglaNegocioException Si la fecha o las observaciones no fueron informadas, si la cantidad a extraer no es mayor a cero o supera el contenido actual del barril, si la fecha no es posterior a la última operación registrada del ciclo de vida del barril, o si el barril no se encuentra en estado operativo {@code CON_CERVEZA}.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FRACCIONAMIENTO_BARRIL)
    public FraccionamientoBarrilResponseDTO registrarFraccionamientoBarril(FraccionamientoBarrilFormDTO fraccionamientoBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha
        if (fraccionamientoBarrilFormDTO.getFecha() == null) {
            throw new ReglaNegocioException("La fecha es obligatoria");
        }

        // 2. Validar que se hayan informado las observaciones
        if (fraccionamientoBarrilFormDTO.getObservaciones() == null || fraccionamientoBarrilFormDTO.getObservaciones().isBlank()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias");
        }

        // 3. Validar que la cantidad a extraer sea mayor a cero
        if (fraccionamientoBarrilFormDTO.getCantidadExtraida() == null || fraccionamientoBarrilFormDTO.getCantidadExtraida() <= 0) {
            throw new ReglaNegocioException("La cantidad a extraer debe ser mayor a cero");
        }

        // 4. Localizar el barril, bloqueado para escritura. Si no existe, se dispara RecursoNoEncontradoException
        Long idBarril = fraccionamientoBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril));

        // 5. Validar que el barril se encuentre en estado operativo CON_CERVEZA
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.CON_CERVEZA) {
            throw new ReglaNegocioException("Solo se puede registrar un fraccionamiento sobre un barril en estado operativo CON_CERVEZA");
        }

        // 6. Validar que la cantidad a extraer no supere el contenido actual del barril
        if (fraccionamientoBarrilFormDTO.getCantidadExtraida() > barrilEntity.getContenidoActual()) {
            throw new ReglaNegocioException("La cantidad a extraer no puede superar el contenido actual del barril");
        }

        // 7. Validar que la fecha sea posterior a la última operación registrada sobre este
        //    barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                fraccionamientoBarrilFormDTO.getFecha(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Descontar la cantidad extraída del contenido actual, y decidir el estado operativo
        //    resultante según si el barril quedó completamente vacío
        double contenidoRestante = barrilEntity.getContenidoActual() - fraccionamientoBarrilFormDTO.getCantidadExtraida();
        EstadoOperativoBarril estadoOperativoResultante = contenidoRestante == 0
                ? EstadoOperativoBarril.EN_LIMPIEZA
                : EstadoOperativoBarril.CON_CERVEZA;

        // 9. Aplicar el contenido restante y el estado operativo resultante sobre el barril y persistirlo
        barrilEntity.setContenidoActual(contenidoRestante);
        barrilEntity.setEstadoOperativo(estadoOperativoResultante);
        barrilRepository.save(barrilEntity);

        // 10. Construir y persistir el fraccionamiento, y retornar el DTO de respuesta correspondiente
        FraccionamientoBarrilEntity fraccionamientoBarrilEntity = FraccionamientoBarrilEntity.builder()
                .fecha(fraccionamientoBarrilFormDTO.getFecha())
                .observaciones(fraccionamientoBarrilFormDTO.getObservaciones())
                .cantidadFraccionada(fraccionamientoBarrilFormDTO.getCantidadExtraida())
                .estadoOperativoResultante(estadoOperativoResultante)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperFraccionamientoBarril.toDTO(fraccionamientoBarrilRepository.save(fraccionamientoBarrilEntity));
    }

    /**
     * Anula un fraccionamiento de barril existente.
     * <p>
     * Solo procede si el barril asociado sigue exactamente en el estado operativo que dejó este
     * fraccionamiento puntual ({@code estadoOperativoResultante}), no en cualquiera de los dos
     * estados posibles en general: evita revertir un fraccionamiento viejo cuando el estado actual
     * del barril en realidad lo dejó otro fraccionamiento u operación posterior. Restituye la
     * cantidad extraída al contenido actual del barril, que vuelve a estado operativo
     * {@code CON_CERVEZA}.
     * </p>
     *
     * @param id El ID del fraccionamiento de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El fraccionamiento de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el fraccionamiento no se encuentra en estado {@code REGISTRADO}, si no es la operación más reciente registrada sobre el barril, o si el barril asociado no se encuentra en el estado operativo que dejó este fraccionamiento.
     * @throws RecursoNoEncontradoException Si el fraccionamiento de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.FRACCIONAMIENTO_BARRIL)
    public FraccionamientoBarrilResponseDTO anularFraccionamientoBarril(Long id, AnulacionFraccionamientoBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar el fraccionamiento de barril. Si no existe, se dispara RecursoNoEncontradoException
        FraccionamientoBarrilEntity fraccionamientoBarrilEntity = fraccionamientoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fraccionamiento de barril con ID: " + id));

        // 3. Validar que el fraccionamiento se encuentre en estado REGISTRADO
        if (fraccionamientoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular fraccionamientos de barril en estado REGISTRADO");
        }

        // 4. Validar que sea la operación más reciente registrada sobre el barril, comparando
        //    contra las 6 tablas del ciclo de vida (evita anular un registro viejo cuando una
        //    operación posterior ya dejó al barril en un estado distinto)
        Long idBarril = fraccionamientoBarrilEntity.getBarril().getId();
        MetodosCicloVida.validarEsOperacionMasReciente(
                fraccionamientoBarrilEntity.getFecha(),
                "barril",
                fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(idBarril).orElse(null),
                mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(idBarril).orElse(null),
                limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(idBarril).orElse(null),
                despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(idBarril).orElse(null),
                devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(idBarril).orElse(null),
                fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(idBarril).orElse(null));

        // 5. Localizar el barril asociado, bloqueado para escritura, y validar que se encuentre
        //    exactamente en el estado operativo que dejó este fraccionamiento puntual
        BarrilEntity barrilEntity = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(fraccionamientoBarrilEntity.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + fraccionamientoBarrilEntity.getBarril().getId()));
        if (barrilEntity.getEstadoOperativo() != fraccionamientoBarrilEntity.getEstadoOperativoResultante()) {
            throw new ReglaNegocioException("Solo se puede anular un fraccionamiento cuyo barril asociado se encuentre en estado operativo " + fraccionamientoBarrilEntity.getEstadoOperativoResultante());
        }

        // 6. Restituir la cantidad extraída al contenido actual del barril, restablecer su estado
        //    operativo a CON_CERVEZA y persistirlo
        barrilEntity.setContenidoActual(barrilEntity.getContenidoActual() + fraccionamientoBarrilEntity.getCantidadFraccionada());
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.CON_CERVEZA);
        barrilRepository.save(barrilEntity);

        // 7. Aplicar la anulación sobre el fraccionamiento y persistirlo
        fraccionamientoBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        fraccionamientoBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        fraccionamientoBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperFraccionamientoBarril.toDTO(fraccionamientoBarrilRepository.save(fraccionamientoBarrilEntity));
    }
}
