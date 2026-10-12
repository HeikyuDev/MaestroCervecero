package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IAjusteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IMotivoAjusteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperMotivoAjuste;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MotivoAjusteServicioImpl implements IMotivoAjusteServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos motivos de ajuste activos con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_motivo_ajuste_nombre";

    private final IMotivoAjusteRepository motivoAjusteRepository;
    private final IAjusteInsumoRepository ajusteInsumoRepository;

    /**
     * Filtra los motivos de ajuste activos, opcionalmente por nombre y/o tipo de ajuste.
     * <p>
     * Los motivos de ajuste dados de baja son excluidos por la condición
     * {@code estado = 'ACTIVO'} aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre del motivo de ajuste, o {@code null} para no filtrar por nombre.
     * @param tipoAjuste Tipo de ajuste exacto a filtrar (INGRESO/EGRESO), o {@code null} para no filtrar por tipo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MotivoAjusteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MotivoAjusteResponseDTO> filtrarMotivosAjuste(String nombre, TipoAjuste tipoAjuste, Pageable pageable) {
        return motivoAjusteRepository.filtrarMotivosAjuste(nombre, tipoAjuste, pageable).map(MapperMotivoAjuste::toDTO);
    }

    /**
     * Busca y retorna un motivo de ajuste activo mediante su identificador único.
     *
     * @param id El ID del motivo de ajuste.
     * @return El motivo de ajuste correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún motivo de ajuste activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MotivoAjusteResponseDTO buscarPorId(Long id) {
        return MapperMotivoAjuste.toDTO(motivoAjusteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + id)));
    }

    /**
     * Registra un nuevo motivo de ajuste en el sistema.
     *
     * @param motivoAjusteFormDTO Los datos del motivo de ajuste a registrar.
     * @return El motivo de ajuste registrado.
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres.
     * @throws RecursoDuplicadoException Si ya existe un motivo de ajuste activo con el mismo nombre (case-insensitive).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MOTIVO_AJUSTE)
    public MotivoAjusteResponseDTO altaMotivoAjuste(MotivoAjusteFormDTO motivoAjusteFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(motivoAjusteFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar que no exista otro motivo de ajuste con el mismo nombre (case-insensitive)
        if (motivoAjusteRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicadoEnAlta(nombre);
        }

        // 4. Construir la entidad que se va a almacenar en la base de datos
        MotivoAjusteEntity motivoAjusteEntity = MotivoAjusteEntity.builder()
                .nombre(nombre)
                .tipoAjuste(motivoAjusteFormDTO.getTipoAjuste())
                .estado(Estado.ACTIVO)
                .build();

        // 5. Guardar la entidad (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y retornar el DTO de respuesta correspondiente
        return MapperMotivoAjuste.toDTO(GuardadoUnico.guardar(motivoAjusteRepository, motivoAjusteEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnAlta(nombre))));
    }

    /**
     * Modifica un motivo de ajuste existente en el sistema.
     *
     * @param id El ID del motivo de ajuste a modificar.
     * @param motivoAjusteFormDTO Los nuevos datos del motivo de ajuste.
     * @return El motivo de ajuste modificado.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro motivo de ajuste activo (case-insensitive).
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, o si el motivo de ajuste tiene al menos un ajuste de insumo en estado REGISTRADO.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MOTIVO_AJUSTE)
    public MotivoAjusteResponseDTO modificarMotivoAjuste(Long id, MotivoAjusteFormDTO motivoAjusteFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(motivoAjusteFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Localizar el motivo de ajuste existente. Si no existe, se dispara RecursoNoEncontradoException
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + id));

        // 4. Validar que el motivo no tenga ningún ajuste de insumo REGISTRADO: no existe un caso
        //    donde un mismo motivo a veces sume y a veces reste, así que una vez usado, su tipo
        //    (INGRESO/EGRESO) queda fijo mientras esos ajustes sigan vigentes.
        if (ajusteInsumoRepository.existsByMotivoAjusteIdAndEstadoRegistrado(id)) {
            throw new ReglaNegocioException("No se puede modificar un motivo de ajuste que tiene ajustes de insumo registrados");
        }

        // 5. Validar que no exista otro motivo de ajuste con el mismo nombre, excluyendo el propio ID
        if (motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicadoEnModificacion(nombre);
        }

        // 6. Recién si todas las validaciones pasaron, aplicar los cambios sobre la entidad
        motivoAjusteEntity.setNombre(nombre);
        motivoAjusteEntity.setTipoAjuste(motivoAjusteFormDTO.getTipoAjuste());

        // 7. Persistir la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y retornar el DTO de respuesta correspondiente
        return MapperMotivoAjuste.toDTO(GuardadoUnico.guardar(motivoAjusteRepository, motivoAjusteEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicadoEnModificacion(nombre))));
    }

    /**
     * Procesa la baja lógica de un motivo de ajuste existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al motivo de ajuste con {@link Estado#BAJA} y
     * persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan de
     * encontrarlo.
     * </p>
     *
     * @param id El ID del motivo de ajuste a dar de baja.
     * @return El motivo de ajuste dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un motivo de ajuste activo con el ID especificado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MOTIVO_AJUSTE)
    public MotivoAjusteResponseDTO bajaMotivoAjuste(Long id) {
        // 1. Localizar el motivo de ajuste. Si no existe, se dispara RecursoNoEncontradoException
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + id));

        // 2. Ejecutar la baja lógica y persistir el cambio
        motivoAjusteEntity.setEstado(Estado.BAJA);
        motivoAjusteRepository.save(motivoAjusteEntity);

        // 3. Retornar el DTO del motivo de ajuste dado de baja
        return MapperMotivoAjuste.toDTO(motivoAjusteEntity);
    }

    /**
     * Valida que el nombre normalizado sea obligatorio y no supere la longitud máxima.
     *
     * @param nombre Nombre normalizado.
     * @throws ReglaNegocioException Si el nombre está vacío o supera los 100 caracteres.
     */
    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            throw new ReglaNegocioException("El nombre es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(nombre, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnAlta(String nombre) {
        return new RecursoDuplicadoException("Ya existe un motivo de ajuste con el nombre '" + nombre + "'");
    }

    private static RecursoDuplicadoException nombreDuplicadoEnModificacion(String nombre) {
        return new RecursoDuplicadoException("El nombre '" + nombre + "' ya está en uso por otro motivo de ajuste");
    }
}
