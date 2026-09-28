package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BusquedaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBusquedaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionBusquedaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BusquedaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBusquedaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BusquedaBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperBusquedaBarril;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BusquedaBarrilServicioImpl implements IBusquedaBarrilServicio {

    private final IBusquedaBarrilRepository busquedaBarrilRepository;
    private final ISolicitudBusquedaRepository solicitudBusquedaRepository;

    /**
     * Filtra las búsquedas de barril, opcionalmente por solicitud de búsqueda y/o estado.
     *
     * @param idSolicitudBusqueda El ID de la solicitud de búsqueda a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link BusquedaBarrilResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<BusquedaBarrilResponseDTO> filtrarBusquedasBarril(Long idSolicitudBusqueda, EstadoTransaccion estado, Pageable pageable) {
        return busquedaBarrilRepository.filtrarBusquedasBarril(idSolicitudBusqueda, estado, pageable)
                .map(MapperBusquedaBarril::toDTO);
    }

    /**
     * Busca y retorna una búsqueda de barril mediante su identificador único.
     *
     * @param id El ID de la búsqueda de barril.
     * @return La búsqueda de barril correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna búsqueda de barril con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public BusquedaBarrilResponseDTO buscarPorId(Long id) {
        return MapperBusquedaBarril.toDTO(busquedaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la búsqueda de barril con ID: " + id)));
    }

    /**
     * Registra una nueva búsqueda de barril.
     * <p>
     * Marca la solicitud de búsqueda asociada como {@code buscado = true}: mientras la búsqueda
     * siga registrada, no se puede volver a registrar otra sobre la misma solicitud.
     * </p>
     *
     * @param busquedaBarrilFormDTO Los datos de la búsqueda a registrar.
     * @return La búsqueda de barril registrada.
     * @throws ReglaNegocioException Si la solicitud de búsqueda ya fue buscada.
     * @throws RecursoNoEncontradoException Si la solicitud de búsqueda referenciada no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.BUSQUEDA_BARRIL)
    public BusquedaBarrilResponseDTO registrarBusquedaBarril(BusquedaBarrilFormDTO busquedaBarrilFormDTO) {
        // 1. Localizar la solicitud de búsqueda. Si no existe, se dispara RecursoNoEncontradoException
        Long idSolicitudBusqueda = busquedaBarrilFormDTO.getIdSolicitudBusqueda();
        SolicitudBusquedaEntity solicitudBusquedaEntity = solicitudBusquedaRepository.findById(idSolicitudBusqueda)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de búsqueda con ID: " + idSolicitudBusqueda));

        // 2. Validar que la solicitud de búsqueda todavía no haya sido buscada
        if (solicitudBusquedaEntity.isBuscado()) {
            throw new ReglaNegocioException("Solo se puede registrar una búsqueda sobre una solicitud de búsqueda que todavía no fue buscada");
        }

        // 3. Marcar la solicitud de búsqueda como buscada y persistirla
        solicitudBusquedaEntity.setBuscado(true);
        solicitudBusquedaRepository.save(solicitudBusquedaEntity);

        // 4. Construir y persistir la búsqueda, y retornar el DTO de respuesta correspondiente
        BusquedaBarrilEntity busquedaBarrilEntity = BusquedaBarrilEntity.builder()
                .estado(EstadoTransaccion.REGISTRADO)
                .solicitudBusqueda(solicitudBusquedaEntity)
                .build();

        return MapperBusquedaBarril.toDTO(busquedaBarrilRepository.save(busquedaBarrilEntity));
    }

    /**
     * Anula una búsqueda de barril existente.
     * <p>
     * Restablece la solicitud de búsqueda asociada a {@code buscado = false}, permitiendo que se
     * registre una nueva búsqueda sobre ella.
     * </p>
     *
     * @param id El ID de la búsqueda de barril a anular.
     * @param anulacionFormDTO Los datos de la anulación (motivo).
     * @return La búsqueda de barril anulada.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, o si la búsqueda no se encuentra en estado {@code REGISTRADO}.
     * @throws RecursoNoEncontradoException Si la búsqueda de barril con el ID especificado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.BUSQUEDA_BARRIL)
    public BusquedaBarrilResponseDTO anularBusquedaBarril(Long id, AnulacionBusquedaBarrilFormDTO anulacionFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anulacionFormDTO.getMotivoAnulacion() == null || anulacionFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar la búsqueda de barril. Si no existe, se dispara RecursoNoEncontradoException
        BusquedaBarrilEntity busquedaBarrilEntity = busquedaBarrilRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la búsqueda de barril con ID: " + id));

        // 3. Validar que la búsqueda se encuentre en estado REGISTRADO
        if (busquedaBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular búsquedas de barril en estado REGISTRADO");
        }

        // 4. Restablecer la solicitud de búsqueda asociada a buscado = false y persistirla
        SolicitudBusquedaEntity solicitudBusquedaEntity = busquedaBarrilEntity.getSolicitudBusqueda();
        solicitudBusquedaEntity.setBuscado(false);
        solicitudBusquedaRepository.save(solicitudBusquedaEntity);

        // 5. Aplicar la anulación sobre la búsqueda y persistirla
        busquedaBarrilEntity.setEstado(EstadoTransaccion.ANULADO);
        busquedaBarrilEntity.setFechaAnulacion(LocalDateTime.now());
        busquedaBarrilEntity.setMotivoAnulacion(anulacionFormDTO.getMotivoAnulacion());

        return MapperBusquedaBarril.toDTO(busquedaBarrilRepository.save(busquedaBarrilEntity));
    }
}
