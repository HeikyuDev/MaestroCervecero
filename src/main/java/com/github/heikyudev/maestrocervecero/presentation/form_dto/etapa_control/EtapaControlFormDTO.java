package com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de formulario para el alta y la modificación de una etapa de control.
 * <p>
 * Contiene únicamente los datos ingresados por el usuario. No incluye {@code id} (la identidad la
 * define la base de datos).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaControlFormDTO {
    /**
     * Nombre de la etapa de control. Debe ser único entre las etapas de control activas (case-insensitive).
     */
    @NotBlank(message = "El nombre de la etapa de control es obligatorio")
    private String nombre;

    /**
     * Descripción de la etapa de control. Puede ser nula o vacía.
     */
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    /**
     * Tipo de etapa a controlar (MACERACION, FERMENTACION, HERVIDO, MADURACION).
     */
    @NotNull(message = "La etapa a controlar es obligatoria")
    private TipoEtapa etapaAControlar;
}
