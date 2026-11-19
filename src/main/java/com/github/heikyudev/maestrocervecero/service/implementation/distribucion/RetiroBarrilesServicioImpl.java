package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.InvitacionRetiroBarrilesEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IConfiguracionDistribucionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IInvitacionRetiroBarrilesRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.RetiroBarrilesFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRetiroInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.barril.ValidacionesSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRetiroBarrilesServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.BarrilRetiroResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.RetiroBarrilesResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementación de {@link IRetiroBarrilesServicio}.
 * <p>
 * La respuesta bloquea el recordatorio antes de volver a validar el enlace (dos envíos del mismo formulario no registran
 * dos retiros) y luego cada barril, en orden de ID, con el mismo lock que usan las operaciones del barril: una devolución
 * que se registra a la vez espera, o el barril ya figura como devuelto y se omite.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RetiroBarrilesServicioImpl implements IRetiroBarrilesServicio {

    static final String MENSAJE_ENLACE_NO_VALIDO = "El enlace para coordinar el retiro de barriles no es válido";
    static final String MENSAJE_ENLACE_VENCIDO = "El enlace para coordinar el retiro de barriles venció";
    static final String MENSAJE_ENLACE_REEMPLAZADO = "El enlace fue reemplazado por uno más reciente: use el enlace del último correo recibido";
    static final String MENSAJE_CLIENTE_INACTIVO = "El enlace ya no está habilitado para coordinar el retiro de barriles";
    static final String MENSAJE_YA_REGISTRADO = "El retiro ya fue coordinado desde este enlace y no se puede modificar";
    static final String MENSAJE_DATOS_OBLIGATORIOS = "Los datos del retiro son obligatorios";
    static final String MENSAJE_SIN_BARRILES_PENDIENTES = "Ninguno de los barriles sigue pendiente de retiro: ya fueron devueltos o su retiro ya está coordinado";
    static final String NOMBRE_FECHA = "La fecha y hora del retiro";
    static final String CAMPO_FECHA = "fechaRetiro";
    static final String CAMPO_OBSERVACIONES = "observaciones";

    private final IInvitacionRetiroBarrilesRepository invitacionRepository;
    private final IConfiguracionDistribucionRepository configuracionDistribucionRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final ISolicitudBusquedaRepository solicitudBusquedaRepository;
    private final IBarrilRepository barrilRepository;
    private final NotificadorRetiroBarriles notificador;

    @Override
    @Transactional(readOnly = true)
    public RetiroBarrilesResponseDTO buscarRetiro(String token) {
        Long idInvitacion = invitacionRepository.buscarIdPorTokenHash(hashDeTokenValido(token))
                .orElseThrow(() -> new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        InvitacionRetiroBarrilesEntity invitacion = invitacionRepository.findById(idInvitacion)
                .orElseThrow(() -> new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        LocalDateTime ahora = LocalDateTime.now();
        validarVigente(invitacion, ahora);
        int dias = buscarConfiguracion().getDiasMaximosAnticipacionRetiro();
        if (invitacion.getFechaRespuesta() != null) {
            return armarRegistrado(invitacion, dias);
        }
        List<BarrilRetiroResponseDTO> pendientes = despachosOrdenados(invitacion).stream()
                .filter(despacho -> despachoBarrilRepository.estaPendienteDeRetiro(despacho.getId()))
                .map(despacho -> aBarril(despacho, null))
                .toList();
        return RetiroBarrilesResponseDTO.builder()
                .nombreCliente(invitacion.getCliente().getNombre())
                .registrado(false)
                .barrilesPendientes(pendientes)
                .fechaMaximaRetiro(ahora.plusDays(dias))
                .diasMaximosAnticipacionRetiro(dias)
                .barrilesRegistrados(List.of())
                .build();
    }

    @Override
    @Transactional
    public RetiroBarrilesResponseDTO registrarRetiro(String token, RetiroBarrilesFormDTO retiroBarrilesFormDTO) {
        // 1. Bloquear el recordatorio del enlace antes de cargarlo (así se lee su estado vigente) y validar que el enlace
        //    sirva y no se haya respondido
        Long idInvitacion = invitacionRepository.buscarIdPorTokenHash(hashDeTokenValido(token))
                .orElseThrow(() -> new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        InvitacionRetiroBarrilesEntity invitacion = invitacionRepository.buscarPorIdParaActualizar(idInvitacion)
                .orElseThrow(() -> new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        LocalDateTime ahora = LocalDateTime.now();
        validarVigente(invitacion, ahora);
        if (invitacion.getFechaRespuesta() != null) {
            throw new ReglaNegocioException(MENSAJE_YA_REGISTRADO);
        }

        // 2. Validar los datos: la fecha (informada, no anterior a la actual y dentro de los días máximos de anticipación)
        //    y las observaciones (opcionales, normalizadas y con su longitud máxima)
        if (retiroBarrilesFormDTO == null) {
            throw new ReglaNegocioException(MENSAJE_DATOS_OBLIGATORIOS);
        }
        ConfiguracionDistribucionEntity configuracion = buscarConfiguracion();
        LocalDateTime fechaRetiro = retiroBarrilesFormDTO.getFechaRetiro();
        ValidacionesSolicitudBusqueda.validarFechaBusqueda(fechaRetiro, ahora, configuracion.getDiasMaximosAnticipacionRetiro(), NOMBRE_FECHA, CAMPO_FECHA);
        String observaciones = ValidacionesSolicitudBusqueda.normalizarObservaciones(retiroBarrilesFormDTO.getObservaciones(), CAMPO_OBSERVACIONES);

        // 3. Bloquear cada barril del recordatorio en orden de ID (siempre el mismo orden: dos respuestas no se bloquean
        //    mutuamente) y quedarse con los despachos que siguen vigentes y sin solicitud pendiente
        List<DespachoBarrilEntity> pendientes = new ArrayList<>();
        List<DespachoBarrilEntity> despachos = new ArrayList<>(invitacion.getDespachos());
        despachos.sort(Comparator.comparing(despacho -> despacho.getBarril().getId()));
        for (DespachoBarrilEntity despacho : despachos) {
            if (barrilRepository.buscarPorIdParaCambiarEstadoOperativo(despacho.getBarril().getId()).isPresent()
                    && despachoBarrilRepository.estaPendienteDeRetiro(despacho.getId())) {
                pendientes.add(despacho);
            }
        }
        if (pendientes.isEmpty()) {
            throw new ReglaNegocioException(MENSAJE_SIN_BARRILES_PENDIENTES);
        }

        // 4. Registrar una solicitud de búsqueda pendiente por barril, vinculada al recordatorio
        pendientes.sort(Comparator.comparing(DespachoBarrilEntity::getFechaDevolucionEstimada).thenComparing(DespachoBarrilEntity::getId));
        List<SolicitudBusquedaEntity> solicitudes = pendientes.stream()
                .map(despacho -> SolicitudBusquedaEntity.builder()
                        .fechaBusqueda(fechaRetiro)
                        .observaciones(observaciones)
                        .despachoBarril(despacho)
                        .estado(EstadoSolicitudBusqueda.PENDIENTE)
                        .invitacionRetiro(invitacion)
                        .build())
                .toList();
        solicitudBusquedaRepository.saveAll(solicitudes);

        // 5. Dejar el recordatorio respondido: el enlace muestra lo registrado al menos hasta la fecha del retiro
        invitacion.setFechaRespuesta(ahora);
        if (fechaRetiro.isAfter(invitacion.getFechaExpiracion())) {
            invitacion.setFechaExpiracion(fechaRetiro);
        }
        invitacionRepository.save(invitacion);

        // 6. Avisar a los encargados de distribución recién confirmada la transacción
        String nombreCliente = invitacion.getCliente().getNombre();
        List<String> barriles = pendientes.stream().map(RecordatorioRetiroServicioImpl::describirBarril).toList();
        ejecutarAlConfirmar(() -> notificador.avisarEncargadosDeDistribucion(nombreCliente, barriles, fechaRetiro, observaciones));

        return RetiroBarrilesResponseDTO.builder()
                .nombreCliente(nombreCliente)
                .registrado(true)
                .barrilesPendientes(List.of())
                .diasMaximosAnticipacionRetiro(configuracion.getDiasMaximosAnticipacionRetiro())
                .fechaRespuesta(ahora)
                .fechaRetiro(fechaRetiro)
                .observaciones(observaciones)
                .barrilesRegistrados(pendientes.stream().map(despacho -> aBarril(despacho, EstadoSolicitudBusqueda.PENDIENTE)).toList())
                .build();
    }

    /**
     * El hash del token, si tiene un formato posible.
     *
     * @throws EnlaceRetiroInvalidoException Si el token falta o es demasiado largo.
     */
    private static String hashDeTokenValido(String token) {
        if (token == null || token.isBlank() || token.length() > TokenRetiroBarriles.LONGITUD_MAXIMA_TOKEN) {
            throw new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_NO_VALIDO);
        }
        return TokenRetiroBarriles.calcularHash(token);
    }

    /**
     * Verifica que el enlace sirva en el momento indicado: no venció, el cliente sigue activo y, si todavía no se
     * respondió, el cliente no recibió un recordatorio más nuevo (enviado, o en envío y no trabado).
     */
    private void validarVigente(InvitacionRetiroBarrilesEntity invitacion, LocalDateTime ahora) {
        if (invitacion.getFechaExpiracion() == null || !invitacion.getFechaExpiracion().isAfter(ahora)) {
            throw new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_VENCIDO);
        }
        if (invitacion.getCliente().getEstado() != Estado.ACTIVO) {
            throw new EnlaceRetiroInvalidoException(MENSAJE_CLIENTE_INACTIVO);
        }
        if (invitacion.getFechaRespuesta() == null
                && invitacionRepository.existePosteriorEnviada(invitacion.getCliente().getId(), invitacion.getId(),
                RecordatorioRetiroServicioImpl.limiteEnEnvio(ahora))) {
            throw new EnlaceRetiroInvalidoException(MENSAJE_ENLACE_REEMPLAZADO);
        }
    }

    /** El retiro ya registrado desde el recordatorio, con cada barril y el estado de su solicitud. */
    private RetiroBarrilesResponseDTO armarRegistrado(InvitacionRetiroBarrilesEntity invitacion, int dias) {
        List<SolicitudBusquedaEntity> solicitudes = solicitudBusquedaRepository.buscarPorInvitacionRetiro(invitacion.getId());
        SolicitudBusquedaEntity primera = solicitudes.isEmpty() ? null : solicitudes.get(0);
        return RetiroBarrilesResponseDTO.builder()
                .nombreCliente(invitacion.getCliente().getNombre())
                .registrado(true)
                .barrilesPendientes(List.of())
                .diasMaximosAnticipacionRetiro(dias)
                .fechaRespuesta(invitacion.getFechaRespuesta())
                .fechaRetiro(primera == null ? null : primera.getFechaBusqueda())
                .observaciones(primera == null ? null : primera.getObservaciones())
                .barrilesRegistrados(solicitudes.stream().map(solicitud -> aBarril(solicitud.getDespachoBarril(), solicitud.getEstado())).toList())
                .build();
    }

    private static List<DespachoBarrilEntity> despachosOrdenados(InvitacionRetiroBarrilesEntity invitacion) {
        List<DespachoBarrilEntity> despachos = new ArrayList<>(invitacion.getDespachos());
        despachos.sort(Comparator.comparing(DespachoBarrilEntity::getFechaDevolucionEstimada).thenComparing(DespachoBarrilEntity::getId));
        return despachos;
    }

    private static BarrilRetiroResponseDTO aBarril(DespachoBarrilEntity despacho, EstadoSolicitudBusqueda estado) {
        return BarrilRetiroResponseDTO.builder()
                .numeroSerie(despacho.getBarril().getNumeroSerie())
                .identificadorInterno(despacho.getBarril().getIdentificadorInterno())
                .fechaDevolucionEstimada(despacho.getFechaDevolucionEstimada())
                .estado(estado)
                .build();
    }

    /**
     * La única fila de configuración de distribución. Existe siempre: la crea la migración V35.
     */
    private ConfiguracionDistribucionEntity buscarConfiguracion() {
        return configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de distribución: la aplicación no inicializó correctamente"));
    }

    /**
     * Ejecuta la acción cuando la transacción en curso se confirma, o en el momento si no hay una transacción activa.
     */
    static void ejecutarAlConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
