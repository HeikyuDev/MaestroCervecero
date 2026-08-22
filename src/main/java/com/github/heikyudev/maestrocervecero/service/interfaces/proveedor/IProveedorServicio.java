package com.github.heikyudev.maestrocervecero.service.interfaces.proveedor;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos para la gestión de proveedores.
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
     */
    ProveedorResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo proveedor, junto con su versión inicial.
     *
     * @param proveedorFormDTO Los datos del proveedor a registrar.
     * @return El proveedor registrado.
     */
    ProveedorResponseDTO altaProveedor(ProveedorFormDTO proveedorFormDTO);

    /**
     * Modifica un proveedor existente, creando una nueva versión en vez de sobrescribir la anterior.
     *
     * @param id El ID del proveedor a modificar.
     * @param proveedorFormDTO Los nuevos datos del proveedor.
     * @return El proveedor modificado.
     */
    ProveedorResponseDTO modificarProveedor(Long id, ProveedorFormDTO proveedorFormDTO);

    /**
     * Realiza la baja lógica de un proveedor existente.
     *
     * @param id El ID del proveedor a dar de baja.
     * @return El proveedor dado de baja.
     */
    ProveedorResponseDTO bajaProveedor(Long id);
}
