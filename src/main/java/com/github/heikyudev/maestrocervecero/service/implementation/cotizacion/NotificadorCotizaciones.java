package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

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
 * Correos del proceso de generación automática de planes de órdenes de compra: la invitación a cada proveedor, con el
 * enlace a su formulario de cotización, y los avisos a los gerentes de compras activos, con el enlace a la solicitud.
 * <p>
 * La invitación se envía en el hilo de quien la invoca e informa si el servidor de correo la aceptó, para que el proceso
 * guarde el resultado y reintente las fallidas. Los avisos se envían en un hilo aparte: una falla se registra en el log
 * y nunca llega a quien los dispara; quien llama los invoca recién cuando su transacción se confirmó.
 * </p>
 */
@Component
public class NotificadorCotizaciones {

    static final String PLANTILLA_INVITACION = "correo/solicitud-cotizacion";
    static final String PLANTILLA_AVISO = "correo/aviso-solicitud-cotizacion";
    static final String ASUNTO_INVITACION = "Solicitud de cotización de Maestro Cervecero";
    static final String RUTA_COTIZACION = "/cotizacion/";
    static final String RUTA_SOLICITUD = "/solicitudes-cotizacion/";
    static final String RUTA_LISTADO = "/solicitudes-cotizacion";

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ICorreoServicio correoServicio;
    private final IUsuarioRepository usuarioRepository;
    private final String urlBase;

    public NotificadorCotizaciones(ICorreoServicio correoServicio,
                                   IUsuarioRepository usuarioRepository,
                                   @Value("${app.url-base}") String urlBase) {
        this.correoServicio = correoServicio;
        this.usuarioRepository = usuarioRepository;
        this.urlBase = urlBase;
    }

    /**
     * Envía a un proveedor la invitación a cotizar, con el enlace a su formulario.
     *
     * @param correo Dirección de correo del proveedor.
     * @param razonSocial Razón social del proveedor.
     * @param token Token del enlace (sin hashear).
     * @param fechaLimite Momento hasta el que puede responder.
     * @param insumos Nombres de los insumos solicitados que ofrece.
     * @return {@code true} si el servidor de correo aceptó el correo.
     */
    public boolean invitarProveedor(String correo, String razonSocial, String token, LocalDateTime fechaLimite, List<String> insumos) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("razonSocial", razonSocial);
        variables.put("enlace", armarEnlace(RUTA_COTIZACION + token));
        variables.put("fechaLimite", fechaLimite.format(FORMATO_FECHA_HORA));
        variables.put("insumos", insumos);
        return correoServicio.intentarEnviarCorreoHtml(correo, ASUNTO_INVITACION, PLANTILLA_INVITACION, variables);
    }

    /**
     * Avisa a cada gerente de compras activo una novedad de una solicitud de cotización, con el enlace a la solicitud
     * (o al listado de solicitudes, si la novedad es que no se pudo generar una).
     *
     * @param idSolicitud ID de la solicitud, o {@code null} si no se generó ninguna.
     * @param asunto Asunto del correo.
     * @param mensaje Explicación de la novedad.
     * @param detalles Datos complementarios (uno por renglón), o vacío.
     */
    public void avisarGerentesDeCompras(Long idSolicitud, String asunto, String mensaje, List<String> detalles) {
        List<UsuarioEntity> gerentes = usuarioRepository.findByRolAndEstado(Rol.GERENTE_DE_COMPRAS, Estado.ACTIVO);
        String enlace = armarEnlace(idSolicitud == null ? RUTA_LISTADO : RUTA_SOLICITUD + idSolicitud);
        for (UsuarioEntity gerente : gerentes) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("nombre", gerente.getNombre());
            variables.put("titulo", asunto);
            variables.put("mensaje", mensaje);
            variables.put("detalles", detalles);
            variables.put("enlace", enlace);
            correoServicio.enviarCorreoHtml(gerente.getCorreo(), asunto, PLANTILLA_AVISO, variables);
        }
    }

    private String armarEnlace(String ruta) {
        return UriComponentsBuilder.fromUriString(urlBase).path(ruta).build().toUriString();
    }
}
