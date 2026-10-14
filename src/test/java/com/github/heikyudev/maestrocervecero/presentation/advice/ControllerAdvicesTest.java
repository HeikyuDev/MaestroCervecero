package com.github.heikyudev.maestrocervecero.presentation.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllerAdvicesTest {

    private static final String MENSAJE_CONFLICTO =
            "No se pudo guardar porque los datos entran en conflicto con otro registro. Revise la información e intente nuevamente";

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorQueFalla())
                .setControllerAdvice(new ControllerAdvices())
                .build();
    }

    @Test
    @DisplayName("CP-CA-01: una violación de integridad de datos redirige al origen con una alerta clara, no con el error inesperado")
    void violacionDeIntegridad_debeRedirigirConAlertaDeConflicto() throws Exception {
        mockMvc.perform(get("/falla-integridad").header("Referer", "http://localhost/clientes/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/clientes/nuevo"))
                .andExpect(flash().attribute("mensaje", MENSAJE_CONFLICTO))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-CA-02: sin cabecera Referer, la violación de integridad redirige a la raíz del sitio")
    void violacionDeIntegridad_sinRefererDebeRedirigirALaRaiz() throws Exception {
        mockMvc.perform(get("/falla-integridad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("mensaje", MENSAJE_CONFLICTO));
    }

    @Test
    @DisplayName("CP-CA-03: una excepción cualquiera sigue mostrándose como error inesperado")
    void excepcionInesperada_debeSeguirMostrandoElMensajeGenerico() throws Exception {
        mockMvc.perform(get("/falla-inesperada").header("Referer", "http://localhost/clientes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Ocurrió un error inesperado. Intente nuevamente"));
    }

    @Test
    @DisplayName("CP-CA-04: un valor de la URL que no se convierte al tipo del parámetro redirige al origen con una alerta amarilla clara")
    void valorInvalidoEnUnaPagina_debeRedirigirConAlertaAmarilla() throws Exception {
        mockMvc.perform(get("/pagina/abc").header("Referer", "http://localhost/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/lotes"))
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(get("/pagina/abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-CA-05: en un buscador en modal (ruta con /buscador o /filtros) un valor que no se convierte responde 400 con el mensaje como texto, sin redirigir")
    void valorInvalidoEnUnBuscador_debeResponderElMensajeSinRedirigir() throws Exception {
        for (String ruta : new String[]{"/lotes/buscador/molinos", "/lotes/filtros/fermentadores"}) {
            mockMvc.perform(get(ruta).param("pagina", "xyz").header("Referer", "http://localhost/lotes/nuevo"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string("El valor indicado no es válido"));
        }
    }

    @Controller
    static class ControladorQueFalla {

        @GetMapping("/pagina/{id}")
        String pagina(@PathVariable Long id) {
            return "pagina";
        }

        @GetMapping({"/lotes/buscador/molinos", "/lotes/filtros/fermentadores"})
        @ResponseBody
        String buscador(@RequestParam(defaultValue = "0") int pagina) {
            return "buscador";
        }

        @GetMapping("/falla-integridad")
        String fallaIntegridad() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
        }

        @GetMapping("/falla-inesperada")
        String fallaInesperada() {
            throw new IllegalStateException("falla cualquiera");
        }
    }
}
