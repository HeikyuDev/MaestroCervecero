package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import java.util.List;

/**
 * Volúmenes de agua de un lote, en litros, calculados según docs/Dominio/Escalado/EscaladoDelAgua.md.
 *
 * @param volumenPreHervor Volumen que debe reunirse en la olla antes de empezar a hervir (Paso 2).
 * @param aguaMacerado Agua de la maceración: masa de malta por la relación de empaste, más el espacio muerto del
 *                     macerador (Paso 3).
 * @param aguaLavado Agua de lavado (sparge) que completa el volumen pre-hervor (Paso 4). Nunca es negativa: si el
 *                   mosto extraído del macerador ya supera el volumen pre-hervor vale 0 y se informa una advertencia.
 * @param aguaTotal Agua total a preparar: agua de macerado más agua de lavado (Paso 5).
 * @param advertencias Avisos del cálculo que no lo impiden; vacía si no hay ninguno.
 */
public record VolumenesAgua(double volumenPreHervor, double aguaMacerado, double aguaLavado, double aguaTotal, List<String> advertencias) {
}
