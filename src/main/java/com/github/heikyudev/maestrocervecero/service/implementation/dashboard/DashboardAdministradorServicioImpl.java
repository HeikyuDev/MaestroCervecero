package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardAdministradorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ConteoDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardAdministradorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardAdministrador;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de {@link IDashboardAdministradorServicio}.
 * <p>
 * Es de solo lectura y corre en una única transacción de lectura. Se apoya en los servicios de usuarios y de auditoría:
 * los totales salen de páginas de un registro y las filas de la primera página de cada listado, con su orden.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DashboardAdministradorServicioImpl implements IDashboardAdministradorServicio {

    private static final Pageable USUARIOS_RECIENTES = PageRequest.of(0, LimitesTecnicos.PANEL_ADMINISTRADOR_FILAS_POR_BLOQUE,
            Sort.by(Sort.Order.desc("lastModifiedDate"), Sort.Order.desc("id")));
    private static final Pageable EVENTOS_RECIENTES = PageRequest.of(0, LimitesTecnicos.PANEL_ADMINISTRADOR_FILAS_POR_BLOQUE,
            Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id")));

    private final IUsuarioServicio usuarioServicio;
    private final IAuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public DashboardAdministradorResponseDTO obtenerDashboardAdministrador() {
        LocalDate hoy = LocalDate.now();
        List<ConteoDashboardResponseDTO> usuariosPorRol = contarUsuariosPorRol();

        return DashboardAdministradorResponseDTO.builder()
                .usuariosActivos(usuariosPorRol.stream().mapToLong(ConteoDashboardResponseDTO::getTotal).sum())
                .usuariosPorRol(usuariosPorRol)
                .eventosDeHoy(auditLogService.filtrarAuditLogs(hoy.atStartOfDay(), hoy.atTime(LocalTime.MAX), null, null, null, LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements())
                .usuariosRecientes(BloqueDashboardResponseDTO.de(usuarioServicio.filtrarUsuarios(null, null, null, null, null, USUARIOS_RECIENTES),
                        MapperDashboardAdministrador::toUsuarioRecienteDTO))
                .actividadReciente(BloqueDashboardResponseDTO.de(auditLogService.filtrarAuditLogs(null, null, null, null, null, EVENTOS_RECIENTES),
                        MapperDashboardAdministrador::toEventoAuditoriaDTO))
                .build();
    }

    private List<ConteoDashboardResponseDTO> contarUsuariosPorRol() {
        List<ConteoDashboardResponseDTO> conteos = new ArrayList<>();
        for (Rol rol : Rol.values()) {
            conteos.add(MapperDashboardAdministrador.toConteoDTO(rol.name(), rol.getEtiqueta(),
                    usuarioServicio.filtrarUsuarios(null, null, null, null, rol, LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements()));
        }
        return conteos;
    }
}
