package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.service.implementation.planificacion_produccion.PlanificacionProduccionRegistradaEvento;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Dispara la generación de la solicitud de cotización cuando se registra una planificación de producción. La solicitud
 * queda pendiente de que el gerente de compras confirme el envío a los proveedores.
 * <p>
 * Escucha recién cuando el registro de la planificación se guardó ({@code AFTER_COMMIT}): si el registro se revierte,
 * no se solicita nada. La generación corre en una transacción propia, y una falla se registra en el log sin afectar a la
 * planificación, que ya quedó registrada; ante una falla se avisa a los gerentes de compras con un texto genérico.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneracionSolicitudCotizacionListener {

    static final String ASUNTO_FALLA_GENERACION = "No se pudo generar una solicitud de cotización";

    private final ISolicitudCotizacionServicio solicitudCotizacionServicio;
    private final NotificadorCotizaciones notificador;

    /**
     * Genera la solicitud de cotización de la planificación registrada.
     *
     * @param evento La planificación de producción registrada.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarPlanificacion(PlanificacionProduccionRegistradaEvento evento) {
        try {
            solicitudCotizacionServicio.generarSolicitud(evento.idPlanificacion());
        } catch (RuntimeException e) {
            // El detalle técnico solo va al log; los gerentes reciben un aviso genérico para revisar la planificación
            log.error("No se pudo generar la solicitud de cotización de la planificación {}: {}", evento.idPlanificacion(), e.getMessage(), e);
            avisarFalla(evento.idPlanificacion());
        }
    }

    private void avisarFalla(Long idPlanificacion) {
        try {
            notificador.avisarGerentesDeCompras(null, ASUNTO_FALLA_GENERACION,
                    "No se pudo generar automáticamente la solicitud de cotización de la planificación N° " + idPlanificacion
                            + ": revise si le faltan insumos y registre las órdenes de compra necesarias", List.of());
        } catch (RuntimeException e) {
            log.error("No se pudo avisar la falla de la generación de la planificación {}: {}", idPlanificacion, e.getMessage());
        }
    }
}
