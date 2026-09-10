package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IIngresoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperIngresoInsumo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class IngresoInsumoServicioImpl implements IIngresoInsumoServicio {

    private final IIngresoInsumoRepository ingresoInsumoRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IDetalleCompraRepository detalleCompraRepository;
    private final IInsumoRepository insumoRepository;

    /**
     * Recupera una página de ingresos de insumo registrados en el sistema.
     * <p>
     * A diferencia del resto de los módulos, incluye tanto los ingresos en estado
     * {@code REGISTRADO} como los {@code ANULADO}: la anulación es un cierre excepcional del
     * registro histórico, no una baja lógica que deba ocultarlo de las búsquedas.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Una página de ingresos de insumo en formato DTO.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<IngresoInsumoResponseDTO> buscarTodos(Pageable pageable) {
        return ingresoInsumoRepository.findAll(pageable).map(MapperIngresoInsumo::toDTO);
    }

    /**
     * Busca y retorna un ingreso de insumo específico mediante su identificador único.
     *
     * @param id El ID del ingreso de insumo.
     * @return El ingreso de insumo correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún ingreso de insumo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public IngresoInsumoResponseDTO buscarPorId(Long id) {
        return MapperIngresoInsumo.toDTO(ingresoInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ingreso de insumo con ID: " + id)));
    }

    /**
     * Registra un ingreso de insumo como recepción de un ítem de una orden de compra.
     *
     * @param ingresoInsumoPorCompraFormDTO Los datos del ingreso a registrar.
     * @return El ingreso de insumo registrado.
     * @throws ReglaNegocioException Si la orden de compra del ítem no está en estado {@code PENDIENTE}, si la cantidad recibida es nula, menor o igual a cero, o supera la cantidad pendiente de entrega del ítem, si la fecha de vencimiento no fue informada o es anterior a la fecha actual, o si ya existe un lote del insumo con la misma identificación de lote de proveedor pero una fecha de vencimiento distinta.
     * @throws RecursoNoEncontradoException Si el ítem de detalle de compra referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.INGRESO_INSUMO)
    public IngresoInsumoResponseDTO registrarIngresoInsumoPorCompra(IngresoInsumoPorCompraFormDTO ingresoInsumoPorCompraFormDTO) {
        // 1. Localizar el ítem de detalle de compra solicitado
        DetalleCompraEntity detalleCompraEntity = detalleCompraRepository.findById(ingresoInsumoPorCompraFormDTO.getIdDetalleCompra())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ítem de detalle de compra con ID: " + ingresoInsumoPorCompraFormDTO.getIdDetalleCompra()));

        // 2. Validar que la orden de compra de ese ítem se encuentre en estado PENDIENTE
        if (detalleCompraEntity.getOrdenCompra().getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden registrar ingresos de ítems de órdenes de compra en estado PENDIENTE");
        }

        // 3. Validar que la cantidad recibida sea mayor a cero y no supere la cantidad pendiente de entrega del ítem
        Double cantidadRecibida = ingresoInsumoPorCompraFormDTO.getCantidadRecibida();
        if (cantidadRecibida == null || cantidadRecibida <= 0) {
            throw new ReglaNegocioException("La cantidad recibida debe ser mayor a cero");
        }
        double cantidadPendiente = calcularCantidadPendiente(detalleCompraEntity);
        if (cantidadRecibida > cantidadPendiente) {
            throw new ReglaNegocioException("La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (" + cantidadPendiente + ")");
        }

        // 4. Validar que la fecha de vencimiento no sea anterior a la fecha actual
        LocalDate fechaVencimiento = ingresoInsumoPorCompraFormDTO.getFechaVencimiento();
        if (fechaVencimiento == null || fechaVencimiento.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de vencimiento no puede ser anterior a la fecha actual");
        }

        // 5. Resolver el insumo del ítem y validar/localizar el lote de insumo correspondiente,
        //    sin mutarlo todavía: si hay conflicto de vencimiento, esto lanza antes de tocar nada
        InsumoEntity insumoEntity = detalleCompraEntity.getCatalogoProveedor().getInsumo();
        LoteInsumoEntity loteInsumoEntity = resolverLoteInsumo(insumoEntity, ingresoInsumoPorCompraFormDTO.getIdentificacionLoteProveedor(), fechaVencimiento);

        // 6. Recién si todas las validaciones pasaron, sumar la cantidad recibida al lote y
        //    persistirlo
        loteInsumoEntity = sumarIngresoAlLote(loteInsumoEntity, cantidadRecibida, detalleCompraEntity.getCostoUnitario());

        // 7. Construir y persistir el ingreso de insumo, y retornar el DTO de respuesta correspondiente
        IngresoInsumoEntity ingresoInsumoEntity = IngresoInsumoEntity.builder()
                .fechaIngreso(ingresoInsumoPorCompraFormDTO.getFechaIngreso())
                .cantidadRecibida(cantidadRecibida)
                .costoUnitario(detalleCompraEntity.getCostoUnitario())
                .tipoIngreso(TipoIngreso.COMPRA)
                .estado(EstadoTransaccion.REGISTRADO)
                .detalleCompra(detalleCompraEntity)
                .loteInsumo(loteInsumoEntity)
                .build();

        return MapperIngresoInsumo.toDTO(ingresoInsumoRepository.save(ingresoInsumoEntity));
    }

    /**
     * Registra un ingreso de insumo sin una orden de compra previa (stock inicial, donación, etc.).
     *
     * @param ingresoInsumoDirectoFormDTO Los datos del ingreso a registrar.
     * @return El ingreso de insumo registrado.
     * @throws ReglaNegocioException Si la cantidad recibida o el costo unitario son nulos, menores o iguales a cero, si la fecha de vencimiento no fue informada o es anterior a la fecha actual, o si ya existe un lote del insumo con la misma identificación de lote de proveedor pero una fecha de vencimiento distinta.
     * @throws RecursoNoEncontradoException Si el insumo referenciado no existe (o no está activo).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.INGRESO_INSUMO)
    public IngresoInsumoResponseDTO registrarIngresoInsumoDirecto(IngresoInsumoDirectoFormDTO ingresoInsumoDirectoFormDTO) {
        // 1. Validar que el insumo a ingresar esté registrado (y activo)
        InsumoEntity insumoEntity = insumoRepository.findById(ingresoInsumoDirectoFormDTO.getIdInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el insumo con ID: " + ingresoInsumoDirectoFormDTO.getIdInsumo()));

        // 2. Validar que la cantidad recibida sea mayor a cero
        Double cantidadRecibida = ingresoInsumoDirectoFormDTO.getCantidadRecibida();
        if (cantidadRecibida == null || cantidadRecibida <= 0) {
            throw new ReglaNegocioException("La cantidad recibida debe ser mayor a cero");
        }

        // 3. Validar que el costo unitario sea mayor a cero
        if (ingresoInsumoDirectoFormDTO.getCostoUnitario() == null || ingresoInsumoDirectoFormDTO.getCostoUnitario().signum() <= 0) {
            throw new ReglaNegocioException("El costo unitario debe ser mayor a cero");
        }

        // 4. Validar que la fecha de vencimiento no sea anterior a la fecha actual
        LocalDate fechaVencimiento = ingresoInsumoDirectoFormDTO.getFechaVencimiento();
        if (fechaVencimiento == null || fechaVencimiento.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de vencimiento no puede ser anterior a la fecha actual");
        }

        // 5. Validar/localizar el lote de insumo correspondiente, sin mutarlo todavía
        LoteInsumoEntity loteInsumoEntity = resolverLoteInsumo(insumoEntity, ingresoInsumoDirectoFormDTO.getIdentificacionLoteProveedor(), fechaVencimiento);

        // 6. Recién si todas las validaciones pasaron, sumar la cantidad recibida al lote y
        //    persistirlo
        loteInsumoEntity = sumarIngresoAlLote(loteInsumoEntity, cantidadRecibida, ingresoInsumoDirectoFormDTO.getCostoUnitario());

        // 7. Construir y persistir el ingreso de insumo, y retornar el DTO de respuesta correspondiente
        IngresoInsumoEntity ingresoInsumoEntity = IngresoInsumoEntity.builder()
                .fechaIngreso(ingresoInsumoDirectoFormDTO.getFechaIngreso())
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
     *
     * @param id El ID del ingreso de insumo a anular.
     * @param anularIngresoInsumoFormDTO Los datos de la anulación (motivo).
     * @return El ingreso de insumo anulado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el ingreso no se encuentra en estado {@code REGISTRADO}, o si su cantidad recibida supera la cantidad disponible del lote de insumo.
     * @throws RecursoNoEncontradoException Si el ingreso de insumo con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.INGRESO_INSUMO)
    public IngresoInsumoResponseDTO anularIngresoInsumo(Long id, AnularIngresoInsumoFormDTO anularIngresoInsumoFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anularIngresoInsumoFormDTO.getMotivoAnulacion() == null || anularIngresoInsumoFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar el ingreso de insumo. Si no existe, se dispara RecursoNoEncontradoException
        IngresoInsumoEntity ingresoInsumoEntity = ingresoInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ingreso de insumo con ID: " + id));

        // 3. Validar que el ingreso se encuentre en estado REGISTRADO
        if (ingresoInsumoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular ingresos de insumo en estado REGISTRADO");
        }

        // 4. Validar que la cantidad del ingreso a anular no supere la cantidad disponible del
        //    lote (parte de esa cantidad puede ya haber sido reservada o consumida)
        LoteInsumoEntity loteInsumoEntity = ingresoInsumoEntity.getLoteInsumo();
        if (ingresoInsumoEntity.getCantidadRecibida() > loteInsumoEntity.getCantidadDisponible()) {
            throw new ReglaNegocioException("No se puede anular el ingreso: su cantidad supera la cantidad disponible del lote de insumo");
        }

        // 5. Recién si todas las validaciones pasaron, descontar la cantidad anulada del lote y
        //    persistirlo
        loteInsumoEntity.anularIngreso(ingresoInsumoEntity.getCantidadRecibida(), ingresoInsumoEntity.getCostoUnitario());
        loteInsumoRepository.save(loteInsumoEntity);

        // 6. Aplicar la anulación sobre el ingreso y persistirlo
        ingresoInsumoEntity.setEstado(EstadoTransaccion.ANULADO);
        ingresoInsumoEntity.setFechaAnulacion(LocalDateTime.now());
        ingresoInsumoEntity.setMotivoAnulacion(anularIngresoInsumoFormDTO.getMotivoAnulacion());

        return MapperIngresoInsumo.toDTO(ingresoInsumoRepository.save(ingresoInsumoEntity));
    }

    /**
     * Calcula la cantidad pendiente de entrega de un ítem de detalle de compra: la cantidad
     * solicitada menos la ya recibida mediante ingresos en estado {@code REGISTRADO}.
     *
     * @param detalleCompraEntity Ítem de detalle de compra a evaluar.
     * @return Cantidad pendiente de entrega.
     */
    private double calcularCantidadPendiente(DetalleCompraEntity detalleCompraEntity) {
        double cantidadYaRecibida = ingresoInsumoRepository.findByDetalleCompraIdAndEstado(detalleCompraEntity.getId(), EstadoTransaccion.REGISTRADO)
                .stream()
                .mapToDouble(IngresoInsumoEntity::getCantidadRecibida)
                .sum();
        return detalleCompraEntity.getCantidad() - cantidadYaRecibida;
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
        return loteInsumoRepository.save(loteInsumoEntity);
    }

    /**
     * Resuelve el lote de insumo al que debe imputarse un ingreso: si ya existe un lote del
     * insumo con la misma identificación de lote de proveedor, lo reutiliza (validando que la
     * fecha de vencimiento coincida); si no existe, construye uno nuevo sin persistirlo todavía.
     *
     * @param insumoEntity Insumo del lote.
     * @param identificacionLoteProveedor Identificación del lote asignada por el proveedor.
     * @param fechaVencimiento Fecha de vencimiento informada en el ingreso.
     * @return El lote de insumo a utilizar (existente o recién construido).
     * @throws ReglaNegocioException Si ya existe un lote con esa identificación de lote de proveedor pero con una fecha de vencimiento distinta.
     */
    private LoteInsumoEntity resolverLoteInsumo(InsumoEntity insumoEntity, String identificacionLoteProveedor, LocalDate fechaVencimiento) {
        return loteInsumoRepository.findByInsumoIdAndIdentificacionLoteProveedor(insumoEntity.getId(), identificacionLoteProveedor)
                .map(loteInsumoEntity -> {
                    if (!loteInsumoEntity.getFechaVencimiento().equals(fechaVencimiento)) {
                        throw new ReglaNegocioException("Ya existe un lote del insumo con la identificación de lote de proveedor '"
                                + identificacionLoteProveedor + "' pero con una fecha de vencimiento distinta");
                    }
                    return loteInsumoEntity;
                })
                .orElseGet(() -> LoteInsumoEntity.builder()
                        .identificacionLoteProveedor(identificacionLoteProveedor)
                        .fechaVencimiento(fechaVencimiento)
                        .insumo(insumoEntity)
                        .build());
    }
}
