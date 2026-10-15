package com.github.heikyudev.maestrocervecero.presentation.controller.cronograma;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-01: el GERENTE_DE_PRODUCCION ve el cronograma con los filtros, el período, el diagrama (un carril por fermentador, barras con receta, plan y lote) y las colas")
    void cronograma_debeMostrarFiltrosDiagramaYColas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2),
                lote(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));

        // Act + Assert
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/cronograma"))
                .andExpect(content().string(containsString("Cronograma de Producci")))
                // Filtros
                .andExpect(content().string(containsString("action=\"/cronograma-produccion\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("data-buscador-url=\"/cronograma-produccion/buscador/fermentadores\"")))
                .andExpect(content().string(containsString(">Desde</label>")))
                .andExpect(content().string(containsString(">Hasta</label>")))
                .andExpect(content().string(containsString("Buscar")))
                .andExpect(content().string(containsString("Limpiar")))
                // Período
                .andExpect(content().string(containsString("data-cronograma-periodo")))
                .andExpect(content().string(containsString("data-periodo-anterior")))
                .andExpect(content().string(containsString("data-periodo-siguiente")))
                .andExpect(content().string(containsString("data-periodo-hoy")))
                // Diagrama
                .andExpect(content().string(containsString("data-gantt")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-1")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-2")))
                .andExpect(content().string(containsString("Stout - Plan #8 - STOUT-R8-1")))
                .andExpect(content().string(containsString("gantt-color--en-ejecucion")))
                .andExpect(content().string(containsString("gantt-color--pendiente")))
                .andExpect(content().string(containsString("data-gantt-hoy-bandera")))
                .andExpect(content().string(containsString("overflow-x-auto")))
                .andExpect(content().string(containsString("sticky left-0")))
                // Colas
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-01\"")))
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-02\"")))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("Mostrar finalizados anteriores").doesNotContain("incluirHistorial");
        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-02: sin lotes en el período no hay carriles: se muestra el mensaje con el período, sin diagrama, colas, paginador ni botones, y el filtro y la navegación siguen visibles")
    void cronograma_sinLotesEnElPeriodo() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-cronograma-sin-lotes").contains("No hay lotes para mostrar entre " + fmt(desdeDefecto()) + " y " + fmt(hastaDefecto()));
        assertThat(html).doesNotContain("data-gantt>").doesNotContain("data-cola-fermentador").doesNotContain("data-paginacion")
                .doesNotContain("data-mover=");
        assertThat(html).contains("data-buscador-url=\"/cronograma-produccion/buscador/fermentadores\"").doesNotContain("<option");
        assertThat(html).contains("data-periodo-anterior").contains("data-periodo-siguiente").contains("data-periodo-hoy");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-03: sin fermentadores activos ni lotes se muestra el mismo mensaje, el filtro de fermentador sigue siendo el buscador (sin opciones en la página) y no hay diagrama ni colas")
    void cronograma_sinFermentadoresNiLotes() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("No hay lotes para mostrar entre ");
        assertThat(html).contains("data-buscador-url=\"/cronograma-produccion/buscador/fermentadores\"").doesNotContain("<option");
        assertThat(html).doesNotContain("data-gantt>").doesNotContain("data-cola-fermentador");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-04: los fermentadores activos se piden todos, ordenados por identificador, para validar el filtro (no se vuelcan a la página como opciones); también se piden, una vez, los dados de baja con lotes")
    void cronograma_pideTodosLosFermentadoresActivosOrdenados() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadores(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno")).isNotNull();
        assertThat(html).doesNotContain("<option").doesNotContain(">F-01<").doesNotContain(">F-02<");
        verify(fermentadorServicio, times(1)).listarDadosDeBajaConLotes();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-05: el fermentador y el período llegan al service; el filtro y los campos quedan con lo elegido, un solo carril y solo la cola de ese fermentador")
    void cronograma_pasaLosFiltrosAlService() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                finalizadoEn(1L, "A-1", "F-02", d(2018, 5, 2), d(2018, 5, 10))));

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "2").param("desde", "2018-04-21").param("hasta", "2019-01-07"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(2L, d(2018, 4, 21), d(2019, 1, 7));
        assertThat(campo(html, "idFermentador")).isEqualTo("2");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-02");
        assertThat(campo(html, "desde")).isEqualTo("2018-04-21");
        assertThat(campo(html, "hasta")).isEqualTo("2019-01-07");
        assertThat(cuenta(html, "data-cola-fermentador=")).isEqualTo(1);
        assertThat(html).contains("data-cola-fermentador=\"F-02\"").doesNotContain("data-cola-fermentador=\"F-01\"");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-06: no hay casilla de historial: el formulario no la tiene y el parámetro incluirHistorial se ignora sin aviso")
    void cronograma_noHayCasillaDeHistorial() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String sinParametro = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String conParametro = mockMvc.perform(get(URL).param("incluirHistorial", "true")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(sinParametro).doesNotContain("Mostrar finalizados anteriores").doesNotContain("incluirHistorial");
        assertThat(conParametro).doesNotContain("bg-amber-50 border-amber-200 text-amber-800");
        verify(loteServicio, times(2)).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-07: la cola de cada fermentador sigue el orden de los carriles, numera los pendientes y deja desactivado Subir en el primero y Bajar en el ultimo")
    void cronograma_colasConBotonesSubirYBajar() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-08: cada boton es un POST con token CSRF hacia /lotes/{id}/subir o /bajar, con ícono, texto compacto y sin cortarse")
    void cronograma_losBotonesSonFormulariosPostConCsrf() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-09: la cola lista solo los pendientes: el lote en ejecución no tiene fila aparte ni el texto \"no se puede mover\", y los finalizados y cancelados tampoco entran")
    void cronograma_elLoteEnEjecucionVaArribaDeLaColaSinBotones() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 2),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 3),
                lote(4L, "PALE_ALE-R7-4", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", 4)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).doesNotContain("data-cola-en-ejecucion").doesNotContain("no se puede mover");
        assertThat(cuenta(html, "data-cola-fila")).isEqualTo(1);
        assertThat(html).contains("lotes/3/subir").doesNotContain("lotes/1/subir").doesNotContain("lotes/2/subir")
                .doesNotContain("lotes/4/subir");
        assertThat(html).doesNotContain("PALE_ALE-R7-4");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-10: la fila de la cola muestra posicion, lote, receta, planificacion, volumen sin ceros sobrantes y fechas estimadas dd/MM/yyyy")
    void cronograma_laFilaDeLaColaMuestraSusDatos() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote2027(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act + Assert
        mockMvc.perform(get(URL).param("desde", "2027-06-25").param("hasta", "2027-07-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Volumen (L)")))
                .andExpect(content().string(containsString("Inicio")))
                .andExpect(content().string(containsString("Fin")))
                .andExpect(content().string(containsString(">PALE_ALE-R7-1<")))
                .andExpect(content().string(containsString(">Plan #7 - Pale Ale<")))
                .andExpect(content().string(containsString(">300<")))
                .andExpect(content().string(not(containsString("300.0"))))
                .andExpect(content().string(containsString("01/07/2027")))
                .andExpect(content().string(containsString("05/07/2027")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-11: el filtro de planificacion es un campo en blanco con ícono de lista que abre el buscador (no un combo), con placeholder y boton para quitar")
    void cronograma_elFiltroDePlanificacionEsUnSelectorConBuscador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-selector-buscar")))
                .andExpect(content().string(containsString("bi-list-ul")))
                .andExpect(content().string(containsString("data-buscador-url=\"/cronograma-produccion/buscador/planificaciones\"")))
                .andExpect(content().string(containsString("name=\"idPlanificacion\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione planificaci")))
                .andExpect(content().string(containsString("data-selector-limpiar")))
                .andExpect(content().string(not(containsString("data-selector-menu"))));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-12: al destacar una planificacion no se quita ningun lote: se atenuan las barras y filas de cola de las demas y el selector muestra la elegida")
    void cronograma_destacarUnaPlanificacionAtenuaLasDemas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
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
        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-13: sin planificacion destacada ninguna barra ni fila de cola se atenua y la leyenda no menciona el atenuado")
    void cronograma_sinDestacarNoAtenuaNada() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-14: los botones de mover llevan filtros, período y página como campos ocultos para volver al cronograma con la misma vista")
    void cronograma_losBotonesConservanLosFiltros() throws Exception {
        // Arrange
        fermentadores(3L, "F-03");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote2027(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "3").param("idPlanificacion", "7")
                        .param("desde", "2018-04-21").param("hasta", "2019-01-07").param("pagina", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: un par de formularios (subir y bajar) con los cinco campos ocultos cada uno
        Matcher formularios = Pattern.compile("(?s)<form[^>]*lotes/1/(subir|bajar)\"[^>]*>(.*?)</form>").matcher(html);
        int encontrados = 0;
        while (formularios.find()) {
            encontrados++;
            String cuerpo = formularios.group(2);
            for (String[] campo : new String[][]{{"idFermentador", "3"}, {"idPlanificacion", "7"}, {"desde", "2018-04-21"}, {"hasta", "2019-01-07"}, {"pagina", "1"}}) {
                assertThat(cuerpo).containsPattern("name=\"" + campo[0] + "\"[^>]*value=\"" + campo[1] + "\"|value=\"" + campo[1] + "\"[^>]*name=\"" + campo[0] + "\"");
            }
        }
        assertThat(encontrados).isEqualTo(2);
        verify(loteServicio).listarLotesParaCronograma(3L, d(2018, 4, 21), d(2019, 1, 7));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-15: sin filtros activos los botones de mover no llevan campos ocultos de filtro")
    void cronograma_sinFiltrosLosBotonesNoLlevanCamposOcultos() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        Matcher formulario = Pattern.compile("(?s)<form[^>]*lotes/1/subir\"[^>]*>(.*?)</form>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group(1)).doesNotContain("name=\"idFermentador\"").doesNotContain("name=\"idPlanificacion\"")
                .doesNotContain("name=\"desde\"").doesNotContain("name=\"hasta\"").doesNotContain("name=\"pagina\"");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-37: el atraso del lote en ejecución se ve en su barra y la cola marca los pendientes corridos por ese atraso, con la fecha planificada en el tooltip; sin corrimiento no se muestra ninguna marca")
    void cronograma_colaMarcaAtrasoYDesplazamiento() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        LoteCronogramaResponseDTO enEjecucion = lote2027(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1)
                .toBuilder().diasDeAtraso(2).build();
        LoteCronogramaResponseDTO corrido = lote2027(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2)
                .toBuilder().diasDeDesplazamiento(2).build();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                enEjecucion, corrido,
                lote2027(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2027-06-25").param("hasta", "2027-07-31")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Atrasado 2 d").contains("Corrido 2 d");
        assertThat(html).contains("Planificado para el 07/07, corrido 2 d");
        assertThat(html).doesNotContain("data-cola-atraso");
        assertThat(html.split("data-cola-desplazado", -1)).hasSize(2);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-38: la leyenda explica los tres colores y las marcas (Pendiente, En ejecución, Finalizado, Limpieza y Hoy) y suma el atenuado solo al destacar una planificación")
    void cronograma_laLeyendaExplicaLosTresColoresYLasMarcas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String sinDestacar = leyenda(mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String destacando = leyenda(mockMvc.perform(get(URL).param("idPlanificacion", "7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        // Assert
        assertThat(sinDestacar.indexOf("Pendiente")).isGreaterThanOrEqualTo(0);
        assertThat(sinDestacar.indexOf("Pendiente")).isLessThan(sinDestacar.indexOf("En ejecuci"));
        assertThat(sinDestacar.indexOf("En ejecuci")).isLessThan(sinDestacar.indexOf("Finalizado"));
        assertThat(sinDestacar.indexOf("Finalizado")).isLessThan(sinDestacar.indexOf("Limpieza"));
        assertThat(sinDestacar.indexOf("Limpieza")).isLessThan(sinDestacar.indexOf("Hoy"));
        assertThat(sinDestacar).doesNotContain("Cancelado").doesNotContain("Atraso").doesNotContain("Desplazado")
                .doesNotContain("transcurrido").doesNotContain("Inicio y fin estimados")
                .doesNotContain("Lote de otra planificaci");
        assertThat(destacando).contains("Lote de otra planificaci");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-39: los lotes cancelados no se dibujan en el diagrama ni entran en la cola")
    void cronograma_losLotesCanceladosNoSeDibujan() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-01", 7L, "Pale Ale", 2)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Pale Ale - Plan #7 - PALE_ALE-R7-1");
        assertThat(html).doesNotContain("Pale Ale - Plan #7 - PALE_ALE-R7-2").doesNotContain("gantt-color--cancelado");
        assertThat(cuenta(html, "gantt-barra focus:outline-none")).isEqualTo(1);
        assertThat(cuenta(html, "data-cola-fila")).isEqualTo(1);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-40: el atraso del lote en ejecución se informa en su barra con la etiqueta \"Atrasado 2 días\", sin color ni tramo aparte")
    void cronograma_elAtrasoSeInformaEnLaBarra() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        LoteCronogramaResponseDTO atrasado = lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1)
                .toBuilder().diasDeAtraso(2).build();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-41: un pendiente corrido por atraso se dibuja azul en su posición corrida, sin marca propia: lo dice el tooltip y la fila de la cola")
    void cronograma_elPendienteCorridoNoLlevaMarcaEnLaBarra() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        LoteCronogramaResponseDTO corrido = lote2027(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2)
                .toBuilder().diasDeDesplazamiento(2).build();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(corrido));

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2027-06-25").param("hasta", "2027-07-31")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("gantt-color--pendiente");
        assertThat(html).doesNotContain("gantt-color--desplazado").doesNotContain("data-gantt-desplazado").doesNotContain("Desplazado");
        assertThat(html).contains("Planificado para el 07/07, corrido 2 d");
        assertThat(html).contains("Corrido 2 d");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-42: la limpieza se dibuja solo donde queda un día libre futuro entre lotes vivos (un único bloque, en el carril de F-01)")
    void cronograma_dibujaLaLimpiezaSoloDondeQuedaUnDiaLibre() throws Exception {
        // Arrange
        LocalDate hoy = LocalDate.now();
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(9), hoy.plusDays(12)),
                loteEn(3L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(4L, "STOUT-R8-2", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", hoy.plusDays(8), hoy.plusDays(12))));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "data-gantt-limpieza") - cuenta(html, "data-gantt-limpieza-icono")).isEqualTo(1);
        assertThat(html).contains("Limpieza de equipamientos");
        assertThat(html.indexOf("data-gantt-limpieza")).isLessThan(html.indexOf("Stout - Plan #8 - STOUT-R8-1"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-76: el bloque de limpieza es celeste (gantt-color--limpieza), distinto del azul de lo planificado, y se reduce a solo color si es muy angosto")
    void cronograma_elBloqueDeLimpiezaEsCelesteYSeReduceASoloColor() throws Exception {
        // Arrange
        String html = cronogramaConUnaLimpieza();

        // Act
        String bloque = bloqueDeLimpieza(html);

        // Assert
        assertThat(bloque).contains("gantt-color--limpieza").doesNotContain("gantt-color--pendiente");
        assertThat(html).contains(".gantt-color--limpieza {").contains("container-type: inline-size")
                .contains("@container (max-width: 15px)");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-77: el bloque de limpieza lleva un icono de destellos decorativo (data-gantt-limpieza-icono, bi-stars, aria-hidden)")
    void cronograma_elBloqueDeLimpiezaLlevaUnIconoDeDestellos() throws Exception {
        // Arrange
        String html = cronogramaConUnaLimpieza();

        // Act
        String bloque = bloqueDeLimpieza(html);

        // Assert
        assertThat(cuenta(bloque, "data-gantt-limpieza-icono")).isEqualTo(1);
        assertThat(bloque).containsPattern("<i[^>]*class=\"[^\"]*bi bi-stars[^\"]*\"[^>]*data-gantt-limpieza-icono[^>]*aria-hidden=\"true\"")
                .contains("role=\"img\"").contains("Limpieza de equipamientos");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-78: la leyenda muestra la limpieza con la misma muestra celeste y el icono de destellos, aunque no haya limpiezas dibujadas")
    void cronograma_laLeyendaMuestraLaLimpiezaConElIconoDeDestellos() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String leyenda = leyenda(mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        // Assert
        assertThat(leyenda).containsPattern("gantt-color--limpieza\"><i[^>]*bi bi-stars[^>]*data-gantt-leyenda-limpieza-icono[^>]*aria-hidden=\"true\"")
                .contains("Limpieza de equipamientos");
    }

    private String cronogramaConUnaLimpieza() throws Exception {
        LocalDate hoy = LocalDate.now();
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(9), hoy.plusDays(12))));
        return mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static String bloqueDeLimpieza(String html) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?s)<div[^>]*data-gantt-limpieza(?![-\\w]).*?</div>").matcher(html);
        assertThat(m.find()).isTrue();
        return m.group();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-45: el filtro se rotula \"Fermentador\" y es un campo en blanco con ícono de lista que abre el buscador (no un combo), con el parámetro idFermentador, placeholder y botón para quitar")
    void cronograma_elFiltroSeRotulaFermentador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("mb-1\">Fermentador</span>");
        assertThat(html).contains("data-buscador-url=\"/cronograma-produccion/buscador/fermentadores\"")
                .contains("name=\"idFermentador\"").contains("data-selector-buscar").contains("bi-list-ul")
                .contains("placeholder=\"Seleccione fermentador\"").contains("data-selector-limpiar")
                .doesNotContain("id=\"idFermentador\"").doesNotContain("<select").doesNotContain("<option")
                .doesNotContain("data-selector-menu");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-46: el carril se rotula \"F-01\", la columna del diagrama \"Fermentador\" y la cola \"Cola pendiente de F-01\"")
    void cronograma_rotulaLosCarrilesYLasColasConElFermentador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains(">F-01<");
        assertThat(html).containsPattern("uppercase tracking-wider\">Fermentador</div>");
        assertThat(html).containsPattern("Cola pendiente de\\s*<span[^>]*>F-01</span>");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-43: el código completo del lote se muestra en la barra, su tooltip, el bloque \"En ejecución\" y la fila de la cola")
    void cronograma_muestraElCodigoCompletoDelLote() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PILSEN_CLASICA-R4-3", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pilsen", 1),
                lote(2L, "PILSEN_CLASICA-R4-4", EstadoLote.PENDIENTE, "F-01", 7L, "Pilsen", 2)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Pilsen - Plan #7 - PILSEN_CLASICA-R4-3").contains("PILSEN_CLASICA-R4-3 - En ejecuci");
        assertThat(html).doesNotContain("data-cola-en-ejecucion");
        assertThat(html).containsPattern("(?s)data-cola-fila.*?>PILSEN_CLASICA-R4-4<");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-44: no se dibuja la limpieza anterior a hoy ni la que sigue a un lote finalizado; las barras se dibujan igual")
    void cronograma_noDibujaLimpiezasEnElPasadoNiTrasFinalizados() throws Exception {
        // Arrange
        LocalDate hoy = LocalDate.now();
        fermentadores(1L, "F-01", 2L, "F-02");
        LoteCronogramaResponseDTO finalizado = loteEn(3L, "STOUT-R8-1", EstadoLote.FINALIZADO, "F-02", 8L, "Stout", hoy.minusDays(10), hoy.minusDays(6))
                .toBuilder().fechaInicio(hoy.minusDays(10).atTime(8, 0)).fechaFinalizacion(hoy.minusDays(6).atTime(18, 0)).build();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.minusDays(10), hoy.minusDays(6)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.minusDays(4), hoy.minusDays(1)),
                finalizado,
                loteEn(4L, "STOUT-R8-2", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", hoy.plusDays(3), hoy.plusDays(6))));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "data-gantt-limpieza")).isZero();
        assertThat(cuenta(html, "gantt-barra focus:outline-none")).isEqualTo(4);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-16: parametros invalidos no rompen la pagina: se ignoran, se avisa con una alerta de advertencia y el service se invoca sin filtros")
    void cronograma_parametrosInvalidosSeIgnoran() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL).param("idFermentador", "abc").param("idPlanificacion", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/cronograma"))
                .andExpect(content().string(containsString("El fermentador indicado no es v")))
                .andExpect(content().string(containsString("La planificaci")))
                .andExpect(content().string(containsString("bg-amber-50 border-amber-200 text-amber-800")));

        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
        verify(planificacionServicio, never()).buscarPorId(any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-17: un fermentador que no existe o no esta activo se ignora con un aviso y se listan todos; el service no recibe ese id")
    void cronograma_fermentadorInexistenteSeIgnora() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act + Assert
        mockMvc.perform(get(URL).param("idFermentador", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El fermentador indicado no existe o no est")))
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-01\"")));

        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-18: una planificacion inexistente se ignora con un aviso: no se destaca ninguna y ningun lote se atenua")
    void cronograma_planificacionInexistenteSeIgnora() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(planificacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-19: la alerta flash de un movimiento anterior se muestra en la pagina")
    void cronograma_muestraLaAlertaFlash() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get(URL).flashAttr("mensaje", "Lote DEMO-3 movido correctamente").flashAttr("tipo", "success"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lote DEMO-3 movido correctamente")))
                .andExpect(content().string(containsString("bg-teal-50")));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-20: la sidebar del gerente de produccion muestra \"Cronograma de Produccion\" (bi-bar-chart-steps) en Operacion, activa en esta pagina")
    void cronograma_laSidebarMuestraLaOpcionActiva() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("href=\"/cronograma-produccion\"").contains("bi-bar-chart-steps");
        assertThat(html).containsPattern("(?s)<a href=\"/cronograma-produccion\"[^>]*bg-amber-500/10 text-amber-500");
        assertThat(html.indexOf("Operaci")).isLessThan(html.indexOf("href=\"/cronograma-produccion\""));
        assertThat(html.indexOf("href=\"/cronograma-produccion\"")).isLessThan(html.indexOf("Administraci"));
    }

    // ==================== Período ====================

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-47: sin parametros el periodo es del hoy - 15 al hoy + 60 (76 dias): llega al service, llena los campos y el texto, sin alerta y con la linea de hoy")
    void periodo_porDefecto() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
        assertThat(campo(html, "desde")).isEqualTo(desdeDefecto().toString());
        assertThat(campo(html, "hasta")).isEqualTo(hastaDefecto().toString());
        assertThat(html).contains("Período: " + fmt(desdeDefecto()) + " al " + fmt(hastaDefecto()) + " (76 días)");
        assertThat(html).doesNotContain(ALERTA_ADVERTENCIA);
        assertThat(html).containsPattern("data-gantt-hoy(?!-)").contains("data-gantt-hoy-bandera");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-48: un periodo explicito se respeta: llega al service, el texto dice 262 dias, se dibuja la barra verde y no hay linea ni bandera de hoy")
    void periodo_explicito() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                finalizadoEn(1L, "A-1", "F-01", d(2018, 5, 2), d(2018, 5, 10))));

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2018-04-21").param("hasta", "2019-01-07"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(null, d(2018, 4, 21), d(2019, 1, 7));
        assertThat(html).contains("Período: 21/04/2018 al 07/01/2019 (262 días)");
        assertThat(campo(html, "desde")).isEqualTo("2018-04-21");
        assertThat(campo(html, "hasta")).isEqualTo("2019-01-07");
        assertThat(html).contains("gantt-color--finalizado").doesNotContain("data-gantt-hoy-bandera").doesNotContainPattern("data-gantt-hoy(?!-)");
        assertThat(html).doesNotContain(ALERTA_ADVERTENCIA);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-49: una fecha con formato invalido (o inexistente) invalida el periodo: alerta de advertencia y periodo por defecto completo, tambien cuando el otro extremo era valido")
    void periodo_fechaInvalida() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        String[][] casos = {{"21/04/2018", "2019-01-07"}, {"abc", "xyz"}, {"2018-02-30", "2019-01-07"}, {"2018-04-21", "07-01-2019"}};

        for (String[] caso : casos) {
            // Act
            String html = mockMvc.perform(get(URL).param("desde", caso[0]).param("hasta", caso[1]))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

            // Assert
            assertThat(html).contains("El período indicado no es válido, se muestra el período por defecto").contains(ALERTA_ADVERTENCIA);
            assertThat(campo(html, "desde")).isEqualTo(desdeDefecto().toString());
            assertThat(campo(html, "hasta")).isEqualTo(hastaDefecto().toString());
        }
        verify(loteServicio, times(4)).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-50: un hasta anterior al desde se rechaza con una alerta y se usa el periodo por defecto")
    void periodo_invertido() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act + Assert
        mockMvc.perform(get(URL).param("desde", "2019-01-07").param("hasta", "2018-04-21"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La fecha de fin del período no puede ser anterior a la fecha de inicio, se muestra el período por defecto")));

        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-51: el tope del periodo es de 1.826 dias contando ambos extremos (5 anios): 1826 se acepta y 1827 se rechaza con una alerta")
    void periodo_topeMaximo() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act
        String aceptado = mockMvc.perform(get(URL).param("desde", "2018-01-01").param("hasta", "2022-12-31"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String rechazado = mockMvc.perform(get(URL).param("desde", "2018-01-01").param("hasta", "2023-01-01"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(null, d(2018, 1, 1), d(2022, 12, 31));
        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
        assertThat(aceptado).contains("(1826 días)").doesNotContain(ALERTA_ADVERTENCIA);
        assertThat(rechazado).contains("El período no puede superar los 1.826 días (5 años), se muestra el período por defecto");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-52: con un solo extremo el otro se completa a 75 dias de distancia; en blanco equivale a no informado; sin alerta")
    void periodo_unSoloExtremo() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act
        String soloDesde = mockMvc.perform(get(URL).param("desde", "2018-04-21")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String soloHasta = mockMvc.perform(get(URL).param("hasta", "2019-01-07")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String enBlanco = mockMvc.perform(get(URL).param("desde", " ").param("hasta", "")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(null, d(2018, 4, 21), d(2018, 7, 5));
        verify(loteServicio).listarLotesParaCronograma(null, d(2018, 10, 24), d(2019, 1, 7));
        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
        assertThat(campo(soloDesde, "hasta")).isEqualTo("2018-07-05");
        assertThat(campo(soloHasta, "desde")).isEqualTo("2018-10-24");
        assertThat(campo(enBlanco, "desde")).isEqualTo(desdeDefecto().toString());
        assertThat(List.of(soloDesde, soloHasta, enBlanco)).allSatisfy(html -> assertThat(html).doesNotContain(ALERTA_ADVERTENCIA));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-53: un periodo de un solo dia es valido: el texto dice \"(1 día)\"")
    void periodo_unSoloDia() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2018-04-21").param("hasta", "2018-04-21"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(null, d(2018, 4, 21), d(2018, 4, 21));
        assertThat(html).contains("Período: 21/04/2018 al 21/04/2018 (1 día)").doesNotContain(ALERTA_ADVERTENCIA);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-54: el formulario envia por GET hacia el cronograma, novalidate, con los campos Desde y Hasta de tipo fecha con el periodo y sin campo pagina; Limpiar no lleva parametros")
    void periodo_formulario() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2018-04-21").param("hasta", "2019-01-07"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        Matcher formulario = Pattern.compile("(?s)<form[^>]*data-cronograma-filtros[^>]*>(.*?)</form>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("method=\"get\"").contains("action=\"/cronograma-produccion\"").contains("novalidate");
        assertThat(formulario.group(1)).containsPattern("<input type=\"date\"[^>]*name=\"desde\"").containsPattern("<input type=\"date\"[^>]*name=\"hasta\"")
                .contains(">Desde</label>").contains(">Hasta</label>").doesNotContain("name=\"pagina\"");
        assertThat(campo(html, "desde")).isEqualTo("2018-04-21");
        assertThat(formulario.group(1)).containsPattern("<a href=\"/cronograma-produccion\"[^>]*>Limpiar</a>");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-55: Anterior y Siguiente desplazan el periodo su misma longitud, sin solaparse ni dejar dias en medio, y no llevan pagina")
    void periodo_anteriorYSiguiente() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act
        String explicito = mockMvc.perform(get(URL).param("desde", "2018-04-21").param("hasta", "2019-01-07").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String porDefecto = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        LocalDate hoy = LocalDate.now();
        assertThat(href(explicito, "data-periodo-anterior")).isEqualTo("/cronograma-produccion?desde=2017-08-02&hasta=2018-04-20");
        assertThat(href(explicito, "data-periodo-siguiente")).isEqualTo("/cronograma-produccion?desde=2019-01-08&hasta=2019-09-26");
        assertThat(href(porDefecto, "data-periodo-anterior")).isEqualTo("/cronograma-produccion?desde=" + hoy.minusDays(91) + "&hasta=" + hoy.minusDays(16));
        assertThat(href(porDefecto, "data-periodo-siguiente")).isEqualTo("/cronograma-produccion?desde=" + hoy.plusDays(61) + "&hasta=" + hoy.plusDays(136));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-56: Hoy restaura el periodo por defecto: apunta al cronograma sin desde, hasta ni pagina, con o sin periodo explicito")
    void periodo_hoy() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");

        // Act
        String explicito = mockMvc.perform(get(URL).param("desde", "2018-04-21").param("hasta", "2019-01-07").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String porDefecto = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(href(explicito, "data-periodo-hoy")).isEqualTo("/cronograma-produccion");
        assertThat(href(porDefecto, "data-periodo-hoy")).isEqualTo("/cronograma-produccion");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-57: la navegacion conserva los filtros y vuelve a la primera pagina: nunca lleva pagina")
    void periodo_laNavegacionConservaLosFiltrosYNoLaPagina() throws Exception {
        // Arrange
        fermentadoresActivos(15);
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(15, d(2018, 5, 2)));

        // Act
        String a = mockMvc.perform(get(URL).param("idPlanificacion", "7").param("desde", "2018-04-21").param("hasta", "2019-01-07").param("pagina", "2"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String b = mockMvc.perform(get(URL).param("idFermentador", "2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String c = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        for (String atributo : List.of("data-periodo-anterior", "data-periodo-siguiente", "data-periodo-hoy")) {
            assertThat(href(a, atributo)).contains("idPlanificacion=7").doesNotContain("pagina=");
            assertThat(href(b, atributo)).contains("idFermentador=2").doesNotContain("pagina=");
            assertThat(href(c, atributo)).doesNotContain("idFermentador").doesNotContain("idPlanificacion").doesNotContain("pagina=");
        }
        assertThat(href(a, "data-periodo-anterior")).contains("desde=").contains("hasta=");
        assertThat(href(a, "data-periodo-hoy")).doesNotContain("desde=").doesNotContain("hasta=");
    }

    // ==================== Paginación, filtro de fermentador y vacío ====================

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-58: hasta 5 fermentadores caben en una pagina: 3 carriles y 3 colas, \"Pagina 1 de 1\", \"3 fermentadores\" y ambos botones de pagina desactivados")
    void paginacion_unaSolaPagina() throws Exception {
        // Arrange
        fermentadoresActivos(3);
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(3, LocalDate.now().plusDays(1)));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(carriles(html)).isEqualTo(3);
        assertThat(cuenta(html, "data-cola-fermentador=")).isEqualTo(3);
        assertThat(html).contains("Página 1 de 1").contains("3 fermentadores");
        assertThat(html).doesNotContainPattern("<a[^>]*data-pagina-anterior").doesNotContainPattern("<a[^>]*data-pagina-siguiente");
        assertThat(html).containsPattern("<span[^>]*data-pagina-anterior").containsPattern("<span[^>]*data-pagina-siguiente");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-59: los carriles se reparten de a 5: la pagina 1 muestra F-01 a F-05 y la 5 muestra F-21 a F-25, con los enlaces de pagina que corresponden")
    void paginacion_variasPaginas() throws Exception {
        // Arrange
        fermentadoresActivos(25);
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(25, LocalDate.now().plusDays(1)));

        // Act
        String pagina1 = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String ultima = mockMvc.perform(get(URL).param("pagina", "5")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(colas(pagina1)).containsExactlyElementsOf(fermentadoresDe(1, 5));
        assertThat(pagina1).contains("Página 1 de 5").contains("25 fermentadores");
        assertThat(pagina1).containsPattern("<span[^>]*data-pagina-anterior");
        assertThat(href(pagina1, "data-pagina-siguiente")).isEqualTo("/cronograma-produccion?pagina=2");
        assertThat(colas(ultima)).containsExactlyElementsOf(fermentadoresDe(21, 25));
        assertThat(ultima).contains("Página 5 de 5");
        assertThat(href(ultima, "data-pagina-anterior")).isEqualTo("/cronograma-produccion?pagina=4");
        assertThat(ultima).containsPattern("<span[^>]*data-pagina-siguiente");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-60: orden estable y paginas contiguas: aunque el service entregue los lotes en orden inverso, la pagina 1 es F-01 a F-05, la 2 F-06 a F-10 y la 3 F-11 y F-12, sin repetir ni omitir, y pedir la misma pagina dos veces da lo mismo")
    void paginacion_ordenEstable() throws Exception {
        // Arrange
        fermentadoresActivos(12);
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>(lotesDeFermentadores(12, LocalDate.now().plusDays(1)));
        java.util.Collections.reverse(lotes);
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotes);

        // Act
        String pagina1 = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String pagina1Otra = mockMvc.perform(get(URL).param("pagina", "1")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String pagina2 = mockMvc.perform(get(URL).param("pagina", "2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String pagina3 = mockMvc.perform(get(URL).param("pagina", "3")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(colas(pagina1)).containsExactlyElementsOf(fermentadoresDe(1, 5));
        assertThat(colas(pagina2)).containsExactlyElementsOf(fermentadoresDe(6, 10));
        assertThat(colas(pagina3)).containsExactly("F-11", "F-12");
        assertThat(colas(pagina1Otra)).isEqualTo(colas(pagina1));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-61: una pagina invalida o fuera de rango se corrige y se avisa, salvo que no haya paginas")
    void paginacion_paginaInvalidaOFueraDeRango() throws Exception {
        // Arrange
        fermentadoresActivos(25);
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(25, LocalDate.now().plusDays(1)));

        // Act
        String fueraDeRango = mockMvc.perform(get(URL).param("pagina", "9")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String enBlanco = mockMvc.perform(get(URL).param("pagina", " ")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(fueraDeRango).contains("Página 5 de 5").contains("La página indicada no existe, se muestra la última");
        for (String invalida : List.of("0", "-2", "abc")) {
            String html = mockMvc.perform(get(URL).param("pagina", invalida)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(html).contains("Página 1 de 5").contains("La página indicada no es válida, se muestra la primera");
        }
        assertThat(enBlanco).contains("Página 1 de 5").doesNotContain(ALERTA_ADVERTENCIA);

        // Sin lotes en el período no hay páginas y no se avisa
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());
        String vacio = mockMvc.perform(get(URL).param("pagina", "5")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(vacio).contains("No hay lotes para mostrar entre ").doesNotContain(ALERTA_ADVERTENCIA);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-62: con 50 fermentadores el diagrama sigue acotado: la pagina 1 tiene 5 carriles y 5 colas (\"Pagina 1 de 10\", \"50 fermentadores\") y la 10 muestra F-46 a F-50")
    void paginacion_cincuentaFermentadores() throws Exception {
        // Arrange
        fermentadoresActivos(50);
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(50, LocalDate.now().plusDays(1)));

        // Act
        String pagina1 = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String pagina10 = mockMvc.perform(get(URL).param("pagina", "10")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(carriles(pagina1)).isEqualTo(5);
        assertThat(colas(pagina1)).containsExactlyElementsOf(fermentadoresDe(1, 5));
        assertThat(pagina1).contains("Página 1 de 10").contains("50 fermentadores").doesNotContain("data-cola-fermentador=\"F-06\"").doesNotContain("data-cola-fermentador=\"F-50\"");
        assertThat(pagina1).doesNotContain("<option");
        assertThat(colas(pagina10)).containsExactlyElementsOf(fermentadoresDe(46, 50));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-63: los fermentadores sin lotes en el periodo no ocupan carril: solo F-03, F-17 y F-42 (F-08 con un pendiente posterior al periodo no)")
    void paginacion_losFermentadoresSinLotesNoOcupanCarril() throws Exception {
        // Arrange
        fermentadoresActivos(50);
        LocalDate hoy = LocalDate.now();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(1L, "A-3", EstadoLote.PENDIENTE, "F-03", 7L, "Pale Ale", hoy.plusDays(1), hoy.plusDays(5)),
                loteEn(2L, "A-17", EstadoLote.PENDIENTE, "F-17", 7L, "Pale Ale", hoy.plusDays(1), hoy.plusDays(5)),
                loteEn(3L, "A-42", EstadoLote.PENDIENTE, "F-42", 7L, "Pale Ale", hoy.plusDays(1), hoy.plusDays(5)),
                loteEn(4L, "A-8", EstadoLote.PENDIENTE, "F-08", 7L, "Pale Ale", hoy.plusDays(120), hoy.plusDays(125))));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Página 1 de 1").contains("3 fermentadores");
        assertThat(colas(html)).containsExactly("F-03", "F-17", "F-42");
        assertThat(carriles(html)).isEqualTo(3);
        assertThat(html).doesNotContain("<option");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-64: el enlace de pagina conserva la planificacion y el periodo pedidos, no lleva el fermentador que no se pidio y suma la pagina")
    void paginacion_losEnlacesConservanElResto() throws Exception {
        // Arrange
        fermentadoresActivos(25);
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(25, d(2018, 5, 2)));

        // Act
        String html = mockMvc.perform(get(URL).param("idPlanificacion", "7").param("desde", "2018-04-21").param("hasta", "2019-01-07"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(href(html, "data-pagina-siguiente")).isEqualTo("/cronograma-produccion?idPlanificacion=7&desde=2018-04-21&hasta=2019-01-07&pagina=2");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-65: con el filtro de fermentador solo se ve ese fermentador: llega al service, un carril, su cola, \"Pagina 1 de 1\" y \"1 fermentador\"")
    void filtro_unFermentador() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(2L, desdeDefecto(), hastaDefecto());
        assertThat(carriles(html)).isEqualTo(1);
        assertThat(colas(html)).containsExactly("F-02");
        assertThat(html).contains("Página 1 de 1").contains("1 fermentador").doesNotContain("1 fermentadores");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-66: el fermentador elegido conserva su carril aunque no tenga lotes en el periodo: carril vacio, mensaje de vacio y su cola sin pendientes")
    void filtro_fermentadorSinLotesEnElPeriodo() throws Exception {
        // Arrange: el service filtra por el fermentador, asi que no devuelve nada
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-gantt-carril-vacio").contains("Sin lotes en este per");
        assertThat(html).contains("No hay lotes para mostrar entre " + fmt(desdeDefecto()) + " y " + fmt(hastaDefecto())).contains("data-cronograma-sin-lotes");
        assertThat(colas(html)).containsExactly("F-02");
        assertThat(html).contains("No hay lotes pendientes en este fermentador");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-67: fermentador, planificacion y periodo se combinan sin quitarse entre si: un carril con las dos barras (la del otro plan atenuada) y los enlaces conservan ambos filtros")
    void filtro_combinado() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote2027(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", 1),
                lote2027(2L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 2)));

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "2").param("idPlanificacion", "7")
                        .param("desde", "2027-06-25").param("hasta", "2027-07-31"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(2L, d(2027, 6, 25), d(2027, 7, 31));
        assertThat(carriles(html)).isEqualTo(1);
        assertThat(cuenta(html, "gantt-barra focus:outline-none")).isEqualTo(2);
        assertThat(cuenta(html, "gantt-barra--atenuada\"")).isEqualTo(1);
        assertThat(fila(html, 2L)).contains("opacity-50");
        assertThat(html).contains("Plan #7 - Pale Ale").contains("value=\"2\"");
        assertThat(campo(html, "idFermentador")).isEqualTo("2");
        assertThat(campo(html, "desde")).isEqualTo("2027-06-25");
        for (String atributo : List.of("data-periodo-anterior", "data-periodo-siguiente", "data-periodo-hoy")) {
            assertThat(href(html, atributo)).contains("idFermentador=2").contains("idPlanificacion=7");
        }
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-68: los lotes solo fuera del periodo no generan carril ni cola: mensaje de vacio, sin diagrama, paginador ni botones")
    void periodo_lotesSoloFueraDelPeriodo() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        LocalDate hoy = LocalDate.now();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(120), hoy.plusDays(125)),
                loteEn(2L, "A-2", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", hoy.plusDays(130), hoy.plusDays(135))
                        .toBuilder().fechaInicio(hoy.plusDays(130).atTime(8, 0)).build()));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("No hay lotes para mostrar entre " + fmt(desdeDefecto()) + " y " + fmt(hastaDefecto()));
        assertThat(html).doesNotContain("data-gantt>").doesNotContain("data-cola-fermentador").doesNotContain("data-paginacion").doesNotContain("data-mover=");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-69: la cola no depende del periodo: el carril dibuja solo las barras del periodo y la cola lista los tres pendientes, con posiciones 1 a 3 y sus botones")
    void periodo_laColaListaTodosLosPendientes() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        LocalDate hoy = LocalDate.now();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                finalizadoEn(1L, "A-1", "F-01", hoy.minusDays(10), hoy.minusDays(6)),
                loteEn(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(2), hoy.plusDays(6)),
                loteEn(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(120), hoy.plusDays(125)),
                loteEn(4L, "A-4", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", hoy.plusDays(127), hoy.plusDays(132))));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cuenta(html, "gantt-barra focus:outline-none")).isEqualTo(2);
        assertThat(cuenta(html, "data-cola-fila")).isEqualTo(3);
        assertThat(html).contains("lotes/2/subir").contains("lotes/3/bajar").contains("lotes/4/subir");
        assertThat(boton(html, "subir", 2L)).contains("disabled=\"disabled\"");
        assertThat(boton(html, "bajar", 4L)).contains("disabled=\"disabled\"");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-70: la marca de hoy solo existe si hoy cae dentro del periodo: con el periodo por defecto si, con un periodo pasado o futuro no")
    void periodo_hoyDentroOFuera() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        LocalDate hoy = LocalDate.now();
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 1)));

        // Act
        String a = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String b = mockMvc.perform(get(URL).param("desde", "2018-04-21").param("hasta", "2019-01-07")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String c = mockMvc.perform(get(URL).param("desde", hoy.plusDays(30).toString()).param("hasta", hoy.plusDays(60).toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(a).containsPattern("data-gantt-hoy(?!-)").contains("data-gantt-hoy-bandera");
        assertThat(b).doesNotContainPattern("data-gantt-hoy").doesNotContain("data-gantt-hoy-bandera");
        assertThat(c).doesNotContainPattern("data-gantt-hoy").doesNotContain("data-gantt-hoy-bandera");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-71: las barras recortadas lo indican: chevron y atributo del lado que se recorta y la linea correspondiente en el tooltip; la barra que cabe entera no tiene ninguno")
    void periodo_barrasRecortadas() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(1L, "A-1", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", d(2027, 6, 25), d(2027, 7, 5)),
                loteEn(2L, "A-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", d(2027, 7, 15), d(2027, 7, 25)),
                loteEn(3L, "A-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", d(2027, 7, 8), d(2027, 7, 10))));

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2027-07-01").param("hasta", "2027-07-20"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        String primera = barra(html, "A-1 - Pendiente");
        String segunda = barra(html, "A-2 - Pendiente");
        String tercera = barra(html, "A-3 - Pendiente");
        assertThat(primera).contains("data-gantt-continua-antes").doesNotContain("data-gantt-continua-despues").contains("Continúa antes del período")
                .contains("Inicio estimado: 25/06/2027");
        assertThat(segunda).contains("data-gantt-continua-despues").doesNotContain("data-gantt-continua-antes").contains("Continúa después del período")
                .contains("Fin estimado: 25/07/2027");
        assertThat(tercera).doesNotContain("data-gantt-continua").doesNotContain("Continúa");
        assertThat(cuenta(html, "bi-chevron-left")).isEqualTo(2);
        assertThat(cuenta(html, "bi-chevron-right")).isEqualTo(2);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-72: armar la pagina no dispara consultas repetidas: una consulta de lotes, una de fermentadores y a lo sumo una de planificacion, sin otra interaccion con el service de lotes")
    void consultas_unaSolaDeLotes() throws Exception {
        // Arrange
        fermentadoresActivos(50);
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(lotesDeFermentadores(50, LocalDate.now().plusDays(1)));

        // Act + Assert: con planificacion destacada, pagina 1
        mockMvc.perform(get(URL).param("idPlanificacion", "7")).andExpect(status().isOk());
        verify(loteServicio, times(1)).listarLotesParaCronograma(any(), any(), any());
        verify(fermentadorServicio, times(1)).filtrarFermentadores(any(), any(), any());
        verify(planificacionServicio, times(1)).buscarPorId(7L);
        verifyNoMoreInteractions(loteServicio);

        // Sin planificacion, pagina 3
        org.mockito.Mockito.clearInvocations(loteServicio, fermentadorServicio, planificacionServicio);
        mockMvc.perform(get(URL).param("pagina", "3")).andExpect(status().isOk());
        verify(loteServicio, times(1)).listarLotesParaCronograma(any(), any(), any());
        verify(fermentadorServicio, times(1)).filtrarFermentadores(any(), any(), any());
        verify(planificacionServicio, never()).buscarPorId(any());
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-73: un periodo de cinco anios no rompe el diagrama: ancho de 3744 px, etiquetas del eje con anio y no mas de 150 marcas")
    void periodo_deCincoAnios() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                finalizadoEn(1L, "A-1", "F-01", d(2018, 3, 1), d(2018, 3, 10)),
                finalizadoEn(2L, "A-2", "F-01", d(2019, 5, 1), d(2019, 5, 10)),
                finalizadoEn(3L, "A-3", "F-01", d(2020, 7, 1), d(2020, 7, 10))));

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2018-01-01").param("hasta", "2022-12-31"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("min-width: 3744px").contains(">01/01/18<");
        assertThat(cuenta(html, "class=\"gantt-marca absolute")).isLessThanOrEqualTo(150);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-74: los fermentadores dados de baja con lotes tienen carril, en orden alfabetico con los activos")
    void fermentadoresDadosDeBaja_tienenCarril() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 3L, "F-03");
        when(fermentadorServicio.listarDadosDeBajaConLotes()).thenReturn(List.of(
                FermentadorResponseDTO.builder().id(2L).identificadorInterno("F-02").build()));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                finalizadoEn(1L, "A-1", "F-01", d(2012, 3, 1), d(2012, 3, 10)),
                finalizadoEn(2L, "A-2", "F-02", d(2012, 5, 1), d(2012, 5, 10))));

        // Act
        String html = mockMvc.perform(get(URL).param("desde", "2012-01-01").param("hasta", "2012-12-31"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(colas(html)).containsExactly("F-01", "F-02");
        assertThat(carriles(html)).isEqualTo(2);
        assertThat(cuenta(html, "No hay lotes pendientes en este fermentador")).isEqualTo(2);
        assertThat(html).contains("Página 1 de 1").contains("2 fermentadores").doesNotContain(ALERTA_ADVERTENCIA);
        assertThat(html).doesNotContain("<option");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-75: un fermentador dado de baja con lotes es un filtro valido; uno dado de baja sin lotes (que no figura en la lista) se ignora con aviso")
    void fermentadoresDadosDeBaja_sonUnFiltroValido() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(fermentadorServicio.listarDadosDeBajaConLotes()).thenReturn(List.of(
                FermentadorResponseDTO.builder().id(2L).identificadorInterno("F-02").build()));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                finalizadoEn(1L, "A-1", "F-02", d(2012, 5, 1), d(2012, 5, 10))));

        // Act
        String valido = mockMvc.perform(get(URL).param("idFermentador", "2").param("desde", "2012-01-01").param("hasta", "2012-12-31"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String sinLotes = mockMvc.perform(get(URL).param("idFermentador", "9")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(2L, d(2012, 1, 1), d(2012, 12, 31));
        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
        assertThat(valido).doesNotContain(ALERTA_ADVERTENCIA);
        assertThat(campo(valido, "idFermentador")).isEqualTo("2");
        assertThat(textoDelSelector(valido, "Seleccione fermentador")).isEqualTo("F-02 (dado de baja)");
        assertThat(colas(valido)).containsExactly("F-02");
        assertThat(sinLotes).contains("El fermentador indicado no existe o no está activo, se muestran todos");
        assertThat(textoDelSelector(sinLotes, "Seleccione fermentador")).isEmpty();
        assertThat(sinLotes).doesNotContain("name=\"idFermentador\" value=\"9\"");
    }

    // ==================== POST /cronograma-produccion/lotes/{id}/subir ====================

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-21: subir correctamente delega en el service y redirige al cronograma con la alerta de exito que informa las nuevas fechas")
    void subir_debeRedirigirConAlertaYFechas() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenReturn(movido("DEMO-3", LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 30)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion"))
                .andExpect(flash().attribute("mensaje", "Lote DEMO-3 movido correctamente (inicio 12/11/2026, fin 30/11/2026)"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(loteServicio).subirLoteEnCola(3L);
        verify(loteServicio, never()).bajarLoteEnCola(any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-22: subir conserva los filtros, el período y la página en la URL de redireccion")
    void subir_debeConservarLosFiltros() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenReturn(movido("DEMO-3", LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 30)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf())
                        .param("idFermentador", "2").param("idPlanificacion", "7").param("desde", "2018-04-21")
                        .param("hasta", "2019-01-07").param("pagina", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion?idFermentador=2&idPlanificacion=7&desde=2018-04-21&hasta=2019-01-07&pagina=2"))
                .andExpect(flash().attribute("tipo", "success"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-23: solo viajan los parametros validos: un periodo invalido, invertido o que supera el tope no viaja en ninguno de sus extremos; con un solo extremo viaja solo ese")
    void subir_ignoraFiltrosInvalidos() throws Exception {
        // Arrange
        when(loteServicio.subirLoteEnCola(3L)).thenReturn(movido("DEMO-3", LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 30)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf())
                        .param("idFermentador", "abc").param("idPlanificacion", "").param("desde", "21/04/2018")
                        .param("hasta", "2019-01-07").param("pagina", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion"))
                .andExpect(flash().attribute("tipo", "success"));
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf()).param("desde", "2019-01-07").param("hasta", "2018-04-21"))
                .andExpect(redirectedUrl("/cronograma-produccion"));
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf()).param("desde", "2018-01-01").param("hasta", "2023-01-01"))
                .andExpect(redirectedUrl("/cronograma-produccion"));
        mockMvc.perform(post(URL + "/lotes/3/subir").with(csrf()).param("desde", "2018-04-21").param("pagina", "abc"))
                .andExpect(redirectedUrl("/cronograma-produccion?desde=2018-04-21"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-26: bajar correctamente delega en el service y redirige al cronograma conservando los filtros, con la alerta de exito que informa las nuevas fechas")
    void bajar_debeRedirigirConAlertaFechasYFiltros() throws Exception {
        // Arrange
        when(loteServicio.bajarLoteEnCola(4L)).thenReturn(movido("DEMO-4", LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 15)));

        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/4/bajar").with(csrf()).param("idFermentador", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cronograma-produccion?idFermentador=2"))
                .andExpect(flash().attribute("mensaje", "Lote DEMO-4 movido correctamente (inicio 01/12/2026, fin 15/12/2026)"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(loteServicio).bajarLoteEnCola(4L);
        verify(loteServicio, never()).subirLoteEnCola(any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-29: un POST sin token CSRF no mueve nada: es rechazado (redirige) y no llega al service")
    void mover_sinCsrfNoLlegaAlService() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post(URL + "/lotes/3/bajar")).andExpect(status().is3xxRedirection());

        verifyNoInteractions(loteServicio);
    }

    // ==================== GET /cronograma-produccion/buscador/planificaciones ====================

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-30: el buscador devuelve un fragmento (sin layout) con el criterio de receta, el placeholder, las planificaciones y el boton Elegir con su id y texto")
    void buscador_debeDevolverElFragmentoConLasPlanificaciones() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(any(), any(), any()))
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
                .andExpect(content().string(containsString("Cantidad (L)")))
                .andExpect(content().string(containsString(">600<")))
                .andExpect(content().string(containsString("01/07/2027")))
                .andExpect(content().string(containsString(">Pendiente<")))
                .andExpect(content().string(containsString(">Finalizada<")))
                .andExpect(content().string(containsString("Elegir")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-31: el buscador pide al service la primera pagina de 8 planificaciones de cualquier estado, con la ultima registrada primero (id descendente), sin filtrar por receta ni estado")
    void buscador_pideLasMasRecientesPrimero() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        mockMvc.perform(get(URL + "/buscador/planificaciones")).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesPorNombreDeReceta(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
        verifyNoMoreInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-32: el criterio de receta se pasa recortado al service, que filtra por parte del nombre; el buscador muestra lo que el service devuelve y conserva el criterio tipeado")
    void buscador_filtraPorNombreDeReceta() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(eq("STOUT"), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        planificacion(9L, "Stout", EstadoSolicitud.PENDIENTE),
                        planificacion(7L, "Imperial Stout", EstadoSolicitud.PENDIENTE))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/planificaciones").param("receta", " STOUT "))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-id=\"9\"").contains("data-id=\"7\"").doesNotContain("data-id=\"8\"");
        assertThat(html).contains("value=\" STOUT \"");
        verify(planificacionServicio).filtrarPlanificacionesPorNombreDeReceta(eq("STOUT"), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-33: el buscador pagina de a 8 con la ultima registrada arriba; la paginacion conserva el criterio, se pide dentro del modal y la segunda pagina trae el resto")
    void buscador_paginaDeA8ConservandoElCriterio() throws Exception {
        // Arrange: 10 planificaciones de "Pale Ale" (ids 10 a 1, la mas reciente primero), que el service reparte de a 8
        List<PlanificacionProduccionResponseDTO> todas = new ArrayList<>();
        for (long id = 10; id >= 1; id--) {
            todas.add(planificacion(id, "Pale Ale", EstadoSolicitud.PENDIENTE));
        }
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(eq("pale"), any(), eq(PageRequest.of(0, 8, Sort.by(Sort.Order.desc("id"))))))
                .thenReturn(new PageImpl<>(todas.subList(0, 8), PageRequest.of(0, 8), 10));
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(eq("pale"), any(), eq(PageRequest.of(1, 8, Sort.by(Sort.Order.desc("id"))))))
                .thenReturn(new PageImpl<>(todas.subList(8, 10), PageRequest.of(1, 8), 10));

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
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-34: sin resultados el buscador muestra un mensaje claro, y una pagina negativa equivale a la primera")
    void buscador_sinResultadosYPaginaNegativa() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(eq("zzz"), any(), any())).thenReturn(Page.empty());
        when(planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(isNull(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE))));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/planificaciones").param("receta", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron planificaciones con los criterios indicados")));
        mockMvc.perform(get(URL + "/buscador/planificaciones").param("pagina", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-id=\"7\"")));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio, times(2)).filtrarPlanificacionesPorNombreDeReceta(any(), any(), captor.capture());
        assertThat(captor.getAllValues()).extracting(Pageable::getPageNumber).containsOnly(0);
    }

    // ==================== GET /cronograma-produccion/buscador/fermentadores ====================

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-88: el buscador de fermentadores devuelve un fragmento (sin layout) con los criterios de identificador, estado operativo e incluir dados de baja, las columnas y el boton Elegir con su id y texto")
    void buscadorFermentadores_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentadorDelBuscador(1L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO),
                fermentadorDelBuscador(2L, "F-02", EstadoOperativo.EN_USO, 400.5, Estado.ACTIVO))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/fermentadores"))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/buscador-fermentador :: buscador"))
                .andExpect(content().string(not(containsString("<html"))))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Buscar fermentador").contains("action=\"/cronograma-produccion/buscador/fermentadores\"")
                .contains("data-modal-buscar").contains("placeholder=\"Ej: F-01\"")
                .contains(">Identificador</label>").contains(">Estado operativo</label>").contains("Incluir dados de baja")
                .contains("type=\"checkbox\"").contains("name=\"incluirDadosDeBaja\"")
                .contains("Capacidad útil (L)").contains(">330<").contains(">400,5<")
                .contains(">Disponible<").contains(">En uso<").contains("Elegir")
                .contains("data-id=\"1\"").contains("data-texto=\"F-01\"").contains("data-id=\"2\"").contains("data-texto=\"F-02\"");
        // El estado operativo es un enum chico: un desplegable con "Todos" y los cuatro estados
        assertThat(html).contains("<select id=\"buscador-estado-operativo\" name=\"estadoOperativo\"");
        assertThat(html).contains(">Todos</option>").contains(">Disponible</option>").contains(">En uso</option>")
                .contains(">En limpieza</option>").contains(">En mantenimiento</option>");
        assertThat(html).doesNotContain("data-fermentador-dado-de-baja");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-89: sin incluir dados de baja el buscador consulta solo los fermentadores activos, de a 8 y en orden alfabetico por identificador; el texto se recorta y el criterio de estado llega al service")
    void buscadorFermentadores_consultaSoloLosActivos() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/fermentadores").param("identificador", " f-0 ").param("estadoOperativo", "EN_USO"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadores(eq("f-0"), eq(EstadoOperativo.EN_USO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno")).isNotNull();
        verify(fermentadorServicio, never()).filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any());
        verifyNoInteractions(loteServicio, planificacionServicio);
        assertThat(html).contains("value=\" f-0 \"").containsPattern("<option value=\"EN_USO\"[^>]*selected=\"selected\"[^>]*>En uso</option>");
        assertThat(html).doesNotContain("checked=\"checked\"");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-90: con incluir dados de baja el buscador consulta tambien los dados de baja con lotes, que se marcan \"Dado de baja\" y se eligen con el texto \"F-02 (dado de baja)\"; el criterio queda tildado")
    void buscadorFermentadores_incluyeLosDadosDeBajaConLotes() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentadorDelBuscador(1L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO),
                fermentadorDelBuscador(2L, "F-02", EstadoOperativo.DISPONIBLE, 330.0, Estado.BAJA))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/fermentadores").param("incluirDadosDeBaja", "true"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadoresConDadosDeBajaConLotes(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno")).isNotNull();
        verify(fermentadorServicio, never()).filtrarFermentadores(any(), any(), any());
        assertThat(html).contains("checked=\"checked\"");
        assertThat(cuenta(html, "data-fermentador-dado-de-baja")).isEqualTo(1);
        assertThat(html).contains("data-texto=\"F-01\"").contains("data-texto=\"F-02 (dado de baja)\"");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-91: el buscador de fermentadores pagina de a 8; los enlaces de pagina conservan identificador, estado operativo e incluir dados de baja y se piden dentro del modal")
    void buscadorFermentadores_paginaConservandoLosCriterios() throws Exception {
        // Arrange: segunda pagina de tres
        when(fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentadorDelBuscador(9L, "F-09", EstadoOperativo.EN_USO, 330.0, Estado.ACTIVO)), PageRequest.of(1, 8), 17));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/fermentadores").param("identificador", "F")
                        .param("estadoOperativo", "EN_USO").param("incluirDadosDeBaja", "true").param("pagina", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadoresConDadosDeBajaConLotes(eq("F"), eq(EstadoOperativo.EN_USO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(html).contains("Página <span>2</span> de <span>3</span>").contains("data-modal-get=\"true\"");
        List<String> enlaces = new ArrayList<>();
        Matcher m = Pattern.compile("href=\"([^\"]*buscador/fermentadores[^\"]*)\"").matcher(html);
        while (m.find()) {
            enlaces.add(HtmlUtils.htmlUnescape(m.group(1)));
        }
        assertThat(enlaces).hasSize(2);
        assertThat(enlaces.get(0)).contains("pagina=0");
        assertThat(enlaces.get(1)).contains("pagina=2");
        assertThat(enlaces).allSatisfy(enlace -> assertThat(enlace).contains("identificador=F").contains("estadoOperativo=EN_USO")
                .contains("incluirDadosDeBaja=true"));
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-92: sin resultados el buscador de fermentadores muestra un mensaje claro, y una pagina negativa equivale a la primera")
    void buscadorFermentadores_sinResultadosYPaginaNegativa() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/fermentadores").param("identificador", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron fermentadores con los criterios indicados")));
        mockMvc.perform(get(URL + "/buscador/fermentadores").param("pagina", "-3")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio, times(2)).filtrarFermentadores(any(), any(), captor.capture());
        assertThat(captor.getAllValues()).extracting(Pageable::getPageNumber).containsExactly(0, 0);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-93: el fermentador elegido se muestra en el campo (con \"(dado de baja)\" si corresponde) y se conserva al navegar el periodo y en los botones de mover")
    void filtroDeFermentador_elElegidoSeMuestraYSeConserva() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(fermentadorServicio.listarDadosDeBajaConLotes()).thenReturn(List.of(
                FermentadorResponseDTO.builder().id(2L).identificadorInterno("F-02").build()));
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "A-1", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", 1),
                lote(2L, "A-2", EstadoLote.PENDIENTE, "F-02", 7L, "Pale Ale", 2)));

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "2")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(campo(html, "idFermentador")).isEqualTo("2");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-02 (dado de baja)");
        for (String atributo : List.of("data-periodo-anterior", "data-periodo-siguiente", "data-periodo-hoy")) {
            assertThat(href(html, atributo)).contains("idFermentador=2");
        }
        assertThat(cuenta(html, "<input type=\"hidden\" name=\"idFermentador\" value=\"2\">")).isEqualTo(4);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-94: sin fermentador elegido el campo queda en blanco con su placeholder, el campo oculto va vacio y se ofrece el boton para quitar la seleccion; Limpiar vuelve al cronograma sin parametros")
    void filtroDeFermentador_sinElegirQuedaEnBlanco() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEmpty();
        assertThat(html).containsPattern("<input[^>]*data-selector-id[^>]*name=\"idFermentador\"[^>]*>");
        assertThat(campo(html, "idFermentador")).isEmpty();
        assertThat(html).contains("data-selector-limpiar");
        assertThat(html).containsPattern("<a href=\"/cronograma-produccion\"[^>]*>Limpiar</a>");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-95: sin JavaScript el filtro sigue funcionando: el campo de texto es de solo lectura y el fermentador viaja en el campo oculto idFermentador del formulario GET de filtros, que se envia con el boton Buscar")
    void filtroDeFermentador_sinJavaScript() throws Exception {
        // Arrange
        fermentadores(1L, "F-01");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL).param("idFermentador", "1")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        Matcher formulario = Pattern.compile("(?s)<form[^>]*data-cronograma-filtros[^>]*>(.*?)</form>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("method=\"get\"").contains("novalidate").contains("type=\"submit\"")
                .contains("type=\"hidden\" data-selector-id name=\"idFermentador\" value=\"1\"").contains("readonly");
    }

    // ==================== Roles ====================

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-CGC-35: los roles sin LOTE_CONSULTAR no pueden ver el cronograma: redirigen con alerta de error y no llegan a ningun service")
    void roles_sinLoteConsultarNoPuedenVerElCronograma(Rol rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL).with(como(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio, fermentadorServicio, planificacionServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-CGC-35: los roles sin PLANIFICACION_PRODUCCION_CONSULTAR no pueden usar el buscador de planificaciones: redirigen con alerta de error y no llegan a ningun service")
    void roles_sinPlanificacionConsultarNoPuedenUsarElBuscadorDePlanificaciones(Rol rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio, fermentadorServicio, planificacionServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-CGC-36: los roles sin LOTE_REGISTRAR (incluido el operario) no pueden subir ni bajar lotes: redirigen con alerta de error y el service no se invoca")
    void roles_sinLoteRegistrarNoPuedenMover(Rol rol) throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/3/subir").with(como(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/lotes/3/bajar").with(como(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-CGC-96: los roles sin LOTE_CONSULTAR no pueden usar el buscador de fermentadores: redirigen con alerta de error y no llegan a ningun service")
    void roles_sinLoteConsultarNoPuedenUsarElBuscadorDeFermentadores(Rol rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/fermentadores").with(como(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio, fermentadorServicio, planificacionServicio);
    }

    // ==================== Operario en solo lectura ====================

    @Test
    @DisplayName("CP-CGC-79: el OPERARIO_DE_PRODUCCION ve el cronograma con el diagrama, los filtros de fermentador y periodo, la paginacion y las colas de cada fermentador")
    void operario_veElCronograma() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act + Assert
        mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("cronograma/cronograma"))
                .andExpect(content().string(containsString("Cronograma de Producci")))
                .andExpect(content().string(containsString("action=\"/cronograma-produccion\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("data-buscador-url=\"/cronograma-produccion/buscador/fermentadores\"")))
                .andExpect(content().string(containsString(">Desde</label>")))
                .andExpect(content().string(containsString(">Hasta</label>")))
                .andExpect(content().string(containsString("Buscar")))
                .andExpect(content().string(containsString("Limpiar")))
                .andExpect(content().string(containsString("data-periodo-anterior")))
                .andExpect(content().string(containsString("data-periodo-siguiente")))
                .andExpect(content().string(containsString("data-periodo-hoy")))
                .andExpect(content().string(containsString("data-gantt")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-1")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-2")))
                .andExpect(content().string(containsString("Pale Ale - Plan #7 - PALE_ALE-R7-3")))
                .andExpect(content().string(containsString("Stout - Plan #8 - STOUT-R8-1")))
                .andExpect(content().string(containsString("data-paginacion")))
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-01\"")))
                .andExpect(content().string(containsString("data-cola-fermentador=\"F-02\"")));

        verify(loteServicio).listarLotesParaCronograma(null, desdeDefecto(), hastaDefecto());
    }

    @Test
    @DisplayName("CP-CGC-80: las colas del operario listan los lotes pendientes sin acciones: ni Subir ni Bajar, ni formularios de mover ni token CSRF, ni columna de acciones")
    void operario_lasColasNoTienenAcciones() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: las colas muestran lo mismo que ve el gerente
        assertThat(html).doesNotContain("no se puede mover").contains("data-cola-fila")
                .contains("Volumen (L)").contains("Inicio").contains("Fin");
        assertThat(colas(html)).containsExactly("F-01", "F-02");
        assertThat(cuenta(html, "data-cola-fila")).isEqualTo(3);
        assertThat(cuenta(html, "data-cola-en-ejecucion")).isZero();

        // Assert: y ninguna accion de mover
        assertThat(html).doesNotContain("Subir").doesNotContain("Bajar").doesNotContain("bi-arrow-up").doesNotContain("bi-arrow-down")
                .doesNotContain("/cronograma-produccion/lotes/2/subir").doesNotContain("/cronograma-produccion/lotes/2/bajar")
                .doesNotContain("data-mover").doesNotContain(">Mover<");
        Matcher contenido = Pattern.compile("(?s)<main.*?</main>").matcher(html);
        assertThat(contenido.find()).isTrue();
        assertThat(contenido.group()).doesNotContain("method=\"post\"").doesNotContain("_csrf");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-81: el GERENTE_DE_PRODUCCION conserva los botones Subir y Bajar como formularios POST en cada fila de la cola, con la columna Mover")
    void gerente_conservaLosBotonesDeMover() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains(">Mover</th>").contains("action=\"/cronograma-produccion/lotes/2/subir\"")
                .contains("action=\"/cronograma-produccion/lotes/2/bajar\"").contains("name=\"_csrf\"");
        assertThat(cuenta(html, "data-mover=\"subir\"")).isEqualTo(3);
        assertThat(cuenta(html, "data-mover=\"bajar\"")).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-CGC-82: el operario filtra por fermentador y periodo igual que el gerente: el service recibe los mismos criterios y los enlaces de periodo los conservan")
    void operario_filtrosYPeriodoFuncionIgual() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                loteEn(4L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", d(2018, 5, 2), d(2018, 5, 6))));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION))
                        .param("idFermentador", "2").param("desde", "2018-04-21").param("hasta", "2019-01-07"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        verify(loteServicio).listarLotesParaCronograma(2L, d(2018, 4, 21), d(2019, 1, 7));
        assertThat(colas(html)).containsExactly("F-02");
        assertThat(carriles(html)).isEqualTo(1);
        assertThat(href(html, "data-periodo-anterior")).contains("idFermentador=2");
        assertThat(href(html, "data-periodo-siguiente")).contains("idFermentador=2");
        assertThat(href(html, "data-periodo-hoy")).contains("idFermentador=2");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-02");
    }

    @Test
    @DisplayName("CP-CGC-83: la sidebar del OPERARIO_DE_PRODUCCION muestra Cronograma de Producción en Operación, despues de Gestión de Lotes y resaltado, y no Planificación de Producción")
    void operario_sidebarConElCronograma() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        Matcher enlace = Pattern.compile("(?s)<a href=\"/cronograma-produccion\"[^>]*>(.*?)</a>").matcher(html);
        assertThat(enlace.find()).isTrue();
        assertThat(enlace.group()).contains("bg-amber-500/10").contains("bi-bar-chart-steps").contains("Cronograma de Producción");
        assertThat(html).matches("(?s).*Operación.*Gestión de Lotes.*Cronograma de Producción.*");
        assertThat(html).doesNotContain("Planificación de Producción").doesNotContain("Administración");
    }

    @Test
    @DisplayName("CP-CGC-84: el operario no tiene el filtro de planificacion: la pagina no ofrece el selector y el parametro idPlanificacion se ignora sin aviso, sin consultar la planificacion ni atenuar nada")
    void operario_sinPlanificacionADestacar() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act
        String sinParametro = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String conParametro = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idPlanificacion", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String invalido = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idPlanificacion", "abc"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        for (String html : List.of(sinParametro, conParametro, invalido)) {
            assertThat(html).doesNotContain("name=\"idPlanificacion\"").doesNotContain("Planificación a destacar")
                    .doesNotContain("data-buscador-url=\"/cronograma-produccion/buscador/planificaciones\"")
                    .doesNotContain("Seleccione planificación").doesNotContain(ALERTA_ADVERTENCIA);
            assertThat(leyenda(html)).doesNotContain("atenuado");
            assertThat(barra(html, "PALE_ALE-R7-1")).doesNotContain("gantt-barra--atenuada");
            assertThat(barra(html, "STOUT-R8-1")).doesNotContain("gantt-barra--atenuada");
            assertThat(fila(html, 4L)).doesNotContain("opacity-50");
            assertThat(href(html, "data-periodo-siguiente")).doesNotContain("idPlanificacion");
        }
        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-85: el GERENTE_DE_PRODUCCION conserva el selector de planificacion y la leyenda con el atenuado cuando hay una planificacion elegida")
    void gerente_conservaElSelectorDePlanificacion() throws Exception {
        // Arrange
        escenarioDelOperario();
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));

        // Act
        String html = mockMvc.perform(get(URL).param("idPlanificacion", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Planificación a destacar")
                .contains("data-buscador-url=\"/cronograma-produccion/buscador/planificaciones\"")
                .contains("name=\"idPlanificacion\"");
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #7 - Pale Ale");
        assertThat(leyenda(html)).contains("Lote de otra planificaci");
    }

    @Test
    @DisplayName("CP-CGC-86: el operario no puede subir ni bajar lotes: ambos POST redirigen con alerta de error y el service no se invoca")
    void operario_noPuedeMoverLotes() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/lotes/2/subir").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/lotes/2/bajar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-CGC-87: sin lotes en el periodo el operario ve el mismo mensaje de vacio, sin diagrama, colas ni paginador")
    void operario_cronogramaSinLotes() throws Exception {
        // Arrange
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of());

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-cronograma-sin-lotes")
                .contains("No hay lotes para mostrar entre " + fmt(desdeDefecto()) + " y " + fmt(hastaDefecto()))
                .doesNotContain("data-gantt ").doesNotContain("data-cola ").doesNotContain("data-paginacion");
    }

    @Test
    @DisplayName("CP-CGC-97: el buscador de fermentadores exige LOTE_CONSULTAR: lo usan el operario y quien solo tiene ese permiso; quien solo tiene LOTE_REGISTRAR es rechazado")
    void operario_usaElBuscadorDeFermentadores() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentadorDelBuscador(1L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO))));

        // Act + Assert
        for (RequestPostProcessor usuario : List.of(como(Rol.OPERARIO_DE_PRODUCCION), conPermisos(Permiso.LOTE_CONSULTAR))) {
            mockMvc.perform(get(URL + "/buscador/fermentadores").with(usuario))
                    .andExpect(status().isOk())
                    .andExpect(view().name("cronograma/buscador-fermentador :: buscador"))
                    .andExpect(content().string(containsString("data-id=\"1\"")))
                    .andExpect(content().string(containsString("data-texto=\"F-01\"")));
        }
        verify(fermentadorServicio, times(2)).filtrarFermentadores(isNull(), isNull(), any(Pageable.class));

        mockMvc.perform(get(URL + "/buscador/fermentadores").with(conPermisos(Permiso.LOTE_REGISTRAR)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        verifyNoMoreInteractions(fermentadorServicio);
    }

    // ==================== Enlaces al detalle del lote ====================

    @Test
    @DisplayName("CP-CGC-98: quien tiene LOTE_CONSULTAR ve el código de cada lote pendiente de las colas como enlace Ver lote hacia /lotes/{id}; el gerente y el operario lo tienen")
    void colas_elCodigoDelLoteEsUnEnlaceAlDetalle() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act + Assert
        for (RequestPostProcessor usuario : List.of(como(Rol.OPERARIO_DE_PRODUCCION), como(Rol.GERENTE_DE_PRODUCCION))) {
            String html = mockMvc.perform(get(URL).with(usuario)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> enlaces = new ArrayList<>();
            Matcher m = Pattern.compile("<a[^>]*aria-label=\"Ver lote\"[^>]*>").matcher(html);
            while (m.find()) {
                enlaces.add(m.group());
            }
            assertThat(enlaces).hasSize(3);
            for (long id = 2; id <= 4; id++) {
                String destino = "href=\"/lotes/" + id + "\"";
                assertThat(enlaces).anyMatch(enlace -> enlace.contains(destino));
            }
        }
    }

    @Test
    @DisplayName("CP-CGC-99: quien tiene LOTE_REGISTRAR pero no LOTE_CONSULTAR no ve el cronograma; quien solo tiene LOTE_CONSULTAR ve los codigos como enlaces y ningun boton de mover")
    void colas_elEnlaceDependeDeLoteConsultar() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act + Assert
        mockMvc.perform(get(URL).with(conPermisos(Permiso.LOTE_REGISTRAR)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        String html = mockMvc.perform(get(URL).with(conPermisos(Permiso.LOTE_CONSULTAR))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("href=\"/lotes/2\"").doesNotContain("data-mover");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-100: un estado operativo que no existe en el buscador de fermentadores se ignora con un aviso dentro del fragmento y se buscan todos; el nombre se acepta sin distinguir mayusculas")
    void buscadorFermentadores_estadoOperativoInvalido() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        // Act
        String invalido = mockMvc.perform(get(URL + "/buscador/fermentadores").param("estadoOperativo", "XYZ"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String valido = mockMvc.perform(get(URL + "/buscador/fermentadores").param("estadoOperativo", "en_uso"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(invalido).doesNotContain("<html").contains("data-modal-buscar").contains("data-aviso-buscador")
                .contains("El estado operativo indicado no es válido, se muestran todos");
        assertThat(valido).doesNotContain("data-aviso-buscador");
        verify(fermentadorServicio).filtrarFermentadores(isNull(), isNull(), any());
        verify(fermentadorServicio).filtrarFermentadores(isNull(), eq(EstadoOperativo.EN_USO), any());
    }

    @Test
    @DisplayName("CP-CGC-102: quien tiene LOTE_CONSULTAR ve cada barra del diagrama como un enlace al detalle del lote (/lotes/{id}) que conserva el tooltip; la barra deja de ser una imagen")
    void diagrama_lasBarrasSonEnlacesAlDetalleDelLote() throws Exception {
        // Arrange
        escenarioDelOperario();

        // Act + Assert
        for (RequestPostProcessor usuario : List.of(como(Rol.OPERARIO_DE_PRODUCCION), como(Rol.GERENTE_DE_PRODUCCION))) {
            String html = mockMvc.perform(get(URL).with(usuario)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> enlaces = new ArrayList<>();
            Matcher m = Pattern.compile("<a[^>]*data-gantt-barra-enlace[^>]*>").matcher(html);
            while (m.find()) {
                enlaces.add(m.group());
            }
            assertThat(enlaces).hasSize(4);
            for (long id = 1; id <= 4; id++) {
                String destino = "href=\"/lotes/" + id + "\"";
                assertThat(enlaces).anyMatch(enlace -> enlace.contains(destino));
            }
            assertThat(enlaces).allSatisfy(enlace -> assertThat(enlace).containsPattern("aria-label=\"[^\"]*(Pendiente|En ejecuci)"));
            String barra = barra(html, "PALE_ALE-R7-1 - En ejecuci");
            assertThat(barra).contains("title=\"PALE_ALE-R7-1 - En ejecuci").doesNotContain("role=\"img\"").doesNotContain("tabindex");
        }
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR", "LOTE_REGISTRAR", "PLANIFICACION_PRODUCCION_CONSULTAR"})
    @DisplayName("CP-CGC-101: un valor de la URL que no se puede convertir (página del buscador o casilla) en un buscador responde 400 con el mensaje El valor indicado no es válido, sin redirigir")
    void buscadores_valorInvalidoEnLaUrl() throws Exception {
        // Act + Assert
        for (String ruta : List.of("/buscador/fermentadores", "/buscador/planificaciones")) {
            mockMvc.perform(get(URL + ruta).param("pagina", "xyz").header("Referer", URL))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string("El valor indicado no es válido"));
        }
        mockMvc.perform(get(URL + "/buscador/fermentadores").param("incluirDadosDeBaja", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El valor indicado no es válido"));
    }

    // ==================== Datos de prueba ====================

    private static final String ALERTA_ADVERTENCIA = "bg-amber-50 border-amber-200 text-amber-800";

    /**
     * Un usuario con las authorities que le da el login: ROLE_ del rol y cada uno de sus permisos.
     */
    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }

    /**
     * Un usuario con solo esos permisos, sin ningún rol.
     */
    private static RequestPostProcessor conPermisos(Permiso... permisos) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        Arrays.stream(permisos).forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user("usuario").authorities(autoridades);
    }

    /**
     * "F-01" con un lote en ejecución (lote 1) y dos pendientes (2 y 3) del plan 7, y "F-02" con un pendiente (lote 4)
     * del plan 8, todos dentro del período por defecto.
     */
    private void escenarioDelOperario() {
        fermentadores(1L, "F-01", 2L, "F-02");
        when(loteServicio.listarLotesParaCronograma(any(), any(), any())).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01", 7L, "Pale Ale", 1),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 2),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-01", 7L, "Pale Ale", 3),
                lote(4L, "STOUT-R8-1", EstadoLote.PENDIENTE, "F-02", 8L, "Stout", 1)));
    }

    private static LocalDate desdeDefecto() {
        return LocalDate.now().minusDays(15);
    }

    private static LocalDate hastaDefecto() {
        return LocalDate.now().plusDays(60);
    }

    private static LocalDate d(int anio, int mes, int dia) {
        return LocalDate.of(anio, mes, dia);
    }

    private static String fmt(LocalDate fecha) {
        return fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Simula {@code cantidad} fermentadores activos "F-01" a "F-nn" con ids 1 a nn.
     */
    private void fermentadoresActivos(int cantidad) {
        Object[] idEIdentificador = new Object[cantidad * 2];
        for (int i = 1; i <= cantidad; i++) {
            idEIdentificador[(i - 1) * 2] = (long) i;
            idEIdentificador[(i - 1) * 2 + 1] = String.format("F-%02d", i);
        }
        fermentadores(idEIdentificador);
    }

    /**
     * Un pendiente de 5 dias que empieza en {@code inicio} por cada fermentador "F-01" a "F-nn".
     */
    private static List<LoteCronogramaResponseDTO> lotesDeFermentadores(int cantidad, LocalDate inicio) {
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            lotes.add(loteEn((long) i, "L-" + i, EstadoLote.PENDIENTE, String.format("F-%02d", i), 7L, "Pale Ale", inicio, inicio.plusDays(4)));
        }
        return lotes;
    }

    private static List<String> fermentadoresDe(int primero, int ultimo) {
        List<String> identificadores = new ArrayList<>();
        for (int i = primero; i <= ultimo; i++) {
            identificadores.add(String.format("F-%02d", i));
        }
        return identificadores;
    }

    private static LoteCronogramaResponseDTO finalizadoEn(Long id, String identificador, String fermentador, LocalDate inicioReal, LocalDate finReal) {
        return loteEn(id, identificador, EstadoLote.FINALIZADO, fermentador, 7L, "Pale Ale", inicioReal, finReal).toBuilder()
                .fechaInicio(inicioReal.atTime(8, 0)).fechaFinalizacion(finReal.atTime(17, 0)).build();
    }

    /**
     * Un fermentador como lo devuelve el service de fermentadores al buscador del cronograma.
     */
    private static FermentadorResponseDTO fermentadorDelBuscador(Long id, String identificador, EstadoOperativo estadoOperativo,
                                                                 double capacidadUtil, Estado estado) {
        return FermentadorResponseDTO.builder().id(id).identificadorInterno(identificador).estadoOperativo(estadoOperativo)
                .capacidadUtil(capacidadUtil).estado(estado).build();
    }

    /**
     * El texto que muestra el selector de entidad con ese placeholder (vacio si no hay nada elegido).
     */
    private static String textoDelSelector(String html, String placeholder) {
        Matcher m = Pattern.compile("<input[^>]*data-selector-texto[^>]*>").matcher(html);
        while (m.find()) {
            if (m.group().contains("placeholder=\"" + placeholder + "\"")) {
                Matcher valor = Pattern.compile("value=\"([^\"]*)\"").matcher(m.group());
                return valor.find() ? HtmlUtils.htmlUnescape(valor.group(1)) : "";
            }
        }
        throw new AssertionError("No hay selector con el placeholder " + placeholder);
    }

    /**
     * El valor del campo del formulario de filtros con ese nombre.
     */
    private static String campo(String html, String nombre) {
        Matcher m = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("campo %s", nombre).isTrue();
        Matcher valor = Pattern.compile("value=\"([^\"]*)\"").matcher(m.group());
        assertThat(valor.find()).isTrue();
        return valor.group(1);
    }

    /**
     * El destino (sin escapar) del enlace marcado con el atributo indicado.
     */
    private static String href(String html, String atributo) {
        Matcher m = Pattern.compile("<a[^>]*" + atributo + "[^>]*>").matcher(html);
        assertThat(m.find()).as("enlace %s", atributo).isTrue();
        Matcher destino = Pattern.compile("href=\"([^\"]*)\"").matcher(m.group());
        assertThat(destino.find()).isTrue();
        return HtmlUtils.htmlUnescape(destino.group(1));
    }

    /**
     * La apertura de la barra del diagrama cuyo tooltip contiene el texto.
     */
    private static String barra(String html, String texto) {
        Matcher m = Pattern.compile("(?s)<div[^>]*gantt-barra[^>]*>").matcher(html);
        while (m.find()) {
            if (m.group().contains(texto)) {
                return m.group();
            }
        }
        throw new AssertionError("No hay barra con " + texto);
    }

    /**
     * Cantidad de carriles dibujados (rotulos de la columna fija del diagrama).
     */
    private static int carriles(String html) {
        return cuenta(html, "truncate text-sm font-medium text-slate-800");
    }

    /**
     * Los fermentadores de las colas mostradas, en el orden de la pagina.
     */
    private static List<String> colas(String html) {
        List<String> colas = new ArrayList<>();
        Matcher m = Pattern.compile("data-cola-fermentador=\"([^\"]*)\"").matcher(html);
        while (m.find()) {
            colas.add(m.group(1));
        }
        return colas;
    }


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
     * Lote de 300 L que dura 4 dias; el inicio es manana (hoy + 1) mas 6 dias por cada orden posterior al primero (orden 1: de hoy + 1 a hoy + 5).
     */
    private static LoteCronogramaResponseDTO lote(Long id, String identificador, EstadoLote estado, String fermentador,
                                                  Long idPlanificacion, String receta, int orden) {
        return loteDesde(LocalDate.now().plusDays(1), id, identificador, estado, fermentador, idPlanificacion, receta, orden);
    }

    /**
     * Igual que {@code lote}, con las fechas fijas de julio de 2027 (orden 1: del 01/07 al 05/07), para los casos que las nombran.
     */
    private static LoteCronogramaResponseDTO lote2027(Long id, String identificador, EstadoLote estado, String fermentador,
                                                      Long idPlanificacion, String receta, int orden) {
        return loteDesde(LocalDate.of(2027, 7, 1), id, identificador, estado, fermentador, idPlanificacion, receta, orden);
    }

    private static LoteCronogramaResponseDTO loteDesde(LocalDate primerInicio, Long id, String identificador, EstadoLote estado, String fermentador,
                                                       Long idPlanificacion, String receta, int orden) {
        LocalDate inicio = primerInicio.plusDays((orden - 1) * 6L);
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
