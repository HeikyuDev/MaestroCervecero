package com.github.heikyudev.maestrocervecero.presentation.controller.orden_compra;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.login.HomeController;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.AnulacionOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.FinalizacionForzadaOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleMaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
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
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

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

@WebMvcTest({OrdenCompraController.class, HomeController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class OrdenCompraControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";
    private static final String URL = "/ordenes-compra";
    private static final String VISTA_FORMULARIO = "orden_compra/orden-form";
    private static final String ENLACE_RESALTADO =
            "(?s).*href=\"/ordenes-compra\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-cart3\"></i>\\s*<span>Órdenes de Compra</span>.*";
    private static final String FILA_RESALTADA = "<tr data-fila data-prefijo=\"detallesCompra\" class=\"bg-rose-50\">";
    private static final String FILA_SIN_RESALTAR = "<tr data-fila data-prefijo=\"detallesCompra\">";
    private static final String FECHA = "2027-07-15";
    private static final String TITULO_AUTOMATICA = "Finalizada automáticamente";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOrdenCompraServicio ordenCompraServicio;

    @MockitoBean
    private IPlanificacionProduccionServicio planificacionServicio;

    @MockitoBean
    private IProveedorServicio proveedorServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /ordenes-compra ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-01: el GERENTE_DE_COMPRAS ve el listado con número, planificación, proveedor, fecha de entrega, estado, total y la acción de ver el detalle")
    void caso_cp_occ_01() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orden(9L, EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("orden_compra/orden-lista"))
                .andExpect(content().string(containsString("<title>Órdenes de compra - Maestro Cervecero</title>")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">N\\.º</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Planificación</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Proveedor</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Entrega estimada</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Estado</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Total \\(\\$\\)</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"[^\"]*\">9</td>.*")))
                .andExpect(content().string(containsString(">Plan #7 - Pale Ale<")))
                .andExpect(content().string(containsString(">Malterías del Sur S.A.<")))
                .andExpect(content().string(containsString(">15/07/2027<")))
                .andExpect(content().string(containsString(">Pendiente<")))
                .andExpect(content().string(containsString(">12.505,2468<")))
                .andExpect(content().string(containsString("href=\"/ordenes-compra/9\"")))
                .andExpect(content().string(containsString("href=\"/ordenes-compra/nuevo\"")))
                .andExpect(content().string(containsString("Nueva orden de compra")))
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-OCC-02: el ENCARGADO_DE_DEPOSITO ve el listado pero no el botón para registrar una orden")
    void caso_cp_occ_02() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orden(9L, EstadoSolicitud.FINALIZADA)), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/ordenes-compra/9\"")))
                .andExpect(content().string(containsString(">Finalizada<")))
                .andExpect(content().string(not(containsString("href=\"/ordenes-compra/nuevo\""))))
                .andExpect(content().string(not(containsString("Nueva orden de compra"))))
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-03: sin filtros pide páginas de 20 con la última registrada arriba (id descendente) y los cuatro criterios en null")
    void caso_cp_occ_03() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL)).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ordenCompraServicio).filtrarOrdenesCompra(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("id")));
        verifyNoInteractions(planificacionServicio, proveedorServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-04: los filtros por planificación, proveedor, estado y fecha llegan al service y los selectores muestran lo elegido")
    void caso_cp_occ_04() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor());

        // Act + Assert
        mockMvc.perform(get(URL).param("idPlanificacion", "7").param("idProveedor", "4")
                        .param("estado", "ANULADA").param("fechaEntregaEstimada", FECHA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/filtros/planificaciones\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione planificación\"")))
                .andExpect(content().string(containsString("value=\"Plan #7 - Pale Ale\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/buscador/proveedores\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione proveedor\"")))
                .andExpect(content().string(containsString("value=\"Malterías del Sur S.A. (30-71234567-8)\"")))
                .andExpect(content().string(containsString("data-selector-limpiar")))
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"ANULADA\" selected=\"selected\">Anulada</option>.*")))
                .andExpect(content().string(containsString("<option value=\"\">Todos</option>")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"fechaEntregaEstimada\"[^>]*value=\"" + FECHA + "\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 15/07/2027\"")));

        verify(ordenCompraServicio).filtrarOrdenesCompra(eq(7L), eq(4L), eq(EstadoSolicitud.ANULADA), eq(LocalDate.parse(FECHA)), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-05: una planificación o un proveedor inexistente no filtra el listado y su selector queda vacío")
    void caso_cp_occ_05() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        when(planificacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"));
        when(proveedorServicio.buscarPorId(98L)).thenThrow(new RecursoNoEncontradoException("No se encontró el proveedor con ID: 98"));

        // Act + Assert
        mockMvc.perform(get(URL).param("idPlanificacion", "99").param("idProveedor", "98"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"99\""))))
                .andExpect(content().string(not(containsString("value=\"98\""))));

        verify(ordenCompraServicio).filtrarOrdenesCompra(isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-06: los criterios se conservan en los links de paginación y el link Limpiar vuelve al listado sin criterios")
    void caso_cp_occ_06() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor());
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orden(9L, EstadoSolicitud.PENDIENTE)), PageRequest.of(1, 20), 60));

        // Act + Assert
        mockMvc.perform(get(URL).param("idPlanificacion", "7").param("idProveedor", "4").param("estado", "PENDIENTE")
                        .param("fechaEntregaEstimada", FECHA).param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(URL + "?pagina=0&amp;idPlanificacion=7&amp;idProveedor=4&amp;estado=PENDIENTE&amp;fechaEntregaEstimada=" + FECHA)))
                .andExpect(content().string(containsString(URL + "?pagina=2&amp;idPlanificacion=7&amp;idProveedor=4&amp;estado=PENDIENTE&amp;fechaEntregaEstimada=" + FECHA)))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/ordenes-compra\"[^>]*>\\s*Limpiar.*")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(ordenCompraServicio).filtrarOrdenesCompra(any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-07: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_occ_07() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron órdenes de compra con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-08: un estado, una fecha o una página que no se pueden convertir no son un error del sistema")
    void caso_cp_occ_08() throws Exception {
        // Act + Assert
        for (String[] parametro : new String[][]{{"estado", "inexistente"}, {"fechaEntregaEstimada", "abc"}, {"idProveedor", "abc"}, {"pagina", "abc"}}) {
            mockMvc.perform(get(URL + "?" + parametro[0] + "=" + parametro[1]).header("Referer", URL))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(URL))
                    .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                    .andExpect(flash().attribute("tipo", "warning"));
        }

        verify(ordenCompraServicio, never()).filtrarOrdenesCompra(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-09: los textos largos de la planificación y del proveedor se truncan con el texto completo en el tooltip, y las columnas de acciones quedan fijas")
    void caso_cp_occ_09() throws Exception {
        // Arrange
        String largo = "ZZ-OC-" + "X".repeat(94);
        OrdenCompraResponseDTO conTextosLargos = OrdenCompraResponseDTO.builder().id(10L).fechaEntregaEstimada(LocalDate.parse(FECHA))
                .estado(EstadoSolicitud.PENDIENTE).total(BigDecimal.ZERO)
                .planificacionProduccion(PlanificacionProduccionResponseDTO.builder().id(7L)
                        .versionReceta(VersionRecetaResponseDTO.builder().nombre(largo).build()).build())
                .proveedor(VersionProveedorResponseDTO.builder().razonSocial(largo).cuit("30712345678").build())
                .detallesCompra(List.of()).build();
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(conTextosLargos), PageRequest.of(0, 20), 1));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String tabla = html.substring(html.indexOf("<table"), html.indexOf("</table>"));

        // Assert
        assertThat(tabla).contains("data-tooltip=\"Plan #7 - " + largo + "\"", "data-tooltip=\"" + largo + "\"")
                .contains("<th class=\"col-acciones px-2 py-3 text-right whitespace-nowrap\">Acciones</th>")
                .contains("<td class=\"col-acciones px-2 py-3\">")
                .contains(">0,00<");
        assertThat(Pattern.compile("inline-block max-w-\\[(10|9)rem\\] truncate").matcher(tabla).results().count()).isEqualTo(2);
        assertThat(html.indexOf("overflow-x-auto")).isLessThan(html.indexOf("<table"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "ADMINISTRADOR", "GERENTE_COMERCIAL"})
    @DisplayName("CP-OCC-10: un rol distinto de GERENTE_DE_COMPRAS y ENCARGADO_DE_DEPOSITO no accede al listado, al detalle ni a los buscadores y no llega a ningún service")
    void caso_cp_occ_10(String rol) throws Exception {
        // Act + Assert
        for (String ruta : List.of(URL, URL + "/9", URL + "/nuevo", URL + "/buscador/planificaciones", URL + "/filtros/planificaciones",
                URL + "/buscador/recetas", URL + "/filtros/recetas", URL + "/buscador/proveedores", URL + "/buscador/items")) {
            mockMvc.perform(get(ruta).with(user("usuario").roles(rol)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }
        mockMvc.perform(post(URL).with(user("usuario").roles(rol)).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(ordenCompraServicio, planificacionServicio, proveedorServicio, recetaServicio);
    }

    @Test
    @DisplayName("CP-OCC-11: un usuario sin sesión es enviado al login en cada operación y en los buscadores, y no llega a ningún service")
    void caso_cp_occ_11() throws Exception {
        // Act + Assert
        for (String ruta : List.of(URL, URL + "/nuevo", URL + "/9", URL + "/buscador/planificaciones", URL + "/filtros/planificaciones",
                URL + "/buscador/recetas", URL + "/filtros/recetas", URL + "/buscador/proveedores", URL + "/buscador/items")) {
            mockMvc.perform(get(ruta)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        }
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/9/anular").with(csrf()).param("motivo", "x")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/9/finalizar").with(csrf()).param("motivo", "x")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));

        verifyNoInteractions(ordenCompraServicio, planificacionServicio, proveedorServicio, recetaServicio);
    }

    // ==================== GET /ordenes-compra/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-12: el formulario de alta muestra, de arriba hacia abajo, planificación, proveedor, fecha e ítems, con asteriscos, placeholders, ayudas y la ventana para agregar ítems fuera del formulario")
    void caso_cp_occ_12() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("Nueva orden de compra")))
                .andExpect(content().string(containsString("action=\"/ordenes-compra\"")))
                .andExpect(content().string(matchesPattern("(?s).*<form id=\"orden-form\"[^>]*method=\"post\" novalidate data-un-solo-envio[^>]*>.*")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/buscador/planificaciones\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione planificación\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/buscador/proveedores\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione proveedor\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 15/07/2027\"")))
                .andExpect(content().string(containsString("Solo planificaciones pendientes. Define qué insumos se pueden pedir.</p>")))
                .andExpect(content().string(containsString("Se usa la versión vigente de su catálogo.</p>")))
                .andExpect(content().string(containsString("Desde hoy y hasta un año.</p>")))
                .andExpect(content().string(containsString("Entre 1 y 100 ítems; cada ítem se puede pedir una sola vez.")))
                .andExpect(content().string(containsString("Agregar ítem")))
                .andExpect(content().string(containsString(">No hay ítems en la orden<")))
                .andExpect(content().string(containsString("<template data-plantilla=\"item\">")))
                .andExpect(content().string(containsString("Costo por presentación ($)")))
                .andExpect(content().string(containsString("Subtotal ($)")))
                .andExpect(content().string(containsString("Total ($):")))
                .andExpect(content().string(containsString("/js/orden-compra-form.js")))
                .andExpect(content().string(containsString("Registrar orden de compra")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/ordenes-compra\"[^>]*>\\s*<i[^>]*></i>\\s*<span>Volver a órdenes de compra.*")))
                .andExpect(content().string(not(containsString("No se pudo registrar"))))
                // Ventana de ítems: selector, cantidad, costo, sus avisos y sus placeholders
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/buscador/items\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione ítem\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 10\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 12500,50\"")))
                .andExpect(content().string(containsString("data-error=\"item\"")))
                .andExpect(content().string(containsString("data-error=\"duplicado\"")))
                .andExpect(content().string(containsString("data-error=\"cantidad\"")))
                .andExpect(content().string(containsString("data-error=\"costo\"")))
                .andReturn().getResponse().getContentAsString();

        // Assert: el orden de la página es planificación, proveedor, fecha, ítems
        assertThat(html.indexOf("name=\"idPlanificacionProduccion\"")).isPositive()
                .isLessThan(html.indexOf("name=\"idProveedor\""));
        assertThat(html.indexOf("name=\"idProveedor\"")).isLessThan(html.indexOf("id=\"fechaEntregaEstimada\""));
        assertThat(html.indexOf("id=\"fechaEntregaEstimada\"")).isLessThan(html.indexOf("data-detalle=\"item\""));
        // El asterisco rojo: cuatro obligatorios en el formulario (tres campos y los ítems) y tres en la ventana
        assertThat(html.split(Pattern.quote(OBLIGATORIO), -1)).hasSize(8);
        // La ventana para agregar ítems va fuera del formulario: sus campos no se envían
        assertThat(html.indexOf("id=\"dialogo-agregar-item\"")).isGreaterThan(html.indexOf("</form>", html.indexOf("<form id=\"orden-form\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-13: el formulario vacío trae los selectores habilitados, el aviso del bloqueo oculto y \"Agregar ítem\" deshabilitado con su ayuda, con todos los atributos que usa orden-compra-form.js")
    void caso_cp_occ_13() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String formulario = formularioDe(html);

        // Assert
        assertThat(formulario).doesNotContain("data-deshabilitado");
        assertThat(formulario).contains("data-seccion-datos", "data-selector-de=\"planificacion\"", "data-selector-de=\"proveedor\"",
                "data-detalle=\"item\"", "data-abrir-agregar=\"item\"", "data-ayuda-agregar", "data-tabla-items", "data-filas",
                "data-fila-vacia", "data-total", "data-quitar-fila");
        assertThat(formulario).containsPattern("<div data-aviso-bloqueo [^>]*hidden=\"hidden\">");
        assertThat(formulario).contains("Para cambiar la planificación o el proveedor, quite primero todos los ítems");
        assertThat(formulario).containsPattern("(?s)<button type=\"button\" data-abrir-agregar=\"item\"[^>]*disabled=\"disabled\"");
        assertThat(formulario).doesNotContainPattern("<p data-ayuda-agregar [^>]*hidden");
        assertThat(formulario).contains("Elija primero la planificación y el proveedor");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_PRODUCCION"})
    @DisplayName("CP-OCC-14: el ENCARGADO_DE_DEPOSITO, que solo consulta, y el GERENTE_DE_PRODUCCION no pueden abrir el formulario de alta")
    void caso_cp_occ_14(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo").with(user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /ordenes-compra ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-15: el registro correcto delega en el service con la fecha, la planificación, el proveedor y los ítems en orden, y redirige al detalle de la orden con alerta de éxito")
    void caso_cp_occ_15() throws Exception {
        // Arrange
        when(ordenCompraServicio.registrarOrdenCompra(any())).thenReturn(orden(9L, EstadoSolicitud.PENDIENTE));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra/9"))
                .andExpect(flash().attribute("mensaje", "Orden de compra registrada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<OrdenCompraFormDTO> captor = ArgumentCaptor.forClass(OrdenCompraFormDTO.class);
        verify(ordenCompraServicio).registrarOrdenCompra(captor.capture());
        OrdenCompraFormDTO form = captor.getValue();
        assertThat(form.getFechaEntregaEstimada()).isEqualTo(LocalDate.parse(FECHA));
        assertThat(form.getIdPlanificacionProduccion()).isEqualTo(7L);
        assertThat(form.getIdProveedor()).isEqualTo(4L);
        assertThat(form.getDetallesCompra()).hasSize(2);
        assertThat(form.getDetallesCompra().get(0).getIdCatalogoProveedor()).isEqualTo(100L);
        assertThat(form.getDetallesCompra().get(0).getCantidad()).isEqualTo(10);
        assertThat(form.getDetallesCompra().get(0).getCostoUnitario()).isEqualByComparingTo("1250.50");
        assertThat(form.getDetallesCompra().get(1).getIdCatalogoProveedor()).isEqualTo(102L);
        assertThat(form.getDetallesCompra().get(1).getCantidad()).isEqualTo(2);
        assertThat(form.getDetallesCompra().get(1).getCostoUnitario()).isEqualByComparingTo("0.1234");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-16: sin datos obligatorios ni ítems vuelve al formulario con cada mensaje, sin llamar al service")
    void caso_cp_occ_16() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("fechaEntregaEstimada", "").param("idPlanificacionProduccion", "").param("idProveedor", ""))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo registrar la orden de compra")))
                .andExpect(content().string(containsString("La fecha de entrega estimada es obligatoria")))
                .andExpect(content().string(containsString("La planificación de producción es obligatoria")))
                .andExpect(content().string(containsString("El proveedor es obligatorio")))
                .andExpect(content().string(containsString("La orden de compra debe tener al menos un ítem")))
                .andExpect(content().string(matchesPattern("(?s).*data-selector-buscar data-selector-texto[^>]*border-rose-400.*")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"fechaEntregaEstimada\"[^>]*border-rose-400.*")))
                .andExpect(content().string(matchesPattern("(?s).*class=\"border rounded-lg overflow-hidden border-rose-400\".*")));

        verify(ordenCompraServicio, never()).registrarOrdenCompra(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-17: una fecha que no se puede convertir vuelve al formulario con el mensaje de fecha inválida junto al campo, sin llamar al service")
    void caso_cp_occ_17() throws Exception {
        // Arrange
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("fechaEntregaEstimada", "no-es-fecha");
        cargarProveedorYPlanificacion();

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("El valor ingresado no es una fecha válida")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"fechaEntregaEstimada\"[^>]*border-rose-400.*")));

        verify(ordenCompraServicio, never()).registrarOrdenCompra(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-18: cada ítem con cantidad, costo o ítem del catálogo inválido muestra su mensaje y resalta su fila (solo las filas con error), sin llamar al service")
    void caso_cp_occ_18() throws Exception {
        // Arrange: la fila 0 con cantidad 0, la 1 con costo 0, la 2 con 5 decimales, la 3 sin ítem del catálogo y la 4 válida
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("detallesCompra[0].cantidad", "0");
        datos.set("detallesCompra[1].costoUnitario", "0");
        datos.add("detallesCompra[2].idCatalogoProveedor", "104");
        datos.add("detallesCompra[2].cantidad", "3");
        datos.add("detallesCompra[2].costoUnitario", "1.23456");
        datos.add("detallesCompra[3].cantidad", "3");
        datos.add("detallesCompra[3].costoUnitario", "5");
        datos.add("detallesCompra[4].idCatalogoProveedor", "101");
        datos.add("detallesCompra[4].cantidad", "1");
        datos.add("detallesCompra[4].costoUnitario", "5");
        cargarProveedorYPlanificacion();

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("La cantidad de cada ítem debe ser mayor a cero")))
                .andExpect(content().string(containsString("El costo unitario de cada ítem debe ser mayor a cero")))
                .andExpect(content().string(containsString("El costo unitario de cada ítem no puede tener más de 4 decimales")))
                .andExpect(content().string(containsString("El ítem del catálogo de cada ítem de la orden de compra es obligatorio")))
                .andReturn().getResponse().getContentAsString();

        // Assert: cuatro filas resaltadas; la fila válida y la plantilla, no
        assertThat(html.split(Pattern.quote(FILA_RESALTADA), -1)).hasSize(5);
        assertThat(html.split(Pattern.quote(FILA_SIN_RESALTAR), -1)).hasSize(3);
        verify(ordenCompraServicio, never()).registrarOrdenCompra(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-19: una cantidad o un costo por encima del máximo vuelve al formulario con el mensaje del tope, sin llamar al service")
    void caso_cp_occ_19() throws Exception {
        // Arrange
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("detallesCompra[0].cantidad", "1000001");
        datos.set("detallesCompra[1].costoUnitario", "1000000000.01");
        cargarProveedorYPlanificacion();

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La cantidad de cada ítem no puede superar 1.000.000")))
                .andExpect(content().string(containsString("El costo unitario de cada ítem no puede superar 1.000.000.000")));

        verify(ordenCompraServicio, never()).registrarOrdenCompra(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-20: índices de ítems con un hueco ([0] y [2]) enlazan la posición faltante como un ítem vacío: vuelve al formulario con esa fila resaltada y numerada sin hueco, sin llamar al service")
    void caso_cp_occ_20() throws Exception {
        // Arrange: sin la fila [1]
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.remove("detallesCompra[1].idCatalogoProveedor");
        datos.remove("detallesCompra[1].cantidad");
        datos.remove("detallesCompra[1].costoUnitario");
        datos.add("detallesCompra[2].idCatalogoProveedor", "104");
        datos.add("detallesCompra[2].cantidad", "4");
        datos.add("detallesCompra[2].costoUnitario", "7");
        cargarProveedorYPlanificacion();

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo registrar la orden de compra")))
                .andReturn().getResponse().getContentAsString();

        // Assert: tres filas renumeradas sin hueco; la del medio, vacía y resaltada
        assertThat(html).contains("name=\"detallesCompra[0].idCatalogoProveedor\" value=\"100\"")
                .contains("name=\"detallesCompra[1].idCatalogoProveedor\" value=\"\"")
                .contains("name=\"detallesCompra[2].idCatalogoProveedor\" value=\"104\"")
                .contains(">Ítem no disponible<");
        assertThat(html.split(Pattern.quote(FILA_RESALTADA), -1)).hasSize(2);
        assertThat(html.indexOf(FILA_RESALTADA)).isGreaterThan(html.indexOf("name=\"detallesCompra[0].idCatalogoProveedor\""))
                .isLessThan(html.indexOf("name=\"detallesCompra[1].idCatalogoProveedor\""));
        verify(ordenCompraServicio, never()).registrarOrdenCompra(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-21: una regla de negocio rechazada por el service se muestra como error general y conserva lo tipeado, la planificación, el proveedor, las filas con sus datos y el total, con los selectores bloqueados")
    void caso_cp_occ_21() throws Exception {
        // Arrange
        cargarProveedorYPlanificacion();
        when(ordenCompraServicio.registrarOrdenCompra(any()))
                .thenThrow(new ReglaNegocioException("La fecha de entrega estimada no puede ser anterior a la fecha actual"));

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo registrar la orden de compra")))
                .andExpect(content().string(containsString("La fecha de entrega estimada no puede ser anterior a la fecha actual")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"fechaEntregaEstimada\"[^>]*value=\"" + FECHA + "\".*")))
                .andExpect(content().string(containsString("value=\"Plan #7 - Pale Ale\"")))
                .andExpect(content().string(containsString("value=\"Malterías del Sur S.A. (30-71234567-8)\"")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"idPlanificacionProduccion\" value=\"7\".*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"idProveedor\" value=\"4\".*")))
                // Las dos filas vuelven con sus ids, cantidad y costo (ocultos) y sus textos, subtotales y total
                .andExpect(content().string(containsString("name=\"detallesCompra[0].idCatalogoProveedor\" value=\"100\"")))
                .andExpect(content().string(containsString("name=\"detallesCompra[0].cantidad\" value=\"10\"")))
                .andExpect(content().string(containsString("name=\"detallesCompra[0].costoUnitario\" value=\"1250.50\"")))
                .andExpect(content().string(containsString("name=\"detallesCompra[1].idCatalogoProveedor\" value=\"102\"")))
                .andExpect(content().string(containsString("name=\"detallesCompra[1].costoUnitario\" value=\"0.1234\"")))
                .andExpect(content().string(containsString(">Pilsen<")))
                .andExpect(content().string(containsString(">Bolsa de 25 Kg<")))
                .andExpect(content().string(containsString(">25 kg<")))
                .andExpect(content().string(containsString(">Cascade<")))
                .andExpect(content().string(containsString(">1.250,50<")))
                .andExpect(content().string(containsString(">12.505,00<")))
                .andExpect(content().string(containsString(">0,1234<")))
                .andExpect(content().string(containsString(">0,2468<")))
                .andExpect(content().string(containsString("data-total>12.505,2468<")))
                .andExpect(content().string(matchesPattern("(?s).*<tr data-fila-vacia hidden=\"hidden\">.*")))
                .andReturn().getResponse().getContentAsString();

        // Assert: con filas, la planificación y el proveedor quedan bloqueados, el aviso visible y "Agregar ítem" habilitado
        String formulario = formularioDe(html);
        assertThat(formulario.split("data-deshabilitado=\"true\"", -1)).hasSize(3);
        assertThat(formulario).containsPattern("data-selector-de=\"planificacion\"[^>]*class=\"opacity-50\"")
                .containsPattern("data-selector-de=\"proveedor\"[^>]*class=\"opacity-50\"")
                .doesNotContainPattern("<div data-aviso-bloqueo [^>]*hidden")
                .doesNotContainPattern("(?s)data-abrir-agregar=\"item\"[^>]*disabled=\"disabled\"")
                .containsPattern("<p data-ayuda-agregar [^>]*hidden=\"hidden\"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-22: si el service no encuentra la planificación o el proveedor lo informa como error general y las filas muestran el ítem como no disponible")
    void caso_cp_occ_22() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());
        when(proveedorServicio.buscarPorId(4L)).thenThrow(new RecursoNoEncontradoException("No se encontró el proveedor con ID: 4"));
        when(ordenCompraServicio.registrarOrdenCompra(any())).thenThrow(new RecursoNoEncontradoException("No se encontró el proveedor con ID: 4"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo registrar la orden de compra")))
                .andExpect(content().string(containsString("No se encontró el proveedor con ID: 4")))
                .andExpect(content().string(containsString("value=\"Plan #7 - Pale Ale\"")))
                .andExpect(content().string(containsString(">Ítem no disponible<")))
                .andExpect(content().string(containsString("name=\"detallesCompra[0].idCatalogoProveedor\" value=\"100\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-53: el listado ofrece al GERENTE_DE_COMPRAS Finalizar y Anular en las órdenes pendientes, igual que el listado de planificaciones, con sus diálogos de motivo")
    void caso_cp_occ_53() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orden(9L, EstadoSolicitud.PENDIENTE), orden(10L, EstadoSolicitud.FINALIZADA)), PageRequest.of(0, 20), 2));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-motivo-url=\"/ordenes-compra/9/finalizar\"", "data-motivo-url=\"/ordenes-compra/9/anular\"",
                "data-motivo-nombre=\"Orden de compra #9\"", "title=\"Finalizar orden de compra\"", "title=\"Anular orden de compra\"",
                "bi-check2-circle", "bi-x-circle");
        assertThat(html).doesNotContain("/ordenes-compra/10/finalizar").doesNotContain("/ordenes-compra/10/anular");
        assertThat(html).containsPattern("(?s)<dialog id=\"dialogo-finalizar\".*name=\"_csrf\".*</dialog>");
        assertThat(html).containsPattern("(?s)<dialog id=\"dialogo-anular\".*name=\"_csrf\".*</dialog>");
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-OCC-54: el listado del ENCARGADO_DE_DEPOSITO solo ofrece el ojo: ni Finalizar ni Anular ni sus diálogos")
    void caso_cp_occ_54() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(orden(9L, EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/ordenes-compra/9\"")))
                .andExpect(content().string(not(containsString("data-motivo-url"))))
                .andExpect(content().string(not(containsString("id=\"dialogo-finalizar\""))))
                .andExpect(content().string(not(containsString("id=\"dialogo-anular\""))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-55: el detalle muestra el avance de la recepción (recibido sobre pedido, porcentaje y barra) y los contadores de ítems, pedidas, recibidas y pendientes")
    void caso_cp_occ_55() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(ordenConDetalle(EstadoSolicitud.PENDIENTE));

        // Act
        String html = mockMvc.perform(get(URL + "/9")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        int inicio = html.indexOf("data-progreso-recepcion");
        String avance = html.substring(inicio, inicio + 1500);
        assertThat(avance).contains("Recibido:", "de 12 unidades", "(33%)", "Faltan 8", "aria-valuenow=\"33\"", "width: 33%", "bg-amber-500");
        assertThat(html).contains("data-contador=\"items\">2<", "data-contador=\"pedidas\">12<", "data-contador=\"recibidas\">4<", "data-contador=\"pendientes\">8<");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-56: con la recepción completa la barra queda verde en 100% y dice \"Recepción completa\"")
    void caso_cp_occ_56() throws Exception {
        // Arrange
        OrdenCompraResponseDTO base = ordenConDetalle(EstadoSolicitud.FINALIZADA);
        OrdenCompraResponseDTO completa = OrdenCompraResponseDTO.builder().id(base.getId()).fechaEntregaEstimada(base.getFechaEntregaEstimada())
                .estado(base.getEstado()).planificacionProduccion(base.getPlanificacionProduccion()).proveedor(base.getProveedor())
                .detallesCompra(base.getDetallesCompra()).total(base.getTotal())
                .totalItems(2).totalPedidas(12).totalRecibidas(12.0).totalPendientes(0.0).porcentajeRecibido(100).build();
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(completa);

        // Act
        String html = mockMvc.perform(get(URL + "/9")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        int inicio = html.indexOf("data-progreso-recepcion");
        String avance = html.substring(inicio, inicio + 1500);
        assertThat(avance).contains("(100%)", "Recepción completa", "width: 100%", "bg-emerald-500").doesNotContain("Faltan");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-57: el formulario con idPlanificacion llega con la planificación elegida (campo oculto y texto del selector) y sin avisos")
    void caso_cp_occ_57() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());

        // Act
        String html = mockMvc.perform(get(URL + "/nuevo").param("idPlanificacion", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).containsPattern("name=\"idPlanificacionProduccion\"[^>]*value=\"7\"|value=\"7\"[^>]*name=\"idPlanificacionProduccion\"");
        assertThat(html).contains("Plan #7 - Pale Ale").doesNotContain("La planificación indicada");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-58: un idPlanificacion que no es un número o que no existe muestra un aviso y deja el selector vacío")
    void caso_cp_occ_58() throws Exception {
        // Arrange
        when(planificacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la planificación con ID: 99"));

        // Act
        String invalida = mockMvc.perform(get(URL + "/nuevo").param("idPlanificacion", "abc"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String inexistente = mockMvc.perform(get(URL + "/nuevo").param("idPlanificacion", "99"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(invalida).contains("La planificación indicada no es válida");
        assertThat(inexistente).contains("La planificación indicada no existe");
        assertThat(invalida + inexistente).doesNotContain("Plan #7 - Pale Ale");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-59: el aviso de que para cambiar la planificación o el proveedor hay que quitar los ítems es un texto pequeño y atenuado, sin banner")
    void caso_cp_occ_59() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int inicio = html.indexOf("data-aviso-bloqueo");
        String aviso = html.substring(inicio, inicio + 600);

        // Assert
        assertThat(aviso).contains("text-xs", "text-slate-400", "Para cambiar la planificación o el proveedor, quite primero todos los ítems")
                .doesNotContain("bg-amber-50").doesNotContain("border-amber-200").doesNotContain("text-amber-800");
    }

    // ==================== GET /ordenes-compra/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-23: el detalle de una orden pendiente muestra el estado, la planificación, el proveedor con su CUIT, la fecha, el total, los ítems con recibida, pendiente, costo y subtotal, y las acciones Finalizar y Anular con sus diálogos")
    void caso_cp_occ_23() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(ordenConDetalle(EstadoSolicitud.PENDIENTE));

        // Act
        String html = mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isOk())
                .andExpect(view().name("orden_compra/orden-detalle"))
                .andExpect(content().string(containsString("<title>Órdenes de compra - Maestro Cervecero</title>")))
                .andExpect(content().string(containsString("Orden de compra #9")))
                .andExpect(content().string(containsString(">Pendiente<")))
                .andExpect(content().string(containsString(">Plan #7 - Pale Ale<")))
                .andExpect(content().string(containsString(">Malterías del Sur S.A.<")))
                .andExpect(content().string(containsString("CUIT 30-71234567-8")))
                .andExpect(content().string(containsString(">15/07/2027<")))
                .andExpect(content().string(containsString("data-total>12.505,2468<")))
                .andExpect(content().string(containsString("Total ($)")))
                // Columnas de los ítems, con la unidad en el encabezado
                .andExpect(content().string(containsString(">Pedida<")))
                .andExpect(content().string(containsString(">Costo ($)<")))
                .andExpect(content().string(containsString("Subtotal ($)")))
                .andExpect(content().string(containsString("data-tooltip=\"Unidades ya recibidas mediante ingresos registrados; debajo, su equivalente en la unidad del insumo\"")))
                .andExpect(content().string(containsString("data-tooltip=\"Unidades que restan recibir; debajo, su equivalente en la unidad del insumo\"")))
                // Acciones y diálogos de cierre, con el token CSRF en cada formulario
                .andExpect(content().string(containsString("data-motivo-url=\"/ordenes-compra/9/finalizar\"")))
                .andExpect(content().string(containsString("data-motivo-url=\"/ordenes-compra/9/anular\"")))
                .andExpect(content().string(containsString("data-motivo-nombre=\"Orden de compra #9\"")))
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-finalizar\".*name=\"_csrf\".*</dialog>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-anular\".*name=\"_csrf\".*</dialog>.*")))
                .andExpect(content().string(containsString("no se podrá reabrir")))
                .andExpect(content().string(containsString("Solo procede si ya tiene un ingreso de insumo registrado")))
                .andExpect(content().string(containsString("Requiere que no tenga ingresos de insumo registrados")))
                .andExpect(content().string(containsString("El motivo admite hasta 255 caracteres")))
                .andExpect(content().string(containsString("placeholder=\"Ej: El proveedor no puede entregar el resto de los ítems\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: La orden se cargó con un proveedor equivocado\"")))
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)))
                .andReturn().getResponse().getContentAsString();

        // Assert: las cantidades recibida y pendiente van en unidades con su equivalente en la unidad del insumo, y los importes con el formato de la aplicación
        String filas = html.substring(html.indexOf("<tbody"), html.indexOf("</tbody>"));
        assertThat(filas).contains(">100 kg<", ">150 kg<", ">0 g<", ">2.000 g<", ">10<", ">4<", ">6<", ">0<", ">2<", ">1.250,50<", ">12.505,00<", ">0,1234<", ">0,2468<");
        assertThat(filas.indexOf("Pilsen")).isLessThan(filas.indexOf("Cascade"));
        assertThat(filas).contains(">25 kg<", ">1 kg<");
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-OCC-24: el ENCARGADO_DE_DEPOSITO ve el detalle completo pero no las acciones Finalizar y Anular ni sus diálogos")
    void caso_cp_occ_24() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(ordenConDetalle(EstadoSolicitud.PENDIENTE));

        // Act + Assert
        mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Orden de compra #9")))
                .andExpect(content().string(containsString("data-total>12.505,2468<")))
                .andExpect(content().string(not(containsString("data-motivo-url"))))
                .andExpect(content().string(not(containsString("id=\"dialogo-finalizar\""))))
                .andExpect(content().string(not(containsString("id=\"dialogo-anular\""))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-25: una orden finalizada automáticamente (sin motivo) indica \"Finalizada automáticamente\" en el diálogo de detalle y no ofrece Finalizar ni Anular")
    void caso_cp_occ_25() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(conCierre(ordenConDetalle(EstadoSolicitud.FINALIZADA), null, LocalDateTime.of(2027, 7, 10, 9, 30), null, null));

        // Act + Assert
        mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Finalizada<")))
                .andExpect(content().string(containsString("data-detalle-titulo=\"" + TITULO_AUTOMATICA + "\"")))
                .andExpect(content().string(not(containsString("data-detalle-motivo="))))
                .andExpect(content().string(containsString("data-detalle-usuario=\"gerente.compras\"")))
                .andExpect(content().string(containsString("data-detalle-fecha=\"10/07/2027 09:30\"")))
                .andExpect(content().string(not(containsString("data-motivo-url"))))
                .andExpect(content().string(not(containsString("id=\"dialogo-finalizar\""))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-26: una orden finalizada de forma forzada muestra su motivo y su fecha en el diálogo de detalle, no en línea")
    void caso_cp_occ_26() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(conCierre(ordenConDetalle(EstadoSolicitud.FINALIZADA),
                "El proveedor discontinuó el producto", LocalDateTime.of(2027, 7, 12, 16, 5), null, null));

        // Act
        String html = mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-detalle-motivo=\"El proveedor discontinuó el producto\"")))
                .andExpect(content().string(containsString("data-detalle-titulo=\"Detalle de la finalización\"")))
                .andExpect(content().string(containsString("data-detalle-usuario=\"gerente.compras\"")))
                .andExpect(content().string(containsString("data-detalle-fecha=\"12/07/2027 16:05\"")))
                .andExpect(content().string(not(containsString(TITULO_AUTOMATICA))))
                .andReturn().getResponse().getContentAsString();

        // Assert: el motivo solo figura dentro del atributo del botón que abre el diálogo
        assertThat(html.split(Pattern.quote("El proveedor discontinuó el producto"), -1)).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-27: una orden anulada muestra su motivo y su fecha de anulación en el diálogo de detalle y no ofrece Finalizar ni Anular")
    void caso_cp_occ_27() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(conCierre(ordenConDetalle(EstadoSolicitud.ANULADA),
                null, null, "La orden se cargó con un proveedor equivocado", LocalDateTime.of(2027, 7, 1, 11, 0)));

        // Act + Assert
        mockMvc.perform(get(URL + "/9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Anulada<")))
                .andExpect(content().string(containsString("data-detalle-titulo=\"Detalle de la anulación\"")))
                .andExpect(content().string(containsString("data-detalle-motivo=\"La orden se cargó con un proveedor equivocado\"")))
                .andExpect(content().string(containsString("data-detalle-usuario=\"gerente.compras\"")))
                .andExpect(content().string(containsString("data-detalle-fecha=\"01/07/2027 11:00\"")))
                .andExpect(content().string(not(containsString("data-motivo-url"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-28: una orden inexistente redirige al origen con el motivo")
    void caso_cp_occ_28() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/99").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró la orden de compra con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-29: un id que no es un número no es un error del sistema")
    void caso_cp_occ_29() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/abc").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));

        verify(ordenCompraServicio, never()).buscarPorId(any());
    }

    // ==================== POST /ordenes-compra/{id}/anular ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-30: anular delega en el service con el motivo y redirige al detalle de la orden con alerta de éxito")
    void caso_cp_occ_30() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/9/anular").with(csrf()).param("motivo", "La orden se cargó con un proveedor equivocado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra/9"))
                .andExpect(flash().attribute("mensaje", "Orden de compra anulada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<AnulacionOrdenCompraFormDTO> captor = ArgumentCaptor.forClass(AnulacionOrdenCompraFormDTO.class);
        verify(ordenCompraServicio).anularOrdenCompra(eq(9L), captor.capture());
        assertThat(captor.getValue().getMotivoAnulacion()).isEqualTo("La orden se cargó con un proveedor equivocado");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-31: si el service rechaza la anulación (sin motivo, orden no pendiente o con ingresos registrados) vuelve al detalle con la alerta de advertencia y no redirige como éxito")
    void caso_cp_occ_31() throws Exception {
        // Arrange
        doThrow(new ReglaNegocioException("No se puede anular la orden de compra porque tiene al menos un ingreso de insumo en estado Registrado asociado"))
                .when(ordenCompraServicio).anularOrdenCompra(eq(9L), any());

        // Act + Assert
        mockMvc.perform(post(URL + "/9/anular").with(csrf()).header("Referer", URL + "/9").param("motivo", "Error de carga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra/9"))
                .andExpect(flash().attribute("mensaje", "No se puede anular la orden de compra porque tiene al menos un ingreso de insumo en estado Registrado asociado"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-32: anular sin el campo motivo lo entrega al service como null para que sea él quien lo rechace, y una orden inexistente vuelve al origen con el motivo")
    void caso_cp_occ_32() throws Exception {
        // Arrange
        doThrow(new ReglaNegocioException("El motivo de anulación es obligatorio")).when(ordenCompraServicio).anularOrdenCompra(eq(9L), any());
        doThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99")).when(ordenCompraServicio).anularOrdenCompra(eq(99L), any());

        // Act + Assert
        mockMvc.perform(post(URL + "/9/anular").with(csrf()).header("Referer", URL + "/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El motivo de anulación es obligatorio"));
        mockMvc.perform(post(URL + "/99/anular").with(csrf()).header("Referer", URL).param("motivo", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró la orden de compra con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));

        ArgumentCaptor<AnulacionOrdenCompraFormDTO> captor = ArgumentCaptor.forClass(AnulacionOrdenCompraFormDTO.class);
        verify(ordenCompraServicio).anularOrdenCompra(eq(9L), captor.capture());
        assertThat(captor.getValue().getMotivoAnulacion()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_PRODUCCION", "ADMINISTRADOR"})
    @DisplayName("CP-OCC-33: un rol distinto de GERENTE_DE_COMPRAS, incluido el ENCARGADO_DE_DEPOSITO, no puede anular ni finalizar una orden")
    void caso_cp_occ_33(String rol) throws Exception {
        // Act + Assert
        for (String accion : List.of("anular", "finalizar")) {
            mockMvc.perform(post(URL + "/9/" + accion).with(user("usuario").roles(rol)).with(csrf()).param("motivo", "x"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }

        verifyNoInteractions(ordenCompraServicio);
    }

    // ==================== POST /ordenes-compra/{id}/finalizar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-34: finalizar delega en el service con el motivo y redirige al detalle de la orden con alerta de éxito")
    void caso_cp_occ_34() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/9/finalizar").with(csrf()).param("motivo", "El proveedor discontinuó el producto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra/9"))
                .andExpect(flash().attribute("mensaje", "Orden de compra finalizada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<FinalizacionForzadaOrdenCompraFormDTO> captor = ArgumentCaptor.forClass(FinalizacionForzadaOrdenCompraFormDTO.class);
        verify(ordenCompraServicio).finalizarOrdenCompra(eq(9L), captor.capture());
        assertThat(captor.getValue().getMotivoFinalizacion()).isEqualTo("El proveedor discontinuó el producto");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-35: si el service rechaza la finalización (sin ingresos registrados, orden no pendiente) vuelve al detalle con la alerta de advertencia")
    void caso_cp_occ_35() throws Exception {
        // Arrange
        doThrow(new ReglaNegocioException("No se puede finalizar la orden de compra porque no tiene ningún ingreso de insumo en estado Registrado asociado: corresponde anularla en su lugar"))
                .when(ordenCompraServicio).finalizarOrdenCompra(eq(9L), any());

        // Act + Assert
        mockMvc.perform(post(URL + "/9/finalizar").with(csrf()).header("Referer", URL + "/9").param("motivo", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ordenes-compra/9"))
                .andExpect(flash().attribute("mensaje", "No se puede finalizar la orden de compra porque no tiene ningún ingreso de insumo en estado Registrado asociado: corresponde anularla en su lugar"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-36: finalizar una orden inexistente vuelve al origen con el motivo")
    void caso_cp_occ_36() throws Exception {
        // Arrange
        doThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99")).when(ordenCompraServicio).finalizarOrdenCompra(eq(99L), any());

        // Act + Assert
        mockMvc.perform(post(URL + "/99/finalizar").with(csrf()).header("Referer", URL).param("motivo", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró la orden de compra con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== Buscadores de planificaciones y de recetas ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-37: el buscador de planificaciones del formulario ofrece solo las pendientes, de a 8 y con la última registrada arriba, con el selector de receta de las recetas activas")
    void caso_cp_occ_37() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion()), PageRequest.of(0, 8), 1));
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Pale Ale", Estado.ACTIVO));

        // Act
        mockMvc.perform(get(URL + "/buscador/planificaciones").param("idReceta", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Filtre y elija la planificación pendiente de la compra.")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/buscador/recetas\"")))
                .andExpect(content().string(containsString("value=\"Pale Ale\"")))
                .andExpect(content().string(containsString("action=\"/ordenes-compra/buscador/planificaciones\"")))
                .andExpect(content().string(containsString("data-id=\"7\" data-texto=\"Plan #7 - Pale Ale\"")))
                .andExpect(content().string(containsString("<table class=\"tabla-buscador\">")));

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesProduccion(eq(3L), eq(EstadoSolicitud.PENDIENTE), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("id")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-OCC-38: el buscador de planificaciones del filtro del listado lo usan ambos roles y ofrece planificaciones de cualquier estado, con las recetas dadas de baja que tienen planificaciones")
    void caso_cp_occ_38() throws Exception {
        // Arrange
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion()), PageRequest.of(0, 8), 1));
        when(recetaServicio.buscarActivaODadaDeBajaConPlanificacionesPorId(3L)).thenReturn(receta(3L, "Stout", Estado.BAJA));
        when(recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(3L, "Stout", Estado.BAJA)), PageRequest.of(0, 8), 1));

        // Act + Assert
        mockMvc.perform(get(URL + "/filtros/planificaciones").param("idReceta", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Filtre y elija la planificación de las órdenes de compra.")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ordenes-compra/filtros/recetas\"")))
                .andExpect(content().string(containsString("value=\"Stout (dada de baja)\"")));
        mockMvc.perform(get(URL + "/filtros/recetas").param("incluirDadosDeBaja", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stout")));

        verify(planificacionServicio).filtrarPlanificacionesProduccion(eq(3L), isNull(), isNull(), any());
        verify(recetaServicio).filtrarRecetasConDadasDeBajaConPlanificaciones(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-39: el buscador de recetas del formulario solo ofrece recetas activas y el del filtro, sin dadas de baja, también")
    void caso_cp_occ_39() throws Exception {
        // Arrange
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(3L, "Pale Ale", Estado.ACTIVO)), PageRequest.of(0, 8), 1));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/recetas").param("nombre", " pale "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pale Ale")));
        mockMvc.perform(get(URL + "/filtros/recetas"))
                .andExpect(status().isOk());

        verify(recetaServicio).filtrarRecetas(eq("pale"), any());
        verify(recetaServicio).filtrarRecetas(isNull(), any());
        verify(recetaServicio, never()).filtrarRecetasConDadasDeBajaConPlanificaciones(any(), any());
    }

    // ==================== Buscador de proveedores ====================

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO"})
    @DisplayName("CP-OCC-40: el buscador de proveedores lo usan ambos roles, filtra por razón social y CUIT recortados y muestra el CUIT con guiones también en el texto que elige")
    void caso_cp_occ_40(String rol) throws Exception {
        // Arrange
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(proveedor()), PageRequest.of(1, 8), 20));

        // Act
        mockMvc.perform(get(URL + "/buscador/proveedores").with(user("usuario").roles(rol))
                        .param("razonSocial", " malterías ").param("cuit", " 30712345678 ").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<table class=\"tabla-buscador\">")))
                .andExpect(content().string(containsString("action=\"/ordenes-compra/buscador/proveedores\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Malterías del Sur S.A.\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 30-71234567-8\"")))
                .andExpect(content().string(containsString(">Malterías del Sur S.A.<")))
                .andExpect(content().string(containsString(">30-71234567-8<")))
                .andExpect(content().string(containsString("data-id=\"4\"")))
                .andExpect(content().string(containsString("data-texto=\"Malterías del Sur S.A. (30-71234567-8)\"")))
                .andExpect(content().string(containsString("/ordenes-compra/buscador/proveedores?pagina=0&amp;razonSocial=malter%C3%ADas&amp;cuit=30712345678")));

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proveedorServicio).filtrarProveedores(eq("malterías"), isNull(), eq("30712345678"), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.unsorted());
    }

    // ==================== Buscador de ítems ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-41: el buscador de ítems ofrece solo los ítems del catálogo del proveedor cuyo insumo usa la receta de la planificación (malta, lúpulo y levadura), en orden alfabético, y deja afuera el insumo que la receta no usa")
    void caso_cp_occ_41() throws Exception {
        // Arrange
        cargarProveedorYReceta();

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<table class=\"tabla-buscador\">")))
                .andExpect(content().string(containsString("action=\"/ordenes-compra/buscador/items\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pilsen\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Bolsa de 25 Kg\"")))
                .andExpect(content().string(containsString("name=\"idProveedor\" value=\"4\"")))
                .andExpect(content().string(containsString("name=\"idPlanificacion\" value=\"7\"")))
                .andReturn().getResponse().getContentAsString();

        // Assert: Cascade, Pilsen (1.000 g antes que 25 Kg) y Safale; no Caramelo
        assertThat(html).contains("data-id=\"102\"", "data-id=\"101\"", "data-id=\"100\"", "data-id=\"104\"")
                .doesNotContain("data-id=\"103\"").doesNotContain("Caramelo");
        assertThat(html.indexOf("data-id=\"102\"")).isLessThan(html.indexOf("data-id=\"101\""));
        assertThat(html.indexOf("data-id=\"101\"")).isLessThan(html.indexOf("data-id=\"100\""));
        assertThat(html.indexOf("data-id=\"100\"")).isLessThan(html.indexOf("data-id=\"104\""));
        // Los datos que la fila de la orden necesita viajan en el botón Elegir
        assertThat(html).contains("data-texto=\"Pilsen - Bolsa de 25 Kg\"", "data-insumo=\"Pilsen\"", "data-presentacion=\"Bolsa de 25 Kg\"",
                "data-medida=\"25 kg\"", "data-medida=\"1.000 g\"", "data-medida=\"11,5 g\"");
        assertThat(html).contains("shrink-0");
        verify(planificacionServicio).buscarVersionRecetaDePlanificacion(7L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-42: el buscador de ítems deja afuera los ya agregados a la orden, filtra por insumo y presentación sin distinguir mayúsculas y conserva el contexto al paginar")
    void caso_cp_occ_42() throws Exception {
        // Arrange
        cargarProveedorYReceta();

        // Act
        String sinAgregados = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7")
                        .param("idsAgregados", "100,102"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String filtrado = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7")
                        .param("insumo", " PILS ").param("presentacion", "1.000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String sinCoincidencias = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7")
                        .param("insumo", "caramelo"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(sinAgregados).doesNotContain("data-id=\"100\"", "data-id=\"102\"").contains("data-id=\"101\"", "data-id=\"104\"")
                .contains("name=\"idsAgregados\" value=\"100,102\"");
        assertThat(filtrado).contains("data-id=\"101\"").doesNotContain("data-id=\"100\"", "data-id=\"102\"", "data-id=\"104\"")
                .contains("name=\"insumo\" value=\"PILS\"");
        assertThat(sinCoincidencias).contains("No se encontraron ítems del proveedor que use la receta de la planificación con los criterios indicados")
                .doesNotContain("data-id=");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-43: el buscador de ítems pagina de a 8 con los links que conservan el proveedor, la planificación, los ítems agregados y los criterios")
    void caso_cp_occ_43() throws Exception {
        // Arrange: 10 ítems del mismo insumo de la receta
        List<CatalogoProveedorResponseDTO> items = new java.util.ArrayList<>();
        for (long i = 0; i < 10; i++) {
            items.add(item(200 + i, malta(11L, "Pilsen"), presentacion(50 + i, "Bolsa " + (char) ('A' + i), 25.0, UnidadDeMedida.KILOGRAMO)));
        }
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedorConCatalogo(items));
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacion());

        // Act
        String primera = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7")
                        .param("idsAgregados", "100").param("insumo", "pil"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String segunda = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7").param("pagina", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(primera.split("data-elegir", -1)).hasSize(9);
        assertThat(primera).contains("/ordenes-compra/buscador/items?pagina=1&amp;idProveedor=4&amp;idPlanificacion=7&amp;idsAgregados=100&amp;insumo=pil");
        assertThat(segunda.split("data-elegir", -1)).hasSize(3);
        assertThat(segunda).contains("data-id=\"208\"", "data-id=\"209\"")
                .contains("/ordenes-compra/buscador/items?pagina=0&amp;idProveedor=4&amp;idPlanificacion=7");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-44: sin planificación o sin proveedor el buscador de ítems responde 422 con el motivo como texto, para que el modal lo muestre")
    void caso_cp_occ_44() throws Exception {
        // Act + Assert
        for (String ruta : List.of(URL + "/buscador/items", URL + "/buscador/items?idProveedor=4", URL + "/buscador/items?idPlanificacion=7")) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().string("Elija primero la planificación de producción y el proveedor"));
        }

        verifyNoInteractions(proveedorServicio, planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-45: si el proveedor o la planificación del buscador de ítems no existen, responde 404 con el motivo como texto")
    void caso_cp_occ_45() throws Exception {
        // Arrange
        when(proveedorServicio.buscarPorId(98L)).thenThrow(new RecursoNoEncontradoException("No se encontró el proveedor con ID: 98"));
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor());
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "98").param("idPlanificacion", "7"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No se encontró el proveedor con ID: 98"));
        mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No se encontró la planificación de producción con ID: 99"));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-OCC-46: el ENCARGADO_DE_DEPOSITO no puede usar los buscadores del formulario de alta (planificaciones pendientes, recetas activas e ítems)")
    void caso_cp_occ_46() throws Exception {
        // Act + Assert
        for (String ruta : List.of(URL + "/buscador/planificaciones", URL + "/buscador/recetas", URL + "/buscador/items?idProveedor=4&idPlanificacion=7")) {
            mockMvc.perform(get(ruta))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }

        verifyNoInteractions(planificacionServicio, proveedorServicio, recetaServicio);
    }

    // ==================== Sidebar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-47: la sidebar del GERENTE_DE_COMPRAS ofrece \"Órdenes de Compra\" como enlace real en Operación, después de Planificación de Producción, sin resaltar en el panel de control")
    void caso_cp_occ_47() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Operación.*href=\"/planificaciones-produccion\".*href=\"/ordenes-compra\"[^>]*>\\s*<i class=\"bi text-lg bi-cart3\"></i>\\s*<span>Órdenes de Compra</span>.*Administración.*")))
                .andExpect(content().string(not(matchesPattern(ENLACE_RESALTADO))))
                .andExpect(content().string(not(containsString("Próximamente"))));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-OCC-48: la sidebar del ENCARGADO_DE_DEPOSITO muestra la sección Operación con \"Órdenes de Compra\" y ninguna otra opción de esa sección")
    void caso_cp_occ_48() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Operación.*href=\"/ordenes-compra\".*<span>Órdenes de Compra</span>.*")))
                .andExpect(content().string(not(containsString("Planificación de Producción"))))
                .andExpect(content().string(not(containsString("Gestión de Lotes"))))
                .andExpect(content().string(not(containsString("Cronograma de Producción"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "ADMINISTRADOR", "GERENTE_COMERCIAL"})
    @DisplayName("CP-OCC-49: la sidebar de los demás roles no muestra \"Órdenes de Compra\"")
    void caso_cp_occ_49(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/ordenes-compra\""))))
                .andExpect(content().string(not(containsString("Órdenes de Compra</span>"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-50: al volver al formulario con errores, el total suma solo las filas con cantidad y costo válidos e ignora las demás")
    void caso_cp_occ_50() throws Exception {
        // Arrange: la fila 0 con cantidad 0 y la 1 válida (2 x 0.1234)
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("detallesCompra[0].cantidad", "0");
        cargarProveedorYPlanificacion();

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("data-total>0,2468<")));
    }

    /**
     * Datos válidos del formulario: dos ítems del catálogo (Pilsen y Cascade) con cantidad y costo unitario.
     */
    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-51: el buscador de ítems atiende como máximo los primeros 100 ids de idsAgregados y los sobrantes se ignoran, sin error del sistema")
    void caso_cp_occ_51() throws Exception {
        // Arrange: 150 ids, del 1 al 150; el 100 (ítem Pilsen) entra en el tope y el 102 (Cascade) no
        cargarProveedorYReceta();
        String ids = java.util.stream.IntStream.rangeClosed(1, 150).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));

        // Act
        String html = mockMvc.perform(get(URL + "/buscador/items").param("idProveedor", "4").param("idPlanificacion", "7").param("idsAgregados", ids))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert: el 100 queda afuera por estar entre los primeros 100; el 102 se ofrece porque su id se descartó
        assertThat(html).doesNotContain("data-id=\"100\"").contains("data-id=\"102\"")
                .contains("value=\"" + java.util.stream.IntStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + "\"");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-OCC-52: un formulario enviado con huecos al comienzo de los índices ([2] sin [0] ni [1]) vuelve con error y filas vacías resaltadas, sin llamar al service ni caer en un error del sistema")
    void caso_cp_occ_52() throws Exception {
        // Arrange: solo la fila [2]
        LinkedMultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("fechaEntregaEstimada", FECHA);
        datos.add("idPlanificacionProduccion", "7");
        datos.add("idProveedor", "4");
        datos.add("detallesCompra[2].idCatalogoProveedor", "104");
        datos.add("detallesCompra[2].cantidad", "4");
        datos.add("detallesCompra[2].costoUnitario", "7");
        cargarProveedorYPlanificacion();

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo registrar la orden de compra")))
                .andReturn().getResponse().getContentAsString();

        // Assert: tres filas sin hueco, las dos primeras vacías y resaltadas
        assertThat(html).contains("name=\"detallesCompra[0].idCatalogoProveedor\" value=\"\"")
                .contains("name=\"detallesCompra[1].idCatalogoProveedor\" value=\"\"")
                .contains("name=\"detallesCompra[2].idCatalogoProveedor\" value=\"104\"");
        assertThat(html.split(Pattern.quote(FILA_RESALTADA), -1)).hasSize(3);
        verify(ordenCompraServicio, never()).registrarOrdenCompra(any());
    }

    // ==================== Datos de prueba ====================

    private static LinkedMultiValueMap<String, String> datosValidos() {
        LinkedMultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("fechaEntregaEstimada", FECHA);
        datos.add("idPlanificacionProduccion", "7");
        datos.add("idProveedor", "4");
        datos.add("detallesCompra[0].idCatalogoProveedor", "100");
        datos.add("detallesCompra[0].cantidad", "10");
        datos.add("detallesCompra[0].costoUnitario", "1250.50");
        datos.add("detallesCompra[1].idCatalogoProveedor", "102");
        datos.add("detallesCompra[1].cantidad", "2");
        datos.add("detallesCompra[1].costoUnitario", "0.1234");
        return datos;
    }

    /**
     * La planificación 7 y el proveedor 4 que referencian los datos válidos, para mostrar sus textos y los nombres de
     * los ítems al volver a renderizar el formulario.
     */
    private void cargarProveedorYPlanificacion() {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor());
    }

    /**
     * El proveedor 4 y la receta de la planificación 7, que usa Pilsen (malta 11), Cascade (lúpulo 12) y Safale US-05
     * (levadura 14), pero no Caramelo (malta 13).
     */
    private void cargarProveedorYReceta() {
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor());
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacion());
    }

    private static VersionRecetaDePlanificacionResponseDTO recetaDePlanificacion() {
        return VersionRecetaDePlanificacionResponseDTO.builder().idPlanificacion(7L).version(VersionRecetaResponseDTO.builder()
                .nombre("Pale Ale")
                .detallesMalta(List.of(DetalleMaltaResponseDTO.builder().malta(malta(11L, "Pilsen")).build()))
                .detallesLupulo(List.of(DetalleLupuloResponseDTO.builder().lupulo(lupulo(12L, "Cascade")).build()))
                .detallesLevadura(List.of(DetalleLevaduraResponseDTO.builder()
                        .levadura(LevaduraResponseDTO.builder().id(14L).nombre("Safale US-05").unidadDeMedida(UnidadDeMedida.GRAMO).build()).build()))
                .build()).build();
    }

    private static MaltaResponseDTO malta(Long id, String nombre) {
        return MaltaResponseDTO.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
    }

    private static LupuloResponseDTO lupulo(Long id, String nombre) {
        return LupuloResponseDTO.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static PresentacionComercialResponseDTO presentacion(Long id, String nombre, Double cantidad, UnidadDeMedida unidad) {
        return PresentacionComercialResponseDTO.builder().id(id).nombre(nombre).cantidad(cantidad).unidadDeMedida(unidad).build();
    }

    private static CatalogoProveedorResponseDTO item(Long id, InsumoResponseDTO insumo, PresentacionComercialResponseDTO presentacion) {
        return CatalogoProveedorResponseDTO.builder().id(id).insumo(insumo).presentacionComercial(presentacion).build();
    }

    /**
     * El catálogo del proveedor 4: Pilsen en dos unidades (100, 101), Cascade (102), Caramelo (103, que la receta
     * no usa) y Safale US-05 (104).
     */
    private static ProveedorResponseDTO proveedor() {
        return proveedorConCatalogo(List.of(
                item(100L, malta(11L, "Pilsen"), presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO)),
                item(101L, malta(11L, "Pilsen"), presentacion(6L, "Bolsa de 1.000 g", 1000.0, UnidadDeMedida.GRAMO)),
                item(102L, lupulo(12L, "Cascade"), presentacion(7L, "Bolsa de 1 Kg", 1.0, UnidadDeMedida.KILOGRAMO)),
                item(103L, malta(13L, "Caramelo"), presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO)),
                item(104L, LevaduraResponseDTO.builder().id(14L).nombre("Safale US-05").unidadDeMedida(UnidadDeMedida.GRAMO).build(),
                        presentacion(8L, "Sobre de 11,5 g", 11.5, UnidadDeMedida.GRAMO))));
    }

    private static ProveedorResponseDTO proveedorConCatalogo(List<CatalogoProveedorResponseDTO> catalogo) {
        return ProveedorResponseDTO.builder().id(4L).estado(Estado.ACTIVO).version(versionProveedor(catalogo)).build();
    }

    private static VersionProveedorResponseDTO versionProveedor(List<CatalogoProveedorResponseDTO> catalogo) {
        return VersionProveedorResponseDTO.builder().id(40L).razonSocial("Malterías del Sur S.A.").nombreComercial("Malterías del Sur")
                .cuit("30712345678").email("ventas@malteriasdelsur.com").esUltimaVersion(true).catalogoProveedor(catalogo).build();
    }

    private static PlanificacionProduccionResponseDTO planificacion() {
        return PlanificacionProduccionResponseDTO.builder().id(7L).cantidadAProducir(600.0).estado(EstadoSolicitud.PENDIENTE)
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1))
                .versionReceta(VersionRecetaResponseDTO.builder().nombre("Pale Ale").build()).build();
    }

    private static RecetaResponseDTO receta(Long id, String nombre, Estado estado) {
        return RecetaResponseDTO.builder().id(id).estado(estado).version(VersionRecetaResponseDTO.builder().nombre(nombre).build()).build();
    }

    /**
     * Una orden como la devuelven los listados: con subtotales y total, sin las cantidades recibida y pendiente.
     */
    private static OrdenCompraResponseDTO orden(Long id, EstadoSolicitud estado) {
        return OrdenCompraResponseDTO.builder().id(id).fechaEntregaEstimada(LocalDate.parse("2027-07-15")).estado(estado)
                .planificacionProduccion(planificacion()).proveedor(versionProveedor(List.of()))
                .detallesCompra(List.of()).total(new BigDecimal("12505.2468")).build();
    }

    /**
     * Una orden como la devuelve el detalle: dos ítems (10 bolsas de Pilsen de 25 kg a 1.250,50 con 4 bolsas (100 kg) recibidas y
     * 2 bolsas de Cascade de 1 kg a 0,1234 sin recibir nada) con sus cantidades recibida y pendiente.
     */
    private static OrdenCompraResponseDTO ordenConDetalle(EstadoSolicitud estado) {
        DetalleCompraResponseDTO pilsen = DetalleCompraResponseDTO.builder().id(1L).cantidad(10).costoUnitario(new BigDecimal("1250.50"))
                .subtotal(new BigDecimal("12505.00")).cantidadRecibida(4.0).cantidadPendiente(6.0)
                .cantidadRecibidaEnUnidadDelInsumo(100.0).cantidadPendienteEnUnidadDelInsumo(150.0)
                .catalogoProveedor(item(100L, malta(11L, "Pilsen"), presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO))).build();
        DetalleCompraResponseDTO cascade = DetalleCompraResponseDTO.builder().id(2L).cantidad(2).costoUnitario(new BigDecimal("0.1234"))
                .subtotal(new BigDecimal("0.2468")).cantidadRecibida(0.0).cantidadPendiente(2.0)
                .cantidadRecibidaEnUnidadDelInsumo(0.0).cantidadPendienteEnUnidadDelInsumo(2000.0)
                .catalogoProveedor(item(102L, lupulo(12L, "Cascade"), presentacion(7L, "Bolsa de 1 Kg", 1.0, UnidadDeMedida.KILOGRAMO))).build();
        return OrdenCompraResponseDTO.builder().id(9L).fechaEntregaEstimada(LocalDate.parse("2027-07-15")).estado(estado)
                .planificacionProduccion(planificacion()).proveedor(versionProveedor(List.of()))
                .detallesCompra(List.of(pilsen, cascade)).total(new BigDecimal("12505.2468"))
                .totalItems(2).totalPedidas(12).totalRecibidas(4.0).totalPendientes(8.0).porcentajeRecibido(33).build();
    }

    private static OrdenCompraResponseDTO conCierre(OrdenCompraResponseDTO base, String motivoFinalizacion, LocalDateTime fechaFinalizacion,
                                                    String motivoAnulacion, LocalDateTime fechaAnulacion) {
        return OrdenCompraResponseDTO.builder().id(base.getId()).fechaEntregaEstimada(base.getFechaEntregaEstimada()).estado(base.getEstado())
                .planificacionProduccion(base.getPlanificacionProduccion()).proveedor(base.getProveedor())
                .detallesCompra(base.getDetallesCompra()).total(base.getTotal())
                .totalItems(base.getTotalItems()).totalPedidas(base.getTotalPedidas()).totalRecibidas(base.getTotalRecibidas())
                .totalPendientes(base.getTotalPendientes()).porcentajeRecibido(base.getPorcentajeRecibido())
                .motivoFinalizacion(motivoFinalizacion).fechaFinalizacion(fechaFinalizacion)
                .motivoAnulacion(motivoAnulacion).fechaAnulacion(fechaAnulacion)
                .usuarioFinalizacion("gerente.compras").usuarioAnulacion("gerente.compras").build();
    }

    /**
     * El HTML del formulario de la orden, sin el resto de la página (el encabezado trae otro formulario, el de salir).
     */
    private static String formularioDe(String html) {
        int inicio = html.indexOf("<form id=\"orden-form\"");
        return html.substring(inicio, html.indexOf("</form>", inicio));
    }

    private static PageImpl<OrdenCompraResponseDTO> paginaVacia() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }
}
