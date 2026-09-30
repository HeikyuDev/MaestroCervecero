package com.github.heikyudev.maestrocervecero.presentation.controller.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller de la gestión de usuarios. Cada método declara su propio rol con
 * {@link PreAuthorize}: solo el ADMINISTRADOR tiene los permisos {@code USUARIO_*}.
 * <p>
 * Sobre la cuenta propia, el administrador no puede darse de baja ni cambiarse el rol: si el
 * único administrador lo hiciera, nadie podría volver a gestionar usuarios.
 * </p>
 */
@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private static final String VISTA_FORMULARIO = "usuario/usuario-form";
    private static final String ATRIBUTO_FORMULARIO = "usuarioForm";
    private static final int TAMANIO_PAGINA = 20;

    /**
     * Usuario es un módulo maestro: se lista en orden alfabético por apellido y luego nombre (sin
     * distinguir mayúsculas), con el id como desempate para que el orden sea siempre estable.
     */
    private static final Sort ORDEN_ALFABETICO = Sort.by(
            Sort.Order.asc("apellido").ignoreCase(),
            Sort.Order.asc("nombre").ignoreCase(),
            Sort.Order.asc("id"));

    private final IUsuarioServicio usuarioServicio;

    /**
     * Lista los usuarios activos, paginados de a {@value #TAMANIO_PAGINA}, en orden alfabético por
     * apellido y nombre. Los criterios de búsqueda son opcionales y se combinan entre sí.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String listarUsuarios(@RequestParam(required = false) String username,
                                 @RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) String apellido,
                                 @RequestParam(required = false) String correo,
                                 @RequestParam(required = false) Rol rol,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Authentication authentication,
                                 Model model) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_ALFABETICO);
        Page<UsuarioResponseDTO> usuarios = usuarioServicio.filtrarUsuarios(
                vacioANulo(nombre), vacioANulo(apellido), vacioANulo(correo), vacioANulo(username), rol, pageable);

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", Rol.values());
        model.addAttribute("usernameActual", authentication.getName());
        model.addAttribute("filtroUsername", username);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroApellido", apellido);
        model.addAttribute("filtroCorreo", correo);
        model.addAttribute("filtroRol", rol);
        return "usuario/usuario-lista";
    }

    /**
     * Muestra el formulario vacío para dar de alta un usuario.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioAlta(Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new UsuarioFormDTO());
        cargarModeloFormulario(model, null, false);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de un usuario (Post-Redirect-Get).
     * <p>
     * Si la validación del formulario o una regla del service falla, se vuelve a renderizar el
     * formulario (sin redirigir) para no perder lo que el usuario ya tipeó.
     * </p>
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String altaUsuario(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) UsuarioFormDTO usuarioForm,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                usuarioServicio.altaUsuario(usuarioForm);
                agregarMensaje(redirectAttributes, "Usuario creado correctamente", TipoAlerta.SUCCESS);
                return "redirect:/usuarios";
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("username", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, false);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del usuario (sin la contraseña).
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Authentication authentication, Model model) {
        UsuarioResponseDTO usuario = usuarioServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, UsuarioFormDTO.builder()
                .username(usuario.getUsername())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .correo(usuario.getCorreo())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .build());
        cargarModeloFormulario(model, id, esCuentaPropia(usuario, authentication));
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de un usuario (Post-Redirect-Get). Si el administrador cambia su
     * propio nombre de usuario, se cierra su sesión: quedaría autenticado con un nombre que ya
     * no existe y el control de "cuenta propia" dejaría de reconocerlo.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String modificarUsuario(@PathVariable Long id,
                                   @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) UsuarioFormDTO usuarioForm,
                                   BindingResult bindingResult,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        UsuarioResponseDTO actual = usuarioServicio.buscarPorId(id);
        boolean cuentaPropia = esCuentaPropia(actual, authentication);

        if (cuentaPropia && usuarioForm.getRol() != null && usuarioForm.getRol() != actual.getRol()) {
            bindingResult.rejectValue("rol", "cuentaPropia", "No podés cambiar tu propio rol.");
        }

        if (!bindingResult.hasErrors()) {
            try {
                usuarioServicio.modificarUsuario(id, usuarioForm);

                if (cuentaPropia && !actual.getUsername().equalsIgnoreCase(usuarioForm.getUsername())) {
                    new SecurityContextLogoutHandler().logout(request, response, authentication);
                    agregarMensaje(redirectAttributes, "Cambiaste tu nombre de usuario: iniciá sesión nuevamente.", TipoAlerta.INFO);
                    return "redirect:/login";
                }

                agregarMensaje(redirectAttributes, "Usuario modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:/usuarios";
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("username", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, cuentaPropia);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a un usuario. Un administrador no puede darse de baja a sí mismo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String bajaUsuario(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        UsuarioResponseDTO usuario = usuarioServicio.buscarPorId(id);
        if (esCuentaPropia(usuario, authentication)) {
            throw new ReglaNegocioException("No podés dar de baja tu propia cuenta.");
        }

        usuarioServicio.bajaUsuario(id);
        agregarMensaje(redirectAttributes, "Usuario dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:/usuarios";
    }

    private void cargarModeloFormulario(Model model, Long usuarioId, boolean cuentaPropia) {
        model.addAttribute("roles", Rol.values());
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("esCuentaPropia", cuentaPropia);
        model.addAttribute("reglasPassword", MetodosPassword.MENSAJE_INVALIDA);
    }

    private static boolean esCuentaPropia(UsuarioResponseDTO usuario, Authentication authentication) {
        return authentication != null && usuario.getUsername().equalsIgnoreCase(authentication.getName());
    }

    private static void agregarMensaje(RedirectAttributes redirectAttributes, String mensaje, TipoAlerta tipo) {
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", tipo.getCodigo());
    }

    private static String vacioANulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
