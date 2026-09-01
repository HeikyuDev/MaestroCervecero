package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IOllaHervorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperOllaHervor;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OllaHervorServicioImpl implements IOllaHervorServicio {

    private final IOllaHervorRepository ollaHervorRepository;
    private final IEquipamientoRepository equipamientoRepository;

    /**
     * Recupera una página de Ollas de Hervor activas registrados en el sistema.
     * <p>
     * Las ollas de hervor dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link OllaHervorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OllaHervorResponseDTO> buscarTodos(Pageable pageable) {
        return  ollaHervorRepository.findAll(pageable).map(MapperOllaHervor::toDTO);
    }

    /**
     * Busca y retorna una olla de hervor específico mediante su identificador único.
     *
     * @param id Identificador clave primaria de la olla de hervor buscada.
     * @return Objeto {@link OllaHervorResponseDTO} con la información de la olla de hervor encontrada.
     * @throws RecursoNoEncontradoException Si no existe ninguna olla de hervor activa con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public OllaHervorResponseDTO buscarPorId(Long id) {
        return MapperOllaHervor.toDTO(ollaHervorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la Olla de Hervor con ID:" + id)));
    }

    /**
     * Registra una nueva olla de hervor en el sistema.
     * <p>
     * Valida que el identificador interno no esté duplicado (case-insensitive) entre los equipamientos activos,
     * </p>
     *
     * @param ollaHervorFormDTO Objeto DTO que contiene los datos de creación de la olla de hervor.
     * @return {@link OllaHervorResponseDTO} representativo de la olla de hervor guardada en la base de datos.
     * @throws ReglaNegocioException Si la capacidad util es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si el porcentaje de evaporacion no esta entre 0 y 100.
     * @throws ReglaNegocioException Si la perdida por trub es negativa.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a una olla de hervor activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR,conceptoAuditoria = ConceptoAuditoria.OLLA_DE_HERVOR)
    public OllaHervorResponseDTO altaOllaHervor(OllaHervorFormDTO ollaHervorFormDTO) {

        // 1. Validar que la capacidad util no sea mayor o igual a la capacidad total
        MetodosEquipamiento.validarCapacidadUtil(ollaHervorFormDTO.getCapacidadTotal(), ollaHervorFormDTO.getCapacidadUtil());

        // 2. Validar que el porcentaje de evaporación esté entre 0 y 100
        validarPorcentajeEvaporacion(ollaHervorFormDTO.getEvaporacion());

        // 3. Validar que la pérdida por trub no sea negativa
        validarPerdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub());

        // 4. Validar que no exista otra olla de hervor con el mismo identificador interno (ignorando mayúsculas y minúsculas)
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(ollaHervorFormDTO.getIdentificadorInterno())) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + ollaHervorFormDTO.getIdentificadorInterno() + "'");
        }

        // 5. Crear la entidad OllaHervorEntity a partir del DTO de formulario
        OllaHervorEntity ollaHervorEntity = OllaHervorEntity.builder()
                .identificadorInterno(ollaHervorFormDTO.getIdentificadorInterno())
                .descripcion(ollaHervorFormDTO.getDescripcion())
                // Cuando se da de alta una nueva olla de hervor, su estado operativo inicial es DISPONIBLE.
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(ollaHervorFormDTO.getCapacidadTotal())
                .capacidadUtil(ollaHervorFormDTO.getCapacidadUtil())
                .evaporacion(ollaHervorFormDTO.getEvaporacion())
                .perdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub())
                .estado(Estado.ACTIVO)
                .build();

        // 6. Guardar la entidad en la base de datos y retonar el DTO de respuesta correspondiente
        return MapperOllaHervor.toDTO(ollaHervorRepository.save(ollaHervorEntity));
    }


    /**
     * Modifica los datos de una olla de hervor existente en el sistema.
     * <p>
     * Valida que el identificador interno no esté duplicado (case-insensitive) entre los equipamientos activos,
     * ignorando la olla de hervor que se está modificando.
     * </p>
     *
     * @param id                Identificador clave primaria de la olla de hervor a modificar.
     * @param ollaHervorFormDTO Objeto DTO que contiene los datos de modificación de la olla de hervor.
     * @return {@link OllaHervorResponseDTO} representativo de la olla de hervor modificada en la base de datos.
     * @throws ReglaNegocioException      Si la capacidad util es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException      Si el porcentaje de evaporacion no esta entre 0 y 100.
     * @throws ReglaNegocioException      Si la perdida por trub es negativa.
     * @throws RecursoDuplicadoException  Si el identificador provisto ya pertenece a una olla de hervor activa distinta a la que se está modificando.
     * @throws RecursoNoEncontradoException Si no existe ninguna olla de hervor activa con el ID especificado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.OLLA_DE_HERVOR)
    public OllaHervorResponseDTO modificarOllaHervor(Long id, OllaHervorFormDTO ollaHervorFormDTO) {
        // 1. Validar que la capacidad util no sea mayor o igual a la capacidad total
        MetodosEquipamiento.validarCapacidadUtil(ollaHervorFormDTO.getCapacidadTotal(), ollaHervorFormDTO.getCapacidadUtil());

        // 2. Validar que el porcentaje de evaporación esté entre 0 y 100
        validarPorcentajeEvaporacion(ollaHervorFormDTO.getEvaporacion());

        // 3. Validar que la pérdida por trub no sea negativa
        validarPerdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub());

        // 4. Validar que no exista otro equipamiento con el mismo identificador interno (ignorando mayúsculas y minúsculas) que no sea el actual
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(ollaHervorFormDTO.getIdentificadorInterno(), id)) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + ollaHervorFormDTO.getIdentificadorInterno() + "'");
        }

        // 5. Localizar la olla de hervor existente. Si no existe, se dispara RecursoNoEncontradoException
        OllaHervorEntity ollaHervorEntity = ollaHervorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la Olla de Hervor con ID:" + id));

        // TODO: Validar que la olla de hervor no esté asociada a lotes Pendientes o en Ejecuciion

        // 6. Actualizar los campos de la entidad con los valores del DTO de formulario
        ollaHervorEntity.setIdentificadorInterno(ollaHervorFormDTO.getIdentificadorInterno());
        ollaHervorEntity.setDescripcion(ollaHervorFormDTO.getDescripcion());
        ollaHervorEntity.setCapacidadTotal(ollaHervorFormDTO.getCapacidadTotal());
        ollaHervorEntity.setCapacidadUtil(ollaHervorFormDTO.getCapacidadUtil());
        ollaHervorEntity.setEvaporacion(ollaHervorFormDTO.getEvaporacion());
        ollaHervorEntity.setPerdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub());

        // 7. Guardar la entidad actualizada en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperOllaHervor.toDTO(ollaHervorRepository.save(ollaHervorEntity));
    }

    /**
     * Procesa la baja lógica de una olla de hervor existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca a la olla de hervor con {@link Estado#BAJA} y
     * persiste el cambio. A partir de ese momento, todas las consultas del repositorio dejan
     * de encontrarla.
     * </p>
     *
     * @param id Identificador clave primaria de la olla de hervor a dar de baja.
     * @return {@link OllaHervorResponseDTO} con los datos de la olla de hervor ya marcada como dada de baja.
     * @throws RecursoNoEncontradoException Si la olla de hervor con el ID especificado no existe o ya fue dado de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.OLLA_DE_HERVOR)
    public OllaHervorResponseDTO bajaOllaHervor(Long id) {
        // 1. Localizar la olla de hervor existente. Si no existe, se dispara RecursoNoEncontradoException
        OllaHervorEntity ollaHervorEntity = ollaHervorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la Olla de Hervor con ID:" + id));

        // TODO: Validar que la olla de hervor no esté asociada a lotes Pendientes o en Ejecuciion

        // 2. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        ollaHervorEntity.setEstado(Estado.BAJA);
        ollaHervorRepository.save(ollaHervorEntity);

        // 3. Retornar el DTO de la olla de hervor dada de baja
        return MapperOllaHervor.toDTO(ollaHervorEntity);
    }

    /**
     * Valida que el porcentaje de evaporación esté entre 0 y 100.
     *
     * @param porcentajeEvaporacion Porcentaje de evaporación de la olla de hervor.
     * @throws ReglaNegocioException Si el porcentaje de evaporación no está entre 0 y 100.
     */
    public static void validarPorcentajeEvaporacion(Double porcentajeEvaporacion) {
        if (porcentajeEvaporacion < 0 || porcentajeEvaporacion > 100) {
            throw new ReglaNegocioException("El porcentaje de evaporación debe estar entre 0 y 100.");
        }
    }

    /**
     * Valida que la pérdida por trub no sea negativa.
     *
     * @param perdidaPorTrub Pérdida por trub de la olla de hervor.
     * @throws ReglaNegocioException Si la pérdida por trub es negativa.
     */
    public static void validarPerdidaPorTrub(Double perdidaPorTrub) {
        if (perdidaPorTrub < 0) {
            throw new ReglaNegocioException("La pérdida por trub no puede ser negativa.");
        }
    }

}
