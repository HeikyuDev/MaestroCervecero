package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento.ControlEquipamientoControllerTest.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(EquipamientoDetalleController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class EquipamientoDetalleControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private IControlEquipamientoServicio servicio;
    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @BeforeEach
    void escenarioBase() {
        when(servicio.buscarResumen(5L)).thenReturn(EquipamientoResumenResponseDTO.builder().id(5L).identificadorInterno("OH-01")
                .tipo(TipoEquipamiento.OLLA_HERVOR).estadoOperativo(EstadoOperativo.DISPONIBLE).usosMaximosAntesMantenimiento(10)
                .usosDesdeUltimoMantenimiento(3L).capacidadTotal(100.0).evaporacion(5.5).build());
        when(servicio.buscarResumen(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el equipamiento con ID: 99"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION"})
    @DisplayName("CP-EQD-01: el gerente y el operario ven el detalle con identidad, solo los parámetros con valor y las fechas de mantenimiento")
    void detalle(Rol rol) throws Exception {
        String html = mockMvc.perform(get("/equipamiento/5/detalle").with(como(rol))).andExpect(status().isOk())
                .andExpect(view().name("equipamiento/equipamiento-detalle")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("OH-01").contains("Olla de hervor").contains("Capacidad total").contains("Evaporación").contains("5,5 L/h")
                .contains("3 de 10").contains("Última limpieza").contains("Último mantenimiento")
                .doesNotContain("Capacidad útil").doesNotContain("Espacio muerto").doesNotContain("Rendimiento de molienda");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DISTRIBUCION", "ADMINISTRADOR"})
    @DisplayName("CP-EQD-02: los demás roles no abren el detalle del equipamiento")
    void rechazados(Rol rol) throws Exception {
        mockMvc.perform(get("/equipamiento/5/detalle").with(como(rol))).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        verifyNoInteractions(servicio);
    }

    private static final String ENLACE_RESALTADO = "bg-amber-500/10 text-amber-500 border-amber-500";

    @Test
    @DisplayName("CP-EQD-04: el gerente de producción ve el detalle dentro de la Gestión de Equipamientos: resalta su opción del menú y Volver lleva a la lista del tipo del equipo")
    void detalleDentroDeLaGestion() throws Exception {
        String html = mockMvc.perform(get("/equipamiento/5/detalle").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("Volver a ollas de hervor").contains("href=\"/equipamiento/ollas-hervor\"");
        assertThat(html.substring(html.indexOf("Gestión de Equipamientos") - 600, html.indexOf("Gestión de Equipamientos"))).contains(ENLACE_RESALTADO);
        assertThat(html.substring(html.indexOf("Control de Equipamientos") - 600, html.indexOf("Control de Equipamientos"))).doesNotContain(ENLACE_RESALTADO);
        assertThat(html).doesNotContain("Volver a control de equipamiento");
    }

    @Test
    @DisplayName("CP-EQD-05: el operario de producción, que ahora consulta la Gestión de Equipamientos, ve el detalle igual que el gerente: opción resaltada y Volver a la lista del tipo")
    void detalleDelOperario() throws Exception {
        String html = mockMvc.perform(get("/equipamiento/5/detalle").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("Volver a ollas de hervor").contains("href=\"/equipamiento/ollas-hervor\"").doesNotContain("Volver a control de equipamiento");
        assertThat(html.substring(html.indexOf("Gestión de Equipamientos") - 600, html.indexOf("Gestión de Equipamientos"))).contains(ENLACE_RESALTADO);
    }

    @Test
    @DisplayName("CP-EQD-03: un equipamiento inexistente o dado de baja vuelve al origen con alerta de error")
    void inexistente() throws Exception {
        mockMvc.perform(get("/equipamiento/99/detalle").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", "/lotes"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }
}
