package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos de un barril para la lista de control: su fabricante, su capacidad, su contenido, su
 * estado y cuánto se usó desde el último mantenimiento.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilControlResponseDTO {

    /** Identificador único del barril. */
    private Long id;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Nombre del fabricante del barril. */
    private String fabricante;

    /** Capacidad total del barril, en litros. */
    private Double capacidad;

    /** Contenido actual del barril, en litros. */
    private Double contenidoActual;

    /** Estado operativo actual del barril. */
    private EstadoOperativoBarril estadoOperativo;

    /**
     * Cantidad de usos desde el último mantenimiento: limpiezas registradas posteriores a su último mantenimiento
     * registrado (o todas, si nunca tuvo uno).
     */
    private Long usosDesdeUltimoMantenimiento;

    /** Cantidad máxima de usos antes de requerir mantenimiento preventivo. */
    private Integer usosMaximosAntesMantenimiento;
}
