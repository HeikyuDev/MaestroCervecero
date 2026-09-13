package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa el consumo efectivo de un insumo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ConsumoInsumoResponseDTO {

    /**
     * Identificador único del consumo.
     */
    private Long id;

    /**
     * Cantidad efectivamente consumida del insumo.
     */
    private Double cantidadConsumida;

    /**
     * Costo unitario promedio ponderado (PPP) del lote de insumo, congelado al momento de
     * registrar el consumo.
     */
    private BigDecimal costoUnitarioPPP;

    /**
     * Fecha de anulación del consumo. Nula salvo que haya sido anulado.
     */
    private LocalDateTime fechaAnulacion;

    /**
     * Motivo de anulación del consumo. Nulo salvo que haya sido anulado.
     */
    private String motivoAnulacion;

    /**
     * Estado del consumo (REGISTRADO o ANULADO).
     */
    private EstadoTransaccion estado;

    /**
     * Camino por el que se registró el consumo (RESERVADO o DIRECTO): determina qué se revierte
     * si el consumo se anula.
     */
    private TipoConsumo tipoConsumo;

    /**
     * Etapa de lote sobre la que se registró este consumo.
     */
    private EtapaLoteResponseDTO etapaLote;

    /**
     * Lote de insumo del que se descontó este consumo.
     */
    private LoteInsumoResponseDTO loteInsumo;

    // === AUDITABLE ENTITY ===

    /**
     * Nombre del usuario que registró el consumo.
     */
    private String createdBy;

    /**
     * Fecha de creación del consumo.
     */
    private LocalDateTime createdDate;

    /**
     * Nombre del usuario que modificó el consumo por última vez.
     */
    private String lastModifiedBy;

    /**
     * Fecha de la última modificación del consumo.
     */
    private LocalDateTime lastModifiedDate;
}
