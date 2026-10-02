package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ResumenEquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios de consulta genéricos sobre equipamiento, sin importar su
 * tipo concreto.
 * <p>
 * Todas las operaciones actúan únicamente sobre equipamientos activos. Son de solo lectura: el
 * alta, la modificación y la baja de cada tipo viven en su propio service.
 * </p>
 */
public interface IEquipamientoServicio {

    /**
     * Obtiene una página de equipamientos activos de cualquier tipo, filtrados opcionalmente
     * por identificador interno (coincidencia parcial), tipo concreto y/o estado operativo
     * (ambos por coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param tipo Tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por tipo.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de equipamientos activos que cumplen los criterios indicados, en formato DTO liviano.
     */
    Page<EquipamientoResponseDTO> filtrarEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Cuenta los equipamientos activos por tipo y estado operativo.
     * <p>
     * El resultado siempre trae todas las combinaciones de tipo y estado, con 0 donde no hay
     * ningún equipamiento.
     * </p>
     *
     * @return Los totales del resumen de equipamiento.
     */
    ResumenEquipamientoResponseDTO obtenerResumen();

    /**
     * Obtiene una página con el resumen de los equipamientos activos de cualquier tipo: su estado
     * operativo y los lotes en los que participan, filtrados opcionalmente por identificador
     * interno (coincidencia parcial, sin distinguir mayúsculas/minúsculas), tipo concreto y/o
     * estado operativo (ambos por coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     * <p>
     * Para cada equipamiento informa la etapa que tiene en curso (si la tiene) y las etapas que
     * tiene reservadas a futuro, estas últimas solo de lotes {@code PENDIENTE} o {@code EN_EJECUCION}.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param tipo Tipo concreto de equipamiento a filtrar, o {@code null} para no filtrar por tipo.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de filas de resumen que cumplen los criterios indicados.
     */
    Page<EquipamientoResumenResponseDTO> filtrarResumenEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable);
}
