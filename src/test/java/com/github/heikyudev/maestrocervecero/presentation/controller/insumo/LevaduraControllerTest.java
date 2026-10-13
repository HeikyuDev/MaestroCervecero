package com.github.heikyudev.maestrocervecero.presentation.controller.insumo;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
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

@WebMvcTest(LevaduraController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class LevaduraControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILevaduraServicio levaduraServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /insumos/levaduras ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-01: el GERENTE_DE_PRODUCCION ve el listado con tipo, células por gramo y unidad, las pestañas y las acciones por fila")
    void listado_debeMostrarLasLevadurasParaGerenteDeProduccion() throws Exception {
        when(levaduraServicio.filtrarLevaduras(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(levadura(3L, "US-05", TipoLevadura.ALE, 6_000_000_000.0)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/insumos/levaduras"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/levadura-lista"))
                .andExpect(content().string(containsString("Gestión de Insumos")))
                .andExpect(content().string(containsString("US-05")))
                .andExpect(content().string(containsString("ALE")))
                .andExpect(content().string(containsString("Células viables (millones/g)")))
                .andExpect(content().string(containsString(">6.000<")))
                .andExpect(content().string(containsString(">g<")))
                .andExpect(content().string(containsString("href=\"/insumos/maltas\"")))
                .andExpect(content().string(containsString("href=\"/insumos/lupulos\"")))
                .andExpect(content().string(containsString("href=\"/insumos/levaduras/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/insumos/levaduras/3/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/insumos/levaduras/3/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(levaduraServicio.filtrarLevaduras(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/insumos/levaduras")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(levaduraServicio).filtrarLevaduras(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-03: los criterios llegan al service; un nombre en blanco se convierte en null y el select \"Todos\" en null")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(levaduraServicio.filtrarLevaduras(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/insumos/levaduras").param("nombre", " us ").param("tipo", "LAGER").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/insumos/levaduras").param("nombre", "   ").param("tipo", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(levaduraServicio).filtrarLevaduras(eq("us"), eq(TipoLevadura.LAGER), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(levaduraServicio).filtrarLevaduras(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(levaduraServicio.filtrarLevaduras(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/insumos/levaduras"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron levaduras con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-LEC-05: solo el GERENTE_DE_PRODUCCION accede a las levaduras: otro rol es rechazado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/insumos/levaduras"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(levaduraServicio, never()).filtrarLevaduras(any(), any(), any());
    }

    // ==================== GET /insumos/levaduras/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-06: el formulario de alta ofrece los tipos, asteriscos de obligatorio y la unidad fijada por el sistema")
    void formularioAlta_debeRenderizarse() throws Exception {
        mockMvc.perform(get("/insumos/levaduras/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/levadura-form"))
                .andExpect(content().string(containsString("Nueva levadura")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/insumos/levaduras\"")))
                .andExpect(content().string(containsString("value=\"HIBRIDA\"")))
                .andExpect(content().string(containsString("la define el sistema")))
                .andExpect(content().string(containsString("Células viables (millones/g)")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 20000\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Safale US-05\"")))
                .andExpect(content().string(containsString("20000 equivale a 20.000.000.000")))
                .andExpect(content().string(containsString("Crear levadura")))
                .andExpect(content().string(containsString("Volver a levaduras")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear la levadura"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-07: abierto desde otro formulario (retorno y campo) conserva ambos y cancelar vuelve a ese formulario")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/insumos/levaduras/nuevo").param("retorno", "/recetas/nueva").param("campo", "idLevadura"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/recetas/nueva\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idLevadura\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/recetas/nueva?desdeAlta=1\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LEC-08: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/insumos/levaduras/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /insumos/levaduras ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(levaduraServicio.altaLevadura(any())).thenReturn(levadura(9L, "US-05", TipoLevadura.ALE, 6_000_000_000.0));

        mockMvc.perform(post("/insumos/levaduras").with(csrf())
                        .param("nombre", "US-05").param("tipo", "ALE").param("celulasEnMillones", "20000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/levaduras"))
                .andExpect(flash().attribute("mensaje", "Levadura creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<LevaduraFormDTO> captor = ArgumentCaptor.forClass(LevaduraFormDTO.class);
        verify(levaduraServicio).altaLevadura(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("US-05");
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoLevadura.ALE);
        // El usuario ingresa millones (20000); el service recibe células (20.000.000.000)
        assertThat(captor.getValue().getCantidadCelulasPorGramo()).isEqualTo(20_000_000_000.0);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-10: sin nombre, tipo ni células por gramo vuelve al formulario con los tres errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/insumos/levaduras").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/levadura-form"))
                .andExpect(content().string(containsString("No se pudo crear la levadura")))
                .andExpect(content().string(containsString("El nombre es obligatorio")))
                .andExpect(content().string(containsString("El tipo de levadura es obligatorio")))
                .andExpect(content().string(containsString("La cantidad de células viables por gramo es obligatoria")));

        verify(levaduraServicio, never()).altaLevadura(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-22: un valor no numérico en las células (en millones) muestra un mensaje en español, sin llamar al service")
    void alta_debeMostrarUnMensajeEnEspanolParaUnNumeroInvalido() throws Exception {
        mockMvc.perform(post("/insumos/levaduras").with(csrf())
                        .param("nombre", "US-05").param("tipo", "ALE").param("celulasEnMillones", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese un número entero válido")));

        verify(levaduraServicio, never()).altaLevadura(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-11: un nombre duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(levaduraServicio.altaLevadura(any())).thenThrow(new RecursoDuplicadoException("Ya existe una levadura con el nombre 'US-05'"));

        mockMvc.perform(post("/insumos/levaduras").with(csrf())
                        .param("nombre", "US-05").param("tipo", "ALE").param("celulasEnMillones", "20000"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/levadura-form"))
                .andExpect(content().string(containsString("Ya existe una levadura con el nombre")))
                .andExpect(content().string(containsString("value=\"US-05\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-12: una cantidad de células fuera de rango rechazada por el service se muestra junto a ese campo")
    void alta_debeMostrarElErrorDeCelulasFueraDeRango() throws Exception {
        String mensaje = "La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones";
        when(levaduraServicio.altaLevadura(any())).thenThrow(new ReglaNegocioException(mensaje));

        mockMvc.perform(post("/insumos/levaduras").with(csrf())
                        .param("nombre", "US-05").param("tipo", "ALE").param("celulasEnMillones", "5000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(mensaje)))
                .andExpect(content().string(containsString("value=\"5000\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-13: el alta contextual vuelve al formulario de origen con la levadura creada en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigen() throws Exception {
        when(levaduraServicio.altaLevadura(any())).thenReturn(levadura(9L, "US-05", TipoLevadura.ALE, 6_000_000_000.0));

        mockMvc.perform(post("/insumos/levaduras").with(csrf())
                        .param("nombre", "US-05").param("tipo", "ALE").param("celulasEnMillones", "20000")
                        .param("retorno", "/recetas/nueva").param("campo", "idLevadura"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/nueva?desdeAlta=1&idLevadura=9"))
                .andExpect(flash().attribute("mensaje", "Levadura creada correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LEC-14: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta una levadura")
    void alta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/levaduras").with(csrf())
                        .param("nombre", "US-05").param("tipo", "ALE").param("celulasEnMillones", "20000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(levaduraServicio, never()).altaLevadura(any());
    }

    // ==================== GET /insumos/levaduras/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-15: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(levaduraServicio.buscarPorId(3L)).thenReturn(levadura(3L, "US-05", TipoLevadura.LAGER, 6_000_000_000.0));

        mockMvc.perform(get("/insumos/levaduras/3/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/levadura-form"))
                .andExpect(content().string(containsString("Editar levadura")))
                .andExpect(content().string(containsString("value=\"US-05\"")))
                // La entidad guarda células (6.000.000.000); el formulario las muestra en millones
                .andExpect(content().string(containsString("value=\"6000\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"LAGER\" selected.*")))
                .andExpect(content().string(containsString("action=\"/insumos/levaduras/3\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    // ==================== POST /insumos/levaduras/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-16: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/insumos/levaduras/3").with(csrf())
                        .param("nombre", "S-23").param("tipo", "LAGER").param("celulasEnMillones", "15000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/levaduras"))
                .andExpect(flash().attribute("mensaje", "Levadura modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(levaduraServicio).modificarLevadura(eq(3L), any(LevaduraFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-17: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(levaduraServicio.modificarLevadura(eq(3L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una levadura con el nombre 'S-23'"));

        mockMvc.perform(post("/insumos/levaduras/3").with(csrf())
                        .param("nombre", "S-23").param("tipo", "LAGER").param("celulasEnMillones", "15000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar la levadura")))
                .andExpect(content().string(containsString("Ya existe una levadura con el nombre")))
                .andExpect(content().string(containsString("action=\"/insumos/levaduras/3\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LEC-18: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar una levadura")
    void modificar_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/levaduras/3").with(csrf())
                        .param("nombre", "S-23").param("tipo", "LAGER").param("celulasEnMillones", "15000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(levaduraServicio, never()).modificarLevadura(any(), any());
    }

    // ==================== POST /insumos/levaduras/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-19: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/insumos/levaduras/3/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/levaduras"))
                .andExpect(flash().attribute("mensaje", "Levadura dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(levaduraServicio).bajaLevadura(3L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-LEC-20: si el service rechaza la baja (forma parte de una receta o tiene stock) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja la levadura porque tiene stock")).when(levaduraServicio).bajaLevadura(3L);

        mockMvc.perform(post("/insumos/levaduras/3/baja").with(csrf()).header("Referer", "/insumos/levaduras"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/levaduras"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja la levadura porque tiene stock"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-LEC-21: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja una levadura")
    void baja_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/levaduras/3/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(levaduraServicio, never()).bajaLevadura(any());
    }

    private static LevaduraResponseDTO levadura(Long id, String nombre, TipoLevadura tipo, Double celulas) {
        return LevaduraResponseDTO.builder().id(id).nombre(nombre).tipo(tipo).cantidadCelulasPorGramo(celulas)
                .unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }
}
