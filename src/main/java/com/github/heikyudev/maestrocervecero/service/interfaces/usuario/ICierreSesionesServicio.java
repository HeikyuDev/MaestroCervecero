package com.github.heikyudev.maestrocervecero.service.interfaces.usuario;

/**
 * Cierre de las sesiones abiertas de un usuario, por ejemplo después de restablecer su contraseña: quien haya iniciado
 * sesión con la contraseña anterior deja de tener acceso.
 */
public interface ICierreSesionesServicio {

    /**
     * Hace vencer todas las sesiones activas del usuario indicado.
     * <p>
     * Postcondiciones: cada sesión del usuario queda vencida y su próximo pedido vuelve a la pantalla de inicio de
     * sesión con el aviso de que la contraseña fue restablecida. Sin sesiones activas, no hace nada.
     * </p>
     *
     * @param username Nombre de usuario, tal como quedó registrado.
     */
    void cerrarSesionesDe(String username);
}
