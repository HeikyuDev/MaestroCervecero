package com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.AnulacionLimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FallaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.LimpiezaEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MantenimientoEquipamientoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFallaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.ILimpiezaEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMantenimientoEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento.ControlEquipamientoControllerTest.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OperacionEquipamientoController.class, ControlEquipamientoController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class OperacionEquipamientoControllerTest {

    private static final String BASE = "/control-equipamiento/5";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ILimpiezaEquipamientoServicio limpiezaServicio;
    @MockitoBean
    private IFallaEquipamientoServicio fallaServicio;
    @MockitoBean
    private IMantenimientoEquipamientoServicio mantenimientoServicio;
    @MockitoBean
    private IControlEquipamientoServicio controlServicio;
    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @BeforeEach
    void escenarioBase() {
        when(controlServicio.buscarResumen(5L)).thenReturn(EquipamientoResumenResponseDTO.builder().id(5L).identificadorInterno("FER-01")
                .tipo(TipoEquipamiento.FERMENTADOR).estadoOperativo(EstadoOperativo.EN_LIMPIEZA).usosMaximosAntesMantenimiento(10).usosDesdeUltimoMantenimiento(3L).build());
        when(controlServicio.listarOperaciones(5L)).thenReturn(OperacionesEquipamientoResponseDTO.builder()
                .limpiezas(List.of(operacion(TipoOperacionEquipamiento.LIMPIEZA))).fallas(List.of(operacion(TipoOperacionEquipamiento.FALLA)))
                .mantenimientos(List.of(operacion(TipoOperacionEquipamiento.MANTENIMIENTO))).build());
    }

    private static OperacionEquipamientoResponseDTO operacion(TipoOperacionEquipamiento tipo) {
        return OperacionEquipamientoResponseDTO.builder().id(7L).tipo(tipo).estado(EstadoTransaccion.REGISTRADO).anulable(true).build();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registro(String ruta, Rol rol, String inicio, String fin, String obs) {
        return post(BASE + "/" + ruta).with(como(rol)).with(csrf()).param("fechaInicio", inicio).param("fechaFin", fin).param("observaciones", obs);
    }

    @Test
    @DisplayName("CP-OPE-01: el operario registra una limpieza: el service recibe el equipamiento de la ruta, las fechas y las observaciones, y vuelve a la pestaña con la alerta de éxito")
    void registrarLimpieza() throws Exception {
        mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "2026-10-20T12:00", "Limpieza con soda"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "?pestana=limpiezas"))
                .andExpect(flash().attribute("mensaje", "Limpieza registrada correctamente")).andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<LimpiezaEquipamientoFormDTO> form = ArgumentCaptor.forClass(LimpiezaEquipamientoFormDTO.class);
        verify(limpiezaServicio).registrarLimpiezaEquipamiento(form.capture());
        assertThat(form.getValue().getIdEquipamiento()).isEqualTo(5L);
        assertThat(form.getValue().getFechaInicio()).isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 0));
        assertThat(form.getValue().getFechaFin()).isEqualTo(LocalDateTime.of(2026, 10, 20, 12, 0));
        assertThat(form.getValue().getObservaciones()).isEqualTo("Limpieza con soda");
    }

    @Test
    @DisplayName("CP-OPE-02: el operario registra una falla y un mantenimiento y vuelve a la pestaña de cada uno")
    void registrarFallaYMantenimiento() throws Exception {
        mockMvc.perform(registro("fallas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "2026-10-20T12:00", "Falla en la resistencia"))
                .andExpect(redirectedUrl(BASE + "?pestana=fallas")).andExpect(flash().attribute("mensaje", "Falla registrada correctamente"));
        mockMvc.perform(registro("mantenimientos", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "2026-10-20T12:00", "Cambio de juntas"))
                .andExpect(redirectedUrl(BASE + "?pestana=mantenimientos")).andExpect(flash().attribute("mensaje", "Mantenimiento registrado correctamente"));

        verify(fallaServicio).registrarFallaEquipamiento(any(FallaEquipamientoFormDTO.class));
        verify(mantenimientoServicio).registrarMantenimientoEquipamiento(any(MantenimientoEquipamientoFormDTO.class));
    }

    @Test
    @DisplayName("CP-OPE-03: un formulario inválido vuelve a la pestaña por redirect sin llamar al service, y el GET siguiente muestra los errores y lo escrito")
    void registrarInvalido() throws Exception {
        MvcResult resultado = mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "", "", "  "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "?pestana=limpiezas")).andReturn();
        verify(limpiezaServicio, never()).registrarLimpiezaEquipamiento(any());

        String html = mockMvc.perform(get(BASE).param("pestana", "limpiezas").with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(resultado.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("No se pudo registrar una limpieza").contains("La fecha de inicio es obligatoria")
                .contains("La fecha de fin es obligatoria").contains("Las observaciones son obligatorias");
    }

    @Test
    @DisplayName("CP-OPE-04: un rechazo del service sobre un campo se muestra en ese campo, y uno general (estado del equipo) en el resumen")
    void registrarRechazadoPorElService() throws Exception {
        when(limpiezaServicio.registrarLimpiezaEquipamiento(any())).thenThrow(new ReglaNegocioException("La fecha de fin no puede ser anterior a la fecha de inicio", "fechaFin"));
        MvcResult campo = mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T12:00", "2026-10-20T10:00", "Limpieza"))
                .andExpect(redirectedUrl(BASE + "?pestana=limpiezas")).andReturn();
        org.mockito.Mockito.doThrow(new ReglaNegocioException("No se puede registrar una limpieza porque el equipamiento está disponible", "idEquipamiento")).when(limpiezaServicio).registrarLimpiezaEquipamiento(any());
        MvcResult general = mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "2026-10-20T12:00", "Limpieza"))
                .andExpect(redirectedUrl(BASE + "?pestana=limpiezas")).andReturn();

        String htmlCampo = mockMvc.perform(get(BASE).param("pestana", "limpiezas").with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(campo.getFlashMap()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String htmlGeneral = mockMvc.perform(get(BASE).param("pestana", "limpiezas").with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(general.getFlashMap()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(htmlCampo).contains("La fecha de fin no puede ser anterior a la fecha de inicio");
        assertThat(htmlGeneral).contains("No se puede registrar una limpieza porque el equipamiento está disponible");
    }

    @Test
    @DisplayName("CP-OPE-05: el operario anula una operación con motivo: el service recibe el ID y el motivo y vuelve a la pestaña con la alerta de éxito")
    void anular() throws Exception {
        for (String[] caso : new String[][]{{"limpiezas", "Limpieza anulada correctamente"}, {"fallas", "Falla anulada correctamente"}, {"mantenimientos", "Mantenimiento anulado correctamente"}}) {
            mockMvc.perform(post(BASE + "/" + caso[0] + "/7/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).param("motivo", "Error de carga"))
                    .andExpect(redirectedUrl(BASE + "?pestana=" + caso[0])).andExpect(flash().attribute("mensaje", caso[1]));
        }
        ArgumentCaptor<AnulacionLimpiezaEquipamientoFormDTO> form = ArgumentCaptor.forClass(AnulacionLimpiezaEquipamientoFormDTO.class);
        verify(limpiezaServicio).anularLimpiezaEquipamiento(eq(7L), form.capture());
        assertThat(form.getValue().getMotivoAnulacion()).isEqualTo("Error de carga");
    }

    @Test
    @DisplayName("CP-OPE-06: si el service rechaza la anulación vuelve al origen con su mensaje en una alerta warning")
    void anularRechazada() throws Exception {
        when(limpiezaServicio.anularLimpiezaEquipamiento(any(), any()))
                .thenThrow(new ReglaNegocioException("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores"));

        mockMvc.perform(post(BASE + "/limpiezas/7/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", BASE).param("motivo", "x"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "warning"))
                .andExpect(flash().attribute("mensaje", "Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores"));
    }

    @Test
    @DisplayName("CP-OPE-09: anular una operación que no es del equipamiento de la ruta responde como inexistente (alerta danger) y no llama al service de anulación")
    void anularOperacionDeOtroEquipamiento() throws Exception {
        for (String ruta : List.of("limpiezas", "fallas", "mantenimientos")) {
            mockMvc.perform(post(BASE + "/" + ruta + "/99/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", BASE).param("motivo", "Error de carga"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(limpiezaServicio, fallaServicio, mantenimientoServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-OPE-07: los roles sin permisos de registrar y anular (incluido el gerente de producción) son rechazados y no llegan a ningún service")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String ruta : List.of("limpiezas", "fallas", "mantenimientos")) {
            mockMvc.perform(registro(ruta, rol, "2026-10-20T10:00", "2026-10-20T12:00", "x")).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
            mockMvc.perform(post(BASE + "/" + ruta + "/7/anular").with(como(rol)).with(csrf()).param("motivo", "x")).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(limpiezaServicio, fallaServicio, mantenimientoServicio);
    }

    @Test
    @DisplayName("CP-OPE-08: registrar sin token CSRF es rechazado y no llega al service")
    void sinCsrf() throws Exception {
        mockMvc.perform(post(BASE + "/limpiezas").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("fechaInicio", "2026-10-20T10:00"))
                .andExpect(status().is3xxRedirection());
        verifyNoInteractions(limpiezaServicio);
    }
}
