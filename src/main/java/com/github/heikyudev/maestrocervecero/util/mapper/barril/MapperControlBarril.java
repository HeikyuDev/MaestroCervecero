package com.github.heikyudev.maestrocervecero.util.mapper.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DevolucionBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FraccionamientoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.OperacionCicloVidaBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ActividadBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.TipoActividadBarril;

import java.time.LocalDateTime;

/**
 * Mapea las entidades del control de barriles (barril, operaciones de su ciclo de vida y envasados) a los DTO de
 * respuesta de las pantallas de control.
 */
public class MapperControlBarril {

    private MapperControlBarril() {
    }

    /**
     * Mapea un barril, con sus usos, a {@link BarrilControlResponseDTO}.
     *
     * @param barril Barril a convertir, con su fabricante cargado.
     * @param usosDesdeUltimoMantenimiento Cantidad de usos desde el último mantenimiento.
     * @return El DTO, o {@code null} si el barril es nulo.
     */
    public static BarrilControlResponseDTO toControlDTO(BarrilEntity barril, long usosDesdeUltimoMantenimiento) {
        if (barril == null) {
            return null;
        }
        return BarrilControlResponseDTO.builder()
                .id(barril.getId())
                .identificadorInterno(barril.getIdentificadorInterno())
                .fabricante(barril.getFabricante().getNombre())
                .capacidad(barril.getCapacidad())
                .contenidoActual(barril.getContenidoActual())
                .estadoOperativo(barril.getEstadoOperativo())
                .usosDesdeUltimoMantenimiento(usosDesdeUltimoMantenimiento)
                .usosMaximosAntesMantenimiento(barril.getUsosMaximosAntesMantenimiento())
                .build();
    }

    /**
     * Mapea un barril, con sus usos y las fechas de su última limpieza y último mantenimiento, a
     * {@link BarrilResumenResponseDTO}.
     */
    public static BarrilResumenResponseDTO toResumenDTO(BarrilEntity barril, long usosDesdeUltimoMantenimiento,
                                                        LocalDateTime fechaUltimaLimpieza, LocalDateTime fechaUltimoMantenimiento) {
        if (barril == null) {
            return null;
        }
        return BarrilResumenResponseDTO.builder()
                .id(barril.getId())
                .identificadorInterno(barril.getIdentificadorInterno())
                .numeroSerie(barril.getNumeroSerie())
                .fabricante(barril.getFabricante().getNombre())
                .capacidad(barril.getCapacidad())
                .contenidoActual(barril.getContenidoActual())
                .estadoOperativo(barril.getEstadoOperativo())
                .usosDesdeUltimoMantenimiento(usosDesdeUltimoMantenimiento)
                .usosMaximosAntesMantenimiento(barril.getUsosMaximosAntesMantenimiento())
                .fechaUltimaLimpieza(fechaUltimaLimpieza)
                .fechaUltimoMantenimiento(fechaUltimoMantenimiento)
                .build();
    }

    /**
     * Mapea una operación del ciclo de vida a {@link OperacionBarrilResponseDTO}.
     *
     * @param operacion Operación a convertir (no un proxy; un despacho con su cliente cargado).
     * @param anulable {@code true} si es la operación que admite anulación.
     */
    public static OperacionBarrilResponseDTO toOperacionDTO(OperacionCicloVidaBarril operacion, boolean anulable) {
        if (operacion == null) {
            return null;
        }
        OperacionBarrilResponseDTO.OperacionBarrilResponseDTOBuilder builder = OperacionBarrilResponseDTO.builder()
                .id(operacion.getId())
                .tipo(TipoOperacionBarril.deOperacion(operacion))
                .estado(operacion.getEstado())
                .fecha(operacion.getFecha())
                .observaciones(operacion.getObservaciones())
                .registradoPor(operacion.getCreatedBy())
                .fechaRegistro(operacion.getCreatedDate())
                .fechaAnulacion(operacion.getFechaAnulacion())
                .motivoAnulacion(operacion.getMotivoAnulacion())
                .anulable(anulable);
        if (operacion instanceof LimpiezaBarrilEntity limpieza) {
            builder.estadoOperativoResultante(limpieza.getEstadoOperativoResultante());
        } else if (operacion instanceof FraccionamientoBarrilEntity fraccionamiento) {
            builder.estadoOperativoResultante(fraccionamiento.getEstadoOperativoResultante())
                    .cantidadExtraida(fraccionamiento.getCantidadFraccionada());
        } else if (operacion instanceof DespachoBarrilEntity despacho) {
            builder.cliente(despacho.getCliente().getNombre())
                    .fechaDevolucionEstimada(despacho.getFechaDevolucionEstimada());
        } else if (operacion instanceof DevolucionBarrilEntity devolucion) {
            builder.litrosDescartados(devolucion.getLitrosDescartados());
        }
        return builder.build();
    }

    /**
     * Mapea un envasado registrado a una fila del historial. El estado resultante es En uso si el lote sigue en
     * ejecución y Con cerveza si ya está finalizado.
     *
     * @param envasado Envasado a convertir, con su etapa y su lote cargados.
     */
    public static ActividadBarrilResponseDTO toActividadDTO(EnvasadoLoteEntity envasado) {
        if (envasado == null) {
            return null;
        }
        EstadoLote estadoLote = envasado.getEtapaLote().getLote().getEstado();
        EstadoOperativoBarril resultante = switch (estadoLote) {
            case EN_EJECUCION -> EstadoOperativoBarril.EN_USO;
            case FINALIZADO -> EstadoOperativoBarril.CON_CERVEZA;
            default -> null;
        };
        return ActividadBarrilResponseDTO.builder()
                .tipo(TipoActividadBarril.ENVASADO)
                .idReferencia(envasado.getId())
                .idLote(envasado.getEtapaLote().getLote().getId())
                .codigoLote(envasado.getEtapaLote().getLote().getIdentificadorInterno())
                .fecha(envasado.getFechaEnvasado())
                .estadoOperativoResultante(resultante)
                .build();
    }

    /** Mapea una operación registrada a una fila del historial. */
    public static ActividadBarrilResponseDTO toActividadDTO(OperacionCicloVidaBarril operacion) {
        if (operacion == null) {
            return null;
        }
        TipoOperacionBarril tipo = TipoOperacionBarril.deOperacion(operacion);
        EstadoOperativoBarril resultante = switch (tipo) {
            case LIMPIEZA -> ((LimpiezaBarrilEntity) operacion).getEstadoOperativoResultante();
            case FALLA -> EstadoOperativoBarril.EN_MANTENIMIENTO;
            case FRACCIONAMIENTO -> ((FraccionamientoBarrilEntity) operacion).getEstadoOperativoResultante();
            case MANTENIMIENTO -> EstadoOperativoBarril.DISPONIBLE;
            case DESPACHO -> EstadoOperativoBarril.DESPACHADO;
            case DEVOLUCION -> EstadoOperativoBarril.EN_LIMPIEZA;
        };
        return ActividadBarrilResponseDTO.builder()
                .tipo(TipoActividadBarril.valueOf(tipo.name()))
                .idReferencia(operacion.getId())
                .observaciones(operacion.getObservaciones())
                .fecha(operacion.getFecha())
                .estadoOperativoResultante(resultante)
                .build();
    }
}
