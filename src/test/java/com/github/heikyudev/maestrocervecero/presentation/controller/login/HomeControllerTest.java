package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardAdministradorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardEncargadoDepositoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardEncargadoDistribucionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardGerenteComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardGerenteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardOperarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ColaFermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ConteoDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilListoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ClienteConBarrilesResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardAdministradorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDistribucionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DevolucionPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteComprasResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraFinalizadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionSinOrdenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EventoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.UsuarioRecienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDepositoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.InsumoSinStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteInsumoVencidoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardGerenteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EtapaEnCursoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteEnProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.PlanificacionPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardOperarioResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EquipoPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteDelDiaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IDashboardOperarioServicio dashboardOperarioServicio;

    @MockitoBean
    private IDashboardGerenteServicio dashboardGerenteServicio;

    @MockitoBean
    private IDashboardEncargadoDepositoServicio dashboardEncargadoDepositoServicio;

    @MockitoBean
    private IDashboardAdministradorServicio dashboardAdministradorServicio;

    @MockitoBean
    private IDashboardGerenteComprasServicio dashboardGerenteComprasServicio;

    @MockitoBean
    private IDashboardEncargadoDistribucionServicio dashboardEncargadoDistribucionServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @DisplayName("CP-HCT-01: el OPERARIO_DE_PRODUCCION recibe el panel en el modelo, que se carga una sola vez, y ve el panel en lugar de las tarjetas de resumen")
    void home_debeCargarElPanelParaElOperario() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenReturn(panelConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardOperario"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(dashboardOperarioServicio).obtenerDashboardOperario();
        verifyNoInteractions(dashboardGerenteServicio);
        assertThat(html).contains("Panel de Control").contains("data-dashboard-operario").contains("data-dashboard-contadores")
                .contains("data-bloque=\"lotes-de-hoy\"").contains("data-bloque=\"limpiezas-pendientes\"").contains("data-bloque=\"pendientes-de-mantenimiento\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("Todos los servicios activos");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Rol.class)
    @DisplayName("CP-HCT-02: cada rol ve exactamente un panel, el suyo, y la pantalla de inicio no tiene tarjetas de resumen")
    void home_cadaRolVeExactamenteUnPanel(Rol rol) throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenReturn(panelVacio());
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenReturn(panelGerenteVacio());
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito()).thenReturn(panelEncargadoVacio());
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador()).thenReturn(panelAdministradorVacio());
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras()).thenReturn(panelComprasVacio());
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion()).thenReturn(panelDistribucionVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(rol)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        java.util.regex.Matcher paneles = java.util.regex.Pattern.compile("data-dashboard-(?!contadores)[a-z-]+(?=[\\s>])").matcher(html);
        List<String> encontrados = new ArrayList<>();
        while (paneles.find()) {
            encontrados.add(paneles.group());
        }
        assertThat(encontrados).hasSize(1);
        assertThat(html).contains("Panel de Control").doesNotContain("Estado del sistema");
    }

    @Test
    @DisplayName("CP-HCT-03: la ruta raíz también carga el panel del operario")
    void raiz_debeCargarElPanelParaElOperario() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenReturn(panelVacio());

        // Act and Assert
        mockMvc.perform(get("/").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardOperario"));
        verify(dashboardOperarioServicio).obtenerDashboardOperario();
    }

    @Test
    @DisplayName("CP-HCT-04: el panel del operario muestra los contadores con el total real de cada bloque y las sumas de equipos y barriles, con sus enlaces a las listas filtradas")
    void home_debeMostrarLosContadoresDelPanel() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenReturn(panelConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "lotes-en-ejecucion")).isEqualTo("7");
        assertThat(totalDelContador(html, "lotes-a-iniciar")).isEqualTo("4");
        assertThat(totalDelContador(html, "en-limpieza")).isEqualTo("5");
        assertThat(totalDelContador(html, "pendientes-de-mantenimiento")).isEqualTo("9");
        assertThat(html).contains("href=\"/lotes?estado=EN_EJECUCION\"")
                .contains("href=\"/lotes?estado=PENDIENTE&amp;inicioEstimadoHasta=" + LocalDate.now() + "\"")
                .contains("href=\"/control-equipamiento?estadoOperativo=EN_LIMPIEZA\"").contains("href=\"/control-barriles?estadoOperativo=EN_LIMPIEZA\"")
                .contains("href=\"/control-equipamiento?estadoOperativo=EN_MANTENIMIENTO\"").contains("href=\"/control-barriles?estadoOperativo=EN_MANTENIMIENTO\"")
                .contains(">3 equipos<").contains(">2 barriles<").contains(">6 equipos<").contains(">3 barriles<");
    }

    @Test
    @DisplayName("CP-HCT-05: el panel del operario muestra las filas de cada bloque con sus atajos: Abrir del lote con su etapa y Registrar limpieza o mantenimiento de cada equipo y barril")
    void home_debeMostrarLasFilasYSusAtajos() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenReturn(panelConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("href=\"/lotes/12?etapa=fermentacion\"").contains("aria-label=\"Abrir LOT-R7-03\"")
                .contains("href=\"/lotes/13\"").contains("aria-label=\"Abrir LOT-R7-04\"")
                .doesNotContain("Falta consumo").doesNotContain("Situación")
                .contains("href=\"/control-equipamiento/21/limpiezas/nueva\"").contains("aria-label=\"Registrar limpieza de FER-02\"")
                .contains("href=\"/control-barriles/31/limpiezas/nueva\"").contains("aria-label=\"Registrar limpieza de BRL-01\"")
                .contains("href=\"/control-equipamiento/22/mantenimientos/nuevo\"").contains("aria-label=\"Registrar mantenimiento de MAC-01\"")
                .contains("href=\"/control-barriles/32/mantenimientos/nuevo\"").contains("aria-label=\"Registrar mantenimiento de BRL-02\"")
                .contains("Falla").contains("Preventivo").contains("Barrilitos SA");
        assertThat(html).doesNotContain("No hay lotes en ejecución").doesNotContain("No hay barriles pendientes de mantenimiento");
    }

    @Test
    @DisplayName("CP-HCT-06: un panel en cero muestra los cuatro contadores con 0 y los seis textos de estado vacío, sin tablas")
    void home_debeMostrarLosEstadosVaciosConElPanelEnCero() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenReturn(panelVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        for (String contador : List.of("lotes-en-ejecucion", "lotes-a-iniciar", "en-limpieza", "pendientes-de-mantenimiento")) {
            assertThat(totalDelContador(html, contador)).as(contador).isEqualTo("0");
        }
        assertThat(html).contains(">No hay lotes en ejecución<").contains(">No hay lotes a iniciar<").contains(">No hay equipos en limpieza<")
                .contains(">No hay barriles en limpieza<").contains(">No hay equipos pendientes de mantenimiento<").contains(">No hay barriles pendientes de mantenimiento<");
        assertThat(html).doesNotContain("<table").doesNotContain("data-boton-abrir").doesNotContain("data-boton-registrar");
    }

    @Test
    @DisplayName("CP-HCT-07: un rechazo del service al armar el panel no redirige: la pantalla se muestra sin el panel y con la alerta del motivo")
    void home_unRechazoDelServiceMuestraLaPantallaSinElPanel() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenThrow(new RecursoNoEncontradoException("No se encontró la configuración de producción"));

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control: No se encontró la configuración de producción"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Panel de Control").contains("No se pudo cargar el panel de control: No se encontró la configuración de producción")
                .doesNotContain("data-dashboard-operario").doesNotContain("Estado del sistema");
    }

    @Test
    @DisplayName("CP-HCT-09: un fallo inesperado al armar el panel muestra la pantalla sin el panel y con la alerta genérica, sin exponer el detalle del error")
    void home_unFalloInesperadoMuestraLaAlertaGenerica() throws Exception {
        // Arrange
        when(dashboardOperarioServicio.obtenerDashboardOperario()).thenThrow(new IllegalStateException("detalle interno"));

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("No se pudo cargar el panel de control").doesNotContain("detalle interno").doesNotContain("data-dashboard-operario");
    }

    @Test
    @DisplayName("CP-HCT-10: el GERENTE_DE_PRODUCCION recibe su panel en el modelo, que se carga una sola vez, sin el del operario, y lo ve en lugar de las tarjetas de resumen")
    void home_debeCargarElPanelParaElGerente() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenReturn(panelGerenteConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardGerente"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(dashboardGerenteServicio).obtenerDashboardGerente();
        verifyNoInteractions(dashboardOperarioServicio);
        assertThat(html).contains("Panel de Control").contains("data-dashboard-gerente").contains("data-dashboard-contadores")
                .contains("data-bloque=\"produccion-en-curso\"").contains("data-bloque=\"cola-de-fermentadores\"").contains("data-bloque=\"equipamiento-y-barriles\"")
                .contains("data-bloque=\"planificaciones-pendientes\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("data-dashboard-operario");
    }

    @Test
    @DisplayName("CP-HCT-11: la ruta raíz también carga el panel del gerente")
    void raiz_debeCargarElPanelParaElGerente() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenReturn(panelGerenteVacio());

        // Act and Assert
        mockMvc.perform(get("/").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardGerente"));
        verify(dashboardGerenteServicio).obtenerDashboardGerente();
    }

    @Test
    @DisplayName("CP-HCT-12: el panel del gerente muestra los cuatro contadores con su número y sus enlaces a las listas")
    void home_debeMostrarLosContadoresDelPanelDelGerente() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenReturn(panelGerenteConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "lotes-en-ejecucion")).isEqualTo("7");
        assertThat(totalDelContador(html, "lotes-atrasados")).isEqualTo("2");
        assertThat(totalDelContador(html, "fermentadores-ocupados")).isEqualTo("3 / 5");
        assertThat(totalDelContador(html, "fuera-de-servicio")).isEqualTo("9");
        assertThat(html).contains("href=\"/lotes?estado=EN_EJECUCION\"").contains("href=\"/control-equipamiento?tipo=FERMENTADOR\"");
    }

    @Test
    @DisplayName("CP-HCT-13: el panel del gerente muestra la producción por etapa con atraso y prioridad, la cola de fermentadores, los conteos por estado con sus enlaces, y las planificaciones y lotes por iniciar")
    void home_debeMostrarLosBloquesDelPanelDelGerente() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenReturn(panelGerenteConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("data-etapa=\"FERMENTACION\"").contains("href=\"/lotes?estado=EN_EJECUCION&amp;etapaActual=FERMENTACION\"")
                .contains("href=\"/lotes/12?etapa=fermentacion\"").contains("aria-label=\"Abrir LOT-R7-03\"").contains("Atrasado 3 días").contains("Alta")
                .contains("data-cola-en-ejecucion").contains("data-cola-proximo").contains("LOT-R7-04").contains("2 lotes pendientes").contains("href=\"/cronograma-produccion\"")
                .contains("data-conteo=\"EN_LIMPIEZA\"").contains("href=\"/control-equipamiento?estadoOperativo=EN_LIMPIEZA\"").contains("href=\"/control-barriles?estadoOperativo=CON_CERVEZA\"")
                .contains("Plan #7 - Pale Ale").contains("href=\"/planificaciones-produccion/7\"").contains("aria-label=\"Ver Plan #7 - Pale Ale\"").contains("href=\"/planificaciones-produccion?estado=PENDIENTE\"")
                .contains("href=\"/lotes/12?etapa=fermentacion\"");
        assertThat(html).doesNotContain("No hay lotes en ejecución").doesNotContain("data-boton-registrar");
    }

    @Test
    @DisplayName("CP-HCT-14: un panel del gerente en cero muestra los contadores en 0 y los textos de estado vacío, sin tablas")
    void home_debeMostrarLosEstadosVaciosDelPanelDelGerente() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenReturn(panelGerenteVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        for (String contador : List.of("lotes-en-ejecucion", "lotes-atrasados", "fuera-de-servicio")) {
            assertThat(totalDelContador(html, contador)).as(contador).isEqualTo("0");
        }
        assertThat(totalDelContador(html, "fermentadores-ocupados")).isEqualTo("0 / 0");
        assertThat(html).contains(">No hay lotes en ejecución<").contains(">No hay fermentadores registrados<")
                .contains(">No hay planificaciones pendientes<");
        assertThat(html).doesNotContain("<table").doesNotContain("data-boton-abrir");
    }

    @Test
    @DisplayName("CP-HCT-15: un rechazo del service al armar el panel del gerente no redirige: la pantalla se muestra sin el panel y con la alerta del motivo")
    void home_unRechazoDelServiceDelGerenteMuestraLaPantallaSinElPanel() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenThrow(new RecursoNoEncontradoException("No se encontró la configuración de producción"));

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeDoesNotExist("dashboardGerente"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control: No se encontró la configuración de producción"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Panel de Control").contains("No se pudo cargar el panel de control: No se encontró la configuración de producción")
                .doesNotContain("data-dashboard-gerente").doesNotContain("Estado del sistema");
    }

    @Test
    @DisplayName("CP-HCT-16: un fallo inesperado al armar el panel del gerente muestra la pantalla sin el panel y con la alerta genérica, sin exponer el detalle del error")
    void home_unFalloInesperadoDelGerenteMuestraLaAlertaGenerica() throws Exception {
        // Arrange
        when(dashboardGerenteServicio.obtenerDashboardGerente()).thenThrow(new IllegalStateException("detalle interno"));

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardGerente"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("No se pudo cargar el panel de control").doesNotContain("detalle interno").doesNotContain("data-dashboard-gerente");
    }

    @Test
    @DisplayName("CP-HCT-17: el ENCARGADO_DE_DEPOSITO recibe su panel en el modelo, que se carga una sola vez, sin los demás, y lo ve en lugar de las tarjetas de resumen")
    void home_debeCargarElPanelParaElEncargadoDeDeposito() throws Exception {
        // Arrange
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito()).thenReturn(panelEncargadoConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardEncargadoDeposito"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andExpect(model().attributeDoesNotExist("dashboardGerente"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(dashboardEncargadoDepositoServicio).obtenerDashboardEncargadoDeposito();
        verifyNoInteractions(dashboardOperarioServicio, dashboardGerenteServicio);
        assertThat(html).contains("Panel de Control").contains("data-dashboard-encargado-deposito").contains("data-dashboard-contadores")
                .contains("data-bloque=\"ordenes-pendientes\"").contains("data-bloque=\"insumos-sin-stock\"").contains("data-bloque=\"lotes-vencidos\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("data-dashboard-operario").doesNotContain("data-dashboard-gerente");
    }

    @Test
    @DisplayName("CP-HCT-18: la ruta raíz también carga el panel del encargado de depósito")
    void raiz_debeCargarElPanelParaElEncargadoDeDeposito() throws Exception {
        // Arrange
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito()).thenReturn(panelEncargadoVacio());

        // Act and Assert
        mockMvc.perform(get("/").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardEncargadoDeposito"));
        verify(dashboardEncargadoDepositoServicio).obtenerDashboardEncargadoDeposito();
    }

    @Test
    @DisplayName("CP-HCT-19: el panel del encargado de depósito muestra los cuatro contadores con su número y sus enlaces a las listas")
    void home_debeMostrarLosContadoresDelPanelDelEncargado() throws Exception {
        // Arrange
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito()).thenReturn(panelEncargadoConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "ordenes-pendientes")).isEqualTo("9");
        assertThat(totalDelContador(html, "ordenes-entrega-vencida")).isEqualTo("4");
        assertThat(totalDelContador(html, "lotes-vencidos")).isEqualTo("12");
        assertThat(totalDelContador(html, "insumos-sin-stock")).isEqualTo("6");
        assertThat(html).contains("href=\"/ordenes-compra?estado=PENDIENTE\"").contains("href=\"/stock-insumos?situacionStock=CON_VENCIDOS\"")
                .contains("href=\"/stock-insumos?situacionStock=SIN_STOCK_DISPONIBLE\"");
    }

    @Test
    @DisplayName("CP-HCT-20: el panel del encargado de depósito muestra las filas con sus atajos: Registrar ingreso y Ver de cada orden, Registrar ajuste y Abrir de cada lote vencido y Abrir de cada insumo sin stock")
    void home_debeMostrarLasFilasYSusAtajosDelEncargado() throws Exception {
        // Arrange
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito()).thenReturn(panelEncargadoConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Orden de compra #7").contains("Malterías del Sur S.A.")
                .contains("href=\"/ingresos-insumo/compra/nuevo?idOrdenCompra=7\"").contains("aria-label=\"Registrar ingreso de la orden de compra #7\"")
                .contains("href=\"/ordenes-compra/7\"").contains("aria-label=\"Ver orden de compra #7\"")
                .contains("Malta Pilsen").contains("L-125").contains("7,5 kg")
                .contains("href=\"/ajustes-insumo/nuevo?idLoteInsumo=10\"").contains("aria-label=\"Registrar ajuste del lote L-125\"")
                .contains("href=\"/stock-insumos/lotes/10\"").contains("aria-label=\"Abrir lote L-125\"")
                .contains("Lúpulo Cascade").contains("href=\"/stock-insumos/4\"").contains("aria-label=\"Abrir Lúpulo Cascade\"");
        assertThat(html).doesNotContain("No hay órdenes de compra pendientes").doesNotContain("Situación");
    }

    @Test
    @DisplayName("CP-HCT-21: un panel del encargado de depósito en cero muestra los contadores con 0 y los tres textos de estado vacío, sin tablas")
    void home_debeMostrarLosEstadosVaciosDelPanelDelEncargado() throws Exception {
        // Arrange
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito()).thenReturn(panelEncargadoVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        for (String contador : List.of("ordenes-pendientes", "ordenes-entrega-vencida", "lotes-vencidos", "insumos-sin-stock")) {
            assertThat(totalDelContador(html, contador)).as(contador).isEqualTo("0");
        }
        assertThat(html).contains(">No hay órdenes de compra pendientes<").contains(">No hay insumos sin stock disponible<").contains(">No hay lotes de insumo vencidos con cantidad<");
        assertThat(html).doesNotContain("<table").doesNotContain("data-boton-abrir").doesNotContain("data-boton-registrar");
    }

    @Test
    @DisplayName("CP-HCT-22: un rechazo o un fallo inesperado del service del encargado de depósito no redirige: la pantalla se muestra sin el panel y con la alerta, sin exponer el detalle de un fallo inesperado")
    void home_unFalloDelServiceDelEncargadoMuestraLaPantallaSinElPanel() throws Exception {
        // Arrange
        when(dashboardEncargadoDepositoServicio.obtenerDashboardEncargadoDeposito())
                .thenThrow(new RecursoNoEncontradoException("No se encontró la configuración de producción"))
                .thenThrow(new IllegalStateException("detalle interno"));

        // Act
        String rechazo = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardEncargadoDeposito"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control: No se encontró la configuración de producción"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inesperado = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardEncargadoDeposito"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(rechazo).contains("No se pudo cargar el panel de control: No se encontró la configuración de producción").doesNotContain("data-dashboard-encargado-deposito").doesNotContain("Estado del sistema");
        assertThat(inesperado).contains("No se pudo cargar el panel de control").doesNotContain("detalle interno").doesNotContain("data-dashboard-encargado-deposito");
    }

    @Test
    @DisplayName("CP-HCT-23: el ADMINISTRADOR recibe su panel en el modelo, que se carga una sola vez, sin los demás, y lo ve en lugar de las tarjetas de resumen")
    void home_debeCargarElPanelParaElAdministrador() throws Exception {
        // Arrange
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador()).thenReturn(panelAdministradorConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardAdministrador"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andExpect(model().attributeDoesNotExist("dashboardGerente"))
                .andExpect(model().attributeDoesNotExist("dashboardEncargadoDeposito"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(dashboardAdministradorServicio).obtenerDashboardAdministrador();
        verifyNoInteractions(dashboardOperarioServicio, dashboardGerenteServicio, dashboardEncargadoDepositoServicio);
        assertThat(html).contains("Panel de Control").contains("data-dashboard-administrador").contains("data-dashboard-contadores")
                .contains("data-bloque=\"usuarios-por-rol\"").contains("data-bloque=\"usuarios-recientes\"").contains("data-bloque=\"actividad-reciente\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("data-dashboard-operario").doesNotContain("data-dashboard-gerente")
                .doesNotContain("data-dashboard-encargado-deposito");
    }

    @Test
    @DisplayName("CP-HCT-24: la ruta raíz también carga el panel del administrador")
    void raiz_debeCargarElPanelParaElAdministrador() throws Exception {
        // Arrange
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador()).thenReturn(panelAdministradorVacio());

        // Act and Assert
        mockMvc.perform(get("/").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardAdministrador"));
        verify(dashboardAdministradorServicio).obtenerDashboardAdministrador();
    }

    @Test
    @DisplayName("CP-HCT-25: el panel del administrador muestra los usuarios activos, los eventos de hoy y los usuarios activos por rol, con sus enlaces")
    void home_debeMostrarLosContadoresDelPanelDelAdministrador() throws Exception {
        // Arrange
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador()).thenReturn(panelAdministradorConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "usuarios-activos")).isEqualTo("8");
        assertThat(totalDelContador(html, "eventos-de-hoy")).isEqualTo("15");
        assertThat(html).contains("data-conteo=\"ADMINISTRADOR\"").contains("Administrador").contains("href=\"/usuarios?rol=OPERARIO_DE_PRODUCCION\"")
                .contains("href=\"/usuarios\"").contains("href=\"/auditoria?fechaDesde=" + LocalDate.now() + "&amp;fechaHasta=" + LocalDate.now() + "\"");
    }

    @Test
    @DisplayName("CP-HCT-26: el panel del administrador muestra los usuarios recientes con su rol y Abrir, y los últimos eventos con el ojo Ver solo si tienen detalle")
    void home_debeMostrarLasFilasDelPanelDelAdministrador() throws Exception {
        // Arrange
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador()).thenReturn(panelAdministradorConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("jperez").contains("Juan Pérez").contains("Operario de Producción")
                .contains("href=\"/usuarios?username=jperez\"").contains("aria-label=\"Abrir jperez\"")
                .contains("admin").contains("Alta").contains("Malta")
                .contains("href=\"/auditoria/41\"").contains("aria-label=\"Ver detalle de la entrada 41\"").doesNotContain("/auditoria/42");
        assertThat(html).doesNotContain("No hay usuarios activos").doesNotContain("No hay eventos registrados");
    }

    @Test
    @DisplayName("CP-HCT-27: un panel del administrador sin eventos muestra los contadores con 0 y el estado vacío de la actividad, sin tabla de eventos")
    void home_debeMostrarLosEstadosVaciosDelPanelDelAdministrador() throws Exception {
        // Arrange
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador()).thenReturn(panelAdministradorVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "usuarios-activos")).isEqualTo("0");
        assertThat(totalDelContador(html, "eventos-de-hoy")).isEqualTo("0");
        assertThat(html).contains(">No hay usuarios activos<").contains(">No hay eventos registrados en la bitácora<");
        assertThat(html).doesNotContain("<table").doesNotContain("data-boton-abrir");
    }

    @Test
    @DisplayName("CP-HCT-28: un rechazo o un fallo inesperado del service del administrador no redirige: la pantalla se muestra sin el panel y con la alerta, sin exponer el detalle de un fallo inesperado")
    void home_unFalloDelServiceDelAdministradorMuestraLaPantallaSinElPanel() throws Exception {
        // Arrange
        when(dashboardAdministradorServicio.obtenerDashboardAdministrador())
                .thenThrow(new RecursoNoEncontradoException("No se encontró el recurso"))
                .thenThrow(new IllegalStateException("detalle interno"));

        // Act
        String rechazo = mockMvc.perform(get("/home").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardAdministrador"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control: No se encontró el recurso"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inesperado = mockMvc.perform(get("/home").with(como(Rol.ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardAdministrador"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(rechazo).contains("No se pudo cargar el panel de control: No se encontró el recurso").doesNotContain("data-dashboard-administrador").doesNotContain("Estado del sistema");
        assertThat(inesperado).contains("No se pudo cargar el panel de control").doesNotContain("detalle interno").doesNotContain("data-dashboard-administrador");
    }

    @Test
    @DisplayName("CP-HCT-29: el GERENTE_DE_COMPRAS recibe su panel en el modelo, que se carga una sola vez, sin los demás, y lo ve en lugar de las tarjetas de resumen")
    void home_debeCargarElPanelParaElGerenteDeCompras() throws Exception {
        // Arrange
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras()).thenReturn(panelComprasConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardGerenteCompras"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andExpect(model().attributeDoesNotExist("dashboardGerente"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(dashboardGerenteComprasServicio).obtenerDashboardGerenteCompras();
        verifyNoInteractions(dashboardOperarioServicio, dashboardGerenteServicio, dashboardEncargadoDepositoServicio, dashboardAdministradorServicio);
        assertThat(html).contains("Panel de Control").contains("data-dashboard-gerente-compras").contains("data-dashboard-contadores")
                .contains("data-bloque=\"planificaciones-sin-orden\"").contains("data-bloque=\"ordenes-pendientes\"").contains("data-bloque=\"ordenes-finalizadas\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("data-dashboard-operario").doesNotContain("data-dashboard-administrador");
    }

    @Test
    @DisplayName("CP-HCT-30: la ruta raíz también carga el panel del gerente de compras")
    void raiz_debeCargarElPanelParaElGerenteDeCompras() throws Exception {
        // Arrange
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras()).thenReturn(panelComprasVacio());

        // Act and Assert
        mockMvc.perform(get("/").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardGerenteCompras"));
        verify(dashboardGerenteComprasServicio).obtenerDashboardGerenteCompras();
    }

    @Test
    @DisplayName("CP-HCT-31: el panel del gerente de compras muestra los cuatro contadores con su número, la ventana de días de las finalizadas y sus enlaces")
    void home_debeMostrarLosContadoresDelPanelDelGerenteDeCompras() throws Exception {
        // Arrange
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras()).thenReturn(panelComprasConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "planificaciones-sin-orden")).isEqualTo("6");
        assertThat(totalDelContador(html, "ordenes-pendientes")).isEqualTo("9");
        assertThat(totalDelContador(html, "ordenes-entrega-vencida")).isEqualTo("3");
        assertThat(totalDelContador(html, "ordenes-finalizadas")).isEqualTo("14");
        assertThat(html).contains("Finalizadas, últimos 30 días").contains("href=\"/planificaciones-produccion?estado=PENDIENTE\"")
                .contains("href=\"/ordenes-compra?estado=PENDIENTE\"").contains("href=\"/ordenes-compra?estado=FINALIZADA\"");
    }

    @Test
    @DisplayName("CP-HCT-32: el panel del gerente de compras muestra las filas con sus atajos: Registrar orden con la planificación elegida y Ver de cada planificación, la recepción de cada orden pendiente y el cierre de cada orden finalizada")
    void home_debeMostrarLasFilasYSusAtajosDelGerenteDeCompras() throws Exception {
        // Arrange
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras()).thenReturn(panelComprasConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Plan #7 - Pale Ale").contains("href=\"/ordenes-compra/nuevo?idPlanificacion=7\"").contains("aria-label=\"Registrar orden de compra de Plan #7 - Pale Ale\"")
                .contains("href=\"/planificaciones-produccion/7\"").contains("aria-label=\"Ver Plan #7 - Pale Ale\"")
                .contains("Orden de compra #7").contains("Malterías del Sur S.A.").contains("40 %").contains("href=\"/ordenes-compra/7\"").contains("aria-label=\"Ver orden de compra #7\"")
                .contains("Orden de compra #5").contains("Automático").contains("Manual").contains("href=\"/ordenes-compra/5\"");
        assertThat(html).doesNotContain("No hay órdenes de compra pendientes").doesNotContain("Situación").doesNotContain("data-boton-abrir");
    }

    @Test
    @DisplayName("CP-HCT-33: un panel del gerente de compras en cero muestra los contadores con 0 y los tres textos de estado vacío, sin tablas")
    void home_debeMostrarLosEstadosVaciosDelPanelDelGerenteDeCompras() throws Exception {
        // Arrange
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras()).thenReturn(panelComprasVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        for (String contador : List.of("planificaciones-sin-orden", "ordenes-pendientes", "ordenes-entrega-vencida", "ordenes-finalizadas")) {
            assertThat(totalDelContador(html, contador)).as(contador).isEqualTo("0");
        }
        assertThat(html).contains(">No hay planificaciones pendientes sin orden de compra<").contains(">No hay órdenes de compra pendientes<")
                .contains(">No hay órdenes de compra finalizadas en el período<");
        assertThat(html).doesNotContain("<table").doesNotContain("data-boton-registrar");
    }

    @Test
    @DisplayName("CP-HCT-34: un rechazo o un fallo inesperado del service del gerente de compras no redirige: la pantalla se muestra sin el panel y con la alerta, sin exponer el detalle de un fallo inesperado")
    void home_unFalloDelServiceDelGerenteDeComprasMuestraLaPantallaSinElPanel() throws Exception {
        // Arrange
        when(dashboardGerenteComprasServicio.obtenerDashboardGerenteCompras())
                .thenThrow(new RecursoNoEncontradoException("No se encontró el recurso"))
                .thenThrow(new IllegalStateException("detalle interno"));

        // Act
        String rechazo = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardGerenteCompras"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control: No se encontró el recurso"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inesperado = mockMvc.perform(get("/home").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardGerenteCompras"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(rechazo).contains("No se pudo cargar el panel de control: No se encontró el recurso").doesNotContain("data-dashboard-gerente-compras").doesNotContain("Estado del sistema");
        assertThat(inesperado).contains("No se pudo cargar el panel de control").doesNotContain("detalle interno").doesNotContain("data-dashboard-gerente-compras");
    }

    @Test
    @DisplayName("CP-HCT-35: el ENCARGADO_DE_DISTRIBUCION recibe su panel en el modelo, que se carga una sola vez, sin los demás, y lo ve en lugar de cualquier tarjeta de resumen")
    void home_debeCargarElPanelParaElEncargadoDeDistribucion() throws Exception {
        // Arrange
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion()).thenReturn(panelDistribucionConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardEncargadoDistribucion"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andExpect(model().attributeDoesNotExist("dashboardGerenteCompras"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(dashboardEncargadoDistribucionServicio).obtenerDashboardEncargadoDistribucion();
        verifyNoInteractions(dashboardOperarioServicio, dashboardGerenteServicio, dashboardEncargadoDepositoServicio, dashboardAdministradorServicio, dashboardGerenteComprasServicio);
        assertThat(html).contains("Panel de Control").contains("data-dashboard-encargado-distribucion").contains("data-dashboard-contadores")
                .contains("data-bloque=\"devoluciones-pendientes\"").contains("data-bloque=\"clientes-con-barriles\"").contains("data-bloque=\"barriles-listos\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("data-dashboard-gerente-compras");
    }

    @Test
    @DisplayName("CP-HCT-36: la ruta raíz también carga el panel del encargado de distribución")
    void raiz_debeCargarElPanelParaElEncargadoDeDistribucion() throws Exception {
        // Arrange
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion()).thenReturn(panelDistribucionVacio());

        // Act and Assert
        mockMvc.perform(get("/").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("dashboardEncargadoDistribucion"));
        verify(dashboardEncargadoDistribucionServicio).obtenerDashboardEncargadoDistribucion();
    }

    @Test
    @DisplayName("CP-HCT-37: el panel del encargado de distribución muestra los cuatro contadores con su número y sus enlaces a los barriles Despachados y Con cerveza")
    void home_debeMostrarLosContadoresDelPanelDelEncargadoDeDistribucion() throws Exception {
        // Arrange
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion()).thenReturn(panelDistribucionConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(totalDelContador(html, "en-poder-de-clientes")).isEqualTo("8");
        assertThat(totalDelContador(html, "devoluciones-vencidas")).isEqualTo("3");
        assertThat(totalDelContador(html, "devoluciones-de-hoy")).isEqualTo("1");
        assertThat(totalDelContador(html, "listos-para-despachar")).isEqualTo("5");
        assertThat(html).contains("href=\"/control-barriles?estadoOperativo=DESPACHADO\"").contains("href=\"/control-barriles?estadoOperativo=CON_CERVEZA\"");
    }

    @Test
    @DisplayName("CP-HCT-38: el panel del encargado de distribución muestra las devoluciones con sus días de atraso, los clientes con su cantidad de barriles y los barriles listos, cada uno con sus atajos")
    void home_debeMostrarLasFilasYSusAtajosDelEncargadoDeDistribucion() throws Exception {
        // Arrange
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion()).thenReturn(panelDistribucionConDatos());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("Bar Don Jorge").contains("BRL-01").contains("3 días")
                .contains("href=\"/control-barriles/21/devoluciones/nueva\"").contains("aria-label=\"Registrar devolución de BRL-01\"")
                .contains("href=\"/control-barriles/21\"").contains("aria-label=\"Abrir BRL-01\"")
                .contains("data-cliente-barriles").contains("href=\"/clientes?nombre=Bar%20Don%20Jorge\"").contains("aria-label=\"Abrir Bar Don Jorge\"")
                .contains("Pale Ale").contains("PALE_ALE-1").contains("50 L")
                .contains("href=\"/control-barriles/31/despachos/nuevo\"").contains("aria-label=\"Registrar despacho de BRL-02\"").contains("aria-label=\"Abrir BRL-02\"");
        assertThat(html).doesNotContain("No hay devoluciones vencidas ni que venzan hoy").doesNotContain("Situación");
    }

    @Test
    @DisplayName("CP-HCT-39: un panel del encargado de distribución en cero muestra los contadores con 0 y los tres textos de estado vacío, sin tablas")
    void home_debeMostrarLosEstadosVaciosDelPanelDelEncargadoDeDistribucion() throws Exception {
        // Arrange
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion()).thenReturn(panelDistribucionVacio());

        // Act
        String html = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        for (String contador : List.of("en-poder-de-clientes", "devoluciones-vencidas", "devoluciones-de-hoy", "listos-para-despachar")) {
            assertThat(totalDelContador(html, contador)).as(contador).isEqualTo("0");
        }
        assertThat(html).contains(">No hay devoluciones vencidas ni que venzan hoy<").contains(">No hay clientes con barriles en su poder<")
                .contains(">No hay barriles con cerveza listos para despachar<");
        assertThat(html).doesNotContain("<table").doesNotContain("data-boton-registrar").doesNotContain("data-boton-abrir");
    }

    @Test
    @DisplayName("CP-HCT-40: un rechazo o un fallo inesperado del service del encargado de distribución no redirige: la pantalla se muestra sin el panel y con la alerta, sin exponer el detalle de un fallo inesperado")
    void home_unFalloDelServiceDelEncargadoDeDistribucionMuestraLaPantallaSinElPanel() throws Exception {
        // Arrange
        when(dashboardEncargadoDistribucionServicio.obtenerDashboardEncargadoDistribucion())
                .thenThrow(new RecursoNoEncontradoException("No se encontró el recurso"))
                .thenThrow(new IllegalStateException("detalle interno"));

        // Act
        String rechazo = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardEncargadoDistribucion"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control: No se encontró el recurso"))
                .andExpect(model().attribute("tipo", "danger"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inesperado = mockMvc.perform(get("/home").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("dashboardEncargadoDistribucion"))
                .andExpect(model().attribute("mensaje", "No se pudo cargar el panel de control"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(rechazo).contains("No se pudo cargar el panel de control: No se encontró el recurso").doesNotContain("data-dashboard-encargado-distribucion");
        assertThat(inesperado).contains("No se pudo cargar el panel de control").doesNotContain("detalle interno").doesNotContain("data-dashboard-encargado-distribucion");
    }

    @Test
    @DisplayName("CP-HCT-08: sin sesión iniciada la pantalla de inicio redirige al login y no consulta el panel")
    void home_sinSesionRedirigeAlLogin() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/home")).andExpect(status().is3xxRedirection());
        verify(dashboardOperarioServicio, never()).obtenerDashboardOperario();
        verify(dashboardGerenteServicio, never()).obtenerDashboardGerente();
        verify(dashboardEncargadoDepositoServicio, never()).obtenerDashboardEncargadoDeposito();
        verify(dashboardAdministradorServicio, never()).obtenerDashboardAdministrador();
        verify(dashboardGerenteComprasServicio, never()).obtenerDashboardGerenteCompras();
        verify(dashboardEncargadoDistribucionServicio, never()).obtenerDashboardEncargadoDistribucion();
    }

    // ---------- Usuarios ----------

    /** Un usuario con las authorities que le da el login: ROLE_ del rol y cada uno de sus permisos. */
    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }

    // ---------- Datos de prueba ----------

    /** El número del contador con ese identificador (data-contador): el primer data-contador-total que le sigue. */
    private static String totalDelContador(String html, String contador) {
        int inicio = html.indexOf("data-contador=\"" + contador + "\"");
        assertThat(inicio).as("contador %s", contador).isNotNegative();
        int total = html.indexOf("data-contador-total", inicio);
        int desde = html.indexOf('>', total) + 1;
        return html.substring(desde, html.indexOf('<', desde)).strip();
    }

    private static DashboardOperarioResponseDTO panelConDatos() {
        LoteDelDiaResponseDTO enEjecucion = LoteDelDiaResponseDTO.builder().id(12L).identificadorInterno("LOT-R7-03").nombreReceta("Pale Ale")
                .etapaActual(TipoEtapa.FERMENTACION).idFermentador(1L).identificadorInternoFermentador("FER-01")
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 7, 5)).build();
        LoteDelDiaResponseDTO aIniciar = LoteDelDiaResponseDTO.builder().id(13L).identificadorInterno("LOT-R7-04").nombreReceta("Stout")
                .idFermentador(2L).identificadorInternoFermentador("FER-02").fechaInicioEstimada(LocalDate.of(2027, 7, 8)).build();
        EquipoPendienteResponseDTO equipoEnLimpieza = EquipoPendienteResponseDTO.builder().id(21L).identificadorInterno("FER-02").tipo(TipoEquipamiento.FERMENTADOR).build();
        BarrilPendienteResponseDTO barrilEnLimpieza = BarrilPendienteResponseDTO.builder().id(31L).identificadorInterno("BRL-01").fabricante("Barrilitos SA").build();
        EquipoPendienteResponseDTO equipoEnMantenimiento = EquipoPendienteResponseDTO.builder().id(22L).identificadorInterno("MAC-01").tipo(TipoEquipamiento.MACERADOR)
                .causaMantenimiento(CausaMantenimiento.FALLA).build();
        BarrilPendienteResponseDTO barrilEnMantenimiento = BarrilPendienteResponseDTO.builder().id(32L).identificadorInterno("BRL-02").fabricante("Barrilitos SA")
                .causaMantenimiento(CausaMantenimiento.PREVENTIVO).build();
        return DashboardOperarioResponseDTO.builder()
                .lotesEnEjecucion(bloque(7, enEjecucion)).lotesAIniciar(bloque(4, aIniciar))
                .equiposEnLimpieza(bloque(3, equipoEnLimpieza)).barrilesEnLimpieza(bloque(2, barrilEnLimpieza))
                .equiposEnMantenimiento(bloque(6, equipoEnMantenimiento)).barrilesEnMantenimiento(bloque(3, barrilEnMantenimiento))
                .build();
    }

    private static DashboardOperarioResponseDTO panelVacio() {
        return DashboardOperarioResponseDTO.builder()
                .lotesEnEjecucion(bloque(0)).lotesAIniciar(bloque(0))
                .equiposEnLimpieza(bloque(0)).barrilesEnLimpieza(bloque(0))
                .equiposEnMantenimiento(bloque(0)).barrilesEnMantenimiento(bloque(0))
                .build();
    }

    private static DashboardGerenteResponseDTO panelGerenteConDatos() {
        LoteEnProduccionResponseDTO enEjecucion = LoteEnProduccionResponseDTO.builder().id(12L).identificadorInterno("LOT-R7-03").nombreReceta("Pale Ale")
                .etapaActual(TipoEtapa.FERMENTACION).idFermentador(1L).identificadorInternoFermentador("FER-01")
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 7, 5)).diasDeAtraso(3).prioridad(PrioridadPlanificacion.ALTA).build();
        LoteEnProduccionResponseDTO proximo = LoteEnProduccionResponseDTO.builder().id(14L).identificadorInterno("LOT-R7-04").nombreReceta("Stout")
                .idFermentador(1L).identificadorInternoFermentador("FER-01").fechaInicioEstimada(LocalDate.of(2027, 7, 8)).prioridad(PrioridadPlanificacion.MEDIA).build();
        ColaFermentadorResponseDTO cola = ColaFermentadorResponseDTO.builder().id(1L).identificadorInterno("FER-01").estadoOperativo(EstadoOperativo.EN_USO)
                .loteEnEjecucion(enEjecucion).proximoLote(proximo).lotesPendientes(2).build();
        PlanificacionPendienteResponseDTO planificacion = PlanificacionPendienteResponseDTO.builder().id(7L).etiqueta("Plan #7 - Pale Ale")
                .prioridad(PrioridadPlanificacion.ALTA).fechaInicioEstimada(LocalDate.of(2027, 8, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 8, 20)).build();
        return DashboardGerenteResponseDTO.builder()
                .lotesEnEjecucion(7).lotesAtrasados(2).fermentadoresOcupados(3).fermentadoresTotal(5).fueraDeServicio(9)
                .produccionEnCurso(List.of(EtapaEnCursoResponseDTO.builder().etapa(TipoEtapa.FERMENTACION).lotes(bloque(4, enEjecucion)).build()))
                .colaDeFermentadores(bloque(5, cola))
                .equiposPorEstado(List.of(conteo("DISPONIBLE", "Disponible", 4), conteo("EN_USO", "En uso", 3), conteo("EN_LIMPIEZA", "En limpieza", 2), conteo("EN_MANTENIMIENTO", "En mantenimiento", 1)))
                .barrilesPorEstado(List.of(conteo("DISPONIBLE", "Disponible", 4), conteo("EN_USO", "En uso", 1), conteo("CON_CERVEZA", "Con cerveza", 6),
                        conteo("DESPACHADO", "Despachado", 2), conteo("EN_LIMPIEZA", "En limpieza", 3), conteo("EN_MANTENIMIENTO", "En mantenimiento", 3)))
                .planificacionesPendientes(bloque(3, planificacion))
                .build();
    }

    private static DashboardGerenteResponseDTO panelGerenteVacio() {
        return DashboardGerenteResponseDTO.builder()
                .produccionEnCurso(List.of()).colaDeFermentadores(bloque(0))
                .equiposPorEstado(List.of()).barrilesPorEstado(List.of())
                .planificacionesPendientes(bloque(0))
                .build();
    }

    private static ConteoDashboardResponseDTO conteo(String codigo, String etiqueta, long total) {
        return ConteoDashboardResponseDTO.builder().codigo(codigo).etiqueta(etiqueta).total(total).build();
    }

    private static DashboardEncargadoDepositoResponseDTO panelEncargadoConDatos() {
        OrdenCompraPendienteResponseDTO orden = OrdenCompraPendienteResponseDTO.builder().id(7L).proveedor("Malterías del Sur S.A.").fechaEntregaEstimada(LocalDate.of(2027, 8, 1)).build();
        LoteInsumoVencidoResponseDTO lote = LoteInsumoVencidoResponseDTO.builder().id(10L).nombreInsumo("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .identificacionLoteProveedor("L-125").fechaVencimiento(LocalDate.of(2026, 10, 1)).cantidadActual(7.5).build();
        InsumoSinStockResponseDTO insumo = InsumoSinStockResponseDTO.builder().id(4L).nombre("Lúpulo Cascade").tipo(TipoInsumo.LUPULO).build();
        return DashboardEncargadoDepositoResponseDTO.builder()
                .ordenesPendientes(bloque(9, orden)).ordenesConEntregaVencida(4)
                .lotesVencidos(bloque(12, lote)).insumosSinStock(bloque(6, insumo))
                .build();
    }

    private static DashboardEncargadoDepositoResponseDTO panelEncargadoVacio() {
        return DashboardEncargadoDepositoResponseDTO.builder()
                .ordenesPendientes(bloque(0)).lotesVencidos(bloque(0)).insumosSinStock(bloque(0))
                .build();
    }

    private static DashboardAdministradorResponseDTO panelAdministradorConDatos() {
        UsuarioRecienteResponseDTO usuario = UsuarioRecienteResponseDTO.builder().id(3L).username("jperez").nombreCompleto("Juan Pérez").rol(Rol.OPERARIO_DE_PRODUCCION).build();
        EventoAuditoriaResponseDTO conDetalle = EventoAuditoriaResponseDTO.builder().id(41L).fechaHora(java.time.LocalDateTime.of(2027, 7, 1, 8, 30))
                .username("admin").accion(AccionAuditoria.CREAR).concepto(ConceptoAuditoria.MALTA).tieneDetalle(true).build();
        EventoAuditoriaResponseDTO sinDetalle = EventoAuditoriaResponseDTO.builder().id(42L).fechaHora(java.time.LocalDateTime.of(2027, 7, 1, 8, 0))
                .username("admin").accion(AccionAuditoria.LOGIN_EXITOSO).concepto(ConceptoAuditoria.MALTA).tieneDetalle(false).build();
        List<ConteoDashboardResponseDTO> porRol = new ArrayList<>();
        for (Rol rol : Rol.values()) {
            porRol.add(conteo(rol.name(), rol.getEtiqueta(), rol == Rol.ADMINISTRADOR ? 1 : 0));
        }
        return DashboardAdministradorResponseDTO.builder()
                .usuariosActivos(8).usuariosPorRol(porRol).eventosDeHoy(15)
                .usuariosRecientes(bloque(8, usuario)).actividadReciente(bloque(120, conDetalle, sinDetalle))
                .build();
    }

    private static DashboardAdministradorResponseDTO panelAdministradorVacio() {
        return DashboardAdministradorResponseDTO.builder()
                .usuariosPorRol(List.of()).usuariosRecientes(bloque(0)).actividadReciente(bloque(0))
                .build();
    }

    private static DashboardGerenteComprasResponseDTO panelComprasConDatos() {
        PlanificacionSinOrdenResponseDTO planificacion = PlanificacionSinOrdenResponseDTO.builder().id(7L).etiqueta("Plan #7 - Pale Ale")
                .fechaInicioEstimada(LocalDate.of(2027, 8, 1)).prioridad(PrioridadPlanificacion.ALTA).build();
        OrdenCompraEnCursoResponseDTO pendiente = OrdenCompraEnCursoResponseDTO.builder().id(7L).proveedor("Malterías del Sur S.A.")
                .fechaEntregaEstimada(LocalDate.of(2027, 8, 10)).avanceRecepcion(40).build();
        OrdenCompraFinalizadaResponseDTO automatica = OrdenCompraFinalizadaResponseDTO.builder().id(5L).proveedor("Lúpulos SA")
                .fechaFinalizacion(java.time.LocalDateTime.of(2027, 7, 1, 9, 0)).cierreManual(false).build();
        OrdenCompraFinalizadaResponseDTO manual = OrdenCompraFinalizadaResponseDTO.builder().id(4L).proveedor("Lúpulos SA")
                .fechaFinalizacion(java.time.LocalDateTime.of(2027, 6, 28, 15, 30)).cierreManual(true).build();
        return DashboardGerenteComprasResponseDTO.builder()
                .planificacionesSinOrden(bloque(6, planificacion)).ordenesPendientes(bloque(9, pendiente)).ordenesConEntregaVencida(3)
                .ordenesFinalizadas(bloque(14, automatica, manual)).diasDeOrdenesFinalizadas(30)
                .build();
    }

    private static DashboardGerenteComprasResponseDTO panelComprasVacio() {
        return DashboardGerenteComprasResponseDTO.builder()
                .planificacionesSinOrden(bloque(0)).ordenesPendientes(bloque(0)).ordenesFinalizadas(bloque(0)).diasDeOrdenesFinalizadas(30)
                .build();
    }

    private static DashboardEncargadoDistribucionResponseDTO panelDistribucionConDatos() {
        DevolucionPendienteResponseDTO devolucion = DevolucionPendienteResponseDTO.builder().idBarril(21L).identificadorInterno("BRL-01").nombreCliente("Bar Don Jorge")
                .fechaDevolucionEstimada(LocalDate.of(2027, 7, 1)).diasDeAtraso(3).build();
        ClienteConBarrilesResponseDTO cliente = ClienteConBarrilesResponseDTO.builder().idCliente(5L).nombre("Bar Don Jorge").cantidadBarriles(2)
                .proximaFechaDevolucion(LocalDate.of(2027, 7, 1)).build();
        BarrilListoResponseDTO listo = BarrilListoResponseDTO.builder().id(31L).identificadorInterno("BRL-02").nombreReceta("Pale Ale").codigoLote("PALE_ALE-1").contenidoActual(50.0).build();
        return DashboardEncargadoDistribucionResponseDTO.builder()
                .barrilesEnPoderDeClientes(8).devolucionesVencidas(3).devolucionesQueVencenHoy(1)
                .devolucionesPendientes(bloque(4, devolucion)).clientesConBarriles(bloque(3, cliente)).barrilesListos(bloque(5, listo))
                .build();
    }

    private static DashboardEncargadoDistribucionResponseDTO panelDistribucionVacio() {
        return DashboardEncargadoDistribucionResponseDTO.builder()
                .devolucionesPendientes(bloque(0)).clientesConBarriles(bloque(0)).barrilesListos(bloque(0))
                .build();
    }

    @SafeVarargs
    private static <T> BloqueDashboardResponseDTO<T> bloque(long total, T... filas) {
        return BloqueDashboardResponseDTO.<T>builder().total(total).filas(List.of(filas)).build();
    }
}
