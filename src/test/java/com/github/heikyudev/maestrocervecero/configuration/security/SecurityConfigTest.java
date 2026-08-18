package com.github.heikyudev.maestrocervecero.configuration.security;

import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.implementation.UserDetailServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el comportamiento real de {@link SecurityConfig}: que una ruta protegida
 * redirija a un usuario no autenticado al login, y que un usuario autenticado con el
 * rol correcto sí pueda acceder (tanto a nivel de URL como de método, gracias a
 * {@code @EnableMethodSecurity}).
 * <p>
 * Es un {@code @WebMvcTest} (slice liviano) en vez de un {@code @SpringBootTest} completo:
 * no necesitamos levantar JPA/Hibernate ni el resto del contexto de la app para probar
 * únicamente la cadena de filtros de seguridad. Por eso se importa explícitamente
 * {@link SecurityConfig} y {@link UserDetailServiceImpl}, y se mockea el repositorio del
 * que depende este último (nunca se toca una base de datos real acá). MockMvc ya viene
 * con el filtro de seguridad real aplicado automáticamente por la auto-configuración
 * de {@code @WebMvcTest} al detectar spring-security-test en el classpath.
 * <p>
 * {@code ProtectedTestController} es un controlador que existe únicamente para este test:
 * como el ERP todavía no tiene entidades/controladores de negocio, expone una ruta
 * protegida mínima ({@code /home}, coincidiendo con el {@code defaultSuccessUrl} real del
 * login) para poder ejercitar la configuración de seguridad sin inventar código de producción.
 */
@WebMvcTest(controllers = SecurityConfigTest.ProtectedTestController.class)
// ProtectedTestController se importa también acá (no solo vía controllers=...) porque, al ser
// una clase anidada dentro de esta clase de test, el component-scan implícito de @WebMvcTest
// no siempre la resuelve como bean; @Import la registra explícitamente sin ambigüedad.
@Import({SecurityConfig.class, UserDetailServiceImpl.class, SecurityConfigTest.ProtectedTestController.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // UserDetailServiceImpl necesita este repositorio para construirse como bean, pero
    // ninguno de estos tests dispara un login real (usan @WithMockUser / .with(user(...))),
    // así que alcanza con un mock vacío para satisfacer la inyección de dependencias.
    @MockitoBean
    private IUsuarioRepository usuarioRepository;

    @Test
    void usuarioNoAutenticado_esRedirigidoAlLogin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void usuarioConRolCorrecto_puedeAccederALaRutaProtegida() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    void usuarioSinElRolRequerido_recibeForbidden() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/home")
        @PreAuthorize("hasRole('ADMINISTRADOR')")
        public String home() {
            return "ok";
        }
    }
}
