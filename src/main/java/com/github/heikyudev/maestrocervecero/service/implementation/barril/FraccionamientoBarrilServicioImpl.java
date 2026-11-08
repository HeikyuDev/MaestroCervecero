package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
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
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FraccionamientoBarrilServicioImpl implements IFraccionamientoBarrilServicio {

    /** Nombre del campo del formulario con la cantidad de litros a extraer. */
    private static final String CAMPO_CANTIDAD_EXTRAIDA = "cantidadExtraida";

    private final IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
     * Descuenta la cantidad extraída del contenido actual del barril. La cantidad admite hasta
     * {@link LimitesTecnicos#ESCALA_CANTIDADES} decimales y el contenido restante se redondea a esa misma escala, para
     * que los residuos de la aritmética de punto flotante no impidan vaciar el barril. Si el contenido restante llega
     * exactamente a cero tras el redondeo, el barril pasa a estado operativo {@code EN_LIMPIEZA}; de lo contrario, se
     * mantiene en {@code CON_CERVEZA}. Ese resultado queda grabado en el propio fraccionamiento
     * ({@code estadoOperativoResultante}) para que una eventual anulación pueda validar contra el estado exacto que
     * este fraccionamiento puntual dejó.
     * </p>
     * <p>
     * La fecha debe ser posterior a la última operación registrada, al último envasado del barril y a la finalización
     * del lote de ese envasado, porque el barril queda {@code CON_CERVEZA} recién al finalizar el lote.
     * </p>
     * <p>
     * Orden de locks: lo primero que se bloquea (PESSIMISTIC_WRITE) es el barril, antes de leer su contenido, su última
     * operación y su último envasado. Es el único lock de la operación.
     * </p>
     *
     * @param fraccionamientoBarrilFormDTO Los datos del fraccionamiento a registrar.
     * @return El fraccionamiento de barril registrado.
     * @throws ReglaNegocioException Si la fecha o las observaciones no fueron informadas, si la fecha es posterior a la fecha y hora actual, si la cantidad a extraer no es mayor a 0, tiene más de cuatro decimales o supera el contenido actual del barril, si el barril no se encuentra en estado operativo {@code CON_CERVEZA}, o si la fecha no es posterior a la última operación registrada, al último envasado ni a la finalización del lote de ese envasado.
     * @throws RecursoNoEncontradoException Si el barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FRACCIONAMIENTO_BARRIL)
    public FraccionamientoBarrilResponseDTO registrarFraccionamientoBarril(FraccionamientoBarrilFormDTO fraccionamientoBarrilFormDTO) {
        // 1. Validar que se haya informado la fecha
        if (fraccionamientoBarrilFormDTO.getFecha() == null) {
            throw new ReglaNegocioException("La fecha y hora de la operación es obligatoria", MetodosCicloVida.CAMPO_FECHA);
        }

        // 2. Validar que la fecha no sea posterior a la fecha y hora actual
        MetodosCicloVida.validarFechaNoFutura(fraccionamientoBarrilFormDTO.getFecha(), MetodosCicloVida.CAMPO_FECHA);

        // 3. Validar las observaciones
        String observaciones = MetodosCicloVida.validarObservaciones(fraccionamientoBarrilFormDTO.getObservaciones());

        // 4. Validar que la cantidad a extraer sea mayor a 0 y no tenga más decimales que los permitidos
        Double cantidadExtraida = fraccionamientoBarrilFormDTO.getCantidadExtraida();
        if (cantidadExtraida == null || !Double.isFinite(cantidadExtraida) || cantidadExtraida <= 0) {
            throw new ReglaNegocioException("La cantidad extraída debe ser mayor a 0", CAMPO_CANTIDAD_EXTRAIDA);
        }
        ValidacionesNumericas.validarMaximoDecimales(cantidadExtraida, LimitesTecnicos.ESCALA_CANTIDADES, "La cantidad extraída", CAMPO_CANTIDAD_EXTRAIDA);

        // 5. Localizar el barril, bloqueado para escritura, antes de leer cualquier otro dato
        Long idBarril = fraccionamientoBarrilFormDTO.getIdBarril();
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 6. Validar que el barril se encuentre en estado operativo CON_CERVEZA
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.CON_CERVEZA) {
            throw new ReglaNegocioException("Solo se puede registrar un fraccionamiento sobre un barril en estado operativo Con cerveza", MetodosCicloVida.CAMPO_BARRIL);
        }

        // 7. Validar que la cantidad a extraer no supere el contenido actual del barril. El contenido restante se
        //    redondea a la escala de las cantidades, de modo que un residuo de punto flotante no cuente como contenido
        double contenidoRestante = Redondeo.redondear(barrilEntity.getContenidoActual() - cantidadExtraida);
        if (contenidoRestante < 0) {
            throw new ReglaNegocioException("La cantidad extraída no puede superar el contenido actual del barril", CAMPO_CANTIDAD_EXTRAIDA);
        }

        // 8. Validar que la fecha sea posterior a la última operación registrada sobre este
        //    barril, considerando también el último envasado que lo cargó
        MetodosCicloVida.validarFechaPosteriorAUltimaOperacion(
                fraccionamientoBarrilFormDTO.getFecha(),
                "barril",
                MetodosCicloVida.CAMPO_FECHA,
                operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(idBarril).orElse(null),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 9. Validar que la fecha sea posterior a la finalización del lote que dejó la cerveza en el barril
        MetodosCicloVida.validarPosteriorAFinalizacionDelLote(
                fraccionamientoBarrilFormDTO.getFecha(),
                envasadoLoteRepository.buscarFechaFinalizacionDelLoteDelUltimoEnvasadoRegistrado(idBarril).orElse(null),
                MetodosCicloVida.CAMPO_FECHA);

        // 9. Decidir el estado operativo resultante según si el barril quedó completamente vacío tras el redondeo
        EstadoOperativoBarril estadoOperativoResultante = contenidoRestante == 0
                ? EstadoOperativoBarril.EN_LIMPIEZA
                : EstadoOperativoBarril.CON_CERVEZA;

        // 10. Aplicar el contenido restante y el estado operativo resultante sobre el barril y persistirlo
        barrilEntity.setContenidoActual(contenidoRestante);
        barrilEntity.setEstadoOperativo(estadoOperativoResultante);
        barrilRepository.save(barrilEntity);

        // 11. Construir y persistir el fraccionamiento, y retornar el DTO de respuesta correspondiente
        FraccionamientoBarrilEntity fraccionamientoBarrilEntity = FraccionamientoBarrilEntity.builder()
                .fecha(fraccionamientoBarrilFormDTO.getFecha())
                .observaciones(observaciones)
                .cantidadFraccionada(cantidadExtraida)
                .estadoOperativoResultante(estadoOperativoResultante)
                .barril(barrilEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperFraccionamientoBarril.toDTO(fraccionamientoBarrilRepository.save(fraccionamientoBarrilEntity));
    }

    /**
     * Anula un fraccionamiento de barril existente.
     * <p>
     * Solo procede si es la última operación registrada del barril (la de mayor fecha, y a igual fecha la de mayor ID,
     * entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones), si el barril no está en uso por un lote,
     * si es posterior al último envasado del barril y si sigue exactamente en el estado
     * operativo que dejó este fraccionamiento puntual ({@code estadoOperativoResultante}), no en cualquiera de los dos
     * estados posibles en general: evita revertir un fraccionamiento viejo cuando el estado actual del barril en
     * realidad lo dejó otro fraccionamiento u operación posterior. Restituye la cantidad extraída al contenido actual
     * del barril, redondeado a {@link LimitesTecnicos#ESCALA_CANTIDADES} decimales, que vuelve a estado operativo
     * {@code CON_CERVEZA}.
     * </p>
     * <p>
     * Orden de locks: se bloquea primero el barril (el ID se resuelve con una consulta escalar, sin cargar el
     * fraccionamiento) y recién después se lee el fraccionamiento y se verifica que sea el último. Es el único lock de
     * la operación.
     * </p>
     *
     * @param id El ID del fraccionamiento de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return El fraccionamiento de barril anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado o supera el máximo, si el fraccionamiento no se encuentra en estado {@code REGISTRADO}, si no es la última operación registrada del barril, si el barril está en uso por un lote, si el barril tuvo un envasado posterior, o si el barril asociado no se encuentra en el estado operativo que dejó este fraccionamiento.
     * @throws RecursoNoEncontradoException Si el fraccionamiento de barril con el ID especificado no existe, o si el barril asociado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.FRACCIONAMIENTO_BARRIL)
    public FraccionamientoBarrilResponseDTO anularFraccionamientoBarril(Long id, AnulacionFraccionamientoBarrilFormDTO anulacionFormDTO) {
        // 1. Validar el motivo de anulación
        String motivoAnulacion = MetodosCicloVida.validarMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        // 2. Resolver el barril del fraccionamiento sin cargarlo y bloquearlo para escritura. Si el fraccionamiento
        //    no existe, se dispara RecursoNoEncontradoException
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fraccionamiento de barril con ID: " + id));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 3. Localizar el fraccionamiento, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        FraccionamientoBarrilEntity fraccionamientoBarrilEntity = fraccionamientoBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fraccionamiento de barril con ID: " + id));

        // 4. Validar que el fraccionamiento se encuentre en estado REGISTRADO
        if (fraccionamientoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular fraccionamientos de barril en estado Registrado");
        }

        // 5. Validar que sea la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID)
        MetodosCicloVida.validarEsLaUltimaOperacion(
                fraccionamientoBarrilEntity.getId(),
                operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril).orElse(null));

        // 6. Validar que el barril no esté en uso por un lote
        MetodosCicloVida.validarNoEstaEnUso(barrilEntity.getEstadoOperativo());

        // 7. Validar que el fraccionamiento sea posterior al último envasado del barril
        MetodosCicloVida.validarPosteriorAlUltimoEnvasado(
                fraccionamientoBarrilEntity.getFecha(),
                envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril).orElse(null));

        // 8. Validar que el barril se encuentre exactamente en el estado operativo que dejó este fraccionamiento
        if (barrilEntity.getEstadoOperativo() != fraccionamientoBarrilEntity.getEstadoOperativoResultante()) {
            throw new ReglaNegocioException("Solo se puede anular un fraccionamiento cuyo barril asociado se encuentre en estado operativo " + fraccionamientoBarrilEntity.getEstadoOperativoResultante().getEtiqueta());
        }

        // 9. Restituir la cantidad extraída al contenido actual del barril (redondeado a la escala de las
        //    cantidades), restablecer su estado operativo a CON_CERVEZA y persistirlo
        barrilEntity.setContenidoActual(Redondeo.redondear(barrilEntity.getContenidoActual() + fraccionamientoBarrilEntity.getCantidadFraccionada()));
        barrilEntity.setEstadoOperativo(EstadoOperativoBarril.CON_CERVEZA);
        barrilRepository.save(barrilEntity);

        // 10. Aplicar la anulación sobre el fraccionamiento y persistirlo
        fraccionamientoBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        fraccionamientoBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        fraccionamientoBarrilEntity.setMotivoAnulacion(motivoAnulacion);

        return MapperFraccionamientoBarril.toDTO(fraccionamientoBarrilRepository.save(fraccionamientoBarrilEntity));
    }
}
