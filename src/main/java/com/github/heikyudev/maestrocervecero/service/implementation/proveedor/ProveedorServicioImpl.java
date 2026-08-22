package com.github.heikyudev.maestrocervecero.service.implementation.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IPresentacionComercialRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IVersionProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.CatalogoProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.VersionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperProveedor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProveedorServicioImpl implements IProveedorServicio {

    private final IProveedorRepository proveedorRepository;
    private final IVersionProveedorRepository versionProveedorRepository;
    private final ILocalidadRepository localidadRepository;
    private final IInsumoRepository insumoRepository;
    private final IPresentacionComercialRepository presentacionComercialRepository;
    private final IOrdenCompraRepository ordenCompraRepository;

    /**
     * Recupera una página de proveedores activos registrados en el sistema.
     * <p>
     * Los proveedores dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ProveedorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> buscarTodos(Pageable pageable) {
        return proveedorRepository.findAll(pageable).map(MapperProveedor::toDTO);
    }

    /**
     * Busca y retorna un proveedor específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del proveedor buscado.
     * @return Objeto {@link ProveedorResponseDTO} con la información del proveedor encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún proveedor activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public ProveedorResponseDTO buscarPorId(Long id) {
        return MapperProveedor.toDTO(proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id)));
    }

    /**
     * Registra un nuevo proveedor en el sistema junto con su versión inicial.
     * <p>
     * {@link ProveedorEntity} no tiene datos propios: toda la información (razón social, CUIT,
     * datos de contacto, localidad y catálogo de productos) se registra en la primera
     * {@link VersionProveedorEntity}, marcada como {@code esUltimaVersion = true}. Gracias al
     * {@code CascadeType.ALL} declarado en las relaciones, persistir el proveedor persiste en
     * cascada toda la versión y su catálogo.
     * </p>
     *
     * @param proveedorFormDTO Objeto DTO que contiene los datos de creación del proveedor y su versión inicial.
     * @return {@link ProveedorResponseDTO} representativo del proveedor guardado en la base de datos.
     * @throws RecursoDuplicadoException Si la razón social o el CUIT provistos ya pertenecen a un proveedor activo.
     * @throws RecursoNoEncontradoException Si la localidad, algún insumo o alguna presentación comercial referenciados no existen o no están activos.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO altaProveedor(ProveedorFormDTO proveedorFormDTO) {
        VersionProveedorFormDTO versionFormDTO = proveedorFormDTO.getVersion();

        // 1. Validar si la razón social o el CUIT ya están registrados en otro proveedor activo
        if (versionProveedorRepository.existsByRazonSocialIgnoreCaseOrCuitAndEsUltimaVersionTrue(
                versionFormDTO.getRazonSocial(), versionFormDTO.getCuit())) {
            throw new RecursoDuplicadoException("Ya existe un proveedor activo con la razón social '"
                    + versionFormDTO.getRazonSocial() + "' o el CUIT '" + versionFormDTO.getCuit() + "'");
        }

        // 2. Crear el contenedor del proveedor y su versión inicial (cascada hacia el catálogo)
        ProveedorEntity proveedorEntity = ProveedorEntity.builder().estado(Estado.ACTIVO).build();
        VersionProveedorEntity versionProveedorEntity = construirVersion(versionFormDTO, proveedorEntity);
        proveedorEntity.getVersiones().add(versionProveedorEntity);

        // 3. Guardar el proveedor en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperProveedor.toDTO(proveedorRepository.save(proveedorEntity));
    }

    /**
     * Modifica un proveedor existente registrando una nueva versión con los datos actualizados.
     * <p>
     * Por diseño, {@link ProveedorEntity} nunca actualiza una versión existente: "modificar un
     * proveedor" crea una nueva {@link VersionProveedorEntity} con {@code esUltimaVersion = true}
     * y desactiva la anterior, para no afectar retroactivamente las órdenes de compra que ya
     * referencian un ítem del catálogo de la versión previa.
     * </p>
     *
     * @param id Identificador clave primaria del proveedor a modificar.
     * @param proveedorFormDTO DTO que contiene los nuevos datos de la versión del proveedor.
     * @return {@link ProveedorResponseDTO} representativo del proveedor con la nueva versión aplicada.
     * @throws RecursoDuplicadoException Si la nueva razón social o el nuevo CUIT ya pertenecen a otro proveedor activo.
     * @throws RecursoNoEncontradoException Si no se localiza un proveedor activo por el ID proporcionado, o si la localidad, algún insumo o alguna presentación comercial referenciados no existen o no están activos.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO modificarProveedor(Long id, ProveedorFormDTO proveedorFormDTO) {
        VersionProveedorFormDTO versionFormDTO = proveedorFormDTO.getVersion();

        // 1. Validar duplicación excluyendo el propio proveedor, de modo que conservar la razón
        //    social o el CUIT actuales no falle contra el mismo registro
        if (versionProveedorRepository.existsByRazonSocialIgnoreCaseOrCuitAndEsUltimaVersionTrueAndProveedorIdNot(
                versionFormDTO.getRazonSocial(), versionFormDTO.getCuit(), id)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor activo con la razón social '"
                    + versionFormDTO.getRazonSocial() + "' o el CUIT '" + versionFormDTO.getCuit() + "'");
        }

        // 2. Localizar el proveedor existente. Si no existe o no está activo, se dispara RecursoNoEncontradoException
        ProveedorEntity proveedorEntity = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id));

        // 3. Construir la nueva versión primero: si la localidad, algún insumo o alguna
        //    presentación comercial no existen, esto lanza antes de tocar la versión anterior
        VersionProveedorEntity nuevaVersionProveedorEntity = construirVersion(versionFormDTO, proveedorEntity);

        // 4. Recién si la construcción fue exitosa, desactivar la versión actualmente activa y
        //    registrar la nueva
        proveedorEntity.getVersiones().stream()
                .filter(VersionProveedorEntity::isEsUltimaVersion)
                .forEach(version -> version.setEsUltimaVersion(false));
        proveedorEntity.getVersiones().add(nuevaVersionProveedorEntity);

        // 5. Persistir el proveedor con la nueva versión y retornar el DTO de respuesta correspondiente
        return MapperProveedor.toDTO(proveedorRepository.save(proveedorEntity));
    }

    /**
     * Procesa la baja lógica de un proveedor existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al proveedor con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del proveedor a dar de baja.
     * @return {@link ProveedorResponseDTO} con los datos del proveedor ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el proveedor con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el proveedor tiene una orden de compra en estado {@code PENDIENTE} asociada a alguna de sus versiones.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO bajaProveedor(Long id) {
        // 1. Buscar el proveedor. Si no existe, se dispara RecursoNoEncontradoException
        ProveedorEntity proveedorEntity = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id));

        // 2. Validar que ninguna versión del proveedor (histórica o activa) tenga una orden de
        //    compra en estado PENDIENTE asociada
        if (ordenCompraRepository.existsByVersionProveedor_Proveedor_IdAndEstado(id, EstadoOrden.PENDIENTE)) {
            throw new ReglaNegocioException("No se puede dar de baja el proveedor porque tiene una orden de compra en estado PENDIENTE asociada");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado, persistimos el cambio y retornamos
        //    el DTO del proveedor dado de baja
        proveedorEntity.setEstado(Estado.BAJA);
        return MapperProveedor.toDTO(proveedorRepository.save(proveedorEntity));
    }

    // === CONSTRUCCIÓN DEL ÁRBOL DE ENTIDADES DE LA VERSIÓN ===

    /**
     * Construye la {@link VersionProveedorEntity} completa a partir del DTO de formulario,
     * resolviendo la localidad referenciada e incluyendo el catálogo de productos.
     *
     * @param versionFormDTO Datos de la versión de proveedor a construir.
     * @param proveedorEntity Proveedor contenedor al que pertenece esta versión.
     * @return Entidad de versión de proveedor, marcada como última versión activa, con su catálogo vinculado.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa con el ID indicado.
     */
    private VersionProveedorEntity construirVersion(VersionProveedorFormDTO versionFormDTO, ProveedorEntity proveedorEntity) {
        LocalidadEntity localidadEntity = localidadRepository.findById(versionFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + versionFormDTO.getIdLocalidad()));

        VersionProveedorEntity versionProveedorEntity = VersionProveedorEntity.builder()
                .razonSocial(versionFormDTO.getRazonSocial())
                .nombreComercial(versionFormDTO.getNombreComercial())
                .cuit(versionFormDTO.getCuit())
                .telefono(versionFormDTO.getTelefono())
                .email(versionFormDTO.getEmail())
                .direccion(versionFormDTO.getDireccion())
                .esUltimaVersion(true)
                .proveedor(proveedorEntity)
                .localidad(localidadEntity)
                .build();

        versionProveedorEntity.getCatalogoProveedor().addAll(versionFormDTO.getCatalogoProveedor().stream()
                .map(detalle -> construirItemCatalogo(detalle, versionProveedorEntity))
                .toList());

        return versionProveedorEntity;
    }

    /**
     * Construye el ítem de catálogo a partir del DTO de formulario, resolviendo la presentación
     * comercial y el insumo referenciados por sus identificadores.
     *
     * @param catalogoFormDTO Datos del ítem de catálogo.
     * @param versionProveedorEntity Versión de proveedor a la que pertenece este ítem.
     * @return Entidad de ítem de catálogo vinculada a la presentación comercial, al insumo y a la versión de proveedor.
     * @throws RecursoNoEncontradoException Si no existe una presentación comercial activa o un insumo activo con el ID indicado.
     */
    private CatalogoProveedorEntity construirItemCatalogo(CatalogoProveedorFormDTO catalogoFormDTO, VersionProveedorEntity versionProveedorEntity) {
        PresentacionComercialEntity presentacionComercialEntity = presentacionComercialRepository.findById(catalogoFormDTO.getIdPresentacionComercial())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la presentación comercial con ID: " + catalogoFormDTO.getIdPresentacionComercial()));

        InsumoEntity insumoEntity = insumoRepository.findById(catalogoFormDTO.getIdInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el insumo con ID: " + catalogoFormDTO.getIdInsumo()));

        return CatalogoProveedorEntity.builder()
                .presentacionComercial(presentacionComercialEntity)
                .insumo(insumoEntity)
                .version(versionProveedorEntity)
                .build();
    }
}
