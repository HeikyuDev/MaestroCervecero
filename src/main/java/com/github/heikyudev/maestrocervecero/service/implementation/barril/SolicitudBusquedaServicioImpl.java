package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
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
     * Filtra las solicitudes de búsqueda de barril, opcionalmente por cliente, rango de fecha de
     * búsqueda y/o estado.
     *
     * @param idCliente El ID del cliente del despacho a filtrar, o {@code null} para no filtrar por él.
     * @param fechaBusquedaDesde Límite inferior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param fechaBusquedaHasta Límite superior (inclusive) del rango de fecha de búsqueda, o {@code null} para no acotarlo.
     * @param estado El estado de la solicitud, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link SolicitudBusquedaResponseDTO} correspondientes.
     * @throws ReglaNegocioException Si la fecha desde es posterior a la fecha hasta.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudBusquedaResponseDTO> filtrarSolicitudesBusqueda(Long idCliente, LocalDateTime fechaBusquedaDesde, LocalDateTime fechaBusquedaHasta, EstadoSolicitudBusqueda estado, Pageable pageable) {
        // Un rango invertido no tiene resultados posibles: se informa en lugar de devolver una lista vacía sin motivo
        if (fechaBusquedaDesde != null && fechaBusquedaHasta != null && fechaBusquedaDesde.isAfter(fechaBusquedaHasta)) {
            throw new ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        return solicitudBusquedaRepository.filtrarSolicitudesBusqueda(idCliente, fechaBusquedaDesde, fechaBusquedaHasta, estado, pageable)
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
     * fecha de devolución estimada pactada en el despacho. Queda {@code PENDIENTE} hasta
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
     * @throws ReglaNegocioException Si la fecha de búsqueda no fue informada, si es anterior a la fecha y hora actual, si las observaciones superan los 255 caracteres, si el despacho no se encuentra en estado {@code REGISTRADO}, si el barril ya no se encuentra en estado operativo {@code DESPACHADO}, si el despacho no es el vigente del barril (su último despacho registrado) o si el despacho ya tiene una solicitud de búsqueda pendiente.
     * @throws RecursoNoEncontradoException Si el despacho de barril referenciado no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.SOLICITUD_BUSQUEDA)
    public SolicitudBusquedaResponseDTO registrarSolicitudBusqueda(SolicitudBusquedaFormDTO solicitudBusquedaFormDTO) {
        // 1. Validar la fecha de búsqueda: informada y no anterior a la fecha y hora actual
        ValidacionesSolicitudBusqueda.validarFechaBusqueda(solicitudBusquedaFormDTO.getFechaBusqueda(), LocalDateTime.now(), null, "La fecha de búsqueda", null);

        // 2. Normalizar las observaciones (opcionales: en blanco equivalen a no informadas) y validar su longitud
        String observaciones = ValidacionesSolicitudBusqueda.normalizarObservaciones(solicitudBusquedaFormDTO.getObservaciones(), null);

        // 3. Resolver el barril del despacho sin cargarlo y bloquearlo para escritura, con el mismo lock que usan
        //    registrar y anular una operación del barril. Si el despacho no existe, se dispara RecursoNoEncontradoException
        Long idDespachoBarril = solicitudBusquedaFormDTO.getIdDespachoBarril();
        Long idBarril = operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(idDespachoBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + idDespachoBarril));
        BarrilEntity barrilEntity = MetodosCicloVida.resolverBarril(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril), idBarril);

        // 4. Localizar el despacho, ya con el barril bloqueado. Si no existe, se dispara RecursoNoEncontradoException
        DespachoBarrilEntity despachoBarrilEntity = despachoBarrilRepository.findById(idDespachoBarril)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el despacho de barril con ID: " + idDespachoBarril));

        // 5. Validar que el despacho se encuentre en estado REGISTRADO: sobre un despacho anulado no hay barril afuera
        if (despachoBarrilEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se puede registrar una solicitud de búsqueda sobre un despacho en estado Registrado");
        }

        // 6. Validar que el barril siga despachado: si ya fue devuelto no hay nada que buscar
        if (barrilEntity.getEstadoOperativo() != EstadoOperativoBarril.DESPACHADO) {
            throw new ReglaNegocioException("Solo se puede registrar una solicitud de búsqueda sobre un barril despachado");
        }

        // 7. Validar que sea el despacho vigente del barril (su último despacho registrado): uno anterior ya no tiene al
        //    barril en poder del cliente
        Long idDespachoVigente = despachoBarrilRepository.findFirstByBarril_IdAndEstadoOrderByFechaDescIdDesc(idBarril, EstadoTransaccion.REGISTRADO)
                .map(DespachoBarrilEntity::getId).orElse(null);
        if (!idDespachoBarril.equals(idDespachoVigente)) {
            throw new ReglaNegocioException("Solo se puede registrar una solicitud de búsqueda sobre el despacho vigente del barril");
        }

        // 8. Validar que el despacho no tenga ya una solicitud pendiente: a lo sumo una por despacho (ver V35)
        if (solicitudBusquedaRepository.findFirstByDespachoBarril_IdAndEstado(idDespachoBarril, EstadoSolicitudBusqueda.PENDIENTE).isPresent()) {
            throw new ReglaNegocioException("El despacho ya tiene una solicitud de búsqueda pendiente");
        }

        // 9. Construir y persistir la solicitud, y retornar el DTO de respuesta correspondiente
        SolicitudBusquedaEntity solicitudBusquedaEntity = SolicitudBusquedaEntity.builder()
                .fechaBusqueda(solicitudBusquedaFormDTO.getFechaBusqueda())
                .observaciones(observaciones)
                .despachoBarril(despachoBarrilEntity)
                .estado(EstadoSolicitudBusqueda.PENDIENTE)
                .build();

        return MapperSolicitudBusqueda.toDTO(solicitudBusquedaRepository.save(solicitudBusquedaEntity));
    }
}
