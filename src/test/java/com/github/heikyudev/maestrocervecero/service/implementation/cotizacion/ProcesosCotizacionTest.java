package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.PlanificacionProduccionRegistradaEvento;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.interfaces.correo.ICorreoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Piezas del proceso automático alrededor del service: el disparo al registrar una planificación de producción, el cierre
 * programado y los correos.
 */
class ProcesosCotizacionTest {

    @Test
    @DisplayName("CP-PRC-01: al registrarse una planificación se genera su solicitud de cotización, sin enviar nada a los proveedores hasta que se confirme")
    void listener_generaSolicitudYEnvia() {
        ISolicitudCotizacionServicio servicio = mock(ISolicitudCotizacionServicio.class);
        when(servicio.generarSolicitud(7L)).thenReturn(java.util.Optional.of(SolicitudCotizacionResponseDTO.builder().id(90L).build()));
        when(servicio.generarSolicitud(8L)).thenReturn(java.util.Optional.empty());
        NotificadorCotizaciones notificador = mock(NotificadorCotizaciones.class);
        GeneracionSolicitudCotizacionListener listener = new GeneracionSolicitudCotizacionListener(servicio, notificador);

        listener.alRegistrarPlanificacion(new PlanificacionProduccionRegistradaEvento(7L));
        listener.alRegistrarPlanificacion(new PlanificacionProduccionRegistradaEvento(8L));

        verify(servicio).generarSolicitud(7L);
        verify(servicio).generarSolicitud(8L);
        verify(servicio, never()).prepararEnvios(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("CP-PRC-06: solo el registro de una planificación dispara el proceso: el listener escucha únicamente ese evento, después de confirmarse la transacción")
    void listener_escuchaSoloElRegistroDePlanificacion() throws NoSuchMethodException {
        java.lang.reflect.Method[] metodos = GeneracionSolicitudCotizacionListener.class.getDeclaredMethods();
        java.util.List<java.lang.reflect.Method> escuchas = java.util.Arrays.stream(metodos)
                .filter(m -> m.isAnnotationPresent(org.springframework.transaction.event.TransactionalEventListener.class)
                        || m.isAnnotationPresent(org.springframework.context.event.EventListener.class))
                .toList();

        assertThat(escuchas).singleElement().satisfies(metodo -> {
            assertThat(metodo.getParameterTypes()).containsExactly(PlanificacionProduccionRegistradaEvento.class);
            assertThat(metodo.getAnnotation(org.springframework.transaction.event.TransactionalEventListener.class).phase())
                    .isEqualTo(org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT);
        });
    }

    @Test
    @DisplayName("CP-PRC-02: una falla al generar la solicitud se registra, avisa a los gerentes de compras con un texto genérico y no llega al registro de la planificación")
    void listener_fallaNoSePropaga() {
        ISolicitudCotizacionServicio servicio = mock(ISolicitudCotizacionServicio.class);
        NotificadorCotizaciones notificador = mock(NotificadorCotizaciones.class);
        when(servicio.generarSolicitud(7L)).thenThrow(new IllegalStateException("org.postgresql.util.PSQLException: deadlock detected"));
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);

        assertThatCode(() -> new GeneracionSolicitudCotizacionListener(servicio, notificador)
                .alRegistrarPlanificacion(new PlanificacionProduccionRegistradaEvento(7L))).doesNotThrowAnyException();

        verify(notificador).avisarGerentesDeCompras(org.mockito.ArgumentMatchers.isNull(), eq(GeneracionSolicitudCotizacionListener.ASUNTO_FALLA_GENERACION),
                mensaje.capture(), eq(List.of()));
        assertThat(mensaje.getValue()).isEqualTo("No se pudo generar automáticamente la solicitud de cotización de la planificación N° 7: "
                + "revise si le faltan insumos y registre las órdenes de compra necesarias");
        assertThat(mensaje.getValue()).doesNotContain("PSQLException").doesNotContain("deadlock");

        // Si también falla el aviso, tampoco se propaga
        doThrow(new IllegalStateException("sin correo")).when(notificador).avisarGerentesDeCompras(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThatCode(() -> new GeneracionSolicitudCotizacionListener(servicio, notificador)
                .alRegistrarPlanificacion(new PlanificacionProduccionRegistradaEvento(7L))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-PRC-03: el proceso programado reintenta los envíos pendientes, cierra cada solicitud vencida y registra la falla de un cierre sin impedir los demás")
    void cierreProgramado_enviaYCierra() {
        ISolicitudCotizacionServicio servicio = mock(ISolicitudCotizacionServicio.class);
        EnvioInvitacionesCotizacion envio = mock(EnvioInvitacionesCotizacion.class);
        when(servicio.buscarIdsConEnviosPendientes()).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("servidor caído")).when(envio).enviarPendientes(1L);
        when(servicio.buscarIdsVencidas()).thenReturn(List.of(3L, 4L, 5L));
        doThrow(new IllegalStateException("bloqueo")).when(servicio).cerrarSolicitud(4L);

        assertThatCode(() -> new CierreSolicitudesCotizacionProgramado(servicio, envio).procesarSolicitudes()).doesNotThrowAnyException();

        verify(envio).enviarPendientes(2L);
        verify(servicio).cerrarSolicitud(3L);
        verify(servicio).cerrarSolicitud(5L);
        verify(servicio).registrarFallaCierre(4L, "bloqueo");
        verify(servicio, never()).registrarFallaCierre(eq(3L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("CP-PRC-04: la invitación al proveedor lleva el enlace con el token, la fecha límite y los insumos, y devuelve si el servidor de correo la aceptó")
    @SuppressWarnings("unchecked")
    void notificador_invitaProveedor() {
        ICorreoServicio correo = mock(ICorreoServicio.class);
        when(correo.intentarEnviarCorreoHtml(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true, false);
        NotificadorCotizaciones notificador = new NotificadorCotizaciones(correo, mock(IUsuarioRepository.class), "https://cerveceria.com");
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);

        assertThat(notificador.invitarProveedor("ventas@malteria.com", "MALTERIA SUR", "abc123", LocalDateTime.of(2026, 11, 20, 10, 30), List.of("Malta Pilsen"))).isTrue();
        assertThat(notificador.invitarProveedor("ventas@malteria.com", "MALTERIA SUR", "abc123", LocalDateTime.of(2026, 11, 20, 10, 30), List.of("Malta Pilsen"))).isFalse();

        verify(correo, org.mockito.Mockito.times(2)).intentarEnviarCorreoHtml(eq("ventas@malteria.com"), eq(NotificadorCotizaciones.ASUNTO_INVITACION),
                eq(NotificadorCotizaciones.PLANTILLA_INVITACION), variables.capture());
        assertThat(variables.getValue())
                .containsEntry("razonSocial", "MALTERIA SUR")
                .containsEntry("enlace", "https://cerveceria.com/cotizacion/abc123")
                .containsEntry("fechaLimite", "20/11/2026 10:30")
                .containsEntry("insumos", List.of("Malta Pilsen"));
    }

    @Test
    @DisplayName("CP-PRC-05: el aviso llega a cada gerente de compras activo con el enlace a la solicitud (o al listado si no hay solicitud), sus renglones y los párrafos de cierre")
    @SuppressWarnings("unchecked")
    void notificador_avisaGerentes() {
        ICorreoServicio correo = mock(ICorreoServicio.class);
        IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
        when(usuarios.findByRolAndEstado(Rol.GERENTE_DE_COMPRAS, Estado.ACTIVO)).thenReturn(List.of(
                UsuarioEntity.builder().nombre("Ana").correo("ana@cerveceria.com").build(),
                UsuarioEntity.builder().nombre("Luis").correo("luis@cerveceria.com").build()));
        NotificadorCotizaciones notificador = new NotificadorCotizaciones(correo, usuarios, "https://cerveceria.com");
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);

        notificador.avisarGerentesDeCompras(90L, "Plan listo", "La solicitud cerró", List.of("Órdenes de compra propuestas: 2"));

        verify(correo).enviarCorreoHtml(eq("ana@cerveceria.com"), eq("Plan listo"), eq(NotificadorCotizaciones.PLANTILLA_AVISO), variables.capture());
        verify(correo).enviarCorreoHtml(eq("luis@cerveceria.com"), eq("Plan listo"), eq(NotificadorCotizaciones.PLANTILLA_AVISO), org.mockito.ArgumentMatchers.anyMap());
        assertThat(variables.getValue())
                .containsEntry("nombre", "Ana")
                .containsEntry("mensaje", "La solicitud cerró")
                .containsEntry("enlace", "https://cerveceria.com/solicitudes-cotizacion/90")
                .containsEntry("detalles", List.of("Órdenes de compra propuestas: 2"))
                .containsEntry("cierre", List.of());

        // Sin solicitud (no se pudo generar) el enlace lleva al listado de procesos de compra
        notificador.avisarGerentesDeCompras(null, "Sin solicitud", "No se generó", List.of());
        verify(correo, org.mockito.Mockito.times(2)).enviarCorreoHtml(eq("ana@cerveceria.com"), org.mockito.ArgumentMatchers.anyString(), eq(NotificadorCotizaciones.PLANTILLA_AVISO), variables.capture());
        assertThat(variables.getValue()).containsEntry("enlace", "https://cerveceria.com/solicitudes-cotizacion");

        // Con párrafos de cierre: van después de los renglones
        notificador.avisarGerentesDeCompras(91L, "Por confirmar", "El stock actual no cubre:", List.of("Malta Pilsen: faltan 120 kg"),
                List.of("Confirme el envío a los proveedores o cancele la solicitud"));
        verify(correo).enviarCorreoHtml(eq("ana@cerveceria.com"), eq("Por confirmar"), eq(NotificadorCotizaciones.PLANTILLA_AVISO), variables.capture());
        assertThat(variables.getValue())
                .containsEntry("detalles", List.of("Malta Pilsen: faltan 120 kg"))
                .containsEntry("cierre", List.of("Confirme el envío a los proveedores o cancele la solicitud"));
    }

    @Test
    @DisplayName("CP-PRC-07: el envío prepara los tokens, manda cada correo fuera de toda transacción y registra los resultados después; una excepción del correo cuenta como fallido")
    void envio_enviaFueraDeLaTransaccion() throws NoSuchMethodException {
        ISolicitudCotizacionServicio servicio = mock(ISolicitudCotizacionServicio.class);
        NotificadorCotizaciones notificador = mock(NotificadorCotizaciones.class);
        LocalDateTime limite = LocalDateTime.of(2026, 11, 20, 10, 30);
        when(servicio.prepararEnvios(90L)).thenReturn(List.of(
                new com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.EnvioInvitacionPreparado(601L, "a@p.com", "MALTERIA SUR", "t1", limite, List.of("Malta Pilsen")),
                new com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.EnvioInvitacionPreparado(602L, "b@p.com", "LUPULOS", "t2", limite, List.of("Malta Pilsen"))));
        when(notificador.invitarProveedor(eq("a@p.com"), eq("MALTERIA SUR"), eq("t1"), eq(limite), eq(List.of("Malta Pilsen")))).thenReturn(true);
        when(notificador.invitarProveedor(eq("b@p.com"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("SMTP caído"));
        EnvioInvitacionesCotizacion envio = new EnvioInvitacionesCotizacion(servicio, notificador);

        envio.enviarPendientes(90L);

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(servicio, notificador);
        orden.verify(servicio).prepararEnvios(90L);
        orden.verify(notificador, org.mockito.Mockito.times(2)).invitarProveedor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        orden.verify(servicio).registrarResultadosEnvio(90L, Map.of(601L, true, 602L, false));
        // El orquestador no abre transacción: el correo nunca se envía con la solicitud bloqueada
        assertThat(EnvioInvitacionesCotizacion.class.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();
        assertThat(EnvioInvitacionesCotizacion.class.getMethod("enviarPendientes", Long.class)
                .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class)).isFalse();

        // Sin nada preparado no se registra nada
        when(servicio.prepararEnvios(91L)).thenReturn(List.of());
        envio.enviarPendientes(91L);
        verify(servicio, never()).registrarResultadosEnvio(eq(91L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("CP-PRC-08: la plantilla real de la invitación pide precio y cantidad por presentación, sin la fecha estimada de entrega")
    void plantillaInvitacion_pidePrecioYCantidadPorPresentacion() {
        // === PREPARACION DE DATOS ===
        ClassLoaderTemplateResolver resolutor = new ClassLoaderTemplateResolver();
        resolutor.setPrefix("templates/");
        resolutor.setSuffix(".html");
        resolutor.setTemplateMode(TemplateMode.HTML);
        resolutor.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolutor);
        Context contexto = new Context();
        contexto.setVariables(Map.of("razonSocial", "MALTERIA SUR", "enlace", "https://cerveceria.com/cotizacion/abc123",
                "fechaLimite", "20/11/2026 10:30", "insumos", List.of("Malta Pilsen")));

        // === EJECUCION ===
        String html = motor.process(NotificadorCotizaciones.PLANTILLA_INVITACION, contexto);

        // === ASSERTS ===
        assertThat(html).contains("Hola MALTERIA SUR:").contains("Malta Pilsen")
                .contains("Para indicar el precio y la cantidad que puede entregar en cada presentación, presione el botón.")
                .contains("href=\"https://cerveceria.com/cotizacion/abc123\"")
                .doesNotContain("puede combinar varias")
                .doesNotContain("fecha estimada de entrega");
    }

    @Test
    @DisplayName("CP-PRC-09: el correo de aviso a los gerentes de compras muestra un renglón por cada detalle y los párrafos de cierre después de la lista")
    void plantillaAviso_muestraRenglonesYCierre() {
        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        org.thymeleaf.spring6.SpringTemplateEngine motor = new org.thymeleaf.spring6.SpringTemplateEngine();
        motor.setTemplateResolver(resolver);
        org.thymeleaf.context.Context contexto = new org.thymeleaf.context.Context();
        contexto.setVariables(Map.of("nombre", "Ana", "titulo", "Por confirmar", "enlace", "https://cerveceria.com/solicitudes-cotizacion/12",
                "mensaje", "Se generó la solicitud de cotización N° 12 para la planificación N° 45. El stock actual no cubre:",
                "detalles", List.of("Malta Pilsen: faltan 120 kg", "Lúpulo Cascade: faltan 2,5 kg"),
                "cierre", List.of("Ningún proveedor activo ofrece Cascade", "Confirme el envío a los proveedores o cancele la solicitud")));

        String html = motor.process(NotificadorCotizaciones.PLANTILLA_AVISO, contexto);

        assertThat(html).containsPattern("<li>Malta Pilsen: faltan 120 kg</li>\\s*<li>Lúpulo Cascade: faltan 2,5 kg</li>\\s*</ul>\\s*"
                + "<p[^>]*>Ningún proveedor activo ofrece Cascade</p>\\s*<p[^>]*>Confirme el envío a los proveedores o cancele la solicitud</p>");
    }

    @Test
    @DisplayName("CP-PRC-10: el aviso de cancelación llega al proveedor en un hilo aparte con su razón social y el número de la solicitud, sin enlace")
    @SuppressWarnings("unchecked")
    void notificador_avisaCancelacionAProveedor() {
        // === PREPARACION DE DATOS ===
        ICorreoServicio correo = mock(ICorreoServicio.class);
        NotificadorCotizaciones notificador = new NotificadorCotizaciones(correo, mock(IUsuarioRepository.class), "https://cerveceria.com");
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);

        // === EJECUCION ===
        notificador.avisarCancelacionAProveedor("ventas@malteria.com", "MALTERIA SUR", 90L);

        // === ASSERTS ===
        verify(correo).enviarCorreoHtml(eq("ventas@malteria.com"), eq("Solicitud de cotización cancelada"),
                eq("correo/cancelacion-solicitud-cotizacion"), variables.capture());
        verify(correo, never()).intentarEnviarCorreoHtml(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(variables.getValue())
                .containsEntry("razonSocial", "MALTERIA SUR")
                .containsEntry("numeroSolicitud", 90L)
                .doesNotContainKey("enlace");
    }

    @Test
    @DisplayName("CP-PRC-11: la plantilla real del aviso de cancelación informa que la solicitud se canceló y que no hace falta responder, sin enlace a la cotización")
    void plantillaCancelacion_informaQueNoHaceFaltaResponder() {
        // === PREPARACION DE DATOS ===
        ClassLoaderTemplateResolver resolutor = new ClassLoaderTemplateResolver();
        resolutor.setPrefix("templates/");
        resolutor.setSuffix(".html");
        resolutor.setTemplateMode(TemplateMode.HTML);
        resolutor.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolutor);
        Context contexto = new Context();
        contexto.setVariables(Map.of("razonSocial", "MALTERIA SUR", "numeroSolicitud", 90L));

        // === EJECUCION ===
        String html = motor.process(NotificadorCotizaciones.PLANTILLA_CANCELACION, contexto);

        // === ASSERTS ===
        assertThat(html).contains("Hola MALTERIA SUR:")
                .contains("La solicitud de cotización N° 90 de Maestro Cervecero fue cancelada")
                .contains("No es necesario que responda")
                .contains("El enlace que recibió ya no acepta respuestas")
                .contains("MAESTRO").contains("CERVECERO")
                .doesNotContain("/cotizacion/")
                .doesNotContain("Motivo");
    }
}
