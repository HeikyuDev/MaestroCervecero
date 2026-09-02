package com.github.heikyudev.maestrocervecero.service.response_dto.cliente;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para representar un cliente.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ClienteResponseDTO {

    /**
     * Identificador único del cliente.
     */
    private Long id;

    /**
     * Nombre del cliente.
     */
    private String nombre;

    /**
     * Teléfono de contacto del cliente.
     */
    private String telefono;

    /**
     * Correo electrónico del cliente.
     */
    private String email;

    /**
     * Dirección del cliente.
     */
    private String direccion;

    /**
     * Localidad a la que pertenece el cliente.
     */
    private LocalidadResponseDTO localidad;

    /**
     * Estado lógico del cliente (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el cliente.
     */
    private String createdBy;

    /**
     * Fecha de creación del cliente.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el cliente por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del cliente.
     */
    private LocalDateTime lastModifiedDate;
}
