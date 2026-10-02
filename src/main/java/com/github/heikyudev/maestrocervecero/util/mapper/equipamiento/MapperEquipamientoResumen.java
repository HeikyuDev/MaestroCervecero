package com.github.heikyudev.maestrocervecero.util.mapper.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ParticipacionLoteResponseDTO;

import java.util.List;

/**
 * MapperEquipamientoResumen tiene la responsabilidad de armar las filas del resumen de
 * equipamiento a partir de la entidad base {@link EquipamientoEntity} y de las etapas de lote en
 * las que participa.
 */
public class MapperEquipamientoResumen {

    /**
     * Arma una fila del resumen de equipamiento.
     *
     * @param equipamientoEntity Entidad de equipamiento a resumir.
     * @param etapaEnCurso Etapa que el equipamiento tiene en curso, o {@code null} si no tiene ninguna.
     * @param etapasPendientes Etapas que el equipamiento tiene reservadas a futuro (puede ser vacía).
     * @return Objeto DTO correspondiente o {@code null} si la entidad de equipamiento es nula.
     */
    public static EquipamientoResumenResponseDTO toDTO(EquipamientoEntity equipamientoEntity,
                                                       EtapaLoteEntity etapaEnCurso,
                                                       List<EtapaLoteEntity> etapasPendientes) {
        if (equipamientoEntity == null) {
            return null;
        }

        return EquipamientoResumenResponseDTO.builder()
                .id(equipamientoEntity.getId())
                .identificadorInterno(equipamientoEntity.getIdentificadorInterno())
                .tipo(tipoDe(equipamientoEntity))
                .estadoOperativo(equipamientoEntity.getEstadoOperativo())
                .etapaEnCurso(toParticipacion(etapaEnCurso))
                .etapasPendientes(etapasPendientes.stream().map(MapperEquipamientoResumen::toParticipacion).toList())
                .build();
    }

    /**
     * Mapea una etapa de lote a la participación que se muestra en el resumen.
     *
     * @param etapaLoteEntity Etapa a convertir.
     * @return Objeto DTO correspondiente o {@code null} si la etapa es nula.
     */
    public static ParticipacionLoteResponseDTO toParticipacion(EtapaLoteEntity etapaLoteEntity) {
        if (etapaLoteEntity == null) {
            return null;
        }

        return ParticipacionLoteResponseDTO.builder()
                .idLote(etapaLoteEntity.getLote().getId())
                .identificadorInternoLote(etapaLoteEntity.getLote().getIdentificadorInterno())
                .etapa(etapaLoteEntity.getEtapa())
                .build();
    }

    /**
     * Determina el tipo concreto de un equipamiento a partir de su clase de entidad.
     *
     * @param equipamientoEntity Equipamiento del que se quiere conocer el tipo.
     * @return El tipo concreto del equipamiento.
     * @throws IllegalStateException Si la clase del equipamiento no corresponde a ningún tipo conocido.
     */
    public static TipoEquipamiento tipoDe(EquipamientoEntity equipamientoEntity) {
        if (equipamientoEntity instanceof MolinoEntity) {
            return TipoEquipamiento.MOLINO;
        }
        if (equipamientoEntity instanceof MaceradorEntity) {
            return TipoEquipamiento.MACERADOR;
        }
        if (equipamientoEntity instanceof OllaHervorEntity) {
            return TipoEquipamiento.OLLA_HERVOR;
        }
        if (equipamientoEntity instanceof FermentadorEntity) {
            return TipoEquipamiento.FERMENTADOR;
        }
        throw new IllegalStateException("Tipo de equipamiento desconocido: " + equipamientoEntity.getClass());
    }
}
