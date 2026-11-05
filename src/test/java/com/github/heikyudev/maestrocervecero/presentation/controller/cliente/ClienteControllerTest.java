package com.github.heikyudev.maestrocervecero.presentation.controller.cliente;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.login.HomeController;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.ORDEN_ALFABETICO;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({ClienteController.class, HomeController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class ClienteControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";
    private static final String URL = "/clientes";
    private static final String VISTA_FORMULARIO = "cliente/cliente-form";
    private static final String ENLACE_RESALTADO =
            "(?s).*href=\"/clientes\"[^>]*bg-amber-500/10[^>]*>\\s*<i class=\"bi text-lg bi-shop\"></i>\\s*<span>Gestión de Clientes</span>.*";
    private static final String MENSAJE_DUPLICADO_CORREO = "Ya existe un cliente con el correo electrónico 'contacto@elpuerto.com'";
    private static final String MENSAJE_BAJA_RECHAZADA = "No se puede dar de baja el cliente porque tiene un barril en su poder";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IClienteServicio clienteServicio;

    @MockitoBean
    private ILocalidadServicio localidadServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /clientes ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-01: el ENCARGADO_DE_DISTRIBUCION ve el listado con nombre, correo, teléfono, ubicación, dirección y las acciones por fila")
    void caso_cp_clc_01() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cliente(4L, "3764123456")), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente/cliente-lista"))
                .andExpect(content().string(containsString("<title>Clientes - Maestro Cervecero</title>")))
                .andExpect(content().string(containsString("Gestión de Clientes")))
                .andExpect(content().string(containsString("Clientes en orden alfabético por nombre.")))
                .andExpect(content().string(matchesPattern("(?s).*<th class=\"[^\"]*\">Nombre</th>.*<th class=\"[^\"]*\">Correo electrónico</th>.*<th class=\"[^\"]*\">Teléfono</th>.*<th class=\"[^\"]*\">Ubicación</th>.*<th class=\"[^\"]*\">Dirección</th>.*<th class=\"col-acciones[^\"]*\">Acciones</th>.*")))
                .andExpect(content().string(containsString(">Bar El Puerto<")))
                .andExpect(content().string(containsString(">contacto@elpuerto.com<")))
                .andExpect(content().string(containsString(">3764123456<")))
                .andExpect(content().string(containsString(">Posadas<")))
                .andExpect(content().string(containsString(">Av. Corrientes 1234<")))
                .andExpect(content().string(containsString("href=\"/clientes/nuevo\"")))
                .andExpect(content().string(containsString("Nuevo cliente")))
                .andExpect(content().string(containsString("href=\"/clientes/4/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/clientes/4/baja\"")))
                .andExpect(content().string(containsString("data-baja-nombre=\"Bar El Puerto\"")))
                .andExpect(content().string(containsString("Si tiene un barril en su poder, el sistema no lo permitirá.")))
                // Sin pantalla de detalle: la tabla ya muestra todos los datos
                .andExpect(content().string(not(containsString("bi-eye"))))
                .andExpect(content().string(not(containsString("href=\"/clientes/4\""))))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")))
                // La sidebar resalta "Gestión de Clientes"
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-02: sin filtros pide páginas de 20 en orden alfabético por nombre, con los criterios en null")
    void caso_cp_clc_02() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL)).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteServicio).filtrarClientes(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort()).isEqualTo(ORDEN_ALFABETICO);
        verifyNoInteractions(localidadServicio);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-03: los criterios de texto llegan recortados al service y un texto en blanco equivale a no filtrar")
    void caso_cp_clc_03() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act
        mockMvc.perform(get(URL).param("nombre", " puerto ").param("direccion", " corrientes "))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL).param("nombre", "   ").param("direccion", "  "))
                .andExpect(status().isOk());

        // Assert
        verify(clienteServicio).filtrarClientes(eq("puerto"), eq("corrientes"), isNull(), any());
        verify(clienteServicio).filtrarClientes(isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-04: el filtro por localidad usa los selectores enlazados País, Provincia y Localidad precargados con la ubicación elegida; una localidad inexistente no filtra")
    void caso_cp_clc_04() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any())).thenReturn(paginaVacia());
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(localidadServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la localidad con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL).param("idLocalidad", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-ubicacion-enlazada")))
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"2\"")))
                .andExpect(content().string(containsString("name=\"idLocalidad\" value=\"9\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")))
                .andExpect(content().string(containsString("value=\"Misiones\"")))
                .andExpect(content().string(containsString("value=\"Posadas\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/paises/buscador\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/provincias/buscador\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/localidades/buscador\"")))
                .andExpect(content().string(not(containsString("<select data-ubicacion"))));
        mockMvc.perform(get(URL).param("idLocalidad", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("value=\"99\""))));

        verify(clienteServicio).filtrarClientes(isNull(), isNull(), eq(9L), any());
        verify(clienteServicio).filtrarClientes(isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-05: los criterios quedan en el formulario con sus placeholders y en los links de paginación, y Limpiar vuelve al listado sin criterios")
    void caso_cp_clc_05() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(clienteServicio.filtrarClientes(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cliente(4L, "3764123456")), PageRequest.of(1, 20), 60));

        // Act
        mockMvc.perform(get(URL).param("nombre", "puerto").param("direccion", "corrientes").param("idLocalidad", "9").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*value=\"puerto\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Bar El Puerto\"")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"direccion\"[^>]*value=\"corrientes\".*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Av. Corrientes 1234\"")))
                .andExpect(content().string(containsString(URL + "?pagina=0&amp;nombre=puerto&amp;direccion=corrientes&amp;idLocalidad=9")))
                .andExpect(content().string(containsString(URL + "?pagina=2&amp;nombre=puerto&amp;direccion=corrientes&amp;idLocalidad=9")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/clientes\"[^>]*>\\s*Limpiar.*")));

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteServicio).filtrarClientes(eq("puerto"), eq("corrientes"), eq(9L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-06: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void caso_cp_clc_06() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any())).thenReturn(paginaVacia());

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron clientes con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-07: un valor del filtro o de la página que no se puede convertir al tipo del parámetro no es un error del sistema")
    void caso_cp_clc_07() throws Exception {
        // Act + Assert
        for (String[] parametro : new String[][]{{"idLocalidad", "abc"}, {"pagina", "abc"}}) {
            mockMvc.perform(get(URL + "?" + parametro[0] + "=" + parametro[1]).header("Referer", URL))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(URL))
                    .andExpect(flash().attribute("mensaje", "El valor indicado no es válido"))
                    .andExpect(flash().attribute("tipo", "warning"));
        }

        verify(clienteServicio, never()).filtrarClientes(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-08: el teléfono vacío se muestra con un guion y los textos largos se truncan con el texto completo en el tooltip")
    void caso_cp_clc_08() throws Exception {
        // Arrange
        String largo = "A".repeat(1000);
        ClienteResponseDTO sinTelefono = cliente(4L, null);
        ClienteResponseDTO conTextosLargos = ClienteResponseDTO.builder().id(5L).nombre(largo).email(largo).direccion(largo)
                .localidad(localidad()).build();
        when(clienteServicio.filtrarClientes(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sinTelefono, conTextosLargos), PageRequest.of(0, 20), 2));

        // Act
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*whitespace-nowrap\">-</td>.*")))
                .andReturn().getResponse().getContentAsString();
        String tabla = html.substring(html.indexOf("<table"), html.indexOf("</table>"));

        // Assert: nombre, correo y dirección acotados con el texto completo en el tooltip
        Matcher acotadas = Pattern.compile("<span class=\"inline-block max-w-\\[(?:9|12)rem\\] truncate[^\"]*\"[^>]*data-tooltip=\"" + largo + "\"").matcher(tabla);
        int cantidadAcotadas = 0;
        while (acotadas.find()) {
            cantidadAcotadas++;
        }
        assertThat(cantidadAcotadas).isEqualTo(3);
        assertThat(tabla).doesNotContain("px-4 py-3").contains("<th class=\"col-acciones px-2 py-3 text-right whitespace-nowrap\">Acciones</th>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR"})
    @DisplayName("CP-CLC-09: un rol distinto de ENCARGADO_DE_DISTRIBUCION no accede al listado y no llega a ningún service")
    void caso_cp_clc_09(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL).with(user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No tiene permisos para realizar esta acción"))
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(clienteServicio, localidadServicio);
    }

    @Test
    @DisplayName("CP-CLC-10: un usuario sin sesión es enviado al login en cada operación y no llega a ningún service")
    void caso_cp_clc_10() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/nuevo")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get(URL + "/4/editar")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(post(URL + "/4/baja").with(csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));

        verifyNoInteractions(clienteServicio, localidadServicio);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-11: el listado muestra la localidad y, debajo, la provincia y el país de cada cliente")
    void caso_cp_clc_11() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cliente(4L, "3764123456")), PageRequest.of(0, 20), 1));

        // Act + Assert
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Posadas<")))
                .andExpect(content().string(containsString(">Misiones, Argentina<")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-12: todos los botones de la fila (modificar y baja) van en la columna Acciones, a la derecha, y la tabla no tiene enlaces de texto")
    void caso_cp_clc_12() throws Exception {
        // Arrange
        when(clienteServicio.filtrarClientes(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cliente(4L, "3764123456")), PageRequest.of(0, 20), 1));

        // Act
        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String cuerpo = html.substring(html.indexOf("<tbody"), html.indexOf("</tbody>"));
        String celdaAcciones = cuerpo.substring(cuerpo.indexOf("<td class=\"col-acciones"));

        // Assert
        assertThat(cuerpo.indexOf("href=\"/clientes/4/editar\"")).isGreaterThan(cuerpo.indexOf("<td class=\"col-acciones"));
        assertThat(cuerpo.indexOf("data-baja-url")).isGreaterThan(cuerpo.indexOf("<td class=\"col-acciones"));
        assertThat(celdaAcciones.indexOf("bi-pencil-square")).isLessThan(celdaAcciones.indexOf("bi-trash"));
        assertThat(cuerpo.substring(0, cuerpo.indexOf("<td class=\"col-acciones"))).doesNotContain("<a ").doesNotContain("<button");
        assertThat(celdaAcciones).contains("justify-end");
    }

    // ==================== GET /clientes/nuevo ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-13: el formulario de alta muestra los campos por tema con asterisco, placeholders y ayudas, y los selectores enlazados País, Provincia y Localidad")
    void caso_cp_clc_13() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("Nuevo cliente")))
                .andExpect(content().string(containsString("action=\"/clientes\"")))
                // Un doble clic en el botón de envío no registra el cliente dos veces (modales.js)
                .andExpect(content().string(matchesPattern("(?s).*<form id=\"cliente-form\"[^>]*method=\"post\" novalidate data-un-solo-envio[^>]*>.*")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Bar El Puerto\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: contacto@elpuerto.com\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 3764123456\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Av. Corrientes 1234\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione país\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione provincia\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione localidad\"")))
                .andExpect(content().string(containsString("Opcional. Solo dígitos, hasta 15.</p>")))
                .andExpect(content().string(containsString("Único entre los clientes, sin distinguir mayúsculas. Hasta 255 caracteres.</p>")))
                .andExpect(content().string(containsString("Hasta 100 caracteres.</p>")))
                .andExpect(content().string(containsString("Hasta 255 caracteres.</p>")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/paises/buscador\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/provincias/buscador\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/localidades/buscador\"")))
                .andExpect(content().string(containsString("name=\"idLocalidad\"")))
                .andExpect(content().string(containsString("/js/ubicacion-enlazada.js")))
                // Solo el administrador da de alta ubicaciones: los selectores de ubicación no ofrecen alta al vuelo
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(containsString("Crear cliente")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/clientes\"[^>]*>\\s*<i[^>]*></i>\\s*<span>Volver a clientes.*")))
                .andExpect(content().string(matchesPattern("(?s).*href=\"/clientes\"[^>]*>Cancelar</a>.*")))
                .andExpect(content().string(not(containsString("No se pudo crear"))))
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)))
                .andReturn().getResponse().getContentAsString();

        // Assert: asterisco rojo en nombre, correo, país, provincia, localidad y dirección; el teléfono es opcional
        assertThat(html.split(Pattern.quote(OBLIGATORIO), -1)).hasSize(7);
        // Orden por tema: nombre; correo y teléfono; ubicación; dirección debajo
        int nombre = html.indexOf("for=\"nombre\"");
        int correo = html.indexOf("for=\"email\"");
        int telefono = html.indexOf("for=\"telefono\"");
        int ubicacion = html.indexOf("data-ubicacion-enlazada");
        int direccion = html.indexOf("for=\"direccion\"");
        assertThat(nombre).isPositive().isLessThan(correo);
        assertThat(correo).isLessThan(telefono);
        assertThat(telefono).isLessThan(ubicacion);
        assertThat(ubicacion).isLessThan(direccion);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-CLC-14: un rol distinto de ENCARGADO_DE_DISTRIBUCION no puede abrir el formulario de alta")
    void caso_cp_clc_14() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /clientes ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-15: alta correcta delega en el service con los datos del formulario (sin País ni Provincia) y redirige al listado con alerta de éxito")
    void caso_cp_clc_15() throws Exception {
        // Arrange
        when(clienteServicio.altaCliente(any())).thenReturn(cliente(4L, "3764123456"));
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.add("idPais", "3");
        datos.add("idProvincia", "2");

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Cliente creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ClienteFormDTO> captor = ArgumentCaptor.forClass(ClienteFormDTO.class);
        verify(clienteServicio).altaCliente(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Bar El Puerto");
        assertThat(captor.getValue().getTelefono()).isEqualTo("3764123456");
        assertThat(captor.getValue().getEmail()).isEqualTo("contacto@elpuerto.com");
        assertThat(captor.getValue().getDireccion()).isEqualTo("Av. Corrientes 1234");
        assertThat(captor.getValue().getIdLocalidad()).isEqualTo(9L);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-16: el teléfono es opcional: un alta sin teléfono delega en el service")
    void caso_cp_clc_16() throws Exception {
        // Arrange
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("telefono", "");

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("tipo", "success"));

        verify(clienteServicio).altaCliente(any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-17: sin localidad vuelve al formulario con el mensaje junto a la localidad, sin llamar al service")
    void caso_cp_clc_17() throws Exception {
        // Arrange
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set("idLocalidad", "");

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datos))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(model().attributeHasFieldErrorCode("clienteForm", "idLocalidad", "obligatorio"))
                .andExpect(content().string(containsString("No se pudo crear el cliente")))
                .andExpect(content().string(containsString("La localidad es obligatoria")))
                .andExpect(content().string(matchesPattern("(?s).*data-selector-buscar data-selector-texto[^>]*border-rose-400.*")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*value=\"Bar El Puerto\".*")));

        verify(clienteServicio, never()).altaCliente(any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-18: un dato obligatorio vacío lo rechaza el service con su mensaje, que se muestra en el resumen del formulario sin perder lo tipeado ni la ubicación")
    void caso_cp_clc_18() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(clienteServicio.altaCliente(any()))
                .thenThrow(new ReglaNegocioException("El nombre es obligatorio"))
                .thenThrow(new ReglaNegocioException("El correo electrónico es obligatorio"))
                .thenThrow(new ReglaNegocioException("La dirección es obligatoria"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("nombre", " ")))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo crear el cliente")))
                .andExpect(content().string(containsString("El nombre es obligatorio")))
                .andExpect(content().string(containsString("value=\"contacto@elpuerto.com\"")))
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"2\"")))
                .andExpect(content().string(containsString("name=\"idLocalidad\" value=\"9\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")))
                .andExpect(content().string(containsString("value=\"Misiones\"")))
                .andExpect(content().string(containsString("value=\"Posadas\"")));
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("email", "")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El correo electrónico es obligatorio")));
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("direccion", "")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La dirección es obligatoria")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-19: un teléfono con letras lo rechaza el service con su mensaje, que se muestra en el formulario conservando lo tipeado")
    void caso_cp_clc_19() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(clienteServicio.altaCliente(any())).thenThrow(new ReglaNegocioException("El teléfono solo puede contener dígitos"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosCon("telefono", "37641abc")))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("No se pudo crear el cliente")))
                .andExpect(content().string(containsString("El teléfono solo puede contener dígitos")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"telefono\"[^>]*value=\"37641abc\".*")))
                .andExpect(content().string(containsString("value=\"Bar El Puerto\"")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-20: un correo electrónico duplicado vuelve al formulario con el mensaje del service junto al campo correo, también si el service no indica el campo")
    void caso_cp_clc_20() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(clienteServicio.altaCliente(any()))
                .thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CORREO))
                .thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CORREO, "email"));

        // Act + Assert
        for (int intento = 0; intento < 2; intento++) {
            mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VISTA_FORMULARIO))
                    .andExpect(model().attributeHasFieldErrorCode("clienteForm", "email", "duplicado"))
                    .andExpect(content().string(containsString("No se pudo crear el cliente")))
                    .andExpect(content().string(containsString("Ya existe un cliente con el correo electrónico &#39;contacto@elpuerto.com&#39;")))
                    .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*")))
                    .andExpect(content().string(not(matchesPattern("(?s).*id=\"nombre\"[^>]*border-rose-400.*"))));
        }
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-21: un correo electrónico con formato inválido vuelve al formulario con el mensaje junto al campo correo, conserva lo tipeado y no llega al service")
    void caso_cp_clc_21() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());

        for (String email : new String[]{"no-es-correo", "a@b", "a b@c.com"}) {
            // Act + Assert
            mockMvc.perform(post(URL).with(csrf()).params(datosCon("email", email)))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VISTA_FORMULARIO))
                    .andExpect(model().attributeHasFieldErrorCode("clienteForm", "email", "Pattern"))
                    .andExpect(content().string(containsString("No se pudo crear el cliente")))
                    .andExpect(content().string(containsString("El correo electrónico no tiene un formato válido")))
                    .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*")))
                    .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*value=\"" + Pattern.quote(email) + "\".*")));
        }

        verify(clienteServicio, never()).altaCliente(any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-22: una localidad que ya no existe vuelve al formulario con el motivo junto a la localidad")
    void caso_cp_clc_22() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenThrow(new RecursoNoEncontradoException("No se encontró la localidad con ID: 9"));
        when(clienteServicio.altaCliente(any())).thenThrow(new RecursoNoEncontradoException("No se encontró la localidad con ID: 9"));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(model().attributeHasFieldErrorCode("clienteForm", "idLocalidad", "noEncontrado"))
                .andExpect(content().string(containsString("No se encontró la localidad con ID: 9")))
                .andExpect(content().string(matchesPattern("(?s).*data-selector-buscar data-selector-texto[^>]*border-rose-400.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CLC-23: un rol distinto de ENCARGADO_DE_DISTRIBUCION no puede dar de alta")
    void caso_cp_clc_23() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(clienteServicio, never()).altaCliente(any());
    }

    // ==================== GET /clientes/{id}/editar ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-24: el formulario de edición precarga los datos actuales y la ubicación, y apunta a la modificación")
    void caso_cp_clc_24() throws Exception {
        // Arrange
        when(clienteServicio.buscarPorId(4L)).thenReturn(cliente(4L, "3764123456"));
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());

        // Act + Assert
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(content().string(containsString("Editar cliente")))
                .andExpect(content().string(containsString("action=\"/clientes/4\"")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"nombre\"[^>]*value=\"Bar El Puerto\".*")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*value=\"contacto@elpuerto.com\".*")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"telefono\"[^>]*value=\"3764123456\".*")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"direccion\"[^>]*value=\"Av. Corrientes 1234\".*")))
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"2\"")))
                .andExpect(content().string(containsString("name=\"idLocalidad\" value=\"9\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")))
                .andExpect(content().string(containsString("value=\"Misiones\"")))
                .andExpect(content().string(containsString("value=\"Posadas\"")))
                .andExpect(content().string(matchesPattern(ENLACE_RESALTADO)));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-25: un cliente sin teléfono se edita con el campo vacío")
    void caso_cp_clc_25() throws Exception {
        // Arrange
        when(clienteServicio.buscarPorId(4L)).thenReturn(cliente(4L, null));
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());

        // Act + Assert
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*id=\"telefono\"[^>]*value=\"\".*")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-26: un cliente inexistente redirige al origen con el motivo")
    void caso_cp_clc_26() throws Exception {
        // Arrange
        when(clienteServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el cliente con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/99/editar").header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró el cliente con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CLC-27: un rol distinto de ENCARGADO_DE_DISTRIBUCION no puede abrir el formulario de edición")
    void caso_cp_clc_27() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/4/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(clienteServicio, never()).buscarPorId(any());
    }

    // ==================== POST /clientes/{id} ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-28: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_clc_28() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Cliente modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ClienteFormDTO> captor = ArgumentCaptor.forClass(ClienteFormDTO.class);
        verify(clienteServicio).modificarCliente(eq(4L), captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Bar El Puerto");
        assertThat(captor.getValue().getIdLocalidad()).isEqualTo(9L);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-29: modificar sin localidad vuelve al formulario de edición con el mensaje junto a la localidad, sin llamar al service")
    void caso_cp_clc_29() throws Exception {
        // Arrange
        when(clienteServicio.buscarPorId(4L)).thenReturn(cliente(4L, "3764123456"));

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosCon("idLocalidad", "")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/clientes/4\"")))
                .andExpect(content().string(containsString("No se pudo modificar el cliente")))
                .andExpect(content().string(containsString("La localidad es obligatoria")));

        verify(clienteServicio, never()).modificarCliente(any(), any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-30: modificar con un correo electrónico duplicado vuelve al formulario de edición con el mensaje junto al campo correo")
    void caso_cp_clc_30() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(clienteServicio.modificarCliente(eq(4L), any())).thenThrow(new RecursoDuplicadoException(MENSAJE_DUPLICADO_CORREO));

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/clientes/4\"")))
                .andExpect(content().string(containsString("No se pudo modificar el cliente")))
                .andExpect(content().string(containsString("Ya existe un cliente con el correo electrónico")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*border-rose-400.*")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-31: al modificar, un correo electrónico con formato inválido vuelve al formulario de edición con el mensaje junto al campo correo y no llega al service")
    void caso_cp_clc_31() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosCon("email", "contacto@elpuerto")))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_FORMULARIO))
                .andExpect(model().attributeHasFieldErrorCode("clienteForm", "email", "Pattern"))
                .andExpect(content().string(containsString("No se pudo modificar el cliente")))
                .andExpect(content().string(containsString("El correo electrónico no tiene un formato válido")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"email\"[^>]*value=\"contacto@elpuerto\".*")));

        verify(clienteServicio, never()).modificarCliente(any(), any());
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-32: si el service rechaza la modificación por una regla de negocio o porque falta la localidad, el motivo se muestra en el formulario de edición")
    void caso_cp_clc_32() throws Exception {
        // Arrange
        when(localidadServicio.buscarPorId(9L)).thenReturn(localidad());
        when(clienteServicio.buscarPorId(4L)).thenReturn(cliente(4L, "3764123456"));
        when(clienteServicio.modificarCliente(eq(4L), any()))
                .thenThrow(new ReglaNegocioException("El teléfono solo puede contener dígitos"))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la localidad con ID: 9"));

        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosCon("telefono", "abc")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el cliente")))
                .andExpect(content().string(containsString("El teléfono solo puede contener dígitos")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"telefono\"[^>]*value=\"abc\".*")));
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("clienteForm", "idLocalidad", "noEncontrado"))
                .andExpect(content().string(containsString("No se encontró la localidad con ID: 9")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-33: modificar un cliente inexistente lo maneja el ControllerAdvices")
    void caso_cp_clc_33() throws Exception {
        // Arrange
        String mensaje = "No se encontró el cliente con ID: 99";
        when(clienteServicio.modificarCliente(eq(99L), any())).thenThrow(new RecursoNoEncontradoException(mensaje));
        when(clienteServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException(mensaje));

        // Act + Assert
        mockMvc.perform(post(URL + "/99").with(csrf()).header("Referer", URL).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", mensaje))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CLC-34: un rol distinto de ENCARGADO_DE_DISTRIBUCION no puede modificar")
    void caso_cp_clc_34() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4").with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(clienteServicio, never()).modificarCliente(any(), any());
    }

    // ==================== POST /clientes/{id}/baja ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-35: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void caso_cp_clc_35() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/4/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Cliente dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(clienteServicio).bajaCliente(4L);
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-36: si el service rechaza la baja (barril en poder del cliente) vuelve al origen con la alerta de advertencia")
    void caso_cp_clc_36() throws Exception {
        // Arrange
        doThrow(new ReglaNegocioException(MENSAJE_BAJA_RECHAZADA)).when(clienteServicio).bajaCliente(4L);

        // Act + Assert
        mockMvc.perform(post(URL + "/4/baja").with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", MENSAJE_BAJA_RECHAZADA))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-37: dar de baja un cliente inexistente vuelve al origen con el motivo")
    void caso_cp_clc_37() throws Exception {
        // Arrange
        doThrow(new RecursoNoEncontradoException("No se encontró el cliente con ID: 99")).when(clienteServicio).bajaCliente(99L);

        // Act + Assert
        mockMvc.perform(post(URL + "/99/baja").with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "No se encontró el cliente con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR"})
    @DisplayName("CP-CLC-38: un rol distinto de ENCARGADO_DE_DISTRIBUCION no puede dar de alta, modificar ni dar de baja y no llega a ningún service")
    void caso_cp_clc_38(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo").with(user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get(URL + "/4/editar").with(user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL).with(user("usuario").roles(rol)).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/4").with(user("usuario").roles(rol)).with(csrf()).params(datosValidos()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/4/baja").with(user("usuario").roles(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(clienteServicio, localidadServicio);
    }

    // ==================== Sidebar ====================

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DISTRIBUCION")
    @DisplayName("CP-CLC-39: la sidebar del ENCARGADO_DE_DISTRIBUCION ofrece solo \"Gestión de Clientes\" en Administración y no la muestra resaltada en el panel de control")
    void caso_cp_clc_39() throws Exception {
        // Act
        String html = mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Administración.*href=\"/clientes\"[^>]*>\\s*<i class=\"bi text-lg bi-shop\"></i>\\s*<span>Gestión de Clientes</span>.*")))
                .andExpect(content().string(not(matchesPattern(ENLACE_RESALTADO))))
                .andReturn().getResponse().getContentAsString();

        // Assert: ninguna otra opción de Administración, Operación, Configuración ni Sistema
        assertThat(html).doesNotContain("Gestión de Insumos").doesNotContain("Gestión de Recetas").doesNotContain("Gestión de Barriles")
                .doesNotContain("Gestión de Equipamientos").doesNotContain("Gestión de Proveedores").doesNotContain("Gestión de Presentaciones")
                .doesNotContain("Gestión de Ubicaciones").doesNotContain("Gestión de Usuarios")
                .doesNotContain(">Operación<").doesNotContain(">Configuración<").doesNotContain(">Sistema<");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR"})
    @DisplayName("CP-CLC-40: la sidebar de los demás roles no muestra \"Gestión de Clientes\"")
    void caso_cp_clc_40(String rol) throws Exception {
        // Act + Assert
        mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/clientes\""))))
                .andExpect(content().string(not(containsString("Gestión de Clientes"))));
    }

    // ==================== Datos de prueba ====================

    private static LinkedMultiValueMap<String, String> datosValidos() {
        LinkedMultiValueMap<String, String> datos = new LinkedMultiValueMap<>();
        datos.add("nombre", "Bar El Puerto");
        datos.add("telefono", "3764123456");
        datos.add("email", "contacto@elpuerto.com");
        datos.add("direccion", "Av. Corrientes 1234");
        datos.add("idLocalidad", "9");
        return datos;
    }

    private static LinkedMultiValueMap<String, String> datosCon(String campo, String valor) {
        LinkedMultiValueMap<String, String> datos = datosValidos();
        datos.set(campo, valor);
        return datos;
    }

    private static LocalidadResponseDTO localidad() {
        return LocalidadResponseDTO.builder().id(9L).nombre("Posadas").codigoPostal("3300")
                .provincia(ProvinciaResponseDTO.builder().id(2L).nombre("Misiones")
                        .pais(PaisResponseDTO.builder().id(3L).nombre("Argentina").build()).build()).build();
    }

    private static ClienteResponseDTO cliente(Long id, String telefono) {
        return ClienteResponseDTO.builder().id(id).nombre("Bar El Puerto").telefono(telefono)
                .email("contacto@elpuerto.com").direccion("Av. Corrientes 1234").localidad(localidad()).build();
    }

    private static PageImpl<ClienteResponseDTO> paginaVacia() {
        return new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
    }
}
