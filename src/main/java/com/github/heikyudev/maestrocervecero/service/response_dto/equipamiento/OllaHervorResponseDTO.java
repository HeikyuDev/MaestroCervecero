package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa los datos de una olla de hervor.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class OllaHervorResponseDTO {
    /**
     * identificador de la olla de hervor. Debe ser único entre las ollas de hervor activas (case-insensitive).
     */
    private Long id;

    /**
     * identificador de la olla de hervor. Debe ser único entre las ollas de hervor activas (case-insensitive).
     */
    private String identificadorInterno;

    /***
     * Descripcion de la olla de hervor, puede ser null o vacio, no es obligatorio
     */
    private String descripcion;

    /**
     * Estado operativo de la olla de hervor (DISPONIBLE, EN LIMPIEZA, EN USO).
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Capacidad total de la olla de hervor en litros.
     */
    private Double capacidadTotal;

    /**
     * Capacidad util de la olla de hervor en litros.
     */
    private Double capacidadUtil;

    /**
     * evaporacion de la olla de hervor. Litros por Hora
     */
    private Double evaporacion;

    /**
     * Perdida por trub de la olla de hervor en Litros.
     */
    private Double perdidaPorTrub;

    /**
     * Estado lógico de la olla de hervor (activa o dada de baja).
     */
    private Estado estado;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que creó la olla de hervor.
     */
    private String createdBy;

    /**
     * Fecha de creación de la olla de hervor.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó la olla de hervor por ultima ves.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación de la olla de hervor.
     */
    private LocalDateTime lastModifiedDate;
}
