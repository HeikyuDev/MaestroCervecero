package com.github.heikyudev.maestrocervecero.service.implementation.correo;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoServicioImplTest {

    private static final String REMITENTE = "no-responder@maestrocervecero.com";
    private static final String NOMBRE_REMITENTE = "Maestro Cervecero";
    private static final String DESTINATARIO = "juan@mail.com";
    private static final String ASUNTO = "Asunto de prueba";
    private static final String PLANTILLA = "correo/restablecer-contrasena";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ITemplateEngine templateEngine;

    private CorreoServicioImpl correoServicio;

    @BeforeEach
    void setUp() {
        correoServicio = new CorreoServicioImpl(mailSender, templateEngine, REMITENTE, NOMBRE_REMITENTE);
    }

    @Test
    @DisplayName("CP-CO-01: enviarCorreoHtml completa la plantilla con las variables y envía un correo HTML con el remitente, el destinatario y el asunto indicados")
    void enviarCorreoHtml_debeEnviarLaPlantillaCompletadaComoHtml() throws Exception {
        // === PREPARACION DE DATOS ===
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(templateEngine.process(eq(PLANTILLA), any(Context.class))).thenReturn("<p>Hola Juan</p>");

        // === EJECUCION ===
        correoServicio.enviarCorreoHtml(DESTINATARIO, ASUNTO, PLANTILLA, Map.of("nombre", "Juan"));

        // === ASSERTS ===
        ArgumentCaptor<Context> contexto = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq(PLANTILLA), contexto.capture());
        assertThat(contexto.getValue().getVariable("nombre")).isEqualTo("Juan");

        ArgumentCaptor<MimeMessage> mensaje = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mensaje.capture());
        MimeMessage enviado = mensaje.getValue();
        InternetAddress remitente = (InternetAddress) enviado.getFrom()[0];
        assertThat(remitente.getAddress()).isEqualTo(REMITENTE);
        assertThat(remitente.getPersonal()).isEqualTo(NOMBRE_REMITENTE);
        assertThat(enviado.getRecipients(Message.RecipientType.TO)).extracting(Object::toString).containsExactly(DESTINATARIO);
        assertThat(enviado.getSubject()).isEqualTo(ASUNTO);
        enviado.saveChanges();
        assertThat(enviado.getContentType()).startsWith("text/html");
        assertThat(enviado.getContent()).isEqualTo("<p>Hola Juan</p>");
    }

    @Test
    @DisplayName("CP-CO-02: enviarCorreoHtml no propaga una falla del servidor de correo")
    void enviarCorreoHtml_noDebePropagarUnaFallaDelServidor() {
        // === PREPARACION DE DATOS ===
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        when(templateEngine.process(eq(PLANTILLA), any(Context.class))).thenReturn("<p>Hola</p>");
        doThrow(new MailSendException("Autenticación rechazada")).when(mailSender).send(any(MimeMessage.class));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> correoServicio.enviarCorreoHtml(DESTINATARIO, ASUNTO, PLANTILLA, Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-CO-03: enviarCorreoHtml no propaga una falla de la plantilla y no envía nada")
    void enviarCorreoHtml_noDebePropagarUnaFallaDeLaPlantillaNiEnviar() {
        // === PREPARACION DE DATOS ===
        when(templateEngine.process(eq(PLANTILLA), any(Context.class))).thenThrow(new TemplateInputException("Plantilla inexistente"));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> correoServicio.enviarCorreoHtml(DESTINATARIO, ASUNTO, PLANTILLA, Map.of()))
                .doesNotThrowAnyException();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("CP-CO-06: intentarEnviarCorreoHtml envía en el mismo hilo e informa si el servidor aceptó el correo, sin propagar fallas")
    void intentarEnviarCorreoHtml_informaElResultado() throws Exception {
        // === PREPARACION DE DATOS ===
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(templateEngine.process(eq(PLANTILLA), any(Context.class))).thenReturn("<p>Hola</p>");

        // === EJECUCION Y ASSERTS ===
        assertThat(correoServicio.intentarEnviarCorreoHtml(DESTINATARIO, ASUNTO, PLANTILLA, Map.of())).isTrue();
        doThrow(new MailSendException("Autenticación rechazada")).when(mailSender).send(any(MimeMessage.class));
        assertThat(correoServicio.intentarEnviarCorreoHtml(DESTINATARIO, ASUNTO, PLANTILLA, Map.of())).isFalse();
        assertThat(CorreoServicioImpl.class.getMethod("intentarEnviarCorreoHtml", String.class, String.class, String.class, Map.class)
                .isAnnotationPresent(Async.class)).isFalse();
    }

    @Test
    @DisplayName("CP-CO-04: enviarCorreoHtml se ejecuta en un hilo aparte (@Async)")
    void enviarCorreoHtml_debeSerAsincrono() throws Exception {
        // === EJECUCION Y ASSERTS ===
        assertThat(CorreoServicioImpl.class.getMethod("enviarCorreoHtml", String.class, String.class, String.class, Map.class)
                .isAnnotationPresent(Async.class)).isTrue();
    }

    @Test
    @DisplayName("CP-CO-05: la plantilla real correo/restablecer-contrasena se procesa sin request HTTP y muestra el nombre, el enlace y los minutos de vigencia")
    void plantillaRestablecerContrasena_debeMostrarNombreEnlaceYVigencia() throws Exception {
        // === PREPARACION DE DATOS ===
        ClassLoaderTemplateResolver resolutor = new ClassLoaderTemplateResolver();
        resolutor.setPrefix("templates/");
        resolutor.setSuffix(".html");
        resolutor.setTemplateMode(TemplateMode.HTML);
        resolutor.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motorReal = new SpringTemplateEngine();
        motorReal.setTemplateResolver(resolutor);
        CorreoServicioImpl servicioConPlantillaReal = new CorreoServicioImpl(mailSender, motorReal, REMITENTE, NOMBRE_REMITENTE);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        String enlace = "http://localhost:7777/restablecer-contrasena?token=abc_DEF-123";

        // === EJECUCION ===
        servicioConPlantillaReal.enviarCorreoHtml(DESTINATARIO, ASUNTO, PLANTILLA,
                Map.of("nombre", "Juan", "enlace", enlace, "minutosVigencia", 30L));

        // === ASSERTS ===
        ArgumentCaptor<MimeMessage> mensaje = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mensaje.capture());
        String html = (String) mensaje.getValue().getContent();
        assertThat(html).contains("Hola <span>Juan</span>")
                .contains("href=\"" + enlace + "\"")
                .contains("<span>30</span> minutos")
                .contains("MAESTRO").contains("CERVECERO");
    }
}
