package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta con el panel del administrador: los usuarios activos, su distribución por rol, la actividad de hoy de
 * la bitácora de auditoría y las primeras filas de los usuarios modificados recientemente y de los últimos eventos.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAdministradorResponseDTO {

    /** Cantidad de usuarios en estado Activo. Es la suma de {@link #usuariosPorRol}. */
    private long usuariosActivos;

    /** Usuarios activos por rol, siempre todos los roles y en el orden en que están declarados. Nunca es nula. */
    private List<ConteoDashboardResponseDTO> usuariosPorRol;

    /** Cantidad de eventos de la bitácora de hoy, desde las 00:00 hasta el final del día. */
    private long eventosDeHoy;

    /** Usuarios activos, el modificado más recientemente primero, con su total real y hasta 5 filas. */
    private BloqueDashboardResponseDTO<UsuarioRecienteResponseDTO> usuariosRecientes;

    /** Eventos de la bitácora, el más reciente primero, con su total real y hasta 5 filas. */
    private BloqueDashboardResponseDTO<EventoAuditoriaResponseDTO> actividadReciente;
}
