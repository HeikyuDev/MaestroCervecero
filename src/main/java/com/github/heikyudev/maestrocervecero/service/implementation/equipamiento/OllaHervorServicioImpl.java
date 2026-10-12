package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IOllaHervorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperOllaHervor;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
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
public class OllaHervorServicioImpl implements IOllaHervorServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos equipamientos activos (de cualquier tipo) con el mismo identificador interno.
     */
    private static final String INDICE_IDENTIFICADOR_INTERNO = "udx_equipamiento_identificador_interno";

    private final IOllaHervorRepository ollaHervorRepository;
    private final IEquipamientoRepository equipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

    /**
     * Recupera una página de Ollas de Hervor activas registradas en el sistema, filtradas
     * opcionalmente por identificador interno (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta).
     * <p>
     * Las ollas de hervor dadas de baja son excluidas por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link OllaHervorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OllaHervorResponseDTO> filtrarOllasHervor(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        return ollaHervorRepository.filtrarOllasHervor(identificadorInternoNormalizado, estadoOperativo, pageable).map(MapperOllaHervor::toDTO);
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
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + id)));
    }

    /**
     * Registra una nueva olla de hervor en el sistema.
     * <p>
     * Valida que el identificador interno no esté duplicado (case-insensitive) entre los equipamientos activos,
     * </p>
     *
     * @param ollaHervorFormDTO Objeto DTO que contiene los datos de creación de la olla de hervor.
     * @return {@link OllaHervorResponseDTO} representativo de la olla de hervor guardada en la base de datos.
     * @throws ReglaNegocioException Si el identificador interno falta o supera los 100 caracteres.
     * @throws ReglaNegocioException Si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, o si la capacidad útil es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si la evaporación no es un número mayor a 0 o supera la capacidad útil.
     * @throws ReglaNegocioException Si la pérdida por trub no es un número mayor a 0 o supera la capacidad útil.
     * @throws ReglaNegocioException Si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a una olla de hervor activa.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR,conceptoAuditoria = ConceptoAuditoria.OLLA_DE_HERVOR)
    public OllaHervorResponseDTO altaOllaHervor(OllaHervorFormDTO ollaHervorFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(ollaHervorFormDTO.getIdentificadorInterno());

        // 2. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 3. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total
        MetodosEquipamiento.validarCapacidadUtil(ollaHervorFormDTO.getCapacidadTotal(), ollaHervorFormDTO.getCapacidadUtil());

        // 4. Validar que la evaporación sea un número mayor a 0 y no supere la capacidad útil
        validarEvaporacion(ollaHervorFormDTO.getEvaporacion(), ollaHervorFormDTO.getCapacidadUtil());

        // 5. Validar que la pérdida por trub sea un número mayor a 0 y no supere la capacidad útil
        validarPerdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub(), ollaHervorFormDTO.getCapacidadUtil());

        // 6. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(ollaHervorFormDTO.getUsosMaximosAntesMantenimiento());

        // 7. Validar que no exista otra olla de hervor con el mismo identificador interno (ignorando mayúsculas y minúsculas)
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(identificadorInterno)) {
            throw identificadorDuplicadoEnAlta(identificadorInterno);
        }

        // 8. Crear la entidad OllaHervorEntity a partir del DTO de formulario
        OllaHervorEntity ollaHervorEntity = OllaHervorEntity.builder()
                .identificadorInterno(identificadorInterno)
                // Cuando se da de alta una nueva olla de hervor, su estado operativo inicial es DISPONIBLE.
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(ollaHervorFormDTO.getCapacidadTotal())
                .capacidadUtil(ollaHervorFormDTO.getCapacidadUtil())
                .evaporacion(ollaHervorFormDTO.getEvaporacion())
                .perdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub())
                .usosMaximosAntesMantenimiento(ollaHervorFormDTO.getUsosMaximosAntesMantenimiento())
                .estado(Estado.ACTIVO)
                .build();

        // 9. Guardar la entidad en la base de datos y retonar el DTO de respuesta correspondiente
        return MapperOllaHervor.toDTO(GuardadoUnico.guardar(ollaHervorRepository, ollaHervorEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnAlta(identificadorInterno))));
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
     * @throws ReglaNegocioException      Si el identificador interno falta o supera los 100 caracteres.
     * @throws ReglaNegocioException      Si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, o si la capacidad útil es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException      Si la evaporación no es un número mayor a 0 o supera la capacidad útil.
     * @throws ReglaNegocioException      Si la pérdida por trub no es un número mayor a 0 o supera la capacidad útil.
     * @throws ReglaNegocioException      Si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000, si la olla de hervor tiene comprometida una etapa en estado Pendiente de un lote, o si se encuentra en uso.
     * @throws RecursoDuplicadoException  Si el identificador provisto ya pertenece a una olla de hervor activa distinta a la que se está modificando.
     * @throws RecursoNoEncontradoException Si no existe ninguna olla de hervor activa con el ID especificado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.OLLA_DE_HERVOR)
    public OllaHervorResponseDTO modificarOllaHervor(Long id, OllaHervorFormDTO ollaHervorFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(ollaHervorFormDTO.getIdentificadorInterno());

        // 2. Localizar la olla de hervor existente. Si no existe, se dispara RecursoNoEncontradoException
        OllaHervorEntity ollaHervorEntity = ollaHervorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + id));

        // 3. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 4. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total
        MetodosEquipamiento.validarCapacidadUtil(ollaHervorFormDTO.getCapacidadTotal(), ollaHervorFormDTO.getCapacidadUtil());

        // 5. Validar que la evaporación sea un número mayor a 0 y no supere la capacidad útil
        validarEvaporacion(ollaHervorFormDTO.getEvaporacion(), ollaHervorFormDTO.getCapacidadUtil());

        // 6. Validar que la pérdida por trub sea un número mayor a 0 y no supere la capacidad útil
        validarPerdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub(), ollaHervorFormDTO.getCapacidadUtil());

        // 7. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(ollaHervorFormDTO.getUsosMaximosAntesMantenimiento());

        // 8. Validar que no exista otro equipamiento con el mismo identificador interno (ignorando mayúsculas y minúsculas) que no sea el actual
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(identificadorInterno, id)) {
            throw identificadorDuplicadoEnModificacion(identificadorInterno);
        }

        // 9. Validar que la olla de hervor no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede modificar la olla de hervor porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 10. Validar que la olla de hervor no se encuentre actualmente en uso.
        if (ollaHervorEntity.getEstadoOperativo() == EstadoOperativo.EN_USO) {
            throw new ReglaNegocioException("No se puede modificar la olla de hervor porque se encuentra en uso");
        }

        // 11. Actualizar los campos de la entidad con los valores del DTO de formulario
        ollaHervorEntity.setIdentificadorInterno(identificadorInterno);
        ollaHervorEntity.setCapacidadTotal(ollaHervorFormDTO.getCapacidadTotal());
        ollaHervorEntity.setCapacidadUtil(ollaHervorFormDTO.getCapacidadUtil());
        ollaHervorEntity.setEvaporacion(ollaHervorFormDTO.getEvaporacion());
        ollaHervorEntity.setPerdidaPorTrub(ollaHervorFormDTO.getPerdidaPorTrub());
        ollaHervorEntity.setUsosMaximosAntesMantenimiento(ollaHervorFormDTO.getUsosMaximosAntesMantenimiento());

        // 12. Guardar la entidad actualizada en la base de datos y retornar el DTO de respuesta correspondiente
        return MapperOllaHervor.toDTO(GuardadoUnico.guardar(ollaHervorRepository, ollaHervorEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnModificacion(identificadorInterno))));
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
     * @throws RecursoNoEncontradoException Si la olla de hervor con el ID especificado no existe o ya fue dada de baja.
     * @throws ReglaNegocioException Si la olla de hervor tiene comprometida una etapa en estado Pendiente de un lote, o si no se encuentra disponible (en uso, en limpieza o en mantenimiento).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.OLLA_DE_HERVOR)
    public OllaHervorResponseDTO bajaOllaHervor(Long id) {
        // 1. Localizar la olla de hervor existente. Si no existe, se dispara RecursoNoEncontradoException
        OllaHervorEntity ollaHervorEntity = ollaHervorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la olla de hervor con ID: " + id));

        // 2. Validar que la olla de hervor no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede dar de baja la olla de hervor porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 3. Validar que la olla de hervor se encuentre disponible (no en uso, en limpieza ni en mantenimiento).
        if (ollaHervorEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("No se puede dar de baja la olla de hervor porque no se encuentra disponible");
        }

        // 4. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        ollaHervorEntity.setEstado(Estado.BAJA);

        // 5. Retornar el DTO de la olla de hervor dada de baja
        return MapperOllaHervor.toDTO(ollaHervorRepository.save(ollaHervorEntity));
    }

    /**
     * Valida que la evaporación (en litros por hora) sea mayor a 0 y no supere la capacidad útil de
     * la olla. La fórmula del lote la multiplica por las horas de hervido, así que es una cantidad
     * de litros y no un porcentaje.
     *
     * @param evaporacion Evaporación de la olla de hervor, en litros por hora.
     * @param capacidadUtil Capacidad útil de la olla de hervor, en litros (ya validada).
     * @throws ReglaNegocioException Si la evaporación es nula, no es mayor a 0, supera la capacidad útil o es NaN.
     */
    public static void validarEvaporacion(Double evaporacion, Double capacidadUtil) {
        if (evaporacion == null) {
            throw new ReglaNegocioException("La evaporación es obligatoria");
        }
        if (evaporacion <= 0) {
            throw new ReglaNegocioException("La evaporación debe ser mayor a 0");
        }
        if (evaporacion > capacidadUtil) {
            throw new ReglaNegocioException("La evaporación no puede superar la capacidad útil de la olla");
        }
        ValidacionesNumericas.validarMayorACero(evaporacion, "La evaporación");
    }

    /**
     * Valida que la pérdida por trub (en litros) sea mayor a 0 y no supere la capacidad útil de la olla.
     *
     * @param perdidaPorTrub Pérdida por trub de la olla de hervor, en litros.
     * @param capacidadUtil Capacidad útil de la olla de hervor, en litros (ya validada).
     * @throws ReglaNegocioException Si la pérdida por trub es nula, no es mayor a 0, supera la capacidad útil o es NaN.
     */
    public static void validarPerdidaPorTrub(Double perdidaPorTrub, Double capacidadUtil) {
        if (perdidaPorTrub == null) {
            throw new ReglaNegocioException("La pérdida por trub es obligatoria");
        }
        if (perdidaPorTrub <= 0) {
            throw new ReglaNegocioException("La pérdida por trub debe ser mayor a 0");
        }
        if (perdidaPorTrub > capacidadUtil) {
            throw new ReglaNegocioException("La pérdida por trub no puede superar la capacidad útil de la olla");
        }
        ValidacionesNumericas.validarMayorACero(perdidaPorTrub, "La pérdida por trub");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnAlta(String identificadorInterno) {
        return new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + identificadorInterno + "'");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnModificacion(String identificadorInterno) {
        return new RecursoDuplicadoException("El identificador interno '" + identificadorInterno + "' ya está en uso por otro equipamiento");
    }
}
