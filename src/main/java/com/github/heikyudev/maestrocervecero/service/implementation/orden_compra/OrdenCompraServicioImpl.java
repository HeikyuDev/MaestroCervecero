package com.github.heikyudev.maestrocervecero.service.implementation.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.OrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_produccion.IOrdenProduccionRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class OrdenCompraServicioImpl implements IOrdenCompraServicio {

    private final IOrdenCompraRepository ordenCompraRepository;
    private final IOrdenProduccionRepository ordenProduccionRepository;
    private final IProveedorRepository proveedorRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;

    /**
     * Recupera una página de órdenes de compra activas registradas en el sistema.
     * <p>
     * Las órdenes de compra eliminadas lógicamente son excluidas automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link OrdenCompraResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrdenCompraResponseDTO> buscarTodos(Pageable pageable) {
        return ordenCompraRepository.findAll(pageable).map(MapperOrdenCompra::toDTO);
    }

    /**
     * Busca y retorna una orden de compra específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la orden de compra buscada.
     * @return Objeto {@link OrdenCompraResponseDTO} con la información de la orden de compra encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna orden de compra activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public OrdenCompraResponseDTO buscarPorId(Long id) {
        return MapperOrdenCompra.toDTO(ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + id)));
    }

    /**
     * Registra una nueva orden de compra en el sistema.
     * <p>
     * La compra está siempre impulsada por una orden de producción pendiente: solo se pueden
     * solicitar ítems del catálogo del proveedor seleccionado cuyo insumo forme parte de la
     * versión de receta asociada a esa orden de producción. El costo unitario de cada ítem se
     * define recién en esta transacción; el catálogo del proveedor no fija precio.
     * </p>
     *
     * @param ordenCompraFormDTO Objeto DTO que contiene los datos de creación de la orden de compra.
     * @return {@link OrdenCompraResponseDTO} representativo de la orden de compra guardada en la base de datos, en estado {@code PENDIENTE}.
     * @throws ReglaNegocioException Si la fecha de entrega estimada no fue informada o es anterior a la fecha actual, si algún ítem tiene cantidad o costo unitario nulo o menor o igual a cero, si la orden de producción no se encuentra en estado {@code PENDIENTE}, si algún ítem del catálogo no pertenece al proveedor seleccionado, o si el insumo de algún ítem no forma parte de la versión de receta de la orden de producción.
     * @throws RecursoNoEncontradoException Si la orden de producción, el proveedor o algún ítem del catálogo referenciado no existen.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_COMPRA)
    public OrdenCompraResponseDTO registrarOrdenCompra(OrdenCompraFormDTO ordenCompraFormDTO) {
        // 1. Validar que la fecha de entrega estimada no sea anterior a la fecha actual
        LocalDate fechaEntregaEstimada = ordenCompraFormDTO.getFechaEntregaEstimada();
        if (fechaEntregaEstimada == null || fechaEntregaEstimada.isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de entrega estimada no puede ser anterior a la fecha actual");
        }

        // 2. Validar que cada ítem tenga cantidad y costo unitario mayores a cero
        List<DetalleCompraFormDTO> detallesCompraFormDTO = ordenCompraFormDTO.getDetallesCompra();
        detallesCompraFormDTO.forEach(this::validarCantidadYCosto);

        // 3. Localizar la orden de producción y validar que se encuentre en estado PENDIENTE
        OrdenProduccionEntity ordenProduccionEntity = ordenProduccionRepository.findById(ordenCompraFormDTO.getIdOrdenProduccion())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de producción con ID: " + ordenCompraFormDTO.getIdOrdenProduccion()));
        if (ordenProduccionEntity.getEstado() != EstadoOrden.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden asociar órdenes de compra a órdenes de producción en estado PENDIENTE");
        }

        // 4. Localizar la última versión activa del proveedor seleccionado
        VersionProveedorEntity versionProveedorEntity = obtenerVersionActiva(ordenCompraFormDTO.getIdProveedor());

        // 5. Crear la entidad de la orden de compra a partir del DTO de formulario
        OrdenCompraEntity ordenCompraEntity = OrdenCompraEntity.builder()
                .fechaEntregaEstimada(fechaEntregaEstimada)
                .estado(EstadoOrden.PENDIENTE)
                .ordenProduccion(ordenProduccionEntity)
                .versionProveedor(versionProveedorEntity)
                .build();

        // 6. Construir el detalle de la compra, validando cada ítem contra el proveedor y la receta
        Set<Long> idsInsumosDeReceta = resolverInsumosDeReceta(ordenProduccionEntity.getVersionReceta());
        ordenCompraEntity.setDetallesCompra(construirDetallesCompra(detallesCompraFormDTO, ordenCompraEntity, versionProveedorEntity.getId(), idsInsumosDeReceta));

        // 7. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
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
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, o si la orden no se encuentra en estado {@code PENDIENTE}.
     * @throws RecursoNoEncontradoException Si la orden de compra con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_COMPRA)
    public OrdenCompraResponseDTO anularOrdenCompra(Long id, AnulacionOrdenCompraFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la orden de compra. Si no existe, se dispara RecursoNoEncontradoException
        OrdenCompraEntity ordenCompraEntity = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + id));

        // 3. Validar que la orden se encuentre en estado PENDIENTE
        if (ordenCompraEntity.getEstado() != EstadoOrden.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden anular órdenes de compra en estado PENDIENTE");
        }

        // TODO: Falta módulo de ingresos. Verifica que la orden de compra no tenga asociados ingresos de insumos a su nombre.

        // 4. Aplicar la anulación sobre la entidad administrada por persistencia
        ordenCompraEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());
        ordenCompraEntity.setFechaAnulacion(LocalDateTime.now());
        ordenCompraEntity.setEstado(EstadoOrden.ANULADA);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
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
     * @throws ReglaNegocioException Si el motivo de finalización no fue informado, o si la orden no se encuentra en estado {@code PENDIENTE}.
     * @throws RecursoNoEncontradoException Si la orden de compra con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.FINALIZAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_COMPRA)
    public OrdenCompraResponseDTO finalizarOrdenCompra(Long id, FinalizacionForzadaOrdenCompraFormDTO finalizacionFormDTO) {
        // 1. Validar que se haya informado el motivo de finalización
        if (finalizacionFormDTO.getMotivoFinalizacion() == null || finalizacionFormDTO.getMotivoFinalizacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de finalización es obligatorio");
        }

        // 2. Localizar la orden de compra. Si no existe, se dispara RecursoNoEncontradoException
        OrdenCompraEntity ordenCompraEntity = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de compra con ID: " + id));

        // 3. Validar que la orden se encuentre en estado PENDIENTE
        if (ordenCompraEntity.getEstado() != EstadoOrden.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden finalizar órdenes de compra en estado PENDIENTE");
        }

        // TODO: Falta módulo de ingresos. Verifica que la orden de compra tenga al menos un ingreso de insumos en estado "Registrado";
        //  si no tiene ninguno, corresponde anularla en lugar de finalizarla.

        // 4. Aplicar la finalización forzada sobre la entidad administrada por persistencia
        ordenCompraEntity.setMotivoFinalizacion(finalizacionFormDTO.getMotivoFinalizacion());
        ordenCompraEntity.setFechaFinalizacion(LocalDateTime.now());
        ordenCompraEntity.setEstado(EstadoOrden.FINALIZADA);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperOrdenCompra.toDTO(ordenCompraRepository.save(ordenCompraEntity));
    }

    /**
     * Valida la regla de negocio de cantidad y costo unitario de un ítem del detalle de compra.
     *
     * @param detalleCompraFormDTO Ítem a validar.
     * @throws ReglaNegocioException Si la cantidad o el costo unitario son nulos o menores o iguales a cero.
     */
    private void validarCantidadYCosto(DetalleCompraFormDTO detalleCompraFormDTO) {
        if (detalleCompraFormDTO.getCantidad() == null || detalleCompraFormDTO.getCantidad() <= 0) {
            throw new ReglaNegocioException("La cantidad de cada ítem debe ser mayor a cero");
        }
        if (detalleCompraFormDTO.getCostoUnitario() == null || detalleCompraFormDTO.getCostoUnitario().signum() <= 0) {
            throw new ReglaNegocioException("El costo unitario de cada ítem debe ser mayor a cero");
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
     * seleccionado y que su insumo forme parte de la versión de receta de la orden de producción.
     *
     * @param detallesCompraFormDTO Ítems solicitados en el formulario.
     * @param ordenCompraEntity Orden de compra a la cual se asocia cada detalle construido.
     * @param idProveedor ID del proveedor seleccionado en la orden de compra.
     * @param idsInsumosDeReceta IDs de los insumos que forman parte de la versión de receta de la orden de producción.
     * @return Lista de entidades {@link DetalleCompraEntity} lista para persistir mediante cascada.
     * @throws RecursoNoEncontradoException Si algún ítem del catálogo referenciado no existe.
     * @throws ReglaNegocioException Si algún ítem del catálogo no pertenece al proveedor seleccionado, o si su insumo no forma parte de la versión de receta.
     */
    private List<DetalleCompraEntity> construirDetallesCompra(List<DetalleCompraFormDTO> detallesCompraFormDTO, OrdenCompraEntity ordenCompraEntity, Long idProveedor, Set<Long> idsInsumosDeReceta) {
        List<DetalleCompraEntity> detallesCompraEntity = new ArrayList<>();

        for (DetalleCompraFormDTO detalleCompraFormDTO : detallesCompraFormDTO) {
            CatalogoProveedorEntity catalogoProveedorEntity = catalogoProveedorRepository.findById(detalleCompraFormDTO.getIdCatalogoProveedor())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ítem de catálogo con ID: " + detalleCompraFormDTO.getIdCatalogoProveedor()));

            if (!catalogoProveedorEntity.getVersion().getId().equals(idProveedor)) {
                throw new ReglaNegocioException("El ítem de catálogo con ID " + detalleCompraFormDTO.getIdCatalogoProveedor() + " no pertenece al proveedor seleccionado");
            }

            if (!idsInsumosDeReceta.contains(catalogoProveedorEntity.getInsumo().getId())) {
                throw new ReglaNegocioException("El insumo del ítem de catálogo con ID " + detalleCompraFormDTO.getIdCatalogoProveedor() + " no forma parte de la versión de receta de la orden de producción seleccionada");
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
     * Resuelve la última versión activa ({@code esUltimaVersion = true}) del proveedor indicado.
     *
     * @param idProveedor Identificador del proveedor cuya última versión activa se quiere obtener.
     * @return Entidad de la última versión activa del proveedor.
     * @throws RecursoNoEncontradoException Si el proveedor no existe, o si no tiene ninguna versión activa.
     */
    private VersionProveedorEntity obtenerVersionActiva(Long idProveedor) {
        ProveedorEntity proveedorEntity = proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + idProveedor));

        return proveedorEntity.getVersiones().stream()
                .filter(VersionProveedorEntity::isEsUltimaVersion)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró una versión activa para el proveedor con ID: " + idProveedor));
    }
}
