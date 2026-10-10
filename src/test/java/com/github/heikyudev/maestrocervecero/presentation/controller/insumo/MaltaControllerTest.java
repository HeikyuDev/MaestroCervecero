package com.github.heikyudev.maestrocervecero.presentation.controller.insumo;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IMaltaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
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

@WebMvcTest(MaltaController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class MaltaControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IMaltaServicio maltaServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /insumos/maltas ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-01: el GERENTE_DE_PRODUCCION ve el listado con tipo, potencial de extracto y unidad, las pestañas y las acciones por fila")
    void listado_debeMostrarLasMaltasParaGerenteDeProduccion() throws Exception {
        when(maltaServicio.filtrarMaltas(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(malta(4L, "Pilsen", TipoMalta.BASE, 80)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/insumos/maltas"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/malta-lista"))
                .andExpect(content().string(containsString("Gestión de Insumos")))
                .andExpect(content().string(containsString("Pilsen")))
                .andExpect(content().string(containsString("BASE")))
                .andExpect(content().string(containsString("80 %")))
                .andExpect(content().string(containsString(">kg<")))
                .andExpect(content().string(containsString("href=\"/insumos/lupulos\"")))
                .andExpect(content().string(containsString("href=\"/insumos/levaduras\"")))
                // Las pestañas llevan íconos propios (trigo, lúpulo, burbujas), no de Bootstrap Icons
                .andExpect(content().string(containsString("<svg")))
                .andExpect(content().string(not(containsString("bi-basket"))))
                // La sección de la sidebar de los maestros se llama "Administración"; los parámetros van en "Configuración"
                .andExpect(content().string(containsString("Administración")))
                .andExpect(content().string(containsString(">Configuración<")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pilsen\"")))
                .andExpect(content().string(containsString("href=\"/insumos/maltas/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/insumos/maltas/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/insumos/maltas/4/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(maltaServicio.filtrarMaltas(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/insumos/maltas")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(maltaServicio).filtrarMaltas(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-03: los criterios llegan al service; un nombre en blanco se convierte en null y el select \"Todos\" en null")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(maltaServicio.filtrarMaltas(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/insumos/maltas").param("nombre", " pil ").param("tipo", "BASE").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/insumos/maltas").param("nombre", "   ").param("tipo", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(maltaServicio).filtrarMaltas(eq("pil"), eq(TipoMalta.BASE), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(maltaServicio).filtrarMaltas(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(maltaServicio.filtrarMaltas(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/insumos/maltas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron maltas con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-MAC-05: solo el GERENTE_DE_PRODUCCION accede a las maltas: otro rol es rechazado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/insumos/maltas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maltaServicio, never()).filtrarMaltas(any(), any(), any());
    }

    // ==================== GET /insumos/maltas/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-06: el formulario de alta ofrece los tipos de malta, asteriscos de obligatorio y la unidad fijada por el sistema")
    void formularioAlta_debeRenderizarse() throws Exception {
        mockMvc.perform(get("/insumos/maltas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/malta-form"))
                .andExpect(content().string(containsString("Nueva malta")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/insumos/maltas\"")))
                .andExpect(content().string(containsString("value=\"CARAMELO\"")))
                .andExpect(content().string(containsString("la define el sistema")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pilsen\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 80\"")))
                .andExpect(content().string(containsString("Un número entero mayor a 0 y hasta 90.")))
                .andExpect(content().string(containsString("Crear malta")))
                .andExpect(content().string(containsString("Volver a maltas")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear la malta"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-07: abierto desde otro formulario (retorno y campo) conserva ambos y cancelar vuelve a ese formulario; un retorno externo se descarta")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/insumos/maltas/nuevo").param("retorno", "/recetas/nueva").param("campo", "idMalta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/recetas/nueva\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idMalta\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/recetas/nueva?desdeAlta=1\"")));

        mockMvc.perform(get("/insumos/maltas/nuevo").param("retorno", "//sitio-malicioso.com").param("campo", "idMalta"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-MAC-08: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/insumos/maltas/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /insumos/maltas ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(maltaServicio.altaMalta(any())).thenReturn(malta(9L, "Pilsen", TipoMalta.BASE, 80));

        mockMvc.perform(post("/insumos/maltas").with(csrf())
                        .param("nombre", "Pilsen").param("tipo", "BASE").param("potencialExtracto", "80"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/maltas"))
                .andExpect(flash().attribute("mensaje", "Malta creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<MaltaFormDTO> captor = ArgumentCaptor.forClass(MaltaFormDTO.class);
        verify(maltaServicio).altaMalta(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Pilsen");
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(captor.getValue().getPotencialExtracto()).isEqualTo(80);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-10: sin nombre, tipo ni potencial vuelve al formulario con los tres errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/insumos/maltas").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/malta-form"))
                .andExpect(content().string(containsString("No se pudo crear la malta")))
                .andExpect(content().string(containsString("El nombre de la malta es obligatorio")))
                .andExpect(content().string(containsString("El tipo de malta es obligatorio")))
                .andExpect(content().string(containsString("El extracto potencial es obligatorio")));

        verify(maltaServicio, never()).altaMalta(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-11: un valor no numérico en el potencial muestra un mensaje en español, sin llamar al service")
    void alta_debeMostrarUnMensajeEnEspanolParaUnNumeroInvalido() throws Exception {
        mockMvc.perform(post("/insumos/maltas").with(csrf())
                        .param("nombre", "Pilsen").param("tipo", "BASE").param("potencialExtracto", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresá un número entero válido")));

        verify(maltaServicio, never()).altaMalta(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-12: un nombre duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(maltaServicio.altaMalta(any())).thenThrow(new RecursoDuplicadoException("Ya existe una malta con el nombre 'Pilsen'"));

        mockMvc.perform(post("/insumos/maltas").with(csrf())
                        .param("nombre", "Pilsen").param("tipo", "BASE").param("potencialExtracto", "80"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/malta-form"))
                .andExpect(content().string(containsString("Ya existe una malta con el nombre")))
                .andExpect(content().string(containsString("value=\"Pilsen\"")))
                .andExpect(content().string(containsString("value=\"80\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-13: un potencial fuera de rango rechazado por el service se muestra junto a ese campo")
    void alta_debeMostrarElErrorDelPotencialFueraDeRango() throws Exception {
        when(maltaServicio.altaMalta(any())).thenThrow(new ReglaNegocioException("El extracto potencial debe ser mayor a 0% y menor o igual a 90%"));

        mockMvc.perform(post("/insumos/maltas").with(csrf())
                        .param("nombre", "Pilsen").param("tipo", "BASE").param("potencialExtracto", "95"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/malta-form"))
                .andExpect(content().string(containsString("El extracto potencial debe ser mayor a 0% y menor o igual a 90%")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-14: el alta contextual vuelve al formulario de origen con la malta creada en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigen() throws Exception {
        when(maltaServicio.altaMalta(any())).thenReturn(malta(9L, "Pilsen", TipoMalta.BASE, 80));

        mockMvc.perform(post("/insumos/maltas").with(csrf())
                        .param("nombre", "Pilsen").param("tipo", "BASE").param("potencialExtracto", "80")
                        .param("retorno", "/recetas/nueva").param("campo", "idMalta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/nueva?desdeAlta=1&idMalta=9"))
                .andExpect(flash().attribute("mensaje", "Malta creada correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-MAC-15: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta una malta")
    void alta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/maltas").with(csrf())
                        .param("nombre", "Pilsen").param("tipo", "BASE").param("potencialExtracto", "80"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maltaServicio, never()).altaMalta(any());
    }

    // ==================== GET /insumos/maltas/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-16: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(maltaServicio.buscarPorId(4L)).thenReturn(malta(4L, "Pilsen", TipoMalta.CARAMELO, 75));

        mockMvc.perform(get("/insumos/maltas/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("insumo/malta-form"))
                .andExpect(content().string(containsString("Editar malta")))
                .andExpect(content().string(containsString("value=\"Pilsen\"")))
                .andExpect(content().string(containsString("value=\"75\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"CARAMELO\" selected.*")))
                .andExpect(content().string(containsString("action=\"/insumos/maltas/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    // ==================== POST /insumos/maltas/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-17: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/insumos/maltas/4").with(csrf())
                        .param("nombre", "Munich").param("tipo", "TOSTADA").param("potencialExtracto", "70"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/maltas"))
                .andExpect(flash().attribute("mensaje", "Malta modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(maltaServicio).modificarMalta(eq(4L), any(MaltaFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-18: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(maltaServicio.modificarMalta(eq(4L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una malta con el nombre 'Munich'"));

        mockMvc.perform(post("/insumos/maltas/4").with(csrf())
                        .param("nombre", "Munich").param("tipo", "TOSTADA").param("potencialExtracto", "70"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar la malta")))
                .andExpect(content().string(containsString("Ya existe una malta con el nombre")))
                .andExpect(content().string(containsString("action=\"/insumos/maltas/4\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-MAC-19: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar una malta")
    void modificar_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/maltas/4").with(csrf())
                        .param("nombre", "Munich").param("tipo", "TOSTADA").param("potencialExtracto", "70"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maltaServicio, never()).modificarMalta(any(), any());
    }

    // ==================== POST /insumos/maltas/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-20: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/insumos/maltas/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/maltas"))
                .andExpect(flash().attribute("mensaje", "Malta dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(maltaServicio).bajaMalta(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MAC-21: si el service rechaza la baja (forma parte de una receta o tiene stock) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja la malta porque tiene stock")).when(maltaServicio).bajaMalta(4L);

        mockMvc.perform(post("/insumos/maltas/4/baja").with(csrf()).header("Referer", "/insumos/maltas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insumos/maltas"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja la malta porque tiene stock"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-MAC-22: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja una malta")
    void baja_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/insumos/maltas/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maltaServicio, never()).bajaMalta(any());
    }

    private static MaltaResponseDTO malta(Long id, String nombre, TipoMalta tipo, Integer potencial) {
        return MaltaResponseDTO.builder().id(id).nombre(nombre).tipo(tipo).potencialExtracto(potencial)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
    }
}
