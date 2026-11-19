package com.github.heikyudev.maestrocervecero.service.interfaces.distribucion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recordatorio de retiro listo para enviar: su token ya quedó guardado (como hash), de modo que el enlace sirve aunque
 * el resultado del envío no se llegue a registrar.
 *
 * @param idInvitacion ID del recordatorio.
 * @param correo Dirección del cliente.
 * @param nombreCliente Nombre del cliente.
 * @param token Token del enlace (sin hashear); solo viaja en el correo.
 * @param fechaExpiracion Momento hasta el que sirve el enlace.
 * @param barriles Descripción de cada barril vencido (número de serie e identificador interno).
 */
public record EnvioRecordatorioRetiroPreparado(Long idInvitacion, String correo, String nombreCliente, String token,
                                               LocalDateTime fechaExpiracion, List<String> barriles) {
}
