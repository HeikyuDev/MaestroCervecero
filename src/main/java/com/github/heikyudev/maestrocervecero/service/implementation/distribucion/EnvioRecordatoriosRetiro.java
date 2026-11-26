package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.EnvioRecordatorioRetiroPreparado;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRecordatorioRetiroServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Envío del recordatorio de retiro de un cliente sin tener una transacción abierta mientras se habla con el servidor de
 * correo: primero el service guarda el token del intento (transacción corta), después se envía el correo y por último el
 * service registra el resultado (otra transacción corta). Un correo que falla queda para que lo reintente el proceso
 * programado ese mismo día.
 * <p>
 * Si otro proceso creó a la vez el recordatorio del día del mismo cliente, la unicidad de cliente y día rechaza el
 * segundo: ese envío queda a cargo del otro proceso y no cuenta como falla.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioRecordatoriosRetiro {

    /** Restricción que impide un segundo recordatorio del mismo cliente el mismo día. */
    static final String RESTRICCION_RECORDATORIO_DEL_DIA = "uk_invitacion_retiro_barriles_cliente_fecha";

    private final IRecordatorioRetiroServicio recordatorioRetiroServicio;
    private final NotificadorRetiroBarriles notificador;

    /** Cómo terminó el envío del recordatorio del día de un cliente. */
    public enum Resultado {
        /** El servidor de correo aceptó el recordatorio. */
        ENVIADO,
        /** No correspondía enviarlo (nada que recordar, ya enviado, en envío o sin intentos) o lo envía otro proceso. */
        SIN_ENVIO,
        /** El correo no salió: queda fallido para reintentarlo. */
        FALLIDO
    }

    /**
     * Prepara, envía y registra el recordatorio del día de un cliente, si corresponde enviarlo.
     *
     * @param idCliente ID del cliente.
     * @param fecha Día del recordatorio.
     * @return {@link Resultado#ENVIADO} si el servidor de correo aceptó el recordatorio, {@link Resultado#FALLIDO} si el
     * correo no salió y {@link Resultado#SIN_ENVIO} si no correspondía enviarlo.
     */
    public Resultado enviar(Long idCliente, LocalDate fecha) {
        Optional<EnvioRecordatorioRetiroPreparado> preparado;
        try {
            preparado = recordatorioRetiroServicio.prepararRecordatorio(idCliente, fecha);
        } catch (DataIntegrityViolationException e) {
            if (!esRecordatorioDelDiaDuplicado(e)) {
                throw e;
            }
            log.info("El recordatorio de retiro del {} del cliente {} ya lo prepara otro proceso", fecha, idCliente);
            return Resultado.SIN_ENVIO;
        }
        if (preparado.isEmpty()) {
            return Resultado.SIN_ENVIO;
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
        return enviado ? Resultado.ENVIADO : Resultado.FALLIDO;
    }

    /** Si la violación de integridad es la de un segundo recordatorio del mismo cliente el mismo día. */
    private static boolean esRecordatorioDelDiaDuplicado(DataIntegrityViolationException e) {
        String detalle = e.getMostSpecificCause().getMessage();
        return detalle != null && detalle.contains(RESTRICCION_RECORDATORIO_DEL_DIA);
    }
}
