package com.github.heikyudev.maestrocervecero.presentation.controller.auditoria;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.CampoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.EstadoCampoAuditoria;
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
                .andExpect(content().string(containsString("<title>Bitácora de Auditoría - Maestro Cervecero</title>")))
                .andExpect(content().string(containsString("15/09/2026 10:30")))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("Orden de compra")))
                .andExpect(content().string(containsString("Alta")))
                .andExpect(content().string(not(containsString("ORDEN COMPRA"))))
                .andExpect(content().string(containsString("127.0.0.1")))
                .andExpect(content().string(not(containsString("data-baja-url"))))
                .andExpect(content().string(not(containsString("Nuevo"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-13: el layout carga los estilos y los íconos desde recursos locales y no referencia ningún CDN")
    void layout_cargaEstilosEIconosLocalesSinCdn() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/css/tailwind.css\"")))
                .andExpect(content().string(containsString("href=\"/vendor/bootstrap-icons/bootstrap-icons.min.css\"")))
                .andExpect(content().string(not(containsString("cdn.tailwindcss.com"))))
                .andExpect(content().string(not(containsString("cdn.jsdelivr.net"))));
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
                .andExpect(content().string(containsString("No se encontraron registros con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-06: un rango de fechas invertido muestra el aviso del service y una lista vacía, conservando los filtros elegidos")
    void listado_debeMostrarElAvisoDeRangoInvertido() throws Exception {
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde"));

        mockMvc.perform(get("/auditoria").param("fechaDesde", "2026-09-30").param("fechaHasta", "2026-09-01")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/auditoria-lista"))
                .andExpect(content().string(containsString("La fecha hasta no puede ser anterior a la fecha desde")))
                .andExpect(content().string(containsString("No se encontraron registros con los criterios indicados")))
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
                .andExpect(content().string(containsString(">Baja<")))
                .andExpect(content().string(containsString(">Cierre de sesión<")))
                .andExpect(content().string(containsString(">-<")));
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
                .andExpect(content().string(containsString(">Anulación<")))
                .andExpect(content().string(containsString(">Planificación de producción<")))
                .andExpect(content().string(containsString("value=\"SESION\"")))
                .andExpect(content().string(containsString("value=\"USUARIO\"")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
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
    @DisplayName("CP-AC-12: una fecha con formato inválido o una acción inexistente se rechaza con una alerta amarilla, sin llegar al service")
    void listado_debeRechazarParametrosInvalidos() throws Exception {
        mockMvc.perform(get("/auditoria").param("fechaDesde", "no-es-una-fecha"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(get("/auditoria").param("accion", "INVENTADA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));

        verify(auditLogService, never()).filtrarAuditLogs(any(), any(), any(), any(), any(), any());
    }

    // ==================== Lista: botón Ver ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-14: la lista tiene la columna Acciones y muestra el ojo Ver hacia /auditoria/{id} solo en las entradas con detalle de estados")
    void listado_debeMostrarElOjoVerSoloConDetalle() throws Exception {
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 15, 10, 30);
        when(auditLogService.filtrarAuditLogs(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        AuditLogResponseDTO.builder().id(41L).username("u").accion(AccionAuditoria.MODIFICAR).conceptoAuditoria(ConceptoAuditoria.MALTA)
                                .entidadId("2").fechaHora(fecha).tieneDetalle(true).build(),
                        AuditLogResponseDTO.builder().id(42L).username("u").accion(AccionAuditoria.LOGOUT).conceptoAuditoria(ConceptoAuditoria.SESION)
                                .fechaHora(fecha).tieneDetalle(false).build()),
                        PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/auditoria"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Acciones</th>")))
                .andExpect(content().string(containsString("href=\"/auditoria/41\"")))
                .andExpect(content().string(containsString("aria-label=\"Ver detalle de la entrada 41\"")))
                .andExpect(content().string(containsString("bi-eye")))
                .andExpect(content().string(not(containsString("href=\"/auditoria/42\""))));
    }

    // ==================== Detalle ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-15: el detalle muestra la cabecera de la entrada y la tabla Campo / Estado anterior / Estado posterior con cada fila resaltada según su cambio")
    void detalle_debeMostrarLaCabeceraYLaComparacion() throws Exception {
        when(auditLogService.buscarPorId(7L)).thenReturn(detalle(7L, AccionAuditoria.MODIFICAR, true, List.of(
                campo("ID", "12", "12", EstadoCampoAuditoria.SIN_CAMBIOS),
                campo("Nombre", "Pilsen", "Pilsen Premium", EstadoCampoAuditoria.MODIFICADO),
                campo("Color", null, "Dorada", EstadoCampoAuditoria.AGREGADO),
                campo("Origen", "Alemania", null, EstadoCampoAuditoria.ELIMINADO))));

        mockMvc.perform(get("/auditoria/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("auditoria/auditoria-detalle"))
                .andExpect(content().string(containsString("<title>Bitácora de Auditoría - Maestro Cervecero</title>")))
                .andExpect(content().string(containsString("Detalle de auditoría")))
                .andExpect(content().string(containsString("Volver a la bitácora")))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(containsString("15/09/2026 10:30")))
                .andExpect(content().string(containsString("127.0.0.1")))
                .andExpect(content().string(containsString("Malta")))
                .andExpect(content().string(containsString(">Estado anterior</th>")))
                .andExpect(content().string(containsString(">Estado posterior</th>")))
                .andExpect(content().string(containsString("data-estado-campo=\"MODIFICADO\"")))
                .andExpect(content().string(containsString("bg-amber-50")))
                .andExpect(content().string(containsString("data-estado-campo=\"AGREGADO\"")))
                .andExpect(content().string(containsString("bg-emerald-50")))
                .andExpect(content().string(containsString("data-estado-campo=\"ELIMINADO\"")))
                .andExpect(content().string(containsString("bg-rose-50")))
                .andExpect(content().string(containsString("data-estado-campo=\"SIN_CAMBIOS\"")))
                .andExpect(content().string(containsString("Pilsen Premium")))
                .andExpect(content().string(containsString("Alemania")))
                .andExpect(content().string(not(containsString("No hay detalle de estados para este registro"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-16: el detalle de un alta muestra un guion en el estado anterior de todos los campos")
    void detalle_deUnAltaMuestraGuionEnElAnterior() throws Exception {
        when(auditLogService.buscarPorId(8L)).thenReturn(detalle(8L, AccionAuditoria.CREAR, true, List.of(
                campo("Nombre", null, "Cascade", EstadoCampoAuditoria.AGREGADO))));

        mockMvc.perform(get("/auditoria/8"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cascade")))
                .andExpect(content().string(containsString("text-slate-700\">-</td>")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-17: una entrada sin estados guardados muestra \"No hay detalle de estados para este registro\" y no la tabla")
    void detalle_sinEstadosMuestraElAvisoGris() throws Exception {
        when(auditLogService.buscarPorId(9L)).thenReturn(detalle(9L, AccionAuditoria.LOGIN_EXITOSO, false, List.of()));

        mockMvc.perform(get("/auditoria/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No hay detalle de estados para este registro")))
                .andExpect(content().string(containsString("text-slate-400")))
                .andExpect(content().string(not(containsString(">Estado anterior</th>"))))
                .andExpect(content().string(not(containsString("data-estado-campo"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-18: el detalle escapa todo el contenido: un valor con HTML se muestra como texto, nunca como marcado")
    void detalle_debeEscaparElContenido() throws Exception {
        when(auditLogService.buscarPorId(10L)).thenReturn(AuditLogDetalleResponseDTO.builder()
                .id(10L).username("<b>admin</b>").accion(AccionAuditoria.MODIFICAR).conceptoAuditoria(ConceptoAuditoria.MALTA)
                .entidadId("1").fechaHora(LocalDateTime.of(2026, 9, 15, 10, 30)).tieneDetalle(true)
                .campos(List.of(campo("Nombre", "<script>alert(1)</script>", "<img src=x onerror=alert(2)>", EstadoCampoAuditoria.MODIFICADO)))
                .build());

        mockMvc.perform(get("/auditoria/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
                .andExpect(content().string(containsString("&lt;b&gt;admin&lt;/b&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)"))))
                .andExpect(content().string(not(containsString("<img src=x"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-19: el detalle muestra solo la cabecera y la tabla comparativa, sin sección de JSON")
    void detalle_noMuestraElJson() throws Exception {
        when(auditLogService.buscarPorId(11L)).thenReturn(detalle(11L, AccionAuditoria.MODIFICAR, true, List.of(
                campo("Nombre", "A", "B", EstadoCampoAuditoria.MODIFICADO))));

        mockMvc.perform(get("/auditoria/11"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-seccion-estados")))
                .andExpect(content().string(containsString(">Estado anterior</th>")))
                .andExpect(content().string(not(containsString("<details"))))
                .andExpect(content().string(not(containsString("JSON"))))
                .andExpect(content().string(not(containsString("&quot;nombre&quot;"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-20: un ID inexistente redirige con la alerta del service")
    void detalle_conIdInexistenteRedirigeConAlerta() throws Exception {
        when(auditLogService.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el registro de auditoría con ID: 99"));

        mockMvc.perform(get("/auditoria/99").header("Referer", "/auditoria"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró el registro de auditoría con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-AC-21: un ID que no es numérico se rechaza sin llegar al service")
    void detalle_conIdInvalidoSeRechaza() throws Exception {
        mockMvc.perform(get("/auditoria/abc").header("Referer", "/auditoria"))
                .andExpect(status().is3xxRedirection());

        verify(auditLogService, never()).buscarPorId(any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-AC-22: un rol distinto de ADMINISTRADOR no puede ver el detalle (no llega al service)")
    void detalle_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/auditoria/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(auditLogService, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("CP-AC-23: un visitante sin sesión es enviado al login al pedir el detalle")
    void detalle_debeRedirigirAlLoginSiNoHaySesion() throws Exception {
        mockMvc.perform(get("/auditoria/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));

        verify(auditLogService, never()).buscarPorId(any());
    }

    private static CampoAuditoriaResponseDTO campo(String nombre, String anterior, String posterior, EstadoCampoAuditoria estado) {
        return CampoAuditoriaResponseDTO.builder().campo(nombre).valorAnterior(anterior).valorPosterior(posterior).estado(estado).build();
    }

    private static AuditLogDetalleResponseDTO detalle(Long id, AccionAuditoria accion, boolean tieneDetalle, List<CampoAuditoriaResponseDTO> campos) {
        return AuditLogDetalleResponseDTO.builder()
                .id(id).username("admin").accion(accion).conceptoAuditoria(ConceptoAuditoria.MALTA).entidadId("12")
                .fechaHora(LocalDateTime.of(2026, 9, 15, 10, 30)).ipAddress("127.0.0.1")
                .tieneDetalle(tieneDetalle).campos(campos)
                .build();
    }

    private static AuditLogResponseDTO entrada(Long id, String username, AccionAuditoria accion, ConceptoAuditoria concepto,
                                               String entidadId, LocalDateTime fechaHora, String ipAddress) {
        return AuditLogResponseDTO.builder()
                .id(id).username(username).accion(accion).conceptoAuditoria(concepto)
                .entidadId(entidadId).fechaHora(fechaHora).ipAddress(ipAddress)
                .build();
    }
}
