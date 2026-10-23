package com.github.heikyudev.maestrocervecero.service.implementation.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional.ICostoDirectoAdicionalRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.costo_adicional.CostoDirectoAdicionalFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.costo_adicional.ICostoDirectoAdicionalServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.costo_adicional.MapperCostoDirectoAdicional;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CostoDirectoAdicionalServicioImpl implements ICostoDirectoAdicionalServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos costos directos adicionales activos con el mismo nombre.
     */
    private static final String INDICE_NOMBRE = "udx_costo_directo_adicional_nombre";

    private final ICostoDirectoAdicionalRepository costoDirectoAdicionalRepository;

    /**
     * Filtra los costos directos adicionales activos, opcionalmente por nombre.
     * <p>
     * Los costos directos adicionales dados de baja son excluidos por la condición
     * {@code estado = 'ACTIVO'} aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link CostoDirectoAdicionalResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CostoDirectoAdicionalResponseDTO> filtrarCostosDirectosAdicionales(String nombre, Pageable pageable) {
        // El texto buscado se normaliza igual que el nombre guardado: sin espacios de más
        String nombreNormalizado = NormalizacionTexto.nombrePropio(nombre);
        if (nombreNormalizado != null && nombreNormalizado.isEmpty()) {
            nombreNormalizado = null;
        }
        return costoDirectoAdicionalRepository.filtrarCostosDirectosAdicionales(nombreNormalizado, pageable).map(MapperCostoDirectoAdicional::toDTO);
    }

    /**
     * Busca y retorna un costo directo adicional activo mediante su identificador único.
     *
     * @param id El ID del costo directo adicional.
     * @return El costo directo adicional correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún costo directo adicional activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public CostoDirectoAdicionalResponseDTO buscarPorId(Long id) {
        return MapperCostoDirectoAdicional.toDTO(costoDirectoAdicionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el costo directo adicional con ID: " + id)));
    }

    /**
     * Registra un nuevo costo directo adicional en el sistema.
     *
     * @param costoDirectoAdicionalFormDTO Los datos del costo directo adicional a registrar.
     * @return El costo directo adicional registrado.
     * @throws RecursoDuplicadoException Si ya existe un costo directo adicional activo con el mismo nombre (case-insensitive).
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, o si el costo por litro es nulo o menor o igual a cero.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.COSTO_DIRECTO_ADICIONAL)
    public CostoDirectoAdicionalResponseDTO altaCostoDirectoAdicional(CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(costoDirectoAdicionalFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Validar que no exista otro costo directo adicional con el mismo nombre (case-insensitive)
        if (costoDirectoAdicionalRepository.existsByNombreIgnoreCase(nombre)) {
            throw nombreDuplicado(nombre);
        }

        // 4. Validar que el costo por litro sea mayor a cero
        validarCostoPorLitro(costoDirectoAdicionalFormDTO.getCostoPorLitro());

        // 5. Construir la entidad que se va a almacenar en la base de datos
        CostoDirectoAdicionalEntity costoDirectoAdicionalEntity = CostoDirectoAdicionalEntity.builder()
                .nombre(nombre)
                .costoPorLitro(costoDirectoAdicionalFormDTO.getCostoPorLitro())
                .estado(Estado.ACTIVO)
                .build();

        // 6. Guardar la entidad (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y retornar el DTO de respuesta correspondiente
        return MapperCostoDirectoAdicional.toDTO(GuardadoUnico.guardar(costoDirectoAdicionalRepository, costoDirectoAdicionalEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicado(nombre))));
    }

    /**
     * Modifica un costo directo adicional existente en el sistema.
     *
     * @param id El ID del costo directo adicional a modificar.
     * @param costoDirectoAdicionalFormDTO Los nuevos datos del costo directo adicional.
     * @return El costo directo adicional modificado.
     * @throws RecursoNoEncontradoException Si no existe un costo directo adicional activo con el ID especificado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya pertenece a otro costo directo adicional activo (case-insensitive).
     * @throws ReglaNegocioException Si el nombre falta o supera los 100 caracteres, o si el costo por litro es nulo o menor o igual a cero.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.COSTO_DIRECTO_ADICIONAL)
    public CostoDirectoAdicionalResponseDTO modificarCostoDirectoAdicional(Long id, CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO) {
        // 1. Normalizar el nombre sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(costoDirectoAdicionalFormDTO.getNombre());

        // 2. Validar que el nombre sea obligatorio y no supere la longitud máxima
        validarNombre(nombre);

        // 3. Localizar el costo directo adicional existente. Si no existe, se dispara RecursoNoEncontradoException
        CostoDirectoAdicionalEntity costoDirectoAdicionalEntity = costoDirectoAdicionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el costo directo adicional con ID: " + id));

        // 4. Validar que no exista otro costo directo adicional con el mismo nombre, excluyendo el propio ID
        if (costoDirectoAdicionalRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw nombreDuplicado(nombre);
        }

        // 5. Validar que el costo por litro sea mayor a cero
        validarCostoPorLitro(costoDirectoAdicionalFormDTO.getCostoPorLitro());

        // 6. Recién si todas las validaciones pasaron, aplicar los cambios sobre la entidad
        costoDirectoAdicionalEntity.setNombre(nombre);
        costoDirectoAdicionalEntity.setCostoPorLitro(costoDirectoAdicionalFormDTO.getCostoPorLitro());

        // 7. Persistir la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y retornar el DTO de respuesta correspondiente
        return MapperCostoDirectoAdicional.toDTO(GuardadoUnico.guardar(costoDirectoAdicionalRepository, costoDirectoAdicionalEntity,
                Map.of(INDICE_NOMBRE, () -> nombreDuplicado(nombre))));
    }

    /**
     * Procesa la baja lógica de un costo directo adicional existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al costo directo adicional con {@link Estado#BAJA}
     * y persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan
     * de encontrarlo.
     * </p>
     *
     * @param id El ID del costo directo adicional a dar de baja.
     * @return El costo directo adicional dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un costo directo adicional activo con el ID especificado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.COSTO_DIRECTO_ADICIONAL)
    public CostoDirectoAdicionalResponseDTO bajaCostoDirectoAdicional(Long id) {
        // 1. Localizar el costo directo adicional. Si no existe, se dispara RecursoNoEncontradoException
        CostoDirectoAdicionalEntity costoDirectoAdicionalEntity = costoDirectoAdicionalRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el costo directo adicional con ID: " + id));

        // 2. Ejecutar la baja lógica y persistir el cambio
        costoDirectoAdicionalEntity.setEstado(Estado.BAJA);
        costoDirectoAdicionalRepository.save(costoDirectoAdicionalEntity);

        // 3. Retornar el DTO del costo directo adicional dado de baja
        return MapperCostoDirectoAdicional.toDTO(costoDirectoAdicionalEntity);
    }

    /**
     * Valida la regla de negocio del costo por litro.
     *
     * @param costoPorLitro Costo por litro a validar.
     * @throws ReglaNegocioException Si el costo por litro es nulo o menor o igual a cero.
     */
    private static void validarCostoPorLitro(BigDecimal costoPorLitro) {
        if (costoPorLitro == null || costoPorLitro.signum() <= 0) {
            throw new ReglaNegocioException("El costo por litro debe ser mayor a cero");
        }
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

    private static RecursoDuplicadoException nombreDuplicado(String nombre) {
        return new RecursoDuplicadoException("Ya existe un costo directo adicional con el nombre '" + nombre + "'");
    }
}
