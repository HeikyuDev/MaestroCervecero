package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMacerador;
import com.github.heikyudev.maestrocervecero.util.method.equipamiento.MetodosEquipamiento;
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
public class MaceradorServicioImpl implements IMaceradorServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos equipamientos activos (de cualquier tipo) con el mismo identificador interno.
     */
    private static final String INDICE_IDENTIFICADOR_INTERNO = "udx_equipamiento_identificador_interno";

    private final IMaceradorRepository maceradorRepository;
    private final IEquipamientoRepository equipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

    /**
     * Recupera una página de Maceradores activos registrados en el sistema, filtrados
     * opcionalmente por identificador interno (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o estado operativo (coincidencia exacta).
     * <p>
     * Los maceradores dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MaceradorResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MaceradorResponseDTO> filtrarMaceradores(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        return maceradorRepository.filtrarMaceradores(identificadorInternoNormalizado, estadoOperativo, pageable).map(MapperMacerador::toDTO);
    }

    /**
     * Busca y retorna un macerador específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del macerador buscado.
     * @return Objeto {@link MaceradorResponseDTO} con la información del macerador encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún macerador activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MaceradorResponseDTO buscarPorId(Long id) {
        return MapperMacerador.toDTO(maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + id)));
    }


    /**
     * Registra un nuevo macerador en el sistema.
     * <p>
     * Valida que el identificador interno no esté duplicado (case-insensitive) entre los equipamientos activos,
     * </p>
     *
     * @param maceradorFormDTO Objeto DTO que contiene los datos de creación del macerador.
     * @return {@link MaceradorResponseDTO} representativo del macerador guardado en la base de datos.
     * @throws ReglaNegocioException Si el identificador interno falta o supera los 100 caracteres.
     * @throws ReglaNegocioException Si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, o si la capacidad útil es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si el espacio muerto no es un número mayor o igual a 0, supera 100.000 litros o no es menor a la capacidad útil.
     * @throws ReglaNegocioException Si la eficiencia de maceracion no es un número entre 40 y 100 inclusive.
     * @throws ReglaNegocioException Si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a un macerador activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO altaMacerador(MaceradorFormDTO maceradorFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(maceradorFormDTO.getIdentificadorInterno());

        // 2. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 3. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total
        MetodosEquipamiento.validarCapacidadUtil(maceradorFormDTO.getCapacidadTotal(), maceradorFormDTO.getCapacidadUtil());

        // 4. Validar el espacio muerto
        validarEspacioMuerto(maceradorFormDTO.getEspacioMuerto(), maceradorFormDTO.getCapacidadUtil());

        // 5. Validar eficiencia de maceracion
        validarEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 6. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento());

        // 7. Validar que el identificador interno no esté duplicado
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(identificadorInterno)) {
            throw identificadorDuplicadoEnAlta(identificadorInterno);
        }

        // 8. Creo la entidad que se va a almacenar en la base de datos.
        MaceradorEntity maceradorEntity = MaceradorEntity.builder()
                .identificadorInterno(identificadorInterno)
                // Cuando se da de alta un nuevo macerador, su estado operativo inicial es DISPONIBLE.
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(maceradorFormDTO.getCapacidadTotal())
                .capacidadUtil(maceradorFormDTO.getCapacidadUtil())
                .espacioMuerto(maceradorFormDTO.getEspacioMuerto())
                .eficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion())
                .usosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento())
                .estado(Estado.ACTIVO)
                .build();

        return MapperMacerador.toDTO(GuardadoUnico.guardar(maceradorRepository, maceradorEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnAlta(identificadorInterno))));
    }

    /**
     * Actualiza la información de un macerador existente en la base de datos.
     * <p>
     * Valida el porcentaje que la capacidad útil sea menor a la capacidad total, que la eficiencia de maceración se encuentre entre 40 y 100 inclusive,
     * luego localiza el macerador por su ID y verifica que el nuevo identificador no colisione con el de otro macerador activo (case-insensitive),
     * permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del macerador a modificar.
     * @param maceradorFormDTO DTO con la información actualizada.
     * @return {@link MaceradorResponseDTO} representativo del macerador con los cambios aplicados.
     * @throws ReglaNegocioException Si el identificador interno falta o supera los 100 caracteres.
     * @throws ReglaNegocioException Si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, o si la capacidad útil es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si el espacio muerto no es un número mayor o igual a 0, supera 100.000 litros o no es menor a la capacidad útil.
     * @throws ReglaNegocioException Si la eficiencia de maceracion no es un número entre 40 y 100 inclusive.
     * @throws ReglaNegocioException Si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000, si el macerador tiene comprometida una etapa en estado Pendiente de un lote, o si se encuentra en uso.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a un macerador activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO modificarMacerador(Long id,MaceradorFormDTO maceradorFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(maceradorFormDTO.getIdentificadorInterno());

        // 2. Localizar el macerador existente. Si no existe, se dispara RecursoNoEncontradoException
        MaceradorEntity maceradorEntity = maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + id));

        // 3. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 4. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total
        MetodosEquipamiento.validarCapacidadUtil(maceradorFormDTO.getCapacidadTotal(), maceradorFormDTO.getCapacidadUtil());

        // 5. Validar el espacio muerto
        validarEspacioMuerto(maceradorFormDTO.getEspacioMuerto(), maceradorFormDTO.getCapacidadUtil());

        // 6. Validar eficiencia de maceracion
        validarEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 7. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento());

        // 8. Validar que el identificador interno no esté duplicado
        if(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(identificadorInterno, id)) {
            throw identificadorDuplicadoEnModificacion(identificadorInterno);
        }

        // 9. Validar que el macerador no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede modificar el macerador porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 10. Validar que el macerador no se encuentre actualmente en uso.
        if (maceradorEntity.getEstadoOperativo() == EstadoOperativo.EN_USO) {
            throw new ReglaNegocioException("No se puede modificar el macerador porque se encuentra en uso");
        }

        // 11. Aplico los cambios sobre la entidad administrada por persistencia.
        maceradorEntity.setIdentificadorInterno(identificadorInterno);
        maceradorEntity.setCapacidadTotal(maceradorFormDTO.getCapacidadTotal());
        maceradorEntity.setCapacidadUtil(maceradorFormDTO.getCapacidadUtil());
        maceradorEntity.setEspacioMuerto(maceradorFormDTO.getEspacioMuerto());
        maceradorEntity.setEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());
        maceradorEntity.setUsosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento());

        // 12. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperMacerador.toDTO(GuardadoUnico.guardar(maceradorRepository, maceradorEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnModificacion(identificadorInterno))));
    }

    /**
     * Procesa la baja lógica de un macerador existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al macerador con {@link Estado#BAJA} y persiste
     * el cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del macerador a dar de baja.
     * @return {@link MaceradorResponseDTO} con los datos del macerador ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el macerador con el ID especificado no existe o ya fue dado de baja.
     * @throws ReglaNegocioException Si el macerador tiene comprometida una etapa en estado Pendiente de un lote, o si no se encuentra disponible (en uso, en limpieza o en mantenimiento).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO bajaMacerador(Long id) {

        // 1. Localizar el macerador existente. Si no existe, se dispara RecursoNoEncontradoException
        MaceradorEntity maceradorEntity = maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + id));

        // 2. Validar que el macerador no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el macerador porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 3. Validar que el macerador se encuentre disponible (no en uso, en limpieza ni en mantenimiento).
        if (maceradorEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("No se puede dar de baja el macerador porque no se encuentra disponible");
        }

        // 4. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        maceradorEntity.setEstado(Estado.BAJA);

        // 5. Retornar el DTO del macerador dado de baja
        return MapperMacerador.toDTO(maceradorRepository.save(maceradorEntity));
    }

    /**
     * Valida que el espacio muerto sea estrictamente menor a la capacidad útil, que sea un número finito mayor o
     * igual a 0 y que no supere {@link LimitesTecnicos#CAPACIDAD_EQUIPO_MAXIMA_LITROS} litros.
     *
     * @param espacioMuerto Espacio muerto del macerador, en litros.
     * @param capacidadUtil Capacidad útil del macerador, en litros (ya validada).
     * @throws ReglaNegocioException Si el espacio muerto es nulo, no es menor a la capacidad útil, es NaN, infinito o negativo, o supera el máximo.
     */
    public static void validarEspacioMuerto(Double espacioMuerto, Double capacidadUtil) {
        if (espacioMuerto == null) {
            throw new ReglaNegocioException("El espacio muerto es obligatorio");
        }
        if (espacioMuerto >= capacidadUtil) {
            throw new ReglaNegocioException("El espacio muerto debe ser menor a la capacidad útil");
        }
        ValidacionesNumericas.validarMayorOIgualACeroConMaximo(espacioMuerto,
                LimitesTecnicos.CAPACIDAD_EQUIPO_MAXIMA_LITROS, "El espacio muerto", "litros");
    }

    /**
     * Valida que la eficiencia de maceración sea un número entre 40 y 100 inclusive.
     *
     * @param eficienciaMaceracion Eficiencia de maceración del macerador.
     * @throws ReglaNegocioException Si la eficiencia de maceración es nula, es NaN o no está entre 40 y 100 inclusive.
     */
    public static void validarEficienciaMaceracion(Double eficienciaMaceracion) {
        if (eficienciaMaceracion == null) {
            throw new ReglaNegocioException("La eficiencia de maceración es obligatoria");
        }
        if (!(eficienciaMaceracion >= 40 && eficienciaMaceracion <= 100)) {
            throw new ReglaNegocioException("La eficiencia de maceración debe estar entre 40 y 100");
        }
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnAlta(String identificadorInterno) {
        return new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + identificadorInterno + "'");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnModificacion(String identificadorInterno) {
        return new RecursoDuplicadoException("El identificador interno '" + identificadorInterno + "' ya está en uso por otro equipamiento");
    }
}
