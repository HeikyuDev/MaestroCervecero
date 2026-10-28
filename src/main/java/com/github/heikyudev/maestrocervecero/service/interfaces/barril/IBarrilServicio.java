package com.github.heikyudev.maestrocervecero.service.interfaces.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de Barril.
 */
public interface IBarrilServicio {

    /**
     * Obtiene una página de barriles activos, filtrados opcionalmente por identificador interno y por número de serie
     * (coincidencia parcial, sin distinguir mayúsculas/minúsculas), capacidad (coincidencia
     * exacta), estado operativo (coincidencia exacta) y/o fabricante (por su ID). Un parámetro
     * nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param numeroSerie Texto a buscar dentro del número de serie, o {@code null} para no filtrar por él.
     * @param capacidad Capacidad exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param idFabricante El ID del fabricante a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de BarrilResponseDTO que cumplen los criterios indicados.
     */
    Page<BarrilResponseDTO> filtrarBarriles(String identificadorInterno, String numeroSerie, Double capacidad, EstadoOperativoBarril estadoOperativo, Long idFabricante, Pageable pageable);

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
     * Modifica un barril existente. El barril queda bloqueado mientras se valida y se escribe.
     *
     * @param id             ID del barril a modificar.
     * @param barrilFormDTO  Datos del barril a modificar.
     * @return BarrilResponseDTO del barril modificado.
     */
    BarrilResponseDTO modificarBarril(Long id, BarrilFormDTO barrilFormDTO);

    /**
     * Da de baja un barril existente.
     * <p>
     * El barril queda bloqueado mientras se valida y se escribe. La baja solo procede si el barril se encuentra en estado operativo {@code DISPONIBLE}.
     * </p>
     *
     * @param id ID del barril a dar de baja.
     * @return BarrilResponseDTO del barril dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un barril activo con el ID especificado.
     * @throws ReglaNegocioException Si el barril no se encuentra en estado operativo {@code DISPONIBLE}.
     */
    BarrilResponseDTO bajaBarril(Long id);
}
