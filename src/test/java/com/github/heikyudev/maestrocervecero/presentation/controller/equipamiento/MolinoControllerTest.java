package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.util.List;

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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MolinoController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class MolinoControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IMolinoServicio molinoServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-01: el GERENTE_DE_PRODUCCION ve el listado con estado operativo, datos técnicos, las pestañas y las acciones por fila")
    void caso_cp_moc_01() throws Exception {
        when(molinoServicio.filtrarMolinos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(equipo(4L, "MOL-01", EstadoOperativo.EN_USO)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/equipamiento/molinos"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/molino-lista"))
                .andExpect(content().string(containsString("Gestión de Equipamiento")))
                .andExpect(content().string(containsString("MOL-01")))
                .andExpect(content().string(containsString("EN USO")))
                .andExpect(content().string(containsString("150,00")))
                .andExpect(content().string(containsString("50")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/maceradores\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/ollas-hervor\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/fermentadores\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/equipamiento/molinos/4/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null")
    void caso_cp_moc_02() throws Exception {
        when(molinoServicio.filtrarMolinos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento/molinos")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(molinoServicio).filtrarMolinos(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("identificadorInterno", "id");
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-03: los criterios llegan al service; un identificador en blanco y el select \"Todos\" se convierten en null")
    void caso_cp_moc_03() throws Exception {
        when(molinoServicio.filtrarMolinos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/equipamiento/molinos").param("identificador", " mol ").param("estadoOperativo", "EN_USO").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/equipamiento/molinos").param("identificador", "   ").param("estadoOperativo", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(molinoServicio).filtrarMolinos(eq("mol"), eq(EstadoOperativo.EN_USO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(molinoServicio).filtrarMolinos(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_moc_04() throws Exception {
        when(molinoServicio.filtrarMolinos(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento/molinos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron molinos con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MOC-05: el OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service")
    void caso_cp_moc_05() throws Exception {
        mockMvc.perform(get("/equipamiento/molinos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(molinoServicio, never()).filtrarMolinos(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-06: el formulario de alta muestra los campos con asterisco, placeholders y la ayuda de cada regla")
    void caso_cp_moc_06() throws Exception {
        mockMvc.perform(get("/equipamiento/molinos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/molino-form"))
                .andExpect(content().string(containsString("Nuevo molino")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/equipamiento/molinos\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: MOL-01\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 150\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 50\"")))
                .andExpect(content().string(containsString("Crear molino")))
                .andExpect(content().string(containsString("Volver a molinos")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-07: abierto desde otro formulario (retorno y campo) los conserva y cancelar vuelve a ese formulario; un retorno externo se descarta")
    void caso_cp_moc_07() throws Exception {
        mockMvc.perform(get("/equipamiento/molinos/nuevo").param("retorno", "/lotes/nuevo").param("campo", "idEquipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/lotes/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idEquipamiento\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/lotes/nuevo?desdeAlta=1\"")));

        mockMvc.perform(get("/equipamiento/molinos/nuevo").param("retorno", "//sitio-malicioso.com").param("campo", "idEquipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MOC-08: el OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta")
    void caso_cp_moc_08() throws Exception {
        mockMvc.perform(get("/equipamiento/molinos/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_moc_09() throws Exception {
        when(molinoServicio.altaMolino(any())).thenReturn(equipo(9L, "MOL-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(post("/equipamiento/molinos").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/molinos"))
                .andExpect(flash().attribute("mensaje", "Molino creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<MolinoFormDTO> captor = ArgumentCaptor.forClass(MolinoFormDTO.class);
        verify(molinoServicio).altaMolino(captor.capture());
        assertThat(captor.getValue().getIdentificadorInterno()).isEqualTo("MOL-01");
        assertThat(captor.getValue().getRendimientoMolienda()).isEqualTo(150.0);
        assertThat(captor.getValue().getUsosMaximosAntesMantenimiento()).isEqualTo(50);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-10: sin identificador ni datos técnicos vuelve al formulario con todos los errores, sin llamar al service")
    void caso_cp_moc_10() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos").with(csrf()).param("identificadorInterno", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/molino-form"))
                .andExpect(content().string(containsString("No se pudo crear el molino")))
                .andExpect(content().string(containsString("El identificador interno es obligatorio")))
                .andExpect(content().string(containsString("El rendimiento de molienda es obligatorio")))
                .andExpect(content().string(containsString("Los usos máximos antes de mantenimiento son obligatorios")));

        verify(molinoServicio, never()).altaMolino(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-11: un valor no numérico en un campo numérico muestra un mensaje en español, sin llamar al service")
    void caso_cp_moc_11() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresá un número válido")));

        verify(molinoServicio, never()).altaMolino(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-12: un identificador duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void caso_cp_moc_12() throws Exception {
        when(molinoServicio.altaMolino(any())).thenThrow(new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'MOL-01'"));

        mockMvc.perform(post("/equipamiento/molinos").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/molino-form"))
                .andExpect(content().string(containsString("Ya existe un equipamiento con el identificador interno")))
                .andExpect(content().string(containsString("value=\"MOL-01\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-13: una regla de negocio rechazada por el service se muestra como error general del formulario")
    void caso_cp_moc_13() throws Exception {
        when(molinoServicio.altaMolino(any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        mockMvc.perform(post("/equipamiento/molinos").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear el molino")))
                .andExpect(content().string(containsString("Regla incumplida")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-14: el alta contextual vuelve al formulario de origen con el equipamiento creado en el campo indicado")
    void caso_cp_moc_14() throws Exception {
        when(molinoServicio.altaMolino(any())).thenReturn(equipo(9L, "MOL-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(post("/equipamiento/molinos").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50")
                        .param("retorno", "/lotes/nuevo").param("campo", "idEquipamiento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/nuevo?desdeAlta=1&idEquipamiento=9"))
                .andExpect(flash().attribute("mensaje", "Molino creado correctamente"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MOC-15: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta")
    void caso_cp_moc_15() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(molinoServicio, never()).altaMolino(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-16: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void caso_cp_moc_16() throws Exception {
        when(molinoServicio.buscarPorId(4L)).thenReturn(equipo(4L, "MOL-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(get("/equipamiento/molinos/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/molino-form"))
                .andExpect(content().string(containsString("Editar molino")))
                .andExpect(content().string(containsString("value=\"MOL-01\"")))
                .andExpect(content().string(containsString("value=\"150.0\"")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(containsString("action=\"/equipamiento/molinos/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-17: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_moc_17() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos/4").with(csrf())
                        .param("identificadorInterno", "OTRO-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/molinos"))
                .andExpect(flash().attribute("mensaje", "Molino modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(molinoServicio).modificarMolino(eq(4L), any(MolinoFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-18: modificar con un identificador duplicado vuelve al formulario de edición con el mensaje del service")
    void caso_cp_moc_18() throws Exception {
        when(molinoServicio.modificarMolino(eq(4L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'MOL-01'"));

        mockMvc.perform(post("/equipamiento/molinos/4").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el molino")))
                .andExpect(content().string(containsString("Ya existe un equipamiento con el identificador interno")))
                .andExpect(content().string(containsString("action=\"/equipamiento/molinos/4\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-19: si el service rechaza la modificación porque el molino está en uso, el motivo se muestra en el formulario")
    void caso_cp_moc_19() throws Exception {
        when(molinoServicio.modificarMolino(eq(4L), any())).thenThrow(new ReglaNegocioException("No se puede modificar el molino porque se encuentra en uso."));

        mockMvc.perform(post("/equipamiento/molinos/4").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el molino")))
                .andExpect(content().string(containsString("No se puede modificar el molino porque se encuentra en uso.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MOC-20: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar")
    void caso_cp_moc_20() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos/4").with(csrf())
                        .param("identificadorInterno", "MOL-01")
                        .param("rendimientoMolienda", "150")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(molinoServicio, never()).modificarMolino(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-21: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_moc_21() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/molinos"))
                .andExpect(flash().attribute("mensaje", "Molino dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(molinoServicio).bajaMolino(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MOC-22: si el service rechaza la baja (etapa pendiente o en uso) vuelve al origen con la alerta de advertencia")
    void caso_cp_moc_22() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja el molino porque tiene comprometida una etapa pendiente de un lote.")).when(molinoServicio).bajaMolino(4L);

        mockMvc.perform(post("/equipamiento/molinos/4/baja").with(csrf()).header("Referer", "/equipamiento/molinos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/molinos"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el molino porque tiene comprometida una etapa pendiente de un lote."))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MOC-23: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja")
    void caso_cp_moc_23() throws Exception {
        mockMvc.perform(post("/equipamiento/molinos/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(molinoServicio, never()).bajaMolino(any());
    }

    private static MolinoResponseDTO equipo(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return MolinoResponseDTO.builder().id(id).identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo).rendimientoMolienda(150.0).usosMaximosAntesMantenimiento(50).build();
    }
}
