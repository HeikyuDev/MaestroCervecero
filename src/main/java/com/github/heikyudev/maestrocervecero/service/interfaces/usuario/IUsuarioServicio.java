package com.github.heikyudev.maestrocervecero.service.interfaces.usuario;


import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con los usuarios.
 */
public interface IUsuarioServicio {
    /**
     * Obtiene una página de usuarios.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de usuarios.
     */
    Page<UsuarioResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene un usuario por su ID.
     *
     * @param id El ID del usuario.
     * @return El usuario correspondiente al ID.
     */
    UsuarioResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo usuario.
     *
     * @param usuarioFormDTO Los datos del usuario a registrar.
     * @return El usuario registrado.
     */
    UsuarioResponseDTO altaUsuario(UsuarioFormDTO usuarioFormDTO);

    /**
     * Modifica un usuario existente.
     *
     * @param id El ID del usuario a modificar.
     * @param usuarioFormDTO Los nuevos datos del usuario.
     * @return El usuario modificado.
     */
    UsuarioResponseDTO modificarUsuario(Long id, UsuarioFormDTO usuarioFormDTO);

    /**
     * Elimina un usuario por su ID.
     *
     * @param id El ID del usuario a eliminar.
     * @return El usuario eliminado.
     */
    UsuarioResponseDTO bajaUsuario(Long id);
}




