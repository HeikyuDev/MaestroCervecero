package com.github.heikyudev.maestrocervecero.presentation.controller.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.distribucion.EnvioRecordatoriosRetiroProgramado;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ISolicitudBusquedaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DespachoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.control_barril.ControlBarrilControllerTest.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SolicitudRetiroController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class SolicitudRetiroControllerTest {

    private static final String URL = "/solicitudes-retiro";
    private static final String VISTA_LISTA = "retiro_barriles/solicitudes-lista";
    private static final String VISTA_DETALLE = "retiro_barriles/solicitud-detalle";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ISolicitudBusquedaServicio solicitudBusquedaServicio;

    @MockitoBean
    private IClienteServicio clienteServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @MockitoBean
    private EnvioRecordatoriosRetiroProgramado envioRecordatoriosRetiro;

    private static SolicitudBusquedaResponseDTO solicitud(Long id, LocalDateTime fechaBusqueda, EstadoSolicitudBusqueda estado) {
        return SolicitudBusquedaResponseDTO.builder().id(id).fechaBusqueda(fechaBusqueda).estado(estado)
                .observaciones("Tocar timbre en el depósito").fechaRegistro(LocalDateTime.of(2026, 11, 18, 9, 30))
                .despachoBarril(DespachoBarrilResponseDTO.builder().id(30L).fechaDevolucionEstimada(LocalDate.of(2026, 11, 10))
                        .barril(BarrilResponseDTO.builder().id(201L).numeroSerie("SN-1001").identificadorInterno("BRL-01").build())
                        .cliente(ClienteResponseDTO.builder().id(5L).nombre("Bar Don Jorge").build())
                        .build())
                .build();
    }

    @Test
    @DisplayName("CP-SRC-01: la lista muestra la última solicitud arriba, con el buscador de clientes en modal, los filtros y la opción activa en la sidebar")
    void caso_cp_src_01() throws Exception {
        // Arrange
        when(solicitudBusquedaServicio.filtrarSolicitudesBusqueda(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(solicitud(71L, LocalDateTime.now().plusDays(2), EstadoSolicitudBusqueda.PENDIENTE), solicitud(70L, LocalDateTime.now().minusDays(1), EstadoSolicitudBusqueda.BUSCADA),
                        solicitud(69L, LocalDateTime.now().minusDays(2), EstadoSolicitudBusqueda.CANCELADA))));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_LISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudBusquedaServicio).filtrarSolicitudesBusqueda(isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(html)
                .contains("Solicitudes de Retiro").contains("data-buscador-url=\"/clientes/buscador\"").contains("Seleccione cliente")
                .contains("<option value=\"PENDIENTE\">Pendiente</option>").contains("<option value=\"BUSCADA\">Buscada</option>").contains("<option value=\"CANCELADA\">Cancelada</option>")
                .contains("placeholder=\"Ej: 01/11/2026\"").contains("Bar Don Jorge").contains("SN-1001").contains("BRL-01")
                .contains("Tocar timbre en el depósito").contains("Registrada 18/11/2026 09:30")
                .contains("href=\"/solicitudes-retiro/71\"").contains("bi-eye")
                .contains(">Pendiente</span>").contains(">Buscada</span>").contains(">Cancelada</span>").doesNotContain("Pendiente vencida")
                .doesNotContain("Nueva solicitud");
        assertThat(html.indexOf("data-solicitud=\"71\"")).isLessThan(html.indexOf("data-solicitud=\"70\""));
    }

    @Test
    @DisplayName("CP-SRC-02: los filtros de cliente, rango de fecha y estado llegan al service y una pendiente con la fecha pasada se resalta como vencida")
    void caso_cp_src_02() throws Exception {
        when(clienteServicio.buscarPorId(5L)).thenReturn(ClienteResponseDTO.builder().id(5L).nombre("Bar Don Jorge").build());
        when(solicitudBusquedaServicio.filtrarSolicitudesBusqueda(eq(5L), any(), any(), eq(EstadoSolicitudBusqueda.PENDIENTE), any()))
                .thenReturn(new PageImpl<>(List.of(solicitud(71L, LocalDateTime.now().minusHours(3), EstadoSolicitudBusqueda.PENDIENTE))));

        String html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION))
                        .param("idCliente", "5").param("fechaDesde", "2026-11-01").param("fechaHasta", "2026-11-30").param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        verify(solicitudBusquedaServicio).filtrarSolicitudesBusqueda(eq(5L), eq(LocalDateTime.of(2026, 11, 1, 0, 0)),
                eq(LocalDate.of(2026, 11, 30).atTime(java.time.LocalTime.MAX)), eq(EstadoSolicitudBusqueda.PENDIENTE), any());
        assertThat(html).contains("value=\"Bar Don Jorge\"").contains("Pendiente vencida").contains("text-rose-700")
                .contains("<option value=\"PENDIENTE\" selected=\"selected\">Pendiente</option>");
    }

    @Test
    @DisplayName("CP-SRC-03: un estado inválido se avisa y muestra todas; un rango invertido muestra el aviso del service y la lista vacía")
    void caso_cp_src_03() throws Exception {
        when(solicitudBusquedaServicio.filtrarSolicitudesBusqueda(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of()));
        String html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).param("estado", "CUALQUIERA"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("El estado indicado no es válido, se muestran todas las solicitudes");

        when(solicitudBusquedaServicio.filtrarSolicitudesBusqueda(isNull(), any(), any(), isNull(), any()))
                .thenThrow(new ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta"));
        html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).param("fechaDesde", "2026-11-30").param("fechaHasta", "2026-11-01"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("La fecha desde no puede ser posterior a la fecha hasta")
                .contains("No se encontraron solicitudes de retiro con los criterios indicados");
    }

    @Test
    @DisplayName("CP-SRC-04: el detalle muestra en solo lectura el cliente, el barril, la fecha propuesta, el estado y las observaciones")
    void caso_cp_src_04() throws Exception {
        when(solicitudBusquedaServicio.buscarPorId(71L)).thenReturn(solicitud(71L, LocalDateTime.of(2030, 11, 25, 10, 30), EstadoSolicitudBusqueda.PENDIENTE));

        String html = mockMvc.perform(get(URL + "/71").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_DETALLE))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("<title>Solicitudes de retiro - Maestro Cervecero</title>").contains("Solicitud de retiro #71").contains("25/11/2030 10:30").contains("Bar Don Jorge")
                .contains("SN-1001").contains("(BRL-01)").contains("10/11/2026").contains("Tocar timbre en el depósito")
                .contains("data-estado-solicitud").contains(">Pendiente<").contains("href=\"/solicitudes-retiro\"")
                .doesNotContain("data-aviso-vencida").doesNotContain("<form method=\"post\"");
    }

    @Test
    @DisplayName("CP-SRC-05: una solicitud inexistente muestra el aviso del ControllerAdvices con el nombre de la pantalla (solicitud de retiro)")
    void caso_cp_src_05() throws Exception {
        when(solicitudBusquedaServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la solicitud de búsqueda con ID: 99"));

        mockMvc.perform(get(URL + "/99").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró la solicitud de retiro con ID: 99"));
    }

    @Test
    @DisplayName("CP-SRC-07: una solicitud cancelada por la anulación del despacho se muestra cancelada, nunca vencida, y se puede filtrar por ese estado")
    void caso_cp_src_07() throws Exception {
        when(solicitudBusquedaServicio.buscarPorId(69L)).thenReturn(solicitud(69L, LocalDateTime.now().minusDays(2), EstadoSolicitudBusqueda.CANCELADA));
        String html = mockMvc.perform(get(URL + "/69").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains(">Cancelada<").doesNotContain("data-aviso-vencida").doesNotContain("Pendiente vencida");

        when(solicitudBusquedaServicio.filtrarSolicitudesBusqueda(isNull(), isNull(), isNull(), eq(EstadoSolicitudBusqueda.CANCELADA), any()))
                .thenReturn(new PageImpl<>(List.of(solicitud(69L, LocalDateTime.now().minusDays(2), EstadoSolicitudBusqueda.CANCELADA))));
        html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).param("estado", "cancelada"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("<option value=\"CANCELADA\" selected=\"selected\">Cancelada</option>").contains(">Cancelada</span>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR", "OPERARIO_DE_PRODUCCION"})
    @DisplayName("CP-SRC-06: ningún otro rol accede a las solicitudes de retiro")
    void caso_cp_src_06(String rol) throws Exception {
        mockMvc.perform(get(URL).with(como(Rol.valueOf(rol)))).andExpect(status().is3xxRedirection());
        mockMvc.perform(get(URL + "/71").with(user("usuario").roles(rol))).andExpect(status().is3xxRedirection());

        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.valueOf(rol))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(solicitudBusquedaServicio, clienteServicio, envioRecordatoriosRetiro);
    }

    // ==================== Envío de los recordatorios de retiro ====================

    @Test
    @DisplayName("CP-SRC-08: el encabezado de la lista ofrece enviar los recordatorios de retiro con un diálogo de confirmación; sin el permiso no se muestra")
    void caso_cp_src_08() throws Exception {
        when(solicitudBusquedaServicio.filtrarSolicitudesBusqueda(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        String html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String sinPermiso = mockMvc.perform(get(URL).with(user("consulta").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SOLICITUD_BUSQUEDA_CONSULTAR"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("action=\"/solicitudes-retiro/recordatorios\"").contains("data-boton-enviar-recordatorios")
                .contains("data-confirmar-titulo=\"Enviar recordatorios de retiro\"")
                .contains("sin esperar la hora configurada")
                .containsPattern("<i class=\"bi bi-envelope\"></i> Enviar recordatorios de retiro")
                .contains("id=\"dialogo-confirmacion\"")
                .doesNotContain("Simular");
        assertThat(sinPermiso).contains("Solicitudes de Retiro").doesNotContain("data-boton-enviar-recordatorios");
    }

    @Test
    @DisplayName("CP-SRC-09: enviar los recordatorios vuelve a la lista con cuántos clientes se notificaron en este envío, en singular o plural, o con que no quedaba a quién recordarles")
    void caso_cp_src_09() throws Exception {
        when(envioRecordatoriosRetiro.enviarRecordatoriosDeHoy())
                .thenReturn(new EnvioRecordatoriosRetiroProgramado.ResultadoEnvio(3, true))
                .thenReturn(new EnvioRecordatoriosRetiroProgramado.ResultadoEnvio(1, true))
                .thenReturn(new EnvioRecordatoriosRetiroProgramado.ResultadoEnvio(0, true));

        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Se enviaron recordatorios de retiro a 3 clientes en este envío"))
                .andExpect(flash().attribute("tipo", "success"));
        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()))
                .andExpect(flash().attribute("mensaje", "Se enviaron recordatorios de retiro a 1 cliente en este envío"));
        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()))
                .andExpect(flash().attribute("mensaje", "No quedan clientes a los que recordarles el retiro hoy"))
                .andExpect(flash().attribute("tipo", "info"));
    }

    @Test
    @DisplayName("CP-SRC-10: un envío incompleto avisa que algunos fallaron; si los de hoy ya se enviaron vuelve al origen con el motivo; sin CSRF no envía nada")
    void caso_cp_src_10() throws Exception {
        when(envioRecordatoriosRetiro.enviarRecordatoriosDeHoy())
                .thenReturn(new EnvioRecordatoriosRetiroProgramado.ResultadoEnvio(2, false))
                .thenThrow(new ReglaNegocioException("Los recordatorios de retiro de hoy ya se enviaron: los que fallaron se reintentan automáticamente"));

        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Se enviaron recordatorios de retiro a 2 clientes en este envío, pero algunos envíos fallaron: se reintentan automáticamente"))
                .andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Los recordatorios de retiro de hoy ya se enviaron: los que fallaron se reintentan automáticamente"));
        org.mockito.Mockito.clearInvocations(envioRecordatoriosRetiro);
        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().is3xxRedirection());

        verify(envioRecordatoriosRetiro, never()).enviarRecordatoriosDeHoy();
    }

    @Test
    @DisplayName("CP-SRC-11: si fallan todos los correos avisa que no se pudo notificar a nadie; si hay un envío en curso vuelve al origen con el motivo")
    void caso_cp_src_11() throws Exception {
        when(envioRecordatoriosRetiro.enviarRecordatoriosDeHoy())
                .thenReturn(new EnvioRecordatoriosRetiroProgramado.ResultadoEnvio(0, false))
                .thenThrow(new ReglaNegocioException("El envío de recordatorios está en curso, intente nuevamente en unos minutos"));

        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se envió ningún recordatorio de retiro en este envío porque algunos envíos fallaron: se reintentan automáticamente"))
                .andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(post(URL + "/recordatorios").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "El envío de recordatorios está en curso, intente nuevamente en unos minutos"));
    }

    @Test
    @DisplayName("CP-SRC-12: enviar los recordatorios exige a la vez consultar las solicitudes de búsqueda y enviar los recordatorios")
    void caso_cp_src_12() throws Exception {
        mockMvc.perform(post(URL + "/recordatorios").with(csrf()).with(user("solo-envio").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("RECORDATORIO_RETIRO_ENVIAR"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/recordatorios").with(csrf()).with(user("solo-consulta").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SOLICITUD_BUSQUEDA_CONSULTAR"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(envioRecordatoriosRetiro);

        when(envioRecordatoriosRetiro.enviarRecordatoriosDeHoy()).thenReturn(new EnvioRecordatoriosRetiroProgramado.ResultadoEnvio(1, true));
        mockMvc.perform(post(URL + "/recordatorios").with(csrf()).with(user("ambos").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("SOLICITUD_BUSQUEDA_CONSULTAR"),
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("RECORDATORIO_RETIRO_ENVIAR"))))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("tipo", "success"));
    }
}
