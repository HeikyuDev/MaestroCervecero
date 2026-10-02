package com.github.heikyudev.maestrocervecero.presentation.controller.receta;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
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

    // ==================== datos de prueba ====================

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
