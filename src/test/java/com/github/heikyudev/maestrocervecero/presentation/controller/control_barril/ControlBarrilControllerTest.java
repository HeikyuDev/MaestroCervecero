package com.github.heikyudev.maestrocervecero.presentation.controller.control_barril;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ActividadBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.TipoActividadBarril;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ControlBarrilController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
public class ControlBarrilControllerTest {

    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IControlBarrilServicio servicio;

    @MockitoBean
    private IFabricanteBarrilServicio fabricanteServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /** Escenario base: barril 5 "BRL-01" EN_LIMPIEZA con una limpieza registrada anulable (7) y una anulada (6). */
    @BeforeEach
    void escenarioBase() {
        when(servicio.filtrarBarrilesParaControl(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                BarrilControlResponseDTO.builder().id(5L).identificadorInterno("BRL-01").fabricante("Barrilitos SA").capacidad(50.0).contenidoActual(0.0)
                        .estadoOperativo(EstadoOperativoBarril.EN_LIMPIEZA).usosDesdeUltimoMantenimiento(3L).usosMaximosAntesMantenimiento(10).build())));
        when(fabricanteServicio.buscarPorId(2L)).thenReturn(FabricanteBarrilResponseDTO.builder().id(2L).nombre("Barrilitos SA").build());
        configurarBarril(EstadoOperativoBarril.EN_LIMPIEZA);
        when(servicio.buscarResumen(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el barril con ID: 99"));
        when(servicio.listarHistorial(eq(5L), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                ActividadBarrilResponseDTO.builder().tipo(TipoActividadBarril.LIMPIEZA).idReferencia(7L).observaciones("Limpieza con soda")
                        .fecha(LocalDateTime.of(2026, 10, 22, 8, 0)).estadoOperativoResultante(EstadoOperativoBarril.DISPONIBLE).build(),
                ActividadBarrilResponseDTO.builder().tipo(TipoActividadBarril.ENVASADO).idReferencia(1L).idLote(12L).codigoLote("LOT-R1-01")
                        .fecha(LocalDateTime.of(2026, 10, 20, 8, 0)).estadoOperativoResultante(EstadoOperativoBarril.EN_USO).build())));
    }

    private void configurarBarril(EstadoOperativoBarril estado) {
        when(servicio.buscarResumen(5L)).thenReturn(BarrilResumenResponseDTO.builder().id(5L).identificadorInterno("BRL-01").numeroSerie("SN-001")
                .fabricante("Barrilitos SA").capacidad(50.0).contenidoActual(0.0).estadoOperativo(estado).usosMaximosAntesMantenimiento(10)
                .usosDesdeUltimoMantenimiento(3L).fechaUltimaLimpieza(FIN).build());
        when(servicio.listarOperaciones(eq(5L), any())).thenReturn(List.of());
        when(servicio.listarOperaciones(5L, TipoOperacionBarril.LIMPIEZA)).thenReturn(List.of(
                operacion(7L, TipoOperacionBarril.LIMPIEZA, EstadoTransaccion.REGISTRADO, true),
                operacion(6L, TipoOperacionBarril.LIMPIEZA, EstadoTransaccion.ANULADO, false)));
    }

    static OperacionBarrilResponseDTO operacion(Long id, TipoOperacionBarril tipo, EstadoTransaccion estado, boolean anulable) {
        return OperacionBarrilResponseDTO.builder().id(id).tipo(tipo).estado(estado).fecha(FIN)
                .observaciones("Limpieza con soda cáustica").registradoPor("operario1").fechaRegistro(FIN.plusMinutes(5))
                .motivoAnulacion(estado == EstadoTransaccion.ANULADO ? "Error de carga" : null)
                .fechaAnulacion(estado == EstadoTransaccion.ANULADO ? FIN.plusHours(1) : null).anulable(anulable).build();
    }

    // ==================== Lista ====================

    @Test
    @DisplayName("CP-CBC-01: el operario ve la lista con las columnas del barril y el botón Abrir, sin el ojo del historial")
    void lista_operario() throws Exception {
        String html = html(get("/control-barriles").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-lista");

        assertThat(html).contains("BRL-01").contains("Barrilitos SA").contains("3 de 10").contains("data-boton-abrir").contains("Capacidad (L)")
                .contains("Contenido (L)").contains("En limpieza").contains("Ej: BRL-01").doesNotContain("data-ver-historial");
    }

    @Test
    @DisplayName("CP-CBC-02: el gerente de producción ve la lista con el ojo del historial y el botón Abrir")
    void lista_gerente() throws Exception {
        String html = html(get("/control-barriles").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-lista");

        assertThat(html).contains("data-ver-historial").contains("/control-barriles/5/historial").contains("data-boton-abrir");
    }

    @Test
    @DisplayName("CP-CBC-03: el encargado de distribución ve la lista con el botón Abrir y sin el ojo")
    void lista_encargado() throws Exception {
        String html = html(get("/control-barriles").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)), "control_barril/control-lista");

        assertThat(html).contains("data-boton-abrir").doesNotContain("data-ver-historial");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "ADMINISTRADOR"})
    @DisplayName("CP-CBC-04: los roles sin permisos de barril no abren la lista, el historial, el panel ni el detalle: redirigen con alerta y no se invoca el service")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String url : List.of("/control-barriles", "/control-barriles/5", "/control-barriles/5/historial", "/control-barriles/5/operaciones/limpieza/7")) {
            mockMvc.perform(get(url).with(como(rol))).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("mensaje", SIN_PERMISOS)).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CBC-05: los criterios de la lista se pasan al service (identificador sin espacios, fabricante y estado operativo)")
    void lista_filtros() throws Exception {
        String html = html(get("/control-barriles").param("identificador", " brl ").param("idFabricante", "2")
                .param("estadoOperativo", "EN_LIMPIEZA").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-lista");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarBarrilesParaControl(eq("brl"), eq(2L), eq(EstadoOperativoBarril.EN_LIMPIEZA), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(html).contains("value=\"2\"").contains("value=\"Barrilitos SA\"");
    }

    @Test
    @DisplayName("CP-CBC-06: un estado operativo inválido y un fabricante inexistente no filtran, y el estado inválido se avisa con una alerta warning")
    void lista_filtrosInvalidos() throws Exception {
        when(fabricanteServicio.buscarPorId(77L)).thenThrow(new RecursoNoEncontradoException("No se encontró el fabricante"));

        String html = html(get("/control-barriles").param("estadoOperativo", "YYY").param("idFabricante", "77")
                .with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-lista");

        verify(servicio).filtrarBarrilesParaControl(isNull(), isNull(), isNull(), any());
        assertThat(html).contains("El estado operativo indicado no es válido, se muestran todos");
    }

    // ==================== Historial ====================

    @Test
    @DisplayName("CP-CBC-07: el historial del gerente muestra la información del barril, los filtros y una fila por actividad con su estado resultante y los botones de acciones")
    void historial_gerente() throws Exception {
        String html = html(get("/control-barriles/5/historial").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");

        verify(servicio).listarHistorial(eq(5L), isNull(), isNull(), isNull(), any(Pageable.class));
        assertThat(html).contains("Historial del barril").contains("data-informacion-barril").contains("name=\"tipo\"").contains("name=\"fechaDesde\"")
                .contains("name=\"fechaHasta\"").contains("Envasado").contains("Devolución").contains("Estado resultante");
        String tabla = html.substring(html.indexOf("data-historial"));
        String encabezado = tabla.substring(tabla.indexOf("<thead"), tabla.indexOf("</thead>"));
        assertThat(encabezado.split("<th ", -1)).hasSize(5);
        assertThat(tabla).contains("Lote · LOT-R1-01 · Envasado").contains("href=\"/lotes/12/detalle\"").contains("20/10/2026 08:00")
                .contains("href=\"/control-barriles/5/operaciones/limpieza/7\"").contains("22/10/2026 08:00").contains("En uso").contains("Disponible");
        assertThat(tabla).contains("data-detalle-titulo=\"Observaciones de la limpieza\"").contains("data-detalle-motivo=\"Limpieza con soda\"")
                .doesNotContain(">Limpieza con soda<");
        String cuerpo = tabla.substring(tabla.indexOf("<tbody"), tabla.indexOf("</tbody>"));
        assertThat(cuerpo).doesNotContain("text-amber-700").contains("data-ver-actividad").contains("class=\"col-acciones");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DISTRIBUCION"})
    @DisplayName("CP-CBC-08: el operario y el encargado no abren el historial del barril")
    void historial_rechazado(Rol rol) throws Exception {
        mockMvc.perform(get("/control-barriles/5/historial").with(como(rol)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("mensaje", SIN_PERMISOS));
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CBC-09: sin actividad el historial muestra El barril no tiene operaciones registradas, y con filtros, No se encontraron resultados")
    void historial_vacio() throws Exception {
        when(servicio.listarHistorial(eq(5L), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        String sinFiltros = html(get("/control-barriles/5/historial").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");
        String conFiltros = html(get("/control-barriles/5/historial").param("tipo", "FALLA").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");

        assertThat(sinFiltros).contains("El barril no tiene operaciones registradas");
        assertThat(conFiltros).contains("No se encontraron resultados").doesNotContain("El barril no tiene operaciones registradas");
    }

    @Test
    @DisplayName("CP-CBC-10: los filtros de tipo y de fechas se pasan al service (la fecha hasta incluye todo su día) y se conservan en el formulario")
    void historial_filtros() throws Exception {
        String html = html(get("/control-barriles/5/historial").param("tipo", "despacho").param("fechaDesde", "2026-10-01").param("fechaHasta", "2026-10-31")
                .param("pagina", "1").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).listarHistorial(eq(5L), eq(TipoActividadBarril.DESPACHO), desde.capture(), hasta.capture(), pagina.capture());
        assertThat(desde.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(hasta.getValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(hasta.getValue().getHour()).isEqualTo(23);
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(1);
        assertThat(html).contains("value=\"2026-10-01\"").contains("value=\"2026-10-31\"");
    }

    @Test
    @DisplayName("CP-CBC-11: un tipo inválido se avisa y no filtra, y un rango invertido muestra el aviso del service con la lista vacía")
    void historial_filtrosInvalidos() throws Exception {
        when(servicio.listarHistorial(eq(5L), any(), eq(LocalDateTime.of(2026, 10, 10, 0, 0)), any(), any()))
                .thenThrow(new ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde", "hasta"));

        String tipoInvalido = html(get("/control-barriles/5/historial").param("tipo", "XXX").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");
        String invertido = html(get("/control-barriles/5/historial").param("fechaDesde", "2026-10-10").param("fechaHasta", "2026-10-01")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");

        assertThat(tipoInvalido).contains("El tipo indicado no es válido, se muestran todos");
        assertThat(invertido).contains("La fecha hasta no puede ser anterior a la fecha desde").contains("No se encontraron resultados");
    }

    // ==================== Panel ====================

    @Test
    @DisplayName("CP-CBC-12: el operario ve las pestañas Limpiezas, Fallas, Fraccionamientos y Mantenimientos, y no Despachos ni Devoluciones")
    void panel_pestanasDelOperario() throws Exception {
        String html = html(get("/control-barriles/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");

        assertThat(html).contains("data-pestana-limpiezas").contains("data-pestana-fallas").contains("data-pestana-fraccionamientos").contains("data-pestana-mantenimientos")
                .doesNotContain("data-pestana-despachos").doesNotContain("data-pestana-devoluciones");
    }

    @Test
    @DisplayName("CP-CBC-13: el encargado de distribución ve solo las pestañas Despachos y Devoluciones, con Despachos activa")
    void panel_pestanasDelEncargado() throws Exception {
        configurarBarril(EstadoOperativoBarril.CON_CERVEZA);

        String html = html(get("/control-barriles/5").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)), "control_barril/control-panel");

        assertThat(html).contains("data-pestana-despachos").contains("data-pestana-devoluciones").doesNotContain("data-pestana-limpiezas").doesNotContain("data-pestana-fallas")
                .doesNotContain("data-pestana-fraccionamientos").doesNotContain("data-pestana-mantenimientos").contains("Despachos registrados").contains("Nuevo despacho");
        verify(servicio).listarOperaciones(5L, TipoOperacionBarril.DESPACHO);
    }

    @Test
    @DisplayName("CP-CBC-14: el encargado que pide una pestaña que no puede consultar ve la primera de las suyas")
    void panel_pestanaNoPermitida() throws Exception {
        html(get("/control-barriles/5").param("pestana", "limpiezas").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)), "control_barril/control-panel");

        verify(servicio).listarOperaciones(5L, TipoOperacionBarril.DESPACHO);
    }

    @Test
    @DisplayName("CP-CBC-15: el gerente de producción ve las seis pestañas en modo consulta, sin botón de alta, de anular ni diálogo de motivo")
    void panel_gerenteSoloConsulta() throws Exception {
        String html = html(get("/control-barriles/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-panel");

        assertThat(html).contains("data-pestana-limpiezas").contains("data-pestana-fallas").contains("data-pestana-fraccionamientos").contains("data-pestana-mantenimientos")
                .contains("data-pestana-despachos").contains("data-pestana-devoluciones")
                .contains("data-aviso-solo-lectura").contains("Solo puede consultar las operaciones del barril")
                .doesNotContain("data-nueva-operacion").doesNotContain("data-motivo-url").doesNotContain("dialogo-anular");
    }

    @Test
    @DisplayName("CP-CBC-16: el operario con el barril EN_LIMPIEZA ve el botón + Nueva limpieza hacia su formulario y Anular solo en la operación anulable")
    void panel_operarioConBotonNueva() throws Exception {
        String html = html(get("/control-barriles/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");

        assertThat(html).contains("data-nueva-operacion").contains("Nueva limpieza").contains("href=\"/control-barriles/5/limpiezas/nueva\"").contains("bi-plus-lg")
                .doesNotContain("datetime-local");
        assertThat(count(html, "data-motivo-url")).isEqualTo(1);
        assertThat(html).contains("/control-barriles/5/limpiezas/7/anular").doesNotContain("/limpiezas/6/anular");
    }

    @Test
    @DisplayName("CP-CBC-17: con un estado que no lo permite el panel explica el motivo con el texto del service y, con el barril EN_USO, avisa que lo ocupa un lote")
    void panel_estadoNoPermite() throws Exception {
        String disponible = html(get("/control-barriles/5").param("pestana", "mantenimientos").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");
        configurarBarril(EstadoOperativoBarril.EN_USO);
        String enUso = html(get("/control-barriles/5").param("pestana", "fallas").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");

        assertThat(disponible).contains("data-aviso-registro").contains("Solo se puede registrar un mantenimiento sobre un barril en estado operativo En mantenimiento")
                .doesNotContain("data-nueva-operacion");
        assertThat(enUso).contains("No se puede registrar una falla porque el barril está en uso por un lote").doesNotContain("data-nueva-operacion");
    }

    @Test
    @DisplayName("CP-CBC-18: la tabla del panel pone las observaciones en el diálogo del botón i y todos los botones de fila en la columna Acciones")
    void panel_columnaAcciones() throws Exception {
        String html = html(get("/control-barriles/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");

        String tabla = html.substring(html.indexOf("data-operaciones-registradas"));
        assertThat(tabla).contains("data-detalle-titulo=\"Observaciones de la limpieza\"").contains("data-detalle-sin-usuario").doesNotContain(">Limpieza con soda cáustica<");
        String cuerpo = tabla.substring(tabla.indexOf("<tbody"), tabla.indexOf("</tbody>"));
        String celda = cuerpo.substring(cuerpo.indexOf("class=\"col-acciones"), cuerpo.indexOf("</td>", cuerpo.indexOf("class=\"col-acciones")));
        assertThat(celda).contains("Ver observaciones").contains("data-ver-operacion").contains("data-motivo-dialogo");
        assertThat(cuerpo).doesNotContain("text-amber-700");
    }

    @Test
    @DisplayName("CP-CBC-19: la tabla del panel se pagina de a 20 con el pie del sistema, que conserva la pestaña")
    void panel_paginacion() throws Exception {
        List<OperacionBarrilResponseDTO> muchas = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            muchas.add(operacion(i, TipoOperacionBarril.LIMPIEZA, EstadoTransaccion.REGISTRADO, false));
        }
        when(servicio.listarOperaciones(5L, TipoOperacionBarril.LIMPIEZA)).thenReturn(muchas);

        String primera = html(get("/control-barriles/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");
        String segunda = html(get("/control-barriles/5").param("pagina", "1").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/control-panel");

        assertThat(count(primera, "data-operacion=")).isEqualTo(20);
        assertThat(primera).contains(">25<").contains("Página <span>1</span> de <span>2</span>").contains("pestana=limpiezas&amp;pagina=1");
        assertThat(count(segunda, "data-operacion=")).isEqualTo(5);
    }

    // ==================== Detalle de una operación ====================

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_PRODUCCION"})
    @DisplayName("CP-CBC-20: el operario y el gerente ven el detalle de una limpieza con la información del barril y los datos por tema")
    void detalleOperacion(Rol rol) throws Exception {
        String html = html(get("/control-barriles/5/operaciones/limpieza/7").with(como(rol)), "control_barril/operacion-detalle");

        assertThat(html).contains("data-informacion-barril").contains("data-operacion-tipo").contains("Limpieza con soda cáustica").contains("Fecha de registro")
                .contains("Registrado por").contains("operario1").doesNotContain("Responsable").doesNotContain("data-operacion-anulacion");
    }

    @Test
    @DisplayName("CP-CBC-21: el detalle de un despacho, un fraccionamiento y una devolución muestra el cliente y la fecha estimada, la cantidad extraída y los litros descartados")
    void detalleConDatosPropios() throws Exception {
        when(servicio.listarOperaciones(5L, TipoOperacionBarril.DESPACHO)).thenReturn(List.of(OperacionBarrilResponseDTO.builder().id(9L).tipo(TipoOperacionBarril.DESPACHO)
                .estado(EstadoTransaccion.REGISTRADO).fecha(FIN).observaciones("Entrega").cliente("Bar El Puerto").fechaDevolucionEstimada(LocalDate.of(2026, 11, 30)).build()));
        when(servicio.listarOperaciones(5L, TipoOperacionBarril.FRACCIONAMIENTO)).thenReturn(List.of(OperacionBarrilResponseDTO.builder().id(10L).tipo(TipoOperacionBarril.FRACCIONAMIENTO)
                .estado(EstadoTransaccion.REGISTRADO).fecha(FIN).observaciones("Muestra").cantidadExtraida(5.5).build()));
        when(servicio.listarOperaciones(5L, TipoOperacionBarril.DEVOLUCION)).thenReturn(List.of(OperacionBarrilResponseDTO.builder().id(11L).tipo(TipoOperacionBarril.DEVOLUCION)
                .estado(EstadoTransaccion.REGISTRADO).fecha(FIN).observaciones("Devuelto").litrosDescartados(12.0).build()));

        String despacho = html(get("/control-barriles/5/operaciones/despacho/9").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)), "control_barril/operacion-detalle");
        String fraccionamiento = html(get("/control-barriles/5/operaciones/fraccionamiento/10").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_barril/operacion-detalle");
        String devolucion = html(get("/control-barriles/5/operaciones/devolucion/11").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/operacion-detalle");

        assertThat(despacho).contains("Bar El Puerto").contains("Fecha estimada de devolución").contains("30/11/2026");
        assertThat(fraccionamiento).contains("Cantidad extraída").contains("5,5 L");
        assertThat(devolucion).contains("Litros descartados").contains("12 L");
    }

    @Test
    @DisplayName("CP-CBC-22: el operario no ve el detalle de un despacho y el encargado no ve el de una limpieza")
    void detallePorPermiso() throws Exception {
        mockMvc.perform(get("/control-barriles/5/operaciones/despacho/9").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", SIN_PERMISOS));
        mockMvc.perform(get("/control-barriles/5/operaciones/limpieza/7").with(como(Rol.ENCARGADO_DE_DISTRIBUCION))).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", SIN_PERMISOS));
    }

    @Test
    @DisplayName("CP-CBC-23: el detalle de una operación anulada muestra cuándo y por qué, sin el usuario")
    void detalleOperacion_anulada() throws Exception {
        String html = html(get("/control-barriles/5/operaciones/limpieza/6").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/operacion-detalle");

        assertThat(html.substring(html.indexOf("data-seccion-operacion"), html.indexOf("</section>", html.indexOf("data-seccion-operacion")))).doesNotContain("Anulado por").doesNotContain("Responsable");
        assertThat(html).contains("data-operacion-anulacion").contains("Error de carga").contains("Fecha de anulación");
    }

    @Test
    @DisplayName("CP-CBC-24: una operación de otro barril o un barril inexistente vuelve al origen con alerta de error, sin revelar datos")
    void detalleOperacion_inexistente() throws Exception {
        mockMvc.perform(get("/control-barriles/5/operaciones/falla/70").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/control-barriles"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get("/control-barriles/99").with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/control-barriles"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-CBC-25: Registrado por aparece solo en el detalle de la operación, no en el panel ni en el historial, y Responsable no aparece en ninguna")
    void registradoPorSoloEnElDetalle() throws Exception {
        String detalle = html(get("/control-barriles/5/operaciones/limpieza/7").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/operacion-detalle");
        String panel = html(get("/control-barriles/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-panel");
        String historial = html(get("/control-barriles/5/historial").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_barril/control-historial");

        assertThat(detalle).contains("Registrado por").doesNotContain("Responsable");
        assertThat(panel).doesNotContain("Registrado por").doesNotContain("operario1").doesNotContain("Responsable");
        assertThat(historial).doesNotContain("Registrado por").doesNotContain("operario1").doesNotContain("Responsable");
    }

    // ==================== Apoyo ====================

    private String html(MockHttpServletRequestBuilder request, String vista) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andExpect(view().name(vista))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static int count(String texto, String buscado) {
        return texto.split(java.util.regex.Pattern.quote(buscado), -1).length - 1;
    }

    public static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
