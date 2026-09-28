package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.SolicitudBusquedaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ISolicitudBusquedaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperSolicitudBusqueda;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SolicitudBusquedaServicioImpl implements ISolicitudBusquedaServicio {

    private final ISolicitudBusquedaRepository solicitudBusquedaRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;

    /**
     * Filtra las solicitudes de búsqueda de barril, opcionalmente por despacho de barril, rango
     * de fecha de búsqueda y/o si ya fueron buscadas.
     *
     * @param idDespachoBarril El ID del despacho de barril a filtrar, o {@code null} para no filtrar por él.
     * @param fechaBusquedaDesde Límite inferior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param fechaBusquedaHasta Límite superior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param buscado Si ya se realizó la búsqueda del barril, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link SolicitudBusquedaResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudBusquedaResponseDTO> filtrarSolicitudesBusqueda(Long idDespachoBarril, LocalDateTime fechaBusquedaDesde, LocalDateTime fechaBusquedaHasta, Boolean buscado, Pageable pageable) {
        return solicitudBusquedaRepository.filtrarSolicitudesBusqueda(idDespachoBarril, fechaBusquedaDesde, fechaBusquedaHasta, buscado, pageable)
                .map(MapperSolicitudBusqueda::toDTO);
    }

    /**
     * Busca y retorna una solicitud de búsqueda de barril mediante su identificador único.
     *
     * @param id El ID de la solicitud de búsqueda.
     * @return La solicitud de búsqueda correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ninguna solicitud de búsqueda con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public SolicitudBusquedaResponseDTO buscarPorId(Long id) {
        return MapperSolicitudBusqueda.toDTO(solicitudBusquedaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de búsqueda con ID: " + id)));
    }

    /**
     * Registra una nueva solicitud de búsqueda de barril.
     * <p>
     * La crea el cliente a través del enlace que le llega por correo cuando su barril supera la
     * fecha de devolución estimada pactada en el despacho. Queda con {@code buscado = false} hasta
     * que efectivamente se realice la búsqueda.
     * </p>
     *
     * @param solicitudBusquedaFormDTO Los datos de la solicitud a registrar.
     * @return La solicitud de búsqueda registrada.
     * @throws ReglaNegocioException Si la fecha de búsqueda no fue informada, o si es anterior a la fecha y hora actual.
     * @throws RecursoNoEncontradoException Si el despacho de barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.SOLICITUD_BUSQUEDA)
    public SolicitudBusquedaResponseDTO registrarSolicitudBusqueda(SolicitudBusquedaFormDTO solicitudBusquedaFormDTO) {
        // 1. Validar que se haya informado la fecha de búsqueda
        if (solicitudBusquedaFormDTO.getFechaBusqueda() == null) {
            throw new ReglaNegocioException("La fecha de búsqueda es obligatoria");
        }

        // 2. Validar que la fecha de búsqueda no sea anterior a la fecha y hora actual
        if (solicitudBusquedaFormDTO.getFechaBusqueda().isBefore(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de búsqueda no puede ser anterior a la fecha y hora actual");
        }

        // 3. Localizar el despacho de barril. Si no existe, se dispara RecursoNoEncontradoException
        Long idDespachoBarril = solicitudBusquedaFormDTO.getIdDespachoBarril();
        DespachoBarrilEntity despachoBarrilEntity = despachoBarrilRepository.findById(idDespachoBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + idDespachoBarril));

        // 4. Construir y persistir la solicitud, y retornar el DTO de respuesta correspondiente
        SolicitudBusquedaEntity solicitudBusquedaEntity = SolicitudBusquedaEntity.builder()
                .fechaBusqueda(solicitudBusquedaFormDTO.getFechaBusqueda())
                .observaciones(solicitudBusquedaFormDTO.getObservaciones())
                .despachoBarril(despachoBarrilEntity)
                .buscado(false)
                .build();

        return MapperSolicitudBusqueda.toDTO(solicitudBusquedaRepository.save(solicitudBusquedaEntity));
    }
}
