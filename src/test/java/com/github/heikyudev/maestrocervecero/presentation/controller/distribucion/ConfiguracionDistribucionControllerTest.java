package com.github.heikyudev.maestrocervecero.presentation.controller.distribucion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.ConfiguracionDistribucionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IConfiguracionDistribucionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.ConfiguracionDistribucionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;

import static com.github.heikyudev.maestrocervecero.presentation.controller.control_barril.ControlBarrilControllerTest.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ConfiguracionDistribucionController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class ConfiguracionDistribucionControllerTest {

    private static final String URL = "/configuracion-distribucion";
    private static final String VISTA = "configuracion/configuracion-distribucion";
    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IConfiguracionDistribucionServicio configuracionDistribucionServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    private static ConfiguracionDistribucionResponseDTO configuracion() {
        return ConfiguracionDistribucionResponseDTO.builder().id(1L).horaEnvioRecordatorioRetiro(LocalTime.of(9, 0)).diasMaximosAnticipacionRetiro(7).build();
    }

    @Test
    @DisplayName("CP-CDC-01: el ENCARGADO_DE_DISTRIBUCION ve la configuración vigente con asteriscos, placeholders, unidad y ayudas, y la opción activa en la sidebar")
    void caso_cp_cdc_01() throws Exception {
        // Arrange
        when(configuracionDistribucionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("<title>Configuración de distribución - Maestro Cervecero</title>")
                .contains("Configuración de Distribución")
                .contains("action=\"/configuracion-distribucion\"").contains("novalidate")
                .contains("Retiro de barriles")
                .contains("Hora de envío del recordatorio " + OBLIGATORIO)
                .contains("Días máximos de anticipación del retiro " + OBLIGATORIO)
                .contains("type=\"time\"").contains("name=\"horaEnvioRecordatorioRetiro\" value=\"09:00\"")
                .contains("name=\"diasMaximosAnticipacionRetiro\" value=\"7\"")
                .contains("placeholder=\"Ej: 09:00\"").contains("placeholder=\"Ej: 7\"").contains(">días<")
                .contains("Un número entero entre 1 y 30")
                .contains(">Configuración<").contains("href=\"/configuracion-distribucion\"")
                .doesNotContain("No se pudo guardar");
    }

    @Test
    @DisplayName("CP-CDC-02: guardar delega el formulario completo en el service y redirige con alerta de éxito")
    void caso_cp_cdc_02() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(como(Rol.ENCARGADO_DE_DISTRIBUCION))
                        .param("horaEnvioRecordatorioRetiro", "10:30").param("diasMaximosAnticipacionRetiro", "14"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Configuración de distribución actualizada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ConfiguracionDistribucionFormDTO> captor = ArgumentCaptor.forClass(ConfiguracionDistribucionFormDTO.class);
        verify(configuracionDistribucionServicio).actualizarConfiguracion(captor.capture());
        assertThat(captor.getValue().getHoraEnvioRecordatorioRetiro()).isEqualTo(LocalTime.of(10, 30));
        assertThat(captor.getValue().getDiasMaximosAnticipacionRetiro()).isEqualTo(14);
    }

    @Test
    @DisplayName("CP-CDC-03: sin datos vuelve al formulario con cada mensaje obligatorio, sin llamar al service")
    void caso_cp_cdc_03() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(como(Rol.ENCARGADO_DE_DISTRIBUCION))
                        .param("horaEnvioRecordatorioRetiro", "").param("diasMaximosAnticipacionRetiro", ""))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andExpect(content().string(containsString("No se pudo guardar la configuración")))
                .andExpect(content().string(containsString("La hora de envío del recordatorio es obligatoria")))
                .andExpect(content().string(containsString("Los días máximos de anticipación del retiro son obligatorios")));

        verify(configuracionDistribucionServicio, never()).actualizarConfiguracion(any());
    }

    @Test
    @DisplayName("CP-CDC-04: los días fuera de 1 a 30 se rechazan en el formulario con su mensaje")
    void caso_cp_cdc_04() throws Exception {
        for (String dias : new String[]{"0", "31"}) {
            mockMvc.perform(post(URL).with(csrf()).with(como(Rol.ENCARGADO_DE_DISTRIBUCION))
                            .param("horaEnvioRecordatorioRetiro", "09:00").param("diasMaximosAnticipacionRetiro", dias))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Los días máximos de anticipación del retiro deben estar entre 1 y 30")));
        }
        verify(configuracionDistribucionServicio, never()).actualizarConfiguracion(any());
    }

    @Test
    @DisplayName("CP-CDC-05: una regla rechazada por el service vuelve al formulario con el motivo y lo tipeado")
    void caso_cp_cdc_05() throws Exception {
        when(configuracionDistribucionServicio.actualizarConfiguracion(any()))
                .thenThrow(new ReglaNegocioException("Los días máximos de anticipación del retiro deben estar entre 1 y 30"));

        mockMvc.perform(post(URL).with(csrf()).with(como(Rol.ENCARGADO_DE_DISTRIBUCION))
                        .param("horaEnvioRecordatorioRetiro", "08:15").param("diasMaximosAnticipacionRetiro", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andExpect(content().string(containsString("Los días máximos de anticipación del retiro deben estar entre 1 y 30")))
                .andExpect(content().string(containsString("name=\"horaEnvioRecordatorioRetiro\" value=\"08:15\"")))
                .andExpect(content().string(containsString("name=\"diasMaximosAnticipacionRetiro\" value=\"5\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR", "OPERARIO_DE_PRODUCCION"})
    @DisplayName("CP-CDC-06: ningún otro rol accede a la configuración de distribución")
    void caso_cp_cdc_06(String rol) throws Exception {
        mockMvc.perform(get(URL).with(user("usuario").roles(rol))).andExpect(status().is3xxRedirection());
        mockMvc.perform(post(URL).with(csrf()).with(user("usuario").roles(rol))).andExpect(status().is3xxRedirection());

        verify(configuracionDistribucionServicio, never()).actualizarConfiguracion(any());
    }
}
