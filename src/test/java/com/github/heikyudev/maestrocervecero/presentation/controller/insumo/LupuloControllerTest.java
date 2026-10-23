package com.github.heikyudev.maestrocervecero.presentation.controller.insumo;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
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

@WebMvcTest(LupuloController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class LupuloControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILupuloServicio lupuloServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /insumos/lupulos ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-01: el GERENTE_DE_PRODUCCION ve el listado con formato, alfa ácidos y unidad, las pestañas y las acciones por fila")
    void listado_debeMostrarLosLupulosParaGerenteDeProduccion() throws Exception {
        when(lupuloServicio.filtrarLupulos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(lupulo(6L, "Cascade", FormatoLupulo.PELLET, 5.5)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/insumos/lupulos"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/lupulo-lista"))
                .andExpect(content().string(containsString("Gestión de Insumos")))
                .andExpect(content().string(containsString("Cascade")))
                .andExpect(content().string(containsString("PELLET")))
                .andExpect(content().string(containsString("Ácidos alfa (%)")))
                .andExpect(content().string(containsString(">5,5<")))
                .andExpect(content().string(containsString(">g<")))
                .andExpect(content().string(containsString("href=\"/insumos/maltas\"")))
                .andExpect(content().string(containsString("href=\"/insumos/levaduras\"")))
                .andExpect(content().string(containsString("href=\"/insumos/lupulos/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/insumos/lupulos/6/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/insumos/lupulos/6/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(lupuloServicio.filtrarLupulos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/insumos/lupulos")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lupuloServicio).filtrarLupulos(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-03: los criterios llegan al service; un nombre en blanco se convierte en null y el select \"Todos\" en null")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(lupuloServicio.filtrarLupulos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/insumos/lupulos").param("nombre", " cas ").param("formato", "FLOR").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/insumos/lupulos").param("nombre", "   ").param("formato", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(lupuloServicio).filtrarLupulos(eq("cas"), eq(FormatoLupulo.FLOR), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(lupuloServicio).filtrarLupulos(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(lupuloServicio.filtrarLupulos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/insumos/lupulos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron lúpulos con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-LUC-05: solo el GERENTE_DE_PRODUCCION accede a los lúpulos: otro rol es rechazado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/insumos/lupulos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(lupuloServicio, never()).filtrarLupulos(any(), any(), any());
    }

    // ==================== GET /insumos/lupulos/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-06: el formulario de alta ofrece los formatos, asteriscos de obligatorio y la unidad fijada por el sistema")
    void formularioAlta_debeRenderizarse() throws Exception {
        mockMvc.perform(get("/insumos/lupulos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/lupulo-form"))
                .andExpect(content().string(containsString("Nuevo lúpulo")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/insumos/lupulos\"")))
                .andExpect(content().string(containsString("value=\"PELLET\"")))
                .andExpect(content().string(containsString("la define el sistema")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Cascade\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 5,5\"")))
                .andExpect(content().string(containsString("Un número mayor a 0 y hasta 25")))
                .andExpect(content().string(containsString("Crear lúpulo")))
                .andExpect(content().string(containsString("Volver a lúpulos")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear el lúpulo"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-07: abierto desde otro formulario (retorno y campo) conserva ambos y cancelar vuelve a ese formulario")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/insumos/lupulos/nuevo").param("retorno", "/recetas/nueva").param("campo", "idLupulo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/recetas/nueva\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idLupulo\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/recetas/nueva?desdeAlta=1\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LUC-08: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/insumos/lupulos/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /insumos/lupulos ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(lupuloServicio.altaLupulo(any())).thenReturn(lupulo(9L, "Cascade", FormatoLupulo.PELLET, 5.5));

        mockMvc.perform(post("/insumos/lupulos").with(csrf())
                        .param("nombre", "Cascade").param("formato", "PELLET").param("aa", "5.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/lupulos"))
                .andExpect(flash().attribute("mensaje", "Lúpulo creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<LupuloFormDTO> captor = ArgumentCaptor.forClass(LupuloFormDTO.class);
        verify(lupuloServicio).altaLupulo(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Cascade");
        assertThat(captor.getValue().getFormato()).isEqualTo(FormatoLupulo.PELLET);
        assertThat(captor.getValue().getAa()).isEqualTo(5.5);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-10: sin nombre, formato ni alfa ácidos vuelve al formulario con los tres errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/insumos/lupulos").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/lupulo-form"))
                .andExpect(content().string(containsString("No se pudo crear el lúpulo")))
                .andExpect(content().string(containsString("El nombre es obligatorio")))
                .andExpect(content().string(containsString("El formato es obligatorio")))
                .andExpect(content().string(containsString("Los ácidos alfa son obligatorios")));

        verify(lupuloServicio, never()).altaLupulo(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-11: un nombre duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(lupuloServicio.altaLupulo(any())).thenThrow(new RecursoDuplicadoException("Ya existe un lúpulo con el nombre 'Cascade'"));

        mockMvc.perform(post("/insumos/lupulos").with(csrf())
                        .param("nombre", "Cascade").param("formato", "PELLET").param("aa", "5.5"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/lupulo-form"))
                .andExpect(content().string(containsString("Ya existe un lúpulo con el nombre")))
                .andExpect(content().string(containsString("value=\"Cascade\"")))
                .andExpect(content().string(containsString("value=\"5.5\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-12: unos ácidos alfa fuera de rango rechazados por el service se muestran junto a ese campo")
    void alta_debeMostrarElErrorDeAcidosAlfaFueraDeRango() throws Exception {
        when(lupuloServicio.altaLupulo(any())).thenThrow(new ReglaNegocioException("Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%"));

        mockMvc.perform(post("/insumos/lupulos").with(csrf())
                        .param("nombre", "Cascade").param("formato", "PELLET").param("aa", "233333"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%")))
                .andExpect(content().string(containsString("value=\"233333\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-13: el alta contextual vuelve al formulario de origen con el lúpulo creado en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigen() throws Exception {
        when(lupuloServicio.altaLupulo(any())).thenReturn(lupulo(9L, "Cascade", FormatoLupulo.PELLET, 5.5));

        mockMvc.perform(post("/insumos/lupulos").with(csrf())
                        .param("nombre", "Cascade").param("formato", "PELLET").param("aa", "5.5")
                        .param("retorno", "/recetas/nueva").param("campo", "idLupulo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/nueva?desdeAlta=1&idLupulo=9"))
                .andExpect(flash().attribute("mensaje", "Lúpulo creado correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LUC-14: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta un lúpulo")
    void alta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/lupulos").with(csrf())
                        .param("nombre", "Cascade").param("formato", "PELLET").param("aa", "5.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(lupuloServicio, never()).altaLupulo(any());
    }

    // ==================== GET /insumos/lupulos/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-15: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(lupuloServicio.buscarPorId(6L)).thenReturn(lupulo(6L, "Cascade", FormatoLupulo.FLOR, 5.5));

        mockMvc.perform(get("/insumos/lupulos/6/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/lupulo-form"))
                .andExpect(content().string(containsString("Editar lúpulo")))
                .andExpect(content().string(containsString("value=\"Cascade\"")))
                .andExpect(content().string(containsString("value=\"5.5\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"FLOR\" selected.*")))
                .andExpect(content().string(containsString("action=\"/insumos/lupulos/6\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    // ==================== POST /insumos/lupulos/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-16: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/insumos/lupulos/6").with(csrf())
                        .param("nombre", "Citra").param("formato", "FLOR").param("aa", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/lupulos"))
                .andExpect(flash().attribute("mensaje", "Lúpulo modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(lupuloServicio).modificarLupulo(eq(6L), any(LupuloFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-17: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(lupuloServicio.modificarLupulo(eq(6L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un lúpulo con el nombre 'Citra'"));

        mockMvc.perform(post("/insumos/lupulos/6").with(csrf())
                        .param("nombre", "Citra").param("formato", "FLOR").param("aa", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el lúpulo")))
                .andExpect(content().string(containsString("Ya existe un lúpulo con el nombre")))
                .andExpect(content().string(containsString("action=\"/insumos/lupulos/6\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LUC-18: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar un lúpulo")
    void modificar_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/lupulos/6").with(csrf())
                        .param("nombre", "Citra").param("formato", "FLOR").param("aa", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(lupuloServicio, never()).modificarLupulo(any(), any());
    }

    // ==================== POST /insumos/lupulos/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-19: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/insumos/lupulos/6/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/lupulos"))
                .andExpect(flash().attribute("mensaje", "Lúpulo dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(lupuloServicio).bajaLupulo(6L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LUC-20: si el service rechaza la baja (forma parte de una receta o tiene stock) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja el lúpulo porque tiene stock")).when(lupuloServicio).bajaLupulo(6L);

        mockMvc.perform(post("/insumos/lupulos/6/baja").with(csrf()).header("Referer", "/insumos/lupulos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/lupulos"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el lúpulo porque tiene stock"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-LUC-21: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja un lúpulo")
    void baja_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/lupulos/6/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(lupuloServicio, never()).bajaLupulo(any());
    }

    private static LupuloResponseDTO lupulo(Long id, String nombre, FormatoLupulo formato, Double aa) {
        return LupuloResponseDTO.builder().id(id).nombre(nombre).formato(formato).aa(aa)
                .unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }
}
