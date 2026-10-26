package com.github.heikyudev.maestrocervecero.presentation.controller.proveedor;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.login.HomeController;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.ProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.proveedor.VersionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IPresentacionComercialServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.PresentacionComercialResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({ProveedorController.class, HomeController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class ProveedorControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";
    private static final String URL = "/proveedores";
    private static final String VISTA_FORMULARIO = "proveedor/proveedor-form";
    private static final String NOTA_VERSION = "Al guardar se registra una nueva versión del proveedor.";
    private static final String ENLACE_RESALTADO =
            "(?s).*href=\"/proveedores\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-truck\"></i>\\s*<span>Gestión de Proveedores</span>.*";
    /** Los campos enlazados con th:field escapan los caracteres no ASCII como entidades HTML. */
    private static final String RAZON_SOCIAL_ESCAPADA = "Malter&iacute;as del Sur S.A.";
    private static final String MENSAJE_DUPLICADO_CUIT = "Ya existe un proveedor con el CUIT '30712345678'";
    private static final String MENSAJE_DUPLICADO_CORREO = "Ya existe un proveedor con el correo electrónico 'ventas@malteriasdelsur.com'";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProveedorServicio proveedorServicio;

    @MockitoBean
    private IInsumoServicio insumoServicio;

    @MockitoBean
    private IPresentacionComercialServicio presentacionComercialServicio;

    @MockitoBean
    private ILocalidadServicio localidadServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /proveedores ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-01: el GERENTE_DE_COMPRAS ve el listado con razón social, nombre comercial, CUIT con guiones, localidad, teléfono, ítems del catálogo y las acciones por fila")
    void caso_cp_pvc_01() throws Exception {
        // Arrange
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(proveedor(4L, "3584123456")), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("proveedor/proveedor-lista"))
                .andExpect(content().string(containsString("<title>Proveedores - Maestro Cervecero</title>")))
                .andExpect(content().string(containsString("Gestión de Proveedores")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Razón social</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Nombre comercial</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">CUIT</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Localidad</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Teléfono</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Ítems del catálogo</th>.*")))
                .andExpect(content().string(containsString(">Malterías del Sur S.A.<")))
                .andExpect(content().string(containsString(">Malterías del Sur<")))
                .andExpect(content().string(containsString(">30-71234567-8<")))
                .andExpect(content().string(not(containsString("30712345678"))))
                .andExpect(content().string(containsString(">Río Cuarto<")))
                .andExpect(content().string(containsString(">3584123456<")))
                .andExpect(content().string(containsString(">2<")))
                .andExpect(content().string(containsString("href=\"/proveedores/nuevo\"")))
                .andExpect(content().string(containsString("Nuevo proveedor")))
                .andExpect(content().string(containsString("href=\"/proveedores/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/proveedores/4/baja\"")))
                .andExpect(content().string(containsString("Si tiene una orden de compra pendiente, el sistema no lo permitirá.")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")))
                // La sidebar resalta "Gestión de Proveedores"
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-02: sin filtros pide páginas de 20 sin ordenamiento en el Pageable (el orden por razón social lo define la consulta), con los criterios en null")
    void caso_cp_pvc_02() throws Exception {
        // Arrange
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL)).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proveedorServicio).filtrarProveedores(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.unsorted());
        verifyNoInteractions(localidadServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-03: los criterios de texto llegan recortados al service, el CUIT con guiones llega tal cual y un texto en blanco equivale a no filtrar")
    void caso_cp_pvc_03() throws Exception {
        // Arrange
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL).param("razonSocial", " malterías ").param("nombreComercial", " sur ").param("cuit", " 30-71234567-8 "))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL).param("razonSocial", "   ").param("nombreComercial", "  ").param("cuit", " "))
                .andExpect(status().isOk());

        // Assert
        verify(proveedorServicio).filtrarProveedores(eq("malterías"), eq("sur"), eq("30-71234567-8"), isNull(), any());
        verify(proveedorServicio).filtrarProveedores(isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-04: el filtro por localidad usa el selector modal de localidades; una localidad inexistente no filtra")
    void caso_cp_pvc_04() throws Exception {
        // Arrange
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any())).thenReturn(paginaVacia());
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(localidadServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la localidad con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL).param("idLocalidad", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/localidades/buscador\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione localidad\"")))
                .andExpect(content().string(containsString("value=\"Río Cuarto (Córdoba)\"")))
                .andExpect(content().string(containsString("data-selector-limpiar")))
                .andExpect(content().string(not(containsString("data-selector-menu"))));
        mockMvc.perform(get(URL).param("idLocalidad", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"99\""))));

        verify(proveedorServicio).filtrarProveedores(isNull(), isNull(), isNull(), eq(9L), any());
        verify(proveedorServicio).filtrarProveedores(isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-05: los criterios quedan en el formulario con sus placeholders y en los links de paginación")
    void caso_cp_pvc_05() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(proveedor(4L, "3584123456")), PageRequest.of(1, 20), 60));

        // Act
        mockMvc.perform(get(URL).param("razonSocial", "malterias").param("nombreComercial", "sur")
                        .param("cuit", "30712345678").param("idLocalidad", "9").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"razonSocial\"[^>]*value=\"malterias\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Malterías del Sur S.A.\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombreComercial\"[^>]*value=\"sur\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Malterías del Sur\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cuit\"[^>]*value=\"30712345678\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 30-71234567-8\"")))
                .andExpect(content().string(containsString(URL + "?pagina=0&amp;razonSocial=malterias&amp;nombreComercial=sur&amp;cuit=30712345678&amp;idLocalidad=9")))
                .andExpect(content().string(containsString(URL + "?pagina=2&amp;razonSocial=malterias&amp;nombreComercial=sur&amp;cuit=30712345678&amp;idLocalidad=9")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/proveedores\"[^>]*>\\s*Limpiar.*")));

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(proveedorServicio).filtrarProveedores(eq("malterias"), eq("sur"), eq("30712345678"), eq(9L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-06: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_pvc_06() throws Exception {
        // Arrange
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron proveedores con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-07: un valor del filtro o de la página que no se puede convertir al tipo del parámetro no es un error del sistema")
    void caso_cp_pvc_07() throws Exception {
        // Act + Assert
        for (String[] parametro : new String[][]{{"idLocalidad", "abc"}, {"pagina", "abc"}}) {
            mockMvc.perform(get(URL + "?" + parametro[0] + "=" + parametro[1]).header("Referer", URL))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(URL))
                    .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                    .andExpect(flash().attribute("tipo", "warning"));
        }

        verify(proveedorServicio, never()).filtrarProveedores(any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-08: el teléfono vacío se muestra con un guion y los textos largos se truncan con el texto completo en el tooltip")
    void caso_cp_pvc_08() throws Exception {
        // Arrange
        String largo = "A".repeat(1000);
        ProveedorResponseDTO sinTelefono = proveedor(4L, null);
        ProveedorResponseDTO conTextosLargos = ProveedorResponseDTO.builder().id(5L).version(VersionProveedorResponseDTO.builder()
                .razonSocial(largo).nombreComercial(largo).cuit("20111111112").localidad(localidad()).catalogoProveedor(List.of()).build()).build();
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sinTelefono, conTextosLargos), PageRequest.of(0, 20), 2));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*whitespace-nowrap\">-</td>.*")))
                .andExpect(content().string(matchesPattern("(?s).*class=\"[^\"]*truncate[^\"]*\"[^>]*data-tooltip=\"" + largo + "\".*")))
                .andExpect(content().string(containsString(">0<")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-48: con textos de 100 caracteres sin espacios las columnas de texto quedan acotadas a 9rem y las celdas usan px-2, así la tabla entra completa con sus acciones")
    void caso_cp_pvc_48() throws Exception {
        // Arrange
        String largo = "ZZ-PV-" + "X".repeat(94);
        ProveedorResponseDTO conTextosLargos = ProveedorResponseDTO.builder().id(5L).version(VersionProveedorResponseDTO.builder()
                .razonSocial(largo).nombreComercial(largo).cuit("20111111112").telefono("123456789012345")
                .localidad(LocalidadResponseDTO.builder().id(9L).nombre(largo).build()).catalogoProveedor(List.of()).build()).build();
        when(proveedorServicio.filtrarProveedores(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(conTextosLargos), PageRequest.of(0, 20), 1));

        // Act
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String tabla = html.substring(html.indexOf("<table"), html.indexOf("</table>"));

        // Assert
        Matcher acotadas = Pattern.compile("<span class=\"inline-block max-w-\\[9rem\\] truncate[^\"]*\"[^>]*data-tooltip=\"" + largo + "\"").matcher(tabla);
        int cantidadAcotadas = 0;
        while (acotadas.find()) {
            cantidadAcotadas++;
        }
        assertThat(cantidadAcotadas).as("razón social, nombre comercial y localidad acotadas a 9rem con tooltip").isEqualTo(3);
        assertThat(tabla).doesNotContain("px-4 py-3").contains("<td class=\"px-2 py-3 text-slate-700 whitespace-nowrap\">20-11111111-2</td>")
                .contains("<td class=\"px-2 py-3 text-slate-700 whitespace-nowrap\">123456789012345</td>")
                .contains("<th class=\"col-acciones px-2 py-3 text-right whitespace-nowrap\">Acciones</th>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "ADMINISTRADOR", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-PVC-09: un rol distinto de GERENTE_DE_COMPRAS no accede al listado ni a los buscadores y no llega a ningún service")
    void caso_cp_pvc_09(String rol) throws Exception {
        // Act + Assert
        for (String ruta : List.of(URL, URL + "/buscador/insumos", URL + "/buscador/presentaciones")) {
            mockMvc.perform(get(ruta).with(user("usuario").roles(rol)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }

        verifyNoInteractions(proveedorServicio, insumoServicio, presentacionComercialServicio, localidadServicio);
    }

    @Test
    @DisplayName("CP-PVC-10: un usuario sin sesión es enviado al login en cada operación y en los buscadores, y no llega a ningún service")
    void caso_cp_pvc_10() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/nuevo")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/4/editar")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/4/baja").with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/buscador/insumos")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/buscador/presentaciones")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));

        verifyNoInteractions(proveedorServicio, insumoServicio, presentacionComercialServicio, localidadServicio);
    }

    // ==================== GET /proveedores/nuevo ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-11: el formulario de alta muestra los campos con asterisco, placeholders y ayudas, el selector de localidad y el catálogo vacío con su ventana para agregar ítems")
    void caso_cp_pvc_11() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("Nuevo proveedor")))
                .andExpect(content().string(containsString("action=\"/proveedores\"")))
                // Un doble clic en el botón de envío no registra el proveedor dos veces (modales.js)
                .andExpect(content().string(matchesPattern("(?s).*<form id=\"proveedor-form\"[^>]*method=\"post\" novalidate data-un-solo-envio[^>]*>.*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Malterías del Sur S.A.\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Malterías del Sur\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 30-71234567-8\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 3584123456\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: ventas@malteriasdelsur.com\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Av. Italia 1234\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione localidad\"")))
                .andExpect(content().string(containsString("11 dígitos, con o sin guiones. Único entre los proveedores.</p>")))
                .andExpect(content().string(containsString("Opcional. Solo dígitos, hasta 15.</p>")))
                .andExpect(content().string(containsString("Único entre los proveedores, sin distinguir mayúsculas. Hasta 255 caracteres.</p>")))
                .andExpect(content().string(containsString("Hasta 100 caracteres.</p>")))
                .andExpect(content().string(containsString("Hasta 255 caracteres.</p>")))
                .andExpect(content().string(containsString("Entre 1 y 100 ítems; el mismo insumo se puede ofrecer en varias presentaciones.")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/localidades/buscador\"")))
                .andExpect(content().string(containsString("name=\"version.idLocalidad\"")))
                // Los gerentes no dan de alta ubicaciones: el selector de localidad no ofrece el menú de alta
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(containsString("Agregar ítem")))
                .andExpect(content().string(containsString(">No hay ítems en el catálogo<")))
                .andExpect(content().string(containsString("<template data-plantilla=\"catalogo\">")))
                .andExpect(content().string(containsString("data-buscador-url=\"/proveedores/buscador/insumos\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/proveedores/buscador/presentaciones\"")))
                .andExpect(content().string(containsString("data-error=\"insumo\"")))
                .andExpect(content().string(containsString("data-error=\"presentacion\"")))
                .andExpect(content().string(containsString("data-error=\"duplicado\"")))
                .andExpect(content().string(containsString("/js/proveedor-form.js")))
                .andExpect(content().string(containsString("Crear proveedor")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/proveedores\"[^>]*>\\s*<i[^>]*></i>\\s*<span>Volver a proveedores.*")))
                .andExpect(content().string(not(containsString(NOTA_VERSION))))
                .andExpect(content().string(not(containsString("No se pudo crear"))))
                .andReturn().getResponse().getContentAsString();

        // Assert: el asterisco rojo en los seis campos obligatorios, en el catálogo y en los dos selectores de la ventana
        assertThat(html.split(Pattern.quote(OBLIGATORIO), -1)).hasSize(10);
        // La ventana para agregar ítems va fuera del formulario: sus campos no se envían
        assertThat(html.indexOf("id=\"dialogo-agregar-catalogo\"")).isGreaterThan(html.indexOf("</form>"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PVC-12: un rol distinto de GERENTE_DE_COMPRAS no puede abrir el formulario de alta")
    void caso_cp_pvc_12() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /proveedores ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-13: alta correcta delega en el service con los datos y las filas del catálogo, y redirige al listado con alerta de éxito")
    void caso_cp_pvc_13() throws Exception {
        // Arrange
        when(proveedorServicio.altaProveedor(any())).thenReturn(proveedor(4L, "3584123456"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Proveedor creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ProveedorFormDTO> captor = ArgumentCaptor.forClass(ProveedorFormDTO.class);
        verify(proveedorServicio).altaProveedor(captor.capture());
        VersionProveedorFormDTO version = captor.getValue().getVersion();
        assertThat(version.getRazonSocial()).isEqualTo("Malterías del Sur S.A.");
        assertThat(version.getNombreComercial()).isEqualTo("Malterías del Sur");
        assertThat(version.getCuit()).isEqualTo("30-71234567-8");
        assertThat(version.getTelefono()).isEqualTo("3584123456");
        assertThat(version.getEmail()).isEqualTo("ventas@malteriasdelsur.com");
        assertThat(version.getDireccion()).isEqualTo("Av. Italia 1234");
        assertThat(version.getIdLocalidad()).isEqualTo(9L);
        assertThat(version.getCatalogoProveedor()).hasSize(2);
        assertThat(version.getCatalogoProveedor().get(0).getIdInsumo()).isEqualTo(11L);
        assertThat(version.getCatalogoProveedor().get(0).getIdPresentacionComercial()).isEqualTo(5L);
        assertThat(version.getCatalogoProveedor().get(1).getIdInsumo()).isEqualTo(11L);
        assertThat(version.getCatalogoProveedor().get(1).getIdPresentacionComercial()).isEqualTo(6L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-14: sin datos obligatorios ni catálogo vuelve al formulario con cada mensaje, sin llamar al service")
    void caso_cp_pvc_14() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf())
                        .param("version.razonSocial", "  ").param("version.nombreComercial", "")
                        .param("version.cuit", "").param("version.telefono", "").param("version.email", " ")
                        .param("version.direccion", "").param("version.idLocalidad", ""))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo crear el proveedor")))
                .andExpect(content().string(containsString("La razón social es obligatoria")))
                .andExpect(content().string(containsString("El nombre comercial es obligatorio")))
                .andExpect(content().string(containsString("El CUIT es obligatorio")))
                .andExpect(content().string(containsString("El correo electrónico es obligatorio")))
                .andExpect(content().string(containsString("La dirección es obligatoria")))
                .andExpect(content().string(containsString("La localidad es obligatoria")))
                .andExpect(content().string(containsString("El catálogo debe tener al menos un ítem")))
                .andExpect(content().string(not(containsString("El teléfono"))))
                .andExpect(content().string(matchesPattern("(?s).*data-selector-buscar data-selector-texto[^>]*border-rose-400.*")))
                .andExpect(content().string(matchesPattern("(?s).*class=\"border rounded-lg overflow-hidden border-rose-400\".*")));

        verify(proveedorServicio, never()).altaProveedor(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-15: cada ítem del catálogo sin insumo o sin presentación comercial muestra su mensaje y resalta su fila (todas las filas con error, no las válidas), sin llamar al service")
    void caso_cp_pvc_15() throws Exception {
        // Arrange: la fila 0 sin insumo, la fila 1 sin presentación y la fila 2 válida
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("version.catalogoProveedor[0].idInsumo", "");
        datos.set("version.catalogoProveedor[1].idPresentacionComercial", "");
        datos.add("version.catalogoProveedor[2].idInsumo", "11");
        datos.add("version.catalogoProveedor[2].idPresentacionComercial", "5");
        cargarCatalogoResuelto();

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El insumo de cada ítem del catálogo es obligatorio")))
                .andExpect(content().string(containsString("La presentación comercial de cada ítem del catálogo es obligatoria")))
                .andReturn().getResponse().getContentAsString();

        // Assert: las dos filas con error quedan resaltadas; la fila válida y la plantilla, no
        String filaResaltada = "<tr data-fila data-prefijo=\"version.catalogoProveedor\" class=\"bg-rose-50\">";
        String filaSinResaltar = "<tr data-fila data-prefijo=\"version.catalogoProveedor\">";
        assertThat(html.split(Pattern.quote(filaResaltada), -1)).hasSize(3);
        assertThat(html.split(Pattern.quote(filaSinResaltar), -1)).hasSize(3);
        assertThat(html.indexOf(filaResaltada)).isLessThan(html.indexOf("name=\"version.catalogoProveedor[0].idInsumo\""));
        assertThat(html.lastIndexOf(filaResaltada)).isLessThan(html.indexOf("name=\"version.catalogoProveedor[1].idInsumo\""));
        assertThat(html.indexOf(filaSinResaltar)).isGreaterThan(html.indexOf("name=\"version.catalogoProveedor[1].idInsumo\""));
        verify(proveedorServicio, never()).altaProveedor(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-41: índices del catálogo con un hueco ([0] y [2]) enlazan la posición faltante como un ítem vacío: vuelve al formulario con sus dos mensajes y esa fila resaltada, sin llamar al service")
    void caso_cp_pvc_41() throws Exception {
        // Arrange: sin la fila [1]
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.remove("version.catalogoProveedor[1].idInsumo");
        datos.remove("version.catalogoProveedor[1].idPresentacionComercial");
        datos.add("version.catalogoProveedor[2].idInsumo", "11");
        datos.add("version.catalogoProveedor[2].idPresentacionComercial", "6");
        cargarCatalogoResuelto();

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo crear el proveedor")))
                .andExpect(content().string(containsString("El insumo de cada ítem del catálogo es obligatorio")))
                .andExpect(content().string(containsString("La presentación comercial de cada ítem del catálogo es obligatoria")))
                .andReturn().getResponse().getContentAsString();

        // Assert: tres filas renumeradas sin hueco; la del medio, vacía y resaltada
        assertThat(html).contains("name=\"version.catalogoProveedor[0].idInsumo\" value=\"11\"")
                .contains("name=\"version.catalogoProveedor[1].idInsumo\" value=\"\"")
                .contains("name=\"version.catalogoProveedor[1].idPresentacionComercial\" value=\"\"")
                .contains("name=\"version.catalogoProveedor[2].idPresentacionComercial\" value=\"6\"")
                .contains(">Insumo no disponible<", ">Presentación no disponible<");
        String filaResaltada = "<tr data-fila data-prefijo=\"version.catalogoProveedor\" class=\"bg-rose-50\">";
        assertThat(html.split(Pattern.quote(filaResaltada), -1)).hasSize(2);
        assertThat(html.indexOf(filaResaltada)).isGreaterThan(html.indexOf("name=\"version.catalogoProveedor[0].idInsumo\""))
                .isLessThan(html.indexOf("name=\"version.catalogoProveedor[1].idInsumo\""));
        verify(proveedorServicio, never()).altaProveedor(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-16:un CUIT duplicado vuelve al formulario con el mensaje del service junto al campo CUIT")
    void caso_cp_pvc_16() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        when(proveedorServicio.altaProveedor(any())).thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CUIT, "cuit"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("Ya existe un proveedor con el CUIT &#39;30712345678&#39;")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cuit\"[^>]*border-rose-400.*")))
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*"))))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cuit\"[^>]*value=\"30-71234567-8\".*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-17: un correo electrónico duplicado vuelve al formulario con el mensaje del service junto al campo correo")
    void caso_cp_pvc_17() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        when(proveedorServicio.altaProveedor(any())).thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CORREO, "email"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe un proveedor con el correo electrónico &#39;ventas@malteriasdelsur.com&#39;")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*")))
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"cuit\"[^>]*border-rose-400.*"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-40: un duplicado sin campo indicado se muestra como error general, sin marcar el CUIT ni el correo aunque el mensaje mencione el CUIT")
    void caso_cp_pvc_40() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        when(proveedorServicio.altaProveedor(any())).thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CUIT));
        when(proveedorServicio.modificarProveedor(eq(4L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un registro con los mismos datos"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo crear el proveedor")))
                .andExpect(content().string(containsString("Ya existe un proveedor con el CUIT &#39;30712345678&#39;")))
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"cuit\"[^>]*border-rose-400.*"))))
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*"))));
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el proveedor")))
                .andExpect(content().string(containsString("Ya existe un registro con los mismos datos")))
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"cuit\"[^>]*border-rose-400.*"))))
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-18: una regla de negocio rechazada por el service se muestra como error general y conserva lo tipeado, la localidad y las filas del catálogo")
    void caso_cp_pvc_18() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        when(proveedorServicio.altaProveedor(any())).thenThrow(new ReglaNegocioException("El CUIT debe tener 11 dígitos"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo crear el proveedor")))
                .andExpect(content().string(containsString("El CUIT debe tener 11 dígitos")))
                .andExpect(content().string(containsString("value=\"" + RAZON_SOCIAL_ESCAPADA + "\"")))
                .andExpect(content().string(containsString("value=\"ventas@malteriasdelsur.com\"")))
                .andExpect(content().string(containsString("value=\"Río Cuarto (Córdoba)\"")))
                .andExpect(content().string(matchesPattern("(?s).*name=\"version.idLocalidad\" value=\"9\".*")))
                // Las dos filas del catálogo vuelven con sus ids, el insumo con su tipo y la presentación con su medida
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[0].idInsumo\" value=\"11\"")))
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[0].idPresentacionComercial\" value=\"5\"")))
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[1].idInsumo\" value=\"11\"")))
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[1].idPresentacionComercial\" value=\"6\"")))
                .andExpect(content().string(containsString(">Pilsen<")))
                .andExpect(content().string(containsString(">Malta<")))
                .andExpect(content().string(containsString(">Bolsa de 25 Kg<")))
                .andExpect(content().string(containsString(">25 kg<")))
                .andExpect(content().string(containsString(">Bolsa de 1.000 g<")))
                .andExpect(content().string(containsString(">1.000 g<")))
                .andExpect(content().string(matchesPattern("(?s).*<tr data-fila-vacia hidden=\"hidden\">.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-19: si falta un insumo o la localidad el service lo informa como error general y la fila muestra el dato como no disponible")
    void caso_cp_pvc_19() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(insumoServicio.buscarPorId(11L)).thenThrow(new RecursoNoEncontradoException("No se encontró el insumo con ID: 11"));
        when(presentacionComercialServicio.buscarPorId(5L)).thenThrow(new RecursoNoEncontradoException("No se encontró la presentación comercial con ID: 5"));
        when(presentacionComercialServicio.buscarPorId(6L)).thenReturn(presentacion(6L, "Bolsa de 1.000 g", 1000.0, UnidadDeMedida.GRAMO));
        when(proveedorServicio.altaProveedor(any())).thenThrow(new RecursoNoEncontradoException("No se encontró el insumo con ID: 11"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo crear el proveedor")))
                .andExpect(content().string(containsString("No se encontró el insumo con ID: 11")))
                .andExpect(content().string(containsString(">Insumo no disponible<")))
                .andExpect(content().string(containsString(">Presentación no disponible<")))
                .andExpect(content().string(containsString(">Bolsa de 1.000 g<")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PVC-20: un rol distinto de GERENTE_DE_COMPRAS no puede dar de alta")
    void caso_cp_pvc_20() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(proveedorServicio, never()).altaProveedor(any());
    }

    // ==================== GET /proveedores/{id}/editar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-21: el formulario de edición precarga los datos vigentes, la localidad y el catálogo, avisa la nueva versión y apunta a la modificación")
    void caso_cp_pvc_21() throws Exception {
        // Arrange
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor(4L, "3584123456"));
        cargarCatalogoResuelto();

        // Act + Assert
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("Editar proveedor")))
                .andExpect(content().string(containsString(NOTA_VERSION)))
                .andExpect(content().string(containsString("action=\"/proveedores/4\"")))
                .andExpect(content().string(containsString("value=\"" + RAZON_SOCIAL_ESCAPADA + "\"")))
                .andExpect(content().string(containsString("value=\"Malter&iacute;as del Sur\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"cuit\"[^>]*value=\"30-71234567-8\".*")))
                .andExpect(content().string(containsString("value=\"3584123456\"")))
                .andExpect(content().string(containsString("value=\"ventas@malteriasdelsur.com\"")))
                .andExpect(content().string(containsString("value=\"Av. Italia 1234\"")))
                .andExpect(content().string(containsString("value=\"Río Cuarto (Córdoba)\"")))
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[0].idInsumo\" value=\"11\"")))
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[1].idPresentacionComercial\" value=\"6\"")))
                .andExpect(content().string(containsString(">25 kg<")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-43: un insumo y una presentación con nombres de 100 caracteres sin espacios se parten dentro de su celda del catálogo, sin ensanchar la tabla")
    void caso_cp_pvc_43() throws Exception {
        // Arrange
        String nombreInsumo = "ZZ-PV-MaltaLarga" + "X".repeat(84);
        String nombrePresentacion = "ZZ-PV-BolsaLarga" + "Y".repeat(84);
        MaltaResponseDTO malta = MaltaResponseDTO.builder().id(11L).nombre(nombreInsumo).build();
        PresentacionComercialResponseDTO bolsa = presentacion(5L, nombrePresentacion, 25.0, UnidadDeMedida.KILOGRAMO);
        when(insumoServicio.buscarPorId(11L)).thenReturn(malta);
        when(presentacionComercialServicio.buscarPorId(5L)).thenReturn(bolsa);
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(proveedorServicio.buscarPorId(4L)).thenReturn(ProveedorResponseDTO.builder().id(4L).version(
                VersionProveedorResponseDTO.builder().razonSocial("Malterías del Sur S.A.").cuit("30712345678").localidad(localidad())
                        .catalogoProveedor(List.of(CatalogoProveedorResponseDTO.builder().insumo(malta).presentacionComercial(bolsa).build()))
                        .build()).build());

        // Act + Assert: table-fixed reparte el ancho entre las columnas y la de "Quitar" tiene ancho fijo (w-32)
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<table class=\"[^\"]*\\btable-fixed\\b[^\"]*\" data-tabla-catalogo>.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-tabla-catalogo>.*<th class=\"[^\"]*\\bw-32\\b[^\"]*\"></th>.*")))
                // Los nombres completos (sin truncar) se parten dentro de su celda con break-words
                .andExpect(content().string(matchesPattern("(?s).*<span class=\"[^\"]*\\bbreak-words\\b[^\"]*\" data-celda=\"insumo\">" + nombreInsumo + "</span>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<span class=\"[^\"]*\\bbreak-words\\b[^\"]*\" data-celda=\"presentacion\">" + nombrePresentacion + "</span>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-22: un proveedor inexistente redirige al origen con el motivo")
    void caso_cp_pvc_22() throws Exception {
        // Arrange
        when(proveedorServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el proveedor con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/99/editar").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró el proveedor con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PVC-23: un rol distinto de GERENTE_DE_COMPRAS no puede abrir el formulario de edición")
    void caso_cp_pvc_23() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(proveedorServicio, never()).buscarPorId(any());
    }

    // ==================== POST /proveedores/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-24: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_pvc_24() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Proveedor modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ProveedorFormDTO> captor = ArgumentCaptor.forClass(ProveedorFormDTO.class);
        verify(proveedorServicio).modificarProveedor(eq(4L), captor.capture());
        assertThat(captor.getValue().getVersion().getCatalogoProveedor()).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-25: modificar sin datos obligatorios vuelve al formulario de edición con los errores, sin llamar al service")
    void caso_cp_pvc_25() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).param("version.razonSocial", "  "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/proveedores/4\"")))
                .andExpect(content().string(containsString("No se pudo modificar el proveedor")))
                .andExpect(content().string(containsString("La razón social es obligatoria")))
                .andExpect(content().string(containsString("El catálogo debe tener al menos un ítem")))
                .andExpect(content().string(containsString(NOTA_VERSION)));

        verify(proveedorServicio, never()).modificarProveedor(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-26: modificar con un correo electrónico duplicado vuelve al formulario de edición con el mensaje junto al campo correo")
    void caso_cp_pvc_26() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        when(proveedorServicio.modificarProveedor(eq(4L), any())).thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CORREO, "email"));

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el proveedor")))
                .andExpect(content().string(containsString("Ya existe un proveedor con el correo electrónico")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*")))
                .andExpect(content().string(containsString("name=\"version.catalogoProveedor[1].idPresentacionComercial\" value=\"6\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-46: en el alta, un correo electrónico con formato inválido (incluido un dominio sin punto) vuelve al formulario con el mensaje junto al campo correo, conserva lo tipeado y no llega al service")
    void caso_cp_pvc_46() throws Exception {
        // Arrange
        cargarCatalogoResuelto();

        for (String email : new String[]{"no-es-correo", "a@b", "a b@c.com"}) {
            LinkedMultiValueMap<String, String> datos = datosValidos();
            datos.set("version.email", email);

            // Act + Assert
            mockMvc.perform(post(URL).with(csrf()).params(datos))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VISTA_FORMULARIO))
                    .andExpect(model().attributeHasFieldErrorCode("proveedorForm", "version.email", "Pattern"))
                    .andExpect(content().string(containsString("No se pudo crear el proveedor")))
                    .andExpect(content().string(containsString("El correo electrónico no tiene un formato válido")))
                    .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*")))
                    .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*value=\"" + Pattern.quote(email) + "\".*")));
        }

        verify(proveedorServicio, never()).altaProveedor(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-47: al modificar, un correo electrónico con formato inválido vuelve al formulario de edición con el mensaje junto al campo correo, conserva lo tipeado y no llega al service")
    void caso_cp_pvc_47() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("version.email", "ventas@malteriasdelsur");

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(model().attributeHasFieldErrorCode("proveedorForm", "version.email", "Pattern"))
                .andExpect(content().string(containsString("No se pudo modificar el proveedor")))
                .andExpect(content().string(containsString("El correo electrónico no tiene un formato válido")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*value=\"ventas@malteriasdelsur\".*")));

        verify(proveedorServicio, never()).modificarProveedor(any(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-27: si el service rechaza la modificación por una regla de negocio o porque falta un insumo, el motivo se muestra en el formulario de edición")
    void caso_cp_pvc_27() throws Exception {
        // Arrange
        cargarCatalogoResuelto();
        String simultaneo = "El proveedor fue modificado por otra persona al mismo tiempo, intente nuevamente";
        when(proveedorServicio.modificarProveedor(eq(4L), any()))
                .thenThrow(new ReglaNegocioException(simultaneo))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la presentación comercial con ID: 6"));
        when(proveedorServicio.buscarPorId(4L)).thenReturn(proveedor(4L, "3584123456"));

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/proveedores/4\"")))
                .andExpect(content().string(containsString(simultaneo)))
                .andExpect(content().string(containsString("value=\"" + RAZON_SOCIAL_ESCAPADA + "\"")));
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el proveedor")))
                .andExpect(content().string(containsString("No se encontró la presentación comercial con ID: 6")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-28: modificar un proveedor inexistente lo maneja el ControllerAdvices")
    void caso_cp_pvc_28() throws Exception {
        // Arrange
        String mensaje = "No se encontró el proveedor con ID: 99";
        when(proveedorServicio.modificarProveedor(eq(99L), any())).thenThrow(new RecursoNoEncontradoException(mensaje));
        when(proveedorServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException(mensaje));

        // Act + Assert
        mockMvc.perform(post(URL + "/99").with(csrf()).header("Referer", URL).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PVC-29: un rol distinto de GERENTE_DE_COMPRAS no puede modificar")
    void caso_cp_pvc_29() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(proveedorServicio, never()).modificarProveedor(any(), any());
    }

    // ==================== POST /proveedores/{id}/baja ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-30: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_pvc_30() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Proveedor dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(proveedorServicio).bajaProveedor(4L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-31: si el service rechaza la baja (orden de compra pendiente) vuelve al origen con la alerta de advertencia")
    void caso_cp_pvc_31() throws Exception {
        // Arrange
        String mensaje = "No se puede dar de baja el proveedor porque tiene al menos una orden de compra en estado Pendiente asociada";
        doThrow(new ReglaNegocioException(mensaje)).when(proveedorServicio).bajaProveedor(4L);

        // Act + Assert
        mockMvc.perform(post(URL + "/4/baja").with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PVC-32: un rol distinto de GERENTE_DE_COMPRAS no puede dar de baja")
    void caso_cp_pvc_32() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(proveedorServicio, never()).bajaProveedor(any());
    }

    // ==================== GET /proveedores/buscador/insumos ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-33: el buscador de insumos filtra por nombre y tipo, pide páginas de 8 en orden alfabético y muestra el tipo de cada insumo")
    void caso_cp_pvc_33() throws Exception {
        // Arrange
        when(insumoServicio.filtrarInsumos(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build(),
                LupuloResponseDTO.builder().id(21L).nombre("Cascade").build(),
                LevaduraResponseDTO.builder().id(31L).nombre("US-05").build()), PageRequest.of(0, 8), 3));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/insumos").param("nombre", " pil ").param("tipo", "LUPULO"))
                .andExpect(status().isOk())
                .andExpect(view().name("proveedor/buscador-insumo :: buscador"))
                .andExpect(content().string(containsString("Buscar insumo")))
                .andExpect(content().string(containsString("<table class=\"tabla-buscador\">")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pilsen\"")))
                .andExpect(content().string(containsString(">Todos<")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"LUPULO\" selected=\"selected\">Lúpulo</option>.*")))
                .andExpect(content().string(containsString("<option value=\"MALTA\">Malta</option>")))
                .andExpect(content().string(containsString("<option value=\"LEVADURA\">Levadura</option>")))
                .andExpect(content().string(matchesPattern("(?s).*data-id=\"11\" data-texto=\"Pilsen\" data-tipo=\"Malta\".*")))
                .andExpect(content().string(matchesPattern("(?s).*data-id=\"21\" data-texto=\"Cascade\" data-tipo=\"Lúpulo\".*")))
                .andExpect(content().string(matchesPattern("(?s).*data-id=\"31\" data-texto=\"US-05\" data-tipo=\"Levadura\".*")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(insumoServicio).filtrarInsumos(eq("pil"), eq(TipoInsumo.LUPULO), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList()).containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-34: el buscador de insumos conserva los filtros al paginar, avisa cuando no hay resultados y responde con un texto ante un tipo inválido")
    void caso_cp_pvc_34() throws Exception {
        // Arrange
        when(insumoServicio.filtrarInsumos(eq("pil"), eq(TipoInsumo.MALTA), any())).thenReturn(new PageImpl<>(List.<InsumoResponseDTO>of(
                MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build()), PageRequest.of(1, 8), 20));
        when(insumoServicio.filtrarInsumos(eq("nada"), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/insumos").param("nombre", "pil").param("tipo", "MALTA").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(URL + "/buscador/insumos?pagina=0&amp;nombre=pil&amp;tipo=MALTA")))
                .andExpect(content().string(containsString(URL + "/buscador/insumos?pagina=2&amp;nombre=pil&amp;tipo=MALTA")))
                .andExpect(content().string(containsString("data-modal-get")));
        mockMvc.perform(get(URL + "/buscador/insumos").param("nombre", "nada"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron insumos con los criterios indicados")));
        mockMvc.perform(get(URL + "/buscador/insumos").param("tipo", "AGUA"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El valor indicado no es válido"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-44: en el buscador de insumos solo el nombre se puede partir; los encabezados y el tipo no se cortan a mitad de palabra")
    void caso_cp_pvc_44() throws Exception {
        // Arrange
        String nombreLargo = "ZZ-PV-MaltaLarga" + "X".repeat(84);
        when(insumoServicio.filtrarInsumos(any(), any(), any())).thenReturn(new PageImpl<>(List.<InsumoResponseDTO>of(
                MaltaResponseDTO.builder().id(11L).nombre(nombreLargo).build()), PageRequest.of(0, 8), 1));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/insumos"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">Nombre</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">Tipo</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">Malta</td>.*")))
                // La celda del nombre conserva el corte libre de tabla-buscador
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"(?![^\"]*whitespace-nowrap)[^\"]*\">" + nombreLargo + "</td>.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-id=\"11\" data-texto=\"" + nombreLargo + "\".*")));
    }

    // ==================== GET /proveedores/buscador/presentaciones ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-35: el buscador de presentaciones filtra por nombre, cantidad y unidad y muestra la cantidad sin \".0\" y la unidad con su símbolo")
    void caso_cp_pvc_35() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO),
                presentacion(7L, "Sobre de 2,5 g", 2.5, UnidadDeMedida.GRAMO)), PageRequest.of(0, 8), 2));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/presentaciones").param("nombre", " bolsa ").param("cantidad", "25").param("unidadDeMedida", "KILOGRAMO"))
                .andExpect(status().isOk())
                .andExpect(view().name("proveedor/buscador-presentacion :: buscador"))
                .andExpect(content().string(containsString("Buscar presentación comercial")))
                .andExpect(content().string(containsString("<table class=\"tabla-buscador\">")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Bolsa de 25 Kg\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 25\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"buscador-cantidad\"[^>]*value=\"25\".*")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"KILOGRAMO\" selected=\"selected\">Kilogramo \\(kg\\)</option>.*")))
                .andExpect(content().string(containsString(">kg<")))
                .andExpect(content().string(containsString(">g<")))
                .andExpect(content().string(containsString(">2,5<")))
                .andExpect(content().string(not(containsString("25.0"))))
                .andExpect(content().string(matchesPattern("(?s).*data-id=\"5\" data-texto=\"Bolsa de 25 Kg\"\\s+data-cantidad=\"25\" data-unidad=\"kg\".*")))
                .andExpect(content().string(matchesPattern("(?s).*data-id=\"7\" data-texto=\"Sobre de 2,5 g\"\\s+data-cantidad=\"2,5\" data-unidad=\"g\".*")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(presentacionComercialServicio).filtrarPresentacionesComerciales(eq("bolsa"), eq(25.0), eq(UnidadDeMedida.KILOGRAMO), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList()).containsExactly("nombre", "id");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-36: el buscador de presentaciones conserva los filtros al paginar y avisa cuando no hay resultados")
    void caso_cp_pvc_36() throws Exception {
        // Arrange
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(eq("bolsa"), eq(25.0), eq(UnidadDeMedida.KILOGRAMO), any()))
                .thenReturn(new PageImpl<>(List.of(presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO)), PageRequest.of(1, 8), 20));
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(eq("nada"), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/presentaciones").param("nombre", "bolsa").param("cantidad", "25")
                        .param("unidadDeMedida", "KILOGRAMO").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(URL + "/buscador/presentaciones?pagina=0&amp;nombre=bolsa&amp;cantidad=25&amp;unidadDeMedida=KILOGRAMO")))
                .andExpect(content().string(containsString(URL + "/buscador/presentaciones?pagina=2&amp;nombre=bolsa&amp;cantidad=25&amp;unidadDeMedida=KILOGRAMO")));
        mockMvc.perform(get(URL + "/buscador/presentaciones").param("nombre", "nada"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron presentaciones comerciales con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-45: en el buscador de presentaciones solo el nombre se puede partir; los encabezados, la cantidad y la unidad no se cortan a mitad de palabra")
    void caso_cp_pvc_45() throws Exception {
        // Arrange
        String nombreLargo = "ZZ-PV-BolsaLarga" + "Y".repeat(84);
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                presentacion(5L, nombreLargo, 1234.5, UnidadDeMedida.KILOGRAMO)), PageRequest.of(0, 8), 1));

        // Act + Assert
        mockMvc.perform(get(URL + "/buscador/presentaciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">Nombre</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">Cantidad</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">Unidad</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">1234,5</td>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"[^\"]*\\bwhitespace-nowrap\\b[^\"]*\">kg</td>.*")))
                // La celda del nombre conserva el corte libre de tabla-buscador
                .andExpect(content().string(matchesPattern("(?s).*<td class=\"(?![^\"]*whitespace-nowrap)[^\"]*\">" + nombreLargo + "</td>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-42: la cantidad que envía el buscador de presentaciones (data-cantidad) con su unidad es exactamente la medida que el servidor pinta en la fila del catálogo, también con miles y decimales")
    void caso_cp_pvc_42() throws Exception {
        // Arrange
        List<PresentacionComercialResponseDTO> presentaciones = List.of(
                presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO),
                presentacion(6L, "Bolsa de 1.000 g", 1000.0, UnidadDeMedida.GRAMO),
                presentacion(7L, "Sobre de 2,5 g", 2.5, UnidadDeMedida.GRAMO),
                presentacion(8L, "Big bag", 1234.5, UnidadDeMedida.KILOGRAMO));
        when(presentacionComercialServicio.filtrarPresentacionesComerciales(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(presentaciones, PageRequest.of(0, 8), 4));
        presentaciones.forEach(p -> when(presentacionComercialServicio.buscarPorId(p.getId())).thenReturn(p));
        MaltaResponseDTO pilsen = MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build();
        when(insumoServicio.buscarPorId(11L)).thenReturn(pilsen);
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(proveedorServicio.buscarPorId(4L)).thenReturn(ProveedorResponseDTO.builder().id(4L).version(
                VersionProveedorResponseDTO.builder().razonSocial("Malterías del Sur S.A.").cuit("30712345678").localidad(localidad())
                        .catalogoProveedor(presentaciones.stream().map(p -> CatalogoProveedorResponseDTO.builder()
                                .insumo(pilsen).presentacionComercial(p).build()).toList()).build()).build());

        // Act
        String buscador = mockMvc.perform(get(URL + "/buscador/presentaciones"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String formulario = mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert: la medida que arma /js/proveedor-form.js (data-cantidad + " " + data-unidad) figura en la fila del servidor
        Matcher elegir = Pattern.compile("data-cantidad=\"([^\"]*)\" data-unidad=\"([^\"]*)\"").matcher(buscador);
        List<String> medidas = new ArrayList<>();
        while (elegir.find()) {
            medidas.add(elegir.group(1) + " " + elegir.group(2));
        }
        assertThat(medidas).containsExactly("25 kg", "1.000 g", "2,5 g", "1234,5 kg");
        assertThat(medidas).allSatisfy(medida -> assertThat(formulario).contains("data-celda=\"medidaPresentacion\">" + medida + "</span>"));
    }

    // ==================== Sidebar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-37: la sidebar del GERENTE_DE_COMPRAS ofrece \"Gestión de Proveedores\" en Administración, después de \"Gestión de Presentaciones\"")
    void caso_cp_pvc_37() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Administración.*<span>Gestión de Presentaciones</span>.*href=\"/proveedores\"[^>]*>\\s*<i class=\"bi text-lg bi-truck\"></i>\\s*<span>Gestión de Proveedores</span>.*")))
                // En el panel de control la opción no está resaltada
                .andExpect(content().string(not(matchesPattern(ENLACE_RESALTADO))))
                // "Órdenes de Compra" sigue como "Próximamente"
                .andExpect(content().string(matchesPattern("(?s).*Órdenes de Compra</span>\\s*<span[^>]*>Próximamente</span>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PVC-38: la opción permanece resaltada en el formulario de alta")
    void caso_cp_pvc_38() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-PVC-39: la sidebar de los demás roles no muestra \"Gestión de Proveedores\"")
    void caso_cp_pvc_39(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/proveedores\""))))
                .andExpect(content().string(not(containsString("Gestión de Proveedores"))));
    }

    // ==================== Datos de prueba ====================

    /**
     * Datos válidos del formulario: dos ítems del catálogo con el mismo insumo en distintas presentaciones.
     */
    private static LinkedMultiValueMap<String, String> datosValidos() {
        LinkedMultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("version.razonSocial", "Malterías del Sur S.A.");
        datos.add("version.nombreComercial", "Malterías del Sur");
        datos.add("version.cuit", "30-71234567-8");
        datos.add("version.telefono", "3584123456");
        datos.add("version.email", "ventas@malteriasdelsur.com");
        datos.add("version.direccion", "Av. Italia 1234");
        datos.add("version.idLocalidad", "9");
        datos.add("version.catalogoProveedor[0].idInsumo", "11");
        datos.add("version.catalogoProveedor[0].idPresentacionComercial", "5");
        datos.add("version.catalogoProveedor[1].idInsumo", "11");
        datos.add("version.catalogoProveedor[1].idPresentacionComercial", "6");
        return datos;
    }

    /**
     * La localidad, el insumo y las presentaciones que referencian los datos válidos, para mostrar sus nombres al
     * volver a renderizar el formulario.
     */
    private void cargarCatalogoResuelto() {
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(insumoServicio.buscarPorId(11L)).thenReturn(MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build());
        when(presentacionComercialServicio.buscarPorId(5L)).thenReturn(presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO));
        when(presentacionComercialServicio.buscarPorId(6L)).thenReturn(presentacion(6L, "Bolsa de 1.000 g", 1000.0, UnidadDeMedida.GRAMO));
    }

    private static LocalidadResponseDTO localidad() {
        return LocalidadResponseDTO.builder().id(9L).nombre("Río Cuarto").codigoPostal("5800")
                .provincia(ProvinciaResponseDTO.builder().id(2L).nombre("Córdoba").build()).build();
    }

    private static PresentacionComercialResponseDTO presentacion(Long id, String nombre, Double cantidad, UnidadDeMedida unidad) {
        return PresentacionComercialResponseDTO.builder().id(id).nombre(nombre).cantidad(cantidad).unidadDeMedida(unidad).build();
    }

    private static ProveedorResponseDTO proveedor(Long id, String telefono) {
        MaltaResponseDTO pilsen = MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build();
        VersionProveedorResponseDTO version = VersionProveedorResponseDTO.builder()
                .id(40L).razonSocial("Malterías del Sur S.A.").nombreComercial("Malterías del Sur")
                .cuit("30712345678").telefono(telefono).email("ventas@malteriasdelsur.com").direccion("Av. Italia 1234")
                .esUltimaVersion(true).localidad(localidad())
                .catalogoProveedor(List.of(
                        CatalogoProveedorResponseDTO.builder().id(100L).insumo(pilsen)
                                .presentacionComercial(presentacion(5L, "Bolsa de 25 Kg", 25.0, UnidadDeMedida.KILOGRAMO)).build(),
                        CatalogoProveedorResponseDTO.builder().id(101L).insumo(pilsen)
                                .presentacionComercial(presentacion(6L, "Bolsa de 1.000 g", 1000.0, UnidadDeMedida.GRAMO)).build()))
                .build();
        return ProveedorResponseDTO.builder().id(id).version(version).build();
    }

    private static PageImpl<ProveedorResponseDTO> paginaVacia() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }
}
