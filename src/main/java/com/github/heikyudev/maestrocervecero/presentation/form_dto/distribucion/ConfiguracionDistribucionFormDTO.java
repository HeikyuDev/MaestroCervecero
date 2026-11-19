package com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

/**
 * DTO de formulario para actualizar la configuración de distribución (parámetros de la coordinación automática del
 * retiro de barriles).
 * <p>
 * No admite actualización parcial: el encargado de distribución siempre reenvía el conjunto completo. Los topes de las
 * anotaciones repiten {@code LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MINIMO} y
 * {@code LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MAXIMO} (una anotación exige un literal).
 * </p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionDistribucionFormDTO {

    /**
     * Hora a partir de la cual se envía el recordatorio diario de retiro.
     */
    @NotNull(message = "La hora de envío del recordatorio es obligatoria")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horaEnvioRecordatorioRetiro;

    /**
     * Días hacia adelante en los que el cliente puede proponer el retiro.
     */
    @NotNull(message = "Los días máximos de anticipación del retiro son obligatorios")
    @Min(value = 1, message = "Los días máximos de anticipación del retiro deben estar entre 1 y 30")
    @Max(value = 30, message = "Los días máximos de anticipación del retiro deben estar entre 1 y 30")
    private Integer diasMaximosAnticipacionRetiro;
}
