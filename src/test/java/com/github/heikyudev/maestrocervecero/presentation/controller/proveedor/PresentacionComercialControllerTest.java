package com.github.heikyudev.maestrocervecero.presentation.controller.proveedor;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.login.HomeController;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.PresentacionComercialFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IPresentacionComercialServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

@WebMvcTest({PresentacionComercialController.class, HomeController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class PresentacionComercialControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";
    private static final String URL = "/presentaciones-comerciales";
    private static final String NOTA_BLOQUEO = "La cantidad y la unidad de medida no se pueden cambiar si la presentación está en el catálogo de un proveedor activo o en una orden de compra pendiente.";
    private static final String ENLACE_RESALTADO =
            "(?s).*href=\"/presentaciones-comerciales\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-box\"></i>\\s*<span>Gestión de Presentaciones</span>.*";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPresentacionComercialServicio presentacionComercialServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /presentaciones-comerciales ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-01: el GERENTE_DE_COMPRAS ve el listado con nombre, cantidad, unidad y las acciones por fila")
    void caso_cp_pcc_01() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(presentacion(3L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO)), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("presentacion_comercial/presentacion-lista"))
                .andExpect(content().string(containsString("Gestión de Presentaciones")))
                .andExpect(content().string(containsString("<title>Presentaciones comerciales - Maestro Cervecero</title>")))
                .andExpect(content().string(containsString("Bolsa de 25 Kg")))
                .andExpect(content().string(containsString("<th class=\"px-4 py-3\">Cantidad</th>")))
                .andExpect(content().string(containsString("<th class=\"px-4 py-3\">Unidad</th>")))
                .andExpect(content().string(containsString(">25<")))
                .andExpect(content().string(containsString(">kg<")))
                .andExpect(content().string(containsString("href=\"/presentaciones-comerciales/nuevo\"")))
                .andExpect(content().string(containsString("Nueva presentación")))
                .andExpect(content().string(containsString("href=\"/presentaciones-comerciales/3/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/presentaciones-comerciales/3/baja\"")))
                .andExpect(content().string(containsString("Si está en el catálogo de un proveedor activo o en una orden de compra pendiente, el sistema no lo permitirá.")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")))
                // La sidebar resalta "Gestión de Presentaciones"
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void caso_cp_pcc_02() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL)).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").getDirection().isAscending()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-03: el criterio de nombre llega recortado al service; un texto en blanco equivale a no filtrar")
    void caso_cp_pcc_03() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL).param("nombre", " bolsa ")).andExpect(status().isOk());
        mockMvc.perform(get(URL).param("nombre", "   ")).andExpect(status().isOk());

        // Assert
        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(eq("bolsa"), isNull(), isNull(), any());
        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-04: los filtros por cantidad y por unidad de medida llegan al service; la unidad ofrece etiquetas legibles y \"Todas\" no filtra")
    void caso_cp_pcc_04() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get(URL).param("cantidad", "25").param("unidadDeMedida", "KILOGRAMO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Todas<")))
                .andExpect(content().string(containsString(">Gramo (g)<")))
                .andExpect(content().string(containsString(">Kilogramo (kg)<")))
                .andExpect(content().string(containsString(">Tonelada (t)<")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"KILOGRAMO\"[^>]*selected.*")));
        mockMvc.perform(get(URL).param("cantidad", "").param("unidadDeMedida", "")).andExpect(status().isOk());

        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(isNull(), eq(25.0), eq(UnidadDeMedida.KILOGRAMO), any());
        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-05: los criterios quedan en el formulario y en los links de paginación")
    void caso_cp_pcc_05() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(presentacion(3L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO)), PageRequest.of(1, 20), 60));

        // Act
        mockMvc.perform(get(URL).param("nombre", "bolsa").param("cantidad", "25")
                        .param("unidadDeMedida", "KILOGRAMO").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"bolsa\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Bolsa de 25 Kg\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cantidad\"[^>]*value=\"25\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 25\"")))
                .andExpect(content().string(containsString(URL + "?pagina=0&amp;nombre=bolsa&amp;cantidad=25&amp;unidadDeMedida=KILOGRAMO")))
                .andExpect(content().string(containsString(URL + "?pagina=2&amp;nombre=bolsa&amp;cantidad=25&amp;unidadDeMedida=KILOGRAMO")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/presentaciones-comerciales\"[^>]*>\\s*Limpiar.*")));

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(eq("bolsa"), eq(25.0), eq(UnidadDeMedida.KILOGRAMO), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-06: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_pcc_06() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron presentaciones comerciales con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-07: un valor del filtro que no se puede convertir al tipo del parámetro no es un error del sistema")
    void caso_cp_pcc_07() throws Exception {
        // Act + Assert
        for (String[] parametro : new String[][]{{"cantidad", "abc"}, {"unidadDeMedida", "LITRO"}}) {
            mockMvc.perform(get(URL + "?" + parametro[0] + "=" + parametro[1]).header("Referer", URL))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(URL))
                    .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                    .andExpect(flash().attribute("tipo", "warning"));
        }

        verify(presentacionComercialServicio, never()).filtrarPresentacionesComerciales(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-08: el listado muestra las cantidades enteras sin \".0\" y conserva los decimales")
    void caso_cp_pcc_08() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        presentacion(3L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO),
                        presentacion(4L, "Sobre de 2,5 g", 2.5, UnidadDeMedida.GRAMO)), PageRequest.of(0, 20), 2));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">25<")))
                .andExpect(content().string(not(containsString("25.0"))))
                .andExpect(content().string(containsString(">2,5<")))
                .andExpect(content().string(containsString(">g<")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "ADMINISTRADOR", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-PCC-09: un rol distinto de GERENTE_DE_COMPRAS no accede al listado y no llega al service")
    void caso_cp_pcc_09(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL).with(user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(presentacionComercialServicio);
    }

    @Test
    @DisplayName("CP-PCC-10: un usuario sin sesión es enviado al login en cada operación y no llega al service")
    void caso_cp_pcc_10() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/nuevo")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/3/editar")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/3").with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/3/baja").with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));

        verifyNoInteractions(presentacionComercialServicio);
    }

    // ==================== GET /presentaciones-comerciales/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-11: el formulario de alta muestra los campos con asterisco, placeholders, ayudas con el límite y el selector de unidad")
    void caso_cp_pcc_11() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("presentacion_comercial/presentacion-form"))
                .andExpect(content().string(containsString("Nueva presentación comercial")))
                .andExpect(content().string(containsString("action=\"/presentaciones-comerciales\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString(">Nombre ")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Bolsa de 25 Kg\"")))
                .andExpect(content().string(containsString("Único entre las presentaciones comerciales, sin distinguir mayúsculas. Hasta 100 caracteres.</p>")))
                .andExpect(content().string(containsString(">Cantidad ")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 25\"")))
                .andExpect(content().string(containsString("Un número mayor a 0. Su equivalente no puede superar los 10.000 kilogramos.</p>")))
                .andExpect(content().string(containsString(">Unidad de medida ")))
                .andExpect(content().string(containsString(">Seleccione unidad<")))
                .andExpect(content().string(containsString(">Gramo (g)<")))
                .andExpect(content().string(containsString(">Kilogramo (kg)<")))
                .andExpect(content().string(containsString(">Tonelada (t)<")))
                .andExpect(content().string(containsString("Crear presentación")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/presentaciones-comerciales\"[^>]*>\\s*<i[^>]*></i>\\s*<span>Volver a presentaciones comerciales.*")))
                .andExpect(content().string(not(containsString(NOTA_BLOQUEO))))
                .andExpect(content().string(not(containsString("No se pudo crear"))))
                .andReturn().getResponse().getContentAsString();

        // Assert: el asterisco rojo de obligatorio en los tres campos
        assertThat(html.split(Pattern.quote(OBLIGATORIO), -1)).hasSize(4);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-12: un rol distinto de GERENTE_DE_COMPRAS no puede abrir el formulario de alta")
    void caso_cp_pcc_12() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /presentaciones-comerciales ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-13: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_pcc_13() throws Exception {
        // Arrange
        when(presentacionComercialServicio.altaPresentacionComercial(any())).thenReturn(presentacion(9L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Presentación comercial creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<PresentacionComercialFormDTO> captor = ArgumentCaptor.forClass(PresentacionComercialFormDTO.class);
        verify(presentacionComercialServicio).altaPresentacionComercial(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Bolsa de 25 Kg");
        assertThat(captor.getValue().getCantidad()).isEqualTo(25.0);
        assertThat(captor.getValue().getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-14: sin ningún dato vuelve al formulario con todos los errores, sin llamar al service")
    void caso_cp_pcc_14() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).param("nombre", "  ").param("cantidad", "").param("unidadDeMedida", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("presentacion_comercial/presentacion-form"))
                .andExpect(content().string(containsString("No se pudo crear la presentación comercial")))
                .andExpect(content().string(containsString("El nombre es obligatorio")))
                .andExpect(content().string(containsString("La cantidad es obligatoria")))
                .andExpect(content().string(containsString("La unidad de medida es obligatoria")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"unidadDeMedida\"[^>]*border-rose-400.*")));

        verify(presentacionComercialServicio, never()).altaPresentacionComercial(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-15: una cantidad no numérica o una unidad inexistente muestran un mensaje en español, sin llamar al service")
    void caso_cp_pcc_15() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("cantidad", "abc")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese un número válido")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cantidad\"[^>]*border-rose-400[^>]*value=\"\".*")))
                .andExpect(content().string(not(containsString("Failed to convert"))));
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("unidadDeMedida", "LITRO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El valor ingresado no es válido")))
                .andExpect(content().string(not(containsString("Failed to convert"))));

        verify(presentacionComercialServicio, never()).altaPresentacionComercial(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-16: un nombre duplicado vuelve al formulario con el mensaje del service junto al campo nombre")
    void caso_cp_pcc_16() throws Exception {
        // Arrange
        when(presentacionComercialServicio.altaPresentacionComercial(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una presentación comercial con el nombre 'Bolsa de 25 Kg'"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name("presentacion_comercial/presentacion-form"))
                .andExpect(content().string(containsString("Ya existe una presentación comercial con el nombre")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*border-rose-400.*")))
                .andExpect(content().string(containsString("value=\"Bolsa de 25 Kg\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"KILOGRAMO\"[^>]*selected.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-17: una regla de negocio rechazada por el service se muestra como error general del formulario")
    void caso_cp_pcc_17() throws Exception {
        // Arrange
        when(presentacionComercialServicio.altaPresentacionComercial(any()))
                .thenThrow(new ReglaNegocioException("La cantidad no puede superar el equivalente a 10.000 kilogramos"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("cantidad", "20000")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear la presentación comercial")))
                .andExpect(content().string(containsString("La cantidad no puede superar el equivalente a 10.000 kilogramos")))
                .andExpect(content().string(containsString("value=\"Bolsa de 25 Kg\"")))
                .andExpect(content().string(containsString("value=\"20000\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-18: un rol distinto de GERENTE_DE_COMPRAS no puede dar de alta")
    void caso_cp_pcc_18() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(presentacionComercialServicio, never()).altaPresentacionComercial(any());
    }

    // ==================== GET /presentaciones-comerciales/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-19: el formulario de edición muestra los datos actuales, la nota de bloqueo y apunta a la modificación")
    void caso_cp_pcc_19() throws Exception {
        // Arrange
        when(presentacionComercialServicio.buscarPorId(3L)).thenReturn(presentacion(3L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO));

        // Act + Assert
        mockMvc.perform(get(URL + "/3/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("presentacion_comercial/presentacion-form"))
                .andExpect(content().string(containsString("Editar presentación comercial")))
                .andExpect(content().string(containsString("value=\"Bolsa de 25 Kg\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cantidad\"[^>]*value=\"25\".*")))
                .andExpect(content().string(not(containsString("value=\"25.0\""))))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"KILOGRAMO\"[^>]*selected.*")))
                .andExpect(content().string(containsString("action=\"/presentaciones-comerciales/3\"")))
                .andExpect(content().string(containsString(NOTA_BLOQUEO)))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(containsString("Volver a presentaciones comerciales")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-20: el formulario de edición conserva los decimales de la cantidad")
    void caso_cp_pcc_20() throws Exception {
        // Arrange
        when(presentacionComercialServicio.buscarPorId(4L)).thenReturn(presentacion(4L, "Sobre de 2,5 g", 2.5, UnidadDeMedida.GRAMO));

        // Act + Assert
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"cantidad\"[^>]*value=\"2.5\".*")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"GRAMO\"[^>]*selected.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-21: una presentación inexistente redirige al origen con el motivo")
    void caso_cp_pcc_21() throws Exception {
        // Arrange
        when(presentacionComercialServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la presentación comercial con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/99/editar").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró la presentación comercial con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-22: un rol distinto de GERENTE_DE_COMPRAS no puede abrir el formulario de edición")
    void caso_cp_pcc_22() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/3/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(presentacionComercialServicio, never()).buscarPorId(any());
    }

    // ==================== POST /presentaciones-comerciales/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-23: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_pcc_23() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/3").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Presentación comercial modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(presentacionComercialServicio).modificarPresentacionComercial(eq(3L), any(PresentacionComercialFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-24: modificar sin datos obligatorios vuelve al formulario de edición con los errores, sin llamar al service")
    void caso_cp_pcc_24() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/3").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/presentaciones-comerciales/3\"")))
                .andExpect(content().string(containsString("No se pudo modificar la presentación comercial")))
                .andExpect(content().string(containsString("El nombre es obligatorio")))
                .andExpect(content().string(containsString("La cantidad es obligatoria")))
                .andExpect(content().string(containsString("La unidad de medida es obligatoria")))
                .andExpect(content().string(containsString(NOTA_BLOQUEO)));

        verify(presentacionComercialServicio, never()).modificarPresentacionComercial(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-25: modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service")
    void caso_cp_pcc_25() throws Exception {
        // Arrange
        when(presentacionComercialServicio.modificarPresentacionComercial(eq(3L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una presentación comercial con el nombre 'Bolsa de 25 Kg'"));

        // Act + Assert
        mockMvc.perform(post(URL + "/3").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar la presentación comercial")))
                .andExpect(content().string(containsString("Ya existe una presentación comercial con el nombre &#39;Bolsa de 25 Kg&#39;")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*border-rose-400.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-26: si el service rechaza el cambio de cantidad o unidad de una presentación en uso, el motivo se muestra en el formulario")
    void caso_cp_pcc_26() throws Exception {
        // Arrange
        String mensaje = "No se puede modificar la cantidad ni la unidad de medida de una presentación comercial que está en un catálogo de proveedor activo o en una orden de compra pendiente";
        when(presentacionComercialServicio.modificarPresentacionComercial(eq(3L), any())).thenThrow(new ReglaNegocioException(mensaje));

        // Act + Assert
        mockMvc.perform(post(URL + "/3").with(csrf()).params(datosCon("cantidad", "50")))
                .andExpect(status().isOk())
                .andExpect(view().name("presentacion_comercial/presentacion-form"))
                .andExpect(content().string(containsString("action=\"/presentaciones-comerciales/3\"")))
                .andExpect(content().string(containsString("No se pudo modificar la presentación comercial")))
                .andExpect(content().string(containsString(mensaje)))
                .andExpect(content().string(containsString("value=\"Bolsa de 25 Kg\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cantidad\"[^>]*value=\"50\".*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-27: modificar una presentación inexistente lo maneja el ControllerAdvices")
    void caso_cp_pcc_27() throws Exception {
        // Arrange
        String mensaje = "No se encontró la presentación comercial con ID: 99";
        when(presentacionComercialServicio.modificarPresentacionComercial(eq(99L), any())).thenThrow(new RecursoNoEncontradoException(mensaje));

        // Act + Assert
        mockMvc.perform(post(URL + "/99").with(csrf()).header("Referer", URL).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-28: un rol distinto de GERENTE_DE_COMPRAS no puede modificar")
    void caso_cp_pcc_28() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/3").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(presentacionComercialServicio, never()).modificarPresentacionComercial(any(), any());
    }

    // ==================== POST /presentaciones-comerciales/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-29: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_pcc_29() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/3/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Presentación comercial dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(presentacionComercialServicio).bajaPresentacionComercial(3L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-30: si el service rechaza la baja (presentación en uso) vuelve al origen con la alerta de advertencia")
    void caso_cp_pcc_30() throws Exception {
        // Arrange
        String mensaje = "No se puede dar de baja la presentación comercial porque se encuentra asociada al catálogo de al menos un proveedor activo";
        doThrow(new ReglaNegocioException(mensaje)).when(presentacionComercialServicio).bajaPresentacionComercial(3L);

        // Act + Assert
        mockMvc.perform(post(URL + "/3/baja").with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PCC-31: un rol distinto de GERENTE_DE_COMPRAS no puede dar de baja")
    void caso_cp_pcc_31() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/3/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(presentacionComercialServicio, never()).bajaPresentacionComercial(any());
    }

    // ==================== Sidebar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-32: la sidebar del GERENTE_DE_COMPRAS ofrece \"Gestión de Presentaciones\" en la sección Administración, debajo de Operación")
    void caso_cp_pcc_32() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Operación.*Órdenes de Compra.*Administración.*href=\"/presentaciones-comerciales\"[^>]*>\\s*<i class=\"bi text-lg bi-box\"></i>\\s*<span>Gestión de Presentaciones</span>.*")))
                // En el panel de control la opción no está resaltada
                .andExpect(content().string(not(matchesPattern(ENLACE_RESALTADO))))
                // El GERENTE_DE_COMPRAS no ve los demás maestros de Administración
                .andExpect(content().string(not(containsString("Gestión de Insumos"))))
                .andExpect(content().string(not(containsString("Gestión de Equipamiento"))))
                .andExpect(content().string(not(containsString("Gestión de Ubicaciones"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PCC-33: la opción permanece resaltada en el formulario de alta y en el de edición")
    void caso_cp_pcc_33() throws Exception {
        // Arrange
        when(presentacionComercialServicio.buscarPorId(3L)).thenReturn(presentacion(3L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO));

        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
        mockMvc.perform(get(URL + "/3/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-PCC-34: la sidebar de los demás roles no muestra \"Gestión de Presentaciones\"")
    void caso_cp_pcc_34(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/presentaciones-comerciales\""))))
                .andExpect(content().string(not(containsString("Gestión de Presentaciones"))));
    }

    @Test
    @DisplayName("CP-PCC-35: la sección Administración sigue apareciendo solo para el GERENTE_DE_PRODUCCION y el ADMINISTRADOR entre los demás roles")
    void caso_cp_pcc_35() throws Exception {
        // Act + Assert
        for (String rol : List.of("GERENTE_DE_PRODUCCION", "ADMINISTRADOR")) {
            mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Administración")));
        }
        for (String rol : List.of("OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL")) {
            mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Administración"))));
        }
    }

    // ==================== Datos de prueba ====================

    private static LinkedMultiValueMap<String, String> datosCon(String campo, String valor) {
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set(campo, valor);
        return datos;
    }

    private static LinkedMultiValueMap<String, String> datosValidos() {
        LinkedMultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("nombre", "Bolsa de 25 Kg");
        datos.add("cantidad", "25");
        datos.add("unidadDeMedida", "KILOGRAMO");
        return datos;
    }

    private static PageImpl<PresentacionComercialResponseDTO> paginaVacia() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }

    private static PresentacionComercialResponseDTO presentacion(Long id, String nombre, Double cantidad, UnidadDeMedida unidad) {
        return PresentacionComercialResponseDTO.builder().id(id).nombre(nombre).cantidad(cantidad).unidadDeMedida(unidad).build();
    }
}
