package com.github.heikyudev.maestrocervecero.service.interfaces.usuario;


import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con los usuarios.
 */
public interface IUsuarioServicio {
    /**
     * Filtra los usuarios activos, opcionalmente por nombre, correo electrónico, username y/o rol.
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param correo Texto a buscar dentro del correo electrónico, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del nombre de usuario, o {@code null} para no filtrar por él.
     * @param rol El rol exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de usuarios activos en formato DTO que cumplen los criterios indicados.
     */
    Page<UsuarioResponseDTO> filtrarUsuarios(String nombre, String correo, String username, Rol rol, Pageable pageable);

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




