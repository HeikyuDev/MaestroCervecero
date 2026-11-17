package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCompras;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteEquipamiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteInsumos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReportePrecios;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteProveedores;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteRecepcion;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteTiempos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReportesDePrueba;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las pestañas de producción y de compras de la pantalla "Estadísticas": permisos, pestaña por defecto, criterios propios
 * de cada una y reporte impreso. Todas se arman con la misma pantalla y la misma plantilla del PDF. Se habilita la pestaña
 * "Tiempos" (deshabilitada por defecto) para verificar su reporte.
 */
@WebMvcTest(EstadisticasController.class)
@TestPropertySource(properties = "estadisticas.tiempos.habilitada=true")
@Import({SecurityConfig.class, ControllerAdvices.class, ReporteLotes.class, ReporteCostos.class, ReporteTiempos.class, ReporteInsumos.class,
        ReporteEquipamiento.class, ReporteCompras.class, ReporteProveedores.class, ReportePrecios.class, ReporteRecepcion.class,
        BuscadoresEstadisticasController.class})
class EstadisticasPestanasControllerTest {

    private static final String URL = "/estadisticas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEstadisticaProduccionServicio estadisticaProduccionServicio;

    @MockitoBean
    private IEstadisticaComprasServicio estadisticaComprasServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private IProveedorServicio proveedorServicio;

    @MockitoBean
    private IControlEquipamientoServicio controlEquipamientoServicio;

    @MockitoBean
    private IUsuarioServicio usuarioServicio;

    @MockitoBean
    private IGeneradorPdfServicio generadorPdfServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    private void todosLosReportes() {
        var reporte = ReportesDePrueba.lotes(2, 2);
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenReturn(reporte);
        when(estadisticaProduccionServicio.generarReporteDeCostos(any(), any())).thenReturn(reporte);
        when(estadisticaProduccionServicio.generarReporteDeTiempos(any(), any())).thenReturn(reporte);
        when(estadisticaProduccionServicio.generarReporteDeInsumos(any(), any())).thenReturn(reporte);
        when(estadisticaProduccionServicio.generarReporteDeEquipamiento(any(), any())).thenReturn(reporte);
        when(estadisticaComprasServicio.generarReporteDeCompras(any(), any())).thenReturn(reporte);
        when(estadisticaComprasServicio.generarReporteDeProveedores(any(), any())).thenReturn(reporte);
        when(estadisticaComprasServicio.generarReporteDePrecios(any(), any())).thenReturn(reporte);
        when(estadisticaComprasServicio.generarReporteDeRecepcion(any(), any())).thenReturn(reporte);
    }

    private String pantalla(String url, Rol rol) throws Exception {
        return HtmlUtils.htmlUnescape(mockMvc.perform(get(url).with(como(rol))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("CP-ESTC-26: un reporte con varias tablas las muestra una debajo de la otra, cada una con su título y sin paginación, y aclara qué fecha decide los lotes del período")
    void pantalla_variasTablasConTitulo() throws Exception {
        when(estadisticaProduccionServicio.generarReporteDeCostos(any(), any())).thenReturn(ReportesDePrueba.costos("Pale Ale", "Stout"));

        String html = pantalla("/estadisticas?pestana=costos&desde=2026-11-01&hasta=2026-11-30", Rol.GERENTE_DE_PRODUCCION);

        assertThat(html.split("data-tabla-reporte", -1).length - 1).isEqualTo(2);
        assertThat(html.split("data-titulo-tabla", -1).length - 1).isEqualTo(2);
        assertThat(html.indexOf(">Pale Ale</h2>")).isNotNegative().isLessThan(html.indexOf(">Stout</h2>"));
        assertThat(html).doesNotContain("Página <span>").contains("Lotes con envasado finalizado dentro del período");
    }

    @Test
    @DisplayName("CP-ESTC-27: con tres indicadores la pantalla usa una grilla de tres columnas y tarjetas más amplias, sin hueco; con cuatro conserva la grilla de cuatro")
    void pantalla_indicadoresSeAdaptanALaCantidad() throws Exception {
        when(estadisticaProduccionServicio.generarReporteDeCostos(any(), any())).thenReturn(ReportesDePrueba.costos("Pale Ale"));
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenReturn(ReportesDePrueba.lotes(1, 1));

        String tres = pantalla("/estadisticas?pestana=costos&desde=2026-11-01&hasta=2026-11-30", Rol.GERENTE_DE_PRODUCCION);
        String cuatro = pantalla("/estadisticas?pestana=lotes&desde=2026-11-01&hasta=2026-11-30", Rol.GERENTE_DE_PRODUCCION);

        assertThat(tres).contains("data-cantidad-indicadores=\"3\"").contains("grid-cols-1 gap-4 md:grid-cols-3").doesNotContain("gap-4 sm:grid-cols-2 lg:grid-cols-4");
        assertThat(tres.split("data-indicador=\"", -1).length - 1).isEqualTo(3);
        assertThat(tres).contains("p-7").contains("text-4xl");
        assertThat(cuatro).contains("data-cantidad-indicadores=\"4\"").contains("gap-4 sm:grid-cols-2 lg:grid-cols-4").doesNotContain("gap-4 md:grid-cols-3");
    }

    // ==================== Pestañas por permiso ====================

    @Test
    @DisplayName("CP-ESTC-14: el GERENTE_DE_PRODUCCION ve las cinco pestañas de producción (Lotes, Costos, Tiempos, Insumos y Equipamiento) con su ícono y ninguna de compras")
    void gerenteDeProduccion_veLasPestanasDeProduccion() throws Exception {
        todosLosReportes();

        String html = pantalla(URL, Rol.GERENTE_DE_PRODUCCION);

        for (PestanaEstadistica p : List.of(PestanaEstadistica.LOTES, PestanaEstadistica.COSTOS, PestanaEstadistica.TIEMPOS,
                PestanaEstadistica.INSUMOS, PestanaEstadistica.EQUIPAMIENTO)) {
            assertThat(html).contains("data-pestana-" + p.getClave() + "=\"true\"").contains("bi " + p.getIcono());
        }
        for (String clave : List.of("compras", "proveedores", "precios", "recepcion")) {
            assertThat(html).doesNotContain("data-pestana-" + clave + "=");
        }
    }

    @Test
    @DisplayName("CP-ESTC-15: el GERENTE_DE_COMPRAS ve las cuatro pestañas de compras (Compras, Proveedores, Precios y Recepción), la primera activa por defecto, y ninguna de producción")
    void gerenteDeCompras_veLasPestanasDeCompras() throws Exception {
        todosLosReportes();

        String html = pantalla(URL, Rol.GERENTE_DE_COMPRAS);

        for (String clave : List.of("compras", "proveedores", "precios", "recepcion")) {
            assertThat(html).contains("data-pestana-" + clave + "=\"true\"");
        }
        for (String clave : List.of("lotes", "costos", "tiempos", "insumos", "equipamiento")) {
            assertThat(html).doesNotContain("data-pestana-" + clave + "=");
        }
        assertThat(html).contains("Cuánto se compró y a qué proveedores");
        verify(estadisticaComprasServicio).generarReporteDeCompras(any(), any());
        verifyNoInteractions(estadisticaProduccionServicio);
    }

    @Test
    @DisplayName("CP-ESTC-16: una pestaña de otra gerencia no se muestra ni se consulta: cada gerente cae en la primera de las suyas, y su PDF tampoco la expone")
    void pestanaDeOtraGerencia() throws Exception {
        todosLosReportes();

        mockMvc.perform(get(URL).param("pestana", "costos").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(model().attribute("pestanaActiva", PestanaEstadistica.COMPRAS));
        mockMvc.perform(get(URL).param("pestana", "precios").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(model().attribute("pestanaActiva", PestanaEstadistica.LOTES));
        when(generadorPdfServicio.generar(any(), anyMap())).thenReturn(new byte[]{1});
        mockMvc.perform(get(URL + "/pdf").param("pestana", "costos").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("estadisticas-compras_")));
        verify(estadisticaProduccionServicio, org.mockito.Mockito.never()).generarReporteDeCostos(any(), any());
    }

    // ==================== Cada pestaña: pantalla y PDF ====================

    @Test
    @DisplayName("CP-ESTC-17: cada pestaña muestra su pregunta y sus criterios propios (como máximo dos) y consulta su reporte, y su PDF usa la plantilla única con el nombre estadisticas-<clave>_desde_hasta.pdf")
    void cadaPestana_pantallaYPdf() throws Exception {
        todosLosReportes();
        when(generadorPdfServicio.generar(eq("estadisticas/pdf/reporte"), anyMap())).thenReturn(new byte[]{1, 2});
        Map<PestanaEstadistica, List<String>> campos = Map.of(
                PestanaEstadistica.LOTES, List.of("idReceta", "estado"),
                PestanaEstadistica.COSTOS, List.of("idReceta"),
                PestanaEstadistica.TIEMPOS, List.of("etapa", "idReceta"),
                PestanaEstadistica.INSUMOS, List.of("tipoInsumo", "idReceta"),
                PestanaEstadistica.EQUIPAMIENTO, List.of("tipo", "idEquipo"),
                PestanaEstadistica.COMPRAS, List.of("idProveedor", "estado"),
                PestanaEstadistica.PROVEEDORES, List.of("idProveedor", "tipoInsumo"),
                PestanaEstadistica.PRECIOS, List.of("tipoInsumo", "idProveedor"),
                PestanaEstadistica.RECEPCION, List.of("idProveedor", "tipoIngreso"));

        for (PestanaEstadistica pestana : PestanaEstadistica.values()) {
            Rol rol = pestana.getPermiso().name().contains("COMPRAS") ? Rol.GERENTE_DE_COMPRAS : Rol.GERENTE_DE_PRODUCCION;
            String html = pantalla(URL + "?pestana=" + pestana.getClave() + "&desde=2026-11-01&hasta=2026-11-30", rol);

            assertThat(html).as(pestana.getClave()).contains(pestana.getDescripcion()).contains("aria-current=\"page\"")
                    .contains("Resultados del 01/11/2026 al 30/11/2026").contains("data-exportar-pdf");
            List<String> esperados = campos.get(pestana);
            assertThat(esperados).hasSizeLessThanOrEqualTo(ReporteDePestana.MAXIMO_CAMPOS);
            for (String campo : esperados) {
                assertThat(html).as(pestana.getClave() + " " + campo).contains("data-campo=\"" + campo + "\"");
            }
            assertThat(html.split("data-campo=\"").length - 1).as(pestana.getClave() + ": solo sus criterios").isEqualTo(esperados.size());
            assertThat(html.split("data-indicador=\"").length - 1).isLessThanOrEqualTo(4);

            mockMvc.perform(get(URL + "/pdf?pestana=" + pestana.getClave() + "&desde=2026-11-01&hasta=2026-11-30").with(como(rol)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/pdf"))
                    .andExpect(header().string("Content-Disposition", "inline; filename=\"estadisticas-" + pestana.getClave() + "_2026-11-01_2026-11-30.pdf\""));
        }
    }

    // ==================== Criterios propios ====================

    @Test
    @DisplayName("CP-ESTC-18: los criterios de las pestañas de producción llegan interpretados al Service: etapa, tipo de insumo, tipo de equipo, equipo y receta; un valor inexistente no filtra y se avisa")
    void criteriosDeProduccion() throws Exception {
        todosLosReportes();
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(7L)).thenReturn(
                RecetaResponseDTO.builder().id(7L).version(VersionRecetaResponseDTO.builder().nombre("Pale Ale").build()).build());
        when(controlEquipamientoServicio.buscarResumen(4L)).thenReturn(EquipamientoResumenResponseDTO.builder().id(4L).identificadorInterno("FER-01").build());

        pantalla(URL + "?pestana=tiempos&etapa=fermentacion&idReceta=7", Rol.GERENTE_DE_PRODUCCION);
        pantalla(URL + "?pestana=insumos&tipoInsumo=LUPULO", Rol.GERENTE_DE_PRODUCCION);
        pantalla(URL + "?pestana=equipamiento&tipo=FERMENTADOR&idEquipo=4", Rol.GERENTE_DE_PRODUCCION);

        ArgumentCaptor<CriteriosReporteTiempos> tiempos = ArgumentCaptor.forClass(CriteriosReporteTiempos.class);
        verify(estadisticaProduccionServicio).generarReporteDeTiempos(tiempos.capture(), any());
        assertThat(tiempos.getValue().etapa()).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(tiempos.getValue().idReceta()).isEqualTo(7L);
        assertThat(tiempos.getValue().textoReceta()).isEqualTo("Pale Ale");
        ArgumentCaptor<CriteriosReporteInsumos> insumos = ArgumentCaptor.forClass(CriteriosReporteInsumos.class);
        verify(estadisticaProduccionServicio).generarReporteDeInsumos(insumos.capture(), any());
        assertThat(insumos.getValue().tipoInsumo()).isEqualTo(TipoInsumo.LUPULO);
        ArgumentCaptor<CriteriosReporteEquipamiento> equipos = ArgumentCaptor.forClass(CriteriosReporteEquipamiento.class);
        verify(estadisticaProduccionServicio).generarReporteDeEquipamiento(equipos.capture(), any());
        assertThat(equipos.getValue().tipo()).isEqualTo(TipoEquipamiento.FERMENTADOR);
        assertThat(equipos.getValue().idEquipo()).isEqualTo(4L);
        assertThat(equipos.getValue().textoEquipo()).isEqualTo("FER-01");

        mockMvc.perform(get(URL + "?pestana=tiempos&etapa=otra").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(model().attribute("tipo", "warning"))
                .andExpect(model().attribute("mensaje", org.hamcrest.Matchers.containsString("La etapa indicada no es válida, se muestran todas")));
    }

    @Test
    @DisplayName("CP-ESTC-19: los criterios de las pestañas de compras llegan interpretados al Service: proveedor, estado, tipo de insumo y tipo de ingreso; un proveedor que ya no existe no filtra")
    void criteriosDeCompras() throws Exception {
        todosLosReportes();
        when(proveedorServicio.buscarPorId(5L)).thenReturn(ProveedorResponseDTO.builder().id(5L)
                .version(VersionProveedorResponseDTO.builder().razonSocial("Maltería del Sur").cuit("30123456789").build()).build());
        when(proveedorServicio.buscarPorId(9L)).thenThrow(new RecursoNoEncontradoException("No"));

        pantalla(URL + "?pestana=compras&idProveedor=5&estado=pendiente", Rol.GERENTE_DE_COMPRAS);
        pantalla(URL + "?pestana=proveedores&tipoInsumo=MALTA&idProveedor=9", Rol.GERENTE_DE_COMPRAS);
        pantalla(URL + "?pestana=precios&tipoInsumo=LEVADURA", Rol.GERENTE_DE_COMPRAS);
        pantalla(URL + "?pestana=recepcion&tipoIngreso=directo", Rol.GERENTE_DE_COMPRAS);

        ArgumentCaptor<CriteriosReporteCompras> compras = ArgumentCaptor.forClass(CriteriosReporteCompras.class);
        verify(estadisticaComprasServicio).generarReporteDeCompras(compras.capture(), any());
        assertThat(compras.getValue().idProveedor()).isEqualTo(5L);
        assertThat(compras.getValue().textoProveedor()).startsWith("Maltería del Sur (");
        assertThat(compras.getValue().estado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        ArgumentCaptor<CriteriosReporteProveedores> proveedores = ArgumentCaptor.forClass(CriteriosReporteProveedores.class);
        verify(estadisticaComprasServicio).generarReporteDeProveedores(proveedores.capture(), any());
        assertThat(proveedores.getValue().tipoInsumo()).isEqualTo(TipoInsumo.MALTA);
        assertThat(proveedores.getValue().idProveedor()).isNull();
        ArgumentCaptor<CriteriosReportePrecios> precios = ArgumentCaptor.forClass(CriteriosReportePrecios.class);
        verify(estadisticaComprasServicio).generarReporteDePrecios(precios.capture(), any());
        assertThat(precios.getValue().tipoInsumo()).isEqualTo(TipoInsumo.LEVADURA);
        ArgumentCaptor<CriteriosReporteRecepcion> recepcion = ArgumentCaptor.forClass(CriteriosReporteRecepcion.class);
        verify(estadisticaComprasServicio).generarReporteDeRecepcion(recepcion.capture(), any());
        assertThat(recepcion.getValue().tipoIngreso()).isEqualTo(TipoIngreso.DIRECTO);
    }

    // ==================== Buscador de equipos ====================

    @Test
    @DisplayName("CP-ESTC-20: el buscador de equipos de la pestaña Equipamiento solo lo usa quien tiene ESTADISTICA_PRODUCCION_CONSULTAR")
    void buscadorDeEquipos_permiso() throws Exception {
        mockMvc.perform(get("/estadisticas/buscador/equipos").with(como(Rol.GERENTE_DE_COMPRAS))).andExpect(status().is3xxRedirection());
        verifyNoInteractions(controlEquipamientoServicio);
    }

    // ==================== Apoyo ====================

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
