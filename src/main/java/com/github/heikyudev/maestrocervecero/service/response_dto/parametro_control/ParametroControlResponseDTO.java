package com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de un parametro de control.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ParametroControlResponseDTO {

    /**
     * Identificador único (clave primaria) del parámetro de control, generado automáticamente por la base de datos.
     */
    private Long id;

    /**
     * Nombre del parámetro de control.
     */
    private String nombre;


    /**
     * Descripción del parámetro de control (Puede ser null).
     */
    private String descripcion;

    /**
     * Valor mínimo aceptable para el parámetro de control.
     */
    private Double valorMinimo;

    /**
     * Valor máximo aceptable para el parámetro de control.
     */
    private Double valorMaximo;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el fermentador.
     */
    private String createdBy;

    /**
     * Fecha de creación del fermentador.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el fermentador por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del fermentador.
     */
    private LocalDateTime lastModifiedDate;
}
