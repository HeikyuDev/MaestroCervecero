package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pestañas del detalle de una solicitud de cotización ({@code /solicitudes-cotizacion/{id}?pestana=<clave>}), en el
 * orden en que se muestran. La del plan de compra solo se ofrece cuando la solicitud tiene un plan generado.
 */
@Getter
@RequiredArgsConstructor
public enum PestanaSolicitudCotizacion {

    SOLICITUD("solicitud", "Solicitud", "bi-file-earmark-text"),
    PROVEEDORES("proveedores", "Proveedores", "bi-people"),
    PLAN("plan", "Plan de compra", "bi-cart-check");

    /** Clave de la pestaña en la URL ({@code ?pestana=}). */
    private final String clave;
    /** Texto de la pestaña. */
    private final String etiqueta;
    /** Ícono de Bootstrap Icons de la pestaña. */
    private final String icono;

    /**
     * Las pestañas que ofrece la solicitud: todas si tiene un plan generado; si no, todas menos la del plan.
     *
     * @param solicitud La solicitud que se muestra.
     * @return Las pestañas, en el orden en que se muestran.
     */
    public static List<PestanaSolicitudCotizacion> disponibles(SolicitudCotizacionResponseDTO solicitud) {
        boolean conPlan = solicitud.getPlan() != null;
        return Arrays.stream(values()).filter(p -> conPlan || p != PLAN).toList();
    }

    /**
     * Resuelve la pestaña activa: la pedida si la solicitud la ofrece; si no (o si no se pidió ninguna), la del plan
     * cuando la solicitud tiene un plan generado y la de la solicitud en caso contrario.
     *
     * @param clave     La clave pedida en la URL, sin distinguir mayúsculas; puede ser nula.
     * @param solicitud La solicitud que se muestra.
     * @return La pestaña activa.
     */
    public static PestanaSolicitudCotizacion resolver(String clave, SolicitudCotizacionResponseDTO solicitud) {
        List<PestanaSolicitudCotizacion> disponibles = disponibles(solicitud);
        return deClave(clave).filter(disponibles::contains)
                .orElse(solicitud.getPlan() != null ? PLAN : SOLICITUD);
    }

    /**
     * Resuelve la pestaña a partir de su clave en la URL.
     *
     * @param clave La clave pedida, sin distinguir mayúsculas.
     * @return La pestaña, o vacío si la clave es nula o no existe.
     */
    public static Optional<PestanaSolicitudCotizacion> deClave(String clave) {
        if (clave == null) {
            return Optional.empty();
        }
        String buscada = clave.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(p -> p.clave.equals(buscada)).findFirst();
    }
}
