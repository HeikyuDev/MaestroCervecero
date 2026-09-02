package com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente;

import lombok.*;

/**
 * DTO de formulario para el alta y la modificación de un cliente.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteFormDTO {

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
     * ID de la localidad a la que pertenece el cliente.
     */
    private Long idLocalidad;
}
