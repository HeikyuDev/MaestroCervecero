package com.github.heikyudev.maestrocervecero.presentation.controller.login;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardOperarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
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
        assertThat(html).contains("Panel de Control").contains("data-dashboard-operario").contains("data-dashboard-contadores")
                .contains("data-bloque=\"lotes-de-hoy\"").contains("data-bloque=\"limpiezas-pendientes\"").contains("data-bloque=\"pendientes-de-mantenimiento\"");
        assertThat(html).doesNotContain("Estado del sistema").doesNotContain("Todos los servicios activos");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = "OPERARIO_DE_PRODUCCION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("CP-HCT-02: los demás roles no reciben el panel en el modelo, no se consulta nada de planta y ven las tarjetas de resumen")
    void home_noDebeCargarElPanelParaLosDemasRoles(Rol rol) throws Exception {
        // Act
        String html = mockMvc.perform(get("/home").with(como(rol)))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeDoesNotExist("dashboardOperario"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verifyNoInteractions(dashboardOperarioServicio);
        assertThat(html).contains("Panel de Control").contains("Estado del sistema").doesNotContain("data-dashboard-operario");
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
                .contains("Atrasado 3 días").contains("Falta consumo").contains("bi-exclamation-circle")
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
    @DisplayName("CP-HCT-08: sin sesión iniciada la pantalla de inicio redirige al login y no consulta el panel")
    void home_sinSesionRedirigeAlLogin() throws Exception {
        // Act and Assert
        mockMvc.perform(get("/home")).andExpect(status().is3xxRedirection());
        verify(dashboardOperarioServicio, never()).obtenerDashboardOperario();
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
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1)).fechaFinalizacionEstimada(LocalDate.of(2027, 7, 5)).diasDeAtraso(3).faltaConsumo(true).build();
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

    @SafeVarargs
    private static <T> BloqueDashboardResponseDTO<T> bloque(long total, T... filas) {
        return BloqueDashboardResponseDTO.<T>builder().total(total).filas(List.of(filas)).build();
    }
}
