package com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO de respuesta con los totales del resumen de equipamiento: cuántos equipos activos hay de
 * cada tipo en cada estado operativo.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ResumenEquipamientoResponseDTO {

    /**
     * Cantidad de equipamientos activos por tipo y estado operativo. Siempre incluye todas las
     * combinaciones posibles (tipo x estado), con 0 cuando no hay ninguno, para que la vista no
     * tenga que completar los faltantes.
     */
    private Map<TipoEquipamiento, Map<EstadoOperativo, Long>> cantidadesPorTipoYEstado;
}
