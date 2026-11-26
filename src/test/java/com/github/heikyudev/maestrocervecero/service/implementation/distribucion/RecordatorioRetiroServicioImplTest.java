package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.InvitacionRetiroBarrilesEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IConfiguracionDistribucionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IEjecucionRecordatoriosRetiroRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IInvitacionRetiroBarrilesRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.EnvioRecordatorioRetiroPreparado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioRetiroServicioImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 19);

    @Mock
    private IConfiguracionDistribucionRepository configuracionDistribucionRepository;
    @Mock
    private IEjecucionRecordatoriosRetiroRepository ejecucionRepository;
    @Mock
    private IInvitacionRetiroBarrilesRepository invitacionRepository;
    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IClienteRepository clienteRepository;

    @InjectMocks
    private RecordatorioRetiroServicioImpl recordatorioRetiroServicio;

    // ==================== hora y ejecución del día ====================

    @Test
    @DisplayName("CP-RRS-01: alcanzoLaHoraDeEnvio es falso antes de la hora configurada y verdadero desde esa hora")
    void alcanzoLaHora_comparaConLaHoraConfigurada() {
        conConfiguracion(LocalTime.of(9, 0), 7);

        assertThat(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(HOY.atTime(8, 59, 59))).isFalse();
        assertThat(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(HOY.atTime(9, 0))).isTrue();
        assertThat(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(HOY.atTime(23, 30))).isTrue();
    }

    @Test
    @DisplayName("CP-RRS-02: seEnviaronLosRecordatorios informa si el día ya tiene registrada su ejecución")
    void seEnviaron_consultaLaEjecucionDelDia() {
        when(ejecucionRepository.existsById(HOY)).thenReturn(true);
        when(ejecucionRepository.existsById(HOY.plusDays(1))).thenReturn(false);

        assertThat(recordatorioRetiroServicio.seEnviaronLosRecordatorios(HOY)).isTrue();
        assertThat(recordatorioRetiroServicio.seEnviaronLosRecordatorios(HOY.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("CP-RRS-03: los clientes a invitar y los envíos pendientes se piden con el día, el máximo de 3 intentos y el límite de 15 minutos de los envíos trabados")
    void busquedas_delegan() {
        when(despachoBarrilRepository.buscarIdsClientesAInvitarAlRetiro(HOY)).thenReturn(List.of(5L, 8L));
        when(invitacionRepository.buscarIdsClientesConEnviosPendientes(eq(HOY), eq(3), any())).thenReturn(List.of(8L));

        assertThat(recordatorioRetiroServicio.buscarIdsClientesAInvitar(HOY)).containsExactly(5L, 8L);
        assertThat(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(HOY)).containsExactly(8L);
        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(invitacionRepository).buscarIdsClientesConEnviosPendientes(eq(HOY), eq(3), limite.capture());
        assertThat(limite.getValue()).isCloseTo(LocalDateTime.now().minusMinutes(15), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    // ==================== prepararRecordatorio ====================

    @Test
    @DisplayName("CP-RRS-04: prepararRecordatorio no prepara nada si el cliente no existe o no está activo")
    void preparar_clienteInexistenteOInactivo_noPrepara() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.empty());
        ClienteEntity deBaja = cliente(6L);
        deBaja.setEstado(Estado.BAJA);
        when(clienteRepository.findById(6L)).thenReturn(Optional.of(deBaja));

        assertThat(recordatorioRetiroServicio.prepararRecordatorio(5L, HOY)).isEmpty();
        assertThat(recordatorioRetiroServicio.prepararRecordatorio(6L, HOY)).isEmpty();
        verifyNoInteractions(invitacionRepository, despachoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RRS-05: prepararRecordatorio crea el recordatorio del día con los despachos vencidos, el hash del token, un intento y en envío")
    void preparar_nuevo_creaElRecordatorio() {
        // === PREPARACION DE DATOS ===
        ClienteEntity cliente = cliente(5L);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(invitacionRepository.buscarDelClienteEnFechaParaActualizar(5L, HOY)).thenReturn(Optional.empty());
        List<DespachoBarrilEntity> vencidos = List.of(despacho(30L, "SN-1001", "BRL-01"), despacho(31L, "SN-1002", "BRL-02"));
        when(despachoBarrilRepository.buscarVencidosSinSolicitudPendienteDelCliente(5L, HOY)).thenReturn(vencidos);
        conConfiguracion(LocalTime.of(9, 0), 7);
        when(invitacionRepository.saveAndFlush(any(InvitacionRetiroBarrilesEntity.class))).thenAnswer(inv -> {
            InvitacionRetiroBarrilesEntity entidad = inv.getArgument(0);
            entidad.setId(90L);
            return entidad;
        });

        // === EJECUCION ===
        Optional<EnvioRecordatorioRetiroPreparado> resultado = recordatorioRetiroServicio.prepararRecordatorio(5L, HOY);

        // === ASSERTS ===
        ArgumentCaptor<InvitacionRetiroBarrilesEntity> captor = ArgumentCaptor.forClass(InvitacionRetiroBarrilesEntity.class);
        verify(invitacionRepository).saveAndFlush(captor.capture());
        InvitacionRetiroBarrilesEntity guardada = captor.getValue();
        assertThat(guardada.getCliente()).isSameAs(cliente);
        assertThat(guardada.getFechaInvitacion()).isEqualTo(HOY);
        assertThat(guardada.getDespachos()).containsExactlyElementsOf(vencidos);
        assertThat(guardada.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.EN_ENVIO);
        assertThat(guardada.getIntentosEnvio()).isEqualTo(1);
        assertThat(guardada.getFechaExpiracion()).isCloseTo(LocalDateTime.now().plusDays(7), within(5, java.time.temporal.ChronoUnit.SECONDS));

        assertThat(resultado).isPresent();
        EnvioRecordatorioRetiroPreparado envio = resultado.get();
        assertThat(envio.idInvitacion()).isEqualTo(90L);
        assertThat(envio.correo()).isEqualTo("compras@donjorge.com");
        assertThat(envio.nombreCliente()).isEqualTo("Bar Don Jorge");
        assertThat(envio.token()).hasSize(43);
        assertThat(guardada.getTokenHash()).isEqualTo(TokenRetiroBarriles.calcularHash(envio.token())).isNotEqualTo(envio.token());
        assertThat(envio.barriles()).containsExactly("N.º de serie SN-1001 (BRL-01)", "N.º de serie SN-1002 (BRL-02)");
    }

    @Test
    @DisplayName("CP-RRS-06: prepararRecordatorio no crea nada si el cliente ya no tiene barriles vencidos sin solicitud pendiente")
    void preparar_sinVencidos_noCrea() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente(5L)));
        when(invitacionRepository.buscarDelClienteEnFechaParaActualizar(5L, HOY)).thenReturn(Optional.empty());
        when(despachoBarrilRepository.buscarVencidosSinSolicitudPendienteDelCliente(5L, HOY)).thenReturn(List.of());

        assertThat(recordatorioRetiroServicio.prepararRecordatorio(5L, HOY)).isEmpty();
        verify(invitacionRepository, never()).saveAndFlush(any());
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RRS-07: prepararRecordatorio retoma el recordatorio del día que falló con un token nuevo y un intento más")
    void preparar_fallido_retomaConTokenNuevo() {
        // === PREPARACION DE DATOS ===
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente(5L)));
        InvitacionRetiroBarrilesEntity existente = invitacion(90L, EstadoEnvioInvitacion.FALLIDA, 1);
        existente.setTokenHash("hash-anterior");
        when(invitacionRepository.buscarDelClienteEnFechaParaActualizar(5L, HOY)).thenReturn(Optional.of(existente));
        when(despachoBarrilRepository.buscarVencidosSinSolicitudPendienteDelCliente(5L, HOY)).thenReturn(List.of(despacho(30L, "SN-1001", "BRL-01")));
        conConfiguracion(LocalTime.of(9, 0), 7);
        when(invitacionRepository.saveAndFlush(existente)).thenReturn(existente);

        // === EJECUCION ===
        Optional<EnvioRecordatorioRetiroPreparado> resultado = recordatorioRetiroServicio.prepararRecordatorio(5L, HOY);

        // === ASSERTS ===
        assertThat(resultado).isPresent();
        assertThat(existente.getIntentosEnvio()).isEqualTo(2);
        assertThat(existente.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.EN_ENVIO);
        assertThat(existente.getTokenHash()).isNotEqualTo("hash-anterior").isEqualTo(TokenRetiroBarriles.calcularHash(resultado.get().token()));
        assertThat(existente.getDespachos()).hasSize(1);
    }

    @Test
    @DisplayName("CP-RRS-08: prepararRecordatorio no vuelve a enviar el recordatorio del día ya enviado, en envío hace menos de 15 minutos, con los intentos agotados u omitido")
    void preparar_noReintentable_noPrepara() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente(5L)));
        InvitacionRetiroBarrilesEntity enEnvioReciente = invitacion(91L, EstadoEnvioInvitacion.EN_ENVIO, 1);
        enEnvioReciente.setFechaUltimoIntentoEnvio(LocalDateTime.now().minusMinutes(14));
        InvitacionRetiroBarrilesEntity trabadoSinIntentos = invitacion(94L, EstadoEnvioInvitacion.EN_ENVIO, 3);
        trabadoSinIntentos.setFechaUltimoIntentoEnvio(LocalDateTime.now().minusMinutes(30));
        for (InvitacionRetiroBarrilesEntity existente : List.of(invitacion(90L, EstadoEnvioInvitacion.ENVIADA, 1),
                enEnvioReciente, invitacion(92L, EstadoEnvioInvitacion.FALLIDA, 3),
                invitacion(93L, EstadoEnvioInvitacion.OMITIDA, 1), trabadoSinIntentos)) {
            when(invitacionRepository.buscarDelClienteEnFechaParaActualizar(5L, HOY)).thenReturn(Optional.of(existente));

            assertThat(recordatorioRetiroServicio.prepararRecordatorio(5L, HOY)).as(existente.getEstadoEnvio().name()).isEmpty();
        }
        verifyNoInteractions(despachoBarrilRepository);
        verify(invitacionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("CP-RRS-09: prepararRecordatorio deja omitido el recordatorio que falló si el cliente ya no tiene barriles que recordar")
    void preparar_fallidoSinVencidos_quedaOmitido() {
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente(5L)));
        InvitacionRetiroBarrilesEntity existente = invitacion(90L, EstadoEnvioInvitacion.FALLIDA, 1);
        when(invitacionRepository.buscarDelClienteEnFechaParaActualizar(5L, HOY)).thenReturn(Optional.of(existente));
        when(despachoBarrilRepository.buscarVencidosSinSolicitudPendienteDelCliente(5L, HOY)).thenReturn(List.of());

        assertThat(recordatorioRetiroServicio.prepararRecordatorio(5L, HOY)).isEmpty();
        assertThat(existente.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.OMITIDA);
        verify(invitacionRepository).save(existente);
    }

    @Test
    @DisplayName("CP-RRS-13: prepararRecordatorio retoma el recordatorio del día trabado en envío hace más de 15 minutos con un token nuevo")
    void preparar_trabadoEnEnvio_seRecupera() {
        // === PREPARACION DE DATOS ===
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente(5L)));
        InvitacionRetiroBarrilesEntity trabado = invitacion(90L, EstadoEnvioInvitacion.EN_ENVIO, 1);
        trabado.setTokenHash("hash-anterior");
        trabado.setFechaUltimoIntentoEnvio(LocalDateTime.now().minusMinutes(20));
        when(invitacionRepository.buscarDelClienteEnFechaParaActualizar(5L, HOY)).thenReturn(Optional.of(trabado));
        when(despachoBarrilRepository.buscarVencidosSinSolicitudPendienteDelCliente(5L, HOY)).thenReturn(List.of(despacho(30L, "SN-1001", "BRL-01")));
        conConfiguracion(LocalTime.of(9, 0), 7);
        when(invitacionRepository.saveAndFlush(trabado)).thenReturn(trabado);

        // === EJECUCION ===
        Optional<EnvioRecordatorioRetiroPreparado> resultado = recordatorioRetiroServicio.prepararRecordatorio(5L, HOY);

        // === ASSERTS ===
        assertThat(resultado).isPresent();
        assertThat(trabado.getIntentosEnvio()).isEqualTo(2);
        assertThat(trabado.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.EN_ENVIO);
        assertThat(trabado.getFechaUltimoIntentoEnvio()).isCloseTo(LocalDateTime.now(), within(5, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(trabado.getTokenHash()).isNotEqualTo("hash-anterior").isEqualTo(TokenRetiroBarriles.calcularHash(resultado.get().token()));
    }

    // ==================== registrarResultadoEnvio y registrarEjecucion ====================

    @Test
    @DisplayName("CP-RRS-10: registrarResultadoEnvio deja ENVIADA o FALLIDA un recordatorio en envío")
    void registrarResultado_enEnvio_actualiza() {
        InvitacionRetiroBarrilesEntity enviada = invitacion(90L, EstadoEnvioInvitacion.EN_ENVIO, 1);
        InvitacionRetiroBarrilesEntity fallida = invitacion(91L, EstadoEnvioInvitacion.EN_ENVIO, 1);
        when(invitacionRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(enviada));
        when(invitacionRepository.buscarPorIdParaActualizar(91L)).thenReturn(Optional.of(fallida));

        recordatorioRetiroServicio.registrarResultadoEnvio(90L, true);
        recordatorioRetiroServicio.registrarResultadoEnvio(91L, false);

        assertThat(enviada.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.ENVIADA);
        assertThat(fallida.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.FALLIDA);
        verify(invitacionRepository).save(enviada);
        verify(invitacionRepository).save(fallida);
    }

    @Test
    @DisplayName("CP-RRS-11: registrarResultadoEnvio no cambia un recordatorio que ya no está en envío ni uno inexistente")
    void registrarResultado_noEnEnvio_noCambia() {
        InvitacionRetiroBarrilesEntity omitida = invitacion(90L, EstadoEnvioInvitacion.OMITIDA, 1);
        when(invitacionRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(omitida));
        when(invitacionRepository.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

        recordatorioRetiroServicio.registrarResultadoEnvio(90L, true);
        recordatorioRetiroServicio.registrarResultadoEnvio(99L, true);

        assertThat(omitida.getEstadoEnvio()).isEqualTo(EstadoEnvioInvitacion.OMITIDA);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RRS-12: registrarEjecucion inserta el día solo si no existe, en una sola sentencia, aunque se registre dos veces")
    void registrarEjecucion_esIdempotente() {
        recordatorioRetiroServicio.registrarEjecucion(HOY);
        recordatorioRetiroServicio.registrarEjecucion(HOY);

        verify(ejecucionRepository, org.mockito.Mockito.times(2)).registrarSiNoExiste(eq(HOY), org.mockito.ArgumentMatchers.notNull());
        verify(ejecucionRepository, never()).save(any());
        verify(ejecucionRepository, never()).existsById(any());
    }

    // ==================== helpers de construcción ====================

    private void conConfiguracion(LocalTime hora, int dias) {
        when(configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)).thenReturn(Optional.of(
                ConfiguracionDistribucionEntity.builder().id(1L).horaEnvioRecordatorioRetiro(hora).diasMaximosAnticipacionRetiro(dias).build()));
    }

    private static ClienteEntity cliente(Long id) {
        return ClienteEntity.builder().id(id).nombre("Bar Don Jorge").email("compras@donjorge.com").estado(Estado.ACTIVO).build();
    }

    private static DespachoBarrilEntity despacho(Long id, String numeroSerie, String identificador) {
        BarrilEntity barril = BarrilEntity.builder().id(id + 100).numeroSerie(numeroSerie).identificadorInterno(identificador).estado(Estado.ACTIVO).build();
        return DespachoBarrilEntity.builder().id(id).barril(barril).estado(EstadoTransaccion.REGISTRADO)
                .fechaDevolucionEstimada(HOY.minusDays(3)).build();
    }

    private static InvitacionRetiroBarrilesEntity invitacion(Long id, EstadoEnvioInvitacion estado, int intentos) {
        return InvitacionRetiroBarrilesEntity.builder().id(id).cliente(cliente(5L)).fechaInvitacion(HOY).estadoEnvio(estado)
                .intentosEnvio(intentos).fechaExpiracion(HOY.plusDays(7).atStartOfDay()).despachos(new ArrayList<>()).build();
    }
}
