package com.github.heikyudev.maestrocervecero.service.implementation.orden_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_produccion.OrdenProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoOrden;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_produccion.IOrdenProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion.AnulacionOrdenProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion.FinalizacionForzadaOrdenProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_produccion.OrdenProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_produccion.IOrdenProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_produccion.OrdenProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.orden_produccion.MapperOrdenProduccion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServicioImpl implements IOrdenProduccionServicio {

    private final IOrdenProduccionRepository ordenProduccionRepository;
    private final IRecetaRepository recetaRepository;

    /**
     * Recupera una página de órdenes de producción activas registradas en el sistema.
     * <p>
     * Las órdenes de producción eliminadas lógicamente son excluidas automáticamente por el
     * {@code @SoftDelete} de Hibernate sobre la entidad.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link OrdenProduccionResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OrdenProduccionResponseDTO> buscarTodos(Pageable pageable) {
        return ordenProduccionRepository.findAll(pageable).map(MapperOrdenProduccion::toDTO);
    }

    /**
     * Busca y retorna una orden de producción específica mediante su identificador único.
     *
     * @param id Identificador clave primaria de la orden de producción buscada.
     * @return Objeto {@link OrdenProduccionResponseDTO} con la información de la orden de producción encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna orden de producción activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public OrdenProduccionResponseDTO buscarPorId(Long id) {
        return MapperOrdenProduccion.toDTO(ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de producción con ID: " + id)));
    }

    /**
     * Registra una nueva orden de producción en el sistema.
     * <p>
     * El usuario elige una receta, no una versión: las versiones son un detalle interno para
     * preservar la trazabilidad de producción. Es responsabilidad de este método resolver cuál
     * es la última versión activa de la receta indicada y asociar esa versión puntual a la orden.
     * </p>
     *
     * @param ordenProduccionFormDTO Objeto DTO que contiene los datos de creación de la orden de producción.
     * @return {@link OrdenProduccionResponseDTO} representativo de la orden de producción guardada en la base de datos, en estado {@code PENDIENTE}.
     * @throws ReglaNegocioException Si la cantidad a producir no es mayor a 0, o si la fecha de finalización estimada es anterior a la fecha de inicio estimada.
     * @throws RecursoNoEncontradoException Si la receta referenciada no existe, o si no tiene ninguna versión activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_PRODUCCION)
    public OrdenProduccionResponseDTO registrarOrdenProduccion(OrdenProduccionFormDTO ordenProduccionFormDTO) {
        // 1. Validar la cantidad a producir
        if (ordenProduccionFormDTO.getCantidadAProducir() == null || ordenProduccionFormDTO.getCantidadAProducir() <= 0) {
            throw new ReglaNegocioException("La cantidad a producir debe ser mayor a 0");
        }

        // 2. Validar que la fecha de finalización estimada no sea anterior a la fecha de inicio estimada
        LocalDate fechaInicioEstimada = ordenProduccionFormDTO.getFechaInicioEstimada();
        LocalDate fechaFinalizacionEstimada = ordenProduccionFormDTO.getFechaFinalizacionEstimada();
        if (fechaInicioEstimada == null || fechaFinalizacionEstimada == null || fechaFinalizacionEstimada.isBefore(fechaInicioEstimada)) {
            throw new ReglaNegocioException("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada");
        }

        // 3. Resolver la última versión activa de la receta seleccionada
        VersionRecetaEntity versionRecetaEntity = obtenerVersionActiva(ordenProduccionFormDTO.getIdReceta());

        // 4. Crear la entidad de la orden de producción a partir del DTO de formulario
        OrdenProduccionEntity ordenProduccionEntity = OrdenProduccionEntity.builder()
                .fechaInicioEstimada(fechaInicioEstimada)
                .fechaFinalizacionEstimada(fechaFinalizacionEstimada)
                .cantidadAProducir(ordenProduccionFormDTO.getCantidadAProducir())
                .estado(EstadoOrden.PENDIENTE)
                .versionReceta(versionRecetaEntity)
                .build();

        // 5. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperOrdenProduccion.toDTO(ordenProduccionRepository.save(ordenProduccionEntity));
    }

    /**
     * Anula una orden de producción existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre órdenes en estado {@code PENDIENTE}:
     * una orden ya finalizada o ya anulada no puede volver a anularse.
     * </p>
     *
     * @param id Identificador clave primaria de la orden de producción a anular.
     * @param anulacionFormDTO DTO que contiene el motivo de la anulación.
     * @return {@link OrdenProduccionResponseDTO} representativo de la orden de producción anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, o si la orden no se encuentra en estado {@code PENDIENTE}.
     * @throws RecursoNoEncontradoException Si la orden de producción con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_PRODUCCION)
    public OrdenProduccionResponseDTO anularOrdenProduccion(Long id, AnulacionOrdenProduccionFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la orden de producción. Si no existe, se dispara RecursoNoEncontradoException
        OrdenProduccionEntity ordenProduccionEntity = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de producción con ID: " + id));

        // 3. Validar que la orden se encuentre en estado PENDIENTE
        if (ordenProduccionEntity.getEstado() != EstadoOrden.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden anular órdenes de producción en estado PENDIENTE");
        }

        // TODO: verifica que la Orden de Producción no tenga asociado Lotes en estado pendiente o En ejecución

        // 4. Aplicar la anulación sobre la entidad administrada por persistencia
        ordenProduccionEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());
        ordenProduccionEntity.setFechaAnulacion(LocalDateTime.now());
        ordenProduccionEntity.setEstado(EstadoOrden.ANULADA);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperOrdenProduccion.toDTO(ordenProduccionRepository.save(ordenProduccionEntity));
    }

    /**
     * Finaliza de forma forzada una orden de producción existente en el sistema.
     * <p>
     * Al ser una operación transaccional, solo procede sobre órdenes en estado {@code PENDIENTE}:
     * una orden ya finalizada o ya anulada no puede volver a finalizarse. La finalización normal
     * (cantidad producida mayor o igual a la cantidad solicitada) es responsabilidad del módulo
     * de lotes, no de este método.
     * </p>
     *
     * @param id Identificador clave primaria de la orden de producción a finalizar.
     * @param finalizacionFormDTO DTO que contiene el motivo de la finalización forzada.
     * @return {@link OrdenProduccionResponseDTO} representativo de la orden de producción finalizada.
     * @throws ReglaNegocioException Si el motivo de finalización no fue informado, o si la orden no se encuentra en estado {@code PENDIENTE}.
     * @throws RecursoNoEncontradoException Si la orden de producción con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.FINALIZAR, conceptoAuditoria = ConceptoAuditoria.ORDEN_PRODUCCION)
    public OrdenProduccionResponseDTO finalizarOrdenProduccion(Long id, FinalizacionForzadaOrdenProduccionFormDTO finalizacionFormDTO) {
        // 1. Validar que se haya informado el motivo de finalización
        if (finalizacionFormDTO.getMotivoFinalizacion() == null || finalizacionFormDTO.getMotivoFinalizacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de finalización es obligatorio");
        }

        // 2. Localizar la orden de producción. Si no existe, se dispara RecursoNoEncontradoException
        OrdenProduccionEntity ordenProduccionEntity = ordenProduccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden de producción con ID: " + id));

        // 3. Validar que la orden se encuentre en estado PENDIENTE
        if (ordenProduccionEntity.getEstado() != EstadoOrden.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden finalizar órdenes de producción en estado PENDIENTE");
        }

        // TODO: verifica que la orden de producción tenga al menos un Lote finalizado asociado.
        // TODO: verifica que la Orden de Producción no tenga asociado Lotes en estado pendiente o En ejecución

        // 4. Aplicar la finalización forzada sobre la entidad administrada por persistencia
        ordenProduccionEntity.setMotivoFinalizacion(finalizacionFormDTO.getMotivoFinalizacion());
        ordenProduccionEntity.setFechaFinalizacion(LocalDateTime.now());
        ordenProduccionEntity.setEstado(EstadoOrden.FINALIZADA);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperOrdenProduccion.toDTO(ordenProduccionRepository.save(ordenProduccionEntity));
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
