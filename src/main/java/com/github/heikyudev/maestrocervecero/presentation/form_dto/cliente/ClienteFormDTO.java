package com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente;

import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import jakarta.validation.constraints.Pattern;
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
     * Correo electrónico del cliente. El formato es el mismo que valida el service
     * ({@link ValidacionesTexto#validarCorreo(String)}), con el mismo mensaje.
     */
    @Pattern(regexp = ValidacionesTexto.PATRON_CORREO_FORMULARIO, message = ValidacionesTexto.MENSAJE_CORREO_INVALIDO)
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
