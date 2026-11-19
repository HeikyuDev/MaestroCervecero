package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.correo.ICorreoServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Correos de la coordinación automática del retiro de barriles: el recordatorio diario a cada cliente, con el enlace a la
 * pantalla donde propone el retiro, y el aviso a los encargados de distribución activos cuando un cliente lo coordina.
 * <p>
 * El recordatorio se envía en el hilo de quien lo invoca e informa si el servidor de correo lo aceptó, para que el
 * proceso guarde el resultado y reintente los fallidos. Los avisos se envían en un hilo aparte: una falla se registra en
 * el log y nunca llega a quien los dispara; quien llama los invoca recién cuando su transacción se confirmó.
 * </p>
 */
@Component
public class NotificadorRetiroBarriles {

    static final String PLANTILLA_RECORDATORIO = "correo/recordatorio-retiro-barriles";
    static final String PLANTILLA_AVISO = "correo/aviso-solicitud-retiro";
    static final String ASUNTO_RECORDATORIO = "Coordinación del retiro de barriles de Maestro Cervecero";
    static final String ASUNTO_AVISO = "Retiro de barriles coordinado por un cliente";
    static final String RUTA_RETIRO = "/retiro-barriles/";
    static final String RUTA_LISTADO = "/solicitudes-retiro";

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ICorreoServicio correoServicio;
    private final IUsuarioRepository usuarioRepository;
    private final String urlBase;

    public NotificadorRetiroBarriles(ICorreoServicio correoServicio,
                                     IUsuarioRepository usuarioRepository,
                                     @Value("${app.url-base}") String urlBase) {
        this.correoServicio = correoServicio;
        this.usuarioRepository = usuarioRepository;
        this.urlBase = urlBase;
    }

    /**
     * Envía a un cliente el recordatorio de sus barriles con la devolución vencida, con el enlace para coordinar el
     * retiro.
     *
     * @param correo Dirección de correo del cliente.
     * @param nombreCliente Nombre del cliente.
     * @param token Token del enlace (sin hashear).
     * @param fechaExpiracion Momento hasta el que sirve el enlace.
     * @param barriles Descripción de cada barril (número de serie e identificador interno).
     * @return {@code true} si el servidor de correo aceptó el correo.
     */
    public boolean recordarRetiro(String correo, String nombreCliente, String token, LocalDateTime fechaExpiracion, List<String> barriles) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("nombreCliente", nombreCliente);
        variables.put("enlace", armarEnlace(RUTA_RETIRO + token));
        variables.put("fechaExpiracion", fechaExpiracion.format(FORMATO_FECHA_HORA));
        variables.put("barriles", barriles);
        return correoServicio.intentarEnviarCorreoHtml(correo, ASUNTO_RECORDATORIO, PLANTILLA_RECORDATORIO, variables);
    }

    /**
     * Avisa a cada encargado de distribución activo que un cliente coordinó el retiro de sus barriles, con el enlace al
     * listado de solicitudes de retiro.
     *
     * @param nombreCliente Nombre del cliente.
     * @param barriles Descripción de cada barril incluido en el retiro.
     * @param fechaRetiro Fecha y hora propuesta por el cliente.
     * @param observaciones Observaciones del cliente, o {@code null}.
     */
    public void avisarEncargadosDeDistribucion(String nombreCliente, List<String> barriles, LocalDateTime fechaRetiro, String observaciones) {
        List<UsuarioEntity> encargados = usuarioRepository.findByRolAndEstado(Rol.ENCARGADO_DE_DISTRIBUCION, Estado.ACTIVO);
        String enlace = armarEnlace(RUTA_LISTADO);
        for (UsuarioEntity encargado : encargados) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nombre", encargado.getNombre());
            variables.put("nombreCliente", nombreCliente);
            variables.put("barriles", barriles);
            variables.put("fechaRetiro", fechaRetiro.format(FORMATO_FECHA_HORA));
            variables.put("observaciones", observaciones);
            variables.put("enlace", enlace);
            correoServicio.enviarCorreoHtml(encargado.getCorreo(), ASUNTO_AVISO, PLANTILLA_AVISO, variables);
        }
    }

    private String armarEnlace(String ruta) {
        return UriComponentsBuilder.fromUriString(urlBase).path(ruta).build().toUriString();
    }
}
