package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.EnvioInvitacionPreparado;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Envío de los correos de invitación de una solicitud de cotización sin tener la solicitud bloqueada ni una transacción
 * abierta mientras se habla con el servidor de correo: primero el service guarda el token de cada intento (transacción
 * corta), después se envía cada correo y por último el service registra los resultados (otra transacción corta). Un
 * correo que falla queda para que lo reintente el proceso programado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioInvitacionesCotizacion {

    private final ISolicitudCotizacionServicio solicitudCotizacionServicio;
    private final NotificadorCotizaciones notificador;

    /**
     * Envía los correos pendientes o fallidos de la solicitud y registra cómo terminó cada uno.
     *
     * @param idSolicitud ID de la solicitud.
     */
    public void enviarPendientes(Long idSolicitud) {
        List<EnvioInvitacionPreparado> preparados = solicitudCotizacionServicio.prepararEnvios(idSolicitud);
        if (preparados.isEmpty()) {
            return;
        }
        Map<Long, Boolean> resultados = new LinkedHashMap<>();
        for (EnvioInvitacionPreparado envio : preparados) {
            boolean enviada;
            try {
                enviada = notificador.invitarProveedor(envio.correo(), envio.razonSocial(), envio.token(), envio.fechaLimite(), envio.insumos());
            } catch (RuntimeException e) {
                log.error("No se pudo enviar la invitación {} de la solicitud de cotización {}: {}", envio.idInvitacion(), idSolicitud, e.getMessage());
                enviada = false;
            }
            resultados.put(envio.idInvitacion(), enviada);
        }
        solicitudCotizacionServicio.registrarResultadosEnvio(idSolicitud, resultados);
    }
}
