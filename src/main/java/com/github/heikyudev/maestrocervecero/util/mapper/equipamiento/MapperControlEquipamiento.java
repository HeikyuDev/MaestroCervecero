package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OperacionCicloVidaEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoOperacionEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ActividadEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoActividadEquipamiento;

import java.time.LocalDateTime;

/**
 * MapperControlEquipamiento tiene la responsabilidad de mapear las entidades del control de equipamiento (equipamiento,
 * operaciones de su ciclo de vida y etapas de lote) a los DTO de respuesta de las pantallas de control.
 */
public class MapperControlEquipamiento {

    private MapperControlEquipamiento() {
    }

    /**
     * Mapea un equipamiento, con sus usos, a {@link EquipamientoControlResponseDTO}.
     *
     * @param equipamiento Equipamiento a convertir (no un proxy).
     * @param usosDesdeUltimoMantenimiento Cantidad de usos desde el último mantenimiento.
     * @return El DTO correspondiente o {@code null} si el equipamiento es nulo.
     */
    public static EquipamientoControlResponseDTO toControlDTO(EquipamientoEntity equipamiento, long usosDesdeUltimoMantenimiento) {
        if (equipamiento == null) {
            return null;
        }
        return EquipamientoControlResponseDTO.builder()
                .id(equipamiento.getId())
                .identificadorInterno(equipamiento.getIdentificadorInterno())
                .tipo(TipoEquipamiento.deEquipamiento(equipamiento))
                .estadoOperativo(equipamiento.getEstadoOperativo())
                .usosDesdeUltimoMantenimiento(usosDesdeUltimoMantenimiento)
                .usosMaximosAntesMantenimiento(equipamiento.getUsosMaximosAntesMantenimiento())
                .build();
    }

    /**
     * Mapea un equipamiento, con sus usos y las fechas de su última limpieza y último mantenimiento, a
     * {@link EquipamientoResumenResponseDTO}, incluyendo los parámetros propios de su tipo concreto.
     *
     * @param equipamiento Equipamiento a convertir (no un proxy).
     * @param usosDesdeUltimoMantenimiento Cantidad de usos desde el último mantenimiento.
     * @param fechaUltimaLimpieza Fecha de fin de la última limpieza registrada, o {@code null}.
     * @param fechaUltimoMantenimiento Fecha de fin del último mantenimiento registrado, o {@code null}.
     * @return El DTO correspondiente o {@code null} si el equipamiento es nulo.
     */
    public static EquipamientoResumenResponseDTO toResumenDTO(EquipamientoEntity equipamiento, long usosDesdeUltimoMantenimiento,
                                                             LocalDateTime fechaUltimaLimpieza, LocalDateTime fechaUltimoMantenimiento) {
        if (equipamiento == null) {
            return null;
        }
        EquipamientoResumenResponseDTO.EquipamientoResumenResponseDTOBuilder builder = EquipamientoResumenResponseDTO.builder()
                .id(equipamiento.getId())
                .identificadorInterno(equipamiento.getIdentificadorInterno())
                .tipo(TipoEquipamiento.deEquipamiento(equipamiento))
                .estadoOperativo(equipamiento.getEstadoOperativo())
                .usosMaximosAntesMantenimiento(equipamiento.getUsosMaximosAntesMantenimiento())
                .usosDesdeUltimoMantenimiento(usosDesdeUltimoMantenimiento)
                .fechaUltimaLimpieza(fechaUltimaLimpieza)
                .fechaUltimoMantenimiento(fechaUltimoMantenimiento);

        if (equipamiento instanceof MolinoEntity molino) {
            builder.rendimientoMolienda(molino.getRendimientoMolienda());
        } else if (equipamiento instanceof MaceradorEntity macerador) {
            builder.capacidadTotal(macerador.getCapacidadTotal())
                    .capacidadUtil(macerador.getCapacidadUtil())
                    .espacioMuerto(macerador.getEspacioMuerto())
                    .eficienciaMaceracion(macerador.getEficienciaMaceracion());
        } else if (equipamiento instanceof OllaHervorEntity olla) {
            builder.capacidadTotal(olla.getCapacidadTotal())
                    .capacidadUtil(olla.getCapacidadUtil())
                    .evaporacion(olla.getEvaporacion())
                    .perdidaPorTrub(olla.getPerdidaPorTrub());
        } else if (equipamiento instanceof FermentadorEntity fermentador) {
            builder.capacidadTotal(fermentador.getCapacidadTotal())
                    .capacidadUtil(fermentador.getCapacidadUtil());
        }
        return builder.build();
    }

    /**
     * Mapea una operación del ciclo de vida a {@link OperacionEquipamientoResponseDTO}.
     *
     * @param operacion Operación a convertir (no un proxy).
     * @param anulable {@code true} si es la última operación registrada del equipamiento.
     * @return El DTO correspondiente o {@code null} si la operación es nula.
     */
    public static OperacionEquipamientoResponseDTO toOperacionDTO(OperacionCicloVidaEquipamiento operacion, boolean anulable) {
        if (operacion == null) {
            return null;
        }
        return OperacionEquipamientoResponseDTO.builder()
                .id(operacion.getId())
                .tipo(TipoOperacionEquipamiento.deOperacion(operacion))
                .estado(operacion.getEstado())
                .fecha(operacion.getFecha())
                .observaciones(operacion.getObservaciones())
                .registradoPor(operacion.getCreatedBy())
                .fechaRegistro(operacion.getCreatedDate())
                .fechaAnulacion(operacion.getFechaAnulacion())
                .motivoAnulacion(operacion.getMotivoAnulacion())
                .estadoOperativoResultante(operacion instanceof LimpiezaEquipamientoEntity limpieza ? limpieza.getEstadoOperativoResultante() : null)
                .anulable(anulable)
                .build();
    }

    /**
     * Mapea una etapa de lote en la que se usó el equipamiento a una fila del historial.
     *
     * @param etapa Etapa a convertir, con su lote cargado.
     * @return La fila del historial, o {@code null} si la etapa es nula.
     */
    public static ActividadEquipamientoResponseDTO toActividadDTO(EtapaLoteEntity etapa) {
        if (etapa == null) {
            return null;
        }
        boolean enCurso = etapa.getEstado() == EstadoEtapaLote.EN_CURSO;
        return ActividadEquipamientoResponseDTO.builder()
                .tipo(TipoActividadEquipamiento.LOTE)
                .idReferencia(etapa.getId())
                .idLote(etapa.getLote().getId())
                .codigoLote(etapa.getLote().getIdentificadorInterno())
                .etapa(etapa.getEtapa())
                .estadoEtapa(etapa.getEstado())
                .fechaInicio(etapa.getFechaInicio())
                .fechaFin(enCurso ? null : etapa.getFechaFinalizacion())
                .enCurso(enCurso)
                .estadoOperativoResultante(estadoOperativoDejadoPorLaEtapa(etapa))
                .build();
    }

    /**
     * El estado operativo en el que una etapa deja al equipamiento, según las transiciones del lote: mientras la etapa
     * está en curso el equipo está {@code EN_USO}; al finalizar Molienda, Maceración, Hervido o Envasado pasa a
     * {@code EN_LIMPIEZA}; Fermentación y Maduración no lo cambian (el fermentador es compartido por las tres últimas
     * etapas y solo pasa a limpieza al finalizar el Envasado); una etapa cancelada que se había iniciado lo deja
     * {@code EN_LIMPIEZA}.
     *
     * @return El estado resultante, o {@code null} si la etapa no cambia el estado del equipamiento.
     */
    static EstadoOperativo estadoOperativoDejadoPorLaEtapa(EtapaLoteEntity etapa) {
        return switch (etapa.getEstado()) {
            case EN_CURSO -> EstadoOperativo.EN_USO;
            case CANCELADA -> EstadoOperativo.EN_LIMPIEZA;
            case FINALIZADA -> switch (etapa.getEtapa()) {
                case MOLIENDA, MACERACION, HERVIDO, ENVASADO -> EstadoOperativo.EN_LIMPIEZA;
                case FERMENTACION, MADURACION -> null;
            };
            case PENDIENTE -> null;
        };
    }

    /**
     * Mapea una operación registrada a una fila del historial.
     *
     * @param operacion Operación a convertir.
     * @return La fila del historial, o {@code null} si la operación es nula.
     */
    public static ActividadEquipamientoResponseDTO toActividadDTO(OperacionCicloVidaEquipamiento operacion) {
        if (operacion == null) {
            return null;
        }
        TipoOperacionEquipamiento tipo = TipoOperacionEquipamiento.deOperacion(operacion);
        EstadoOperativo resultante = switch (tipo) {
            case LIMPIEZA -> ((LimpiezaEquipamientoEntity) operacion).getEstadoOperativoResultante();
            case FALLA -> EstadoOperativo.EN_MANTENIMIENTO;
            case MANTENIMIENTO -> EstadoOperativo.DISPONIBLE;
        };
        return ActividadEquipamientoResponseDTO.builder()
                .tipo(TipoActividadEquipamiento.valueOf(tipo.name()))
                .idReferencia(operacion.getId())
                .observaciones(operacion.getObservaciones())
                .fechaInicio(operacion.getFecha())
                .estadoOperativoResultante(resultante)
                .build();
    }
}
