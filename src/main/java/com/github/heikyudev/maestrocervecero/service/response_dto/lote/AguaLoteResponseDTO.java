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
     * Avisos del cálculo que no lo impiden; vacía si no hay ninguno.
     */
    @Builder.Default
    private List<String> advertencias = List.of();
}
