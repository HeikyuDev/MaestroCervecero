package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.EnvioRecordatorioRetiroPreparado;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRecordatorioRetiroServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Envío del recordatorio de retiro de un cliente sin tener una transacción abierta mientras se habla con el servidor de
 * correo: primero el service guarda el token del intento (transacción corta), después se envía el correo y por último el
 * service registra el resultado (otra transacción corta). Un correo que falla queda para que lo reintente el proceso
 * programado ese mismo día.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioRecordatoriosRetiro {

    private final IRecordatorioRetiroServicio recordatorioRetiroServicio;
    private final NotificadorRetiroBarriles notificador;

    /**
     * Prepara, envía y registra el recordatorio del día de un cliente, si corresponde enviarlo.
     *
     * @param idCliente ID del cliente.
     * @param fecha Día del recordatorio.
     * @return {@code true} si el servidor de correo aceptó el recordatorio; {@code false} si no correspondía enviarlo o
     * si el correo no salió.
     */
    public boolean enviar(Long idCliente, LocalDate fecha) {
        Optional<EnvioRecordatorioRetiroPreparado> preparado = recordatorioRetiroServicio.prepararRecordatorio(idCliente, fecha);
        if (preparado.isEmpty()) {
            return false;
        }
        EnvioRecordatorioRetiroPreparado envio = preparado.get();
        boolean enviado;
        try {
            enviado = notificador.recordarRetiro(envio.correo(), envio.nombreCliente(), envio.token(), envio.fechaExpiracion(), envio.barriles());
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el recordatorio de retiro {} del cliente {}: {}", envio.idInvitacion(), idCliente, e.getMessage());
            enviado = false;
        }
        recordatorioRetiroServicio.registrarResultadoEnvio(envio.idInvitacion(), enviado);
        return enviado;
    }
}
