package com.github.heikyudev.maestrocervecero.presentation.controller.barril;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FabricanteBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
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

@WebMvcTest(FabricanteBarrilController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class FabricanteBarrilControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";
    private static final String URL = "/barriles/fabricantes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IFabricanteBarrilServicio fabricanteBarrilServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /barriles/fabricantes ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-01: el GERENTE_DE_PRODUCCION ve el listado con las pestañas y las acciones por fila")
    void caso_cp_fbc_01() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any()))
                .thenReturn(new PageImpl<>(List.of(fabricante(2L, "Barrilitos SA")), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/fabricante-lista"))
                .andExpect(content().string(containsString("Gestión de Barriles")))
                .andExpect(content().string(containsString("Barrilitos SA")))
                .andExpect(content().string(containsString("href=\"/barriles\"")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles/fabricantes\"[^>]*border-amber-500[^>]*>\\s*(<[^>]*>\\s*)*Fabricantes de barril\\s*</a>.*")))
                .andExpect(content().string(containsString("href=\"/barriles/fabricantes/nuevo\"")))
                .andExpect(content().string(containsString("Nuevo fabricante")))
                .andExpect(content().string(containsString("href=\"/barriles/fabricantes/2/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/barriles/fabricantes/2/baja\"")))
                .andExpect(content().string(containsString("Si tiene barriles activos asociados, el sistema no lo permitirá.")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")))
                // La sidebar resalta "Gestión de Barriles"
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-boxes\"></i>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con el criterio en null")
    void caso_cp_fbc_02() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL)).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fabricanteBarrilServicio).filtrarFabricantesBarril(isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").getDirection().isAscending()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-03: el criterio de nombre llega recortado al service; un texto en blanco equivale a no filtrar")
    void caso_cp_fbc_03() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL).param("nombre", " ton ").param("pagina", "1")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("nombre", "   ")).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fabricanteBarrilServicio).filtrarFabricantesBarril(eq("ton"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        verify(fabricanteBarrilServicio).filtrarFabricantesBarril(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-04: el criterio queda en el formulario y en los links de paginación")
    void caso_cp_fbc_04() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any()))
                .thenReturn(new PageImpl<>(List.of(fabricante(2L, "Tonelería del Sur")), PageRequest.of(1, 20), 60));

        // Act + Assert
        mockMvc.perform(get(URL).param("nombre", "ton").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"ton\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Tonelería del Sur\"")))
                .andExpect(content().string(containsString(URL + "?pagina=0&amp;nombre=ton")))
                .andExpect(content().string(containsString(URL + "?pagina=2&amp;nombre=ton")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles/fabricantes\"[^>]*>\\s*Limpiar.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-05: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_fbc_05() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron fabricantes de barril con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FBC-06: el OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service")
    void caso_cp_fbc_06() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fabricanteBarrilServicio, never()).filtrarFabricantesBarril(any(), any());
    }

    // ==================== GET /barriles/fabricantes/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-07: el formulario de alta muestra el campo con asterisco y placeholder")
    void caso_cp_fbc_07() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/fabricante-form"))
                .andExpect(content().string(containsString("Nuevo fabricante de barril")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/barriles/fabricantes\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("Nombre")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Tonelería del Sur\"")))
                .andExpect(content().string(containsString("Crear fabricante")))
                .andExpect(content().string(containsString("Volver a fabricantes de barril")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles/fabricantes\"[^>]*>\\s*<i[^>]*></i>\\s*<span>Volver a fabricantes de barril.*")))
                .andExpect(content().string(not(containsString("No se pudo crear"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FBC-08: el OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta")
    void caso_cp_fbc_08() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /barriles/fabricantes ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-09: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_fbc_09() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.altaFabricanteBarril(any())).thenReturn(fabricante(9L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Fabricante de barril creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<FabricanteBarrilFormDTO> captor = ArgumentCaptor.forClass(FabricanteBarrilFormDTO.class);
        verify(fabricanteBarrilServicio).altaFabricanteBarril(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Barrilitos SA");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-10: sin nombre vuelve al formulario con el error, sin llamar al service")
    void caso_cp_fbc_10() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/fabricante-form"))
                .andExpect(content().string(containsString("No se pudo crear el fabricante de barril")))
                .andExpect(content().string(containsString("El nombre es obligatorio")));

        verify(fabricanteBarrilServicio, never()).altaFabricanteBarril(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-11: un nombre duplicado vuelve al formulario con el mensaje del service junto al campo nombre")
    void caso_cp_fbc_11() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.altaFabricanteBarril(any())).thenThrow(new RecursoDuplicadoException("Ya existe un fabricante de barril con el nombre 'Barrilitos SA'"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/fabricante-form"))
                .andExpect(content().string(containsString("Ya existe un fabricante de barril con el nombre")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*border-rose-400.*")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-12: una regla de negocio rechazada por el service se muestra como error general del formulario")
    void caso_cp_fbc_12() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.altaFabricanteBarril(any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear el fabricante de barril")))
                .andExpect(content().string(containsString("Regla incumplida")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FBC-13: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta")
    void caso_cp_fbc_13() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fabricanteBarrilServicio, never()).altaFabricanteBarril(any());
    }

    // ==================== GET /barriles/fabricantes/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-14: el formulario de edición muestra el nombre actual y apunta a la modificación")
    void caso_cp_fbc_14() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(get(URL + "/2/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/fabricante-form"))
                .andExpect(content().string(containsString("Editar fabricante de barril")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")))
                .andExpect(content().string(containsString("action=\"/barriles/fabricantes/2\"")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(containsString("Volver a fabricantes de barril")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-15: un fabricante inexistente redirige al origen con el motivo")
    void caso_cp_fbc_15() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/99/editar").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró el fabricante de barril con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FBC-16: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de edición")
    void caso_cp_fbc_16() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/2/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fabricanteBarrilServicio, never()).buscarPorId(any());
    }

    // ==================== POST /barriles/fabricantes/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-17: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_fbc_17() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/2").with(csrf()).param("nombre", "Tonelería del Sur"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Fabricante de barril modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(fabricanteBarrilServicio).modificarFabricanteBarril(eq(2L), any(FabricanteBarrilFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-18: modificar sin nombre vuelve al formulario de edición con el error, sin llamar al service")
    void caso_cp_fbc_18() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/2").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/barriles/fabricantes/2\"")))
                .andExpect(content().string(containsString("No se pudo modificar el fabricante de barril")))
                .andExpect(content().string(containsString("El nombre es obligatorio")));

        verify(fabricanteBarrilServicio, never()).modificarFabricanteBarril(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-19: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void caso_cp_fbc_19() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.modificarFabricanteBarril(eq(2L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un fabricante de barril con el nombre 'Barrilitos SA'"));

        // Act + Assert
        mockMvc.perform(post(URL + "/2").with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el fabricante de barril")))
                .andExpect(content().string(containsString("Ya existe un fabricante de barril con el nombre &#39;Barrilitos SA&#39;")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*border-rose-400.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-20: si el service rechaza la modificación por una regla de negocio, el motivo se muestra en el formulario")
    void caso_cp_fbc_20() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.modificarFabricanteBarril(eq(2L), any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        // Act + Assert
        mockMvc.perform(post(URL + "/2").with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el fabricante de barril")))
                .andExpect(content().string(containsString("Regla incumplida")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FBC-21: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar")
    void caso_cp_fbc_21() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/2").with(csrf()).param("nombre", "Barrilitos SA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fabricanteBarrilServicio, never()).modificarFabricanteBarril(any(), any());
    }

    // ==================== POST /barriles/fabricantes/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-22: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_fbc_22() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/2/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Fabricante de barril dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(fabricanteBarrilServicio).bajaFabricanteBarril(2L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-FBC-23: si el service rechaza la baja (barriles activos asociados) vuelve al origen con la alerta de advertencia")
    void caso_cp_fbc_23() throws Exception {
        // Arrange
        doThrow(new ReglaNegocioException("No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados")).when(fabricanteBarrilServicio).bajaFabricanteBarril(2L);

        // Act + Assert
        mockMvc.perform(post(URL + "/2/baja").with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-FBC-24: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja")
    void caso_cp_fbc_24() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/2/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fabricanteBarrilServicio, never()).bajaFabricanteBarril(any());
    }

    // ==================== Datos de prueba ====================

    private static PageImpl<FabricanteBarrilResponseDTO> paginaVacia() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }

    private static FabricanteBarrilResponseDTO fabricante(Long id, String nombre) {
        return FabricanteBarrilResponseDTO.builder().id(id).nombre(nombre).build();
    }
}
