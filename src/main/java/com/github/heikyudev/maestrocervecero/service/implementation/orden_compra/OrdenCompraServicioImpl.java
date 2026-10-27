package com.github.heikyudev.maestrocervecero.service.implementation.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.AnulacionOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.DetalleCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.FinalizacionForzadaOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.orden_compra.MapperOrdenCompra;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.method.MetodosVersionado;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrdenCompraServicioImpl implements IOrdenCompraServicio {

    private final IOrdenCompraRepository ordenCompraRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IProveedorRepository proveedorRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;
    private final IIngresoInsumoRepository ingresoInsumoRepository;

    /**
     * Filtra las órdenes de compra, opcionalmente por planificación de producción, proveedor,
     * estado y/o fecha de entrega estimada.
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción a filtrar, o {@code null} para no filtrar por ella.
     * @param idProveedor El ID del proveedor a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado de la orden de compra a filtrar, o {@code null} para no filtrar por él.
     * @param fechaEntregaEstimada La fecha de entrega estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link OrdenCompraResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrdenCompraResponseDTO> filtrarOrdenesCompra(Long idPlanificacionProduccion, Long idProveedor, EstadoSolicitud estado, LocalDate fechaEntregaEstimada, Pageable pageable) {
        return ordenCompraRepository.filtrarOrdenesCompra(idPlanificacionProduccion, idProveedor, estado, fechaEntregaEstimada, pageable)
                .map(MapperOrdenCompra::toDTO);
    }

    /**
     * Busca y retorna una orden de compra específica mediante su identificador único.
     * <p>
     * Cada ítem informa la cantidad recibida (ingresos {@code REGISTRADO}) y la cantidad pendiente de entrega,
     * calculadas con la misma regla con la que el ingreso de insumos valida y cierra la orden.
     * </p>
     *
     * @param id Identificador clave primaria de la orden de compra buscada.
     * @return Objeto {@link OrdenCompraResponseDTO} con la información de la orden de compra encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna orden de compra con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public OrdenCompraResponseDTO buscarPorId(Long id) {
        return MapperOrdenCompra.toDTOConCantidades(ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + id)),
                detalleCompraEntity -> CantidadesDetalleCompra.sumarCantidadRecibida(
                        ingresoInsumoRepository.findByDetalleCompraIdAndEstado(detalleCompraEntity.getId(), EstadoTransaccion.REGISTRADO)));
    }

    /**
     * Registra una nueva orden de compra en el sistema.
     * <p>
     * La compra está siempre impulsada por una planificación de producción en estado Pendiente: solo se pueden
     * solicitar ítems del catálogo del proveedor seleccionado cuyo insumo forme parte de la
     * versión de receta asociada a esa planificación de producción. El costo unitario de cada ítem se
     * define recién en esta transacción; el catálogo del proveedor no fija precio.
     * </p>
     * <p>
     * La fecha de entrega estimada no puede ser anterior a la fecha actual ni superar un año desde ella, y la
     * orden tiene entre 1 y {@link LimitesTecnicos#ORDEN_COMPRA_MAXIMO_ITEMS} ítems. Cada ítem solicita hasta
     * {@link LimitesTecnicos#CANTIDAD_COMPRA_MAXIMA} unidades, con un costo unitario mayor a cero, de hasta
     * {@link LimitesTecnicos#ESCALA_COSTO_UNITARIO} decimales y que no supere {@link LimitesTecnicos#COSTO_UNITARIO_COMPRA_MAXIMO}.
     * Se bloquean las filas de la planificación de producción y del proveedor hasta el fin de la transacción: la
     * baja del proveedor y el registro de la orden no se cruzan.
     * </p>
     *
     * @param ordenCompraFormDTO Objeto DTO que contiene los datos de creación de la orden de compra.
     * @return {@link OrdenCompraResponseDTO} representativo de la orden de compra guardada en la base de datos, en estado {@code PENDIENTE}.
     * @throws ReglaNegocioException Si faltan los datos de la orden; si la fecha de entrega estimada no fue informada, es anterior a la fecha actual o supera un año desde ella; si falta la planificación de producción o el proveedor; si la orden no tiene ítems, supera el máximo de ítems, tiene un ítem nulo o sin ítem del catálogo, o repite el mismo ítem del catálogo; si algún ítem tiene cantidad o costo unitario nulo o menor o igual a cero, supera el máximo de cantidad o de costo unitario, o el costo unitario tiene más de 4 decimales; si la planificación de producción no se encuentra en estado {@code PENDIENTE}; si el proveedor está dado de baja; si algún ítem del catálogo no pertenece al proveedor seleccionado; o si el insumo de algún ítem no forma parte de la versión de receta de la planificación de producción.
     * @throws RecursoNoEncontradoException Si la planificación de producción, el proveedor o algún ítem del catálogo referenciado no existen, o si el proveedor no tiene una versión activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_COMPRA)
    public OrdenCompraResponseDTO registrarOrdenCompra(OrdenCompraFormDTO ordenCompraFormDTO) {
        // 1. Validar que se hayan informado los datos de la orden
        if (ordenCompraFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la orden de compra son obligatorios");
        }

        // 2. Validar la fecha de entrega estimada: informada, no anterior a la fecha actual y a no más de un año
        LocalDate fechaEntregaEstimada = ordenCompraFormDTO.getFechaEntregaEstimada();
        if (fechaEntregaEstimada == null) {
            throw new ReglaNegocioException("La fecha de entrega estimada es obligatoria");
        }
        if (fechaEntregaEstimada.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de entrega estimada no puede ser anterior a la fecha actual");
        }
        if (fechaEntregaEstimada.isAfter(LocalDate.now().plusDays(LimitesTecnicos.ORDEN_COMPRA_MAXIMO_DIAS_ENTREGA))) {
            throw new ReglaNegocioException("La fecha de entrega estimada no puede superar un año desde la fecha actual");
        }

        // 3. Validar que se hayan informado la planificación de producción y el proveedor
        if (ordenCompraFormDTO.getIdPlanificacionProduccion() == null) {
            throw new ReglaNegocioException("La planificación de producción es obligatoria");
        }
        if (ordenCompraFormDTO.getIdProveedor() == null) {
            throw new ReglaNegocioException("El proveedor es obligatorio");
        }

        // 4. Validar los ítems: cantidad, ítem del catálogo, cantidad y costo unitario de cada uno, y que no se repita el mismo ítem del catálogo
        List<DetalleCompraFormDTO> detallesCompraFormDTO = ordenCompraFormDTO.getDetallesCompra();
        validarDetallesCompra(detallesCompraFormDTO);

        // 5. Localizar, bloqueándola para escritura, la planificación de producción y validar que se encuentre en estado PENDIENTE
        PlanificacionProduccionEntity planificacionProduccionEntity = planificacionProduccionRepository.buscarPorIdParaActualizar(ordenCompraFormDTO.getIdPlanificacionProduccion())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + ordenCompraFormDTO.getIdPlanificacionProduccion()));
        if (planificacionProduccionEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden asociar órdenes de compra a planificaciones de producción en estado Pendiente");
        }

        // 6. Localizar la última versión activa del proveedor seleccionado, bloqueando su fila
        VersionProveedorEntity versionProveedorEntity = obtenerVersionActiva(ordenCompraFormDTO.getIdProveedor());

        // 7. Crear la entidad de la orden de compra a partir del DTO de formulario
        OrdenCompraEntity ordenCompraEntity = OrdenCompraEntity.builder()
                .fechaEntregaEstimada(fechaEntregaEstimada)
                .estado(EstadoSolicitud.PENDIENTE)
                .planificacionProduccion(planificacionProduccionEntity)
                .versionProveedor(versionProveedorEntity)
                .build();

        // 8. Construir el detalle de la compra, validando cada ítem contra el proveedor y la receta
        Set<Long> idsInsumosDeReceta = resolverInsumosDeReceta(planificacionProduccionEntity.getVersionReceta());
        ordenCompraEntity.setDetallesCompra(construirDetallesCompra(detallesCompraFormDTO, ordenCompraEntity, versionProveedorEntity.getId(), idsInsumosDeReceta));

        // 9. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperOrdenCompra.toDTO(ordenCompraRepository.save(ordenCompraEntity));
    }

    /**
     * Anula una orden de compra existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre órdenes en estado {@code PENDIENTE}:
     * una orden ya finalizada o ya anulada no puede volver a anularse.
     * </p>
     *
     * @param id Identificador clave primaria de la orden de compra a anular.
     * @param anulacionFormDTO DTO que contiene el motivo de la anulación.
     * @return {@link OrdenCompraResponseDTO} representativo de la orden de compra anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la orden no se encuentra en estado {@code PENDIENTE}, o si tiene al menos un ingreso de insumo en estado {@code REGISTRADO} asociado.
     * @throws RecursoNoEncontradoException Si la orden de compra con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_COMPRA)
    public OrdenCompraResponseDTO anularOrdenCompra(Long id, AnulacionOrdenCompraFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        String motivoAnulacion = NormalizacionTexto.nombrePropio(anulacionFormDTO.getMotivoAnulacion());
        if (motivoAnulacion == null || motivoAnulacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoAnulacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de anulación");

        // 2. Localizar la orden de compra bloqueando su fila: un ingreso que se está registrando o anulando sobre la
        //    misma orden no se cruza con este cambio de estado. Si no existe, se dispara RecursoNoEncontradoException
        OrdenCompraEntity ordenCompraEntity = ordenCompraRepository.buscarPorIdParaCambiarEstado(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + id));

        // 3. Validar que la orden se encuentre en estado PENDIENTE
        if (ordenCompraEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden anular órdenes de compra en estado Pendiente");
        }

        // 4. Validar que la orden no tenga ingresos de insumo REGISTRADO asociados: una anulación
        //    corresponde a un error de carga (ítem olvidado, cantidad mal solicitada, etc.), no a una
        //    compra que ya empezó a recibirse
        if (ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(id, EstadoTransaccion.REGISTRADO)) {
            throw new ReglaNegocioException("No se puede anular la orden de compra porque tiene al menos un ingreso de insumo en estado Registrado asociado");
        }

        // 5. Aplicar la anulación sobre la entidad administrada por persistencia
        ordenCompraEntity.setMotivoAnulacion(motivoAnulacion);
        ordenCompraEntity.setFechaAnulacion(LocalDateTime.now());
        ordenCompraEntity.setEstado(EstadoSolicitud.ANULADA);

        // 6. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperOrdenCompra.toDTO(ordenCompraRepository.save(ordenCompraEntity));
    }

    /**
     * Finaliza de forma forzada una orden de compra existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre órdenes en estado {@code PENDIENTE}:
     * una orden ya finalizada o ya anulada no puede volver a finalizarse.
     * </p>
     *
     * @param id Identificador clave primaria de la orden de compra a finalizar.
     * @param finalizacionFormDTO DTO que contiene el motivo de la finalización forzada.
     * @return {@link OrdenCompraResponseDTO} representativo de la orden de compra finalizada.
     * @throws ReglaNegocioException Si el motivo de finalización no fue informado, si la orden no se encuentra en estado {@code PENDIENTE}, o si no tiene ningún ingreso de insumo en estado {@code REGISTRADO} asociado.
     * @throws RecursoNoEncontradoException Si la orden de compra con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.FINALIZAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_COMPRA)
    public OrdenCompraResponseDTO finalizarOrdenCompra(Long id, FinalizacionForzadaOrdenCompraFormDTO finalizacionFormDTO) {
        // 1. Validar que se haya informado el motivo de finalización
        String motivoFinalizacion = NormalizacionTexto.nombrePropio(finalizacionFormDTO.getMotivoFinalizacion());
        if (motivoFinalizacion == null || motivoFinalizacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de finalización es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoFinalizacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de finalización");

        // 2. Localizar la orden de compra bloqueando su fila: un ingreso que se está registrando o anulando sobre la
        //    misma orden no se cruza con este cambio de estado. Si no existe, se dispara RecursoNoEncontradoException
        OrdenCompraEntity ordenCompraEntity = ordenCompraRepository.buscarPorIdParaCambiarEstado(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + id));

        // 3. Validar que la orden se encuentre en estado PENDIENTE
        if (ordenCompraEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden finalizar órdenes de compra en estado Pendiente");
        }

        // 4. Validar que la orden tenga al menos un ingreso de insumo REGISTRADO asociado: la
        //    finalización forzada cierra una orden que sí llegó a recibir algo pero no se puede
        //    completar (cantidad ingresada menor a la solicitada, proveedor discontinuado, etc.);
        //    si no recibió nada, corresponde anularla en lugar de finalizarla
        if (!ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(id, EstadoTransaccion.REGISTRADO)) {
            throw new ReglaNegocioException("No se puede finalizar la orden de compra porque no tiene ningún ingreso de insumo en estado Registrado asociado: corresponde anularla en su lugar");
        }

        // 5. Aplicar la finalización forzada sobre la entidad administrada por persistencia
        ordenCompraEntity.setMotivoFinalizacion(motivoFinalizacion);
        ordenCompraEntity.setFechaFinalizacion(LocalDateTime.now());
        ordenCompraEntity.setEstado(EstadoSolicitud.FINALIZADA);

        // 6. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperOrdenCompra.toDTO(ordenCompraRepository.save(ordenCompraEntity));
    }

    /**
     * Valida los ítems de la orden de compra: debe haber entre 1 y {@link LimitesTecnicos#ORDEN_COMPRA_MAXIMO_ITEMS},
     * ninguno nulo ni sin ítem del catálogo, cada uno con cantidad y costo unitario válidos, y sin repetir el mismo
     * ítem del catálogo.
     *
     * @param detallesCompraFormDTO Ítems solicitados en el formulario.
     * @throws ReglaNegocioException Si no hay ítems, supera el máximo, algún ítem es nulo o no indica el ítem del catálogo, algún ítem tiene cantidad o costo unitario inválidos, o el mismo ítem del catálogo figura más de una vez.
     */
    private void validarDetallesCompra(List<DetalleCompraFormDTO> detallesCompraFormDTO) {
        if (detallesCompraFormDTO == null || detallesCompraFormDTO.isEmpty()) {
            throw new ReglaNegocioException("La orden de compra debe tener al menos un ítem");
        }
        if (detallesCompraFormDTO.size() > LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS) {
            throw new ReglaNegocioException("La orden de compra no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS) + " ítems");
        }
        for (DetalleCompraFormDTO detalleCompraFormDTO : detallesCompraFormDTO) {
            if (detalleCompraFormDTO == null) {
                throw new ReglaNegocioException("Los ítems de la orden de compra son obligatorios");
            }
            if (detalleCompraFormDTO.getIdCatalogoProveedor() == null) {
                throw new ReglaNegocioException("El ítem del catálogo de cada ítem de la orden de compra es obligatorio");
            }
            validarCantidadYCosto(detalleCompraFormDTO);
        }
        validarSinItemsRepetidos(detallesCompraFormDTO);
    }

    /**
     * Valida la regla de negocio de cantidad y costo unitario de un ítem del detalle de compra: ambos deben ser
     * mayores a cero y no superar su máximo, y el costo unitario no puede tener más decimales que la columna de la
     * base de datos.
     *
     * @param detalleCompraFormDTO Ítem a validar.
     * @throws ReglaNegocioException Si la cantidad o el costo unitario son nulos o menores o iguales a cero, superan su máximo, o el costo unitario tiene más de 4 decimales.
     */
    private void validarCantidadYCosto(DetalleCompraFormDTO detalleCompraFormDTO) {
        Integer cantidad = detalleCompraFormDTO.getCantidad();
        if (cantidad == null || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad de cada ítem debe ser mayor a cero");
        }
        if (cantidad > LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA) {
            throw new ReglaNegocioException("La cantidad de cada ítem no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA));
        }
        BigDecimal costoUnitario = detalleCompraFormDTO.getCostoUnitario();
        if (costoUnitario == null || costoUnitario.signum() <= 0) {
            throw new ReglaNegocioException("El costo unitario de cada ítem debe ser mayor a cero");
        }
        if (costoUnitario.compareTo(BigDecimal.valueOf(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO)) > 0) {
            throw new ReglaNegocioException("El costo unitario de cada ítem no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO));
        }
        if (costoUnitario.stripTrailingZeros().scale() > LimitesTecnicos.ESCALA_COSTO_UNITARIO) {
            throw new ReglaNegocioException("El costo unitario de cada ítem no puede tener más de "
                    + LimitesTecnicos.ESCALA_COSTO_UNITARIO + " decimales");
        }
    }

    /**
     * Valida que la orden de compra no repita el mismo ítem del catálogo del proveedor.
     *
     * @param detallesCompraFormDTO Ítems solicitados en el formulario.
     * @throws ReglaNegocioException Si el mismo ítem del catálogo figura más de una vez.
     */
    private void validarSinItemsRepetidos(List<DetalleCompraFormDTO> detallesCompraFormDTO) {
        Set<Long> idsCatalogoVistos = new HashSet<>();
        for (DetalleCompraFormDTO detalleCompraFormDTO : detallesCompraFormDTO) {
            if (!idsCatalogoVistos.add(detalleCompraFormDTO.getIdCatalogoProveedor())) {
                throw new ReglaNegocioException("La orden de compra no puede repetir el mismo ítem del catálogo");
            }
        }
    }

    /**
     * Recolecta los identificadores de todos los insumos (maltas, lúpulos y levaduras)
     * planificados en la versión de receta indicada.
     *
     * @param versionRecetaEntity Versión de receta de la cual extraer los insumos utilizados.
     * @return Conjunto de IDs de insumos que forman parte de la versión de receta.
     */
    private Set<Long> resolverInsumosDeReceta(VersionRecetaEntity versionRecetaEntity) {
        return Stream.of(
                        versionRecetaEntity.getDetallesMalta().stream().map(detalle -> detalle.getMalta().getId()),
                        versionRecetaEntity.getDetallesLupulo().stream().map(detalle -> detalle.getLupulo().getId()),
                        versionRecetaEntity.getDetallesLevadura().stream().map(detalle -> detalle.getLevadura().getId())
                )
                .flatMap(stream -> stream)
                .collect(Collectors.toSet());
    }

    /**
     * Construye el detalle de una orden de compra a partir del DTO de formulario, validando
     * para cada ítem que el catálogo referenciado exista, que pertenezca al proveedor
     * seleccionado y que su insumo forme parte de la versión de receta de la planificación de producción.
     *
     * @param detallesCompraFormDTO Ítems solicitados en el formulario.
     * @param ordenCompraEntity Orden de compra a la cual se asocia cada detalle construido.
     * @param idVersionProveedor ID de la versión activa del proveedor seleccionado en la orden de compra.
     * @param idsInsumosDeReceta IDs de los insumos que forman parte de la versión de receta de la planificación de producción.
     * @return Lista de entidades {@link DetalleCompraEntity} lista para persistir mediante cascada.
     * @throws RecursoNoEncontradoException Si algún ítem del catálogo referenciado no existe.
     * @throws ReglaNegocioException Si algún ítem del catálogo no pertenece al proveedor seleccionado, o si su insumo no forma parte de la versión de receta.
     */
    private List<DetalleCompraEntity> construirDetallesCompra(List<DetalleCompraFormDTO> detallesCompraFormDTO, OrdenCompraEntity ordenCompraEntity, Long idVersionProveedor, Set<Long> idsInsumosDeReceta) {
        List<DetalleCompraEntity> detallesCompraEntity = new ArrayList<>();

        for (DetalleCompraFormDTO detalleCompraFormDTO : detallesCompraFormDTO) {
            CatalogoProveedorEntity catalogoProveedorEntity = catalogoProveedorRepository.findById(detalleCompraFormDTO.getIdCatalogoProveedor())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ítem de catálogo con ID: " + detalleCompraFormDTO.getIdCatalogoProveedor()));

            if (!catalogoProveedorEntity.getVersion().getId().equals(idVersionProveedor)) {
                throw new ReglaNegocioException("El ítem de catálogo con ID " + detalleCompraFormDTO.getIdCatalogoProveedor() + " no pertenece al proveedor seleccionado");
            }

            if (!idsInsumosDeReceta.contains(catalogoProveedorEntity.getInsumo().getId())) {
                throw new ReglaNegocioException("El insumo del ítem de catálogo con ID " + detalleCompraFormDTO.getIdCatalogoProveedor() + " no forma parte de la versión de receta de la planificación de producción seleccionada");
            }

            detallesCompraEntity.add(DetalleCompraEntity.builder()
                    .cantidad(detalleCompraFormDTO.getCantidad())
                    .costoUnitario(detalleCompraFormDTO.getCostoUnitario())
                    .ordenCompra(ordenCompraEntity)
                    .catalogoProveedor(catalogoProveedorEntity)
                    .build());
        }

        return detallesCompraEntity;
    }

    /**
     * Resuelve la última versión activa ({@code esUltimaVersion = true}) del proveedor indicado, bloqueando la fila
     * del proveedor ({@code PESSIMISTIC_WRITE}) hasta el fin de la transacción: la baja del proveedor, que verifica
     * que no tenga órdenes pendientes, no se cruza con el registro de una orden nueva.
     *
     * @param idProveedor Identificador del proveedor cuya última versión activa se quiere obtener.
     * @return Entidad de la última versión activa del proveedor.
     * @throws RecursoNoEncontradoException Si el proveedor no existe, o si no tiene ninguna versión activa.
     * @throws ReglaNegocioException Si el proveedor está dado de baja.
     */
    private VersionProveedorEntity obtenerVersionActiva(Long idProveedor) {
        ProveedorEntity proveedorEntity = proveedorRepository.buscarPorIdParaRegistrarOrdenCompra(idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + idProveedor));
        if (proveedorEntity.getEstado() == Estado.BAJA) {
            throw new ReglaNegocioException("No se puede registrar la orden de compra para un proveedor dado de baja");
        }

        return MetodosVersionado.obtenerVersionActiva(proveedorEntity.getVersiones(),
                () -> new RecursoNoEncontradoException("No se encontró una versión activa para el proveedor con ID: " + idProveedor));
    }
}
