package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.RegistrarEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEnvasadoLoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.lote.MapperEnvasadoLote;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementación de {@link IEnvasadoLoteServicio}.
 */
@Service
@RequiredArgsConstructor
public class EnvasadoLoteServicioImpl implements IEnvasadoLoteServicio {

    private final IEnvasadoLoteRepository envasadoLoteRepository;
    private final IEtapaLoteRepository etapaLoteRepository;
    private final IBarrilRepository barrilRepository;

    /**
     * Filtra los envasados de lote de una etapa de lote puntual.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param idBarril El ID del barril utilizado a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para asumir {@code REGISTRADO}.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link EnvasadoLoteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EnvasadoLoteResponseDTO> filtrarEnvasadosLote(Long idEtapaLote, Long idBarril, EstadoTransaccion estado, Pageable pageable) {
        EstadoTransaccion estadoEfectivo = estado != null ? estado : EstadoTransaccion.REGISTRADO;
        return envasadoLoteRepository.filtrarEnvasadosLote(idEtapaLote, idBarril, estadoEfectivo, pageable)
                .map(MapperEnvasadoLote::toDTO);
    }

    /**
     * Busca y retorna un envasado de lote mediante su identificador único.
     *
     * @param id El ID del envasado.
     * @return El envasado correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un envasado con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public EnvasadoLoteResponseDTO buscarPorId(Long id) {
        return MapperEnvasadoLote.toDTO(envasadoLoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el envasado de lote con ID: " + id)));
    }

    /**
     * Registra el traspaso de cerveza desde el fermentador hacia el barril seleccionado, durante
     * la etapa de Envasado de un lote.
     * <p>
     * El barril se busca bloqueado para escritura ({@code buscarPorIdParaCambiarEstadoOperativo}):
     * dos envasados registrados en simultáneo sobre el mismo barril no deben poder pasar ambos.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote sobre la que se registra el envasado (obligatorio; no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla).
     * @param registrarEnvasadoLoteFormDTO Los datos del envasado a registrar.
     * @return El envasado registrado.
     * @throws RecursoNoEncontradoException Si la etapa de lote o el barril referenciados no existen.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si la etapa
     *                               no está EN_CURSO, si el tipo de etapa no admite registrar
     *                               envasados, si el barril no se encuentra en estado operativo
     *                               DISPONIBLE, si la cantidad a envasar no es mayor a cero, o si
     *                               supera la capacidad del barril seleccionado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.ENVASADO_LOTE)
    public EnvasadoLoteResponseDTO registrarEnvasadoLote(Long idEtapaLote, RegistrarEnvasadoLoteFormDTO registrarEnvasadoLoteFormDTO) {
        // 1. Validar que la etapa de lote esté registrada en el sistema y habilitada para envasar
        EtapaLoteEntity etapaLote = etapaLoteRepository.findById(idEtapaLote)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa de lote con ID: " + idEtapaLote));
        validarEtapaHabilitadaParaEnvasado(etapaLote);

        // 2. Validar que el barril esté registrado en el sistema, bloqueándolo para escritura
        BarrilEntity barril = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(registrarEnvasadoLoteFormDTO.getIdBarril())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + registrarEnvasadoLoteFormDTO.getIdBarril()));

        // 3. Validar que el barril se encuentre en estado operativo DISPONIBLE
        if (barril.getEstadoOperativo() != EstadoOperativoBarril.DISPONIBLE) {
            throw new ReglaNegocioException("El barril debe encontrarse en estado operativo DISPONIBLE para poder envasar en él");
        }

        // 4. Validar que la cantidad a envasar sea mayor a cero
        Double cantidad = registrarEnvasadoLoteFormDTO.getCantidad();
        if (cantidad == null || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad a envasar debe ser mayor a cero");
        }

        // 5. Validar que la cantidad a envasar no supere la capacidad del barril seleccionado
        if (cantidad > barril.getCapacidad()) {
            throw new ReglaNegocioException("La cantidad a envasar no puede superar la capacidad del barril seleccionado");
        }

        // 6. El barril pasa a contener la cerveza envasada
        barril.setEstadoOperativo(EstadoOperativoBarril.CON_CERVEZA);
        barril.setContenidoActual(cantidad);
        barrilRepository.save(barril);

        // 7. Registrar el envasado y persistir
        EnvasadoLoteEntity envasadoLote = EnvasadoLoteEntity.builder()
                .cantidadEnvasada(cantidad)
                .estado(EstadoTransaccion.REGISTRADO)
                .etapaLote(etapaLote)
                .barril(barril)
                .build();

        return MapperEnvasadoLote.toDTO(envasadoLoteRepository.save(envasadoLote));
    }

    /**
     * Anula un envasado de lote existente, revirtiendo el traspaso: el barril vuelve a estar
     * vacío y disponible.
     * <p>
     * El barril se busca bloqueado para escritura ({@code buscarPorIdParaCambiarEstadoOperativo}),
     * por el mismo motivo de concurrencia que en {@code registrarEnvasadoLote}.
     * </p>
     *
     * @param id El ID del envasado de lote a anular.
     * @param anularEnvasadoLoteFormDTO Los datos de la anulación (motivo).
     * @return El envasado de lote anulado.
     * @throws RecursoNoEncontradoException Si el envasado de lote o el barril asociado no existen.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el envasado no
     *                               se encuentra en estado REGISTRADO, si el lote asociado no se
     *                               encuentra en estado EN_EJECUCION, o si el barril asociado ya
     *                               no mantiene su contenido intacto (por ejemplo, porque ya fue
     *                               despachado o limpiado por otra operación).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.ENVASADO_LOTE)
    public EnvasadoLoteResponseDTO anularEnvasadoLote(Long id, AnularEnvasadoLoteFormDTO anularEnvasadoLoteFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anularEnvasadoLoteFormDTO.getMotivoAnulacion() == null || anularEnvasadoLoteFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Validar que el envasado de lote esté registrado en el sistema
        EnvasadoLoteEntity envasadoLote = envasadoLoteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el envasado de lote con ID: " + id));

        // 3. Validar que el envasado se encuentre en estado REGISTRADO
        if (envasadoLote.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular envasados en estado REGISTRADO");
        }

        // 4. Validar que el lote asociado se encuentre en estado EN_EJECUCION
        LoteEntity lote = envasadoLote.getEtapaLote().getLote();
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder anular un envasado");
        }

        // 5. Recuperar el barril bloqueado para escritura y validar que mantenga su contenido intacto
        BarrilEntity barril = barrilRepository.buscarPorIdParaCambiarEstadoOperativo(envasadoLote.getBarril().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + envasadoLote.getBarril().getId()));

        if (barril.getEstadoOperativo() != EstadoOperativoBarril.CON_CERVEZA
                || !barril.getContenidoActual().equals(envasadoLote.getCantidadEnvasada())) {
            throw new ReglaNegocioException("No se puede anular el envasado porque el barril asociado ya no mantiene su contenido intacto");
        }

        // 6. Revertir el barril: vuelve a estar vacío y disponible
        barril.setEstadoOperativo(EstadoOperativoBarril.DISPONIBLE);
        barril.setContenidoActual(0.0);
        barrilRepository.save(barril);

        // 7. Aplicar la anulación sobre el envasado y persistir
        envasadoLote.setEstado(EstadoTransaccion.ANULADO);
        envasadoLote.setFechaAnulacion(LocalDateTime.now());
        envasadoLote.setMotivoAnulacion(anularEnvasadoLoteFormDTO.getMotivoAnulacion());

        return MapperEnvasadoLote.toDTO(envasadoLoteRepository.save(envasadoLote));
    }

    /**
     * Valida que una etapa de lote esté habilitada para registrar envasados: el lote asociado
     * debe estar EN_EJECUCION, la etapa debe ser la etapa actual del lote (EN_CURSO), y su tipo de
     * etapa debe admitir el registro de envasados (solo ENVASADO lo admite).
     *
     * @param etapaLote La etapa de lote a validar.
     * @throws ReglaNegocioException Si alguna de las tres condiciones no se cumple.
     */
    private void validarEtapaHabilitadaParaEnvasado(EtapaLoteEntity etapaLote) {
        LoteEntity lote = etapaLote.getLote();
        if (lote.getEstado() != EstadoLote.EN_EJECUCION) {
            throw new ReglaNegocioException("El lote debe encontrarse en estado EN_EJECUCION para poder registrar un envasado");
        }

        if (etapaLote.getEstado() != EstadoEtapaLote.EN_CURSO) {
            throw new ReglaNegocioException("Solo se pueden registrar envasados sobre la etapa actualmente en curso del lote");
        }

        TipoEtapa tipoEtapa = etapaLote.getEtapa();
        if (!tipoEtapa.isPermiteRegistrarEnvasado()) {
            throw new ReglaNegocioException("La etapa " + tipoEtapa + " no admite el registro de envasados");
        }
    }
}
