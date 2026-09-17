package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa el envasado de un lote, es decir, el traspaso de cerveza
 * desde el fermentador hacia un barril.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class EnvasadoLoteResponseDTO {

    /**
     * Identificador único del envasado.
     */
    private Long id;

    /**
     * Cantidad de cerveza envasada, en litros.
     */
    private Double cantidadEnvasada;

    /**
     * Fecha de anulación del envasado. Nula salvo que haya sido anulado.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo de anulación del envasado. Nulo salvo que haya sido anulado.
     */
    private String motivoAnulacion;

    /**
     * Estado del envasado (REGISTRADO o ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Etapa de lote sobre la que se registró este envasado.
     */
    private EtapaLoteResponseDTO etapaLote;

    /**
     * Barril al que se traspasó la cerveza.
     */
    private BarrilResponseDTO barril;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró el envasado.
     */
    private String createdBy;

    /**
     * Fecha de creación del envasado.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el envasado por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del envasado.
     */
    private LocalDateTime lastModifiedDate;
}
