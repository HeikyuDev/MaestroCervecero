package com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.planificacion_produccion.MapperPlanificacionProduccion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PlanificacionProduccionServicioImpl implements IPlanificacionProduccionServicio {

    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IRecetaRepository recetaRepository;

    /**
     * Recupera una página de planificaciones de producción registradas en el sistema, filtradas
     * opcionalmente por el ID de la receta contenedora de la versión utilizada, por estado y/o
     * por fecha de inicio estimada (los tres por coincidencia exacta). Un parámetro nulo no
     * restringe por ese criterio.
     * <p>
     * Esta entidad no tiene baja lógica: no existe el concepto de "planificación inactiva", por
     * lo que no se aplica ningún filtro adicional de estado activo/baja.
     * </p>
     *
     * @param idReceta ID de la receta contenedora cuya versión se usó, o {@code null} para no filtrar por ella.
     * @param estado Estado exacto a filtrar, o {@code null} para no filtrar por estado.
     * @param fechaInicio Fecha de inicio estimada exacta a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link PlanificacionProduccionResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PlanificacionProduccionResponseDTO> filtrarPlanificacionesProduccion(Long idReceta, EstadoSolicitud estado, LocalDate fechaInicio, Pageable pageable) {
        return planificacionProduccionRepository.filtrarPlanificacionesProduccion(idReceta, estado, fechaInicio, pageable).map(MapperPlanificacionProduccion::toDTO);
    }

    /**
     * Busca y retorna una planificación de producción específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la planificación de producción buscada.
     * @return Objeto {@link PlanificacionProduccionResponseDTO} con la información de la planificación de producción encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna planificación de producción activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public PlanificacionProduccionResponseDTO buscarPorId(Long id) {
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + id)));
    }

    /**
     * Registra una nueva planificación de producción en el sistema.
     * <p>
     * El usuario elige una receta, no una versión: las versiones son un detalle interno para
     * preservar la trazabilidad de producción. Es responsabilidad de este método resolver cuál
     * es la última versión activa de la receta indicada y asociar esa versión puntual a la planificación.
     * </p>
     *
     * @param planificacionProduccionFormDTO Objeto DTO que contiene los datos de creación de la planificación de producción.
     * @return {@link PlanificacionProduccionResponseDTO} representativo de la planificación de producción guardada en la base de datos, en estado {@code PENDIENTE}.
     * @throws ReglaNegocioException Si la cantidad a producir no es mayor a 0, o si la fecha de finalización estimada es anterior a la fecha de inicio estimada.
     * @throws RecursoNoEncontradoException Si la receta referenciada no existe, o si no tiene ninguna versión activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PLANIFICACION_PRODUCCION)
    public PlanificacionProduccionResponseDTO registrarPlanificacionProduccion(PlanificacionProduccionFormDTO planificacionProduccionFormDTO) {
        // 1. Validar la cantidad a producir
        if (planificacionProduccionFormDTO.getCantidadAProducir() == null || planificacionProduccionFormDTO.getCantidadAProducir() <= 0) {
            throw new ReglaNegocioException("La cantidad a producir debe ser mayor a 0");
        }

        // 2. Validar que la fecha de finalización estimada no sea anterior a la fecha de inicio estimada
        LocalDate fechaInicioEstimada = planificacionProduccionFormDTO.getFechaInicioEstimada();
        LocalDate fechaFinalizacionEstimada = planificacionProduccionFormDTO.getFechaFinalizacionEstimada();
        if (fechaInicioEstimada == null || fechaFinalizacionEstimada == null || fechaFinalizacionEstimada.isBefore(fechaInicioEstimada)) {
            throw new ReglaNegocioException("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada");
        }

        // 3. Resolver la última versión activa de la receta seleccionada
        VersionRecetaEntity versionRecetaEntity = obtenerVersionActiva(planificacionProduccionFormDTO.getIdReceta());

        // 4. Crear la entidad de la planificación de producción a partir del DTO de formulario
        PlanificacionProduccionEntity planificacionProduccionEntity = PlanificacionProduccionEntity.builder()
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionEstimada(fechaFinalizacionEstimada)
                .cantidadAProducir(planificacionProduccionFormDTO.getCantidadAProducir())
                .estado(EstadoSolicitud.PENDIENTE)
                .versionReceta(versionRecetaEntity)
                .build();

        // 5. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.save(planificacionProduccionEntity));
    }

    /**
     * Anula una planificación de producción existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre planificaciones en estado {@code PENDIENTE}:
     * una planificación ya finalizada o ya anulada no puede volver a anularse.
     * </p>
     *
     * @param id Identificador clave primaria de la planificación de producción a anular.
     * @param anulacionFormDTO DTO que contiene el motivo de la anulación.
     * @return {@link PlanificacionProduccionResponseDTO} representativo de la planificación de producción anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, o si la planificación no se encuentra en estado {@code PENDIENTE}.
     * @throws RecursoNoEncontradoException Si la planificación de producción con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.PLANIFICACION_PRODUCCION)
    public PlanificacionProduccionResponseDTO anularPlanificacionProduccion(Long id, AnulacionPlanificacionProduccionFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la planificación de producción. Si no existe, se dispara RecursoNoEncontradoException
        PlanificacionProduccionEntity planificacionProduccionEntity = planificacionProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + id));

        // 3. Validar que la planificación se encuentre en estado PENDIENTE
        if (planificacionProduccionEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden anular planificaciones de producción en estado PENDIENTE");
        }

        // TODO: verifica que la Planificación de Producción no tenga asociado Lotes en estado pendiente o En ejecución

        // 4. Aplicar la anulación sobre la entidad administrada por persistencia
        planificacionProduccionEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());
        planificacionProduccionEntity.setFechaAnulacion(LocalDateTime.now());
        planificacionProduccionEntity.setEstado(EstadoSolicitud.ANULADA);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.save(planificacionProduccionEntity));
    }

    /**
     * Finaliza de forma forzada una planificación de producción existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre planificaciones en estado {@code PENDIENTE}:
     * una planificación ya finalizada o ya anulada no puede volver a finalizarse. La finalización normal
     * (cantidad producida mayor o igual a la cantidad solicitada) es responsabilidad del módulo
     * de lotes, no de este método.
     * </p>
     *
     * @param id Identificador clave primaria de la planificación de producción a finalizar.
     * @param finalizacionFormDTO DTO que contiene el motivo de la finalización forzada.
     * @return {@link PlanificacionProduccionResponseDTO} representativo de la planificación de producción finalizada.
     * @throws ReglaNegocioException Si el motivo de finalización no fue informado, o si la planificación no se encuentra en estado {@code PENDIENTE}.
     * @throws RecursoNoEncontradoException Si la planificación de producción con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.FINALIZAR, conceptoAuditoria = ConceptoAuditoria.PLANIFICACION_PRODUCCION)
    public PlanificacionProduccionResponseDTO finalizarPlanificacionProduccion(Long id, FinalizacionForzadaPlanificacionProduccionFormDTO finalizacionFormDTO) {
        // 1. Validar que se haya informado el motivo de finalización
        if (finalizacionFormDTO.getMotivoFinalizacion() == null || finalizacionFormDTO.getMotivoFinalizacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de finalización es obligatorio");
        }

        // 2. Localizar la planificación de producción. Si no existe, se dispara RecursoNoEncontradoException
        PlanificacionProduccionEntity planificacionProduccionEntity = planificacionProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + id));

        // 3. Validar que la planificación se encuentre en estado PENDIENTE
        if (planificacionProduccionEntity.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden finalizar planificaciones de producción en estado PENDIENTE");
        }

        // TODO: verifica que la planificación de producción tenga al menos un Lote finalizado asociado.
        // TODO: verifica que la Planificación de Producción no tenga asociado Lotes en estado pendiente o En ejecución

        // 4. Aplicar la finalización forzada sobre la entidad administrada por persistencia
        planificacionProduccionEntity.setMotivoFinalizacion(finalizacionFormDTO.getMotivoFinalizacion());
        planificacionProduccionEntity.setFechaFinalizacion(LocalDateTime.now());
        planificacionProduccionEntity.setEstado(EstadoSolicitud.FINALIZADA);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperPlanificacionProduccion.toDTO(planificacionProduccionRepository.save(planificacionProduccionEntity));
    }

    /**
     * Resuelve la última versión activa ({@code esUltimaVersion = true}) de la receta indicada.
     *
     * @param idReceta Identificador de la receta cuya última versión activa se quiere obtener.
     * @return Entidad de la última versión activa de la receta.
     * @throws RecursoNoEncontradoException Si la receta no existe, o si no tiene ninguna versión activa.
     */
    private VersionRecetaEntity obtenerVersionActiva(Long idReceta) {
        RecetaEntity recetaEntity = recetaRepository.findById(idReceta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la receta con ID: " + idReceta));

        return recetaEntity.getVersiones().stream()
                .filter(VersionRecetaEntity::isEsUltimaVersion)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró una versión activa para la receta con ID: " + idReceta));
    }
}
