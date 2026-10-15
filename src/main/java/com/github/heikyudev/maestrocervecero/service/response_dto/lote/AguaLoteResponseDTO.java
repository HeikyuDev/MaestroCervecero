package com.github.heikyudev.maestrocervecero.service.response_dto.lote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta que representa el agua que la planta debe preparar para un lote, en litros.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AguaLoteResponseDTO {

    /**
     * Volumen objetivo del lote, en litros.
     */
    private Double volumenObjetivo;

    /**
     * Masa total de malta escalada para el lote, en kilogramos.
     */
    private Double masaMalta;

    /**
     * Masa de malta planificada (la requerida por la receta, según el escalado del lote), en kilogramos.
     */
    private Double masaMaltaPlanificada;

    /**
     * {@code true} si {@code masaMalta} es la malta realmente molida (consumos registrados en la Molienda) y no la planificada.
     */
    private boolean maltaMolida;

    /**
     * Volumen que debe reunirse en la olla antes de empezar a hervir, en litros.
     */
    private Double volumenPreHervor;

    /**
     * Agua de la maceración (strike water), en litros: incluye el agua que queda retenida en el espacio muerto
     * del macerador.
     */
    private Double aguaMacerado;

    /**
     * Agua de lavado (sparge), en litros. Nunca es negativa.
     */
    private Double aguaLavado;

    /**
     * Agua total a preparar, en litros: agua de macerado más agua de lavado.
     */
    private Double aguaTotal;

    /**
     * Relación de empaste de la receta, en litros por kilogramo de malta.
     */
    private Double relacionDeEmpaste;
    /**
     * Parte del agua de macerado que sale de la relación de empaste (masa de malta por relación de empaste), en litros.
     */
    private Double aguaPorRelacionDeEmpaste;
    /**
     * Agua retenida en el espacio muerto del macerador, en litros.
     */
    private Double espacioMuerto;
    /**
     * Litros de mosto que retiene cada kilogramo de grano.
     */
    private Double absorcionPorKg;
    /**
     * Agua que retiene el grano (masa de malta por la absorción por kilogramo), en litros.
     */
    private Double perdidaPorBagazo;
    /**
     * Mosto que se extrae del macerador, en litros.
     */
    private Double mostoExtraido;
    /**
     * Avisos del cálculo que no lo impiden; vacía si no hay ninguno.
     */
    @Builder.Default
    private List<String> advertencias = List.of();
}
