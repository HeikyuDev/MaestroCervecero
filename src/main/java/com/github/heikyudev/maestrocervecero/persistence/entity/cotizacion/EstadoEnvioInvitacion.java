package com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Estado del envío por correo de la invitación a un proveedor: {@code PENDIENTE} mientras no se intentó;
 * {@code EN_ENVIO} desde que se guardó el token del intento hasta que se registra el resultado (no se reintenta: el
 * correo pudo haber salido con ese token); {@code ENVIADA} cuando el servidor de correo aceptó el correo; {@code FALLIDA}
 * si el último intento falló (el proceso programado lo reintenta hasta un máximo de intentos); y {@code OMITIDA} si no
 * corresponde enviarlo porque el proveedor se dio de baja o no tiene una versión vigente (estado final). Cada valor trae
 * su nombre legible ({@code etiqueta}).
 */
@Getter
@RequiredArgsConstructor
public enum EstadoEnvioInvitacion {
    PENDIENTE("Pendiente"),
    EN_ENVIO("Enviando"),
    ENVIADA("Enviada"),
    FALLIDA("No entregada"),
    OMITIDA("Omitida");

    private final String etiqueta;
}
