package com.github.heikyudev.maestrocervecero.service.implementation.correo;

import com.github.heikyudev.maestrocervecero.service.interfaces.correo.ICorreoServicio;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Implementación de {@link ICorreoServicio} sobre SMTP ({@link JavaMailSender}, configurado con las propiedades
 * {@code spring.mail.*} de cada perfil).
 * <p>
 * Corre en un hilo aparte (@Async): quien envía un correo no espera la respuesta del servidor SMTP, y el tiempo de
 * respuesta de la pantalla que lo dispara no depende de que el correo se haya enviado. Por la misma razón, toda falla
 * se atrapa aquí adentro y se registra en el log: nunca llega al usuario.
 * </p>
 */
@Slf4j
@Service
public class CorreoServicioImpl implements ICorreoServicio {

    private static final Locale IDIOMA_CORREOS = Locale.of("es", "AR");

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String remitente;
    private final String nombreRemitente;

    public CorreoServicioImpl(JavaMailSender mailSender,
                              ITemplateEngine templateEngine,
                              @Value("${app.correo.remitente}") String remitente,
                              @Value("${app.correo.nombre-remitente}") String nombreRemitente) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    @Override
    @Async
    public void enviarCorreoHtml(String destinatario, String asunto, String plantilla, Map<String, Object> variables) {
        try {
            String html = templateEngine.process(plantilla, new Context(IDIOMA_CORREOS, variables));

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, false, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            mailSender.send(mensaje);

            log.info("Correo enviado (plantilla={})", plantilla);
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            log.error("No se pudo enviar el correo (plantilla={}, falla={}): {}",
                    plantilla, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
