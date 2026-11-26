package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.InvitacionRetiroBarrilesEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IConfiguracionDistribucionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IEjecucionRecordatoriosRetiroRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IInvitacionRetiroBarrilesRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.EnvioRecordatorioRetiroPreparado;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRecordatorioRetiroServicio;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link IRecordatorioRetiroServicio}.
 * <p>
 * Un cliente recibe a lo sumo un recordatorio por día: lo garantiza la unicidad de cliente y día del recordatorio, de modo
 * que dos procesos que preparan el mismo recordatorio a la vez no envían dos correos (el segundo falla al guardarlo). Cada
 * intento de envío genera un token nuevo, que se guarda antes de enviar el correo. Un recordatorio que sigue en envío
 * después de {@code LimitesTecnicos.ENVIO_RECORDATORIO_RETIRO_TRABADO_MINUTOS} minutos se considera trabado (la aplicación
 * se detuvo antes de registrar el resultado) y se vuelve a intentar.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RecordatorioRetiroServicioImpl implements IRecordatorioRetiroServicio {

    static final int MAXIMO_INTENTOS_ENVIO = LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO;

    private final IConfiguracionDistribucionRepository configuracionDistribucionRepository;
    private final IEjecucionRecordatoriosRetiroRepository ejecucionRepository;
    private final IInvitacionRetiroBarrilesRepository invitacionRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IClienteRepository clienteRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean alcanzoLaHoraDeEnvio(LocalDateTime ahora) {
        return !ahora.toLocalTime().isBefore(buscarConfiguracion().getHoraEnvioRecordatorioRetiro());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean seEnviaronLosRecordatorios(LocalDate fecha) {
        return ejecucionRepository.existsById(fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> buscarIdsClientesAInvitar(LocalDate fecha) {
        return despachoBarrilRepository.buscarIdsClientesAInvitarAlRetiro(fecha);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> buscarIdsClientesConEnviosPendientes(LocalDate fecha) {
        return invitacionRepository.buscarIdsClientesConEnviosPendientes(fecha, MAXIMO_INTENTOS_ENVIO, limiteEnEnvio(LocalDateTime.now()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<EnvioRecordatorioRetiroPreparado> prepararRecordatorio(Long idCliente, LocalDate fecha) {
        // 1. El cliente tiene que existir y seguir activo
        Optional<ClienteEntity> encontrado = clienteRepository.findById(idCliente);
        if (encontrado.isEmpty() || encontrado.get().getEstado() != Estado.ACTIVO) {
            return Optional.empty();
        }
        ClienteEntity cliente = encontrado.get();

        // 2. Si ya tiene el recordatorio del día (bloqueado), solo se retoma si no se envió todavía o falló y le quedan intentos
        Optional<InvitacionRetiroBarrilesEntity> existente = invitacionRepository.buscarDelClienteEnFechaParaActualizar(idCliente, fecha);
        if (existente.isPresent() && !esReintentable(existente.get(), LocalDateTime.now())) {
            return Optional.empty();
        }

        // 3. Los barriles a recordar son los de despachos vigentes vencidos sin solicitud pendiente, en el momento del envío.
        //    Sin ninguno, el recordatorio que falló queda omitido (no hay nada que recordar) y uno nuevo no se crea
        List<DespachoBarrilEntity> vencidos = despachoBarrilRepository.buscarVencidosSinSolicitudPendienteDelCliente(idCliente, fecha);
        if (vencidos.isEmpty()) {
            existente.ifPresent(invitacion -> {
                invitacion.setEstadoEnvio(EstadoEnvioInvitacion.OMITIDA);
                invitacionRepository.save(invitacion);
            });
            return Optional.empty();
        }
        InvitacionRetiroBarrilesEntity invitacion = existente.orElseGet(() -> InvitacionRetiroBarrilesEntity.builder()
                .cliente(cliente)
                .fechaInvitacion(fecha)
                .build());
        invitacion.getDespachos().clear();
        invitacion.getDespachos().addAll(vencidos);

        // 4. Cada intento usa un token nuevo (el del intento fallido nunca llegó) y se guarda antes de enviar: si el
        //    resultado no se llega a registrar, el enlace que recibió el cliente igual sirve
        LocalDateTime ahora = LocalDateTime.now();
        String token = TokenRetiroBarriles.generar();
        invitacion.setTokenHash(TokenRetiroBarriles.calcularHash(token));
        invitacion.setFechaExpiracion(ahora.plusDays(buscarConfiguracion().getDiasMaximosAnticipacionRetiro()));
        invitacion.setIntentosEnvio(invitacion.getIntentosEnvio() + 1);
        invitacion.setFechaUltimoIntentoEnvio(ahora);
        invitacion.setEstadoEnvio(EstadoEnvioInvitacion.EN_ENVIO);
        InvitacionRetiroBarrilesEntity guardada = invitacionRepository.saveAndFlush(invitacion);

        List<String> barriles = vencidos.stream().map(RecordatorioRetiroServicioImpl::describirBarril).toList();
        return Optional.of(new EnvioRecordatorioRetiroPreparado(guardada.getId(), cliente.getEmail(), cliente.getNombre(), token,
                guardada.getFechaExpiracion(), barriles));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarResultadoEnvio(Long idInvitacion, boolean enviado) {
        invitacionRepository.buscarPorIdParaActualizar(idInvitacion)
                .filter(invitacion -> invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.EN_ENVIO)
                .ifPresent(invitacion -> {
                    invitacion.setEstadoEnvio(enviado ? EstadoEnvioInvitacion.ENVIADA : EstadoEnvioInvitacion.FALLIDA);
                    invitacionRepository.save(invitacion);
                });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEjecucion(LocalDate fecha) {
        // Una sola sentencia que no inserta si el día ya existe: dos procesos que lo registran a la vez no chocan
        ejecucionRepository.registrarSiNoExiste(fecha, LocalDateTime.now());
    }

    /** Un barril como lo identifica el cliente: su número de serie y, entre paréntesis, el identificador interno. */
    static String describirBarril(DespachoBarrilEntity despacho) {
        return "N.º de serie " + despacho.getBarril().getNumeroSerie() + " (" + despacho.getBarril().getIdentificadorInterno() + ")";
    }

    /**
     * Un recordatorio se vuelve a intentar si no se envió todavía, falló o quedó trabado en envío, y le quedan intentos.
     */
    private static boolean esReintentable(InvitacionRetiroBarrilesEntity invitacion, LocalDateTime ahora) {
        boolean trabado = invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.EN_ENVIO
                && invitacion.getFechaUltimoIntentoEnvio() != null && invitacion.getFechaUltimoIntentoEnvio().isBefore(limiteEnEnvio(ahora));
        return (invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.PENDIENTE || invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.FALLIDA || trabado)
                && invitacion.getIntentosEnvio() < MAXIMO_INTENTOS_ENVIO;
    }

    /**
     * Momento antes del cual un recordatorio que sigue en envío se considera trabado: la aplicación se detuvo antes de
     * registrar el resultado del envío.
     */
    static LocalDateTime limiteEnEnvio(LocalDateTime ahora) {
        return ahora.minusMinutes(LimitesTecnicos.ENVIO_RECORDATORIO_RETIRO_TRABADO_MINUTOS);
    }

    /**
     * La única fila de configuración de distribución. Existe siempre: la crea la migración V35.
     */
    private ConfiguracionDistribucionEntity buscarConfiguracion() {
        return configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de distribución: la aplicación no inicializó correctamente"));
    }
}
