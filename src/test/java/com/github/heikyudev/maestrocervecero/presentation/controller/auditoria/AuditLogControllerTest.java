package com.github.heikyudev.maestrocervecero.presentation.controller.auditoria;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AuditLogController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAuditLogService auditLogService;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-01: el ADMINISTRADOR ve la bitácora con fecha y hora, usuario, módulo, acción, ID afectado e IP, sin acciones de alta, edición ni baja")
    void listado_debeMostrarLasEntradasParaAdministrador() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        entrada(1L, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.ORDEN_COMPRA, "12",
                                LocalDateTime.of(2026, 9, 15, 10, 30, 5), "127.0.0.1")),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/auditoria-lista"))
                .andExpect(content().string(containsString("Bitácora de Auditoría")))
                .andExpect(content().string(containsString("15/09/2026 10:30:05")))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("ORDEN COMPRA")))
                .andExpect(content().string(containsString("CREAR")))
                .andExpect(content().string(containsString("127.0.0.1")))
                .andExpect(content().string(not(containsString("data-baja-url"))))
                .andExpect(content().string(not(containsString("Nuevo"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-02: sin filtros pide páginas de 20 ordenadas con la entrada más reciente primero, con todos los criterios en null")
    void listado_debePedirPaginasDe20ConLaMasRecientePrimero() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/auditoria")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogService).filtrarAuditLogs(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("fechaHora", "id");
        assertThat(captor.getValue().getSort().stream().allMatch(orden -> orden.getDirection() == Sort.Direction.DESC)).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-03: los criterios llegan al service; \"desde\" se toma desde las 00:00 y \"hasta\" hasta el final del día")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/auditoria")
                        .param("fechaDesde", "2026-09-01")
                        .param("fechaHasta", "2026-09-30")
                        .param("username", "  adm  ")
                        .param("accion", "CREAR")
                        .param("concepto", "MALTA")
                        .param("pagina", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogService).filtrarAuditLogs(
                eq(LocalDateTime.of(2026, 9, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 9, 30, 23, 59, 59, 999_000_000)),
                eq(AccionAuditoria.CREAR), eq(ConceptoAuditoria.MALTA), eq("adm"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-04: un usuario en blanco y selects en \"Todas\" / \"Todos\" (valor vacío) equivalen a no filtrar")
    void listado_debeTratarLosValoresVaciosComoSinFiltro() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/auditoria")
                        .param("fechaDesde", "").param("fechaHasta", "")
                        .param("username", "   ").param("accion", "").param("concepto", ""))
                .andExpect(status().isOk());

        verify(auditLogService).filtrarAuditLogs(isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-05: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron registros con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-06: un rango de fechas invertido muestra el aviso del service y una lista vacía, conservando los filtros elegidos")
    void listado_debeMostrarElAvisoDeRangoInvertido() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta."));

        mockMvc.perform(get("/auditoria").param("fechaDesde", "2026-09-30").param("fechaHasta", "2026-09-01")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/auditoria-lista"))
                .andExpect(content().string(containsString("La fecha desde no puede ser posterior a la fecha hasta.")))
                .andExpect(content().string(containsString("No se encontraron registros con los criterios indicados.")))
                .andExpect(content().string(containsString("value=\"2026-09-30\"")))
                .andExpect(content().string(containsString("value=\"2026-09-01\"")))
                .andExpect(content().string(containsString("value=\"admin\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-07: la paginación conserva todos los criterios de búsqueda en los enlaces")
    void listado_debeConservarLosFiltrosEnLaPaginacion() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        entrada(1L, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "1",
                                LocalDateTime.of(2026, 9, 15, 10, 30), null)),
                        PageRequest.of(1, 20), 60));

        mockMvc.perform(get("/auditoria")
                        .param("fechaDesde", "2026-09-01").param("fechaHasta", "2026-09-30")
                        .param("username", "admin").param("accion", "CREAR").param("concepto", "MALTA")
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("fechaDesde=2026-09-01")))
                .andExpect(content().string(containsString("fechaHasta=2026-09-30")))
                .andExpect(content().string(containsString("username=admin")))
                .andExpect(content().string(containsString("accion=CREAR")))
                .andExpect(content().string(containsString("concepto=MALTA")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-08: la acción se muestra con un color según su tipo y los datos sin valor se muestran con un guion")
    void listado_debeColorearLaAccionSegunSuTipo() throws Exception {
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 15, 10, 30);
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        entrada(1L, "u", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, null, fecha, null),
                        entrada(2L, "u", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "2", fecha, "10.0.0.1"),
                        entrada(3L, "u", AccionAuditoria.ELIMINAR, ConceptoAuditoria.MALTA, "3", fecha, "10.0.0.1"),
                        entrada(4L, "u", AccionAuditoria.FINALIZAR, ConceptoAuditoria.LOTE, "4", fecha, "10.0.0.1"),
                        entrada(5L, "u", AccionAuditoria.LOGOUT, ConceptoAuditoria.SESION, null, fecha, "10.0.0.1")),
                        PageRequest.of(0, 20), 5));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bg-emerald-50 text-emerald-700")))
                .andExpect(content().string(containsString("bg-blue-50 text-blue-700")))
                .andExpect(content().string(containsString("bg-rose-50 text-rose-700")))
                .andExpect(content().string(containsString("bg-amber-50 text-amber-700")))
                .andExpect(content().string(containsString("bg-slate-100 text-slate-600")))
                .andExpect(content().string(containsString("—")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-09: los selects de acción y módulo ofrecen todos los valores de los catálogos")
    void listado_debeOfrecerTodasLasAccionesYModulos() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/auditoria").param("accion", "LOGIN_EXITOSO").param("concepto", "ORDEN_COMPRA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"LOGIN_EXITOSO\" selected")))
                .andExpect(content().string(containsString("value=\"ORDEN_COMPRA\" selected")))
                .andExpect(content().string(containsString("value=\"ANULAR\"")))
                .andExpect(content().string(containsString("value=\"SESION\"")))
                .andExpect(content().string(containsString("value=\"USUARIO\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-AC-10: un rol distinto de ADMINISTRADOR no puede ver la bitácora (no llega al service)")
    void listado_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(auditLogService, never()).filtrarAuditLogs(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CP-AC-11: un visitante sin sesión es enviado al login")
    void listado_debeRedirigirAlLoginSiNoHaySesion() throws Exception {
        mockMvc.perform(get("/auditoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));

        verify(auditLogService, never()).filtrarAuditLogs(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-12: una fecha con formato inválido o una acción inexistente se rechaza con una alerta, sin llegar al service")
    void listado_debeRechazarParametrosInvalidos() throws Exception {
        mockMvc.perform(get("/auditoria").param("fechaDesde", "no-es-una-fecha"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get("/auditoria").param("accion", "INVENTADA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(auditLogService, never()).filtrarAuditLogs(any(), any(), any(), any(), any(), any());
    }

    private static AuditLogResponseDTO entrada(Long id, String username, AccionAuditoria accion, ConceptoAuditoria concepto,
                                               String entidadId, LocalDateTime fechaHora, String ipAddress) {
        return AuditLogResponseDTO.builder()
                .id(id).username(username).accion(accion).conceptoAuditoria(concepto)
                .entidadId(entidadId).fechaHora(fechaHora).ipAddress(ipAddress)
                .build();
    }
}
