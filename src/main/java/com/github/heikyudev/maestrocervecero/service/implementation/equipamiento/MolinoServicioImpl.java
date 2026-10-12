package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMolino;
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
public class MolinoServicioImpl implements IMolinoServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos equipamientos activos (de cualquier tipo) con el mismo identificador interno.
     */
    private static final String INDICE_IDENTIFICADOR_INTERNO = "udx_equipamiento_identificador_interno";

    private final IMolinoRepository molinoRepository;
    private final IEquipamientoRepository equipamientoRepository;
    private final IEtapaLoteRepository etapaLoteRepository;

    /**
     * Recupera una página de Molinos activos registrados en el sistema, filtrados opcionalmente
     * por identificador interno (coincidencia parcial, sin distinguir mayúsculas/minúsculas) y/o
     * estado operativo (coincidencia exacta).
     * <p>
     * Los molinos dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estadoOperativo Estado operativo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MolinoResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MolinoResponseDTO> filtrarMolinos(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable) {
        // El texto buscado se normaliza igual que el código guardado: sin espacios de más y en mayúsculas
        String identificadorInternoNormalizado = NormalizacionTexto.codigo(identificadorInterno);
        if (identificadorInternoNormalizado != null && identificadorInternoNormalizado.isEmpty()) {
            identificadorInternoNormalizado = null;
        }
        return molinoRepository.filtrarMolinos(identificadorInternoNormalizado, estadoOperativo, pageable).map(MapperMolino::toDTO);
    }


    /**
     * Recupera un molino por su ID.
     *
     * @param id ID del molino a buscar.
     * @return {@link MolinoResponseDTO} con los datos del molino encontrado.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     */
    @Override
    @Transactional(readOnly = true)
    public MolinoResponseDTO buscarPorId(Long id) {
        return MapperMolino.toDTO(molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + id)));
    }

    /**
     * Crea un nuevo molino en el sistema.
     *
     * @param molinoFormDTO Datos del molino a crear.
     * @return {@link MolinoResponseDTO} con los datos del molino creado.
     * @throws RecursoDuplicadoException Si ya existe un equipamiento activo con el mismo identificador interno (sin distinguir mayúsculas).
     * @throws ReglaNegocioException      Si el identificador interno falta o supera los 100 caracteres, si el rendimiento de molienda no es un número mayor a 0 o supera 10.000 kg/h, o si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO altaMolino(MolinoFormDTO molinoFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(molinoFormDTO.getIdentificadorInterno());

        // 2. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 3. Validar que el rendimiento de molienda sea un número mayor a 0 y no supere el máximo.
        validarRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 4. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento());

        // 5. Validar que no haya otro equipamiento registrado con el mismo identificador interno.
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCase(identificadorInterno)) {
            throw identificadorDuplicadoEnAlta(identificadorInterno);
        }

        // 6. Crear la entidad MolinoEntity.
        MolinoEntity molinoEntity = MolinoEntity.builder().
                identificadorInterno(identificadorInterno).
                rendimientoMolienda(molinoFormDTO.getRendimientoMolienda()).
                usosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento()).
                estadoOperativo(EstadoOperativo.DISPONIBLE).
                estado(Estado.ACTIVO).
                build();

        // 7. Guardar la entidad en la base de datos.
        return MapperMolino.toDTO(GuardadoUnico.guardar(molinoRepository, molinoEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnAlta(identificadorInterno))));
    }

    /**
     * Modifica un molino existente en el sistema.
     *
     * @param id             ID del molino a modificar.
     * @param molinoFormDTO  Datos actualizados del molino.
     * @return {@link MolinoResponseDTO} con los datos del molino modificado.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     * @throws RecursoDuplicadoException    Si otro equipamiento ya tiene el mismo identificador interno.
     * @throws ReglaNegocioException         Si el identificador interno falta o supera los 100 caracteres, si el rendimiento de molienda no es un número mayor a 0 o supera 10.000 kg/h, si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000, si el molino tiene comprometida una etapa en estado Pendiente de un lote, o si se encuentra en uso.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO modificarMolino(Long id, MolinoFormDTO molinoFormDTO) {
        // 1. Normalizar el identificador interno sin modificar el DTO.
        String identificadorInterno = NormalizacionTexto.codigo(molinoFormDTO.getIdentificadorInterno());

        // 2. Buscar el molino existente por su ID.
        MolinoEntity molinoEntity = molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + id));

        // 3. Validar que el identificador interno sea obligatorio y no supere la longitud máxima.
        MetodosEquipamiento.validarIdentificadorInterno(identificadorInterno);

        // 4. Validar que el rendimiento de molienda sea un número mayor a 0 y no supere el máximo.
        validarRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 5. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento());

        // 6. Validar que no haya otro equipamiento registrado con el mismo identificador interno (excluyendo el actual).
        if (equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot(identificadorInterno, id)) {
            throw identificadorDuplicadoEnModificacion(identificadorInterno);
        }

        // 7. Validar que el molino no tenga comprometida una etapa en estado Pendiente de un lote (ni a
        //    punto de arrancar, ni futura dentro de un lote ya en ejecución).
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede modificar el molino porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 8. Validar que el molino no se encuentre actualmente en uso.
        if (molinoEntity.getEstadoOperativo() == EstadoOperativo.EN_USO) {
            throw new ReglaNegocioException("No se puede modificar el molino porque se encuentra en uso");
        }

        // 9. Actualizar los campos del molinoEntity con los datos del molinoFormDTO.
        molinoEntity.setIdentificadorInterno(identificadorInterno);
        molinoEntity.setRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());
        molinoEntity.setUsosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento());

        // 10. Guardar la entidad actualizada en la base de datos.
        return MapperMolino.toDTO(GuardadoUnico.guardar(molinoRepository, molinoEntity,
                Map.of(INDICE_IDENTIFICADOR_INTERNO, () -> identificadorDuplicadoEnModificacion(identificadorInterno))));
    }

    /**
     * Da de baja un molino existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al molino con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id ID del molino a dar de baja.
     * @return {@link MolinoResponseDTO} con los datos del molino ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     * @throws ReglaNegocioException Si el molino tiene comprometida una etapa en estado Pendiente de un lote, o si no se encuentra disponible (en uso, en limpieza o en mantenimiento).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO bajaMolino(Long id) {
        // 1. Buscar el molino existente por su ID.
        MolinoEntity molinoEntity = molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + id));

        // 2. Validar que el molino no tenga comprometida una etapa en estado Pendiente de un lote.
        if (etapaLoteRepository.existsLotePendienteAsociado(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el molino porque tiene comprometida una etapa en estado Pendiente de un lote");
        }

        // 3. Validar que el molino se encuentre disponible (no en uso, en limpieza ni en mantenimiento).
        if (molinoEntity.getEstadoOperativo() != EstadoOperativo.DISPONIBLE) {
            throw new ReglaNegocioException("No se puede dar de baja el molino porque no se encuentra disponible");
        }

        // 4. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        molinoEntity.setEstado(Estado.BAJA);

        // 5. Retornar el DTO del molino dado de baja
        return MapperMolino.toDTO(molinoRepository.save(molinoEntity));
    }

    /**
     * Valida que el rendimiento de molienda haya sido informado, sea mayor a 0, sea un número finito y no supere
     * {@link LimitesTecnicos#RENDIMIENTO_MOLINO_MAXIMO_KG_H} kg/h.
     *
     * @param rendimientoMolienda Rendimiento de molienda a validar.
     * @throws ReglaNegocioException Si el rendimiento de molienda es nulo, NaN, infinito, menor o igual a 0 o supera el máximo.
     */
    public static void validarRendimientoMolienda(Double rendimientoMolienda) {
        if (rendimientoMolienda == null) {
            throw new ReglaNegocioException("El rendimiento de molienda es obligatorio");
        }
        if (rendimientoMolienda <= 0) {
            throw new ReglaNegocioException("El rendimiento de molienda debe ser mayor a 0");
        }
        ValidacionesNumericas.validarMayorACeroConMaximo(rendimientoMolienda,
                LimitesTecnicos.RENDIMIENTO_MOLINO_MAXIMO_KG_H, "El rendimiento de molienda", "kg/h");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnAlta(String identificadorInterno) {
        return new RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno '" + identificadorInterno + "'");
    }

    private static RecursoDuplicadoException identificadorDuplicadoEnModificacion(String identificadorInterno) {
        return new RecursoDuplicadoException("El identificador interno '" + identificadorInterno + "' ya está en uso por otro equipamiento");
    }
}
