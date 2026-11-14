package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

/**
 * Cerveza que contiene un barril: el lote de su último envasado registrado y la receta de ese lote.
 *
 * @param idLote Identificador único del lote.
 * @param codigoLote Identificador interno del lote (por ejemplo, PALE_ALE-1).
 * @param nombreReceta Nombre de la receta del lote, que es el nombre de la cerveza.
 */
public record ContenidoBarrilResponseDTO(Long idLote, String codigoLote, String nombreReceta) {
}
