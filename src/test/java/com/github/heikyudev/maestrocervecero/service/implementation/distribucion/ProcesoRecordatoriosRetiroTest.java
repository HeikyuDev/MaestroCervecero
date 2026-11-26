package com.github.heikyudev.maestrocervecero.service.implementation.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.correo.ICorreoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.EnvioRecordatorioRetiroPreparado;
import com.github.heikyudev.maestrocervecero.service.interfaces.distribucion.IRecordatorioRetiroServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proceso de los recordatorios de retiro: el envío de un recordatorio en tres pasos, el proceso programado y los correos.
 */
class ProcesoRecordatoriosRetiroTest {

    private static final String TOKEN = "kV2s9fJ1x0QmB7r3TzYwLp4cHn8aUe6dGqXo5iSjNkA";
    private static final LocalDateTime EXPIRACION = LocalDateTime.of(2026, 11, 26, 9, 0);

    @Nested
    @ExtendWith(MockitoExtension.class)
    class EnvioDeUnRecordatorio {

        @Mock
        private IRecordatorioRetiroServicio recordatorioRetiroServicio;
        @Mock
        private NotificadorRetiroBarriles notificador;

        private EnvioRecordatoriosRetiro envio() {
            return new EnvioRecordatoriosRetiro(recordatorioRetiroServicio, notificador);
        }

        @Test
        @DisplayName("CP-PRR-01: el envío prepara el recordatorio, envía el correo y registra que el servidor lo aceptó, en ese orden, e informa que se notificó al cliente")
        void envia_yRegistraElResultado() {
            LocalDate hoy = LocalDate.of(2026, 11, 19);
            EnvioRecordatorioRetiroPreparado preparado = new EnvioRecordatorioRetiroPreparado(90L, "compras@donjorge.com", "Bar Don Jorge",
                    TOKEN, EXPIRACION, List.of("N.º de serie SN-1001 (BRL-01)"));
            when(recordatorioRetiroServicio.prepararRecordatorio(5L, hoy)).thenReturn(Optional.of(preparado));
            when(notificador.recordarRetiro("compras@donjorge.com", "Bar Don Jorge", TOKEN, EXPIRACION, List.of("N.º de serie SN-1001 (BRL-01)"))).thenReturn(true);

            assertThat(envio().enviar(5L, hoy)).isTrue();

            InOrder orden = inOrder(recordatorioRetiroServicio, notificador);
            orden.verify(recordatorioRetiroServicio).prepararRecordatorio(5L, hoy);
            orden.verify(notificador).recordarRetiro(any(), any(), any(), any(), any());
            orden.verify(recordatorioRetiroServicio).registrarResultadoEnvio(90L, true);
        }

        @Test
        @DisplayName("CP-PRR-02: si el correo falla o lanza una excepción, el envío queda registrado como fallido y no cuenta como notificado")
        void falla_registraFallido() {
            LocalDate hoy = LocalDate.of(2026, 11, 19);
            EnvioRecordatorioRetiroPreparado preparado = new EnvioRecordatorioRetiroPreparado(90L, "compras@donjorge.com", "Bar Don Jorge",
                    TOKEN, EXPIRACION, List.of());
            when(recordatorioRetiroServicio.prepararRecordatorio(5L, hoy)).thenReturn(Optional.of(preparado));
            when(notificador.recordarRetiro(any(), any(), any(), any(), any())).thenReturn(false).thenThrow(new IllegalStateException("SMTP caído"));

            assertThat(envio().enviar(5L, hoy)).isFalse();
            assertThat(envio().enviar(5L, hoy)).isFalse();

            verify(recordatorioRetiroServicio, times(2)).registrarResultadoEnvio(90L, false);
        }

        @Test
        @DisplayName("CP-PRR-03: si no corresponde enviar el recordatorio no se envía ningún correo ni se registra resultado")
        void sinPreparado_noEnvia() {
            LocalDate hoy = LocalDate.of(2026, 11, 19);
            when(recordatorioRetiroServicio.prepararRecordatorio(5L, hoy)).thenReturn(Optional.empty());

            assertThat(envio().enviar(5L, hoy)).isFalse();

            verify(notificador, never()).recordarRetiro(any(), any(), any(), any(), any());
            verify(recordatorioRetiroServicio, never()).registrarResultadoEnvio(any(), anyBoolean());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ProcesoProgramado {

        @Mock
        private IRecordatorioRetiroServicio recordatorioRetiroServicio;
        @Mock
        private EnvioRecordatoriosRetiro envioRecordatorios;

        private EnvioRecordatoriosRetiroProgramado proceso() {
            return new EnvioRecordatoriosRetiroProgramado(recordatorioRetiroServicio, envioRecordatorios);
        }

        @Test
        @DisplayName("CP-PRR-04: antes de la hora de envío el proceso no hace nada")
        void antesDeLaHora_noHaceNada() {
            when(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(any())).thenReturn(false);

            proceso().procesarRecordatorios();

            verify(recordatorioRetiroServicio, never()).buscarIdsClientesAInvitar(any());
            verify(recordatorioRetiroServicio, never()).buscarIdsClientesConEnviosPendientes(any());
            verify(envioRecordatorios, never()).enviar(any(), any());
        }

        @Test
        @DisplayName("CP-PRR-05: desde la hora, reintenta los pendientes, envía a cada cliente por separado y, sin fallas, registra la ejecución del día")
        void primeraCorridaDelDia_enviaYRegistra() {
            LocalDate hoy = LocalDate.now();
            when(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(any())).thenReturn(true);
            when(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)).thenReturn(List.of(3L));
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)).thenReturn(false);
            when(recordatorioRetiroServicio.buscarIdsClientesAInvitar(hoy)).thenReturn(List.of(5L, 8L));

            proceso().procesarRecordatorios();

            InOrder orden = inOrder(envioRecordatorios, recordatorioRetiroServicio);
            orden.verify(envioRecordatorios).enviar(3L, hoy);
            orden.verify(envioRecordatorios).enviar(5L, hoy);
            orden.verify(envioRecordatorios).enviar(8L, hoy);
            orden.verify(recordatorioRetiroServicio).registrarEjecucion(hoy);
        }

        @Test
        @DisplayName("CP-PRR-09: si el envío a un cliente falla, sigue con los demás pero deja el día abierto para reintentarlo en la próxima corrida")
        void clienteQueFalla_noRegistraLaEjecucion() {
            LocalDate hoy = LocalDate.now();
            when(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(any())).thenReturn(true);
            when(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)).thenReturn(List.of());
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)).thenReturn(false);
            when(recordatorioRetiroServicio.buscarIdsClientesAInvitar(hoy)).thenReturn(List.of(5L, 8L));
            lenient().doThrow(new IllegalStateException("Falla puntual")).when(envioRecordatorios).enviar(5L, hoy);

            proceso().procesarRecordatorios();

            verify(envioRecordatorios).enviar(8L, hoy);
            verify(recordatorioRetiroServicio, never()).registrarEjecucion(any());
        }

        @Test
        @DisplayName("CP-PRR-10: una corrida que empieza justo antes de la medianoche usa el mismo momento para la hora de envío y para el día de todo el envío")
        void cruceDeMedianoche_usaUnSoloMomento() {
            java.time.ZoneId zona = java.time.ZoneId.of("America/Argentina/Buenos_Aires");
            LocalDateTime casiMedianoche = LocalDateTime.of(2026, 11, 19, 23, 59, 59, 999_000_000);
            java.time.Clock reloj = java.time.Clock.fixed(casiMedianoche.atZone(zona).toInstant(), zona);
            LocalDate dia = LocalDate.of(2026, 11, 19);
            when(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(casiMedianoche)).thenReturn(true);
            when(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(dia)).thenReturn(List.of());
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(dia)).thenReturn(false);
            when(recordatorioRetiroServicio.buscarIdsClientesAInvitar(dia)).thenReturn(List.of(5L));

            new EnvioRecordatoriosRetiroProgramado(recordatorioRetiroServicio, envioRecordatorios, reloj).procesarRecordatorios();

            verify(envioRecordatorios).enviar(5L, dia);
            verify(recordatorioRetiroServicio).registrarEjecucion(dia);
            verify(recordatorioRetiroServicio, never()).buscarIdsClientesAInvitar(dia.plusDays(1));
            verify(recordatorioRetiroServicio, never()).registrarEjecucion(dia.plusDays(1));
        }

        @Test
        @DisplayName("CP-PRR-06: si el día ya se envió, solo reintenta los fallidos y no vuelve a invitar ni a registrar la ejecución")
        void diaYaEnviado_soloReintenta() {
            LocalDate hoy = LocalDate.now();
            when(recordatorioRetiroServicio.alcanzoLaHoraDeEnvio(any())).thenReturn(true);
            when(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)).thenReturn(List.of(3L));
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)).thenReturn(true);

            proceso().procesarRecordatorios();

            verify(envioRecordatorios).enviar(3L, hoy);
            verify(recordatorioRetiroServicio, never()).buscarIdsClientesAInvitar(any());
            verify(recordatorioRetiroServicio, never()).registrarEjecucion(any());
        }

        @Test
        @DisplayName("CP-PRR-11: el envío pedido a mano no espera la hora de envío: envía el día, lo registra y cuenta los clientes notificados")
        void envioAMano_noEsperaLaHora() {
            LocalDate hoy = LocalDate.now();
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)).thenReturn(false);
            when(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)).thenReturn(List.of());
            when(recordatorioRetiroServicio.buscarIdsClientesAInvitar(hoy)).thenReturn(List.of(5L, 8L, 9L));
            when(envioRecordatorios.enviar(5L, hoy)).thenReturn(true);
            when(envioRecordatorios.enviar(8L, hoy)).thenReturn(false);
            when(envioRecordatorios.enviar(9L, hoy)).thenReturn(true);

            EnvioRecordatoriosRetiroProgramado.ResultadoEnvio resultado = proceso().enviarRecordatoriosDeHoy();

            assertThat(resultado.clientesNotificados()).isEqualTo(2);
            assertThat(resultado.completo()).isTrue();
            verify(recordatorioRetiroServicio, never()).alcanzoLaHoraDeEnvio(any());
            verify(recordatorioRetiroServicio).registrarEjecucion(hoy);
        }

        @Test
        @DisplayName("CP-PRR-12: el envío pedido a mano se rechaza si los recordatorios de hoy ya se enviaron, sin enviar nada")
        void envioAMano_diaYaEnviado_seRechaza() {
            LocalDate hoy = LocalDate.now();
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)).thenReturn(true);

            org.assertj.core.api.Assertions.assertThatThrownBy(() -> proceso().enviarRecordatoriosDeHoy())
                    .isInstanceOf(com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException.class)
                    .hasMessage("Los recordatorios de retiro de hoy ya se enviaron: los que fallaron se reintentan automáticamente");

            verify(envioRecordatorios, never()).enviar(any(), any());
            verify(recordatorioRetiroServicio, never()).registrarEjecucion(any());
        }

        @Test
        @DisplayName("CP-PRR-13: si en el envío pedido a mano un cliente falla, informa el envío incompleto y no registra el día")
        void envioAMano_conFalla_quedaIncompleto() {
            LocalDate hoy = LocalDate.now();
            when(recordatorioRetiroServicio.seEnviaronLosRecordatorios(hoy)).thenReturn(false);
            when(recordatorioRetiroServicio.buscarIdsClientesConEnviosPendientes(hoy)).thenReturn(List.of(3L));
            when(recordatorioRetiroServicio.buscarIdsClientesAInvitar(hoy)).thenReturn(List.of(5L));
            when(envioRecordatorios.enviar(3L, hoy)).thenReturn(true);
            doThrow(new IllegalStateException("Falla puntual")).when(envioRecordatorios).enviar(5L, hoy);

            EnvioRecordatoriosRetiroProgramado.ResultadoEnvio resultado = proceso().enviarRecordatoriosDeHoy();

            assertThat(resultado.clientesNotificados()).isEqualTo(1);
            assertThat(resultado.completo()).isFalse();
            verify(recordatorioRetiroServicio, never()).registrarEjecucion(any());
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class Correos {

        @Mock
        private ICorreoServicio correoServicio;
        @Mock
        private IUsuarioRepository usuarioRepository;

        private NotificadorRetiroBarriles notificador() {
            return new NotificadorRetiroBarriles(correoServicio, usuarioRepository, "http://localhost:7777");
        }

        @Test
        @DisplayName("CP-PRR-07: el recordatorio al cliente usa su plantilla, el enlace con el token y la vigencia, e informa si el servidor lo aceptó")
        @SuppressWarnings("unchecked")
        void recordatorio_armaElCorreo() {
            when(correoServicio.intentarEnviarCorreoHtml(eq("compras@donjorge.com"), anyString(), anyString(), any())).thenReturn(true);

            boolean aceptado = notificador().recordarRetiro("compras@donjorge.com", "Bar Don Jorge", TOKEN, EXPIRACION, List.of("N.º de serie SN-1001 (BRL-01)"));

            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(correoServicio).intentarEnviarCorreoHtml(eq("compras@donjorge.com"), eq("Coordinación del retiro de barriles de Maestro Cervecero"),
                    eq("correo/recordatorio-retiro-barriles"), variables.capture());
            assertThat(aceptado).isTrue();
            assertThat(variables.getValue())
                    .containsEntry("nombreCliente", "Bar Don Jorge")
                    .containsEntry("enlace", "http://localhost:7777/retiro-barriles/" + TOKEN)
                    .containsEntry("fechaExpiracion", "26/11/2026 09:00")
                    .containsEntry("barriles", List.of("N.º de serie SN-1001 (BRL-01)"));
        }

        @Test
        @DisplayName("CP-PRR-08: el aviso llega a cada encargado de distribución activo con el cliente, los barriles, la fecha propuesta y las observaciones")
        @SuppressWarnings("unchecked")
        void aviso_aCadaEncargadoActivo() {
            UsuarioEntity ana = mock(UsuarioEntity.class);
            when(ana.getNombre()).thenReturn("Ana");
            when(ana.getCorreo()).thenReturn("ana@maestro.com");
            UsuarioEntity luis = mock(UsuarioEntity.class);
            when(luis.getNombre()).thenReturn("Luis");
            when(luis.getCorreo()).thenReturn("luis@maestro.com");
            when(usuarioRepository.findByRolAndEstado(Rol.ENCARGADO_DE_DISTRIBUCION, Estado.ACTIVO)).thenReturn(List.of(ana, luis));

            notificador().avisarEncargadosDeDistribucion("Bar Don Jorge", List.of("N.º de serie SN-1001 (BRL-01)"),
                    LocalDateTime.of(2026, 11, 25, 10, 30), "Tocar timbre");

            ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
            verify(correoServicio).enviarCorreoHtml(eq("ana@maestro.com"), eq("Retiro de barriles coordinado por un cliente"),
                    eq("correo/aviso-solicitud-retiro"), variables.capture());
            verify(correoServicio).enviarCorreoHtml(eq("luis@maestro.com"), any(), any(), any());
            assertThat(variables.getValue())
                    .containsEntry("nombre", "Ana")
                    .containsEntry("nombreCliente", "Bar Don Jorge")
                    .containsEntry("fechaRetiro", "25/11/2026 10:30")
                    .containsEntry("observaciones", "Tocar timbre")
                    .containsEntry("enlace", "http://localhost:7777/solicitudes-retiro");
        }
    }
}
