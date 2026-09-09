package com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Obtiene una página de planificaciones de producción activas.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de planificaciones de producción en formato DTO.
     */
    Page<PlanificacionProduccionResponseDTO> buscarTodos(Pageable pageable);

    /**
     * Obtiene una planificación de producción activa por su ID.
     *
     * @param id El ID de la planificación de producción.
     * @return La planificación de producción correspondiente al ID.
     */
    PlanificacionProduccionResponseDTO buscarPorId(Long id);

    /**
     * Registra una nueva planificación de producción.
     *
     * @param planificacionProduccionFormDTO Los datos de la planificación de producción a registrar.
     * @return La planificación de producción registrada.
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
}
