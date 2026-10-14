package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IReservaInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
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

@WebMvcTest(ConsumoInsumoController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class ConsumoInsumoControllerTest {

    private static final String URL_PANTALLA = "/lotes/12/etapas/hervido/consumos";
    private static final String URL_BUSCADOR = URL_PANTALLA + "/buscador/lotes-insumo";
    private static final String ALERTA_ADVERTENCIA = "bg-amber-50 border-amber-200 text-amber-800";
    private static final String ALERTA_EXITO = "bg-teal-50 border-teal-200 text-teal-800";
    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final String VENCIDO = "Este lote de insumo se encuentra vencido (venció el 01/01/2020)";
    private static final String EXCESO = "El total consumido de Citra en esta etapa (20 g) supera la cantidad requerida (15 g)";
    private static final long ETAPA_HERVIDO = 33L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILoteServicio loteServicio;

    @MockitoBean
    private IConsumoInsumoServicio consumoInsumoServicio;

    @MockitoBean
    private IReservaInsumoServicio reservaInsumoServicio;

    @MockitoBean
    private ILoteInsumoServicio loteInsumoServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /**
     * Escenario base: lote 12 "PALE_ALE-R7-3" En ejecución con el Hervido (etapa 33) En curso, mínimo de consumo 80 %,
     * Cascade (reservas "L-123" con saldo y "L-124" agotada) y Citra (reserva vencida "C-900"), y los consumos 7 y 6.
     * Cada caso cambia solo lo que dice.
     */
    @BeforeEach
    void escenarioBase() {
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, etapasEnCurso(TipoEtapa.HERVIDO)));
        when(loteServicio.calcularAvanceDeEtapa(12L, TipoEtapa.HERVIDO)).thenReturn(AvanceEtapaResponseDTO.builder().porcentajeMinimo(80.0).cumpleMinimo(false)
                .insumos(List.of(avance("Cascade", 25, 15, 60, false), avance("Citra", 15, 0, 0, false))).build());
        when(consumoInsumoServicio.filtrarInsumosRequeridos(ETAPA_HERVIDO)).thenReturn(List.of(
                requerido(cascade(), 25, 10, 15), requerido(citra(), 15, 15, 0)));
        when(reservaInsumoServicio.filtrarLotesInsumoReservados(ETAPA_HERVIDO, 31L)).thenReturn(List.of(
                reserva(1L, loteInsumo(5L, cascade(), "L-123", LocalDate.of(2028, 3, 12), 10, false), 10),
                reserva(2L, loteInsumo(6L, cascade(), "L-124", null, 0, false), 0)));
        when(reservaInsumoServicio.filtrarLotesInsumoReservados(ETAPA_HERVIDO, 32L)).thenReturn(List.of(
                reserva(3L, loteInsumo(7L, citra(), "C-900", LocalDate.of(2020, 1, 1), 15, true), 15)));
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                consumo(7L, ETAPA_HERVIDO, cascade(), "L-123", TipoConsumo.DIRECTO, 15, EstadoTransaccion.REGISTRADO, null),
                consumo(6L, ETAPA_HERVIDO, cascade(), "L-124", TipoConsumo.RESERVADO, 5, EstadoTransaccion.ANULADO, "Error de carga"))));
        when(consumoInsumoServicio.buscarPorId(7L)).thenReturn(consumo(7L, ETAPA_HERVIDO, cascade(), "L-123", TipoConsumo.DIRECTO, 15, EstadoTransaccion.REGISTRADO, null));
        when(consumoInsumoServicio.buscarPorId(8L)).thenReturn(consumo(8L, 31L, cascade(), "L-123", TipoConsumo.DIRECTO, 15, EstadoTransaccion.REGISTRADO, null));
        when(consumoInsumoServicio.buscarPorId(15L)).thenReturn(consumo(15L, 66L, cascade(), "L-123", TipoConsumo.DIRECTO, 15, EstadoTransaccion.REGISTRADO, null));
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(any(), any())).thenReturn(respuestaDeConsumo());
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenReturn(respuestaDeConsumo());
        when(consumoInsumoServicio.anularConsumoInsumo(any(), any())).thenReturn(respuestaDeConsumo());
        when(loteInsumoServicio.filtrarLotesInsumoDisponibles(any(Collection.class), any(), any(), any())).thenReturn(Page.empty());
        when(loteInsumoServicio.buscarPorId(8L)).thenReturn(loteInsumo(8L, cascade(), "L-125", LocalDate.of(2028, 3, 12), 40, false));
    }

    // ==================== GET /lotes/{idLote}/etapas/{etapa}/consumos ====================

    @Test
    @DisplayName("CP-LCO-01: la pantalla de consumos es una página con el título, el link Volver al lote, el código, el estado de la etapa y los cuatro bloques, sin diálogo modal para registrar")
    void pantalla_debeMostrarLosCuatroBloques() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/consumo-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Consumos de Hervido", "PALE_ALE-R7-3", "En curso");
        assertThat(enlaceDe(html, "Volver al lote")).isEqualTo("/lotes/12?etapa=hervido");
        assertThat(html).contains("data-resumen-consumo").contains("data-insumos-reservados").contains("data-consumo-directo").contains("data-consumos-registrados");
        assertThat(cuenta(html, "<dialog")).isEqualTo(1);
        assertThat(html).contains("id=\"dialogo-anular\"");
    }

    @Test
    @DisplayName("CP-LCO-02: el resumen muestra por insumo lo consumido, lo requerido, lo reservado y el avance hacia el mínimo, y el porcentaje mínimo de la etapa")
    void pantalla_resumenPorInsumo() throws Exception {
        // Act
        String resumen = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-resumen-consumo");

        // Assert
        assertThat(celdas(resumen, "Cascade")).containsExactly("Cascade", "15 de 25 g", "10 g", "60% de 80% requerido");
        assertThat(celdas(resumen, "Citra")).containsExactly("Citra", "0 de 15 g", "15 g", "0% de 80% requerido");
        assertThat(texto(resumen)).contains("Porcentaje mínimo para avanzar de etapa: 80%");
    }

    @Test
    @DisplayName("CP-LCO-03: con porcentaje mínimo 0 el resumen dice Sin consumo mínimo requerido en lugar del avance por insumo")
    void pantalla_sinPorcentajeMinimo() throws Exception {
        // Arrange
        when(loteServicio.calcularAvanceDeEtapa(12L, TipoEtapa.HERVIDO)).thenReturn(AvanceEtapaResponseDTO.builder().porcentajeMinimo(0.0).cumpleMinimo(true)
                .insumos(List.of(avance("Cascade", 25, 15, 60, true), avance("Citra", 15, 0, 0, true))).build());

        // Act
        String resumen = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-resumen-consumo");

        // Assert
        assertThat(texto(resumen)).contains("Sin consumo mínimo requerido").doesNotContain("requerido)").doesNotContain("% de").doesNotContain("Avance")
                .doesNotContain("Porcentaje mínimo");
        assertThat(celdas(resumen, "Cascade")).containsExactly("Cascade", "15 de 25 g", "10 g");
    }

    @Test
    @DisplayName("CP-LCO-04: cada reserva con saldo es su propio formulario con el lote de insumo oculto, la cantidad con su placeholder, el botón Registrar y el token CSRF")
    void pantalla_reservasConSaldo() throws Exception {
        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);
        String reservas = bloque(html, "data-insumos-reservados");
        String fila = elementoConAtributo(reservas, "tr", "data-reserva=\"5\"");

        // Assert
        assertThat(texto(reservas)).contains("Insumos reservados", "Lote de insumo", "Vencimiento", "Saldo reservado (g)", "Cantidad (g) *");
        assertThat(celdas(fila, "L-123")).containsExactly("L-123", "12/03/2028", "10", "Registrar");
        Matcher formulario = Pattern.compile("<form[^>]*>").matcher(fila);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("method=\"post\"").contains("action=\"/lotes/12/etapas/hervido/consumos/reservado\"").contains("novalidate");
        assertThat(campo(fila, "idLoteInsumo")).isEqualTo("5");
        assertThat(fila).contains("name=\"cantidadConsumida\"").contains("placeholder=\"Ej: 10\"").contains("name=\"_csrf\"");
        assertThat(cuenta(html, "action=\"/lotes/12/etapas/hervido/consumos/reservado\"")).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-LCO-05: las reservas agotadas van al final, atenuadas, con el texto Agotada y sin campo de cantidad ni botón Registrar")
    void pantalla_reservasAgotadasAlFinal() throws Exception {
        // Arrange
        when(reservaInsumoServicio.filtrarLotesInsumoReservados(ETAPA_HERVIDO, 31L)).thenReturn(List.of(
                reserva(2L, loteInsumo(6L, cascade(), "L-124", null, 0, false), 0),
                reserva(1L, loteInsumo(5L, cascade(), "L-123", LocalDate.of(2028, 3, 12), 10, false), 10)));

        // Act
        String reservas = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-insumos-reservados");
        String agotada = elementoConAtributo(reservas, "tr", "data-reserva=\"6\"");

        // Assert
        assertThat(reservas.indexOf("data-reserva=\"5\"")).isLessThan(reservas.indexOf("data-reserva=\"6\""));
        assertThat(agotada).contains("opacity-50").doesNotContain("cantidadConsumida").doesNotContain("<form").doesNotContain("<button");
        assertThat(texto(agotada)).contains("L-124", "0", "Agotada").doesNotContain("Registrar");
    }

    @Test
    @DisplayName("CP-LCO-06: una reserva vencida se marca con la etiqueta Vencido, conserva el campo y el botón Registrar y no muestra la advertencia")
    void pantalla_reservaVencida() throws Exception {
        // Act
        String fila = elementoConAtributo(bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-insumos-reservados"), "tr", "data-reserva=\"7\"");

        // Assert
        assertThat(celdas(fila, "C-900")).containsExactly("C-900 Vencido", "01/01/2020", "15", "Registrar");
        assertThat(fila).contains("name=\"cantidadConsumida\"").contains("type=\"submit\"");
        assertThat(texto(fila)).doesNotContain("se encuentra vencido");
    }

    @Test
    @DisplayName("CP-LCO-07: un insumo sin reservas indica usar el consumo directo y no muestra tabla")
    void pantalla_insumoSinReservas() throws Exception {
        // Arrange
        when(reservaInsumoServicio.filtrarLotesInsumoReservados(ETAPA_HERVIDO, 32L)).thenReturn(List.of());

        // Act
        String citra = elementoConAtributo(bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-insumos-reservados"), "div", "data-insumo-reservado=\"32\"");

        // Assert
        assertThat(texto(citra)).contains("Citra", "No hay lotes reservados para este insumo, use el consumo directo");
        assertThat(citra).doesNotContain("<table");
    }

    @Test
    @DisplayName("CP-LCO-08: el consumo directo elige el lote de insumo con un selector de entidad (modal) y pide la cantidad, con asteriscos rojos, placeholders y novalidate")
    void pantalla_consumoDirecto() throws Exception {
        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);
        String directo = bloque(html, "data-consumo-directo");

        // Assert
        Matcher formulario = Pattern.compile("<form[^>]*>").matcher(directo);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("method=\"post\"").contains("action=\"/lotes/12/etapas/hervido/consumos/directo\"").contains("novalidate");
        assertThat(directo).contains("name=\"_csrf\"").contains("data-selector-buscar").contains("bi-list-ul")
                .contains("data-buscador-url=\"" + URL_BUSCADOR + "\"").contains("placeholder=\"Seleccione lote de insumo\"")
                .contains("name=\"idLoteInsumo\"").contains("name=\"cantidadConsumida\"").contains("placeholder=\"Ej: 10\"")
                .doesNotContain("data-selector-menu").doesNotContain("data-selector-alta").doesNotContain("data-selector-limpiar").doesNotContain("<select");
        assertThat(texto(directo)).contains("Lote de insumo *", "Cantidad (g o kg, según el insumo) *", "Registrar consumo directo");
        assertThat(cuenta(directo, "text-rose-600")).isEqualTo(2);
        assertThat(textoDelSelector(directo, "Seleccione lote de insumo")).isEmpty();
    }

    @Test
    @DisplayName("CP-LCO-09: los consumos registrados se listan con el último arriba, y el anulado atenuado con su motivo y sin botón Anular; se piden de a 20 por id descendente sin filtrar")
    void pantalla_consumosRegistrados() throws Exception {
        // Act
        String consumos = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-consumos-registrados");
        String registrado = elementoConAtributo(consumos, "tr", "data-consumo=\"7\"");
        String anulado = elementoConAtributo(consumos, "tr", "data-consumo=\"6\"");

        // Assert
        assertThat(consumos.indexOf("data-consumo=\"7\"")).isLessThan(consumos.indexOf("data-consumo=\"6\""));
        assertThat(celdas(registrado, "L-123")).containsExactly("Cascade", "L-123", "Directo", "15 g", "01/07/2027 10:15", "Registrado", "Anular");
        assertThat(celdas(anulado, "L-124")).containsExactly("Cascade", "L-124", "Reservado", "5 g", "01/07/2027 10:15", "Anulado Motivo: Error de carga", "");
        assertThat(anulado).contains("opacity-60").doesNotContain("data-motivo-url");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(consumoInsumoServicio).filtrarConsumosInsumo(eq(ETAPA_HERVIDO), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("CP-LCO-10: Anular abre el diálogo de motivo: el botón lleva la URL de anulación y el nombre del insumo, y la página incluye el diálogo con el campo motivo")
    void pantalla_anularAbreElDialogoDeMotivo() throws Exception {
        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);
        Matcher boton = Pattern.compile("<button[^>]*data-motivo-dialogo[^>]*>").matcher(html);

        // Assert
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).contains("data-motivo-dialogo=\"dialogo-anular\"").contains("data-motivo-url=\"/lotes/12/etapas/hervido/consumos/7/anular\"")
                .contains("data-motivo-nombre=\"Cascade\"");
        assertThat(boton.find()).isFalse();
        String dialogo = elementoConAtributo(html, "dialog", "id=\"dialogo-anular\"");
        assertThat(texto(dialogo)).contains("Anular consumo");
        assertThat(dialogo).contains("name=\"motivo\"").contains("placeholder=\"Ej: Se cargó el lote de insumo equivocado\"").contains("text-rose-600").contains("name=\"_csrf\"");
    }

    @Test
    @DisplayName("CP-LCO-11: sin consumos muestra el mensaje en lugar de una tabla vacía")
    void pantalla_sinConsumos() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), any(), any())).thenReturn(Page.empty());

        // Act
        String consumos = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-consumos-registrados");

        // Assert
        assertThat(texto(consumos)).contains("No hay consumos registrados en esta etapa");
        assertThat(consumos).doesNotContain("<table");
    }

    @Test
    @DisplayName("CP-LCO-12: con 45 consumos y pagina=1 muestra Página 2 de 3 y los links a las páginas anterior y siguiente de la misma pantalla")
    void pantalla_paginacion() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(consumo(7L, ETAPA_HERVIDO, cascade(), "L-123", TipoConsumo.DIRECTO, 15, EstadoTransaccion.REGISTRADO, null)), PageRequest.of(1, 20), 45));

        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).param("pagina", "1").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("45 consumos", "Página 2 de 3");
        assertThat(enlaceDe(html, "Anterior")).isEqualTo(URL_PANTALLA + "?pagina=0");
        assertThat(enlaceDe(html, "Siguiente")).isEqualTo(URL_PANTALLA + "?pagina=2");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(consumoInsumoServicio).filtrarConsumosInsumo(eq(ETAPA_HERVIDO), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-LCO-13: las alertas del Post-Redirect-Get muestran el éxito y, debajo, una alerta amarilla por advertencia antes del resumen; sin advertencias no hay alertas amarillas")
    void pantalla_alertasDelPostRedirectGet() throws Exception {
        // Act
        String conAdvertencias = mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION))
                        .flashAttr("mensaje", "Consumo registrado correctamente").flashAttr("tipo", "success").flashAttr("advertencias", List.of(VENCIDO, EXCESO)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String sinAdvertencias = mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION))
                        .flashAttr("mensaje", "Consumo registrado correctamente").flashAttr("tipo", "success"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(conAdvertencias, "data-advertencia")).isEqualTo(2);
        assertThat(cuenta(conAdvertencias, ALERTA_ADVERTENCIA)).isEqualTo(2);
        assertThat(conAdvertencias.indexOf(ALERTA_EXITO)).isLessThan(conAdvertencias.indexOf("data-advertencia"));
        assertThat(conAdvertencias.lastIndexOf("data-advertencia")).isLessThan(conAdvertencias.indexOf("data-resumen-consumo"));
        assertThat(texto(conAdvertencias)).contains("Consumo registrado correctamente", VENCIDO, EXCESO);
        assertThat(conAdvertencias.indexOf(VENCIDO)).isLessThan(conAdvertencias.indexOf(EXCESO));
        assertThat(sinAdvertencias).contains(ALERTA_EXITO).doesNotContain("data-advertencia").doesNotContain(ALERTA_ADVERTENCIA);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"maceracion", "envasado"})
    @DisplayName("CP-LCO-14: una etapa que no admite consumos redirige al detalle del lote con un aviso y no invoca ningún service de consumos")
    void pantalla_etapaQueNoAdmiteConsumos(String etapa) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/lotes/12/etapas/" + etapa + "/consumos").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", "La etapa de " + (etapa.equals("maceracion") ? "Maceración" : "Envasado") + " no admite consumos de insumo"))
                .andExpect(flash().attribute("tipo", "warning"));
        verifyNoInteractions(consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-15: una etapa inexistente redirige al detalle del lote con el aviso La etapa indicada no es válida")
    void pantalla_etapaInexistente() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/lotes/12/etapas/xyz/consumos").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", "La etapa indicada no es válida"))
                .andExpect(flash().attribute("tipo", "warning"));
        verifyNoInteractions(consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-16: con la etapa fuera de curso la pantalla es de solo lectura: aviso, resumen y consumos, sin formularios, selector ni botones Anular")
    void pantalla_etapaQueNoEstaEnCurso() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarInsumosRequeridos(34L)).thenReturn(List.of(requerido(levadura(), 11.5, 0, 0)));
        when(loteServicio.calcularAvanceDeEtapa(12L, TipoEtapa.FERMENTACION)).thenReturn(AvanceEtapaResponseDTO.builder().porcentajeMinimo(80.0)
                .insumos(List.of(avance("US-05", 11.5, 0, 0, false))).build());
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(34L), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                consumo(9L, 34L, levadura(), "Y-001", TipoConsumo.DIRECTO, 2, EstadoTransaccion.REGISTRADO, null))));

        // Act
        String html = mockMvc.perform(get("/lotes/12/etapas/fermentacion/consumos").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("La etapa de Fermentación no está en curso, solo se pueden consultar los consumos");
        assertSoloLectura(html);
        assertThat(texto(bloque(html, "data-resumen-consumo"))).contains("US-05");
        assertThat(texto(bloque(html, "data-consumos-registrados"))).contains("Y-001");
        verifyNoInteractions(reservaInsumoServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = EstadoLote.class, names = {"FINALIZADO", "CANCELADO"})
    @DisplayName("CP-LCO-17: un lote que no está en ejecución se consulta sin operar: aviso de solo lectura y sin formularios ni botones Anular")
    void pantalla_loteQueNoEstaEnEjecucion(EstadoLote estado) throws Exception {
        // Arrange
        stubDetalle(detalle(estado, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA)));

        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(texto(html)).contains("La etapa de Hervido no está en curso, solo se pueden consultar los consumos");
        assertSoloLectura(html);
    }

    @Test
    @DisplayName("CP-LCO-18: un usuario solo con CONSUMO_INSUMO_CONSULTAR ve el resumen y los consumos registrados, sin formularios de registro, selector ni botones Anular")
    void pantalla_usuarioSoloConConsulta() throws Exception {
        // Act
        String html = html(como(Rol.ENCARGADO_DE_DEPOSITO));

        // Assert
        assertSoloLectura(html);
        assertThat(texto(bloque(html, "data-resumen-consumo"))).contains("Cascade", "Citra");
        assertThat(texto(bloque(html, "data-consumos-registrados"))).contains("L-123", "L-124");
        assertThat(html).doesNotContain("dialogo-anular").doesNotContain("Volver al lote");
        assertThat(texto(html)).doesNotContain("solo se pueden consultar los consumos");
    }

    @Test
    @DisplayName("CP-LCO-19: un lote inexistente redirige al origen con la alerta de error y el mensaje del service")
    void pantalla_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.buscarDetallePorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(get("/lotes/99/etapas/hervido/consumos").with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
        verifyNoInteractions(consumoInsumoServicio, reservaInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-20: la pantalla pide una vez el lote, el avance, los insumos requeridos, las reservas de cada insumo y los consumos de la etapa, y nada de otras etapas")
    void pantalla_datosPedidosALosServices() throws Exception {
        // Act
        html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        verify(loteServicio, times(1)).buscarDetallePorId(12L);
        verify(loteServicio, times(1)).calcularAvanceDeEtapa(12L, TipoEtapa.HERVIDO);
        verify(consumoInsumoServicio, times(1)).filtrarInsumosRequeridos(ETAPA_HERVIDO);
        verify(consumoInsumoServicio, times(1)).filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), any(), any());
        verify(reservaInsumoServicio, times(1)).filtrarLotesInsumoReservados(ETAPA_HERVIDO, 31L);
        verify(reservaInsumoServicio, times(1)).filtrarLotesInsumoReservados(ETAPA_HERVIDO, 32L);
        verifyNoMoreInteractions(loteServicio, consumoInsumoServicio, reservaInsumoServicio);
        verifyNoInteractions(loteInsumoServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCO-21: los roles sin CONSUMO_INSUMO_CONSULTAR no abren la pantalla: redirigen con alerta de error y no se invoca ningún service")
    void pantalla_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL_PANTALLA).with(como(rol))));
        verifyNingunService();
    }

    // ==================== POST .../consumos/reservado ====================

    @Test
    @DisplayName("CP-LCO-22: un consumo reservado correcto llama al service con la etapa 33 y el DTO enlazado, y vuelve a la pantalla con la alerta de éxito y sin advertencias")
    void reservado_debeRegistrarYVolverALaPantalla() throws Exception {
        // Act
        mockMvc.perform(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Consumo registrado correctamente"))
                .andExpect(flash().attribute("tipo", "success"))
                .andExpect(flash().attributeCount(2));

        // Assert
        ArgumentCaptor<ConsumoInsumoFormDTO> form = ArgumentCaptor.forClass(ConsumoInsumoFormDTO.class);
        verify(consumoInsumoServicio).registrarConsumoInsumoReservado(eq(ETAPA_HERVIDO), form.capture());
        assertThat(form.getValue().getIdLoteInsumo()).isEqualTo(5L);
        assertThat(form.getValue().getCantidadConsumida()).isEqualTo(10.0);
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoDirecto(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-23: las advertencias del service viajan en el atributo flash advertencias, en su orden, y no impiden el consumo")
    void reservado_debePasarLasAdvertencias() throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(any(), any())).thenReturn(respuestaDeConsumo(VENCIDO, EXCESO));

        // Act + Assert
        mockMvc.perform(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "7", "20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Consumo registrado correctamente"))
                .andExpect(flash().attribute("tipo", "success"))
                .andExpect(flash().attribute("advertencias", List.of(VENCIDO, EXCESO)));
    }

    @Test
    @DisplayName("CP-LCO-24: sin cantidad redirige a la pantalla (Post-Redirect-Get) que se muestra completa con el resumen de errores y no invoca al service de registro")
    void reservado_sinCantidad() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", null));

        // Assert
        assertThat(texto(bloque(html, "data-errores-formulario"))).contains("No se pudo registrar el consumo", "La cantidad consumida es obligatoria");
        assertThat(html).contains("data-resumen-consumo").contains("data-insumos-reservados").contains("data-consumo-directo").contains("data-consumos-registrados");
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoReservado(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-25: sin lote de insumo redirige a la pantalla con El lote de insumo es obligatorio y no invoca al service de registro")
    void reservado_sinLoteDeInsumo() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), null, "10"));

        // Assert
        assertThat(texto(bloque(html, "data-errores-formulario"))).contains("No se pudo registrar el consumo", "El lote de insumo es obligatorio");
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoReservado(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-26: una cantidad no numérica muestra Ingrese un número válido y la fila del lote de insumo conserva lo escrito")
    void reservado_cantidadNoNumerica() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", "abc"));
        String fila = elementoConAtributo(bloque(html, "data-insumos-reservados"), "tr", "data-reserva=\"5\"");

        // Assert
        assertThat(texto(bloque(html, "data-errores-formulario"))).contains("Ingrese un número válido");
        assertThat(texto(fila)).contains("Ingrese un número válido");
        assertThat(campo(fila, "cantidadConsumida")).isEqualTo("abc");
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoReservado(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-27: el error de una fila no borra lo escrito en ella y no pasa a las otras filas")
    void reservado_loEscritoSoloQuedaEnSuFila() throws Exception {
        // Act
        String reservas = bloque(enviarConErroresYVolver(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", "abc")), "data-insumos-reservados");

        // Assert
        assertThat(campo(elementoConAtributo(reservas, "tr", "data-reserva=\"5\""), "cantidadConsumida")).isEqualTo("abc");
        String otraFila = elementoConAtributo(reservas, "tr", "data-reserva=\"7\"");
        assertThat(otraFila).doesNotContain("value=\"abc\"").doesNotContain("text-rose-600");
        assertThat(texto(otraFila)).doesNotContain("Ingrese un número válido");
        assertThat(campo(otraFila, "cantidadConsumida")).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"0", "-5", "NaN", "Infinity"})
    @DisplayName("CP-LCO-28: una cantidad no válida para el negocio llega al service con el valor recibido y su rechazo vuelve al origen con la alerta amarilla")
    void reservado_cantidadNoValidaParaElService(String cantidad) throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(any(), any())).thenThrow(new ReglaNegocioException("La cantidad consumida debe ser mayor a 0"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", cantidad).header("Referer", URL_PANTALLA)),
                "La cantidad consumida debe ser mayor a 0", "warning");
        ArgumentCaptor<ConsumoInsumoFormDTO> form = ArgumentCaptor.forClass(ConsumoInsumoFormDTO.class);
        verify(consumoInsumoServicio).registrarConsumoInsumoReservado(eq(ETAPA_HERVIDO), form.capture());
        assertThat(form.getValue().getCantidadConsumida()).isEqualTo(Double.valueOf(cantidad));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "La cantidad consumida no puede superar la cantidad reservada del lote de insumo (10 g)",
            "No existe una reserva de este lote de insumo para esta etapa; utilice el registro de consumo directo",
            "El lote debe encontrarse en estado En ejecución para poder registrar consumos de insumo",
            "La etapa debe estar en curso para poder registrar consumos de insumo",
            "El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta"})
    @DisplayName("CP-LCO-29/30/31/32: los rechazos de negocio del service (tope reservado, sin reserva, lote o etapa que no admiten el consumo, insumo ajeno a la etapa) vuelven al origen con la alerta amarilla y su mensaje")
    void reservado_rechazosDelService(String mensaje) throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", "50").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LCO-33: un lote de insumo inexistente vuelve al origen con la alerta de error y el mensaje del service")
    void reservado_loteDeInsumoInexistente() throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(any(), any())).thenThrow(new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: 99"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "99", "10").header("Referer", URL_PANTALLA)),
                "No se encontró el lote de insumo con ID: 99", "danger");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"maceracion", "xyz"})
    @DisplayName("CP-LCO-34: registrar un consumo reservado en una etapa que no admite consumos o inexistente redirige al detalle del lote y no invoca el registro")
    void reservado_etapaQueNoAdmiteConsumosOInexistente(String etapa) throws Exception {
        // Act + Assert
        mockMvc.perform(post("/lotes/12/etapas/" + etapa + "/consumos/reservado").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf())
                        .param("idLoteInsumo", "5").param("cantidadConsumida", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", etapa.equals("xyz") ? "La etapa indicada no es válida" : "La etapa de Maceración no admite consumos de insumo"))
                .andExpect(flash().attribute("tipo", "warning"));
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-35: un consumo reservado sin token CSRF es rechazado (redirige) y no llega al service")
    void reservado_sinCsrf() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL_PANTALLA + "/reservado").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idLoteInsumo", "5").param("cantidadConsumida", "10"))
                .andExpect(status().is3xxRedirection());
        verifyNingunService();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCO-36: los roles sin CONSUMO_INSUMO_REGISTRAR no registran consumos reservados: redirigen con alerta de error y el service no se invoca")
    void reservado_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(postReservado(como(rol), "5", "10")));
        verifyNingunService();
    }

    // ==================== POST .../consumos/directo ====================

    @Test
    @DisplayName("CP-LCO-37: un consumo directo correcto llama a registrarConsumoInsumoDirecto con la etapa 33 y el DTO enlazado, y vuelve a la pantalla con la alerta de éxito")
    void directo_debeRegistrarYVolverALaPantalla() throws Exception {
        // Act
        mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Consumo registrado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        // Assert
        ArgumentCaptor<ConsumoInsumoFormDTO> form = ArgumentCaptor.forClass(ConsumoInsumoFormDTO.class);
        verify(consumoInsumoServicio).registrarConsumoInsumoDirecto(eq(ETAPA_HERVIDO), form.capture());
        assertThat(form.getValue().getIdLoteInsumo()).isEqualTo(8L);
        assertThat(form.getValue().getCantidadConsumida()).isEqualTo(5.0);
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoReservado(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-38: el consumo directo de un lote vencido se permite y la advertencia viaja en el atributo flash advertencias")
    void directo_loteVencidoConAdvertencia() throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenReturn(respuestaDeConsumo(VENCIDO));

        // Act + Assert
        mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "7", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("tipo", "success"))
                .andExpect(flash().attribute("advertencias", List.of(VENCIDO)));
    }

    @Test
    @DisplayName("CP-LCO-39: un consumo mayor a lo requerido se advierte y no se bloquea")
    void directo_excesoConAdvertencia() throws Exception {
        // Arrange
        String exceso = "El total consumido de Cascade en esta etapa (30 g) supera la cantidad requerida (25 g)";
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenReturn(respuestaDeConsumo(exceso));

        // Act + Assert
        mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Consumo registrado correctamente"))
                .andExpect(flash().attribute("advertencias", List.of(exceso)));
    }

    @Test
    @DisplayName("CP-LCO-40: un stock insuficiente lo rechaza el service y vuelve al origen con la alerta amarilla")
    void directo_stockInsuficiente() throws Exception {
        // Arrange
        String mensaje = "La cantidad consumida no puede superar la cantidad disponible del lote de insumo (3 g)";
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "50").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LCO-41: sin lote de insumo elegido redirige a la pantalla con el error debajo del selector, conserva la cantidad y no invoca al service de registro")
    void directo_sinLoteDeInsumo() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), null, "5"));
        String directo = bloque(html, "data-consumo-directo");

        // Assert
        assertThat(texto(bloque(html, "data-errores-formulario"))).contains("No se pudo registrar el consumo", "El lote de insumo es obligatorio");
        assertThat(texto(directo)).contains("El lote de insumo es obligatorio");
        assertThat(directo.indexOf("data-selector-buscar")).isLessThan(directo.indexOf("El lote de insumo es obligatorio"));
        assertThat(campo(directo, "cantidadConsumida")).isEqualTo("5");
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoDirecto(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-42: un error de la cantidad conserva el lote de insumo elegido en el selector, con su insumo, vencimiento y saldo")
    void directo_elLoteElegidoSeConserva() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", null));
        String directo = bloque(html, "data-consumo-directo");

        // Assert
        assertThat(textoDelSelector(directo, "Seleccione lote de insumo")).isEqualTo("Cascade - L-125");
        assertThat(campo(directo, "idLoteInsumo")).isEqualTo("8");
        assertThat(texto(directo)).contains("La cantidad consumida es obligatoria");
        Matcher elegido = Pattern.compile("<p[^>]*data-lote-elegido[^>]*>").matcher(directo);
        assertThat(elegido.find()).isTrue();
        assertThat(elegido.group()).doesNotContain("hidden");
        assertThat(texto(elementoConAtributo(directo, "p", "data-lote-elegido"))).contains("Vencimiento: 12/03/2028", "Disponible: 40 g");
        assertThat(Pattern.compile("<span[^>]*data-lote-vencido[^>]*>").matcher(directo).results().findFirst().orElseThrow().group()).contains("hidden");
        verify(loteInsumoServicio).buscarPorId(8L);
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoDirecto(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-43: una cantidad no numérica muestra Ingrese un número válido y conserva el lote elegido; los valores 0, -5, NaN e Infinity los rechaza el service")
    void directo_cantidadNoNumericaOInvalida() throws Exception {
        // Act: el formato es un error del campo
        String html = enviarConErroresYVolver(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "abc"));
        String directo = bloque(html, "data-consumo-directo");

        // Assert
        assertThat(texto(directo)).contains("Ingrese un número válido");
        assertThat(campo(directo, "cantidadConsumida")).isEqualTo("abc");
        assertThat(textoDelSelector(directo, "Seleccione lote de insumo")).isEqualTo("Cascade - L-125");
        verify(consumoInsumoServicio, never()).registrarConsumoInsumoDirecto(any(), any());

        // Act + Assert: el valor es una regla del service
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new ReglaNegocioException("La cantidad consumida debe ser mayor a 0"));
        for (String cantidad : List.of("0", "-5", "NaN", "Infinity")) {
            assertRechazoDelService(mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", cantidad).header("Referer", URL_PANTALLA)),
                    "La cantidad consumida debe ser mayor a 0", "warning");
        }
        verify(consumoInsumoServicio, times(4)).registrarConsumoInsumoDirecto(eq(ETAPA_HERVIDO), any());
    }

    @Test
    @DisplayName("CP-LCO-44: un lote de insumo de otro insumo lo rechaza el service y vuelve al origen con la alerta amarilla")
    void directo_loteDeOtroInsumo() throws Exception {
        // Arrange
        String mensaje = "El lote de insumo no corresponde a un insumo utilizado en esta etapa según la receta";
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "9", "5").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "El lote debe encontrarse en estado En ejecución para poder registrar consumos de insumo",
            "La etapa debe estar en curso para poder registrar consumos de insumo"})
    @DisplayName("CP-LCO-45: un lote o una etapa que no admiten el consumo los rechaza el service y vuelve al origen con la alerta amarilla")
    void directo_loteOEtapaQueNoAdmitenElConsumo(String mensaje) throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "5").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LCO-46: un lote de insumo inexistente vuelve al origen con la alerta de error y el mensaje del service")
    void directo_loteDeInsumoInexistente() throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: 99"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "99", "5").header("Referer", URL_PANTALLA)),
                "No se encontró el lote de insumo con ID: 99", "danger");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"envasado", "xyz"})
    @DisplayName("CP-LCO-47: registrar un consumo directo en una etapa que no admite consumos o inexistente redirige al detalle del lote con un aviso y no invoca el registro")
    void directo_etapaQueNoAdmiteConsumosOInexistente(String etapa) throws Exception {
        // Act + Assert
        mockMvc.perform(post("/lotes/12/etapas/" + etapa + "/consumos/directo").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf())
                        .param("idLoteInsumo", "8").param("cantidadConsumida", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", etapa.equals("xyz") ? "La etapa indicada no es válida" : "La etapa de Envasado no admite consumos de insumo"))
                .andExpect(flash().attribute("tipo", "warning"));
        verifyNoInteractions(consumoInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-48: un consumo directo sin token CSRF es rechazado (redirige) y no llega al service")
    void directo_sinCsrf() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL_PANTALLA + "/directo").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idLoteInsumo", "8").param("cantidadConsumida", "5"))
                .andExpect(status().is3xxRedirection());
        verifyNingunService();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCO-49: los roles sin CONSUMO_INSUMO_REGISTRAR no registran consumos directos: redirigen con alerta de error y el service no se invoca")
    void directo_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(postDirecto(como(rol), "8", "5")));
        verifyNingunService();
    }

    // ==================== POST .../consumos/{idConsumo}/anular ====================

    @Test
    @DisplayName("CP-LCO-50: una anulación correcta llama a anularConsumoInsumo con el motivo y vuelve a la misma pantalla con la alerta de éxito")
    void anular_debeAnularYVolverALaPantalla() throws Exception {
        // Act
        mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 7L, "Se cargó el lote de insumo equivocado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Consumo anulado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        // Assert
        ArgumentCaptor<AnularConsumoInsumoFormDTO> form = ArgumentCaptor.forClass(AnularConsumoInsumoFormDTO.class);
        verify(consumoInsumoServicio).anularConsumoInsumo(eq(7L), form.capture());
        assertThat(form.getValue().getMotivoAnulacion()).isEqualTo("Se cargó el lote de insumo equivocado");
    }

    @Test
    @DisplayName("CP-LCO-51: sin motivo el controller no valida: llama al service con motivo null y su rechazo vuelve al origen con la alerta amarilla")
    void anular_sinMotivo() throws Exception {
        // Arrange
        when(consumoInsumoServicio.anularConsumoInsumo(eq(7L), any())).thenThrow(new ReglaNegocioException("El motivo de anulación es obligatorio"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 7L, null).header("Referer", URL_PANTALLA)),
                "El motivo de anulación es obligatorio", "warning");
        ArgumentCaptor<AnularConsumoInsumoFormDTO> form = ArgumentCaptor.forClass(AnularConsumoInsumoFormDTO.class);
        verify(consumoInsumoServicio).anularConsumoInsumo(eq(7L), form.capture());
        assertThat(form.getValue().getMotivoAnulacion()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "Solo se pueden anular consumos de insumo en estado Registrado",
            "El lote debe encontrarse en estado En ejecución para poder anular un consumo de insumo",
            "La etapa debe estar en curso para poder anular un consumo de insumo"})
    @DisplayName("CP-LCO-52/53: un consumo ya anulado, o un lote o una etapa que no admiten la anulación, los rechaza el service y vuelve al origen con la alerta amarilla")
    void anular_rechazosDelService(String mensaje) throws Exception {
        // Arrange
        when(consumoInsumoServicio.anularConsumoInsumo(eq(7L), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 7L, "Error de carga").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LCO-54: un consumo inexistente vuelve al origen con la alerta de error y el mensaje del service, sin anular")
    void anular_consumoInexistente() throws Exception {
        // Arrange
        when(consumoInsumoServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el consumo de insumo con ID: 99"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 99L, "Error de carga").header("Referer", URL_PANTALLA)),
                "No se encontró el consumo de insumo con ID: 99", "danger");
        verify(consumoInsumoServicio, never()).anularConsumoInsumo(any(), any());
    }

    @ParameterizedTest(name = "consumo {0}")
    @ValueSource(longs = {8, 15})
    @DisplayName("CP-LCO-55: un consumo de otra etapa o de otro lote vuelve a la pantalla con el aviso El consumo no pertenece a esta etapa del lote y no se anula")
    void anular_consumoDeOtraEtapaOOtroLote(long idConsumo) throws Exception {
        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), idConsumo, "Error de carga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "El consumo no pertenece a esta etapa del lote"))
                .andExpect(flash().attribute("tipo", "warning"));
        verify(consumoInsumoServicio, never()).anularConsumoInsumo(any(), any());
    }

    @Test
    @DisplayName("CP-LCO-56: una anulación sin token CSRF es rechazada (redirige) y no llega al service")
    void anular_sinCsrf() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL_PANTALLA + "/7/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("motivo", "Error de carga"))
                .andExpect(status().is3xxRedirection());
        verifyNingunService();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCO-57: los roles sin CONSUMO_INSUMO_ANULAR no anulan consumos: redirigen con alerta de error y el service no se invoca")
    void anular_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(postAnular(como(rol), 7L, "Error de carga")));
        verifyNingunService();
    }

    // ==================== GET .../consumos/buscador/lotes-insumo ====================

    @Test
    @DisplayName("CP-LCO-58: el buscador devuelve un fragmento con los criterios Insumo e Identificación del lote, las columnas y un botón Elegir por fila con el id, el texto y el disponible")
    void buscador_debeDevolverElFragmento() throws Exception {
        // Arrange
        stubLotesInsumo(new PageImpl<>(List.of(
                loteInsumo(8L, cascade(), "L-125", LocalDate.of(2028, 3, 12), 40, false),
                loteInsumo(7L, citra(), "C-900", LocalDate.of(2020, 1, 1), 15, true))));

        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(html).doesNotContain("<html").doesNotContain("<select");
        assertThat(texto(html)).contains("Buscar lote de insumo", "Insumo", "Identificación del lote", "Lote", "Vencimiento", "Disponible");
        Matcher formulario = Pattern.compile("<form[^>]*>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("data-modal-buscar").contains("action=\"" + URL_BUSCADOR + "\"").contains("method=\"get\"");
        assertThat(html).contains("name=\"insumo\"").contains("placeholder=\"Ej: Cascade\"").contains("name=\"identificacion\"").contains("placeholder=\"Ej: L-125\"");
        Matcher elegir = Pattern.compile("<button[^>]*data-elegir[^>]*>").matcher(html);
        assertThat(elegir.find()).isTrue();
        assertThat(elegir.group()).contains("data-id=\"8\"").contains("data-texto=\"Cascade - L-125\"").contains("data-disponible=\"40 g\"").contains("data-vencimiento=\"12/03/2028\"");
        assertThat(elegir.find()).isTrue();
        assertThat(elegir.group()).contains("data-id=\"7\"").contains("data-texto=\"Citra - C-900\"").contains("data-disponible=\"15 g\"");
        assertThat(celdas(html, "L-125")).containsExactly("Cascade", "L-125", "12/03/2028", "40 g", "Elegir");
    }

    @Test
    @DisplayName("CP-LCO-59: el buscador pide solo los lotes de los insumos que la etapa requiere, de a 8 y sin ordenar (el orden lo da el service)")
    void buscador_soloOfreceLotesDeLosInsumosDeLaEtapa() throws Exception {
        // Act
        buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(loteInsumoServicio).filtrarLotesInsumoDisponibles(eq(List.of(31L, 32L)), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(8);
        assertThat(pageable.getValue().getSort().isUnsorted()).isTrue();
        verify(consumoInsumoServicio).filtrarInsumosRequeridos(ETAPA_HERVIDO);
    }

    @Test
    @DisplayName("CP-LCO-60: un lote vencido se marca con la etiqueta Vencido y su botón Elegir lleva data-vencido; uno que vence hoy o más adelante no se marca")
    void buscador_marcaLosLotesVencidos() throws Exception {
        // Arrange
        stubLotesInsumo(new PageImpl<>(List.of(
                loteInsumo(8L, cascade(), "L-125", LocalDate.of(2028, 3, 12), 40, false),
                loteInsumo(9L, cascade(), "L-130", LocalDate.now(), 10, false),
                loteInsumo(7L, citra(), "C-900", LocalDate.of(2020, 1, 1), 15, true))));

        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(celdas(html, "C-900")).containsExactly("Citra", "C-900", "01/01/2020 Vencido", "15 g", "Elegir");
        assertThat(celdas(html, "L-125").get(2)).isEqualTo("12/03/2028");
        assertThat(celdas(html, "L-130").get(2)).doesNotContain("Vencido");
        assertThat(botonElegir(html, 7)).contains("data-vencido=\"true\"");
        assertThat(botonElegir(html, 8)).doesNotContain("data-vencido");
        assertThat(botonElegir(html, 9)).doesNotContain("data-vencido");
        assertThat(cuenta(html, "Vencido")).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-LCO-61: el buscador muestra los lotes en el orden recibido del service, con los vencidos al final, y no los reordena")
    void buscador_respetaElOrdenDelService() throws Exception {
        // Arrange
        stubLotesInsumo(new PageImpl<>(List.of(
                loteInsumo(8L, cascade(), "L-125", LocalDate.of(2028, 3, 12), 40, false),
                loteInsumo(9L, cascade(), "L-126", LocalDate.of(2029, 6, 1), 10, false),
                loteInsumo(7L, citra(), "C-900", LocalDate.of(2020, 1, 1), 15, true))));

        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(IntStream.of(8, 9, 7).map(id -> html.indexOf("data-id=\"" + id + "\"")).boxed().toList()).isSorted().doesNotContain(-1);
        assertThat(html.lastIndexOf("Vencido")).isGreaterThan(html.indexOf("data-id=\"9\""));
    }

    @Test
    @DisplayName("CP-LCO-62: los criterios llegan recortados al service y los en blanco equivalen a no filtrar; los campos conservan lo escrito y un insumo sin coincidencias muestra la página vacía")
    void buscador_criteriosDeInsumoEIdentificacion() throws Exception {
        // Act
        String escrito = mockMvc.perform(get(URL_BUSCADOR).param("insumo", " cascade ").param("identificacion", " l-125 ").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(get(URL_BUSCADOR).param("insumo", "  ").param("identificacion", "  ").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk());
        String sinCoincidencias = mockMvc.perform(get(URL_BUSCADOR).param("insumo", "zzz").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        verify(loteInsumoServicio).filtrarLotesInsumoDisponibles(eq(List.of(31L, 32L)), eq("cascade"), eq("l-125"), any());
        verify(loteInsumoServicio).filtrarLotesInsumoDisponibles(eq(List.of(31L, 32L)), isNull(), isNull(), any());
        verify(loteInsumoServicio).filtrarLotesInsumoDisponibles(eq(List.of(31L, 32L)), eq("zzz"), isNull(), any());
        assertThat(campo(escrito, "insumo")).isEqualTo(" cascade ");
        assertThat(campo(escrito, "identificacion")).isEqualTo(" l-125 ");
        assertThat(campo(sinCoincidencias, "insumo")).isEqualTo("zzz");
        assertThat(texto(sinCoincidencias)).contains("No se encontraron lotes de insumo con los criterios indicados");
    }

    @Test
    @DisplayName("CP-LCO-63: el buscador pagina de a 8: la primera página trae 8 filas con el link a la siguiente y la segunda las 2 restantes con el link a la anterior, conservando los criterios")
    void buscador_paginaDeA8() throws Exception {
        // Arrange
        List<LoteInsumoResponseDTO> lotes = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> loteInsumo(100L + i, cascade(), "L-" + i, LocalDate.of(2028, 1, i), 10, false)).toList();
        stubLotesInsumo(new PageImpl<>(lotes.subList(0, 8), PageRequest.of(0, 8), 10));
        String primera = mockMvc.perform(get(URL_BUSCADOR).param("insumo", "casc").param("identificacion", "L-").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        stubLotesInsumo(new PageImpl<>(lotes.subList(8, 10), PageRequest.of(1, 8), 10));

        // Act
        String segunda = mockMvc.perform(get(URL_BUSCADOR).param("pagina", "1").param("insumo", "casc").param("identificacion", "L-").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(primera, "data-elegir")).isEqualTo(8);
        assertThat(texto(primera)).contains("10 lotes de insumo", "Página 1 de 2");
        assertThat(primera).contains("data-modal-get=\"true\"");
        assertThat(enlaceDe(primera, "Siguiente")).startsWith(URL_BUSCADOR + "?pagina=1").contains("insumo=casc").contains("identificacion=L-");
        assertThat(primera).doesNotContain("Anterior");
        assertThat(cuenta(segunda, "data-elegir")).isEqualTo(2);
        assertThat(texto(segunda)).contains("Página 2 de 2");
        assertThat(enlaceDe(segunda, "Anterior")).startsWith(URL_BUSCADOR + "?pagina=0").contains("insumo=casc").contains("identificacion=L-");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(loteInsumoServicio, times(2)).filtrarLotesInsumoDisponibles(any(Collection.class), eq("casc"), eq("L-"), pageable.capture());
        assertThat(pageable.getAllValues()).extracting(Pageable::getPageNumber).containsExactly(0, 1);
    }

    @Test
    @DisplayName("CP-LCO-64: sin resultados el buscador muestra un mensaje claro")
    void buscador_sinResultados() throws Exception {
        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(texto(html)).contains("No se encontraron lotes de insumo con los criterios indicados");
        assertThat(html).doesNotContain("data-elegir");
    }

    @Test
    @DisplayName("CP-LCO-65: con una etapa que no admite consumos el buscador responde 200 con el mensaje dentro del fragmento, sin criterios ni filas")
    void buscador_etapaQueNoAdmiteConsumos() throws Exception {
        // Act
        String html = mockMvc.perform(get("/lotes/12/etapas/maceracion/consumos/buscador/lotes-insumo").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-lote-insumo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("La etapa de Maceración no admite consumos de insumo");
        assertThat(html).doesNotContain("name=\"insumo\"").doesNotContain("data-elegir").doesNotContain("data-modal-buscar");
        verifyNoInteractions(consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LCO-66: los roles sin CONSUMO_INSUMO_REGISTRAR no abren el buscador: redirigen con alerta de error y no se invoca ningún service")
    void buscador_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL_BUSCADOR).with(como(rol))));
        verifyNingunService();
    }

    @Test
    @DisplayName("CP-LCO-67: el criterio Insumo es un campo de texto con el placeholder Ej: Cascade y el fragmento no tiene ningún desplegable")
    void buscador_elCriterioInsumoEsUnCampoDeTexto() throws Exception {
        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(Pattern.compile("<input[^>]*name=\"insumo\"[^>]*>").matcher(html).results().findFirst().orElseThrow().group())
                .contains("type=\"text\"").contains("placeholder=\"Ej: Cascade\"");
        assertThat(html).doesNotContain("<select").doesNotContain("<option");
    }

    @Test
    @DisplayName("CP-LCO-68: un lote de insumo sin fecha de vencimiento muestra un guion, no se marca como vencido y va después de los vigentes con fecha")
    void buscador_loteSinFechaDeVencimiento() throws Exception {
        // Arrange
        stubLotesInsumo(new PageImpl<>(List.of(
                loteInsumo(8L, cascade(), "L-125", LocalDate.of(2028, 3, 12), 40, false),
                loteInsumo(10L, cascade(), "L-127", null, 25, false),
                loteInsumo(7L, citra(), "C-900", LocalDate.of(2020, 1, 1), 15, true))));

        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(IntStream.of(8, 10, 7).map(id -> html.indexOf("data-id=\"" + id + "\"")).boxed().toList()).isSorted().doesNotContain(-1);
        assertThat(celdas(html, "L-127")).containsExactly("Cascade", "L-127", "-", "25 g", "Elegir");
        assertThat(botonElegir(html, 10)).doesNotContain("data-vencido").contains("data-vencimiento=\"-\"");
    }

    // ==================== Transversales ====================

    @Test
    @DisplayName("CP-LCO-69: registrar (reservado y directo) y anular son Post-Redirect-Get a la misma pantalla, también en otra etapa, con la alerta de éxito y sin responder 200")
    void transversal_postRedirectGet() throws Exception {
        // Act + Assert
        for (MockHttpServletRequestBuilder peticion : List.of(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", "10"),
                postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "5"), postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 7L, "Error de carga"))) {
            mockMvc.perform(peticion)
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(URL_PANTALLA))
                    .andExpect(flash().attribute("tipo", "success"));
        }
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(eq(34L), any())).thenReturn(respuestaDeConsumo());
        mockMvc.perform(post("/lotes/12/etapas/fermentacion/consumos/reservado").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf())
                        .param("idLoteInsumo", "5").param("cantidadConsumida", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12/etapas/fermentacion/consumos"))
                .andExpect(flash().attribute("tipo", "success"));
    }

    @Test
    @DisplayName("CP-LCO-70: un rechazo del service sin Referer redirige a la raíz con la alerta amarilla y el mensaje del service")
    void transversal_rechazoSinReferer() throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new ReglaNegocioException("La etapa debe estar en curso para poder registrar consumos de insumo"));

        // Act + Assert
        mockMvc.perform(postDirecto(como(Rol.OPERARIO_DE_PRODUCCION), "8", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("mensaje", "La etapa debe estar en curso para poder registrar consumos de insumo"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-LCO-71: sin JavaScript las acciones funcionan: cada fila de reserva y el consumo directo son formularios POST con action y CSRF, novalidate, data-un-solo-envio y sin required, min ni pattern")
    void transversal_sinJavaScript() throws Exception {
        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        Matcher formularios = Pattern.compile("<form[^>]*method=\"post\"[^>]*>").matcher(contenido(html));
        List<String> etiquetas = new ArrayList<>();
        while (formularios.find()) {
            etiquetas.add(formularios.group());
        }
        assertThat(etiquetas).hasSize(4);
        assertThat(etiquetas).allSatisfy(form -> assertThat(form).contains("method=\"post\"").contains("action=\"").contains("novalidate").contains("data-un-solo-envio"));
        assertThat(cuenta(contenido(html), "name=\"_csrf\"")).isEqualTo(4);
        assertThat(html).doesNotContain(" required").doesNotContain(" min=").doesNotContain(" pattern=")
                .contains("/js/modales.js").contains("/js/consumo-lista.js");
    }

    @Test
    @DisplayName("CP-LCO-72: los campos de cantidad y el selector llevan asterisco rojo y placeholder de ejemplo; la pantalla no tiene emojis y ninguna alerta termina en punto")
    void transversal_ayudasVisuales() throws Exception {
        // Act
        List<String> paginas = List.of(
                html(Rol.OPERARIO_DE_PRODUCCION),
                mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION))
                                .flashAttr("mensaje", "Consumo registrado correctamente").flashAttr("tipo", "success").flashAttr("advertencias", List.of(VENCIDO, EXCESO)))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                enviarConErroresYVolver(postReservado(como(Rol.OPERARIO_DE_PRODUCCION), "5", "abc")));

        // Assert
        String directo = bloque(paginas.get(0), "data-consumo-directo");
        assertThat(cuenta(directo, "text-rose-600")).isEqualTo(2);
        assertThat(directo).contains("placeholder=\"Seleccione lote de insumo\"").contains("placeholder=\"Ej: 10\"");
        assertThat(cuenta(bloque(paginas.get(0), "data-insumos-reservados"), "placeholder=\"Ej: 10\"")).isEqualTo(2);
        assertThat(texto(bloque(paginas.get(0), "data-insumos-reservados"))).contains("Cantidad (g) *");
        Pattern alerta = Pattern.compile("(?s)role=\"alert\"[^>]*>(.*?)</div>");
        Pattern emoji = Pattern.compile("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{FE0F}]");
        for (String pagina : paginas) {
            Matcher alertas = alerta.matcher(pagina);
            while (alertas.find()) {
                assertThat(texto(alertas.group(1))).isNotEmpty().doesNotEndWith(".");
            }
            assertThat(emoji.matcher(pagina).find()).isFalse();
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"reservado", "directo"})
    @DisplayName("CP-LCO-74: un error de validación y luego el rechazo del service no terminan en una ruta sin pantalla: el navegador queda siempre en la pantalla de consumos, sin 405 ni bucle")
    void transversal_errorDeValidacionYLuegoRechazoDelService(String formulario) throws Exception {
        // Arrange
        when(consumoInsumoServicio.registrarConsumoInsumoReservado(any(), any())).thenThrow(new ReglaNegocioException("La cantidad consumida debe ser mayor a 0"));
        when(consumoInsumoServicio.registrarConsumoInsumoDirecto(any(), any())).thenThrow(new ReglaNegocioException("La cantidad consumida debe ser mayor a 0"));
        String idLoteInsumo = formulario.equals("reservado") ? "5" : "8";

        // Act: 1) el formulario es inválido y el navegador vuelve a la pantalla, no queda en la URL del POST
        MvcResult invalido = mockMvc.perform(postConsumo(URL_PANTALLA + "/" + formulario, como(Rol.OPERARIO_DE_PRODUCCION), idLoteInsumo, null)
                        .header("Referer", URL_PANTALLA))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String ubicacion = invalido.getResponse().getRedirectedUrl();
        mockMvc.perform(get(ubicacion).with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(invalido.getFlashMap())).andExpect(status().isOk());

        // Act: 2) desde esa ubicación, el service rechaza un envío con datos válidos
        MvcResult rechazado = mockMvc.perform(postConsumo(URL_PANTALLA + "/" + formulario, como(Rol.OPERARIO_DE_PRODUCCION), idLoteInsumo, "0")
                        .header("Referer", ubicacion))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String destino = rechazado.getResponse().getRedirectedUrl();
        String html = mockMvc.perform(get(destino).with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(rechazado.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert: las dos veces vuelve a la pantalla (que tiene GET) y el rechazo se muestra como alerta
        assertThat(ubicacion).isEqualTo(URL_PANTALLA);
        assertThat(destino).isEqualTo(URL_PANTALLA);
        assertThat(texto(html)).contains("La cantidad consumida debe ser mayor a 0");
    }

    @Test
    @DisplayName("CP-LCO-75: el buscador de un lote inexistente responde 200 con el mensaje dentro del fragmento, sin criterios ni filas")
    void buscador_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.buscarDetallePorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act
        String html = mockMvc.perform(get("/lotes/99/etapas/hervido/consumos/buscador/lotes-insumo").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-lote-insumo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("No se encontró el lote con ID: 99");
        assertThat(html).doesNotContain("name=\"insumo\"").doesNotContain("data-elegir").doesNotContain("data-modal-buscar");
        verifyNoInteractions(consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-77: los consumos registrados se pueden filtrar por estado con un desplegable (Todos, Registrado, Anulado) que por defecto muestra todos, sin pedirle estado al service")
    void pantalla_filtroDeEstadoPorDefectoMuestraTodos() throws Exception {
        // Act
        String consumos = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-consumos-registrados");
        String filtro = bloque(consumos, "data-filtro-consumos");

        // Assert
        assertThat(filtro).contains("method=\"get\"").contains("action=\"" + URL_PANTALLA + "\"").contains("name=\"estado\"");
        assertThat(cuenta(filtro, "<option")).isEqualTo(3);
        assertThat(texto(filtro)).contains("Estado", "Todos", "Registrado", "Anulado", "Filtrar");
        assertThat(filtro).doesNotContain("selected");
        verify(consumoInsumoServicio).filtrarConsumosInsumo(eq(ETAPA_HERVIDO), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("CP-LCO-78: con estado=ANULADO pide al service solo los anulados, deja elegida esa opción y conserva el estado en los links de paginación")
    void pantalla_filtroDeEstadoAnulados() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), eq(EstadoTransaccion.ANULADO), any())).thenReturn(new PageImpl<>(
                List.of(consumo(6L, ETAPA_HERVIDO, cascade(), "L-124", TipoConsumo.RESERVADO, 5, EstadoTransaccion.ANULADO, "Error de carga")), PageRequest.of(1, 20), 45));

        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).param("estado", "ANULADO").param("pagina", "1").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(elementoConAtributo(html, "option", "value=\"ANULADO\"")).contains("selected");
        assertThat(enlaceDe(html, "Anterior")).isEqualTo(URL_PANTALLA + "?pagina=0&estado=ANULADO");
        assertThat(enlaceDe(html, "Siguiente")).isEqualTo(URL_PANTALLA + "?pagina=2&estado=ANULADO");
        verify(consumoInsumoServicio).filtrarConsumosInsumo(eq(ETAPA_HERVIDO), isNull(), isNull(), eq(EstadoTransaccion.ANULADO), any(Pageable.class));
    }

    @Test
    @DisplayName("CP-LCO-79: un estado inválido en la URL se ignora con un aviso amarillo y se muestran los consumos de todos los estados")
    void pantalla_filtroDeEstadoInvalido() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).param("estado", "XYZ").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("El estado indicado no es válido, se muestran todos los consumos");
        assertThat(html).contains(ALERTA_ADVERTENCIA);
        verify(consumoInsumoServicio).filtrarConsumosInsumo(eq(ETAPA_HERVIDO), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("CP-LCO-80: un consumo anulado muestra el motivo, la fecha de anulación y el usuario que lo anuló; uno registrado no muestra detalle de anulación")
    void pantalla_detalleDeLaAnulacion() throws Exception {
        // Arrange
        ConsumoInsumoResponseDTO anulado = ConsumoInsumoResponseDTO.builder().id(6L).cantidadConsumida(5.0).tipoConsumo(TipoConsumo.RESERVADO)
                .estado(EstadoTransaccion.ANULADO).motivoAnulacion("Error de carga").fechaAnulacion(LocalDateTime.of(2027, 7, 2, 9, 30)).usuarioAnulacion("operario")
                .fechaRegistro(LocalDateTime.of(2027, 7, 1, 10, 15)).etapaLote(EtapaLoteResponseDTO.builder().id(ETAPA_HERVIDO).build())
                .loteInsumo(LoteInsumoResponseDTO.builder().identificacionLoteProveedor("L-124").insumo(cascade()).build()).build();
        ConsumoInsumoResponseDTO registrado = consumo(7L, ETAPA_HERVIDO, cascade(), "L-123", TipoConsumo.DIRECTO, 15, EstadoTransaccion.REGISTRADO, null);
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(registrado, anulado)));

        // Act
        String consumos = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-consumos-registrados");

        // Assert
        assertThat(texto(elementoConAtributo(consumos, "tr", "data-consumo=\"6\""))).contains("Anulado Motivo: Error de carga Anulado el 02/07/2027 09:30 por operario");
        assertThat(elementoConAtributo(consumos, "tr", "data-consumo=\"7\"")).doesNotContain("data-anulacion");
    }

    @Test
    @DisplayName("CP-LCO-81: filtrar por un estado sin consumos muestra el mensaje del estado indicado en lugar de una tabla vacía")
    void pantalla_filtroDeEstadoSinResultados() throws Exception {
        // Arrange
        when(consumoInsumoServicio.filtrarConsumosInsumo(eq(ETAPA_HERVIDO), any(), any(), eq(EstadoTransaccion.ANULADO), any())).thenReturn(Page.empty());

        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).param("estado", "ANULADO").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String consumos = bloque(html, "data-consumos-registrados");

        // Assert
        assertThat(texto(consumos)).contains("No hay consumos en el estado indicado");
        assertThat(consumos).doesNotContain("<table");
    }

    @Test
    @DisplayName("CP-LCO-76: un valor de la URL que no se puede convertir (lote, consumo o página) en una pantalla redirige con la alerta amarilla El valor indicado no es válido y en el buscador responde 400 con el mensaje")
    void transversal_valoresInvalidosEnLaUrl() throws Exception {
        // Act + Assert: las pantallas vuelven al origen con un aviso, sin error inesperado
        List<MockHttpServletRequestBuilder> pantallas = List.of(
                get("/lotes/abc/etapas/hervido/consumos"),
                get(URL_PANTALLA).param("pagina", "xyz"),
                post(URL_PANTALLA + "/xyz/anular").with(csrf()).param("motivo", "Error de carga"));
        for (MockHttpServletRequestBuilder pantalla : pantallas) {
            mockMvc.perform(pantalla.with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/lotes/12"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/lotes/12"))
                    .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                    .andExpect(flash().attribute("tipo", "warning"));
        }

        // Act + Assert: un modal no puede recibir una página completa
        mockMvc.perform(get(URL_BUSCADOR).param("pagina", "xyz").with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", URL_PANTALLA))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("text/plain;charset=UTF-8"))
                .andExpect(content().string("El valor indicado no es válido"));
        verifyNoInteractions(consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
    }

    @Test
    @DisplayName("CP-LCO-73: matriz de permisos: cada endpoint procede solo con su permiso; con cualquier otro permiso de consumos, lotes o envasados redirige con alerta de error y no invoca al service")
    void transversal_matrizDePermisos() throws Exception {
        // Arrange
        List<EndpointDeConsumo> endpoints = List.of(
                new EndpointDeConsumo("GET pantalla", Permiso.CONSUMO_INSUMO_CONSULTAR, () -> get(URL_PANTALLA)),
                new EndpointDeConsumo("POST reservado", Permiso.CONSUMO_INSUMO_REGISTRAR,
                        () -> post(URL_PANTALLA + "/reservado").param("idLoteInsumo", "5").param("cantidadConsumida", "10")),
                new EndpointDeConsumo("POST directo", Permiso.CONSUMO_INSUMO_REGISTRAR,
                        () -> post(URL_PANTALLA + "/directo").param("idLoteInsumo", "8").param("cantidadConsumida", "5")),
                new EndpointDeConsumo("POST anular", Permiso.CONSUMO_INSUMO_ANULAR, () -> post(URL_PANTALLA + "/7/anular").param("motivo", "Error de carga")),
                new EndpointDeConsumo("GET buscador", Permiso.CONSUMO_INSUMO_REGISTRAR, () -> get(URL_BUSCADOR)));
        List<Permiso> permisosRelacionados = Arrays.stream(Permiso.values())
                .filter(p -> p.name().startsWith("LOTE_") || p.name().startsWith("CONSUMO_INSUMO_") || p.name().startsWith("ENVASADO_")).toList();

        for (EndpointDeConsumo endpoint : endpoints) {
            // Act + Assert: con solo su permiso procede
            ResultActions conPermiso = mockMvc.perform(endpoint.peticion().get().with(conPermisos(endpoint.permiso())).with(csrf()));
            if (endpoint.esConsulta()) {
                conPermiso.andExpect(status().isOk());
            } else {
                conPermiso.andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "success"));
            }

            // Act + Assert: con todos los demás permisos relacionados es rechazado
            org.mockito.Mockito.clearInvocations(loteServicio, consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
            Permiso[] otros = permisosRelacionados.stream().filter(p -> p != endpoint.permiso()).toArray(Permiso[]::new);
            assertRechazado(mockMvc.perform(endpoint.peticion().get().with(conPermisos(otros)).with(csrf())));
            verifyNingunService();
        }
    }

    // ==================== Apoyo de las pruebas ====================

    /**
     * Un endpoint del controller con el permiso que exige y cómo se pide.
     */
    private record EndpointDeConsumo(String nombre, Permiso permiso, Supplier<MockHttpServletRequestBuilder> peticion) {

        boolean esConsulta() {
            return nombre.startsWith("GET");
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private String html(Rol rol) throws Exception {
        return html(como(rol));
    }

    private String html(RequestPostProcessor usuario) throws Exception {
        return mockMvc.perform(get(URL_PANTALLA).with(usuario)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Envía un formulario inválido y sigue la redirección como lo hace el navegador: el Post-Redirect-Get vuelve a la
     * pantalla de consumos con los errores y lo escrito en los atributos flash, y esa pantalla es la que se devuelve.
     */
    private String enviarConErroresYVolver(MockHttpServletRequestBuilder envio) throws Exception {
        MvcResult enviado = mockMvc.perform(envio).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL_PANTALLA)).andReturn();
        return mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(enviado.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/consumo-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String buscador(Rol rol) throws Exception {
        return mockMvc.perform(get(URL_BUSCADOR).with(como(rol)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-lote-insumo :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private MockHttpServletRequestBuilder postReservado(RequestPostProcessor usuario, String idLoteInsumo, String cantidad) {
        return postConsumo(URL_PANTALLA + "/reservado", usuario, idLoteInsumo, cantidad);
    }

    private MockHttpServletRequestBuilder postDirecto(RequestPostProcessor usuario, String idLoteInsumo, String cantidad) {
        return postConsumo(URL_PANTALLA + "/directo", usuario, idLoteInsumo, cantidad);
    }

    private static MockHttpServletRequestBuilder postConsumo(String url, RequestPostProcessor usuario, String idLoteInsumo, String cantidad) {
        MockHttpServletRequestBuilder peticion = post(url).with(usuario).with(csrf());
        if (idLoteInsumo != null) {
            peticion.param("idLoteInsumo", idLoteInsumo);
        }
        if (cantidad != null) {
            peticion.param("cantidadConsumida", cantidad);
        }
        return peticion;
    }

    private MockHttpServletRequestBuilder postAnular(RequestPostProcessor usuario, long idConsumo, String motivo) {
        MockHttpServletRequestBuilder peticion = post(URL_PANTALLA + "/" + idConsumo + "/anular").with(usuario).with(csrf());
        if (motivo != null) {
            peticion.param("motivo", motivo);
        }
        return peticion;
    }

    private void assertRechazoDelService(ResultActions resultado, String mensaje, String tipo) throws Exception {
        resultado.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", tipo));
    }

    private void assertRechazado(ResultActions resultado) throws Exception {
        resultado.andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"))
                .andExpect(flash().attribute("mensaje", SIN_PERMISOS));
    }

    private void verifyNingunService() {
        verifyNoInteractions(loteServicio, consumoInsumoServicio, reservaInsumoServicio, loteInsumoServicio);
    }

    /**
     * La pantalla de solo lectura: el resumen y los consumos registrados se ven, pero no hay formularios de registro,
     * campo de cantidad, selector ni botones Anular.
     */
    private static void assertSoloLectura(String html) {
        assertThat(html).contains("data-resumen-consumo").contains("data-consumos-registrados")
                .doesNotContain("data-insumos-reservados").doesNotContain("data-consumo-directo").doesNotContain("cantidadConsumida")
                .doesNotContain("data-selector").doesNotContain("data-motivo-url");
        // El único formulario es el filtro por estado (GET): ningún formulario de operación ni botón de anulación
        assertThat(contenido(html)).doesNotContain("method=\"post\"").doesNotContain("data-motivo-dialogo");
        assertThat(texto(contenido(html))).doesNotContain("Anular").doesNotContainPattern("Registrar\\b");
    }

    /**
     * Lo que muestra la pantalla dentro del layout, sin el encabezado (que tiene su propio formulario de cierre de sesión).
     */
    private static String contenido(String html) {
        return html.substring(html.indexOf("<main"));
    }

    private void stubDetalle(LoteDetalleResponseDTO detalle) {
        when(loteServicio.buscarDetallePorId(12L)).thenReturn(detalle);
    }

    private void stubLotesInsumo(Page<LoteInsumoResponseDTO> pagina) {
        when(loteInsumoServicio.filtrarLotesInsumoDisponibles(any(Collection.class), any(), any(), any())).thenReturn(pagina);
    }

    private static String botonElegir(String html, long id) {
        Matcher m = Pattern.compile("<button[^>]*data-id=\"" + id + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("botón Elegir del lote %s", id).isTrue();
        return m.group();
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

    /**
     * Lote 12 "PALE_ALE-R7-3" con sus seis etapas (ids 31 a 36, Molienda a Envasado).
     */
    private static LoteDetalleResponseDTO detalle(EstadoLote estado, List<EtapaLoteResponseDTO> etapas) {
        return LoteDetalleResponseDTO.builder().id(12L).identificadorInterno("PALE_ALE-R7-3").estado(estado).volumenObjetivo(300.0)
                .idPlanificacionProduccion(7L).etapas(etapas).etapaPorDefecto(TipoEtapa.HERVIDO).build();
    }

    private static List<EtapaLoteResponseDTO> etapas(EstadoEtapaLote... estados) {
        List<EtapaLoteResponseDTO> etapas = new ArrayList<>();
        for (TipoEtapa tipo : TipoEtapa.values()) {
            etapas.add(EtapaLoteResponseDTO.builder().id(31L + tipo.ordinal()).etapa(tipo).estado(estados[tipo.ordinal()])
                    .equipamiento(EquipamientoResponseDTO.builder().id(10L + tipo.ordinal()).identificadorInterno("EQ-0" + tipo.ordinal()).build()).build());
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

    private static InsumoResponseDTO cascade() {
        return LupuloResponseDTO.builder().id(31L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static InsumoResponseDTO citra() {
        return LupuloResponseDTO.builder().id(32L).nombre("Citra").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static InsumoResponseDTO levadura() {
        return LevaduraResponseDTO.builder().id(41L).nombre("US-05").unidadDeMedida(UnidadDeMedida.GRAMO).build();
    }

    private static InsumoRequeridoResponseDTO requerido(InsumoResponseDTO insumo, double requerida, double reservada, double consumida) {
        return InsumoRequeridoResponseDTO.builder().insumo(insumo).unidadDeMedida(UnidadDeMedida.GRAMO).cantidadRequerida(requerida).cantidadReservada(reservada)
                .cantidadConsumida(consumida).porcentajeConsumido(requerida == 0 ? 0.0 : consumida * 100 / requerida).build();
    }

    private static AvanceInsumoResponseDTO avance(String nombre, double requerida, double consumida, double porcentaje, boolean cumple) {
        return AvanceInsumoResponseDTO.builder().nombreInsumo(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).requerida(requerida).consumida(consumida)
                .porcentaje(porcentaje).cumple(cumple).build();
    }

    private static LoteInsumoResponseDTO loteInsumo(Long id, InsumoResponseDTO insumo, String identificacion, LocalDate vencimiento, double disponible, boolean vencido) {
        return LoteInsumoResponseDTO.builder().id(id).insumo(insumo).identificacionLoteProveedor(identificacion).fechaVencimiento(vencimiento)
                .cantidadDisponible(disponible).vencido(vencido).build();
    }

    private static ReservaInsumoResponseDTO reserva(Long id, LoteInsumoResponseDTO loteInsumo, double saldo) {
        return ReservaInsumoResponseDTO.builder().id(id).cantidadReservada(saldo).loteInsumo(loteInsumo).build();
    }

    private static ConsumoInsumoResponseDTO consumo(Long id, Long idEtapa, InsumoResponseDTO insumo, String identificacion, TipoConsumo tipo, double cantidad,
                                                    EstadoTransaccion estado, String motivo) {
        return ConsumoInsumoResponseDTO.builder().id(id).cantidadConsumida(cantidad).tipoConsumo(tipo).estado(estado).motivoAnulacion(motivo)
                .fechaRegistro(LocalDateTime.of(2027, 7, 1, 10, 15)).etapaLote(EtapaLoteResponseDTO.builder().id(idEtapa).build())
                .loteInsumo(LoteInsumoResponseDTO.builder().identificacionLoteProveedor(identificacion).insumo(insumo).build()).build();
    }

    private static ConsumoInsumoResponseDTO respuestaDeConsumo(String... advertencias) {
        return ConsumoInsumoResponseDTO.builder().id(20L).advertencias(List.of(advertencias)).build();
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
     * El elemento de esa etiqueta que lleva el atributo con su valor (por ejemplo, {@code data-reserva="5"}).
     */
    private static String elementoConAtributo(String html, String etiqueta, String atributoConValor) {
        Matcher m = Pattern.compile("(?s)<" + etiqueta + "\\b[^>]*\\s" + Pattern.quote(atributoConValor) + "[^>]*>.*?</" + etiqueta + ">").matcher(html);
        assertThat(m.find()).as("%s con %s", etiqueta, atributoConValor).isTrue();
        return m.group();
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
     * El valor del primer campo con ese nombre.
     */
    private static String campo(String html, String nombre) {
        Matcher m = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("campo %s", nombre).isTrue();
        Matcher valor = Pattern.compile("value=\"([^\"]*)\"").matcher(m.group());
        assertThat(valor.find()).as("valor del campo %s", nombre).isTrue();
        return HtmlUtils.htmlUnescape(valor.group(1));
    }
}
