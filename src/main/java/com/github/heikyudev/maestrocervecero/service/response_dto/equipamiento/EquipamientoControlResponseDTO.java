package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta con los datos de un equipamiento para la pantalla de control: su estado, cuánto se usó desde el
 * último mantenimiento y cuál fue su última operación registrada.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipamientoControlResponseDTO {

    /**
     * Identificador único del equipamiento.
     */
    private Long id;

    /**
     * Identificador interno del equipamiento (por ejemplo, MOL-01).
     */
    private String identificadorInterno;

    /**
     * Tipo concreto del equipamiento.
     */
    private TipoEquipamiento tipo;

    /**
     * Estado operativo actual del equipamiento.
     */
    private EstadoOperativo estadoOperativo;

    /**
     * Cantidad de usos desde el último mantenimiento: limpiezas registradas que empezaron después del fin de su
     * último mantenimiento registrado (o todas, si nunca tuvo uno).
     */
    private Long usosDesdeUltimoMantenimiento;

    /**
     * Cantidad máxima de usos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;

    /**
     * Tipo de la última operación registrada, o {@code null} si el equipamiento no tiene ninguna.
     */
    private TipoOperacionEquipamiento tipoUltimaOperacion;

    /**
     * Fecha y hora de fin de la última operación registrada, o {@code null} si el equipamiento no tiene ninguna.
     */
    private LocalDateTime fechaFinUltimaOperacion;
}
