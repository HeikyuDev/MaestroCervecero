package com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Correo de invitación listo para enviar: su token ya quedó guardado (como hash) en la invitación, de modo que el enlace
 * sirve aunque el resultado del envío no se llegue a registrar.
 *
 * @param idInvitacion ID de la invitación.
 * @param correo Dirección del proveedor.
 * @param razonSocial Razón social del proveedor.
 * @param token Token del enlace (sin hashear); solo viaja en el correo.
 * @param fechaLimite Fecha límite de respuesta.
 * @param insumos Nombres de los insumos solicitados que ofrece.
 */
public record EnvioInvitacionPreparado(Long idInvitacion, String correo, String razonSocial, String token,
                                       LocalDateTime fechaLimite, List<String> insumos) {
}
