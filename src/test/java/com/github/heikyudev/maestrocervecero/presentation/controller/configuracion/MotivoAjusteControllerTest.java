package com.github.heikyudev.maestrocervecero.presentation.controller.configuracion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IMotivoAjusteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MotivoAjusteController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class MotivoAjusteControllerTest {

    private static final String URL = "/configuracion-produccion/motivos-ajuste";
    private static final String SIN_PERMISOS = "No tiene permisos para realizar esta acción";
    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IMotivoAjusteServicio servicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    /** Escenario base: motivo 1 "Merma por derrame" EGRESO sin ajustes, motivo 2 "Recuento de inventario" INGRESO con ajustes. */
    @BeforeEach
    void escenarioBase() {
        when(servicio.filtrarMotivosAjuste(any(), any(), any())).thenReturn(new PageImpl<>(List.of(motivo(1L, "Merma por derrame", TipoAjuste.EGRESO, null),
                motivo(2L, "Recuento de inventario", TipoAjuste.INGRESO, null))));
        when(servicio.buscarPorId(1L)).thenReturn(motivo(1L, "Merma por derrame", TipoAjuste.EGRESO, false));
        when(servicio.buscarPorId(2L)).thenReturn(motivo(2L, "Recuento de inventario", TipoAjuste.INGRESO, true));
        when(servicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: 99"));
    }

    // ==================== Lista ====================

    @Test
    @DisplayName("CP-CMA-01: la lista muestra el título, la pestaña activa, las columnas, el tipo como Ingreso o Egreso, los botones de alta, edición y baja y el diálogo de baja")
    void lista_columnas() throws Exception {
        String html = html(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivos-ajuste-lista");

        assertThat(html).contains("Configuración de Producción").contains("Motivos de ajuste").contains("data-pestanas-configuracion").contains("data-pestana-motivos-ajuste")
                .contains("Nombre").contains("Tipo de ajuste").contains("Acciones")
                .contains("Merma por derrame").contains("Recuento de inventario").contains(">Egreso<").contains(">Ingreso<")
                .contains("data-nuevo-motivo-ajuste").contains(URL + "/nuevo").contains(URL + "/1/editar").contains(URL + "/2/baja")
                .contains("Ej: Merma por derrame").contains("Limpiar").contains("Buscar").contains("id=\"dialogo-baja\"");
        assertThat(count(html, "aria-current=\"page\"")).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-CMA-02: los criterios de la lista (nombre sin espacios y tipo) se pasan al service, de a 20 y ordenados por nombre, y se conservan en el formulario")
    void lista_filtros() throws Exception {
        String html = html(get(URL).param("nombre", " merma ").param("tipo", "EGRESO").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivos-ajuste-lista");

        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarMotivosAjuste(eq("merma"), eq(TipoAjuste.EGRESO), pagina.capture());
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(0);
        assertThat(pagina.getValue().getSort().getOrderFor("nombre")).isNotNull();
        assertThat(pagina.getValue().getSort().getOrderFor("nombre").isAscending()).isTrue();
        assertThat(pagina.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
        assertThat(html).contains("value=\" merma \"").contains("<option value=\"EGRESO\" selected");
    }

    @Test
    @DisplayName("CP-CMA-03: sin criterios no se restringe por ninguno, un nombre en blanco equivale a no filtrar y un tipo inválido o una página negativa se resuelven sin consultar mal")
    void lista_sinCriterios() throws Exception {
        html(get(URL).param("nombre", "   ").param("pagina", "-3").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivos-ajuste-lista");
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(servicio).filtrarMotivosAjuste(isNull(), isNull(), pagina.capture());
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(0);

        mockMvc.perform(get(URL).param("tipo", "OTRO").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", URL))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @DisplayName("CP-CMA-04: la lista vacía muestra el mensaje de sin resultados")
    void lista_vacia() throws Exception {
        when(servicio.filtrarMotivosAjuste(any(), any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        String html = html(get(URL).param("nombre", "zzz").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivos-ajuste-lista");

        assertThat(html).contains("No se encontraron motivos de ajuste con los criterios indicados");
    }

    @Test
    @DisplayName("CP-CMA-05: los enlaces de paginación de la lista conservan el nombre y el tipo")
    void lista_paginacion() throws Exception {
        when(servicio.filtrarMotivosAjuste(any(), any(), any())).thenReturn(new PageImpl<>(
                Collections.nCopies(20, motivo(1L, "Merma por derrame", TipoAjuste.EGRESO, null)), PageRequest.of(0, 20), 45));

        String html = html(get(URL).param("nombre", "merma").param("tipo", "EGRESO").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivos-ajuste-lista");

        assertThat(html).contains("<span>45</span> <span>motivos de ajuste</span>").contains("pagina=1").contains("nombre=merma").contains("tipo=EGRESO");
    }

    // ==================== Permisos ====================

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_COMPRAS", "OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "ENCARGADO_DE_DISTRIBUCION", "ADMINISTRADOR"})
    @DisplayName("CP-CMA-06: solo el gerente de producción accede al módulo: los demás roles redirigen con la alerta de permisos en todas las rutas y no se invoca el service")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String url : List.of(URL, URL + "/nuevo", URL + "/1/editar")) {
            mockMvc.perform(get(url).with(como(rol))).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("mensaje", SIN_PERMISOS)).andExpect(flash().attribute("tipo", "danger"));
        }
        for (String url : List.of(URL, URL + "/1", URL + "/1/baja")) {
            mockMvc.perform(post(url).with(como(rol)).with(csrf()).param("nombre", "X").param("tipoAjuste", "INGRESO")).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("mensaje", SIN_PERMISOS)).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    @DisplayName("CP-CMA-07: cada método del Controller exige su permiso de motivo de ajuste (consultar, crear, modificar o eliminar)")
    void preAuthorize() {
        Map<String, String> esperado = Map.of(
                "listarMotivosAjuste", "MOTIVO_AJUSTE_CONSULTAR",
                "mostrarFormularioAlta", "MOTIVO_AJUSTE_CREAR",
                "altaMotivoAjuste", "MOTIVO_AJUSTE_CREAR",
                "mostrarFormularioEdicion", "MOTIVO_AJUSTE_MODIFICAR",
                "modificarMotivoAjuste", "MOTIVO_AJUSTE_MODIFICAR",
                "bajaMotivoAjuste", "MOTIVO_AJUSTE_ELIMINAR");
        int metodos = 0;
        for (Method metodo : MotivoAjusteController.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(metodo.getModifiers())) {
                metodos++;
                PreAuthorize anotacion = metodo.getAnnotation(PreAuthorize.class);
                assertThat(anotacion).as(metodo.getName()).isNotNull();
                assertThat(anotacion.value()).as(metodo.getName()).isEqualTo("hasAuthority('" + esperado.get(metodo.getName()) + "')");
            }
        }
        assertThat(metodos).isEqualTo(esperado.size());
    }

    @Test
    @DisplayName("CP-CMA-08: la pestaña Motivos de ajuste solo la ve quien tiene MOTIVO_AJUSTE_CONSULTAR, es decir, solo el gerente de producción")
    void pestana() {
        for (Rol rol : Rol.values()) {
            boolean tiene = rol.getPermisos().stream().anyMatch(p -> p.name().equals("MOTIVO_AJUSTE_CONSULTAR"));
            assertThat(tiene).as(rol.name()).isEqualTo(rol == Rol.GERENTE_DE_PRODUCCION);
        }
    }

    // ==================== Alta ====================

    @Test
    @DisplayName("CP-CMA-09: el formulario de alta muestra Nombre y Tipo de ajuste con asterisco, ejemplos, la ayuda del tipo, el selector habilitado y el botón Crear")
    void formularioAlta() throws Exception {
        String html = html(get(URL + "/nuevo").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivo-ajuste-form");

        assertThat(html).contains("Nuevo motivo de ajuste").contains("Volver a motivos de ajuste").contains("novalidate")
                .contains("Nombre " + OBLIGATORIO).contains("Tipo de ajuste " + OBLIGATORIO).contains("Ej: Merma por derrame")
                .contains("<option value=\"INGRESO\">Ingreso</option>").contains("<option value=\"EGRESO\">Egreso</option>")
                .contains("Ingreso suma cantidad al lote de insumo; Egreso la resta.").contains("Crear motivo de ajuste")
                .doesNotContainPattern("<select[^>]*disabled=\"disabled\"").doesNotContain("type=\"hidden\" name=\"tipoAjuste\"").doesNotContain("data-tipo-bloqueado");
    }

    @Test
    @DisplayName("CP-CMA-10: el alta válida llama al service, redirige a la lista con la alerta de éxito")
    void alta_ok() throws Exception {
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Merma por derrame").param("tipoAjuste", "EGRESO"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Motivo de ajuste creado correctamente")).andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<MotivoAjusteFormDTO> form = ArgumentCaptor.forClass(MotivoAjusteFormDTO.class);
        verify(servicio).altaMotivoAjuste(form.capture());
        assertThat(form.getValue().getNombre()).isEqualTo("Merma por derrame");
        assertThat(form.getValue().getTipoAjuste()).isEqualTo(TipoAjuste.EGRESO);
    }

    @Test
    @DisplayName("CP-CMA-11: el alta sin nombre ni tipo vuelve al formulario con ambos errores en su campo y en el resumen, conserva lo escrito y no llama al service")
    void alta_camposVacios() throws Exception {
        String html = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "").param("tipoAjuste", ""))
                .andExpect(status().isOk()).andExpect(view().name("configuracion/motivo-ajuste-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("No se pudo crear el motivo de ajuste").contains("El nombre es obligatorio").contains("El tipo de ajuste es obligatorio");
        assertThat(count(html, "El nombre es obligatorio")).isEqualTo(2);
        verify(servicio, never()).altaMotivoAjuste(any());
    }

    @Test
    @DisplayName("CP-CMA-12: un nombre duplicado se marca en el campo nombre y una regla del service sobre el tipo, en el campo tipoAjuste")
    void alta_erroresDelService() throws Exception {
        when(servicio.altaMotivoAjuste(any())).thenThrow(new RecursoDuplicadoException("Ya existe un motivo de ajuste con el nombre 'Merma por derrame'"));
        String duplicado = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Merma por derrame").param("tipoAjuste", "EGRESO"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(duplicado).contains("Ya existe un motivo de ajuste con el nombre").contains("value=\"Merma por derrame\"").contains("mt-1 text-xs text-rose-600");

        doThrow(new ReglaNegocioException("El tipo de ajuste es obligatorio", "tipoAjuste")).when(servicio).altaMotivoAjuste(any());
        String regla = mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Merma por derrame").param("tipoAjuste", "EGRESO"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(regla).contains("El tipo de ajuste es obligatorio").contains("border-rose-400");
    }

    // ==================== Edición ====================

    @Test
    @DisplayName("CP-CMA-13: el formulario de edición de un motivo sin ajustes carga sus datos y deja el selector del tipo habilitado")
    void formularioEdicion_sinAjustes() throws Exception {
        String html = html(get(URL + "/1/editar").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivo-ajuste-form");

        assertThat(html).contains("Editar motivo de ajuste").contains("value=\"Merma por derrame\"").contains("<option value=\"EGRESO\" selected=\"selected\">Egreso</option>")
                .contains("Guardar cambios").contains(URL + "/1")
                .doesNotContainPattern("<select[^>]*disabled=\"disabled\"").doesNotContain("name=\"tipoAjuste\" type=\"hidden\"").doesNotContain("data-tipo-bloqueado");
    }

    @Test
    @DisplayName("CP-CMA-14: el formulario de edición de un motivo con ajustes deshabilita el selector del tipo, envía el tipo real en un campo oculto y explica el bloqueo")
    void formularioEdicion_conAjustes() throws Exception {
        String html = html(get(URL + "/2/editar").with(como(Rol.GERENTE_DE_PRODUCCION)), "configuracion/motivo-ajuste-form");

        assertThat(html).containsPattern("<select[^>]*disabled=\"disabled\"").contains("<option value=\"INGRESO\" selected=\"selected\">Ingreso</option>")
                .containsPattern("<input type=\"hidden\" name=\"tipoAjuste\" value=\"INGRESO\"")
                .contains("data-tipo-bloqueado").contains("No se puede cambiar porque el motivo ya tiene ajustes.")
                .doesNotContain("Ingreso suma cantidad al lote de insumo; Egreso la resta.");
    }

    @Test
    @DisplayName("CP-CMA-15: la modificación válida llama al service con el ID y redirige a la lista con la alerta de éxito")
    void modificar_ok() throws Exception {
        mockMvc.perform(post(URL + "/1").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Merma por rotura").param("tipoAjuste", "EGRESO"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Motivo de ajuste modificado correctamente")).andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<MotivoAjusteFormDTO> form = ArgumentCaptor.forClass(MotivoAjusteFormDTO.class);
        verify(servicio).modificarMotivoAjuste(eq(1L), form.capture());
        assertThat(form.getValue().getNombre()).isEqualTo("Merma por rotura");
    }

    @Test
    @DisplayName("CP-CMA-16: cambiar el tipo de un motivo con ajustes vuelve al formulario con el error en el campo tipoAjuste, el selector sigue deshabilitado y se conserva lo escrito")
    void modificar_cambioDeTipoRechazado() throws Exception {
        when(servicio.modificarMotivoAjuste(eq(2L), any())).thenThrow(new ReglaNegocioException(
                "No se puede cambiar el tipo de ajuste porque el motivo ya tiene ajustes de insumo asociados", "tipoAjuste"));

        String html = mockMvc.perform(post(URL + "/2").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Recuento nuevo").param("tipoAjuste", "EGRESO"))
                .andExpect(status().isOk()).andExpect(view().name("configuracion/motivo-ajuste-form"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("No se pudo modificar el motivo de ajuste").contains("No se puede cambiar el tipo de ajuste porque el motivo ya tiene ajustes de insumo asociados")
                .contains("value=\"Recuento nuevo\"").containsPattern("<select[^>]*disabled=\"disabled\"").contains("border-rose-400");
    }

    @Test
    @DisplayName("CP-CMA-17: un nombre duplicado en la modificación se marca en el campo nombre y un motivo inexistente redirige con alerta")
    void modificar_duplicadoEInexistente() throws Exception {
        when(servicio.modificarMotivoAjuste(eq(1L), any())).thenThrow(new RecursoDuplicadoException("Ya existe un motivo de ajuste con el nombre 'Otro'"));
        String html = mockMvc.perform(post(URL + "/1").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("nombre", "Otro").param("tipoAjuste", "EGRESO"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Ya existe un motivo de ajuste con el nombre").contains("value=\"Otro\"");

        mockMvc.perform(get(URL + "/99/editar").with(como(Rol.GERENTE_DE_PRODUCCION)).header("Referer", URL))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== Baja ====================

    @Test
    @DisplayName("CP-CMA-18: la baja válida llama al service y redirige a la lista con la alerta de éxito")
    void baja_ok() throws Exception {
        mockMvc.perform(post(URL + "/1/baja").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Motivo de ajuste dado de baja correctamente")).andExpect(flash().attribute("tipo", "success"));

        verify(servicio).bajaMotivoAjuste(1L);
    }

    @Test
    @DisplayName("CP-CMA-20: la baja de un motivo inexistente redirige con alerta de error")
    void baja_inexistente() throws Exception {
        doThrow(new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: 99")).when(servicio).bajaMotivoAjuste(99L);

        mockMvc.perform(post(URL + "/99/baja").with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).header("Referer", URL))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== Apoyo ====================

    private static MotivoAjusteResponseDTO motivo(Long id, String nombre, TipoAjuste tipo, Boolean tieneAjustes) {
        return MotivoAjusteResponseDTO.builder().id(id).nombre(nombre).tipoAjuste(tipo).estado(Estado.ACTIVO).tieneAjustes(tieneAjustes).build();
    }

    private String html(MockHttpServletRequestBuilder request, String vista) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andExpect(view().name(vista))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static int count(String texto, String buscado) {
        return texto.split(java.util.regex.Pattern.quote(buscado), -1).length - 1;
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
