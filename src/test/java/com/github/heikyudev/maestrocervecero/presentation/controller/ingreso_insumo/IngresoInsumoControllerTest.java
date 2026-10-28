package com.github.heikyudev.maestrocervecero.presentation.controller.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IIngresoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(IngresoInsumoController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class IngresoInsumoControllerTest {

    private static final String URL = "/ingresos-insumo";
    private static final String URL_FORMULARIO_COMPRA = URL + "/compra/nuevo";
    private static final String URL_FORMULARIO_DIRECTO = URL + "/directo/nuevo";
    private static final String URL_BUSCADOR_ORDENES = URL + "/compra/buscador/ordenes-compra";
    private static final String URL_BUSCADOR_ITEMS = URL + "/compra/buscador/items";
    private static final String URL_BUSCADOR_INSUMOS = URL + "/directo/buscador/insumos";
    private static final String URL_FILTRO_INSUMOS = URL + "/filtros/insumos";
    private static final String URL_BUSCADOR_LOTES = URL + "/buscador/lotes-insumo";
    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final String REGISTRADO = "Ingreso de insumo registrado correctamente";
    private static final LocalDate HOY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IIngresoInsumoServicio ingresoInsumoServicio;

    @MockitoBean
    private IOrdenCompraServicio ordenCompraServicio;

    @MockitoBean
    private IInsumoServicio insumoServicio;

    @MockitoBean
    private ILoteInsumoServicio loteInsumoServicio;

    @MockitoBean
    private IProveedorServicio proveedorServicio;

    @MockitoBean
    private IPlanificacionProduccionServicio planificacionServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /**
     * Escenario base: los ingresos 21 (registrado) y 20 (anulado) por compra de la orden 7; el insumo Pilsen (id 10, kg)
     * y Cascade (id 31, g); la orden pendiente 7 con cuatro ítems (Pilsen, Cascade sin pendiente, Caramelo y Citra), la
     * orden finalizada 8 y la orden pendiente 9 con un solo ítem; el proveedor 3 y la planificación 5; y tres lotes
     * del insumo 10 (uno agotado y uno vencido). Cada caso cambia solo lo que dice.
     */
    @BeforeEach
    void escenarioBase() {
        stubIngresos(paginaDe(ingresoCompra(21L, EstadoTransaccion.REGISTRADO, null), ingresoCompra(20L, EstadoTransaccion.ANULADO, "Error de carga")));
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any())).thenReturn(ingresoCompra(22L, EstadoTransaccion.REGISTRADO, null));
        when(ingresoInsumoServicio.registrarIngresoInsumoDirecto(any())).thenReturn(ingresoDirecto(23L, EstadoTransaccion.REGISTRADO));
        when(ingresoInsumoServicio.anularIngresoInsumo(any(), any())).thenReturn(ingresoCompra(21L, EstadoTransaccion.ANULADO, "Error de carga"));
        when(ingresoInsumoServicio.anularIngresoInsumo(eq(30L), any())).thenReturn(ingresoDirecto(30L, EstadoTransaccion.ANULADO));
        when(insumoServicio.buscarPorId(10L)).thenReturn(pilsen());
        when(insumoServicio.buscarPorId(31L)).thenReturn(cascade());
        when(insumoServicio.filtrarInsumos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(pilsen(), cascade()), PageRequest.of(0, 8), 2));
        when(ordenCompraServicio.buscarPorId(7L)).thenReturn(ordenSiete(EstadoSolicitud.PENDIENTE));
        when(ordenCompraServicio.buscarPorId(8L)).thenReturn(orden(8L, EstadoSolicitud.FINALIZADA, List.of()));
        when(ordenCompraServicio.buscarPorId(9L)).thenReturn(orden(9L, EstadoSolicitud.PENDIENTE,
                List.of(detalle(90L, 2, 0.0, 2.0, pilsen(), "Saco", 25.0, UnidadDeMedida.KILOGRAMO))));
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(ordenSiete(EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 8), 1));
        when(proveedorServicio.buscarPorId(3L)).thenReturn(ProveedorResponseDTO.builder().id(3L).version(proveedor()).build());
        when(planificacionServicio.buscarPorId(5L)).thenReturn(PlanificacionProduccionResponseDTO.builder().id(5L)
                .versionReceta(VersionRecetaResponseDTO.builder().nombre("Pale Ale").build()).build());
        when(loteInsumoServicio.filtrarLotesInsumoPorInsumo(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                lote(1L, "L-124", LocalDate.of(2020, 1, 1), 0.0, true),
                lote(2L, "L-125", LocalDate.of(2028, 3, 12), 40.0, false),
                lote(3L, "L-126", LocalDate.of(2028, 9, 1), 0.0, false)), PageRequest.of(0, 8), 3));
    }

    // ==================== GET /ingresos-insumo ====================

    @Test
    @DisplayName("CP-IIC-01: la lista abre en la pestaña Por compra: pide al service los ingresos COMPRA de a 20, por fecha de ingreso e id descendentes, sin otros criterios")
    void lista_debeAbrirEnPorCompraConElOrdenPedido() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Ingresos de Insumo", "Por compra", "Directos");
        assertThat(html).contains("data-pestanas-ingreso").contains("data-ingresos-registrados").contains("data-pestana=\"compra\"");
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(isNull(), isNull(), isNull(), eq(TipoIngreso.COMPRA), isNull(), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isZero();
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(pagina.getValue().getSort().getOrderFor("fechaIngreso").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pagina.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("CP-IIC-02: la pestaña Por compra muestra fecha, insumo, lote, vencimiento, cantidad en presentaciones con su equivalente, precio por presentación y por unidad del insumo (en celdas de dos líneas), enlace a la orden, estado y la acción Anular solo del ingreso registrado")
    void lista_debeMostrarLasColumnasDeCadaIngreso() throws Exception {
        // Act
        String html = html(Rol.ENCARGADO_DE_DEPOSITO);

        // Assert
        assertThat(texto(bloque(html, "thead"))).isEqualTo("Ingreso Insumo y lote Cantidad Precio ($) Orden Acciones");
        assertThat(celdas(html, "data-ingreso=\"21\"")).containsExactly("01/07/2027 Registrado", "Pilsen Lote L-125 · vence 12/03/2028", "2 Bolsa 25 kg 50 kg", "12.000,00 = 480,00 / kg", "N.º 7", "Anular");
        assertThat(celdas(html, "data-ingreso=\"20\"")).containsExactly("01/07/2027 Anulado", "Pilsen Lote L-125 · vence 12/03/2028", "2 Bolsa 25 kg 50 kg", "12.000,00 = 480,00 / kg", "N.º 7", "");
        assertThat(enlaceDe(html, "N.º 7")).isEqualTo("/ordenes-compra/7");
        assertThat(elementoConAtributo(html, "tr", "data-ingreso=\"21\"")).contains("data-motivo-dialogo=\"dialogo-anular\"").contains("data-motivo-url=\"/ingresos-insumo/21/anular\"");
        assertThat(elementoConAtributo(html, "tr", "data-ingreso=\"20\"")).doesNotContain("data-motivo-url").contains("opacity-60");
    }

    @Test
    @DisplayName("CP-IIC-03: la pestaña Directos pide los ingresos DIRECTO, no tiene la columna de la orden de compra y su botón Nuevo lleva al formulario directo")
    void lista_debeMostrarLaPestanaDirectos() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).param("tipo", "directo").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(isNull(), isNull(), isNull(), eq(TipoIngreso.DIRECTO), isNull(), any(Pageable.class));
        assertThat(html).contains("data-pestana=\"directo\"").doesNotContain("data-orden-compra");
        assertThat(texto(bloque(html, "thead"))).doesNotContain("Orden").endsWith("Precio ($) Acciones");
        assertThat(enlaceDe(html, "Nuevo ingreso directo")).isEqualTo(URL_FORMULARIO_DIRECTO);
        assertThat(elementoConAtributo(html, "a", "data-pestana-directo")).contains("aria-current=\"page\"");
        assertThat(elementoConAtributo(html, "a", "data-pestana-compra")).doesNotContain("aria-current");
    }

    @Test
    @DisplayName("CP-IIC-04: la pestaña por compra tiene el botón Nuevo ingreso por compra hacia su formulario y una pestaña desconocida cae en Por compra")
    void lista_debeOfrecerElNuevoIngresoPorCompra() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).param("tipo", "cualquiera").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(isNull(), isNull(), isNull(), eq(TipoIngreso.COMPRA), isNull(), any(Pageable.class));
        assertThat(enlaceDe(html, "Nuevo ingreso por compra")).isEqualTo(URL_FORMULARIO_COMPRA);
    }

    @Test
    @DisplayName("CP-IIC-05: los criterios de insumo, identificación del lote, estado y fecha de ingreso viajan al service junto con la pestaña y se conservan en el formulario de búsqueda")
    void lista_debeAplicarYConservarLosCriterios() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).param("tipo", "directo").param("idInsumo", "10").param("identificacion", " L-12 ")
                        .param("estado", "REGISTRADO").param("fechaIngreso", "2027-07-01").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(eq(10L), eq("L-12"), eq(EstadoTransaccion.REGISTRADO), eq(TipoIngreso.DIRECTO),
                eq(LocalDate.of(2027, 7, 1)), any(Pageable.class));
        String filtro = bloque(html, "data-filtro-ingresos");
        assertThat(textoDelSelector(filtro, "Seleccione insumo")).isEqualTo("Pilsen");
        assertThat(campo(filtro, "idInsumo")).isEqualTo("10");
        assertThat(campo(filtro, "identificacion")).isEqualTo("L-12");
        assertThat(campo(filtro, "fechaIngreso")).isEqualTo("2027-07-01");
        assertThat(campo(filtro, "tipo")).isEqualTo("directo");
        assertThat(elementoConAtributo(filtro, "option", "value=\"REGISTRADO\"")).contains("selected");
        assertThat(texto(filtro)).contains("Insumo", "Identificación del lote del proveedor", "Estado", "Todos", "Registrado", "Anulado", "Fecha de ingreso", "Limpiar", "Buscar");
        assertThat(filtro).contains("placeholder=\"Ej: L-125\"").contains("placeholder=\"Ej: 15/07/2027\"").contains("data-buscador-url=\"" + URL_FILTRO_INSUMOS + "\"");
        assertThat(enlaceDe(filtro, "Limpiar")).isEqualTo(URL + "?tipo=directo");
    }

    @Test
    @DisplayName("CP-IIC-06: un estado inválido muestra todos los ingresos con un aviso amarillo, sin error")
    void lista_estadoInvalidoMuestraTodosConAviso() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).param("estado", "XYZ").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(isNull(), isNull(), isNull(), eq(TipoIngreso.COMPRA), isNull(), any(Pageable.class));
        assertThat(texto(html)).contains("El estado indicado no es válido, se muestran todos los ingresos");
    }

    @Test
    @DisplayName("CP-IIC-07: un insumo que ya no existe no restringe el listado y el selector queda vacío")
    void lista_insumoInexistenteNoRestringe() throws Exception {
        // Arrange
        when(insumoServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el insumo con ID: 99"));

        // Act
        String html = mockMvc.perform(get(URL).param("idInsumo", "99").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(isNull(), isNull(), isNull(), eq(TipoIngreso.COMPRA), isNull(), any(Pageable.class));
        assertThat(textoDelSelector(html, "Seleccione insumo")).isEmpty();
    }

    @Test
    @DisplayName("CP-IIC-08: sin ingresos la lista muestra un mensaje distinto en cada pestaña y ninguna fila")
    void lista_sinIngresosMuestraElMensaje() throws Exception {
        // Arrange
        stubIngresos(Page.empty());

        // Act
        String porCompra = html(Rol.ENCARGADO_DE_DEPOSITO);
        String directos = mockMvc.perform(get(URL).param("tipo", "directo").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(porCompra)).contains("No se encontraron ingresos por compra con los criterios indicados");
        assertThat(texto(directos)).contains("No se encontraron ingresos directos con los criterios indicados");
        assertThat(porCompra).doesNotContain("data-ingreso=");
    }

    @Test
    @DisplayName("CP-IIC-09: un ingreso anulado ofrece el detalle de la anulación (motivo, usuario y fecha) en un diálogo y el registrado no")
    void lista_debeOfrecerElDetalleDeLaAnulacion() throws Exception {
        // Act
        String html = html(Rol.ENCARGADO_DE_DEPOSITO);

        // Assert
        String anulado = elementoConAtributo(html, "tr", "data-ingreso=\"20\"");
        assertThat(anulado).contains("data-detalle-anulacion").contains("data-detalle-motivo=\"Error de carga\"").contains("data-detalle-usuario=\"encargado\"")
                .contains("data-detalle-fecha=\"02/07/2027 09:30\"").contains("data-detalle-titulo=\"Detalle de la anulación\"");
        assertThat(elementoConAtributo(html, "tr", "data-ingreso=\"21\"")).doesNotContain("data-detalle-anulacion");
        assertThat(html).contains("id=\"dialogo-anular\"").contains("id=\"dialogo-detalle-anulacion\"");
    }

    @Test
    @DisplayName("CP-IIC-10: los enlaces de paginación conservan la pestaña y los criterios")
    void lista_paginacionConservaLosCriterios() throws Exception {
        // Arrange
        stubIngresos(new PageImpl<>(List.of(ingresoCompra(21L, EstadoTransaccion.REGISTRADO, null)), PageRequest.of(1, 20), 45));

        // Act
        String html = mockMvc.perform(get(URL).param("idInsumo", "10").param("estado", "REGISTRADO").param("pagina", "1").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(enlaceDe(html, "Anterior")).contains("tipo=compra").contains("pagina=0").contains("idInsumo=10").contains("estado=REGISTRADO");
        assertThat(enlaceDe(html, "Siguiente")).contains("tipo=compra").contains("pagina=2").contains("idInsumo=10").contains("estado=REGISTRADO");
        assertThat(texto(html)).contains("45 ingresos", "Página 2 de 3");
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(eq(10L), isNull(), eq(EstadoTransaccion.REGISTRADO), eq(TipoIngreso.COMPRA), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("CP-IIC-11: una página negativa se pide como la primera")
    void lista_paginaNegativaEsLaPrimera() throws Exception {
        // Act
        mockMvc.perform(get(URL).param("pagina", "-5").with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(ingresoInsumoServicio).filtrarIngresoInsumo(any(), any(), any(), any(), any(), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("CP-IIC-12: quien solo puede consultar no ve el botón Nuevo, la columna Acciones ni el diálogo de anulación")
    void lista_soloConsultaEsDeSoloLectura() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL).with(conPermisos(Permiso.INGRESO_INSUMO_CONSULTAR)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(bloque(html, "thead"))).doesNotContain("Acciones");
        assertThat(html).doesNotContain("data-nuevo-ingreso").doesNotContain("data-motivo-url").doesNotContain("id=\"dialogo-anular\"");
        assertThat(texto(html)).contains("Pilsen", "L-125");
    }

    @Test
    @DisplayName("CP-IIC-13: los textos largos de la fila se truncan con tooltip flotante")
    void lista_textosLargosConTooltip() throws Exception {
        // Act
        String fila = elementoConAtributo(html(Rol.ENCARGADO_DE_DEPOSITO), "tr", "data-ingreso=\"21\"");

        // Assert
        assertThat(fila).contains("data-tooltip=\"Pilsen\"").contains("data-tooltip=\"Lote L-125 · vence 12/03/2028\"").contains("truncate");
    }

    @Test
    @DisplayName("CP-IIC-14: la sidebar ofrece Ingresos de Insumo, resaltado en su pantalla, y el título de la página lo nombra")
    void lista_sidebarYTitulo() throws Exception {
        // Act
        String html = html(Rol.ENCARGADO_DE_DEPOSITO);

        // Assert
        assertThat(enlaceDe(html, "Ingresos de Insumo")).isEqualTo(URL);
        assertThat(html).containsPattern("href=\"/ingresos-insumo\"[^>]*bg-amber-500/10").contains("bi-box-arrow-in-down");
        assertThat(html).containsPattern("<title[^>]*>[^<]*Ingresos de insumo");
    }

    @Test
    @DisplayName("CP-IIC-15: las pantallas no tienen emojis, ninguna alerta termina en punto y los formularios funcionan sin JavaScript (POST con CSRF, novalidate, sin required)")
    void transversal_ayudasVisualesYSinJavaScript() throws Exception {
        // Arrange
        MvcResult rechazo = mockMvc.perform(post(URL + "/compra").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf())).andReturn();
        List<String> paginas = List.of(
                html(Rol.ENCARGADO_DE_DEPOSITO),
                mockMvc.perform(get(URL).flashAttr("mensaje", REGISTRADO).flashAttr("tipo", "success").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                mockMvc.perform(get(URL_FORMULARIO_COMPRA).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                mockMvc.perform(get(URL_FORMULARIO_DIRECTO).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                rechazo.getResponse().getContentAsString(StandardCharsets.UTF_8));

        // Assert
        Pattern alerta = Pattern.compile("(?s)role=\"alert\"[^>]*>(.*?)</div>");
        Pattern emoji = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}]");
        for (String pagina : paginas) {
            Matcher alertas = alerta.matcher(pagina);
            while (alertas.find()) {
                assertThat(texto(alertas.group(1))).isNotEmpty().doesNotEndWith(".");
            }
            assertThat(emoji.matcher(pagina).find()).isFalse();
        }
        for (String formulario : List.of(paginas.get(2), paginas.get(3))) {
            assertThat(contenido(formulario)).contains("method=\"post\"").contains("novalidate").contains("data-un-solo-envio").contains("/js/ingreso-insumo-form.js");
            assertThat(cuenta(contenido(formulario), "name=\"_csrf\"")).isEqualTo(1);
            assertThat(formulario).doesNotContain(" required").doesNotContain(" min=").doesNotContain(" pattern=");
        }
    }

    @Test
    @DisplayName("CP-IIC-68: la pestaña Directos muestra la cantidad y el precio por unidad en la unidad del insumo, en una sola columna de precio")
    void lista_pestanaDirectosMuestraElPrecioPorUnidadDelInsumo() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.filtrarIngresoInsumo(any(), any(), any(), eq(TipoIngreso.DIRECTO), any(), any()))
                .thenReturn(paginaDe(ingresoDirecto(30L, EstadoTransaccion.REGISTRADO)));

        // Act
        String html = mockMvc.perform(get(URL).param("tipo", "directo").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(bloque(html, "thead"))).isEqualTo("Ingreso Insumo y lote Cantidad Precio ($) Acciones");
        assertThat(celdas(html, "data-ingreso=\"30\"")).containsExactly("01/07/2027 Registrado", "Pilsen Lote L-125 · vence 12/03/2028", "50 kg", "12,50 / kg", "Anular");
        assertThat(html).doesNotContain("data-precio-presentacion");
    }

    @Test
    @DisplayName("CP-IIC-69: la cabecera del precio es corta y su tooltip explica la relación entre el precio por presentación y por unidad; la tabla compacta conserva sus ganchos y la columna Acciones fija")
    void lista_cabeceraDelPrecioYTablaCompacta() throws Exception {
        // Act
        String html = html(Rol.ENCARGADO_DE_DEPOSITO);

        // Assert
        assertThat(texto(bloque(html, "thead"))).contains("Precio ($)").doesNotContain("Precio por");
        assertThat(html).contains("12.000 por Bolsa 25 kg = 480 por kg").contains("data-precio-presentacion").contains("data-precio-unidad").contains("col-acciones");
        assertThat(html).doesNotContain("whitespace-nowrap\">Cantidad recibida").contains("overflow-x-auto");
    }

    @Test
    @DisplayName("CP-IIC-16: el formulario por compra vacío tiene los campos con asterisco rojo y placeholder de ejemplo, la fecha de ingreso de hoy, el ítem y los lotes existentes deshabilitados y los dos botones de registro")
    void formularioCompra_debeMostrarseVacio() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_FORMULARIO_COMPRA).with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-compra-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        String formulario = bloque(html, "data-formulario-ingreso");
        assertThat(formulario).contains("action=\"" + URL + "/compra\"");
        assertThat(texto(formulario)).contains("Orden de compra *", "Ítem de la orden *", "Identificación del lote del proveedor *", "Fecha de vencimiento *",
                "Fecha de ingreso *", "Cantidad de presentaciones recibidas *", "Se guarda en mayúsculas y sin espacios de más", "Elija primero la orden de compra",
                "Ver lotes existentes", "Elija primero el insumo", "Solo se ofrecen órdenes pendientes", "Registrar y cargar otro ítem de esta orden", "Registrar ingreso");
        assertThat(cuenta(formulario, "title=\"Obligatorio\"")).isEqualTo(6);
        assertThat(formulario).contains("placeholder=\"Seleccione orden de compra\"").contains("placeholder=\"Seleccione ítem de la orden\"")
                .contains("placeholder=\"Ej: L-125\"").contains("placeholder=\"Ej: 2\"");
        assertThat(cuenta(formulario, "placeholder=\"Ej: 15/07/2027\"")).isEqualTo(2);
        assertThat(campo(formulario, "fechaIngreso")).isEqualTo(HOY.toString());
        assertThat(textoDelSelector(formulario, "Seleccione orden de compra")).isEmpty();
        assertThat(apertura(formulario, "data-selector-item")).contains("data-deshabilitado");
        assertThat(apertura(formulario, "data-lote-existente")).contains("data-deshabilitado");
        assertThat(elementoConAtributo(formulario, "button", "name=\"cargarOtro\"")).contains("type=\"submit\"");
        assertThat(formulario).contains("data-lote-limpiar").contains("data-ingresa-al-lote");
        assertThat(apertura(formulario, "data-ingresa-al-lote")).contains("hidden");
        assertThat(formulario).doesNotContain("Cantidad recibida");
        assertThat(enlaceDe(html, "Cancelar")).isEqualTo(URL + "?tipo=compra");
        assertThat(enlaceDe(html, "Volver a ingresos")).isEqualTo(URL + "?tipo=compra");
        verifyNoInteractions(ingresoInsumoServicio);
    }

    @Test
    @DisplayName("CP-IIC-17: con idOrdenCompra el formulario llega con la orden pendiente elegida y el selector de ítems habilitado y apuntando a esa orden")
    void formularioCompra_conOrdenPreseleccionada() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_FORMULARIO_COMPRA).param("idOrdenCompra", "7").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(textoDelSelector(html, "Seleccione orden de compra")).isEqualTo("N.º 7 - Malterías del Sur S.A.");
        assertThat(campo(html, "idOrdenCompra")).isEqualTo("7");
        assertThat(apertura(html, "data-selector-item")).doesNotContain("data-deshabilitado");
        assertThat(html).contains("data-buscador-url=\"" + URL_BUSCADOR_ITEMS + "?idOrdenCompra=7\"");
        assertThat(texto(html)).doesNotContain("La orden de compra indicada no está pendiente");
    }

    @Test
    @DisplayName("CP-IIC-18: una orden que ya no está pendiente, o que no existe, no se elige y se avisa con una alerta amarilla")
    void formularioCompra_ordenNoPendienteONoExistente() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99"));

        // Act
        String finalizada = mockMvc.perform(get(URL_FORMULARIO_COMPRA).param("idOrdenCompra", "8").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String inexistente = mockMvc.perform(get(URL_FORMULARIO_COMPRA).param("idOrdenCompra", "99").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        for (String html : List.of(finalizada, inexistente)) {
            assertThat(texto(html)).contains("La orden de compra indicada no está pendiente, elija otra");
            assertThat(textoDelSelector(html, "Seleccione orden de compra")).isEmpty();
            assertThat(apertura(html, "data-selector-item")).contains("data-deshabilitado");
        }
    }

    @Test
    @DisplayName("CP-IIC-19: un idOrdenCompra que no es un número vuelve al origen con una alerta amarilla, sin error")
    void formularioCompra_ordenNoNumerica() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL_FORMULARIO_COMPRA).param("idOrdenCompra", "abc").header("Referer", URL).with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    // ==================== POST /ingresos-insumo/compra ====================

    @Test
    @DisplayName("CP-IIC-20: registrar por compra envía los datos al service y vuelve a la pestaña Por compra con la alerta de éxito")
    void registrarPorCompra_debeRegistrarYVolverALaLista() throws Exception {
        // Act + Assert
        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=compra"))
                .andExpect(flash().attribute("mensaje", REGISTRADO))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<IngresoInsumoPorCompraFormDTO> dto = ArgumentCaptor.forClass(IngresoInsumoPorCompraFormDTO.class);
        verify(ingresoInsumoServicio).registrarIngresoInsumoPorCompra(dto.capture());
        assertThat(dto.getValue().getIdDetalleCompra()).isEqualTo(70L);
        assertThat(dto.getValue().getIdentificacionLoteProveedor()).isEqualTo("l-125");
        assertThat(dto.getValue().getCantidadPresentaciones()).isEqualTo(2);
        assertThat(dto.getValue().getFechaIngreso()).isEqualTo(HOY);
        assertThat(dto.getValue().getFechaVencimiento()).isEqualTo(HOY.plusYears(1));
    }

    @Test
    @DisplayName("CP-IIC-21: Registrar y cargar otro ítem vuelve al formulario con la misma orden elegida mientras siga pendiente")
    void registrarPorCompra_cargarOtroConOrdenPendiente() throws Exception {
        // Act + Assert
        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("cargarOtro", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_FORMULARIO_COMPRA + "?idOrdenCompra=7"))
                .andExpect(flash().attribute("mensaje", REGISTRADO))
                .andExpect(flash().attribute("tipo", "success"));
    }

    @Test
    @DisplayName("CP-IIC-22: Registrar y cargar otro ítem con una orden que el ingreso completó vuelve a la lista con un aviso informativo de que la orden quedó completa")
    void registrarPorCompra_cargarOtroConOrdenCompletada() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any())).thenReturn(ingresoCompra(22L, EstadoTransaccion.REGISTRADO, null, 8L));

        // Act + Assert
        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("cargarOtro", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=compra"))
                .andExpect(flash().attribute("tipo", "info"))
                .andExpect(flash().attribute("mensaje", REGISTRADO + ". La orden de compra N.º 8 quedó completa y ya no tiene ítems pendientes"));
    }

    @Test
    @DisplayName("CP-IIC-23: Registrar y cargar otro ítem cuando la orden ya no se puede consultar vuelve a la lista con el aviso, sin error")
    void registrarPorCompra_cargarOtroConOrdenInexistente() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(7L)).thenThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 7"));

        // Act + Assert
        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("cargarOtro", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=compra"))
                .andExpect(flash().attribute("tipo", "info"));
    }

    @Test
    @DisplayName("CP-IIC-24: una regla de la cantidad rechazada por el service vuelve al formulario (200) con el mensaje en el campo y en el resumen, y con lo cargado conservado")
    void registrarPorCompra_reglaDeLaCantidadVuelveAlFormulario() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any()))
                .thenThrow(new ReglaNegocioException("La cantidad de presentaciones recibidas no puede superar la cantidad pendiente de entrega del ítem (3)", "cantidadPresentaciones"));

        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-compra-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        String mensaje = "La cantidad de presentaciones recibidas no puede superar la cantidad pendiente de entrega del ítem (3)";
        assertThat(cuenta(texto(html), mensaje)).isEqualTo(2);
        assertThat(texto(html)).contains("No se pudo registrar el ingreso");
        assertThat(etiquetaDelCampo(html, "cantidadPresentaciones")).contains("border-rose-400");
        assertThat(etiquetaDelCampo(html, "identificacionLoteProveedor")).doesNotContain("border-rose-400");
        assertThat(campo(html, "identificacionLoteProveedor")).isEqualTo("l-125");
        assertThat(campo(html, "cantidadPresentaciones")).isEqualTo("2");
        assertThat(texto(html)).contains("Ingresan 50 kg al lote");
        assertThat(apertura(html, "data-ingresa-al-lote")).doesNotContain("hidden");
        assertThat(campo(html, "fechaVencimiento")).isEqualTo(HOY.plusYears(1).toString());
        assertThat(campo(html, "fechaIngreso")).isEqualTo(HOY.toString());
    }

    @Test
    @DisplayName("CP-IIC-25: una regla que no nombra un campo (la orden no está pendiente) queda como error general en el resumen, sin marcar ningún campo")
    void registrarPorCompra_reglaGeneralQuedaEnElResumen() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any()))
                .thenThrow(new ReglaNegocioException("Solo se pueden registrar ingresos de ítems de órdenes de compra en estado Pendiente"));

        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(texto(html), "Solo se pueden registrar ingresos de ítems de órdenes de compra en estado Pendiente")).isEqualTo(1);
        assertThat(html).doesNotContain("border-rose-400");
    }

    @Test
    @DisplayName("CP-IIC-26: el conflicto de vencimiento del lote existente se marca en el campo de la fecha de vencimiento")
    void registrarPorCompra_conflictoDeVencimientoMarcaElCampo() throws Exception {
        // Arrange
        String mensaje = "Ya existe un lote del insumo con la identificación de lote de proveedor 'L-125' pero con una fecha de vencimiento distinta";
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any())).thenThrow(new ReglaNegocioException(mensaje, "fechaVencimiento"));

        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(etiquetaDelCampo(html, "fechaVencimiento")).contains("border-rose-400");
        assertThat(cuenta(texto(html), mensaje)).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-IIC-27: un ítem que el service no encuentra vuelve al formulario con el mensaje en el campo del ítem")
    void registrarPorCompra_itemNoEncontrado() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el ítem de detalle de compra con ID: 70", "idDetalleCompra"));

        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "7"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(texto(html), "No se encontró el ítem de detalle de compra con ID: 70")).isEqualTo(2);
        assertThat(html).contains("border-rose-400");
    }

    @Test
    @DisplayName("CP-IIC-28: un duplicado informado por el service vuelve al formulario con el mensaje, en el campo si lo indica y como error general si no")
    void registrarPorCompra_duplicado() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe el lote", "identificacionLoteProveedor"))
                .thenThrow(new RecursoDuplicadoException("Ya existe otro ingreso"));

        // Act
        String conCampo = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String sinCampo = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(etiquetaDelCampo(conCampo, "identificacionLoteProveedor")).contains("border-rose-400");
        assertThat(texto(sinCampo)).contains("Ya existe otro ingreso");
        assertThat(sinCampo).doesNotContain("border-rose-400");
    }

    @Test
    @DisplayName("CP-IIC-29: un formulario vacío (ítem, lote, fechas y cantidad nulos) vuelve al formulario con todos los mensajes y sin llamar al service")
    void registrarPorCompra_formularioVacio() throws Exception {
        // Act
        String html = mockMvc.perform(post(URL + "/compra").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-compra-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("La identificación del lote del proveedor es obligatoria", "La fecha de ingreso es obligatoria", "La cantidad de presentaciones recibidas es obligatoria",
                "La fecha de vencimiento es obligatoria", "El ítem de la orden de compra es obligatorio");
        verify(ingresoInsumoServicio, never()).registrarIngresoInsumoPorCompra(any());
    }

    @Test
    @DisplayName("CP-IIC-30: valores que no son del tipo del campo (cantidad de presentaciones decimal, fecha o ítem) vuelven al formulario con un mensaje claro y sin llamar al service")
    void registrarPorCompra_valoresDeTipoInvalido() throws Exception {
        // Act
        String html = mockMvc.perform(post(URL + "/compra").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf())
                        .param("cantidadPresentaciones", "2.5").param("fechaIngreso", "no-es-fecha").param("idDetalleCompra", "x"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("El valor ingresado no es una fecha válida");
        // Cada error aparece en el campo y en el resumen: el de la cantidad y el del ítem
        assertThat(cuenta(texto(html), "Ingrese un número entero válido")).isEqualTo(4);
        verify(ingresoInsumoServicio, never()).registrarIngresoInsumoPorCompra(any());
    }

    @Test
    @DisplayName("CP-IIC-31: tras un rechazo, el formulario conserva la orden y el ítem elegidos, muestra lo pendiente, la unidad y los lotes existentes del insumo habilitados")
    void registrarPorCompra_rechazoConservaLaOrdenYElItem() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any())).thenThrow(new ReglaNegocioException("La cantidad de presentaciones recibidas debe ser mayor a cero"));

        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "7"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(textoDelSelector(html, "Seleccione orden de compra")).isEqualTo("N.º 7 - Malterías del Sur S.A.");
        assertThat(textoDelSelector(html, "Seleccione ítem de la orden")).isEqualTo("Pilsen - Bolsa");
        assertThat(campo(html, "idDetalleCompra")).isEqualTo("70");
        assertThat(apertura(html, "data-selector-item")).doesNotContain("data-deshabilitado");
        assertThat(apertura(html, "data-lote-existente")).doesNotContain("data-deshabilitado").contains("data-buscador-url=\"" + URL_BUSCADOR_LOTES + "?idInsumo=10\"");
        assertThat(apertura(html, "data-formulario-ingreso")).contains("data-factor=\"25\"").contains("data-unidad-item=\"kg\"");
        assertThat(texto(bloque(html, "data-item-elegido"))).isEqualTo("Insumo: Pilsen · Presentación: Bolsa (25 kg) · Pedido: 4 · Recibido: 1 · Pendiente: 3 Bolsa");
        assertThat(texto(html)).contains("Cantidad de presentaciones recibidas *", "Ingresan 50 kg al lote");
    }

    @Test
    @DisplayName("CP-IIC-32: un rechazo con una orden que ya no existe vuelve al formulario con los selectores vacíos, sin error")
    void registrarPorCompra_rechazoConOrdenInexistente() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any())).thenThrow(new ReglaNegocioException("La cantidad de presentaciones recibidas debe ser mayor a cero"));
        when(ordenCompraServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99"));

        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "99"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(textoDelSelector(html, "Seleccione orden de compra")).isEmpty();
        assertThat(textoDelSelector(html, "Seleccione ítem de la orden")).isEmpty();
    }

    @Test
    @DisplayName("CP-IIC-33: la orden del formulario no es parte del DTO: sin idOrdenCompra el registro procede igual (el ítem determina la orden) y un idOrdenCompra no numérico vuelve al origen sin llamar al service")
    void registrarPorCompra_laOrdenEsUnParametroDeLaPantalla() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/compra").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf()).param("idDetalleCompra", "70").param("identificacionLoteProveedor", "L-1")
                        .param("fechaIngreso", HOY.toString()).param("cantidadPresentaciones", "5").param("fechaVencimiento", HOY.plusDays(30).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=compra"));
        verify(ingresoInsumoServicio).registrarIngresoInsumoPorCompra(any());

        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "abc").header("Referer", URL_FORMULARIO_COMPRA))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"));
        verify(ingresoInsumoServicio).registrarIngresoInsumoPorCompra(any());
    }

    @Test
    @DisplayName("CP-IIC-64: un ítem que no es de la orden elegida vuelve al formulario con el mensaje en el campo del ítem y sin llamar al service")
    void registrarPorCompra_itemQueNoEsDeLaOrden() throws Exception {
        // Act
        String html = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "9"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-compra-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(texto(html), "El ítem elegido no pertenece a la orden de compra seleccionada")).isEqualTo(2);
        assertThat(html).contains("border-rose-400");
        assertThat(textoDelSelector(html, "Seleccione orden de compra")).isEqualTo("N.º 9 - Malterías del Sur S.A.");
        verify(ingresoInsumoServicio, never()).registrarIngresoInsumoPorCompra(any());
    }

    @Test
    @DisplayName("CP-IIC-65: un ítem de la orden elegida se registra, y sin orden o con una orden que ya no existe el service decide")
    void registrarPorCompra_itemDeLaOrden() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99"));

        // Act + Assert
        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "7"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL + "?tipo=compra"));
        mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO)).param("idOrdenCompra", "99"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL + "?tipo=compra"));
        verify(ingresoInsumoServicio, org.mockito.Mockito.times(2)).registrarIngresoInsumoPorCompra(any());
    }

    @Test
    @DisplayName("CP-IIC-66: una orden con ítems incompletos (sin catálogo, sin cantidad pendiente o sin orden) no rompe el formulario ni el buscador de ítems")
    void compra_ordenConItemsIncompletos() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(11L)).thenReturn(OrdenCompraResponseDTO.builder().id(11L).estado(EstadoSolicitud.PENDIENTE).detallesCompra(List.of(
                DetalleCompraResponseDTO.builder().id(110L).cantidad(1).cantidadPendiente(10.0).build(),
                DetalleCompraResponseDTO.builder().id(111L).cantidad(1).build())).build());
        when(ordenCompraServicio.buscarPorId(12L)).thenReturn(OrdenCompraResponseDTO.builder().id(12L).estado(EstadoSolicitud.PENDIENTE).build());
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any())).thenThrow(new ReglaNegocioException("La cantidad de presentaciones recibidas debe ser mayor a cero", "cantidadPresentaciones"));

        // Act
        String formulario = mockMvc.perform(post(URL + "/compra").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf()).param("idOrdenCompra", "11").param("idDetalleCompra", "110")
                        .param("identificacionLoteProveedor", "L-1").param("fechaIngreso", HOY.toString()).param("cantidadPresentaciones", "0").param("fechaVencimiento", HOY.plusDays(9).toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String items = mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "11").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String sinDetalles = mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "12").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String preseleccion = mockMvc.perform(get(URL_FORMULARIO_COMPRA).param("idOrdenCompra", "12").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(formulario)).contains("La cantidad de presentaciones recibidas debe ser mayor a cero");
        assertThat(textoDelSelector(formulario, "Seleccione ítem de la orden")).isEmpty();
        assertThat(items + sinDetalles).doesNotContain("data-elegir");
        assertThat(texto(items)).contains("La orden no tiene ítems pendientes de recibir");
        assertThat(textoDelSelector(preseleccion, "Seleccione orden de compra")).isEqualTo("N.º 12");
    }

    @Test
    @DisplayName("CP-IIC-67: el campo del rechazo lo indica el service y no el texto del mensaje: sin campo, o con un campo que el formulario no tiene, el mensaje queda solo en el resumen")
    void registrar_elCampoLoIndicaElService() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoPorCompra(any()))
                .thenThrow(new ReglaNegocioException("La cantidad de presentaciones recibidas debe ser mayor a cero"))
                .thenThrow(new ReglaNegocioException("El costo unitario debe ser mayor a cero", "costoUnitario"));

        // Act
        String sinCampo = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String campoAjeno = mockMvc.perform(postCompra(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(texto(sinCampo), "La cantidad de presentaciones recibidas debe ser mayor a cero")).isEqualTo(1);
        assertThat(cuenta(texto(campoAjeno), "El costo unitario debe ser mayor a cero")).isEqualTo(1);
        assertThat(sinCampo + campoAjeno).doesNotContain("border-rose-400");
    }

    // ==================== GET /ingresos-insumo/directo/nuevo ====================

    @Test
    @DisplayName("CP-IIC-34: el formulario directo vacío tiene los campos con asterisco rojo y placeholder de ejemplo, la fecha de ingreso de hoy y los lotes existentes deshabilitados")
    void formularioDirecto_debeMostrarseVacio() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_FORMULARIO_DIRECTO).with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-directo-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        String formulario = bloque(html, "data-formulario-ingreso");
        assertThat(formulario).contains("action=\"" + URL + "/directo\"");
        assertThat(texto(formulario)).contains("Insumo *", "Identificación del lote del proveedor *", "Fecha de vencimiento *", "Fecha de ingreso *",
                "Cantidad recibida *", "Precio unitario ($) *", "Precio de cada unidad del insumo que ingresa", "Ver lotes existentes", "Registrar ingreso");
        assertThat(cuenta(formulario, "title=\"Obligatorio\"")).isEqualTo(6);
        assertThat(cuenta(formulario, "data-unidad-sufijo")).isEqualTo(2);
        assertThat(formulario).contains(">-<");
        assertThat(formulario).contains("placeholder=\"Seleccione insumo\"").contains("placeholder=\"Ej: L-125\"").contains("placeholder=\"Ej: 25\"").contains("placeholder=\"Ej: 12.50\"");
        assertThat(campo(formulario, "fechaIngreso")).isEqualTo(HOY.toString());
        assertThat(textoDelSelector(formulario, "Seleccione insumo")).isEmpty();
        assertThat(apertura(formulario, "data-lote-existente")).contains("data-deshabilitado");
        assertThat(formulario).contains("data-buscador-url=\"" + URL_BUSCADOR_INSUMOS + "\"");
        assertThat(enlaceDe(html, "Cancelar")).isEqualTo(URL + "?tipo=directo");
    }

    // ==================== POST /ingresos-insumo/directo ====================

    @Test
    @DisplayName("CP-IIC-35: registrar un ingreso directo envía los datos al service y vuelve a la pestaña Directos con la alerta de éxito")
    void registrarDirecto_debeRegistrarYVolverALaLista() throws Exception {
        // Act + Assert
        mockMvc.perform(postDirecto(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=directo"))
                .andExpect(flash().attribute("mensaje", REGISTRADO))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<IngresoInsumoDirectoFormDTO> dto = ArgumentCaptor.forClass(IngresoInsumoDirectoFormDTO.class);
        verify(ingresoInsumoServicio).registrarIngresoInsumoDirecto(dto.capture());
        assertThat(dto.getValue().getIdInsumo()).isEqualTo(10L);
        assertThat(dto.getValue().getIdentificacionLoteProveedor()).isEqualTo("l-125");
        assertThat(dto.getValue().getCantidadRecibida()).isEqualTo(25.5);
        assertThat(dto.getValue().getCostoUnitario()).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(dto.getValue().getFechaIngreso()).isEqualTo(HOY);
        assertThat(dto.getValue().getFechaVencimiento()).isEqualTo(HOY.plusYears(1));
    }

    @Test
    @DisplayName("CP-IIC-36: una regla del costo rechazada por el service vuelve al formulario directo con el mensaje en el campo del costo y en el resumen, y con lo cargado conservado")
    void registrarDirecto_reglaDelCostoVuelveAlFormulario() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoDirecto(any())).thenThrow(new ReglaNegocioException("El costo unitario debe ser mayor a cero", "costoUnitario"));

        // Act
        String html = mockMvc.perform(postDirecto(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/ingreso-directo-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(texto(html), "El costo unitario debe ser mayor a cero")).isEqualTo(2);
        assertThat(etiquetaDelCampo(html, "costoUnitario")).contains("border-rose-400");
        assertThat(etiquetaDelCampo(html, "cantidadRecibida")).doesNotContain("border-rose-400");
        assertThat(campo(html, "costoUnitario")).isEqualTo("12.5");
        assertThat(campo(html, "identificacionLoteProveedor")).isEqualTo("l-125");
    }

    @Test
    @DisplayName("CP-IIC-37: un insumo que el service no encuentra vuelve al formulario directo con el mensaje en el campo del insumo")
    void registrarDirecto_insumoNoEncontrado() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoDirecto(any())).thenThrow(new RecursoNoEncontradoException("No se encontró el insumo con ID: 10", "idInsumo"));

        // Act
        String html = mockMvc.perform(postDirecto(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(texto(html), "No se encontró el insumo con ID: 10")).isEqualTo(2);
        assertThat(html).contains("border-rose-400");
    }

    @Test
    @DisplayName("CP-IIC-38: un formulario directo vacío vuelve al formulario con todos los mensajes y sin llamar al service")
    void registrarDirecto_formularioVacio() throws Exception {
        // Act
        String html = mockMvc.perform(post(URL + "/directo").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("El insumo es obligatorio", "La identificación del lote del proveedor es obligatoria", "La cantidad recibida es obligatoria",
                "El costo unitario es obligatorio", "La fecha de vencimiento es obligatoria", "La fecha de ingreso es obligatoria");
        verify(ingresoInsumoServicio, never()).registrarIngresoInsumoDirecto(any());
    }

    @Test
    @DisplayName("CP-IIC-39: un costo o un insumo que no son números vuelven al formulario directo con un mensaje claro y sin llamar al service")
    void registrarDirecto_valoresDeTipoInvalido() throws Exception {
        // Act
        String html = mockMvc.perform(post(URL + "/directo").with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf()).param("costoUnitario", "x").param("idInsumo", "y"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Ingrese un número válido", "Ingrese un número entero válido");
        verify(ingresoInsumoServicio, never()).registrarIngresoInsumoDirecto(any());
    }

    @Test
    @DisplayName("CP-IIC-40: tras un rechazo, el formulario directo conserva el insumo elegido, su tipo, su unidad y los lotes existentes habilitados")
    void registrarDirecto_rechazoConservaElInsumo() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.registrarIngresoInsumoDirecto(any())).thenThrow(new ReglaNegocioException("La cantidad recibida debe ser mayor a cero"));

        // Act
        String html = mockMvc.perform(postDirecto(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(textoDelSelector(html, "Seleccione insumo")).isEqualTo("Pilsen");
        assertThat(campo(html, "idInsumo")).isEqualTo("10");
        assertThat(texto(bloque(html, "data-insumo-elegido"))).isEqualTo("Tipo: Malta");
        assertThat(texto(html)).contains("Cantidad recibida *", "Precio unitario ($) *");
        assertThat(cuenta(html, "data-unidad-sufijo")).isEqualTo(2);
        assertThat(html).contains(">kg<").contains(">/ kg<");
        assertThat(apertura(html, "data-lote-existente")).doesNotContain("data-deshabilitado").contains("data-buscador-url=\"" + URL_BUSCADOR_LOTES + "?idInsumo=10\"");
    }

    // ==================== POST /ingresos-insumo/{id}/anular ====================

    @Test
    @DisplayName("CP-IIC-41: anular un ingreso por compra envía el motivo al service y vuelve a la pestaña Por compra con la alerta de éxito")
    void anular_ingresoPorCompra() throws Exception {
        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "21", "Error de carga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=compra"))
                .andExpect(flash().attribute("mensaje", "Ingreso de insumo anulado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<AnularIngresoInsumoFormDTO> dto = ArgumentCaptor.forClass(AnularIngresoInsumoFormDTO.class);
        verify(ingresoInsumoServicio).anularIngresoInsumo(eq(21L), dto.capture());
        assertThat(dto.getValue().getMotivoAnulacion()).isEqualTo("Error de carga");
    }

    @Test
    @DisplayName("CP-IIC-42: anular un ingreso directo vuelve a la pestaña Directos")
    void anular_ingresoDirecto() throws Exception {
        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "30", "Error de carga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=directo"))
                .andExpect(flash().attribute("tipo", "success"));
    }

    @Test
    @DisplayName("CP-IIC-43: un motivo en blanco o ausente rechazado por el service vuelve al origen con una alerta amarilla con el motivo del rechazo, sin error")
    void anular_motivoRechazadoPorElService() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.anularIngresoInsumo(eq(21L), any())).thenThrow(new ReglaNegocioException("El motivo de anulación es obligatorio"));

        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "21", "   ").header("Referer", URL + "?tipo=compra"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?tipo=compra"))
                .andExpect(flash().attribute("mensaje", "El motivo de anulación es obligatorio"))
                .andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "21", null).header("Referer", URL + "?tipo=compra"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "warning"));

        ArgumentCaptor<AnularIngresoInsumoFormDTO> dto = ArgumentCaptor.forClass(AnularIngresoInsumoFormDTO.class);
        verify(ingresoInsumoServicio, org.mockito.Mockito.times(2)).anularIngresoInsumo(eq(21L), dto.capture());
        assertThat(dto.getAllValues()).extracting(AnularIngresoInsumoFormDTO::getMotivoAnulacion).containsExactly("   ", null);
    }

    @Test
    @DisplayName("CP-IIC-44: un motivo de más de 255 caracteres rechazado por el service vuelve al origen con el mensaje del service como alerta")
    void anular_motivoDemasiadoLargo() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.anularIngresoInsumo(eq(21L), any())).thenThrow(new ReglaNegocioException("El motivo de anulación no puede superar 255 caracteres"));

        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "21", "x".repeat(256)).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "El motivo de anulación no puede superar 255 caracteres"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-IIC-45: anular un ingreso inexistente o ya anulado vuelve al origen con la alerta del motivo del rechazo")
    void anular_ingresoInexistenteOYaAnulado() throws Exception {
        // Arrange
        when(ingresoInsumoServicio.anularIngresoInsumo(eq(99L), any())).thenThrow(new RecursoNoEncontradoException("No se encontró el ingreso de insumo con ID: 99"));
        when(ingresoInsumoServicio.anularIngresoInsumo(eq(20L), any())).thenThrow(new ReglaNegocioException("Solo se pueden anular ingresos de insumo en estado Registrado"));

        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "99", "Error").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró el ingreso de insumo con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "20", "Error").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Solo se pueden anular ingresos de insumo en estado Registrado"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-IIC-46: un id de ingreso que no es un número vuelve al origen con una alerta amarilla, sin llamar al service")
    void anular_idNoNumerico() throws Exception {
        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.ENCARGADO_DE_DEPOSITO), "abc", "Error").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                .andExpect(flash().attribute("tipo", "warning"));
        verifyNoInteractions(ingresoInsumoServicio);
    }

    // ==================== GET .../compra/buscador/ordenes-compra ====================

    @Test
    @DisplayName("CP-IIC-47: el buscador de órdenes ofrece solo las pendientes de a 8, con la última arriba, con los criterios de proveedor, planificación y fecha y el botón Elegir con el texto de la orden")
    void buscadorOrdenes_soloPendientes() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_ORDENES).with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/buscador-orden-compra :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(ordenCompraServicio).filtrarOrdenesCompra(isNull(), isNull(), eq(EstadoSolicitud.PENDIENTE), isNull(), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(8);
        assertThat(pagina.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(botonElegir(html, 7L)).contains("data-texto=\"N.º 7 - Malterías del Sur S.A.\"");
        assertThat(celdas(html, "data-id=\"7\"")).containsExactly("7", "Plan #5 - Pale Ale", "Malterías del Sur S.A.", "15/07/2027", "Elegir");
        assertThat(html).contains("class=\"tabla-buscador\"").contains("action=\"" + URL_BUSCADOR_ORDENES + "\"")
                .contains("data-buscador-url=\"/ordenes-compra/buscador/proveedores\"").contains("data-buscador-url=\"/ordenes-compra/filtros/planificaciones\"")
                .contains("placeholder=\"Seleccione proveedor\"").contains("placeholder=\"Seleccione planificación\"").contains("placeholder=\"Ej: 15/07/2027\"");
        assertThat(texto(html)).contains("Buscar orden de compra", "Proveedor", "Planificación de producción", "Fecha de entrega estimada");
    }

    @Test
    @DisplayName("CP-IIC-48: los criterios de proveedor, planificación y fecha de entrega viajan al service y se conservan; un proveedor o una planificación inexistentes no restringen")
    void buscadorOrdenes_criterios() throws Exception {
        // Arrange
        when(proveedorServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el proveedor con ID: 99"));

        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_ORDENES).param("idProveedor", "3").param("idPlanificacion", "5").param("fechaEntregaEstimada", "2027-07-15")
                        .with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(get(URL_BUSCADOR_ORDENES).param("idProveedor", "99").param("idPlanificacion", "98").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk());

        // Assert
        verify(ordenCompraServicio).filtrarOrdenesCompra(eq(5L), eq(3L), eq(EstadoSolicitud.PENDIENTE), eq(LocalDate.of(2027, 7, 15)), any(Pageable.class));
        verify(ordenCompraServicio).filtrarOrdenesCompra(isNull(), isNull(), eq(EstadoSolicitud.PENDIENTE), isNull(), any(Pageable.class));
        assertThat(textoDelSelector(html, "Seleccione proveedor")).isEqualTo("Malterías del Sur S.A. (30-71234567-8)");
        assertThat(textoDelSelector(html, "Seleccione planificación")).isEqualTo("Plan #5 - Pale Ale");
        assertThat(campo(html, "fechaEntregaEstimada")).isEqualTo("2027-07-15");
    }

    @Test
    @DisplayName("CP-IIC-49: la paginación del buscador de órdenes pide la página dentro del modal y conserva los criterios; sin resultados muestra el mensaje")
    void buscadorOrdenes_paginacionYVacio() throws Exception {
        // Arrange
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(ordenSiete(EstadoSolicitud.PENDIENTE)), PageRequest.of(1, 8), 20));

        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_ORDENES).param("idProveedor", "3").param("pagina", "1").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        when(ordenCompraServicio.filtrarOrdenesCompra(any(), any(), any(), any(), any())).thenReturn(Page.empty());
        String vacio = mockMvc.perform(get(URL_BUSCADOR_ORDENES).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("data-modal-get=\"true\"");
        assertThat(enlaceDe(html, "Siguiente")).contains(URL_BUSCADOR_ORDENES).contains("pagina=2").contains("idProveedor=3");
        assertThat(texto(vacio)).contains("No se encontraron órdenes de compra pendientes con los criterios indicados");
    }

    // ==================== GET .../compra/buscador/items ====================

    @Test
    @DisplayName("CP-IIC-50: el buscador de ítems ofrece solo los ítems de la orden con cantidad pendiente, por insumo, con pedido, recibido y pendiente en presentaciones")
    void buscadorItems_soloLosPendientes() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "7").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/buscador-item-orden :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Orden de compra N.º 7 - Malterías del Sur S.A.");
        assertThat(texto(html)).doesNotContain("Cascade");
        assertThat(celdas(html, "data-id=\"73\"")).containsExactly("Citra", "Bolsa (1 kg)", "2", "0", "2", "Elegir");
        assertThat(celdas(html, "data-id=\"70\"")).containsExactly("Pilsen", "Bolsa (25 kg)", "4", "1", "3", "Elegir");
        assertThat(celdas(html, "data-id=\"72\"")).containsExactly("Caramelo", "Saco (25 kg)", "2", "0", "2", "Elegir");
        assertThat(html.indexOf("data-id=\"72\"")).isLessThan(html.indexOf("data-id=\"73\""));
        assertThat(html.indexOf("data-id=\"73\"")).isLessThan(html.indexOf("data-id=\"70\""));
        assertThat(html).doesNotContain("data-id=\"71\"");
    }

    @Test
    @DisplayName("CP-IIC-51: el botón Elegir del ítem lleva el insumo, la unidad, lo pedido, recibido y pendiente en presentaciones, la presentación y la cantidad de insumo por presentación (también convertida de kg a g)")
    void buscadorItems_datosDelBotonElegir() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "7").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(botonElegir(html, 70L)).contains("data-texto=\"Pilsen - Bolsa\"").contains("data-insumo-id=\"10\"").contains("data-insumo=\"Pilsen\"")
                .contains("data-unidad=\"kg\"").contains("data-pedido=\"4\"").contains("data-recibido=\"1\"").contains("data-pendiente=\"3 Bolsa\"").contains("data-presentacion=\"Bolsa (25 kg)\"").contains("data-factor=\"25\"").contains("shrink-0");
        assertThat(botonElegir(html, 73L)).contains("data-unidad=\"g\"").contains("data-pendiente=\"2 Bolsa\"").contains("data-factor=\"1000\"");
    }

    @Test
    @DisplayName("CP-IIC-52: sin orden elegida, con una orden que ya no está pendiente o sin ítems pendientes, el buscador de ítems lo informa dentro del modal sin filas")
    void buscadorItems_mensajes() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(10L)).thenReturn(orden(10L, EstadoSolicitud.PENDIENTE, List.of(detalle(100L, 1, 1.0, 0.0, pilsen(), "Bolsa", 25.0, UnidadDeMedida.KILOGRAMO))));

        // Act
        String sinOrden = mockMvc.perform(get(URL_BUSCADOR_ITEMS).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String finalizada = mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "8").with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String sinPendientes = mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "10").with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(sinOrden)).contains("Elija primero la orden de compra");
        assertThat(texto(finalizada)).contains("La orden de compra ya no está pendiente");
        assertThat(texto(sinPendientes)).contains("La orden no tiene ítems pendientes de recibir");
        assertThat(sinOrden + finalizada).doesNotContain("data-elegir");
        verify(ordenCompraServicio, never()).buscarPorId(null);
    }

    @Test
    @DisplayName("CP-IIC-53: el buscador de ítems de una orden inexistente responde 404 con el motivo como texto, que el modal muestra dentro de sí")
    void buscadorItems_ordenInexistente() throws Exception {
        // Arrange
        when(ordenCompraServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la orden de compra con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "99").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("No se encontró la orden de compra con ID: 99"));
    }

    // ==================== GET .../directo/buscador/insumos y .../filtros/insumos ====================

    @Test
    @DisplayName("CP-IIC-54: el buscador de insumos del ingreso directo ofrece insumos de cualquier tipo de a 8 en orden alfabético, con el nombre y el tipo como criterios y la unidad en el botón Elegir")
    void buscadorInsumos_ofreceTodosLosTipos() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_INSUMOS).param("nombre", " pil ").param("tipo", "MALTA").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/buscador-insumo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(insumoServicio).filtrarInsumos(eq("pil"), eq(TipoInsumo.MALTA), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(8);
        assertThat(pagina.getValue().getSort().getOrderFor("nombre")).isNotNull();
        assertThat(botonElegir(html, 10L)).contains("data-texto=\"Pilsen\"").contains("data-tipo=\"Malta\"").contains("data-unidad=\"kg\"");
        assertThat(botonElegir(html, 31L)).contains("data-texto=\"Cascade\"").contains("data-tipo=\"Lúpulo\"").contains("data-unidad=\"g\"");
        assertThat(html).contains("action=\"" + URL_BUSCADOR_INSUMOS + "\"").contains("placeholder=\"Ej: Pilsen\"").contains("class=\"tabla-buscador\"");
        assertThat(campo(html, "nombre")).isEqualTo("pil");
        assertThat(elementoConAtributo(html, "option", "value=\"MALTA\"")).contains("selected");
        assertThat(texto(html)).contains("Malta", "Lúpulo", "Levadura");
    }

    @Test
    @DisplayName("CP-IIC-55: el buscador de insumos del filtro del listado usa su propia URL en el formulario y en la paginación, y un tipo inválido responde 400 con el motivo como texto")
    void filtroInsumos_usaSuPropiaUrl() throws Exception {
        // Arrange
        when(insumoServicio.filtrarInsumos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(pilsen()), PageRequest.of(1, 8), 20));

        // Act
        String html = mockMvc.perform(get(URL_FILTRO_INSUMOS).param("nombre", "pil").param("pagina", "1").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/buscador-insumo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("action=\"" + URL_FILTRO_INSUMOS + "\"");
        assertThat(enlaceDe(html, "Siguiente")).contains(URL_FILTRO_INSUMOS).contains("pagina=2").contains("nombre=pil");
        mockMvc.perform(get(URL_FILTRO_INSUMOS).param("tipo", "XYZ").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    // ==================== GET .../buscador/lotes-insumo ====================

    @Test
    @DisplayName("CP-IIC-56: el buscador de lotes existentes ofrece todos los lotes del insumo, también los agotados y los vencidos, de a 8, con el vencimiento y el stock actual, y el botón Elegir con el vencimiento en ISO")
    void buscadorLotes_incluyeAgotadosYVencidos() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_LOTES).param("idInsumo", "10").param("identificacion", " L-12 ").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andExpect(status().isOk())
                .andExpect(view().name("ingreso_insumo/buscador-lote-existente :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(loteInsumoServicio).filtrarLotesInsumoPorInsumo(eq(10L), eq("L-12"), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(8);
        assertThat(pagina.getValue().getSort().isUnsorted()).isTrue();
        assertThat(celdas(html, "data-id=\"1\"")).containsExactly("L-124", "01/01/2020 Vencido", "0 kg", "Elegir");
        assertThat(celdas(html, "data-id=\"2\"")).containsExactly("L-125", "12/03/2028", "40 kg", "Elegir");
        assertThat(celdas(html, "data-id=\"3\"")).containsExactly("L-126", "01/09/2028", "0 kg", "Elegir");
        assertThat(botonElegir(html, 2L)).contains("data-texto=\"L-125\"").contains("data-vencimiento=\"12/03/2028\"").contains("data-vencimiento-iso=\"2028-03-12\"").contains("shrink-0");
        assertThat(botonElegir(html, 1L)).contains("data-vencido=\"true\"");
        assertThat(campo(html, "idInsumo")).isEqualTo("10");
        assertThat(campo(html, "identificacion")).isEqualTo("L-12");
        assertThat(html).contains("action=\"" + URL_BUSCADOR_LOTES + "\"").contains("placeholder=\"Ej: L-125\"").contains("class=\"tabla-buscador\"");
    }

    @Test
    @DisplayName("CP-IIC-57: sin insumo elegido el buscador de lotes lo informa dentro del modal sin criterios ni filas y sin consultar; sin lotes muestra el mensaje")
    void buscadorLotes_sinInsumoYSinLotes() throws Exception {
        // Act
        String sinInsumo = mockMvc.perform(get(URL_BUSCADOR_LOTES).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        verifyNoInteractions(loteInsumoServicio);
        when(loteInsumoServicio.filtrarLotesInsumoPorInsumo(any(), any(), any())).thenReturn(Page.empty());
        String vacio = mockMvc.perform(get(URL_BUSCADOR_LOTES).param("idInsumo", "10").with(como(Rol.ENCARGADO_DE_DEPOSITO))).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(sinInsumo)).contains("Elija primero el insumo");
        assertThat(sinInsumo).doesNotContain("data-elegir").doesNotContain("data-modal-buscar");
        assertThat(texto(vacio)).contains("No se encontraron lotes del insumo con los criterios indicados");
    }

    @Test
    @DisplayName("CP-IIC-58: la paginación del buscador de lotes pide la página dentro del modal y conserva el insumo y la identificación")
    void buscadorLotes_paginacion() throws Exception {
        // Arrange
        when(loteInsumoServicio.filtrarLotesInsumoPorInsumo(any(), any(), any())).thenReturn(new PageImpl<>(List.of(lote(2L, "L-125", LocalDate.of(2028, 3, 12), 40.0, false)), PageRequest.of(0, 8), 20));

        // Act
        String html = mockMvc.perform(get(URL_BUSCADOR_LOTES).param("idInsumo", "10").param("identificacion", "L-1").with(como(Rol.ENCARGADO_DE_DEPOSITO)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains("data-modal-get=\"true\"");
        assertThat(enlaceDe(html, "Siguiente")).contains(URL_BUSCADOR_LOTES).contains("pagina=1").contains("idInsumo=10").contains("identificacion=L-1");
    }

    // ==================== Seguridad ====================

    /** Un endpoint del controller con el permiso que exige y cómo se pide. */
    private record Endpoint(String nombre, Permiso permiso, Supplier<MockHttpServletRequestBuilder> peticion) {

        @Override
        public String toString() {
            return nombre;
        }
    }

    static Stream<Endpoint> endpoints() {
        return Stream.of(
                new Endpoint("GET lista", Permiso.INGRESO_INSUMO_CONSULTAR, () -> get(URL)),
                new Endpoint("GET filtro de insumos", Permiso.INGRESO_INSUMO_CONSULTAR, () -> get(URL_FILTRO_INSUMOS)),
                new Endpoint("GET formulario por compra", Permiso.INGRESO_INSUMO_REGISTRAR, () -> get(URL_FORMULARIO_COMPRA)),
                new Endpoint("GET formulario directo", Permiso.INGRESO_INSUMO_REGISTRAR, () -> get(URL_FORMULARIO_DIRECTO)),
                new Endpoint("POST registrar por compra", Permiso.INGRESO_INSUMO_REGISTRAR, () -> postCompra(null)),
                new Endpoint("POST registrar directo", Permiso.INGRESO_INSUMO_REGISTRAR, () -> postDirecto(null)),
                new Endpoint("POST anular", Permiso.INGRESO_INSUMO_ANULAR, () -> postAnular(null, "21", "Error")),
                new Endpoint("GET buscador de órdenes", Permiso.INGRESO_INSUMO_REGISTRAR, () -> get(URL_BUSCADOR_ORDENES)),
                new Endpoint("GET buscador de ítems", Permiso.INGRESO_INSUMO_REGISTRAR, () -> get(URL_BUSCADOR_ITEMS).param("idOrdenCompra", "7")),
                new Endpoint("GET buscador de insumos", Permiso.INGRESO_INSUMO_REGISTRAR, () -> get(URL_BUSCADOR_INSUMOS)),
                new Endpoint("GET buscador de lotes", Permiso.INGRESO_INSUMO_REGISTRAR, () -> get(URL_BUSCADOR_LOTES).param("idInsumo", "10")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("CP-IIC-59: el Encargado de Depósito puede usar cada endpoint")
    void seguridad_elEncargadoPuedeUsarCadaEndpoint(Endpoint endpoint) throws Exception {
        // Act
        MvcResult resultado = mockMvc.perform(endpoint.peticion().get().with(como(Rol.ENCARGADO_DE_DEPOSITO)).with(csrf())).andReturn();

        // Assert
        assertThat(resultado.getResponse().getStatus()).isIn(200, 302);
        assertThat(resultado.getFlashMap().get("mensaje")).isNotEqualTo(SIN_PERMISOS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("CP-IIC-60: ningún otro rol puede usar un endpoint: vuelve al origen con la alerta de permisos y sin llamar a ningún service")
    void seguridad_losOtrosRolesQuedanAfuera(Endpoint endpoint) throws Exception {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.ENCARGADO_DE_DEPOSITO) {
                continue;
            }
            // Act + Assert
            mockMvc.perform(endpoint.peticion().get().with(como(rol)).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"))
                    .andExpect(flash().attribute("mensaje", SIN_PERMISOS));
        }
        verifyNoInteractions(ingresoInsumoServicio, ordenCompraServicio, insumoServicio, loteInsumoServicio, proveedorServicio, planificacionServicio);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    @DisplayName("CP-IIC-61: cada endpoint exige su propio permiso: con los otros dos permisos del módulo se rechaza y con el suyo solo se permite")
    void seguridad_cadaEndpointExigeSuPermiso(Endpoint endpoint) throws Exception {
        // Arrange
        Permiso[] otros = Arrays.stream(new Permiso[]{Permiso.INGRESO_INSUMO_CONSULTAR, Permiso.INGRESO_INSUMO_REGISTRAR, Permiso.INGRESO_INSUMO_ANULAR})
                .filter(permiso -> permiso != endpoint.permiso()).toArray(Permiso[]::new);

        // Act + Assert
        mockMvc.perform(endpoint.peticion().get().with(conPermisos(otros)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", SIN_PERMISOS));
        MvcResult permitido = mockMvc.perform(endpoint.peticion().get().with(conPermisos(endpoint.permiso())).with(csrf())).andReturn();
        assertThat(permitido.getResponse().getStatus()).isIn(200, 302);
        assertThat(permitido.getFlashMap().get("mensaje")).isNotEqualTo(SIN_PERMISOS);
    }

    @Test
    @DisplayName("CP-IIC-62: la sidebar ofrece Ingresos de Insumo solo con el permiso de consultarlos")
    void seguridad_laSidebarSoloLoOfreceConElPermiso() throws Exception {
        // Act
        String conPermiso = html(Rol.ENCARGADO_DE_DEPOSITO);
        RequestPostProcessor sinConsulta = user("usuario").authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO_DE_DEPOSITO"),
                new SimpleGrantedAuthority(Permiso.INGRESO_INSUMO_REGISTRAR.name()));
        String sinPermiso = mockMvc.perform(get(URL_FORMULARIO_DIRECTO).with(sinConsulta)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(conPermiso).contains("href=\"/ingresos-insumo\"");
        assertThat(sinPermiso).doesNotContain("bi-box-arrow-in-down");
        assertThat(sinPermiso).doesNotContain("href=\"/ingresos-insumo\"");
    }

    @Test
    @DisplayName("CP-IIC-63: sin iniciar sesión cada endpoint redirige al login, y un POST sin token CSRF se rechaza sin llamar al service")
    void seguridad_sinSesionYSinCsrf() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL_FORMULARIO_COMPRA)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(postCompra(null).with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(postAnular(null, "21", "Error").with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(postCompra(null).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().is3xxRedirection());
        mockMvc.perform(postDirecto(null).with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().is3xxRedirection());
        mockMvc.perform(postAnular(null, "21", "Error").with(como(Rol.ENCARGADO_DE_DEPOSITO))).andExpect(status().is3xxRedirection());
        verifyNoInteractions(ingresoInsumoServicio);
    }

    // ==================== Apoyo de las pruebas ====================

    private String html(Rol rol) throws Exception {
        return mockMvc.perform(get(URL).with(como(rol))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static MockHttpServletRequestBuilder postCompra(RequestPostProcessor usuario) {
        MockHttpServletRequestBuilder peticion = post(URL + "/compra").param("idDetalleCompra", "70").param("identificacionLoteProveedor", "l-125")
                .param("fechaIngreso", HOY.toString()).param("cantidadPresentaciones", "2").param("fechaVencimiento", HOY.plusYears(1).toString());
        return usuario == null ? peticion : peticion.with(usuario).with(csrf());
    }

    private static MockHttpServletRequestBuilder postDirecto(RequestPostProcessor usuario) {
        MockHttpServletRequestBuilder peticion = post(URL + "/directo").param("idInsumo", "10").param("identificacionLoteProveedor", "l-125")
                .param("fechaIngreso", HOY.toString()).param("cantidadRecibida", "25.5").param("costoUnitario", "12.5").param("fechaVencimiento", HOY.plusYears(1).toString());
        return usuario == null ? peticion : peticion.with(usuario).with(csrf());
    }

    private static MockHttpServletRequestBuilder postAnular(RequestPostProcessor usuario, String id, String motivo) {
        MockHttpServletRequestBuilder peticion = post(URL + "/" + id + "/anular");
        if (motivo != null) {
            peticion.param("motivo", motivo);
        }
        return usuario == null ? peticion : peticion.with(usuario).with(csrf());
    }

    private void stubIngresos(Page<IngresoInsumoResponseDTO> pagina) {
        when(ingresoInsumoServicio.filtrarIngresoInsumo(any(), any(), any(), any(), any(), any())).thenReturn(pagina);
    }

    private static Page<IngresoInsumoResponseDTO> paginaDe(IngresoInsumoResponseDTO... ingresos) {
        return new PageImpl<>(List.of(ingresos), PageRequest.of(0, 20), ingresos.length);
    }

    /** Lo que muestra la pantalla dentro del layout, sin el encabezado (que tiene su propio formulario de cierre de sesión). */
    private static String contenido(String html) {
        return html.substring(html.indexOf("<main"));
    }

    // ---------- Usuarios ----------

    /** Un usuario con las authorities que le da el login: ROLE_ del rol y cada uno de sus permisos. */
    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user("encargado").authorities(autoridades);
    }

    /** Un usuario con solo esos permisos, sin ningún rol. */
    private static RequestPostProcessor conPermisos(Permiso... permisos) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        Arrays.stream(permisos).forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user("usuario").authorities(autoridades);
    }

    // ---------- Datos de prueba ----------

    private static InsumoResponseDTO pilsen() {
        return MaltaResponseDTO.builder().id(10L).nombre("Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
    }

    private static InsumoResponseDTO cascade() {
        return LupuloResponseDTO.builder().id(31L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static VersionProveedorResponseDTO proveedor() {
        return VersionProveedorResponseDTO.builder().id(3L).razonSocial("Malterías del Sur S.A.").cuit("30712345678").build();
    }

    private static LoteInsumoResponseDTO lote(Long id, String identificacion, LocalDate vencimiento, double actual, boolean vencido) {
        return LoteInsumoResponseDTO.builder().id(id).insumo(pilsen()).identificacionLoteProveedor(identificacion).fechaVencimiento(vencimiento)
                .cantidadActual(actual).cantidadDisponible(actual).vencido(vencido).build();
    }

    private static IngresoInsumoResponseDTO ingresoCompra(Long id, EstadoTransaccion estado, String motivo) {
        return ingresoCompra(id, estado, motivo, 7L);
    }

    private static IngresoInsumoResponseDTO ingresoCompra(Long id, EstadoTransaccion estado, String motivo, Long idOrden) {
        return IngresoInsumoResponseDTO.builder().id(id).tipoIngreso(TipoIngreso.COMPRA).estado(estado).fechaIngreso(LocalDate.of(2027, 7, 1))
                .cantidadRecibida(50.0).cantidadPresentaciones(2.0).costoUnitario(new BigDecimal("480")).idOrdenCompra(idOrden)
                .detalleCompra(DetalleCompraResponseDTO.builder().id(70L).cantidad(4).costoUnitario(new BigDecimal("12000"))
                        .catalogoProveedor(CatalogoProveedorResponseDTO.builder().id(1070L).insumo(pilsen())
                                .presentacionComercial(PresentacionComercialResponseDTO.builder().id(2070L).nombre("Bolsa 25 kg").cantidad(25.0)
                                        .unidadDeMedida(UnidadDeMedida.KILOGRAMO).build()).build()).build())
                .motivoAnulacion(motivo).usuarioAnulacion(motivo == null ? null : "encargado")
                .fechaAnulacion(motivo == null ? null : LocalDateTime.of(2027, 7, 2, 9, 30))
                .loteInsumo(lote(2L, "L-125", LocalDate.of(2028, 3, 12), 40.0, false)).build();
    }

    private static IngresoInsumoResponseDTO ingresoDirecto(Long id, EstadoTransaccion estado) {
        return IngresoInsumoResponseDTO.builder().id(id).tipoIngreso(TipoIngreso.DIRECTO).estado(estado).fechaIngreso(LocalDate.of(2027, 7, 1))
                .cantidadRecibida(50.0).costoUnitario(new BigDecimal("12.5"))
                .loteInsumo(lote(2L, "L-125", LocalDate.of(2028, 3, 12), 40.0, false)).build();
    }

    /** Orden 7 de Malterías del Sur, para el plan 5: Pilsen (3 de 4 bolsas pendientes), Cascade (sin pendiente), Caramelo y Citra. */
    private static OrdenCompraResponseDTO ordenSiete(EstadoSolicitud estado) {
        InsumoResponseDTO caramelo = MaltaResponseDTO.builder().id(11L).nombre("Caramelo").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        InsumoResponseDTO citra = LupuloResponseDTO.builder().id(32L).nombre("Citra").unidadDeMedida(UnidadDeMedida.GRAMO).build();
        return orden(7L, estado, List.of(
                detalle(70L, 4, 1.0, 3.0, pilsen(), "Bolsa", 25.0, UnidadDeMedida.KILOGRAMO),
                detalle(71L, 2, 2.0, 0.0, cascade(), "Pack", 500.0, UnidadDeMedida.GRAMO),
                detalle(72L, 2, 0.0, 2.0, caramelo, "Saco", 25.0, UnidadDeMedida.KILOGRAMO),
                detalle(73L, 2, 0.0, 2.0, citra, "Bolsa", 1.0, UnidadDeMedida.KILOGRAMO)));
    }

    private static OrdenCompraResponseDTO orden(Long id, EstadoSolicitud estado, List<DetalleCompraResponseDTO> detalles) {
        return OrdenCompraResponseDTO.builder().id(id).estado(estado).fechaEntregaEstimada(LocalDate.of(2027, 7, 15))
                .planificacionProduccion(PlanificacionProduccionResponseDTO.builder().id(5L).versionReceta(VersionRecetaResponseDTO.builder().nombre("Pale Ale").build()).build())
                .proveedor(proveedor()).detallesCompra(detalles).total(new BigDecimal("1000")).build();
    }

    private static DetalleCompraResponseDTO detalle(Long id, int cantidad, double recibida, double pendiente, InsumoResponseDTO insumo, String presentacion,
                                                     double contenido, UnidadDeMedida unidad) {
        return DetalleCompraResponseDTO.builder().id(id).cantidad(cantidad).costoUnitario(new BigDecimal("10")).cantidadRecibida(recibida).cantidadPendiente(pendiente)
                .catalogoProveedor(CatalogoProveedorResponseDTO.builder().id(id + 1000).insumo(insumo)
                        .presentacionComercial(PresentacionComercialResponseDTO.builder().id(id + 2000).nombre(presentacion).cantidad(contenido).unidadDeMedida(unidad).build()).build())
                .build();
    }

    // ---------- Lectura del HTML ----------

    /** El texto visible: sin etiquetas, comentarios ni scripts, con las entidades resueltas y los espacios colapsados. */
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

    /** El elemento (de apertura a cierre) que lleva el atributo indicado. */
    private static String bloque(String html, String atributo) {
        Matcher m = Pattern.compile("(?s)<(\\w+)\\b[^>]*\\s" + Pattern.quote(atributo) + "(?=[\\s>=])[^>]*>.*?</\\1>").matcher(html);
        if (!m.find()) {
            m = Pattern.compile("(?s)<(" + Pattern.quote(atributo) + ")\\b[^>]*>.*?</\\1>").matcher(html);
            assertThat(m.find()).as("elemento con %s", atributo).isTrue();
        }
        return m.group();
    }

    /** La etiqueta de apertura del primer elemento que lleva el atributo indicado. */
    private static String apertura(String html, String atributo) {
        Matcher m = Pattern.compile("<\\w+\\b[^>]*\\s" + Pattern.quote(atributo) + "(?=[\\s>=])[^>]*>").matcher(html);
        assertThat(m.find()).as("elemento con %s", atributo).isTrue();
        return m.group();
    }

    /** El elemento de esa etiqueta que lleva el atributo con su valor (por ejemplo, {@code data-ingreso="21"}). */
    private static String elementoConAtributo(String html, String etiqueta, String atributoConValor) {
        Matcher m = Pattern.compile("(?s)<" + etiqueta + "\\b[^>]*\\s" + Pattern.quote(atributoConValor) + "[^>]*>.*?</" + etiqueta + ">").matcher(html);
        assertThat(m.find()).as("%s con %s", etiqueta, atributoConValor).isTrue();
        return m.group();
    }

    /** El texto de cada celda de la primera fila cuyo contenido incluye la marca. */
    private static List<String> celdas(String html, String marca) {
        Matcher filas = Pattern.compile("(?s)<tr[^>]*>(.*?)</tr>").matcher(html);
        while (filas.find()) {
            if (filas.group(0).contains(marca)) {
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

    /** El destino (sin escapar) del enlace cuyo texto incluye la etiqueta. */
    private static String enlaceDe(String html, String etiqueta) {
        Matcher m = Pattern.compile("(?s)<a\\s[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>").matcher(html);
        while (m.find()) {
            if (texto(m.group(2)).contains(etiqueta)) {
                return HtmlUtils.htmlUnescape(m.group(1));
            }
        }
        throw new AssertionError("No hay un enlace " + etiqueta);
    }

    /** El texto que muestra el selector de entidad con ese placeholder (vacío si no hay nada elegido). */
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

    /** El valor del primer campo con ese nombre. */
    private static String campo(String html, String nombre) {
        Matcher m = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("campo %s", nombre).isTrue();
        Matcher valor = Pattern.compile("value=\"([^\"]*)\"").matcher(m.group());
        return valor.find() ? HtmlUtils.htmlUnescape(valor.group(1)) : "";
    }

    /** La etiqueta del campo con ese nombre (para ver si está marcado con error). */
    private static String etiquetaDelCampo(String html, String nombre) {
        Matcher m = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("campo %s", nombre).isTrue();
        return m.group();
    }

    /** El botón Elegir de la fila con ese id. */
    private static String botonElegir(String html, long id) {
        Matcher m = Pattern.compile("<button[^>]*data-id=\"" + id + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("botón Elegir %s", id).isTrue();
        return HtmlUtils.htmlUnescape(m.group());
    }
}
