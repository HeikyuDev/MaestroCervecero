package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRecordatorioRetiroServicio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proceso programado de la coordinación automática del retiro de barriles: una vez por día, desde la hora configurada,
 * envía el recordatorio a cada cliente con barriles con la devolución vencida sin solicitud de búsqueda pendiente, y
 * reintenta ese día los recordatorios cuyo correo falló o cuyo envío quedó trabado.
 * <p>
 * Corre cada {@code retiro.recordatorio.intervalo-ms} milisegundos (por defecto, un minuto) desde que termina la corrida
 * anterior. Cada corrida toma la fecha y hora una sola vez: la hora de envío y el día del recordatorio salen del mismo
 * momento, aunque la corrida cruce la medianoche. El envío del día se da por hecho al registrar su ejecución, al terminar
 * de recorrer los clientes y solo si ninguno falló: si la aplicación se detiene a mitad del envío o un cliente falla, la
 * corrida siguiente lo retoma con los clientes que todavía no tienen el recordatorio del día. Cada cliente se procesa en
 * sus propias transacciones: una falla en uno no impide los demás. Los reintentos se detienen a los
 * {@code LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO} intentos.
 * </p>
 */
@Slf4j
@Component
public class EnvioRecordatoriosRetiroProgramado {

    private final IRecordatorioRetiroServicio recordatorioRetiroServicio;
    private final EnvioRecordatoriosRetiro envioRecordatorios;
    private final Clock reloj;

    @Autowired
    public EnvioRecordatoriosRetiroProgramado(IRecordatorioRetiroServicio recordatorioRetiroServicio, EnvioRecordatoriosRetiro envioRecordatorios) {
        this(recordatorioRetiroServicio, envioRecordatorios, Clock.systemDefaultZone());
    }

    EnvioRecordatoriosRetiroProgramado(IRecordatorioRetiroServicio recordatorioRetiroServicio, EnvioRecordatoriosRetiro envioRecordatorios, Clock reloj) {
        this.recordatorioRetiroServicio = recordatorioRetiroServicio;
        this.envioRecordatorios = envioRecordatorios;
        this.reloj = reloj;
    }

    /**
     * Si ya es la hora de envío, reintenta los recordatorios del día pendientes y, si el envío del día no se completó,
     * envía los que faltan y, si ninguno falló, lo registra.
     */
    @Scheduled(fixedDelayString = "${retiro.recordatorio.intervalo-ms:60000}", initialDelayString = "${retiro.recordatorio.demora-inicial-ms:30000}")
    public void procesarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDate hoy = ahora.toLocalDate();
        if (!recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(ahora)) {
            return;
        }
        for (Long idCliente : recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)) {
            enviar(idCliente, hoy);
        }
        if (recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)) {
            return;
        }
        boolean huboFallas = false;
        for (Long idCliente : recordatorioRetiroServicio.buscarIdsClientesAInvitar(hoy)) {
            huboFallas |= !enviar(idCliente, hoy);
        }
        if (huboFallas) {
            log.warn("El envío de los recordatorios de retiro del {} queda abierto: se reintenta en la próxima corrida", hoy);
            return;
        }
        try {
            recordatorioRetiroServicio.registrarEjecucion(hoy);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar el envío de los recordatorios de retiro del {}: {}", hoy, e.getMessage(), e);
        }
    }

    /** Envía el recordatorio de un cliente; {@code false} si el proceso falló (el correo que no salió no cuenta como falla). */
    private boolean enviar(Long idCliente, LocalDate hoy) {
        try {
            envioRecordatorios.enviar(idCliente, hoy);
            return true;
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el recordatorio de retiro del cliente {}: {}", idCliente, e.getMessage(), e);
            return false;
        }
    }
}
