package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.RetiroPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilListoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ClienteConBarrilesResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DevolucionPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCronograma;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mapea los despachos, las solicitudes de retiro pendientes, las filas agrupadas por cliente y los barriles de la pantalla de control a los DTO de respuesta del
 * panel del encargado de distribución. No ejecuta consultas: lee solo de las relaciones ya cargadas.
 */
public class MapperDashboardEncargadoDistribucion {

    private MapperDashboardEncargadoDistribucion() {
    }

    /**
     * Mapea un despacho vigente (con su barril y su cliente cargados) a {@link DevolucionPendienteResponseDTO}.
     *
     * @param despacho Despacho a convertir.
     * @param hoy La fecha de hoy, con la que se calculan los días de atraso (0 si la devolución vence hoy).
     * @return El DTO, o {@code null} si el despacho es nulo.
     */
    public static DevolucionPendienteResponseDTO toDevolucionPendienteDTO(DespachoBarrilEntity despacho, LocalDate hoy) {
        if (despacho == null) {
            return null;
        }
        return DevolucionPendienteResponseDTO.builder()
                .idBarril(despacho.getBarril().getId())
                .identificadorInterno(despacho.getBarril().getIdentificadorInterno())
                .nombreCliente(despacho.getCliente().getNombre())
                .fechaDevolucionEstimada(despacho.getFechaDevolucionEstimada())
                .diasDeAtraso(MetodosCronograma.calcularDiasDeAtraso(despacho.getFechaDevolucionEstimada(), hoy))
                .build();
    }

    /**
     * Mapea una solicitud de búsqueda pendiente (con su despacho, el barril y el cliente cargados) a
     * {@link RetiroPendienteResponseDTO}.
     *
     * @param solicitud Solicitud a convertir.
     * @param ahora Momento de referencia: la solicitud está vencida si su fecha de búsqueda es anterior.
     * @return El DTO, o {@code null} si la solicitud es nula.
     */
    public static RetiroPendienteResponseDTO toRetiroPendienteDTO(SolicitudBusquedaEntity solicitud, LocalDateTime ahora) {
        if (solicitud == null) {
            return null;
        }
        DespachoBarrilEntity despacho = solicitud.getDespachoBarril();
        return RetiroPendienteResponseDTO.builder()
                .idSolicitud(solicitud.getId())
                .idBarril(despacho.getBarril().getId())
                .identificadorInterno(despacho.getBarril().getIdentificadorInterno())
                .numeroSerie(despacho.getBarril().getNumeroSerie())
                .nombreCliente(despacho.getCliente().getNombre())
                .fechaBusqueda(solicitud.getFechaBusqueda())
                .vencida(solicitud.getFechaBusqueda().isBefore(ahora))
                .build();
    }

    /**
     * Mapea una fila de la consulta de clientes con barriles en su poder, {@code [idCliente, nombre, cantidad, próxima
     * fecha de devolución]}, a {@link ClienteConBarrilesResponseDTO}.
     *
     * @param fila Fila a convertir.
     * @return El DTO, o {@code null} si la fila es nula.
     */
    public static ClienteConBarrilesResponseDTO toClienteConBarrilesDTO(Object[] fila) {
        if (fila == null) {
            return null;
        }
        return ClienteConBarrilesResponseDTO.builder()
                .idCliente((Long) fila[0])
                .nombre((String) fila[1])
                .cantidadBarriles((Long) fila[2])
                .proximaFechaDevolucion((LocalDate) fila[3])
                .build();
    }

    /**
     * Mapea un barril de la pantalla de control a {@link BarrilListoResponseDTO}.
     *
     * @param barril Barril a convertir.
     * @return El DTO, o {@code null} si el barril es nulo.
     */
    public static BarrilListoResponseDTO toBarrilListoDTO(BarrilControlResponseDTO barril) {
        if (barril == null) {
            return null;
        }
        return BarrilListoResponseDTO.builder()
                .id(barril.getId())
                .identificadorInterno(barril.getIdentificadorInterno())
                .nombreReceta(barril.getContenido() != null ? barril.getContenido().nombreReceta() : null)
                .codigoLote(barril.getContenido() != null ? barril.getContenido().codigoLote() : null)
                .contenidoActual(barril.getContenidoActual())
                .build();
    }
}
