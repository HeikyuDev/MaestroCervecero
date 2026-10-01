package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.ProvinciaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IPaisServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
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

@WebMvcTest(ProvinciaController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class ProvinciaControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProvinciaServicio provinciaServicio;

    @MockitoBean
    private IPaisServicio paisServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /ubicaciones/provincias ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-01: el ADMINISTRADOR ve el listado con el país de cada provincia, el alta y las acciones de editar y dar de baja")
    void listado_debeMostrarLasProvinciasParaAdministrador() throws Exception {
        when(provinciaServicio.filtrarProvincias(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(provincia(5L, "Córdoba", 3L, "Argentina")), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/ubicaciones/provincias"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/provincia-lista"))
                .andExpect(content().string(containsString("Córdoba")))
                .andExpect(content().string(containsString("Argentina")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/provincias/nuevo\"")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/provincias/5/editar\"")))
                .andExpect(content().string(containsString("data-baja-url=\"/ubicaciones/provincias/5/baja\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(provinciaServicio.filtrarProvincias(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/provincias")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(provinciaServicio).filtrarProvincias(isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("nombre", "id");
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-03: filtrar por país envía su id al service y muestra el país elegido en el selector")
    void listado_debeFiltrarPorPaisYMostrarloEnElSelector() throws Exception {
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));
        when(provinciaServicio.filtrarProvincias(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/provincias").param("nombre", "cor").param("idPais", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")));

        verify(provinciaServicio).filtrarProvincias(eq("cor"), eq(3L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-04: si el país del filtro ya no existe, el filtro se ignora en vez de dejar el listado vacío")
    void listado_debeIgnorarUnPaisInexistente() throws Exception {
        when(paisServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe el país"));
        when(provinciaServicio.filtrarProvincias(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/ubicaciones/provincias").param("idPais", "99"))
                .andExpect(status().isOk());

        verify(provinciaServicio).filtrarProvincias(isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PRC-05: un gerente no tiene acceso al módulo (el listado se rechaza y no llega al service)")
    void listado_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/ubicaciones/provincias"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).filtrarProvincias(any(), any(), any());
    }

    // ==================== GET /ubicaciones/provincias/buscador ====================

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PRC-06: el GERENTE_COMERCIAL puede usar el buscador: páginas de 8 y un botón para elegir cada provincia")
    void buscador_debeEstarDisponibleParaGerenteComercial() throws Exception {
        when(provinciaServicio.filtrarProvincias(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(provincia(5L, "Córdoba", 3L, "Argentina")), PageRequest.of(0, 8), 1));

        mockMvc.perform(get("/ubicaciones/provincias/buscador").param("nombre", "cor"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/buscador-provincia :: buscador"))
                .andExpect(content().string(containsString("data-elegir data-id=\"5\" data-texto=\"Córdoba (Argentina)\"")))
                .andExpect(content().string(not(containsString("data-baja-url"))));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(provinciaServicio).filtrarProvincias(eq("cor"), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PRC-07: el GERENTE_DE_COMPRAS también puede usar el buscador")
    void buscador_debeEstarDisponibleParaGerenteDeCompras() throws Exception {
        when(provinciaServicio.filtrarProvincias(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        mockMvc.perform(get("/ubicaciones/provincias/buscador"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron provincias con los criterios indicados.")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PRC-08: un rol sin relación con clientes ni proveedores no puede usar el buscador")
    void buscador_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/ubicaciones/provincias/buscador"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).filtrarProvincias(any(), any(), any());
    }

    // ==================== GET /ubicaciones/provincias/nuevo ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-09: el formulario de alta ofrece el selector de país con el botón \"+\" para crearlo, y asteriscos de obligatorio")
    void formularioAlta_debeOfrecerElSelectorDePaisConAltaAlVuelo() throws Exception {
        mockMvc.perform(get("/ubicaciones/provincias/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/provincia-form"))
                .andExpect(content().string(containsString("Nueva provincia")))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("action=\"/ubicaciones/provincias\"")))
                .andExpect(content().string(containsString("data-buscador-url=\"/ubicaciones/paises/buscador\"")))
                .andExpect(content().string(containsString("data-alta-url=\"/ubicaciones/paises/nuevo\"")))
                .andExpect(content().string(containsString("data-selector-alta")))
                .andExpect(content().string(containsString("Volver a provincias")))
                .andExpect(content().string(not(containsString("name=\"retorno\""))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-10: abierto desde otro formulario (retorno y campo) conserva ambos en campos ocultos y cancelar vuelve a ese formulario")
    void formularioAlta_debeConservarElRetornoContextual() throws Exception {
        mockMvc.perform(get("/ubicaciones/provincias/nuevo")
                        .param("retorno", "/ubicaciones/localidades/nuevo").param("campo", "idProvincia"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"retorno\" value=\"/ubicaciones/localidades/nuevo\"")))
                .andExpect(content().string(containsString("name=\"campo\" value=\"idProvincia\"")))
                .andExpect(content().string(containsString("Volver al formulario anterior")))
                .andExpect(content().string(containsString("href=\"/ubicaciones/localidades/nuevo?desdeAlta=1\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-26: al volver de crear un país (idPais en la URL) el selector lo muestra elegido")
    void formularioAlta_debePreseleccionarElPaisRecienCreado() throws Exception {
        when(paisServicio.buscarPorId(7L)).thenReturn(pais(7L, "Chile"));

        mockMvc.perform(get("/ubicaciones/provincias/nuevo").param("idPais", "7").param("desdeAlta", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idPais\" value=\"7\"")))
                .andExpect(content().string(containsString("value=\"Chile\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PRC-11: un gerente no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/ubicaciones/provincias/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /ubicaciones/provincias ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-12: alta correcta delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        when(provinciaServicio.altaProvincia(any())).thenReturn(provincia(5L, "Córdoba", 3L, "Argentina"));

        mockMvc.perform(post("/ubicaciones/provincias").with(csrf()).param("nombre", "Córdoba").param("idPais", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/provincias"))
                .andExpect(flash().attribute("mensaje", "Provincia creada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ProvinciaFormDTO> captor = ArgumentCaptor.forClass(ProvinciaFormDTO.class);
        verify(provinciaServicio).altaProvincia(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Córdoba");
        assertThat(captor.getValue().getIdPais()).isEqualTo(3L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-13: sin nombre ni país responde 422 con ambos errores, sin llamar al service")
    void alta_debeVolverAlFormularioSiFaltanLosDatosObligatorios() throws Exception {
        mockMvc.perform(post("/ubicaciones/provincias").with(csrf()).param("nombre", "  "))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/provincia-form"))
                .andExpect(content().string(containsString("No se pudo crear la provincia")))
                .andExpect(content().string(containsString("El nombre de la provincia es obligatorio")))
                .andExpect(content().string(containsString("El país es obligatorio")));

        verify(provinciaServicio, never()).altaProvincia(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-14: al volver con errores el selector conserva el país elegido")
    void alta_debeConservarElPaisElegidoAlVolverConErrores() throws Exception {
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));

        mockMvc.perform(post("/ubicaciones/provincias").with(csrf()).param("nombre", "").param("idPais", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-15: un nombre duplicado responde 422 con el mensaje del service y conserva lo tipeado")
    void alta_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));
        when(provinciaServicio.altaProvincia(any())).thenThrow(new RecursoDuplicadoException("Ya existe una provincia con ese nombre"));

        mockMvc.perform(post("/ubicaciones/provincias").with(csrf()).param("nombre", "Córdoba").param("idPais", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya existe una provincia con ese nombre")))
                // th:field escapa las tildes como entidades HTML
                .andExpect(content().string(containsString("value=\"C&oacute;rdoba\"")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-16: si el país elegido ya no existe, el error se muestra en el campo país")
    void alta_debeMostrarElErrorDePaisInexistente() throws Exception {
        when(paisServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No existe el país indicado"));
        when(provinciaServicio.altaProvincia(any())).thenThrow(new RecursoNoEncontradoException("No existe el país indicado"));

        mockMvc.perform(post("/ubicaciones/provincias").with(csrf()).param("nombre", "Córdoba").param("idPais", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No existe el país indicado")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-17: el alta contextual vuelve al formulario de origen con la provincia creada en el campo indicado")
    void altaContextual_debeVolverAlFormularioDeOrigenConLaProvinciaCreada() throws Exception {
        when(provinciaServicio.altaProvincia(any())).thenReturn(provincia(5L, "Córdoba", 3L, "Argentina"));

        mockMvc.perform(post("/ubicaciones/provincias").with(csrf())
                        .param("nombre", "Córdoba").param("idPais", "3")
                        .param("retorno", "/ubicaciones/localidades/nuevo").param("campo", "idProvincia"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/localidades/nuevo?desdeAlta=1&idProvincia=5"))
                .andExpect(flash().attribute("mensaje", "Provincia creada correctamente"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-27: el alta contextual encadenada conserva el retorno original dentro de la URL de vuelta")
    void altaContextual_debeSoportarElEncadenamiento() throws Exception {
        when(provinciaServicio.altaProvincia(any())).thenReturn(provincia(5L, "Córdoba", 3L, "Argentina"));

        // El alta de provincia se abrió desde una localidad que a su vez tenía su propio retorno
        String retorno = "/ubicaciones/localidades/nuevo?retorno=%2Fx&campo=idLocalidad";
        mockMvc.perform(post("/ubicaciones/provincias").with(csrf())
                        .param("nombre", "Córdoba").param("idPais", "3")
                        .param("retorno", retorno).param("campo", "idProvincia"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/localidades/nuevo?retorno=%2Fx&campo=idLocalidad&desdeAlta=1&idProvincia=5"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PRC-18: un gerente no puede dar de alta una provincia")
    void alta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/provincias").with(csrf()).param("nombre", "Córdoba").param("idPais", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).altaProvincia(any());
    }

    // ==================== GET /ubicaciones/provincias/{id}/editar ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-19: el formulario de edición muestra nombre y país actuales y apunta a la modificación")
    void formularioEdicion_debeMostrarLosDatosActuales() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba", 3L, "Argentina"));
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));

        mockMvc.perform(get("/ubicaciones/provincias/5/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("ubicacion/provincia-form"))
                .andExpect(content().string(containsString("Editar provincia")))
                .andExpect(content().string(containsString("value=\"C&oacute;rdoba\"")))
                .andExpect(content().string(containsString("name=\"idPais\" value=\"3\"")))
                .andExpect(content().string(containsString("value=\"Argentina\"")))
                .andExpect(content().string(containsString("action=\"/ubicaciones/provincias/5\"")))
                .andExpect(content().string(containsString("Guardar cambios")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-28: al editar y volver de crear un país, el país de la URL reemplaza al actual")
    void formularioEdicion_debeReemplazarElPaisPorElRecienCreado() throws Exception {
        when(provinciaServicio.buscarPorId(5L)).thenReturn(provincia(5L, "Córdoba", 3L, "Argentina"));
        when(paisServicio.buscarPorId(7L)).thenReturn(pais(7L, "Chile"));

        mockMvc.perform(get("/ubicaciones/provincias/5/editar").param("idPais", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"idPais\" value=\"7\"")))
                .andExpect(content().string(containsString("value=\"Chile\"")));
    }

    // ==================== POST /ubicaciones/provincias/{id} ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-20: la modificación correcta delega en el service y redirige al listado con alerta de éxito")
    void modificar_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/ubicaciones/provincias/5").with(csrf()).param("nombre", "Santa Fe").param("idPais", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/provincias"))
                .andExpect(flash().attribute("mensaje", "Provincia modificada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(provinciaServicio).modificarProvincia(eq(5L), any(ProvinciaFormDTO.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-21: modificar con un nombre duplicado responde 422 con el formulario de edición y el mensaje")
    void modificar_debeMostrarElErrorDeNombreDuplicado() throws Exception {
        when(paisServicio.buscarPorId(3L)).thenReturn(pais(3L, "Argentina"));
        when(provinciaServicio.modificarProvincia(eq(5L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe una provincia con ese nombre"));

        mockMvc.perform(post("/ubicaciones/provincias/5").with(csrf()).param("nombre", "Santa Fe").param("idPais", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar la provincia")))
                .andExpect(content().string(containsString("Ya existe una provincia con ese nombre")))
                .andExpect(content().string(containsString("action=\"/ubicaciones/provincias/5\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-PRC-22: un gerente no puede modificar una provincia")
    void modificar_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/provincias/5").with(csrf()).param("nombre", "Santa Fe").param("idPais", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).modificarProvincia(any(), any());
    }

    // ==================== POST /ubicaciones/provincias/{id}/baja ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-23: la baja correcta delega en el service y redirige al listado con alerta de éxito")
    void baja_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/ubicaciones/provincias/5/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/provincias"))
                .andExpect(flash().attribute("mensaje", "Provincia dada de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(provinciaServicio).bajaProvincia(5L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PRC-24: si el service rechaza la baja (tiene localidades activas) vuelve al origen con la alerta de advertencia")
    void baja_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("La provincia tiene localidades asociadas")).when(provinciaServicio).bajaProvincia(5L);

        mockMvc.perform(post("/ubicaciones/provincias/5/baja").with(csrf()).header("Referer", "/ubicaciones/provincias"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ubicaciones/provincias"))
                .andExpect(flash().attribute("mensaje", "La provincia tiene localidades asociadas"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PRC-25: un gerente no puede dar de baja una provincia")
    void baja_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/ubicaciones/provincias/5/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).bajaProvincia(any());
    }

    private static PaisResponseDTO pais(Long id, String nombre) {
        return PaisResponseDTO.builder().id(id).nombre(nombre).build();
    }

    private static ProvinciaResponseDTO provincia(Long id, String nombre, Long idPais, String nombrePais) {
        return ProvinciaResponseDTO.builder().id(id).nombre(nombre).pais(pais(idPais, nombrePais)).build();
    }
}
