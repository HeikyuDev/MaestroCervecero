package com.github.heikyudev.maestrocervecero.service.interfaces.proveedor;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de proveedores.
 * <p>
 * Todas las operaciones actúan únicamente sobre proveedores activos: los registros con
 * baja lógica (soft delete) son filtrados automáticamente por Hibernate y no son
 * listados, ni obtenidos, ni modificables, ni re-eliminables.
 * </p>
 */
public interface IProveedorServicio {

    /**
     * Obtiene una página de proveedores activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de proveedores activos en formato DTO.
     */
    Page<ProveedorResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un proveedor activo por su ID.
     *
     * @param id El ID del proveedor.
     * @return El proveedor correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un proveedor activo con el ID especificado.
     */
    ProveedorResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo proveedor, junto con su catálogo de productos.
     *
     * @param proveedorFormDTO Los datos del proveedor a registrar.
     * @return El proveedor registrado.
     * @throws RecursoNoEncontradoException Si no existe una localidad activa, una presentación comercial activa o un insumo activo con alguno de los ID especificados.
     * @throws RecursoDuplicadoException Si la razón social o el CUIT provistos ya pertenecen a un proveedor activo.
     */
    ProveedorResponseDTO altaProveedor(ProveedorFormDTO proveedorFormDTO);

    /**
     * Modifica un proveedor existente, reemplazando por completo su catálogo de productos
     * por el recibido en el DTO.
     *
     * @param id El ID del proveedor a modificar.
     * @param proveedorFormDTO Los nuevos datos del proveedor.
     * @return El proveedor modificado.
     * @throws RecursoNoEncontradoException Si no existe un proveedor activo con el ID especificado, o si no existe una localidad activa, una presentación comercial activa o un insumo activo con alguno de los ID especificados.
     * @throws RecursoDuplicadoException Si el nuevo CUIT ya pertenece a otro proveedor activo.
     */
    ProveedorResponseDTO modificarProveedor(Long id, ProveedorFormDTO proveedorFormDTO);

    /**
     * Elimina lógicamente un proveedor por su ID.
     *
     * @param id El ID del proveedor a eliminar.
     * @return El proveedor eliminado.
     * @throws RecursoNoEncontradoException Si no existe un proveedor activo con el ID especificado.
     */
    ProveedorResponseDTO bajaProveedor(Long id);
}
