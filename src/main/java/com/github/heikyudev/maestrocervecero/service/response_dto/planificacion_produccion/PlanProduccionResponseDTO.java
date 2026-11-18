package com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta con un plan de producción calculado al momento (nunca se persiste): los lotes que cubren los
 * litros que le faltan planificar a una planificación de producción y sus totales.
 * <p>
 * Un plan no disponible no trae lotes e informa el motivo. Un plan disponible puede traer avisos: situaciones que no
 * impiden verlo pero que la confirmación rechazaría si el plan se confirma tal como está (por ejemplo, un excedente
 * mayor al configurado).
 * </p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PlanProduccionResponseDTO {

    /** Modalidad del plan. */
    private ModalidadPlanProduccion modalidad;

    /** Nombre legible del criterio de selección de equipos con el que se generó el plan. */
    private String criterio;

    /** {@code true} si el plan se pudo generar. */
    private boolean disponible;

    /** Motivo por el que el plan no está disponible, o {@code null} si lo está. */
    private String motivoNoDisponible;

    /** Avisos del plan (lista vacía si no hay ninguno). */
    @Builder.Default
    private List<String> avisos = List.of();

    /** Lotes del plan en el orden en que se registrarán (lista vacía si el plan no está disponible). */
    @Builder.Default
    private List<LotePlanProduccionResponseDTO> lotes = List.of();

    /** Litros que le faltan planificar a la planificación: la cantidad a producir menos lo producido y lo comprometido por sus lotes vivos. */
    private Double litrosAPlanificar;

    /** Cantidad de lotes del plan. */
    private int cantidadLotes;

    /** Suma del volumen objetivo de los lotes del plan, en litros. */
    private Double volumenTotal;

    /** Litros en que el volumen total supera a los litros a planificar (0 si no los supera). */
    private Double excedente;

    /** Cantidad de fermentadores distintos que usa el plan. */
    private int fermentadoresUtilizados;

    /** Fecha de finalización estimada del último lote del plan, o {@code null} si el plan no tiene lotes. */
    private LocalDate fechaFinalizacionPlan;
}
