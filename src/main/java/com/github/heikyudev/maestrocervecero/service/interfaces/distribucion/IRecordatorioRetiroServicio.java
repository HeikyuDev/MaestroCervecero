package com.github.heikyudev.maestrocervecero.service.interfaces.distribucion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define los servicios del envío diario de los recordatorios de retiro de barriles, que usa el proceso
 * programado.
 * <p>
 * Un día corresponde enviar desde la hora configurada; el envío del día se da por hecho al registrar su ejecución, que
 * sobrevive a los reinicios. Cada cliente recibe a lo sumo un recordatorio por día, con todos sus barriles con la
 * devolución vencida que no tienen una solicitud de búsqueda pendiente. Cada recordatorio se prepara, se envía y se
 * registra su resultado por separado: el correo se envía sin una transacción abierta.
 * </p>
 */
public interface IRecordatorioRetiroServicio {

    /**
     * Indica si ya es la hora de enviar los recordatorios del día del momento indicado.
     *
     * @param ahora Momento de referencia.
     * @return {@code true} si la hora de {@code ahora} es igual o posterior a la hora de envío configurada.
     */
    boolean alcanzoLaHoraDeEnvio(LocalDateTime ahora);

    /**
     * Indica si ya se completó el envío de los recordatorios del día indicado.
     *
     * @param fecha El día.
     * @return {@code true} si ese día ya tiene registrada su ejecución.
     */
    boolean seEnviaronLosRecordatorios(LocalDate fecha);

    /**
     * Los IDs de los clientes activos que todavía no recibieron el recordatorio del día y tienen algún barril con la
     * devolución vencida sin solicitud de búsqueda pendiente, de menor a mayor.
     *
     * @param fecha El día del recordatorio.
     * @return Los IDs de los clientes a los que corresponde enviarles el recordatorio.
     */
    List<Long> buscarIdsClientesAInvitar(LocalDate fecha);

    /**
     * Los IDs de los clientes cuyo recordatorio del día no se envió todavía, falló o quedó trabado en envío (más de
     * {@code LimitesTecnicos.ENVIO_RECORDATORIO_RETIRO_TRABADO_MINUTOS} minutos), hasta
     * {@code LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO} intentos.
     *
     * @param fecha El día del recordatorio.
     * @return Los IDs de los clientes con el envío del día pendiente.
     */
    List<Long> buscarIdsClientesConEnviosPendientes(LocalDate fecha);

    /**
     * Prepara el recordatorio del día de un cliente, en su propia transacción: lo crea si no existe (con los despachos
     * vencidos sin solicitud pendiente) o retoma el que falló, guarda el hash de un token nuevo, suma un intento y lo deja
     * en envío. La fecha de expiración del enlace es el momento de la preparación más los días máximos de anticipación
     * configurados.
     * <p>
     * Postcondiciones: si devuelve un envío, el recordatorio queda {@code EN_ENVIO} con el hash del token devuelto. No
     * devuelve nada si el cliente no existe o no está activo, si ya no tiene barriles vencidos sin solicitud pendiente
     * (un recordatorio que falló queda {@code OMITIDA}), o si el recordatorio del día ya se envió, está en envío o agotó
     * sus intentos. Un recordatorio trabado en envío se retoma como uno fallido.
     * </p>
     *
     * @param idCliente El ID del cliente.
     * @param fecha El día del recordatorio.
     * @return El correo listo para enviar, o vacío si no corresponde enviarlo.
     */
    Optional<EnvioRecordatorioRetiroPreparado> prepararRecordatorio(Long idCliente, LocalDate fecha);

    /**
     * Registra, en su propia transacción, cómo terminó el envío de un recordatorio: {@code ENVIADA} si el servidor de
     * correo lo aceptó y {@code FALLIDA} si no. Un recordatorio que ya no está en envío no cambia.
     *
     * @param idInvitacion El ID del recordatorio.
     * @param enviado Si el servidor de correo aceptó el correo.
     */
    void registrarResultadoEnvio(Long idInvitacion, boolean enviado);

    /**
     * Registra, en su propia transacción, que se completó el envío de los recordatorios del día. Registrarlo dos veces,
     * aunque sea a la vez desde dos procesos, no tiene efecto ni falla.
     *
     * @param fecha El día.
     */
    void registrarEjecucion(LocalDate fecha);
}
