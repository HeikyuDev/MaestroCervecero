package com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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
     * Obtiene una página de fermentadores activos y de fermentadores dados de baja que tienen lotes (sin contar los
     * cancelados), filtrados opcionalmente por identificador interno (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Información de paginación.
     * @return Página de objetos FermentadorResponseDTO, activos y dados de baja con lotes, que cumplen los criterios indicados.
     */
    Page<FermentadorResponseDTO> filtrarFermentadoresConDadosDeBajaConLotes(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable);

    /**
     * Lista los fermentadores dados de baja que tienen lotes (sin contar los cancelados), ordenados por identificador
     * interno. Los fermentadores dados de baja sin lotes no se incluyen.
     *
     * @return Lista de objetos FermentadorResponseDTO dados de baja con lotes, o una lista vacía si no hay ninguno.
     */
    List<FermentadorResponseDTO> listarDadosDeBajaConLotes();

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
     * <p>
     * La baja solo procede si el fermentador se encuentra disponible: no se puede dar de baja mientras
     * está en uso, en limpieza o en mantenimiento, ni si tiene comprometida una etapa pendiente
     * de un lote.
     * </p>
     *
     * @param id ID del fermentador a dar de baja.
     * @return Objeto FermentadorResponseDTO del fermentador dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un fermentador activo con el ID especificado.
     * @throws ReglaNegocioException Si el fermentador tiene comprometida una etapa en estado Pendiente de un lote, o si no se encuentra disponible (en uso, en limpieza o en mantenimiento).
     */
    FermentadorResponseDTO bajaFermentador(Long id);
}
