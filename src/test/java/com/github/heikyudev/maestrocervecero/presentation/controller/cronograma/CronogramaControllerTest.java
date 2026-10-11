package com.github.heikyudev.maestrocervecero.presentation.controller.cronograma;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CronogramaController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class CronogramaControllerTest {

    private static final String URL = "/cronograma-produccion";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILoteServicio loteServicio;

    @MockitoBean
    private IFermentadorServicio fermentadorServicio;

    @MockitoBean
    private IPlanificacionProduccionServicio planificacionServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /cronograma-produccion ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-01: el GERENTE_DE_PRODUCCION ve el cronograma con los filtros, el diagrama (un carril por fermentador, barras con receta, plan y lote) y las colas")
    void cronograma_debeMostrarFiltrosDiagramaYColas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2),
                lote(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/cronograma"))
                .andExpect(content().string(containsString("Cronograma de Producci")))
                // Filtros
                .andExpect(content().string(containsString("action=\"/cronograma-produccion\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString(">Todos</option>")))
                .andExpect(content().string(containsString("Mostrar finalizados anteriores")))
                .andExpect(content().string(containsString("Aplicar")))
                .andExpect(content().string(containsString("Limpiar")))
                // Diagrama
                .andExpect(content().string(containsString("data-gantt")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-1")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-2")))
                .andExpect(content().string(containsString("Stout - Plan #8 - STOUT-R8-1")))
                .andExpect(content().string(containsString("gantt-color--en-ejecucion")))
                .andExpect(content().string(containsString("gantt-color--pendiente")))
                .andExpect(content().string(containsString("data-gantt-hoy")))
                .andExpect(content().string(containsString("data-gantt-hoy-bandera")))
                .andExpect(content().string(containsString("overflow-x-auto")))
                .andExpect(content().string(containsString("sticky left-0")))
                // Colas
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-01\"")))
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-02\"")));

        verify(loteServicio).listarLotesParaCronograma(null, false);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-02: sin lotes cada fermentador activo igual tiene su carril, con el aviso \"Sin lotes en este periodo\", y las colas muestran el mensaje de vacio")
    void cronograma_sinLotesMuestraLosCarrilesVacios() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-gantt")))
                .andExpect(content().string(containsString("No hay lotes para mostrar con los criterios indicados")))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "data-gantt-carril-vacio")).isEqualTo(2);
        assertThat(cuenta(html, "Sin lotes en este per")).isEqualTo(2);
        assertThat(cuenta(html, "data-cola-vacia")).isEqualTo(2);
        assertThat(cuenta(html, "No hay lotes pendientes en este fermentador")).isEqualTo(2);
        assertThat(html).doesNotContain("data-mover=");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-03: sin fermentadores activos ni lotes no se dibuja el diagrama ni hay colas, y se explica por que")
    void cronograma_sinFermentadoresNiLotes() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-cronograma-sin-fermentadores")))
                .andExpect(content().string(containsString("No hay fermentadores activos ni lotes para mostrar")))
                .andExpect(content().string(not(containsString("data-gantt>"))))
                .andExpect(content().string(not(containsString("data-cola-fermentador"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-04: los fermentadores activos se piden todos, ordenados por identificador, y llenan el filtro y los carriles")
    void cronograma_pideTodosLosFermentadoresActivosOrdenados() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadores(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno")).isNotNull();
        assertThat(html).containsPattern("<option value=\"1\"[^>]*>F-01</option>");
        assertThat(html).containsPattern("<option value=\"2\"[^>]*>F-02</option>");
        assertThat(html.indexOf(">F-01</option>")).isLessThan(html.indexOf(">F-02</option>"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-05: el fermentador y el historial llegan al service; el filtro queda elegido, la casilla tildada y solo el carril de ese fermentador")
    void cronograma_pasaLosFiltrosAlService() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "2").param("incluirHistorial", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(2L, true);
        assertThat(html).containsPattern("<option value=\"2\"[^>]*selected=\"selected\"[^>]*>F-02</option>");
        assertThat(html).containsPattern("<input type=\"checkbox\"[^>]*name=\"incluirHistorial\"[^>]*checked=\"checked\"");
        assertThat(cuenta(html, "data-gantt-carril-vacio")).isEqualTo(1);
        assertThat(html).contains("data-cola-fermentador=\"F-02\"").doesNotContain("data-cola-fermentador=\"F-01\"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-06: la casilla de historial acepta \"on\" (valor de un checkbox sin value) y cualquier otro valor equivale a no tildada")
    void cronograma_interpretaLaCasillaDeHistorial() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        mockMvc.perform(get(URL).param("incluirHistorial", "on")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("incluirHistorial", "no")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("incluirHistorial", "")).andExpect(status().isOk());

        // Assert
        verify(loteServicio).listarLotesParaCronograma(null, true);
        verify(loteServicio, org.mockito.Mockito.times(2)).listarLotesParaCronograma(null, false);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-07: la cola de cada fermentador sigue el orden de los carriles, numera los pendientes y deja desactivado Subir en el primero y Bajar en el ultimo")
    void cronograma_colasConBotonesSubirYBajar() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 3),
                lote(4L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: orden de las secciones
        assertThat(html.indexOf("data-cola-fermentador=\"F-01\"")).isLessThan(html.indexOf("data-cola-fermentador=\"F-02\""));
        assertThat(html).contains("Cola pendiente de");
        // Filas en orden de cola
        assertThat(html.indexOf("data-lote=\"1\"")).isLessThan(html.indexOf("data-lote=\"2\""));
        // Subir: desactivado solo en el primero de cada cola
        assertThat(boton(html, "subir", 1L)).contains("disabled=\"disabled\"");
        assertThat(boton(html, "subir", 2L)).doesNotContain("disabled=\"disabled\"");
        assertThat(boton(html, "subir", 3L)).doesNotContain("disabled=\"disabled\"");
        assertThat(boton(html, "subir", 4L)).contains("disabled=\"disabled\"");
        // Bajar: desactivado solo en el ultimo de cada cola
        assertThat(boton(html, "bajar", 1L)).doesNotContain("disabled=\"disabled\"");
        assertThat(boton(html, "bajar", 2L)).doesNotContain("disabled=\"disabled\"");
        assertThat(boton(html, "bajar", 3L)).contains("disabled=\"disabled\"");
        assertThat(boton(html, "bajar", 4L)).contains("disabled=\"disabled\"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-08: cada boton es un POST con token CSRF hacia /lotes/{id}/subir o /bajar, con ícono, texto compacto y sin cortarse")
    void cronograma_losBotonesSonFormulariosPostConCsrf() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2)));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/cronograma-produccion/lotes/1/subir\"")))
                .andExpect(content().string(containsString("action=\"/cronograma-produccion/lotes/1/bajar\"")))
                .andExpect(content().string(containsString("action=\"/cronograma-produccion/lotes/2/subir\"")))
                .andExpect(content().string(containsString("action=\"/cronograma-produccion/lotes/2/bajar\"")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("bi-arrow-up")))
                .andExpect(content().string(containsString("bi-arrow-down")))
                .andExpect(content().string(containsString("Subir")))
                .andExpect(content().string(containsString("Bajar")))
                .andExpect(content().string(containsString("whitespace-nowrap")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-09: el lote en ejecucion se muestra arriba de la cola como \"En ejecucion (no se puede mover)\", sin botones, y los finalizados y cancelados no entran en la cola")
    void cronograma_elLoteEnEjecucionVaArribaDeLaColaSinBotones() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 2),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 3),
                lote(4L, "PALE_ALE-R7-4", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", 4)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-cola-en-ejecucion").contains("En ejecuci").contains("(no se puede mover)");
        assertThat(html.indexOf("data-cola-en-ejecucion")).isLessThan(html.indexOf("data-cola-fila"));
        assertThat(cuenta(html, "data-cola-fila")).isEqualTo(1);
        assertThat(html).contains("lotes/3/subir").doesNotContain("lotes/1/subir").doesNotContain("lotes/2/subir")
                .doesNotContain("lotes/4/subir");
        assertThat(html).doesNotContain("PALE_ALE-R7-4");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-10: la fila de la cola muestra posicion, lote, receta, planificacion, volumen sin ceros sobrantes y fechas estimadas dd/MM/yyyy")
    void cronograma_laFilaDeLaColaMuestraSusDatos() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vol. (L)")))
                .andExpect(content().string(containsString("Inicio")))
                .andExpect(content().string(containsString("Fin")))
                .andExpect(content().string(containsString(">PALE_ALE-R7-1<")))
                .andExpect(content().string(containsString(">#7<")))
                .andExpect(content().string(containsString(">300<")))
                .andExpect(content().string(not(containsString("300.0"))))
                .andExpect(content().string(containsString("01/07/2027")))
                .andExpect(content().string(containsString("05/07/2027")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-11: el filtro de planificacion es un campo en blanco con ícono de lista que abre el buscador (no un combo), con placeholder y boton para quitar")
    void cronograma_elFiltroDePlanificacionEsUnSelectorConBuscador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-selector-buscar")))
                .andExpect(content().string(containsString("bi-list-ul")))
                .andExpect(content().string(containsString("data-buscador-url=\"/cronograma-produccion/buscador/planificaciones\"")))
                .andExpect(content().string(containsString("name=\"idPlanificacion\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar planificaci")))
                .andExpect(content().string(containsString("data-selector-limpiar")))
                .andExpect(content().string(not(containsString("data-selector-menu"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-12: al destacar una planificacion no se quita ningun lote: se atenuan las barras y filas de cola de las demas y el selector muestra la elegida")
    void cronograma_destacarUnaPlanificacionAtenuaLasDemas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", 2)));

        // Act
        String html = mockMvc.perform(get(URL).param("idPlanificacion", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Pale Ale - Plan #7 - PALE_ALE-R7-1").contains("Stout - Plan #8 - STOUT-R8-1");
        assertThat(cuenta(html, "gantt-barra--atenuada\"")).isEqualTo(1);
        assertThat(html).contains("Lote de otra planificaci");
        assertThat(fila(html, 2L)).contains("opacity-50");
        assertThat(fila(html, 1L)).doesNotContain("opacity-50");
        assertThat(html).containsPattern("name=\"idPlanificacion\"[^>]*value=\"7\"|value=\"7\"[^>]*name=\"idPlanificacion\"");
        assertThat(html).contains("Plan #7 - Pale Ale");
        verify(loteServicio).listarLotesParaCronograma(null, false);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-13: sin planificacion destacada ninguna barra ni fila de cola se atenua y la leyenda no menciona el atenuado")
    void cronograma_sinDestacarNoAtenuaNada() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-01", 8L, "Stout", 2)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "gantt-barra--atenuada\"")).isZero();
        assertThat(html).doesNotContain("opacity-50").doesNotContain("Lote de otra planificaci");
        verify(planificacionServicio, never()).buscarPorId(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-14: los botones de mover llevan los filtros activos como campos ocultos para volver al cronograma con la misma vista")
    void cronograma_losBotonesConservanLosFiltros() throws Exception {
        // Arrange
        fermentadores(3L, "F-03");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "3").param("idPlanificacion", "7").param("incluirHistorial", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: un par de formularios (subir y bajar) con los tres campos ocultos cada uno
        Matcher formularios = Pattern.compile("(?s)<form[^>]*lotes/1/(subir|bajar)\"[^>]*>(.*?)</form>").matcher(html);
        int encontrados = 0;
        while (formularios.find()) {
            encontrados++;
            String cuerpo = formularios.group(2);
            assertThat(cuerpo).containsPattern("name=\"idFermentador\"[^>]*value=\"3\"|value=\"3\"[^>]*name=\"idFermentador\"");
            assertThat(cuerpo).containsPattern("name=\"idPlanificacion\"[^>]*value=\"7\"|value=\"7\"[^>]*name=\"idPlanificacion\"");
            assertThat(cuerpo).containsPattern("name=\"incluirHistorial\"[^>]*value=\"true\"|value=\"true\"[^>]*name=\"incluirHistorial\"");
        }
        assertThat(encontrados).isEqualTo(2);
        verify(loteServicio).listarLotesParaCronograma(3L, true);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-15: sin filtros activos los botones de mover no llevan campos ocultos de filtro")
    void cronograma_sinFiltrosLosBotonesNoLlevanCamposOcultos() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        Matcher formulario = Pattern.compile("(?s)<form[^>]*lotes/1/subir\"[^>]*>(.*?)</form>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group(1)).doesNotContain("name=\"idFermentador\"").doesNotContain("name=\"idPlanificacion\"")
                .doesNotContain("name=\"incluirHistorial\"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-37: la cola marca el lote en ejecucion atrasado y los pendientes corridos por ese atraso, con la fecha planificada en el tooltip; sin atraso no se muestra ninguna marca")
    void cronograma_colaMarcaAtrasoYDesplazamiento() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        LoteCronogramaResponseDTO enEjecucion = lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1)
                .toBuilder().diasDeAtraso(2).build();
        LoteCronogramaResponseDTO corrido = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2)
                .toBuilder().diasDeDesplazamiento(2).build();
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                enEjecucion, corrido,
                lote(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Atrasado 2 d").contains("Corrido 2 d");
        assertThat(html).contains("Planificado para el 07/07, corrido 2 d");
        assertThat(html.split("data-cola-atraso", -1)).hasSize(2);
        assertThat(html.split("data-cola-desplazado", -1)).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-38: la leyenda explica los tres colores y las marcas (Planificado, En ejecución, Finalizado, Limpieza y Hoy) y suma el atenuado solo al destacar una planificación")
    void cronograma_laLeyendaExplicaLosTresColoresYLasMarcas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String sinDestacar = leyenda(mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String destacando = leyenda(mockMvc.perform(get(URL).param("idPlanificacion", "7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // Assert
        assertThat(sinDestacar.indexOf("Planificado")).isGreaterThanOrEqualTo(0);
        assertThat(sinDestacar.indexOf("Planificado")).isLessThan(sinDestacar.indexOf("En ejecuci"));
        assertThat(sinDestacar.indexOf("En ejecuci")).isLessThan(sinDestacar.indexOf("Finalizado"));
        assertThat(sinDestacar.indexOf("Finalizado")).isLessThan(sinDestacar.indexOf("Limpieza"));
        assertThat(sinDestacar.indexOf("Limpieza")).isLessThan(sinDestacar.indexOf("Hoy"));
        assertThat(sinDestacar).doesNotContain("Cancelado").doesNotContain("Atraso").doesNotContain("Desplazado")
                .doesNotContain("transcurrido").doesNotContain("Pendiente").doesNotContain("Inicio y fin estimados")
                .doesNotContain("Lote de otra planificaci");
        assertThat(destacando).contains("Lote de otra planificaci");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-39: los lotes cancelados no se dibujan en el diagrama ni entran en la cola")
    void cronograma_losLotesCanceladosNoSeDibujan() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", 2)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Pale Ale - Plan #7 - PALE_ALE-R7-1");
        assertThat(html).doesNotContain("Pale Ale - Plan #7 - PALE_ALE-R7-2").doesNotContain("gantt-color--cancelado");
        assertThat(cuenta(html, "role=\"img\"")).isEqualTo(1);
        assertThat(cuenta(html, "data-cola-fila")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-40: el atraso del lote en ejecución se informa en su barra con la etiqueta \"Atrasado 2 días\", sin color ni tramo aparte")
    void cronograma_elAtrasoSeInformaEnLaBarra() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        LoteCronogramaResponseDTO atrasado = lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1)
                .toBuilder().diasDeAtraso(2).build();
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                atrasado,
                lote(2L, "STOUT-R8-1", EstadoLote.EN_EJECUCION, "F-02", 8L, "Stout", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("gantt-color--en-ejecucion");
        assertThat(cuenta(html, "data-gantt-atraso-etiqueta")).isEqualTo(1);
        assertThat(html).contains("Atrasado 2 d");
        assertThat(Pattern.compile("data-gantt-atraso(?!-etiqueta)").matcher(html).find()).isFalse();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-41: un pendiente corrido por atraso se dibuja azul en su posición corrida, sin marca propia: lo dice el tooltip y la fila de la cola")
    void cronograma_elPendienteCorridoNoLlevaMarcaEnLaBarra() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        LoteCronogramaResponseDTO corrido = lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2)
                .toBuilder().diasDeDesplazamiento(2).build();
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(corrido));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("gantt-color--pendiente");
        assertThat(html).doesNotContain("gantt-color--desplazado").doesNotContain("data-gantt-desplazado").doesNotContain("Desplazado");
        assertThat(html).contains("Planificado para el 07/07, corrido 2 d");
        assertThat(html).contains("Corrido 2 d");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-42: la limpieza se dibuja solo donde queda un día libre futuro entre lotes vivos (un único bloque, en el carril de F-01)")
    void cronograma_dibujaLaLimpiezaSoloDondeQuedaUnDiaLibre() throws Exception {
        // Arrange
        LocalDate hoy = LocalDate.now();
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(9), hoy.plusDays(12)),
                loteEn(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(4L, "STOUT-R8-2", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", hoy.plusDays(8), hoy.plusDays(12))));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "data-gantt-limpieza")).isEqualTo(1);
        assertThat(html).contains("Limpieza del fermentador y demás equipamiento");
        assertThat(html.indexOf("data-gantt-limpieza")).isLessThan(html.indexOf("Stout - Plan #8 - STOUT-R8-1"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-45: el filtro se rotula \"Fermentador\", envía el parámetro idFermentador y ofrece el identificador de cada fermentador como opción")
    void cronograma_elFiltroSeRotulaFermentador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).containsPattern("for=\"idFermentador\"[^>]*>Fermentador</label>");
        assertThat(html).contains("<select id=\"idFermentador\" name=\"idFermentador\"");
        assertThat(html).containsPattern("value=\"1\"[^>]*>F-01</option>");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-46: el carril se rotula \"F-01\", la columna del diagrama \"Fermentador\" y la cola \"Cola pendiente de F-01\"")
    void cronograma_rotulaLosCarrilesYLasColasConElFermentador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains(">F-01<");
        assertThat(html).containsPattern("uppercase tracking-wider\">Fermentador</div>");
        assertThat(html).containsPattern("Cola pendiente de\\s*<span[^>]*>F-01</span>");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-43: el código completo del lote se muestra en la barra, su tooltip, el bloque \"En ejecución\" y la fila de la cola")
    void cronograma_muestraElCodigoCompletoDelLote() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PILSEN_CLASICA-R4-3", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pilsen", 1),
                lote(2L, "PILSEN_CLASICA-R4-4", EstadoLote.PENDIENTE, "F-01", 7L, "Pilsen", 2)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Pilsen - Plan #7 - PILSEN_CLASICA-R4-3").contains("PILSEN_CLASICA-R4-3 - En ejecuci");
        assertThat(html).containsPattern("(?s)data-cola-en-ejecucion.*?>PILSEN_CLASICA-R4-3<");
        assertThat(html).containsPattern("(?s)data-cola-fila.*?>PILSEN_CLASICA-R4-4<");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-44: no se dibuja la limpieza anterior a hoy ni la que sigue a un lote finalizado; las barras se dibujan igual")
    void cronograma_noDibujaLimpiezasEnElPasadoNiTrasFinalizados() throws Exception {
        // Arrange
        LocalDate hoy = LocalDate.now();
        fermentadores(1L, "F-01", 2L, "F-02");
        LoteCronogramaResponseDTO finalizado = loteEn(3L, "STOUT-R8-1", EstadoLote.FINALIZADO, "F-02", 8L, "Stout", hoy.minusDays(10), hoy.minusDays(6))
                .toBuilder().fechaInicio(hoy.minusDays(10).atTime(8, 0)).fechaFinalizacion(hoy.minusDays(6).atTime(18, 0)).build();
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.minusDays(10), hoy.minusDays(6)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.minusDays(4), hoy.minusDays(1)),
                finalizado,
                loteEn(4L, "STOUT-R8-2", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", hoy.plusDays(3), hoy.plusDays(6))));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "data-gantt-limpieza")).isZero();
        assertThat(cuenta(html, "role=\"img\"")).isEqualTo(4);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-16: parametros invalidos no rompen la pagina: se ignoran, se avisa con una alerta de advertencia y el service se invoca sin filtros")
    void cronograma_parametrosInvalidosSeIgnoran() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL).param("idFermentador", "abc").param("idPlanificacion", "-5").param("incluirHistorial", "xyz"))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/cronograma"))
                .andExpect(content().string(containsString("El fermentador indicado no es v")))
                .andExpect(content().string(containsString("La planificaci")))
                .andExpect(content().string(containsString("bg-amber-50 border-amber-200 text-amber-800")));

        verify(loteServicio).listarLotesParaCronograma(null, false);
        verify(planificacionServicio, never()).buscarPorId(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-17: un fermentador que no existe o no esta activo se ignora con un aviso y se listan todos; el service no recibe ese id")
    void cronograma_fermentadorInexistenteSeIgnora() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL).param("idFermentador", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El fermentador indicado no existe o no est")))
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-01\"")));

        verify(loteServicio).listarLotesParaCronograma(null, false);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-18: una planificacion inexistente se ignora con un aviso: no se destaca ninguna y ningun lote se atenua")
    void cronograma_planificacionInexistenteSeIgnora() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(planificacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"));
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL).param("idPlanificacion", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La planificaci")))
                .andExpect(content().string(containsString("no existe")))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "gantt-barra--atenuada\"")).isZero();
        assertThat(html).doesNotContain("Lote de otra planificaci");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-19: la alerta flash de un movimiento anterior se muestra en la pagina")
    void cronograma_muestraLaAlertaFlash() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL).flashAttr("mensaje", "Lote DEMO-3 movido en la cola").flashAttr("tipo", "success"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lote DEMO-3 movido en la cola")))
                .andExpect(content().string(containsString("bg-teal-50")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-20: la sidebar del gerente de produccion muestra \"Cronograma de Produccion\" (bi-bar-chart-steps) en Operacion, activa en esta pagina")
    void cronograma_laSidebarMuestraLaOpcionActiva() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), anyBoolean())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("href=\"/cronograma-produccion\"").contains("bi-bar-chart-steps");
        assertThat(html).containsPattern("(?s)<a href=\"/cronograma-produccion\"[^>]*bg-amber-500/10 text-amber-500");
        assertThat(html.indexOf("Operaci")).isLessThan(html.indexOf("href=\"/cronograma-produccion\""));
        assertThat(html.indexOf("href=\"/cronograma-produccion\"")).isLessThan(html.indexOf("Administraci"));
    }

    // ==================== POST /cronograma-produccion/lotes/{id}/subir ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-21: subir correctamente delega en el service y redirige al cronograma con la alerta de exito que informa las nuevas fechas")
    void subir_debeRedirigirConAlertaYFechas() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenReturn(movido("DEMO-3", LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 30)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion"))
                .andExpect(flash().attribute("mensaje", "Lote DEMO-3 movido en la cola: ahora del 12/11/2026 al 30/11/2026"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(loteServicio).subirLoteEnCola(3L);
        verify(loteServicio, never()).bajarLoteEnCola(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-22: subir conserva los filtros (fermentador, planificacion e historial) en la URL de redireccion")
    void subir_debeConservarLosFiltros() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenReturn(movido("DEMO-3", LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 30)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf())
                        .param("idFermentador", "2").param("idPlanificacion", "7").param("incluirHistorial", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion?idFermentador=2&idPlanificacion=7&incluirHistorial=true"))
                .andExpect(flash().attribute("tipo", "success"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-23: los filtros invalidos o vacios del POST no viajan en la redireccion y no rompen el movimiento")
    void subir_ignoraFiltrosInvalidos() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenReturn(movido("DEMO-3", LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 30)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf())
                        .param("idFermentador", "abc").param("idPlanificacion", "").param("incluirHistorial", "no"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion"))
                .andExpect(flash().attribute("tipo", "success"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-24: si el service rechaza subir (ya es el primero o no esta pendiente) vuelve al origen con la alerta de advertencia y el mensaje del service")
    void subir_debeMostrarElMotivoDelRechazo() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenThrow(new ReglaNegocioException("Solo se pueden mover lotes en estado Pendiente"));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf()).header("Referer", "/cronograma-produccion?idFermentador=2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion?idFermentador=2"))
                .andExpect(flash().attribute("mensaje", "Solo se pueden mover lotes en estado Pendiente"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-25: subir un lote inexistente vuelve al origen con la alerta de error del service")
    void subir_debeMostrarElErrorSiNoExiste() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/99/subir").with(csrf()).header("Referer", "/cronograma-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /cronograma-produccion/lotes/{id}/bajar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-26: bajar correctamente delega en el service y redirige al cronograma conservando los filtros, con la alerta de exito que informa las nuevas fechas")
    void bajar_debeRedirigirConAlertaFechasYFiltros() throws Exception {
        // Arrange
        when(loteServicio.bajarLoteEnCola(4L)).thenReturn(movido("DEMO-4", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 15)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/4/bajar").with(csrf()).param("idFermentador", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion?idFermentador=2"))
                .andExpect(flash().attribute("mensaje", "Lote DEMO-4 movido en la cola: ahora del 01/12/2026 al 15/12/2026"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(loteServicio).bajarLoteEnCola(4L);
        verify(loteServicio, never()).subirLoteEnCola(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-27: si el service rechaza bajar (es el ultimo de la cola) vuelve al origen con la alerta de advertencia y el mensaje del service")
    void bajar_debeMostrarElMotivoDelRechazo() throws Exception {
        // Arrange
        when(loteServicio.bajarLoteEnCola(4L)).thenThrow(new ReglaNegocioException("El lote ya es el último de la cola de su fermentador"));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/4/bajar").with(csrf()).header("Referer", "/cronograma-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion"))
                .andExpect(flash().attribute("mensaje", "El lote ya es el último de la cola de su fermentador"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-28: bajar un lote inexistente vuelve al origen con la alerta de error del service")
    void bajar_debeMostrarElErrorSiNoExiste() throws Exception {
        // Arrange
        when(loteServicio.bajarLoteEnCola(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/99/bajar").with(csrf()).header("Referer", "/cronograma-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-29: un POST sin token CSRF no mueve nada: es rechazado (redirige) y no llega al service")
    void mover_sinCsrfNoLlegaAlService() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post(URL + "/lotes/3/bajar")).andExpect(status().is3xxRedirection());

        verifyNoInteractions(loteServicio);
    }

    // ==================== GET /cronograma-produccion/buscador/planificaciones ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-30: el buscador devuelve un fragmento (sin layout) con el criterio de receta, el placeholder, las planificaciones y el boton Elegir con su id y texto")
    void buscador_debeDevolverElFragmentoConLasPlanificaciones() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        planificacion(8L, "Stout", EstadoSolicitud.FINALIZADA),
                        planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE))));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/planificaciones"))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/buscador-planificacion :: buscador"))
                .andExpect(content().string(containsString("Buscar planificaci")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pale Ale\"")))
                .andExpect(content().string(containsString("action=\"/cronograma-produccion/buscador/planificaciones\"")))
                .andExpect(content().string(containsString("data-modal-buscar")))
                .andExpect(content().string(containsString("data-id=\"7\"")))
                .andExpect(content().string(containsString("data-texto=\"Plan #7 - Pale Ale\"")))
                .andExpect(content().string(containsString("data-id=\"8\"")))
                .andExpect(content().string(containsString("Litros (L)")))
                .andExpect(content().string(containsString(">600<")))
                .andExpect(content().string(containsString("01/07/2027")))
                .andExpect(content().string(containsString(">Pendiente<")))
                .andExpect(content().string(containsString(">Finalizada<")))
                .andExpect(content().string(containsString("Elegir")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-31: el buscador pide al service todas las planificaciones con la ultima registrada primero (id descendente), sin filtrar por receta, estado ni fecha")
    void buscador_pideLasMasRecientesPrimero() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        mockMvc.perform(get(URL + "/buscador/planificaciones")).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesProduccion(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-32: el criterio de receta filtra por parte del nombre sin distinguir mayusculas ni espacios sobrantes")
    void buscador_filtraPorNombreDeReceta() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        planificacion(9L, "Stout", EstadoSolicitud.PENDIENTE),
                        planificacion(8L, "Pale Ale", EstadoSolicitud.PENDIENTE),
                        planificacion(7L, "Imperial Stout", EstadoSolicitud.PENDIENTE))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/planificaciones").param("receta", " STOUT "))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-id=\"9\"").contains("data-id=\"7\"").doesNotContain("data-id=\"8\"");
        assertThat(html).contains("value=\" STOUT \"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-33: el buscador pagina de a 8 con la ultima registrada arriba; la paginacion conserva el criterio, se pide dentro del modal y la segunda pagina trae el resto")
    void buscador_paginaDeA8ConservandoElCriterio() throws Exception {
        // Arrange: 10 planificaciones de "Pale Ale" (ids 10 a 1, la mas reciente primero)
        List<PlanificacionProduccionResponseDTO> todas = new ArrayList<>();
        for (long id = 10; id >= 1; id--) {
            todas.add(planificacion(id, "Pale Ale", EstadoSolicitud.PENDIENTE));
        }
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any())).thenReturn(new PageImpl<>(todas));

        // Act
        String primera = mockMvc.perform(get(URL + "/buscador/planificaciones").param("receta", "pale"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String segunda = mockMvc.perform(get(URL + "/buscador/planificaciones").param("receta", "pale").param("pagina", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(primera, "data-elegir")).isEqualTo(8);
        assertThat(primera).contains("data-id=\"10\"").contains("data-id=\"3\"").doesNotContain("data-id=\"2\"");
        assertThat(primera).contains("Página <span>1</span> de <span>2</span>").contains("data-modal-get=\"true\"")
                .contains("pagina=1").contains("receta=pale");
        assertThat(cuenta(segunda, "data-elegir")).isEqualTo(2);
        assertThat(segunda).contains("data-id=\"2\"").contains("data-id=\"1\"").contains("pagina=0");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CGC-34: sin resultados el buscador muestra un mensaje claro, y una pagina negativa equivale a la primera")
    void buscador_sinResultadosYPaginaNegativa() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE))));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/planificaciones").param("receta", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron planificaciones con los criterios indicados.")));
        mockMvc.perform(get(URL + "/buscador/planificaciones").param("pagina", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-id=\"7\"")));
    }

    // ==================== Roles ====================

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GERENTE_DE_COMPRAS", "OPERARIO_DE_PRODUCCION", "ADMINISTRADOR"})
    @DisplayName("CP-CGC-35: GERENTE_DE_COMPRAS, OPERARIO_DE_PRODUCCION y ADMINISTRADOR no pueden ver el cronograma ni usar el buscador: redirigen con alerta de error y no llegan a ningun service")
    void roles_noAutorizadosNoPuedenVer(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL).with(user("x").roles(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get(URL + "/buscador/planificaciones").with(user("x").roles(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio, fermentadorServicio, planificacionServicio);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GERENTE_DE_COMPRAS", "OPERARIO_DE_PRODUCCION", "ADMINISTRADOR"})
    @DisplayName("CP-CGC-36: GERENTE_DE_COMPRAS, OPERARIO_DE_PRODUCCION y ADMINISTRADOR no pueden subir ni bajar lotes: redirigen con alerta de error y el service no se invoca")
    void roles_noAutorizadosNoPuedenMover(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(user("x").roles(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/lotes/3/bajar").with(user("x").roles(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio);
    }

    // ==================== Datos de prueba ====================

    /**
     * Simula los fermentadores activos: se pasan de a pares (id, identificador).
     */
    private void fermentadores(Object... idEIdentificador) {
        List<FermentadorResponseDTO> lista = new ArrayList<>();
        for (int i = 0; i < idEIdentificador.length; i += 2) {
            lista.add(FermentadorResponseDTO.builder()
                    .id((Long) idEIdentificador[i])
                    .identificadorInterno((String) idEIdentificador[i + 1])
                    .build());
        }
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(lista));
    }

    /**
     * Etiqueta del boton de mover de un lote (la apertura de su tag), para comprobar si esta desactivado.
     */
    private static String boton(String html, String accion, Long idLote) {
        Matcher m = Pattern.compile("<button[^>]*data-mover=\"" + accion + "\"[^>]*>").matcher(html);
        while (m.find()) {
            if (m.group().contains("data-lote=\"" + idLote + "\"")) {
                return m.group();
            }
        }
        throw new AssertionError("No hay boton " + accion + " del lote " + idLote);
    }

    /**
     * Etiqueta de apertura de la fila de la cola de un lote.
     */
    private static String fila(String html, Long idLote) {
        Matcher m = Pattern.compile("<tr[^>]*data-cola-fila[^>]*>").matcher(html);
        while (m.find()) {
            if (m.group().contains("data-lote=\"" + idLote + "\"")) {
                return m.group();
            }
        }
        throw new AssertionError("No hay fila de cola del lote " + idLote);
    }

    /**
     * Lote con las fechas estimadas indicadas.
     */
    private static LoteCronogramaResponseDTO loteEn(Long id, String identificador, EstadoLote estado, String fermentador,
                                                    Long idPlanificacion, String receta, LocalDate inicio, LocalDate fin) {
        return lote(id, identificador, estado, fermentador, idPlanificacion, receta, 1).toBuilder()
                .fechaInicioEstimada(inicio)
                .fechaFinalizacionEstimada(fin)
                .build();
    }

    /**
     * El contenido de la leyenda del diagrama.
     */
    private static String leyenda(String html) {
        Matcher m = Pattern.compile("(?s)aria-label=\"Leyenda del diagrama\">(.*?)</ul>").matcher(html);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static int cuenta(String html, String texto) {
        return html.split(Pattern.quote(texto), -1).length - 1;
    }

    private static LoteCronogramaResponseDTO movido(String identificador, LocalDate inicio, LocalDate fin) {
        return LoteCronogramaResponseDTO.builder()
                .id(3L)
                .identificadorInterno(identificador)
                .estado(EstadoLote.PENDIENTE)
                .fechaInicioEstimada(inicio)
                .fechaFinalizacionEstimada(fin)
                .build();
    }

    private static PlanificacionProduccionResponseDTO planificacion(Long id, String nombreReceta, EstadoSolicitud estado) {
        return PlanificacionProduccionResponseDTO.builder()
                .id(id)
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1))
                .fechaFinalizacionEstimada(LocalDate.of(2027, 7, 25))
                .cantidadAProducir(600.0)
                .estado(estado)
                .versionReceta(VersionRecetaResponseDTO.builder().id(70L).nombre(nombreReceta).volumenBase(20.0).build())
                .build();
    }

    /**
     * Lote de 300 L cuyo inicio estimado es el 01/07/2027 mas 4 dias por cada orden y dura 4 dias (orden 1: del 01/07 al 05/07).
     */
    private static LoteCronogramaResponseDTO lote(Long id, String identificador, EstadoLote estado, String fermentador,
                                                  Long idPlanificacion, String receta, int orden) {
        LocalDate inicio = LocalDate.of(2027, 7, 1).plusDays((orden - 1) * 6L);
        return LoteCronogramaResponseDTO.builder()
                .id(id)
                .identificadorInterno(identificador)
                .estado(estado)
                .volumenObjetivo(300.0)
                .fechaInicioEstimada(inicio)
                .fechaFinalizacionEstimada(inicio.plusDays(4))
                .fechaInicio(estado == EstadoLote.PENDIENTE ? null : inicio.atTime(8, 0))
                .fechaFinalizacion(estado == EstadoLote.FINALIZADO ? inicio.plusDays(4).atTime(18, 0) : null)
                .fechaCancelacion(estado == EstadoLote.CANCELADO ? inicio.plusDays(2).atTime(10, 0) : null)
                .idPlanificacionProduccion(idPlanificacion)
                .nombreReceta(receta)
                .identificadorInternoFermentador(fermentador)
                .build();
    }
}
