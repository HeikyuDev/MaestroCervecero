package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.CriterioPlanCompra;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.ConfiguracionComprasFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.IConfiguracionComprasServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ConfiguracionComprasResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ConfiguracionComprasController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class ConfiguracionComprasControllerTest {

    private static final String URL = "/configuracion-compras";
    private static final String VISTA = "configuracion/configuracion-compras";
    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IConfiguracionComprasServicio configuracionComprasServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    private static ConfiguracionComprasResponseDTO configuracion() {
        return ConfiguracionComprasResponseDTO.builder().id(1L).plazoRespuestaHoras(48)
                .criterioPlanCompra(CriterioPlanCompra.PONDERADO).pesoPrecio(50.0).build();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-CCC-01: el GERENTE_DE_COMPRAS ve la configuración vigente con asteriscos, placeholders, unidades, ayudas y sin ceros sobrantes")
    void caso_cp_ccc_01() throws Exception {
        // Arrange
        when(configuracionComprasServicio.buscarConfiguracion()).thenReturn(configuracion());

        // Act
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("Configuración de Compras")
                .contains("action=\"/configuracion-compras\"").contains("novalidate")
                .contains("Solicitudes de cotización").contains("Plan de órdenes de compra")
                .contains("Plazo de respuesta " + OBLIGATORIO).doesNotContain("Cantidad de planes")
                .contains("Criterio del plan " + OBLIGATORIO).contains("Peso del precio " + OBLIGATORIO)
                .contains("name=\"plazoRespuestaHoras\" value=\"48\"").doesNotContain("cantidadPlanes")
                .contains("name=\"pesoPrecio\" value=\"50\"").doesNotContain("value=\"50.0\"")
                .contains("placeholder=\"Ej: 48\"").contains("placeholder=\"Ej: 50\"")
                .contains("<option value=\"PONDERADO\" selected=\"selected\">Ponderado entre precio y cantidad</option>")
                .contains("Menor precio").contains("Mayor cantidad disponible")
                .contains("Un número entero mayor a 0 y hasta 720")
                .contains("Solo se usa con el criterio ponderado")
                .contains("href=\"/configuracion-compras\"")
                .doesNotContain("No se pudo guardar");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-CCC-02: guardar delega el formulario completo en el service y redirige con alerta de éxito")
    void caso_cp_ccc_02() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("plazoRespuestaHoras", "24")
                        .param("criterioPlanCompra", "MENOR_PRECIO").param("pesoPrecio", "70.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Configuración de compras actualizada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ConfiguracionComprasFormDTO> captor = ArgumentCaptor.forClass(ConfiguracionComprasFormDTO.class);
        verify(configuracionComprasServicio).actualizarConfiguracion(captor.capture());
        assertThat(captor.getValue().getPlazoRespuestaHoras()).isEqualTo(24);
        assertThat(captor.getValue().getCriterioPlanCompra()).isEqualTo(CriterioPlanCompra.MENOR_PRECIO);
        assertThat(captor.getValue().getPesoPrecio()).isEqualTo(70.5);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-CCC-03: sin datos vuelve al formulario con cada mensaje obligatorio, sin llamar al service")
    void caso_cp_ccc_03() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("plazoRespuestaHoras", "")
                        .param("criterioPlanCompra", "").param("pesoPrecio", ""))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andExpect(content().string(containsString("No se pudo guardar la configuración")))
                .andExpect(content().string(containsString("El plazo de respuesta es obligatorio")))
                .andExpect(content().string(containsString("El criterio del plan es obligatorio")))
                .andExpect(content().string(containsString("El peso del precio es obligatorio")));

        verify(configuracionComprasServicio, never()).actualizarConfiguracion(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-CCC-04: los topes y rangos se rechazan en el formulario con el mensaje de cada campo")
    void caso_cp_ccc_04() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("plazoRespuestaHoras", "721")
                        .param("criterioPlanCompra", "PONDERADO").param("pesoPrecio", "100.5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El plazo de respuesta no puede superar 720 horas")))
                .andExpect(content().string(containsString("El peso del precio debe estar entre 0 y 100")));
        mockMvc.perform(post(URL).with(csrf()).param("plazoRespuestaHoras", "0")
                        .param("criterioPlanCompra", "PONDERADO").param("pesoPrecio", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El plazo de respuesta debe ser mayor a 0")))
                .andExpect(content().string(containsString("El peso del precio debe estar entre 0 y 100")));

        verify(configuracionComprasServicio, never()).actualizarConfiguracion(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-CCC-05: una regla rechazada por el service vuelve al formulario con el motivo y lo tipeado")
    void caso_cp_ccc_05() throws Exception {
        when(configuracionComprasServicio.actualizarConfiguracion(any())).thenThrow(new ReglaNegocioException("El peso del precio debe estar entre 0 y 100"));

        mockMvc.perform(post(URL).with(csrf()).param("plazoRespuestaHoras", "24")
                        .param("criterioPlanCompra", "MENOR_PRECIO").param("pesoPrecio", "30"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andExpect(content().string(containsString("El peso del precio debe estar entre 0 y 100")))
                .andExpect(content().string(containsString("name=\"plazoRespuestaHoras\" value=\"24\"")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DISTRIBUCION"})
    @DisplayName("CP-CCC-06: ningún otro rol accede a la configuración de compras")
    void caso_cp_ccc_06(String rol) throws Exception {
        mockMvc.perform(get(URL).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post(URL).with(csrf()).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection());

        verify(configuracionComprasServicio, never()).actualizarConfiguracion(any());
    }
}
