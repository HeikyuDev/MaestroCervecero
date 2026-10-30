package com.github.heikyudev.maestrocervecero.presentation.controller.costo_adicional;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.costo_adicional.CostoDirectoAdicionalFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.costo_adicional.ICostoDirectoAdicionalServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

@WebMvcTest(CostoDirectoAdicionalController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class CostoDirectoAdicionalControllerTest {

    private static final String URL = "/configuracion-produccion/costos-directos-adicionales";
    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICostoDirectoAdicionalServicio costoDirectoAdicionalServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET (lista) ====================

    @Test
    @DisplayName("CP-CDC-16: el GERENTE_DE_PRODUCCION ve la pestaña Costos directos adicionales con el botón de alta a la derecha del encabezado, los costos en orden alfabético con el importe formateado y las acciones de editar y dar de baja")
    void lista_debeMostrarLosCostos() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(
                respuesta(5L, "Gas butano", "12.5000"), respuesta(6L, "Dióxido de carbono", "1234.5")), PageRequest.of(0, 20), 2));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/costos-directos-lista"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("<title>Configuración de producción - Maestro Cervecero</title>").contains("data-costos-directos").contains("href=\"" + URL + "/nuevo\"")
                .contains("Nuevo costo directo adicional").contains(">12,50<").contains(">1.234,50<").contains("Costo por litro ($ por litro)")
                .contains("href=\"" + URL + "/5/editar\"").contains("data-baja-url=\"" + URL + "/6/baja\"").contains("data-baja-nombre=\"Gas butano\"")
                .contains("class=\"col-acciones px-4 py-3 text-right\">Acciones");
        assertThat(html).containsPattern("(?s)data-pestana-costos[^>]*aria-current=\"page\"").contains("data-pestana-parametros").contains("bi-cash-coin").contains("bi-sliders");
        assertThat(html).containsPattern("(?s)<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>");
        assertThat(html.indexOf("data-nuevo-costo-directo")).isLessThan(html.indexOf("data-pestanas-configuracion"));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(costoDirectoAdicionalServicio).filtrarCostosDirectosAdicionales(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList()).containsExactly("nombre", "id");
    }

    @Test
    @DisplayName("CP-CDC-17: sin costos la lista lo informa; sin permisos de modificar, dar de baja o crear no se muestran esas acciones; sin consultar, la lista es rechazada")
    void lista_sinCostosOSinPermisos() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(respuesta(5L, "Gas", "1")), PageRequest.of(0, 20), 1));
        RequestPostProcessor soloConsultar = user("c").authorities(new SimpleGrantedAuthority("ROLE_GERENTE_DE_PRODUCCION"), new SimpleGrantedAuthority("COSTO_DIRECTO_ADICIONAL_CONSULTAR"));

        // Act
        String consulta = mockMvc.perform(get(URL).with(soloConsultar)).andReturn().getResponse().getContentAsString();
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        String vacia = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(consulta).doesNotContain("Nuevo costo directo adicional").doesNotContain("/editar").doesNotContain("data-baja-url").doesNotContain("dialogo-baja");
        assertThat(vacia).contains("No hay costos directos adicionales registrados");
        mockMvc.perform(get(URL).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @DisplayName("CP-CDC-18: la lista pide la página indicada")
    void lista_debePedirLaPagina() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 25));

        // Act
        mockMvc.perform(get(URL).param("pagina", "1").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(costoDirectoAdicionalServicio).filtrarCostosDirectosAdicionales(any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    // ==================== GET /nuevo ====================

    @Test
    @DisplayName("CP-CDC-01: el GERENTE_DE_PRODUCCION ve el formulario de alta con los campos obligatorios, los placeholders, la unidad como sufijo y la ayuda")
    void alta_formulario_debeMostrarLosCamposDelAlta() throws Exception {
        // Act
        String html = mockMvc.perform(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/costo-directo-form"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Nuevo costo directo adicional").contains("action=\"" + URL + "\"").contains("novalidate")
                .contains("placeholder=\"Ej: Gas butano, Dióxido de carbono\"").contains("placeholder=\"Ej: 12,50\"")
                .contains("$ por litro").contains("data-grupo-unidad").contains("Crear costo directo adicional")
                .contains("href=\"" + URL + "\"").contains("Volver a costos directos adicionales");
        assertThat(html.split(java.util.regex.Pattern.quote(OBLIGATORIO), -1)).hasSize(3);
    }

    @Test
    @DisplayName("CP-CDC-02: un rol sin el permiso de crear no abre el formulario de alta y no se invoca el service")
    void alta_formulario_debeRechazarUnRolSinPermiso() throws Exception {
        // Act + Assert
        mockMvc.perform(get(URL + "/nuevo").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST (alta) ====================

    @Test
    @DisplayName("CP-CDC-03: el alta válida llega al service con el nombre y el costo y vuelve a la pestaña de costos con el mensaje de éxito")
    void alta_debeRegistrarYVolverALaConfiguracion() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.altaCostoDirectoAdicional(any())).thenReturn(respuesta(5L, "Gas butano", "12.5000"));

        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas butano").param("costoPorLitro", "12.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Costo directo adicional creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<CostoDirectoAdicionalFormDTO> captor = ArgumentCaptor.forClass(CostoDirectoAdicionalFormDTO.class);
        verify(costoDirectoAdicionalServicio).altaCostoDirectoAdicional(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Gas butano");
        assertThat(captor.getValue().getCostoPorLitro()).isEqualByComparingTo("12.5");
    }

    @Test
    @DisplayName("CP-CDC-04: sin nombre ni costo vuelve al formulario con los mensajes de cada campo y sin llamar al service")
    void alta_sinDatos_debeVolverConLosErrores() throws Exception {
        // Act
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "").param("costoPorLitro", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/costo-directo-form"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("No se pudo crear el costo directo adicional").contains("El nombre es obligatorio").contains("El costo por litro es obligatorio");
        verify(costoDirectoAdicionalServicio, never()).altaCostoDirectoAdicional(any());
    }

    @Test
    @DisplayName("CP-CDC-05: un nombre duplicado vuelve al formulario con el mensaje en el campo nombre y conserva lo tipeado")
    void alta_nombreDuplicado_debeMostrarElMensajeEnElCampo() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.altaCostoDirectoAdicional(any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un costo directo adicional con el nombre 'Gas butano'"));

        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas butano").param("costoPorLitro", "12.5"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/costo-directo-form"))
                .andExpect(content().string(containsString("Ya existe un costo directo adicional con el nombre &#39;Gas butano&#39;")))
                .andExpect(content().string(containsString("value=\"Gas butano\"")))
                .andExpect(content().string(containsString("value=\"12.5\"")));
    }

    @Test
    @DisplayName("CP-CDC-06: una regla del service sobre el costo (por ejemplo el máximo) se marca en el campo del costo")
    void alta_reglaDelCosto_debeMostrarseEnElCampo() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.altaCostoDirectoAdicional(any()))
                .thenThrow(new ReglaNegocioException("El costo por litro no puede superar 1.000.000", "costoPorLitro"));

        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas butano").param("costoPorLitro", "2000000"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El costo por litro no puede superar 1.000.000")))
                .andExpect(content().string(containsString("border-rose-400 bg-rose-50")));
    }

    @Test
    @DisplayName("CP-CDC-07: una regla sin campo (por ejemplo un valor no numérico) se muestra como error general y un costo no numérico no llega al service")
    void alta_costoNoNumerico_debeVolverConElError() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas butano").param("costoPorLitro", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/costo-directo-form"))
                .andExpect(content().string(containsString("No se pudo crear el costo directo adicional")));
        verify(costoDirectoAdicionalServicio, never()).altaCostoDirectoAdicional(any());
    }

    @Test
    @DisplayName("CP-CDC-08: el alta sin token CSRF no llega al service")
    void alta_sinCsrf_noDebeLlegarAlService() throws Exception {
        // Act + Assert
        int estado = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).param("nombre", "Gas butano").param("costoPorLitro", "12.5"))
                .andReturn().getResponse().getStatus();
        assertThat(estado).isNotEqualTo(200);
        verify(costoDirectoAdicionalServicio, never()).altaCostoDirectoAdicional(any());
    }

    // ==================== GET /{id}/editar ====================

    @Test
    @DisplayName("CP-CDC-09: el formulario de edición muestra el nombre y el costo actuales sin ceros sobrantes y apunta a la modificación")
    void edicion_formulario_debePrecargarLosDatos() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.buscarPorId(5L)).thenReturn(respuesta(5L, "Gas butano", "12.5000"));

        // Act
        String html = mockMvc.perform(get(URL + "/5/editar").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/costo-directo-form"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("Editar costo directo adicional").contains("action=\"" + URL + "/5\"").contains("value=\"Gas butano\"")
                .contains("value=\"12.5\"").contains("Guardar cambios");
    }

    @Test
    @DisplayName("CP-CDC-10: un costo entero se muestra sin notación científica y un costo inexistente vuelve a Configuración con la alerta")
    void edicion_formulario_debeManejarEnterosEInexistentes() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.buscarPorId(5L)).thenReturn(respuesta(5L, "Agua", "10.0000"));
        when(costoDirectoAdicionalServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el costo directo adicional con ID: 99"));

        // Act + Assert
        mockMvc.perform(get(URL + "/5/editar").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(content().string(containsString("value=\"10\"")));
        mockMvc.perform(get(URL + "/99/editar").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /{id} ====================

    @Test
    @DisplayName("CP-CDC-11: la modificación válida llega al service con el id y vuelve a la pestaña de costos con el mensaje de éxito")
    void modificacion_debeActualizarYVolverALaConfiguracion() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(eq(5L), any())).thenReturn(respuesta(5L, "Gas butano", "15"));

        // Act + Assert
        mockMvc.perform(post(URL + "/5").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas butano").param("costoPorLitro", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Costo directo adicional modificado correctamente"));
        verify(costoDirectoAdicionalServicio).modificarCostoDirectoAdicional(eq(5L), any());
    }

    @Test
    @DisplayName("CP-CDC-12: la modificación con un nombre duplicado o datos inválidos vuelve al formulario de edición con el error")
    void modificacion_conErrores_debeVolverAlFormulario() throws Exception {
        // Arrange
        when(costoDirectoAdicionalServicio.modificarCostoDirectoAdicional(eq(5L), any()))
                .thenThrow(new RecursoDuplicadoException("Ya existe un costo directo adicional con el nombre 'Agua'"));

        // Act + Assert
        mockMvc.perform(post(URL + "/5").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Agua").param("costoPorLitro", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo modificar el costo directo adicional")))
                .andExpect(content().string(containsString("Ya existe un costo directo adicional con el nombre &#39;Agua&#39;")))
                .andExpect(content().string(containsString("action=\"" + URL + "/5\"")));
        mockMvc.perform(post(URL + "/5").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "").param("costoPorLitro", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El nombre es obligatorio")));
    }

    // ==================== POST /{id}/baja ====================

    @Test
    @DisplayName("CP-CDC-13: la baja llama al service y vuelve a la pestaña de costos con el mensaje de éxito")
    void baja_debeDarDeBajaYVolver() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL + "/5/baja").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Costo directo adicional dado de baja correctamente"));
        verify(costoDirectoAdicionalServicio).bajaCostoDirectoAdicional(5L);
    }

    @Test
    @DisplayName("CP-CDC-14: un rol sin permiso de crear, modificar o dar de baja es rechazado y no se invoca el service")
    void operaciones_debenRechazarUnRolSinPermiso() throws Exception {
        // Act + Assert
        mockMvc.perform(post(URL).with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas").param("costoPorLitro", "1"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get(URL + "/5/editar").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/5").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).param("nombre", "Gas").param("costoPorLitro", "1"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/5/baja").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));

        verify(costoDirectoAdicionalServicio, never()).altaCostoDirectoAdicional(any());
        verify(costoDirectoAdicionalServicio, never()).modificarCostoDirectoAdicional(any(), any());
        verify(costoDirectoAdicionalServicio, never()).bajaCostoDirectoAdicional(any());
        verify(costoDirectoAdicionalServicio, never()).buscarPorId(any());
    }

    @Test
    @DisplayName("CP-CDC-15: el GERENTE_DE_PRODUCCION tiene los cuatro permisos de costos directos adicionales")
    void gerenteDeProduccion_tieneLosPermisosDeCostos() {
        // Assert
        assertThat(Rol.GERENTE_DE_PRODUCCION.getPermisos().stream().map(Enum::name).filter(n -> n.startsWith("COSTO_DIRECTO_ADICIONAL_")))
                .containsExactlyInAnyOrder("COSTO_DIRECTO_ADICIONAL_CREAR", "COSTO_DIRECTO_ADICIONAL_MODIFICAR",
                        "COSTO_DIRECTO_ADICIONAL_ELIMINAR", "COSTO_DIRECTO_ADICIONAL_CONSULTAR");
    }

    // ---------- Apoyo ----------

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }

    private static CostoDirectoAdicionalResponseDTO respuesta(Long id, String nombre, String costo) {
        return CostoDirectoAdicionalResponseDTO.builder().id(id).nombre(nombre).costoPorLitro(new BigDecimal(costo)).build();
    }
}
