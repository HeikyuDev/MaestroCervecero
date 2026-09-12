package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los métodos del servicio de fermentadores.
 */
public interface IFermentadorServicio {

    /**
     * Obtiene una página de fermentadores activos, filtrados opcionalmente por identificador
     * interno (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o estado operativo
     * (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de objetos FermentadorResponseDTO que cumplen los criterios indicados.
     */
    Page<FermentadorResponseDTO> filtrarFermentadores(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Busca un fermentador por su ID.
     *
     * @param id ID del fermentador.
     * @return Objeto FermentadorResponseDTO correspondiente al ID proporcionado.
     */
    FermentadorResponseDTO buscarPorId(Long id);

    /**
     * Da de alta un nuevo fermentador.
     *
     * @param fermentadorFormDTO Datos del fermentador a crear.
     * @return Objeto FermentadorResponseDTO del fermentador creado.
     */
    FermentadorResponseDTO altaFermentador(FermentadorFormDTO fermentadorFormDTO);

    /**
     * Modifica un fermentador existente.
     *
     * @param id ID del fermentador a modificar.
     * @param fermentadorFormDTO Datos del fermentador a modificar.
     * @return Objeto FermentadorResponseDTO del fermentador modificado.
     */
    FermentadorResponseDTO modificarFermentador(Long id,FermentadorFormDTO fermentadorFormDTO);

    /**
     * Da de baja un fermentador existente.
     *
     * @param id ID del fermentador a dar de baja.
     * @return Objeto FermentadorResponseDTO del fermentador dado de baja.
     */
    FermentadorResponseDTO bajaFermentador(Long id);
}
