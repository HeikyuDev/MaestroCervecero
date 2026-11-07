package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.SolicitudBusquedaFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ISolicitudBusquedaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.barril.MapperSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCicloVida;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
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
    private final IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    private final IBarrilRepository barrilRepository;

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
     * <p>
     * Orden de locks: el ID del barril del despacho se resuelve con una consulta escalar, sin cargar el despacho, y
     * lo primero que se bloquea (PESSIMISTIC_WRITE) es ese barril, antes de leer el despacho y validar su estado y el
     * del barril. Es el único lock de la operación.
     * </p>
     *
     * @param solicitudBusquedaFormDTO Los datos de la solicitud a registrar.
     * @return La solicitud de búsqueda registrada.
     * @throws ReglaNegocioException Si la fecha de búsqueda no fue informada, si es anterior a la fecha y hora actual, si las observaciones superan los 255 caracteres, si el despacho no se encuentra en estado {@code REGISTRADO}, o si el barril ya no se encuentra en estado operativo {@code DESPACHADO}.
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

        // 3. Normalizar las observaciones (opcionales: en blanco equivale a no informadas) y validar su longitud
        String observaciones = NormalizacionTexto.nombrePropio(solicitudBusquedaFormDTO.getObservaciones());
        if (observaciones != null && observaciones.isEmpty()) {
            observaciones = null;
        }
        if (observaciones != null && observaciones.length() > LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO) {
            throw new ReglaNegocioException("Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres");
        }

        // 4. Resolver el barril del despacho sin cargarlo y bloquearlo para escritura, con el mismo lock que usan
        //    registrar y anular una operación del barril. Si el despacho no existe, se dispara RecursoNoEncontradoException
        Long idDespachoBarril = solicitudBusquedaFormDTO.getIdDespachoBarril();
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(idDespachoBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + idDespachoBarril));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 5. Localizar el despacho, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        DespachoBarrilEntity despachoBarrilEntity = despachoBarrilRepository.findById(idDespachoBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + idDespachoBarril));

        // 6. Validar que el despacho se encuentre en estado REGISTRADO: sobre un despacho anulado no hay barril afuera
        if (despachoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se puede registrar una solicitud de búsqueda sobre un despacho en estado Registrado");
        }

        // 7. Validar que el barril siga despachado: si ya fue devuelto no hay nada que buscar
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("Solo se puede registrar una solicitud de búsqueda sobre un barril despachado");
        }

        // 8. Construir y persistir la solicitud, y retornar el DTO de respuesta correspondiente
        SolicitudBusquedaEntity solicitudBusquedaEntity = SolicitudBusquedaEntity.builder()
                .fechaBusqueda(solicitudBusquedaFormDTO.getFechaBusqueda())
                .observaciones(observaciones)
                .despachoBarril(despachoBarrilEntity)
                .buscado(false)
                .build();

        return MapperSolicitudBusqueda.toDTO(solicitudBusquedaRepository.save(solicitudBusquedaEntity));
    }
}
