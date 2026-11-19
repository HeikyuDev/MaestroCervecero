package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.ConfiguracionDistribucionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.InvitacionRetiroBarrilesEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IConfiguracionDistribucionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.distribucion.IInvitacionRetiroBarrilesRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.RetiroBarrilesFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRetiroInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.RetiroBarrilesResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetiroBarrilesServicioImplTest {

    private static final String TOKEN = "kV2s9fJ1x0QmB7r3TzYwLp4cHn8aUe6dGqXo5iSjNkA";
    private static final String HASH = TokenRetiroBarriles.calcularHash(TOKEN);

    @Mock
    private IInvitacionRetiroBarrilesRepository invitacionRepository;
    @Mock
    private IConfiguracionDistribucionRepository configuracionDistribucionRepository;
    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private ISolicitudBusquedaRepository solicitudBusquedaRepository;
    @Mock
    private IBarrilRepository barrilRepository;
    @Mock
    private NotificadorRetiroBarriles notificador;

    @InjectMocks
    private RetiroBarrilesServicioImpl retiroBarrilesServicio;

    // ==================== validación del enlace ====================

    @Test
    @DisplayName("CP-RBS-01: un token nulo, en blanco o demasiado largo no es un enlace válido y no se consulta la base")
    void buscar_tokenConFormatoImposible_rechaza() {
        for (String token : new String[]{null, "  ", "a".repeat(129)}) {
            assertThatThrownBy(() -> retiroBarrilesServicio.buscarRetiro(token))
                    .isInstanceOf(EnlaceRetiroInvalidoException.class)
                    .hasMessage("El enlace para coordinar el retiro de barriles no es válido");
        }
        verifyNoInteractions(invitacionRepository);
    }

    @Test
    @DisplayName("CP-RBS-02: un token que no corresponde a ningún recordatorio no es un enlace válido")
    void buscar_tokenDesconocido_rechaza() {
        when(invitacionRepository.buscarIdPorTokenHash(HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> retiroBarrilesServicio.buscarRetiro(TOKEN))
                .isInstanceOf(EnlaceRetiroInvalidoException.class)
                .hasMessage("El enlace para coordinar el retiro de barriles no es válido");
    }

    @Test
    @DisplayName("CP-RBS-03: un enlace vencido, de un cliente dado de baja o reemplazado por un recordatorio más nuevo no sirve")
    void buscar_enlaceQueNoSirve_rechazaConSuMotivo() {
        InvitacionRetiroBarrilesEntity vencida = invitacion(LocalDateTime.now().minusMinutes(1), null);
        conInvitacion(vencida);
        assertThatThrownBy(() -> retiroBarrilesServicio.buscarRetiro(TOKEN))
                .isInstanceOf(EnlaceRetiroInvalidoException.class).hasMessage("El enlace para coordinar el retiro de barriles venció");

        InvitacionRetiroBarrilesEntity deBaja = invitacion(LocalDateTime.now().plusDays(3), null);
        deBaja.getCliente().setEstado(Estado.BAJA);
        conInvitacion(deBaja);
        assertThatThrownBy(() -> retiroBarrilesServicio.buscarRetiro(TOKEN))
                .isInstanceOf(EnlaceRetiroInvalidoException.class).hasMessage("El enlace ya no está habilitado para coordinar el retiro de barriles");

        InvitacionRetiroBarrilesEntity reemplazada = invitacion(LocalDateTime.now().plusDays(3), null);
        conInvitacion(reemplazada);
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(true);
        assertThatThrownBy(() -> retiroBarrilesServicio.buscarRetiro(TOKEN))
                .isInstanceOf(EnlaceRetiroInvalidoException.class)
                .hasMessage("El enlace fue reemplazado por uno más reciente: use el enlace del último correo recibido");
    }

    // ==================== buscarRetiro ====================

    @Test
    @DisplayName("CP-RBS-04: buscarRetiro muestra solo los barriles que siguen pendientes de retiro, por fecha de devolución, con el plazo máximo")
    void buscar_sinResponder_muestraPendientes() {
        // === PREPARACION DE DATOS ===
        DespachoBarrilEntity reciente = despacho(31L, 202L, "SN-1002", "BRL-02", LocalDate.of(2026, 11, 15));
        DespachoBarrilEntity antiguo = despacho(30L, 201L, "SN-1001", "BRL-01", LocalDate.of(2026, 11, 10));
        DespachoBarrilEntity devuelto = despacho(32L, 203L, "SN-1003", "BRL-03", LocalDate.of(2026, 11, 12));
        InvitacionRetiroBarrilesEntity invitacion = invitacion(LocalDateTime.now().plusDays(3), null, reciente, antiguo, devuelto);
        conInvitacion(invitacion);
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);
        when(despachoBarrilRepository.estaPendienteDeRetiro(30L)).thenReturn(true);
        when(despachoBarrilRepository.estaPendienteDeRetiro(31L)).thenReturn(true);
        when(despachoBarrilRepository.estaPendienteDeRetiro(32L)).thenReturn(false);

        // === EJECUCION ===
        RetiroBarrilesResponseDTO resultado = retiroBarrilesServicio.buscarRetiro(TOKEN);

        // === ASSERTS ===
        assertThat(resultado.isRegistrado()).isFalse();
        assertThat(resultado.getNombreCliente()).isEqualTo("Bar Don Jorge");
        assertThat(resultado.getBarrilesPendientes()).extracting("numeroSerie").containsExactly("SN-1001", "SN-1002");
        assertThat(resultado.getDiasMaximosAnticipacionRetiro()).isEqualTo(7);
        assertThat(resultado.getFechaMaximaRetiro()).isCloseTo(LocalDateTime.now().plusDays(7), within(5, ChronoUnit.SECONDS));
        assertThat(resultado.getBarrilesRegistrados()).isEmpty();
    }

    @Test
    @DisplayName("CP-RBS-05: buscarRetiro de un enlace respondido muestra el retiro registrado aunque haya un recordatorio más nuevo")
    void buscar_respondido_muestraLoRegistrado() {
        // === PREPARACION DE DATOS ===
        LocalDateTime respuesta = LocalDateTime.now().minusDays(1);
        LocalDateTime fechaRetiro = LocalDateTime.now().plusDays(2);
        DespachoBarrilEntity despacho = despacho(30L, 201L, "SN-1001", "BRL-01", LocalDate.of(2026, 11, 10));
        InvitacionRetiroBarrilesEntity invitacion = invitacion(LocalDateTime.now().plusDays(3), respuesta, despacho);
        conInvitacion(invitacion);
        conConfiguracion(7);
        SolicitudBusquedaEntity solicitud = SolicitudBusquedaEntity.builder().id(70L).despachoBarril(despacho).fechaBusqueda(fechaRetiro)
                .observaciones("Tocar timbre").estado(EstadoSolicitudBusqueda.BUSCADA).build();
        when(solicitudBusquedaRepository.buscarPorInvitacionRetiro(90L)).thenReturn(List.of(solicitud));

        // === EJECUCION ===
        RetiroBarrilesResponseDTO resultado = retiroBarrilesServicio.buscarRetiro(TOKEN);

        // === ASSERTS ===
        assertThat(resultado.isRegistrado()).isTrue();
        assertThat(resultado.getFechaRespuesta()).isEqualTo(respuesta);
        assertThat(resultado.getFechaRetiro()).isEqualTo(fechaRetiro);
        assertThat(resultado.getObservaciones()).isEqualTo("Tocar timbre");
        assertThat(resultado.getBarrilesRegistrados()).singleElement().satisfies(b -> {
            assertThat(b.getNumeroSerie()).isEqualTo("SN-1001");
            assertThat(b.getEstado()).isEqualTo(EstadoSolicitudBusqueda.BUSCADA);
        });
        assertThat(resultado.getBarrilesPendientes()).isEmpty();
        verify(invitacionRepository, never()).existePosteriorEnviada(any(), any(), any());
    }

    // ==================== registrarRetiro ====================

    @Test
    @DisplayName("CP-RBS-06: registrarRetiro rechaza un enlace ya respondido sin registrar nada")
    void registrar_yaRespondido_rechaza() {
        conInvitacionBloqueada(invitacion(LocalDateTime.now().plusDays(3), LocalDateTime.now().minusHours(1)));

        assertThatThrownBy(() -> retiroBarrilesServicio.registrarRetiro(TOKEN, form(LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El retiro ya fue coordinado desde este enlace y no se puede modificar");
        verifyNoInteractions(solicitudBusquedaRepository, barrilRepository, notificador);
    }

    @Test
    @DisplayName("CP-RBS-07: registrarRetiro vuelve a validar el enlace con el recordatorio bloqueado")
    void registrar_enlaceReemplazado_rechaza() {
        conInvitacionBloqueada(invitacion(LocalDateTime.now().plusDays(3), null));
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(true);

        assertThatThrownBy(() -> retiroBarrilesServicio.registrarRetiro(TOKEN, form(LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(EnlaceRetiroInvalidoException.class);
        verify(invitacionRepository, never()).findById(any());
        verifyNoInteractions(solicitudBusquedaRepository, notificador);
    }

    @Test
    @DisplayName("CP-RBS-08: registrarRetiro rechaza los datos nulos, la fecha faltante, pasada o posterior a los días máximos y las observaciones largas")
    void registrar_datosInvalidos_rechazaConSuCampo() {
        conInvitacionBloqueada(invitacion(LocalDateTime.now().plusDays(3), null));
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);

        assertThatThrownBy(() -> retiroBarrilesServicio.registrarRetiro(TOKEN, null))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("Los datos del retiro son obligatorios");
        verificarRechazo(form(null, null), "La fecha y hora del retiro es obligatoria", "fechaRetiro");
        verificarRechazo(form(LocalDateTime.now().minusMinutes(5), null), "La fecha y hora del retiro no puede ser anterior a la fecha y hora actual", "fechaRetiro");
        verificarRechazo(form(LocalDateTime.now().plusDays(7).plusMinutes(5), null), "La fecha y hora del retiro no puede superar 7 días desde la fecha y hora actual", "fechaRetiro");
        verificarRechazo(form(LocalDateTime.now().plusDays(1), "x".repeat(256)), "Las observaciones no pueden superar 255 caracteres", "observaciones");
        verifyNoInteractions(barrilRepository, solicitudBusquedaRepository, notificador);
    }

    @Test
    @DisplayName("CP-RBS-09: registrarRetiro rechaza el retiro si ninguno de los barriles sigue pendiente")
    void registrar_sinBarrilesPendientes_rechaza() {
        DespachoBarrilEntity despacho = despacho(30L, 201L, "SN-1001", "BRL-01", LocalDate.of(2026, 11, 10));
        conInvitacionBloqueada(invitacion(LocalDateTime.now().plusDays(3), null, despacho));
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(201L)).thenReturn(Optional.of(despacho.getBarril()));
        when(despachoBarrilRepository.estaPendienteDeRetiro(30L)).thenReturn(false);

        assertThatThrownBy(() -> retiroBarrilesServicio.registrarRetiro(TOKEN, form(LocalDateTime.now().plusDays(1), null)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ninguno de los barriles sigue pendiente de retiro: ya fueron devueltos o su retiro ya está coordinado");
        verify(solicitudBusquedaRepository, never()).saveAll(anyList());
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("CP-RBS-10: registrarRetiro bloquea los barriles en orden de ID, registra una solicitud por barril pendiente, deja respondido el recordatorio y avisa a los encargados")
    void registrar_caminoFeliz() {
        // === PREPARACION DE DATOS ===
        DespachoBarrilEntity segundo = despacho(31L, 202L, "SN-1002", "BRL-02", LocalDate.of(2026, 11, 15));
        DespachoBarrilEntity primero = despacho(30L, 201L, "SN-1001", "BRL-01", LocalDate.of(2026, 11, 10));
        DespachoBarrilEntity devuelto = despacho(32L, 203L, "SN-1003", "BRL-03", LocalDate.of(2026, 11, 12));
        LocalDateTime expiracion = LocalDateTime.now().plusDays(3);
        InvitacionRetiroBarrilesEntity invitacion = invitacion(expiracion, null, segundo, devuelto, primero);
        conInvitacionBloqueada(invitacion);
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);
        for (DespachoBarrilEntity d : List.of(primero, segundo, devuelto)) {
            when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(d.getBarril().getId())).thenReturn(Optional.of(d.getBarril()));
        }
        when(despachoBarrilRepository.estaPendienteDeRetiro(30L)).thenReturn(true);
        when(despachoBarrilRepository.estaPendienteDeRetiro(31L)).thenReturn(true);
        when(despachoBarrilRepository.estaPendienteDeRetiro(32L)).thenReturn(false);
        LocalDateTime fechaRetiro = LocalDateTime.now().plusDays(5).withSecond(0).withNano(0);

        // === EJECUCION ===
        RetiroBarrilesResponseDTO resultado = retiroBarrilesServicio.registrarRetiro(TOKEN, form(fechaRetiro, "  Tocar   timbre  "));

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(201L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(202L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(203L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SolicitudBusquedaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(solicitudBusquedaRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2).allSatisfy(s -> {
            assertThat(s.getFechaBusqueda()).isEqualTo(fechaRetiro);
            assertThat(s.getObservaciones()).isEqualTo("Tocar timbre");
            assertThat(s.getEstado()).isEqualTo(EstadoSolicitudBusqueda.PENDIENTE);
            assertThat(s.getInvitacionRetiro()).isSameAs(invitacion);
        });
        assertThat(captor.getValue()).extracting(SolicitudBusquedaEntity::getDespachoBarril).containsExactly(primero, segundo);

        assertThat(invitacion.getFechaRespuesta()).isNotNull();
        assertThat(invitacion.getFechaExpiracion()).isEqualTo(fechaRetiro);
        verify(invitacionRepository).save(invitacion);
        verify(notificador).avisarEncargadosDeDistribucion(eq("Bar Don Jorge"),
                eq(List.of("N.º de serie SN-1001 (BRL-01)", "N.º de serie SN-1002 (BRL-02)")), eq(fechaRetiro), eq("Tocar timbre"));

        assertThat(resultado.isRegistrado()).isTrue();
        assertThat(resultado.getBarrilesRegistrados()).extracting("numeroSerie").containsExactly("SN-1001", "SN-1002");
    }

    @Test
    @DisplayName("CP-RBS-11: registrarRetiro conserva la expiración del enlace si es posterior a la fecha del retiro y guarda observaciones en blanco como nulas")
    void registrar_expiracionPosterior_seConserva() {
        DespachoBarrilEntity despacho = despacho(30L, 201L, "SN-1001", "BRL-01", LocalDate.of(2026, 11, 10));
        LocalDateTime expiracion = LocalDateTime.now().plusDays(6);
        InvitacionRetiroBarrilesEntity invitacion = invitacion(expiracion, null, despacho);
        conInvitacionBloqueada(invitacion);
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(201L)).thenReturn(Optional.of(despacho.getBarril()));
        when(despachoBarrilRepository.estaPendienteDeRetiro(30L)).thenReturn(true);

        RetiroBarrilesResponseDTO resultado = retiroBarrilesServicio.registrarRetiro(TOKEN, form(LocalDateTime.now().plusDays(1), "   "));

        assertThat(invitacion.getFechaExpiracion()).isEqualTo(expiracion);
        assertThat(resultado.getObservaciones()).isNull();
    }

    @Test
    @DisplayName("CP-RBS-12: un recordatorio posterior solo invalida el enlace si se envió o está en envío desde hace menos de 15 minutos")
    void buscar_recordatorioPosteriorTrabado_noInvalida() {
        conInvitacion(invitacion(LocalDateTime.now().plusDays(3), null));
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);

        RetiroBarrilesResponseDTO resultado = retiroBarrilesServicio.buscarRetiro(TOKEN);

        assertThat(resultado.isRegistrado()).isFalse();
        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(invitacionRepository).existePosteriorEnviada(eq(5L), eq(90L), limite.capture());
        assertThat(limite.getValue()).isCloseTo(LocalDateTime.now().minusMinutes(15), within(5, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("CP-RBS-13: el aviso a los encargados se envía recién al confirmarse la transacción; si se revierte después de guardar las solicitudes, no se envía")
    void registrar_avisoSoloAlConfirmar() {
        // === PREPARACION DE DATOS ===
        DespachoBarrilEntity despacho = despacho(30L, 201L, "SN-1001", "BRL-01", LocalDate.of(2026, 11, 10));
        conInvitacionBloqueada(invitacion(LocalDateTime.now().plusDays(3), null, despacho));
        when(invitacionRepository.existePosteriorEnviada(eq(5L), eq(90L), any())).thenReturn(false);
        conConfiguracion(7);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(201L)).thenReturn(Optional.of(despacho.getBarril()));
        when(despachoBarrilRepository.estaPendienteDeRetiro(30L)).thenReturn(true);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            // === EJECUCION: se registra con una transacción activa, que luego se revierte ===
            retiroBarrilesServicio.registrarRetiro(TOKEN, form(LocalDateTime.now().plusDays(1), null));
            verify(solicitudBusquedaRepository).saveAll(anyList());
            List<org.springframework.transaction.support.TransactionSynchronization> sincronizaciones =
                    org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            sincronizaciones.forEach(s -> s.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

            // === ASSERTS ===
            verifyNoInteractions(notificador);

            // Con la transacción confirmada, el aviso sí se envía
            sincronizaciones.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(notificador).avisarEncargadosDeDistribucion(eq("Bar Don Jorge"), eq(List.of("N.º de serie SN-1001 (BRL-01)")), any(), isNull());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ==================== helpers ====================

    private void verificarRechazo(RetiroBarrilesFormDTO formDTO, String mensaje, String campo) {
        assertThatThrownBy(() -> retiroBarrilesServicio.registrarRetiro(TOKEN, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje)
                .extracting("campo").isEqualTo(campo);
    }

    private void conInvitacion(InvitacionRetiroBarrilesEntity invitacion) {
        when(invitacionRepository.buscarIdPorTokenHash(HASH)).thenReturn(Optional.of(invitacion.getId()));
        when(invitacionRepository.findById(invitacion.getId())).thenReturn(Optional.of(invitacion));
    }

    private void conInvitacionBloqueada(InvitacionRetiroBarrilesEntity invitacion) {
        when(invitacionRepository.buscarIdPorTokenHash(HASH)).thenReturn(Optional.of(invitacion.getId()));
        when(invitacionRepository.buscarPorIdParaActualizar(invitacion.getId())).thenReturn(Optional.of(invitacion));
    }

    private void conConfiguracion(int dias) {
        when(configuracionDistribucionRepository.findById(ConfiguracionDistribucionEntity.SINGLETON_ID)).thenReturn(Optional.of(
                ConfiguracionDistribucionEntity.builder().id(1L).horaEnvioRecordatorioRetiro(LocalTime.of(9, 0)).diasMaximosAnticipacionRetiro(dias).build()));
    }

    private static RetiroBarrilesFormDTO form(LocalDateTime fecha, String observaciones) {
        return RetiroBarrilesFormDTO.builder().fechaRetiro(fecha).observaciones(observaciones).build();
    }

    private static InvitacionRetiroBarrilesEntity invitacion(LocalDateTime expiracion, LocalDateTime respuesta, DespachoBarrilEntity... despachos) {
        ClienteEntity cliente = ClienteEntity.builder().id(5L).nombre("Bar Don Jorge").email("compras@donjorge.com").estado(Estado.ACTIVO).build();
        return InvitacionRetiroBarrilesEntity.builder().id(90L).cliente(cliente).fechaInvitacion(LocalDate.now())
                .tokenHash(HASH).fechaExpiracion(expiracion).fechaRespuesta(respuesta).estadoEnvio(EstadoEnvioInvitacion.ENVIADA)
                .intentosEnvio(1).despachos(new ArrayList<>(List.of(despachos))).build();
    }

    private static DespachoBarrilEntity despacho(Long id, Long idBarril, String numeroSerie, String identificador, LocalDate devolucion) {
        BarrilEntity barril = BarrilEntity.builder().id(idBarril).numeroSerie(numeroSerie).identificadorInterno(identificador).estado(Estado.ACTIVO).build();
        return DespachoBarrilEntity.builder().id(id).barril(barril).estado(EstadoTransaccion.REGISTRADO).fechaDevolucionEstimada(devolucion).build();
    }
}
