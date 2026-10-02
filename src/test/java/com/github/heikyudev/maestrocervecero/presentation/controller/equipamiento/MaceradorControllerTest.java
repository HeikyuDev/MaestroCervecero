package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
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

@WebMvcTest(MaceradorController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class MaceradorControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IMaceradorServicio maceradorServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-01: el GERENTE_DE_PRODUCCION ve el listado con estado operativo, datos técnicos, las pestañas y las acciones por fila")
    void caso_cp_mcc_01() throws Exception {
        when(maceradorServicio.filtrarMaceradores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(equipo(4L, "MAC-01", EstadoOperativo.EN_USO)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/equipamiento/maceradores"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/macerador-lista"))
                .andExpect(content().string(containsString("Gestión de Equipamiento")))
                .andExpect(content().string(containsString("MAC-01")))
                .andExpect(content().string(containsString("EN USO")))
                .andExpect(content().string(containsString("100,00")))
                .andExpect(content().string(containsString("80,00")))
                .andExpect(content().string(containsString("2,00")))
                .andExpect(content().string(containsString("75,00 %")))
                .andExpect(content().string(containsString("50")))
                .andExpect(content().string(containsString("href=\"/equipamiento\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/ollas-hervor\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/fermentadores\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/maceradores/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/maceradores/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/equipamiento/maceradores/4/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null")
    void caso_cp_mcc_02() throws Exception {
        when(maceradorServicio.filtrarMaceradores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento/maceradores")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(maceradorServicio).filtrarMaceradores(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("identificadorInterno", "id");
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-03: los criterios llegan al service; un identificador en blanco y el select \"Todos\" se convierten en null")
    void caso_cp_mcc_03() throws Exception {
        when(maceradorServicio.filtrarMaceradores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/equipamiento/maceradores").param("identificador", " mol ").param("estadoOperativo", "EN_USO").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/equipamiento/maceradores").param("identificador", "   ").param("estadoOperativo", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(maceradorServicio).filtrarMaceradores(eq("mol"), eq(EstadoOperativo.EN_USO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(maceradorServicio).filtrarMaceradores(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_mcc_04() throws Exception {
        when(maceradorServicio.filtrarMaceradores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento/maceradores"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron maceradores con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MCC-05: el OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service")
    void caso_cp_mcc_05() throws Exception {
        mockMvc.perform(get("/equipamiento/maceradores"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maceradorServicio, never()).filtrarMaceradores(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-06: el formulario de alta muestra los campos con asterisco, placeholders y la ayuda de cada regla")
    void caso_cp_mcc_06() throws Exception {
        mockMvc.perform(get("/equipamiento/maceradores/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/macerador-form"))
                .andExpect(content().string(containsString("Nuevo macerador")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/equipamiento/maceradores\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: MAC-01\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 100\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 80\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 2\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 75\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 50\"")))
                .andExpect(content().string(containsString("Crear macerador")))
                .andExpect(content().string(containsString("Volver a maceradores")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-07: abierto desde otro formulario (retorno y campo) los conserva y cancelar vuelve a ese formulario; un retorno externo se descarta")
    void caso_cp_mcc_07() throws Exception {
        mockMvc.perform(get("/equipamiento/maceradores/nuevo").param("retorno", "/lotes/nuevo").param("campo", "idEquipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/lotes/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idEquipamiento\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/lotes/nuevo?desdeAlta=1\"")));

        mockMvc.perform(get("/equipamiento/maceradores/nuevo").param("retorno", "//sitio-malicioso.com").param("campo", "idEquipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MCC-08: el OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta")
    void caso_cp_mcc_08() throws Exception {
        mockMvc.perform(get("/equipamiento/maceradores/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_mcc_09() throws Exception {
        when(maceradorServicio.altaMacerador(any())).thenReturn(equipo(9L, "MAC-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(post("/equipamiento/maceradores").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/maceradores"))
                .andExpect(flash().attribute("mensaje", "Macerador creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<MaceradorFormDTO> captor = ArgumentCaptor.forClass(MaceradorFormDTO.class);
        verify(maceradorServicio).altaMacerador(captor.capture());
        assertThat(captor.getValue().getIdentificadorInterno()).isEqualTo("MAC-01");
        assertThat(captor.getValue().getCapacidadTotal()).isEqualTo(100.0);
        assertThat(captor.getValue().getCapacidadUtil()).isEqualTo(80.0);
        assertThat(captor.getValue().getEspacioMuerto()).isEqualTo(2.0);
        assertThat(captor.getValue().getEficienciaMaceracion()).isEqualTo(75.0);
        assertThat(captor.getValue().getUsosMaximosAntesMantenimiento()).isEqualTo(50);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-10: sin identificador ni datos técnicos vuelve al formulario con todos los errores, sin llamar al service")
    void caso_cp_mcc_10() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores").with(csrf()).param("identificadorInterno", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/macerador-form"))
                .andExpect(content().string(containsString("No se pudo crear el macerador")))
                .andExpect(content().string(containsString("El identificador interno es obligatorio")))
                .andExpect(content().string(containsString("La capacidad total es obligatoria")))
                .andExpect(content().string(containsString("La capacidad útil es obligatoria")))
                .andExpect(content().string(containsString("El espacio muerto es obligatorio")))
                .andExpect(content().string(containsString("La eficiencia de maceración es obligatoria")))
                .andExpect(content().string(containsString("Los usos máximos antes de mantenimiento son obligatorios")));

        verify(maceradorServicio, never()).altaMacerador(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-11: un valor no numérico en un campo numérico muestra un mensaje en español, sin llamar al service")
    void caso_cp_mcc_11() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresá un número válido")));

        verify(maceradorServicio, never()).altaMacerador(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-12: un identificador duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void caso_cp_mcc_12() throws Exception {
        when(maceradorServicio.altaMacerador(any())).thenThrow(new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'MAC-01'"));

        mockMvc.perform(post("/equipamiento/maceradores").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/macerador-form"))
                .andExpect(content().string(containsString("Ya existe un equipamiento con el identificador interno")))
                .andExpect(content().string(containsString("value=\"MAC-01\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-13: una regla de negocio rechazada por el service se muestra como error general del formulario")
    void caso_cp_mcc_13() throws Exception {
        when(maceradorServicio.altaMacerador(any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        mockMvc.perform(post("/equipamiento/maceradores").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear el macerador")))
                .andExpect(content().string(containsString("Regla incumplida")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-14: el alta contextual vuelve al formulario de origen con el equipamiento creado en el campo indicado")
    void caso_cp_mcc_14() throws Exception {
        when(maceradorServicio.altaMacerador(any())).thenReturn(equipo(9L, "MAC-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(post("/equipamiento/maceradores").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50")
                        .param("retorno", "/lotes/nuevo").param("campo", "idEquipamiento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/nuevo?desdeAlta=1&idEquipamiento=9"))
                .andExpect(flash().attribute("mensaje", "Macerador creado correctamente"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MCC-15: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta")
    void caso_cp_mcc_15() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maceradorServicio, never()).altaMacerador(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-16: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void caso_cp_mcc_16() throws Exception {
        when(maceradorServicio.buscarPorId(4L)).thenReturn(equipo(4L, "MAC-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(get("/equipamiento/maceradores/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/macerador-form"))
                .andExpect(content().string(containsString("Editar macerador")))
                .andExpect(content().string(containsString("value=\"MAC-01\"")))
                .andExpect(content().string(containsString("value=\"Descripcion de prueba\"")))
                .andExpect(content().string(containsString("value=\"100.0\"")))
                .andExpect(content().string(containsString("value=\"80.0\"")))
                .andExpect(content().string(containsString("value=\"2.0\"")))
                .andExpect(content().string(containsString("value=\"75.0\"")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(containsString("action=\"/equipamiento/maceradores/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-17: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_mcc_17() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores/4").with(csrf())
                        .param("identificadorInterno", "OTRO-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/maceradores"))
                .andExpect(flash().attribute("mensaje", "Macerador modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(maceradorServicio).modificarMacerador(eq(4L), any(MaceradorFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-18: modificar con un identificador duplicado vuelve al formulario de edición con el mensaje del service")
    void caso_cp_mcc_18() throws Exception {
        when(maceradorServicio.modificarMacerador(eq(4L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'MAC-01'"));

        mockMvc.perform(post("/equipamiento/maceradores/4").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el macerador")))
                .andExpect(content().string(containsString("Ya existe un equipamiento con el identificador interno")))
                .andExpect(content().string(containsString("action=\"/equipamiento/maceradores/4\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-19: si el service rechaza la modificación porque el macerador está en uso, el motivo se muestra en el formulario")
    void caso_cp_mcc_19() throws Exception {
        when(maceradorServicio.modificarMacerador(eq(4L), any())).thenThrow(new ReglaNegocioException("No se puede modificar el macerador porque se encuentra en uso."));

        mockMvc.perform(post("/equipamiento/maceradores/4").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el macerador")))
                .andExpect(content().string(containsString("No se puede modificar el macerador porque se encuentra en uso.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MCC-20: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar")
    void caso_cp_mcc_20() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores/4").with(csrf())
                        .param("identificadorInterno", "MAC-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("espacioMuerto", "2")
                        .param("eficienciaMaceracion", "75")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maceradorServicio, never()).modificarMacerador(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-21: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_mcc_21() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/maceradores"))
                .andExpect(flash().attribute("mensaje", "Macerador dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(maceradorServicio).bajaMacerador(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-MCC-22: si el service rechaza la baja (etapa pendiente o en uso) vuelve al origen con la alerta de advertencia")
    void caso_cp_mcc_22() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja el macerador porque tiene comprometida una etapa pendiente de un lote.")).when(maceradorServicio).bajaMacerador(4L);

        mockMvc.perform(post("/equipamiento/maceradores/4/baja").with(csrf()).header("Referer", "/equipamiento/maceradores"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/maceradores"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el macerador porque tiene comprometida una etapa pendiente de un lote."))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-MCC-23: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja")
    void caso_cp_mcc_23() throws Exception {
        mockMvc.perform(post("/equipamiento/maceradores/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(maceradorServicio, never()).bajaMacerador(any());
    }

    private static MaceradorResponseDTO equipo(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return MaceradorResponseDTO.builder().id(id).identificadorInterno(identificadorInterno).descripcion("Descripcion de prueba")
                .estadoOperativo(estadoOperativo).capacidadTotal(100.0).capacidadUtil(80.0).espacioMuerto(2.0).eficienciaMaceracion(75.0).usosMaximosAntesMantenimiento(50).build();
    }
}
