package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.RestablecimientoContrasenaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRestablecimientoInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IRestablecimientoContrasenaServicio;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RestablecimientoContrasenaController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class RestablecimientoContrasenaControllerTest {

    private static final String MENSAJE_SOLICITUD = "Si el nombre de usuario existe, se envió un correo con las instrucciones para restablecer la contraseña";
    private static final String TOKEN = "Q2xhdmVEZVBydWViYVBhcmFFbFJlc3RhYmxlY2ltaWVudG8";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @MockitoBean
    private IRestablecimientoContrasenaServicio restablecimientoContrasenaServicio;

    // ==================== GET /recuperar-contrasena ====================

    @Test
    @DisplayName("CP-RCT-01: la pantalla para pedir el enlace es pública y pide el nombre de usuario obligatorio, con placeholder y sin validación del navegador")
    void mostrarFormularioSolicitud_esPublicaYPideElNombreDeUsuario() throws Exception {
        mockMvc.perform(get("/recuperar-contrasena"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/recuperar-contrasena"))
                .andExpect(model().attributeExists("solicitudForm"))
                .andExpect(content().string(containsString("Nombre de usuario <span class=\"text-rose-500\" title=\"Obligatorio\">*</span>")))
                .andExpect(content().string(containsString("placeholder=\"Ej: admin\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("href=\"/login\"")));
    }

    // ==================== POST /recuperar-contrasena ====================

    @Test
    @DisplayName("CP-RCT-02: pedir el enlace deja el pedido al service con el nombre de usuario y la IP del cliente y vuelve al inicio de sesión con el aviso neutral")
    void solicitarRestablecimiento_redirigeAlLoginConElAvisoNeutral() throws Exception {
        mockMvc.perform(post("/recuperar-contrasena").with(csrf()).param("username", "jperez"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje", MENSAJE_SOLICITUD))
                .andExpect(flash().attribute("tipo", "info"));

        verify(restablecimientoContrasenaServicio).solicitarRestablecimiento("jperez", "127.0.0.1");
    }

    @Test
    @DisplayName("CP-RCT-03: si el service falla, la respuesta es la misma redirección con el mismo aviso neutral")
    void solicitarRestablecimiento_fallaDelServiceNoCambiaLaRespuesta() throws Exception {
        doThrow(new IllegalStateException("Base de datos no disponible"))
                .when(restablecimientoContrasenaServicio).solicitarRestablecimiento(anyString(), any());

        mockMvc.perform(post("/recuperar-contrasena").with(csrf()).param("username", "jperez"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje", MENSAJE_SOLICITUD))
                .andExpect(flash().attribute("tipo", "info"));
    }

    @Test
    @DisplayName("CP-RCT-04: un nombre de usuario vacío vuelve a mostrar el formulario con el error bajo el campo y no llama al service")
    void solicitarRestablecimiento_usernameVacioMuestraElError() throws Exception {
        mockMvc.perform(post("/recuperar-contrasena").with(csrf()).param("username", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("login/recuperar-contrasena"))
                .andExpect(model().attributeHasFieldErrors("solicitudForm", "username"))
                .andExpect(content().string(containsString("El nombre de usuario es obligatorio")));

        verifyNoInteractions(restablecimientoContrasenaServicio);
    }

    @Test
    @DisplayName("CP-RCT-05: pedir el enlace sin token CSRF es rechazado (redirige al inicio de sesión por sesión inválida) y no llama al service")
    void solicitarRestablecimiento_sinCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/recuperar-contrasena").param("username", "jperez"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?timeout"));

        verifyNoInteractions(restablecimientoContrasenaServicio);
    }

    // ==================== GET /restablecer-contrasena ====================

    @Test
    @DisplayName("CP-RCT-06: con un enlace que sirve, muestra el formulario de la contraseña nueva con el token aparte y los botones para mostrar la contraseña")
    void mostrarFormularioRestablecimiento_enlaceValidoMuestraElFormulario() throws Exception {
        mockMvc.perform(get("/restablecer-contrasena").param("token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(view().name("login/restablecer-contrasena"))
                .andExpect(model().attributeExists("restablecimientoForm"))
                .andExpect(model().attribute("token", TOKEN))
                .andExpect(content().string(containsString("name=\"token\" value=\"" + TOKEN + "\"")))
                .andExpect(content().string(containsString("data-alternar-contrasena=\"password\"")))
                .andExpect(content().string(containsString("data-alternar-contrasena=\"confirmacionPassword\"")))
                .andExpect(content().string(containsString("aria-label=\"Mostrar contraseña\"")))
                .andExpect(content().string(containsString("/js/mostrar-contrasena.js")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("<meta name=\"referrer\" content=\"no-referrer\">")))
                .andExpect(content().string(containsString(MetodosPassword.MENSAJE_INVALIDA)));

        verify(restablecimientoContrasenaServicio).validarEnlace(TOKEN);
    }

    @Test
    @DisplayName("CP-RCT-07: con un enlace que no sirve, muestra el motivo y ofrece pedir un enlace nuevo")
    void mostrarFormularioRestablecimiento_enlaceInvalidoOfrecePedirOtro() throws Exception {
        doThrow(new EnlaceRestablecimientoInvalidoException("El enlace para restablecer la contraseña ya fue utilizado"))
                .when(restablecimientoContrasenaServicio).validarEnlace(TOKEN);

        mockMvc.perform(get("/restablecer-contrasena").param("token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(view().name("login/enlace-invalido"))
                .andExpect(model().attribute("mensaje", "El enlace para restablecer la contraseña ya fue utilizado"))
                .andExpect(model().attribute("tipo", "warning"))
                .andExpect(content().string(containsString("El enlace para restablecer la contraseña ya fue utilizado")))
                .andExpect(content().string(containsString("href=\"/recuperar-contrasena\"")))
                .andExpect(content().string(containsString("Solicitar un enlace nuevo")))
                .andExpect(content().string(not(containsString(TOKEN))));
    }

    // ==================== POST /restablecer-contrasena ====================

    @Test
    @DisplayName("CP-RCT-08: una contraseña nueva válida llama al service con el token y el formulario y vuelve al inicio de sesión con el aviso de éxito")
    void restablecerContrasena_validaRedirigeAlLogin() throws Exception {
        mockMvc.perform(post("/restablecer-contrasena").with(csrf())
                        .param("token", TOKEN)
                        .param("password", "NuevaClave123")
                        .param("confirmacionPassword", "NuevaClave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje", "La contraseña se restableció correctamente: inicie sesión con la contraseña nueva"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<RestablecimientoContrasenaFormDTO> formulario = ArgumentCaptor.forClass(RestablecimientoContrasenaFormDTO.class);
        verify(restablecimientoContrasenaServicio).restablecerContrasena(eq(TOKEN), formulario.capture());
        assertThat(formulario.getValue().getPassword()).isEqualTo("NuevaClave123");
        assertThat(formulario.getValue().getConfirmacionPassword()).isEqualTo("NuevaClave123");
    }

    @Test
    @DisplayName("CP-RCT-09: con la contraseña y la confirmación vacías vuelve a mostrar el formulario con un error bajo cada campo y no llama al service")
    void restablecerContrasena_camposVaciosMuestranLosErrores() throws Exception {
        mockMvc.perform(post("/restablecer-contrasena").with(csrf())
                        .param("token", TOKEN)
                        .param("password", "")
                        .param("confirmacionPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login/restablecer-contrasena"))
                .andExpect(model().attributeHasFieldErrors("restablecimientoForm", "password", "confirmacionPassword"))
                .andExpect(model().attribute("token", TOKEN))
                .andExpect(content().string(containsString("La contraseña es obligatoria")))
                .andExpect(content().string(containsString("La confirmación de la contraseña es obligatoria")));

        verify(restablecimientoContrasenaServicio, never()).restablecerContrasena(anyString(), any());
    }

    @Test
    @DisplayName("CP-RCT-10: una contraseña que no cumple las reglas muestra el mensaje de MetodosPassword bajo el campo y no llama al service")
    void restablecerContrasena_passwordInvalidaMuestraLasReglas() throws Exception {
        mockMvc.perform(post("/restablecer-contrasena").with(csrf())
                        .param("token", TOKEN)
                        .param("password", "corta")
                        .param("confirmacionPassword", "corta"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/restablecer-contrasena"))
                .andExpect(model().attributeHasFieldErrors("restablecimientoForm", "password"))
                .andExpect(content().string(containsString(MetodosPassword.MENSAJE_INVALIDA)))
                .andExpect(content().string(not(containsString("value=\"corta\""))));

        verify(restablecimientoContrasenaServicio, never()).restablecerContrasena(anyString(), any());
    }

    @Test
    @DisplayName("CP-RCT-11: una regla rechazada por el service se muestra bajo el campo que indica")
    void restablecerContrasena_reglaDelServiceSeMuestraBajoElCampo() throws Exception {
        doThrow(new ReglaNegocioException("La confirmación no coincide con la contraseña", "confirmacionPassword"))
                .when(restablecimientoContrasenaServicio).restablecerContrasena(eq(TOKEN), any());

        mockMvc.perform(post("/restablecer-contrasena").with(csrf())
                        .param("token", TOKEN)
                        .param("password", "NuevaClave123")
                        .param("confirmacionPassword", "OtraClave123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/restablecer-contrasena"))
                .andExpect(model().attributeHasFieldErrorCode("restablecimientoForm", "confirmacionPassword", "reglaNegocio"))
                .andExpect(content().string(containsString("La confirmación no coincide con la contraseña")));
    }

    @Test
    @DisplayName("CP-RCT-12: si el enlace dejó de servir al enviar la contraseña, muestra el motivo y ofrece pedir un enlace nuevo")
    void restablecerContrasena_enlaceInvalidoOfrecePedirOtro() throws Exception {
        doThrow(new EnlaceRestablecimientoInvalidoException("El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo"))
                .when(restablecimientoContrasenaServicio).restablecerContrasena(eq(TOKEN), any());

        mockMvc.perform(post("/restablecer-contrasena").with(csrf())
                        .param("token", TOKEN)
                        .param("password", "NuevaClave123")
                        .param("confirmacionPassword", "NuevaClave123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/enlace-invalido"))
                .andExpect(content().string(containsString("El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo")))
                .andExpect(content().string(containsString("href=\"/recuperar-contrasena\"")));
    }

    @Test
    @DisplayName("CP-RCT-13: enviar la contraseña nueva sin token CSRF es rechazado (redirige al inicio de sesión por sesión inválida) y no llama al service")
    void restablecerContrasena_sinCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/restablecer-contrasena")
                        .param("token", TOKEN)
                        .param("password", "NuevaClave123")
                        .param("confirmacionPassword", "NuevaClave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?timeout"));

        verifyNoInteractions(restablecimientoContrasenaServicio);
    }
}
