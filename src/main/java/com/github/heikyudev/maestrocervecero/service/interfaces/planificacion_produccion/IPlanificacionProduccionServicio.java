package com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.SugerenciaFechaFinalizacionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

/**
 * Interfaz que define los métodos para la gestión de planificaciones de producción.
 * <p>
 * A diferencia de los demás módulos, este no es un CRUD: una planificación de producción es un registro
 * transaccional que nunca se modifica ni se da de baja. Solo admite su registro inicial y dos
 * cierres excepcionales (anulación o finalización forzada); el cierre normal ocurre cuando la
 * cantidad producida alcanza la cantidad solicitada, algo que resuelve el módulo de lotes.
 * </p>
 */
public interface IPlanificacionProduccionServicio {

    /**
     * Obtiene una página de planificaciones de producción, filtradas opcionalmente por el ID de
     * la receta contenedora de la versión utilizada, por estado y/o por fecha de inicio estimada
     * (los tres por coincidencia exacta). Un parámetro nulo no restringe por ese criterio.
     *
     * @param idReceta ID de la receta contenedora cuya versión se usó, o {@code null} para no filtrar por ella.
     * @param estado Estado exacto a filtrar, o {@code null} para no filtrar por estado.
     * @param fechaInicio Fecha de inicio estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable La configuración de paginación.
     * @return Una página de planificaciones de producción que cumplen los criterios indicados, en formato DTO. Cada una incluye sus
     * litros producidos ({@code litrosProducidos}): la suma de la cantidad envasada de los envasados {@code REGISTRADO}
     * (no anulados) de cualquier lote de la planificación, o {@code 0.0} si no tiene ninguno.
     */
    Page<PlanificacionProduccionResponseDTO> filtrarPlanificacionesProduccion(Long idReceta, EstadoSolicitud estado, LocalDate fechaInicio, Pageable pageable);

    /**
     * Obtiene una planificación de producción activa por su ID.
     *
     * @param id El ID de la planificación de producción.
     * @return La planificación de producción correspondiente al ID, con sus litros producidos ({@code litrosProducidos}, calculados
     * como en {@link #filtrarPlanificacionesProduccion}).
     */
    PlanificacionProduccionResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva planificación de producción.
     *
     * @param planificacionProduccionFormDTO Los datos de la planificación de producción a registrar.
     * @return La planificación de producción registrada.
     * @throws ReglaNegocioException Si la cantidad a producir no es mayor a 0, si la fecha de inicio estimada es anterior a la fecha actual (hoy es válido), o si la fecha de finalización estimada es anterior a la fecha de inicio estimada.
     * @throws RecursoNoEncontradoException Si la receta referenciada no existe, o si no tiene ninguna versión activa.
     */
    PlanificacionProduccionResponseDTO registrarPlanificacionProduccion(PlanificacionProduccionFormDTO planificacionProduccionFormDTO);

    /**
     * Anula una planificación de producción existente.
     *
     * @param id El ID de la planificación de producción a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La planificación de producción anulada.
     */
    PlanificacionProduccionResponseDTO anularPlanificacionProduccion(Long id, AnulacionPlanificacionProduccionFormDTO anulacionFormDTO);

    /**
     * Finaliza de forma forzada una planificación de producción existente.
     *
     * @param id El ID de la planificación de producción a finalizar.
     * @param finalizacionFormDTO Los datos de la finalización forzada (motivo).
     * @return La planificación de producción finalizada.
     */
    PlanificacionProduccionResponseDTO finalizarPlanificacionProduccion(Long id, FinalizacionForzadaPlanificacionProduccionFormDTO finalizacionFormDTO);

    /**
     * Sugiere una fecha de finalización para una planificación que todavía no se registró,
     * simulando una producción secuencial de todos sus lotes en un único fermentador.
     * <p>
     * Es solo una estimación: no persiste nada ni crea lotes, y no usa el molino, el macerador ni
     * la olla de hervor, ni las velocidades de la configuración. El cálculo es:
     * </p>
     * <ul>
     *     <li>Cantidad de lotes = {@code cantidadAProducir} / capacidad de lote estándar de la
     *     configuración de producción, redondeada hacia arriba.</li>
     *     <li>Duración de cada lote, en días = (duración de maceración + duración de hervido, en
     *     minutos, / 1440) redondeado hacia arriba, más la duración de fermentación y la de
     *     maduración de la última versión activa de la receta.</li>
     *     <li>El primer lote comienza en {@code fechaInicioEstimada}; cada lote siguiente comienza
     *     un día después de que termina el anterior (margen de limpieza del fermentador); cada lote
     *     termina su duración en días después de su inicio.</li>
     *     <li>La fecha sugerida es la de finalización del último lote.</li>
     * </ul>
     *
     * @param idReceta ID de la receta contenedora cuya última versión activa se usa para la simulación.
     * @param cantidadAProducir Cantidad total a producir, en litros; debe ser mayor a cero.
     * @param fechaInicioEstimada Fecha en que comenzaría el primer lote; obligatoria.
     * @return La sugerencia con la fecha de finalización y los datos de la simulación.
     * @throws RecursoNoEncontradoException Si la receta no existe (o está dada de baja), si no tiene una versión activa, o si no se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si la cantidad a producir es nula, no es finita o no es mayor a cero, si la fecha de inicio estimada es nula o anterior a la fecha actual (hoy es válido), si la capacidad de lote estándar configurada no es mayor a cero, o si la cantidad de lotes resultante excede el máximo representable ({@code Integer.MAX_VALUE}).
     */
    SugerenciaFechaFinalizacionResponseDTO sugerirFechaFinalizacion(Long idReceta, Double cantidadAProducir, LocalDate fechaInicioEstimada);
}
