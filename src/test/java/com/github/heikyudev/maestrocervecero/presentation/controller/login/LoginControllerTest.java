package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(LoginController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @DisplayName("CP-LGC-01: la pantalla de login es pública y no muestra ningún aviso cuando no llegan parámetros")
    void login_sinParametrosNoMuestraAvisos() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("Iniciar sesión")))
                .andExpect(content().string(containsString("Nombre de usuario")))
                .andExpect(content().string(not(containsString("Su sesión"))))
                .andExpect(content().string(not(containsString("Sesión cerrada correctamente"))))
                .andExpect(content().string(not(containsString("Usuario o contraseña incorrectos"))));
    }

    @Test
    @DisplayName("CP-LGC-02: con el parámetro error muestra \"Usuario o contraseña incorrectos\"")
    void login_conErrorMuestraCredencialesIncorrectas() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario o contraseña incorrectos")));
    }

    @Test
    @DisplayName("CP-LGC-03: con el parámetro logout muestra \"Sesión cerrada correctamente\"")
    void login_conLogoutMuestraSesionCerrada() throws Exception {
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sesión cerrada correctamente")))
                .andExpect(content().string(not(containsString("Has cerrado sesión"))));
    }

    @Test
    @DisplayName("CP-LGC-04: con el parámetro timeout (la cookie de sesión ya no es válida) avisa que la sesión venció por inactividad")
    void login_conTimeoutAvisaQueLaSesionVencio() throws Exception {
        mockMvc.perform(get("/login").param("timeout", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("mensaje", "Su sesión venció por inactividad, inicie sesión nuevamente"))
                .andExpect(model().attribute("tipo", "warning"))
                .andExpect(content().string(containsString("Su sesión venció por inactividad, inicie sesión nuevamente")))
                .andExpect(content().string(containsString("bi-exclamation-triangle-fill")));
    }

    @Test
    @DisplayName("CP-LGC-05: con el parámetro expired (la sesión fue desplazada por otro inicio) avisa que se inició en otro dispositivo")
    void login_conExpiredAvisaQueSeInicioEnOtroDispositivo() throws Exception {
        mockMvc.perform(get("/login").param("expired", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("mensaje", "Su sesión se cerró porque se inició en otro dispositivo"))
                .andExpect(model().attribute("tipo", "warning"))
                .andExpect(content().string(containsString("Su sesión se cerró porque se inició en otro dispositivo")));
    }

    @Test
    @DisplayName("CP-LGC-06: los parámetros mensaje y tipo de la URL se ignoran y nunca se muestran en la pantalla pública")
    void login_ignoraMensajeYTipoRecibidosPorParametro() throws Exception {
        mockMvc.perform(get("/login").param("mensaje", "Comuniquese al 0800-FALSO").param("tipo", "danger"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("mensaje"))
                .andExpect(model().attributeDoesNotExist("tipo"))
                .andExpect(content().string(not(containsString("Comuniquese al 0800-FALSO"))));
    }

    @Test
    @DisplayName("CP-LGC-07: un aviso enviado como atributo flash por otra pantalla se muestra y prevalece sobre el aviso de timeout")
    void login_muestraElAvisoFlashDeOtraPantalla() throws Exception {
        mockMvc.perform(get("/login").param("timeout", "").flashAttr("mensaje", "Cambió su nombre de usuario: inicie sesión nuevamente").flashAttr("tipo", "info"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mensaje", "Cambió su nombre de usuario: inicie sesión nuevamente"))
                .andExpect(content().string(containsString("Cambió su nombre de usuario: inicie sesión nuevamente")))
                .andExpect(content().string(not(containsString("Su sesión venció por inactividad"))));
    }
}
