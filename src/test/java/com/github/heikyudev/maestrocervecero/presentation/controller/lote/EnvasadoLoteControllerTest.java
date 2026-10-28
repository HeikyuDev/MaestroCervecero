package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.RegistrarEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEnvasadoLoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ResumenEnvasadoResponseDTO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

@WebMvcTest(EnvasadoLoteController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class EnvasadoLoteControllerTest {

    private static final String URL_PANTALLA = "/lotes/12/envasados";
    private static final String URL_BUSCADOR = URL_PANTALLA + "/buscador/barriles";
    private static final String ALERTA_EXITO = "bg-teal-50 border-teal-200 text-teal-800";
    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final long ETAPA_ENVASADO = 36L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILoteServicio loteServicio;

    @MockitoBean
    private IEnvasadoLoteServicio envasadoLoteServicio;

    @MockitoBean
    private IBarrilServicio barrilServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /**
     * Escenario base: lote 12 "PALE_ALE-R7-3" En ejecución, volumen objetivo 300 L, con el Envasado (etapa 36) En curso y
     * el fermentador "F-01" (capacidad útil 330 L). Envasados 7 ("B-001", 50 L), 8 ("B-002", 50 L) y 9 ("B-003", 20 L)
     * Registrados (120 L en total) y 10 ("B-004", 30 L) Anulado. Cada caso cambia solo lo que dice.
     */
    @BeforeEach
    void escenarioBase() {
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, etapasEnCurso(TipoEtapa.ENVASADO)));
        when(envasadoLoteServicio.resumirEnvasado(ETAPA_ENVASADO)).thenReturn(resumen(120, 330, 210, 0));
        when(envasadoLoteServicio.filtrarEnvasadosLote(eq(ETAPA_ENVASADO), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                envasado(10L, ETAPA_ENVASADO, "B-004", 30, EstadoTransaccion.ANULADO, "Barril equivocado"),
                envasado(9L, ETAPA_ENVASADO, "B-003", 20, EstadoTransaccion.REGISTRADO, null),
                envasado(8L, ETAPA_ENVASADO, "B-002", 50, EstadoTransaccion.REGISTRADO, null),
                envasado(7L, ETAPA_ENVASADO, "B-001", 50, EstadoTransaccion.REGISTRADO, null))));
        when(envasadoLoteServicio.buscarPorId(9L)).thenReturn(envasado(9L, ETAPA_ENVASADO, "B-003", 20, EstadoTransaccion.REGISTRADO, null));
        when(envasadoLoteServicio.buscarPorId(15L)).thenReturn(envasado(15L, 66L, "B-009", 20, EstadoTransaccion.REGISTRADO, null));
        when(envasadoLoteServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el envasado de lote con ID: 99"));
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenReturn(envasado(11L, ETAPA_ENVASADO, "B-001", 50, EstadoTransaccion.REGISTRADO, null));
        when(envasadoLoteServicio.anularEnvasadoLote(any(), any())).thenReturn(envasado(9L, ETAPA_ENVASADO, "B-003", 20, EstadoTransaccion.ANULADO, "Motivo"));
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                barril(3L, "B-001", 50, "Barrilitos SA"), barril(4L, "B-002", 30, null))));
        when(barrilServicio.buscarPorId(3L)).thenReturn(barril(3L, "B-001", 50, "Barrilitos SA"));
    }

    // ==================== GET /lotes/{idLote}/envasados ====================

    @Test
    @DisplayName("CP-LEN-01: la pantalla de envasados es una página con el título, el link Volver al lote, el código, el estado de la etapa, el indicador, el formulario de registro y la tabla, sin diálogo modal para registrar")
    void pantalla_debeMostrarLosBloques() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/envasado-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("Envasados", "PALE_ALE-R7-3", "En curso");
        assertThat(enlaceDe(html, "Volver al lote")).isEqualTo("/lotes/12?etapa=envasado");
        assertThat(html).contains("data-indicador-envasado").contains("data-registrar-envasado").contains("data-envasados-registrados");
        assertThat(cuenta(html, "<dialog")).isEqualTo(2);
        assertThat(html).contains("id=\"dialogo-anular\"").contains("id=\"dialogo-detalle-anulacion\"");
    }

    @Test
    @DisplayName("CP-LEN-02: el indicador dice cuánto se envasó frente a la capacidad útil del fermentador, el volumen objetivo y cuánto queda, con la barra de avance")
    void pantalla_indicadorDeCapacidadUtil() throws Exception {
        // Act
        String indicador = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-indicador-envasado");

        // Assert
        assertThat(texto(indicador)).contains("120 L de 330 L (capacidad útil del fermentador F-01)", "Volumen objetivo: 300 L", "Quedan 210 L");
        assertThat(indicador).contains("role=\"progressbar\"").contains("aria-valuenow=\"36\"")
                .doesNotContain("data-aviso-exceso-objetivo").doesNotContain("data-aviso-capacidad-alcanzada");
    }

    @Test
    @DisplayName("CP-LEN-03: sin envasados el indicador muestra 0 L de la capacidad útil, la barra en 0 y la tabla dice que no hay envasados registrados")
    void pantalla_sinEnvasados() throws Exception {
        // Arrange
        when(envasadoLoteServicio.resumirEnvasado(ETAPA_ENVASADO)).thenReturn(resumen(0, 330, 330, 0));
        when(envasadoLoteServicio.filtrarEnvasadosLote(eq(ETAPA_ENVASADO), any(), any(), any())).thenReturn(Page.empty());

        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        String indicador = bloque(html, "data-indicador-envasado");
        assertThat(texto(indicador)).contains("0 L de 330 L (capacidad útil del fermentador F-01)", "Quedan 330 L");
        assertThat(indicador).contains("aria-valuenow=\"0\"");
        String registrados = bloque(html, "data-envasados-registrados");
        assertThat(texto(registrados)).contains("No hay envasados registrados");
        assertThat(registrados).doesNotContain("<table");
    }

    @Test
    @DisplayName("CP-LEN-04: superar el volumen objetivo agrega un aviso informativo (no de error) y el formulario sigue visible")
    void pantalla_superoElVolumenObjetivo() throws Exception {
        // Arrange
        when(envasadoLoteServicio.resumirEnvasado(ETAPA_ENVASADO)).thenReturn(resumen(310, 330, 20, 10));

        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);
        String indicador = bloque(html, "data-indicador-envasado");
        String aviso = bloque(indicador, "data-aviso-exceso-objetivo");

        // Assert
        assertThat(texto(indicador)).contains("310 L de 330 L", "Quedan 20 L");
        assertThat(texto(aviso)).isEqualTo("El total envasado supera el volumen objetivo del lote (300 L)");
        assertThat(aviso).contains("bg-sky-50").doesNotContain("rose");
        assertThat(html).contains("data-registrar-envasado");
    }

    @Test
    @DisplayName("CP-LEN-05: con la capacidad útil alcanzada el indicador dice Quedan 0 L con el aviso correspondiente y el formulario sigue visible")
    void pantalla_capacidadUtilAlcanzada() throws Exception {
        // Arrange
        when(envasadoLoteServicio.resumirEnvasado(ETAPA_ENVASADO)).thenReturn(resumen(330, 330, 0, 30));

        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);
        String indicador = bloque(html, "data-indicador-envasado");

        // Assert
        assertThat(texto(indicador)).contains("330 L de 330 L", "Quedan 0 L", "Se alcanzó la capacidad útil del fermentador");
        assertThat(indicador).contains("aria-valuenow=\"100\"");
        assertThat(html).contains("data-registrar-envasado");
    }

    @Test
    @DisplayName("CP-LEN-06: el formulario de registro es un POST con CSRF y novalidate, con el selector de barril (modal, sin alta ni quitar), la cantidad con placeholder, los asteriscos rojos y el botón Crear envasado")
    void pantalla_formularioDeRegistro() throws Exception {
        // Act
        String registrar = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-registrar-envasado");

        // Assert
        Matcher formulario = Pattern.compile("<form[^>]*>").matcher(registrar);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("method=\"post\"").contains("action=\"/lotes/12/envasados\"").contains("novalidate");
        assertThat(registrar).contains("name=\"_csrf\"").contains("data-selector-buscar").contains("bi-list-ul")
                .contains("data-buscador-url=\"" + URL_BUSCADOR + "\"").contains("placeholder=\"Seleccione barril\"")
                .contains("name=\"idBarril\"").contains("name=\"cantidad\"").contains("placeholder=\"Ej: 50\"")
                .doesNotContain("data-selector-menu").doesNotContain("data-selector-alta").doesNotContain("data-selector-limpiar").doesNotContain("<select");
        assertThat(texto(registrar)).contains("Barril *", "Cantidad a envasar (L) *", "Nuevo envasado", "Crear envasado");
        assertThat(cuenta(registrar, "text-rose-600")).isEqualTo(3);
        assertThat(textoDelSelector(registrar, "Seleccione barril")).isEmpty();
    }

    @Test
    @DisplayName("CP-LEN-07: la tabla lista del más reciente al más antiguo, con el anulado atenuado, con el botón de detalle de la anulación y sin botón Anular; se piden de a 20 por id descendente sin filtrar por barril ni por estado")
    void pantalla_tablaDeEnvasados() throws Exception {
        // Act
        String registrados = bloque(html(Rol.OPERARIO_DE_PRODUCCION), "data-envasados-registrados");
        String anulado = elementoConAtributo(registrados, "tr", "data-envasado=\"10\"");

        // Assert
        assertThat(IntStream.of(10, 9, 8, 7).map(id -> registrados.indexOf("data-envasado=\"" + id + "\"")).boxed().toList()).isSorted().doesNotContain(-1);
        assertThat(texto(registrados)).contains("Barril", "Cantidad (L)", "Fecha de registro", "Estado");
        assertThat(celdas(anulado, "B-004")).containsExactly("B-004", "30", "01/07/2027 10:15", "Anulado", "");
        assertThat(anulado).contains("opacity-60").doesNotContain("data-motivo-url").doesNotContain("Barril equivocado</").contains("data-detalle-anulacion")
                .contains("data-detalle-titulo=\"Detalle de la anulación\"").contains("data-detalle-motivo=\"Barril equivocado\"").contains("data-detalle-usuario=\"operario\"").contains("data-detalle-fecha=\"02/07/2027 09:30\"").contains("aria-label=\"Ver detalle de la anulación\"");
        assertThat(celdas(registrados, "B-003")).containsExactly("B-003", "20", "01/07/2027 10:15", "Registrado", "Anular");
        assertThat(celdas(registrados, "B-002")).containsExactly("B-002", "50", "01/07/2027 10:15", "Registrado", "Anular");
        assertThat(celdas(registrados, "B-001")).containsExactly("B-001", "50", "01/07/2027 10:15", "Registrado", "Anular");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(envasadoLoteServicio).filtrarEnvasadosLote(eq(ETAPA_ENVASADO), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("CP-LEN-08: Anular abre el diálogo de motivo: el botón lleva la URL de anulación y el barril, y la página incluye el diálogo con el campo motivo")
    void pantalla_anularAbreElDialogoDeMotivo() throws Exception {
        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);
        Matcher boton = Pattern.compile("<button[^>]*data-motivo-dialogo[^>]*data-motivo-url=\"/lotes/12/envasados/9/anular\"[^>]*>").matcher(html);

        // Assert
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).contains("data-motivo-dialogo=\"dialogo-anular\"").contains("data-motivo-nombre=\"B-003\"");
        assertThat(cuenta(html, "data-motivo-url")).isEqualTo(3);
        String dialogo = elementoConAtributo(html, "dialog", "id=\"dialogo-anular\"");
        assertThat(texto(dialogo)).contains("Anular envasado");
        assertThat(dialogo).contains("name=\"motivo\"").contains("placeholder=\"Ej: Se eligió el barril equivocado\"").contains("text-rose-600").contains("name=\"_csrf\"");
    }

    @Test
    @DisplayName("CP-LEN-09: con 45 envasados y pagina=1 muestra Página 2 de 3 y los links a las páginas anterior y siguiente de la misma pantalla")
    void pantalla_paginacion() throws Exception {
        // Arrange
        when(envasadoLoteServicio.filtrarEnvasadosLote(eq(ETAPA_ENVASADO), any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(envasado(7L, ETAPA_ENVASADO, "B-001", 50, EstadoTransaccion.REGISTRADO, null)), PageRequest.of(1, 20), 45));

        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).param("pagina", "1").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("45 envasados", "Página 2 de 3");
        assertThat(enlaceDe(html, "Anterior")).isEqualTo(URL_PANTALLA + "?pagina=0");
        assertThat(enlaceDe(html, "Siguiente")).isEqualTo(URL_PANTALLA + "?pagina=2");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(envasadoLoteServicio).filtrarEnvasadosLote(eq(ETAPA_ENVASADO), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-LEN-10: las alertas del Post-Redirect-Get muestran el mensaje de éxito en el layout")
    void pantalla_alertasDelPostRedirectGet() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION))
                        .flashAttr("mensaje", "Envasado creado correctamente").flashAttr("tipo", "success"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(html).contains(ALERTA_EXITO);
        assertThat(texto(html)).contains("Envasado creado correctamente");
    }

    @Test
    @DisplayName("CP-LEN-11: con el Envasado fuera de curso la pantalla es de solo lectura: aviso, indicador y tabla, sin formulario, selector ni botones Anular")
    void pantalla_etapaQueNoEstaEnCurso() throws Exception {
        // Arrange
        stubDetalle(detalle(EstadoLote.EN_EJECUCION, etapasEnCurso(TipoEtapa.HERVIDO)));

        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(texto(html)).contains("La etapa de Envasado no está en curso, solo se pueden consultar los envasados");
        assertSoloLectura(html);
        assertThat(texto(bloque(html, "data-indicador-envasado"))).contains("120 L de 330 L");
        assertThat(texto(bloque(html, "data-envasados-registrados"))).contains("B-001", "B-004");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = EstadoLote.class, names = {"FINALIZADO", "CANCELADO"})
    @DisplayName("CP-LEN-12: un lote cerrado se consulta sin operar: aviso de solo lectura y sin formulario ni botones Anular")
    void pantalla_loteCerrado(EstadoLote estado) throws Exception {
        // Arrange
        stubDetalle(detalle(estado, etapas(EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA,
                EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA)));

        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(texto(html)).contains("La etapa de Envasado no está en curso, solo se pueden consultar los envasados");
        assertSoloLectura(html);
    }

    @Test
    @DisplayName("CP-LEN-13: un lote inexistente redirige al origen con la alerta de error y el mensaje del service")
    void pantalla_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.buscarDetallePorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act + Assert
        mockMvc.perform(get("/lotes/99/envasados").with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes"))
                .andExpect(flash().attribute("mensaje", "No se encontró el lote con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
        verifyNoInteractions(envasadoLoteServicio, barrilServicio);
    }

    @Test
    @DisplayName("CP-LEN-14: la pantalla pide una vez el lote, el resumen del envasado de la etapa y los envasados de la etapa, y no consulta barriles")
    void pantalla_datosPedidosALosServices() throws Exception {
        // Act
        html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        verify(loteServicio, times(1)).buscarDetallePorId(12L);
        verify(envasadoLoteServicio, times(1)).resumirEnvasado(ETAPA_ENVASADO);
        verify(envasadoLoteServicio, times(1)).filtrarEnvasadosLote(eq(ETAPA_ENVASADO), any(), any(), any());
        verifyNoMoreInteractions(loteServicio, envasadoLoteServicio);
        verifyNoInteractions(barrilServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LEN-15: los roles sin ENVASADO_CONSULTAR no abren la pantalla: redirigen con alerta de error y no se invoca ningún service")
    void pantalla_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL_PANTALLA).with(como(rol))));
        verifyNingunService();
    }

    @Test
    @DisplayName("CP-LEN-49: si el envasado no se puede resumir, la pantalla vuelve al detalle del lote con el motivo en una alerta amarilla")
    void pantalla_resumenNoDisponible() throws Exception {
        // Arrange
        when(envasadoLoteServicio.resumirEnvasado(ETAPA_ENVASADO)).thenThrow(new ReglaNegocioException("La etapa Envasado no tiene un fermentador como equipamiento"));

        // Act + Assert
        mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", URL_PANTALLA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/12"))
                .andExpect(flash().attribute("mensaje", "La etapa Envasado no tiene un fermentador como equipamiento"))
                .andExpect(flash().attribute("tipo", "warning"));
        verify(envasadoLoteServicio, never()).filtrarEnvasadosLote(any(), any(), any(), any());
    }

    // ==================== POST /lotes/{idLote}/envasados ====================

    @Test
    @DisplayName("CP-LEN-16: un envasado correcto llama al service con la etapa 36 y el DTO enlazado, y vuelve a la pantalla con la alerta de éxito")
    void registrar_debeRegistrarYVolverALaPantalla() throws Exception {
        // Act
        mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Envasado creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        // Assert
        ArgumentCaptor<RegistrarEnvasadoLoteFormDTO> form = ArgumentCaptor.forClass(RegistrarEnvasadoLoteFormDTO.class);
        verify(envasadoLoteServicio).registrarEnvasadoLote(eq(ETAPA_ENVASADO), form.capture());
        assertThat(form.getValue().getIdBarril()).isEqualTo(3L);
        assertThat(form.getValue().getCantidad()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CP-LEN-17: el formulario vacío redirige a la pantalla (Post-Redirect-Get) que se muestra completa con el resumen de errores y no invoca al service de registro")
    void registrar_formularioVacio() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), null, null));

        // Assert
        assertThat(texto(bloque(html, "data-errores-formulario"))).contains("No se pudo crear el envasado", "El barril es obligatorio", "La cantidad a envasar es obligatoria");
        assertThat(html).contains("data-indicador-envasado").contains("data-registrar-envasado").contains("data-envasados-registrados");
        assertThat(texto(bloque(html, "data-indicador-envasado"))).contains("120 L de 330 L");
        verify(envasadoLoteServicio, never()).registrarEnvasadoLote(any(), any());
        verifyNoInteractions(barrilServicio);
    }

    @Test
    @DisplayName("CP-LEN-18: una cantidad no numérica muestra Ingrese un número válido, conserva lo escrito y el barril elegido y no invoca al service de registro")
    void registrar_cantidadNoNumerica() throws Exception {
        // Act
        String html = enviarConErroresYVolver(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "abc"));
        String registrar = bloque(html, "data-registrar-envasado");

        // Assert
        assertThat(texto(bloque(html, "data-errores-formulario"))).contains("Ingrese un número válido");
        assertThat(texto(registrar)).contains("Ingrese un número válido");
        assertThat(campo(registrar, "cantidad")).isEqualTo("abc");
        assertThat(campo(registrar, "idBarril")).isEqualTo("3");
        assertThat(textoDelSelector(registrar, "Seleccione barril")).isEqualTo("B-001");
        assertThat(texto(bloque(registrar, "data-barril-elegido"))).contains("Fabricante: Barrilitos SA", "Capacidad: 50 L");
        verify(envasadoLoteServicio, never()).registrarEnvasadoLote(any(), any());
    }

    @Test
    @DisplayName("CP-LEN-19: sin barril muestra El barril es obligatorio debajo del selector y conserva la cantidad")
    void registrar_sinBarril() throws Exception {
        // Act
        String registrar = bloque(enviarConErroresYVolver(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), null, "50")), "data-registrar-envasado");

        // Assert
        assertThat(texto(registrar)).contains("El barril es obligatorio");
        assertThat(campo(registrar, "cantidad")).isEqualTo("50");
        assertThat(textoDelSelector(registrar, "Seleccione barril")).isEmpty();
        verify(envasadoLoteServicio, never()).registrarEnvasadoLote(any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"0", "-5", "NaN", "Infinity"})
    @DisplayName("CP-LEN-20: una cantidad no válida para el negocio llega al service con el valor recibido y su rechazo vuelve al origen con la alerta amarilla")
    void registrar_cantidadNoValidaParaElService(String cantidad) throws Exception {
        // Arrange
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException("La cantidad a envasar debe ser mayor a 0"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", cantidad).header("Referer", URL_PANTALLA)),
                "La cantidad a envasar debe ser mayor a 0", "warning");
        ArgumentCaptor<RegistrarEnvasadoLoteFormDTO> form = ArgumentCaptor.forClass(RegistrarEnvasadoLoteFormDTO.class);
        verify(envasadoLoteServicio).registrarEnvasadoLote(eq(ETAPA_ENVASADO), form.capture());
        assertThat(form.getValue().getCantidad()).isEqualTo(Double.valueOf(cantidad));
    }

    @Test
    @DisplayName("CP-LEN-21: un barril que no está disponible lo rechaza el service y vuelve al origen con la alerta amarilla")
    void registrar_barrilNoDisponible() throws Exception {
        // Arrange
        String mensaje = "El barril debe encontrarse en estado operativo Disponible para poder envasar en él";
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "50").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LEN-22: una cantidad mayor a la capacidad del barril la rechaza el service y vuelve al origen con la alerta amarilla")
    void registrar_cantidadMayorALaCapacidadDelBarril() throws Exception {
        // Arrange
        String mensaje = "La cantidad a envasar no puede superar la capacidad del barril seleccionado (50 L)";
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "80").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LEN-23: una cantidad mayor a la capacidad útil del fermentador la rechaza el service y vuelve al origen con la alerta amarilla")
    void registrar_cantidadMayorALaCapacidadUtil() throws Exception {
        // Arrange
        String mensaje = "La cantidad a envasar supera la capacidad útil del fermentador (quedan 10 L disponibles para envasar)";
        when(envasadoLoteServicio.resumirEnvasado(ETAPA_ENVASADO)).thenReturn(resumen(320, 330, 10, 20));
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "20").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "El lote debe encontrarse en estado En ejecución para poder registrar un envasado",
            "La etapa debe estar en curso para poder registrar un envasado"})
    @DisplayName("CP-LEN-24: los rechazos del service por un lote o una etapa que no admiten el envasado vuelven al origen con la alerta amarilla y su mensaje")
    void registrar_loteOEtapaQueNoAdmitenElEnvasado(String mensaje) throws Exception {
        // Arrange
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "50").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LEN-25: un barril inexistente vuelve al origen con la alerta de error y el mensaje del service")
    void registrar_barrilInexistente() throws Exception {
        // Arrange
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new RecursoNoEncontradoException("No se encontró el barril con ID: 99"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "99", "50").header("Referer", URL_PANTALLA)),
                "No se encontró el barril con ID: 99", "danger");
    }

    @Test
    @DisplayName("CP-LEN-26: registrar sin token CSRF es rechazado (redirige) y no llega al service")
    void registrar_sinCsrf() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION)).param("idBarril", "3").param("cantidad", "50"))
                .andExpect(status().is3xxRedirection());
        verifyNingunService();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LEN-27: los roles sin ENVASADO_REGISTRAR no registran envasados: redirigen con alerta de error y el service no se invoca")
    void registrar_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(postRegistrar(como(rol), "3", "50")));
        verifyNingunService();
    }

    // ==================== POST /lotes/{idLote}/envasados/{idEnvasado}/anular ====================

    @Test
    @DisplayName("CP-LEN-28: una anulación correcta llama al service con el envasado 9 y el motivo, y vuelve a la pantalla con la alerta de éxito")
    void anular_debeAnularYVolverALaPantalla() throws Exception {
        // Act
        mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 9L, "Se eligió el barril equivocado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "Envasado anulado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        // Assert
        ArgumentCaptor<AnularEnvasadoLoteFormDTO> form = ArgumentCaptor.forClass(AnularEnvasadoLoteFormDTO.class);
        verify(envasadoLoteServicio).anularEnvasadoLote(eq(9L), form.capture());
        assertThat(form.getValue().getMotivoAnulacion()).isEqualTo("Se eligió el barril equivocado");
    }

    @Test
    @DisplayName("CP-LEN-29: sin motivo el controller no valida: llama al service con el motivo nulo y su rechazo vuelve al origen con la alerta amarilla")
    void anular_sinMotivo() throws Exception {
        // Arrange
        when(envasadoLoteServicio.anularEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException("El motivo de anulación es obligatorio"));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 9L, null).header("Referer", URL_PANTALLA)),
                "El motivo de anulación es obligatorio", "warning");
        ArgumentCaptor<AnularEnvasadoLoteFormDTO> form = ArgumentCaptor.forClass(AnularEnvasadoLoteFormDTO.class);
        verify(envasadoLoteServicio).anularEnvasadoLote(eq(9L), form.capture());
        assertThat(form.getValue().getMotivoAnulacion()).isNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "No se puede anular el envasado porque el barril asociado ya no mantiene su contenido intacto",
            "Solo se pueden anular envasados en estado Registrado",
            "El lote debe encontrarse en estado En ejecución para poder anular un envasado"})
    @DisplayName("CP-LEN-30/31/32: los rechazos de negocio del service (barril sin contenido intacto, envasado ya anulado, lote que no está en ejecución) vuelven al origen con la alerta amarilla y su mensaje")
    void anular_rechazosDelService(String mensaje) throws Exception {
        // Arrange
        when(envasadoLoteServicio.anularEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 9L, "Error de carga").header("Referer", URL_PANTALLA)), mensaje, "warning");
    }

    @Test
    @DisplayName("CP-LEN-33: un envasado inexistente vuelve al origen con la alerta de error y el mensaje del service, sin anular")
    void anular_envasadoInexistente() throws Exception {
        // Act + Assert
        assertRechazoDelService(mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 99L, "Error de carga").header("Referer", URL_PANTALLA)),
                "No se encontró el envasado de lote con ID: 99", "danger");
        verify(envasadoLoteServicio, never()).anularEnvasadoLote(any(), any());
    }

    @Test
    @DisplayName("CP-LEN-34: un envasado de otro lote vuelve a la pantalla con la alerta amarilla El envasado no pertenece a este lote y no invoca la anulación")
    void anular_envasadoDeOtroLote() throws Exception {
        // Act + Assert
        mockMvc.perform(postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 15L, "Error de carga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL_PANTALLA))
                .andExpect(flash().attribute("mensaje", "El envasado no pertenece a este lote"))
                .andExpect(flash().attribute("tipo", "warning"));
        verify(envasadoLoteServicio, never()).anularEnvasadoLote(any(), any());
    }

    @Test
    @DisplayName("CP-LEN-35: anular sin token CSRF es rechazado (redirige) y no llega al service")
    void anular_sinCsrf() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL_PANTALLA + "/9/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("motivo", "Error de carga"))
                .andExpect(status().is3xxRedirection());
        verifyNingunService();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LEN-36: los roles sin ENVASADO_ANULAR no anulan envasados: redirigen con alerta de error y el service no se invoca")
    void anular_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(postAnular(como(rol), 9L, "Error de carga")));
        verifyNingunService();
    }

    // ==================== GET /lotes/{idLote}/envasados/buscador/barriles ====================

    @Test
    @DisplayName("CP-LEN-37: el buscador devuelve un fragmento con el criterio Identificador interno, las columnas y por fila el botón Elegir con el id, el texto, el fabricante y la capacidad")
    void buscador_fragmento() throws Exception {
        // Act
        String html = buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        assertThat(html).doesNotContain("<html");
        assertThat(texto(html)).contains("Buscar barril", "Identificador interno", "Número de serie", "SN-B-001", "Capacidad (L)", "Fabricante", "Estado", "Disponible", "Barrilitos SA", "Elegir");
        Matcher formulario = Pattern.compile("<form[^>]*>").matcher(html);
        assertThat(formulario.find()).isTrue();
        assertThat(formulario.group()).contains("data-modal-buscar").contains("action=\"" + URL_BUSCADOR + "\"").contains("method=\"get\"");
        assertThat(html).contains("name=\"identificadorInterno\"").contains("placeholder=\"Ej: BRL-01\"").doesNotContain("<select");
        assertThat(botonElegir(html, 3)).contains("data-texto=\"B-001\"").contains("data-capacidad=\"50\"").contains("data-fabricante=\"Barrilitos SA\"");
        assertThat(botonElegir(html, 4)).contains("data-texto=\"B-002\"").contains("data-capacidad=\"30\"").contains("data-fabricante=\"-\"");
    }

    @Test
    @DisplayName("CP-LEN-38: sin parámetros pide al service solo barriles Disponibles, de a 8 y en orden ascendente por identificador interno")
    void buscador_soloBarrilesDisponibles() throws Exception {
        // Act
        buscador(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), eq(EstadoOperativoBarril.DISPONIBLE), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(8);
        assertThat(pageable.getValue().getSort().getOrderFor("identificadorInterno").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("CP-LEN-39: el identificador llega recortado al service, uno en blanco equivale a no filtrar y el campo conserva lo escrito")
    void buscador_criterioDeIdentificador() throws Exception {
        // Act
        String escrito = mockMvc.perform(get(URL_BUSCADOR).param("identificadorInterno", " b-001 ").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        mockMvc.perform(get(URL_BUSCADOR).param("identificadorInterno", "  ").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk());

        // Assert
        verify(barrilServicio).filtrarBarriles(eq("b-001"), isNull(), isNull(), eq(EstadoOperativoBarril.DISPONIBLE), isNull(), any());
        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), eq(EstadoOperativoBarril.DISPONIBLE), isNull(), any());
        assertThat(campo(escrito, "identificadorInterno")).isEqualTo(" b-001 ");
    }

    @Test
    @DisplayName("CP-LEN-40: con 10 barriles disponibles se muestran de a 8, con Página 1 de 2 y los links del modal conservando el identificador")
    void buscador_paginacionDeA8() throws Exception {
        // Arrange
        List<BarrilResponseDTO> diez = IntStream.rangeClosed(1, 10).mapToObj(i -> barril((long) i, "B-0" + (i < 10 ? "0" + i : i), 50, "Barrilitos SA")).toList();
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenAnswer(invocacion -> {
            Pageable pageable = invocacion.getArgument(5);
            int desde = (int) pageable.getOffset();
            return new PageImpl<>(diez.subList(desde, Math.min(desde + pageable.getPageSize(), diez.size())), pageable, diez.size());
        });

        // Act
        String primera = mockMvc.perform(get(URL_BUSCADOR).param("identificadorInterno", "B-0").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String segunda = mockMvc.perform(get(URL_BUSCADOR).param("identificadorInterno", "B-0").param("pagina", "1").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(cuenta(primera, "data-elegir")).isEqualTo(8);
        assertThat(texto(primera)).contains("10 barriles", "Página 1 de 2");
        assertThat(enlaceDe(primera, "Siguiente")).isEqualTo(URL_BUSCADOR + "?pagina=1&identificadorInterno=B-0");
        assertThat(primera).contains("data-modal-get=\"true\"");
        assertThat(cuenta(segunda, "data-elegir")).isEqualTo(2);
        assertThat(texto(segunda)).contains("Página 2 de 2");
        assertThat(enlaceDe(segunda, "Anterior")).isEqualTo(URL_BUSCADOR + "?pagina=0&identificadorInterno=B-0");
    }

    @Test
    @DisplayName("CP-LEN-41: sin resultados muestra el mensaje claro y una página negativa equivale a la primera")
    void buscador_sinResultados() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        // Act
        String vacio = buscador(Rol.OPERARIO_DE_PRODUCCION);
        mockMvc.perform(get(URL_BUSCADOR).param("pagina", "-3").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk());

        // Assert
        assertThat(texto(vacio)).contains("No se encontraron barriles disponibles con los criterios indicados");
        assertThat(vacio).doesNotContain("data-elegir");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(barrilServicio, times(2)).filtrarBarriles(any(), any(), any(), any(), any(), pageable.capture());
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isZero();
    }

    @Test
    @DisplayName("CP-LEN-42: un lote inexistente se informa dentro del fragmento con el mensaje del service, sin criterios ni filas, porque un modal no puede redirigir")
    void buscador_loteInexistente() throws Exception {
        // Arrange
        when(loteServicio.buscarDetallePorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el lote con ID: 99"));

        // Act
        String html = mockMvc.perform(get("/lotes/99/envasados/buscador/barriles").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-barril :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(texto(html)).contains("No se encontró el lote con ID: 99");
        assertThat(html).doesNotContain("data-modal-buscar").doesNotContain("data-elegir");
        verifyNoInteractions(barrilServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-LEN-43: los roles sin ENVASADO_REGISTRAR no abren el buscador: redirigen con alerta de error y no se invoca ningún service")
    void buscador_losRolesNoAutorizadosSonRechazados(Rol rol) throws Exception {
        // Act + Assert
        assertRechazado(mockMvc.perform(get(URL_BUSCADOR).with(como(rol))));
        verifyNingunService();
    }

    // ==================== Transversales ====================

    @Test
    @DisplayName("CP-LEN-44: registrar y anular son Post-Redirect-Get a la misma pantalla, con la alerta de éxito y sin responder 200")
    void transversal_postRedirectGet() throws Exception {
        // Act + Assert
        for (MockHttpServletRequestBuilder peticion : List.of(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "50"),
                postAnular(como(Rol.OPERARIO_DE_PRODUCCION), 9L, "Error de carga"))) {
            mockMvc.perform(peticion)
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(URL_PANTALLA))
                    .andExpect(flash().attribute("tipo", "success"));
        }
    }

    @Test
    @DisplayName("CP-LEN-45: un rechazo del service sin Referer redirige a la raíz con la alerta amarilla y el mensaje del service")
    void transversal_rechazoSinReferer() throws Exception {
        // Arrange
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException("La etapa debe estar en curso para poder registrar un envasado"));

        // Act + Assert
        mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("mensaje", "La etapa debe estar en curso para poder registrar un envasado"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-LEN-46: sin JavaScript el registro es un formulario POST con action y CSRF, novalidate, data-un-solo-envio y sin required, min ni pattern; solo el selector del barril y el diálogo de motivo usan modales.js")
    void transversal_sinJavaScript() throws Exception {
        // Act
        String html = html(Rol.OPERARIO_DE_PRODUCCION);

        // Assert
        Matcher formularios = Pattern.compile("<form[^>]*>").matcher(contenido(html));
        List<String> etiquetas = new ArrayList<>();
        while (formularios.find()) {
            etiquetas.add(formularios.group());
        }
        assertThat(etiquetas).hasSize(2);
        assertThat(etiquetas).allSatisfy(form -> assertThat(form).contains("method=\"post\"").contains("action=\"").contains("novalidate").contains("data-un-solo-envio"));
        assertThat(cuenta(contenido(html), "name=\"_csrf\"")).isEqualTo(2);
        assertThat(html).doesNotContain(" required").doesNotContain(" min=").doesNotContain(" pattern=")
                .contains("/js/modales.js").contains("/js/envasado-lista.js");
    }

    @Test
    @DisplayName("CP-LEN-50: un error de validación y luego el rechazo del service no terminan en una ruta sin pantalla: el navegador queda siempre en la pantalla de envasados, sin 405 ni bucle")
    void transversal_errorDeValidacionYLuegoRechazoDelService() throws Exception {
        // Arrange
        when(envasadoLoteServicio.registrarEnvasadoLote(any(), any())).thenThrow(new ReglaNegocioException("El barril no está disponible"));

        // Act: 1) el formulario es inválido y el navegador vuelve a la pantalla
        MvcResult invalido = mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", null).header("Referer", URL_PANTALLA))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String ubicacion = invalido.getResponse().getRedirectedUrl();
        mockMvc.perform(get(ubicacion).with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(invalido.getFlashMap())).andExpect(status().isOk());

        // Act: 2) desde esa ubicación, el service rechaza un envío con datos válidos
        MvcResult rechazado = mockMvc.perform(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "50").header("Referer", ubicacion))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String destino = rechazado.getResponse().getRedirectedUrl();
        String html = mockMvc.perform(get(destino).with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(rechazado.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Assert
        assertThat(ubicacion).isEqualTo(URL_PANTALLA);
        assertThat(destino).isEqualTo(URL_PANTALLA);
        assertThat(texto(html)).contains("El barril no está disponible");
    }

    @Test
    @DisplayName("CP-LEN-51: un valor de la URL que no se puede convertir (lote, envasado o página) en una pantalla redirige con la alerta amarilla El valor indicado no es válido y en el buscador responde 400 con el mensaje")
    void transversal_valoresInvalidosEnLaUrl() throws Exception {
        // Act + Assert: las pantallas vuelven al origen con un aviso, sin error inesperado
        List<MockHttpServletRequestBuilder> pantallas = List.of(
                get("/lotes/abc/envasados"),
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
        verifyNoInteractions(envasadoLoteServicio, barrilServicio);
    }

    @Test
    @DisplayName("CP-LEN-47: el selector y la cantidad llevan asterisco rojo y placeholder de ejemplo, la unidad va en la etiqueta y en la cabecera, y la pantalla no tiene emojis ni alertas con punto final")
    void transversal_ayudasVisuales() throws Exception {
        // Act
        List<String> paginas = List.of(
                html(Rol.OPERARIO_DE_PRODUCCION),
                mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION))
                                .flashAttr("mensaje", "Envasado creado correctamente").flashAttr("tipo", "success"))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                enviarConErroresYVolver(postRegistrar(como(Rol.OPERARIO_DE_PRODUCCION), "3", "abc")));

        // Assert
        String registrar = bloque(paginas.get(0), "data-registrar-envasado");
        assertThat(cuenta(registrar, "text-rose-600")).isEqualTo(3);
        assertThat(registrar).contains("placeholder=\"Seleccione barril\"").contains("placeholder=\"Ej: 50\"");
        assertThat(texto(registrar)).contains("Cantidad a envasar (L) *");
        assertThat(texto(bloque(paginas.get(0), "data-envasados-registrados"))).contains("Cantidad (L)");
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

    @Test
    @DisplayName("CP-LEN-48: matriz de permisos: cada endpoint procede solo con su permiso; con cualquier otro permiso de envasados, consumos o lotes redirige con alerta de error y no invoca al service")
    void transversal_matrizDePermisos() throws Exception {
        // Arrange
        List<EndpointDeEnvasado> endpoints = List.of(
                new EndpointDeEnvasado("GET pantalla", Permiso.ENVASADO_CONSULTAR, () -> get(URL_PANTALLA)),
                new EndpointDeEnvasado("POST registrar", Permiso.ENVASADO_REGISTRAR, () -> post(URL_PANTALLA).param("idBarril", "3").param("cantidad", "50")),
                new EndpointDeEnvasado("POST anular", Permiso.ENVASADO_ANULAR, () -> post(URL_PANTALLA + "/9/anular").param("motivo", "Error de carga")),
                new EndpointDeEnvasado("GET buscador", Permiso.ENVASADO_REGISTRAR, () -> get(URL_BUSCADOR)));
        List<Permiso> permisosRelacionados = Arrays.stream(Permiso.values())
                .filter(p -> p.name().startsWith("LOTE_") || p.name().startsWith("CONSUMO_INSUMO_") || p.name().startsWith("ENVASADO_")).toList();

        for (EndpointDeEnvasado endpoint : endpoints) {
            // Act + Assert: con solo su permiso procede
            ResultActions conPermiso = mockMvc.perform(endpoint.peticion().get().with(conPermisos(endpoint.permiso())).with(csrf()));
            if (endpoint.esConsulta()) {
                conPermiso.andExpect(status().isOk());
            } else {
                conPermiso.andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "success"));
            }

            // Act + Assert: con todos los demás permisos relacionados es rechazado
            org.mockito.Mockito.clearInvocations(loteServicio, envasadoLoteServicio, barrilServicio);
            Permiso[] otros = permisosRelacionados.stream().filter(p -> p != endpoint.permiso()).toArray(Permiso[]::new);
            assertRechazado(mockMvc.perform(endpoint.peticion().get().with(conPermisos(otros)).with(csrf())));
            verifyNingunService();
        }
    }

    // ==================== Apoyo de las pruebas ====================

    /**
     * Un endpoint del controller con el permiso que exige y cómo se pide.
     */
    private record EndpointDeEnvasado(String nombre, Permiso permiso, Supplier<MockHttpServletRequestBuilder> peticion) {

        boolean esConsulta() {
            return nombre.startsWith("GET");
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private String html(Rol rol) throws Exception {
        return mockMvc.perform(get(URL_PANTALLA).with(como(rol))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Envía un formulario inválido y sigue la redirección como lo hace el navegador: el Post-Redirect-Get vuelve a la
     * pantalla de envasados con los errores y lo escrito en los atributos flash, y esa pantalla es la que se devuelve.
     */
    private String enviarConErroresYVolver(MockHttpServletRequestBuilder envio) throws Exception {
        MvcResult enviado = mockMvc.perform(envio).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL_PANTALLA)).andReturn();
        return mockMvc.perform(get(URL_PANTALLA).with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(enviado.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/envasado-lista"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String buscador(Rol rol) throws Exception {
        return mockMvc.perform(get(URL_BUSCADOR).with(como(rol)))
                .andExpect(status().isOk())
                .andExpect(view().name("lote/buscador-barril :: buscador"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static MockHttpServletRequestBuilder postRegistrar(RequestPostProcessor usuario, String idBarril, String cantidad) {
        MockHttpServletRequestBuilder peticion = post(URL_PANTALLA).with(usuario).with(csrf());
        if (idBarril != null) {
            peticion.param("idBarril", idBarril);
        }
        if (cantidad != null) {
            peticion.param("cantidad", cantidad);
        }
        return peticion;
    }

    private static MockHttpServletRequestBuilder postAnular(RequestPostProcessor usuario, long idEnvasado, String motivo) {
        MockHttpServletRequestBuilder peticion = post(URL_PANTALLA + "/" + idEnvasado + "/anular").with(usuario).with(csrf());
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
        verifyNoInteractions(loteServicio, envasadoLoteServicio, barrilServicio);
    }

    /**
     * La pantalla de solo lectura: el indicador y los envasados registrados se ven, pero no hay formulario de registro,
     * campo de cantidad, selector ni botones Anular.
     */
    private static void assertSoloLectura(String html) {
        assertThat(html).contains("data-indicador-envasado").contains("data-envasados-registrados")
                .doesNotContain("data-registrar-envasado").doesNotContain("name=\"cantidad\"")
                .doesNotContain("data-selector").doesNotContain("data-motivo-url").doesNotContain("dialogo-anular");
        assertThat(contenido(html)).doesNotContain("<form").doesNotContain("<button");
        assertThat(texto(contenido(html))).doesNotContain("Anular").doesNotContain("Nuevo envasado").doesNotContain("Crear envasado");
    }

    /**
     * Lo que muestra la pantalla dentro del layout, sin el encabezado (que tiene su propio formulario de cierre de sesión).
     */
    private static String contenido(String html) {
        // Sin el diálogo compartido del detalle de una anulación ni sus botones (solo informan, no operan)
        return html.substring(html.indexOf("<main"))
                .replaceAll("(?s)<dialog id=\"dialogo-detalle-anulacion\".*?</dialog>", "")
                .replaceAll("(?s)<button[^>]*data-detalle-anulacion[^>]*>.*?</button>", "");
    }

    private void stubDetalle(LoteDetalleResponseDTO detalle) {
        when(loteServicio.buscarDetallePorId(12L)).thenReturn(detalle);
    }

    private static String botonElegir(String html, long id) {
        Matcher m = Pattern.compile("<button[^>]*data-id=\"" + id + "\"[^>]*>").matcher(html);
        assertThat(m.find()).as("botón Elegir del barril %s", id).isTrue();
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
                .idPlanificacionProduccion(7L).etapas(etapas).etapaPorDefecto(TipoEtapa.ENVASADO).build();
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

    /**
     * El resumen del envasado del lote 12: fermentador "F-01" y volumen objetivo 300 L.
     */
    private static ResumenEnvasadoResponseDTO resumen(double total, double capacidadUtil, double restante, double exceso) {
        return ResumenEnvasadoResponseDTO.builder().totalEnvasado(total).capacidadUtilFermentador(capacidadUtil).restante(restante)
                .excesoSobreVolumenObjetivo(exceso).identificadorFermentador("F-01").volumenObjetivo(300.0).build();
    }

    private static BarrilResponseDTO barril(Long id, String identificador, double capacidad, String fabricante) {
        return BarrilResponseDTO.builder().id(id).identificadorInterno(identificador).numeroSerie("SN-" + identificador).capacidad(capacidad).estadoOperativo(EstadoOperativoBarril.DISPONIBLE)
                .fabricante(fabricante == null ? null : FabricanteBarrilResponseDTO.builder().id(1L).nombre(fabricante).build()).build();
    }

    private static EnvasadoLoteResponseDTO envasado(Long id, Long idEtapa, String barril, double cantidad, EstadoTransaccion estado, String motivo) {
        return EnvasadoLoteResponseDTO.builder().id(id).cantidadEnvasada(cantidad).estado(estado).motivoAnulacion(motivo)
                .usuarioAnulacion(motivo != null ? "operario" : null).fechaAnulacion(motivo != null ? LocalDateTime.of(2027, 7, 2, 9, 30) : null)
                .fechaRegistro(LocalDateTime.of(2027, 7, 1, 10, 15)).etapaLote(EtapaLoteResponseDTO.builder().id(idEtapa).build())
                .barril(BarrilResponseDTO.builder().id(id + 100).identificadorInterno(barril).build()).build();
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
     * El elemento de esa etiqueta que lleva el atributo con su valor (por ejemplo, {@code data-envasado="9"}).
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
