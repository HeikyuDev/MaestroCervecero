package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.EncabezadoReporte;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.GraficoBarras;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteLotes;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PeriodoEstadistica;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReportesDePrueba;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(EstadisticasController.class)
@Import({SecurityConfig.class, ControllerAdvices.class, ReporteLotes.class})
class EstadisticasControllerTest {

    private static final String URL = "/estadisticas";
    private static final String URL_PDF = "/estadisticas/pdf";
    private static final byte[] PDF = "%PDF-1.7 contenido".getBytes(StandardCharsets.ISO_8859_1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEstadisticaProduccionServicio estadisticaProduccionServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private IUsuarioServicio usuarioServicio;

    @MockitoBean
    private IGeneradorPdfServicio generadorPdfServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== Permisos y pestañas ====================

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "GERENTE_DE_COMPRAS"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("CP-ESTC-01: los roles sin permisos de estadísticas no ven la pantalla ni el PDF: se redirige con la alerta de permisos y no se consulta nada")
    void sinPermiso_seRechaza(Rol rol) throws Exception {
        for (String url : List.of(URL, URL_PDF)) {
            mockMvc.perform(get(url).with(como(rol)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"))
                    .andExpect(flash().attribute("mensaje", "No tiene permisos para realizar esta acción"));
        }
        verifyNoInteractions(estadisticaProduccionServicio, generadorPdfServicio);
    }

    @Test
    @DisplayName("CP-ESTC-02: sin sesión la pantalla y el PDF redirigen al inicio de sesión")
    void sinSesion_redirigeAlLogin() throws Exception {
        for (String url : List.of(URL, URL_PDF)) {
            mockMvc.perform(get(url)).andExpect(status().is3xxRedirection());
        }
        verifyNoInteractions(estadisticaProduccionServicio, generadorPdfServicio);
    }

    @Test
    @DisplayName("CP-ESTC-03: el GERENTE_DE_COMPRAS entra a Estadísticas pero no ve la pestaña Lotes (es de producción): la pantalla dice que no hay reportes y su PDF vuelve a la pantalla")
    void gerenteDeCompras_noVeLasPestanasDeProduccion() throws Exception {
        String html = pantalla(get(URL).with(como(Rol.GERENTE_DE_COMPRAS)));

        assertThat(html).contains("No hay reportes disponibles para su usuario").doesNotContain("data-pestana-lotes");
        mockMvc.perform(get(URL_PDF).with(como(Rol.GERENTE_DE_COMPRAS))).andExpect(redirectedUrl(URL));
        verifyNoInteractions(estadisticaProduccionServicio, generadorPdfServicio);
    }

    @Test
    @DisplayName("CP-ESTC-04: sin parámetros el GERENTE_DE_PRODUCCION ve la primera pestaña que puede ver (Lotes), activa y con ícono, la pregunta que responde y el período del mes en curso hasta hoy")
    void pantalla_pestanaYPeriodoPorDefecto() throws Exception {
        LocalDate hoy = LocalDate.now();
        conReporte(ReportesDePrueba.lotes(3, 3));

        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("estadisticas/estadisticas"))
                .andExpect(model().attribute("pestanaActiva", PestanaEstadistica.LOTES))
                .andExpect(model().attribute("pestanas", List.of(PestanaEstadistica.LOTES)))
                .andExpect(model().attribute("filtroDesde", hoy.withDayOfMonth(1)))
                .andExpect(model().attribute("filtroHasta", hoy))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        CriteriosReporteLotes criterios = criteriosUsados();
        assertThat(criterios.desde()).isEqualTo(hoy.withDayOfMonth(1));
        assertThat(criterios.hasta()).isEqualTo(hoy);
        assertThat(criterios.idReceta()).isNull();
        assertThat(criterios.estado()).isNull();
        String texto = HtmlUtils.htmlUnescape(html);
        assertThat(texto).contains("<title>Estadísticas - Maestro Cervecero</title>")
                .contains("data-pestana-lotes=\"true\"").contains("aria-current=\"page\"").contains("bi bi-collection")
                .contains("Cuántos lotes se planificaron en el período y cómo terminaron");
    }

    @Test
    @DisplayName("CP-ESTC-05: la pantalla muestra el encabezado de resultados con el período y 'Exportar PDF' con los mismos criterios, como máximo 4 indicadores, un único gráfico con sus valores y una tabla de hasta 7 columnas con 'Abrir' centrado, la fila de totales y la paginación")
    void pantalla_estructuraDelReporte() throws Exception {
        conReporte(ReportesDePrueba.lotes(3, 3));

        String html = HtmlUtils.htmlUnescape(pantalla(get(URL).param("desde", "2026-11-01").param("hasta", "2026-11-30")
                .param("estado", "FINALIZADO").with(como(Rol.GERENTE_DE_PRODUCCION))));

        assertThat(contar(html, "data-indicador=\"")).isEqualTo(4).isLessThanOrEqualTo(ReporteEstadisticoResponseDTO.MAXIMO_INDICADORES);
        assertThat(html).contains("Lotes del período").contains("Terminados en plazo").contains("data-tooltip-contenido=\"ayuda-indicador-en-plazo\"");
        assertThat(contar(html, "data-grafico-barras=\"true\"")).isEqualTo(1);
        assertThat(html).contains("Lotes por estado").contains("data-valor-barra=\"true\"");
        String encabezado = html.substring(html.indexOf("<thead"), html.indexOf("</thead>"));
        assertThat(contar(encabezado, "<th ")).isEqualTo(6);
        assertThat(encabezado).contains("Lote").contains("Estado").contains("Litros envasados").contains("Fin estimado").contains("Fin real")
                .contains("text-center col-acciones");
        assertThat(contar(html, "data-boton-abrir")).isEqualTo(3);
        assertThat(html).contains("href=\"/lotes/1\"").contains("Totales del período").contains("data-fila-totales")
                .contains("<span>3</span> <span>lotes</span>").contains("Página <span>1</span> de <span>1</span>")
                .contains("href=\"/estadisticas/pdf?pestana=lotes&amp;desde=2026-11-01&amp;hasta=2026-11-30&amp;estado=FINALIZADO\"".replace("&amp;", "&"))
                .contains("href=\"/estadisticas?pestana=lotes\" data-limpiar-filtros");
        assertThat(html).contains("Resultados del 01/11/2026 al 30/11/2026").contains("data-criterio-fecha")
                .contains("Se consideran los lotes con inicio estimado dentro del período, en cualquier estado");
        assertThat(html).contains("method=\"get\" novalidate").contains("<option value=\"FINALIZADO\" selected=\"selected\">Finalizado</option>");
    }

    @Test
    @DisplayName("CP-ESTC-06: sin LOTE_CONSULTAR la columna Acciones no muestra el botón 'Abrir'")
    void pantalla_sinLoteConsultar_noMuestraAbrir() throws Exception {
        conReporte(ReportesDePrueba.lotes(2, 2));

        String html = pantalla(get(URL).with(conPermisos(Permiso.ESTADISTICA_PRODUCCION_CONSULTAR)));

        assertThat(html).doesNotContain("data-boton-abrir").contains("LOT-R7-01");
    }

    @Test
    @DisplayName("CP-ESTC-07: la paginación conserva todos los criterios, y una página fuera de rango muestra la última")
    void pantalla_paginacion() throws Exception {
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenAnswer(invocacion -> {
            Pageable pageable = invocacion.getArgument(1);
            return pageable.getPageNumber() >= 3 ? ReportesDePrueba.lotesEnPagina(0, pageable, 60) : ReportesDePrueba.lotesEnPagina(20, pageable, 60);
        });

        String html = HtmlUtils.htmlUnescape(pantalla(get(URL).param("desde", "2026-11-01").param("hasta", "2026-11-30")
                .param("pagina", "9").with(como(Rol.GERENTE_DE_PRODUCCION))));

        assertThat(html).contains("Página <span>3</span> de <span>3</span>")
                .contains("href=\"/estadisticas?pestana=lotes&desde=2026-11-01&hasta=2026-11-30&pagina=1\"");
        ArgumentCaptor<Pageable> paginas = ArgumentCaptor.forClass(Pageable.class);
        verify(estadisticaProduccionServicio, times(2)).generarReporteDeLotes(any(), paginas.capture());
        assertThat(paginas.getAllValues()).extracting(Pageable::getPageNumber).containsExactly(9, 2);
    }

    @Test
    @DisplayName("CP-ESTC-08: un período que el Service rechaza se muestra como alerta, conserva los criterios y en lugar del reporte se pide un período válido, sin 'Exportar PDF'")
    void pantalla_periodoInvalido() throws Exception {
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenThrow(new ReglaNegocioException(PeriodoEstadistica.MENSAJE_PERIODO_EXCEDIDO));

        String html = HtmlUtils.htmlUnescape(mockMvc.perform(get(URL).param("desde", "2026-01-01").param("hasta", "2027-06-30")
                        .with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mensaje", PeriodoEstadistica.MENSAJE_PERIODO_EXCEDIDO))
                .andExpect(model().attribute("tipo", "warning"))
                .andExpect(model().attribute("filtroDesde", LocalDate.of(2026, 1, 1)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(html).contains("data-sin-reporte").contains("Cuántos lotes se planificaron").doesNotContain("data-exportar-pdf")
                .doesNotContain("data-indicador=");
    }

    @Test
    @DisplayName("CP-ESTC-09: una fecha que no existe se informa sin llamar al Service; Desde y Hasta vacíos se envían al Service, que los rechaza como obligatorios")
    void pantalla_fechasInvalidasYVacias() throws Exception {
        mockMvc.perform(get(URL).param("desde", "2026-02-30").param("hasta", "2026-03-10").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(model().attribute("mensaje", "La fecha desde no es válida"));
        verifyNoInteractions(estadisticaProduccionServicio);

        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenThrow(new ReglaNegocioException(PeriodoEstadistica.MENSAJE_DESDE_OBLIGATORIA));
        mockMvc.perform(get(URL).param("desde", "").param("hasta", "").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(model().attribute("mensaje", PeriodoEstadistica.MENSAJE_DESDE_OBLIGATORIA));
        CriteriosReporteLotes criterios = criteriosUsados();
        assertThat(criterios.desde()).isNull();
        assertThat(criterios.hasta()).isNull();
    }

    @Test
    @DisplayName("CP-ESTC-10: la receta elegida con el buscador y el estado filtran; un estado inexistente no filtra y se avisa, y una receta que el buscador no ofrece no filtra")
    void pantalla_criteriosPropios() throws Exception {
        conReporte(ReportesDePrueba.lotes(1, 1));
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(7L)).thenReturn(receta(7L, "Pale Ale", Estado.BAJA));
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(8L)).thenThrow(new RecursoNoEncontradoException("No"));

        String html = HtmlUtils.htmlUnescape(pantalla(get(URL).param("idReceta", "7").param("estado", "cancelado")
                .with(como(Rol.GERENTE_DE_PRODUCCION))));
        CriteriosReporteLotes conFiltros = criteriosUsados();
        assertThat(conFiltros.idReceta()).isEqualTo(7L);
        assertThat(conFiltros.textoReceta()).isEqualTo("Pale Ale (dada de baja)");
        assertThat(conFiltros.estado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(html).contains("value=\"Pale Ale (dada de baja)\"").contains("data-buscador-url=\"/lotes/filtros/recetas\"");

        mockMvc.perform(get(URL).param("idReceta", "8").param("estado", "OTRO").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(model().attribute("tipo", "warning"));
        ArgumentCaptor<CriteriosReporteLotes> captor = ArgumentCaptor.forClass(CriteriosReporteLotes.class);
        verify(estadisticaProduccionServicio, times(2)).generarReporteDeLotes(captor.capture(), any());
        assertThat(captor.getValue().idReceta()).isNull();
        assertThat(captor.getValue().estado()).isNull();
    }

    @Test
    @DisplayName("CP-ESTC-11: una pestaña inexistente muestra la primera que el usuario puede ver")
    void pantalla_pestanaInexistente() throws Exception {
        conReporte(ReportesDePrueba.lotes(1, 1));

        mockMvc.perform(get(URL).param("pestana", "costos").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(model().attribute("pestanaActiva", PestanaEstadistica.LOTES));
    }

    @Test
    @DisplayName("CP-ESTC-24: una tabla sin filas dice 'Sin resultados para los criterios indicados' y no muestra la fila de totales")
    void pantalla_tablaVaciaSinTotales() throws Exception {
        conReporte(ReportesDePrueba.lotes(0, 0));

        String html = pantalla(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)));

        assertThat(html).contains("Sin resultados para los criterios indicados").doesNotContain("data-fila-totales");
    }

    // ==================== Reporte impreso ====================

    @Test
    @DisplayName("CP-ESTC-12: el PDF se devuelve como application/pdf en línea con el nombre estadisticas-lotes_desde_hasta.pdf, con la plantilla única, la tabla completa hasta el tope técnico el emisor y la terminal")
    void pdf_contenidoYNombre() throws Exception {
        conReporte(ReportesDePrueba.lotes(3, 3));
        when(usuarioServicio.buscarPorUsername("GERENTE_DE_PRODUCCION")).thenReturn(UsuarioResponseDTO.builder().nombre("Ana").apellido("Pérez").build());
        when(generadorPdfServicio.generar(eq("estadisticas/pdf/reporte"), anyMap())).thenReturn(PDF);

        mockMvc.perform(get(URL_PDF).param("pestana", "lotes").param("desde", "2026-11-01").param("hasta", "2026-11-30")
                        .with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"estadisticas-lotes_2026-11-01_2026-11-30.pdf\""));

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(estadisticaProduccionServicio).generarReporteDeLotes(any(), pagina.capture());
        assertThat(pagina.getValue()).isEqualTo(PageRequest.of(0, com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos.REPORTE_IMPRESO_MAXIMO_FILAS));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> modelo = ArgumentCaptor.forClass(Map.class);
        verify(generadorPdfServicio).generar(eq("estadisticas/pdf/reporte"), modelo.capture());
        assertThat(modelo.getValue()).containsKeys("reporte", "grafico", "encabezado", "fuente");
        assertThat(((EncabezadoReporte) modelo.getValue().get("encabezado")).getEmitidoPor()).isEqualTo("GERENTE_DE_PRODUCCION (Ana Pérez)");
        assertThat(((EncabezadoReporte) modelo.getValue().get("encabezado")).getTerminal()).as("terminal de la petición").isNotBlank();
        assertThat(modelo.getValue().get("grafico")).isInstanceOf(GraficoBarras.class);
    }

    @Test
    @DisplayName("CP-ESTC-13: si el PDF no se puede generar por los criterios (fecha inexistente o período rechazado), vuelve a la pantalla con los mismos parámetros")
    void pdf_criteriosInvalidos_vuelveALaPantalla() throws Exception {
        mockMvc.perform(get(URL_PDF).param("pestana", "lotes").param("desde", "2026-02-30").param("hasta", "2026-03-01")
                        .with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(redirectedUrl("/estadisticas?pestana=lotes&desde=2026-02-30&hasta=2026-03-01"));

        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenThrow(new ReglaNegocioException(PeriodoEstadistica.MENSAJE_PERIODO_INVERTIDO));
        mockMvc.perform(get(URL_PDF).param("pestana", "lotes").param("desde", "2026-03-01").param("hasta", "2026-02-01").param("estado", "PENDIENTE")
                        .with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(redirectedUrl("/estadisticas?pestana=lotes&desde=2026-03-01&hasta=2026-02-01&estado=PENDIENTE"));
        verifyNoInteractions(generadorPdfServicio);
    }

    // ==================== Apoyo ====================

    private void conReporte(ReporteEstadisticoResponseDTO reporte) {
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenReturn(reporte);
    }

    private CriteriosReporteLotes criteriosUsados() {
        ArgumentCaptor<CriteriosReporteLotes> captor = ArgumentCaptor.forClass(CriteriosReporteLotes.class);
        verify(estadisticaProduccionServicio).generarReporteDeLotes(captor.capture(), any());
        return captor.getValue();
    }

    private String pantalla(org.springframework.test.web.servlet.RequestBuilder peticion) throws Exception {
        return mockMvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static RecetaResponseDTO receta(Long id, String nombre, Estado estado) {
        return RecetaResponseDTO.builder().id(id).estado(estado).version(VersionRecetaResponseDTO.builder().nombre(nombre).build()).build();
    }

    private static int contar(String texto, String buscado) {
        Matcher m = Pattern.compile(Pattern.quote(buscado)).matcher(texto);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }

    private static RequestPostProcessor conPermisos(Permiso... permisos) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (Permiso permiso : permisos) {
            autoridades.add(new SimpleGrantedAuthority(permiso.name()));
        }
        return user("usuario").authorities(autoridades);
    }
}
