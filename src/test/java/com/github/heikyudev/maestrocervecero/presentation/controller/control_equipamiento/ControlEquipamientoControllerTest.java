package com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ActividadEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoActividadEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ControlEquipamientoController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
public class ControlEquipamientoControllerTest {

    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IControlEquipamientoServicio servicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /** Escenario base: fermentador 5 "FER-01" EN_LIMPIEZA con una limpieza registrada anulable (7) y una anulada (6). */
    @BeforeEach
    void escenarioBase() {
        when(servicio.filtrarEquipamientosParaControl(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                EquipamientoControlResponseDTO.builder().id(5L).identificadorInterno("FER-01").tipo(TipoEquipamiento.FERMENTADOR)
                        .estadoOperativo(EstadoOperativo.EN_LIMPIEZA).usosDesdeUltimoMantenimiento(3L).usosMaximosAntesMantenimiento(10)
                        .tipoUltimaOperacion(TipoOperacionEquipamiento.LIMPIEZA).fechaUltimaOperacion(FIN).build())));
        configurarEquipo(EstadoOperativo.EN_LIMPIEZA);
        when(servicio.buscarResumen(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el equipamiento con ID: 99"));
        when(servicio.listarHistorial(eq(5L), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                ActividadEquipamientoResponseDTO.builder().tipo(TipoActividadEquipamiento.LIMPIEZA).idReferencia(7L).observaciones("Limpieza con soda")
                        .fechaInicio(LocalDateTime.of(2026, 10, 22, 8, 0)).estadoOperativoResultante(EstadoOperativo.DISPONIBLE).build(),
                ActividadEquipamientoResponseDTO.builder().tipo(TipoActividadEquipamiento.LOTE).idReferencia(1L).idLote(12L).codigoLote("LOT-R1-01")
                        .etapa(TipoEtapa.FERMENTACION).estadoEtapa(EstadoEtapaLote.EN_CURSO).fechaInicio(LocalDateTime.of(2026, 10, 20, 8, 0)).enCurso(true)
                        .estadoOperativoResultante(EstadoOperativo.EN_USO).build())));
    }

    private void configurarEquipo(EstadoOperativo estado) {
        when(servicio.buscarResumen(5L)).thenReturn(EquipamientoResumenResponseDTO.builder().id(5L).identificadorInterno("FER-01")
                .tipo(TipoEquipamiento.FERMENTADOR).estadoOperativo(estado).usosMaximosAntesMantenimiento(10).usosDesdeUltimoMantenimiento(3L)
                .capacidadTotal(1000.0).capacidadUtil(900.0).fechaUltimaLimpieza(FIN).build());
        when(servicio.listarOperaciones(5L)).thenReturn(OperacionesEquipamientoResponseDTO.builder()
                .limpiezas(List.of(
                        operacion(7L, TipoOperacionEquipamiento.LIMPIEZA, EstadoTransaccion.REGISTRADO, true),
                        operacion(6L, TipoOperacionEquipamiento.LIMPIEZA, EstadoTransaccion.ANULADO, false)))
                .fallas(List.of()).mantenimientos(List.of()).build());
    }

    private static OperacionEquipamientoResponseDTO operacion(Long id, TipoOperacionEquipamiento tipo, EstadoTransaccion estado, boolean anulable) {
        return OperacionEquipamientoResponseDTO.builder().id(id).tipo(tipo).estado(estado).fecha(FIN)
                .observaciones("Limpieza con soda cáustica").registradoPor("operario1").fechaRegistro(FIN.plusMinutes(5))
                .motivoAnulacion(estado == EstadoTransaccion.ANULADO ? "Error de carga" : null)
                .fechaAnulacion(estado == EstadoTransaccion.ANULADO ? FIN.plusHours(1) : null).anulable(anulable).build();
    }

    // ==================== Lista ====================

    @Test
    @DisplayName("CP-CCE-01: el operario ve la lista con el botón Abrir y sin el ojo del historial")
    void lista_operario() throws Exception {
        String html = html(get("/control-equipamiento").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-lista");

        assertThat(html).contains("FER-01").contains("3 de 10").contains("data-boton-abrir").contains("Fermentador")
                .doesNotContain("data-ver-historial");
    }

    @Test
    @DisplayName("CP-CCE-02: el gerente ve la lista con el ojo del historial y el botón Abrir")
    void lista_gerente() throws Exception {
        String html = html(get("/control-equipamiento").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-lista");

        assertThat(html).contains("data-ver-historial").contains("/control-equipamiento/5/historial").contains("data-boton-abrir");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-CCE-03: los demás roles no abren la lista, el historial ni el panel: redirigen con alerta y no se invoca el service")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String url : List.of("/control-equipamiento", "/control-equipamiento/5", "/control-equipamiento/5/historial",
                "/control-equipamiento/5/operaciones/limpieza/7")) {
            mockMvc.perform(get(url).with(como(rol))).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("mensaje", SIN_PERMISOS)).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CCE-04: los criterios de la lista se pasan al service (identificador sin espacios, tipo y estado operativo)")
    void lista_filtros() throws Exception {
        mockMvc.perform(get("/control-equipamiento").param("identificador", " fer ").param("tipo", "FERMENTADOR")
                .param("estadoOperativo", "EN_LIMPIEZA").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarEquipamientosParaControl(eq("fer"), eq(TipoEquipamiento.FERMENTADOR), eq(EstadoOperativo.EN_LIMPIEZA), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("CP-CCE-05: un tipo y un estado operativo inválidos no filtran y se avisa con una alerta warning")
    void lista_filtrosInvalidos() throws Exception {
        String html = html(get("/control-equipamiento").param("tipo", "XXX").param("estadoOperativo", "YYY")
                .with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-lista");

        verify(servicio).filtrarEquipamientosParaControl(isNull(), isNull(), isNull(), any());
        assertThat(html).contains("El tipo indicado no es válido, se muestran todos").contains("El estado operativo indicado no es válido");
    }

    // ==================== Historial ====================

    @Test
    @DisplayName("CP-CCE-06: el historial del gerente muestra la información del equipo, los filtros y una fila por actividad con su estado resultante y los enlaces al lote y a la operación")
    void historial_gerente() throws Exception {
        String html = html(get("/control-equipamiento/5/historial").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");

        verify(servicio).listarHistorial(eq(5L), isNull(), isNull(), isNull(), any(Pageable.class));
        assertThat(html).contains("Historial del equipamiento").contains("data-informacion-equipo").contains("name=\"tipo\"").contains("name=\"fechaDesde\"")
                .contains("name=\"fechaHasta\"").contains("Ej: 01/07/2027").contains("Fecha y hora").contains("Estado resultante");
        String tabla = html.substring(html.indexOf("data-historial"));
        String encabezado = tabla.substring(tabla.indexOf("<thead"), tabla.indexOf("</thead>"));
        assertThat(encabezado.split("<th ", -1)).hasSize(5);
        assertThat(encabezado).contains("Fecha y hora").contains("Operación").contains("Estado resultante").contains(">Acciones<").doesNotContain("Detalle").doesNotContain(">Tipo<");
        assertThat(tabla).contains("Lote · LOT-R1-01 · Fermentación").contains("href=\"/lotes/12/detalle\"").contains("20/10/2026 08:00").contains("En curso")
                .contains("href=\"/control-equipamiento/5/operaciones/limpieza/7\"").contains("Limpieza con soda").contains("22/10/2026 08:00")
                .contains("En uso").contains("Disponible");
        // Las observaciones no se escriben en la fila: van en el botón i que abre el diálogo compartido
        assertThat(tabla).contains("data-detalle-titulo=\"Observaciones de la limpieza\"").contains("data-detalle-motivo=\"Limpieza con soda\"")
                .contains("data-detalle-etiqueta-motivo=\"Observaciones\"").contains("aria-label=\"Ver observaciones\"")
                .doesNotContain(">Limpieza con soda<");
        // Ningún dato es un enlace: la navegación va en los botones de la columna Acciones
        String cuerpo = tabla.substring(tabla.indexOf("<tbody"), tabla.indexOf("</tbody>"));
        assertThat(cuerpo).doesNotContain("text-amber-700").contains("data-ver-actividad").contains("aria-label=\"Ver el lote LOT-R1-01\"").contains("class=\"col-acciones");
        assertThat(cuerpo.substring(cuerpo.indexOf("class=\"col-acciones"))).contains("Ver observaciones");
    }

    @Test
    @DisplayName("CP-CCE-07: el operario no abre el historial del equipo")
    void historial_operarioRechazado() throws Exception {
        mockMvc.perform(get("/control-equipamiento/5/historial").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("mensaje", SIN_PERMISOS));
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CCE-08: sin actividad el historial muestra El equipamiento no tiene operaciones registradas, y con filtros, No se encontraron resultados")
    void historial_vacio() throws Exception {
        when(servicio.listarHistorial(eq(5L), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        String sinFiltros = html(get("/control-equipamiento/5/historial").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");
        String conFiltros = html(get("/control-equipamiento/5/historial").param("tipo", "FALLA").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");

        assertThat(sinFiltros).contains("El equipamiento no tiene operaciones registradas");
        assertThat(conFiltros).contains("No se encontraron resultados").doesNotContain("El equipamiento no tiene operaciones registradas");
    }

    @Test
    @DisplayName("CP-CCE-09: los filtros de tipo y de fechas se pasan al service (la fecha hasta incluye todo su día) y se conservan en el formulario y en la paginación")
    void historial_filtros() throws Exception {
        String html = html(get("/control-equipamiento/5/historial").param("tipo", "falla").param("fechaDesde", "2026-10-01").param("fechaHasta", "2026-10-31")
                .param("pagina", "1").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).listarHistorial(eq(5L), eq(TipoActividadEquipamiento.FALLA), desde.capture(), hasta.capture(), pagina.capture());
        assertThat(desde.getValue()).isEqualTo(LocalDateTime.of(2026, 10, 1, 0, 0));
        assertThat(hasta.getValue().toLocalDate()).isEqualTo(java.time.LocalDate.of(2026, 10, 31));
        assertThat(hasta.getValue().getHour()).isEqualTo(23);
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(html).contains("value=\"2026-10-01\"").contains("value=\"2026-10-31\"");
    }

    @Test
    @DisplayName("CP-CCE-10: un tipo inválido se avisa y no filtra, y un rango invertido muestra el aviso del service con la lista vacía")
    void historial_filtrosInvalidos() throws Exception {
        when(servicio.listarHistorial(eq(5L), any(), eq(LocalDateTime.of(2026, 10, 10, 0, 0)), any(), any()))
                .thenThrow(new com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde", "hasta"));

        String tipoInvalido = html(get("/control-equipamiento/5/historial").param("tipo", "XXX").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");
        String invertido = html(get("/control-equipamiento/5/historial").param("fechaDesde", "2026-10-10").param("fechaHasta", "2026-10-01")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");

        assertThat(tipoInvalido).contains("El tipo indicado no es válido, se muestran todos");
        assertThat(invertido).contains("La fecha hasta no puede ser anterior a la fecha desde").contains("No se encontraron resultados");
    }

    // ==================== Panel ====================

    @Test
    @DisplayName("CP-CCE-20: la tabla de operaciones del panel no escribe las observaciones en la fila: el botón i las abre en el diálogo compartido, con el título del tipo y la fecha de la operación")
    void panel_observacionesEnElDialogo() throws Exception {
        String html = html(get("/control-equipamiento/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        String tabla = html.substring(html.indexOf("data-operaciones-registradas"));
        assertThat(tabla).contains("data-detalle-titulo=\"Observaciones de la limpieza\"").contains("data-detalle-motivo=\"Limpieza con soda cáustica\"")
                .contains("data-detalle-fecha=\"").contains("data-detalle-sin-usuario").contains("title=\"Ver observaciones\"")
                .doesNotContain(">Limpieza con soda cáustica<");
        String encabezado = tabla.substring(tabla.indexOf("<thead"), tabla.indexOf("</thead>"));
        assertThat(encabezado).doesNotContain("Observaciones").contains("Fecha");
        String cuerpo = tabla.substring(tabla.indexOf("<tbody"), tabla.indexOf("</tbody>"));
        String celdaAcciones = cuerpo.substring(cuerpo.indexOf("class=\"col-acciones"), cuerpo.indexOf("</td>", cuerpo.indexOf("class=\"col-acciones")));
        assertThat(celdaAcciones).contains("Ver observaciones").contains("data-ver-operacion").contains("data-motivo-dialogo");
        assertThat(cuerpo).doesNotContain("text-amber-700");
        assertThat(html).contains("id=\"dialogo-detalle-anulacion\"").contains("data-detalle-etiqueta-motivo-destino");
    }

    @Test
    @DisplayName("CP-CCE-21: la columna Acciones del panel lleva siempre el ojo y el botón i, y Anular solo en la operación anulable y solo para el operario; los títulos de las tablas concuerdan en género")
    void panel_columnaAcciones() throws Exception {
        when(servicio.listarOperaciones(5L)).thenReturn(OperacionesEquipamientoResponseDTO.builder()
                .limpiezas(List.of(operacion(6L, TipoOperacionEquipamiento.LIMPIEZA, EstadoTransaccion.REGISTRADO, false)))
                .fallas(List.of()).mantenimientos(List.of(operacion(8L, TipoOperacionEquipamiento.MANTENIMIENTO, EstadoTransaccion.REGISTRADO, false))).build());

        String sinAnulable = html(get("/control-equipamiento/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");
        String mantenimientos = html(get("/control-equipamiento/5").param("pestana", "mantenimientos").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");
        configurarEquipo(EstadoOperativo.EN_LIMPIEZA);
        String gerente = html(get("/control-equipamiento/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(sinAnulable).contains("Limpiezas registradas").contains(">Acciones<").contains("data-ver-operacion").doesNotContain("data-motivo-url");
        assertThat(mantenimientos).contains("Mantenimientos registrados");
        assertThat(gerente).contains(">Acciones<").contains("data-ver-operacion").contains("Ver observaciones").doesNotContain("data-motivo-url");
    }

    @Test
    @DisplayName("CP-CCE-22: la tabla de operaciones del panel se pagina de a 20 con el pie de paginación del sistema, que conserva la pestaña")
    void panel_paginacion() throws Exception {
        List<OperacionEquipamientoResponseDTO> muchas = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            muchas.add(operacion(i, TipoOperacionEquipamiento.LIMPIEZA, EstadoTransaccion.REGISTRADO, false));
        }
        when(servicio.listarOperaciones(5L)).thenReturn(OperacionesEquipamientoResponseDTO.builder().limpiezas(muchas).fallas(List.of()).mantenimientos(List.of()).build());

        String primera = html(get("/control-equipamiento/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");
        String segunda = html(get("/control-equipamiento/5").param("pagina", "1").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(count(primera, "data-operacion=")).isEqualTo(20);
        assertThat(primera).contains(">25<").contains(">operaciones<").contains("Página <span>1</span> de <span>2</span>").contains("pestana=limpiezas&amp;pagina=1");
        assertThat(count(segunda, "data-operacion=")).isEqualTo(5);
    }

    @Test
    @DisplayName("CP-CCE-11: el operario con el equipo EN_LIMPIEZA ve la tabla de limpiezas con el botón + Nueva limpieza hacia su formulario, sin formulario incrustado, y Anular solo en la operación anulable")
    void panel_operarioConBotonNueva() throws Exception {
        String html = html(get("/control-equipamiento/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(html).contains("data-nueva-operacion").contains("Nueva limpieza").contains("href=\"/control-equipamiento/5/limpiezas/nueva\"")
                .contains("bi-plus-lg").contains("data-pestanas-operaciones").doesNotContain("datetime-local");
        assertThat(count(html, "data-motivo-url")).isEqualTo(1);
        assertThat(html).contains("/control-equipamiento/5/limpiezas/7/anular").doesNotContain("/limpiezas/6/anular");
    }

    @Test
    @DisplayName("CP-CCE-12: con un estado que no lo permite el panel no tiene formulario y explica el motivo con el texto del service")
    void panel_estadoNoPermite() throws Exception {
        configurarEquipo(EstadoOperativo.EN_USO);

        String html = html(get("/control-equipamiento/5").param("pestana", "fallas").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(html).contains("data-aviso-registro").contains("No se puede registrar una falla porque el equipamiento está en uso")
                .doesNotContain("data-nueva-operacion");
    }

    @Test
    @DisplayName("CP-CCE-13: el gerente ve el panel en modo consulta, sin formulario ni botones de anular")
    void panel_gerenteSoloConsulta() throws Exception {
        String html = html(get("/control-equipamiento/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(html).contains("data-aviso-solo-lectura").contains("Solo puede consultar las operaciones del equipamiento")
                .contains("Error de carga").doesNotContain(">Limpieza con soda cáustica<").doesNotContain("data-nueva-operacion").doesNotContain("data-motivo-url").doesNotContain("dialogo-anular");
    }

    @Test
    @DisplayName("CP-CCE-14: una pestaña inexistente muestra la primera, y la pestaña pedida se marca como activa")
    void panel_pestanas() throws Exception {
        String inexistente = html(get("/control-equipamiento/5").param("pestana", "otra").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");
        String mantenimientos = html(get("/control-equipamiento/5").param("pestana", "mantenimientos").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(inexistente).contains("Nueva limpieza");
        assertThat(mantenimientos).contains("No hay mantenimientos registrados").contains("pestana=mantenimientos");
    }

    // ==================== Detalle de una operación ====================

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"OPERARIO_DE_PRODUCCION", "GERENTE_DE_PRODUCCION"})
    @DisplayName("CP-CCE-15: ambos roles ven el detalle de una operación con la información del equipo y los datos por tema")
    void detalleOperacion(Rol rol) throws Exception {
        String html = html(get("/control-equipamiento/5/operaciones/limpieza/7").with(como(rol)), "control_equipamiento/operacion-detalle");

        assertThat(html).contains("data-informacion-equipo").contains("data-operacion-tipo").contains("Limpieza con soda cáustica")
                .contains("Fecha de registro").contains("Registrado por").contains("operario1").doesNotContain("Responsable").doesNotContain("data-operacion-anulacion");
    }

    @Test
    @DisplayName("CP-CCE-16: el detalle de una operación anulada muestra cuándo y por qué, sin el usuario")
    void detalleOperacion_anulada() throws Exception {
        String html = html(get("/control-equipamiento/5/operaciones/limpieza/6").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/operacion-detalle");

        assertThat(html.substring(html.indexOf("data-seccion-operacion"), html.indexOf("</section>", html.indexOf("data-seccion-operacion")))).doesNotContain("Anulado por").doesNotContain("Responsable");
        assertThat(html).contains("data-operacion-anulacion").contains("Error de carga").contains("Fecha de anulación");
    }

    @Test
    @DisplayName("CP-CCE-17: una operación o un equipamiento inexistente vuelve al origen con alerta de error")
    void detalleOperacion_inexistente() throws Exception {
        mockMvc.perform(get("/control-equipamiento/5/operaciones/falla/70").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/control-equipamiento"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get("/control-equipamiento/99").with(como(Rol.OPERARIO_DE_PRODUCCION)).header("Referer", "/control-equipamiento"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-CCE-18: Registrado por aparece solo en el detalle de la operación, no en el panel ni en el historial, y Responsable no aparece en ninguna")
    void registradoPorSoloEnElDetalle() throws Exception {
        String detalle = html(get("/control-equipamiento/5/operaciones/limpieza/7").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/operacion-detalle");
        String panel = html(get("/control-equipamiento/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-panel");
        String historial = html(get("/control-equipamiento/5/historial").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-historial");

        assertThat(detalle).contains("Registrado por");
        assertThat(panel).doesNotContain("Registrado por").doesNotContain("operario1").doesNotContain("Responsable");
        assertThat(historial).doesNotContain("Registrado por").doesNotContain("operario1").doesNotContain("Responsable");
        assertThat(detalle).doesNotContain("Responsable");
    }

    // ==================== Apoyo ====================

    private String html(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String vista) throws Exception {
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
