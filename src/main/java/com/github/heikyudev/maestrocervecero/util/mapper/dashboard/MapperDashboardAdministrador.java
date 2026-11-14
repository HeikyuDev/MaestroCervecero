package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ConteoDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EventoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.UsuarioRecienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;

/**
 * Mapea los DTO de usuarios y de la bitácora de auditoría a los DTO de respuesta del panel del administrador. No ejecuta
 * consultas.
 */
public class MapperDashboardAdministrador {

    private MapperDashboardAdministrador() {
    }

    /**
     * Mapea un usuario a {@link UsuarioRecienteResponseDTO}, con su nombre y apellido unidos.
     *
     * @param usuario Usuario a convertir.
     * @return El DTO, o {@code null} si el usuario es nulo.
     */
    public static UsuarioRecienteResponseDTO toUsuarioRecienteDTO(UsuarioResponseDTO usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioRecienteResponseDTO.builder()
                .id(usuario.getId())
                .username(usuario.getUsername())
                .nombreCompleto(usuario.getNombre() + " " + usuario.getApellido())
                .rol(usuario.getRol())
                .build();
    }

    /**
     * Mapea una entrada de la bitácora a {@link EventoAuditoriaResponseDTO}.
     *
     * @param entrada Entrada a convertir.
     * @return El DTO, o {@code null} si la entrada es nula.
     */
    public static EventoAuditoriaResponseDTO toEventoAuditoriaDTO(AuditLogResponseDTO entrada) {
        if (entrada == null) {
            return null;
        }
        return EventoAuditoriaResponseDTO.builder()
                .id(entrada.getId())
                .fechaHora(entrada.getFechaHora())
                .username(entrada.getUsername())
                .accion(entrada.getAccion())
                .concepto(entrada.getConceptoAuditoria())
                .tieneDetalle(entrada.isTieneDetalle())
                .build();
    }

    /**
     * Arma el conteo de usuarios de un rol.
     *
     * @param codigo Nombre del valor del enum {@code Rol}.
     * @param etiqueta Nombre del rol para mostrar en pantalla.
     * @param total Cantidad de usuarios activos del rol.
     * @return El DTO con los tres datos recibidos.
     */
    public static ConteoDashboardResponseDTO toConteoDTO(String codigo, String etiqueta, long total) {
        return ConteoDashboardResponseDTO.builder().codigo(codigo).etiqueta(etiqueta).total(total).build();
    }
}
