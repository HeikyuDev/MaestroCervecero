package com.github.heikyudev.maestrocervecero.util.mapper.proveedor;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.CatalogoProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.VersionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;

import java.util.ArrayList;
import java.util.List;

/**
 * MapperProveedorForm tiene la responsabilidad de convertir un {@link ProveedorResponseDTO} en el
 * {@link ProveedorFormDTO} con el que se precarga el formulario de modificación: los datos de la
 * versión vigente quedan como valores iniciales de cada campo (el CUIT con guiones, como se muestra en el
 * listado) y cada ítem del catálogo como una fila.
 */
public class MapperProveedorForm {

    private MapperProveedorForm() {
    }

    /**
     * Convierte la versión vigente de un proveedor en el DTO de formulario equivalente.
     *
     * @param proveedor Proveedor con su versión vigente.
     * @return DTO de formulario precargado, o {@code null} si el proveedor es nulo. Si el proveedor no
     * tiene versión vigente, devuelve un formulario vacío.
     */
    public static ProveedorFormDTO toFormDTO(ProveedorResponseDTO proveedor) {
        if (proveedor == null) {
            return null;
        }
        VersionProveedorResponseDTO version = proveedor.getVersion();
        if (version == null) {
            return new ProveedorFormDTO();
        }

        List<CatalogoProveedorFormDTO> catalogo = new ArrayList<>();
        if (version.getCatalogoProveedor() != null) {
            version.getCatalogoProveedor().forEach(item -> catalogo.add(mapItem(item)));
        }

        VersionProveedorFormDTO versionForm = VersionProveedorFormDTO.builder()
                .razonSocial(version.getRazonSocial())
                .nombreComercial(version.getNombreComercial())
                .cuit(ValidacionesTexto.formatearCuit(version.getCuit()))
                .telefono(version.getTelefono())
                .email(version.getEmail())
                .direccion(version.getDireccion())
                .idLocalidad(version.getLocalidad() != null ? version.getLocalidad().getId() : null)
                .catalogoProveedor(catalogo)
                .build();

        return ProveedorFormDTO.builder().version(versionForm).build();
    }

    /**
     * Un ítem del catálogo se referencia por el id de su insumo y el de su presentación comercial: el catálogo se
     * vuelve a crear en cada versión, así que el id del ítem no viaja en el formulario.
     */
    private static CatalogoProveedorFormDTO mapItem(CatalogoProveedorResponseDTO item) {
        return CatalogoProveedorFormDTO.builder()
                .idInsumo(item.getInsumo() != null ? item.getInsumo().getId() : null)
                .idPresentacionComercial(item.getPresentacionComercial() != null ? item.getPresentacionComercial().getId() : null)
                .build();
    }
}
