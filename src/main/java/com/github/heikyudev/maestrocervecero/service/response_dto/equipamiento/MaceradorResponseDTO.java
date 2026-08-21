package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de un Macerador.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MaceradorResponseDTO {

    /**
     * Identificador único del macerador.
     */
    private Long id;

    /**
     * Código de identificación interno del equipamiento en la fábrica.
     */
    private String identificadorInterno;

    /**
     * Características específicas u otra información relevante sobre el macerador.
     */
    private String descripcion;

    /**
     * Estado operativo actual del macerador.
     * - DISPONIBLE: Listo para ser utilizado en un nuevo lote.
     * - EN_LIMPIEZA: Requiere limpieza para volver a estar disponible.
     * - OCUPADO: Está siendo utilizado en un lote de producción.
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Capacidad total de líquido que puede contener el macerador, medida en litros.
     */
    private Double capacidadTotal;

    /**
     * Capacidad real de líquido que se puede utilizar en el proceso de maceración, medida en litros.
     */
    private Double capacidadUtil;

    /**
     * Volumen de líquido que queda por debajo del grifo y no se puede drenar, medido en litros.
     */
    private Double espacioMuerto;

    /**
     * Eficiencia del proceso de maceración, representada como un porcentaje (0-100).
     */
    private Double eficienciaMaceracion;

    /**
     * Estado lógico del macerador (activo o dado de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó el macerador.
     */
    private String createdBy;

    /**
     * Fecha de creación del macerador.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el macerador por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del macerador.
     */
    private LocalDateTime lastModifiedDate;
}
