package com.github.heikyudev.maestrocervecero.service.response_dto.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * DTO de respuesta que representa los datos de un Barril.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class BarrilResponseDTO {

    /**
     * Identificador único del barril.
     */
    private Long id;

    /**
     * Identificador interno del barril, generado por el sistema (por ejemplo BRL-01).
     */
    private String identificadorInterno;

    /**
     * Número de serie del barril, tal como lo ingresó el usuario.
     */
    private String numeroSerie;

    /**
     * Capacidad total del barril en litros.
     */
    private Double capacidad;

    /**
     * Contenido actual de cerveza en el barril, medido en litros.
     */
    private Double contenidoActual;

    /**
     * Estado operativo actual del barril.
     * - CON_CERVEZA: El barril contiene cerveza.
     * - DESPACHADO: El barril ha sido entregado a un cliente.
     * - DISPONIBLE: El barril está vacío y listo para ser utilizado.
     * - EN_LIMPIEZA: El barril se encuentra en proceso de limpieza.
     * - EN_MANTENIMIENTO: El barril está en proceso de mantenimiento y no puede ser utilizado hasta que se complete el mantenimiento.
     */
    private EstadoOperativoBarril estadoOperativo;

    /**
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;

    /**
     * Estado lógico del barril (activo o dado de baja).
     */
    private Estado estado;

    /**
     * Fabricante de barril al que pertenece este barril.
     */
    private FabricanteBarrilResponseDTO fabricante;
}
