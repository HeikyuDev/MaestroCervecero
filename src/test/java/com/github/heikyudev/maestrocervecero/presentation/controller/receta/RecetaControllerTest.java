package com.github.heikyudev.maestrocervecero.presentation.controller.receta;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.etapa_control.IEtapaControlServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IMaltaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.parametro_control.IParametroControlServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleMaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.PlanMonitoreoEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;

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

@WebMvcTest(RecetaController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class RecetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private IMaltaServicio maltaServicio;

    @MockitoBean
    private ILupuloServicio lupuloServicio;

    @MockitoBean
    private ILevaduraServicio levaduraServicio;

    @MockitoBean
    private IEtapaControlServicio etapaControlServicio;

    @MockitoBean
    private IParametroControlServicio parametroControlServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /recetas ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-01: el GERENTE_DE_PRODUCCION ve el listado con los datos de la versión vigente, la duración total y las acciones por fila")
    void listado_debeMostrarLasRecetasParaGerenteDeProduccion() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(7L, "Pale Ale")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-lista"))
                .andExpect(content().string(containsString("Gestión de Recetas")))
                .andExpect(content().string(containsString("Pale Ale")))
                .andExpect(content().string(containsString("Volumen base (L)")))
                .andExpect(content().string(containsString("OG objetivo")))
                .andExpect(content().string(containsString("FG objetivo")))
                .andExpect(content().string(containsString("IBU objetivo")))
                .andExpect(content().string(containsString("Duración total estimada")))
                .andExpect(content().string(containsString(">20<")))
                .andExpect(content().string(containsString(">1,050<")))
                .andExpect(content().string(containsString(">35<")))
                .andExpect(content().string(containsString("14 días 2 h 30 min")))
                .andExpect(content().string(not(containsString("20.0"))))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pale Ale\"")))
                .andExpect(content().string(containsString("href=\"/recetas/nuevo\"")))
                .andExpect(content().string(containsString("Nueva receta")))
                .andExpect(content().string(containsString("href=\"/recetas/7\"")))
                .andExpect(content().string(containsString("bi-eye")))
                .andExpect(content().string(containsString("href=\"/recetas/7/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/recetas/7/baja\"")))
                .andExpect(content().string(containsString("data-baja-nombre=\"Pale Ale\"")))
                // La tabla ancha se desplaza horizontalmente dentro de su tarjeta
                .andExpect(content().string(containsString("overflow-x-auto")))
                .andExpect(content().string(containsString("whitespace-nowrap")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-02: las tres pestañas del módulo enlazan a sus URL y marcan la de recetas como activa")
    void listado_debeMostrarLasTresPestanias() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/recetas/etapas-control\"")))
                .andExpect(content().string(containsString("href=\"/recetas/parametros-control\"")))
                .andExpect(content().string(containsString("Etapas de control")))
                .andExpect(content().string(containsString("Parámetros de control")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/recetas\"\\s+class=\"[^\"]*border-amber-500 text-amber-600[^\"]*\"[^>]*>\\s*<i class=\"bi bi-journal-bookmark.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-03: sin filtros pide páginas de 20, página 0 y sin ordenamiento en el Pageable, con el criterio en null")
    void listado_debePedirPaginasDe20() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio).filtrarRecetas(isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("CP-RCC-04: el orden alfabético por el nombre de la versión vigente (sin distinguir mayúsculas, con el id como desempate) lo define la consulta del repositorio")
    void listado_elOrdenAlfabeticoLoDefineLaConsultaDelRepositorio() throws Exception {
        Query consulta = IRecetaRepository.class
                .getMethod("filtrarRecetas", String.class, Pageable.class)
                .getAnnotation(Query.class);

        assertThat(consulta.value()).contains("ORDER BY UPPER(v.nombre) ASC, r.id ASC");
        assertThat(consulta.countQuery()).doesNotContain("ORDER BY");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-05: el criterio llega al service sin espacios sobrantes; un nombre en blanco se convierte en null")
    void listado_debePasarElFiltroAlService() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/recetas").param("nombre", " pale ").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/recetas").param("nombre", "   "))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio).filtrarRecetas(eq("pale"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(recetaServicio).filtrarRecetas(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-06: los enlaces de paginación conservan el criterio de búsqueda")
    void listado_debeConservarElCriterioEnLaPaginacion() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(7L, "Pale Ale")), PageRequest.of(1, 20), 45));

        mockMvc.perform(get("/recetas").param("nombre", "pale").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página <span>2</span> de <span>3</span>")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("nombre=pale")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-07: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron recetas con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-08: el diálogo de baja del listado envía un POST con token CSRF y avisa de la regla de planificaciones pendientes")
    void listado_debeIncluirElDialogoDeBajaConCsrf() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(7L, "Pale Ale")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dar de baja receta")))
                .andExpect(content().string(containsString("Si tiene una planificación de producción pendiente, el sistema no lo permitirá.")))
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-RCC-09: solo el GERENTE_DE_PRODUCCION accede al listado: otro rol es rechazado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(recetaServicio, never()).filtrarRecetas(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-10: en la sidebar \"Gestión de Recetas\" está en la sección Administración, junto a insumos y equipamiento, y no en Operación")
    void sidebar_debeUbicarGestionDeRecetasEnAdministracion() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/recetas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Operación")))
                .andExpect(content().string(containsString("Administración")))
                .andExpect(content().string(not(matchesPattern("(?s).*Operación((?!Administración).)*Gestión de Recetas.*"))))
                .andExpect(content().string(matchesPattern("(?s).*Administración.*Gestión de Insumos.*Gestión de Recetas.*Gestión de Equipamiento.*")))
                .andExpect(content().string(containsString("Control de Lotes")));
    }

    // ==================== GET /recetas/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-11: el detalle muestra todas las secciones de la versión vigente con sus unidades, enums legibles y la duración total")
    void detalle_debeMostrarTodasLasSecciones() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));

        mockMvc.perform(get("/recetas/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-detalle"))
                // Datos generales
                .andExpect(content().string(containsString("Datos generales")))
                .andExpect(content().string(containsString("Volumen base (L)")))
                .andExpect(content().string(containsString("Relación de empaste (L/kg)")))
                .andExpect(content().string(containsString("OG objetivo")))
                .andExpect(content().string(containsString("FG objetivo")))
                .andExpect(content().string(containsString("IBU objetivo")))
                .andExpect(content().string(containsString(">3<")))
                .andExpect(content().string(containsString(">1,050<")))
                .andExpect(content().string(containsString(">1,010<")))
                // Duraciones estimadas
                .andExpect(content().string(containsString("Duraciones estimadas")))
                .andExpect(content().string(containsString("Maceración (minutos)")))
                .andExpect(content().string(containsString("Hervido (minutos)")))
                .andExpect(content().string(containsString("Fermentación (días)")))
                .andExpect(content().string(containsString("Maduración (días)")))
                .andExpect(content().string(containsString("Duración total estimada")))
                .andExpect(content().string(containsString("14 días 2 h 30 min")))
                // Maltas
                .andExpect(content().string(containsString("Maltas (cantidad en kg)")))
                .andExpect(content().string(containsString("Pilsen")))
                .andExpect(content().string(matchesPattern("(?s).*Pilsen.*?>\\s*Base\\s*<.*?>\\s*4,5\\s*<.*")))
                // Levaduras
                .andExpect(content().string(containsString("Levaduras (cantidad en g)")))
                .andExpect(content().string(matchesPattern("(?s).*US-05.*?>\\s*Ale\\s*<.*?>\\s*11,5\\s*<.*")))
                // Plan de monitoreo
                .andExpect(content().string(containsString("Planes de monitoreo")))
                .andExpect(content().string(containsString("Control de densidad")))
                .andExpect(content().string(matchesPattern("(?s).*etapa a controlar: Fermentación.*")))
                .andExpect(content().string(matchesPattern("(?s).*>Densidad<.*?>SG<.*?>\\s*1,01\\s*<.*?>\\s*1,012\\s*<.*?>\\s*1,015\\s*<.*")))
                .andExpect(content().string(not(containsString("La receta no tiene plan de monitoreo."))))
                // Botones
                .andExpect(content().string(containsString("Volver al listado")))
                .andExpect(content().string(containsString("href=\"/recetas\"")))
                .andExpect(content().string(containsString("href=\"/recetas/7/editar\"")))
                .andExpect(content().string(not(containsString("NaN"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-12: los lúpulos muestran formato, alfa, uso y etapa legibles; el tiempo de hervor solo aparece en el uso Hervor")
    void detalle_debeMostrarElTiempoDeHervorSoloParaHervor() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));

        mockMvc.perform(get("/recetas/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lúpulos (cantidad en g, tiempo de hervor en minutos)")))
                .andExpect(content().string(containsString("Tiempo de hervor (min)")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*Cascade.*?>\\s*Pellet\\s*<.*?>\\s*5,5\\s*<.*?>\\s*Hervor\\s*<.*?>\\s*Hervido\\s*<.*?>\\s*60\\s*<.*?>\\s*25\\s*<.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*Citra.*?>\\s*Flor\\s*<.*?>\\s*12\\s*<.*?>\\s*Dry hop\\s*<.*?>\\s*Fermentación\\s*<.*?>\\s*-\\s*<.*?>\\s*40\\s*<.*")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*Mosaic.*?>\\s*Whirlpool\\s*<.*?>\\s*Hervido\\s*<.*?>\\s*-\\s*<.*?>\\s*30\\s*<.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-13: una receta sin plan de monitoreo lo dice claramente y no muestra tablas de parámetros")
    void detalle_debeIndicarQueNoHayPlanDeMonitoreo() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(RecetaResponseDTO.builder().id(7L)
                .version(versionBuilder("Pale Ale").planesMonitoreo(List.of()).build()).build());

        mockMvc.perform(get("/recetas/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Planes de monitoreo")))
                .andExpect(content().string(containsString("La receta no tiene plan de monitoreo.")))
                .andExpect(content().string(not(containsString("Control de densidad"))))
                .andExpect(content().string(not(containsString(">Parámetro<"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-14: una receta inexistente vuelve al origen con la alerta de error del service")
    void detalle_debeMostrarElErrorSiLaRecetaNoExiste() throws Exception {
        when(recetaServicio.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"));

        mockMvc.perform(get("/recetas/99").header("Referer", "/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "No se encontró la receta con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-RCC-15: un rol distinto de GERENTE_DE_PRODUCCION no puede ver el detalle y no llega al service")
    void detalle_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(recetaServicio, never()).buscarPorId(any());
    }

    // ==================== POST /recetas/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-16: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/recetas/7/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "Receta dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(recetaServicio).bajaReceta(7L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-17: si el service rechaza la baja (planificación PENDIENTE) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja la receta porque tiene una planificación de producción en estado PENDIENTE asociada"))
                .when(recetaServicio).bajaReceta(7L);

        mockMvc.perform(post("/recetas/7/baja").with(csrf()).header("Referer", "/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja la receta porque tiene una planificación de producción en estado PENDIENTE asociada"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-18: dar de baja una receta inexistente vuelve al origen con la alerta de error del service")
    void baja_debeMostrarElErrorSiLaRecetaNoExiste() throws Exception {
        doThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"))
                .when(recetaServicio).bajaReceta(99L);

        mockMvc.perform(post("/recetas/99/baja").with(csrf()).header("Referer", "/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "No se encontró la receta con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-RCC-19: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja una receta")
    void baja_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(post("/recetas/7/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(recetaServicio, never()).bajaReceta(any());
    }

    // ==================== GET /recetas/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-20: el formulario de alta muestra las secciones, los placeholders de ejemplo, los botones de agregar y ninguna opción de crear insumos, etapas ni parámetros")
    void formularioAlta_debeMostrarLasSeccionesYLosCamposVacios() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("Nueva receta")))
                .andExpect(content().string(containsString("action=\"/recetas\"")))
                .andExpect(content().string(containsString("Datos de la receta")))
                .andExpect(content().string(containsString("Objetivos de la receta")))
                .andExpect(content().string(containsString("escala los insumos de la receta para llegar a ellos")))
                .andExpect(content().string(containsString("Duraciones estimadas")))
                .andExpect(content().string(containsString("Maltas")))
                .andExpect(content().string(containsString("Plan de monitoreo")))
                // Placeholders de ejemplo en todos los campos
                .andExpect(content().string(containsString("placeholder=\"Ej: Pale Ale\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 20\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 3\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 1050\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 1010\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 35\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 60\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 90\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 10\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 7\"")))
                // Las unidades van en la etiqueta de cada campo y en la cabecera de cada columna
                .andExpect(content().string(containsString("Volumen base (litros)")))
                .andExpect(content().string(containsString("Relación de empaste (L/kg)")))
                .andExpect(content().string(containsString("IBU objetivo (IBU)")))
                .andExpect(content().string(containsString("Maceración (minutos)")))
                .andExpect(content().string(containsString("Hervido (minutos)")))
                .andExpect(content().string(containsString("Fermentación (días)")))
                .andExpect(content().string(containsString("Maduración (días)")))
                .andExpect(content().string(containsString("Cantidad (kg)")))
                .andExpect(content().string(containsString("Cantidad (g)")))
                .andExpect(content().string(containsString("Tiempo de hervor (min)")))
                // Botones de agregar y tablas vacias con su texto
                .andExpect(content().string(containsString("Agregar malta")))
                .andExpect(content().string(containsString("Agregar lúpulo")))
                .andExpect(content().string(containsString("Agregar levadura")))
                .andExpect(content().string(containsString("data-abrir-agregar=\"malta\"")))
                .andExpect(content().string(containsString("data-abrir-agregar=\"lupulo\"")))
                .andExpect(content().string(containsString("data-abrir-agregar=\"levadura\"")))
                .andExpect(content().string(containsString("Todavía no agregaste maltas a la receta.")))
                .andExpect(content().string(containsString("Todavía no agregaste lúpulos a la receta.")))
                .andExpect(content().string(containsString("Todavía no agregaste levaduras a la receta.")))
                .andExpect(content().string(containsString("La receta no tiene plan de monitoreo.")))
                .andExpect(content().string(containsString("Agregar plan de monitoreo")))
                // Sin alta contextual de insumos, etapas ni parametros
                .andExpect(content().string(not(containsString("data-selector-alta"))))
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(not(containsString("Crear nuev"))))
                // Acciones y comportamiento
                .andExpect(content().string(containsString("href=\"/recetas\"")))
                .andExpect(content().string(containsString("Crear receta")))
                .andExpect(content().string(containsString("Cancelar")))
                .andExpect(content().string(containsString("/js/receta-form.js")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-21: las ventanas emergentes de agregar usan el selector de entidad con ícono de lista, el buscador de cada insumo, las unidades en el encabezado y \"Agregar a la receta\"")
    void formularioAlta_debeIncluirLasVentanasEmergentesDeAgregar() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dialogo-agregar-malta\"")))
                .andExpect(content().string(containsString("id=\"dialogo-agregar-lupulo\"")))
                .andExpect(content().string(containsString("id=\"dialogo-agregar-levadura\"")))
                // Selector compartido: campo de solo lectura con icono de lista y la URL del buscador
                .andExpect(content().string(containsString("data-buscador-url=\"/recetas/buscador/maltas\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/recetas/buscador/lupulos\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/recetas/buscador/levaduras\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar malta\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar lúpulo\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar levadura\"")))
                .andExpect(content().string(containsString("bi bi-list-ul")))
                // Unidades en la etiqueta de cada campo y placeholders de ejemplo de la cantidad
                .andExpect(content().string(containsString("Cantidad (kg) <span")))
                .andExpect(content().string(containsString("Cantidad (g) <span")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 4,5\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 25\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 11,5\"")))
                // Uso del lupulo: Hervor muestra el tiempo; Dry hop la etapa (Fermentacion / Maduracion)
                .andExpect(content().string(containsString("<option value=\"HERVOR\">Hervor</option>")))
                .andExpect(content().string(containsString("<option value=\"WHIRLPOOL\">Whirlpool</option>")))
                .andExpect(content().string(containsString("<option value=\"DRY_HOP\">Dry hop</option>")))
                .andExpect(content().string(containsString("Tiempo de hervor (minutos)")))
                .andExpect(content().string(containsString("data-grupo=\"tiempoDeHervor\"")))
                .andExpect(content().string(containsString("data-grupo=\"etapaDeUso\"")))
                .andExpect(content().string(containsString("<option value=\"FERMENTACION\">")))
                .andExpect(content().string(containsString("<option value=\"MADURACION\">")))
                // La etapa HERVIDO nunca se ofrece: la define el sistema
                .andExpect(content().string(not(containsString("<option value=\"HERVIDO\">"))))
                .andExpect(content().string(containsString("Agregar a la receta")))
                .andExpect(content().string(containsString("data-confirmar-agregar")))
                // Las filas se clonan de una plantilla con el mismo markup que las del servidor
                .andExpect(content().string(containsString("data-plantilla=\"malta\"")))
                .andExpect(content().string(containsString("data-plantilla=\"lupulo\"")))
                .andExpect(content().string(containsString("data-plantilla=\"levadura\"")))
                .andExpect(content().string(containsString("data-quitar-fila")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-22: los campos obligatorios llevan asterisco rojo y los campos del formulario se enlazan al DTO anidado (version.*)")
    void formularioAlta_debeMarcarObligatoriosYEnlazarCamposAnidados() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>")))
                .andExpect(content().string(containsString("name=\"version.nombre\"")))
                .andExpect(content().string(containsString("name=\"version.volumenBase\"")))
                .andExpect(content().string(containsString("name=\"version.relacionDeEmpaste\"")))
                .andExpect(content().string(containsString("name=\"version.ogObjetivo\"")))
                .andExpect(content().string(containsString("name=\"version.fgObjetivo\"")))
                .andExpect(content().string(containsString("name=\"version.ibuObjetivo\"")))
                .andExpect(content().string(containsString("name=\"version.duracionMaceracion\"")))
                .andExpect(content().string(containsString("name=\"version.duracionHervido\"")))
                .andExpect(content().string(containsString("name=\"version.duracionFermentacion\"")))
                .andExpect(content().string(containsString("name=\"version.duracionMaduracion\"")));
    }

    // ==================== GET /recetas/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-23: el formulario de modificación carga los datos de la versión vigente y sus filas con el nombre de cada insumo")
    void formularioEdicion_debeCargarLosDatosYLasFilas() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        stubInsumos();

        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("Modificar receta")))
                .andExpect(content().string(containsString("action=\"/recetas/7\"")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(containsString("Al guardar se registra una nueva versi")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.nombre\"[^>]*value=\"Pale Ale\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.volumenBase\"[^>]*value=\"20\\.0\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.duracionHervido\"[^>]*value=\"90\".*")))
                // Malta: campos ocultos indexados y fila visible con el nombre del insumo
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesMalta\\[0\\]\\.idMalta\"[^>]*value=\"1\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesMalta\\[0\\]\\.cantidad\"[^>]*value=\"4\\.5\".*")))
                .andExpect(content().string(matchesPattern("(?s).*data-celda=\"nombre\">Pilsen<.*")))
                // Cada cantidad de la fila se muestra con su unidad
                .andExpect(content().string(matchesPattern("(?s).*data-celda=\"cantidad\">4,5 kg<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-celda=\"cantidad\">11,5 g<.*")))
                // Levadura
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLevadura\\[0\\]\\.idLevadura\"[^>]*value=\"1\".*")))
                .andExpect(content().string(matchesPattern("(?s).*data-celda=\"nombre\">US-05<.*")))
                // La tabla de cada seccion ya no muestra el texto de tabla vacia visible
                .andExpect(content().string(matchesPattern("(?s).*data-fila-vacia[^>]*hidden.*")))
                .andExpect(content().string(not(containsString("action=\"/recetas\""))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-24: los lúpulos cargados muestran uso, etapa y tiempo; el tiempo de hervor solo viaja en el uso Hervor")
    void formularioEdicion_debeCargarLosLupulosConSuUso() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        stubInsumos();

        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                // Fila 0: Mosaic, Whirlpool, sin tiempo de hervor
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[0\\]\\.idLupulo\"[^>]*value=\"3\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[0\\]\\.uso\"[^>]*value=\"WHIRLPOOL\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[0\\]\\.etapaDeUso\"[^>]*value=\"HERVIDO\".*")))
                // Solo la plantilla y la fila de Cascade llevan campo de tiempo de hervor (no Mosaic ni Citra)
                .andExpect(result -> assertThat(result.getResponse().getContentAsString().split("data-campo=\"tiempoDeHervor\"", -1).length - 1).isEqualTo(2))
                // Fila 1: Cascade, Hervor con 60 minutos
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[1\\]\\.uso\"[^>]*value=\"HERVOR\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[1\\]\\.tiempoDeHervor\"[^>]*value=\"60\\.0\".*")))
                // Fila 2: Citra, Dry hop en fermentacion, sin tiempo de hervor
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[2\\]\\.uso\"[^>]*value=\"DRY_HOP\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.detallesLupulo\\[2\\]\\.etapaDeUso\"[^>]*value=\"FERMENTACION\".*")))
                .andExpect(content().string(not(containsString("version.detallesLupulo[2].tiempoDeHervor"))))
                .andExpect(content().string(matchesPattern("(?s).*data-celda=\"nombre\">Mosaic<.*data-celda=\"nombre\">Cascade<.*data-celda=\"nombre\">Citra<.*")))
                .andExpect(content().string(matchesPattern("(?s).*Whirlpool.*Hervor.*Dry hop.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-25: el plan de monitoreo existente se muestra en un bloque por plan, con cada valor y su unidad, y se conserva con campos ocultos indexados de dos niveles")
    void formularioEdicion_debeMostrarYConservarElPlanDeMonitoreo() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        stubInsumos();

        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Control de densidad")))
                .andExpect(matchesPatternLocal("etapa a controlar: <span data-celda=\"etapa\">Fermentaci"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Densidad<"))
                // Cada valor se muestra con la unidad del parametro
                .andExpect(matchesPatternLocal("data-celda=\"valorMinimo\">1,01 SG<"))
                .andExpect(matchesPatternLocal("data-celda=\"valorIdeal\">1,012 SG<"))
                .andExpect(matchesPatternLocal("data-celda=\"valorMaximo\">1,015 SG<"))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[0\\]\\.idEtapaControl\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[0\\]\\.detallesParametroControl\\[0\\]\\.idParametroControl\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("detallesParametroControl\\[0\\]\\.valorMinimo\"[^>]*value=\"1\\.01\""))
                .andExpect(matchesPatternLocal("detallesParametroControl\\[0\\]\\.valorIdeal\"[^>]*value=\"1\\.012\""))
                .andExpect(matchesPatternLocal("detallesParametroControl\\[0\\]\\.valorMaximo\"[^>]*value=\"1\\.015\""))
                .andExpect(content().string(containsString("data-quitar-plan")))
                .andExpect(content().string(containsString("Quitar plan")))
                .andExpect(matchesPatternLocal("data-planes-vacio[^>]*hidden"))
                .andExpect(content().string(not(matchesPattern("(?s).*<tr[^>]*data-parametro-plan[^>]*bg-rose-50.*"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-26: si una malta, lúpulo o levadura de la receta ya no existe, su fila lo indica en lugar de fallar")
    void formularioEdicion_debeIndicarInsumosQueYaNoExisten() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        when(maltaServicio.buscarPorId(1L)).thenThrow(new RecursoNoEncontradoException("No se encontró la malta con ID: 1"));

        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Malta no disponible")))
                .andExpect(content().string(containsString("no disponible")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-27: modificar una receta inexistente vuelve al origen con la alerta de error del service")
    void formularioEdicion_debeMostrarElErrorSiLaRecetaNoExiste() throws Exception {
        when(recetaServicio.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"));

        mockMvc.perform(get("/recetas/99/editar").header("Referer", "/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "No se encontró la receta con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /recetas ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-28: un alta válida enlaza los datos y las filas indexadas al DTO, delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(recetaValida(post("/recetas")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "Receta creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<RecetaFormDTO> captor = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).altaReceta(captor.capture());
        var version = captor.getValue().getVersion();
        assertThat(version.getNombre()).isEqualTo("Pale Ale");
        assertThat(version.getVolumenBase()).isEqualTo(20.0);
        assertThat(version.getRelacionDeEmpaste()).isEqualTo(3.0);
        assertThat(version.getOgObjetivo()).isEqualTo(1.05);
        assertThat(version.getFgObjetivo()).isEqualTo(1.01);
        assertThat(version.getIbuObjetivo()).isEqualTo(35);
        assertThat(version.getDuracionMaceracion()).isEqualTo(60);
        assertThat(version.getDuracionHervido()).isEqualTo(90);
        assertThat(version.getDuracionFermentacion()).isEqualTo(10);
        assertThat(version.getDuracionMaduracion()).isEqualTo(7);
        assertThat(version.getDetallesMalta()).hasSize(1);
        assertThat(version.getDetallesMalta().get(0).getIdMalta()).isEqualTo(1L);
        assertThat(version.getDetallesMalta().get(0).getCantidad()).isEqualTo(4.5);
        assertThat(version.getDetallesLupulo()).hasSize(2);
        assertThat(version.getDetallesLupulo().get(0).getUso()).isEqualTo(UsoLupulo.HERVOR);
        assertThat(version.getDetallesLupulo().get(0).getEtapaDeUso()).isEqualTo(TipoEtapa.HERVIDO);
        assertThat(version.getDetallesLupulo().get(0).getTiempoDeHervor()).isEqualTo(60.0);
        assertThat(version.getDetallesLupulo().get(1).getUso()).isEqualTo(UsoLupulo.DRY_HOP);
        assertThat(version.getDetallesLupulo().get(1).getEtapaDeUso()).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(version.getDetallesLupulo().get(1).getTiempoDeHervor()).isNull();
        assertThat(version.getDetallesLevadura()).hasSize(1);
        assertThat(version.getDetallesLevadura().get(0).getIdLevadura()).isEqualTo(1L);
        assertThat(version.getPlanesMonitoreo()).hasSize(1);
        assertThat(version.getPlanesMonitoreo().get(0).getIdEtapaControl()).isEqualTo(1L);
        assertThat(version.getPlanesMonitoreo().get(0).getDetallesParametroControl().get(0).getValorIdeal()).isEqualTo(1.012);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-29: un alta sin plan de monitoreo es válida: el plan es opcional")
    void alta_debeAceptarUnaRecetaSinPlanDeMonitoreo() throws Exception {
        mockMvc.perform(post("/recetas").with(csrf()).params(parametrosBasicos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"));

        ArgumentCaptor<RecetaFormDTO> captor = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).altaReceta(captor.capture());
        assertThat(captor.getValue().getVersion().getPlanesMonitoreo()).isEmpty();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-30: sin ningún dato el alta no llega al service y muestra el resumen de errores y el mensaje junto a cada campo y cada tabla")
    void alta_debeMostrarLosErroresDeBeanValidation() throws Exception {
        mockMvc.perform(post("/recetas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo crear la receta")))
                .andExpect(content().string(containsString("role=\"alert\"")))
                .andExpect(content().string(containsString("El nombre de la receta es obligatorio")))
                .andExpect(content().string(containsString("El volumen base es obligatorio")))
                .andExpect(content().string(containsString("La OG objetivo es obligatoria")))
                .andExpect(content().string(containsString("El IBU objetivo es obligatorio")))
                .andExpect(content().string(containsString("La receta debe tener al menos una malta")))
                .andExpect(content().string(containsString("La receta debe tener al menos un l")))
                .andExpect(content().string(containsString("La receta debe tener al menos una levadura")))
                .andExpect(content().string(containsString("border-rose-400")))
                // El mensaje de la tabla aparece debajo de ella, ademas de en el resumen
                .andExpect(content().string(matchesPattern("(?s).*Todav.a no agregaste maltas.*text-rose-600\">La receta debe tener al menos una malta<.*")));

        verify(recetaServicio, never()).altaReceta(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-31: valores no positivos en datos generales, duraciones y cantidades se rechazan con un mensaje claro")
    void alta_debeRechazarValoresNoPositivos() throws Exception {
        mockMvc.perform(recetaValida(post("/recetas"),
                        "version.volumenBase", "0",
                        "version.ibuObjetivo", "-1",
                        "version.duracionHervido", "0",
                        "version.detallesMalta[0].cantidad", "0",
                        "version.detallesLevadura[0].cantidad", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("El volumen base debe ser mayor a 0")))
                .andExpect(content().string(containsString("El IBU objetivo no puede ser negativo")))
                .andExpect(content().string(containsString("La duraci")))
                .andExpect(content().string(containsString("La cantidad de cada malta debe ser mayor a 0")))
                .andExpect(content().string(containsString("La cantidad de cada levadura debe ser mayor a 0")));

        verify(recetaServicio, never()).altaReceta(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-32: un nombre repetido se informa en el campo nombre (y en el resumen), sin redirigir")
    void alta_debeMostrarElNombreDuplicadoEnElCampo() throws Exception {
        when(recetaServicio.altaReceta(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una receta con el nombre 'Pale Ale'"));

        mockMvc.perform(recetaValida(post("/recetas")))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo crear la receta")))
                // Debajo del input del nombre, antes del siguiente campo
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.nombre\".*?text-rose-600\">Ya existe una receta con el nombre .*Pale Ale.*?<.*name=\"version\\.volumenBase\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version\\.nombre\"[^>]*value=\"Pale Ale\".*")))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-33: una regla de negocio del service se informa como error global en el resumen, sin redirigir")
    void alta_debeMostrarLaReglaDeNegocioComoErrorGlobal() throws Exception {
        when(recetaServicio.altaReceta(any()))
                .thenThrow(new ReglaNegocioException("La receta debe tener al menos un lupulo con uso HERVOR"));

        mockMvc.perform(recetaValida(post("/recetas")))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo crear la receta")))
                .andExpect(content().string(containsString("La receta debe tener al menos un lupulo con uso HERVOR")))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-34: si un insumo elegido ya no existe, el alta vuelve al formulario con el motivo y no al origen")
    void alta_debeMostrarElInsumoInexistenteEnElFormulario() throws Exception {
        when(recetaServicio.altaReceta(any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontro la malta con ID: 1"));

        mockMvc.perform(recetaValida(post("/recetas")))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se encontro la malta con ID: 1")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-35: ante un error se vuelve a mostrar todo lo cargado: datos, filas de las tres tablas con el nombre de cada insumo y el plan de monitoreo")
    void alta_conErrorDebeVolverAMostrarTodoLoCargado() throws Exception {
        stubInsumos();
        when(recetaServicio.altaReceta(any()))
                .thenThrow(new ReglaNegocioException("La receta debe tener al menos un lupulo con uso HERVOR"));

        mockMvc.perform(recetaValida(post("/recetas")))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("name=\"version\\.nombre\"[^>]*value=\"Pale Ale\""))
                .andExpect(matchesPatternLocal("name=\"version\\.ogObjetivo\"[^>]*value=\"1050\""))
                .andExpect(matchesPatternLocal("name=\"version\\.duracionMaduracion\"[^>]*value=\"7\""))
                // Filas ya cargadas, con el nombre que resuelve el servidor
                .andExpect(matchesPatternLocal("name=\"version\\.detallesMalta\\[0\\]\\.idMalta\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Pilsen<"))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesLupulo\\[0\\]\\.tiempoDeHervor\"[^>]*value=\"60\\.0\""))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Cascade<"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Citra<"))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesLevadura\\[0\\]\\.idLevadura\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">US-05<"))
                // El plan de monitoreo enviado se conserva
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[0\\]\\.idEtapaControl\"[^>]*value=\"1\""))
                .andExpect(content().string(containsString("Control de densidad")))
                .andExpect(matchesPatternLocal("data-fila-vacia[^>]*hidden"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-RCC-36: solo el GERENTE_DE_PRODUCCION accede al alta: otro rol es rechazado en el formulario y en el POST, y no llega al service")
    void alta_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(recetaValida(post("/recetas")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(recetaServicio, never()).altaReceta(any());
    }

    // ==================== POST /recetas/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-37: una modificación válida delega en el service con el id de la URL y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(recetaValida(post("/recetas/7")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "Receta modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<RecetaFormDTO> captor = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).modificarReceta(eq(7L), captor.capture());
        assertThat(captor.getValue().getVersion().getNombre()).isEqualTo("Pale Ale");
        // El plan de monitoreo que viajaba oculto en el formulario llega intacto al service
        assertThat(captor.getValue().getVersion().getPlanesMonitoreo()).hasSize(1);
        assertThat(captor.getValue().getVersion().getPlanesMonitoreo().get(0).getDetallesParametroControl().get(0).getValorMinimo()).isEqualTo(1.01);
        verify(recetaServicio, never()).altaReceta(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-38: errores de validación, nombre repetido y regla de negocio al modificar vuelven al formulario de modificación con lo cargado")
    void modificar_debeVolverAlFormularioConLosErrores() throws Exception {
        stubInsumos();

        // Bean Validation
        mockMvc.perform(recetaValida(post("/recetas/7"), "version.nombre", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo modificar la receta")))
                .andExpect(content().string(containsString("El nombre de la receta es obligatorio")))
                .andExpect(content().string(containsString("action=\"/recetas/7\"")))
                .andExpect(content().string(containsString("Modificar receta")))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Pilsen<"));
        verify(recetaServicio, never()).modificarReceta(any(), any());

        // Nombre repetido
        when(recetaServicio.modificarReceta(eq(7L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una receta con el nombre 'Pale Ale'"));
        mockMvc.perform(recetaValida(post("/recetas/7")))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("name=\"version\\.nombre\".*?text-rose-600\">Ya existe una receta con el nombre .*Pale Ale.*?<.*name=\"version\\.volumenBase\""));

        // Regla de negocio
        when(recetaServicio.modificarReceta(eq(7L), any()))
                .thenThrow(new ReglaNegocioException("La receta no puede tener la misma malta mas de una vez"));
        mockMvc.perform(recetaValida(post("/recetas/7")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La receta no puede tener la misma malta mas de una vez")))
                .andExpect(content().string(containsString("action=\"/recetas/7\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-39: modificar una receta inexistente vuelve al origen con la alerta de error; si lo que falta es un insumo, el motivo se muestra en el formulario")
    void modificar_debeDistinguirRecetaInexistenteDeInsumoInexistente() throws Exception {
        // Falta la propia receta: el ControllerAdvices redirige al origen
        when(recetaServicio.modificarReceta(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontro la receta con ID: 99"));
        when(recetaServicio.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontro la receta con ID: 99"));

        mockMvc.perform(recetaValida(post("/recetas/99")).header("Referer", "/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"))
                .andExpect(flash().attribute("mensaje", "No se encontro la receta con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));

        // La receta existe y falta un insumo: se informa en el formulario
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        when(recetaServicio.modificarReceta(eq(7L), any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontro la malta con ID: 1"));

        mockMvc.perform(recetaValida(post("/recetas/7")))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se encontro la malta con ID: 1")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-RCC-40: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de modificación ni modificar, y no llega al service")
    void modificar_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(recetaValida(post("/recetas/7")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(recetaServicio, never()).buscarPorId(any());
        verify(recetaServicio, never()).modificarReceta(any(), any());
    }

    // ==================== GET /recetas/buscador/{maltas|lupulos|levaduras} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-41: el buscador de maltas devuelve el fragmento con los criterios (nombre y tipo), los resultados con \"Elegir\" y páginas de 8 en orden alfabético")
    void buscadorMaltas_debeDevolverElFragmentoConResultados() throws Exception {
        when(maltaServicio.filtrarMaltas(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        MaltaResponseDTO.builder().id(1L).nombre("Caramunich").tipo(TipoMalta.CARAMELO).build(),
                        MaltaResponseDTO.builder().id(2L).nombre("Pilsen").tipo(TipoMalta.BASE).build()),
                        PageRequest.of(0, 8), 2));

        mockMvc.perform(get("/recetas/buscador/maltas"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/buscador-malta :: buscador"))
                .andExpect(content().string(containsString("Buscar malta")))
                .andExpect(content().string(containsString("action=\"/recetas/buscador/maltas\"")))
                .andExpect(content().string(containsString("data-modal-buscar")))
                .andExpect(content().string(containsString("name=\"nombre\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pilsen\"")))
                .andExpect(content().string(containsString("name=\"tipo\"")))
                .andExpect(content().string(containsString("<option value=\"CARAMELO\"")))
                .andExpect(content().string(matchesPattern("(?s).*Caramunich.*>\\s*Caramelo\\s*<.*Pilsen.*>\\s*Base\\s*<.*")))
                .andExpect(content().string(containsString("data-id=\"1\"")))
                .andExpect(content().string(containsString("data-texto=\"Caramunich\"")))
                .andExpect(content().string(containsString("data-id=\"2\"")))
                .andExpect(content().string(containsString("Elegir")))
                // Es un fragmento: no incluye el layout
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("Gestión de Insumos"))));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(maltaServicio).filtrarMaltas(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().getOrderFor("nombre").getDirection().isAscending()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-42: el buscador de lúpulos filtra por nombre y formato y muestra formato y alfa; el de levaduras filtra por nombre y tipo")
    void buscadoresDeLupulosYLevaduras_debenFiltrarYMostrarResultados() throws Exception {
        when(lupuloServicio.filtrarLupulos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        LupuloResponseDTO.builder().id(5L).nombre("Cascade").formato(FormatoLupulo.PELLET).aa(5.5).build()),
                        PageRequest.of(1, 8), 9));
        when(levaduraServicio.filtrarLevaduras(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        LevaduraResponseDTO.builder().id(6L).nombre("W-34/70").tipo(TipoLevadura.LAGER).build()),
                        PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/recetas/buscador/lupulos").param("nombre", "casc").param("formato", "PELLET").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/buscador-lupulo :: buscador"))
                .andExpect(content().string(containsString("Buscar l")))
                .andExpect(content().string(containsString("name=\"formato\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Cascade\"")))
                .andExpect(content().string(matchesPattern("(?s).*Cascade.*>\\s*Pellet\\s*<.*>\\s*5,5\\s*<.*")))
                .andExpect(content().string(containsString("data-id=\"5\"")))
                .andExpect(content().string(containsString("data-texto=\"Cascade\"")))
                // La paginacion dentro del modal conserva los criterios
                .andExpect(content().string(containsString("data-modal-get")))
                .andExpect(content().string(containsString("/recetas/buscador/lupulos?pagina=0")))
                .andExpect(content().string(containsString("formato=PELLET")))
                .andExpect(content().string(containsString("nombre=casc")));
        ArgumentCaptor<Pageable> captorLupulos = ArgumentCaptor.forClass(Pageable.class);
        verify(lupuloServicio).filtrarLupulos(eq("casc"), eq(FormatoLupulo.PELLET), captorLupulos.capture());
        assertThat(captorLupulos.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captorLupulos.getValue().getPageSize()).isEqualTo(8);
        assertThat(captorLupulos.getValue().getSort().getOrderFor("nombre").isAscending()).isTrue();

        mockMvc.perform(get("/recetas/buscador/levaduras").param("nombre", "w").param("tipo", "LAGER"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/buscador-levadura :: buscador"))
                .andExpect(content().string(containsString("Buscar levadura")))
                .andExpect(content().string(containsString("name=\"tipo\"")))
                .andExpect(content().string(matchesPattern("(?s).*W-34/70.*>\\s*Lager\\s*<.*")))
                .andExpect(content().string(containsString("data-id=\"6\"")));
        ArgumentCaptor<Pageable> captorLevaduras = ArgumentCaptor.forClass(Pageable.class);
        verify(levaduraServicio).filtrarLevaduras(eq("w"), eq(TipoLevadura.LAGER), captorLevaduras.capture());
        assertThat(captorLevaduras.getValue().getSort().getOrderFor("nombre").isAscending()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-43: un buscador sin resultados muestra un mensaje claro y un criterio en blanco equivale a no filtrar")
    void buscadores_debenMostrarMensajeSinResultados() throws Exception {
        when(maltaServicio.filtrarMaltas(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));
        when(lupuloServicio.filtrarLupulos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));
        when(levaduraServicio.filtrarLevaduras(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        mockMvc.perform(get("/recetas/buscador/maltas").param("nombre", "   ").param("tipo", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron maltas con los criterios indicados.")));
        verify(maltaServicio).filtrarMaltas(isNull(), isNull(), any());

        mockMvc.perform(get("/recetas/buscador/lupulos"))
                .andExpect(content().string(containsString("No se encontraron l")))
                .andExpect(content().string(containsString("con los criterios indicados.")));
        mockMvc.perform(get("/recetas/buscador/levaduras"))
                .andExpect(content().string(containsString("No se encontraron levaduras con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-RCC-44: solo el GERENTE_DE_PRODUCCION usa los buscadores de insumos: otro rol es rechazado y no llega a los services")
    void buscadores_debenRechazarRolNoAutorizado() throws Exception {
        for (String url : List.of("/recetas/buscador/maltas", "/recetas/buscador/lupulos", "/recetas/buscador/levaduras")) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }

        verify(maltaServicio, never()).filtrarMaltas(any(), any(), any());
        verify(lupuloServicio, never()).filtrarLupulos(any(), any(), any());
        verify(levaduraServicio, never()).filtrarLevaduras(any(), any(), any());
    }

    // ==================== OG y FG con cuatro dígitos ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-45: la OG y la FG ingresadas con cuatro dígitos (1050 y 1010) llegan al service como 1.05 y 1.01, en el alta y en la modificación")
    void alta_ymodificacion_debenConvertirLosCuatroDigitosEnDensidades() throws Exception {
        mockMvc.perform(recetaValida(post("/recetas"), "version.ogObjetivo", "1050", "version.fgObjetivo", "1010"))
                .andExpect(redirectedUrl("/recetas"));
        mockMvc.perform(recetaValida(post("/recetas/7"), "version.ogObjetivo", "1085", "version.fgObjetivo", "1000"))
                .andExpect(redirectedUrl("/recetas"));

        ArgumentCaptor<RecetaFormDTO> alta = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).altaReceta(alta.capture());
        assertThat(alta.getValue().getVersion().getOgObjetivo()).isEqualTo(1.05);
        assertThat(alta.getValue().getVersion().getFgObjetivo()).isEqualTo(1.01);

        ArgumentCaptor<RecetaFormDTO> modificacion = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).modificarReceta(eq(7L), modificacion.capture());
        assertThat(modificacion.getValue().getVersion().getOgObjetivo()).isEqualTo(1.085);
        assertThat(modificacion.getValue().getVersion().getFgObjetivo()).isEqualTo(1.0);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-46: una OG o FG que no tiene exactamente cuatro dígitos (\"105\", \"10500\", \"1,050\", \"abcd\") vuelve al formulario con el error del campo y sin llamar al service")
    void alta_ymodificacion_debenRechazarDensidadesSinCuatroDigitos() throws Exception {
        for (String invalido : List.of("105", "10500", "1,050", "abcd")) {
            mockMvc.perform(recetaValida(post("/recetas"), "version.ogObjetivo", invalido))
                    .andExpect(status().isOk())
                    .andExpect(view().name("receta/receta-form"))
                    .andExpect(content().string(containsString("No se pudo crear la receta")))
                    .andExpect(content().string(containsString("La OG debe tener exactamente cuatro dígitos, por ejemplo 1050 (equivale a 1,050)")))
                    // Se vuelve a mostrar lo que el usuario escribió y el campo queda marcado
                    .andExpect(matchesPatternLocal("name=\"version\\.ogObjetivo\"[^>]*value=\"" + java.util.regex.Pattern.quote(invalido) + "\""));
        }
        mockMvc.perform(recetaValida(post("/recetas"), "version.fgObjetivo", "10500"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La FG debe tener exactamente cuatro dígitos, por ejemplo 1010 (equivale a 1,010)")));
        mockMvc.perform(recetaValida(post("/recetas/7"), "version.fgObjetivo", "abcd"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo modificar la receta")))
                .andExpect(content().string(containsString("La FG debe tener exactamente cuatro dígitos, por ejemplo 1010 (equivale a 1,010)")));

        verify(recetaServicio, never()).altaReceta(any());
        verify(recetaServicio, never()).modificarReceta(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-47: los campos de OG y FG son de texto numérico de cuatro dígitos con su ayuda; el formulario de modificación los muestra como 1050 y 1010, y el detalle y el listado siguen mostrando 1,050 y 1,010")
    void formularios_debenMostrarLasDensidadesConCuatroDigitos() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("type=\"text\"[^>]*id=\"ogObjetivo\"[^>]*inputmode=\"numeric\"[^>]*pattern=\"\\[0-9\\]\\{4\\}\"[^>]*maxlength=\"4\""))
                .andExpect(matchesPatternLocal("type=\"text\"[^>]*id=\"fgObjetivo\"[^>]*inputmode=\"numeric\"[^>]*pattern=\"\\[0-9\\]\\{4\\}\"[^>]*maxlength=\"4\""))
                .andExpect(content().string(containsString("4 dígitos: 1050 equivale a 1,050 (entre 1020 y 1120)")))
                .andExpect(content().string(containsString("4 dígitos: 1010 equivale a 1,010 (entre 1000 y 1040)")));

        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        stubInsumos();
        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("name=\"version\\.ogObjetivo\"[^>]*value=\"1050\""))
                .andExpect(matchesPatternLocal("name=\"version\\.fgObjetivo\"[^>]*value=\"1010\""));

        mockMvc.perform(get("/recetas/7"))
                .andExpect(content().string(containsString(">1,050<")))
                .andExpect(content().string(containsString(">1,010<")));
    }

    // ==================== Validación solo en el servidor ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-48: el formulario no valida en el navegador (novalidate) y avisa que las cantidades de maltas, levaduras y lúpulos en Hervor son proporciones")
    void formulario_debeValidarSoloEnElServidorYAvisarDeLasProporciones() throws Exception {
        String html = mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("<form id=\"receta-form\"[^>]*novalidate"))
                // Aviso de proporciones bajo las tres tablas
                .andExpect(content().string(containsString("Las cantidades se usan como proporciones entre insumos; el sistema calcula las cantidades reales seg")))
                .andExpect(content().string(containsString("la OG y el volumen del lote.")))
                .andExpect(content().string(containsString("con uso Hervor se usan como proporci")))
                .andExpect(content().string(containsString("Whirlpool y Dry hop se escalan seg")))
                .andExpect(content().string(containsString("el volumen base de la receta.")))
                // Las ventanas emergentes solo avisan que falta elegir el insumo
                .andExpect(content().string(containsString("data-error=\"insumo\"")))
                .andExpect(content().string(not(containsString("data-error=\"cantidad\""))))
                .andExpect(content().string(not(containsString("data-error=\"duplicado\""))))
                .andReturn().getResponse().getContentAsString();
        // El aviso de maltas y levaduras (mismo texto) aparece una vez por tabla
        assertThat(html.split("Las cantidades se usan como proporciones entre insumos", -1)).hasSize(3);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-49: una cantidad en 0, vacía, negativa o no numérica en una fila se informa debajo de la tabla y en el resumen, resalta la fila y conserva todas las filas, en el alta y en la modificación")
    void alta_ymodificacion_debenInformarCantidadesInvalidasPorFila() throws Exception {
        stubInsumos();

        // Alta: cantidad 0 en la malta, vacía en el lúpulo en Hervor y negativa en la levadura
        mockMvc.perform(recetaValida(post("/recetas"),
                        "version.detallesMalta[0].cantidad", "0",
                        "version.detallesLupulo[0].cantidad", "",
                        "version.detallesLevadura[0].cantidad", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo crear la receta")))
                .andExpect(content().string(containsString("La cantidad de cada malta debe ser mayor a 0")))
                .andExpect(content().string(containsString("La cantidad de cada levadura debe ser mayor a 0")))
                .andExpect(matchesPatternLocal("text-rose-600\">La cantidad de cada l.pulo es obligatoria<"))
                // Filas resaltadas y todas las filas conservadas
                .andExpect(matchesPatternLocal("<tr[^>]*data-prefijo=\"version\\.detallesMalta\"[^>]*bg-rose-50"))
                .andExpect(matchesPatternLocal("<tr[^>]*data-prefijo=\"version\\.detallesLupulo\"[^>]*bg-rose-50"))
                .andExpect(matchesPatternLocal("<tr[^>]*data-prefijo=\"version\\.detallesLevadura\"[^>]*bg-rose-50"))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesMalta\\[0\\]\\.idMalta\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesLupulo\\[1\\]\\.idLupulo\"[^>]*value=\"2\""))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesLevadura\\[0\\]\\.idLevadura\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Cascade<"));

        // El mismo mensaje de dos filas se muestra una sola vez debajo de la tabla
        String html = mockMvc.perform(recetaValida(post("/recetas"),
                        "version.detallesLupulo[0].cantidad", "0",
                        "version.detallesLupulo[1].cantidad", "-5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html.split("text-rose-600\">La cantidad de cada l.pulo debe ser mayor a 0<", -1)).hasSize(2);

        // Texto no numérico: error de conversión claro y la fila se conserva
        mockMvc.perform(recetaValida(post("/recetas"), "version.detallesMalta[0].cantidad", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("un número válido")))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesMalta\\[0\\]\\.idMalta\"[^>]*value=\"1\""));

        // Modificación: mismo comportamiento dentro del formulario de modificación
        mockMvc.perform(recetaValida(post("/recetas/7"), "version.detallesMalta[0].cantidad", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo modificar la receta")))
                .andExpect(content().string(containsString("La cantidad de cada malta es obligatoria")))
                .andExpect(content().string(containsString("action=\"/recetas/7\"")))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Pilsen<"));

        verify(recetaServicio, never()).altaReceta(any());
        verify(recetaServicio, never()).modificarReceta(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-50: un lúpulo sin uso, o con tiempo de hervor negativo, se rechaza con su mensaje; un lúpulo en Hervor sin tiempo de hervor lo rechaza el service y el formulario conserva las filas")
    void alta_debeInformarErroresDelUsoYDelTiempoDeHervor() throws Exception {
        stubInsumos();

        mockMvc.perform(recetaValida(post("/recetas"), "version.detallesLupulo[1].uso", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("es obligatorio")))
                .andExpect(matchesPatternLocal("El uso de cada l.pulo es obligatorio"))
                .andExpect(matchesPatternLocal("<tr[^>]*data-prefijo=\"version\\.detallesLupulo\"[^>]*bg-rose-50"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Citra<"));

        mockMvc.perform(recetaValida(post("/recetas"), "version.detallesLupulo[0].tiempoDeHervor", "-10"))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("El tiempo de hervor de cada l.pulo debe ser mayor a 0"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Cascade<"));
        verify(recetaServicio, never()).altaReceta(any());

        // Hervor sin tiempo: Bean Validation lo deja pasar y lo rechaza la regla cruzada del service
        when(recetaServicio.altaReceta(any())).thenThrow(new ReglaNegocioException(
                "El tiempo de hervor de cada lúpulo con uso HERVOR debe ser mayor a 0 y menor a la duración estimada del hervido"));
        mockMvc.perform(recetaValida(post("/recetas"), "version.detallesLupulo[0].tiempoDeHervor", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo crear la receta")))
                .andExpect(matchesPatternLocal("El tiempo de hervor de cada l.pulo con uso HERVOR debe ser mayor a 0 y menor a la duraci"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Cascade<"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Citra<"));

        ArgumentCaptor<RecetaFormDTO> captor = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).altaReceta(captor.capture());
        assertThat(captor.getValue().getVersion().getDetallesLupulo().get(0).getTiempoDeHervor()).isNull();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-51: un dry hop sin etapa de uso y una malta repetida los rechaza el service; el formulario vuelve con el motivo y con todas las filas")
    void alta_ymodificacion_debenInformarReglasDeFilasDelService() throws Exception {
        stubInsumos();

        // Dry hop sin etapa: la etapa llega vacía y el service informa la regla
        when(recetaServicio.altaReceta(any())).thenThrow(new ReglaNegocioException(
                "La etapa de uso de un lúpulo con uso DRY_HOP debe ser FERMENTACION o MADURACION"));
        mockMvc.perform(recetaValida(post("/recetas"), "version.detallesLupulo[1].etapaDeUso", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("DRY_HOP debe ser FERMENTACION o MADURACION")))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Citra<"));
        ArgumentCaptor<RecetaFormDTO> alta = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).altaReceta(alta.capture());
        assertThat(alta.getValue().getVersion().getDetallesLupulo().get(1).getEtapaDeUso()).isNull();

        // Malta repetida: dos filas con la misma malta, ambas se conservan
        when(recetaServicio.modificarReceta(eq(7L), any())).thenThrow(new ReglaNegocioException(
                "La receta no puede tener la misma malta más de una vez"));
        String html = mockMvc.perform(recetaValida(post("/recetas/7"),
                        "version.detallesMalta[1].idMalta", "1",
                        "version.detallesMalta[1].cantidad", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo modificar la receta")))
                .andExpect(matchesPatternLocal("La receta no puede tener la misma malta m.s de una vez"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("name=\"version.detallesMalta[0].idMalta\"", "name=\"version.detallesMalta[1].idMalta\"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-52: un POST armado a mano con una fila sin insumo o con índices con huecos no produce un error 500: vuelve al formulario con el mensaje y sin llamar al service")
    void alta_ymodificacion_debenResistirRequestsMalformados() throws Exception {
        stubInsumos();

        // Fila de malta sin idMalta (solo con cantidad)
        LinkedMultiValueMap<String, String> sinMalta = parametrosBasicos();
        sinMalta.remove("version.detallesMalta[0].idMalta");
        mockMvc.perform(post("/recetas").with(csrf()).params(sinMalta))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("Cada malta de la receta debe estar seleccionada")))
                .andExpect(matchesPatternLocal("<tr[^>]*data-prefijo=\"version\\.detallesMalta\"[^>]*bg-rose-50"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Cascade<"));

        // Fila de lúpulo sin idLupulo ni uso, y fila de levadura vacía
        LinkedMultiValueMap<String, String> filasVacias = parametrosBasicos();
        filasVacias.remove("version.detallesLupulo[1].idLupulo");
        filasVacias.remove("version.detallesLupulo[1].uso");
        filasVacias.remove("version.detallesLevadura[0].idLevadura");
        filasVacias.set("version.detallesLevadura[0].cantidad", "");
        mockMvc.perform(post("/recetas/7").with(csrf()).params(filasVacias))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("Cada l.pulo de la receta debe estar seleccionado"))
                .andExpect(content().string(containsString("Cada levadura de la receta debe estar seleccionada")))
                .andExpect(content().string(containsString("La cantidad de cada levadura es obligatoria")));

        // Índices con huecos: solo existe detallesMalta[2]; Spring completa las filas [0] y [1] vacías y se informan
        LinkedMultiValueMap<String, String> conHuecos = parametrosBasicos();
        conHuecos.remove("version.detallesMalta[0].idMalta");
        conHuecos.remove("version.detallesMalta[0].cantidad");
        conHuecos.add("version.detallesMalta[2].idMalta", "1");
        conHuecos.add("version.detallesMalta[2].cantidad", "3");
        mockMvc.perform(post("/recetas").with(csrf()).params(conHuecos))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("Cada malta de la receta debe estar seleccionada")))
                .andExpect(matchesPatternLocal("name=\"version\\.detallesMalta\\[2\\]\\.idMalta\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Pilsen<"));

        verify(recetaServicio, never()).altaReceta(any());
        verify(recetaServicio, never()).modificarReceta(any(), any());
    }

    // ==================== Plan de monitoreo: formulario y buscadores ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-53: la sección Plan de monitoreo explica qué es un plan, ofrece \"Agregar plan de monitoreo\", trae las plantillas de plan y de parámetro y no ofrece crear etapas ni parámetros")
    void formulario_debeMostrarLaSeccionDelPlanDeMonitoreo() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plan de monitoreo (opcional)")))
                .andExpect(content().string(containsString("Los planes definen los par")))
                .andExpect(content().string(containsString("con sus valores m")))
                .andExpect(content().string(containsString("puede tener varias etapas de control, cada una con su propio plan")))
                .andExpect(content().string(containsString("quitalo y agregalo de nuevo")))
                .andExpect(content().string(containsString("Agregar plan de monitoreo")))
                .andExpect(content().string(containsString("data-abrir-plan")))
                .andExpect(content().string(containsString("La receta no tiene plan de monitoreo.")))
                .andExpect(content().string(containsString("<template data-plantilla-plan>")))
                .andExpect(content().string(containsString("<template data-plantilla-parametro-plan>")))
                .andExpect(content().string(containsString("Quitar plan")))
                .andExpect(matchesPatternLocal("<form id=\"receta-form\"[^>]*novalidate"))
                .andExpect(content().string(not(containsString("data-selector-alta"))))
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(not(containsString("Crear nueva etapa"))))
                .andExpect(content().string(not(containsString("Crear nuevo par"))))
                // Sin planes no hay bloques en la receta: el texto de vacio esta visible
                .andExpect(matchesPatternLocal("data-planes-vacio(?![^>]*hidden)"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-54: la ventana de agregar plan tiene el selector de etapa de control, la tabla de parámetros y el mini formulario con unidades y placeholders, fuera del formulario de la receta")
    void formulario_debeIncluirLaVentanaDeAgregarPlan() throws Exception {
        mockMvc.perform(get("/recetas/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dialogo-agregar-plan\"")))
                .andExpect(content().string(containsString("Agregar plan de monitoreo</h3>")))
                // Selectores de entidad con icono de lista (nunca un combo) y su buscador
                .andExpect(content().string(containsString("data-buscador-url=\"/recetas/buscador/etapas-control\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/recetas/buscador/parametros-control\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar etapa de control\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar par")))
                .andExpect(content().string(containsString("bi bi-list-ul")))
                .andExpect(content().string(containsString("data-error=\"etapa\"")))
                .andExpect(content().string(containsString("data-error=\"parametro\"")))
                // Mini formulario: la unidad del parametro elegido se escribe en las etiquetas y cada campo tiene su ejemplo
                .andExpect(content().string(containsString("data-unidad-parametro>unidad</span>")))
                .andExpect(content().string(containsString("data-rango-parametro")))
                .andExpect(matchesPatternLocal("id=\"agregar-plan-minimo\"[^>]*placeholder=\"Ej: 18\""))
                .andExpect(matchesPatternLocal("id=\"agregar-plan-ideal\"[^>]*placeholder=\"Ej: 20\""))
                .andExpect(matchesPatternLocal("id=\"agregar-plan-maximo\"[^>]*placeholder=\"Ej: 22\""))
                .andExpect(content().string(containsString("data-agregar-parametro")))
                .andExpect(content().string(containsString("Agregar par")))
                .andExpect(content().string(containsString("Todav")))
                .andExpect(content().string(containsString("data-parametros-dialogo-vacio")))
                .andExpect(content().string(containsString("data-plantilla-parametro-dialogo")))
                .andExpect(content().string(containsString("data-quitar-parametro-dialogo")))
                .andExpect(content().string(containsString("data-confirmar-plan")))
                .andExpect(content().string(containsString("data-cerrar-plan")))
                // Ventana FUERA del formulario: sus campos no se envian
                .andExpect(matchesPatternLocal("</form>.*id=\"dialogo-agregar-plan\""))
                // El mini formulario no tiene validaciones de navegador: solo el aviso de que falta elegir
                .andExpect(matchesPatternLocal("id=\"agregar-plan-minimo\"(?![^>]*(required|min=|max=))[^>]*>"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-55: al modificar, cada plan existente es un bloque con su etapa de control, su etapa a controlar, sus parámetros con unidad y los campos ocultos indexados de los dos niveles")
    void formularioEdicion_debeMostrarCadaPlanComoBloque() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(recetaConDosPlanes());
        stubInsumos();
        stubSegundoPlan();

        String html = mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Control de densidad<"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Control de temperatura<"))
                .andExpect(matchesPatternLocal("etapa a controlar: <span data-celda=\"etapa\">Fermentaci"))
                .andExpect(matchesPatternLocal("etapa a controlar: <span data-celda=\"etapa\">Maduraci"))
                // Plan 1, parametros 0 y 1: ids y valores en campos ocultos con los dos indices
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.idEtapaControl\"[^>]*value=\"2\""))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[0\\]\\.idParametroControl\"[^>]*value=\"2\""))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[0\\]\\.valorIdeal\"[^>]*value=\"20\\.0\""))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[1\\]\\.idParametroControl\"[^>]*value=\"3\""))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[1\\]\\.valorMaximo\"[^>]*value=\"5\\.6\""))
                // Cada valor visible lleva la unidad de su parametro (sin ceros sobrantes)
                .andExpect(matchesPatternLocal("data-celda=\"valorMinimo\">18 .C<"))
                .andExpect(matchesPatternLocal("data-celda=\"valorMaximo\">22 .C<"))
                .andExpect(matchesPatternLocal("data-celda=\"valorMinimo\">5,2 pH<"))
                .andExpect(matchesPatternLocal("data-celda=\"valorMaximo\">5,6 pH<"))
                .andExpect(matchesPatternLocal("data-planes-vacio[^>]*hidden"))
                .andReturn().getResponse().getContentAsString();

        // Dos bloques de plan reales, mas el de la plantilla; un boton "Quitar plan" por bloque
        assertThat(html.split("data-quitar-plan", -1).length - 1).isEqualTo(3);
        // Tres filas de parametro reales (1 + 2), mas la de la plantilla
        assertThat(html.split("data-parametro-plan", -1).length - 1).isEqualTo(4);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-56: si la etapa de control o el parámetro de un plan ya no existen, el bloque lo indica en lugar de fallar y conserva los ids")
    void formularioEdicion_debeIndicarEtapasYParametrosQueYaNoExisten() throws Exception {
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        stubInsumos();
        when(etapaControlServicio.buscarPorId(1L)).thenThrow(new RecursoNoEncontradoException("No se encontro la etapa de control con ID: 1"));
        when(parametroControlServicio.buscarPorId(1L)).thenThrow(new RecursoNoEncontradoException("No se encontro el parametro de control con ID: 1"));

        mockMvc.perform(get("/recetas/7/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Etapa de control no disponible")))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Par.metro no disponible<"))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[0\\]\\.idEtapaControl\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("planesMonitoreo\\[0\\]\\.detallesParametroControl\\[0\\]\\.idParametroControl\"[^>]*value=\"1\""))
                // Sin la unidad, el valor se muestra solo
                .andExpect(matchesPatternLocal("data-celda=\"valorIdeal\">1,012 <"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-57: un alta y una modificación válidas con dos planes llaman al service con los planes y sus parámetros anidados (ids y valores)")
    void altaYModificacion_debenEnlazarLosPlanesAnidados() throws Exception {
        mockMvc.perform(post("/recetas").with(csrf()).params(parametrosConPlanes()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"));
        mockMvc.perform(post("/recetas/7").with(csrf()).params(parametrosConPlanes()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/recetas"));

        ArgumentCaptor<RecetaFormDTO> alta = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).altaReceta(alta.capture());
        ArgumentCaptor<RecetaFormDTO> modificacion = ArgumentCaptor.forClass(RecetaFormDTO.class);
        verify(recetaServicio).modificarReceta(eq(7L), modificacion.capture());

        for (RecetaFormDTO enviado : List.of(alta.getValue(), modificacion.getValue())) {
            var planes = enviado.getVersion().getPlanesMonitoreo();
            assertThat(planes).hasSize(2);
            assertThat(planes.get(0).getIdEtapaControl()).isEqualTo(1L);
            assertThat(planes.get(0).getDetallesParametroControl()).hasSize(1);
            assertThat(planes.get(1).getIdEtapaControl()).isEqualTo(2L);
            assertThat(planes.get(1).getDetallesParametroControl()).hasSize(2);
            var temperatura = planes.get(1).getDetallesParametroControl().get(0);
            assertThat(temperatura.getIdParametroControl()).isEqualTo(2L);
            assertThat(temperatura.getValorMinimo()).isEqualTo(18.0);
            assertThat(temperatura.getValorIdeal()).isEqualTo(20.0);
            assertThat(temperatura.getValorMaximo()).isEqualTo(22.0);
            var ph = planes.get(1).getDetallesParametroControl().get(1);
            assertThat(ph.getIdParametroControl()).isEqualTo(3L);
            assertThat(ph.getValorMaximo()).isEqualTo(5.6);
        }
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-58: una regla del plan que rechaza el service (etapa repetida, plan sin parámetros, parámetro repetido, rango) se informa en el resumen y el formulario conserva todos los planes con sus nombres y unidades")
    void alta_debeInformarLasReglasDelPlanYConservarLosPlanes() throws Exception {
        stubInsumos();
        stubSegundoPlan();

        for (String regla : List.of(
                "La receta no puede tener mas de un plan de monitoreo para la misma etapa de control",
                "Cada plan de monitoreo debe tener al menos un parametro de control",
                "Un plan de monitoreo no puede repetir el mismo parametro de control",
                "El valor ideal del parametro de control Temperatura debe estar dentro del rango minimo y maximo definido",
                "El valor maximo del parametro de control Temperatura no puede ser superior al valor maximo posible (100.0)")) {
            doThrow(new ReglaNegocioException(regla)).when(recetaServicio).altaReceta(any());

            String html = mockMvc.perform(post("/recetas").with(csrf()).params(parametrosConPlanes()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("receta/receta-form"))
                    .andExpect(content().string(containsString("No se pudo crear la receta")))
                    .andExpect(matchesPatternLocal("<li>" + java.util.regex.Pattern.quote(regla) + "</li>"))
                    // Los dos planes y sus tres parametros siguen en el formulario, con nombre y unidad
                    .andExpect(matchesPatternLocal("data-celda=\"nombre\">Control de densidad<"))
                    .andExpect(matchesPatternLocal("data-celda=\"nombre\">Control de temperatura<"))
                    .andExpect(matchesPatternLocal("data-celda=\"nombre\">Temperatura<"))
                    .andExpect(matchesPatternLocal("data-celda=\"valorIdeal\">20 .C<"))
                    .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[1\\]\\.valorMaximo\"[^>]*value=\"5\\.6\""))
                    .andExpect(matchesPatternLocal("data-planes-vacio[^>]*hidden"))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html.split("data-parametro-plan", -1).length - 1).isEqualTo(4);
        }
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-59: una etapa de control, un parámetro o un valor faltante o no numérico en un plan se informa debajo de la sección y en el resumen, resalta el plan o la fila con problemas y no llega al service")
    void alta_ymodificacion_debenInformarCamposIncompletosDelPlan() throws Exception {
        stubInsumos();
        stubSegundoPlan();

        // Plan sin etapa de control: se resalta solo ese bloque
        LinkedMultiValueMap<String, String> sinEtapa = parametrosConPlanes();
        sinEtapa.remove("version.planesMonitoreo[1].idEtapaControl");
        String html = mockMvc.perform(post("/recetas").with(csrf()).params(sinEtapa))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo crear la receta")))
                .andExpect(matchesPatternLocal("text-rose-600\">Cada plan de monitoreo debe tener una etapa de control<"))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Control de densidad<"))
                .andReturn().getResponse().getContentAsString();
        assertThat(html.split("data-plan\\s+class=\"border rounded-lg overflow-hidden border-rose-400\"", -1).length - 1).isEqualTo(1);

        // Parametro sin seleccionar: se resalta la fila
        LinkedMultiValueMap<String, String> sinParametro = parametrosConPlanes();
        sinParametro.remove("version.planesMonitoreo[1].detallesParametroControl[1].idParametroControl");
        mockMvc.perform(post("/recetas/7").with(csrf()).params(sinParametro))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se pudo modificar la receta")))
                .andExpect(matchesPatternLocal("text-rose-600\">Cada par.metro de control del plan debe estar seleccionado<"))
                .andExpect(matchesPatternLocal("<tr[^>]*data-parametro-plan[^>]*bg-rose-50"));

        // Valores faltantes: cada mensaje aparece una sola vez debajo de la seccion
        LinkedMultiValueMap<String, String> sinValores = parametrosConPlanes();
        sinValores.remove("version.planesMonitoreo[0].detallesParametroControl[0].valorMinimo");
        sinValores.remove("version.planesMonitoreo[0].detallesParametroControl[0].valorIdeal");
        sinValores.remove("version.planesMonitoreo[1].detallesParametroControl[0].valorIdeal");
        sinValores.remove("version.planesMonitoreo[1].detallesParametroControl[0].valorMaximo");
        String htmlSinValores = mockMvc.perform(post("/recetas").with(csrf()).params(sinValores))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("text-rose-600\">El valor m.nimo de cada par.metro de control es obligatorio<"))
                .andExpect(matchesPatternLocal("text-rose-600\">El valor ideal de cada par.metro de control es obligatorio<"))
                .andExpect(matchesPatternLocal("text-rose-600\">El valor m.ximo de cada par.metro de control es obligatorio<"))
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlSinValores.split("text-rose-600\">El valor ideal de cada par.metro de control es obligatorio<", -1)).hasSize(2);

        // Valor no numerico: error de conversion claro y el plan se conserva
        LinkedMultiValueMap<String, String> noNumerico = parametrosConPlanes();
        noNumerico.set("version.planesMonitoreo[1].detallesParametroControl[0].valorMaximo", "abc");
        String htmlNoNumerico = mockMvc.perform(post("/recetas").with(csrf()).params(noNumerico))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(matchesPatternLocal("text-rose-600\">Ingres. un n.mero v.lido<"))
                .andExpect(matchesPatternLocal("<tr[^>]*data-parametro-plan[^>]*bg-rose-50"))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[1\\]\\.idParametroControl\"[^>]*value=\"3\""))
                .andExpect(matchesPatternLocal("data-celda=\"nombre\">Control de temperatura<"))
                .andReturn().getResponse().getContentAsString();
        assertThat(htmlNoNumerico.split("text-rose-600\">Ingres. un n.mero v.lido<", -1)).hasSize(2);

        verify(recetaServicio, never()).altaReceta(any());
        verify(recetaServicio, never()).modificarReceta(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-60: un POST armado a mano con índices con huecos en los planes o en sus parámetros, o con una etapa inexistente, no produce un error 500: vuelve al formulario con el mensaje")
    void alta_ymodificacion_debenResistirPlanesMalformados() throws Exception {
        stubInsumos();

        // Solo existe el plan [2] y el parametro [3] del plan [1]: Spring completa los huecos y se informan
        LinkedMultiValueMap<String, String> conHuecos = parametrosBasicos();
        conHuecos.add("version.planesMonitoreo[2].idEtapaControl", "1");
        conHuecos.add("version.planesMonitoreo[1].idEtapaControl", "1");
        conHuecos.add("version.planesMonitoreo[1].detallesParametroControl[3].idParametroControl", "1");
        conHuecos.add("version.planesMonitoreo[1].detallesParametroControl[3].valorMinimo", "1");
        mockMvc.perform(post("/recetas").with(csrf()).params(conHuecos))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(matchesPatternLocal("Cada plan de monitoreo debe tener una etapa de control"))
                .andExpect(matchesPatternLocal("Cada par.metro de control del plan debe estar seleccionado"))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[2\\]\\.idEtapaControl\"[^>]*value=\"1\""))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[1\\]\\.detallesParametroControl\\[3\\]\\.idParametroControl\"[^>]*value=\"1\""));

        // Etapa de control que no existe: la valida el service y el bloque lo indica
        when(recetaServicio.modificarReceta(eq(7L), any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontro la etapa de control con ID: 99"));
        when(recetaServicio.buscarPorId(7L)).thenReturn(receta(7L, "Pale Ale"));
        LinkedMultiValueMap<String, String> inexistente = parametrosConPlanes();
        inexistente.set("version.planesMonitoreo[0].idEtapaControl", "99");
        when(etapaControlServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontro la etapa de control con ID: 99"));
        mockMvc.perform(post("/recetas/7").with(csrf()).params(inexistente))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/receta-form"))
                .andExpect(content().string(containsString("No se encontro la etapa de control con ID: 99")))
                .andExpect(content().string(containsString("Etapa de control no disponible")))
                .andExpect(matchesPatternLocal("name=\"version\\.planesMonitoreo\\[0\\]\\.idEtapaControl\"[^>]*value=\"99\""));

        verify(recetaServicio, never()).altaReceta(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-61: el buscador de etapas de control devuelve el fragmento con nombre y etapa a controlar (solo las cuatro controlables), los resultados con \"Elegir\" y páginas de 8 en orden alfabético")
    void buscadorEtapasControl_debeDevolverElFragmentoConResultados() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        EtapaControlResponseDTO.builder().id(4L).nombre("Control de densidad").etapaAControlar(TipoEtapa.FERMENTACION).build(),
                        EtapaControlResponseDTO.builder().id(5L).nombre("Control de temperatura").etapaAControlar(TipoEtapa.MACERACION).build()),
                        PageRequest.of(0, 8), 2));

        mockMvc.perform(get("/recetas/buscador/etapas-control"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/buscador-etapa-control :: buscador"))
                .andExpect(content().string(containsString("Buscar etapa de control")))
                .andExpect(content().string(containsString("action=\"/recetas/buscador/etapas-control\"")))
                .andExpect(content().string(containsString("data-modal-buscar")))
                .andExpect(content().string(containsString("name=\"nombre\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Control de densidad\"")))
                .andExpect(content().string(containsString("name=\"etapa\"")))
                // Solo las cuatro etapas que se pueden controlar
                .andExpect(content().string(containsString("<option value=\"MACERACION\"")))
                .andExpect(content().string(containsString("<option value=\"HERVIDO\"")))
                .andExpect(content().string(containsString("<option value=\"FERMENTACION\"")))
                .andExpect(content().string(containsString("<option value=\"MADURACION\"")))
                .andExpect(content().string(not(containsString("<option value=\"MOLIENDA\""))))
                .andExpect(content().string(not(containsString("<option value=\"ENVASADO\""))))
                .andExpect(matchesPatternLocal("Control de densidad.*>\\s*Fermentaci.n\\s*<.*Control de temperatura.*>\\s*Maceraci.n\\s*<"))
                .andExpect(content().string(containsString("data-id=\"4\"")))
                .andExpect(content().string(containsString("data-texto=\"Control de densidad\"")))
                .andExpect(content().string(containsString("data-etapa=\"FERMENTACION\"")))
                .andExpect(content().string(containsString("data-id=\"5\"")))
                .andExpect(content().string(containsString("data-etapa=\"MACERACION\"")))
                .andExpect(content().string(containsString("Elegir")))
                // Es un fragmento: no incluye el layout
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("Gestión de Insumos"))));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(etapaControlServicio).filtrarEtapasControl(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().getOrderFor("nombre").getDirection().isAscending()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-62: los buscadores de etapas de control y de parámetros filtran por sus criterios, conservan los criterios en la paginación y el de parámetros muestra unidad y rango admitido")
    void buscadoresDelPlan_debenFiltrarYMostrarResultados() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        EtapaControlResponseDTO.builder().id(4L).nombre("Control de densidad").etapaAControlar(TipoEtapa.FERMENTACION).build()),
                        PageRequest.of(1, 8), 9));
        when(parametroControlServicio.filtrarParametrosControl(any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        ParametroControlResponseDTO.builder().id(2L).nombre("Temperatura").unidadDeMedida("°C").valorMinimo(0.0).valorMaximo(100.0).build(),
                        ParametroControlResponseDTO.builder().id(3L).nombre("pH").unidadDeMedida("pH").valorMinimo(0.0).valorMaximo(14.0).build()),
                        PageRequest.of(1, 8), 10));

        mockMvc.perform(get("/recetas/buscador/etapas-control").param("nombre", "dens").param("etapa", "FERMENTACION").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-modal-get")))
                .andExpect(content().string(containsString("/recetas/buscador/etapas-control?pagina=0")))
                .andExpect(content().string(containsString("etapa=FERMENTACION")))
                .andExpect(content().string(containsString("nombre=dens")))
                .andExpect(matchesPatternLocal("<option value=\"FERMENTACION\" selected"));
        ArgumentCaptor<Pageable> captorEtapas = ArgumentCaptor.forClass(Pageable.class);
        verify(etapaControlServicio).filtrarEtapasControl(eq("dens"), eq(TipoEtapa.FERMENTACION), captorEtapas.capture());
        assertThat(captorEtapas.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captorEtapas.getValue().getPageSize()).isEqualTo(8);
        assertThat(captorEtapas.getValue().getSort().getOrderFor("nombre").isAscending()).isTrue();

        mockMvc.perform(get("/recetas/buscador/parametros-control").param("nombre", "temp").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("receta/buscador-parametro-control :: buscador"))
                .andExpect(content().string(containsString("Buscar par")))
                .andExpect(content().string(containsString("action=\"/recetas/buscador/parametros-control\"")))
                .andExpect(content().string(containsString("data-modal-buscar")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Temperatura\"")))
                .andExpect(content().string(containsString("Rango admitido")))
                .andExpect(matchesPatternLocal("Temperatura.*>\\s*.C\\s*<.*>0&ndash;100 .C<"))
                .andExpect(matchesPatternLocal(">0&ndash;14 pH<"))
                // Cada fila lleva los datos que el formulario usa para mostrar la unidad y el rango
                .andExpect(content().string(containsString("data-id=\"2\"")))
                .andExpect(content().string(containsString("data-texto=\"Temperatura\"")))
                .andExpect(matchesPatternLocal("data-unidad=\".C\""))
                .andExpect(content().string(containsString("data-minimo=\"0.0\"")))
                .andExpect(content().string(containsString("data-maximo=\"100.0\"")))
                .andExpect(content().string(containsString("data-id=\"3\"")))
                .andExpect(content().string(containsString("data-unidad=\"pH\"")))
                .andExpect(content().string(containsString("data-modal-get")))
                .andExpect(content().string(containsString("/recetas/buscador/parametros-control?pagina=0")))
                .andExpect(content().string(containsString("nombre=temp")))
                .andExpect(content().string(not(containsString("<html"))));
        ArgumentCaptor<Pageable> captorParametros = ArgumentCaptor.forClass(Pageable.class);
        verify(parametroControlServicio).filtrarParametrosControl(eq("temp"), captorParametros.capture());
        assertThat(captorParametros.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captorParametros.getValue().getPageSize()).isEqualTo(8);
        assertThat(captorParametros.getValue().getSort().getOrderFor("nombre").isAscending()).isTrue();
        assertThat(captorParametros.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captorParametros.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-RCC-63: los buscadores del plan sin resultados muestran un mensaje claro y un criterio en blanco equivale a no filtrar")
    void buscadoresDelPlan_debenMostrarMensajeSinResultados() throws Exception {
        when(etapaControlServicio.filtrarEtapasControl(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));
        when(parametroControlServicio.filtrarParametrosControl(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        mockMvc.perform(get("/recetas/buscador/etapas-control").param("nombre", "   ").param("etapa", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron etapas de control con los criterios indicados.")));
        verify(etapaControlServicio).filtrarEtapasControl(isNull(), isNull(), any());

        mockMvc.perform(get("/recetas/buscador/parametros-control").param("nombre", "   "))
                .andExpect(status().isOk())
                .andExpect(matchesPatternLocal("No se encontraron par.metros de control con los criterios indicados."));
        verify(parametroControlServicio).filtrarParametrosControl(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-RCC-64: solo el GERENTE_DE_PRODUCCION usa los buscadores del plan: otro rol es rechazado y no llega a los services")
    void buscadoresDelPlan_debenRechazarRolNoAutorizado() throws Exception {
        for (String url : List.of("/recetas/buscador/etapas-control", "/recetas/buscador/parametros-control")) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }

        verify(etapaControlServicio, never()).filtrarEtapasControl(any(), any(), any());
        verify(parametroControlServicio, never()).filtrarParametrosControl(any(), any());
    }

    // ==================== datos de prueba ====================

    /**
     * Agrega al request los datos de una receta completa y válida: datos generales, una malta, dos
     * lúpulos (uno en Hervor y otro en Dry hop), una levadura y un plan de monitoreo con un parámetro.
     * {@code cambios} son pares (nombre, valor) que reemplazan al valor válido de ese parámetro.
     */
    private static MockHttpServletRequestBuilder recetaValida(MockHttpServletRequestBuilder request, String... cambios) {
        LinkedMultiValueMap<String, String> parametros = parametrosBasicos();
        for (int i = 0; i < cambios.length; i += 2) {
            parametros.set(cambios[i], cambios[i + 1]);
        }
        return request.with(csrf()).params(parametros)
                .param("version.planesMonitoreo[0].idEtapaControl", "1")
                .param("version.planesMonitoreo[0].detallesParametroControl[0].idParametroControl", "1")
                .param("version.planesMonitoreo[0].detallesParametroControl[0].valorMinimo", "1.01")
                .param("version.planesMonitoreo[0].detallesParametroControl[0].valorIdeal", "1.012")
                .param("version.planesMonitoreo[0].detallesParametroControl[0].valorMaximo", "1.015");
    }

    /** Parámetros de una receta válida sin plan de monitoreo. */
    private static LinkedMultiValueMap<String, String> parametrosBasicos() {
        LinkedMultiValueMap<String, String> p = new LinkedMultiValueMap<>();
        p.add("version.nombre", "Pale Ale");
        p.add("version.volumenBase", "20");
        p.add("version.relacionDeEmpaste", "3");
        p.add("version.ogObjetivo", "1050");
        p.add("version.fgObjetivo", "1010");
        p.add("version.ibuObjetivo", "35");
        p.add("version.duracionMaceracion", "60");
        p.add("version.duracionHervido", "90");
        p.add("version.duracionFermentacion", "10");
        p.add("version.duracionMaduracion", "7");
        p.add("version.detallesMalta[0].idMalta", "1");
        p.add("version.detallesMalta[0].cantidad", "4.5");
        p.add("version.detallesLupulo[0].idLupulo", "1");
        p.add("version.detallesLupulo[0].cantidad", "25");
        p.add("version.detallesLupulo[0].uso", "HERVOR");
        p.add("version.detallesLupulo[0].etapaDeUso", "HERVIDO");
        p.add("version.detallesLupulo[0].tiempoDeHervor", "60");
        p.add("version.detallesLupulo[1].idLupulo", "2");
        p.add("version.detallesLupulo[1].cantidad", "40");
        p.add("version.detallesLupulo[1].uso", "DRY_HOP");
        p.add("version.detallesLupulo[1].etapaDeUso", "FERMENTACION");
        p.add("version.detallesLevadura[0].idLevadura", "1");
        p.add("version.detallesLevadura[0].cantidad", "11.5");
        return p;
    }

    /** Parámetros de una receta válida con dos planes: densidad (1 parámetro) y temperatura (temperatura y pH). */
    private static LinkedMultiValueMap<String, String> parametrosConPlanes() {
        LinkedMultiValueMap<String, String> p = parametrosBasicos();
        p.add("version.planesMonitoreo[0].idEtapaControl", "1");
        p.add("version.planesMonitoreo[0].detallesParametroControl[0].idParametroControl", "1");
        p.add("version.planesMonitoreo[0].detallesParametroControl[0].valorMinimo", "1.01");
        p.add("version.planesMonitoreo[0].detallesParametroControl[0].valorIdeal", "1.012");
        p.add("version.planesMonitoreo[0].detallesParametroControl[0].valorMaximo", "1.015");
        p.add("version.planesMonitoreo[1].idEtapaControl", "2");
        p.add("version.planesMonitoreo[1].detallesParametroControl[0].idParametroControl", "2");
        p.add("version.planesMonitoreo[1].detallesParametroControl[0].valorMinimo", "18");
        p.add("version.planesMonitoreo[1].detallesParametroControl[0].valorIdeal", "20");
        p.add("version.planesMonitoreo[1].detallesParametroControl[0].valorMaximo", "22");
        p.add("version.planesMonitoreo[1].detallesParametroControl[1].idParametroControl", "3");
        p.add("version.planesMonitoreo[1].detallesParametroControl[1].valorMinimo", "5.2");
        p.add("version.planesMonitoreo[1].detallesParametroControl[1].valorIdeal", "5.4");
        p.add("version.planesMonitoreo[1].detallesParametroControl[1].valorMaximo", "5.6");
        return p;
    }

    /** Datos del segundo plan de {@link #parametrosConPlanes()}: etapa 2 y los parámetros 2 (temperatura) y 3 (pH). */
    private void stubSegundoPlan() {
        when(etapaControlServicio.buscarPorId(2L)).thenReturn(EtapaControlResponseDTO.builder().id(2L).nombre("Control de temperatura")
                .etapaAControlar(TipoEtapa.MADURACION).build());
        when(parametroControlServicio.buscarPorId(2L)).thenReturn(ParametroControlResponseDTO.builder().id(2L).nombre("Temperatura")
                .unidadDeMedida("°C").valorMinimo(0.0).valorMaximo(100.0).build());
        when(parametroControlServicio.buscarPorId(3L)).thenReturn(ParametroControlResponseDTO.builder().id(3L).nombre("pH")
                .unidadDeMedida("pH").valorMinimo(0.0).valorMaximo(14.0).build());
    }

    /** La receta de {@link #receta(Long, String)} con un segundo plan (etapa 2: temperatura y pH). */
    private static RecetaResponseDTO recetaConDosPlanes() {
        var vigente = versionBuilder("Pale Ale").build();
        var planes = new java.util.ArrayList<>(vigente.getPlanesMonitoreo());
        planes.add(PlanMonitoreoEtapaResponseDTO.builder().id(2L)
                .etapaControl(EtapaControlResponseDTO.builder().id(2L).nombre("Control de temperatura")
                        .etapaAControlar(TipoEtapa.MADURACION).build())
                .detallesParametroControl(List.of(
                        DetalleParametroControlResponseDTO.builder().id(2L).valorMinimo(18.0).valorIdeal(20.0).valorMaximo(22.0)
                                .parametroControl(ParametroControlResponseDTO.builder().id(2L).nombre("Temperatura").unidadDeMedida("°C").build())
                                .build(),
                        DetalleParametroControlResponseDTO.builder().id(3L).valorMinimo(5.2).valorIdeal(5.4).valorMaximo(5.6)
                                .parametroControl(ParametroControlResponseDTO.builder().id(3L).nombre("pH").unidadDeMedida("pH").build())
                                .build()))
                .build());
        return RecetaResponseDTO.builder().id(7L).version(versionBuilder("Pale Ale").planesMonitoreo(planes).build()).build();
    }

    /** Datos de los insumos, la etapa y el parámetro que referencian las filas de {@link #receta(Long, String)}. */
    private void stubInsumos() {
        when(maltaServicio.buscarPorId(1L)).thenReturn(MaltaResponseDTO.builder().id(1L).nombre("Pilsen").tipo(TipoMalta.BASE).build());
        when(lupuloServicio.buscarPorId(1L)).thenReturn(LupuloResponseDTO.builder().id(1L).nombre("Cascade").formato(FormatoLupulo.PELLET).aa(5.5).build());
        when(lupuloServicio.buscarPorId(2L)).thenReturn(LupuloResponseDTO.builder().id(2L).nombre("Citra").formato(FormatoLupulo.FLOR).aa(12.0).build());
        when(lupuloServicio.buscarPorId(3L)).thenReturn(LupuloResponseDTO.builder().id(3L).nombre("Mosaic").formato(FormatoLupulo.PELLET).aa(11.5).build());
        when(levaduraServicio.buscarPorId(1L)).thenReturn(LevaduraResponseDTO.builder().id(1L).nombre("US-05").tipo(TipoLevadura.ALE).build());
        when(etapaControlServicio.buscarPorId(1L)).thenReturn(EtapaControlResponseDTO.builder().id(1L).nombre("Control de densidad")
                .etapaAControlar(TipoEtapa.FERMENTACION).build());
        when(parametroControlServicio.buscarPorId(1L)).thenReturn(ParametroControlResponseDTO.builder().id(1L).nombre("Densidad").unidadDeMedida("SG").build());
    }

    /** Verifica que el HTML contenga una coincidencia de la expresión regular (con saltos de línea). */
    private static org.springframework.test.web.servlet.ResultMatcher matchesPatternLocal(String regex) {
        return content().string(matchesPattern("(?s).*" + regex + ".*"));
    }

    private static RecetaResponseDTO receta(Long id, String nombre) {
        return RecetaResponseDTO.builder().id(id).version(versionBuilder(nombre).build()).build();
    }

    private static VersionRecetaResponseDTO.VersionRecetaResponseDTOBuilder versionBuilder(String nombre) {
        return VersionRecetaResponseDTO.builder()
                .id(70L)
                .nombre(nombre)
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.05)
                .fgObjetivo(1.01)
                .ibuObjetivo(35)
                .duracionMaceracion(60)
                .duracionHervido(90)
                .duracionFermentacion(10)
                .duracionMaduracion(4)
                .esUltimaVersion(true)
                .detallesMalta(List.of(DetalleMaltaResponseDTO.builder().id(1L).cantidad(4.5)
                        .malta(MaltaResponseDTO.builder().id(1L).nombre("Pilsen").tipo(TipoMalta.BASE).build()).build()))
                .detallesLupulo(List.of(
                        DetalleLupuloResponseDTO.builder().id(3L).cantidad(30.0).uso(UsoLupulo.WHIRLPOOL)
                                .etapaDeUso(TipoEtapa.HERVIDO)
                                .lupulo(LupuloResponseDTO.builder().id(3L).nombre("Mosaic").formato(FormatoLupulo.PELLET).aa(11.5).build()).build(),
                        DetalleLupuloResponseDTO.builder().id(1L).cantidad(25.0).uso(UsoLupulo.HERVOR)
                                .etapaDeUso(TipoEtapa.HERVIDO).tiempoDeHervor(60.0)
                                .lupulo(LupuloResponseDTO.builder().id(1L).nombre("Cascade").formato(FormatoLupulo.PELLET).aa(5.5).build()).build(),
                        DetalleLupuloResponseDTO.builder().id(2L).cantidad(40.0).uso(UsoLupulo.DRY_HOP)
                                .etapaDeUso(TipoEtapa.FERMENTACION)
                                .lupulo(LupuloResponseDTO.builder().id(2L).nombre("Citra").formato(FormatoLupulo.FLOR).aa(12.0).build()).build()))
                .detallesLevadura(List.of(DetalleLevaduraResponseDTO.builder().id(1L).cantidad(11.5)
                        .levadura(LevaduraResponseDTO.builder().id(1L).nombre("US-05").tipo(TipoLevadura.ALE).build()).build()))
                .planesMonitoreo(List.of(PlanMonitoreoEtapaResponseDTO.builder().id(1L)
                        .etapaControl(EtapaControlResponseDTO.builder().id(1L).nombre("Control de densidad")
                                .etapaAControlar(TipoEtapa.FERMENTACION).build())
                        .detallesParametroControl(List.of(DetalleParametroControlResponseDTO.builder().id(1L)
                                .valorMinimo(1.01).valorIdeal(1.012).valorMaximo(1.015)
                                .parametroControl(ParametroControlResponseDTO.builder().id(1L).nombre("Densidad").unidadDeMedida("SG").build())
                                .build()))
                        .build()));
    }
}
