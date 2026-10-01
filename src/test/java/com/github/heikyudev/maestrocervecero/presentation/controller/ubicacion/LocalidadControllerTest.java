package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.LocalidadFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
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

@WebMvcTest(LocalidadController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class LocalidadControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILocalidadServicio localidadServicio;

    @MockitoBean
    private IProvinciaServicio provinciaServicio;

    @MockitoBean
    private IPaisServicio paisServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /ubicaciones/localidades ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-01: el ADMINISTRADOR ve el listado con código postal, provincia y país, el alta y las acciones de editar y dar de baja")
    void listado_debeMostrarLasLocalidadesParaAdministrador() throws Exception {
        when(localidadServicio.filtrarLocalidades(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(localidad(8L, "Río Cuarto", "5800")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/ubicaciones/localidades"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/localidad-lista"))
                .andExpect(content().string(containsString("Río Cuarto")))
                .andExpect(content().string(containsString("5800")))
                .andExpect(content().string(containsString("Córdoba")))
                .andExpect(content().string(containsString("Argentina")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/localidades/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/localidades/8/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/ubicaciones/localidades/8/baja\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(localidadServicio.filtrarLocalidades(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/localidades")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(localidadServicio).filtrarLocalidades(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-03: los cuatro criterios llegan al service y los selectores muestran la provincia y el país elegidos")
    void listado_debeFiltrarPorLosCuatroCriterios() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba"));
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));
        when(localidadServicio.filtrarLocalidades(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/localidades")
                        .param("nombre", "rio").param("codigoPostal", "5800")
                        .param("idProvincia", "5").param("idPais", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"5\"")))
                .andExpect(content().string(containsString("value=\"Córdoba (Argentina)\"")))
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")));

        verify(localidadServicio).filtrarLocalidades(eq("rio"), eq("5800"), eq(5L), eq(3L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-04: si la provincia o el país del filtro ya no existen, esos filtros se ignoran")
    void listado_debeIgnorarProvinciaYPaisInexistentes() throws Exception {
        when(provinciaServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe la provincia"));
        when(paisServicio.buscarPorId(98L)).thenThrow(new RecursoNoEncontradoException("No existe el país"));
        when(localidadServicio.filtrarLocalidades(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/localidades").param("idProvincia", "99").param("idPais", "98"))
                .andExpect(status().isOk());

        verify(localidadServicio).filtrarLocalidades(isNull(), isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-LOC-05: un gerente no tiene acceso al módulo (el listado se rechaza y no llega al service)")
    void listado_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/ubicaciones/localidades"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(localidadServicio, never()).filtrarLocalidades(any(), any(), any(), any(), any());
    }

    // ==================== GET /ubicaciones/localidades/buscador ====================

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-LOC-06: el GERENTE_COMERCIAL puede buscar la localidad de un cliente: páginas de 8 y un botón para elegir cada una")
    void buscador_debeEstarDisponibleParaGerenteComercial() throws Exception {
        when(localidadServicio.filtrarLocalidades(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(localidad(8L, "Río Cuarto", "5800")), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/ubicaciones/localidades/buscador").param("nombre", "rio"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/buscador-localidad :: buscador"))
                .andExpect(content().string(containsString("data-elegir data-id=\"8\" data-texto=\"Río Cuarto (Córdoba)\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/provincias/buscador\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/paises/buscador\"")))
                .andExpect(content().string(not(containsString("data-baja-url"))))
                .andExpect(content().string(not(containsString("data-selector-alta"))));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(localidadServicio).filtrarLocalidades(eq("rio"), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-LOC-07: el GERENTE_DE_COMPRAS puede buscar la localidad de un proveedor")
    void buscador_debeEstarDisponibleParaGerenteDeCompras() throws Exception {
        when(localidadServicio.filtrarLocalidades(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        mockMvc.perform(get("/ubicaciones/localidades/buscador"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron localidades con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "ENCARGADO_DE_DEPOSITO")
    @DisplayName("CP-LOC-08: un rol sin relación con clientes ni proveedores no puede usar el buscador")
    void buscador_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/ubicaciones/localidades/buscador"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(localidadServicio, never()).filtrarLocalidades(any(), any(), any(), any(), any());
    }

    // ==================== GET /ubicaciones/localidades/nuevo ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-09: el formulario de alta ofrece el selector de provincia (clic en el campo) con el menú de tres puntitos \"Crear nueva provincia\", y asteriscos de obligatorio")
    void formularioAlta_debeOfrecerElSelectorDeProvinciaConAltaAlVuelo() throws Exception {
        mockMvc.perform(get("/ubicaciones/localidades/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/localidad-form"))
                .andExpect(content().string(containsString("Nueva localidad")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/ubicaciones/localidades\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/provincias/buscador\"")))
                .andExpect(content().string(containsString("data-alta-url=\"/ubicaciones/provincias/nuevo\"")))
                .andExpect(content().string(containsString("data-selector-buscar data-selector-texto")))
                .andExpect(content().string(containsString("placeholder=\"Seleccionar provincia\"")))
                .andExpect(content().string(containsString("bi-chevron-down")))
                .andExpect(content().string(containsString("data-selector-menu")))
                .andExpect(content().string(containsString("Crear nueva provincia")))
                .andExpect(content().string(containsString("Volver a localidades")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-10: abierto desde otro formulario (retorno y campo) conserva ambos en campos ocultos y cancelar vuelve a ese formulario")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/ubicaciones/localidades/nuevo")
                        .param("retorno", "/clientes/nuevo").param("campo", "idLocalidad"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/clientes/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idLocalidad\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/clientes/nuevo?desdeAlta=1\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-26: al volver de crear una provincia (idProvincia en la URL) el selector la muestra elegida")
    void formularioAlta_debePreseleccionarLaProvinciaRecienCreada() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba"));

        mockMvc.perform(get("/ubicaciones/localidades/nuevo").param("idProvincia", "5").param("desdeAlta", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"5\"")))
                .andExpect(content().string(containsString("value=\"Córdoba (Argentina)\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-LOC-11: un gerente no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/ubicaciones/localidades/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /ubicaciones/localidades ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-12: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(localidadServicio.altaLocalidad(any())).thenReturn(localidad(8L, "Río Cuarto", "5800"));

        mockMvc.perform(post("/ubicaciones/localidades").with(csrf())
                        .param("nombre", "Río Cuarto").param("codigoPostal", "5800").param("idProvincia", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/localidades"))
                .andExpect(flash().attribute("mensaje", "Localidad creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<LocalidadFormDTO> captor = ArgumentCaptor.forClass(LocalidadFormDTO.class);
        verify(localidadServicio).altaLocalidad(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Río Cuarto");
        assertThat(captor.getValue().getCodigoPostal()).isEqualTo("5800");
        assertThat(captor.getValue().getIdProvincia()).isEqualTo(5L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-13: sin nombre, código postal ni provincia responde 422 con los tres errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/ubicaciones/localidades").with(csrf()).param("nombre", " "))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/localidad-form"))
                .andExpect(content().string(containsString("No se pudo crear la localidad")))
                .andExpect(content().string(containsString("El nombre de la localidad es obligatorio")))
                .andExpect(content().string(containsString("El código postal es obligatorio")))
                .andExpect(content().string(containsString("La provincia es obligatoria")));

        verify(localidadServicio, never()).altaLocalidad(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-14: al volver con errores el selector conserva la provincia elegida")
    void alta_debeConservarLaProvinciaElegidaAlVolverConErrores() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba"));

        mockMvc.perform(post("/ubicaciones/localidades").with(csrf())
                        .param("nombre", "").param("codigoPostal", "5800").param("idProvincia", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"5\"")))
                .andExpect(content().string(containsString("value=\"Córdoba (Argentina)\"")))
                .andExpect(content().string(containsString("value=\"5800\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-15: un nombre duplicado en la provincia responde 422 con el mensaje del service")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba"));
        when(localidadServicio.altaLocalidad(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una localidad con ese nombre en la provincia"));

        mockMvc.perform(post("/ubicaciones/localidades").with(csrf())
                        .param("nombre", "Río Cuarto").param("codigoPostal", "5800").param("idProvincia", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe una localidad con ese nombre en la provincia")))
                // th:field escapa las tildes como entidades HTML
                .andExpect(content().string(containsString("value=\"R&iacute;o Cuarto\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-16: si la provincia elegida ya no existe, el error se muestra en el campo provincia")
    void alta_debeMostrarElErrorDeProvinciaInexistente() throws Exception {
        when(provinciaServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe la provincia indicada"));
        when(localidadServicio.altaLocalidad(any())).thenThrow(new RecursoNoEncontradoException("No existe la provincia indicada"));

        mockMvc.perform(post("/ubicaciones/localidades").with(csrf())
                        .param("nombre", "Río Cuarto").param("codigoPostal", "5800").param("idProvincia", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No existe la provincia indicada")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-17: el alta contextual vuelve al formulario de origen con la localidad creada en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigenConLaLocalidadCreada() throws Exception {
        when(localidadServicio.altaLocalidad(any())).thenReturn(localidad(8L, "Río Cuarto", "5800"));

        mockMvc.perform(post("/ubicaciones/localidades").with(csrf())
                        .param("nombre", "Río Cuarto").param("codigoPostal", "5800").param("idProvincia", "5")
                        .param("retorno", "/clientes/nuevo").param("campo", "idLocalidad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clientes/nuevo?desdeAlta=1&idLocalidad=8"))
                .andExpect(flash().attribute("mensaje", "Localidad creada correctamente"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-LOC-18: un gerente no puede dar de alta una localidad")
    void alta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/localidades").with(csrf())
                        .param("nombre", "Río Cuarto").param("codigoPostal", "5800").param("idProvincia", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(localidadServicio, never()).altaLocalidad(any());
    }

    // ==================== GET /ubicaciones/localidades/{id}/editar ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-19: el formulario de edición muestra nombre, código postal y provincia actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(localidadServicio.buscarPorId(8L)).thenReturn(localidad(8L, "Río Cuarto", "5800"));
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba"));

        mockMvc.perform(get("/ubicaciones/localidades/8/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/localidad-form"))
                .andExpect(content().string(containsString("Editar localidad")))
                .andExpect(content().string(containsString("value=\"R&iacute;o Cuarto\"")))
                .andExpect(content().string(containsString("value=\"5800\"")))
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"5\"")))
                .andExpect(content().string(containsString("value=\"Córdoba (Argentina)\"")))
                .andExpect(content().string(containsString("action=\"/ubicaciones/localidades/8\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-27: al editar y volver de crear una provincia, la provincia de la URL reemplaza a la actual")
    void formularioEdicion_debeReemplazarLaProvinciaPorLaRecienCreada() throws Exception {
        when(localidadServicio.buscarPorId(8L)).thenReturn(localidad(8L, "Río Cuarto", "5800"));
        when(provinciaServicio.buscarPorId(9L)).thenReturn(provincia(9L, "Santa Fe"));

        mockMvc.perform(get("/ubicaciones/localidades/8/editar").param("idProvincia", "9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idProvincia\" value=\"9\"")))
                .andExpect(content().string(containsString("value=\"Santa Fe (Argentina)\"")));
    }

    // ==================== POST /ubicaciones/localidades/{id} ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-20: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/ubicaciones/localidades/8").with(csrf())
                        .param("nombre", "Villa María").param("codigoPostal", "5900").param("idProvincia", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/localidades"))
                .andExpect(flash().attribute("mensaje", "Localidad modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(localidadServicio).modificarLocalidad(eq(8L), any(LocalidadFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-21: modificar con un nombre duplicado responde 422 con el formulario de edición y el mensaje")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba"));
        when(localidadServicio.modificarLocalidad(eq(8L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una localidad con ese nombre en la provincia"));

        mockMvc.perform(post("/ubicaciones/localidades/8").with(csrf())
                        .param("nombre", "Villa María").param("codigoPostal", "5900").param("idProvincia", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar la localidad")))
                .andExpect(content().string(containsString("Ya existe una localidad con ese nombre en la provincia")))
                .andExpect(content().string(containsString("action=\"/ubicaciones/localidades/8\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-LOC-22: un gerente no puede modificar una localidad")
    void modificar_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/localidades/8").with(csrf())
                        .param("nombre", "Villa María").param("codigoPostal", "5900").param("idProvincia", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(localidadServicio, never()).modificarLocalidad(any(), any());
    }

    // ==================== POST /ubicaciones/localidades/{id}/baja ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-23: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/ubicaciones/localidades/8/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/localidades"))
                .andExpect(flash().attribute("mensaje", "Localidad dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(localidadServicio).bajaLocalidad(8L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-LOC-24: si el service rechaza la baja (asociada a clientes o proveedores) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("La localidad está asociada a un cliente activo")).when(localidadServicio).bajaLocalidad(8L);

        mockMvc.perform(post("/ubicaciones/localidades/8/baja").with(csrf()).header("Referer", "/ubicaciones/localidades"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/localidades"))
                .andExpect(flash().attribute("mensaje", "La localidad está asociada a un cliente activo"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-LOC-25: un gerente no puede dar de baja una localidad")
    void baja_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/localidades/8/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(localidadServicio, never()).bajaLocalidad(any());
    }

    private static PaisResponseDTO pais(Long id, String nombre) {
        return PaisResponseDTO.builder().id(id).nombre(nombre).build();
    }

    private static ProvinciaResponseDTO provincia(Long id, String nombre) {
        return ProvinciaResponseDTO.builder().id(id).nombre(nombre).pais(pais(3L, "Argentina")).build();
    }

    private static LocalidadResponseDTO localidad(Long id, String nombre, String codigoPostal) {
        return LocalidadResponseDTO.builder().id(id).nombre(nombre).codigoPostal(codigoPostal)
                .provincia(provincia(5L, "Córdoba")).build();
    }
}
