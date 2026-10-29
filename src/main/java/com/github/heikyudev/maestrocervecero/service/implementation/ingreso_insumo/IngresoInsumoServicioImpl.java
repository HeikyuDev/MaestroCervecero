package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IIngresoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperIngresoInsumo;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Implementación del servicio de ingresos de insumo.
 * <p>
 * Orden de bloqueo (invariante): todo método que bloquea varias filas lo hace siempre en el mismo orden, para que
 * dos transacciones nunca se esperen entre sí. Registrar un ingreso por compra: ítem de la orden, orden de compra,
 * insumo y lote. Anular un ingreso: ingreso, orden de compra y lote. Quien cambia el estado de la orden de compra
 * (anular, finalizar) solo bloquea la orden. Un método nuevo que bloquee estas filas debe respetar este orden.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class IngresoInsumoServicioImpl implements IIngresoInsumoServicio {

    /**
     * Índice único de la base de datos que garantiza que un insumo no tenga dos lotes con la misma identificación de lote de proveedor.
     */
    private static final String INDICE_LOTE_INSUMO = "udx_lote_insumo_insumo_identificacion";

    private final IIngresoInsumoRepository ingresoInsumoRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IDetalleCompraRepository detalleCompraRepository;
    private final IOrdenCompraRepository ordenCompraRepository;
    private final IInsumoRepository insumoRepository;

    /**
     * Filtra los ingresos de insumo, opcionalmente por insumo, identificación del lote del
     * proveedor, estado, tipo de ingreso y/o fecha de ingreso.
     * <p>
     * A diferencia del resto de los módulos, incluye tanto los ingresos en estado
     * {@code REGISTRADO} como los {@code ANULADO} cuando {@code estado} es {@code null}: la
     * anulación es un cierre excepcional del registro histórico, no una baja lógica que deba
     * ocultarlo de las búsquedas.
     * </p>
     *
     * @param idInsumo El ID del insumo a filtrar, o {@code null} para no filtrar por él.
     * @param identificacionLoteProveedor Texto a buscar dentro de la identificación del lote del proveedor, o {@code null} para no filtrar por ella.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param tipoIngreso El tipo de ingreso exacto a filtrar (COMPRA/DIRECTO), o {@code null} para no filtrar por él.
     * @param fechaIngreso La fecha de ingreso exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Una página de ingresos de insumo en formato DTO que cumplen los criterios indicados.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<IngresoInsumoResponseDTO> filtrarIngresoInsumo(Long idInsumo, String identificacionLoteProveedor, EstadoTransaccion estado, TipoIngreso tipoIngreso, LocalDate fechaIngreso, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más (se compara sin distinguir mayúsculas)
        String identificacionLoteNormalizada = NormalizacionTexto.nombrePropio(identificacionLoteProveedor);
        if (identificacionLoteNormalizada != null && identificacionLoteNormalizada.isEmpty()) {
            identificacionLoteNormalizada = null;
        }
        return ingresoInsumoRepository.filtrarIngresoInsumo(idInsumo, identificacionLoteNormalizada, estado, tipoIngreso, fechaIngreso, pageable)
                .map(MapperIngresoInsumo::toDTO);
    }

    /**
     * Busca y retorna un ingreso de insumo específico mediante su identificador único.
     *
     * @param id El ID del ingreso de insumo.
     * @return El ingreso de insumo correspondiente al ID.
     * @throws ReglaNegocioException Si el ID es nulo.
     * @throws RecursoNoEncontradoException Si no existe ningún ingreso de insumo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public IngresoInsumoResponseDTO buscarPorId(Long id) {
        if (id == null) {
            throw new ReglaNegocioException("El ingreso de insumo es obligatorio");
        }
        return MapperIngresoInsumo.toDTO(ingresoInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ingreso de insumo con ID: " + id)));
    }

    /**
     * Registra un ingreso de insumo como recepción de un ítem de una orden de compra.
     * <p>
     * Bloquea la fila del ítem y la de su orden de compra ({@code PESSIMISTIC_WRITE}), de modo que el ingreso no se
     * cruza con una anulación o una finalización de la orden. Si con este ingreso todos los ítems de la orden
     * quedan completamente recibidos, la orden se finaliza automáticamente: pasa a {@code FINALIZADA}, con la fecha
     * de finalización y sin motivo (el motivo solo existe en la finalización forzada).
     * </p>
     *
     * @param ingresoInsumoPorCompraFormDTO Los datos del ingreso a registrar. La cantidad se informa en unidades del
     *                                      ítem (ej. 2 bolsas); el service la convierte a la unidad del insumo.
     * @return El ingreso de insumo registrado.
     * @throws ReglaNegocioException Si faltan los datos del ingreso o el ítem de la orden, si la cantidad de unidades recibidas no fue informada o es menor o igual a cero, si la fecha de ingreso no fue informada o es posterior a la fecha actual, si la fecha de vencimiento no fue informada o es anterior a la fecha actual, si la orden de compra del ítem no está en estado {@code PENDIENTE}, si el insumo del ítem está dado de baja, si las unidades recibidas superan las unidades pendientes de entrega del ítem, o si ya existe un lote del insumo con la misma identificación de lote de proveedor pero una fecha de vencimiento distinta, o si otra persona registró el mismo lote al mismo tiempo.
     * @throws RecursoNoEncontradoException Si el ítem de detalle de compra referenciado o su orden de compra no existen.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.INGRESO_INSUMO)
    public IngresoInsumoResponseDTO registrarIngresoInsumoPorCompra(IngresoInsumoPorCompraFormDTO ingresoInsumoPorCompraFormDTO) {
        // 1. Validar los datos del formulario antes de consultar ningún repositorio
        if (ingresoInsumoPorCompraFormDTO == null) {
            throw new ReglaNegocioException("Los datos del ingreso de insumo son obligatorios");
        }
        if (ingresoInsumoPorCompraFormDTO.getIdDetalleCompra() == null) {
            throw new ReglaNegocioException("El ítem de la orden de compra es obligatorio", "idDetalleCompra");
        }
        Integer cantidadPresentaciones = ingresoInsumoPorCompraFormDTO.getCantidadPresentaciones();
        validarCantidadPresentaciones(cantidadPresentaciones);
        LocalDate fechaIngreso = ingresoInsumoPorCompraFormDTO.getFechaIngreso();
        validarFechaIngreso(fechaIngreso);
        LocalDate fechaVencimiento = ingresoInsumoPorCompraFormDTO.getFechaVencimiento();
        validarFechaVencimiento(fechaVencimiento);

        // 2. Localizar, bloqueándolo para escritura, el ítem de detalle de compra solicitado: evita
        //    que dos ingresos concurrentes sobre el mismo ítem calculen la misma cantidad pendiente
        //    y, juntos, terminen recibiendo más de lo solicitado
        DetalleCompraEntity detalleCompraEntity = detalleCompraRepository.buscarPorIdParaIngresar(ingresoInsumoPorCompraFormDTO.getIdDetalleCompra())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ítem de detalle de compra con ID: " + ingresoInsumoPorCompraFormDTO.getIdDetalleCompra(), "idDetalleCompra"));

        // 3. Localizar, bloqueándola para escritura, la orden de compra de ese ítem y validar que se encuentre en
        //    estado PENDIENTE: una anulación o una finalización simultáneas de la orden esperan a este ingreso
        OrdenCompraEntity ordenCompraEntity = ordenCompraRepository.buscarPorIdParaCambiarEstado(detalleCompraEntity.getOrdenCompra().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + detalleCompraEntity.getOrdenCompra().getId()));
        if (ordenCompraEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden registrar ingresos de ítems de órdenes de compra en estado Pendiente");
        }

        // 4. Validar que el insumo del ítem no esté dado de baja
        if (detalleCompraEntity.getCatalogoProveedor().getInsumo().getEstado() == Estado.BAJA) {
            throw new ReglaNegocioException("No se puede registrar el ingreso porque el insumo está dado de baja");
        }

        // 5. Validar que las unidades recibidas no superen las unidades pendientes de entrega del ítem.
        //    El pendiente se redondea a la escala de las cantidades: el mismo criterio con el que se decide si la orden
        //    quedó completamente recibida, para que el residuo de la coma flotante (0,1 + 0,2) no rechace un ingreso exacto
        double presentacionesPendientes = calcularPresentacionesPendientes(detalleCompraEntity);
        if (cantidadPresentaciones > presentacionesPendientes) {
            throw new ReglaNegocioException("La cantidad de unidades recibidas no puede superar la cantidad pendiente de entrega del ítem ("
                    + ValidacionesNumericas.formatearNumero(presentacionesPendientes) + ")", "cantidadPresentaciones");
        }

        // La cantidad que ingresa al lote y al ingreso es la de insumo, en su unidad: unidades por contenido de cada una
        double cantidadRecibida = CantidadesDetalleCompra.convertirPresentacionesAUnidadDelInsumo(detalleCompraEntity, cantidadPresentaciones);

        // 6. Resolver el insumo del ítem y validar/localizar el lote de insumo correspondiente,
        //    sin mutarlo todavía: si hay conflicto de vencimiento, esto lanza antes de tocar nada
        InsumoEntity insumoEntity = detalleCompraEntity.getCatalogoProveedor().getInsumo();
        LoteInsumoEntity loteInsumoEntity = resolverLoteInsumo(insumoEntity, ingresoInsumoPorCompraFormDTO.getIdentificacionLoteProveedor(), fechaVencimiento);

        // 7. Recién si todas las validaciones pasaron, sumar la cantidad recibida al lote y
        //    persistirlo
        // El costo del ítem es por presentación: el ingreso y el costo promedio del lote son por unidad del insumo
        BigDecimal costoPorUnidadDelInsumo = CantidadesDetalleCompra.calcularCostoPorUnidadDelInsumo(detalleCompraEntity);
        loteInsumoEntity = sumarIngresoAlLote(loteInsumoEntity, cantidadRecibida, costoPorUnidadDelInsumo);

        // 8. Construir y persistir el ingreso de insumo
        IngresoInsumoEntity ingresoInsumoEntity = IngresoInsumoEntity.builder()
                .fechaIngreso(fechaIngreso)
                .cantidadRecibida(cantidadRecibida)
                .costoUnitario(costoPorUnidadDelInsumo)
                .tipoIngreso(TipoIngreso.COMPRA)
                .estado(EstadoTransaccion.REGISTRADO)
                .detalleCompra(detalleCompraEntity)
                .loteInsumo(loteInsumoEntity)
                .build();
        IngresoInsumoEntity ingresoGuardado = ingresoInsumoRepository.save(ingresoInsumoEntity);

        // 9. Si con este ingreso todos los ítems de la orden quedaron completamente recibidos, finalizar la orden
        //    automáticamente: sin motivo, porque el motivo solo existe en la finalización forzada
        if (ordenCompraCompletamenteRecibida(ordenCompraEntity)) {
            ordenCompraEntity.setEstado(EstadoSolicitud.FINALIZADA);
            ordenCompraEntity.setFechaFinalizacion(LocalDateTime.now());
            ordenCompraEntity.setMotivoFinalizacion(null);
            ordenCompraRepository.save(ordenCompraEntity);
        }

        return MapperIngresoInsumo.toDTO(ingresoGuardado);
    }

    /**
     * Registra un ingreso de insumo sin una orden de compra previa (stock inicial, donación, etc.).
     *
     * @param ingresoInsumoDirectoFormDTO Los datos del ingreso a registrar.
     * @return El ingreso de insumo registrado.
     * @throws ReglaNegocioException Si faltan los datos del ingreso o el insumo, si la cantidad recibida no fue informada, es menor o igual a cero, supera 1.000.000 o tiene más de 4 decimales, si el costo unitario no fue informado, es menor o igual a cero, supera 1.000.000.000 o tiene más de 4 decimales, si la fecha de ingreso no fue informada o es posterior a la fecha actual, si la fecha de vencimiento no fue informada o es anterior a la fecha actual, o si ya existe un lote del insumo con la misma identificación de lote de proveedor pero una fecha de vencimiento distinta, o si otra persona registró el mismo lote al mismo tiempo.
     * @throws RecursoNoEncontradoException Si el insumo referenciado no existe (o no está activo).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.INGRESO_INSUMO)
    public IngresoInsumoResponseDTO registrarIngresoInsumoDirecto(IngresoInsumoDirectoFormDTO ingresoInsumoDirectoFormDTO) {
        // 1. Validar los datos del formulario antes de consultar ningún repositorio
        if (ingresoInsumoDirectoFormDTO == null) {
            throw new ReglaNegocioException("Los datos del ingreso de insumo son obligatorios");
        }
        if (ingresoInsumoDirectoFormDTO.getIdInsumo() == null) {
            throw new ReglaNegocioException("El insumo es obligatorio", "idInsumo");
        }
        Double cantidadRecibida = ingresoInsumoDirectoFormDTO.getCantidadRecibida();
        validarCantidadRecibida(cantidadRecibida);
        validarCostoUnitario(ingresoInsumoDirectoFormDTO.getCostoUnitario());
        LocalDate fechaIngreso = ingresoInsumoDirectoFormDTO.getFechaIngreso();
        validarFechaIngreso(fechaIngreso);
        LocalDate fechaVencimiento = ingresoInsumoDirectoFormDTO.getFechaVencimiento();
        validarFechaVencimiento(fechaVencimiento);

        // 2. Validar que el insumo a ingresar esté registrado (y activo)
        InsumoEntity insumoEntity = insumoRepository.findById(ingresoInsumoDirectoFormDTO.getIdInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el insumo con ID: " + ingresoInsumoDirectoFormDTO.getIdInsumo(), "idInsumo"));

        // 3. Validar/localizar el lote de insumo correspondiente, sin mutarlo todavía
        LoteInsumoEntity loteInsumoEntity = resolverLoteInsumo(insumoEntity, ingresoInsumoDirectoFormDTO.getIdentificacionLoteProveedor(), fechaVencimiento);

        // 4. Recién si todas las validaciones pasaron, sumar la cantidad recibida al lote y
        //    persistirlo
        loteInsumoEntity = sumarIngresoAlLote(loteInsumoEntity, cantidadRecibida, ingresoInsumoDirectoFormDTO.getCostoUnitario());

        // 5. Construir y persistir el ingreso de insumo, y retornar el DTO de respuesta correspondiente
        IngresoInsumoEntity ingresoInsumoEntity = IngresoInsumoEntity.builder()
                .fechaIngreso(fechaIngreso)
                .cantidadRecibida(cantidadRecibida)
                .costoUnitario(ingresoInsumoDirectoFormDTO.getCostoUnitario())
                .tipoIngreso(TipoIngreso.DIRECTO)
                .estado(EstadoTransaccion.REGISTRADO)
                .loteInsumo(loteInsumoEntity)
                .build();

        return MapperIngresoInsumo.toDTO(ingresoInsumoRepository.save(ingresoInsumoEntity));
    }

    /**
     * Anula un ingreso de insumo existente en el sistema.
     * <p>
     * No existe la baja lógica para este registro: un ingreso solo puede pasar de
     * {@code REGISTRADO} a {@code ANULADO}, nunca eliminarse.
     * </p>
     * <p>
     * Si el ingreso es de un ítem de una orden de compra, bloquea la fila de la orden ({@code PESSIMISTIC_WRITE}).
     * Si la orden fue finalizada automáticamente (sin motivo de finalización) y, tras la anulación, algún ítem
     * vuelve a tener cantidad pendiente, la orden se reabre: vuelve a {@code PENDIENTE} y pierde la fecha de
     * finalización. Si la orden fue finalizada manualmente (con motivo), el ingreso no se puede anular.
     * </p>
     *
     * @param id El ID del ingreso de insumo a anular.
     * @param anularIngresoInsumoFormDTO Los datos de la anulación (motivo).
     * @return El ingreso de insumo anulado.
     * @throws ReglaNegocioException Si el ID o los datos de la anulación no fueron informados, si el motivo de anulación no fue informado, si el ingreso no se encuentra en estado {@code REGISTRADO}, si pertenece a una orden de compra finalizada manualmente, o si su cantidad recibida supera la cantidad disponible del lote de insumo.
     * @throws RecursoNoEncontradoException Si el ingreso de insumo con el ID especificado no existe, o si su lote de insumo o su orden de compra asociados no existen.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.INGRESO_INSUMO)
    public IngresoInsumoResponseDTO anularIngresoInsumo(Long id, AnularIngresoInsumoFormDTO anularIngresoInsumoFormDTO) {
        // 1. Validar que se hayan informado el ingreso y el motivo de anulación
        if (id == null) {
            throw new ReglaNegocioException("El ingreso de insumo es obligatorio");
        }
        if (anularIngresoInsumoFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la anulación del ingreso de insumo son obligatorios");
        }
        String motivoAnulacion = NormalizacionTexto.nombrePropio(anularIngresoInsumoFormDTO.getMotivoAnulacion());
        if (motivoAnulacion == null || motivoAnulacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoAnulacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de anulación");

        // 2. Localizar el ingreso de insumo bloqueando su fila antes de validar su estado: una segunda anulación del
        //    mismo ingreso espera a la primera y luego lo encuentra ANULADO. Si no existe, se dispara RecursoNoEncontradoException
        IngresoInsumoEntity ingresoInsumoEntity = ingresoInsumoRepository.buscarPorIdParaAnular(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ingreso de insumo con ID: " + id));

        // 3. Validar que el ingreso se encuentre en estado REGISTRADO
        if (ingresoInsumoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular ingresos de insumo en estado Registrado");
        }

        // 4. Si el ingreso es de un ítem de una orden de compra, bloquear la fila de la orden para escritura antes de
        //    tocar el lote: una finalización o una anulación simultáneas de la orden esperan a esta anulación
        OrdenCompraEntity ordenCompraEntity = null;
        if (ingresoInsumoEntity.getDetalleCompra() != null) {
            Long idOrdenCompra = ingresoInsumoEntity.getDetalleCompra().getOrdenCompra().getId();
            ordenCompraEntity = ordenCompraRepository.buscarPorIdParaCambiarEstado(idOrdenCompra)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + idOrdenCompra));
            // Una orden finalizada manualmente (con motivo) es un cierre deliberado: sus ingresos no se anulan
            if (ordenCompraEntity.getEstado() == EstadoSolicitud.FINALIZADA && ordenCompraEntity.getMotivoFinalizacion() != null) {
                throw new ReglaNegocioException("No se puede anular el ingreso porque la orden de compra fue finalizada manualmente");
            }
        }

        // 5. Bloquear el lote de insumo asociado para escritura, y validar que la cantidad del
        //    ingreso a anular no supere la cantidad disponible del lote (parte de esa cantidad
        //    puede ya haber sido reservada o consumida)
        LoteInsumoEntity loteInsumoEntity = loteInsumoRepository.buscarPorIdParaAnularIngreso(ingresoInsumoEntity.getLoteInsumo().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + ingresoInsumoEntity.getLoteInsumo().getId()));
        if (ingresoInsumoEntity.getCantidadRecibida() > loteInsumoEntity.getCantidadDisponible()) {
            throw new ReglaNegocioException("No se puede anular el ingreso: su cantidad supera la cantidad disponible del lote de insumo");
        }

        // 6. Recién si todas las validaciones pasaron, descontar la cantidad anulada del lote y
        //    persistirlo
        loteInsumoEntity.anularIngreso(ingresoInsumoEntity.getCantidadRecibida(), ingresoInsumoEntity.getCostoUnitario());
        loteInsumoRepository.save(loteInsumoEntity);

        // 7. Aplicar la anulación sobre el ingreso y persistirlo
        ingresoInsumoEntity.setEstado(EstadoTransaccion.ANULADO);
        ingresoInsumoEntity.setFechaAnulacion(LocalDateTime.now());
        ingresoInsumoEntity.setMotivoAnulacion(motivoAnulacion);
        IngresoInsumoEntity ingresoGuardado = ingresoInsumoRepository.save(ingresoInsumoEntity);

        // 8. Si la orden fue finalizada automáticamente (sin motivo) y la anulación dejó algún ítem con cantidad
        //    pendiente, reabrirla. Una finalización manual (con motivo) ya se rechazó antes. Que el motivo nulo
        //    identifique el cierre automático es un invariante: finalizarOrdenCompra exige siempre un motivo no vacío
        if (ordenCompraEntity != null
                && ordenCompraEntity.getEstado() == EstadoSolicitud.FINALIZADA
                && ordenCompraEntity.getMotivoFinalizacion() == null
                && !ordenCompraCompletamenteRecibida(ordenCompraEntity)) {
            ordenCompraEntity.setEstado(EstadoSolicitud.PENDIENTE);
            ordenCompraEntity.setFechaFinalizacion(null);
            ordenCompraRepository.save(ordenCompraEntity);
        }

        return MapperIngresoInsumo.toDTO(ingresoGuardado);
    }

    /**
     * Valida que la fecha de ingreso haya sido informada y no sea posterior a la fecha actual.
     *
     * @param fechaIngreso Fecha de ingreso a validar.
     * @throws ReglaNegocioException Si la fecha de ingreso es nula o posterior a la fecha actual.
     */
    private static void validarFechaIngreso(LocalDate fechaIngreso) {
        if (fechaIngreso == null) {
            throw new ReglaNegocioException("La fecha de ingreso es obligatoria", "fechaIngreso");
        }
        if (fechaIngreso.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de ingreso no puede ser posterior a la fecha actual", "fechaIngreso");
        }
    }

    /**
     * Valida que la fecha de vencimiento haya sido informada y no sea anterior a la fecha actual.
     *
     * @param fechaVencimiento Fecha de vencimiento a validar.
     * @throws ReglaNegocioException Si la fecha de vencimiento es nula o anterior a la fecha actual.
     */
    private static void validarFechaVencimiento(LocalDate fechaVencimiento) {
        if (fechaVencimiento == null) {
            throw new ReglaNegocioException("La fecha de vencimiento es obligatoria", "fechaVencimiento");
        }
        if (fechaVencimiento.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de vencimiento no puede ser anterior a la fecha actual", "fechaVencimiento");
        }
    }

    /**
     * Valida la cantidad de unidades recibidas de un ingreso por compra: informada y mayor a cero. El máximo lo
     * fija lo que resta recibir del ítem.
     *
     * @param cantidadPresentaciones Cantidad de unidades a validar.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero.
     */
    private static void validarCantidadPresentaciones(Integer cantidadPresentaciones) {
        if (cantidadPresentaciones == null) {
            throw new ReglaNegocioException("La cantidad de unidades recibidas es obligatoria", "cantidadPresentaciones");
        }
        if (cantidadPresentaciones <= 0) {
            throw new ReglaNegocioException("La cantidad de unidades recibidas debe ser mayor a cero", "cantidadPresentaciones");
        }
    }

    /**
     * Valida la cantidad recibida de un ingreso, expresada en la unidad de medida del insumo: informada, un número
     * finito mayor a cero, que no supere {@link LimitesTecnicos#CANTIDAD_INGRESO_MAXIMA} y con no más de
     * {@link LimitesTecnicos#ESCALA_CANTIDADES} decimales.
     *
     * @param cantidadRecibida Cantidad recibida a validar.
     * @throws ReglaNegocioException Si la cantidad es nula, NaN, infinita, menor o igual a cero, supera el máximo o tiene más decimales que la escala.
     */
    private static void validarCantidadRecibida(Double cantidadRecibida) {
        if (cantidadRecibida == null) {
            throw new ReglaNegocioException("La cantidad recibida es obligatoria", "cantidadRecibida");
        }
        if (!Double.isFinite(cantidadRecibida) || cantidadRecibida <= 0) {
            throw new ReglaNegocioException("La cantidad recibida debe ser mayor a cero", "cantidadRecibida");
        }
        if (cantidadRecibida > LimitesTecnicos.CANTIDAD_INGRESO_MAXIMA) {
            throw new ReglaNegocioException("La cantidad recibida no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CANTIDAD_INGRESO_MAXIMA), "cantidadRecibida");
        }
        ValidacionesNumericas.validarMaximoDecimales(cantidadRecibida, LimitesTecnicos.ESCALA_CANTIDADES, "La cantidad recibida");
    }

    /**
     * Valida el costo unitario de un ingreso directo: informado, mayor a cero, que no supere
     * {@link LimitesTecnicos#COSTO_UNITARIO_COMPRA_MAXIMO} y con no más de {@link LimitesTecnicos#ESCALA_COSTO_UNITARIO} decimales.
     *
     * @param costoUnitario Costo unitario a validar.
     * @throws ReglaNegocioException Si el costo es nulo, menor o igual a cero, supera el máximo o tiene más decimales que la escala.
     */
    private static void validarCostoUnitario(BigDecimal costoUnitario) {
        if (costoUnitario == null) {
            throw new ReglaNegocioException("El costo unitario es obligatorio", "costoUnitario");
        }
        if (costoUnitario.signum() <= 0) {
            throw new ReglaNegocioException("El costo unitario debe ser mayor a cero", "costoUnitario");
        }
        if (costoUnitario.compareTo(BigDecimal.valueOf(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO)) > 0) {
            throw new ReglaNegocioException("El costo unitario no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO), "costoUnitario");
        }
        if (costoUnitario.stripTrailingZeros().scale() > LimitesTecnicos.ESCALA_COSTO_UNITARIO) {
            throw new ReglaNegocioException("El costo unitario no puede tener más de "
                    + LimitesTecnicos.ESCALA_COSTO_UNITARIO + " decimales", "costoUnitario");
        }
    }

    /**
     * Calcula la cantidad pendiente de entrega de un ítem de detalle de compra, en la unidad de
     * medida base del insumo: la cantidad total solicitada (unidades de la presentación comercial
     * pedidas, convertida a la unidad del insumo) menos la ya recibida mediante ingresos en
     * estado {@code REGISTRADO}.
     * El cálculo es el de {@link CantidadesDetalleCompra#calcularCantidadPendiente}, compartido con el detalle de la orden de compra.
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @return Cantidad pendiente de entrega, en la unidad de medida base del insumo.
     */
    private double calcularCantidadPendiente(DetalleCompraEntity detalleCompraEntity) {
        double cantidadYaRecibida = CantidadesDetalleCompra.sumarCantidadRecibida(
                ingresoInsumoRepository.findByDetalleCompraIdAndEstado(detalleCompraEntity.getId(), EstadoTransaccion.REGISTRADO));
        return CantidadesDetalleCompra.calcularCantidadPendiente(detalleCompraEntity, cantidadYaRecibida);
    }

    /**
     * Calcula las unidades que restan recibir de un ítem de detalle de compra: las pedidas menos las ya recibidas
     * mediante ingresos en estado {@code REGISTRADO}, estas últimas convertidas desde la unidad del insumo.
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @return Unidades pendientes de entrega.
     */
    private double calcularPresentacionesPendientes(DetalleCompraEntity detalleCompraEntity) {
        double cantidadYaRecibida = CantidadesDetalleCompra.sumarCantidadRecibida(
                ingresoInsumoRepository.findByDetalleCompraIdAndEstado(detalleCompraEntity.getId(), EstadoTransaccion.REGISTRADO));
        return CantidadesDetalleCompra.calcularPresentacionesPendientes(detalleCompraEntity, cantidadYaRecibida);
    }

    /**
     * Indica si todos los ítems de una orden de compra están completamente recibidos: ninguno tiene cantidad
     * pendiente de entrega (redondeada a la escala de las cantidades), contando los ingresos {@code REGISTRADO}
     * persistidos hasta el momento. Una orden sin ítems no se considera recibida.
     *
     * @param ordenCompraEntity Orden de compra a evaluar.
     * @return {@code true} si todos sus ítems están completamente recibidos, {@code false} en caso contrario.
     */
    private boolean ordenCompraCompletamenteRecibida(OrdenCompraEntity ordenCompraEntity) {
        return !ordenCompraEntity.getDetallesCompra().isEmpty()
                && ordenCompraEntity.getDetallesCompra().stream()
                .allMatch(detalleCompraEntity -> Redondeo.redondear(calcularCantidadPendiente(detalleCompraEntity)) <= 0);
    }

    /**
     * Suma la cantidad recibida a un lote de insumo (nuevo o existente), actualiza su PPP y lo persiste.
     *
     * @param loteInsumoEntity Lote de insumo a actualizar.
     * @param cantidadRecibida Cantidad a sumar.
     * @param costoUnitario Costo unitario al que se recibió esta cantidad.
     * @return El lote de insumo persistido.
     */
    private LoteInsumoEntity sumarIngresoAlLote(LoteInsumoEntity loteInsumoEntity, double cantidadRecibida, BigDecimal costoUnitario) {
        loteInsumoEntity.sumarIngreso(cantidadRecibida, costoUnitario);
        String identificacionLoteProveedor = loteInsumoEntity.getIdentificacionLoteProveedor();
        return GuardadoUnico.guardar(loteInsumoRepository, loteInsumoEntity, Map.of(
                INDICE_LOTE_INSUMO, () -> loteRegistradoEnSimultaneo(identificacionLoteProveedor)));
    }

    /**
     * Resuelve el lote de insumo al que debe imputarse un ingreso: si ya existe un lote del
     * insumo con la misma identificación de lote de proveedor, lo reutiliza (validando que la
     * fecha de vencimiento coincida); si no existe, construye uno nuevo sin persistirlo todavía.
     * <p>
     * La identificación se guarda tal como figura en el envase del proveedor (conserva las mayúsculas; se quitan
     * los espacios de más) y se compara sin distinguir mayúsculas, de modo que {@code lote 12} y {@code LOTE  12} son
     * el mismo lote. Antes de buscarlo se bloquea la fila del
     * insumo ({@code PESSIMISTIC_WRITE}): los ingresos simultáneos del mismo insumo se ejecutan uno a uno, y el
     * segundo encuentra el lote que creó el primero en lugar de intentar crearlo otra vez.
     * </p>
     *
     * @param insumoEntity Insumo del lote.
     * @param identificacionLoteProveedor Identificación del lote asignada por el proveedor.
     * @param fechaVencimiento Fecha de vencimiento informada en el ingreso.
     * @return El lote de insumo a utilizar (existente o recién construido).
     * @throws ReglaNegocioException Si el insumo está dado de baja, si la identificación de lote de proveedor falta, supera los 255 caracteres, o si ya existe un lote con esa identificación de lote de proveedor pero con una fecha de vencimiento distinta.
     * @throws RecursoNoEncontradoException Si el insumo ya no existe.
     */
    private LoteInsumoEntity resolverLoteInsumo(InsumoEntity insumoEntity, String identificacionLoteProveedor, LocalDate fechaVencimiento) {
        String identificacion = NormalizacionTexto.nombrePropio(identificacionLoteProveedor);
        if (identificacion == null || identificacion.isEmpty()) {
            throw new ReglaNegocioException("La identificación del lote del proveedor es obligatoria", "identificacionLoteProveedor");
        }
        ValidacionesNumericas.validarLongitudMaxima(identificacion, LimitesTecnicos.LONGITUD_MAXIMA_IDENTIFICACION_LOTE,
                "La identificación del lote del proveedor", "identificacionLoteProveedor");

        InsumoEntity insumoBloqueado = insumoRepository.buscarPorIdParaIngresar(insumoEntity.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el insumo con ID: " + insumoEntity.getId()));
        // El estado se vuelve a validar sobre el insumo bloqueado: una baja concurrente no se ve en el chequeo previo
        if (insumoBloqueado.getEstado() == Estado.BAJA) {
            throw new ReglaNegocioException("No se puede registrar el ingreso porque el insumo está dado de baja");
        }

        return loteInsumoRepository.buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(insumoBloqueado.getId(), identificacion)
                .map(loteInsumoEntity -> {
                    if (!loteInsumoEntity.getFechaVencimiento().equals(fechaVencimiento)) {
                        throw new ReglaNegocioException("Ya existe un lote del insumo con la identificación de lote de proveedor '"
                                + identificacion + "' pero con una fecha de vencimiento distinta", "fechaVencimiento");
                    }
                    return loteInsumoEntity;
                })
                .orElseGet(() -> LoteInsumoEntity.builder()
                        .identificacionLoteProveedor(identificacion)
                        .fechaVencimiento(fechaVencimiento)
                        .insumo(insumoBloqueado)
                        .build());
    }

    private static ReglaNegocioException loteRegistradoEnSimultaneo(String identificacionLoteProveedor) {
        return new ReglaNegocioException("El lote del insumo con la identificación de lote de proveedor '" + identificacionLoteProveedor
                + "' fue registrado por otra persona al mismo tiempo, intente nuevamente", "identificacionLoteProveedor");
    }
}
