package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios de consulta genéricos sobre equipamiento, sin importar su
 * tipo concreto.
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
}
