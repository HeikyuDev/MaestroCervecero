package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperFermentador;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FermentadorServicioImpl implements IFermentadorServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos equipamientos activos (de cualquier tipo) con el mismo identificador interno.
     */
    private static final String INDICE_IDENTIFICADOR_INTERNO = "udx_equipamiento_identificador_interno";

    private final IFermentadorRepository fermentadorRepository;
    private final IEquipamientoRepository equipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;


    /**
     * Recupera una página de Fermentadores activos registrados en el sistema, filtrados
     * opcionalmente por identificador interno (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta).
     * <p>
     * Los fermentadores dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FermentadorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FermentadorResponseDTO> filtrarFermentadores(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        return fermentadorRepository.filtrarFermentadores(identificadorInternoNormalizado, estadoOperativo, pageable).map(MapperFermentador::toDTO);
    }

    /**
     * Recupera una página de Fermentadores activos y de fermentadores dados de baja que tienen lotes (en cualquier
     * estado, incluso si todos fueron cancelados), filtrados opcionalmente por identificador interno (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta).
     * <p>
     * Se usa para buscar, además de los fermentadores activos, los dados de baja cuyo historial de lotes sigue
     * consultable. Los fermentadores dados de baja sin lotes no se incluyen.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link FermentadorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<FermentadorResponseDTO> filtrarFermentadoresConDadosDeBajaConLotes(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        return fermentadorRepository.filtrarFermentadoresConDadosDeBajaConLotes(identificadorInternoNormalizado, estadoOperativo, pageable).map(MapperFermentador::toDTO);
    }

    /**
     * Recupera los fermentadores dados de baja que tienen lotes en cualquier estado, en orden alfabético por
     * identificador interno. Los fermentadores dados de baja sin lotes no se incluyen.
     * <p>
     * Se usa para filtrar el cronograma de producción por un fermentador que ya no está activo y cuyos lotes
     * siguen en el historial.
     * </p>
     *
     * @return Lista de {@link FermentadorResponseDTO} dados de baja con lotes, o una lista vacía si no hay ninguno.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FermentadorResponseDTO> listarDadosDeBajaConLotes() {
        return fermentadorRepository.buscarDadosDeBajaConLotes().stream().map(MapperFermentador::toDTO).toList();
    }

    /**
     * Busca y retorna un fermentador específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del fermentador buscado.
     * @return Objeto {@link FermentadorResponseDTO} con la información del fermentador encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún fermentador activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public FermentadorResponseDTO buscarPorId(Long id) {
        return MapperFermentador.toDTO(fermentadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + id)));
    }


    /**
     * Crea un nuevo fermentador en el sistema.
     *
     * @param fermentadorFormDTO DTO que contiene los datos del fermentador a crear.
     * @return Objeto {@link FermentadorResponseDTO} con la información del fermentador creado.
     * @throws ReglaNegocioException Si el identificador interno falta o supera los 100 caracteres, si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, si la capacidad útil es mayor o igual a la capacidad total, o si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000.
     * @throws RecursoDuplicadoException Si ya existe un equipamiento activo con el mismo identificador interno (sin distinguir mayúsculas).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.FERMENTADOR)
    public FermentadorResponseDTO altaFermentador(FermentadorFormDTO fermentadorFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(fermentadorFormDTO.getIdentificadorInterno());

        // 2. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 3. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total.
        MetodosEquipamiento.validarCapacidadUtil(fermentadorFormDTO.getCapacidadTotal(), fermentadorFormDTO.getCapacidadUtil());

        // 4. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(fermentadorFormDTO.getUsosMaximosAntesMantenimiento());

        // 5. Validar que no haya otro equipamiento registrado con el mismo identificador interno.
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(identificadorInterno)) {
            throw identificadorDuplicadoEnAlta(identificadorInterno);
        }

        // 6. Crear la entidad del fermentador a partir del DTO de formulario y establecer su estado operativo como DISPONIBLE.
        FermentadorEntity fermentadorEntity = FermentadorEntity.builder()
                .identificadorInterno(identificadorInterno)
                .capacidadTotal(fermentadorFormDTO.getCapacidadTotal())
                .capacidadUtil(fermentadorFormDTO.getCapacidadUtil())
                .usosMaximosAntesMantenimiento(fermentadorFormDTO.getUsosMaximosAntesMantenimiento())
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .estado(Estado.ACTIVO)
                .build();

        // 7. Guardar la entidad en la base de datos y retornar el DTO de respuesta correspondiente.
        return MapperFermentador.toDTO(GuardadoUnico.guardar(fermentadorRepository, fermentadorEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnAlta(identificadorInterno))));
    }

    /**
     * Modifica un fermentador existente en el sistema.
     *
     * @param id                 Identificador único del fermentador a modificar.
     * @param fermentadorFormDTO DTO que contiene los nuevos datos del fermentador.
     * @return Objeto {@link FermentadorResponseDTO} con la información actualizada del fermentador.
     * @throws ReglaNegocioException        Si el identificador interno falta o supera los 100 caracteres, si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, si la capacidad útil es mayor o igual a la capacidad total, si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000, si el fermentador tiene comprometida una etapa en estado Pendiente de un lote, o si se encuentra en uso.
     * @throws RecursoNoEncontradoException Si no existe ningún fermentador activo con el ID especificado.
     * @throws RecursoDuplicadoException    Si otro equipamiento ya tiene el mismo identificador interno que se intenta asignar.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.FERMENTADOR)
    public FermentadorResponseDTO modificarFermentador(Long id, FermentadorFormDTO fermentadorFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(fermentadorFormDTO.getIdentificadorInterno());

        // 2. Buscar el fermentador existente por su ID.
        FermentadorEntity fermentadorEntity = fermentadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + id));

        // 3. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 4. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total.
        MetodosEquipamiento.validarCapacidadUtil(fermentadorFormDTO.getCapacidadTotal(), fermentadorFormDTO.getCapacidadUtil());

        // 5. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(fermentadorFormDTO.getUsosMaximosAntesMantenimiento());

        // 6. Validar que no haya otro equipamiento registrado con el mismo identificador interno (excluyendo el actual).
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(identificadorInterno, id)) {
            throw identificadorDuplicadoEnModificacion(identificadorInterno);
        }

        // 7. Validar que el fermentador no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede modificar el fermentador porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 8. Validar que el fermentador no se encuentre actualmente en uso.
        if (fermentadorEntity.getEstadoOperativo() == EstadoOperativo.EN_USO) {
            throw new ReglaNegocioException("No se puede modificar el fermentador porque se encuentra en uso");
        }

        // 9. Actualizar los campos del fermentador con los datos del DTO de formulario.
        fermentadorEntity.setIdentificadorInterno(identificadorInterno);
        fermentadorEntity.setCapacidadTotal(fermentadorFormDTO.getCapacidadTotal());
        fermentadorEntity.setCapacidadUtil(fermentadorFormDTO.getCapacidadUtil());
        fermentadorEntity.setUsosMaximosAntesMantenimiento(fermentadorFormDTO.getUsosMaximosAntesMantenimiento());

        // 10. Guardar la entidad actualizada en la base de datos y retornar el DTO de respuesta correspondiente.
        return MapperFermentador.toDTO(GuardadoUnico.guardar(fermentadorRepository, fermentadorEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnModificacion(identificadorInterno))));
    }

    /**
     * Procesa la baja lógica de un Fermentador existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al fermentador con {@link Estado#BAJA} y persiste
     * el cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del fermentador a dar de baja.
     * @return {@link FermentadorResponseDTO} con los datos del fermentador ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el fermentador con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el fermentador tiene comprometida una etapa en estado Pendiente de un lote, o si no se encuentra disponible (en uso, en limpieza o en mantenimiento).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.FERMENTADOR)
    public FermentadorResponseDTO bajaFermentador(Long id) {

        // 1. Buscar el fermentador existente por su ID.
        FermentadorEntity fermentadorEntity = fermentadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + id));

        // 2. Validar que el fermentador no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el fermentador porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 3. Validar que el fermentador se encuentre disponible (no en uso, en limpieza ni en mantenimiento).
        if (fermentadorEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("No se puede dar de baja el fermentador porque no se encuentra disponible");
        }

        // 4. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        fermentadorEntity.setEstado(Estado.BAJA);

        // 5. Retornar el DTO del fermentador dado de baja
        return MapperFermentador.toDTO(fermentadorRepository.save(fermentadorEntity));
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnAlta(String identificadorInterno) {
        return new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + identificadorInterno + "'");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnModificacion(String identificadorInterno) {
        return new RecursoDuplicadoException("El identificador interno '" + identificadorInterno + "' ya está en uso por otro equipamiento");
    }
}
