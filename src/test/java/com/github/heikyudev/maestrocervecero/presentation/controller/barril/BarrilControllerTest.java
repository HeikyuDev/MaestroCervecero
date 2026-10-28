package com.github.heikyudev.maestrocervecero.presentation.controller.barril;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.login.HomeController;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

@WebMvcTest({BarrilController.class, HomeController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class BarrilControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";
    private static final String BUSCADOR_FABRICANTES = "/barriles/buscador/fabricantes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IBarrilServicio barrilServicio;

    @MockitoBean
    private IFabricanteBarrilServicio fabricanteBarrilServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @BeforeEach
    void fabricanteBarrilesDelFormulario() {
        when(barrilServicio.buscarPorId(4L)).thenReturn(barril(4L, "BRL-01", 2L, "Barrilitos SA"));
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));
    }

    // ==================== GET /barriles ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-01: el GERENTE_DE_PRODUCCION ve el listado con fabricante, capacidad, contenido, estado operativo, las pestañas y las acciones por fila")
    void caso_cp_bct_01() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(barril(4L, "BRL-01", 2L, "Barrilitos SA")), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get("/barriles"))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/barril-lista"))
                .andExpect(content().string(containsString("Gestión de Barriles")))
                .andExpect(content().string(containsString("BRL-01")))
                .andExpect(content().string(containsString("Barrilitos SA")))
                .andExpect(content().string(containsString(">50<")))
                .andExpect(content().string(containsString(">0<")))
                .andExpect(content().string(containsString(">Disponible<")))
                .andExpect(content().string(containsString(">100<")))
                .andExpect(content().string(containsString("Fabricantes de barril")))
                .andExpect(content().string(containsString("href=\"/barriles/fabricantes\"")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles\"[^>]*border-amber-500[^>]*>\\s*(<[^>]*>\\s*)*Barriles\\s*</a>.*")))
                .andExpect(content().string(containsString("href=\"/barriles/nuevo\"")))
                .andExpect(content().string(containsString("Nuevo barril")))
                .andExpect(content().string(containsString("href=\"/barriles/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/barriles/4/baja\"")))
                .andExpect(content().string(containsString("Si no está Disponible, el sistema no lo permitirá.")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador interno, con los criterios en null")
    void caso_cp_bct_02() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get("/barriles")).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("identificadorInterno", "id");
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno").getDirection().isAscending()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-03: los filtros por identificador interno y por número de serie llegan recortados al service; un texto en blanco equivale a no filtrar")
    void caso_cp_bct_03() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get("/barriles").param("identificadorInterno", " b-0 ").param("numeroSerie", " sn-1 ")).andExpect(status().isOk());
        mockMvc.perform(get("/barriles").param("identificadorInterno", "   ").param("numeroSerie", "   ")).andExpect(status().isOk());

        // Assert
        verify(barrilServicio).filtrarBarriles(eq("b-0"), eq("sn-1"), isNull(), isNull(), isNull(), any());
        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-04: el filtro por capacidad llega al service como valor exacto")
    void caso_cp_bct_04() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get("/barriles").param("capacidad", "50")).andExpect(status().isOk());

        // Assert
        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), eq(50.0), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-05: el filtro por estado operativo ofrece las etiquetas del enum; la opción \"Todos\" no filtra")
    void caso_cp_bct_05() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get("/barriles").param("estadoOperativo", "CON_CERVEZA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Todos<")))
                .andExpect(content().string(containsString(">Con cerveza<")))
                .andExpect(content().string(containsString(">Despachado<")))
                .andExpect(content().string(containsString(">Disponible<")))
                .andExpect(content().string(containsString(">En limpieza<")))
                .andExpect(content().string(containsString(">En mantenimiento<")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"CON_CERVEZA\"[^>]*selected.*")));
        mockMvc.perform(get("/barriles").param("estadoOperativo", "")).andExpect(status().isOk());

        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), eq(EstadoOperativoBarril.CON_CERVEZA), isNull(), any());
        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-06: el filtro por fabricante se elige con el selector y se muestra su nombre")
    void caso_cp_bct_06() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(get("/barriles").param("idFabricante", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*name=\"idFabricante\"[^>]*value=\"2\".*")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"" + BUSCADOR_FABRICANTES + "\"")))
                .andExpect(content().string(containsString("data-selector-limpiar")))
                .andExpect(content().string(not(containsString("data-selector-menu"))));

        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), isNull(), eq(2L), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-07: fabricante del filtro inexistente")
    void caso_cp_bct_07() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        when(fabricanteBarrilServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: 99"));

        // Act + Assert
        mockMvc.perform(get("/barriles").param("idFabricante", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(matchesPattern("(?s).*name=\"idFabricante\"[^>]*value=\"99\".*"))))
                .andExpect(content().string(containsString("data-selector-texto value=\"\"")));

        verify(barrilServicio).filtrarBarriles(isNull(), isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-08: los cinco criterios combinados llegan al service junto con la página pedida")
    void caso_cp_bct_08() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act
        mockMvc.perform(get("/barriles").param("identificadorInterno", "b-0").param("numeroSerie", "sn-1").param("capacidad", "50")
                        .param("estadoOperativo", "DISPONIBLE").param("idFabricante", "2").param("pagina", "1"))
                .andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(barrilServicio).filtrarBarriles(eq("b-0"), eq("sn-1"), eq(50.0), eq(EstadoOperativoBarril.DISPONIBLE), eq(2L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-09: los criterios quedan en el formulario y en los links de paginación")
    void caso_cp_bct_09() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(barril(4L, "BRL-01", 2L, "Barrilitos SA")), PageRequest.of(1, 20), 60));
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(get("/barriles").param("identificadorInterno", "b-0").param("numeroSerie", "sn-1").param("capacidad", "50")
                        .param("estadoOperativo", "DISPONIBLE").param("idFabricante", "2").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"b-0\"")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"DISPONIBLE\"[^>]*selected.*")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"idFabricante\"[^>]*value=\"2\".*")))
                .andExpect(content().string(containsString("/barriles?pagina=0&amp;identificadorInterno=b-0&amp;numeroSerie=sn-1&amp;capacidad=50&amp;estadoOperativo=DISPONIBLE&amp;idFabricante=2")))
                .andExpect(content().string(containsString("/barriles?pagina=2&amp;identificadorInterno=b-0&amp;numeroSerie=sn-1&amp;capacidad=50&amp;estadoOperativo=DISPONIBLE&amp;idFabricante=2")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles\"[^>]*>\\s*Limpiar.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-10: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_bct_10() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get("/barriles"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron barriles con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-11: un valor del filtro que no se puede convertir al tipo del parámetro no es un error del sistema")
    void caso_cp_bct_11() throws Exception {
        // Act + Assert
        for (String[] parametro : new String[][]{{"capacidad", "abc"}, {"estadoOperativo", "XYZ"}, {"idFabricante", "abc"}}) {
            mockMvc.perform(get("/barriles?" + parametro[0] + "=" + parametro[1]).header("Referer", "/barriles"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/barriles"))
                    .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                    .andExpect(flash().attribute("tipo", "warning"));
        }

        verify(barrilServicio, never()).filtrarBarriles(any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-BCT-12: el OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service")
    void caso_cp_bct_12() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/barriles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(barrilServicio, never()).filtrarBarriles(any(), any(), any(), any(), any(), any());
    }

    // ==================== GET /barriles/buscador/fabricantes ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-13: el buscador filtra por nombre y ofrece \"Elegir\" en cada fabricante")
    void caso_cp_bct_13() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any()))
                .thenReturn(new PageImpl<>(List.of(fabricante(2L, "Tonelería del Sur")), PageRequest.of(0, 8), 1));

        // Act + Assert
        mockMvc.perform(get(BUSCADOR_FABRICANTES).param("nombre", " ton "))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/buscador-fabricante :: buscador"))
                .andExpect(content().string(containsString("data-modal-buscar")))
                .andExpect(content().string(containsString("name=\"nombre\"")))
                .andExpect(content().string(containsString("value=\" ton \"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Tonelería del Sur\"")))
                .andExpect(content().string(matchesPattern("(?s).*data-elegir[^>]*data-id=\"2\"[^>]*data-texto=\"Tonelería del Sur\".*")))
                .andExpect(content().string(not(containsString("/editar"))))
                .andExpect(content().string(not(containsString("data-baja-url"))))
                .andExpect(content().string(not(containsString("<html"))));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fabricanteBarrilServicio).filtrarFabricantesBarril(eq("ton"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").getDirection().isAscending()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-14: sin criterio o con el nombre en blanco no filtra")
    void caso_cp_bct_14() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any())).thenReturn(paginaVaciaFabricantes());

        // Act
        mockMvc.perform(get(BUSCADOR_FABRICANTES)).andExpect(status().isOk());
        mockMvc.perform(get(BUSCADOR_FABRICANTES).param("nombre", "   ")).andExpect(status().isOk());

        // Assert
        verify(fabricanteBarrilServicio, times(2)).filtrarFabricantesBarril(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-15: sin resultados muestra un mensaje claro dentro del modal")
    void caso_cp_bct_15() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any())).thenReturn(paginaVaciaFabricantes());

        // Act + Assert
        mockMvc.perform(get(BUSCADOR_FABRICANTES))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron fabricantes de barril con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-16: la paginación del buscador conserva el nombre buscado")
    void caso_cp_bct_16() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any()))
                .thenReturn(new PageImpl<>(List.of(fabricante(2L, "Tonelería del Sur")), PageRequest.of(1, 8), 30));

        // Act + Assert
        mockMvc.perform(get(BUSCADOR_FABRICANTES).param("nombre", "ton").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(BUSCADOR_FABRICANTES + "?pagina=0&amp;nombre=ton")))
                .andExpect(content().string(containsString(BUSCADOR_FABRICANTES + "?pagina=2&amp;nombre=ton")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(fabricanteBarrilServicio).filtrarFabricantesBarril(eq("ton"), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-BCT-17: un rol distinto de GERENTE_DE_PRODUCCION no accede al buscador")
    void caso_cp_bct_17() throws Exception {
        // Act + Assert
        mockMvc.perform(get(BUSCADOR_FABRICANTES))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(fabricanteBarrilServicio, never()).filtrarFabricantesBarril(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-45: un fabricante con un nombre muy largo y sin espacios no desborda el buscador y conserva el botón \"Elegir\"")
    void caso_cp_bct_45() throws Exception {
        // Arrange
        String nombreLargo = "A".repeat(100);
        when(fabricanteBarrilServicio.filtrarFabricantesBarril(any(), any()))
                .thenReturn(new PageImpl<>(List.of(fabricante(2L, nombreLargo)), PageRequest.of(0, 8), 1));

        // Act + Assert
        mockMvc.perform(get(BUSCADOR_FABRICANTES))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<table class=\"tabla-buscador\">")))
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"px-4 py-3 font-medium text-slate-900\"\\s*>" + nombreLargo + "</td>.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-elegir[^>]*data-id=\"2\"[^>]*data-texto=\"" + nombreLargo + "\".*>Elegir</button>.*")));
    }

    // ==================== GET /barriles/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-18: el formulario de alta muestra los campos con asterisco, placeholders y el selector de fabricante")
    void caso_cp_bct_18() throws Exception {
        // Act
        String html = mockMvc.perform(get("/barriles/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/barril-form"))
                .andExpect(content().string(containsString("Nuevo barril")))
                .andExpect(content().string(containsString("action=\"/barriles\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString(">Número de serie ")))
                .andExpect(content().string(containsString("Se asigna al guardar")))
                .andExpect(content().string(containsString("Único entre los barriles del mismo fabricante.")))
                .andExpect(content().string(not(containsString("name=\"identificadorInterno\""))))
                .andExpect(content().string(containsString("placeholder=\"Ej: SN-2024-001\"")))
                .andExpect(content().string(containsString("Capacidad (L)")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 50\"")))
                .andExpect(content().string(containsString("Un número mayor a 0 y hasta 1.000.</p>")))
                .andExpect(content().string(containsString("Usos antes de mantenimiento")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 100\"")))
                .andExpect(content().string(containsString("Un número entero mayor a 0 y hasta 100.000. Al llegar a esa cantidad de usos se requiere mantenimiento.</p>")))
                .andExpect(content().string(containsString("Fabricante")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione fabricante\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"" + BUSCADOR_FABRICANTES + "\"")))
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(containsString("Crear barril")))
                .andExpect(content().string(containsString("Volver a barriles")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/barriles\"[^>]*>\\s*<i[^>]*></i>\\s*<span>Volver a barriles.*")))
                .andExpect(content().string(not(containsString("No se pudo crear"))))
                .andReturn().getResponse().getContentAsString();

        // Assert: el asterisco rojo de obligatorio en los cuatro campos
        assertThat(html.split(Pattern.quote(OBLIGATORIO), -1)).hasSize(5);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-19: el formulario de alta no ofrece estado operativo ni contenido actual")
    void caso_cp_bct_19() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/barriles/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("estadoOperativo"))))
                .andExpect(content().string(not(containsString("contenidoActual"))))
                .andExpect(content().string(not(containsString("Contenido actual"))))
                .andExpect(content().string(not(containsString("Estado operativo"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-BCT-20: el OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta")
    void caso_cp_bct_20() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/barriles/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /barriles ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-21: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_bct_21() throws Exception {
        // Arrange
        when(barrilServicio.altaBarril(any())).thenReturn(barril(9L, "BRL-01", 2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("mensaje", "Barril creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<BarrilFormDTO> captor = ArgumentCaptor.forClass(BarrilFormDTO.class);
        verify(barrilServicio).altaBarril(captor.capture());
        assertThat(captor.getValue().getNumeroSerie()).isEqualTo("SN-2024-001");
        assertThat(captor.getValue().getCapacidad()).isEqualTo(50.0);
        assertThat(captor.getValue().getUsosMaximosAntesMantenimiento()).isEqualTo(100);
        assertThat(captor.getValue().getIdFabricanteBarril()).isEqualTo(2L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-22: sin ningún dato vuelve al formulario con todos los errores, sin llamar al service")
    void caso_cp_bct_22() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).param("numeroSerie", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/barril-form"))
                .andExpect(content().string(containsString("No se pudo crear el barril")))
                .andExpect(content().string(containsString("El número de serie es obligatorio")))
                .andExpect(content().string(containsString("La capacidad es obligatoria")))
                .andExpect(content().string(containsString("Los usos máximos antes de mantenimiento son obligatorios")))
                .andExpect(content().string(containsString("El fabricante de barril es obligatorio")));

        verify(barrilServicio, never()).altaBarril(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-23: una capacidad no numérica muestra un mensaje en español, sin llamar al service")
    void caso_cp_bct_23() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosCon("capacidad", "abc")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese un número válido")))
                .andExpect(content().string(not(containsString("Failed to convert"))));

        verify(barrilServicio, never()).altaBarril(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-24: unos usos no enteros muestran un mensaje en español, sin llamar al service")
    void caso_cp_bct_24() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosCon("usosMaximosAntesMantenimiento", "abc")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese un número entero válido")));

        verify(barrilServicio, never()).altaBarril(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-25: al volver con errores el selector conserva el fabricante elegido")
    void caso_cp_bct_25() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).param("numeroSerie", "").param("capacidad", "50")
                        .param("idFabricanteBarril", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*name=\"idFabricanteBarril\"[^>]*value=\"2\".*")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(not(containsString("value=\"50.0\""))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-26: un número de serie duplicado vuelve al formulario con el mensaje del service junto al campo número de serie")
    void caso_cp_bct_26() throws Exception {
        // Arrange
        when(barrilServicio.altaBarril(any())).thenThrow(new RecursoDuplicadoException("Ya existe un barril con el número de serie 'SN-2024-001' para el fabricante seleccionado"));
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/barril-form"))
                .andExpect(content().string(containsString("Ya existe un barril con el número de serie")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"numeroSerie\"[^>]*border-rose-400.*")))
                .andExpect(content().string(containsString("value=\"SN-2024-001\"")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-27: una regla de negocio rechazada por el service se muestra como error general del formulario")
    void caso_cp_bct_27() throws Exception {
        // Arrange
        when(barrilServicio.altaBarril(any())).thenThrow(new ReglaNegocioException("Regla incumplida"));

        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear el barril")))
                .andExpect(content().string(containsString("Regla incumplida")))
                .andExpect(content().string(containsString("value=\"SN-2024-001\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-28: un fabricante inexistente se informa junto al campo fabricante")
    void caso_cp_bct_28() throws Exception {
        // Arrange
        String mensaje = "No se encontró el fabricante de barril con ID: 99";
        when(barrilServicio.altaBarril(any())).thenThrow(new RecursoNoEncontradoException(mensaje));
        when(fabricanteBarrilServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException(mensaje));

        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosCon("idFabricanteBarril", "99")))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/barril-form"))
                .andExpect(content().string(containsString(mensaje)))
                .andExpect(content().string(containsString("data-selector-texto value=\"\"")))
                .andExpect(content().string(containsString("value=\"SN-2024-001\"")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-BCT-29: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta")
    void caso_cp_bct_29() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(barrilServicio, never()).altaBarril(any());
    }

    // ==================== GET /barriles/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-30: el formulario de edición muestra los datos actuales, el fabricante elegido y apunta a la modificación")
    void caso_cp_bct_30() throws Exception {
        // Arrange
        when(barrilServicio.buscarPorId(4L)).thenReturn(barril(4L, "BRL-01", 2L, "Barrilitos SA"));
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(get("/barriles/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("barril/barril-form"))
                .andExpect(content().string(containsString("Editar barril")))
                .andExpect(content().string(containsString("value=\"SN-2024-001\"")))
                .andExpect(content().string(containsString(">BRL-01<")))
                .andExpect(content().string(containsString("value=\"50\"")))
                .andExpect(content().string(not(containsString("value=\"50.0\""))))
                .andExpect(content().string(containsString("value=\"100\"")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"idFabricanteBarril\"[^>]*value=\"2\".*")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")))
                .andExpect(content().string(containsString("action=\"/barriles/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(containsString("Volver a barriles")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-31: un barril inexistente redirige al origen con el motivo")
    void caso_cp_bct_31() throws Exception {
        // Arrange
        when(barrilServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el barril con ID: 99"));

        // Act + Assert
        mockMvc.perform(get("/barriles/99/editar").header("Referer", "/barriles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("mensaje", "No se encontró el barril con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-BCT-32: un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de edición")
    void caso_cp_bct_32() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/barriles/4/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(barrilServicio, never()).buscarPorId(any());
    }

    // ==================== POST /barriles/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-33: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_bct_33() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles/4").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("mensaje", "Barril modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(barrilServicio).modificarBarril(eq(4L), any(BarrilFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-34: modificar sin datos obligatorios vuelve al formulario de edición con los errores, sin llamar al service")
    void caso_cp_bct_34() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(post("/barriles/4").with(csrf()).param("numeroSerie", "  ").param("idFabricanteBarril", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/barriles/4\"")))
                .andExpect(content().string(containsString("No se pudo modificar el barril")))
                .andExpect(content().string(containsString("El número de serie es obligatorio")))
                .andExpect(content().string(containsString("value=\"Barrilitos SA\"")));

        verify(barrilServicio, never()).modificarBarril(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-35: modificar con un número de serie duplicado vuelve al formulario de edición con el mensaje del service")
    void caso_cp_bct_35() throws Exception {
        // Arrange
        when(barrilServicio.modificarBarril(eq(4L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un barril con el número de serie 'SN-2' para el fabricante seleccionado"));

        // Act + Assert
        mockMvc.perform(post("/barriles/4").with(csrf()).params(datosCon("numeroSerie", "SN-2")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el barril")))
                .andExpect(content().string(containsString("Ya existe un barril con el número de serie &#39;SN-2&#39; para el fabricante seleccionado")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"numeroSerie\"[^>]*border-rose-400.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-36: si el service rechaza la modificación por una regla de negocio, el motivo se muestra en el formulario")
    void caso_cp_bct_36() throws Exception {
        // Arrange
        when(barrilServicio.modificarBarril(eq(4L), any())).thenThrow(new ReglaNegocioException("La capacidad no puede ser menor al contenido actual del barril (30.0 L)"));

        // Act + Assert
        mockMvc.perform(post("/barriles/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el barril")))
                .andExpect(content().string(containsString("La capacidad no puede ser menor al contenido actual del barril (30.0 L)")))
                .andExpect(content().string(containsString("value=\"SN-2024-001\"")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-BCT-37: un rol distinto de GERENTE_DE_PRODUCCION no puede modificar")
    void caso_cp_bct_37() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles/4").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(barrilServicio, never()).modificarBarril(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-44: modificar un barril inexistente (con el fabricante vigente) lo maneja el ControllerAdvices, no el campo fabricante")
    void caso_cp_bct_44() throws Exception {
        // Arrange
        String mensaje = "No se encontró el barril con ID: 99";
        when(barrilServicio.modificarBarril(eq(99L), any())).thenThrow(new RecursoNoEncontradoException(mensaje));
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        mockMvc.perform(post("/barriles/99").with(csrf()).header("Referer", "/barriles").params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /barriles/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-38: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_bct_38() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("mensaje", "Barril dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(barrilServicio).bajaBarril(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-39: si el service rechaza la baja (barril no disponible) vuelve al origen con la alerta de advertencia")
    void caso_cp_bct_39() throws Exception {
        // Arrange
        doThrow(new ReglaNegocioException("No se puede dar de baja el barril porque no se encuentra disponible")).when(barrilServicio).bajaBarril(4L);

        // Act + Assert
        mockMvc.perform(post("/barriles/4/baja").with(csrf()).header("Referer", "/barriles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barriles"))
                .andExpect(flash().attribute("mensaje", "No se puede dar de baja el barril porque no se encuentra disponible"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-BCT-40: un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja")
    void caso_cp_bct_40() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/barriles/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(barrilServicio, never()).bajaBarril(any());
    }

    // ==================== Sidebar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-41: la sidebar del GERENTE_DE_PRODUCCION ofrece \"Gestión de Barriles\" en Administración, justo encima de \"Gestión de Equipamiento\"")
    void caso_cp_bct_41() throws Exception {
        // Arrange
        when(barrilServicio.filtrarBarriles(any(), any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get("/barriles"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Administración.*href=\"/barriles\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-boxes\"></i>\\s*<span>Gestión de Barriles</span>.*")))
                .andExpect(content().string(matchesPattern("(?s).*Gestión de Barriles((?!Gestión de )[\\s\\S])*Gestión de Equipamiento.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-42: la opción permanece resaltada en el formulario de alta y en el de edición")
    void caso_cp_bct_42() throws Exception {
        // Arrange
        when(barrilServicio.buscarPorId(4L)).thenReturn(barril(4L, "BRL-01", 2L, "Barrilitos SA"));
        String resaltada = "(?s).*href=\"/barriles\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-boxes\"></i>.*";

        // Act + Assert
        mockMvc.perform(get("/barriles/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(resaltada)));
        mockMvc.perform(get("/barriles/4/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(resaltada)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-BCT-43: la sidebar de los demás roles no muestra \"Gestión de Barriles\"")
    void caso_cp_bct_43(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/barriles\""))))
                .andExpect(content().string(not(containsString("Gestión de Barriles"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-BCT-46: al volver con errores la capacidad decimal se muestra con punto y un texto inválido deja el campo vacío con su error")
    void caso_cp_bct_46() throws Exception {
        // Arrange
        when(fabricanteBarrilServicio.buscarPorId(2L)).thenReturn(fabricante(2L, "Barrilitos SA"));

        // Act + Assert
        LinkedMultiValueMap<String, String> sinIdentificador = datosCon("capacidad", "4.5");
        sinIdentificador.set("numeroSerie", "");
        mockMvc.perform(post("/barriles").with(csrf()).params(sinIdentificador))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"4.5\"")));

        mockMvc.perform(post("/barriles").with(csrf()).params(datosCon("capacidad", "abc")))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"capacidad\"[^>]*border-rose-400[^>]*value=\"\".*")));

        verify(barrilServicio, never()).altaBarril(any());
    }

    // ==================== Datos de prueba ====================

    private static LinkedMultiValueMap<String, String> datosCon(String campo, String valor) {
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set(campo, valor);
        return datos;
    }

    private static LinkedMultiValueMap<String, String> datosValidos() {
        LinkedMultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("numeroSerie", "SN-2024-001");
        datos.add("capacidad", "50");
        datos.add("usosMaximosAntesMantenimiento", "100");
        datos.add("idFabricanteBarril", "2");
        return datos;
    }

    private static PageImpl<BarrilResponseDTO> paginaVacia() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }

    private static PageImpl<FabricanteBarrilResponseDTO> paginaVaciaFabricantes() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 8), 0);
    }

    private static FabricanteBarrilResponseDTO fabricante(Long id, String nombre) {
        return FabricanteBarrilResponseDTO.builder().id(id).nombre(nombre).build();
    }

    private static BarrilResponseDTO barril(Long id, String identificadorInterno, Long idFabricante, String nombreFabricante) {
        return BarrilResponseDTO.builder().id(id).identificadorInterno(identificadorInterno).numeroSerie("SN-2024-001").capacidad(50.0).contenidoActual(0.0)
                .estadoOperativo(EstadoOperativoBarril.DISPONIBLE).usosMaximosAntesMantenimiento(100)
                .fabricante(fabricante(idFabricante, nombreFabricante)).build();
    }
}
