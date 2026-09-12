package com.github.heikyudev.maestrocervecero.service.implementation.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.parametro_control.IParametroControlRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control.ParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.parametro_control.IParametroControlServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.parametro_control.MapperParametroControl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParametroControlServicioImpl implements IParametroControlServicio {

    private final IParametroControlRepository parametroControlRepository;

    /**
     * Recupera una página de parámetros de control activos registrados en el sistema, filtrados
     * opcionalmente por nombre (coincidencia parcial, sin distinguir mayúsculas/minúsculas).
     * <p>
     * Los parámetros de control dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre del parámetro de control, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ParametroControlResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ParametroControlResponseDTO> filtrarParametrosControl(String nombre, Pageable pageable) {
        return parametroControlRepository.filtrarParametrosControl(nombre, pageable).map(MapperParametroControl::toDTO);
    }

    /**
     * Busca y retorna un parámetro de control específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del parámetro de control buscado.
     * @return Objeto {@link ParametroControlResponseDTO} con la información del parámetro de control encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún parámetro de control activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public ParametroControlResponseDTO buscarPorId(Long id) {
        return MapperParametroControl.toDTO(parametroControlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el parámetro de control con ID: " + id)));
    }

    /**
     * Registra un nuevo parámetro de control en el sistema.
     * <p>
     * Valida que el valor mínimo sea positivo (mayor a 0) y que no sea mayor al valor máximo, y
     * verifica que no exista otro parámetro de control activo con el mismo nombre (case-insensitive).
     * </p>
     *
     * @param parametroControlFormDTO Objeto DTO que contiene los datos de creación del parámetro de control.
     * @return {@link ParametroControlResponseDTO} representativo del parámetro de control guardado en la base de datos.
     * @throws ReglaNegocioException Si el valor mínimo no es positivo (mayor a 0) o es mayor al valor máximo.
     * @throws RecursoDuplicadoException Si el nombre provisto ya pertenece a un parámetro de control activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.PARAMETRO_CONTROL)
    public ParametroControlResponseDTO altaParametroControl(ParametroControlFormDTO parametroControlFormDTO) {
        // 1. Validar la regla de negocio del rango de valores (mínimo positivo y no mayor al máximo)
        validarRangoValores(parametroControlFormDTO.getValorMinimo(), parametroControlFormDTO.getValorMaximo());

        // 2. Validar si el nombre ya está registrado en otro parámetro de control (case-insensitive)
        if (parametroControlRepository.existsByNombreIgnoreCase(parametroControlFormDTO.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe un parámetro de control con el nombre '" + parametroControlFormDTO.getNombre() + "'");
        }

        // 3. Crear la entidad del parámetro de control a partir del DTO de formulario
        ParametroControlEntity parametroControlEntity = ParametroControlEntity.builder()
                .nombre(parametroControlFormDTO.getNombre())
                .descripcion(parametroControlFormDTO.getDescripcion())
                .valorMinimo(parametroControlFormDTO.getValorMinimo())
                .valorMaximo(parametroControlFormDTO.getValorMaximo())
                .estado(Estado.ACTIVO)
                .build();

        // 4. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperParametroControl.toDTO(parametroControlRepository.save(parametroControlEntity));
    }

    /**
     * Modifica un parámetro de control existente en el sistema.
     * <p>
     * Valida que el valor mínimo sea positivo (mayor a 0) y que no sea mayor al valor máximo, y
     * verifica que no exista otro parámetro de control activo con el mismo nombre (case-insensitive),
     * excluyendo de la verificación al propio parámetro de control que se está modificando.
     * </p>
     *
     * @param id Identificador clave primaria del parámetro de control a modificar.
     * @param parametroControlFormDTO DTO que contiene los nuevos datos del parámetro de control.
     * @return {@link ParametroControlResponseDTO} representativo del parámetro de control con los cambios aplicados.
     * @throws ReglaNegocioException Si el valor mínimo no es positivo (mayor a 0) o es mayor al valor máximo.
     * @throws RecursoNoEncontradoException Si no se localiza un parámetro de control activo por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo nombre ya se encuentra asignado a otro parámetro de control activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.PARAMETRO_CONTROL)
    public ParametroControlResponseDTO modificarParametroControl(Long id, ParametroControlFormDTO parametroControlFormDTO) {
        // 1. Validar la regla de negocio del rango de valores (mínimo positivo y no mayor al máximo)
        validarRangoValores(parametroControlFormDTO.getValorMinimo(), parametroControlFormDTO.getValorMaximo());

        // 2. Validar duplicación excluyendo el propio ID, de modo que conservar el nombre actual
        //    no falle contra el mismo registro
        if (parametroControlRepository.existsByNombreIgnoreCaseAndIdNot(parametroControlFormDTO.getNombre(), id)) {
            throw new RecursoDuplicadoException("Ya existe un parámetro de control con el nombre '" + parametroControlFormDTO.getNombre() + "'");
        }

        // 3. Localizar el parámetro de control existente. Si no existe, se dispara RecursoNoEncontradoException
        ParametroControlEntity parametroControlEntity = parametroControlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el parámetro de control con ID: " + id));

        // 4. Aplicar los cambios sobre la entidad administrada por persistencia
        parametroControlEntity.setNombre(parametroControlFormDTO.getNombre());
        parametroControlEntity.setDescripcion(parametroControlFormDTO.getDescripcion());
        parametroControlEntity.setValorMinimo(parametroControlFormDTO.getValorMinimo());
        parametroControlEntity.setValorMaximo(parametroControlFormDTO.getValorMaximo());

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperParametroControl.toDTO(parametroControlRepository.save(parametroControlEntity));
    }

    /**
     * Procesa la baja lógica de un parámetro de control existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al parámetro de control con {@link Estado#BAJA} y
     * persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan de
     * encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del parámetro de control a dar de baja.
     * @return {@link ParametroControlResponseDTO} con los datos del parámetro de control ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el parámetro de control con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el parámetro de control está asociado a un plan de monitoreo de una receta activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.PARAMETRO_CONTROL)
    public ParametroControlResponseDTO bajaParametroControl(Long id) {
        // 1. Buscar el parámetro de control. Si no existe, se dispara RecursoNoEncontradoException
        ParametroControlEntity parametroControlEntity = parametroControlRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el parámetro de control con ID: " + id));

        // 2. Validar que el parámetro de control no esté asociado a un plan de monitoreo de una receta activa
        if (parametroControlRepository.existsPlanMonitoreoActivoAsociado(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el parámetro de control porque está asociado al plan de monitoreo de una receta activa");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        parametroControlEntity.setEstado(Estado.BAJA);
        parametroControlRepository.save(parametroControlEntity);

        // 4. Retornar el DTO del parámetro de control dado de baja
        return MapperParametroControl.toDTO(parametroControlEntity);
    }

    /**
     * Valida la regla de negocio del rango de valores aceptables del parámetro de control:
     * el valor mínimo debe ser positivo (mayor a 0) y no puede ser mayor al valor máximo.
     *
     * @param valorMinimo Valor mínimo a validar.
     * @param valorMaximo Valor máximo a validar.
     * @throws ReglaNegocioException Si el valor mínimo es nulo o no es positivo (mayor a 0), o si es mayor al valor máximo.
     */
    private static void validarRangoValores(Double valorMinimo, Double valorMaximo) {
        if (valorMinimo == null || valorMinimo <= 0) {
            throw new ReglaNegocioException("El valor mínimo debe ser mayor a 0");
        }
        if (valorMaximo == null || valorMinimo > valorMaximo) {
            throw new ReglaNegocioException("El valor mínimo no puede ser mayor al valor máximo");
        }
    }
}
