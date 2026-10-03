package com.github.heikyudev.maestrocervecero.presentation.controller.parametro_control;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control.ParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.parametro_control.IParametroControlServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
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

@WebMvcTest(ParametroControlController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class ParametroControlControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IParametroControlServicio parametroControlServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /recetas/parametros-control ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-01: el GERENTE_DE_PRODUCCION ve el listado con la unidad, el rango sin ceros sobrantes, las pestañas y las acciones por fila, sin la descripción en la tabla")
    void listado_debeMostrarLosParametrosParaGerenteDeProduccion() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(parametro(4L, "Temperatura de maceración", "Temperatura del macerado", 62.0, 68.5)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/recetas/parametros-control"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-lista"))
                .andExpect(content().string(containsString("Gestión de Recetas")))
                .andExpect(content().string(containsString("Temperatura de maceración")))
                .andExpect(content().string(containsString(">Unidad<")))
                .andExpect(content().string(containsString(">°C<")))
                // La descripción no es una columna de la tabla: se consulta con el botón "Ver descripción"
                .andExpect(content().string(not(containsString(">Descripción<"))))
                .andExpect(content().string(not(matchesPattern("(?s).*<td[^>]*>\\s*Temperatura del macerado.*"))))
                .andExpect(content().string(containsString(">62<")))
                .andExpect(content().string(containsString(">68,5<")))
                .andExpect(content().string(not(containsString("62.0"))))
                .andExpect(content().string(containsString("placeholder=\"Ej: Temperatura\"")))
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/recetas/parametros-control/4/baja\"")))
                // La tabla ancha se desplaza horizontalmente dentro de su tarjeta
                .andExpect(content().string(containsString("overflow-x-auto")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-02: las tres pestañas del módulo de recetas enlazan a sus URL y marcan la de parámetros de control como activa")
    void listado_debeMostrarLasTresPestanias() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/parametros-control"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/recetas\"")))
                .andExpect(content().string(containsString("href=\"/recetas/etapas-control\"")))
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control\"")))
                .andExpect(content().string(containsString("bi-journal-bookmark")))
                .andExpect(content().string(containsString("bi-list-check")))
                .andExpect(content().string(containsString("bi-speedometer2")))
                .andExpect(content().string(containsString("Etapas de control")))
                .andExpect(content().string(containsString("Parámetros de control")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/recetas/parametros-control\"\\s+class=\"[^\"]*border-amber-500 text-amber-600.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-03: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con el criterio en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/parametros-control")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(parametroControlServicio).filtrarParametrosControl(isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-04: el criterio llega al service sin espacios sobrantes; un nombre en blanco se convierte en null")
    void listado_debePasarElFiltroAlService() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/recetas/parametros-control").param("nombre", " temp ").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/recetas/parametros-control").param("nombre", "   "))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(parametroControlServicio).filtrarParametrosControl(eq("temp"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(parametroControlServicio).filtrarParametrosControl(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-05: los enlaces de paginación conservan el criterio de búsqueda")
    void listado_debeConservarElCriterioEnLaPaginacion() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(parametro(4L, "Temperatura (°C)", null, 62.0, 68.0)),
                        PageRequest.of(1, 20), 45));

        mockMvc.perform(get("/recetas/parametros-control").param("nombre", "temp").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página <span>2</span> de <span>3</span>")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("nombre=temp")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-06: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/parametros-control"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron parámetros de control con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PCC-07: solo el GERENTE_DE_PRODUCCION accede a los parámetros de control: otro rol es rechazado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/parametros-control"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(parametroControlServicio, never()).filtrarParametrosControl(any(), any());
    }

    // ==================== GET /recetas/parametros-control/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-08: el formulario de alta tiene el campo de unidad de medida, marca los obligatorios y muestra placeholders con ejemplo")
    void formularioAlta_debeRenderizarse() throws Exception {
        mockMvc.perform(get("/recetas/parametros-control/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("Nuevo parámetro de control")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/recetas/parametros-control\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Temperatura\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: °C\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 62\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 68\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Temperatura del mosto en el punto de control\"")))
                .andExpect(content().string(containsString("name=\"unidadDeMedida\"")))
                .andExpect(content().string(containsString("Unidad de medida")))
                .andExpect(content().string(containsString("Los valores mínimo y máximo se expresan en la unidad de medida indicada.")))
                .andExpect(content().string(containsString("Crear parámetro de control")))
                .andExpect(content().string(containsString("Volver a parámetros de control")))
                .andExpect(content().string(containsString("whitespace-nowrap")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear el parámetro de control"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-09: abierto desde otro formulario (retorno y campo) conserva ambos y cancelar vuelve a ese formulario; un retorno externo se descarta")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/recetas/parametros-control/nuevo").param("retorno", "/recetas/nueva").param("campo", "idParametroControl"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/recetas/nueva\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idParametroControl\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/recetas/nueva?desdeAlta=1\"")));

        mockMvc.perform(get("/recetas/parametros-control/nuevo").param("retorno", "//sitio-malicioso.com").param("campo", "idParametroControl"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PCC-10: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/parametros-control/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /recetas/parametros-control ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-11: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(parametroControlServicio.altaParametroControl(any()))
                .thenReturn(parametro(9L, "Temperatura (°C)", "Temperatura del macerado", 62.0, 68.5));

        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura (°C)").param("descripcion", "Temperatura del macerado")
                        .param("unidadDeMedida", "°C").param("valorMinimo", "62").param("valorMaximo", "68.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/parametros-control"))
                .andExpect(flash().attribute("mensaje", "Parámetro de control creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ParametroControlFormDTO> captor = ArgumentCaptor.forClass(ParametroControlFormDTO.class);
        verify(parametroControlServicio).altaParametroControl(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Temperatura (°C)");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Temperatura del macerado");
        assertThat(captor.getValue().getUnidadDeMedida()).isEqualTo("°C");
        assertThat(captor.getValue().getValorMinimo()).isEqualTo(62.0);
        assertThat(captor.getValue().getValorMaximo()).isEqualTo(68.5);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-12: la descripción es opcional: un alta sin descripción es válida")
    void alta_debeAceptarUnaDescripcionVacia() throws Exception {
        when(parametroControlServicio.altaParametroControl(any()))
                .thenReturn(parametro(9L, "Temperatura (°C)", null, 62.0, 68.0));

        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura (°C)").param("unidadDeMedida", "°C").param("valorMinimo", "62").param("valorMaximo", "68"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/parametros-control"));

        verify(parametroControlServicio).altaParametroControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-13: sin nombre, unidad ni valores vuelve al formulario con los cuatro errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("No se pudo crear el parámetro de control")))
                .andExpect(content().string(containsString("El nombre del parámetro de control es obligatorio")))
                .andExpect(content().string(containsString("La unidad de medida es obligatoria")))
                .andExpect(content().string(containsString("El valor mínimo es obligatorio")))
                .andExpect(content().string(containsString("El valor máximo es obligatorio")));

        verify(parametroControlServicio, never()).altaParametroControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-14: un valor no numérico muestra un mensaje en español, sin llamar al service")
    void alta_debeMostrarUnMensajeEnEspanolParaUnNumeroInvalido() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura (°C)").param("unidadDeMedida", "°C").param("valorMinimo", "abc").param("valorMaximo", "68"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresá un número válido")));

        verify(parametroControlServicio, never()).altaParametroControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-15: una descripción de más de 255 caracteres vuelve al formulario con el error, sin llamar al service")
    void alta_debeRechazarUnaDescripcionDemasiadoLarga() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura (°C)").param("unidadDeMedida", "°C").param("valorMinimo", "62").param("valorMaximo", "68")
                        .param("descripcion", "x".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("La descripción no puede superar los 255 caracteres")));

        verify(parametroControlServicio, never()).altaParametroControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-16: un nombre duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(parametroControlServicio.altaParametroControl(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un parámetro de control con el nombre 'Temperatura (°C)'"));

        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura").param("unidadDeMedida", "°C").param("valorMinimo", "62").param("valorMaximo", "68"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("Ya existe un parámetro de control con el nombre")))
                .andExpect(content().string(containsString("value=\"Temperatura\"")))
                .andExpect(content().string(containsString("value=\"62.0\"")))
                .andExpect(content().string(containsString("value=\"68.0\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-17: un rango inválido rechazado por el service se muestra en el panel de errores y conserva lo tipeado")
    void alta_debeMostrarElErrorDelRango() throws Exception {
        when(parametroControlServicio.altaParametroControl(any()))
                .thenThrow(new ReglaNegocioException("El valor mínimo no puede ser mayor al valor máximo"));

        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura").param("unidadDeMedida", "°C").param("valorMinimo", "70").param("valorMaximo", "60"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("No se pudo crear el parámetro de control")))
                .andExpect(content().string(containsString("El valor mínimo no puede ser mayor al valor máximo")))
                .andExpect(content().string(containsString("value=\"70.0\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-18: el alta contextual vuelve al formulario de origen con el parámetro creado en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigen() throws Exception {
        when(parametroControlServicio.altaParametroControl(any()))
                .thenReturn(parametro(9L, "Temperatura (°C)", null, 62.0, 68.0));

        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura (°C)").param("unidadDeMedida", "°C").param("valorMinimo", "62").param("valorMaximo", "68")
                        .param("retorno", "/recetas/nueva").param("campo", "idParametroControl"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/nueva?desdeAlta=1&idParametroControl=9"))
                .andExpect(flash().attribute("mensaje", "Parámetro de control creado correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PCC-19: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta un parámetro de control")
    void alta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura").param("unidadDeMedida", "°C").param("valorMinimo", "62").param("valorMaximo", "68"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(parametroControlServicio, never()).altaParametroControl(any());
    }

    // ==================== GET /recetas/parametros-control/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-20: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(parametroControlServicio.buscarPorId(4L))
                .thenReturn(parametro(4L, "Temperatura", "Temperatura del macerado", 62.0, 68.5));

        mockMvc.perform(get("/recetas/parametros-control/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("Editar parámetro de control")))
                .andExpect(content().string(containsString("value=\"Temperatura\"")))
                .andExpect(content().string(containsString("value=\"&deg;C\"")))
                .andExpect(content().string(containsString("value=\"62.0\"")))
                .andExpect(content().string(containsString("value=\"68.5\"")))
                .andExpect(content().string(containsString("Temperatura del macerado")))
                .andExpect(content().string(containsString("action=\"/recetas/parametros-control/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PCC-21: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de edición")
    void formularioEdicion_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/parametros-control/4/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(parametroControlServicio, never()).buscarPorId(any());
    }

    // ==================== POST /recetas/parametros-control/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-22: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control/4").with(csrf())
                        .param("nombre", "pH").param("unidadDeMedida", "°C").param("valorMinimo", "5.2").param("valorMaximo", "5.6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/parametros-control"))
                .andExpect(flash().attribute("mensaje", "Parámetro de control modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ParametroControlFormDTO> captor = ArgumentCaptor.forClass(ParametroControlFormDTO.class);
        verify(parametroControlServicio).modificarParametroControl(eq(4L), captor.capture());
        assertThat(captor.getValue().getValorMinimo()).isEqualTo(5.2);
        assertThat(captor.getValue().getValorMaximo()).isEqualTo(5.6);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-23: modificar con datos inválidos vuelve al formulario de edición con los errores, sin llamar al service")
    void modificar_debeVolverAlFormularioSiLosDatosSonInvalidos() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control/4").with(csrf()).param("nombre", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("No se pudo modificar el parámetro de control")))
                .andExpect(content().string(containsString("El valor mínimo es obligatorio")))
                .andExpect(content().string(containsString("action=\"/recetas/parametros-control/4\"")));

        verify(parametroControlServicio, never()).modificarParametroControl(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-24: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(parametroControlServicio.modificarParametroControl(eq(4L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un parámetro de control con el nombre 'pH'"));

        mockMvc.perform(post("/recetas/parametros-control/4").with(csrf())
                        .param("nombre", "pH").param("unidadDeMedida", "°C").param("valorMinimo", "5.2").param("valorMaximo", "5.6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe un parámetro de control con el nombre")))
                .andExpect(content().string(containsString("action=\"/recetas/parametros-control/4\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-25: modificar con un mínimo que no es mayor a 0 vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDelRango() throws Exception {
        when(parametroControlServicio.modificarParametroControl(eq(4L), any()))
                .thenThrow(new ReglaNegocioException("El valor mínimo debe ser mayor a 0"));

        mockMvc.perform(post("/recetas/parametros-control/4").with(csrf())
                        .param("nombre", "pH").param("unidadDeMedida", "°C").param("valorMinimo", "0").param("valorMaximo", "5.6"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("El valor mínimo debe ser mayor a 0")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PCC-26: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar un parámetro de control")
    void modificar_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control/4").with(csrf())
                        .param("nombre", "pH").param("unidadDeMedida", "°C").param("valorMinimo", "5.2").param("valorMaximo", "5.6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(parametroControlServicio, never()).modificarParametroControl(any(), any());
    }

    // ==================== POST /recetas/parametros-control/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-27: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/parametros-control"))
                .andExpect(flash().attribute("mensaje", "Parámetro de control dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(parametroControlServicio).bajaParametroControl(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-28: si el service rechaza la baja (plan de monitoreo de una receta activa) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja el parámetro de control porque está asociado al plan de monitoreo de una receta activa"))
                .when(parametroControlServicio).bajaParametroControl(4L);

        mockMvc.perform(post("/recetas/parametros-control/4/baja").with(csrf()).header("Referer", "/recetas/parametros-control"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/parametros-control"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el parámetro de control porque está asociado al plan de monitoreo de una receta activa"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PCC-29: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja un parámetro de control")
    void baja_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(parametroControlServicio, never()).bajaParametroControl(any());
    }

    // ==================== unidad de medida y visor de descripción ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-30: una unidad de medida en blanco vuelve al formulario con el error, sin llamar al service")
    void alta_debeRechazarUnaUnidadDeMedidaEnBlanco() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura").param("unidadDeMedida", "   ")
                        .param("valorMinimo", "62").param("valorMaximo", "68"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("La unidad de medida es obligatoria")))
                .andExpect(content().string(containsString("value=\"Temperatura\"")));

        verify(parametroControlServicio, never()).altaParametroControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-31: una unidad de medida de más de 20 caracteres vuelve al formulario con el error, sin llamar al service")
    void alta_debeRechazarUnaUnidadDeMedidaDemasiadoLarga() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control").with(csrf())
                        .param("nombre", "Temperatura").param("unidadDeMedida", "x".repeat(21))
                        .param("valorMinimo", "62").param("valorMaximo", "68"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("La unidad de medida no puede superar los 20 caracteres")));

        verify(parametroControlServicio, never()).altaParametroControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-32: modificar con una unidad de medida en blanco vuelve al formulario de edición con el error, sin llamar al service")
    void modificar_debeRechazarUnaUnidadDeMedidaEnBlanco() throws Exception {
        mockMvc.perform(post("/recetas/parametros-control/4").with(csrf())
                        .param("nombre", "pH").param("unidadDeMedida", "")
                        .param("valorMinimo", "5.2").param("valorMaximo", "5.6"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/parametro-control-form"))
                .andExpect(content().string(containsString("No se pudo modificar el parámetro de control")))
                .andExpect(content().string(containsString("La unidad de medida es obligatoria")))
                .andExpect(content().string(containsString("action=\"/recetas/parametros-control/4\"")));

        verify(parametroControlServicio, never()).modificarParametroControl(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-33: la fila con descripción muestra el botón \"Ver descripción\" con el nombre y el texto escapados, y el diálogo se renderiza una vez")
    void listado_debeMostrarElBotonVerDescripcionCuandoHayDescripcion() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(parametro(4L, "Temperatura", "Línea 1 <b>negrita</b>", 62.0, 68.0)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/recetas/parametros-control"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("title=\"Ver descripción\"")))
                .andExpect(content().string(containsString("aria-label=\"Ver descripción\"")))
                .andExpect(content().string(containsString("bi-info-circle")))
                .andExpect(content().string(containsString("data-descripcion-titulo=\"Temperatura\"")))
                .andExpect(content().string(containsString("data-descripcion-texto=\"Línea 1 &lt;b&gt;negrita&lt;/b&gt;\"")))
                .andExpect(content().string(not(containsString("<b>negrita</b>"))))
                // Diálogo compartido: texto con saltos de línea respetados y cierre con botón
                .andExpect(content().string(containsString("<dialog id=\"dialogo-descripcion\"")))
                .andExpect(content().string(containsString("whitespace-pre-wrap break-words")))
                .andExpect(content().string(containsString("data-cerrar-descripcion")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-34: la fila sin descripción (nula o en blanco) no muestra el botón \"Ver descripción\"")
    void listado_noDebeMostrarElBotonVerDescripcionSinDescripcion() throws Exception {
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        parametro(4L, "Temperatura", null, 62.0, 68.0),
                        parametro(5L, "pH", "   ", 5.0, 5.5)),
                        PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/recetas/parametros-control"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control/4/editar\"")))
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control/5/editar\"")))
                .andExpect(content().string(not(containsString("data-descripcion-texto="))))
                .andExpect(content().string(not(containsString("title=\"Ver descripción\""))));
    }

    private static ParametroControlResponseDTO parametro(Long id, String nombre, String descripcion, Double minimo, Double maximo) {
        return ParametroControlResponseDTO.builder().id(id).nombre(nombre).descripcion(descripcion)
                .unidadDeMedida("°C").valorMinimo(minimo).valorMaximo(maximo).build();
    }
}
