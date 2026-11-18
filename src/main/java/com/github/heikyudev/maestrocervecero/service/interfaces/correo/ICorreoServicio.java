package com.github.heikyudev.maestrocervecero.service.interfaces.correo;

import java.util.Map;

/**
 * Envío de correos electrónicos del sistema. Es el único punto por el que la aplicación envía correos, sea cual sea el
 * destinatario (usuarios, proveedores, personal de la fábrica) o el proceso que lo dispara.
 */
public interface ICorreoServicio {

    /**
     * Envía un correo HTML cuyo cuerpo es una plantilla Thymeleaf de {@code templates} completada con las variables
     * indicadas. El envío ocurre en un hilo aparte: quien lo invoca no espera al servidor de correo.
     * <p>
     * Precondiciones: la plantilla existe y solo usa las variables recibidas (no tiene la request HTTP disponible, por
     * lo que los enlaces deben llegar completos como variables).
     * </p>
     * <p>
     * Postcondiciones: el correo se entrega al servidor SMTP configurado con el remitente del sistema. Una falla (de la
     * plantilla, de la conexión o de las credenciales) se registra en el log y nunca se propaga a quien lo invocó.
     * </p>
     *
     * @param destinatario Dirección de correo del destinatario.
     * @param asunto Asunto del correo.
     * @param plantilla Nombre de la plantilla relativo a {@code templates}, sin extensión (ej. {@code correo/restablecer-contrasena}).
     * @param variables Variables que usa la plantilla.
     */
    void enviarCorreoHtml(String destinatario, String asunto, String plantilla, Map<String, Object> variables);

    /**
     * Igual que {@link #enviarCorreoHtml}, pero en el hilo de quien lo invoca, informando si el servidor de correo
     * aceptó el correo. Lo usan los procesos que guardan el resultado del envío y lo reintentan si falló.
     * <p>
     * Postcondiciones: el correo se entrega al servidor SMTP configurado, o la falla se registra en el log; nunca se
     * propaga.
     * </p>
     *
     * @param destinatario Dirección de correo del destinatario.
     * @param asunto Asunto del correo.
     * @param plantilla Nombre de la plantilla relativo a {@code templates}, sin extensión.
     * @param variables Variables que usa la plantilla.
     * @return {@code true} si el servidor aceptó el correo; {@code false} si falló la plantilla, la conexión o las credenciales.
     */
    boolean intentarEnviarCorreoHtml(String destinatario, String asunto, String plantilla, Map<String, Object> variables);
}
