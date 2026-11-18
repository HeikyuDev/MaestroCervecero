package com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.LotePlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.LotePlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * MapperPlanProduccion tiene la responsabilidad de mapear los lotes de un plan de producción (calculado al momento,
 * nunca persistido) entre sus equipos, su DTO de respuesta y sus DTOs de formulario.
 */
public class MapperPlanProduccion {

    /**
     * Arma el DTO de respuesta de un lote del plan a partir de sus equipos y de sus fechas estimadas.
     *
     * @param orden Posición del lote en el plan (1 es el primero).
     * @param volumenObjetivo Volumen objetivo del lote, en litros.
     * @param fermentador Fermentador del lote.
     * @param molino Molino del lote.
     * @param macerador Macerador del lote.
     * @param ollaHervor Olla de hervor del lote.
     * @param fechaInicioEstimada Fecha de inicio estimada del lote.
     * @param fechaFinalizacionEstimada Fecha de finalización estimada del lote.
     * @return El DTO de respuesta del lote.
     */
    public static LotePlanProduccionResponseDTO toLoteDTO(int orden, double volumenObjetivo, FermentadorEntity fermentador, MolinoEntity molino,
                                                          MaceradorEntity macerador, OllaHervorEntity ollaHervor,
                                                          LocalDate fechaInicioEstimada, LocalDate fechaFinalizacionEstimada) {
        return LotePlanProduccionResponseDTO.builder()
                .orden(orden)
                .volumenObjetivo(volumenObjetivo)
                .idFermentador(fermentador.getId())
                .identificadorFermentador(fermentador.getIdentificadorInterno())
                .capacidadFermentador(fermentador.getCapacidadUtil())
                .idMolino(molino.getId())
                .identificadorMolino(molino.getIdentificadorInterno())
                .idMacerador(macerador.getId())
                .identificadorMacerador(macerador.getIdentificadorInterno())
                .idOllaHervor(ollaHervor.getId())
                .identificadorOllaHervor(ollaHervor.getIdentificadorInterno())
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionEstimada(fechaFinalizacionEstimada)
                .build();
    }

    /**
     * Mapea un plan de producción a su DTO de formulario, con sus lotes en el mismo orden.
     *
     * @param plan El plan a convertir.
     * @return El DTO de formulario, o {@code null} si el plan es nulo.
     */
    public static PlanProduccionFormDTO toFormDTO(PlanProduccionResponseDTO plan) {
        if (plan == null) {
            return null;
        }
        return PlanProduccionFormDTO.builder()
                .modalidad(plan.getModalidad())
                .lotes(new ArrayList<>(plan.getLotes().stream().map(MapperPlanProduccion::toLoteFormDTO).toList()))
                .build();
    }

    /**
     * Mapea un lote de un plan de producción a su DTO de formulario.
     *
     * @param lote El lote a convertir.
     * @return El DTO de formulario del lote.
     */
    public static LotePlanProduccionFormDTO toLoteFormDTO(LotePlanProduccionResponseDTO lote) {
        return LotePlanProduccionFormDTO.builder()
                .volumenObjetivo(lote.getVolumenObjetivo())
                .idFermentador(lote.getIdFermentador())
                .idMolino(lote.getIdMolino())
                .idMacerador(lote.getIdMacerador())
                .idOllaHervor(lote.getIdOllaHervor())
                .build();
    }

    /**
     * Mapea un lote de un plan de producción al DTO de formulario del registro de un lote.
     *
     * @param idPlanificacion ID de la planificación de producción del plan.
     * @param lote El lote del plan.
     * @return El DTO de formulario con el que se registra el lote.
     */
    public static LoteFormDTO toLoteFormDTO(Long idPlanificacion, LotePlanProduccionFormDTO lote) {
        return LoteFormDTO.builder()
                .idPlanificacionProduccion(idPlanificacion)
                .volumenObjetivo(lote.getVolumenObjetivo())
                .idFermentador(lote.getIdFermentador())
                .idMolino(lote.getIdMolino())
                .idMacerador(lote.getIdMacerador())
                .idOllaHervor(lote.getIdOllaHervor())
                .build();
    }
}
