package com.github.heikyudev.maestrocervecero.presentation.advice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
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
                .andExpect(redirectedUrl("/clientes/nuevo"))
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
                .andExpect(redirectedUrl("/lotes"))
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

    @Test
    @DisplayName("CP-CA-06: en un buscador en modal cualquier rechazo (recurso inexistente, regla de negocio, error inesperado) responde con su estado HTTP y el motivo como texto, sin redirigir al origen; el rechazo de permisos sí redirige")
    void rechazoEnUnBuscador_debeResponderElMotivoSinRedirigir() throws Exception {
        String[][] casos = {
                                {"/lotes/buscador/inexistente", "404", "No se encontró el lote con ID: 9"},
                {"/lotes/buscador/regla", "422", "La etapa no admite consumos"},
                {"/lotes/buscador/falla", "500", "Ocurrió un error inesperado. Intente nuevamente"}};
        for (String[] caso : casos) {
            mockMvc.perform(get(caso[0]).header("Referer", "http://localhost/lotes/12/etapas/molienda/consumos"))
                    .andExpect(status().is(Integer.parseInt(caso[1])))
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string(caso[2]));
        }
    }

    @Test
    @DisplayName("CP-CA-07: un Referer de otro sitio no se usa para redirigir: se vuelve a la raíz con la alerta")
    void refererDeOtroSitio_debeRedirigirALaRaiz() throws Exception {
        for (String referer : new String[]{"http://sitio-malicioso.example/robo", "https://localhost/clientes",
                "http://localhost:9999/clientes", "http://usuario@localhost/clientes", "//sitio-malicioso.example/robo",
                "javascript:alert(1)", "no es una url"}) {
            mockMvc.perform(get("/falla-integridad").header("Referer", referer))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("mensaje", MENSAJE_CONFLICTO));
        }
    }

    @Test
    @DisplayName("CP-CA-08: un Referer del mismo sitio se convierte en ruta relativa conservando la consulta y descartando el fragmento")
    void refererDelMismoSitio_debeConservarRutaYConsulta() throws Exception {
        mockMvc.perform(get("/falla-integridad").header("Referer", "http://localhost/lotes?pagina=2&estado=PENDIENTE#fin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes?pagina=2&estado=PENDIENTE"));
        mockMvc.perform(get("/falla-integridad").header("Referer", "http://localhost:80/clientes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clientes"));
        mockMvc.perform(get("/falla-integridad").header("Referer", "/lotes?pagina=1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes?pagina=1"));
    }

    @Test
    @DisplayName("CP-CA-09: las llaves de la ruta del Referer no se expanden como variables de plantilla")
    void refererConLlaves_noDebeExpandirseComoVariableDePlantilla() throws Exception {
        mockMvc.perform(get("/falla-integridad").header("Referer", "http://localhost/lotes/%7Bid%7D?filtro=%7Bx%7D"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/%7Bid%7D?filtro=%7Bx%7D"));
        mockMvc.perform(get("/falla-integridad").header("Referer", "http://localhost/lotes/{id}"))
                .andExpect(status().is3xxRedirection());
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

        @GetMapping("/lotes/buscador/sin-permiso")
        String sinPermiso() {
            throw new AccessDeniedException("Access Denied");
        }

        @GetMapping("/lotes/buscador/inexistente")
        String inexistente() {
            throw new RecursoNoEncontradoException("No se encontró el lote con ID: 9");
        }

        @GetMapping("/lotes/buscador/regla")
        String regla() {
            throw new ReglaNegocioException("La etapa no admite consumos");
        }

        @GetMapping("/lotes/buscador/falla")
        String falla() {
            throw new IllegalStateException("falla cualquiera");
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
