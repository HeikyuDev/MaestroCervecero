package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.MedicionLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.PlanMonitoreoEtapaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IMedicionLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IDetalleParametroControlRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularMedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.MedicionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IMedicionLoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.MedicionLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperMedicionLote;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación de {@link IMedicionLoteServicio}.
 */
@Service
@RequiredArgsConstructor
public class MedicionLoteServicioImpl implements IMedicionLoteServicio {

    private final IEtapaLoteRepository etapaLoteRepository;
    private final IDetalleParametroControlRepository detalleParametroControlRepository;
    private final IMedicionLoteRepository medicionLoteRepository;

    /**
     * Recupera una página de mediciones de lote de una etapa de lote y un detalle de parámetro de
     * control determinados, filtradas opcionalmente por estado (coincidencia exacta) y/o por un
     * rango de fecha y hora de medición.
     * <p>
     * {@code idEtapaLote} e {@code idDetalleParametroControl} no son opcionales: lo determina el
     * contexto fijo desde el que se entra a gestionar mediciones, nunca lo tipea el usuario.
     * </p>
     * <p>
     * {@code estado} sí es un criterio de negocio legítimo para el usuario: si no lo especifica,
     * se asume {@code REGISTRADO} por defecto (nunca se filtra por baja lógica acá — este campo es
     * {@code EstadoTransaccion}, no {@code Estado} — así que "ver lo anulado" es una elección
     * explícita del usuario, no un criterio que se le oculte).
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se gestionan mediciones (obligatorio).
     * @param idDetalleParametroControl El ID del detalle de parámetro de control sobre el que se gestionan mediciones (obligatorio).
     * @param estado El estado a filtrar, o {@code null} para asumir {@code REGISTRADO} por defecto.
     * @param fechaMedicionDesde Límite inferior (inclusive) del rango de fecha de medición, o {@code null} para no acotarlo.
     * @param fechaMedicionHasta Límite superior (inclusive) del rango de fecha de medición, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link MedicionLoteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MedicionLoteResponseDTO> filtrarMedicionesLote(Long idEtapaLote, Long idDetalleParametroControl, EstadoTransaccion estado,
                                                                 LocalDateTime fechaMedicionDesde, LocalDateTime fechaMedicionHasta, Pageable pageable) {
        EstadoTransaccion estadoEfectivo = estado != null ? estado : EstadoTransaccion.REGISTRADO;
        return medicionLoteRepository.filtrarMedicionesLote(idEtapaLote, idDetalleParametroControl, estadoEfectivo, fechaMedicionDesde, fechaMedicionHasta, pageable)
                .map(MapperMedicionLote::toDTO);
    }

    /**
     * Busca y retorna una medición de lote mediante su identificador único.
     *
     * @param id El ID de la medición.
     * @return La medición correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe una medición con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public MedicionLoteResponseDTO buscarPorId(Long id) {
        return MapperMedicionLote.toDTO(medicionLoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la medición con ID: " + id)));
    }

    /**
     * Registra una medición de un parámetro de control sobre una etapa de un lote.
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra la medición (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param medicionLoteFormDTO Los datos de la medición a registrar.
     * @return La medición registrada.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el detalle de parámetro de
     *                                      control referenciados no existen.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si la etapa
     *                               no es la etapa actual del lote (EN_CURSO), si la etapa no admite
     *                               registrar mediciones, si la receta no tiene
     *                               configurado ningún plan de monitoreo para esa etapa, si el
     *                               detalle de parámetro de control no corresponde a la etapa
     *                               referenciada, si la fecha de medición es posterior a la fecha y
     *                               hora actual, si ya existe una medición registrada para ese mismo
     *                               parámetro en esa misma fecha y hora, o si el valor medido excede
     *                               el rango real (mínimo/máximo) del parámetro de control.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.MEDICION_LOTE)
    public MedicionLoteResponseDTO registrarMedicion(Long idEtapaLote, MedicionLoteFormDTO medicionLoteFormDTO) {
        // 1. Validar que la etapa de lote esté registrada en el sistema
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));

        // 2. Validar que el lote se encuentre en estado EN_EJECUCION
        LoteEntity lote = etapaLote.getLote();
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder registrar mediciones");
        }

        // 3. Validar que la etapa sea la etapa actual del lote (EN_CURSO)
        if (etapaLote.getEstado() != EstadoEtapaLote.EN_CURSO) {
            throw new ReglaNegocioException("Solo se pueden registrar mediciones sobre la etapa actualmente en curso del lote");
        }

        // 4. Validar que la etapa admita el registro de mediciones
        TipoEtapa tipoEtapa = etapaLote.getEtapa();
        if (!tipoEtapa.isPermiteRegistrarMediciones()) {
            throw new ReglaNegocioException("La etapa " + tipoEtapa + " no admite el registro de mediciones");
        }

        // 5. Validar que la receta asociada tenga configurado al menos un plan de monitoreo para esta etapa
        boolean tienePlanDeMonitoreo = lote.getPlanificacionProduccion().getVersionReceta().getPlanesMonitoreo().stream()
                .anyMatch(plan -> plan.getEtapaControl().getEtapaAControlar() == tipoEtapa);
        if (!tienePlanDeMonitoreo) {
            throw new ReglaNegocioException("La receta no tiene configurado ningún plan de monitoreo para la etapa " + tipoEtapa);
        }

        // 6. Validar que el detalle de parámetro de control esté registrado en el sistema
        DetalleParametroControlEntity detalleParametroControl = detalleParametroControlRepository.findById(medicionLoteFormDTO.getIdDetalleParametroControl())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el detalle de parámetro de control con ID: " + medicionLoteFormDTO.getIdDetalleParametroControl()));

        // 7. Validar que el detalle de parámetro de control corresponda a la etapa actual del lote
        PlanMonitoreoEtapaEntity planMonitoreoEtapa = detalleParametroControl.getPlanMonitoreoEtapa();
        if (planMonitoreoEtapa.getEtapaControl().getEtapaAControlar() != tipoEtapa) {
            throw new ReglaNegocioException("El detalle de parámetro de control seleccionado no corresponde a la etapa " + tipoEtapa);
        }

        // 8. Validar que la fecha y hora de medición no sea posterior a la fecha y hora actual
        LocalDateTime fechaMedicion = medicionLoteFormDTO.getFechaMedicion();
        if (fechaMedicion.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha y hora de medición no puede ser posterior a la fecha y hora actual");
        }

        // 9. Validar que no haya otra medición registrada para ese mismo parámetro en esa misma fecha y hora
        if (medicionLoteRepository.existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(detalleParametroControl.getId(), fechaMedicion, EstadoTransaccion.REGISTRADO)) {
            throw new ReglaNegocioException("Ya existe una medición registrada para ese parámetro de control en esa misma fecha y hora");
        }

        // 10. Validar que el valor medido no exceda los límites reales del parámetro de control.
        //     Los valores mínimo/máximo de DetalleParametroControlEntity son el rango IDEAL deseado
        //     para esta receta (una desviación ahí es normal y solo dispara una alerta); los de
        //     ParametroControlEntity son los límites físicos reales del parámetro — una medición
        //     fuera de ese rango es un dato inválido, no una desviación de calidad.
        ParametroControlEntity parametroControl = detalleParametroControl.getParametroControl();
        Double valorMedido = medicionLoteFormDTO.getValorMedido();
        if (valorMedido < parametroControl.getValorMinimo() || valorMedido > parametroControl.getValorMaximo()) {
            throw new ReglaNegocioException("El valor medido (" + valorMedido + ") está fuera del rango real del parámetro de control '"
                    + parametroControl.getNombre() + "' (" + parametroControl.getValorMinimo() + " - " + parametroControl.getValorMaximo() + ")");
        }

        // 11. Determinar si el valor medido cae fuera del rango ideal configurado en la receta (alerta de desviación de calidad)
        boolean hayAlerta = valorMedido < detalleParametroControl.getValorMinimo()
                || valorMedido > detalleParametroControl.getValorMaximo();

        // 12. Registrar la medición y persistir
        MedicionLoteEntity medicionLote = MedicionLoteEntity.builder()
                .valorMedido(medicionLoteFormDTO.getValorMedido())
                .fechaMedicion(fechaMedicion)
                .estado(EstadoTransaccion.REGISTRADO)
                .hayAlerta(hayAlerta)
                .detalleParametroControl(detalleParametroControl)
                .etapaLote(etapaLote)
                .build();

        return MapperMedicionLote.toDTO(medicionLoteRepository.save(medicionLote));
    }

    /**
     * Anula una medición de lote previamente registrada.
     *
     * @param id El ID de la medición a anular.
     * @param anularMedicionLoteFormDTO Los datos de la anulación (motivo).
     * @return La medición anulada.
     * @throws RecursoNoEncontradoException Si no existe una medición con el ID especificado.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si la medición no
     *                               se encuentra en estado REGISTRADO, o si el lote asociado no se
     *                               encuentra en estado EN_EJECUCION.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.MEDICION_LOTE)
    public MedicionLoteResponseDTO anularMedicion(Long id, AnularMedicionLoteFormDTO anularMedicionLoteFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anularMedicionLoteFormDTO.getMotivoAnulacion() == null || anularMedicionLoteFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Validar que la medición de lote esté registrada en el sistema
        MedicionLoteEntity medicionLote = medicionLoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la medición con ID: " + id));

        // 3. Validar que la medición se encuentre en estado REGISTRADO
        if (medicionLote.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular mediciones en estado REGISTRADO");
        }

        // 4. Validar que el lote asociado se encuentre en estado EN_EJECUCION
        LoteEntity lote = medicionLote.getEtapaLote().getLote();
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder anular una medición");
        }

        // 5. Aplicar la anulación y persistir
        medicionLote.setEstado(EstadoTransaccion.ANULADO);
        medicionLote.setFechaAnulacion(LocalDateTime.now());
        medicionLote.setMotivoAnulacion(anularMedicionLoteFormDTO.getMotivoAnulacion());

        return MapperMedicionLote.toDTO(medicionLoteRepository.save(medicionLote));
    }
}
