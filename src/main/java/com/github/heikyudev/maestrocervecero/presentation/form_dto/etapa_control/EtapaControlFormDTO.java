package com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de formulario para el alta y la modificación de una etapa de control.
 * <p>
 * Es inmutable (no expone setters) y contiene únicamente los datos ingresados por el usuario.
 * No incluye {@code id} (la identidad la define la base de datos).
 * </p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaControlFormDTO {
    /**
     * Nombre de la etapa de control. Debe ser único entre las etapas de control activas (case-insensitive).
     */
    private String nombre;

    /**
     * Descripción de la etapa de control. Puede ser nula o vacía.
     */
    private String descripcion;

    /**
     * Tipo de etapa a controlar (MACERACION, FERMENTACION, HERVIDO, MADURACION).
     */
    private TipoEtapa etapaAControlar;
}
