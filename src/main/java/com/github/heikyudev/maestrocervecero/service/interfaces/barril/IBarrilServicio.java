package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de Barril.
 */
public interface IBarrilServicio {

    /**
     * Obtiene una página de barriles activos, filtrados opcionalmente por identificador
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas), capacidad (coincidencia
     * exacta) y/o estado operativo (coincidencia exacta). Un parámetro nulo no restringe por ese
     * criterio.
     *
     * @param identificador Texto a buscar dentro del identificador, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de BarrilResponseDTO que cumplen los criterios indicados.
     */
    Page<BarrilResponseDTO> filtrarBarriles(String identificador, Double capacidad, EstadoOperativoBarril estadoOperativo, Pageable pageable);

    /**
     * Busca un barril por su ID.
     *
     * @param id ID del barril.
     * @return BarrilResponseDTO correspondiente al ID proporcionado.
     */
    BarrilResponseDTO buscarPorId(Long id);

    /**
     * Da de alta un nuevo barril.
     *
     * @param barrilFormDTO Datos del barril a dar de alta.
     * @return BarrilResponseDTO del barril dado de alta.
     */
    BarrilResponseDTO altaBarril(BarrilFormDTO barrilFormDTO);

    /**
     * Modifica un barril existente.
     *
     * @param id             ID del barril a modificar.
     * @param barrilFormDTO  Datos del barril a modificar.
     * @return BarrilResponseDTO del barril modificado.
     */
    BarrilResponseDTO modificarBarril(Long id, BarrilFormDTO barrilFormDTO);

    /**
     * Da de baja un barril existente.
     *
     * @param id ID del barril a dar de baja.
     * @return BarrilResponseDTO del barril dado de baja.
     */
    BarrilResponseDTO bajaBarril(Long id);
}
