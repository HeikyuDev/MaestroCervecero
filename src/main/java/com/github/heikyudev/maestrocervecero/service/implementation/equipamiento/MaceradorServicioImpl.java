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
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MaceradorServicioImpl implements IMaceradorServicio {

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
        return maceradorRepository.filtrarMaceradores(identificadorInterno, estadoOperativo, pageable).map(MapperMacerador::toDTO);
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
     * @throws ReglaNegocioException Si el identificador interno supera los 100 caracteres.
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

        // 1. Validar la longitud máxima del identificador interno
        MetodosEquipamiento.validarLongitudIdentificadorInterno(maceradorFormDTO.getIdentificadorInterno());

        // 2. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total
        MetodosEquipamiento.validarCapacidadUtil(maceradorFormDTO.getCapacidadTotal(), maceradorFormDTO.getCapacidadUtil());

        // 3. Validar el espacio muerto
        validarEspacioMuerto(maceradorFormDTO.getEspacioMuerto(), maceradorFormDTO.getCapacidadUtil());

        // 4. Validar eficiencia de maceracion
        validarEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 5. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento());

        // 6. Validar que el identificador interno no esté duplicado
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(maceradorFormDTO.getIdentificadorInterno())) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + maceradorFormDTO.getIdentificadorInterno() + "'");
        }

        // 7. Creo la entidad que se va a almacenar en la base de datos.
        MaceradorEntity maceradorEntity = MaceradorEntity.builder()
                .identificadorInterno(maceradorFormDTO.getIdentificadorInterno())
                // Cuando se da de alta un nuevo macerador, su estado operativo inicial es DISPONIBLE.
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(maceradorFormDTO.getCapacidadTotal())
                .capacidadUtil(maceradorFormDTO.getCapacidadUtil())
                .espacioMuerto(maceradorFormDTO.getEspacioMuerto())
                .eficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion())
                .usosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento())
                .estado(Estado.ACTIVO)
                .build();

        return MapperMacerador.toDTO(maceradorRepository.save(maceradorEntity));
    }

    /**
     * Actualiza la información de un macerador existente en la base de datos.
     * <p>
     * Valida el porcentaje que la capacidad util no sea mayor a la capacidad total, que la eficiencia de maceracion se encuentre entre 40 y 100 inclusive,
     * luego localiza el macerador por su ID y verifica que el nuevo identificador no colisione con el de otro macerador activo (case-insensitive),
     * permitiendo conservar el propio nombre actual.
     * </p>
     *
     * @param id Identificador clave primaria del macerador a modificar.
     * @param maceradorFormDTO DTO con la información actualizada.
     * @return {@link MaceradorResponseDTO} representativo del macerador con los cambios aplicados.
     * @throws ReglaNegocioException Si el identificador interno supera los 100 caracteres.
     * @throws ReglaNegocioException Si alguna capacidad no es un número mayor a 0 o supera 100.000 litros, o si la capacidad útil es mayor o igual a la capacidad total.
     * @throws ReglaNegocioException Si el espacio muerto no es un número mayor o igual a 0, supera 100.000 litros o no es menor a la capacidad útil.
     * @throws ReglaNegocioException Si la eficiencia de maceracion no es un número entre 40 y 100 inclusive.
     * @throws ReglaNegocioException Si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000, si el macerador tiene comprometida una etapa pendiente de un lote, o si se encuentra en uso.
     * @throws RecursoDuplicadoException Si el identificador provisto ya pertenece a un macerador activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO modificarMacerador(Long id,MaceradorFormDTO maceradorFormDTO) {
        // 1. Localizar el macerador existente. Si no existe, se dispara RecursoNoEncontradoException
        MaceradorEntity maceradorEntity = maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + id));

        // 2. Validar la longitud máxima del identificador interno
        MetodosEquipamiento.validarLongitudIdentificadorInterno(maceradorFormDTO.getIdentificadorInterno());

        // 3. Validar las capacidades (números mayores a 0 y dentro del máximo) y que la capacidad útil sea menor a la total
        MetodosEquipamiento.validarCapacidadUtil(maceradorFormDTO.getCapacidadTotal(), maceradorFormDTO.getCapacidadUtil());

        // 4. Validar el espacio muerto
        validarEspacioMuerto(maceradorFormDTO.getEspacioMuerto(), maceradorFormDTO.getCapacidadUtil());

        // 5. Validar eficiencia de maceracion
        validarEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());

        // 6. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento());

        // 7. Validar que el identificador interno no esté duplicado
        if(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(maceradorFormDTO.getIdentificadorInterno(), id)) {
            throw new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + maceradorFormDTO.getIdentificadorInterno() + "'");
        }

        // 8. Validar que el macerador no tenga comprometida una etapa pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede modificar el macerador porque tiene comprometida una etapa pendiente de un lote.");
        }

        // 9. Validar que el macerador no se encuentre actualmente en uso.
        if (maceradorEntity.getEstadoOperativo() == EstadoOperativo.EN_USO) {
            throw new ReglaNegocioException("No se puede modificar el macerador porque se encuentra en uso.");
        }

        // 10. Aplico los cambios sobre la entidad administrada por persistencia.
        maceradorEntity.setIdentificadorInterno(maceradorFormDTO.getIdentificadorInterno());
        maceradorEntity.setCapacidadTotal(maceradorFormDTO.getCapacidadTotal());
        maceradorEntity.setCapacidadUtil(maceradorFormDTO.getCapacidadUtil());
        maceradorEntity.setEspacioMuerto(maceradorFormDTO.getEspacioMuerto());
        maceradorEntity.setEficienciaMaceracion(maceradorFormDTO.getEficienciaMaceracion());
        maceradorEntity.setUsosMaximosAntesMantenimiento(maceradorFormDTO.getUsosMaximosAntesMantenimiento());

        // 11. Persisto la entidad actualizada y devuelvo el DTO correspondiente
        return MapperMacerador.toDTO(maceradorRepository.save(maceradorEntity));
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
     * @throws ReglaNegocioException Si el macerador tiene comprometida una etapa pendiente de un lote, o si se encuentra en uso.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MACERADOR)
    public MaceradorResponseDTO bajaMacerador(Long id) {

        // 1. Localizar el macerador existente. Si no existe, se dispara RecursoNoEncontradoException
        MaceradorEntity maceradorEntity = maceradorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el macerador con ID: " + id));

        // 2. Validar que el macerador no tenga comprometida una etapa pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el macerador porque tiene comprometida una etapa pendiente de un lote.");
        }

        // 3. Validar que el macerador no se encuentre actualmente en uso.
        if (maceradorEntity.getEstadoOperativo() == EstadoOperativo.EN_USO) {
            throw new ReglaNegocioException("No se puede dar de baja el macerador porque se encuentra en uso.");
        }

        // 4. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        maceradorEntity.setEstado(Estado.BAJA);

        // 5. Retornar el DTO del macerador dado de baja
        return MapperMacerador.toDTO(maceradorRepository.save(maceradorEntity));
    }

    /**
     * Valida que el espacio muerto sea un número finito mayor o igual a 0, que no supere
     * {@link LimitesTecnicos#CAPACIDAD_EQUIPO_MAXIMA_LITROS} litros y que sea estrictamente menor a la
     * capacidad útil.
     *
     * @param espacioMuerto Espacio muerto del macerador, en litros.
     * @param capacidadUtil Capacidad útil del macerador, en litros (ya validada).
     * @throws ReglaNegocioException Si el espacio muerto es nulo, NaN, infinito, negativo, supera el máximo o no es menor a la capacidad útil.
     */
    public static void validarEspacioMuerto(Double espacioMuerto, Double capacidadUtil) {
        ValidacionesNumericas.validarMayorOIgualACeroConMaximo(espacioMuerto,
                LimitesTecnicos.CAPACIDAD_EQUIPO_MAXIMA_LITROS, "El espacio muerto", "litros");
        if (espacioMuerto >= capacidadUtil) {
            throw new ReglaNegocioException("El espacio muerto debe ser menor a la capacidad útil");
        }
    }

    /**
     * Valida que la eficiencia de maceración esté entre 40 y 100 inclusive y que sea un número.
     *
     * @param eficienciaMaceracion Eficiencia de maceración del macerador.
     * @throws ReglaNegocioException Si la eficiencia de maceración es nula, no está entre 40 y 100 inclusive o es NaN.
     */
    public static void validarEficienciaMaceracion(Double eficienciaMaceracion) {
        if (eficienciaMaceracion == null) {
            throw new ReglaNegocioException("La eficiencia de maceración es obligatoria.");
        }
        if (eficienciaMaceracion < 40 || eficienciaMaceracion > 100) {
            throw new ReglaNegocioException("La eficiencia de maceración debe estar entre 40 y 100 inclusive.");
        }
        ValidacionesNumericas.validarMayorACero(eficienciaMaceracion, "La eficiencia de maceración");
    }

}
