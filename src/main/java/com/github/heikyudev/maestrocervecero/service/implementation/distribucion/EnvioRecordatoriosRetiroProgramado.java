package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
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
 * <p>
 * El encargado de distribución también puede pedir el envío del día desde la pantalla de solicitudes de retiro, sin
 * esperar la hora configurada: es el mismo proceso, salvo la espera de la hora de envío.
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

    /** Mensaje con el que se rechaza un envío pedido a mano cuando el del día ya se hizo. */
    static final String MENSAJE_RECORDATORIOS_YA_ENVIADOS = "Los recordatorios de retiro de hoy ya se enviaron: los que fallaron se reintentan automáticamente";

    /**
     * Resultado de un envío pedido a mano.
     *
     * @param clientesNotificados Clientes cuyo recordatorio aceptó el servidor de correo en esta corrida.
     * @param completo {@code true} si ningún cliente falló y el envío del día quedó registrado.
     */
    public record ResultadoEnvio(int clientesNotificados, boolean completo) {
    }

    /**
     * Si ya es la hora de envío, reintenta los recordatorios del día pendientes y, si el envío del día no se completó,
     * envía los que faltan y, si ninguno falló, lo registra.
     */
    @Scheduled(fixedDelayString = "${retiro.recordatorio.intervalo-ms:60000}", initialDelayString = "${retiro.recordatorio.demora-inicial-ms:30000}")
    public void procesarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(ahora)) {
            return;
        }
        enviarRecordatoriosDelDia(ahora.toLocalDate());
    }

    /**
     * Envía ahora los recordatorios de hoy, sin esperar la hora de envío configurada: el mismo proceso que la corrida
     * programada.
     *
     * @return Cuántos clientes recibieron el recordatorio y si el envío del día quedó completo.
     * @throws ReglaNegocioException Si los recordatorios de hoy ya se enviaron.
     */
    public ResultadoEnvio enviarRecordatoriosDeHoy() {
        LocalDate hoy = LocalDate.now(reloj);
        if (recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)) {
            throw new ReglaNegocioException(MENSAJE_RECORDATORIOS_YA_ENVIADOS);
        }
        return enviarRecordatoriosDelDia(hoy);
    }

    /**
     * Envío del día, común a la corrida programada y al pedido a mano: reintenta los recordatorios pendientes y, si el
     * envío del día no se completó, envía los que faltan y, si ninguno falló, lo registra.
     */
    private ResultadoEnvio enviarRecordatoriosDelDia(LocalDate hoy) {
        int notificados = 0;
        for (Long idCliente : recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)) {
            if (enviar(idCliente, hoy) == ResultadoCliente.NOTIFICADO) {
                notificados++;
            }
        }
        if (recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)) {
            return new ResultadoEnvio(notificados, true);
        }
        boolean huboFallas = false;
        for (Long idCliente : recordatorioRetiroServicio.buscarIdsClientesAInvitar(hoy)) {
            ResultadoCliente resultado = enviar(idCliente, hoy);
            huboFallas |= resultado == ResultadoCliente.FALLO;
            if (resultado == ResultadoCliente.NOTIFICADO) {
                notificados++;
            }
        }
        if (huboFallas) {
            log.warn("El envío de los recordatorios de retiro del {} queda abierto: se reintenta en la próxima corrida", hoy);
            return new ResultadoEnvio(notificados, false);
        }
        try {
            recordatorioRetiroServicio.registrarEjecucion(hoy);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar el envío de los recordatorios de retiro del {}: {}", hoy, e.getMessage(), e);
            return new ResultadoEnvio(notificados, false);
        }
        return new ResultadoEnvio(notificados, true);
    }

    /** Resultado del envío a un cliente: el correo que no salió no cuenta como falla del proceso (se reintenta). */
    private enum ResultadoCliente { NOTIFICADO, SIN_NOTIFICAR, FALLO }

    private ResultadoCliente enviar(Long idCliente, LocalDate hoy) {
        try {
            return envioRecordatorios.enviar(idCliente, hoy) ? ResultadoCliente.NOTIFICADO : ResultadoCliente.SIN_NOTIFICAR;
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el recordatorio de retiro del cliente {}: {}", idCliente, e.getMessage(), e);
            return ResultadoCliente.FALLO;
        }
    }
}
