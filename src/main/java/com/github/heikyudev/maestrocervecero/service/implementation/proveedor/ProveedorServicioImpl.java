package com.github.heikyudev.maestrocervecero.service.implementation.proveedor;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IPresentacionComercialRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.CatalogoProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.proveedor.MapperProveedor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProveedorServicioImpl implements IProveedorServicio {

    // Inyecto los repositorios gracias a LOMBOK
    private final IProveedorRepository proveedorRepository;
    private final ILocalidadRepository localidadRepository;
    private final IPresentacionComercialRepository presentacionComercialRepository;
    private final IInsumoRepository insumoRepository;
    private final ICatalogoProveedorRepository catalogoProveedorRepository;

    /**
     * Recupera una página de proveedores activos registrados en el sistema.
     * <p>
     * Los proveedores eliminados lógicamente son excluidos automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ProveedorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> buscarTodos(Pageable pageable) {
        // Obtengo las entidades de la base de datos y devuelvo el DTO correspondiente
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
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperProveedor.toDTO(proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proveedor no existe")));
    }

    /**
     * Registra un nuevo proveedor en el sistema, junto con su catálogo de productos.
     * <p>
     * Valida que la razón social y el CUIT no estén duplicados entre los proveedores activos,
     * que la localidad seleccionada exista, y que cada ítem del catálogo referencie una
     * presentación comercial y un insumo activos. Registra el evento en la auditoría.
     * </p>
     *
     * @param proveedorFormDTO Objeto DTO que contiene los datos de creación del proveedor.
     * @return {@link ProveedorResponseDTO} representativo del proveedor guardado en la base de datos.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa, una presentación comercial activa o un insumo activo con alguno de los ID especificados.
     * @throws RecursoDuplicadoException Si la razón social o el CUIT provistos ya pertenecen a un proveedor activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO altaProveedor(ProveedorFormDTO proveedorFormDTO) {
        // 1. Validar que la razón social y el CUIT no estén registrados en otro proveedor
        validarRazonSocialYCuitUnicos(proveedorFormDTO.getRazonSocial(), proveedorFormDTO.getCuit());

        // 2. Verificar que la localidad seleccionada exista
        LocalidadEntity localidadEntity = localidadRepository.findById(proveedorFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("La localidad no existe"));

        // 3. Creo la entidad que se va a almacenar en la base de datos
        ProveedorEntity proveedorEntity = ProveedorEntity.builder()
                .razonSocial(proveedorFormDTO.getRazonSocial())
                .nombreComercial(proveedorFormDTO.getNombreComercial())
                .cuit(proveedorFormDTO.getCuit())
                .telefono(proveedorFormDTO.getTelefono())
                .email(proveedorFormDTO.getEmail())
                .direccion(proveedorFormDTO.getDireccion())
                .localidad(localidadEntity)
                .build();

        // 4. Construyo el catálogo validando que cada presentación comercial e insumo referenciados existan
        proveedorEntity.setCatalogoProveedor(construirCatalogo(proveedorFormDTO.getCatalogoProveedor(), proveedorEntity));

        // 5. Guardo la entidad en la base de datos (cascada persiste el catálogo) y devuelvo el DTO correspondiente
        return MapperProveedor.toDTO(proveedorRepository.save(proveedorEntity));
    }

    /**
     * Actualiza la información de un proveedor existente en la base de datos, reemplazando
     * por completo su catálogo de productos.
     * <p>
     * Localiza el proveedor por su ID, verifica que la razón social y el CUIT no colisionen
     * con los de otro proveedor activo, y que la localidad seleccionada exista. Valida el
     * nuevo catálogo antes de dar de baja lógica el catálogo actual, para no perder los
     * datos existentes si el nuevo catálogo es inválido.
     * </p>
     *
     * @param id Identificador clave primaria del proveedor a modificar.
     * @param proveedorFormDTO DTO con la información actualizada.
     * @return {@link ProveedorResponseDTO} representativo del proveedor con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza un proveedor activo por el ID proporcionado, o si no existe una localidad activa, una presentación comercial activa o un insumo activo con alguno de los ID especificados.
     * @throws RecursoDuplicadoException Si la nueva razón social o el nuevo CUIT ya se encuentran asignados a otro proveedor activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO modificarProveedor(Long id, ProveedorFormDTO proveedorFormDTO) {
        // 1. Localizar el proveedor existente. Si no existe, se dispara RecursoNoEncontradoException
        ProveedorEntity proveedorEntity = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El proveedor no existe"));

        // 2. Validar duplicación de razón social y CUIT excluyendo el propio ID
        validarRazonSocialYCuitUnicos(proveedorFormDTO.getRazonSocial(), proveedorFormDTO.getCuit(), id);

        // 3. Verificar que la localidad seleccionada exista
        LocalidadEntity localidadEntity = localidadRepository.findById(proveedorFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("La localidad no existe"));

        // 4. Construyo el nuevo catálogo ANTES de tocar el actual: si alguna presentación comercial
        //    o insumo referenciado no existe, el catálogo actual permanece intacto
        List<CatalogoProveedorEntity> nuevoCatalogo = construirCatalogo(proveedorFormDTO.getCatalogoProveedor(), proveedorEntity);

        // 5. Doy de baja lógica el catálogo actual y lo reemplazo por el nuevo
        catalogoProveedorRepository.deleteAll(proveedorEntity.getCatalogoProveedor());
        proveedorEntity.setCatalogoProveedor(nuevoCatalogo);

        // 6. Aplico los cambios sobre la entidad administrada por persistencia
        proveedorEntity.setRazonSocial(proveedorFormDTO.getRazonSocial());
        proveedorEntity.setNombreComercial(proveedorFormDTO.getNombreComercial());
        proveedorEntity.setCuit(proveedorFormDTO.getCuit());
        proveedorEntity.setTelefono(proveedorFormDTO.getTelefono());
        proveedorEntity.setEmail(proveedorFormDTO.getEmail());
        proveedorEntity.setDireccion(proveedorFormDTO.getDireccion());
        proveedorEntity.setLocalidad(localidadEntity);

        // 7. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperProveedor.toDTO(proveedorRepository.save(proveedorEntity));
    }

    /**
     * Procesa la baja lógica de un proveedor existente en el sistema.
     * <p>
     * Invoca el método de eliminación del repositorio. Como {@link ProveedorEntity} está
     * anotada con {@code @SoftDelete}, Hibernate ejecuta un UPDATE sobre el flag de
     * borrado en lugar de una eliminación física.
     * </p>
     *
     * @param id Identificador clave primaria del proveedor a dar de baja.
     * @return {@link ProveedorResponseDTO} con los datos del proveedor procesado antes de su inactivación.
     * @throws RecursoNoEncontradoException Si el proveedor con el ID especificado no existe o ya fue dado de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PROVEEDOR)
    public ProveedorResponseDTO bajaProveedor(Long id) {
        // 1. Buscamos el proveedor. Si no existe, se dispara RecursoNoEncontradoException
        ProveedorEntity proveedorEntity = proveedorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el proveedor con ID: " + id));

        // 2. Ejecutamos la baja. Hibernate aplicará automáticamente la anotación de Soft Delete
        // TODO: Falta modulo de ordenes de compra, verifica que el proveedor seleccionado no posea Órdenes de Compra en estado "Pendiente".
        proveedorRepository.delete(proveedorEntity);

        // 3. Retornamos el DTO del proveedor dado de baja
        return MapperProveedor.toDTO(proveedorEntity);
    }

    /**
     * Valida que la razón social y el CUIT indicados no estén registrados en otro proveedor activo.
     *
     * @param razonSocial Razón social a validar.
     * @param cuit CUIT a validar.
     * @throws RecursoDuplicadoException Si la razón social o el CUIT ya pertenecen a un proveedor activo.
     */
    private void validarRazonSocialYCuitUnicos(String razonSocial, String cuit) {
        if (proveedorRepository.existsByRazonSocialIgnoreCase(razonSocial)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con la razón social '" + razonSocial + "'");
        }
        if (proveedorRepository.existsByCuit(cuit)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con el CUIT '" + cuit + "'");
        }
    }

    /**
     * Valida que la razón social y el CUIT indicados no estén registrados en otro proveedor
     * activo, excluyendo de la verificación al proveedor con el ID indicado.
     *
     * @param razonSocial Razón social a validar.
     * @param cuit CUIT a validar.
     * @param id ID del proveedor a excluir de la verificación.
     * @throws RecursoDuplicadoException Si la razón social o el CUIT ya pertenecen a otro proveedor activo.
     */
    private void validarRazonSocialYCuitUnicos(String razonSocial, String cuit, Long id) {
        if (proveedorRepository.existsByRazonSocialIgnoreCaseAndIdNot(razonSocial, id)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con la razón social '" + razonSocial + "'");
        }
        if (proveedorRepository.existsByCuitAndIdNot(cuit, id)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con el CUIT '" + cuit + "'");
        }
    }

    /**
     * Construye la lista de ítems de catálogo asociados a un proveedor, validando que cada
     * presentación comercial y cada insumo referenciados existan.
     *
     * @param catalogoFormDTO Lista de ítems de catálogo a construir.
     * @param proveedorEntity Proveedor al que se asocian los ítems del catálogo.
     * @return Lista de {@link CatalogoProveedorEntity} construidos.
     * @throws RecursoNoEncontradoException Si alguna presentación comercial o algún insumo referenciado no existe.
     */
    private List<CatalogoProveedorEntity> construirCatalogo(List<CatalogoProveedorFormDTO> catalogoFormDTO, ProveedorEntity proveedorEntity) {
        return catalogoFormDTO.stream()
                .map(itemFormDTO -> {
                    PresentacionComercialEntity presentacionComercialEntity = presentacionComercialRepository.findById(itemFormDTO.getIdPresentacionComercial())
                            .orElseThrow(() -> new RecursoNoEncontradoException("La presentación comercial no existe"));
                    InsumoEntity insumoEntity = insumoRepository.findById(itemFormDTO.getIdInsumo())
                            .orElseThrow(() -> new RecursoNoEncontradoException("El insumo no existe"));

                    return CatalogoProveedorEntity.builder()
                            .proveedor(proveedorEntity)
                            .presentacionComercial(presentacionComercialEntity)
                            .insumo(insumoEntity)
                            .build();
                })
                .toList();
    }
}
