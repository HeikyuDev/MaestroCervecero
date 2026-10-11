package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
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

@WebMvcTest(FermentadorController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class FermentadorControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IFermentadorServicio fermentadorServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-01: el GERENTE_DE_PRODUCCION ve el listado con estado operativo, datos técnicos, las pestañas y las acciones por fila")
    void caso_cp_fec_01() throws Exception {
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(equipo(4L, "FER-01", EstadoOperativo.EN_USO)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/equipamiento/fermentadores"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/fermentador-lista"))
                .andExpect(content().string(containsString("Gestión de Equipamiento")))
                .andExpect(content().string(containsString("FER-01")))
                .andExpect(content().string(containsString("En uso")))
                .andExpect(content().string(containsString("100,00")))
                .andExpect(content().string(containsString("80,00")))
                .andExpect(content().string(containsString("50")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/maceradores\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/ollas-hervor\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/fermentadores/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/fermentadores/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/equipamiento/fermentadores/4/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null")
    void caso_cp_fec_02() throws Exception {
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento/fermentadores")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadores(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("identificadorInterno", "id");
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-03: los criterios llegan al service; un identificador en blanco y el select \"Todos\" se convierten en null")
    void caso_cp_fec_03() throws Exception {
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/equipamiento/fermentadores").param("identificador", " mol ").param("estadoOperativo", "EN_USO").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/equipamiento/fermentadores").param("identificador", "   ").param("estadoOperativo", ""))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fermentadorServicio).filtrarFermentadores(eq("mol"), eq(EstadoOperativo.EN_USO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(fermentadorServicio).filtrarFermentadores(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_fec_04() throws Exception {
        when(fermentadorServicio.filtrarFermentadores(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento/fermentadores"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron fermentadores con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FEC-05: el OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service")
    void caso_cp_fec_05() throws Exception {
        mockMvc.perform(get("/equipamiento/fermentadores"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fermentadorServicio, never()).filtrarFermentadores(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-06: el formulario de alta muestra los campos con asterisco, placeholders y la ayuda de cada regla")
    void caso_cp_fec_06() throws Exception {
        mockMvc.perform(get("/equipamiento/fermentadores/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/fermentador-form"))
                .andExpect(content().string(containsString("Nuevo fermentador")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/equipamiento/fermentadores\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: FER-01\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 100\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 80\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 50\"")))
                .andExpect(content().string(containsString("Crear fermentador")))
                .andExpect(content().string(containsString("Volver a fermentadores")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("No se pudo crear"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-07: abierto desde otro formulario (retorno y campo) los conserva y cancelar vuelve a ese formulario; un retorno externo se descarta")
    void caso_cp_fec_07() throws Exception {
        mockMvc.perform(get("/equipamiento/fermentadores/nuevo").param("retorno", "/lotes/nuevo").param("campo", "idEquipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/lotes/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idEquipamiento\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/lotes/nuevo?desdeAlta=1\"")));

        mockMvc.perform(get("/equipamiento/fermentadores/nuevo").param("retorno", "//sitio-malicioso.com").param("campo", "idEquipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"retorno\""))))
                .andExpect(content().string(not(containsString("sitio-malicioso"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FEC-08: el OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta")
    void caso_cp_fec_08() throws Exception {
        mockMvc.perform(get("/equipamiento/fermentadores/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_fec_09() throws Exception {
        when(fermentadorServicio.altaFermentador(any())).thenReturn(equipo(9L, "FER-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/fermentadores"))
                .andExpect(flash().attribute("mensaje", "Fermentador creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<FermentadorFormDTO> captor = ArgumentCaptor.forClass(FermentadorFormDTO.class);
        verify(fermentadorServicio).altaFermentador(captor.capture());
        assertThat(captor.getValue().getIdentificadorInterno()).isEqualTo("FER-01");
        assertThat(captor.getValue().getCapacidadTotal()).isEqualTo(100.0);
        assertThat(captor.getValue().getCapacidadUtil()).isEqualTo(80.0);
        assertThat(captor.getValue().getUsosMaximosAntesMantenimiento()).isEqualTo(50);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-10: sin identificador ni datos técnicos vuelve al formulario con todos los errores, sin llamar al service")
    void caso_cp_fec_10() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf()).param("identificadorInterno", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/fermentador-form"))
                .andExpect(content().string(containsString("No se pudo crear el fermentador")))
                .andExpect(content().string(containsString("El identificador interno es obligatorio")))
                .andExpect(content().string(containsString("La capacidad total es obligatoria")))
                .andExpect(content().string(containsString("La capacidad útil es obligatoria")))
                .andExpect(content().string(containsString("Los usos máximos antes de mantenimiento son obligatorios")));

        verify(fermentadorServicio, never()).altaFermentador(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-11: un valor no numérico en un campo numérico muestra un mensaje en español, sin llamar al service")
    void caso_cp_fec_11() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese un número válido")));

        verify(fermentadorServicio, never()).altaFermentador(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-12: un identificador duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado")
    void caso_cp_fec_12() throws Exception {
        when(fermentadorServicio.altaFermentador(any())).thenThrow(new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'FER-01'"));

        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/fermentador-form"))
                .andExpect(content().string(containsString("Ya existe un equipamiento con el identificador interno")))
                .andExpect(content().string(containsString("value=\"FER-01\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-13: una regla de negocio rechazada por el service se muestra como error general del formulario")
    void caso_cp_fec_13() throws Exception {
        when(fermentadorServicio.altaFermentador(any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear el fermentador")))
                .andExpect(content().string(containsString("Regla incumplida")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-14: el alta contextual vuelve al formulario de origen con el equipamiento creado en el campo indicado")
    void caso_cp_fec_14() throws Exception {
        when(fermentadorServicio.altaFermentador(any())).thenReturn(equipo(9L, "FER-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50")
                        .param("retorno", "/lotes/nuevo").param("campo", "idEquipamiento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lotes/nuevo?desdeAlta=1&idEquipamiento=9"))
                .andExpect(flash().attribute("mensaje", "Fermentador creado correctamente"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FEC-15: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta")
    void caso_cp_fec_15() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fermentadorServicio, never()).altaFermentador(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-16: el formulario de edición muestra los datos actuales y apunta a la modificación")
    void caso_cp_fec_16() throws Exception {
        when(fermentadorServicio.buscarPorId(4L)).thenReturn(equipo(4L, "FER-01", EstadoOperativo.DISPONIBLE));

        mockMvc.perform(get("/equipamiento/fermentadores/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/fermentador-form"))
                .andExpect(content().string(containsString("Editar fermentador")))
                .andExpect(content().string(containsString("value=\"FER-01\"")))
                .andExpect(content().string(containsString("value=\"100.0\"")))
                .andExpect(content().string(containsString("value=\"80.0\"")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(containsString("action=\"/equipamiento/fermentadores/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-17: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_fec_17() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores/4").with(csrf())
                        .param("identificadorInterno", "OTRO-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/fermentadores"))
                .andExpect(flash().attribute("mensaje", "Fermentador modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(fermentadorServicio).modificarFermentador(eq(4L), any(FermentadorFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-18: modificar con un identificador duplicado vuelve al formulario de edición con el mensaje del service")
    void caso_cp_fec_18() throws Exception {
        when(fermentadorServicio.modificarFermentador(eq(4L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'FER-01'"));

        mockMvc.perform(post("/equipamiento/fermentadores/4").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el fermentador")))
                .andExpect(content().string(containsString("Ya existe un equipamiento con el identificador interno")))
                .andExpect(content().string(containsString("action=\"/equipamiento/fermentadores/4\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-19: si el service rechaza la modificación porque el fermentador está en uso, el motivo se muestra en el formulario")
    void caso_cp_fec_19() throws Exception {
        when(fermentadorServicio.modificarFermentador(eq(4L), any())).thenThrow(new ReglaNegocioException("No se puede modificar el fermentador porque se encuentra en uso"));

        mockMvc.perform(post("/equipamiento/fermentadores/4").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el fermentador")))
                .andExpect(content().string(containsString("No se puede modificar el fermentador porque se encuentra en uso")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FEC-20: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar")
    void caso_cp_fec_20() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores/4").with(csrf())
                        .param("identificadorInterno", "FER-01")
                        .param("capacidadTotal", "100")
                        .param("capacidadUtil", "80")
                        .param("usosMaximosAntesMantenimiento", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fermentadorServicio, never()).modificarFermentador(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-21: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_fec_21() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/fermentadores"))
                .andExpect(flash().attribute("mensaje", "Fermentador dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(fermentadorServicio).bajaFermentador(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FEC-22: si el service rechaza la baja (etapa pendiente o en uso) vuelve al origen con la alerta de advertencia")
    void caso_cp_fec_22() throws Exception {
        doThrow(new ReglaNegocioException("No se puede dar de baja el fermentador porque tiene comprometida una etapa en estado Pendiente de un lote")).when(fermentadorServicio).bajaFermentador(4L);

        mockMvc.perform(post("/equipamiento/fermentadores/4/baja").with(csrf()).header("Referer", "/equipamiento/fermentadores"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipamiento/fermentadores"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el fermentador porque tiene comprometida una etapa en estado Pendiente de un lote"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FEC-23: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja")
    void caso_cp_fec_23() throws Exception {
        mockMvc.perform(post("/equipamiento/fermentadores/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fermentadorServicio, never()).bajaFermentador(any());
    }

    private static FermentadorResponseDTO equipo(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return FermentadorResponseDTO.builder().id(id).identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo).capacidadTotal(100.0).capacidadUtil(80.0).usosMaximosAntesMantenimiento(50).build();
    }
}
