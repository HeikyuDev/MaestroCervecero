package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IOllaHervorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEnvasadoLoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AguaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AdicionLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ResumenEnvasadoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

@WebMvcTest(LoteController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class LoteControllerTest {

    private static final String URL = "/lotes";
    private static final String ALERTA_ADVERTENCIA = "bg-amber-50 border-amber-200 text-amber-800";
    private static final String ALERTA_EXITO = "bg-teal-50 border-teal-200 text-teal-800";
    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILoteServicio loteServicio;

    @MockitoBean
    private IConsumoInsumoServicio consumoInsumoServicio;

    @MockitoBean
    private IEnvasadoLoteServicio envasadoLoteServicio;

    @MockitoBean
    private IPlanificacionProduccionServicio planificacionServicio;

    @MockitoBean
    private IMolinoServicio molinoServicio;

    @MockitoBean
    private IMaceradorServicio maceradorServicio;

    @MockitoBean
    private IOllaHervorServicio ollaHervorServicio;

    @MockitoBean
    private IFermentadorServicio fermentadorServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /**
     * Escenario base: lo que cada pantalla necesita para responder 200 o redirigir con éxito. Cada caso cambia solo
     * lo que dice.
     */
    @BeforeEach
    void escenarioBase() {
        when(loteServicio.buscarDetallePorId(12L)).thenReturn(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(loteDeLista(12L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, "F-01"))));
        when(loteServicio.registrarLote(any())).thenReturn(respuestaDeLote());
        when(loteServicio.iniciarLote(any())).thenReturn(respuestaDeLote());
        when(loteServicio.cancelarLote(any(), any())).thenReturn(respuestaDeLote());
        when(loteServicio.finalizarEnvasado(any())).thenReturn(respuestaDeLote());
        when(consumoInsumoServicio.filtrarConsumosInsumo(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(envasadoLoteServicio.filtrarEnvasadosLote(any(), any(), any(), any())).thenReturn(Page.empty());
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any())).thenReturn(Page.empty());
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(molinoServicio.filtrarMolinos(any(), any(), any())).thenReturn(Page.empty());
        when(maceradorServicio.filtrarMaceradores(any(), any(), any())).thenReturn(Page.empty());
        when(ollaHervorServicio.filtrarOllasHervor(any(), any(), any())).thenReturn(Page.empty());
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(Page.empty());
        when(fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any())).thenReturn(Page.empty());
        when(recetaServicio.filtrarRecetas(any(), any())).thenReturn(Page.empty());
        when(recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any())).thenReturn(Page.empty());
    }

    // ==================== GET /lotes ====================

    @Test
    @DisplayName("CP-LCT-01: el GERENTE_DE_PRODUCCION ve el listado con las columnas, los valores legibles, el botón Nuevo lote y, al final de cada fila, el botón Gestionar lote en la columna Acciones")
    void listar_debeMostrarElListadoAlGerente() throws Exception {
        // Arrange
        LoteCronogramaResponseDTO lote = loteDeLista(12L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, "F-01").toBuilder()
                .fechaInicio(LocalDateTime.of(2027, 7, 1, 8, 30)).build();
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(lote)));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Gestión de Lotes").contains("action=\"/lotes\"").contains("novalidate");
        assertThat(texto(html)).contains("Código", "Planificación", "Fermentador", "Estado y etapa", "Inicio", "Fin");
        assertThat(celdas(html, "PALE_ALE-R7-3")).startsWith("PALE_ALE-R7-3", "Plan #7 - Pale Ale", "F-01", "En ejecución Hervido", "Est. 01/07/2027 Real 01/07/2027", "Est. 05/07/2027 Real -");
        assertThat(html).contains("href=\"/lotes/nuevo\"").contains("bi-plus-lg").contains("Nuevo lote")
                .contains("href=\"/lotes/12\"").contains("overflow-x-auto");
        assertThat(html).doesNotContain("bi-eye").doesNotContain(">Ver<").doesNotContain("data-fila-lote");
        assertThat(texto(html)).contains("Acciones");
        assertThat(html).contains("title=\"Gestionar lote\"").contains("aria-label=\"Gestionar lote\"");
    }

    @Test
    @DisplayName("CP-LCT-02: el OPERARIO_DE_PRODUCCION ve el mismo listado con los selectores de planificación y fermentador, sin el botón Nuevo lote")
    void listar_debeMostrarElListadoAlOperarioSinRegistrar() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("href=\"/lotes/12\"")
                .contains("data-buscador-url=\"/lotes/filtros/planificaciones\"")
                .contains("data-buscador-url=\"/lotes/filtros/fermentadores\"")
                .doesNotContain("Nuevo lote").doesNotContain("/lotes/nuevo");
    }

    @Test
    @DisplayName("CP-LCT-03: sin criterios llama al service con todos los criterios en null y una página de 20 con el último lote registrado primero")
    void listar_debePaginarDeA20ConElUltimoPrimero() throws Exception {
        // Act
        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(loteServicio).filtrarLotesParaListado(isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("CP-LCT-141: un número de página desmedido en el listado y en el buscador en modal se acota al máximo en lugar de romper la consulta")
    void listar_unaPaginaDesmedidaSeAcota() throws Exception {
        // Act
        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).param("pagina", "2147483647")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("pagina", "107000000")).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> listado = ArgumentCaptor.forClass(Pageable.class);
        verify(loteServicio).filtrarLotesParaListado(isNull(), isNull(), isNull(), isNull(), listado.capture());
        assertThat(listado.getValue().getPageNumber()).isEqualTo(Paginacion.PAGINA_MAXIMA);
        ArgumentCaptor<Pageable> buscador = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesProduccion(any(), any(), any(), buscador.capture());
        assertThat(buscador.getValue().getPageNumber()).isEqualTo(Paginacion.PAGINA_MAXIMA);
    }

    @Test
    @DisplayName("CP-LCT-04: los cuatro criterios llegan al service (el código recortado) y el listado los conserva en los selectores, el campo de código y el estado")
    void listar_debeAplicarYConservarLosFiltros() throws Exception {
        // Arrange
        when(fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(2L)).thenReturn(fermentador(2L, "F-02", EstadoOperativo.EN_USO, 400.0, Estado.ACTIVO));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("idPlanificacion", "7").param("idFermentador", "2").param("codigo", " pale ")
                        .param("estado", "EN_EJECUCION").param("pagina", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(loteServicio).filtrarLotesParaListado(eq(7L), eq(2L), eq("pale"), eq(EstadoLote.EN_EJECUCION), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #7 - Pale Ale");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-02");
        assertThat(campo(html, "idPlanificacion")).isEqualTo("7");
        assertThat(campo(html, "idFermentador")).isEqualTo("2");
        assertThat(campo(html, "codigo")).isEqualTo("pale");
        assertThat(html).contains("<option value=\"EN_EJECUCION\" selected=\"selected\">En ejecución</option>");
    }

    @Test
    @DisplayName("CP-LCT-05: los criterios vacíos equivalen a no filtrar y el estado ofrece Todos y los cuatro estados con su nombre legible")
    void listar_debeTratarLosCriteriosVaciosComoSinFiltro() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("idPlanificacion", "").param("idFermentador", "").param("codigo", "  ").param("estado", ""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(loteServicio).filtrarLotesParaListado(isNull(), isNull(), isNull(), isNull(), any());
        verifyNoInteractions(planificacionServicio, fermentadorServicio);
        assertThat(html).contains(">Todos</option>").contains(">Pendiente</option>").contains(">En ejecución</option>")
                .contains(">Finalizado</option>").contains(">Cancelado</option>").doesNotContain(ALERTA_ADVERTENCIA);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION"})
    @DisplayName("CP-LCT-06: planificación y fermentador son selectores de entidad con buscador en modal y botón para quitar; el único desplegable de la página es el estado")
    void listar_losFiltrosSonSelectoresDeEntidad(Rol rol) throws Exception {
        // Arrange
        when(fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(3L)).thenReturn(fermentador(3L, "F-03", EstadoOperativo.DISPONIBLE, 330.0, Estado.BAJA));

        // Act
        String html = mockMvc.perform(get(URL).with(como(rol)).param("idFermentador", "3"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("data-buscador-url=\"/lotes/filtros/planificaciones\"")
                .contains("data-buscador-url=\"/lotes/filtros/fermentadores\"")
                .contains("placeholder=\"Seleccione planificación\"").contains("placeholder=\"Seleccione fermentador\"")
                .contains("bi-list-ul").contains("data-selector-limpiar")
                .doesNotContain("data-selector-menu").doesNotContain("data-selector-alta")
                .contains("placeholder=\"Ej: PALE_ALE-R7-3\"");
        assertThat(cuenta(html, "<select")).isEqualTo(1);
        assertThat(html).contains("<select id=\"estado\" name=\"estado\"");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-03 (dado de baja)");
    }

    @Test
    @DisplayName("CP-LCT-137: un fermentador dado de baja con lotes filtra el listado y se muestra como dado de baja en el selector, sin aviso")
    void listar_debeFiltrarPorUnFermentadorDadoDeBaja() throws Exception {
        // Arrange
        when(fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(3L))
                .thenReturn(fermentador(3L, "FERM-99", EstadoOperativo.DISPONIBLE, 330.0, Estado.BAJA));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).param("idFermentador", "3"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(loteServicio).filtrarLotesParaListado(isNull(), eq(3L), isNull(), isNull(), any());
        verify(fermentadorServicio, never()).buscarPorId(any());
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("FERM-99 (dado de baja)");
        assertThat(campo(html, "idFermentador")).isEqualTo("3");
        assertThat(html).doesNotContain(ALERTA_ADVERTENCIA);
    }

    @Test
    @DisplayName("CP-LCT-07:los parámetros inválidos se ignoran con alertas amarillas y el listado se muestra sin esos filtros")
    void listar_debeIgnorarLosParametrosInvalidosYAvisar() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("idPlanificacion", "abc").param("idFermentador", "-5").param("estado", "XYZ"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains(ALERTA_ADVERTENCIA).contains("La planificación indicada no es válida, se muestran todas. "
                + "El fermentador indicado no es válido, se muestran todos. El estado indicado no es válido, se muestran todos");
        verify(loteServicio).filtrarLotesParaListado(isNull(), isNull(), isNull(), isNull(), any());
        verifyNoInteractions(planificacionServicio, fermentadorServicio);
    }

    @Test
    @DisplayName("CP-LCT-08: una planificación o un fermentador inexistentes se ignoran con una alerta amarilla")
    void listar_debeIgnorarLosFiltrosInexistentes() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la planificación con ID: 99"));
        when(fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el fermentador con ID: 99"));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).param("idPlanificacion", "99").param("idFermentador", "99"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains(ALERTA_ADVERTENCIA)
                .contains("La planificación indicada no existe, se muestran todas. El fermentador indicado no existe, se muestran todos");
        verify(loteServicio).filtrarLotesParaListado(isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("CP-LCT-09: la etapa actual solo se muestra en el lote en ejecución; en los otros estados se muestra un guion")
    void listar_laEtapaActualSoloExisteMientrasElLoteSeEjecuta() throws Exception {
        // Arrange
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                loteDeLista(1L, "L-PENDIENTE", EstadoLote.PENDIENTE, null, "F-01"),
                loteDeLista(2L, "L-EJECUCION", EstadoLote.EN_EJECUCION, TipoEtapa.MADURACION, "F-01"),
                loteDeLista(3L, "L-FINALIZADO", EstadoLote.FINALIZADO, null, "F-01"),
                loteDeLista(4L, "L-CANCELADO", EstadoLote.CANCELADO, null, "F-01"))));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(celdas(html, "L-PENDIENTE")).contains("Pendiente -");
        assertThat(celdas(html, "L-EJECUCION")).contains("En ejecución Maduración");
        assertThat(celdas(html, "L-FINALIZADO")).contains("Finalizado -");
        assertThat(celdas(html, "L-CANCELADO")).contains("Cancelado -");
    }

    @Test
    @DisplayName("CP-LCT-10: el código se muestra completo y un fermentador ausente se muestra como guion")
    void listar_debeMostrarElCodigoCompletoYElFermentadorAusente() throws Exception {
        // Arrange
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                loteDeLista(5L, "PILSEN_CLASICA-R4-3", EstadoLote.PENDIENTE, null, null))));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(celdas(html, "PILSEN_CLASICA-R4-3")).startsWith("PILSEN_CLASICA-R4-3").element(2).isEqualTo("-");
    }

    @Test
    @DisplayName("CP-LCT-11: las fechas van en las columnas Inicio y Fin, cada una con las líneas Estimado y Real (con un guion si falta la fecha real), sin columnas que dependan del ancho de la pantalla")
    void listar_debeMostrarLasFechasCompactasYCompletas() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains(">Inicio</th>").contains(">Fin</th>").doesNotContain("xl:hidden").doesNotContain("xl:table-cell");
        List<String> celdas = celdas(html, "PALE_ALE-R7-3");
        assertThat(celdas.get(4)).isEqualTo("Est. 01/07/2027 Real -");
        assertThat(celdas.get(5)).isEqualTo("Est. 05/07/2027 Real -");
        assertThat(html).contains("data-fecha-inicio-estimada").contains("data-fecha-fin-estimado").contains("data-fecha-inicio-real").contains("data-fecha-fin-real");
    }

    @Test
    @DisplayName("CP-LCT-12: la paginación muestra la página actual y conserva los filtros en los links de anterior y siguiente")
    void listar_laPaginacionConservaLosFiltros() throws Exception {
        // Arrange
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(loteDeLista(12L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, null, "F-01")), PageRequest.of(1, 20), 45));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("idPlanificacion", "7").param("estado", "PENDIENTE").param("pagina", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("45 lotes · Página 2 de 3");
        String anterior = enlaceDe(html, "Anterior");
        String siguiente = enlaceDe(html, "Siguiente");
        assertThat(anterior).contains("pagina=0").contains("idPlanificacion=7").contains("estado=PENDIENTE");
        assertThat(siguiente).contains("pagina=2").contains("idPlanificacion=7").contains("estado=PENDIENTE");
    }

    @Test
    @DisplayName("CP-LCT-13: sin resultados muestra el mensaje en lugar de una tabla vacía")
    void listar_sinResultadosMuestraElMensaje() throws Exception {
        // Arrange
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        // Act + Assert
        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron lotes con los criterios indicados")));
    }

    @Test
    @DisplayName("CP-LCT-14: armar la página no dispara consultas por fila: el service de lotes se invoca una única vez")
    void listar_armaLaPaginaConUnaSolaConsulta() throws Exception {
        // Arrange
        List<LoteCronogramaResponseDTO> lotes = new ArrayList<>();
        for (long i = 1; i <= 20; i++) {
            lotes.add(loteDeLista(i, "L-" + i, EstadoLote.PENDIENTE, null, "F-01"));
        }
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(lotes));

        // Act
        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());

        // Assert
        verify(loteServicio).filtrarLotesParaListado(any(), any(), any(), any(), any());
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-15: la alerta flash del Post-Redirect-Get se muestra con el estilo de éxito")
    void listar_debeMostrarLaAlertaFlash() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .flashAttr("mensaje", "Lote PALE_ALE-R7-3 cancelado correctamente").flashAttr("tipo", "success"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lote PALE_ALE-R7-3 cancelado correctamente")))
                .andExpect(content().string(containsString(ALERTA_EXITO)));
    }

    @Test
    @DisplayName("CP-LCT-16: la sidebar del GERENTE_DE_PRODUCCION tiene Gestión de Lotes como enlace activo en Operación, antes de Planificación de Producción, y el título de la pestaña lo nombra")
    void sidebar_elGerenteVeGestionDeLotes() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("<title>Gestión de lotes - Maestro Cervecero</title>");
        Matcher enlace = Pattern.compile("(?s)<a href=\"/lotes\"[^>]*>(.*?)</a>").matcher(html);
        assertThat(enlace.find()).isTrue();
        assertThat(enlace.group()).contains("bg-amber-500/10").contains("bi-collection").contains("Gestión de Lotes");
        assertThat(html.indexOf("Gestión de Lotes")).isLessThan(html.indexOf("Planificación de Producción"));
        assertThat(html).doesNotContain("Próximamente\">Control");
    }

    @Test
    @DisplayName("CP-LCT-17: la sidebar del OPERARIO_DE_PRODUCCION tiene Gestión de Lotes y Cronograma de Producción, y no Planificación de Producción ni la sección Administración")
    void sidebar_elOperarioVeGestionDeLotes() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("href=\"/lotes\"").contains("Gestión de Lotes").contains("Operación")
                .contains("href=\"/cronograma-produccion\"").contains("Cronograma de Producción")
                .doesNotContain("Planificación de Producción").doesNotContain("Administración");
        assertThat(html.indexOf("Cronograma de Producción")).isLessThan(html.indexOf("Gestión de Lotes"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-18: los roles sin LOTE_CONSULTAR no pueden ver el listado: redirigen con alerta de error y no llegan a ningún service")
    void listar_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL).with(como(rol))));
        verifyNingunService();
    }

    // ==================== GET /lotes/nuevo ====================

    @Test
    @DisplayName("CP-LCT-19: el formulario de alta es una página con las tres secciones, las etiquetas, los placeholders, el asterisco rojo, novalidate y el token CSRF")
    void nuevo_debeMostrarElFormularioDeAlta() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Nuevo lote").contains("Crear lote").contains("action=\"/lotes\"").contains("method=\"post\"").contains("novalidate")
                .contains("name=\"_csrf\"").contains("href=\"/lotes\"");
        assertThat(texto(html)).contains("Planificación", "Equipamiento", "Producción", "Planificación de producción", "Molino", "Macerador",
                "Olla de hervor", "Fermentador", "Volumen objetivo (L)");
        assertThat(html).contains("placeholder=\"Seleccione planificación\"").contains("placeholder=\"Seleccione molino\"")
                .contains("placeholder=\"Seleccione macerador\"").contains("placeholder=\"Seleccione olla de hervor\"")
                .contains("placeholder=\"Seleccione fermentador\"").contains("placeholder=\"Ej: 300\"");
        assertThat(cuenta(html, "text-rose-600\" title=\"Obligatorio\">*")).isEqualTo(6);
    }

    @Test
    @DisplayName("CP-LCT-20: los cinco campos de entidad usan el selector con buscador en modal, sin alta al vuelo ni botón de quitar; el volumen es numérico y sin atributos de validación")
    void nuevo_losCincoCamposUsanElBuscador() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(html, "data-selector-buscar")).isEqualTo(5);
        assertThat(html).contains("name=\"idPlanificacionProduccion\"").contains("name=\"idMolino\"").contains("name=\"idMacerador\"")
                .contains("name=\"idOllaHervor\"").contains("name=\"idFermentador\"")
                .contains("data-buscador-url=\"/lotes/buscador/planificaciones\"").contains("data-buscador-url=\"/lotes/buscador/molinos\"")
                .contains("data-buscador-url=\"/lotes/buscador/maceradores\"").contains("data-buscador-url=\"/lotes/buscador/ollas\"")
                .contains("data-buscador-url=\"/lotes/buscador/fermentadores\"").contains("bi-list-ul")
                .doesNotContain("data-selector-menu").doesNotContain("data-selector-alta").doesNotContain("data-selector-limpiar")
                .doesNotContain("<select");
        Matcher volumen = Pattern.compile("<input[^>]*name=\"volumenObjetivo\"[^>]*>").matcher(html);
        assertThat(volumen.find()).isTrue();
        assertThat(volumen.group()).contains("type=\"number\"").doesNotContain("required").doesNotContain("min=");
    }

    @Test
    @DisplayName("CP-LCT-21: con idPlanificacion llega la planificación elegida (una sola consulta) y los demás campos vacíos")
    void nuevo_debePrecargarLaPlanificacion() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idPlanificacion", "7"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #7 - Pale Ale");
        assertThat(campo(html, "idPlanificacionProduccion")).isEqualTo("7");
        assertThat(textoDelSelector(html, "Seleccione molino")).isEmpty();
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEmpty();
        verify(planificacionServicio, times(1)).buscarPorId(7L);
    }

    @Test
    @DisplayName("CP-LCT-22: una planificación indicada inválida o inexistente se ignora con una alerta amarilla y el formulario vacío")
    void nuevo_debeIgnorarLaPlanificacionInvalidaOInexistente() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la planificación con ID: 99"));

        // Act
        String invalida = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idPlanificacion", "abc"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inexistente = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idPlanificacion", "99"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(invalida).contains(ALERTA_ADVERTENCIA).contains("La planificación indicada no es válida");
        assertThat(textoDelSelector(invalida, "Seleccione planificación")).isEmpty();
        assertThat(inexistente).contains(ALERTA_ADVERTENCIA).contains("La planificación indicada no existe");
        assertThat(textoDelSelector(inexistente, "Seleccione planificación")).isEmpty();
    }

    @Test
    @DisplayName("CP-LCT-23: el controller no valida el estado de la planificación: una finalizada llega elegida y sin aviso")
    void nuevo_noValidaElEstadoDeLaPlanificacion() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(8L)).thenReturn(planificacion(8L, "Stout", EstadoSolicitud.FINALIZADA));

        // Act
        String html = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idPlanificacion", "8"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #8 - Stout");
        assertThat(html).doesNotContain(ALERTA_ADVERTENCIA);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-24: los roles sin LOTE_REGISTRAR no pueden abrir el formulario de alta: redirigen con alerta de error y no llegan a ningún service")
    void nuevo_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL + "/nuevo").with(como(rol))));
        verifyNingunService();
    }

    // ==================== POST /lotes ====================

    @Test
    @DisplayName("CP-LCT-25: el alta válida llama al service con el DTO enlazado y redirige a la lista con la alerta de éxito")
    void registrar_debeRegistrarYRedirigirALaLista() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(formularioValido()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "Lote PALE_ALE-R7-3 creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<LoteFormDTO> formulario = ArgumentCaptor.forClass(LoteFormDTO.class);
        verify(loteServicio).registrarLote(formulario.capture());
        assertThat(formulario.getValue().getIdPlanificacionProduccion()).isEqualTo(7L);
        assertThat(formulario.getValue().getIdMolino()).isEqualTo(1L);
        assertThat(formulario.getValue().getIdMacerador()).isEqualTo(2L);
        assertThat(formulario.getValue().getIdOllaHervor()).isEqualTo(3L);
        assertThat(formulario.getValue().getIdFermentador()).isEqualTo(4L);
        assertThat(formulario.getValue().getVolumenObjetivo()).isEqualTo(300.0);
    }

    @Test
    @DisplayName("CP-LCT-26: el formulario vacío vuelve a mostrarse con el resumen y los seis mensajes de campo obligatorio, sin invocar al service de lotes")
    void registrar_formularioVacioMuestraLosErrores() throws Exception {
        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("No se pudo crear el lote", "El volumen objetivo del lote es obligatorio",
                "La planificación de producción es obligatoria", "El fermentador es obligatorio", "El molino es obligatorio",
                "El macerador es obligatorio", "La olla de hervor es obligatoria");
        verifyNoInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-27: cada campo de entidad es obligatorio: el mensaje aparece en el resumen y debajo del campo, y se conservan los demás valores elegidos")
    void registrar_cadaCampoEsObligatorio() throws Exception {
        // Arrange
        when(molinoServicio.buscarPorId(1L)).thenReturn(MolinoResponseDTO.builder().id(1L).identificadorInterno("M-01").build());
        when(maceradorServicio.buscarPorId(2L)).thenReturn(MaceradorResponseDTO.builder().id(2L).identificadorInterno("MA-01").build());
        when(ollaHervorServicio.buscarPorId(3L)).thenReturn(OllaHervorResponseDTO.builder().id(3L).identificadorInterno("OH-01").build());
        when(fermentadorServicio.buscarPorId(4L)).thenReturn(fermentador(4L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO));
        String[][] casos = {
                {"idMolino", "El molino es obligatorio"}, {"idMacerador", "El macerador es obligatorio"},
                {"idOllaHervor", "La olla de hervor es obligatoria"}, {"idFermentador", "El fermentador es obligatorio"},
                {"idPlanificacionProduccion", "La planificación de producción es obligatoria"}};

        for (String[] caso : casos) {
            // Act
            org.springframework.util.MultiValueMap<String, String> parametros = formularioValido();
            parametros.remove(caso[0]);
            String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(parametros))
                    .andExpect(status().isOk())
                    .andExpect(view().name("lote/lote-form"))
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

            // Assert
            assertThat(cuenta(html, caso[1])).as(caso[0]).isEqualTo(2);
            assertThat(html).contains("value=\"300\"");
            if (!caso[0].equals("idMolino")) {
                assertThat(textoDelSelector(html, "Seleccione molino")).isEqualTo("M-01");
            }
            if (!caso[0].equals("idFermentador")) {
                assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-01");
            }
        }
        verifyNoInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-28: los valores con formato inválido vuelven a mostrar el formulario con el error de campo, sin invocar al service de lotes")
    void registrar_valoresConFormatoInvalido() throws Exception {
        // Arrange
        stubEntidadesDelFormulario();
        org.springframework.util.MultiValueMap<String, String> parametros = formularioValido();
        parametros.set("volumenObjetivo", "abc");
        parametros.set("idMolino", "x");

        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(parametros))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Ingrese un número válido", "Ingrese un número entero válido");
        verifyNoInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-29: una regla de negocio rechazada vuelve a mostrar el formulario con el mensaje en el resumen y conserva los cinco selectores y el volumen")
    void registrar_reglaDeNegocioConservaLoCargado() throws Exception {
        // Arrange
        stubEntidadesDelFormulario();
        when(loteServicio.registrarLote(any())).thenThrow(new ReglaNegocioException("La planificación de producción debe encontrarse en estado Pendiente para poder registrar un lote"));

        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(formularioValido()))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("No se pudo crear el lote", "La planificación de producción debe encontrarse en estado Pendiente para poder registrar un lote");
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #7 - Pale Ale");
        assertThat(textoDelSelector(html, "Seleccione molino")).isEqualTo("M-01");
        assertThat(textoDelSelector(html, "Seleccione macerador")).isEqualTo("MA-01");
        assertThat(textoDelSelector(html, "Seleccione olla de hervor")).isEqualTo("OH-01");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-01");
        assertThat(html).contains("value=\"300\"");
    }

    @Test
    @DisplayName("CP-LCT-30: un volumen que supera la capacidad lo rechaza el service y el formulario conserva todo lo cargado")
    void registrar_volumenQueSuperaLaCapacidad() throws Exception {
        // Arrange
        stubEntidadesDelFormulario();
        when(loteServicio.registrarLote(any())).thenThrow(new ReglaNegocioException("El volumen objetivo del lote (500 L) supera la capacidad útil del fermentador (330 L)"));
        org.springframework.util.MultiValueMap<String, String> parametros = formularioValido();
        parametros.set("volumenObjetivo", "500");

        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(parametros))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("El volumen objetivo del lote (500 L) supera la capacidad útil del fermentador (330 L)");
        assertThat(html).contains("value=\"500\"");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEqualTo("F-01");
    }

    @ParameterizedTest(name = "volumen {0}")
    @ValueSource(strings = {"0", "-10", "NaN", "Infinity"})
    @DisplayName("CP-LCT-31: Bean Validation solo exige presencia: un volumen no válido llega al service y su rechazo vuelve a mostrar el formulario con el mensaje y el resto cargado")
    void registrar_volumenNoValidoLoRechazaElService(String volumen) throws Exception {
        // Arrange
        stubEntidadesDelFormulario();
        when(loteServicio.registrarLote(any())).thenThrow(new ReglaNegocioException("El volumen objetivo del lote debe ser mayor a 0"));
        org.springframework.util.MultiValueMap<String, String> parametros = formularioValido();
        parametros.set("volumenObjetivo", volumen);

        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(parametros))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<LoteFormDTO> formulario = ArgumentCaptor.forClass(LoteFormDTO.class);
        verify(loteServicio).registrarLote(formulario.capture());
        assertThat(formulario.getValue().getVolumenObjetivo()).isEqualTo(Double.valueOf(volumen));
        assertThat(texto(html)).contains("El volumen objetivo del lote debe ser mayor a 0");
        assertThat(textoDelSelector(html, "Seleccione molino")).isEqualTo("M-01");
    }

    @Test
    @DisplayName("CP-LCT-32: una planificación cubierta la rechaza el service y el formulario conserva lo cargado")
    void registrar_planificacionCubierta() throws Exception {
        // Arrange
        stubEntidadesDelFormulario();
        when(loteServicio.registrarLote(any())).thenThrow(new ReglaNegocioException("La planificación ya tiene lotes que cubren la cantidad a producir (600 L)"));

        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(formularioValido()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("La planificación ya tiene lotes que cubren la cantidad a producir (600 L)");
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #7 - Pale Ale");
        assertThat(html).contains("value=\"300\"");
    }

    @Test
    @DisplayName("CP-LCT-33: un recurso inexistente vuelve a mostrar el formulario con el mensaje del service y el selector de ese recurso en blanco")
    void registrar_recursoInexistente() throws Exception {
        // Arrange
        stubEntidadesDelFormulario();
        when(fermentadorServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el fermentador con ID: 99"));
        when(loteServicio.registrarLote(any())).thenThrow(new RecursoNoEncontradoException("No se encontró el fermentador con ID: 99"));
        org.springframework.util.MultiValueMap<String, String> parametros = formularioValido();
        parametros.set("idFermentador", "99");

        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(parametros))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("No se encontró el fermentador con ID: 99");
        assertThat(textoDelSelector(html, "Seleccione fermentador")).isEmpty();
        assertThat(textoDelSelector(html, "Seleccione molino")).isEqualTo("M-01");
    }

    @Test
    @DisplayName("CP-LCT-34: un alta sin token CSRF es rechazada (redirige) y no llega al service")
    void registrar_sinCsrfNoLlegaAlService() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).params(formularioValido()))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(loteServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-35: los roles sin LOTE_REGISTRAR no pueden registrar lotes: redirigen con alerta de error y el service no se invoca")
    void registrar_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(post(URL).with(como(rol)).with(csrf()).params(formularioValido())));
        verifyNingunService();
    }

    // ==================== GET /lotes/{id} ====================

    @Test
    @DisplayName("CP-LCT-36: el OPERARIO_DE_PRODUCCION ve el detalle de un lote pendiente con las seis pestañas Pendiente, el panel de Molienda y el botón Iniciar lote, sin Cancelar lote")
    void detalle_loteDelPendienteParaElOperario() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).fechaInicio(null).build());

        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/lote-detalle"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("data-lote-cabecera").contains("data-etapa-panel=\"molienda\"");
        assertThat(texto(bloque(html, "data-lote-cabecera"))).contains("PALE_ALE-R7-3", "Pendiente", "Iniciar lote").doesNotContain("Cancelar lote");
        assertThat(pestanas(html)).isEqualTo("Molienda Pendiente Maceración Pendiente Hervido Pendiente Fermentación Pendiente Maduración Pendiente Envasado Pendiente");
    }

    @Test
    @DisplayName("CP-LCT-37: el GERENTE_DE_PRODUCCION ve el lote pendiente con Cancelar lote y sin Iniciar lote")
    void detalle_loteDelPendienteParaElGerente() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());

        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Cancelar lote").contains("data-motivo-url=\"/lotes/12/cancelar\"")
                .doesNotContain("Iniciar lote").doesNotContain("/iniciar");
    }

    @Test
    @DisplayName("CP-LCT-38: la cabecera muestra el código, el estado, la planificación con link para el gerente, la receta con su versión, el fermentador, el volumen, las fechas y el link para volver")
    void detalle_cabeceraCompleta() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        String cabecera = texto(bloque(html, "data-lote-cabecera"));
        assertThat(cabecera).contains("PALE_ALE-R7-3", "En ejecución", "Plan #7 - Pale Ale", "Pale Ale - versión 3 de 3", "Fermentador F-01",
                "Volumen objetivo 300 L", "Inicio estimado 01/07/2027", "Fin estimado 05/07/2027", "Inicio real 01/07/2027 08:30", "Fin real -", "Volver a lotes");
        assertThat(html).contains("href=\"/planificaciones-produccion/7\"").contains("href=\"/lotes\"");
    }

    @Test
    @DisplayName("CP-LCT-39: el operario no tiene acceso a las planificaciones: la cabecera muestra el plan como texto, sin link")
    void detalle_laPlanificacionNoEsLinkParaElOperario() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(bloque(html, "data-lote-cabecera"))).contains("Plan #7 - Pale Ale");
        assertThat(html).doesNotContain("/planificaciones-produccion/7");
    }

    @Test
    @DisplayName("CP-LCT-40: se abre la etapa en curso: la pestaña de Hervido es la actual, el panel visible es el suyo y solo se consultan los datos de esa etapa")
    void detalle_seAbreLaEtapaEnCurso() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.EN_CURSO, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());

        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(pestanas(html)).isEqualTo("Molienda Finalizada Maceración Finalizada Hervido En curso Fermentación Pendiente Maduración Pendiente Envasado Pendiente");
        assertThat(cuenta(html, "aria-current=\"page\"")).isEqualTo(1);
        Matcher actual = Pattern.compile("<a[^>]*aria-current=\"page\"[^>]*>").matcher(html);
        assertThat(actual.find()).isTrue();
        assertThat(actual.group()).contains("href=\"/lotes/12?etapa=hervido\"");
        assertThat(cuenta(html, "data-etapa-panel=")).isEqualTo(1);
        assertThat(html).contains("data-etapa-panel=\"hervido\"");
    }

    @Test
    @DisplayName("CP-LCT-41: las pestañas son enlaces y se puede ver cualquier etapa, incluidas las anteriores: una pendiente sin acciones y una finalizada con sus insumos")
    void detalle_sePuedeElegirOtraPestania() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.EN_CURSO, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 40.0, 0.0, 40.0)));

        // Act
        String pendiente = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("etapa", "fermentacion"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String finalizada = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("etapa", "molienda"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(pendiente).contains("data-etapa-panel=\"fermentacion\"").doesNotContain("Nuevo consumo").doesNotContain("Finalizar etapa");
        assertThat(texto(bloque(pendiente, "data-franja-estado"))).isEqualTo("Fermentación Estado Pendiente Equipo F-01 Inicio - Fin -");
        assertThat(finalizada).contains("data-etapa-panel=\"molienda\"").doesNotContain("Nuevo consumo").doesNotContain("Finalizar etapa");
        assertThat(texto(bloque(finalizada, "data-franja-estado"))).startsWith("Molienda Estado Finalizada Equipo M-01");
        assertThat(texto(bloque(finalizada, "data-insumos-etapa"))).contains("Pilsen");
        for (String etapa : List.of("molienda", "maceracion", "hervido", "fermentacion", "maduracion", "envasado")) {
            assertThat(pendiente).contains("href=\"/lotes/12?etapa=" + etapa + "\"");
        }
    }

    @Test
    @DisplayName("CP-LCT-42: una etapa inválida se ignora con una alerta amarilla y se muestra la etapa en curso")
    void detalle_etapaInvalida() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("etapa", "xyz"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains(ALERTA_ADVERTENCIA).contains("La etapa indicada no es válida, se muestra la etapa actual")
                .contains("data-etapa-panel=\"hervido\"");
    }

    @Test
    @DisplayName("CP-LCT-43: un lote finalizado se abre en Envasado, con las seis etapas Finalizada y sin ningún botón ni formulario de acción")
    void detalle_loteFinalizado() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.FINALIZADO, TipoEtapa.ENVASADO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA))
                .fechaFinalizacion(LocalDateTime.of(2027, 7, 5, 17, 0)).build());

        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("data-etapa-panel=\"envasado\"").contains("05/07/2027 17:00");
        assertThat(pestanas(html)).isEqualTo("Molienda Finalizada Maceración Finalizada Hervido Finalizada Fermentación Finalizada Maduración Finalizada Envasado Finalizada");
        assertThat(html).doesNotContain("Iniciar lote").doesNotContain("Cancelar lote").doesNotContain("Nuevo consumo")
                .doesNotContain("Nuevo envasado").doesNotContain("Finalizar etapa").doesNotContain("Finalizar envasado")
                .doesNotContain("/finalizar").doesNotContain("/iniciar");
    }

    @Test
    @DisplayName("CP-LCT-44: un lote cancelado no muestra el motivo en la cabecera: junto al estado tiene un botón que abre el diálogo con el motivo, el usuario y la fecha y hora de la cancelación; abre la etapa Cancelada y no tiene botones de operación")
    void detalle_loteCancelado() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.HERVIDO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA))
                .fechaCancelacion(LocalDateTime.of(2027, 7, 3, 10, 15)).motivoCancelacion("Se detectó una contaminación en el mosto").usuarioCancelacion("gerente").build());

        // Act
        String html = mockMvc.perform(get(URL + "/12").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        String cabecera = bloque(html, "data-lote-cabecera");
        assertThat(texto(cabecera)).contains("Cancelado").doesNotContain("Motivo de la cancelación").doesNotContain("contaminación");
        Matcher detalle = Pattern.compile("<button[^>]*data-detalle-anulacion[^>]*>").matcher(cabecera);
        assertThat(detalle.find()).isTrue();
        assertThat(detalle.group()).contains("type=\"button\"").contains("data-detalle-titulo=\"Detalle de la cancelación\"").contains("data-detalle-etiqueta-usuario=\"Cancelado por\"")
                .contains("data-detalle-motivo=\"Se detectó una contaminación en el mosto\"").contains("data-detalle-usuario=\"gerente\"")
                .contains("data-detalle-fecha=\"03/07/2027 10:15\"").contains("aria-label=\"Ver detalle de la cancelación\"");
        assertThat(cabecera.indexOf("Cancelado")).isLessThan(cabecera.indexOf("data-detalle-anulacion"));
        assertThat(html).contains("id=\"dialogo-detalle-anulacion\"");
        assertThat(html).contains("data-etapa-panel=\"hervido\"").doesNotContain("Cancelar lote");
        assertThat(pestanas(html)).contains("Hervido Cancelada");
        assertThat(texto(bloque(html, "data-franja-estado"))).startsWith("Hervido Estado Cancelada");
    }

    @Test
    @DisplayName("CP-LCT-45: la pestaña por defecto es la que indica el service, también en un lote cancelado sin etapas iniciadas (Molienda, con las fechas en guion) o con la última iniciada (Hervido)")
    void detalle_pestaniaPorDefectoDeUnLoteCancelado() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA,
                EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA))
                .fechaInicio(null).fechaCancelacion(LocalDateTime.of(2027, 6, 30, 9, 0)).motivoCancelacion("Cambio de plan").build());

        // Act
        String sinEmpezar = mockMvc.perform(get(URL + "/12").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.HERVIDO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE))
                .fechaCancelacion(LocalDateTime.of(2027, 7, 3, 10, 15)).motivoCancelacion("Cambio de plan").build());
        String conEtapasIniciadas = mockMvc.perform(get(URL + "/12").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(sinEmpezar).contains("data-etapa-panel=\"molienda\"");
        assertThat(texto(bloque(sinEmpezar, "data-franja-estado"))).isEqualTo("Molienda Estado Cancelada Equipo M-01 Inicio - Fin -");
        assertThat(conEtapasIniciadas).contains("data-etapa-panel=\"hervido\"");
    }

    @Test
    @DisplayName("CP-LCT-46: la franja de estado de cada etapa muestra su nombre, su estado, su equipo y sus fechas de inicio y fin, y el botón Finalizar etapa solo en la etapa en curso del operario")
    void detalle_franjaDeEstadoDeCadaEtapa() throws Exception {
        // Arrange
        List<EtapaLoteResponseDTO> etapas = new ArrayList<>(etapasEnCurso(TipoEtapa.HERVIDO));
        etapas.set(0, etapa(TipoEtapa.MOLIENDA, EstadoEtapaLote.FINALIZADA, LocalDateTime.of(2027, 7, 1, 8, 30), LocalDateTime.of(2027, 7, 1, 10, 0)));
        etapas.set(1, etapa(TipoEtapa.MACERACION, EstadoEtapaLote.FINALIZADA, LocalDateTime.of(2027, 7, 1, 10, 0), LocalDateTime.of(2027, 7, 1, 11, 30)));
        etapas.set(2, etapa(TipoEtapa.HERVIDO, EstadoEtapaLote.EN_CURSO, LocalDateTime.of(2027, 7, 1, 11, 30), null));
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapas).build());

        // Act + Assert
        assertThat(franja("molienda")).isEqualTo("Molienda Estado Finalizada Equipo M-01 Inicio 01/07/2027 08:30 Fin 01/07/2027 10:00");
        assertThat(franja("maceracion")).isEqualTo("Maceración Estado Finalizada Equipo MA-01 Inicio 01/07/2027 10:00 Fin 01/07/2027 11:30");
        assertThat(franja("hervido")).isEqualTo("Hervido Estado En curso Equipo OH-01 Inicio 01/07/2027 11:30 Fin - Finalizar etapa");
        assertThat(franja("fermentacion")).isEqualTo("Fermentación Estado Pendiente Equipo F-01 Inicio - Fin -");
        assertThat(franja("maduracion")).contains("Equipo F-01");
        assertThat(franja("envasado")).contains("Equipo F-01");
    }

    @Test
    @DisplayName("CP-LCT-47: las seis pestañas tienen la misma estructura: franja de estado (con la acción de finalizar), guía e insumos (solo las etapas que consumen insumos), en ese orden y solo del panel de esa etapa; Envasado suma la sección de envasados registrados y no existe una tarjeta de acciones aparte")
    void detalle_estructuraIdenticaEnLasSeisPestanias() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(any())).thenReturn(List.of(requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 0.0)));

        for (String etapa : List.of("molienda", "maceracion", "hervido", "fermentacion", "maduracion", "envasado")) {
            boolean consumeInsumos = !etapa.equals("maceracion") && !etapa.equals("envasado");
            List<String> marcas = consumeInsumos ? List.of("data-franja-estado", "data-guia", "data-insumos-etapa") : List.of("data-franja-estado", "data-guia");
            // Act
            String html = mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("etapa", etapa))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

            // Assert
            int posicion = html.indexOf("data-etapa-panel=\"" + etapa + "\"");
            assertThat(posicion).as(etapa).isPositive();
            for (String marca : marcas) {
                int siguiente = html.indexOf(marca, posicion);
                assertThat(siguiente).as("%s en %s", marca, etapa).isGreaterThan(posicion);
                assertThat(cuenta(html, marca + ">") + cuenta(html, marca + " ")).as("%s en %s", marca, etapa).isEqualTo(1);
                posicion = siguiente;
            }
            assertThat(cuenta(html, "data-etapa-panel=")).isEqualTo(1);
            assertThat(html.contains("data-insumos-etapa")).as("tarjeta de insumos en %s", etapa).isEqualTo(consumeInsumos);
            assertThat(html).as(etapa).doesNotContain("data-acciones-etapa").doesNotContain(">Acciones<");
            assertThat(cuenta(html, "data-registros-etapa")).as(etapa).isEqualTo(etapa.equals("envasado") ? 1 : 0);
        }
    }

    @Test
    @DisplayName("CP-LCT-48: la guía de Molienda nombra el molino y lista la malta a moler con su cantidad, sin el avance del consumo ni textos redundantes")
    void detalle_guiaDeMolienda() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA, etapasEnCurso(TipoEtapa.MOLIENDA)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(
                requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 40.0, 20.0, 20.0), requerido(caramelo(), UnidadDeMedida.KILOGRAMO, 5.0, 5.0, 0.0)));

        // Act
        String guia = texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "molienda"), "data-guia"));

        // Assert
        assertThat(guia).contains("Molino M-01", "Malta a moler", "Pilsen: 40 kg", "Caramelo 60: 5 kg")
                .doesNotContain("20.0").doesNotContain("40.0").doesNotContain(" de 40 kg").doesNotContain("de 5 kg");
        assertThat(guia).doesNotContain("La malta se consume en esta etapa").doesNotContain("Se pueden moler varias maltas").doesNotContain("Moler la malta de la receta");
    }

    @Test
    @DisplayName("CP-LCT-49: la guía de Maceración muestra con etiquetas la malta, el agua de macerado, el de lavado, el total, la duración y el macerador, con un solo cálculo; la fórmula no está en el diseño sino en el panel que abre el botón de información (cerrado por defecto)")
    void detalle_guiaDeMaceracion() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(
                requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 40.0, 0.0, 40.0), requerido(caramelo(), UnidadDeMedida.KILOGRAMO, 5.0, 0.0, 5.0)));
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));

        // Act
        String guiaHtml = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia");
        String panel = elementoConAtributo(guiaHtml, "div", "id=\"info-agua-macerado\"");
        String guia = texto(guiaHtml.replace(panel, ""));

        // Assert
        assertThat(guia).contains("Malta molida a agregar Caramelo 60: 5 kg Pilsen: 40 kg Total: 45 kg Requerida por la receta: 45 kg", "Agua de macerado 139 L", "Agua de lavado 60 L", "Agua total 199 L",
                "Duración estimada 60 min", "Macerador MA-01")
                .doesNotContain("3 L/kg").doesNotContain("Incluye el espacio muerto del macerador").doesNotContain("=");
        assertThat(texto(panel)).contains("3 L/kg × 45 kg + 4 L de espacio muerto = 139 L", "espacio muerto");
        assertThat(panel).contains("hidden");
        verify(loteServicio, times(1)).calcularAguaDelLote(12L);
    }

    @Test
    @DisplayName("CP-LCT-50: sin agua de lavado la guía avisa que no se requiere y muestra cada advertencia del cálculo en una alerta amarilla; con lavado y sin advertencias no hay alertas")
    void detalle_maceracionSinAguaDeLavado() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        String advertencia = "El mosto extraído del macerador (150 L) supera el volumen pre-hervor (145 L), por lo que no se requiere agua de lavado";
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(AguaLoteResponseDTO.builder().volumenObjetivo(300.0).masaMalta(45.0).volumenPreHervor(145.0)
                .aguaMacerado(139.0).aguaLavado(0.0).aguaTotal(139.0).relacionDeEmpaste(3.0).espacioMuerto(4.0).advertencias(List.of(advertencia)).build());

        // Act
        String sinLavado = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia");
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        String conLavado = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia");

        // Assert
        assertThat(texto(sinLavado)).contains("Agua de lavado No se requiere", "Agua total 139 L", advertencia).doesNotContain("Agua de lavado 0");
        assertThat(sinLavado).contains(ALERTA_ADVERTENCIA);
        assertThat(conLavado).doesNotContain(ALERTA_ADVERTENCIA);
    }

    @Test
    @DisplayName("CP-LCT-142: una receta con un nombre de 60 caracteres no rompe la lista: la planificación, el código y el fermentador se acortan dentro de un ancho máximo, con puntos suspensivos y el texto completo en un tooltip")
    void listar_unaRecetaConNombreLargoNoRompeLaTabla() throws Exception {
        // Arrange
        String nombreLargo = "Imperial Stout Barrel Aged con Cafe Vainilla y Cacao Nibs 27";
        assertThat(nombreLargo).hasSize(60);
        LoteCronogramaResponseDTO lote = loteDeLista(12L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, "F-01").toBuilder()
                .nombreReceta(nombreLargo).build();
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(lote)));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        String etiqueta = "Plan #7 - " + nombreLargo;
        String celda = elementoConAtributo(html, "div", "data-planificacion-etiqueta");
        assertThat(celda).contains("truncate").contains("max-w-[9rem]").contains("data-tooltip=\"" + etiqueta + "\"").contains("tabindex=\"0\"");
        assertThat(texto(celda)).isEqualTo(etiqueta);
        assertThat(elementoConAtributo(html, "div", "data-lote-codigo")).contains("truncate").contains("max-w-[9rem]");
        assertThat(elementoConAtributo(html, "div", "data-lote-fermentador")).contains("truncate").contains("max-w-[6rem]");
        assertThat(html).contains("overflow-x-auto");
    }

    @Test
    @DisplayName("CP-LCT-143: la guía de Maceración muestra la malta realmente molida (la suma de los consumos registrados en la Molienda, por malta y en total) y, en gris, la cantidad requerida por la receta; el cálculo del agua no cambia")
    void detalle_guiaDeMaceracionMuestraLaMaltaMolida() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(
                requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 18.0, 0.0, 16.7131), requerido(caramelo(), UnidadDeMedida.KILOGRAMO, 2.8914, 0.0, 2.0892)));

        // Act
        String guiaHtml = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia");
        String guia = texto(guiaHtml.replace(elementoConAtributo(guiaHtml, "div", "id=\"info-agua-macerado\""), ""));

        // Assert
        assertThat(guia).contains("Malta molida a agregar Caramelo 60: 2,0892 kg Pilsen: 16,7131 kg Total: 18,8023 kg Requerida por la receta: 20,8914 kg")
                .doesNotContain("Malta a agregar (requerida por la receta)").doesNotContain("45 kg");
        assertThat(guiaHtml).contains("data-origen=\"molienda\"");
        assertThat(guia).contains("Agua de macerado 139 L", "Agua de lavado 60 L", "Agua total 199 L");
        verify(loteServicio, times(1)).calcularAguaDelLote(12L);
    }

    @Test
    @DisplayName("CP-LCT-144: la guía de Maceración con una sola malta molida no repite el total; la malta requerida que no tiene consumos no aparece en la lista")
    void detalle_guiaDeMaceracionConUnaSolaMaltaMolida() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(
                requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 18.0, 0.0, 16.7131), requerido(caramelo(), UnidadDeMedida.KILOGRAMO, 2.8914, 2.8914, 0.0)));

        // Act
        String guia = texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia"));

        // Assert
        assertThat(guia).contains("Malta molida a agregar Pilsen: 16,7131 kg Requerida por la receta: 20,8914 kg")
                .doesNotContain("Total:").doesNotContain("Caramelo 60: ");
    }

    @Test
    @DisplayName("CP-LCT-145: la guía de Maceración sin consumos de malta en la Molienda muestra la cantidad requerida con la etiqueta \"Malta a agregar (requerida por la receta)\" y avisa que todavía no hay malta molida registrada")
    void detalle_guiaDeMaceracionSinMaltaMolida() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(
                requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 18.0, 18.0, 0.0), requerido(caramelo(), UnidadDeMedida.KILOGRAMO, 2.8914, 2.8914, 0.0)));

        // Act
        String guiaHtml = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia");
        String guia = texto(guiaHtml);

        // Assert
        assertThat(guia).contains("Malta a agregar (requerida por la receta) 20,8914 kg", "Todavía no hay malta molida registrada en la Molienda.")
                .doesNotContain("Malta molida a agregar").doesNotContain("Total:");
        assertThat(guiaHtml).contains("data-origen=\"receta\"");
    }

    @Test
    @DisplayName("CP-LCT-146: si los requerimientos de la Molienda no se pueden calcular, la guía de Maceración usa la masa de malta del cálculo del agua y no cierra el detalle")
    void detalle_guiaDeMaceracionSinRequerimientosDeMolienda() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenThrow(new ReglaNegocioException("El lote no tiene escalado"));

        // Act
        String guia = texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia"));

        // Assert
        assertThat(guia).contains("Malta a agregar (requerida por la receta) 45 kg", "Agua de macerado 139 L");
    }

    @Test
    @DisplayName("CP-LCT-147: el pie de paginación usa el singular con un solo lote (\"1 lote\") y el plural con varios (\"2 lotes\")")
    void listar_laPaginacionPluralizaLaEtiqueta() throws Exception {
        // Arrange
        LoteCronogramaResponseDTO uno = loteDeLista(12L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, "F-01");
        LoteCronogramaResponseDTO otro = loteDeLista(13L, "PALE_ALE-R7-4", EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, "F-02");
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(uno)));

        // Act
        String singular = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(uno, otro)));
        String plural = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(singular)).contains("1 lote · Página 1 de 1").doesNotContain("1 lotes");
        assertThat(texto(plural)).contains("2 lotes · Página 1 de 1");
    }

    @Test
    @DisplayName("CP-LCT-148: con la malta molida el agua de la guía sale de ella: el tooltip de Agua de macerado muestra los kilos molidos y dice \"malta molida\", y la advertencia de desborde del macerador se muestra en una alerta amarilla")
    void detalle_guiaDeMaceracionConElAguaDeLaMaltaMolida() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        String advertencia = "La mezcla de agua y malta molida (16,1 L) desbordará el macerador (capacidad útil: 10 L)";
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(AguaLoteResponseDTO.builder().volumenObjetivo(300.0).masaMalta(3.0).masaMaltaPlanificada(45.0)
                .maltaMolida(true).volumenPreHervor(25.0).aguaMacerado(14.0).aguaLavado(19.0).aguaTotal(33.0).relacionDeEmpaste(3.0).espacioMuerto(5.0)
                .advertencias(List.of(advertencia)).build());

        // Act
        String guiaHtml = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"), "data-guia");
        String panel = elementoConAtributo(guiaHtml, "div", "id=\"info-agua-macerado\"");
        String guia = texto(guiaHtml.replace(panel, ""));

        // Assert
        assertThat(guia).contains("Agua de macerado 14 L", "Agua de lavado 19 L", "Agua total 33 L", advertencia);
        assertThat(texto(panel)).contains("Relación de empaste × malta molida + espacio muerto del macerador", "3 L/kg × 3 kg + 5 L de espacio muerto = 14 L")
                .doesNotContain("requerida por la receta");
        assertThat(guiaHtml).contains(ALERTA_ADVERTENCIA);
    }

    @Test
    @DisplayName("CP-LCT-51: la guía de Hervido muestra el volumen pre-hervor, la duración, la olla y los lúpulos a agregar como líneas de texto, sin tabla, una por adición con su cantidad y cuándo agregarla; el dry hop de Maduración no aparece")
    void detalle_guiaDeHervido() throws Exception {
        // Arrange
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(consumoInsumoServicio.filtrarAdicionesLupulo(1003L)).thenReturn(List.of(
                adicion("Cascade", UsoLupulo.HERVOR, 60.0, 25.0), adicion("Citra", UsoLupulo.WHIRLPOOL, 0.0, 15.0)));

        // Act
        String guia = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido"), "data-guia");

        // Assert
        assertThat(texto(guia)).contains("Volumen pre-hervor 345 L", "Duración estimada 90 min", "Olla de hervor OH-01", "Lúpulos a agregar").doesNotContain("40 g");
        assertThat(guia).doesNotContain("<table");
        assertThat(texto(elementoConTexto(guia, "li", "Cascade"))).isEqualTo("Cascade: 25 g, agregar cuando falten 60 min para terminar el hervor");
        assertThat(texto(elementoConTexto(guia, "li", "Citra"))).isEqualTo("Citra: 15 g, agregar en el whirlpool");
        assertThat(cuenta(guia, "data-lupulo>") + cuenta(guia, "data-lupulo ")).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-LCT-52: la guía de Fermentación muestra con etiquetas la levadura a utilizar con su cantidad, la OG y la FG objetivo, la duración y el fermentador")
    void detalle_guiaDeFermentacion() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION, etapasEnCurso(TipoEtapa.FERMENTACION)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1004L)).thenReturn(List.of(requerido(levadura(), UnidadDeMedida.GRAMO, 11.5, 11.5, 0.0)));

        // Act
        String guia = texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "fermentacion"), "data-guia"));

        // Assert
        assertThat(guia).contains("Levadura a utilizar US-05: 11,5 g", "OG objetivo 1,050", "FG objetivo 1,010", "Duración estimada 10 días", "Fermentador F-01")
                .doesNotContain("0 de 11,5");
    }

    @Test
    @DisplayName("CP-LCT-53: la guía de Maduración muestra la duración, el fermentador y los lúpulos a agregar en seco; sin requerimientos no se muestra la tarjeta de insumos")
    void detalle_guiaDeMaduracion() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MADURACION, etapasEnCurso(TipoEtapa.MADURACION)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1005L)).thenReturn(List.of(requerido(citra(), UnidadDeMedida.GRAMO, 40.0, 40.0, 0.0)));

        // Act
        String conInsumos = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maduracion");
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1005L)).thenReturn(List.of());
        String sinInsumos = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maduracion");

        // Assert
        assertThat(texto(bloque(conInsumos, "data-guia"))).contains("Duración estimada 4 días", "Fermentador F-01", "Lúpulos a agregar en seco Citra: 40 g");
        assertThat(texto(bloque(conInsumos, "data-insumos-etapa"))).contains("Citra");
        assertThat(sinInsumos).doesNotContain("data-insumos-etapa").doesNotContain("Esta etapa no consume insumos").doesNotContain("Gestionar consumos").doesNotContain("data-ir-consumos");
    }

    @Test
    @DisplayName("CP-LCT-54: la guía de Envasado muestra el volumen objetivo, lo envasado hasta ahora y el máximo según la capacidad útil del fermentador")
    void detalle_guiaDeEnvasado() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO, etapasEnCurso(TipoEtapa.ENVASADO)).build());
        when(envasadoLoteServicio.resumirEnvasado(1006L)).thenReturn(resumen(120.0, 330.0, 210.0, 0.0));

        // Act
        String guia = texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "envasado"), "data-guia"));

        // Assert
        assertThat(guia).contains("Volumen objetivo 300 L", "Envasado hasta ahora 120 L",
                "Máximo según la capacidad útil del fermentador F-01 330 L quedan 210 L").doesNotContain("supera el volumen objetivo");
    }

    @Test
    @DisplayName("CP-LCT-55: superar el volumen objetivo no es un error: se informa que el total supera el volumen objetivo y lo que queda hasta la capacidad, sin alerta")
    void detalle_envasadoPorEncimaDelObjetivo() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO, etapasEnCurso(TipoEtapa.ENVASADO)).build());
        when(envasadoLoteServicio.resumirEnvasado(1006L)).thenReturn(resumen(310.0, 330.0, 20.0, 10.0));

        // Act
        String guia = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "envasado"), "data-guia");

        // Assert
        assertThat(texto(guia)).contains("El total envasado supera el volumen objetivo del lote (300 L)", "quedan 20 L");
        assertThat(guia).doesNotContain("bg-amber-50").doesNotContain("bg-rose-50");
    }

    @Test
    @DisplayName("CP-LCT-56: la tabla de insumos de la etapa muestra requerido, reservado, consumido y el avance con el formato X de Y")
    void detalle_tablaDeInsumosDeLaEtapa() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MOLIENDA, etapasEnCurso(TipoEtapa.MOLIENDA)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1001L)).thenReturn(List.of(
                requerido(pilsen(), UnidadDeMedida.KILOGRAMO, 40.0, 40.0, 20.0), requerido(caramelo(), UnidadDeMedida.KILOGRAMO, 5.0, 5.0, 5.0)));

        // Act
        String insumos = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "molienda"), "data-insumos-etapa");

        // Assert
        assertThat(texto(insumos)).contains("Insumo Requerido Reservado Consumido Avance");
        assertThat(celdas(insumos, "Pilsen")).containsExactly("Pilsen", "40 kg", "40 kg", "20 kg", "20 de 40 kg");
        assertThat(celdas(insumos, "Caramelo 60")).containsExactly("Caramelo 60", "5 kg", "5 kg", "5 kg", "5 de 5 kg");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"maceracion", "envasado"})
    @DisplayName("CP-LCT-57: las etapas que no consumen insumos no muestran la tarjeta de insumos (ni el mensaje, ni la tabla, ni el botón Gestionar consumos ni el avance), con las demás secciones presentes")
    void detalle_etapasSinConsumos(String etapa) throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.EN_CURSO,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.EN_CURSO)).build());

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, etapa);

        // Assert
        assertThat(html).doesNotContain("data-insumos-etapa").doesNotContain("Esta etapa no consume insumos").doesNotContain("Gestionar consumos").doesNotContain("data-ir-consumos").doesNotContain("data-avance")
                .contains("data-franja-estado").contains("data-guia").doesNotContain("data-acciones-etapa");
        verify(consumoInsumoServicio, never()).filtrarInsumosRequeridos(etapa.equals("maceracion") ? 1002L : 1006L);
    }

    @Test
    @DisplayName("CP-LCT-58: la pestaña de una etapa que consume insumos no lista los consumos registrados: solo la tabla de insumos de la etapa, y no consulta los consumos")
    void detalle_noListaLosConsumosRegistrados() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1003L)).thenReturn(List.of(requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 0.0)));

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");

        // Assert
        assertThat(html).doesNotContain("data-registros-etapa").doesNotContain("Consumos registrados").doesNotContain("No hay consumos registrados");
        assertThat(texto(bloque(html, "data-insumos-etapa"))).contains("Insumo Requerido Reservado Consumido Avance");
        verify(consumoInsumoServicio, never()).filtrarConsumosInsumo(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("CP-LCT-59: el pie de la sección Insumos de la etapa tiene el texto de ayuda a la izquierda y el botón Gestionar consumos a la derecha, con el ícono bi-list-check, hacia la pantalla de consumos de la etapa")
    void detalle_botonRegistrarConsumo() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1003L)).thenReturn(List.of(requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 0.0)));

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");

        // Assert
        String insumos = bloque(html, "data-insumos-etapa");
        assertThat(texto(insumos)).endsWith("Registre y consulte los consumos de esta etapa. Gestionar consumos");
        assertThat(insumos).contains("data-ir-consumos").contains("href=\"/lotes/12/etapas/hervido/consumos\"").contains("bi-list-check")
                .contains("bg-amber-500").doesNotContain("bi-box-arrow-in-down");
        assertThat(html).doesNotContain("Nuevo consumo");
    }

    @Test
    @DisplayName("CP-LCT-60: Gestionar consumos aparece para quien tiene CONSUMO_INSUMO_CONSULTAR aunque la etapa esté pendiente o finalizada o el lote cancelado, y no para el gerente, que no tiene ese permiso")
    void detalle_registrarConsumoSegunEstadoYRol() throws Exception {
        // Arrange
        List<EtapaLoteResponseDTO> pendiente = etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE);
        List<EtapaLoteResponseDTO> finalizada = etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.EN_CURSO, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE);
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1003L)).thenReturn(List.of(requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 0.0)));

        // Act
        String gerente = detalleHtml(Rol.GERENTE_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, pendiente).build());
        String etapaPendiente = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION, finalizada).build());
        String etapaFinalizada = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        String loteCancelado = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");

        // Assert
        assertThat(gerente).doesNotContain("Gestionar consumos").doesNotContain("/consumos");
        for (String html : List.of(etapaPendiente, etapaFinalizada, loteCancelado)) {
            assertThat(html).contains("Gestionar consumos").contains("href=\"/lotes/12/etapas/hervido/consumos\"").doesNotContain("Nuevo consumo");
        }
    }

    @Test
    @DisplayName("CP-LCT-61: Finalizar etapa es un formulario POST con CSRF, siempre habilitado, con el avance hacia el mínimo informado por el service y el texto de confirmación")
    void detalle_finalizarEtapaConAvance() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1003L)).thenReturn(List.of(
                requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 18.75), requerido(citra(), UnidadDeMedida.GRAMO, 15.0, 15.0, 15.0)));
        when(loteServicio.calcularAvanceDeEtapa(12L, TipoEtapa.HERVIDO)).thenReturn(AvanceEtapaResponseDTO.builder().porcentajeMinimo(80.0).cumpleMinimo(false)
                .insumos(List.of(avance("Cascade", 25.0, 18.75, 75.0, false), avance("Citra", 15.0, 15.0, 100.0, true))).build());

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");

        // Assert
        Matcher formulario = Pattern.compile("(?s)<form[^>]*action=\"/lotes/12/etapas/hervido/finalizar\"[^>]*>(.*?)</form>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("method=\"post\"").contains("name=\"_csrf\"").contains("Finalizar etapa")
                .contains("data-confirmar-titulo=\"Finalizar etapa\"")
                .contains("data-confirmar-mensaje=\"¿Finalizar la etapa Hervido? La olla de hervor pasará a En limpieza\"");
        assertThat(boton(formulario.group())).doesNotContain("disabled");
        assertThat(bloque(html, "data-franja-estado")).contains("action=\"/lotes/12/etapas/hervido/finalizar\"");
        assertThat(bloque(html, "data-insumos-etapa")).contains("data-avance");
        String avance = bloque(html, "data-avance");
        assertThat(texto(avance)).contains("Cascade: 75% de 80% requerido", "Citra: 100% de 80% requerido");
        assertThat(avance).contains("data-cumple=\"false\"").contains("data-cumple=\"true\"");
        assertThat(elementoConTexto(avance, "li", "Cascade")).contains("data-cumple=\"false\"");
        assertThat(elementoConTexto(avance, "li", "Citra")).contains("data-cumple=\"true\"");
    }

    @Test
    @DisplayName("CP-LCT-62: con porcentaje mínimo 0 el avance dice que no hay consumo mínimo y el botón Finalizar etapa sigue presente")
    void detalle_sinPorcentajeMinimo() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarInsumosRequeridos(1003L)).thenReturn(List.of(requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 0.0)));
        when(loteServicio.calcularAvanceDeEtapa(12L, TipoEtapa.HERVIDO)).thenReturn(AvanceEtapaResponseDTO.builder().porcentajeMinimo(0.0).cumpleMinimo(true)
                .insumos(List.of(avance("Cascade", 25.0, 0.0, 0.0, true))).build());

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");

        // Assert
        assertThat(texto(bloque(html, "data-avance"))).isEqualTo("Sin consumo mínimo requerido");
        assertThat(html).contains("action=\"/lotes/12/etapas/hervido/finalizar\"").contains("Finalizar etapa");
    }

    @Test
    @DisplayName("CP-LCT-63: una etapa sin insumos se finaliza sin avance")
    void detalle_finalizarEtapaSinInsumos() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion");

        // Assert
        assertThat(html).contains("action=\"/lotes/12/etapas/maceracion/finalizar\"").doesNotContain("data-avance");
    }

    @Test
    @DisplayName("CP-LCT-64: Finalizar etapa solo aparece para quien tiene el permiso de la etapa y con esa etapa en curso de un lote en ejecución")
    void detalle_finalizarEtapaSegunEstadoYRol() throws Exception {
        // Arrange
        List<EtapaLoteResponseDTO> pendiente = etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE);
        List<EtapaLoteResponseDTO> finalizada = etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.EN_CURSO, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE);

        // Act
        String gerente = detalleHtml(Rol.GERENTE_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, pendiente).build());
        String etapaPendiente = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.FERMENTACION, finalizada).build());
        String etapaFinalizada = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        String loteCancelado = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        String operario = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        String soloOtraEtapa = detalleHtml(conPermisos(Permiso.LOTE_CONSULTAR, Permiso.LOTE_FINALIZAR_MOLIENDA), "hervido");

        // Assert
        for (String html : List.of(gerente, etapaPendiente, etapaFinalizada, loteCancelado, soloOtraEtapa)) {
            assertThat(html).doesNotContain("/finalizar");
        }
        assertThat(operario).contains("/lotes/12/etapas/hervido/finalizar");
    }

    @Test
    @DisplayName("CP-LCT-65: la pestaña de Envasado ofrece Finalizar envasado en la franja de estado y Nuevo envasado en la cabecera de los envasados registrados al operario, sin Gestionar consumos, y ninguno de los dos al gerente")
    void detalle_pestaniaDeEnvasadoConSusAcciones() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO, etapasEnCurso(TipoEtapa.ENVASADO)).build());

        // Act
        String operario = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "envasado");
        String gerente = detalleHtml(Rol.GERENTE_DE_PRODUCCION, "envasado");

        // Assert
        assertThat(texto(bloque(operario, "data-franja-estado"))).contains("Finalizar envasado");
        assertThat(texto(bloque(operario, "data-registros-etapa"))).startsWith("Envasados registrados Nuevo envasado");
        assertThat(operario).doesNotContain("Nuevo consumo").doesNotContain("Gestionar consumos");
        assertThat(operario).contains("href=\"/lotes/12/envasados\"");
        Matcher formulario = Pattern.compile("(?s)<form[^>]*action=\"/lotes/12/etapas/envasado/finalizar\"[^>]*>(.*?)</form>").matcher(operario);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("name=\"_csrf\"").contains("data-confirmar-titulo=\"Finalizar envasado\"")
                .contains("data-confirmar-mensaje=\"¿Finalizar el envasado? El lote quedará Finalizado y el fermentador pasará a En limpieza\"");
        assertThat(gerente).doesNotContain("Nuevo envasado").doesNotContain("/envasados").doesNotContain("/finalizar");
    }

    @Test
    @DisplayName("CP-LCT-66: los envasados registrados de la etapa se ven en solo lectura, los anulados con el botón de detalle de la anulación, y sin envasados se informa")
    void detalle_envasadosRegistradosDeLaEtapa() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO, etapasEnCurso(TipoEtapa.ENVASADO)).build());
        when(envasadoLoteServicio.filtrarEnvasadosLote(eq(1006L), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                envasado("B-001", 100.0, EstadoTransaccion.REGISTRADO, null), envasado("B-002", 20.0, EstadoTransaccion.ANULADO, "Barril equivocado"))));

        // Act
        String registros = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "envasado"), "data-registros-etapa");

        // Assert
        assertThat(celdas(registros, "B-001")).containsExactly("B-001", "100", "Registrado");
        assertThat(celdas(registros, "B-002")).containsExactly("B-002", "20", "Anulado");
        assertThat(registros).doesNotContain("<form").doesNotContain("Barril equivocado</").doesNotContain("data-motivo-url");
        assertThat(cuenta(registros, "<button")).isEqualTo(1);
        assertThat(registros).contains("data-detalle-anulacion").contains("data-detalle-motivo=\"Barril equivocado\"").contains("data-detalle-titulo=\"Detalle de la anulación\"");
        when(envasadoLoteServicio.filtrarEnvasadosLote(eq(1006L), any(), any(), any())).thenReturn(Page.empty());
        assertThat(texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "envasado"), "data-registros-etapa"))).contains("No hay envasados registrados");
    }

    @Test
    @DisplayName("CP-LCT-67: el gerente ve en cada pestaña la franja con las fechas, la guía y los insumos (y los envasados en Envasado), sin botones de operación: el único control es Cancelar lote")
    void detalle_elGerenteVeTodoSinBotonesDeOperacion() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.ENVASADO, etapasEnCurso(TipoEtapa.ENVASADO)).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(any())).thenReturn(List.of(requerido(cascade(), UnidadDeMedida.GRAMO, 25.0, 25.0, 25.0)));
        when(consumoInsumoServicio.filtrarConsumosInsumo(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                consumo(cascade(), "L-123", TipoConsumo.DIRECTO, 25.0, EstadoTransaccion.REGISTRADO, null))));
        when(envasadoLoteServicio.filtrarEnvasadosLote(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                envasado("B-001", 100.0, EstadoTransaccion.REGISTRADO, null), envasado("B-002", 20.0, EstadoTransaccion.REGISTRADO, null))));
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(envasadoLoteServicio.resumirEnvasado(any())).thenReturn(resumen(120.0, 330.0, 210.0, 0.0));

        for (String etapa : List.of("molienda", "maceracion", "hervido", "fermentacion", "maduracion", "envasado")) {
            // Act
            String html = detalleHtml(Rol.GERENTE_DE_PRODUCCION, etapa);

            // Assert
            assertThat(html).as(etapa).contains("data-franja-estado").contains("data-guia")
                    .doesNotContain("Nuevo consumo").doesNotContain("Gestionar consumos").doesNotContain("Nuevo envasado").doesNotContain("Finalizar")
                    .doesNotContain("/finalizar").doesNotContain("/consumos").doesNotContain("/envasados").doesNotContain("/iniciar")
                    .contains("Cancelar lote");
            assertThat(texto(bloque(html, "data-franja-estado"))).as(etapa).contains("Inicio").contains("Fin");
            if (etapa.equals("maceracion") || etapa.equals("envasado")) {
                assertThat(html).as(etapa).doesNotContain("data-insumos-etapa");
            } else {
                assertThat(html).as(etapa).contains("data-insumos-etapa");
            }
        }
    }

    @Test
    @DisplayName("CP-LCT-68: Cancelar lote sin envasados registrados abre el diálogo de motivo y no está desactivado")
    void detalle_cancelarLoteHabilitado() throws Exception {
        // Act
        String html = detalleHtml(Rol.GERENTE_DE_PRODUCCION, "hervido");

        // Assert
        Matcher boton = Pattern.compile("<button[^>]*data-motivo-dialogo=\"dialogo-cancelar\"[^>]*>").matcher(html);
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).contains("data-motivo-url=\"/lotes/12/cancelar\"").contains("data-motivo-nombre=\"PALE_ALE-R7-3\"").doesNotContain("disabled");
    }

    @Test
    @DisplayName("CP-LCT-69: con envasados registrados Cancelar lote se muestra desactivado y sin URL, con el aviso de cómo desbloquearlo")
    void detalle_cancelarLoteBloqueadoPorEnvasados() throws Exception {
        // Arrange
        when(envasadoLoteServicio.existenEnvasadosRegistrados(12L)).thenReturn(true);

        // Act
        String html = detalleHtml(Rol.GERENTE_DE_PRODUCCION, "hervido");

        // Assert
        Matcher boton = Pattern.compile("<button[^>]*disabled[^>]*>\\s*<i[^>]*></i>\\s*Cancelar lote").matcher(html);
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).doesNotContain("data-motivo-url");
        assertThat(html).doesNotContain("data-motivo-url=\"/lotes/12/cancelar\"");
        assertThat(texto(bloque(html, "data-lote-cabecera"))).contains("Anule los envasados registrados para poder cancelar el lote");
    }

    @Test
    @DisplayName("CP-LCT-70: la cancelación pide un motivo obligatorio en un diálogo con placeholder, asterisco rojo, cancelar y CSRF; un lote finalizado o cancelado no lo incluye")
    void detalle_dialogoDeMotivoDeCancelacion() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());

        // Act
        String html = detalleHtml(Rol.GERENTE_DE_PRODUCCION, null);

        // Assert
        Matcher dialogo = Pattern.compile("(?s)<dialog[^>]*id=\"dialogo-cancelar\".*?</dialog>").matcher(html);
        assertThat(dialogo.find()).isTrue();
        assertThat(dialogo.group()).contains("Cancelar lote").contains("name=\"motivo\"").contains("placeholder=\"Ej: Se detectó una contaminación en el mosto\"")
                .contains("title=\"Obligatorio\">*").contains("data-cerrar-dialogo").contains("name=\"_csrf\"");
        for (EstadoLote cerrado : List.of(EstadoLote.FINALIZADO, EstadoLote.CANCELADO)) {
            stubDetalle(detalle(cerrado, TipoEtapa.ENVASADO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                    EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA)).build());
            assertThat(detalleHtml(Rol.GERENTE_DE_PRODUCCION, null)).doesNotContain("dialogo-cancelar");
        }
    }

    @Test
    @DisplayName("CP-LCT-71: Iniciar lote y Finalizar etapa son formularios POST con CSRF que piden confirmación con JavaScript, con el texto propio de cada etapa, y funcionan sin él")
    void detalle_confirmacionDeIniciarYFinalizar() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());

        // Act
        String pendiente = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, null);

        // Assert
        Matcher iniciar = Pattern.compile("(?s)<form[^>]*action=\"/lotes/12/iniciar\"[^>]*>(.*?)</form>").matcher(pendiente);
        assertThat(iniciar.find()).isTrue();
        assertThat(iniciar.group()).contains("method=\"post\"").contains("name=\"_csrf\"").contains("data-confirmar-titulo=\"Iniciar lote\"")
                .contains("data-confirmar-mensaje=\"¿Iniciar el lote PALE_ALE-R7-3? Se reservarán los insumos y los equipos pasarán a En uso\"");
        assertThat(pendiente).contains("id=\"dialogo-confirmacion\"");

        String[][] etapas = {
                {"molienda", "Finalizar etapa", "¿Finalizar la etapa Molienda? El molino pasará a En limpieza"},
                {"maceracion", "Finalizar etapa", "¿Finalizar la etapa Maceración? El macerador pasará a En limpieza"},
                {"hervido", "Finalizar etapa", "¿Finalizar la etapa Hervido? La olla de hervor pasará a En limpieza"},
                {"fermentacion", "Finalizar etapa", "¿Finalizar la etapa Fermentación? El fermentador sigue En uso hasta finalizar el envasado"},
                {"maduracion", "Finalizar etapa", "¿Finalizar la etapa Maduración? El fermentador sigue En uso hasta finalizar el envasado"},
                {"envasado", "Finalizar envasado", "¿Finalizar el envasado? El lote quedará Finalizado y el fermentador pasará a En limpieza"}};
        for (String[] etapa : etapas) {
            TipoEtapa tipo = TipoEtapa.valueOf(etapa[0].toUpperCase(Locale.ROOT));
            stubDetalle(detalle(EstadoLote.EN_EJECUCION, tipo, etapasEnCurso(tipo)).build());
            String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, etapa[0]);
            Matcher formulario = Pattern.compile("(?s)<form[^>]*action=\"/lotes/12/etapas/" + etapa[0] + "/finalizar\"[^>]*>(.*?)</form>").matcher(html);
            assertThat(formulario.find()).as(etapa[0]).isTrue();
            assertThat(formulario.group()).contains("method=\"post\"").contains("name=\"_csrf\"").contains("data-confirmar-titulo=\"" + etapa[1] + "\"")
                    .contains("data-confirmar-mensaje=\"" + etapa[2] + "\"");
            assertThat(html).contains("id=\"dialogo-confirmacion\"");
        }
    }

    @Test
    @DisplayName("CP-LCT-72: un lote inexistente vuelve al origen con la alerta de error del service, sin consultar nada más")
    void detalle_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.buscarDetallePorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/99").with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
        verifyNoInteractions(consumoInsumoServicio, envasadoLoteServicio);
        verify(loteServicio).buscarDetallePorId(99L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-73: el cálculo de agua se invoca una sola vez en Maceración y en Hervido y en ninguna otra etapa")
    void detalle_aguaSoloDondeSeUsa() throws Exception {
        // Act
        for (String etapa : List.of("molienda", "fermentacion", "maduracion", "envasado")) {
            detalleHtml(Rol.OPERARIO_DE_PRODUCCION, etapa);
        }

        // Assert
        verify(loteServicio, never()).calcularAguaDelLote(any());
        detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion");
        verify(loteServicio, times(1)).calcularAguaDelLote(12L);
        detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        verify(loteServicio, times(2)).calcularAguaDelLote(12L);
    }

    @Test
    @DisplayName("CP-LCT-74: si el cálculo del agua falla, el detalle sigue abierto y la guía muestra el mensaje en una alerta amarilla en lugar de los volúmenes")
    void detalle_fallaDelCalculoDeAgua() throws Exception {
        // Arrange
        String mensaje = "No se puede calcular el agua del lote porque los datos de la receta o de los equipos producen un valor no válido";
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(loteServicio.calcularAguaDelLote(12L)).thenThrow(new ReglaNegocioException(mensaje));

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion");

        // Assert
        String guia = bloque(html, "data-guia");
        assertThat(guia).contains(ALERTA_ADVERTENCIA);
        assertThat(texto(guia)).contains(mensaje).doesNotContain("Agua de macerado").doesNotContain("Agua total");
        assertThat(html).contains("data-franja-estado").contains("data-finalizar-etapa").contains("/lotes/12/etapas/maceracion/finalizar");
    }

    @Test
    @DisplayName("CP-LCT-75: el detalle consulta una sola vez el lote y solo los insumos de la etapa mostrada, sin consultar los consumos")
    void detalle_soloCalculaLaEtapaMostrada() throws Exception {
        // Act
        detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");

        // Assert
        verify(loteServicio, times(1)).buscarDetallePorId(12L);
        verify(consumoInsumoServicio, times(1)).filtrarInsumosRequeridos(1003L);
        verify(consumoInsumoServicio, times(1)).filtrarAdicionesLupulo(1003L);
        verifyNoMoreInteractions(consumoInsumoServicio);
        verify(envasadoLoteServicio).existenEnvasadosRegistrados(12L);
        verifyNoMoreInteractions(envasadoLoteServicio);
    }

    @Test
    @DisplayName("CP-LCT-76: las pestañas funcionan sin JavaScript: son enlaces con su estado visible y la actual lleva aria-current, sin scripts ni eventos en línea")
    void detalle_lasPestaniasFuncionanSinJavaScript() throws Exception {
        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, null);

        // Assert
        String nav = bloque(html, "data-pestanas");
        assertThat(cuenta(nav, "<a href=\"/lotes/12?etapa=")).isEqualTo(6);
        assertThat(html).contains("aria-current=\"page\"").doesNotContain("data-tab-js").doesNotContain("onclick");
    }

    @Test
    @DisplayName("CP-LCT-77: la alerta flash del Post-Redirect-Get se muestra en el detalle con el estilo de éxito")
    void detalle_debeMostrarLaAlertaFlash() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/12").with(como(Rol.OPERARIO_DE_PRODUCCION))
                        .flashAttr("mensaje", "Lote PALE_ALE-R7-3 iniciado correctamente").flashAttr("tipo", "success"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lote PALE_ALE-R7-3 iniciado correctamente")))
                .andExpect(content().string(containsString(ALERTA_EXITO)));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-78: los roles sin LOTE_CONSULTAR no pueden ver el detalle: redirigen con alerta de error y no llegan a ningún service")
    void detalle_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL + "/12").with(como(rol))));
        verifyNingunService();
    }

    // ==================== POST /lotes/{id}/iniciar ====================

    @Test
    @DisplayName("CP-LCT-79: el inicio correcto llama solo a iniciarLote(12) y redirige al detalle con la alerta de éxito")
    void iniciar_debeIniciarYRedirigirAlDetalle() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/iniciar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", "Lote PALE_ALE-R7-3 iniciado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));
        verify(loteServicio).iniciarLote(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "Stock disponible insuficiente de los siguientes insumos: Pilsen (faltan 5 kg)",
            "El molino M-01 no se encuentra disponible",
            "El lote debe encontrarse en estado Pendiente para poder iniciarse"})
    @DisplayName("CP-LCT-80, CP-LCT-81 y CP-LCT-82: un rechazo del service al iniciar (stock insuficiente, equipamiento no disponible, estado del lote) vuelve al origen con la alerta de advertencia y su mensaje")
    void iniciar_debeMostrarElMotivoDelRechazo(String mensaje) throws Exception {
        // Arrange
        when(loteServicio.iniciarLote(12L)).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        mockMvc.perform(post(URL + "/12/iniciar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", "/lotes/12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-LCT-83: iniciar un lote inexistente vuelve al origen con la alerta de error del service")
    void iniciar_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.iniciarLote(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(post(URL + "/99/iniciar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-LCT-84: iniciar sin token CSRF es rechazado (redirige) y no llega al service")
    void iniciar_sinCsrfNoLlegaAlService() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/iniciar").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection());
        verifyNoInteractions(loteServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-85: los roles sin LOTE_INICIAR no pueden iniciar un lote: redirigen con alerta de error y el service no se invoca")
    void iniciar_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(post(URL + "/12/iniciar").with(como(rol)).with(csrf())));
        verifyNingunService();
    }

    // ==================== POST /lotes/{id}/cancelar ====================

    @Test
    @DisplayName("CP-LCT-86: la cancelación correcta llama al service con el id y el motivo y redirige al detalle con la alerta de éxito")
    void cancelar_debeCancelarYRedirigirAlDetalle() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("motivo", "Se detectó una contaminación en el mosto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", "Lote PALE_ALE-R7-3 cancelado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));
        ArgumentCaptor<CancelacionLoteFormDTO> formulario = ArgumentCaptor.forClass(CancelacionLoteFormDTO.class);
        verify(loteServicio).cancelarLote(eq(12L), formulario.capture());
        assertThat(formulario.getValue().getMotivoCancelacion()).isEqualTo("Se detectó una contaminación en el mosto");
    }

    @Test
    @DisplayName("CP-LCT-87: sin motivo el controller no valida: llama al service con el motivo null y vuelve al origen con el mensaje del service")
    void cancelar_sinMotivoLoValidaElService() throws Exception {
        // Arrange
        when(loteServicio.cancelarLote(eq(12L), any())).thenThrow(new ReglaNegocioException("El motivo de cancelación es obligatorio"));

        // Act + Assert
        mockMvc.perform(post(URL + "/12/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).header("Referer", "/lotes/12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", "El motivo de cancelación es obligatorio"))
                .andExpect(flash().attribute("tipo", "warning"));
        ArgumentCaptor<CancelacionLoteFormDTO> formulario = ArgumentCaptor.forClass(CancelacionLoteFormDTO.class);
        verify(loteServicio).cancelarLote(eq(12L), formulario.capture());
        assertThat(formulario.getValue().getMotivoCancelacion()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "No se puede cancelar el lote porque tiene envasados registrados: anule los envasados para poder cancelarlo",
            "El lote debe encontrarse en estado Pendiente o En ejecución para poder cancelarse"})
    @DisplayName("CP-LCT-88 y CP-LCT-90: un rechazo del service al cancelar (envasados registrados, estado que no se puede cancelar) vuelve al origen con la alerta de advertencia y su mensaje")
    void cancelar_debeMostrarElMotivoDelRechazo(String mensaje) throws Exception {
        // Arrange
        when(loteServicio.cancelarLote(eq(12L), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        mockMvc.perform(post(URL + "/12/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("motivo", "Cambio de plan").header("Referer", "/lotes/12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-LCT-89: cancelar un lote en ejecución sin envasados redirige al detalle con la alerta de éxito y el controller no invoca ningún otro service")
    void cancelar_noInvocaOtrosServices() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("motivo", "Cambio de plan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("tipo", "success"));
        verifyNoInteractions(consumoInsumoServicio, envasadoLoteServicio, planificacionServicio, molinoServicio, maceradorServicio, ollaHervorServicio, fermentadorServicio);
    }

    @Test
    @DisplayName("CP-LCT-91: cancelar un lote inexistente vuelve al origen con la alerta de error del service")
    void cancelar_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.cancelarLote(eq(99L), any())).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(post(URL + "/99/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("motivo", "Cambio de plan").header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-LCT-92: cancelar sin token CSRF es rechazado (redirige) y no llega al service")
    void cancelar_sinCsrfNoLlegaAlService() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).param("motivo", "Cambio de plan")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(loteServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-93: los roles sin LOTE_ANULAR no pueden cancelar un lote: redirigen con alerta de error y el service no se invoca")
    void cancelar_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(post(URL + "/12/cancelar").with(como(rol)).with(csrf()).param("motivo", "Cambio de plan")));
        verifyNingunService();
    }

    // ==================== POST /lotes/{id}/etapas/{etapa}/finalizar ====================

    @Test
    @DisplayName("CP-LCT-94: finalizar Molienda llama solo a finalizarMolienda(12) y redirige al detalle con la alerta de éxito")
    void finalizar_molienda() throws Exception {
        // Act + Assert
        finalizarCorrectamente("molienda", "Etapa de Molienda finalizada correctamente");
        verify(loteServicio).finalizarMolienda(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-95: finalizar Maceración llama solo a finalizarMaceracion(12) y redirige al detalle con la alerta de éxito")
    void finalizar_maceracion() throws Exception {
        // Act + Assert
        finalizarCorrectamente("maceracion", "Etapa de Maceración finalizada correctamente");
        verify(loteServicio).finalizarMaceracion(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-96: finalizar Hervido llama solo a finalizarHervido(12) y redirige al detalle con la alerta de éxito")
    void finalizar_hervido() throws Exception {
        // Act + Assert
        finalizarCorrectamente("hervido", "Etapa de Hervido finalizada correctamente");
        verify(loteServicio).finalizarHervido(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-97: finalizar Fermentación llama solo a finalizarFermentacion(12) y redirige al detalle con la alerta de éxito")
    void finalizar_fermentacion() throws Exception {
        // Act + Assert
        finalizarCorrectamente("fermentacion", "Etapa de Fermentación finalizada correctamente");
        verify(loteServicio).finalizarFermentacion(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-98: finalizar Maduración llama solo a finalizarMaduracion(12) y redirige al detalle con la alerta de éxito")
    void finalizar_maduracion() throws Exception {
        // Act + Assert
        finalizarCorrectamente("maduracion", "Etapa de Maduración finalizada correctamente");
        verify(loteServicio).finalizarMaduracion(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-99: finalizar Envasado llama solo a finalizarEnvasado(12) y redirige al detalle con la alerta de éxito del lote finalizado")
    void finalizar_envasado() throws Exception {
        // Act + Assert
        finalizarCorrectamente("envasado", "Lote PALE_ALE-R7-3 finalizado correctamente");
        verify(loteServicio).finalizarEnvasado(12L);
        verifyNoMoreInteractions(loteServicio);
    }

    @Test
    @DisplayName("CP-LCT-100: el Post-Redirect-Get no fija la pestaña: el redirect es a /lotes/12 sin el parámetro etapa, de modo que el detalle abre la etapa en curso")
    void finalizar_noFijaLaPestania() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/etapas/molienda/finalizar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"));
    }

    @Test
    @DisplayName("CP-LCT-101: si no se alcanzó el porcentaje mínimo, el rechazo del service vuelve al origen con la alerta de advertencia y su mensaje")
    void finalizar_porcentajeMinimoNoAlcanzado() throws Exception {
        // Arrange
        String mensaje = "No se puede finalizar la etapa: los siguientes insumos no alcanzaron el 80% mínimo de consumo requerido: Cascade";
        when(loteServicio.finalizarHervido(12L)).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        finalizarRechazado("hervido", mensaje);
    }

    @Test
    @DisplayName("CP-LCT-102: si la etapa en curso es otra, el rechazo del service vuelve al origen con la alerta de advertencia y su mensaje")
    void finalizar_laEtapaEnCursoEsOtra() throws Exception {
        // Arrange
        String mensaje = "La etapa actual del lote es Maceración, no Molienda";
        when(loteServicio.finalizarMolienda(12L)).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        finalizarRechazado("molienda", mensaje);
    }

    @Test
    @DisplayName("CP-LCT-103: si el lote no está en ejecución, el rechazo del service vuelve al origen con la alerta de advertencia y su mensaje")
    void finalizar_loteQueNoEstaEnEjecucion() throws Exception {
        // Arrange
        String mensaje = "El lote debe encontrarse en estado En ejecución para poder finalizar una etapa";
        when(loteServicio.finalizarFermentacion(12L)).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        finalizarRechazado("fermentacion", mensaje);
    }

    @Test
    @DisplayName("CP-LCT-104: un envasado sin litros envasados no se finaliza: el rechazo del service vuelve al origen con la alerta de advertencia y su mensaje")
    void finalizar_envasadoSinLitrosEnvasados() throws Exception {
        // Arrange
        String mensaje = "No se puede finalizar el envasado de un lote sin litros envasados, si no se va a producir debe cancelarse el lote";
        when(loteServicio.finalizarEnvasado(12L)).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        finalizarRechazado("envasado", mensaje);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"molienda", "maceracion", "hervido", "fermentacion", "maduracion", "envasado"})
    @DisplayName("CP-LCT-105: finalizar cualquier etapa de un lote inexistente vuelve al origen con la alerta de error del service")
    void finalizar_loteInexistente(String etapa) throws Exception {
        // Arrange
        RecursoNoEncontradoException excepcion = new RecursoNoEncontradoException("No se encontró el lote con ID: 99");
        when(loteServicio.finalizarMolienda(99L)).thenThrow(excepcion);
        when(loteServicio.finalizarMaceracion(99L)).thenThrow(excepcion);
        when(loteServicio.finalizarHervido(99L)).thenThrow(excepcion);
        when(loteServicio.finalizarFermentacion(99L)).thenThrow(excepcion);
        when(loteServicio.finalizarMaduracion(99L)).thenThrow(excepcion);
        when(loteServicio.finalizarEnvasado(99L)).thenThrow(excepcion);

        // Act + Assert
        mockMvc.perform(post(URL + "/99/etapas/" + etapa + "/finalizar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-LCT-106: los seis POST de finalizar sin token CSRF son rechazados (redirigen) y el service no se invoca")
    void finalizar_sinCsrfNoLlegaAlService() throws Exception {
        // Act + Assert
        for (String etapa : ETAPAS) {
            mockMvc.perform(post(URL + "/12/etapas/" + etapa + "/finalizar").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection());
        }
        verifyNoInteractions(loteServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-107: los roles sin los permisos de finalización no pueden finalizar ninguna etapa: redirigen con alerta de error y ninguno de los seis métodos del service se invoca")
    void finalizar_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        for (String etapa : ETAPAS) {
            assertRechazado(mockMvc.perform(post(URL + "/12/etapas/" + etapa + "/finalizar").with(como(rol)).with(csrf())));
        }
        verifyNingunService();
    }

    @Test
    @DisplayName("CP-LCT-108: cada etapa exige su propio permiso: con solo LOTE_FINALIZAR_MOLIENDA procede Molienda y las otras cinco son rechazadas")
    void finalizar_unPermisoPorEtapa() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/12/etapas/molienda/finalizar").with(conPermisos(Permiso.LOTE_FINALIZAR_MOLIENDA)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("tipo", "success"));
        verify(loteServicio).finalizarMolienda(12L);
        for (String etapa : ETAPAS.subList(1, ETAPAS.size())) {
            assertRechazado(mockMvc.perform(post(URL + "/12/etapas/" + etapa + "/finalizar").with(conPermisos(Permiso.LOTE_FINALIZAR_MOLIENDA)).with(csrf())));
        }
        verifyNoMoreInteractions(loteServicio);
    }

    // ==================== Buscadores del formulario de registro ====================

    @Test
    @DisplayName("CP-LCT-109: el buscador de planificaciones devuelve un fragmento (sin layout) con el criterio de receta como selector de entidad, las columnas y el botón Elegir con su id y texto")
    void buscadorDePlanificaciones_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                planificacion(8L, "Stout", EstadoSolicitud.PENDIENTE), planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-planificacion :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).doesNotContain("<html").contains("action=\"/lotes/buscador/planificaciones\"").contains("data-modal-buscar");
        assertThat(html).contains("data-selector-buscar").contains("bi-list-ul").contains("data-buscador-url=\"/lotes/buscador/recetas\"")
                .contains("placeholder=\"Seleccione receta\"").contains("data-selector-limpiar")
                .containsPattern("<input[^>]*data-selector-id[^>]*name=\"idReceta\"[^>]*>")
                .doesNotContain("data-selector-menu").doesNotContain("name=\"receta\"").doesNotContain("placeholder=\"Ej: Pale Ale\"");
        assertThat(textoDelSelector(html, "Seleccione receta")).isEmpty();
        assertThat(texto(html)).contains("Buscar planificación", "Receta", "Planificación Receta Cantidad (L) Inicio estimado Estado");
        assertThat(celdas(html, "Pale Ale")).startsWith("#7", "Pale Ale", "600", "01/07/2027", "Pendiente");
        assertThat(html).contains("data-id=\"7\"").contains("data-texto=\"Plan #7 - Pale Ale\"").contains("data-id=\"8\"").contains("data-texto=\"Plan #8 - Stout\"");
    }

    @Test
    @DisplayName("CP-LCT-110: solo se ofrecen planificaciones pendientes, sin receta ni fecha, con la última registrada arriba y de a 8, sin consultar recetas")
    void buscadorDePlanificaciones_soloPendientes() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesProduccion(isNull(), eq(EstadoSolicitud.PENDIENTE), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(8);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        verifyNoInteractions(recetaServicio);
        assertThat(textoDelSelector(html, "Seleccione receta")).isEmpty();
    }

    @Test
    @DisplayName("CP-LCT-111: la receta elegida llega como identificador al service de planificaciones y el selector la muestra con su id oculto")
    void buscadorDePlanificaciones_filtraPorLaRecetaElegida() throws Exception {
        // Arrange
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Stout", Estado.ACTIVO));
        when(planificacionServicio.filtrarPlanificacionesProduccion(eq(3L), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                planificacion(9L, "Stout", EstadoSolicitud.PENDIENTE), planificacion(7L, "Stout", EstadoSolicitud.PENDIENTE))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", "3"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(planificacionServicio).filtrarPlanificacionesProduccion(eq(3L), eq(EstadoSolicitud.PENDIENTE), isNull(), any(Pageable.class));
        assertThat(html).contains("data-texto=\"Plan #9 - Stout\"").contains("data-texto=\"Plan #7 - Stout\"").doesNotContain("Plan #8");
        assertThat(textoDelSelector(html, "Seleccione receta")).isEqualTo("Stout");
        assertThat(campo(html, "idReceta")).isEqualTo("3");
    }

    @Test
    @DisplayName("CP-LCT-112: la paginación del modal es de a 8 y conserva la receta elegida en los links marcados para pedir el fragmento")
    void buscadorDePlanificaciones_paginacion() throws Exception {
        // Arrange
        List<PlanificacionProduccionResponseDTO> primera = new ArrayList<>();
        for (long id = 10; id >= 3; id--) {
            primera.add(planificacion(id, "Pale Ale", EstadoSolicitud.PENDIENTE));
        }
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Pale Ale", Estado.ACTIVO));
        when(planificacionServicio.filtrarPlanificacionesProduccion(eq(3L), any(), any(), eq(PageRequest.of(0, 8, Sort.by(Sort.Order.desc("id")))))).thenReturn(new PageImpl<>(primera, PageRequest.of(0, 8), 10));
        when(planificacionServicio.filtrarPlanificacionesProduccion(eq(3L), any(), any(), eq(PageRequest.of(1, 8, Sort.by(Sort.Order.desc("id")))))).thenReturn(new PageImpl<>(
                List.of(planificacion(2L, "Pale Ale", EstadoSolicitud.PENDIENTE), planificacion(1L, "Pale Ale", EstadoSolicitud.PENDIENTE)), PageRequest.of(1, 8), 10));

        // Act
        String primeraPagina = mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", "3"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String segundaPagina = mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", "3").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(primeraPagina, "data-elegir")).isEqualTo(8);
        assertThat(texto(primeraPagina)).contains("10 planificaciones · Página 1 de 2");
        assertThat(enlaceDe(primeraPagina, "Siguiente")).contains("pagina=1").contains("idReceta=3");
        assertThat(primeraPagina).contains("data-modal-get=\"true\"");
        assertThat(cuenta(segundaPagina, "data-elegir")).isEqualTo(2);
        assertThat(texto(segundaPagina)).contains("Página 2 de 2");
        assertThat(enlaceDe(segundaPagina, "Anterior")).contains("pagina=0").contains("idReceta=3");
    }

    @Test
    @DisplayName("CP-LCT-113: sin resultados el modal lo informa y una página negativa equivale a la primera")
    void buscadorDePlanificaciones_vacioYPaginaNegativa() throws Exception {
        // Arrange
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Stout", Estado.ACTIVO));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron planificaciones con los criterios indicados")));
        mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("pagina", "-3")).andExpect(status().isOk());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio, times(2)).filtrarPlanificacionesProduccion(any(), any(), any(), pageable.capture());
        assertThat(pageable.getAllValues()).allSatisfy(p -> assertThat(p.getPageNumber()).isZero());
    }

    @Test
    @DisplayName("CP-LCT-114: el buscador de molinos devuelve un fragmento con el criterio de identificador, el estado operativo legible y el rendimiento")
    void buscadorDeMolinos_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(molinoServicio.filtrarMolinos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                MolinoResponseDTO.builder().id(1L).identificadorInterno("M-01").estadoOperativo(EstadoOperativo.DISPONIBLE).rendimientoMolienda(120.5).build(),
                MolinoResponseDTO.builder().id(2L).identificadorInterno("M-02").estadoOperativo(EstadoOperativo.EN_USO).rendimientoMolienda(150.0).build())));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/molinos").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-equipo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).doesNotContain("<html").contains("placeholder=\"Ej: M-01\"").contains("action=\"/lotes/buscador/molinos\"").contains("data-modal-buscar");
        assertThat(texto(html)).contains("Buscar molino", "Identificador Estado Rendimiento (kg/h)");
        assertThat(celdas(html, "M-01")).startsWith("M-01", "Disponible", "120,5");
        assertThat(celdas(html, "M-02")).startsWith("M-02", "En uso", "150");
        assertThat(html).contains("data-id=\"1\"").contains("data-texto=\"M-01\"").contains("data-id=\"2\"").contains("data-texto=\"M-02\"");
    }

    @Test
    @DisplayName("CP-LCT-115: el buscador de maceradores muestra la capacidad útil y el espacio muerto sin ceros sobrantes")
    void buscadorDeMaceradores_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(maceradorServicio.filtrarMaceradores(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                MaceradorResponseDTO.builder().id(1L).identificadorInterno("MA-01").estadoOperativo(EstadoOperativo.DISPONIBLE).capacidadUtil(330.0).espacioMuerto(4.0).build())));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/maceradores").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-equipo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("placeholder=\"Ej: MA-01\"").contains("action=\"/lotes/buscador/maceradores\"");
        assertThat(texto(html)).contains("Buscar macerador", "Identificador Estado Capacidad útil (L) Espacio muerto (L)");
        assertThat(celdas(html, "MA-01")).startsWith("MA-01", "Disponible", "330", "4");
    }

    @Test
    @DisplayName("CP-LCT-116: el buscador de ollas de hervor muestra la capacidad útil")
    void buscadorDeOllas_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(ollaHervorServicio.filtrarOllasHervor(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                OllaHervorResponseDTO.builder().id(1L).identificadorInterno("OH-01").estadoOperativo(EstadoOperativo.DISPONIBLE).capacidadUtil(400.0).build())));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/ollas").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-equipo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("placeholder=\"Ej: OH-01\"").contains("action=\"/lotes/buscador/ollas\"");
        assertThat(texto(html)).contains("Buscar olla de hervor", "Identificador Estado Capacidad útil (L)");
        assertThat(celdas(html, "OH-01")).startsWith("OH-01", "Disponible", "400");
    }

    @Test
    @DisplayName("CP-LCT-117: el buscador de fermentadores muestra la capacidad útil")
    void buscadorDeFermentadores_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentador(1L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-equipo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("placeholder=\"Ej: F-01\"").contains("action=\"/lotes/buscador/fermentadores\"");
        assertThat(texto(html)).contains("Buscar fermentador", "Identificador Estado Capacidad útil (L)");
        assertThat(celdas(html, "F-01")).startsWith("F-01", "Disponible", "330");
    }

    @Test
    @DisplayName("CP-LCT-118: los cuatro buscadores piden todos los equipos activos, de a 8 y en orden alfabético por identificador, sin exigir disponibilidad")
    void buscadoresDeEquipos_ofrecenLosActivosEnCualquierEstado() throws Exception {
        // Arrange
        when(molinoServicio.filtrarMolinos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                MolinoResponseDTO.builder().id(2L).identificadorInterno("M-02").estadoOperativo(EstadoOperativo.EN_USO).rendimientoMolienda(150.0).build(),
                MolinoResponseDTO.builder().id(3L).identificadorInterno("M-03").estadoOperativo(EstadoOperativo.EN_LIMPIEZA).rendimientoMolienda(100.0).build())));

        // Act
        String molinos = mockMvc.perform(get(URL + "/buscador/molinos").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        for (String buscador : List.of("maceradores", "ollas", "fermentadores")) {
            mockMvc.perform(get(URL + "/buscador/" + buscador).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());
        }

        // Assert
        assertThat(celdas(molinos, "M-02")).contains("En uso");
        assertThat(celdas(molinos, "M-03")).contains("En limpieza");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(molinoServicio).filtrarMolinos(isNull(), isNull(), pageable.capture());
        verify(maceradorServicio).filtrarMaceradores(isNull(), isNull(), pageable.capture());
        verify(ollaHervorServicio).filtrarOllasHervor(isNull(), isNull(), pageable.capture());
        verify(fermentadorServicio).filtrarFermentadores(isNull(), isNull(), pageable.capture());
        assertThat(pageable.getAllValues()).hasSize(4).allSatisfy(p -> {
            assertThat(p.getPageNumber()).isZero();
            assertThat(p.getPageSize()).isEqualTo(8);
            assertThat(p.getSort().getOrderFor("identificadorInterno").getDirection()).isEqualTo(Sort.Direction.ASC);
        });
    }

    @Test
    @DisplayName("CP-LCT-119: el identificador llega recortado al service y un criterio en blanco equivale a no filtrar; el campo conserva lo escrito")
    void buscadoresDeEquipos_criterioDeIdentificador() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/buscador/molinos").with(como(Rol.GERENTE_DE_PRODUCCION)).param("identificador", " m-01 "))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(get(URL + "/buscador/molinos").with(como(Rol.GERENTE_DE_PRODUCCION)).param("identificador", "  ")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "/buscador/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION)).param("identificador", " f-01 ").param("estadoOperativo", "EN_USO")).andExpect(status().isOk());

        // Assert
        verify(molinoServicio).filtrarMolinos(eq("m-01"), isNull(), any());
        verify(molinoServicio).filtrarMolinos(isNull(), isNull(), any());
        verify(fermentadorServicio).filtrarFermentadores(eq("f-01"), eq(EstadoOperativo.EN_USO), any());
        assertThat(campo(html, "identificador")).isEqualTo(" m-01 ");
    }

    @Test
    @DisplayName("CP-LCT-120: la paginación del modal conserva el criterio y, sin resultados, cada buscador nombra su equipo en plural")
    void buscadoresDeEquipos_paginacionYVacio() throws Exception {
        // Arrange
        when(molinoServicio.filtrarMolinos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                MolinoResponseDTO.builder().id(9L).identificadorInterno("M-09").estadoOperativo(EstadoOperativo.DISPONIBLE).rendimientoMolienda(100.0).build()), PageRequest.of(1, 8), 17));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/molinos").with(como(Rol.GERENTE_DE_PRODUCCION)).param("identificador", "M").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Página 2 de 3");
        assertThat(enlaceDe(html, "Anterior")).contains("pagina=0").contains("identificador=M");
        assertThat(enlaceDe(html, "Siguiente")).contains("pagina=2").contains("identificador=M");
        assertThat(html).contains("data-modal-get=\"true\"");
        when(molinoServicio.filtrarMolinos(any(), any(), any())).thenReturn(Page.empty());
        String[][] vacios = {{"molinos", "molinos"}, {"maceradores", "maceradores"}, {"ollas", "ollas de hervor"}, {"fermentadores", "fermentadores"}};
        for (String[] vacio : vacios) {
            mockMvc.perform(get(URL + "/buscador/" + vacio[0]).with(como(Rol.GERENTE_DE_PRODUCCION)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("No se encontraron " + vacio[1] + " con los criterios indicados")));
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-121: los roles sin LOTE_REGISTRAR no pueden usar los seis buscadores del formulario: redirigen con alerta de error y no llegan a ningún service")
    void buscadoresDelFormulario_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        for (String buscador : List.of("planificaciones", "recetas", "molinos", "maceradores", "ollas", "fermentadores")) {
            assertRechazado(mockMvc.perform(get(URL + "/buscador/" + buscador).with(como(rol))));
        }
        verifyNingunService();
    }

    @Test
    @DisplayName("CP-LCT-149: una receta inexistente o dada de baja en el buscador de planificaciones del registro se ignora sin aviso: se busca sin filtrar por receta y el selector queda en blanco")
    void buscadorDePlanificaciones_ignoraUnaRecetaQueNoSeOfrece() throws Exception {
        // Arrange: buscarPorId solo ve recetas activas, así que también lanza para una dada de baja
        when(recetaServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"));
        when(recetaServicio.buscarPorId(5L)).thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 5"));

        for (String idReceta : List.of("99", "5")) {
            // Act
            String html = mockMvc.perform(get(URL + "/buscador/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", idReceta))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

            // Assert
            assertThat(html).doesNotContain("<html").doesNotContain("role=\"alert\"");
            assertThat(textoDelSelector(html, "Seleccione receta")).isEmpty();
            assertThat(campoOVacio(html, "idReceta")).isEmpty();
        }
        verify(planificacionServicio, times(2)).filtrarPlanificacionesProduccion(isNull(), eq(EstadoSolicitud.PENDIENTE), isNull(), any(Pageable.class));
        verify(recetaServicio, never()).buscarActivaODadaDeBajaConPlanificacionesPorId(any());
    }

    @Test
    @DisplayName("CP-LCT-150: el buscador de recetas del registro devuelve el fragmento compartido con el criterio de nombre, las columnas y el botón Elegir, sin la casilla Incluir dados de baja")
    void buscadorDeRecetas_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(recetaServicio.filtrarRecetas(any(), any())).thenReturn(new PageImpl<>(List.of(
                receta(3L, "Pale Ale", Estado.ACTIVO), receta(4L, "Stout", Estado.ACTIVO))));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/buscador-receta :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).doesNotContain("<html").contains("placeholder=\"Ej: Pale Ale\"").contains("action=\"/lotes/buscador/recetas\"").contains("data-modal-buscar")
                .doesNotContain("incluirDadosDeBaja");
        assertThat(texto(html)).contains("Buscar receta", "Nombre", "Nombre Volumen base (L)").doesNotContain("Incluir dados de baja");
        assertThat(celdas(html, "Pale Ale")).startsWith("Pale Ale", "20");
        assertThat(html).contains("data-id=\"3\"").contains("data-texto=\"Pale Ale\"").contains("data-id=\"4\"").contains("data-texto=\"Stout\"");
    }

    @Test
    @DisplayName("CP-LCT-151: el buscador de recetas del registro ofrece solo recetas activas (aun con incluirDadosDeBaja), recorta el nombre, pagina de a 8 conservando el criterio, informa el vacío y una página negativa pide la primera")
    void buscadorDeRecetas_soloActivasCriterioYPaginacion() throws Exception {
        // Act
        mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());
        mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("nombre", " pale ")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("nombre", "  ")).andExpect(status().isOk());
        mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("incluirDadosDeBaja", "true")).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio, times(3)).filtrarRecetas(isNull(), pageable.capture());
        verify(recetaServicio).filtrarRecetas(eq("pale"), pageable.capture());
        assertThat(pageable.getAllValues()).allSatisfy(p -> {
            assertThat(p.getPageNumber()).isZero();
            assertThat(p.getPageSize()).isEqualTo(8);
            assertThat(p.getSort().isUnsorted()).isTrue();
        });
        verify(recetaServicio, never()).filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any());

        // Arrange + Act + Assert: paginación, vacío y página negativa
        when(recetaServicio.filtrarRecetas(any(), any())).thenReturn(new PageImpl<>(List.of(receta(3L, "Pale Ale", Estado.ACTIVO)), PageRequest.of(1, 8), 20));
        String html = mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("nombre", "pale").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(texto(html)).contains("Página 2 de 3");
        assertThat(enlaceDe(html, "Anterior")).contains("pagina=0").contains("nombre=pale").doesNotContain("incluirDadosDeBaja=true");
        assertThat(enlaceDe(html, "Siguiente")).contains("pagina=2").contains("nombre=pale").doesNotContain("incluirDadosDeBaja=true");
        assertThat(html).contains("data-modal-get=\"true\"");
        when(recetaServicio.filtrarRecetas(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get(URL + "/buscador/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("pagina", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron recetas con los criterios indicados")));
        ArgumentCaptor<Pageable> negativa = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio, times(6)).filtrarRecetas(any(), negativa.capture());
        assertThat(negativa.getAllValues().get(5).getPageNumber()).isZero();
    }

    // ==================== Buscadores de los filtros de la lista ====================

    @Test
    @DisplayName("CP-LCT-122: el buscador de planificaciones del filtro devuelve un fragmento que puede usar el operario, con el criterio de receta como selector de entidad y el estado legible de cada planificación")
    void filtroDePlanificaciones_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                planificacion(8L, "Stout", EstadoSolicitud.FINALIZADA), planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE))));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/filtro-planificacion :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).doesNotContain("<html").contains("action=\"/lotes/filtros/planificaciones\"").contains("data-modal-buscar");
        assertThat(html).contains("data-selector-buscar").contains("bi-list-ul").contains("data-buscador-url=\"/lotes/filtros/recetas\"")
                .contains("placeholder=\"Seleccione receta\"").contains("data-selector-limpiar")
                .containsPattern("<input[^>]*data-selector-id[^>]*name=\"idReceta\"[^>]*>")
                .doesNotContain("data-selector-menu").doesNotContain("name=\"receta\"").doesNotContain("placeholder=\"Ej: Pale Ale\"");
        assertThat(texto(html)).contains("Buscar planificación", "Planificación Receta Cantidad (L) Inicio estimado Estado");
        assertThat(celdas(html, "Stout")).contains("Finalizada");
        assertThat(celdas(html, "Pale Ale")).contains("Pendiente");
        assertThat(html).contains("data-id=\"7\"").contains("data-texto=\"Plan #7 - Pale Ale\"").contains("data-id=\"8\"").contains("data-texto=\"Plan #8 - Stout\"");
    }

    @Test
    @DisplayName("CP-LCT-123: el buscador de planificaciones del filtro ofrece las de cualquier estado, sin fecha, con la última registrada arriba, de a 8 y filtradas por la receta elegida")
    void filtroDePlanificaciones_cualquierEstadoRecetaYPaginacion() throws Exception {
        // Arrange
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(3L)).thenReturn(receta(3L, "Stout", Estado.ACTIVO));
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(4L)).thenReturn(receta(4L, "Pale Ale", Estado.ACTIVO));
        when(planificacionServicio.filtrarPlanificacionesProduccion(eq(3L), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                planificacion(9L, "Stout", EstadoSolicitud.PENDIENTE), planificacion(7L, "Stout", EstadoSolicitud.ANULADA))));
        List<PlanificacionProduccionResponseDTO> primera = new ArrayList<>();
        for (long id = 10; id >= 3; id--) {
            primera.add(planificacion(id, "Pale Ale", EstadoSolicitud.PENDIENTE));
        }
        when(planificacionServicio.filtrarPlanificacionesProduccion(eq(4L), any(), any(), any())).thenReturn(new PageImpl<>(primera, PageRequest.of(0, 8), 10));

        // Act
        mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());
        verifyNoInteractions(recetaServicio);
        String stout = mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", "3"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String pale = mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("idReceta", "4"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.GERENTE_DE_PRODUCCION)).param("pagina", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron planificaciones con los criterios indicados")));

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio, times(2)).filtrarPlanificacionesProduccion(isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getAllValues()).allSatisfy(p -> {
            assertThat(p.getPageSize()).isEqualTo(8);
            assertThat(p.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        });
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isZero();
        verify(planificacionServicio).filtrarPlanificacionesProduccion(eq(3L), isNull(), isNull(), any(Pageable.class));
        assertThat(stout).contains("data-texto=\"Plan #9 - Stout\"").contains("data-texto=\"Plan #7 - Stout\"").doesNotContain("Plan #8");
        assertThat(textoDelSelector(stout, "Seleccione receta")).isEqualTo("Stout");
        assertThat(campo(stout, "idReceta")).isEqualTo("3");
        assertThat(texto(pale)).contains("10 planificaciones · Página 1 de 2");
        assertThat(enlaceDe(pale, "Siguiente")).contains("pagina=1").contains("idReceta=4");
        assertThat(pale).contains("data-modal-get=\"true\"");
    }

    @Test
    @DisplayName("CP-LCT-124: el buscador de fermentadores del filtro devuelve un fragmento con los criterios de identificador, estado operativo e incluir dados de baja")
    void filtroDeFermentadores_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentador(1L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO), fermentador(2L, "F-02", EstadoOperativo.EN_USO, 400.5, Estado.ACTIVO))));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/filtro-fermentador :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).doesNotContain("<html").contains("placeholder=\"Ej: F-01\"").contains("action=\"/lotes/filtros/fermentadores\"").contains("data-modal-buscar");
        assertThat(texto(html)).contains("Buscar fermentador", "Estado operativo", "Incluir dados de baja", "Identificador Estado operativo Capacidad útil (L)");
        assertThat(html).contains(">Todos</option>").contains(">Disponible</option>").contains(">En uso</option>").contains(">En limpieza</option>").contains(">En mantenimiento</option>");
        Matcher casilla = Pattern.compile("<input[^>]*name=\"incluirDadosDeBaja\"[^>]*>").matcher(html);
        assertThat(casilla.find()).isTrue();
        assertThat(casilla.group()).contains("type=\"checkbox\"").doesNotContain("checked");
        assertThat(celdas(html, "F-01")).startsWith("F-01", "Disponible", "330");
        assertThat(celdas(html, "F-02")).startsWith("F-02", "En uso", "400,5");
        assertThat(html).contains("data-id=\"1\"").contains("data-texto=\"F-01\"").contains("data-id=\"2\"").contains("data-texto=\"F-02\"");
    }

    @Test
    @DisplayName("CP-LCT-125: sin la casilla solo se consultan los fermentadores activos; con ella se suman los dados de baja con lotes, marcados y con su texto")
    void filtroDeFermentadores_soloActivosODadosDeBaja() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentador(1L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO), fermentador(2L, "F-02", EstadoOperativo.DISPONIBLE, 330.0, Estado.BAJA))));

        // Act
        mockMvc.perform(get(URL + "/filtros/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION)).param("identificador", " f-0 ").param("estadoOperativo", "EN_USO"))
                .andExpect(status().isOk());
        String html = mockMvc.perform(get(URL + "/filtros/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION)).param("incluirDadosDeBaja", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadores(eq("f-0"), eq(EstadoOperativo.EN_USO), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(8);
        assertThat(pageable.getValue().getSort().getOrderFor("identificadorInterno").getDirection()).isEqualTo(Sort.Direction.ASC);
        verify(fermentadorServicio).filtrarFermentadoresConDadosDeBajaConLotes(isNull(), isNull(), any());
        verify(fermentadorServicio, times(1)).filtrarFermentadores(any(), any(), any());
        assertThat(celdas(html, "F-02").get(0)).isEqualTo("F-02 Dado de baja");
        assertThat(html).contains("data-fermentador-dado-de-baja").contains("data-texto=\"F-02 (dado de baja)\"").contains("data-texto=\"F-01\"");
        Matcher casilla = Pattern.compile("<input[^>]*name=\"incluirDadosDeBaja\"[^>]*>").matcher(html);
        assertThat(casilla.find()).isTrue();
        assertThat(casilla.group()).contains("checked");
    }

    @Test
    @DisplayName("CP-LCT-126: la paginación del buscador de fermentadores conserva los tres criterios; sin resultados lo informa y una página negativa pide la primera")
    void filtroDeFermentadores_paginacionYVacio() throws Exception {
        // Arrange
        when(fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fermentador(9L, "F-09", EstadoOperativo.EN_USO, 330.0, Estado.ACTIVO)), PageRequest.of(1, 8), 17));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("identificador", "F").param("estadoOperativo", "EN_USO").param("incluirDadosDeBaja", "true").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        when(fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get(URL + "/filtros/fermentadores").with(como(Rol.GERENTE_DE_PRODUCCION)).param("incluirDadosDeBaja", "true").param("pagina", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron fermentadores con los criterios indicados")));

        // Assert
        assertThat(texto(html)).contains("Página 2 de 3");
        for (String enlace : List.of(enlaceDe(html, "Anterior"), enlaceDe(html, "Siguiente"))) {
            assertThat(enlace).contains("identificador=F").contains("estadoOperativo=EN_USO").contains("incluirDadosDeBaja=true");
        }
        assertThat(enlaceDe(html, "Anterior")).contains("pagina=0");
        assertThat(enlaceDe(html, "Siguiente")).contains("pagina=2");
        assertThat(html).contains("data-modal-get=\"true\"");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio, times(2)).filtrarFermentadoresConDadosDeBajaConLotes(any(), any(), pageable.capture());
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isZero();
    }

    @Test
    @DisplayName("CP-LCT-127: los buscadores del filtro los usan el gerente y el operario porque exigen LOTE_CONSULTAR y no LOTE_REGISTRAR")
    void filtros_losUsanAmbosRoles() throws Exception {
        // Act + Assert
        for (RequestPostProcessor usuario : List.of(como(Rol.GERENTE_DE_PRODUCCION), como(Rol.OPERARIO_DE_PRODUCCION), conPermisos(Permiso.LOTE_CONSULTAR))) {
            mockMvc.perform(get(URL + "/filtros/planificaciones").with(usuario)).andExpect(status().isOk())
                    .andExpect(view().name("lote/filtro-planificacion :: buscador"));
            mockMvc.perform(get(URL + "/filtros/recetas").with(usuario)).andExpect(status().isOk())
                    .andExpect(view().name("planificacion_produccion/buscador-receta :: buscador"));
            mockMvc.perform(get(URL + "/filtros/fermentadores").with(usuario)).andExpect(status().isOk())
                    .andExpect(view().name("lote/filtro-fermentador :: buscador"));
        }
        assertRechazado(mockMvc.perform(get(URL + "/filtros/planificaciones").with(conPermisos(Permiso.LOTE_REGISTRAR))));
        assertRechazado(mockMvc.perform(get(URL + "/filtros/recetas").with(conPermisos(Permiso.LOTE_REGISTRAR))));
        assertRechazado(mockMvc.perform(get(URL + "/filtros/fermentadores").with(conPermisos(Permiso.LOTE_REGISTRAR))));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCT-128: los roles sin LOTE_CONSULTAR no pueden usar los buscadores del filtro: redirigen con alerta de error y no llegan a ningún service")
    void filtros_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(rol))));
        assertRechazado(mockMvc.perform(get(URL + "/filtros/recetas").with(como(rol))));
        assertRechazado(mockMvc.perform(get(URL + "/filtros/fermentadores").with(como(rol))));
        verifyNingunService();
    }

    @Test
    @DisplayName("CP-LCT-152: una receta dada de baja con planificaciones se puede elegir en el filtro: filtra sus planificaciones de cualquier estado y el selector la muestra con (dada de baja)")
    void filtroDePlanificaciones_admiteUnaRecetaDadaDeBaja() throws Exception {
        // Arrange
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(5L)).thenReturn(receta(5L, "Stout", Estado.BAJA));
        when(planificacionServicio.filtrarPlanificacionesProduccion(eq(5L), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                planificacion(12L, "Stout", EstadoSolicitud.ANULADA))));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idReceta", "5"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(planificacionServicio).filtrarPlanificacionesProduccion(eq(5L), isNull(), isNull(), any(Pageable.class));
        verify(recetaServicio, never()).buscarPorId(any());
        assertThat(textoDelSelector(html, "Seleccione receta")).isEqualTo("Stout (dada de baja)");
        assertThat(campo(html, "idReceta")).isEqualTo("5");
        assertThat(celdas(html, "Plan #12")).contains("Anulada");
    }

    @Test
    @DisplayName("CP-LCT-153: una receta inexistente o dada de baja sin planificaciones en el filtro se ignora sin aviso: se busca sin filtrar por receta y el selector queda en blanco")
    void filtroDePlanificaciones_ignoraUnaRecetaQueNoSeOfrece() throws Exception {
        // Arrange
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/planificaciones").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idReceta", "99"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(planificacionServicio).filtrarPlanificacionesProduccion(isNull(), isNull(), isNull(), any(Pageable.class));
        assertThat(html).doesNotContain("<html").doesNotContain("role=\"alert\"");
        assertThat(textoDelSelector(html, "Seleccione receta")).isEmpty();
        assertThat(campoOVacio(html, "idReceta")).isEmpty();
    }

    @Test
    @DisplayName("CP-LCT-154: el buscador de recetas del filtro devuelve el fragmento compartido, que puede usar el operario, con la casilla Incluir dados de baja desmarcada")
    void filtroDeRecetas_debeDevolverElFragmento() throws Exception {
        // Arrange
        when(recetaServicio.filtrarRecetas(any(), any())).thenReturn(new PageImpl<>(List.of(
                receta(3L, "Pale Ale", Estado.ACTIVO), receta(4L, "Stout", Estado.ACTIVO))));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/recetas").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/buscador-receta :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).doesNotContain("<html").contains("placeholder=\"Ej: Pale Ale\"").contains("action=\"/lotes/filtros/recetas\"").contains("data-modal-buscar")
                .doesNotContain("data-receta-dada-de-baja");
        assertThat(texto(html)).contains("Buscar receta", "Incluir dados de baja", "Nombre Volumen base (L)");
        Matcher casilla = Pattern.compile("<input[^>]*name=\"incluirDadosDeBaja\"[^>]*>").matcher(html);
        assertThat(casilla.find()).isTrue();
        assertThat(casilla.group()).contains("type=\"checkbox\"").doesNotContain("checked");
        assertThat(html).contains("data-id=\"3\"").contains("data-texto=\"Pale Ale\"").contains("data-id=\"4\"").contains("data-texto=\"Stout\"");
    }

    @Test
    @DisplayName("CP-LCT-155: sin la casilla el buscador de recetas del filtro consulta solo las activas; con ella se suman las dadas de baja con planificaciones, marcadas y con su texto")
    void filtroDeRecetas_soloActivasODadasDeBaja() throws Exception {
        // Arrange
        when(recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any())).thenReturn(new PageImpl<>(List.of(
                receta(3L, "Pale Ale", Estado.ACTIVO), receta(4L, "Stout", Estado.BAJA))));

        // Act
        mockMvc.perform(get(URL + "/filtros/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("nombre", " pale ")).andExpect(status().isOk());
        String html = mockMvc.perform(get(URL + "/filtros/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("incluirDadosDeBaja", "true"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio).filtrarRecetas(eq("pale"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(8);
        assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
        verify(recetaServicio).filtrarRecetasConDadasDeBajaConPlanificaciones(isNull(), any(Pageable.class));
        verify(recetaServicio, times(1)).filtrarRecetas(any(), any());
        assertThat(celdas(html, "Stout").get(0)).isEqualTo("Stout Dada de baja");
        assertThat(html).contains("data-receta-dada-de-baja").contains("data-texto=\"Stout (dada de baja)\"").contains("data-texto=\"Pale Ale\"");
        assertThat(cuenta(html, "data-receta-dada-de-baja")).isEqualTo(1);
        Matcher casilla = Pattern.compile("<input[^>]*name=\"incluirDadosDeBaja\"[^>]*>").matcher(html);
        assertThat(casilla.find()).isTrue();
        assertThat(casilla.group()).contains("checked");
    }

    @Test
    @DisplayName("CP-LCT-156: la paginación del buscador de recetas del filtro conserva el nombre y la casilla; sin resultados lo informa y una página negativa pide la primera")
    void filtroDeRecetas_paginacionYVacio() throws Exception {
        // Arrange
        when(recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any())).thenReturn(new PageImpl<>(List.of(
                receta(3L, "Pale Ale", Estado.ACTIVO)), PageRequest.of(1, 8), 20));

        // Act
        String html = mockMvc.perform(get(URL + "/filtros/recetas").with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("nombre", "pale").param("incluirDadosDeBaja", "true").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        when(recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get(URL + "/filtros/recetas").with(como(Rol.GERENTE_DE_PRODUCCION)).param("incluirDadosDeBaja", "true").param("pagina", "-3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron recetas con los criterios indicados")));

        // Assert
        assertThat(texto(html)).contains("Página 2 de 3");
        for (String enlace : List.of(enlaceDe(html, "Anterior"), enlaceDe(html, "Siguiente"))) {
            assertThat(enlace).contains("nombre=pale").contains("incluirDadosDeBaja=true");
        }
        assertThat(enlaceDe(html, "Anterior")).contains("pagina=0");
        assertThat(enlaceDe(html, "Siguiente")).contains("pagina=2");
        assertThat(html).contains("data-modal-get=\"true\"");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio, times(2)).filtrarRecetasConDadasDeBajaConPlanificaciones(any(), pageable.capture());
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isZero();
    }

    // ==================== Transversales ====================

    @Test
    @DisplayName("CP-LCT-129: toda acción exitosa es un Post-Redirect-Get: registrar redirige a /lotes y las demás a /lotes/{id}, todas con la alerta de éxito")
    void transversal_todasLasAccionesSonPostRedirectGet() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).params(formularioValido()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/lotes")).andExpect(flash().attribute("tipo", "success"));
        mockMvc.perform(post(URL + "/12/iniciar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/lotes/12")).andExpect(flash().attribute("tipo", "success"));
        mockMvc.perform(post(URL + "/12/cancelar").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("motivo", "Cambio de plan"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/lotes/12")).andExpect(flash().attribute("tipo", "success"));
        for (String etapa : ETAPAS) {
            mockMvc.perform(post(URL + "/12/etapas/" + etapa + "/finalizar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/lotes/12")).andExpect(flash().attribute("tipo", "success"));
        }
    }

    @Test
    @DisplayName("CP-LCT-130: un rechazo sin Referer redirige a la raíz con la alerta de advertencia y el mensaje del service")
    void transversal_rechazoSinReferer() throws Exception {
        // Arrange
        when(loteServicio.iniciarLote(12L)).thenThrow(new ReglaNegocioException("El lote debe encontrarse en estado Pendiente para poder iniciarse"));

        // Act + Assert
        mockMvc.perform(post(URL + "/12/iniciar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("mensaje", "El lote debe encontrarse en estado Pendiente para poder iniciarse"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-LCT-131: sin JavaScript las acciones funcionan: iniciar y finalizar son formularios POST con action y CSRF y data-un-solo-envio, las pestañas son enlaces y los formularios son novalidate")
    void transversal_funcionaSinJavaScript() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());

        // Act
        String pendiente = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, null);
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        String enEjecucion = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido");
        String alta = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        Matcher iniciar = Pattern.compile("(?s)<form[^>]*action=\"/lotes/12/iniciar\"[^>]*>(.*?)</form>").matcher(pendiente);
        assertThat(iniciar.find()).isTrue();
        assertThat(iniciar.group()).contains("method=\"post\"").contains("action=\"/lotes/12/iniciar\"").contains("name=\"_csrf\"").contains("type=\"submit\"")
                .contains("data-un-solo-envio");
        Matcher finalizar = Pattern.compile("(?s)<form[^>]*action=\"/lotes/12/etapas/hervido/finalizar\"[^>]*>").matcher(enEjecucion);
        assertThat(finalizar.find()).isTrue();
        assertThat(finalizar.group()).contains("data-un-solo-envio");
        assertThat(cuenta(enEjecucion, "<a href=\"/lotes/12?etapa=")).isEqualTo(6);
        assertThat(enEjecucion).doesNotContain("data-tab-js");
        Matcher formularioAlta = Pattern.compile("<form[^>]*id=\"lote-form\"[^>]*>").matcher(alta);
        assertThat(formularioAlta.find()).isTrue();
        assertThat(formularioAlta.group()).contains("method=\"post\"").contains("novalidate").contains("action=\"/lotes\"");
    }

    @Test
    @DisplayName("CP-LCT-132: los textos de la interfaz son uniformes: ninguna alerta termina en punto, no hay emojis y las unidades van en las cabeceras")
    void transversal_textosDeLaInterfaz() throws Exception {
        // Arrange
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(AguaLoteResponseDTO.builder().masaMalta(45.0).volumenPreHervor(145.0).aguaMacerado(139.0).aguaLavado(0.0)
                .aguaTotal(139.0).relacionDeEmpaste(3.0).espacioMuerto(4.0)
                .advertencias(List.of("El mosto extraído del macerador (150 L) supera el volumen pre-hervor (145 L), por lo que no se requiere agua de lavado")).build());

        // Act
        List<String> paginas = List.of(
                mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).param("idPlanificacion", "abc").param("estado", "XYZ"))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion"),
                detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "xyz"),
                mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf())).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        // Assert
        Pattern alerta = Pattern.compile("(?s)role=\"alert\"[^>]*>(.*?)</div>");
        Pattern emoji = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}]");
        for (String html : paginas) {
            Matcher alertas = alerta.matcher(html);
            int cantidad = 0;
            while (alertas.find()) {
                cantidad++;
                assertThat(texto(alertas.group(1))).isNotEmpty().doesNotEndWith(".");
            }
            assertThat(cantidad).isPositive();
            assertThat(emoji.matcher(html).find()).isFalse();
        }
    }

    @Test
    @DisplayName("CP-LCT-134: un estado operativo que no existe en los cuatro buscadores de equipos y en el del filtro de fermentadores se ignora con un aviso dentro del fragmento y se buscan todos; el nombre se acepta sin distinguir mayúsculas")
    void buscadores_estadoOperativoInvalido() throws Exception {
        // Act + Assert: un valor inválido no rompe el modal
        for (String ruta : List.of("/buscador/molinos", "/buscador/maceradores", "/buscador/ollas", "/buscador/fermentadores", "/filtros/fermentadores")) {
            String html = mockMvc.perform(get(URL + ruta).with(como(Rol.GERENTE_DE_PRODUCCION)).param("estadoOperativo", "XYZ"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(texto(bloque(html, "data-aviso-buscador"))).isEqualTo("El estado operativo indicado no es válido, se muestran todos");
            assertThat(html).contains("data-modal-buscar").doesNotContain("<html");
        }
        verify(molinoServicio).filtrarMolinos(isNull(), isNull(), any());
        verify(maceradorServicio).filtrarMaceradores(isNull(), isNull(), any());
        verify(ollaHervorServicio).filtrarOllasHervor(isNull(), isNull(), any());
        verify(fermentadorServicio, times(2)).filtrarFermentadores(isNull(), isNull(), any());

        // Act + Assert: el nombre válido se interpreta y no hay aviso
        String html = mockMvc.perform(get(URL + "/buscador/molinos").with(como(Rol.GERENTE_DE_PRODUCCION)).param("estadoOperativo", "disponible"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).doesNotContain("data-aviso-buscador");
        verify(molinoServicio).filtrarMolinos(isNull(), eq(EstadoOperativo.DISPONIBLE), any());
    }

    @Test
    @DisplayName("CP-LCT-135: un valor de la URL que no se puede convertir (lote, página o casilla) en una pantalla redirige con la alerta amarilla El valor indicado no es válido y en un buscador responde 400 con el mensaje, sin error inesperado")
    void transversal_valoresInvalidosEnLaUrl() throws Exception {
        // Act + Assert: las pantallas y las acciones vuelven al origen con un aviso
        List<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> pantallas = List.of(
                get(URL + "/abc"),
                get(URL + "?pagina=xyz"),
                post(URL + "/abc/iniciar").with(csrf()),
                post(URL + "/abc/cancelar").with(csrf()).param("motivo", "Error de carga"),
                post(URL + "/abc/etapas/hervido/finalizar").with(csrf()));
        for (org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pantalla : pantallas) {
            assertAvisoDeValorInvalido(mockMvc.perform(pantalla.with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/lotes")));
        }

        // Act + Assert: un modal no puede recibir una página completa
        String[][] buscadores = {
                {"/buscador/planificaciones", "pagina"}, {"/buscador/recetas", "pagina"}, {"/buscador/molinos", "pagina"},
                {"/filtros/planificaciones", "pagina"}, {"/filtros/recetas", "pagina"}, {"/filtros/fermentadores", "pagina"},
                {"/buscador/planificaciones", "idReceta"}, {"/filtros/planificaciones", "idReceta"},
                {"/filtros/recetas", "incluirDadosDeBaja"}, {"/filtros/fermentadores", "incluirDadosDeBaja"}};
        for (String[] buscador : buscadores) {
            mockMvc.perform(get(URL + buscador[0]).with(como(Rol.GERENTE_DE_PRODUCCION)).param(buscador[1], "xyz").header("Referer", "/lotes"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andExpect(content().string("El valor indicado no es válido"));
        }
    }

    @Test
    @DisplayName("CP-LCT-136: la guía de Hervido muestra una línea por adición con su propia cantidad (un lúpulo usado dos veces aparece dos veces, sin sumar): al inicio del hervor si hierve toda la duración, cuando faltan N minutos si no, y en el whirlpool; primero las de hervor de la más temprana a la más tardía y al final el whirlpool")
    void detalle_guiaDeHervidoUnaLineaPorAdicion() throws Exception {
        // Arrange
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));
        when(consumoInsumoServicio.filtrarAdicionesLupulo(1003L)).thenReturn(List.of(
                adicion("Mosaic", UsoLupulo.WHIRLPOOL, 0.0, 7.0), adicion("Cascade", UsoLupulo.HERVOR, 15.0, 15.5),
                adicion("Saaz", UsoLupulo.HERVOR, 90.0, 215.0), adicion("Cascade", UsoLupulo.HERVOR, 60.0, 25.0),
                adicion("Mosaic", UsoLupulo.HERVOR, 10.0, 5.0), adicion("Citra", UsoLupulo.DRY_HOP, 0.0, 40.0)));

        // Act
        String guia = bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "hervido"), "data-guia");

        // Assert
        List<String> lineas = new ArrayList<>();
        Matcher m = Pattern.compile("(?s)<li[^>]*data-lupulo[^>]*>.*?</li>").matcher(guia);
        while (m.find()) {
            lineas.add(texto(m.group()));
        }
        assertThat(lineas).containsExactly(
                "Saaz: 215 g, agregar al inicio del hervor",
                "Cascade: 25 g, agregar cuando falten 60 min para terminar el hervor",
                "Cascade: 15,5 g, agregar cuando falten 15 min para terminar el hervor",
                "Mosaic: 5 g, agregar cuando falten 10 min para terminar el hervor",
                "Mosaic: 7 g, agregar en el whirlpool");
        assertThat(guia).doesNotContain("<table").doesNotContain("Citra");
    }

    @Test
    @DisplayName("CP-LCT-138: la última columna de la lista, Acciones, tiene en cada fila el botón de ícono Gestionar lote (bi-gear, con tooltip, igual que los botones de las otras listas) hacia el detalle, y la cabecera y el botón quedan alineados a la derecha dentro de la columna; la fila no es clickeable y el código es texto")
    void listar_cadaFilaTieneElBotonGestionarLote() throws Exception {
        // Arrange
        LoteCronogramaResponseDTO lote = loteDeLista(12L, "PALE_ALE-R7-3", EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, "F-01");
        when(loteServicio.filtrarLotesParaListado(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(lote)));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        Matcher boton = Pattern.compile("<a[^>]*data-gestionar-lote[^>]*>.*?</a>", Pattern.DOTALL).matcher(html);
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).contains("href=\"/lotes/12\"").contains("title=\"Gestionar lote\"").contains("aria-label=\"Gestionar lote\"").contains("bi-gear").contains("hover:bg-amber-500");
        assertThat(html).doesNotContain("sticky right-0").contains("text-center");
        assertThat(html.substring(html.indexOf("<thead"), html.indexOf("</thead>"))).contains("text-center");
        assertThat(texto(boton.group())).isEmpty();
        assertThat(boton.find()).isFalse();
        assertThat(celdas(html, "PALE_ALE-R7-3").get(0)).isEqualTo("PALE_ALE-R7-3");
        assertThat(texto(html.substring(html.indexOf("<thead"), html.indexOf("</thead>")))).endsWith("Acciones");
        assertThat(html).doesNotContain("data-fila-lote").doesNotContain("data-href").doesNotContain("/js/lote-lista.js");
        assertThat(html).doesNotContain("bi-eye").doesNotContain("aria-label=\"Ver lote\"");
    }

    @Test
    @DisplayName("CP-LCT-139: la cabecera muestra siempre las cuatro fechas (inicio y fin estimados, inicio y fin reales) en cualquier estado del lote, con un guion en las reales que todavía no existen y la fecha de cancelación como fin real del lote cancelado")
    void detalle_fechasDeLaCabeceraSegunElEstado() throws Exception {
        // Arrange + Act
        stubDetalle(detalle(EstadoLote.PENDIENTE, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE)).build());
        String pendiente = texto(bloque(detalleHtml(Rol.GERENTE_DE_PRODUCCION, null), "data-lote-cabecera"));
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.MOLIENDA, etapas(EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA,
                EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA))
                .fechaInicio(null).fechaCancelacion(LocalDateTime.of(2027, 6, 30, 9, 0)).motivoCancelacion("Cambio de plan").build());
        String canceladoSinEmpezar = texto(bloque(detalleHtml(Rol.GERENTE_DE_PRODUCCION, null), "data-lote-cabecera"));
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.HERVIDO, etapasEnCurso(TipoEtapa.HERVIDO)).build());
        String enEjecucion = texto(bloque(detalleHtml(Rol.GERENTE_DE_PRODUCCION, null), "data-lote-cabecera"));
        stubDetalle(detalle(EstadoLote.FINALIZADO, TipoEtapa.ENVASADO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA))
                .fechaFinalizacion(LocalDateTime.of(2027, 7, 5, 17, 0)).build());
        String finalizado = texto(bloque(detalleHtml(Rol.GERENTE_DE_PRODUCCION, null), "data-lote-cabecera"));
        stubDetalle(detalle(EstadoLote.CANCELADO, TipoEtapa.HERVIDO, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA, EstadoEtapaLote.CANCELADA))
                .fechaCancelacion(LocalDateTime.of(2027, 7, 3, 10, 15)).motivoCancelacion("Cambio de plan").build());
        String canceladoQueEmpezo = texto(bloque(detalleHtml(Rol.GERENTE_DE_PRODUCCION, null), "data-lote-cabecera"));

        // Assert
        assertThat(pendiente).contains("Inicio estimado 01/07/2027", "Fin estimado 05/07/2027", "Inicio real -", "Fin real -");
        assertThat(canceladoSinEmpezar).contains("Inicio estimado 01/07/2027", "Fin estimado 05/07/2027", "Inicio real -", "Fin real 30/06/2027 09:00");
        assertThat(enEjecucion).contains("Inicio estimado 01/07/2027", "Fin estimado 05/07/2027", "Inicio real 01/07/2027 08:30", "Fin real -");
        assertThat(finalizado).contains("Inicio estimado 01/07/2027", "Fin estimado 05/07/2027", "Inicio real 01/07/2027 08:30", "Fin real 05/07/2027 17:00");
        assertThat(canceladoQueEmpezo).contains("Inicio estimado 01/07/2027", "Fin estimado 05/07/2027", "Inicio real 01/07/2027 08:30", "Fin real 03/07/2027 10:15");
    }

    @Test
    @DisplayName("CP-LCT-140: junto a Agua de macerado hay un botón de información (bi-info-circle, aria-label) que muestra como tooltip flotante el cálculo, contenido en un elemento oculto que no ocupa lugar en el diseño; las demás etapas no lo tienen")
    void detalle_tooltipDelAguaDeMacerado() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, TipoEtapa.MACERACION, etapasEnCurso(TipoEtapa.MACERACION)).build());
        when(loteServicio.calcularAguaDelLote(12L)).thenReturn(agua(60.0, List.of()));

        // Act
        String html = detalleHtml(Rol.OPERARIO_DE_PRODUCCION, "maceracion");

        // Assert
        String aguaMacerado = bloque(html, "data-agua-macerado");
        Matcher boton = Pattern.compile("<button[^>]*data-tooltip-contenido[^>]*>").matcher(aguaMacerado);
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).contains("type=\"button\"").contains("data-tooltip-contenido=\"info-agua-macerado\"").containsPattern("aria-label=\"[^\"]+\"")
                .doesNotContain("aria-expanded").doesNotContain("aria-controls");
        assertThat(aguaMacerado).contains("bi-info-circle");
        assertThat(cuenta(html, "data-tooltip-contenido")).isEqualTo(1);
        String panel = elementoConAtributo(html, "div", "id=\"info-agua-macerado\"");
        assertThat(panel).contains("data-info-panel").contains("hidden");
        assertThat(texto(panel)).contains("Cómo se calcula el agua de macerado", "3 L/kg × 45 kg + 4 L de espacio muerto = 139 L", "El espacio muerto es el agua que queda retenida");
        assertThat(html).doesNotContain("/js/lote-detalle.js").doesNotContain("data-info-toggle").doesNotContain("aria-expanded");
        for (String otra : List.of("molienda", "hervido")) {
            assertThat(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, otra)).doesNotContain("data-tooltip-contenido").doesNotContain("info-agua-macerado");
        }
    }

    @Test
    @DisplayName("CP-LCT-133: matriz de permisos: cada endpoint procede solo con su permiso; con cualquier otro permiso de lote redirige con alerta de error y no invoca al service")
    void transversal_matrizDePermisos() throws Exception {
        // Arrange
        List<EndpointDeLote> endpoints = new ArrayList<>(List.of(
                new EndpointDeLote("GET /lotes", Permiso.LOTE_CONSULTAR, () -> get(URL)),
                new EndpointDeLote("GET /lotes/{id}", Permiso.LOTE_CONSULTAR, () -> get(URL + "/12")),
                new EndpointDeLote("GET /lotes/filtros/planificaciones", Permiso.LOTE_CONSULTAR, () -> get(URL + "/filtros/planificaciones")),
                new EndpointDeLote("GET /lotes/filtros/fermentadores", Permiso.LOTE_CONSULTAR, () -> get(URL + "/filtros/fermentadores")),
                new EndpointDeLote("GET /lotes/nuevo", Permiso.LOTE_REGISTRAR, () -> get(URL + "/nuevo")),
                new EndpointDeLote("POST /lotes", Permiso.LOTE_REGISTRAR, () -> post(URL).params(formularioValido())),
                new EndpointDeLote("POST /lotes/{id}/iniciar", Permiso.LOTE_INICIAR, () -> post(URL + "/12/iniciar")),
                new EndpointDeLote("POST /lotes/{id}/cancelar", Permiso.LOTE_ANULAR, () -> post(URL + "/12/cancelar").param("motivo", "Cambio de plan"))));
        for (String buscador : List.of("planificaciones", "molinos", "maceradores", "ollas", "fermentadores")) {
            endpoints.add(new EndpointDeLote("GET /lotes/buscador/" + buscador, Permiso.LOTE_REGISTRAR, () -> get(URL + "/buscador/" + buscador)));
        }
        for (TipoEtapa etapa : TipoEtapa.values()) {
            endpoints.add(new EndpointDeLote("POST finalizar " + etapa, Permiso.valueOf("LOTE_FINALIZAR_" + etapa.name()),
                    () -> post(URL + "/12/etapas/" + etapa.name().toLowerCase(Locale.ROOT) + "/finalizar")));
        }
        List<Permiso> permisosDeLotes = Arrays.stream(Permiso.values())
                .filter(p -> p.name().startsWith("LOTE_") || p.name().startsWith("CONSUMO_INSUMO_") || p.name().startsWith("ENVASADO_")).toList();

        for (EndpointDeLote endpoint : endpoints) {
            // Act + Assert: con solo su permiso procede
            org.springframework.test.web.servlet.ResultActions conPermiso = mockMvc.perform(endpoint.peticion().get().with(conPermisos(endpoint.permiso())).with(csrf()));
            if (endpoint.esConsulta()) {
                conPermiso.andExpect(status().isOk());
            } else {
                conPermiso.andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "success"));
            }

            // Act + Assert: con todos los demás permisos de lotes, consumos y envasados es rechazado
            org.mockito.Mockito.clearInvocations(loteServicio, consumoInsumoServicio, envasadoLoteServicio, planificacionServicio, molinoServicio, maceradorServicio,
                    ollaHervorServicio, fermentadorServicio);
            Permiso[] otros = permisosDeLotes.stream().filter(p -> p != endpoint.permiso()).toArray(Permiso[]::new);
            assertRechazado(mockMvc.perform(endpoint.peticion().get().with(conPermisos(otros)).with(csrf())));
            verifyNingunService();
        }
    }

    // ==================== Apoyo de las pruebas ====================

    private static final List<String> ETAPAS = List.of("molienda", "maceracion", "hervido", "fermentacion", "maduracion", "envasado");

    /**
     * Un endpoint del controller con el permiso que exige y cómo se pide.
     */
    private record EndpointDeLote(String nombre, Permiso permiso, java.util.function.Supplier<org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> peticion) {

        boolean esConsulta() {
            return nombre.startsWith("GET");
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private void finalizarCorrectamente(String etapa, String mensaje) throws Exception {
        mockMvc.perform(post(URL + "/12/etapas/" + etapa + "/finalizar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "success"));
    }

    private void finalizarRechazado(String etapa, String mensaje) throws Exception {
        mockMvc.perform(post(URL + "/12/etapas/" + etapa + "/finalizar").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", "/lotes/12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    private void assertRechazado(org.springframework.test.web.servlet.ResultActions resultado) throws Exception {
        resultado.andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"))
                .andExpect(flash().attribute("mensaje", SIN_PERMISOS));
    }

    private void verifyNingunService() {
        verifyNoInteractions(loteServicio, consumoInsumoServicio, envasadoLoteServicio, planificacionServicio, molinoServicio, maceradorServicio,
                ollaHervorServicio, fermentadorServicio, recetaServicio);
    }

    private void assertAvisoDeValorInvalido(org.springframework.test.web.servlet.ResultActions resultado) throws Exception {
        resultado.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    private void stubDetalle(LoteDetalleResponseDTO detalle) {
        when(loteServicio.buscarDetallePorId(12L)).thenReturn(detalle);
    }

    private void stubEntidadesDelFormulario() {
        when(molinoServicio.buscarPorId(1L)).thenReturn(MolinoResponseDTO.builder().id(1L).identificadorInterno("M-01").build());
        when(maceradorServicio.buscarPorId(2L)).thenReturn(MaceradorResponseDTO.builder().id(2L).identificadorInterno("MA-01").build());
        when(ollaHervorServicio.buscarPorId(3L)).thenReturn(OllaHervorResponseDTO.builder().id(3L).identificadorInterno("OH-01").build());
        when(fermentadorServicio.buscarPorId(4L)).thenReturn(fermentador(4L, "F-01", EstadoOperativo.DISPONIBLE, 330.0, Estado.ACTIVO));
    }

    private String detalleHtml(Rol rol, String etapa) throws Exception {
        return detalleHtml(como(rol), etapa);
    }

    private String detalleHtml(RequestPostProcessor usuario, String etapa) throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion = get(URL + "/12").with(usuario);
        if (etapa != null) {
            peticion.param("etapa", etapa);
        }
        return mockMvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String franja(String etapa) throws Exception {
        return texto(bloque(detalleHtml(Rol.OPERARIO_DE_PRODUCCION, etapa), "data-franja-estado"));
    }

    private static org.springframework.util.MultiValueMap<String, String> formularioValido() {
        org.springframework.util.LinkedMultiValueMap<String, String> parametros = new org.springframework.util.LinkedMultiValueMap<>();
        parametros.add("idPlanificacionProduccion", "7");
        parametros.add("idMolino", "1");
        parametros.add("idMacerador", "2");
        parametros.add("idOllaHervor", "3");
        parametros.add("idFermentador", "4");
        parametros.add("volumenObjetivo", "300");
        return parametros;
    }

    // ---------- Usuarios ----------

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

    // ---------- Datos de prueba ----------

    private static LoteResponseDTO respuestaDeLote() {
        return LoteResponseDTO.builder().id(12L).identificadorInterno("PALE_ALE-R7-3").build();
    }

    private static LoteCronogramaResponseDTO loteDeLista(Long id, String codigo, EstadoLote estado, TipoEtapa etapaActual, String fermentador) {
        return LoteCronogramaResponseDTO.builder().id(id).identificadorInterno(codigo).estado(estado).volumenObjetivo(300.0)
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 7, 5))
                .idPlanificacionProduccion(7L).nombreReceta("Pale Ale").idFermentador(fermentador != null ? 1L : null)
                .identificadorInternoFermentador(fermentador).etapaActual(etapaActual).build();
    }

    private static PlanificacionProduccionResponseDTO planificacion(Long id, String receta, EstadoSolicitud estado) {
        return PlanificacionProduccionResponseDTO.builder().id(id).cantidadAProducir(600.0).fechaInicioEstimada(LocalDate.of(2027, 7, 1))
                .estado(estado).versionReceta(VersionRecetaResponseDTO.builder().nombre(receta).build()).build();
    }

    private static RecetaResponseDTO receta(Long id, String nombre, Estado estado) {
        return RecetaResponseDTO.builder().id(id).estado(estado)
                .version(VersionRecetaResponseDTO.builder().nombre(nombre).volumenBase(20.0).build()).build();
    }

    private static FermentadorResponseDTO fermentador(Long id, String identificador, EstadoOperativo estadoOperativo, double capacidadUtil, Estado estado) {
        return FermentadorResponseDTO.builder().id(id).identificadorInterno(identificador).estadoOperativo(estadoOperativo)
                .capacidadUtil(capacidadUtil).estado(estado).build();
    }

    /**
     * Escenario base del detalle: lote 12 "PALE_ALE-R7-3" de la planificación 7 (Pale Ale, versión 3 de 3), de 300 L,
     * con fermentador "F-01" y la receta con sus duraciones y lúpulos.
     */
    private static LoteDetalleResponseDTO.LoteDetalleResponseDTOBuilder detalle(EstadoLote estado, TipoEtapa porDefecto, List<EtapaLoteResponseDTO> etapas) {
        return LoteDetalleResponseDTO.builder().id(12L).identificadorInterno("PALE_ALE-R7-3").estado(estado).volumenObjetivo(300.0)
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 7, 5))
                .fechaInicio(estado == EstadoLote.PENDIENTE ? null : LocalDateTime.of(2027, 7, 1, 8, 30))
                .idPlanificacionProduccion(7L).idFermentador(14L).identificadorInternoFermentador("F-01")
                .versionReceta(VersionRecetaDePlanificacionResponseDTO.builder().idPlanificacion(7L).idReceta(3L).numeroVersion(3).cantidadVersiones(3).version(receta()).build())
                .etapas(etapas).etapaPorDefecto(porDefecto);
    }

    private static VersionRecetaResponseDTO receta() {
        return VersionRecetaResponseDTO.builder().id(30L).nombre("Pale Ale").relacionDeEmpaste(3.0).ogObjetivo(1.050).fgObjetivo(1.010)
                .duracionMaceracion(60).duracionHervido(90).duracionFermentacion(10).duracionMaduracion(4)
                .detallesLupulo(List.of(
                        DetalleLupuloResponseDTO.builder().cantidad(25.0).uso(UsoLupulo.HERVOR).etapaDeUso(TipoEtapa.HERVIDO).tiempoDeHervor(60.0).lupulo(cascade()).build(),
                        DetalleLupuloResponseDTO.builder().cantidad(15.0).uso(UsoLupulo.WHIRLPOOL).etapaDeUso(TipoEtapa.HERVIDO).tiempoDeHervor(0.0).lupulo(citra()).build(),
                        DetalleLupuloResponseDTO.builder().cantidad(40.0).uso(UsoLupulo.DRY_HOP).etapaDeUso(TipoEtapa.MADURACION).tiempoDeHervor(0.0).lupulo(citra()).build()))
                .detallesLevadura(List.of(DetalleLevaduraResponseDTO.builder().cantidad(11.5).levadura(levadura()).build()))
                .build();
    }

    private static List<EtapaLoteResponseDTO> etapas(EstadoEtapaLote... estados) {
        List<EtapaLoteResponseDTO> etapas = new ArrayList<>();
        for (TipoEtapa tipo : TipoEtapa.values()) {
            etapas.add(etapa(tipo, estados[tipo.ordinal()]));
        }
        return etapas;
    }

    /**
     * Las etapas anteriores a la indicada finalizadas, la indicada en curso y las siguientes pendientes.
     */
    private static List<EtapaLoteResponseDTO> etapasEnCurso(TipoEtapa enCurso) {
        EstadoEtapaLote[] estados = new EstadoEtapaLote[TipoEtapa.values().length];
        for (TipoEtapa tipo : TipoEtapa.values()) {
            estados[tipo.ordinal()] = tipo.ordinal() < enCurso.ordinal() ? EstadoEtapaLote.FINALIZADA
                    : (tipo == enCurso ? EstadoEtapaLote.EN_CURSO : EstadoEtapaLote.PENDIENTE);
        }
        return etapas(estados);
    }

    private static EtapaLoteResponseDTO etapa(TipoEtapa tipo, EstadoEtapaLote estado) {
        return etapa(tipo, estado, null, null);
    }

    private static EtapaLoteResponseDTO etapa(TipoEtapa tipo, EstadoEtapaLote estado, LocalDateTime inicio, LocalDateTime fin) {
        return EtapaLoteResponseDTO.builder().id(1001L + tipo.ordinal()).etapa(tipo).estado(estado).fechaInicio(inicio).fechaFinalizacion(fin)
                .equipamiento(switch (tipo) {
                    case MOLIENDA -> equipamiento(11L, "M-01");
                    case MACERACION -> equipamiento(12L, "MA-01");
                    case HERVIDO -> equipamiento(13L, "OH-01");
                    default -> equipamiento(14L, "F-01");
                }).build();
    }

    private static EquipamientoResponseDTO equipamiento(Long id, String identificador) {
        return EquipamientoResponseDTO.builder().id(id).identificadorInterno(identificador).build();
    }

    private static InsumoResponseDTO pilsen() {
        return MaltaResponseDTO.builder().id(21L).nombre("Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
    }

    private static InsumoResponseDTO caramelo() {
        return MaltaResponseDTO.builder().id(22L).nombre("Caramelo 60").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
    }

    private static LupuloResponseDTO cascade() {
        return LupuloResponseDTO.builder().id(31L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static AdicionLupuloResponseDTO adicion(String nombre, UsoLupulo uso, double tiempoDeHervor, double cantidad) {
        return AdicionLupuloResponseDTO.builder().nombreInsumo(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).uso(uso)
                .tiempoDeHervor(tiempoDeHervor).cantidad(cantidad).build();
    }

    private static LupuloResponseDTO mosaic() {
        return LupuloResponseDTO.builder().id(33L).nombre("Mosaic").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static LupuloResponseDTO citra() {
        return LupuloResponseDTO.builder().id(32L).nombre("Citra").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static LevaduraResponseDTO levadura() {
        return LevaduraResponseDTO.builder().id(41L).nombre("US-05").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static InsumoRequeridoResponseDTO requerido(InsumoResponseDTO insumo, UnidadDeMedida unidad, double requerida, double reservada, double consumida) {
        return InsumoRequeridoResponseDTO.builder().insumo(insumo).unidadDeMedida(unidad).cantidadRequerida(requerida).cantidadReservada(reservada)
                .cantidadConsumida(consumida).porcentajeConsumido(requerida == 0 ? 0.0 : consumida * 100 / requerida).build();
    }

    private static AvanceInsumoResponseDTO avance(String nombre, double requerida, double consumida, double porcentaje, boolean cumple) {
        return AvanceInsumoResponseDTO.builder().nombreInsumo(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).requerida(requerida).consumida(consumida)
                .porcentaje(porcentaje).cumple(cumple).build();
    }

    private static ConsumoInsumoResponseDTO consumo(InsumoResponseDTO insumo, String lote, TipoConsumo tipo, double cantidad, EstadoTransaccion estado, String motivo) {
        return ConsumoInsumoResponseDTO.builder().cantidadConsumida(cantidad).tipoConsumo(tipo).estado(estado).motivoAnulacion(motivo)
                .loteInsumo(LoteInsumoResponseDTO.builder().identificacionLoteProveedor(lote).insumo(insumo).build()).build();
    }

    private static EnvasadoLoteResponseDTO envasado(String barril, double cantidad, EstadoTransaccion estado, String motivo) {
        return EnvasadoLoteResponseDTO.builder().cantidadEnvasada(cantidad).estado(estado).motivoAnulacion(motivo)
                .barril(BarrilResponseDTO.builder().identificador(barril).build()).build();
    }

    private static AguaLoteResponseDTO agua(double lavado, List<String> advertencias) {
        return AguaLoteResponseDTO.builder().volumenObjetivo(300.0).masaMalta(45.0).volumenPreHervor(345.0).aguaMacerado(139.0).aguaLavado(lavado)
                .aguaTotal(139.0 + lavado).relacionDeEmpaste(3.0).espacioMuerto(4.0).advertencias(advertencias).build();
    }

    private static ResumenEnvasadoResponseDTO resumen(double total, double capacidad, double restante, double exceso) {
        return ResumenEnvasadoResponseDTO.builder().totalEnvasado(total).capacidadUtilFermentador(capacidad).restante(restante)
                .excesoSobreVolumenObjetivo(exceso).identificadorFermentador("F-01").volumenObjetivo(300.0).build();
    }

    // ---------- Lectura del HTML ----------

    /**
     * El texto visible: sin etiquetas, comentarios ni scripts, con las entidades resueltas y los espacios colapsados.
     */
    private static String texto(String html) {
        String sinScripts = html.replaceAll("(?s)<script.*?</script>", "").replaceAll("(?s)<!--.*?-->", "");
        return HtmlUtils.htmlUnescape(sinScripts.replaceAll("(?s)<[^>]+>", "")).replaceAll("\\s+", " ").trim();
    }

    private static int cuenta(String html, String fragmento) {
        int cantidad = 0;
        for (int i = html.indexOf(fragmento); i >= 0; i = html.indexOf(fragmento, i + fragmento.length())) {
            cantidad++;
        }
        return cantidad;
    }

    /**
     * El elemento (de apertura a cierre) que lleva el atributo indicado.
     */
    private static String bloque(String html, String atributo) {
        Matcher m = Pattern.compile("(?s)<(\\w+)\\b[^>]*\\s" + Pattern.quote(atributo) + "(?=[\\s>=])[^>]*>.*?</\\1>").matcher(html);
        assertThat(m.find()).as("elemento con %s", atributo).isTrue();
        return m.group();
    }

    /**
     * El elemento de esa etiqueta que lleva el atributo con su valor (por ejemplo, {@code id="info-agua-macerado"}).
     */
    private static String elementoConAtributo(String html, String etiqueta, String atributoConValor) {
        Matcher m = Pattern.compile("(?s)<" + etiqueta + "\\b[^>]*\\s" + Pattern.quote(atributoConValor) + "[^>]*>.*?</" + etiqueta + ">").matcher(html);
        assertThat(m.find()).as("%s con %s", etiqueta, atributoConValor).isTrue();
        return m.group();
    }

    private static String pestanas(String html) {
        return texto(bloque(html, "data-pestanas"));
    }

    /**
     * El texto de cada celda de la primera fila cuyo contenido incluye la marca.
     */
    private static List<String> celdas(String html, String marca) {
        Matcher filas = Pattern.compile("(?s)<tr[^>]*>(.*?)</tr>").matcher(html);
        while (filas.find()) {
            if (filas.group(1).contains(marca)) {
                List<String> celdas = new ArrayList<>();
                Matcher celda = Pattern.compile("(?s)<td[^>]*>(.*?)</td>").matcher(filas.group(1));
                while (celda.find()) {
                    celdas.add(texto(celda.group(1)));
                }
                return celdas;
            }
        }
        throw new AssertionError("No hay una fila con " + marca);
    }

    /**
     * El primer elemento de esa etiqueta cuyo texto incluye el fragmento.
     */
    private static String elementoConTexto(String html, String etiqueta, String fragmento) {
        Matcher m = Pattern.compile("(?s)<" + etiqueta + "\\b[^>]*>.*?</" + etiqueta + ">").matcher(html);
        while (m.find()) {
            if (texto(m.group()).contains(fragmento)) {
                return m.group();
            }
        }
        throw new AssertionError("No hay " + etiqueta + " con " + fragmento);
    }

    private static String boton(String html) {
        Matcher m = Pattern.compile("<button[^>]*>").matcher(html);
        assertThat(m.find()).isTrue();
        return m.group();
    }

    /**
     * El destino (sin escapar) del enlace cuyo texto incluye la etiqueta.
     */
    private static String enlaceDe(String html, String etiqueta) {
        Matcher m = Pattern.compile("(?s)<a\\s[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>").matcher(html);
        while (m.find()) {
            if (texto(m.group(2)).contains(etiqueta)) {
                return HtmlUtils.htmlUnescape(m.group(1));
            }
        }
        throw new AssertionError("No hay un enlace " + etiqueta);
    }

    /**
     * El texto que muestra el selector de entidad con ese placeholder (vacío si no hay nada elegido).
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
     * El valor del campo con ese nombre, o vacío si el campo no tiene valor.
     */
    private static String campoOVacio(String html, String nombre) {
        Matcher m = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("campo %s", nombre).isTrue();
        Matcher valor = Pattern.compile("value=\"([^\"]*)\"").matcher(m.group());
        return valor.find() ? HtmlUtils.htmlUnescape(valor.group(1)) : "";
    }

    /**
     * El valor del campo con ese nombre.
     */
    private static String campo(String html, String nombre) {
        Matcher m = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("campo %s", nombre).isTrue();
        Matcher valor = Pattern.compile("value=\"([^\"]*)\"").matcher(m.group());
        assertThat(valor.find()).isTrue();
        return HtmlUtils.htmlUnescape(valor.group(1));
    }
}
