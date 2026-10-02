package com.github.heikyudev.maestrocervecero.presentation.controller.etapa_control;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control.EtapaControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.etapa_control.IEtapaControlServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
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

@WebMvcTest(EtapaControlController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class EtapaControlControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEtapaControlServicio etapaControlServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /recetas/etapas-control ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-01: el GERENTE_DE_PRODUCCION ve el listado con el nombre legible de la etapa, la descripción, las pestañas y las acciones por fila")
    void listado_debeMostrarLasEtapasDeControlParaGerenteDeProduccion() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(etapaControl(4L, "Control de densidad", "Densidad del mosto", TipoEtapa.FERMENTACION)),
                        PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/recetas/etapas-control"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-lista"))
                .andExpect(content().string(containsString("Gestión de Recetas")))
                .andExpect(content().string(containsString("Control de densidad")))
                .andExpect(content().string(containsString("Densidad del mosto")))
                .andExpect(content().string(containsString("Fermentación")))
                .andExpect(content().string(not(containsString("FERMENTACION</span>"))))
                .andExpect(content().string(containsString("placeholder=\"Ej: Control de densidad\"")))
                .andExpect(content().string(containsString("href=\"/recetas/etapas-control/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/recetas/etapas-control/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/recetas/etapas-control/4/baja\"")))
                // La tabla ancha se desplaza horizontalmente dentro de su tarjeta
                .andExpect(content().string(containsString("overflow-x-auto")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-02: las tres pestañas del módulo de recetas enlazan a sus URL y marcan la etapas de control como activa")
    void listado_debeMostrarLasTresPestanias() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/etapas-control"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/recetas\"")))
                .andExpect(content().string(containsString("href=\"/recetas/etapas-control\"")))
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control\"")))
                .andExpect(content().string(containsString("bi-journal-bookmark")))
                .andExpect(content().string(containsString("bi-list-check")))
                .andExpect(content().string(containsString("bi-speedometer2")))
                .andExpect(content().string(containsString("Etapas de control")))
                .andExpect(content().string(containsString("Parámetros de control")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/recetas/etapas-control\"\\s+class=\"[^\"]*border-amber-500 text-amber-600.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-03: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/etapas-control")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etapaControlServicio).filtrarEtapasControl(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-04: los criterios llegan al service; un nombre en blanco se convierte en null y el select \"Todas\" en null")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/recetas/etapas-control").param("nombre", " dens ").param("etapa", "HERVIDO").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/recetas/etapas-control").param("nombre", "   ").param("etapa", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etapaControlServicio).filtrarEtapasControl(eq("dens"), eq(TipoEtapa.HERVIDO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(etapaControlServicio).filtrarEtapasControl(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-05: el filtro de etapa ofrece solo las cuatro etapas controlables, con su nombre legible, y conserva la elegida")
    void listado_debeOfrecerSoloLasEtapasControlables() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/etapas-control").param("etapa", "HERVIDO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"MACERACION\"")))
                .andExpect(content().string(containsString("value=\"HERVIDO\" selected")))
                .andExpect(content().string(containsString("value=\"FERMENTACION\"")))
                .andExpect(content().string(containsString("value=\"MADURACION\"")))
                .andExpect(content().string(not(containsString("value=\"MOLIENDA\""))))
                .andExpect(content().string(not(containsString("value=\"ENVASADO\""))))
                .andExpect(content().string(containsString("Maceración")))
                .andExpect(content().string(containsString("Hervido")))
                .andExpect(content().string(containsString("Maduración")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-06: los enlaces de paginación conservan los criterios de búsqueda")
    void listado_debeConservarLosCriteriosEnLaPaginacion() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(etapaControl(4L, "Control de densidad", null, TipoEtapa.HERVIDO)),
                        PageRequest.of(1, 20), 45));

        mockMvc.perform(get("/recetas/etapas-control").param("nombre", "dens").param("etapa", "HERVIDO").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página <span>2</span> de <span>3</span>")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("nombre=dens")))
                .andExpect(content().string(containsString("etapa=HERVIDO")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-07: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas/etapas-control"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron etapas de control con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-ECC-08: solo el GERENTE_DE_PRODUCCION accede a las etapas de control: otro rol es rechazado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/etapas-control"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(etapaControlServicio, never()).filtrarEtapasControl(any(), any(), any());
    }

    // ==================== GET /recetas/etapas-control/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-09: el formulario de alta ofrece las cuatro etapas controlables, asteriscos de obligatorio y placeholders con ejemplo")
    void formularioAlta_debeRenderizarse() throws Exception {
        mockMvc.perform(get("/recetas/etapas-control/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("Nueva etapa de control")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/recetas/etapas-control\"")))
                .andExpect(content().string(containsString("value=\"MACERACION\"")))
                .andExpect(content().string(containsString("value=\"HERVIDO\"")))
                .andExpect(content().string(containsString("value=\"FERMENTACION\"")))
                .andExpect(content().string(containsString("value=\"MADURACION\"")))
                .andExpect(content().string(not(containsString("value=\"MOLIENDA\""))))
                .andExpect(content().string(not(containsString("value=\"ENVASADO\""))))
                .andExpect(content().string(containsString("Maceración")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Control de densidad\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Medición de la densidad del mosto al finalizar la maceración\"")))
                .andExpect(content().string(containsString("Crear etapa de control")))
                .andExpect(content().string(containsString("Volver a etapas de control")))
                .andExpect(content().string(containsString("whitespace-nowrap")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear la etapa de control"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-10: abierto desde otro formulario (retorno y campo) conserva ambos y cancelar vuelve a ese formulario; un retorno externo se descarta")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/recetas/etapas-control/nuevo").param("retorno", "/recetas/nueva").param("campo", "idEtapaControl"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/recetas/nueva\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idEtapaControl\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/recetas/nueva?desdeAlta=1\"")));

        mockMvc.perform(get("/recetas/etapas-control/nuevo").param("retorno", "//sitio-malicioso.com").param("campo", "idEtapaControl"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-ECC-11: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/etapas-control/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /recetas/etapas-control ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-12: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(etapaControlServicio.altaEtapaControl(any()))
                .thenReturn(etapaControl(9L, "Control de densidad", "Densidad del mosto", TipoEtapa.MACERACION));

        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("descripcion", "Densidad del mosto")
                        .param("etapaAControlar", "MACERACION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/etapas-control"))
                .andExpect(flash().attribute("mensaje", "Etapa de control creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<EtapaControlFormDTO> captor = ArgumentCaptor.forClass(EtapaControlFormDTO.class);
        verify(etapaControlServicio).altaEtapaControl(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Control de densidad");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Densidad del mosto");
        assertThat(captor.getValue().getEtapaAControlar()).isEqualTo(TipoEtapa.MACERACION);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-13: la descripción es opcional: un alta sin descripción es válida")
    void alta_debeAceptarUnaDescripcionVacia() throws Exception {
        when(etapaControlServicio.altaEtapaControl(any()))
                .thenReturn(etapaControl(9L, "Control de densidad", null, TipoEtapa.MACERACION));

        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("etapaAControlar", "MACERACION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/etapas-control"));

        verify(etapaControlServicio).altaEtapaControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-14: sin nombre ni etapa vuelve al formulario con los dos errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("No se pudo crear la etapa de control")))
                .andExpect(content().string(containsString("El nombre de la etapa de control es obligatorio")))
                .andExpect(content().string(containsString("La etapa a controlar es obligatoria")));

        verify(etapaControlServicio, never()).altaEtapaControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-15: una descripción de más de 255 caracteres vuelve al formulario con el error, sin llamar al service")
    void alta_debeRechazarUnaDescripcionDemasiadoLarga() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("etapaAControlar", "MACERACION")
                        .param("descripcion", "x".repeat(256)))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("La descripción no puede superar los 255 caracteres")));

        verify(etapaControlServicio, never()).altaEtapaControl(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-16: un nombre duplicado para la misma etapa vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(etapaControlServicio.altaEtapaControl(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una etapa de control con el nombre 'Control de densidad' para la etapa MACERACION"));

        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("etapaAControlar", "MACERACION"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("Ya existe una etapa de control con el nombre")))
                .andExpect(content().string(containsString("value=\"Control de densidad\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"MACERACION\" selected.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-17: una etapa no controlable rechazada por el service se muestra junto al campo de la etapa")
    void alta_debeMostrarElErrorDeEtapaNoControlable() throws Exception {
        when(etapaControlServicio.altaEtapaControl(any()))
                .thenThrow(new ReglaNegocioException("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION"));

        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("etapaAControlar", "MOLIENDA"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("No se pudo crear la etapa de control")))
                .andExpect(content().string(containsString("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-18: el alta contextual vuelve al formulario de origen con la etapa de control creada en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigen() throws Exception {
        when(etapaControlServicio.altaEtapaControl(any()))
                .thenReturn(etapaControl(9L, "Control de densidad", null, TipoEtapa.MACERACION));

        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("etapaAControlar", "MACERACION")
                        .param("retorno", "/recetas/nueva").param("campo", "idEtapaControl"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/nueva?desdeAlta=1&idEtapaControl=9"))
                .andExpect(flash().attribute("mensaje", "Etapa de control creada correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-ECC-19: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta una etapa de control")
    void alta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control").with(csrf())
                        .param("nombre", "Control de densidad").param("etapaAControlar", "MACERACION"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(etapaControlServicio, never()).altaEtapaControl(any());
    }

    // ==================== GET /recetas/etapas-control/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-20: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(etapaControlServicio.buscarPorId(4L))
                .thenReturn(etapaControl(4L, "Control de densidad", "Densidad del mosto", TipoEtapa.FERMENTACION));

        mockMvc.perform(get("/recetas/etapas-control/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("Editar etapa de control")))
                .andExpect(content().string(containsString("value=\"Control de densidad\"")))
                .andExpect(content().string(containsString("Densidad del mosto")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"FERMENTACION\" selected.*")))
                .andExpect(content().string(containsString("action=\"/recetas/etapas-control/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-ECC-21: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de edición")
    void formularioEdicion_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/etapas-control/4/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(etapaControlServicio, never()).buscarPorId(any());
    }

    // ==================== POST /recetas/etapas-control/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-22: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control/4").with(csrf())
                        .param("nombre", "Control de gravedad").param("etapaAControlar", "HERVIDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/etapas-control"))
                .andExpect(flash().attribute("mensaje", "Etapa de control modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<EtapaControlFormDTO> captor = ArgumentCaptor.forClass(EtapaControlFormDTO.class);
        verify(etapaControlServicio).modificarEtapaControl(eq(4L), captor.capture());
        assertThat(captor.getValue().getEtapaAControlar()).isEqualTo(TipoEtapa.HERVIDO);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-23: modificar con datos inválidos vuelve al formulario de edición con los errores, sin llamar al service")
    void modificar_debeVolverAlFormularioSiLosDatosSonInvalidos() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control/4").with(csrf()).param("nombre", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("No se pudo modificar la etapa de control")))
                .andExpect(content().string(containsString("El nombre de la etapa de control es obligatorio")))
                .andExpect(content().string(containsString("La etapa a controlar es obligatoria")))
                .andExpect(content().string(containsString("action=\"/recetas/etapas-control/4\"")));

        verify(etapaControlServicio, never()).modificarEtapaControl(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-24: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(etapaControlServicio.modificarEtapaControl(eq(4L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una etapa de control con el nombre 'Control de gravedad' para la etapa HERVIDO"));

        mockMvc.perform(post("/recetas/etapas-control/4").with(csrf())
                        .param("nombre", "Control de gravedad").param("etapaAControlar", "HERVIDO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar la etapa de control")))
                .andExpect(content().string(containsString("Ya existe una etapa de control con el nombre")))
                .andExpect(content().string(containsString("action=\"/recetas/etapas-control/4\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-25: modificar a una etapa no controlable vuelve al formulario de edición con el mensaje del service")
    void modificar_debeMostrarElErrorDeEtapaNoControlable() throws Exception {
        when(etapaControlServicio.modificarEtapaControl(eq(4L), any()))
                .thenThrow(new ReglaNegocioException("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION"));

        mockMvc.perform(post("/recetas/etapas-control/4").with(csrf())
                        .param("nombre", "Control de gravedad").param("etapaAControlar", "ENVASADO"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/etapa-control-form"))
                .andExpect(content().string(containsString("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-ECC-26: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar una etapa de control")
    void modificar_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control/4").with(csrf())
                        .param("nombre", "Control de gravedad").param("etapaAControlar", "HERVIDO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(etapaControlServicio, never()).modificarEtapaControl(any(), any());
    }

    // ==================== POST /recetas/etapas-control/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-27: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/etapas-control"))
                .andExpect(flash().attribute("mensaje", "Etapa de control dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(etapaControlServicio).bajaEtapaControl(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-ECC-28: si el service rechaza la baja (plan de monitoreo de una receta activa) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja la etapa de control porque está asociada al plan de monitoreo de una receta activa"))
                .when(etapaControlServicio).bajaEtapaControl(4L);

        mockMvc.perform(post("/recetas/etapas-control/4/baja").with(csrf()).header("Referer", "/recetas/etapas-control"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas/etapas-control"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja la etapa de control porque está asociada al plan de monitoreo de una receta activa"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-ECC-29: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja una etapa de control")
    void baja_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/etapas-control/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(etapaControlServicio, never()).bajaEtapaControl(any());
    }

    private static EtapaControlResponseDTO etapaControl(Long id, String nombre, String descripcion, TipoEtapa etapa) {
        return EtapaControlResponseDTO.builder().id(id).nombre(nombre).descripcion(descripcion).etapaAControlar(etapa).build();
    }
}
