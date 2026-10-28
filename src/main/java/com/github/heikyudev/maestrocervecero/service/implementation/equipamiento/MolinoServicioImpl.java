package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.equipamiento.MapperMolino;
import com.github.heikyudev.maestrocervecero.util.method.GeneradorIdentificador;
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


@Service
@RequiredArgsConstructor
public class MolinoServicioImpl implements IMolinoServicio {

    private final IMolinoRepository molinoRepository;
    private final ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;
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
     * Crea un nuevo molino en el sistema. El identificador interno lo genera el sistema como último paso, después de validar los datos.
     *
     * @param molinoFormDTO Datos del molino a crear.
     * @return {@link MolinoResponseDTO} con los datos del molino creado.
     * @throws ReglaNegocioException      Si el rendimiento de molienda no es un número mayor a 0 o supera 10.000 kg/h, o si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO altaMolino(MolinoFormDTO molinoFormDTO) {
        // 1. Validar que el rendimiento de molienda sea un número mayor a 0 y no supere el máximo.
        validarRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 2. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento());

        // 3. Generar el identificador interno: es el último paso antes de guardar, para que un error de validación no consuma un número de la secuencia.
        String identificadorInterno = GeneradorIdentificador.generar(GeneradorIdentificador.PREFIJO_MOLINO, secuenciaIdentificadorRepository.siguienteNumeroMolino());

        // 4. Crear la entidad que se va a almacenar en la base de datos.
        MolinoEntity molinoEntity = MolinoEntity.builder().
                identificadorInterno(identificadorInterno).
                rendimientoMolienda(molinoFormDTO.getRendimientoMolienda()).
                usosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento()).
                estadoOperativo(EstadoOperativo.DISPONIBLE).
                estado(Estado.ACTIVO).
                build();

        // 5. Guardar la entidad en la base de datos.
        return MapperMolino.toDTO(GuardadoUnico.guardar(molinoRepository, molinoEntity));
    }

    /**
     * Modifica un molino existente en el sistema.
     *
     * @param id             ID del molino a modificar.
     * @param molinoFormDTO  Datos actualizados del molino.
     * @return {@link MolinoResponseDTO} con los datos del molino modificado.
     * @throws RecursoNoEncontradoException Si el molino con el ID proporcionado no existe.
     * @throws ReglaNegocioException         Si el rendimiento de molienda no es un número mayor a 0 o supera 10.000 kg/h, si los usos máximos antes de mantenimiento no son un entero mayor a 0 o superan 100.000.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.MOLINO)
    public MolinoResponseDTO modificarMolino(Long id, MolinoFormDTO molinoFormDTO) {
        // 1. Buscar el molino existente por su ID.
        MolinoEntity molinoEntity = molinoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el molino con ID: " + id));

        // 2. Validar que el rendimiento de molienda sea un número mayor a 0 y no supere el máximo.
        validarRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());

        // 3. Validar que los usos máximos antes de mantenimiento sean un entero mayor a 0 y no superen el máximo.
        MetodosEquipamiento.validarUsosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento());

        // 4. Actualizar los campos del molinoEntity con los datos del molinoFormDTO.
        molinoEntity.setRendimientoMolienda(molinoFormDTO.getRendimientoMolienda());
        molinoEntity.setUsosMaximosAntesMantenimiento(molinoFormDTO.getUsosMaximosAntesMantenimiento());

        // 5. Guardar la entidad actualizada en la base de datos.
        return MapperMolino.toDTO(GuardadoUnico.guardar(molinoRepository, molinoEntity));
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
}
