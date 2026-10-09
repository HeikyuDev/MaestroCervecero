package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import lombok.*;


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
     * Cantidad de usos máximos antes de requerir mantenimiento preventivo.
     */
    private Integer usosMaximosAntesMantenimiento;

    /**
     * Estado lógico del macerador (activo o dado de baja).
     */
    private Estado estado;
}
