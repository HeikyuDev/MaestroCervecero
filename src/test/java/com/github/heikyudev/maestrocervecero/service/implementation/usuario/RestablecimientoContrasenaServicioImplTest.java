package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.TokenRestablecimientoContrasenaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.ITokenRestablecimientoContrasenaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.RestablecimientoContrasenaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRestablecimientoInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.auditoria.IAuditLogService;
import com.github.heikyudev.maestrocervecero.service.interfaces.correo.ICorreoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.ICierreSesionesServicio;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestablecimientoContrasenaServicioImplTest {

    private static final String URL_BASE = "http://localhost:7777";
    private static final long MINUTOS_VIGENCIA = 30;
    private static final long MINUTOS_ENTRE_SOLICITUDES = 2;
    private static final String TOKEN = "Q2xhdmVEZVBydWViYVBhcmFFbFJlc3RhYmxlY2ltaWVudG8";
    private static final String PASSWORD_NUEVA = "NuevaClave123";
    private static final String IP = "10.0.0.1";

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private ITokenRestablecimientoContrasenaRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ICorreoServicio correoServicio;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private ICierreSesionesServicio cierreSesionesServicio;

    private RestablecimientoContrasenaServicioImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new RestablecimientoContrasenaServicioImpl(usuarioRepository, tokenRepository, passwordEncoder,
                correoServicio, auditLogService, cierreSesionesServicio, URL_BASE, MINUTOS_VIGENCIA, MINUTOS_ENTRE_SOLICITUDES);
    }

    // ==================== solicitarRestablecimiento ====================

    @Test
    @DisplayName("CP-SRC-01: solicitarRestablecimiento guarda solo el hash del token, con vencimiento a los 30 minutos, y envía el enlace al correo del usuario")
    @SuppressWarnings("unchecked")
    void solicitarRestablecimiento_debeGuardarElHashYEnviarElEnlace() throws Exception {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuario = crearUsuario(Estado.ACTIVO);
        when(usuarioRepository.findUserEntityByUsername("jperez")).thenReturn(Optional.of(usuario));
        when(tokenRepository.existeEnviadoDespuesDe(eq(1L), any(LocalDateTime.class))).thenReturn(false);

        // === EJECUCION ===
        LocalDateTime antes = LocalDateTime.now();
        servicio.solicitarRestablecimiento("jperez", IP);

        // === ASSERTS ===
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(correoServicio).enviarCorreoHtml(eq("juan@mail.com"), eq("Restablecimiento de contraseña de Maestro Cervecero"),
                eq("correo/restablecer-contrasena"), variables.capture());
        String enlace = (String) variables.getValue().get("enlace");
        assertThat(enlace).startsWith(URL_BASE + "/restablecer-contrasena?token=");
        String token = enlace.substring(enlace.indexOf("token=") + "token=".length());
        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(variables.getValue()).containsEntry("nombre", "Juan").containsEntry("minutosVigencia", MINUTOS_VIGENCIA);

        ArgumentCaptor<TokenRestablecimientoContrasenaEntity> guardado = ArgumentCaptor.forClass(TokenRestablecimientoContrasenaEntity.class);
        verify(tokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getUsuario()).isSameAs(usuario);
        assertThat(guardado.getValue().getTokenHash()).isEqualTo(sha256(token)).isNotEqualTo(token);
        assertThat(guardado.getValue().getFechaUso()).isNull();
        assertThat(guardado.getValue().getFechaExpiracion()).isCloseTo(antes.plusMinutes(MINUTOS_VIGENCIA), within(5, ChronoUnit.SECONDS));

        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(tokenRepository).existeEnviadoDespuesDe(eq(1L), desde.capture());
        assertThat(desde.getValue()).isCloseTo(antes.minusMinutes(MINUTOS_ENTRE_SOLICITUDES), within(5, ChronoUnit.SECONDS));
        verify(tokenRepository).vencerVigentesDelUsuario(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-SRC-02: solicitarRestablecimiento registra la solicitud en la bitácora con el usuario, el concepto Sesión y la IP recibida")
    void solicitarRestablecimiento_debeRegistrarLaSolicitudEnLaBitacora() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findUserEntityByUsername("jperez")).thenReturn(Optional.of(crearUsuario(Estado.ACTIVO)));

        // === EJECUCION ===
        servicio.solicitarRestablecimiento("jperez", IP);

        // === ASSERTS ===
        ArgumentCaptor<AuditLogEntity> auditoria = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogService).guardar(auditoria.capture());
        assertThat(auditoria.getValue().getUsername()).isEqualTo("jperez");
        assertThat(auditoria.getValue().getAccion()).isEqualTo(AccionAuditoria.SOLICITUD_RESTABLECIMIENTO_CONTRASENA);
        assertThat(auditoria.getValue().getConceptoAuditoria()).isEqualTo(ConceptoAuditoria.SESION);
        assertThat(auditoria.getValue().getEntidadId()).isEqualTo("1");
        assertThat(auditoria.getValue().getFechaHora()).isNotNull();
        assertThat(auditoria.getValue().getIpAddress()).isEqualTo(IP);
    }

    @Test
    @DisplayName("CP-SRC-03: solicitarRestablecimiento normaliza el nombre de usuario como al guardarlo antes de buscarlo")
    void solicitarRestablecimiento_debeNormalizarElNombreDeUsuario() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findUserEntityByUsername("jperez")).thenReturn(Optional.empty());

        // === EJECUCION ===
        servicio.solicitarRestablecimiento("  JPerez ", IP);

        // === ASSERTS ===
        verify(usuarioRepository).findUserEntityByUsername("jperez");
    }

    @Test
    @DisplayName("CP-SRC-04: solicitarRestablecimiento con un nombre de usuario inexistente no lanza excepción, no guarda nada, no audita y no envía correo")
    void solicitarRestablecimiento_usuarioInexistenteNoHaceNada() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findUserEntityByUsername("inexistente")).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> servicio.solicitarRestablecimiento("inexistente", IP)).doesNotThrowAnyException();
        verifyNoInteractions(tokenRepository, correoServicio, auditLogService);
    }

    @Test
    @DisplayName("CP-SRC-05: solicitarRestablecimiento con un nombre de usuario nulo o en blanco no consulta nada")
    void solicitarRestablecimiento_usernameNuloOEnBlancoNoConsulta() {
        // === EJECUCION ===
        servicio.solicitarRestablecimiento(null, IP);
        servicio.solicitarRestablecimiento("   ", IP);

        // === ASSERTS ===
        verifyNoInteractions(usuarioRepository, tokenRepository, correoServicio, auditLogService);
    }

    @Test
    @DisplayName("CP-SRC-06: solicitarRestablecimiento no hace nada si al usuario se le envió un enlace hace menos de 2 minutos")
    void solicitarRestablecimiento_enlaceRecienteNoEnviaOtro() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findUserEntityByUsername("jperez")).thenReturn(Optional.of(crearUsuario(Estado.ACTIVO)));
        when(tokenRepository.existeEnviadoDespuesDe(eq(1L), any(LocalDateTime.class))).thenReturn(true);

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> servicio.solicitarRestablecimiento("jperez", IP)).doesNotThrowAnyException();
        verify(tokenRepository, never()).vencerVigentesDelUsuario(anyLong(), any(LocalDateTime.class));
        verify(tokenRepository, never()).save(any(TokenRestablecimientoContrasenaEntity.class));
        verifyNoInteractions(correoServicio, auditLogService);
    }

    @Test
    @DisplayName("CP-SRC-07: dos solicitudes generan tokens distintos")
    void solicitarRestablecimiento_cadaSolicitudGeneraUnTokenDistinto() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findUserEntityByUsername("jperez")).thenReturn(Optional.of(crearUsuario(Estado.ACTIVO)));

        // === EJECUCION ===
        servicio.solicitarRestablecimiento("jperez", IP);
        servicio.solicitarRestablecimiento("jperez", IP);

        // === ASSERTS ===
        ArgumentCaptor<TokenRestablecimientoContrasenaEntity> guardados = ArgumentCaptor.forClass(TokenRestablecimientoContrasenaEntity.class);
        verify(tokenRepository, times(2)).save(guardados.capture());
        List<TokenRestablecimientoContrasenaEntity> tokens = guardados.getAllValues();
        assertThat(tokens.get(0).getTokenHash()).isNotEqualTo(tokens.get(1).getTokenHash());
    }

    @Test
    @DisplayName("CP-SRC-08: solicitarRestablecimiento se ejecuta en un hilo aparte (@Async), para que la request no espere su trabajo")
    void solicitarRestablecimiento_debeSerAsincrono() throws Exception {
        // === EJECUCION Y ASSERTS ===
        assertThat(RestablecimientoContrasenaServicioImpl.class.getMethod("solicitarRestablecimiento", String.class, String.class)
                .isAnnotationPresent(Async.class)).isTrue();
    }

    // ==================== validarEnlace ====================

    @Test
    @DisplayName("CP-VEC-01: validarEnlace acepta un enlace sin usar, sin vencer y de un usuario activo, buscándolo por el hash del token")
    void validarEnlace_enlaceVigenteNoLanzaExcepcion() throws Exception {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(sha256(TOKEN))).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> servicio.validarEnlace(TOKEN)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VEC-02: validarEnlace rechaza un token nulo, en blanco o demasiado largo sin consultar la base")
    void validarEnlace_tokenNuloEnBlancoOLargoEsInvalido() {
        // === EJECUCION Y ASSERTS ===
        for (String token : new String[]{null, "  ", "a".repeat(129)}) {
            assertThatThrownBy(() -> servicio.validarEnlace(token))
                    .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                    .hasMessage("El enlace para restablecer la contraseña no es válido");
        }
        verifyNoInteractions(tokenRepository);
    }

    @Test
    @DisplayName("CP-VEC-03: validarEnlace rechaza un token que no corresponde a ningún enlace")
    void validarEnlace_tokenDesconocidoEsInvalido() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.validarEnlace(TOKEN))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña no es válido");
    }

    @Test
    @DisplayName("CP-VEC-04: validarEnlace rechaza un enlace ya usado")
    void validarEnlace_enlaceUsadoEsInvalido() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString()))
                .thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10))));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.validarEnlace(TOKEN))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña ya fue utilizado");
    }

    @Test
    @DisplayName("CP-VEC-05: validarEnlace rechaza un enlace vencido o reemplazado por uno más nuevo")
    void validarEnlace_enlaceVencidoEsInvalido() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString()))
                .thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().minusSeconds(1))));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.validarEnlace(TOKEN))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo");
    }

    @Test
    @DisplayName("CP-VEC-06: validarEnlace rechaza el enlace de un usuario dado de baja")
    void validarEnlace_usuarioDeBajaEsInvalido() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString()))
                .thenReturn(Optional.of(crearEnlace(Estado.BAJA, null, LocalDateTime.now().plusMinutes(10))));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.validarEnlace(TOKEN))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña no es válido");
    }

    // ==================== restablecerContrasena ====================

    @Test
    @DisplayName("CP-RCC-01: restablecerContrasena reclama el enlace antes de guardar el hash de la contraseña nueva, vence los demás enlaces, audita y cierra las sesiones del usuario")
    void restablecerContrasena_debeReemplazarLaContrasenaYUsarElEnlace() throws Exception {
        // === PREPARACION DE DATOS ===
        TokenRestablecimientoContrasenaEntity enlace = crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.buscarPorTokenHash(sha256(TOKEN))).thenReturn(Optional.of(enlace));
        when(tokenRepository.reclamarParaUso(eq(10L), any(LocalDateTime.class))).thenReturn(1);
        when(passwordEncoder.encode(PASSWORD_NUEVA)).thenReturn("hashNuevo");

        // === EJECUCION ===
        servicio.restablecerContrasena(TOKEN, crearFormulario(PASSWORD_NUEVA, PASSWORD_NUEVA));

        // === ASSERTS ===
        assertThat(enlace.getUsuario().getPassword()).isEqualTo("hashNuevo");
        ArgumentCaptor<LocalDateTime> momentoReclamo = ArgumentCaptor.forClass(LocalDateTime.class);
        InOrder orden = inOrder(tokenRepository, passwordEncoder, usuarioRepository);
        orden.verify(tokenRepository).reclamarParaUso(eq(10L), momentoReclamo.capture());
        orden.verify(passwordEncoder).encode(PASSWORD_NUEVA);
        orden.verify(usuarioRepository).save(enlace.getUsuario());
        orden.verify(tokenRepository).vencerVigentesDelUsuario(eq(1L), any(LocalDateTime.class));
        assertThat(momentoReclamo.getValue()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
        verify(tokenRepository, never()).save(any(TokenRestablecimientoContrasenaEntity.class));
        verify(cierreSesionesServicio).cerrarSesionesDe("jperez");

        ArgumentCaptor<AuditLogEntity> auditoria = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogService).guardar(auditoria.capture());
        assertThat(auditoria.getValue().getUsername()).isEqualTo("jperez");
        assertThat(auditoria.getValue().getAccion()).isEqualTo(AccionAuditoria.RESTABLECIMIENTO_CONTRASENA);
        assertThat(auditoria.getValue().getConceptoAuditoria()).isEqualTo(ConceptoAuditoria.SESION);
        assertThat(auditoria.getValue().getEntidadId()).isEqualTo("1");
    }

    @Test
    @DisplayName("CP-RCC-02: restablecerContrasena con un enlace que no sirve lanza EnlaceRestablecimientoInvalidoException y no cambia la contraseña")
    void restablecerContrasena_enlaceInvalidoNoCambiaNada() {
        // === PREPARACION DE DATOS ===
        TokenRestablecimientoContrasenaEntity enlace = crearEnlace(Estado.ACTIVO, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(10));
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(enlace));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario(PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña ya fue utilizado");
        verifyNoInteractions(passwordEncoder, usuarioRepository, auditLogService, cierreSesionesServicio);
        verify(tokenRepository, never()).reclamarParaUso(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-RCC-03: restablecerContrasena rechaza una contraseña vacía indicando el campo password")
    void restablecerContrasena_passwordVaciaEsRechazada() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario("", "")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La contraseña es obligatoria")
                .extracting(e -> ((ReglaNegocioException) e).getCampo()).isEqualTo("password");
        verifyNoInteractions(passwordEncoder, usuarioRepository);
        verify(tokenRepository, never()).reclamarParaUso(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-RCC-04: restablecerContrasena rechaza una contraseña que no cumple las reglas de MetodosPassword indicando el campo password")
    void restablecerContrasena_passwordInvalidaEsRechazada() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));

        // === EJECUCION Y ASSERTS ===
        for (String password : new String[]{"corta", "con espacio1"}) {
            assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario(password, password)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage(MetodosPassword.MENSAJE_INVALIDA)
                    .extracting(e -> ((ReglaNegocioException) e).getCampo()).isEqualTo("password");
        }
        verifyNoInteractions(passwordEncoder, usuarioRepository);
        verify(tokenRepository, never()).reclamarParaUso(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-RCC-05: restablecerContrasena rechaza una contraseña de más de 72 bytes indicando el campo password")
    void restablecerContrasena_passwordDemasiadoLargaEnBytesEsRechazada() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));
        String password = "ñ".repeat(40);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario(password, password)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageStartingWith("La contraseña es demasiado larga")
                .extracting(e -> ((ReglaNegocioException) e).getCampo()).isEqualTo("password");
        verifyNoInteractions(passwordEncoder, usuarioRepository);
        verify(tokenRepository, never()).reclamarParaUso(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-RCC-06: restablecerContrasena rechaza una confirmación distinta de la contraseña indicando el campo confirmacionPassword")
    void restablecerContrasena_confirmacionDistintaEsRechazada() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario(PASSWORD_NUEVA, "OtraClave123")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La confirmación no coincide con la contraseña")
                .extracting(e -> ((ReglaNegocioException) e).getCampo()).isEqualTo("confirmacionPassword");
        verifyNoInteractions(passwordEncoder, usuarioRepository, auditLogService);
        verify(tokenRepository, never()).reclamarParaUso(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-RCC-07: si otro pedido usó el enlace entre la validación y el reclamo, lanza \"ya fue utilizado\" sin cambiar la contraseña")
    void restablecerContrasena_reclamoPerdidoPorUsoNoCambiaNada() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));
        when(tokenRepository.reclamarParaUso(eq(10L), any(LocalDateTime.class))).thenReturn(0);
        when(tokenRepository.buscarFechaUso(10L)).thenReturn(Optional.of(LocalDateTime.now()));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario(PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña ya fue utilizado");
        verifyNoInteractions(passwordEncoder, usuarioRepository, auditLogService, cierreSesionesServicio);
        verify(tokenRepository, never()).vencerVigentesDelUsuario(anyLong(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("CP-RCC-08: si el enlace venció o fue reemplazado entre la validación y el reclamo, lanza \"venció o fue reemplazado\" sin cambiar la contraseña")
    void restablecerContrasena_reclamoPerdidoPorVencimientoNoCambiaNada() {
        // === PREPARACION DE DATOS ===
        when(tokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(crearEnlace(Estado.ACTIVO, null, LocalDateTime.now().plusMinutes(10))));
        when(tokenRepository.reclamarParaUso(eq(10L), any(LocalDateTime.class))).thenReturn(0);
        when(tokenRepository.buscarFechaUso(10L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> servicio.restablecerContrasena(TOKEN, crearFormulario(PASSWORD_NUEVA, PASSWORD_NUEVA)))
                .isInstanceOf(EnlaceRestablecimientoInvalidoException.class)
                .hasMessage("El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo");
        verifyNoInteractions(passwordEncoder, usuarioRepository, auditLogService, cierreSesionesServicio);
    }

    // ==================== Auxiliares ====================

    private static UsuarioEntity crearUsuario(Estado estado) {
        return UsuarioEntity.builder()
                .id(1L)
                .username("jperez")
                .password("hashActual")
                .nombre("Juan")
                .apellido("Perez")
                .correo("juan@mail.com")
                .rol(Rol.OPERARIO_DE_PRODUCCION)
                .estado(estado)
                .build();
    }

    private static TokenRestablecimientoContrasenaEntity crearEnlace(Estado estadoUsuario, LocalDateTime fechaUso, LocalDateTime fechaExpiracion) {
        return TokenRestablecimientoContrasenaEntity.builder()
                .id(10L)
                .usuario(crearUsuario(estadoUsuario))
                .tokenHash("hash")
                .fechaUso(fechaUso)
                .fechaExpiracion(fechaExpiracion)
                .build();
    }

    private static RestablecimientoContrasenaFormDTO crearFormulario(String password, String confirmacion) {
        return RestablecimientoContrasenaFormDTO.builder().password(password).confirmacionPassword(confirmacion).build();
    }

    private static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }
}
