package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Proceso programado de las solicitudes de cotización: reintenta el envío de las invitaciones cuyo correo falló y
 * cierra las solicitudes abiertas cuyo plazo de respuesta venció, generando sus planes de órdenes de compra.
 * <p>
 * Corre cada {@code cotizacion.cierre.intervalo-ms} milisegundos (por defecto, un minuto) desde que termina la corrida
 * anterior. El estado de cada solicitud vive en la base de datos, así que una solicitud que venció con la aplicación
 * detenida se cierra en la primera corrida al volver a arrancar. Cada envío y cada cierre corren en su propia
 * transacción: una falla en uno no impide los demás. Un cierre que falla se registra como intento fallido; después de
 * {@code LimitesTecnicos.INTENTOS_CIERRE_SOLICITUD_MAXIMO} intentos la solicitud queda marcada y deja de
 * reintentarse. Los envíos dejan de reintentarse a los {@code LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CierreSolicitudesCotizacionProgramado {

    private final ISolicitudCotizacionServicio solicitudCotizacionServicio;
    private final EnvioInvitacionesCotizacion envioInvitaciones;

    /**
     * Reintenta los envíos pendientes y cierra las solicitudes vencidas, de la más antigua a la más nueva.
     */
    @Scheduled(fixedDelayString = "${cotizacion.cierre.intervalo-ms:60000}", initialDelayString = "${cotizacion.cierre.demora-inicial-ms:30000}")
    public void procesarSolicitudes() {
        for (Long idSolicitud : solicitudCotizacionServicio.buscarIdsConEnviosPendientes()) {
            try {
                envioInvitaciones.enviarPendientes(idSolicitud);
            } catch (RuntimeException e) {
                log.error("No se pudieron enviar las invitaciones de la solicitud de cotización {}: {}", idSolicitud, e.getMessage(), e);
            }
        }
        for (Long idSolicitud : solicitudCotizacionServicio.buscarIdsVencidas()) {
            try {
                solicitudCotizacionServicio.cerrarSolicitud(idSolicitud);
            } catch (RuntimeException e) {
                log.error("No se pudo cerrar la solicitud de cotización {}: {}", idSolicitud, e.getMessage(), e);
                registrarFalla(idSolicitud, e);
            }
        }
    }

    private void registrarFalla(Long idSolicitud, RuntimeException falla) {
        try {
            solicitudCotizacionServicio.registrarFallaCierre(idSolicitud, falla.getMessage());
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la falla del cierre de la solicitud de cotización {}: {}", idSolicitud, e.getMessage(), e);
        }
    }
}
