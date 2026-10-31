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
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionesEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;
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
        when(servicio.listarLineaDeTiempo(eq(5L), any(), any())).thenReturn(List.of(
                BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LOTE).idReferencia(1L).idLote(12L)
                        .codigoLote("LOT-R1-01").etapa(TipoEtapa.FERMENTACION).estadoEtapa(EstadoEtapaLote.FINALIZADA)
                        .fechaInicio(LocalDateTime.now().minusDays(5)).fechaFin(LocalDateTime.now().minusDays(2)).build(),
                BloqueLineaTiempoEquipamientoResponseDTO.builder().tipo(TipoBloqueLineaTiempo.LIMPIEZA).idReferencia(7L)
                        .observaciones("Limpieza con soda")
                        .fechaInicio(LocalDateTime.now().minusDays(1)).fechaFin(LocalDateTime.now().minusDays(1).plusHours(2)).build()));
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
    @DisplayName("CP-CCE-01: el operario ve la lista con el botón Abrir y sin el ojo del cronograma")
    void lista_operario() throws Exception {
        String html = html(get("/control-equipamiento").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-lista");

        assertThat(html).contains("FER-01").contains("3 de 10").contains("data-boton-abrir").contains("Fermentador")
                .doesNotContain("data-ver-cronograma");
    }

    @Test
    @DisplayName("CP-CCE-02: el gerente ve la lista con el ojo del cronograma y el botón Abrir")
    void lista_gerente() throws Exception {
        String html = html(get("/control-equipamiento").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-lista");

        assertThat(html).contains("data-ver-cronograma").contains("/control-equipamiento/5/cronograma").contains("data-boton-abrir");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-CCE-03: los demás roles no abren la lista, el cronograma ni el panel: redirigen con alerta y no se invoca el service")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String url : List.of("/control-equipamiento", "/control-equipamiento/5", "/control-equipamiento/5/cronograma",
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

    // ==================== Cronograma ====================

    @Test
    @DisplayName("CP-CCE-06: el cronograma del gerente pide los últimos 30 días hasta hoy y dibuja un bloque por actividad con enlaces y leyenda")
    void cronograma_gerente() throws Exception {
        String html = html(get("/control-equipamiento/5/cronograma").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(servicio).listarLineaDeTiempo(eq(5L), desde.capture(), hasta.capture());
        assertThat(desde.getValue().toLocalDate()).isEqualTo(LocalDate.now().minusDays(29));
        assertThat(hasta.getValue().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(html).contains("data-informacion-equipo").contains("LOT-R1-01 · Fermentación").contains("/lotes/12/detalle")
                .contains("/control-equipamiento/5/operaciones/limpieza/7").contains("Etapa de lote finalizada").contains("Limpieza")
                .contains("Inicio: ").contains("Fin: ").contains("(30 días)");
    }

    @Test
    @DisplayName("CP-CCE-07: el operario no abre el cronograma del equipo")
    void cronograma_operarioRechazado() throws Exception {
        mockMvc.perform(get("/control-equipamiento/5/cronograma").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("mensaje", SIN_PERMISOS));
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CCE-08: sin actividad en el período el cronograma muestra el mensaje de vacío y no el diagrama")
    void cronograma_sinActividad() throws Exception {
        when(servicio.listarLineaDeTiempo(eq(5L), any(), any())).thenReturn(List.of());

        String html = html(get("/control-equipamiento/5/cronograma").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");

        assertThat(html).contains("El equipamiento no tiene actividad en el período").doesNotContain("data-gantt");
    }

    @Test
    @DisplayName("CP-CCE-09: Anterior y Siguiente desplazan el período su misma longitud y Hoy vuelve al período por defecto")
    void cronograma_navegacion() throws Exception {
        String html = html(get("/control-equipamiento/5/cronograma").param("desde", "2026-10-01").param("hasta", "2026-10-10")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");

        assertThat(html).contains("/control-equipamiento/5/cronograma?desde=2026-09-21&amp;hasta=2026-09-30")
                .contains("/control-equipamiento/5/cronograma?desde=2026-10-11&amp;hasta=2026-10-20").contains("(10 días)");
    }

    @Test
    @DisplayName("CP-CCE-10: un período inválido se avisa y se muestra el período por defecto")
    void cronograma_periodoInvalido() throws Exception {
        String html = html(get("/control-equipamiento/5/cronograma").param("desde", "2026-10-10").param("hasta", "2026-10-01")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");

        assertThat(html).contains("La fecha de fin del período no puede ser anterior a la fecha de inicio").contains("(30 días)");
    }

    // ==================== Panel ====================

    @Test
    @DisplayName("CP-CCE-11: el operario con el equipo EN_LIMPIEZA ve el formulario de registro de limpieza con fechas, observaciones y placeholder, y Anular solo en la operación anulable")
    void panel_operarioConFormulario() throws Exception {
        String html = html(get("/control-equipamiento/5").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(html).contains("data-registrar-operacion").contains("type=\"datetime-local\"").contains("name=\"fecha\"").doesNotContain("fechaFin").doesNotContain("fechaInicio").contains("Ej: Limpieza con soda cáustica y enjuague").contains("novalidate")
                .contains("/control-equipamiento/5/limpiezas").contains("data-pestanas-operaciones");
        assertThat(count(html, "data-motivo-url")).isEqualTo(1);
        assertThat(html).contains("/control-equipamiento/5/limpiezas/7/anular").doesNotContain("/limpiezas/6/anular");
    }

    @Test
    @DisplayName("CP-CCE-12: con un estado que no lo permite el panel no tiene formulario y explica el motivo con el texto del service")
    void panel_estadoNoPermite() throws Exception {
        configurarEquipo(EstadoOperativo.EN_USO);

        String html = html(get("/control-equipamiento/5").param("pestana", "fallas").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(html).contains("data-aviso-registro").contains("No se puede registrar una falla porque el equipamiento está en uso")
                .doesNotContain("data-registrar-operacion");
    }

    @Test
    @DisplayName("CP-CCE-13: el gerente ve el panel en modo consulta, sin formulario ni botones de anular")
    void panel_gerenteSoloConsulta() throws Exception {
        String html = html(get("/control-equipamiento/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(html).contains("data-aviso-solo-lectura").contains("Solo puede consultar las operaciones del equipamiento")
                .contains("Error de carga").doesNotContain("data-registrar-operacion").doesNotContain("data-motivo-url").doesNotContain("dialogo-anular");
    }

    @Test
    @DisplayName("CP-CCE-14: una pestaña inexistente muestra la primera, y la pestaña pedida se marca como activa")
    void panel_pestanas() throws Exception {
        String inexistente = html(get("/control-equipamiento/5").param("pestana", "otra").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");
        String mantenimientos = html(get("/control-equipamiento/5").param("pestana", "mantenimientos").with(como(Rol.OPERARIO_DE_PRODUCCION)), "control_equipamiento/control-panel");

        assertThat(inexistente).contains("Registrar una limpieza");
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
    @DisplayName("CP-CCE-18: Registrado por aparece solo en el detalle de la operación, no en el panel ni en el cronograma, y Responsable no aparece en ninguna")
    void registradoPorSoloEnElDetalle() throws Exception {
        String detalle = html(get("/control-equipamiento/5/operaciones/limpieza/7").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/operacion-detalle");
        String panel = html(get("/control-equipamiento/5").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-panel");
        String cronograma = html(get("/control-equipamiento/5/cronograma").with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");

        assertThat(detalle).contains("Registrado por");
        assertThat(panel).doesNotContain("Registrado por").doesNotContain("operario1").doesNotContain("Responsable");
        assertThat(cronograma).doesNotContain("Registrado por").doesNotContain("operario1").doesNotContain("Responsable");
        assertThat(detalle).doesNotContain("Responsable");
    }

    @Test
    @DisplayName("CP-CCE-19: un período en el borde del rango de fechas no ofrece el enlace que se saldría del rango y una fecha fuera de rango se avisa como inválida")
    void cronograma_periodoEnElBordeDelRango() throws Exception {
        String borde = html(get("/control-equipamiento/5/cronograma").param("desde", "2200-12-20").param("hasta", "2200-12-31")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");
        String fuera = html(get("/control-equipamiento/5/cronograma").param("desde", "9999-12-31")
                .with(como(Rol.GERENTE_DE_PRODUCCION)), "control_equipamiento/control-cronograma");

        assertThat(borde).contains("data-periodo-anterior").doesNotContain("data-periodo-siguiente");
        assertThat(fuera).contains("El período indicado no es válido").contains("(30 días)");
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
