package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.PaisFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(PaisController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class PaisControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPaisServicio paisServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /ubicaciones/paises ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-01: el ADMINISTRADOR ve el listado con las pestañas, el botón de alta y las acciones de editar y dar de baja")
    void listado_debeMostrarLosPaisesParaAdministrador() throws Exception {
        when(paisServicio.filtrarPaises(any(), any()))
                .thenReturn(new PageImpl<>(List.of(pais(3L, "Argentina")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/ubicaciones/paises"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/pais-lista"))
                .andExpect(content().string(containsString("Argentina")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/provincias\"")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/localidades\"")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/paises/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/paises/3/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/ubicaciones/paises/3/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con el criterio en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(paisServicio.filtrarPaises(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/paises")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paisServicio).filtrarPaises(isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-03: el criterio de nombre llega al service y un texto en blanco se convierte en null")
    void listado_debePasarElFiltroAlService() throws Exception {
        when(paisServicio.filtrarPaises(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/ubicaciones/paises").param("nombre", " arg ").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/ubicaciones/paises").param("nombre", "   "))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paisServicio).filtrarPaises(eq("arg"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(paisServicio).filtrarPaises(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(paisServicio.filtrarPaises(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/paises"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron países con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PAC-05: un gerente no tiene acceso al módulo (el listado se rechaza y no llega al service)")
    void listado_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/ubicaciones/paises"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(paisServicio, never()).filtrarPaises(any(), any());
    }

    // ==================== GET /ubicaciones/paises/buscador ====================

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PAC-06: el GERENTE_COMERCIAL puede usar el buscador: páginas de 8 y un botón para elegir cada país")
    void buscador_debeEstarDisponibleParaGerenteComercial() throws Exception {
        when(paisServicio.filtrarPaises(any(), any()))
                .thenReturn(new PageImpl<>(List.of(pais(3L, "Argentina")), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/ubicaciones/paises/buscador").param("nombre", "arg"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/buscador-pais :: buscador"))
                .andExpect(content().string(containsString("data-elegir data-id=\"3\" data-texto=\"Argentina\"")))
                .andExpect(content().string(not(containsString("data-baja-url"))));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(paisServicio).filtrarPaises(eq("arg"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PAC-07: el GERENTE_DE_COMPRAS también puede usar el buscador")
    void buscador_debeEstarDisponibleParaGerenteDeCompras() throws Exception {
        when(paisServicio.filtrarPaises(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        mockMvc.perform(get("/ubicaciones/paises/buscador"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron países con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PAC-08: un rol sin relación con clientes ni proveedores no puede usar el buscador")
    void buscador_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/ubicaciones/paises/buscador"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(paisServicio, never()).filtrarPaises(any(), any());
    }

    // ==================== GET /ubicaciones/paises/nuevo ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-09: el formulario de alta es una página con asterisco de obligatorio, enlace a la lista y sin panel de errores")
    void formularioAlta_debeRenderizarseComoPagina() throws Exception {
        mockMvc.perform(get("/ubicaciones/paises/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/pais-form"))
                .andExpect(content().string(containsString("Nuevo país")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/ubicaciones/paises\"")))
                .andExpect(content().string(containsString("Crear país")))
                .andExpect(content().string(containsString("Volver a países")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear el país"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-10: abierto desde otro formulario (retorno y campo) conserva ambos en campos ocultos y cancelar vuelve a ese formulario")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/ubicaciones/paises/nuevo")
                        .param("retorno", "/ubicaciones/provincias/nuevo").param("campo", "idPais"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/ubicaciones/provincias/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idPais\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/provincias/nuevo?desdeAlta=1\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-28: un retorno que apunta fuera del sitio se descarta (no hay redirección abierta)")
    void formularioAlta_debeDescartarUnRetornoExterno() throws Exception {
        mockMvc.perform(get("/ubicaciones/paises/nuevo").param("retorno", "//sitio-malicioso.com/x").param("campo", "idPais"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))))
                .andExpect(content().string(containsString("Volver a países")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PAC-11: un gerente no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/ubicaciones/paises/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /ubicaciones/paises ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-12: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(paisServicio.altaPais(any())).thenReturn(pais(9L, "Chile"));

        mockMvc.perform(post("/ubicaciones/paises").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/paises"))
                .andExpect(flash().attribute("mensaje", "País creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<PaisFormDTO> captor = ArgumentCaptor.forClass(PaisFormDTO.class);
        verify(paisServicio).altaPais(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Chile");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-13: con el nombre en blanco responde 422 con el formulario y el error, sin llamar al service")
    void alta_debeVolverAlFormularioSiElNombreEstaEnBlanco() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises").with(csrf()).param("nombre", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/pais-form"))
                .andExpect(content().string(containsString("No se pudo crear el país")))
                .andExpect(content().string(containsString("El nombre del país es obligatorio")));

        verify(paisServicio, never()).altaPais(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-14: un nombre duplicado responde 422 con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(paisServicio.altaPais(any())).thenThrow(new RecursoDuplicadoException("Ya existe un país con ese nombre"));

        mockMvc.perform(post("/ubicaciones/paises").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/pais-form"))
                .andExpect(content().string(containsString("Ya existe un país con ese nombre")))
                .andExpect(content().string(containsString("value=\"Chile\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-15: una regla de negocio rechazada se muestra como error general del formulario")
    void alta_debeMostrarLaReglaDeNegocioRechazada() throws Exception {
        when(paisServicio.altaPais(any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        mockMvc.perform(post("/ubicaciones/paises").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Regla incumplida")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-16: el alta contextual vuelve al formulario de origen con el país creado en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigenConElPaisCreado() throws Exception {
        when(paisServicio.altaPais(any())).thenReturn(pais(9L, "Chile"));

        mockMvc.perform(post("/ubicaciones/paises").with(csrf())
                        .param("nombre", "Chile")
                        .param("retorno", "/ubicaciones/provincias/nuevo").param("campo", "idPais"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/provincias/nuevo?desdeAlta=1&idPais=9"))
                .andExpect(flash().attribute("mensaje", "País creado correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-17: el alta contextual con errores vuelve a mostrar el formulario conservando retorno y campo")
    void altaContextual_debeConservarElRetornoAlVolverConErrores() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises").with(csrf())
                        .param("nombre", "")
                        .param("retorno", "/ubicaciones/provincias/nuevo").param("campo", "idPais"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/ubicaciones/provincias/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idPais\"")));

        verify(paisServicio, never()).altaPais(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-29: un retorno que apunta fuera del sitio se ignora y el alta redirige al listado")
    void altaContextual_debeIgnorarUnRetornoExterno() throws Exception {
        when(paisServicio.altaPais(any())).thenReturn(pais(9L, "Chile"));

        mockMvc.perform(post("/ubicaciones/paises").with(csrf())
                        .param("nombre", "Chile").param("retorno", "https://sitio-malicioso.com").param("campo", "idPais"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/paises"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PAC-18: un gerente no puede dar de alta un país")
    void alta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(paisServicio, never()).altaPais(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-19: sin token CSRF el alta es rechazada y no llega al service")
    void alta_debeRechazarPeticionSinCsrf() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises").param("nombre", "Chile"))
                .andExpect(status().is3xxRedirection());

        verify(paisServicio, never()).altaPais(any());
    }

    // ==================== GET /ubicaciones/paises/{id}/editar ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-20: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));

        mockMvc.perform(get("/ubicaciones/paises/3/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/pais-form"))
                .andExpect(content().string(containsString("Editar país")))
                .andExpect(content().string(containsString("value=\"Argentina\"")))
                .andExpect(content().string(containsString("action=\"/ubicaciones/paises/3\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    // ==================== POST /ubicaciones/paises/{id} ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-21: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises/3").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/paises"))
                .andExpect(flash().attribute("mensaje", "País modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(paisServicio).modificarPais(eq(3L), any(PaisFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-22: modificar con un nombre duplicado responde 422 con el formulario de edición y el mensaje")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(paisServicio.modificarPais(eq(3L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un país con ese nombre"));

        mockMvc.perform(post("/ubicaciones/paises/3").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el país")))
                .andExpect(content().string(containsString("Ya existe un país con ese nombre")))
                .andExpect(content().string(containsString("action=\"/ubicaciones/paises/3\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-23: modificar con el nombre en blanco responde 422 sin llamar al service")
    void modificar_debeVolverAlFormularioSiElNombreEstaEnBlanco() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises/3").with(csrf()).param("nombre", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El nombre del país es obligatorio")));

        verify(paisServicio, never()).modificarPais(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PAC-24: un gerente no puede modificar un país")
    void modificar_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises/3").with(csrf()).param("nombre", "Chile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(paisServicio, never()).modificarPais(any(), any());
    }

    // ==================== POST /ubicaciones/paises/{id}/baja ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-25: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises/3/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/paises"))
                .andExpect(flash().attribute("mensaje", "País dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(paisServicio).bajaPais(3L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PAC-26: si el service rechaza la baja (tiene provincias activas) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("El país tiene provincias asociadas")).when(paisServicio).bajaPais(3L);

        mockMvc.perform(post("/ubicaciones/paises/3/baja").with(csrf()).header("Referer", "/ubicaciones/paises"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/paises"))
                .andExpect(flash().attribute("mensaje", "El país tiene provincias asociadas"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PAC-27: un gerente no puede dar de baja un país")
    void baja_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/paises/3/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(paisServicio, never()).bajaPais(any());
    }

    private static PaisResponseDTO pais(Long id, String nombre) {
        return PaisResponseDTO.builder().id(id).nombre(nombre).build();
    }
}
