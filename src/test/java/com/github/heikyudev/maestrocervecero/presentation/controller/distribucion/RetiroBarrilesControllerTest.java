package com.github.heikyudev.maestrocervecero.presentation.controller.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.RetiroBarrilesFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRetiroInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRetiroBarrilesServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.BarrilRetiroResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.RetiroBarrilesResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RetiroBarrilesController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class RetiroBarrilesControllerTest {

    private static final String TOKEN = "kV2s9fJ1x0QmB7r3TzYwLp4cHn8aUe6dGqXo5iSjNkA";
    private static final String URL = "/retiro-barriles/" + TOKEN;
    private static final String VISTA = "retiro_barriles/retiro-barriles";
    private static final String VISTA_INVALIDO = "retiro_barriles/enlace-invalido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IRetiroBarrilesServicio retiroBarrilesServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    private static RetiroBarrilesResponseDTO pendiente() {
        return RetiroBarrilesResponseDTO.builder().nombreCliente("Bar Don Jorge").registrado(false)
                .barrilesPendientes(List.of(
                        BarrilRetiroResponseDTO.builder().numeroSerie("SN-1001").identificadorInterno("BRL-01").fechaDevolucionEstimada(LocalDate.of(2026, 11, 10)).build(),
                        BarrilRetiroResponseDTO.builder().numeroSerie("SN-1002").identificadorInterno("BRL-02").fechaDevolucionEstimada(LocalDate.of(2026, 11, 15)).build()))
                .fechaMaximaRetiro(LocalDateTime.of(2026, 11, 26, 9, 0)).diasMaximosAnticipacionRetiro(7)
                .barrilesRegistrados(List.of()).build();
    }

    private static RetiroBarrilesResponseDTO registrado() {
        return RetiroBarrilesResponseDTO.builder().nombreCliente("Bar Don Jorge").registrado(true).barrilesPendientes(List.of())
                .diasMaximosAnticipacionRetiro(7).fechaRespuesta(LocalDateTime.of(2026, 11, 19, 11, 5))
                .fechaRetiro(LocalDateTime.of(2026, 11, 25, 10, 30)).observaciones("Tocar timbre en el depósito")
                .barrilesRegistrados(List.of(
                        BarrilRetiroResponseDTO.builder().numeroSerie("SN-1001").identificadorInterno("BRL-01").estado(EstadoSolicitudBusqueda.BUSCADA).build(),
                        BarrilRetiroResponseDTO.builder().numeroSerie("SN-1002").identificadorInterno("BRL-02").estado(EstadoSolicitudBusqueda.PENDIENTE).build(),
                        BarrilRetiroResponseDTO.builder().numeroSerie("SN-1003").identificadorInterno("BRL-03").estado(EstadoSolicitudBusqueda.CANCELADA).build()))
                .build();
    }

    @Test
    @DisplayName("CP-RBC-01: la pantalla es pública, sin el menú de la aplicación, y muestra los barriles por número de serie con el formulario del retiro")
    void caso_cp_rbc_01() throws Exception {
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(pendiente());

        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("Retiro de barriles").contains("Bar Don Jorge")
                .contains("SN-1001").contains("SN-1002").contains("BRL-01").contains("10/11/2026")
                .contains("action=\"/retiro-barriles/" + TOKEN + "\"").contains("novalidate").contains("name=\"_csrf\"")
                .containsPattern("Fecha y hora del retiro <span class=\"text-rose-600\"[^>]*>\\*</span>")
                .contains("type=\"datetime-local\"").contains("max=\"2026-11-26T09:00\"").contains("26/11/2026 09:00")
                .contains("placeholder=\"Ej: 25/11/2026 10:30\"").contains("placeholder=\"Ej: Tocar timbre en el depósito de la calle lateral\"")
                .contains("Confirmar retiro")
                .contains("<meta name=\"referrer\" content=\"no-referrer\">")
                .doesNotContain("Cerrar sesión").doesNotContain("href=\"/home\"");
    }

    @Test
    @DisplayName("CP-RBC-02: un retiro ya coordinado se muestra en solo lectura con el estado de cada barril (retirado, pendiente o cancelado), sin formulario")
    void caso_cp_rbc_02() throws Exception {
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(registrado());

        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html)
                .contains("data-retiro-registrado").contains("25/11/2026 10:30").contains("19/11/2026 11:05")
                .contains("Tocar timbre en el depósito").contains("Retirado").contains("Pendiente de retiro").contains("Cancelado")
                .contains("no se puede modificar")
                .doesNotContain("<form").doesNotContain("Confirmar retiro");
    }

    @Test
    @DisplayName("CP-RBC-03: si ningún barril sigue pendiente se muestra el aviso, sin formulario")
    void caso_cp_rbc_03() throws Exception {
        RetiroBarrilesResponseDTO sinBarriles = RetiroBarrilesResponseDTO.builder().nombreCliente("Bar Don Jorge").registrado(false)
                .barrilesPendientes(List.of()).barrilesRegistrados(List.of()).diasMaximosAnticipacionRetiro(7)
                .fechaMaximaRetiro(LocalDateTime.of(2026, 11, 26, 9, 0)).build();
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(sinBarriles);

        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("data-sin-barriles").contains("ya fueron devueltos o su retiro ya está coordinado").doesNotContain("<form");
    }

    @Test
    @DisplayName("CP-RBC-04: un enlace que no sirve muestra la pantalla que explica el motivo")
    void caso_cp_rbc_04() throws Exception {
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenThrow(new EnlaceRetiroInvalidoException("El enlace para coordinar el retiro de barriles venció"));

        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_INVALIDO))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("El enlace no está disponible").contains("El enlace para coordinar el retiro de barriles venció")
                .doesNotContain("<form");
    }

    @Test
    @DisplayName("CP-RBC-05: confirmar delega la fecha y las observaciones en el service y vuelve a la pantalla con el aviso (Post-Redirect-Get)")
    void caso_cp_rbc_05() throws Exception {
        when(retiroBarrilesServicio.registrarRetiro(eq(TOKEN), any())).thenReturn(registrado());

        mockMvc.perform(post(URL).with(csrf()).param("fechaRetiro", "2026-11-25T10:30").param("observaciones", "Tocar timbre en el depósito"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Retiro coordinado correctamente para el 25/11/2026 10:30"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<RetiroBarrilesFormDTO> captor = ArgumentCaptor.forClass(RetiroBarrilesFormDTO.class);
        verify(retiroBarrilesServicio).registrarRetiro(eq(TOKEN), captor.capture());
        assertThat(captor.getValue().getFechaRetiro()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 30));
        assertThat(captor.getValue().getObservaciones()).isEqualTo("Tocar timbre en el depósito");
    }

    @Test
    @DisplayName("CP-RBC-06: sin fecha o con observaciones demasiado largas vuelve al formulario con el error junto al campo, sin llamar al registro")
    void caso_cp_rbc_06() throws Exception {
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(pendiente());

        String html = mockMvc.perform(post(URL).with(csrf()).param("fechaRetiro", "").param("observaciones", "x".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("No se pudo coordinar el retiro")
                .contains("La fecha y hora del retiro es obligatoria")
                .contains("Las observaciones no pueden superar 255 caracteres");
        verify(retiroBarrilesServicio, never()).registrarRetiro(any(), any());
    }

    @Test
    @DisplayName("CP-RBC-07: una regla de la fecha rechazada por el service vuelve al formulario con el error junto al campo y lo cargado")
    void caso_cp_rbc_07() throws Exception {
        when(retiroBarrilesServicio.registrarRetiro(eq(TOKEN), any()))
                .thenThrow(new ReglaNegocioException("La fecha y hora del retiro no puede superar 7 días desde la fecha y hora actual", "fechaRetiro"));
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(pendiente());

        String html = mockMvc.perform(post(URL).with(csrf()).param("fechaRetiro", "2026-12-30T10:30").param("observaciones", "Tocar timbre"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("La fecha y hora del retiro no puede superar 7 días desde la fecha y hora actual")
                .contains("name=\"fechaRetiro\" value=\"2026-12-30T10:30\"")
                .contains("Tocar timbre</textarea>");
    }

    @Test
    @DisplayName("CP-RBC-08: un envío sobre un retiro ya coordinado muestra el retiro registrado con el motivo del rechazo")
    void caso_cp_rbc_08() throws Exception {
        when(retiroBarrilesServicio.registrarRetiro(eq(TOKEN), any()))
                .thenThrow(new ReglaNegocioException("El retiro ya fue coordinado desde este enlace y no se puede modificar"));
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(registrado());

        String html = mockMvc.perform(post(URL).with(csrf()).param("fechaRetiro", "2026-11-25T10:30"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("El retiro ya fue coordinado desde este enlace y no se puede modificar").contains("data-retiro-registrado");
    }

    @Test
    @DisplayName("CP-RBC-09: si el enlace dejó de servir al confirmar, se muestra la pantalla que explica el motivo")
    void caso_cp_rbc_09() throws Exception {
        when(retiroBarrilesServicio.registrarRetiro(eq(TOKEN), any()))
                .thenThrow(new EnlaceRetiroInvalidoException("El enlace fue reemplazado por uno más reciente: use el enlace del último correo recibido"));

        mockMvc.perform(post(URL).with(csrf()).param("fechaRetiro", "2026-11-25T10:30"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_INVALIDO));
    }

    @Test
    @DisplayName("CP-RBC-10: la pantalla responde con Referrer-Policy no-referrer, sin crear la sesión de la aplicación, y rechaza el envío sin token CSRF")
    void caso_cp_rbc_10() throws Exception {
        when(retiroBarrilesServicio.buscarRetiro(TOKEN)).thenReturn(pendiente());

        MockHttpServletResponse respuesta = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andReturn().getResponse();
        assertThat(respuesta.getCookie("JSESSIONID")).isNull();

        mockMvc.perform(post(URL).param("fechaRetiro", "2026-11-25T10:30")).andExpect(status().isForbidden());
        verify(retiroBarrilesServicio, never()).registrarRetiro(any(), any());
    }
}
