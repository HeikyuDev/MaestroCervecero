package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
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
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IRestablecimientoContrasenaServicio;
import com.github.heikyudev.maestrocervecero.util.RequestUtils;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación de {@link IRestablecimientoContrasenaServicio}.
 * <p>
 * El token del enlace son {@value #BYTES_TOKEN} bytes aleatorios de un generador criptográficamente seguro, codificados
 * en Base64URL sin relleno. Solo se guarda su hash SHA-256: el token en sí viaja únicamente en el enlace del correo.
 * </p>
 * <p>
 * La solicitud no informa si el nombre de usuario existe: corre entera en un hilo aparte (@Async), no lanza excepciones
 * por un usuario inexistente y el correo se envía a su vez en otro hilo ({@link ICorreoServicio}), de modo que tampoco el
 * tiempo de respuesta lo revela. El correo y la bitácora se despachan recién cuando la transacción se confirma: si el
 * enlace no llega a guardarse, no se envía.
 * </p>
 * <p>
 * El restablecimiento reclama el enlace con una única sentencia condicional antes de tocar la contraseña: de dos pedidos
 * simultáneos con el mismo enlace, solo uno lo usa. Confirmada la transacción, se cierran las sesiones del usuario.
 * </p>
 */
@Service
public class RestablecimientoContrasenaServicioImpl implements IRestablecimientoContrasenaServicio {

    static final String PLANTILLA_CORREO = "correo/restablecer-contrasena";
    static final String ASUNTO_CORREO = "Restablecimiento de contraseña de Maestro Cervecero";
    static final String RUTA_ENLACE = "/restablecer-contrasena";

    private static final int BYTES_TOKEN = 32;

    // Un token Base64URL de 32 bytes tiene 43 caracteres: uno mucho más largo no corresponde a ningún enlace
    private static final int LONGITUD_MAXIMA_TOKEN = 128;

    private static final String MENSAJE_ENLACE_NO_VALIDO = "El enlace para restablecer la contraseña no es válido";
    private static final String MENSAJE_ENLACE_USADO = "El enlace para restablecer la contraseña ya fue utilizado";
    private static final String MENSAJE_ENLACE_VENCIDO = "El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo";

    private static final SecureRandom GENERADOR = new SecureRandom();
    private static final Base64.Encoder CODIFICADOR_TOKEN = Base64.getUrlEncoder().withoutPadding();

    private final IUsuarioRepository usuarioRepository;
    private final ITokenRestablecimientoContrasenaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ICorreoServicio correoServicio;
    private final IAuditLogService auditLogService;
    private final ICierreSesionesServicio cierreSesionesServicio;
    private final String urlBase;
    private final long minutosVigencia;
    private final long minutosEntreSolicitudes;

    public RestablecimientoContrasenaServicioImpl(IUsuarioRepository usuarioRepository,
                                                  ITokenRestablecimientoContrasenaRepository tokenRepository,
                                                  PasswordEncoder passwordEncoder,
                                                  ICorreoServicio correoServicio,
                                                  IAuditLogService auditLogService,
                                                  ICierreSesionesServicio cierreSesionesServicio,
                                                  @Value("${app.url-base}") String urlBase,
                                                  @Value("${restablecimiento-contrasena.minutos-vigencia}") long minutosVigencia,
                                                  @Value("${restablecimiento-contrasena.minutos-entre-solicitudes}") long minutosEntreSolicitudes) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoServicio = correoServicio;
        this.auditLogService = auditLogService;
        this.cierreSesionesServicio = cierreSesionesServicio;
        this.urlBase = urlBase;
        this.minutosVigencia = minutosVigencia;
        this.minutosEntreSolicitudes = minutosEntreSolicitudes;
    }

    @Override
    @Async
    @Transactional
    public void solicitarRestablecimiento(String username, String ipCliente) {
        String usernameNormalizado = NormalizacionTexto.username(username);
        if (usernameNormalizado == null || usernameNormalizado.isEmpty()) {
            return;
        }
        Optional<UsuarioEntity> usuarioEncontrado = usuarioRepository.findUserEntityByUsername(usernameNormalizado);
        if (usuarioEncontrado.isEmpty()) {
            return;
        }
        UsuarioEntity usuario = usuarioEncontrado.get();
        LocalDateTime ahora = LocalDateTime.now();

        // Un enlace enviado hace instantes no se repite: evita llenar la casilla del usuario con pedidos ajenos
        if (tokenRepository.existeEnviadoDespuesDe(usuario.getId(), ahora.minusMinutes(minutosEntreSolicitudes))) {
            return;
        }

        tokenRepository.vencerVigentesDelUsuario(usuario.getId(), ahora);
        String token = generarToken();
        tokenRepository.save(TokenRestablecimientoContrasenaEntity.builder()
                .usuario(usuario)
                .tokenHash(calcularHash(token))
                .fechaExpiracion(ahora.plusMinutes(minutosVigencia))
                .build());

        AuditLogEntity auditoria = armarAuditoria(usuario, AccionAuditoria.SOLICITUD_RESTABLECIMIENTO_CONTRASENA, ahora, ipCliente);
        Map<String, Object> variables = Map.of(
                "nombre", usuario.getNombre(),
                "enlace", armarEnlace(token),
                "minutosVigencia", minutosVigencia);
        String destinatario = usuario.getCorreo();
        ejecutarAlConfirmar(() -> {
            auditLogService.guardar(auditoria);
            correoServicio.enviarCorreoHtml(destinatario, ASUNTO_CORREO, PLANTILLA_CORREO, variables);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public void validarEnlace(String token) {
        buscarEnlaceVigente(token, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void restablecerContrasena(String token, RestablecimientoContrasenaFormDTO formDTO) {
        LocalDateTime ahora = LocalDateTime.now();
        TokenRestablecimientoContrasenaEntity enlace = buscarEnlaceVigente(token, ahora);

        String password = formDTO.getPassword();
        if (password == null || password.isEmpty()) {
            throw new ReglaNegocioException("La contraseña es obligatoria", "password");
        }
        try {
            MetodosPassword.validarPassword(password);
        } catch (ReglaNegocioException e) {
            throw new ReglaNegocioException(e.getMessage(), "password");
        }
        if (!password.equals(formDTO.getConfirmacionPassword())) {
            throw new ReglaNegocioException("La confirmación no coincide con la contraseña", "confirmacionPassword");
        }

        // El reclamo va antes que el cambio de contraseña: si otro pedido ya usó el enlace, aquí no se cambia nada. Una
        // falla posterior revierte la transacción y con ella el reclamo
        if (tokenRepository.reclamarParaUso(enlace.getId(), ahora) == 0) {
            boolean usado = tokenRepository.buscarFechaUso(enlace.getId()).isPresent();
            throw new EnlaceRestablecimientoInvalidoException(usado ? MENSAJE_ENLACE_USADO : MENSAJE_ENLACE_VENCIDO);
        }

        UsuarioEntity usuario = enlace.getUsuario();
        usuario.setPassword(passwordEncoder.encode(password));
        usuarioRepository.save(usuario);
        tokenRepository.vencerVigentesDelUsuario(usuario.getId(), ahora);

        AuditLogEntity auditoria = armarAuditoria(usuario, AccionAuditoria.RESTABLECIMIENTO_CONTRASENA, ahora,
                RequestUtils.obtenerIpCliente());
        String username = usuario.getUsername();
        ejecutarAlConfirmar(() -> {
            auditLogService.guardar(auditoria);
            cierreSesionesServicio.cerrarSesionesDe(username);
        });
    }

    /**
     * Busca el enlace del token y verifica que sirva en el momento indicado.
     *
     * @throws EnlaceRestablecimientoInvalidoException Si el token falta o es desconocido, si el enlace ya se usó, si
     *         venció o si su usuario ya no está activo.
     */
    private TokenRestablecimientoContrasenaEntity buscarEnlaceVigente(String token, LocalDateTime ahora) {
        if (token == null || token.isBlank() || token.length() > LONGITUD_MAXIMA_TOKEN) {
            throw new EnlaceRestablecimientoInvalidoException(MENSAJE_ENLACE_NO_VALIDO);
        }
        TokenRestablecimientoContrasenaEntity enlace = tokenRepository.buscarPorTokenHash(calcularHash(token))
                .orElseThrow(() -> new EnlaceRestablecimientoInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        if (enlace.getFechaUso() != null) {
            throw new EnlaceRestablecimientoInvalidoException(MENSAJE_ENLACE_USADO);
        }
        if (!enlace.getFechaExpiracion().isAfter(ahora)) {
            throw new EnlaceRestablecimientoInvalidoException(MENSAJE_ENLACE_VENCIDO);
        }
        if (enlace.getUsuario().getEstado() != Estado.ACTIVO) {
            throw new EnlaceRestablecimientoInvalidoException(MENSAJE_ENLACE_NO_VALIDO);
        }
        return enlace;
    }

    private static String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        GENERADOR.nextBytes(bytes);
        return CODIFICADOR_TOKEN.encodeToString(bytes);
    }

    private static String calcularHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Toda JVM está obligada a ofrecer SHA-256: no llega a ocurrir
            throw new IllegalStateException("La JVM no ofrece el algoritmo SHA-256", e);
        }
    }

    private String armarEnlace(String token) {
        return UriComponentsBuilder.fromUriString(urlBase)
                .path(RUTA_ENLACE)
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    // La IP llega resuelta desde el hilo de la request: la bitácora se guarda en un hilo aparte que ya no la tiene
    private static AuditLogEntity armarAuditoria(UsuarioEntity usuario, AccionAuditoria accion, LocalDateTime ahora,
                                                 String ipCliente) {
        return AuditLogEntity.builder()
                .username(usuario.getUsername())
                .accion(accion)
                .conceptoAuditoria(ConceptoAuditoria.SESION)
                .entidadId(String.valueOf(usuario.getId()))
                .fechaHora(ahora)
                .ipAddress(ipCliente)
                .build();
    }

    // Con una transacción en curso, la acción espera a que se confirme; sin transacción, se ejecuta en el momento
    private static void ejecutarAlConfirmar(Runnable accion) {
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
