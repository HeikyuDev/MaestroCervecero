package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ConteoDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardAdministradorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EventoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.UsuarioRecienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAdministradorServicioImplTest {

    private static final Pageable SOLO_EL_TOTAL = PageRequest.of(0, 1);

    @Mock
    private IUsuarioServicio usuarioServicio;

    @Mock
    private IAuditLogService auditLogService;

    @InjectMocks
    private DashboardAdministradorServicioImpl dashboardServicio;

    @Test
    @DisplayName("CP-DAD-01: obtenerDashboardAdministrador sin usuarios ni eventos devuelve los contadores en cero, todos los roles con 0 y los bloques sin filas")
    void obtener_debeDevolverElPanelEnCeroConElSistemaVacio() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        DashboardAdministradorResponseDTO resultado = dashboardServicio.obtenerDashboardAdministrador();

        // === ASSERTS ===
        assertThat(resultado.getUsuariosActivos()).isZero();
        assertThat(resultado.getUsuariosPorRol()).hasSize(Rol.values().length).allMatch(c -> c.getTotal() == 0);
        assertThat(resultado.getEventosDeHoy()).isZero();
        assertThat(resultado.getUsuariosRecientes().getTotal()).isZero();
        assertThat(resultado.getUsuariosRecientes().getFilas()).isEmpty();
        assertThat(resultado.getActividadReciente().getTotal()).isZero();
        assertThat(resultado.getActividadReciente().getFilas()).isEmpty();
    }

    @Test
    @DisplayName("CP-DAD-02: los usuarios activos se cuentan por rol, siempre todos los roles y en el orden en que están declarados, y los usuarios activos son la suma")
    void obtener_debeContarLosUsuariosActivosPorRol() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        when(usuarioServicio.filtrarUsuarios(isNull(), isNull(), isNull(), isNull(), eq(Rol.ADMINISTRADOR), eq(SOLO_EL_TOTAL))).thenReturn(paginaConTotal(2));
        when(usuarioServicio.filtrarUsuarios(isNull(), isNull(), isNull(), isNull(), eq(Rol.OPERARIO_DE_PRODUCCION), eq(SOLO_EL_TOTAL))).thenReturn(paginaConTotal(5));

        // === EJECUCION ===
        DashboardAdministradorResponseDTO resultado = dashboardServicio.obtenerDashboardAdministrador();

        // === ASSERTS ===
        assertThat(resultado.getUsuariosActivos()).isEqualTo(7);
        assertThat(resultado.getUsuariosPorRol()).extracting(ConteoDashboardResponseDTO::getCodigo)
                .containsExactly(java.util.Arrays.stream(Rol.values()).map(Rol::name).toArray(String[]::new));
        ConteoDashboardResponseDTO administrador = resultado.getUsuariosPorRol().stream().filter(c -> c.getCodigo().equals("ADMINISTRADOR")).findFirst().orElseThrow();
        assertThat(administrador.getEtiqueta()).isEqualTo("Administrador");
        assertThat(administrador.getTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-DAD-03: los eventos de hoy se cuentan con el rango de hoy, de las 00:00 al final del día")
    void obtener_debeContarLosEventosDeHoy() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(auditLogService.filtrarAuditLogs(eq(hoy.atStartOfDay()), eq(hoy.atTime(LocalTime.MAX)), isNull(), isNull(), isNull(), eq(SOLO_EL_TOTAL)))
                .thenReturn(new PageImpl<>(List.of(), SOLO_EL_TOTAL, 15));

        // === EJECUCION ===
        DashboardAdministradorResponseDTO resultado = dashboardServicio.obtenerDashboardAdministrador();

        // === ASSERTS ===
        assertThat(resultado.getEventosDeHoy()).isEqualTo(15);
    }

    @Test
    @DisplayName("CP-DAD-04: los usuarios recientes se piden con 5 filas ordenadas por modificación descendente y, a igualdad, por ID descendente, con el total real y el nombre completo")
    void obtener_debeInformarLosUsuariosRecientes() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        UsuarioResponseDTO usuario = UsuarioResponseDTO.builder().id(3L).username("jperez").nombre("Juan").apellido("Pérez").rol(Rol.OPERARIO_DE_PRODUCCION).estado(Estado.ACTIVO).build();
        when(usuarioServicio.filtrarUsuarios(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario), PageRequest.of(0, 5), 8));

        // === EJECUCION ===
        DashboardAdministradorResponseDTO resultado = dashboardServicio.obtenerDashboardAdministrador();

        // === ASSERTS ===
        assertThat(resultado.getUsuariosRecientes().getTotal()).isEqualTo(8);
        UsuarioRecienteResponseDTO fila = resultado.getUsuariosRecientes().getFilas().get(0);
        assertThat(fila.getId()).isEqualTo(3L);
        assertThat(fila.getUsername()).isEqualTo("jperez");
        assertThat(fila.getNombreCompleto()).isEqualTo("Juan Pérez");
        assertThat(fila.getRol()).isEqualTo(Rol.OPERARIO_DE_PRODUCCION);
        verify(usuarioServicio).filtrarUsuarios(isNull(), isNull(), isNull(), isNull(), isNull(), pagina.capture());
        Pageable pedida = pagina.getAllValues().stream().filter(p -> p.getPageSize() == 5).findFirst().orElseThrow();
        assertThat(pedida.getSort().toString()).isEqualTo("lastModifiedDate: DESC,id: DESC");
    }

    @Test
    @DisplayName("CP-DAD-05: la actividad reciente se pide con 5 filas, la más reciente primero y, a igualdad, la de mayor ID, con el total real y el detalle de cada evento")
    void obtener_debeInformarLaActividadReciente() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        AuditLogResponseDTO entrada = AuditLogResponseDTO.builder().id(41L).username("admin").accion(AccionAuditoria.CREAR).conceptoAuditoria(ConceptoAuditoria.MALTA)
                .fechaHora(LocalDateTime.of(2027, 7, 1, 8, 30)).tieneDetalle(true).build();
        when(auditLogService.filtrarAuditLogs(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entrada), PageRequest.of(0, 5), 120));

        // === EJECUCION ===
        DashboardAdministradorResponseDTO resultado = dashboardServicio.obtenerDashboardAdministrador();

        // === ASSERTS ===
        assertThat(resultado.getActividadReciente().getTotal()).isEqualTo(120);
        EventoAuditoriaResponseDTO fila = resultado.getActividadReciente().getFilas().get(0);
        assertThat(fila.getId()).isEqualTo(41L);
        assertThat(fila.getUsername()).isEqualTo("admin");
        assertThat(fila.getAccion()).isEqualTo(AccionAuditoria.CREAR);
        assertThat(fila.getConcepto()).isEqualTo(ConceptoAuditoria.MALTA);
        assertThat(fila.getFechaHora()).isEqualTo(LocalDateTime.of(2027, 7, 1, 8, 30));
        assertThat(fila.isTieneDetalle()).isTrue();
    }

    // ==================== helpers ====================

    /** Deja todos los conteos y listados vacíos; cada test pisa solo lo que ejercita. */
    private void prepararSistemaVacio() {
        lenient().when(usuarioServicio.filtrarUsuarios(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(SOLO_EL_TOTAL));
        lenient().when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(SOLO_EL_TOTAL));
    }

    private static Page<UsuarioResponseDTO> paginaConTotal(long total) {
        return new PageImpl<>(List.of(), SOLO_EL_TOTAL, total);
    }
}
