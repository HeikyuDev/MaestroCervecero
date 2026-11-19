package com.github.heikyudev.maestrocervecero.service.response_dto.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta con un barril que el cliente tiene que devolver, tal como lo ve en la pantalla pública del retiro.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarrilRetiroResponseDTO {

    /** Número de serie del barril, con el que el cliente lo identifica. */
    private String numeroSerie;

    /** Identificador interno del barril (por ejemplo, BRL-01). */
    private String identificadorInterno;

    /** Fecha de devolución estimada del despacho. */
    private LocalDate fechaDevolucionEstimada;

    /**
     * Estado de la solicitud de búsqueda del barril (pendiente de retiro, retirado o cancelado porque se anuló el
     * despacho). Solo se informa en un retiro registrado; nulo en los barriles pendientes de coordinar.
     */
    private EstadoSolicitudBusqueda estado;
}
