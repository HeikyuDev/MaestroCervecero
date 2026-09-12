package com.github.heikyudev.maestrocervecero.service.interfaces.receta;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.PlanMonitoreoEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interfaz que define los métodos para la gestión de recetas.
 */
public interface IRecetaServicio {

    /**
     * Obtiene una página de recetas activas, filtradas opcionalmente por el nombre de su versión
     * vigente (coincidencia parcial, sin distinguir mayúsculas/minúsculas). Un parámetro nulo no
     * restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre de la versión vigente de la receta, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de recetas activas que cumplen el criterio indicado, en formato DTO.
     */
    Page<RecetaResponseDTO> filtrarRecetas(String nombre, Pageable pageable);

    /**
     * Obtiene los detalles de parámetro de control configurados en un plan de monitoreo de etapa.
     * <p>
     * {@code idPlanMonitoreoEtapa} lo elige el usuario, típicamente entre las opciones devueltas
     * por {@link #filtrarPlanesMonitoreo(Long)} (que ya vienen correctamente acotadas a la etapa
     * actual del lote) — por eso este método no necesita volver a validar la correspondencia con
     * ninguna etapa.
     * </p>
     *
     * @param idPlanMonitoreoEtapa El ID del plan de monitoreo de etapa elegido por el usuario.
     * @return Los detalles de parámetro de control configurados en ese plan, en formato DTO.
     * @throws RecursoNoEncontradoException Si el plan de monitoreo de etapa referenciado no existe.
     */
    List<DetalleParametroControlResponseDTO> filtrarDetallesParametroControl(Long idPlanMonitoreoEtapa);

    /**
     * Obtiene los planes de monitoreo configurados para la etapa (tipo) de una etapa de lote
     * determinada, dentro de la versión de receta que está utilizando el lote en ejecución.
     * <p>
     * {@code idEtapaLote} no lo tipea el usuario: lo determina el sistema según en qué etapa de
     * qué lote se está parado (por ejemplo, al entrar a la futura pantalla "Registrar Medición"
     * estando en el contexto de la etapa de Maceración de un lote). A partir de esa única etapa de
     * lote, tanto el tipo de etapa a controlar como la versión de receta vigente para ese lote
     * (navegando {@code etapaLote.lote.planificacionProduccion.versionReceta}) se derivan solos,
     * sin que el usuario tenga que informarlos.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote actual.
     * @return Los planes de monitoreo de esa versión de receta cuya etapa a controlar coincide con
     *         el tipo de etapa del lote indicado, en formato DTO. Vacía si no hay ninguno configurado.
     * @throws RecursoNoEncontradoException Si la etapa de lote referenciada no existe.
     */
    List<PlanMonitoreoEtapaResponseDTO> filtrarPlanesMonitoreo(Long idEtapaLote);

    /**
     * Obtiene una receta activa por su ID.
     *
     * @param id El ID de la receta.
     * @return La receta correspondiente al ID.
     */
    RecetaResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva receta.
     *
     * @param recetaFormDTO Los datos de la receta a registrar.
     * @return La receta registrada.
     */
    RecetaResponseDTO altaReceta(RecetaFormDTO recetaFormDTO);

    /**
     * Modifica una receta existente.
     *
     * @param id El ID de la receta a modificar.
     * @param recetaFormDTO Los nuevos datos de la receta.
     * @return La receta modificada.
     */
    RecetaResponseDTO modificarReceta(Long id, RecetaFormDTO recetaFormDTO);

    /**
     * Realiza la baja lógica de una receta existente.
     *
     * @param id El ID de la receta a dar de baja.
     * @return La receta dada de baja.
     */
    RecetaResponseDTO bajaReceta(Long id);
}
