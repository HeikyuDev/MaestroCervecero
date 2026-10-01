package com.github.heikyudev.maestrocervecero.presentation.controller.usuario;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class UsuarioControllerTest {

    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUsuarioServicio usuarioServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /usuarios ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-01: el ADMINISTRADOR ve el listado con nombre y apellido separados, y las acciones de editar y dar de baja")
    void listado_debeMostrarLosUsuariosParaAdministrador() throws Exception {
        when(usuarioServicio.filtrarUsuarios(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-lista"))
                .andExpect(content().string(containsString("carlos@mail.com")))
                .andExpect(content().string(containsString("Carlos")))
                .andExpect(content().string(containsString("Gomez")))
                .andExpect(content().string(containsString("GERENTE DE COMPRAS")))
                .andExpect(content().string(containsString("/usuarios/nuevo")))
                .andExpect(content().string(containsString("/usuarios/7/editar")))
                .andExpect(content().string(containsString("data-baja-url=\"/usuarios/7/baja\"")))
                // El diálogo de baja envía un POST: su formulario debe llevar el token CSRF
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-02: sin filtros pide páginas de 20 ordenadas alfabéticamente por apellido y nombre, con todos los criterios en null")
    void listado_debePedirPaginasDe20OrdenadasAlfabeticamente() throws Exception {
        when(usuarioServicio.filtrarUsuarios(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/usuarios")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioServicio).filtrarUsuarios(isNull(), isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        // Orden alfabético (sin distinguir mayúsculas): apellido, luego nombre, y el id como desempate
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("apellido", "nombre", "id");
        assertThat(captor.getValue().getSort().stream().allMatch(orden -> orden.getDirection() == Sort.Direction.ASC)).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("apellido").isIgnoreCase()).isTrue();
        assertThat(captor.getValue().getSort().getOrderFor("nombre").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-03: los criterios (incluidos nombre y apellido por separado) llegan al service y los vacíos se convierten en null")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(usuarioServicio.filtrarUsuarios(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/usuarios")
                        .param("username", "carl")
                        .param("nombre", "   ")
                        .param("apellido", "gom")
                        .param("correo", "mail")
                        .param("rol", "GERENTE_DE_COMPRAS")
                        .param("pagina", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioServicio).filtrarUsuarios(isNull(), eq("gom"), eq("mail"), eq("carl"), eq(Rol.GERENTE_DE_COMPRAS), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-04: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(usuarioServicio.filtrarUsuarios(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron usuarios con los criterios indicados.")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-05: la fila de la cuenta propia no ofrece dar de baja (el basurero queda deshabilitado)")
    void listado_noDebeOfrecerBajaSobreLaCuentaPropia() throws Exception {
        when(usuarioServicio.filtrarUsuarios(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(usuario(1L, "admin", Rol.ADMINISTRADOR)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No podés dar de baja tu propia cuenta")))
                .andExpect(content().string(not(containsString("data-baja-url=\"/usuarios/1/baja\""))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-UC-06: un rol distinto de ADMINISTRADOR no puede ver el listado (no llega al service)")
    void listado_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(usuarioServicio, never()).filtrarUsuarios(any(), any(), any(), any(), any(), any());
    }

    // ==================== GET /usuarios/nuevo ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-07: el ADMINISTRADOR ve el formulario de alta con nombre y apellido separados, roles y asteriscos de obligatorio")
    void formularioAlta_debeRenderizarseParaAdministrador() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeExists("usuarioForm", "roles"))
                .andExpect(content().string(containsString("Nuevo usuario")))
                .andExpect(content().string(containsString("Nombre/s")))
                .andExpect(content().string(containsString("Apellido/s")))
                .andExpect(content().string(not(containsString("Nombre completo"))))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("Crear usuario")))
                .andExpect(content().string(containsString("GERENTE DE PRODUCCION")))
                .andExpect(content().string(not(containsString("No se pudo crear el usuario"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-UC-08: un rol distinto de ADMINISTRADOR es rechazado con un mensaje de permisos (no 'error inesperado')")
    void formularioAlta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No tenés permisos para realizar esta acción."))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /usuarios ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-09: alta con datos válidos llama al service y redirige (PRG) con mensaje de éxito")
    void alta_debeGuardarYRedirigirConDatosValidos() throws Exception {
        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("username", "carlos")
                        .param("password", "ClaveSegura123")
                        .param("nombre", "Carlos")
                        .param("apellido", "Gomez")
                        .param("correo", "carlos@mail.com")
                        .param("telefono", "3511234567")
                        .param("rol", "GERENTE_DE_COMPRAS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("mensaje", "Usuario creado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(usuarioServicio).altaUsuario(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-10: datos inválidos vuelven al formulario con el panel resumen y errores por campo, sin llamar al service")
    void alta_debeVolverAlFormularioConErroresDeValidacion() throws Exception {
        mockMvc.perform(post("/usuarios").with(csrf())
                        .param("username", "")
                        .param("nombre", "Carlos")
                        .param("correo", "no-es-un-correo")
                        .param("telefono", "3511234567"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "username", "apellido", "correo", "rol"))
                .andExpect(content().string(containsString("No se pudo crear el usuario")))
                .andExpect(content().string(containsString("El nombre de usuario es obligatorio")))
                .andExpect(content().string(containsString("El apellido es obligatorio")))
                .andExpect(content().string(containsString("El correo electrónico no tiene un formato válido")))
                .andExpect(content().string(containsString("El rol es obligatorio")));

        verify(usuarioServicio, never()).altaUsuario(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-11: username duplicado vuelve al formulario con el error asociado al campo, conservando lo tipeado")
    void alta_debeMostrarErrorEnElCampoCuandoElUsernameEstaDuplicado() throws Exception {
        doThrow(new RecursoDuplicadoException("El Username ya esta registrado")).when(usuarioServicio).altaUsuario(any());

        mockMvc.perform(post("/usuarios").with(csrf()).params(paramsValidos("carlos")))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "username"))
                .andExpect(content().string(containsString("El Username ya esta registrado")))
                .andExpect(content().string(containsString("Gomez")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-12: una regla de negocio del service (ej. contraseña vacía) se muestra en el panel resumen")
    void alta_debeMostrarErrorGlobalCuandoFallaUnaReglaDeNegocio() throws Exception {
        doThrow(new ReglaNegocioException("La contraseña es obligatoria")).when(usuarioServicio).altaUsuario(any());

        mockMvc.perform(post("/usuarios").with(csrf()).params(paramsValidos("carlos")))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(content().string(containsString("No se pudo crear el usuario")))
                .andExpect(content().string(containsString("La contraseña es obligatoria")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-UC-13: un rol distinto de ADMINISTRADOR no puede dar de alta (no llega al service)")
    void alta_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/usuarios").with(csrf()).params(paramsValidos("carlos")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(usuarioServicio, never()).altaUsuario(any());
    }

    @Test
    @DisplayName("CP-UC-14: sin sesión, el POST de alta exige autenticación")
    void alta_debeExigirAutenticacion() throws Exception {
        mockMvc.perform(post("/usuarios").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(usuarioServicio, never()).altaUsuario(any());
    }

    // ==================== GET /usuarios/{id}/editar ====================

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-15: el formulario de edición se reabre con los datos actuales, sin exigir la contraseña")
    void formularioEdicion_debeReabrirseConLosDatosActuales() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));

        mockMvc.perform(get("/usuarios/7/editar"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attribute("usuarioId", 7L))
                .andExpect(model().attribute("esCuentaPropia", false))
                .andExpect(content().string(containsString("Editar usuario")))
                .andExpect(content().string(containsString("Guardar cambios")))
                .andExpect(content().string(containsString("value=\"carlos\"")))
                .andExpect(content().string(containsString("value=\"Gomez\"")))
                .andExpect(content().string(containsString("Dejala vacía para conservar la actual")))
                .andExpect(content().string(containsString("action=\"/usuarios/7\"")))
                .andExpect(content().string(containsString("<select id=\"rol\"")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-16: al editar la cuenta propia el rol se muestra fijo (no hay selector para cambiarlo)")
    void formularioEdicion_debeFijarElRolEnLaCuentaPropia() throws Exception {
        when(usuarioServicio.buscarPorId(1L)).thenReturn(usuario(1L, "admin", Rol.ADMINISTRADOR));

        mockMvc.perform(get("/usuarios/1/editar"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("esCuentaPropia", true))
                .andExpect(content().string(containsString("No podés cambiar tu propio rol.")))
                .andExpect(content().string(not(containsString("<select id=\"rol\""))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-17: editar un usuario inexistente muestra el motivo en una alerta (no un error inesperado)")
    void formularioEdicion_debeInformarCuandoElUsuarioNoExiste() throws Exception {
        when(usuarioServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró el usuario con ID: 99"));

        mockMvc.perform(get("/usuarios/99/editar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró el usuario con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /usuarios/{id} ====================

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-18: modificar con datos válidos llama al service y redirige (PRG) con mensaje de éxito")
    void modificar_debeGuardarYRedirigir() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));

        mockMvc.perform(post("/usuarios/7").with(csrf()).params(paramsValidos("carlos")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("mensaje", "Usuario modificado correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(usuarioServicio).modificarUsuario(eq(7L), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-19: modificar con datos inválidos vuelve al formulario de edición con el panel resumen, sin llamar al service")
    void modificar_debeVolverAlFormularioConErroresDeValidacion() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));

        mockMvc.perform(post("/usuarios/7").with(csrf())
                        .param("username", "carlos")
                        .param("nombre", "")
                        .param("apellido", "Gomez")
                        .param("correo", "carlos@mail.com")
                        .param("telefono", "3511234567")
                        .param("rol", "GERENTE_DE_COMPRAS"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attribute("usuarioId", 7L))
                .andExpect(content().string(containsString("No se pudo modificar el usuario")))
                .andExpect(content().string(containsString("El nombre es obligatorio")));

        verify(usuarioServicio, never()).modificarUsuario(any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-20: modificar con un username ya usado por otra cuenta vuelve al formulario con el error en el campo")
    void modificar_debeMostrarErrorCuandoElUsernameEstaEnUso() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));
        doThrow(new RecursoDuplicadoException("El nombre de usuario 'pedro' ya está en uso.")).when(usuarioServicio).modificarUsuario(eq(7L), any());

        mockMvc.perform(post("/usuarios/7").with(csrf()).params(paramsValidos("pedro")))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "username"))
                .andExpect(content().string(containsString("El nombre de usuario &#39;pedro&#39; ya está en uso.")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-21: un administrador no puede cambiar su propio rol (no llega al service)")
    void modificar_debeBloquearElCambioDeRolPropio() throws Exception {
        when(usuarioServicio.buscarPorId(1L)).thenReturn(usuario(1L, "admin", Rol.ADMINISTRADOR));

        mockMvc.perform(post("/usuarios/1").with(csrf())
                        .param("username", "admin")
                        .param("nombre", "Admin")
                        .param("apellido", "Sistema")
                        .param("correo", "admin@mail.com")
                        .param("telefono", "1111")
                        .param("rol", "GERENTE_DE_COMPRAS"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "rol"))
                .andExpect(content().string(containsString("No podés cambiar tu propio rol.")));

        verify(usuarioServicio, never()).modificarUsuario(any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-22: si el administrador cambia su propio username se cierra su sesión y vuelve al login con un aviso")
    void modificar_debeCerrarLaSesionCuandoSeCambiaElPropioUsername() throws Exception {
        when(usuarioServicio.buscarPorId(1L)).thenReturn(usuario(1L, "admin", Rol.ADMINISTRADOR));

        mockMvc.perform(post("/usuarios/1").with(csrf())
                        .param("username", "admin2")
                        .param("nombre", "Admin")
                        .param("apellido", "Sistema")
                        .param("correo", "admin@mail.com")
                        .param("telefono", "1111")
                        .param("rol", "ADMINISTRADOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje", "Cambiaste tu nombre de usuario: iniciá sesión nuevamente."))
                .andExpect(flash().attribute("tipo", "info"));

        verify(usuarioServicio).modificarUsuario(eq(1L), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-UC-23: un rol distinto de ADMINISTRADOR no puede modificar (no llega al service)")
    void modificar_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/usuarios/7").with(csrf()).params(paramsValidos("carlos")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(usuarioServicio, never()).modificarUsuario(any(), any());
    }

    // ==================== POST /usuarios/{id}/baja ====================

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-24: dar de baja a otro usuario llama al service y redirige (PRG) con mensaje de éxito")
    void baja_debeDarDeBajaAOtroUsuario() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));

        mockMvc.perform(post("/usuarios/7/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("mensaje", "Usuario dado de baja correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(usuarioServicio).bajaUsuario(7L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-25: un administrador no puede darse de baja a sí mismo (no llega al service) y se le explica el motivo")
    void baja_debeBloquearLaBajaPropia() throws Exception {
        when(usuarioServicio.buscarPorId(1L)).thenReturn(usuario(1L, "admin", Rol.ADMINISTRADOR));

        mockMvc.perform(post("/usuarios/1/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No podés dar de baja tu propia cuenta."))
                .andExpect(flash().attribute("tipo", "warning"));

        verify(usuarioServicio, never()).bajaUsuario(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-UC-26: un rol distinto de ADMINISTRADOR no puede dar de baja (no llega al service)")
    void baja_debeRechazarRolNoAdministrador() throws Exception {
        mockMvc.perform(post("/usuarios/7/baja").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(usuarioServicio, never()).bajaUsuario(any());
    }

    // ==================== validación de la contraseña ====================

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-27: en el alta, una contraseña de solo espacios marca el campo contraseña con el error y no llega al service")
    void alta_debeRechazarPasswordSoloConEspacios() throws Exception {
        org.springframework.util.LinkedMultiValueMap<String, String> params = paramsValidos("carlos");
        params.set("password", "                ");

        mockMvc.perform(post("/usuarios").with(csrf()).params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "password"))
                .andExpect(content().string(containsString("No se pudo crear el usuario")))
                .andExpect(content().string(containsString("no puede contener espacios en blanco")));

        verify(usuarioServicio, never()).altaUsuario(any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-28: en el alta, una contraseña corta o con espacios en el medio es rechazada en el formulario")
    void alta_debeRechazarPasswordCortaOConEspacios() throws Exception {
        for (String password : new String[]{"abc123", "clave segura1"}) {
            org.springframework.util.LinkedMultiValueMap<String, String> params = paramsValidos("carlos");
            params.set("password", password);

            mockMvc.perform(post("/usuarios").with(csrf()).params(params))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("usuarioForm", "password"));
        }

        verify(usuarioServicio, never()).altaUsuario(any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-29: al editar, una contraseña de solo espacios se rechaza en el formulario")
    void modificar_debeRechazarPasswordSoloConEspacios() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));
        org.springframework.util.LinkedMultiValueMap<String, String> params = paramsValidos("carlos");
        params.set("password", "                ");

        mockMvc.perform(post("/usuarios/7").with(csrf()).params(params))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/usuario-form"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "password"))
                .andExpect(content().string(containsString("No se pudo modificar el usuario")));

        verify(usuarioServicio, never()).modificarUsuario(any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-30: al editar, dejar la contraseña vacía es válido (conserva la actual)")
    void modificar_debeAceptarPasswordVacia() throws Exception {
        when(usuarioServicio.buscarPorId(7L)).thenReturn(usuario(7L, "carlos", Rol.GERENTE_DE_COMPRAS));
        org.springframework.util.LinkedMultiValueMap<String, String> params = paramsValidos("carlos");
        params.set("password", "");

        mockMvc.perform(post("/usuarios/7").with(csrf()).params(params))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios"));

        verify(usuarioServicio).modificarUsuario(eq(7L), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UC-31: el formulario muestra las reglas de la contraseña como ayuda")
    void formulario_debeMostrarLasReglasDeLaContrasena() throws Exception {
        mockMvc.perform(get("/usuarios/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La contraseña debe tener entre 8 y 72 caracteres y no puede contener espacios en blanco")));
    }

    // ==================== helpers ====================

    private static UsuarioResponseDTO usuario(Long id, String username, Rol rol) {
        return UsuarioResponseDTO.builder()
                .id(id).username(username).nombre("Carlos").apellido("Gomez")
                .correo("carlos@mail.com").telefono("3511234567").rol(rol).build();
    }

    private static org.springframework.util.LinkedMultiValueMap<String, String> paramsValidos(String username) {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("username", username);
        params.add("password", "ClaveSegura123");
        params.add("nombre", "Carlos");
        params.add("apellido", "Gomez");
        params.add("correo", "carlos@mail.com");
        params.add("telefono", "3511234567");
        params.add("rol", "GERENTE_DE_COMPRAS");
        return params;
    }
}
