package com.github.heikyudev.maestrocervecero.presentation.controller.stock_insumo;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo.IStockInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.CantidadesStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(StockInsumoController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class StockInsumoControllerTest {

    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final List<Rol> ROLES_CON_ACCESO = List.of(Rol.GERENTE_DE_PRODUCCION, Rol.GERENTE_DE_COMPRAS, Rol.OPERARIO_DE_PRODUCCION, Rol.ENCARGADO_DE_DEPOSITO);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IStockInsumoServicio servicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /**
     * Escenario base: insumo 1 "Malta Pilsen" (kg) con disponible 6, reservada 4, vencida 7 y total 17; lote de insumo 10
     * "L-125" vigente con una reserva activa (20) y un consumo directo anulado (30) del lote de producción 12 "LOT-R1-02".
     */
    @BeforeEach
    void escenarioBase() {
        when(servicio.filtrarInsumos(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(insumo())));
        when(servicio.obtenerStockInsumo(1L)).thenReturn(insumo());
        when(servicio.obtenerStockInsumo(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el insumo con ID: 99"));
        when(servicio.filtrarLotesDeInsumo(eq(1L), any())).thenReturn(new PageImpl<>(List.of(
                StockLoteInsumoResponseDTO.builder().id(10L).identificacionLoteProveedor("L-125").fechaVencimiento(LocalDate.of(2026, 12, 1)).vencido(false)
                        .cantidades(cantidades(6.0, 4.0, 0.0, 10.0)).build(),
                StockLoteInsumoResponseDTO.builder().id(11L).identificacionLoteProveedor("L-VIEJO").fechaVencimiento(LocalDate.of(2026, 1, 1)).vencido(true)
                        .cantidades(cantidades(0.0, 0.0, 7.0, 7.0)).build())));
        when(servicio.obtenerDetalleLoteInsumo(10L)).thenReturn(StockLoteInsumoDetalleResponseDTO.builder().id(10L).idInsumo(1L).nombreInsumo("Malta Pilsen")
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO).identificacionLoteProveedor("L-125").fechaVencimiento(LocalDate.of(2026, 12, 1)).vencido(false)
                .cantidades(cantidades(6.0, 4.0, 0.0, 10.0)).build());
        when(servicio.obtenerDetalleLoteInsumo(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: 99"));
        when(servicio.filtrarReservasDeLoteInsumo(eq(10L), any())).thenReturn(new PageImpl<>(List.of(
                ReservaInsumoResponseDTO.builder().id(20L).cantidadReservada(5.0).cantidadConsumida(1.0).cantidadLiberada(0.0).cantidadReducidaPorAjuste(0.0)
                        .cantidadPendiente(4.0).estado(EstadoReservaInsumo.ACTIVA).etapaLote(etapa()).build())));
        when(servicio.filtrarConsumosDeLoteInsumo(eq(10L), any())).thenReturn(new PageImpl<>(List.of(
                ConsumoInsumoResponseDTO.builder().id(30L).cantidadConsumida(2.0).costoUnitarioPPP(BigDecimal.ONE).estado(EstadoTransaccion.ANULADO)
                        .tipoConsumo(TipoConsumo.DIRECTO).etapaLote(etapa()).fechaRegistro(LocalDateTime.of(2026, 10, 14, 9, 30))
                        .motivoAnulacion("Error de carga").usuarioAnulacion("operario1").fechaAnulacion(LocalDateTime.of(2026, 10, 14, 10, 0)).build())));
    }

    // ==================== Lista ====================

    @Test
    @DisplayName("CP-CSI-01: la lista muestra las columnas, las cantidades con la unidad del insumo, el botón Abrir y ningún distintivo Vencido")
    void lista_columnas() throws Exception {
        String html = html(get("/stock-insumos").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-lista");

        assertThat(html).contains("Stock de Insumos").contains("Malta Pilsen").contains("Malta").contains(">6 kg<").contains(">4 kg<").contains(">7 kg<").contains(">17 kg<")
                .contains("Disponible").contains("Reservada").contains("Vencida").contains("Total").doesNotContain(">Unidad</th>").contains("Acciones")
                .contains("data-cantidad-disponible").contains("data-cantidad-total")
                .contains("data-boton-abrir").contains("/stock-insumos/1").contains("Abrir Malta Pilsen")
                .contains("Ej: Pilsen").contains("Limpiar").contains("Buscar")
                .doesNotContain("Vencido");
    }

    @Test
    @DisplayName("CP-CSI-02: los criterios de la lista se pasan al service (nombre sin espacios, tipo, situación), de a 20, y se conservan en el formulario")
    void lista_filtros() throws Exception {
        String html = html(get("/stock-insumos").param("nombre", " pils ").param("tipo", "MALTA").param("situacionStock", "CON_VENCIDOS")
                .with(como(Rol.OPERARIO_DE_PRODUCCION)), "stock_insumo/stock-lista");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarInsumos(eq("pils"), eq(TipoInsumo.MALTA), eq(SituacionStockInsumo.CON_VENCIDOS), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(0);
        assertThat(html).contains("value=\" pils \"").contains("<option value=\"MALTA\" selected").contains("<option value=\"CON_VENCIDOS\" selected");
    }

    @Test
    @DisplayName("CP-CSI-03: sin criterios no se restringe por ninguno y el nombre en blanco equivale a no filtrar")
    void lista_sinCriterios() throws Exception {
        html(get("/stock-insumos").param("nombre", "   ").with(como(Rol.GERENTE_DE_COMPRAS)), "stock_insumo/stock-lista");

        verify(servicio).filtrarInsumos(isNull(), isNull(), isNull(), any());
    }

    @Test
    @DisplayName("CP-CSI-04: una página negativa se acota a la primera y un tipo o situación inválidos redirigen con una alerta de advertencia sin consultar el service")
    void lista_parametrosInvalidos() throws Exception {
        html(get("/stock-insumos").param("pagina", "-4").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-lista");
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarInsumos(any(), any(), any(), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(0);

        mockMvc.perform(get("/stock-insumos").param("tipo", "CERVEZA").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/stock-insumos"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(get("/stock-insumos").param("situacionStock", "NADA").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/stock-insumos"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "warning"));
        mockMvc.perform(get("/stock-insumos").param("pagina", "abc").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/stock-insumos"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("CP-CSI-05: la lista vacía muestra el mensaje de sin resultados")
    void lista_vacia() throws Exception {
        when(servicio.filtrarInsumos(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        String html = html(get("/stock-insumos").param("nombre", "zzz").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-lista");

        assertThat(html).contains("No se encontraron insumos con los criterios indicados");
    }

    @Test
    @DisplayName("CP-CSI-06: los enlaces de paginación de la lista conservan los criterios")
    void lista_paginacion() throws Exception {
        when(servicio.filtrarInsumos(any(), any(), any(), any())).thenReturn(new PageImpl<>(java.util.Collections.nCopies(20, insumo()), PageRequest.of(0, 20), 45));

        String html = html(get("/stock-insumos").param("nombre", "pils").param("tipo", "MALTA").param("situacionStock", "CON_STOCK_DISPONIBLE")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-lista");

        assertThat(html).contains("<span>45</span> <span>insumos</span>").contains("Página <span>1</span> de <span>3</span>").contains("pagina=1").contains("nombre=pils").contains("tipo=MALTA").contains("situacionStock=CON_STOCK_DISPONIBLE");
    }

    // ==================== Permisos ====================

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_DISTRIBUCION", "ADMINISTRADOR"})
    @DisplayName("CP-CSI-07: el encargado de distribución y el administrador no abren ninguna pantalla del módulo: redirigen con alerta y no se invoca el service")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String url : List.of("/stock-insumos", "/stock-insumos/1", "/stock-insumos/lotes/10")) {
            mockMvc.perform(get(url).with(como(rol))).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("mensaje", SIN_PERMISOS)).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CSI-08: los cuatro roles con el permiso abren las tres pantallas")
    void rolesAutorizados() throws Exception {
        for (Rol rol : ROLES_CON_ACCESO) {
            for (String url : List.of("/stock-insumos", "/stock-insumos/1", "/stock-insumos/lotes/10")) {
                mockMvc.perform(get(url).with(como(rol))).andExpect(status().isOk());
            }
        }
    }

    @Test
    @DisplayName("CP-CSI-09: cada método del Controller exige el permiso STOCK_INSUMO_CONSULTAR")
    void preAuthorize() {
        int metodos = 0;
        for (Method metodo : StockInsumoController.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(metodo.getModifiers())) {
                metodos++;
                PreAuthorize anotacion = metodo.getAnnotation(PreAuthorize.class);
                assertThat(anotacion).as(metodo.getName()).isNotNull();
                assertThat(anotacion.value()).isEqualTo("hasAuthority('STOCK_INSUMO_CONSULTAR')");
            }
        }
        assertThat(metodos).isEqualTo(3);
    }

    // ==================== Detalle del insumo ====================

    @Test
    @DisplayName("CP-CSI-10: el detalle del insumo muestra el resumen con las cuatro cantidades y la unidad, y la tabla de lotes con sus columnas y el botón Abrir")
    void detalleInsumo() throws Exception {
        String html = html(get("/stock-insumos/1").with(como(Rol.ENCARGADO_DE_DEPOSITO)), "stock_insumo/stock-detalle-insumo");

        assertThat(html).contains("Stock de <span>Malta Pilsen</span>").contains("Volver a stock de insumos")
                .contains("data-cantidades").contains("6 kg").contains("4 kg").contains("7 kg").contains("17 kg")
                .contains("Lote del proveedor").contains("Vence").contains("Disponible").contains("Reservada").contains("Vencida").contains("Total")
                .contains("L-125").contains("01/12/2026").contains("data-boton-abrir").contains("/stock-insumos/lotes/10").contains("Abrir lote L-125")
                .doesNotContain("Vencido");
    }

    @Test
    @DisplayName("CP-CSI-11: los lotes del insumo se muestran en el orden que entrega el service (FEFO), también los vencidos y sin stock")
    void detalleInsumo_ordenFefo() throws Exception {
        String html = html(get("/stock-insumos/1").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-detalle-insumo");

        assertThat(html.indexOf("L-125")).isLessThan(html.indexOf("L-VIEJO"));
        assertThat(html).contains("L-VIEJO").contains("01/01/2026");
    }

    @Test
    @DisplayName("CP-CSI-12: el detalle del insumo pide la página con tamaño 20 y la acota cuando es negativa")
    void detalleInsumo_pagina() throws Exception {
        html(get("/stock-insumos/1").param("pagina", "-1").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-detalle-insumo");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarLotesDeInsumo(eq(1L), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("CP-CSI-13: un insumo inexistente o dado de baja, o un ID que no es número, redirigen con alerta")
    void detalleInsumo_inexistente() throws Exception {
        mockMvc.perform(get("/stock-insumos/99").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/stock-insumos"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get("/stock-insumos/abc").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/stock-insumos"))
                .andExpect(status().is3xxRedirection());
    }

    // ==================== Detalle del lote de insumo ====================

    @Test
    @DisplayName("CP-CSI-14: el detalle del lote muestra la cabecera con las cuatro cantidades y las reservas con todas sus columnas en la pestaña por defecto")
    void detalleLote_reservas() throws Exception {
        String html = html(get("/stock-insumos/lotes/10").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-detalle-lote");

        assertThat(html).contains("Lote <span>L-125</span>").contains("Volver a <span>Malta Pilsen</span>").contains("/stock-insumos/1")
                .contains("data-cantidades").contains("6 kg").contains("10 kg")
                .contains("data-seccion-reservas").contains("Lote de producción").contains("Etapa").contains("Molienda").contains("Reservada").contains("Consumida")
                .contains("Liberada").contains("Reducida por ajuste").contains("Pendiente").contains("Estado").contains("Activa")
                .doesNotContain("data-seccion-consumos").doesNotContain("Vencido");
        verify(servicio, never()).filtrarConsumosDeLoteInsumo(any(), any());
    }

    @Test
    @DisplayName("CP-CSI-15: la pestaña Consumos muestra sus columnas, el tipo, el estado y el botón i del detalle de la anulación, y no consulta las reservas")
    void detalleLote_consumos() throws Exception {
        String html = html(get("/stock-insumos/lotes/10").param("pestana", "consumos").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-detalle-lote");

        assertThat(html).contains("data-seccion-consumos").contains("Lote de producción").contains("Etapa").contains("Cantidad").contains("Tipo").contains("Fecha")
                .contains("Directo").contains("Anulado").contains("14/10/2026 09:30").contains("data-detalle-anulacion").contains("Error de carga")
                .doesNotContain("data-seccion-reservas");
        verify(servicio, never()).filtrarReservasDeLoteInsumo(any(), any());
    }

    @Test
    @DisplayName("CP-CSI-16: una pestaña desconocida equivale a Reservas")
    void detalleLote_pestanaDesconocida() throws Exception {
        String html = html(get("/stock-insumos/lotes/10").param("pestana", "otra").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-detalle-lote");

        assertThat(html).contains("data-seccion-reservas").doesNotContain("data-seccion-consumos");
        verify(servicio, never()).filtrarConsumosDeLoteInsumo(any(), any());
    }

    @Test
    @DisplayName("CP-CSI-17: el lote de producción es un enlace al panel del lote solo para quien tiene LOTE_CONSULTAR")
    void detalleLote_enlaceAlLoteDeProduccion() throws Exception {
        for (String pestana : List.of("reservas", "consumos")) {
            for (Rol rol : List.of(Rol.GERENTE_DE_PRODUCCION, Rol.OPERARIO_DE_PRODUCCION)) {
                String html = html(get("/stock-insumos/lotes/10").param("pestana", pestana).with(como(rol)), "stock_insumo/stock-detalle-lote");
                assertThat(html).as(pestana + " " + rol).contains("data-lote-produccion").contains("href=\"/lotes/12\"").contains("LOT-R1-02");
            }
            for (Rol rol : List.of(Rol.GERENTE_DE_COMPRAS, Rol.ENCARGADO_DE_DEPOSITO)) {
                String html = html(get("/stock-insumos/lotes/10").param("pestana", pestana).with(como(rol)), "stock_insumo/stock-detalle-lote");
                assertThat(html).as(pestana + " " + rol).contains("LOT-R1-02").doesNotContain("data-lote-produccion").doesNotContain("/lotes/12");
            }
        }
    }

    @Test
    @DisplayName("CP-CSI-18: las pestañas del lote conservan la pestaña en la paginación y pide páginas de 20")
    void detalleLote_paginacion() throws Exception {
        when(servicio.filtrarConsumosDeLoteInsumo(eq(10L), any())).thenReturn(new PageImpl<>(java.util.Collections.nCopies(20,
                ConsumoInsumoResponseDTO.builder().id(30L).cantidadConsumida(2.0).estado(EstadoTransaccion.REGISTRADO).tipoConsumo(TipoConsumo.RESERVADO).etapaLote(etapa()).build()),
                PageRequest.of(0, 20), 41));

        String html = html(get("/stock-insumos/lotes/10").param("pestana", "consumos").param("pagina", "0").with(como(Rol.GERENTE_DE_PRODUCCION)), "stock_insumo/stock-detalle-lote");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarConsumosDeLoteInsumo(eq(10L), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(html).contains("<span>41</span> <span>consumos</span>").contains("pestana=consumos").contains("pagina=1");
    }

    @Test
    @DisplayName("CP-CSI-19: un lote de insumo inexistente o de un insumo dado de baja redirige con alerta")
    void detalleLote_inexistente() throws Exception {
        mockMvc.perform(get("/stock-insumos/lotes/99").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/stock-insumos"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-CSI-20: las pantallas muestran la opción Stock de Insumos del menú, activa, a los cuatro roles con el permiso")
    void menu() throws Exception {
        for (Rol rol : ROLES_CON_ACCESO) {
            String html = html(get("/stock-insumos").with(como(rol)), "stock_insumo/stock-lista");
            assertThat(html).as(rol.name()).contains("Stock de Insumos").contains("href=\"/stock-insumos\"").contains("<title>Stock de insumos - Maestro Cervecero</title>");
        }
    }

    // ==================== Apoyo ====================

    private static StockInsumoResponseDTO insumo() {
        return StockInsumoResponseDTO.builder().id(1L).nombre("Malta Pilsen").tipo(TipoInsumo.MALTA).unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .cantidades(cantidades(6.0, 4.0, 7.0, 17.0)).build();
    }

    private static CantidadesStockResponseDTO cantidades(double disponible, double reservada, double vencida, double total) {
        return CantidadesStockResponseDTO.builder().disponible(disponible).reservada(reservada).vencida(vencida).total(total).build();
    }

    private static EtapaLoteResponseDTO etapa() {
        return EtapaLoteResponseDTO.builder().id(100L).etapa(TipoEtapa.MOLIENDA).idLote(12L).identificadorInternoLote("LOT-R1-02").build();
    }

    private String html(MockHttpServletRequestBuilder request, String vista) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andExpect(view().name(vista))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
