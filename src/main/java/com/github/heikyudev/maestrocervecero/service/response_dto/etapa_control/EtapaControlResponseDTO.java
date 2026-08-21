package com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de una etapa de control.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EtapaControlResponseDTO {

    /**
     * Identificador único (clave primaria) de la etapa de control, generado automáticamente por la base de datos.
     */
    private Long id;

    /**
     * Nombre de la etapa de control.
     */
    private String nombre;

    /**
     * Descripción de la etapa de control (Puede ser null).
     */
    private String descripcion;

    /**
     * Tipo de etapa que se controla (Maceración, Hervidor, Fermentación, Maduración).
     */
    private TipoEtapa etapaAControlar;

    /**
     * Estado lógico de la etapa de control (activa o dada de baja).
     */
    private Estado estado;

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
