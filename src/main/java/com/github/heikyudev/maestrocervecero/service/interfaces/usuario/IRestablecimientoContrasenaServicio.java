package com.github.heikyudev.maestrocervecero.service.interfaces.usuario;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.RestablecimientoContrasenaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRestablecimientoInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;

/**
 * Restablecimiento de la contraseña de un usuario que la olvidó, mediante un enlace de un solo uso enviado por correo
 * a la dirección registrada del usuario.
 */
public interface IRestablecimientoContrasenaServicio {

    /**
     * Envía, a la dirección de correo registrada del usuario activo con ese nombre de usuario, un enlace para
     * restablecer su contraseña.
     * <p>
     * Se ejecuta en un hilo aparte: quien lo invoca recibe el control de inmediato, de modo que ni el resultado ni el
     * tiempo de respuesta informan si el nombre de usuario existe. Una falla se registra en el log y nunca se propaga.
     * </p>
     * <p>
     * Precondiciones: ninguna.
     * </p>
     * <p>
     * Postcondiciones: si existe un usuario activo con ese nombre de usuario (normalizado como al guardarlo) y no se le
     * envió otro enlace en los últimos minutos configurados, los enlaces anteriores sin usar del usuario dejan de servir,
     * se registra un enlace nuevo que vence a los minutos de vigencia configurados, se registra la solicitud en la
     * bitácora y se envía el correo con el enlace. En cualquier otro caso no se modifica ningún dato ni se envía nada.
     * </p>
     *
     * @param username Nombre de usuario tal como lo escribió el usuario.
     * @param ipCliente IP desde la que se hizo el pedido, para la bitácora (el hilo aparte ya no tiene la request), o
     *                  {@code null} si no se conoce.
     */
    void solicitarRestablecimiento(String username, String ipCliente);

    /**
     * Verifica que un enlace para restablecer la contraseña sirva, antes de mostrar el formulario de la contraseña nueva.
     * <p>
     * Postcondiciones: no modifica ningún dato.
     * </p>
     *
     * @param token Token recibido en el enlace.
     * @throws EnlaceRestablecimientoInvalidoException Si el token falta o no corresponde a ningún enlace, si el enlace ya
     *         se usó, si venció o fue reemplazado por uno más nuevo, o si su usuario ya no está activo.
     */
    void validarEnlace(String token);

    /**
     * Reemplaza la contraseña del usuario del enlace por la contraseña nueva.
     * <p>
     * Precondiciones: el enlace sirve; la contraseña nueva cumple las reglas de
     * {@link com.github.heikyudev.maestrocervecero.util.method.MetodosPassword} y la confirmación coincide con ella.
     * </p>
     * <p>
     * Postcondiciones: el enlace queda usado (se reclama con una única sentencia, de modo que dos pedidos simultáneos no
     * pueden usarlo dos veces), la contraseña del usuario queda reemplazada (guardada como hash), los demás enlaces sin
     * usar del usuario dejan de servir y se registra el restablecimiento en la bitácora, todo en una misma transacción.
     * Confirmada la transacción, se cierran las sesiones abiertas del usuario.
     * </p>
     *
     * @param token Token recibido en el enlace.
     * @param formDTO Contraseña nueva y su confirmación.
     * @throws EnlaceRestablecimientoInvalidoException Si el enlace no sirve (ver {@link #validarEnlace(String)}).
     * @throws ReglaNegocioException Si la contraseña nueva no cumple las reglas o la confirmación no coincide; indica el
     *         campo del formulario afectado.
     */
    void restablecerContrasena(String token, RestablecimientoContrasenaFormDTO formDTO);
}
