package com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con la invitación de un proveedor a una solicitud de cotización y el estado de su respuesta.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitacionCotizacionResponseDTO {

    /**
     * Identificador de la invitación.
     */
    private Long id;

    /**
     * Identificador del proveedor.
     */
    private Long idProveedor;

    /**
     * Razón social vigente del proveedor.
     */
    private String razonSocial;

    /**
     * CUIT del proveedor, con guiones.
     */
    private String cuit;

    /**
     * {@code false} si el proveedor se dio de baja: sus ofertas no cuentan para los planes.
     */
    private boolean proveedorActivo;

    /**
     * Momento en que se envió la invitación.
     */
    private LocalDateTime fechaEnvio;

    /**
     * Momento del último envío de la respuesta, o {@code null} si no respondió.
     */
    private LocalDateTime fechaRespuesta;

    /**
     * Cantidad de insumos que cotizó.
     */
    private int cantidadOfertas;

    /**
     * Resultado del envío del correo con el enlace.
     */
    private EstadoEnvioInvitacion estadoEnvio;

    /**
     * Intentos de envío del correo.
     */
    private int intentosEnvio;

    /**
     * Situación del proveedor en la solicitud: Dado de baja, Correo fallido, Respondió, Invitado (mientras está abierta)
     * o Sin respuesta.
     */
    private String estadoParticipacion;

    /**
     * Nombres de los insumos que cotizó.
     */
    private List<String> insumosCotizados;
}
