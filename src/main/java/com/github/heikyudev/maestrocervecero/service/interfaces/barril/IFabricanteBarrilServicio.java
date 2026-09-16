package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FabricanteBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de Fabricante de Barril.
 */
public interface IFabricanteBarrilServicio {

    /**
     * Obtiene una página de fabricantes de barril activos, filtrados opcionalmente por razón
     * social, nombre comercial y/o CUIT (todas coincidencias parciales, sin distinguir mayúsculas/
     * minúsculas). Un parámetro nulo no restringe por ese criterio.
     *
     * @param razonSocial Texto a buscar dentro de la razón social, o {@code null} para no filtrar por ella.
     * @param nombreComercial Texto a buscar dentro del nombre comercial, o {@code null} para no filtrar por él.
     * @param cuit Texto a buscar dentro del CUIT, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de FabricanteBarrilResponseDTO que cumplen los criterios indicados.
     */
    Page<FabricanteBarrilResponseDTO> filtrarFabricantesBarril(String razonSocial, String nombreComercial, String cuit, Pageable pageable);

    /**
     * Busca un fabricante de barril por su ID.
     *
     * @param id ID del fabricante de barril.
     * @return FabricanteBarrilResponseDTO correspondiente al ID proporcionado.
     */
    FabricanteBarrilResponseDTO buscarPorId(Long id);

    /**
     * Da de alta un nuevo fabricante de barril.
     *
     * @param fabricanteBarrilFormDTO Datos del fabricante de barril a dar de alta.
     * @return FabricanteBarrilResponseDTO del fabricante de barril dado de alta.
     */
    FabricanteBarrilResponseDTO altaFabricanteBarril(FabricanteBarrilFormDTO fabricanteBarrilFormDTO);

    /**
     * Modifica un fabricante de barril existente.
     *
     * @param id                       ID del fabricante de barril a modificar.
     * @param fabricanteBarrilFormDTO  Datos del fabricante de barril a modificar.
     * @return FabricanteBarrilResponseDTO del fabricante de barril modificado.
     */
    FabricanteBarrilResponseDTO modificarFabricanteBarril(Long id, FabricanteBarrilFormDTO fabricanteBarrilFormDTO);

    /**
     * Da de baja un fabricante de barril existente.
     *
     * @param id ID del fabricante de barril a dar de baja.
     * @return FabricanteBarrilResponseDTO del fabricante de barril dado de baja.
     */
    FabricanteBarrilResponseDTO bajaFabricanteBarril(Long id);
}
