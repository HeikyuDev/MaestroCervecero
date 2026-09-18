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
     * Filtra los proveedores activos, opcionalmente por razón social, nombre comercial, CUIT y/o
     * localidad.
     * <p>
     * {@link com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity}
     * no tiene datos propios: la razón social, el nombre comercial, el CUIT y la localidad viven
     * en su {@code VersionProveedorEntity} marcada como {@code esUltimaVersion = true}, así que el
     * filtrado se resuelve contra esa versión vigente, no contra el historial completo.
     * </p>
     *
     * @param razonSocial Texto a buscar dentro de la razón social, o {@code null} para no filtrar por ella.
     * @param nombreComercial Texto a buscar dentro del nombre comercial, o {@code null} para no filtrar por él.
     * @param cuit El CUIT exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idLocalidad El ID de la localidad a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de proveedores activos en formato DTO que cumplen los criterios indicados.
     */
    Page<ProveedorResponseDTO> filtrarProveedores(String razonSocial, String nombreComercial, String cuit, Long idLocalidad, Pageable pageable);

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
