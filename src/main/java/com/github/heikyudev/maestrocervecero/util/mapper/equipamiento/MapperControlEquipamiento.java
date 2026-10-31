package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
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
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.BloqueLineaTiempoEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OperacionEquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.TipoBloqueLineaTiempo;

import java.time.LocalDateTime;

/**
 * MapperControlEquipamiento tiene la responsabilidad de mapear las entidades del control de equipamiento (equipamiento,
 * operaciones de su ciclo de vida y etapas de lote) a los DTO de respuesta de las pantallas de control.
 */
public class MapperControlEquipamiento {

    private MapperControlEquipamiento() {
    }

    /**
     * Mapea un equipamiento, con sus usos y su última operación registrada, a {@link EquipamientoControlResponseDTO}.
     *
     * @param equipamiento Equipamiento a convertir (no un proxy).
     * @param usosDesdeUltimoMantenimiento Cantidad de usos desde el último mantenimiento.
     * @param ultimaOperacion Última operación registrada del equipamiento, o {@code null} si no tiene ninguna.
     * @return El DTO correspondiente o {@code null} si el equipamiento es nulo.
     */
    public static EquipamientoControlResponseDTO toControlDTO(EquipamientoEntity equipamiento, long usosDesdeUltimoMantenimiento, OperacionCicloVidaEquipamiento ultimaOperacion) {
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
                .tipoUltimaOperacion(ultimaOperacion != null ? TipoOperacionEquipamiento.deOperacion(ultimaOperacion) : null)
                .fechaFinUltimaOperacion(ultimaOperacion != null ? ultimaOperacion.getFechaFin() : null)
                .build();
    }

    /**
     * Mapea un equipamiento, con sus usos y las fechas de su última limpieza y último mantenimiento, a
     * {@link EquipamientoResumenResponseDTO}, incluyendo los parámetros propios de su tipo concreto.
     *
     * @param equipamiento Equipamiento a convertir (no un proxy).
     * @param usosDesdeUltimoMantenimiento Cantidad de usos desde el último mantenimiento.
     * @param fechaFinUltimaLimpieza Fecha de fin de la última limpieza registrada, o {@code null}.
     * @param fechaFinUltimoMantenimiento Fecha de fin del último mantenimiento registrado, o {@code null}.
     * @return El DTO correspondiente o {@code null} si el equipamiento es nulo.
     */
    public static EquipamientoResumenResponseDTO toResumenDTO(EquipamientoEntity equipamiento, long usosDesdeUltimoMantenimiento,
                                                             LocalDateTime fechaFinUltimaLimpieza, LocalDateTime fechaFinUltimoMantenimiento) {
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
                .fechaFinUltimaLimpieza(fechaFinUltimaLimpieza)
                .fechaFinUltimoMantenimiento(fechaFinUltimoMantenimiento);

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
                .fechaInicio(operacion.getFecha())
                .fechaFin(operacion.getFechaFin())
                .observaciones(operacion.getObservaciones())
                .responsable(operacion.getCreatedBy())
                .fechaAnulacion(operacion.getFechaAnulacion())
                .motivoAnulacion(operacion.getMotivoAnulacion())
                .estadoOperativoResultante(operacion instanceof LimpiezaEquipamientoEntity limpieza ? limpieza.getEstadoOperativoResultante() : null)
                .anulable(anulable)
                .build();
    }

    /**
     * Mapea una etapa de lote con uso real del equipamiento a un bloque de la línea de tiempo. Una etapa en curso no
     * tiene fecha de finalización: su fin es provisorio y vale {@code ahora}. Una etapa ya terminada sin fecha de
     * finalización informada se muestra como un bloque sin duración (fin igual al inicio).
     *
     * @param etapa Etapa de lote a convertir, con su lote cargado.
     * @param ahora Momento de la consulta, usado como fin provisorio de las etapas en curso.
     * @return El bloque correspondiente o {@code null} si la etapa es nula.
     */
    public static BloqueLineaTiempoEquipamientoResponseDTO toBloqueDTO(EtapaLoteEntity etapa, LocalDateTime ahora) {
        if (etapa == null) {
            return null;
        }
        boolean enCurso = etapa.getEstado() == EstadoEtapaLote.EN_CURSO;
        LocalDateTime fin = enCurso ? ahora : (etapa.getFechaFinalizacion() != null ? etapa.getFechaFinalizacion() : etapa.getFechaInicio());
        return BloqueLineaTiempoEquipamientoResponseDTO.builder()
                .tipo(TipoBloqueLineaTiempo.LOTE)
                .idReferencia(etapa.getId())
                .codigoLote(etapa.getLote().getIdentificadorInterno())
                .etapa(etapa.getEtapa())
                .estadoEtapa(etapa.getEstado())
                .fechaInicio(etapa.getFechaInicio())
                .fechaFin(fin)
                .finProvisorio(enCurso)
                .build();
    }

    /**
     * Mapea una operación registrada del ciclo de vida a un bloque de la línea de tiempo.
     *
     * @param operacion Operación a convertir (no un proxy).
     * @return El bloque correspondiente o {@code null} si la operación es nula.
     */
    public static BloqueLineaTiempoEquipamientoResponseDTO toBloqueDTO(OperacionCicloVidaEquipamiento operacion) {
        if (operacion == null) {
            return null;
        }
        TipoOperacionEquipamiento tipo = TipoOperacionEquipamiento.deOperacion(operacion);
        return BloqueLineaTiempoEquipamientoResponseDTO.builder()
                .tipo(tipo != null ? TipoBloqueLineaTiempo.valueOf(tipo.name()) : null)
                .idReferencia(operacion.getId())
                .observaciones(operacion.getObservaciones())
                .fechaInicio(operacion.getFecha())
                .fechaFin(operacion.getFechaFin())
                .finProvisorio(false)
                .build();
    }
}
