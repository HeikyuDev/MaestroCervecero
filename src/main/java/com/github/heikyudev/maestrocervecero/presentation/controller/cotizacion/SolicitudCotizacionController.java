package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.service.implementation.cotizacion.EnvioInvitacionesCotizacion;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.PlanCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.TAMANIO_PAGINA;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Controller de los procesos de compra (solicitudes de cotización) que genera el sistema al registrar una
 * planificación de producción con insumos faltantes. Se entra desde el botón del listado de órdenes de compra
 * (etiqueta en {@code TextosProcesoCompra.ENTRADA}), que es la única entrada. Es del GERENTE_DE_COMPRAS, igual que el registro de órdenes de compra: el
 * gerente de producción no consulta órdenes de compra.
 * <p>
 * El listado muestra primero las solicitudes pendientes de confirmación, después las abiertas con el tiempo que les
 * queda y luego las demás, de la más nueva a la más vieja. Una solicitud pendiente de confirmación se confirma (envío a
 * los proveedores) o se cancela desde su detalle. El detalle muestra cada plan en su pestaña ({@code ?plan=N}); cada orden propuesta se confirma en el alta
 * de órdenes de compra prearmada ({@code /ordenes-compra/nuevo?idOrdenPropuesta=X}) y las del plan elegido se pueden
 * descartar.
 * </p>
 */
@Controller
@RequestMapping("/solicitudes-cotizacion")
@PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")
@RequiredArgsConstructor
public class SolicitudCotizacionController {

    private static final String VISTA_LISTA = "cotizacion/solicitudes-lista";
    private static final String VISTA_DETALLE = "cotizacion/solicitud-detalle";
    private static final String URL_LISTA = "/solicitudes-cotizacion";

    private final ISolicitudCotizacionServicio solicitudCotizacionServicio;
    private final EnvioInvitacionesCotizacion envioInvitaciones;

    /**
     * Lista las solicitudes, paginadas de a {@code TAMANIO_PAGINA}: primero las abiertas, de la que vence antes a la que
     * vence después, y luego las demás, de la más nueva a la más vieja, con el filtro opcional de estado (un estado que
     * no existe se ignora).
     */
    @GetMapping
    public String listarSolicitudes(@RequestParam(required = false) String estado,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        EstadoSolicitudCotizacion estadoFiltro = aEstado(estado);
        // El orden lo define la consulta del repositorio: primero las abiertas por vencimiento, después las demás de la más nueva a la más vieja
        model.addAttribute("solicitudes", solicitudCotizacionServicio.filtrarSolicitudes(estadoFiltro, null,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA)));
        model.addAttribute("estados", EstadoSolicitudCotizacion.values());
        model.addAttribute("filtroEstado", estadoFiltro);
        return VISTA_LISTA;
    }

    /**
     * Muestra el detalle de una solicitud: sus datos, los insumos solicitados, los proveedores invitados y los planes,
     * uno por pestaña. Sin {@code plan} (o con uno que no existe) se muestra el primero. Si la solicitud no existe, el
     * {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}")
    public String mostrarDetalle(@PathVariable Long id,
                                 @RequestParam(required = false) Integer plan,
                                 Model model) {
        SolicitudCotizacionResponseDTO solicitud = solicitudCotizacionServicio.buscarPorId(id);
        PlanCompraResponseDTO planActivo = solicitud.getPlanes().stream()
                .filter(p -> p.getNumero().equals(plan))
                .findFirst()
                .orElse(solicitud.getPlanes().isEmpty() ? null : solicitud.getPlanes().get(0));
        model.addAttribute("solicitud", solicitud);
        model.addAttribute("planActivo", planActivo);
        return VISTA_DETALLE;
    }

    /**
     * Descarta una orden propuesta del plan elegido y vuelve a la pestaña del plan. Si el service la rechaza, el
     * {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @PostMapping("/{id}/ordenes-propuestas/{idOrdenPropuesta}/descartar")
    public String descartarOrdenPropuesta(@PathVariable Long id,
                                          @PathVariable Long idOrdenPropuesta,
                                          @RequestParam(required = false) Integer plan,
                                          RedirectAttributes redirectAttributes) {
        solicitudCotizacionServicio.descartarOrdenPropuesta(id, idOrdenPropuesta);
        agregarMensaje(redirectAttributes, "Orden propuesta descartada correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id + (plan == null ? "" : "?plan=" + plan);
    }

    /**
     * Confirma el envío de una solicitud pendiente de confirmación y vuelve a su detalle. Confirmada la transacción, envía
     * los correos a los proveedores en otra transacción (un envío fallido lo reintenta el proceso programado). Si el
     * service rechaza la confirmación, el {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @PostMapping("/{id}/confirmar-envio")
    public String confirmarEnvio(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        solicitudCotizacionServicio.confirmarEnvio(id);
        envioInvitaciones.enviarPendientes(id);
        agregarMensaje(redirectAttributes, "Envío a los proveedores confirmado correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id;
    }

    /**
     * Cancela a mano una solicitud pendiente de confirmación, abierta o con planes sin elegir y vuelve a su detalle. El motivo es obligatorio: si
     * falta o el service rechaza la cancelación, el {@code ControllerAdvices} vuelve al origen con el motivo del rechazo.
     */
    @PostMapping("/{id}/cancelar")
    public String cancelarSolicitud(@PathVariable Long id,
                                    @RequestParam(required = false) String motivo,
                                    RedirectAttributes redirectAttributes) {
        solicitudCotizacionServicio.cancelarSolicitud(id, motivo);
        agregarMensaje(redirectAttributes, "Solicitud de cotización cancelada correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id;
    }

    /** El estado con ese nombre (sin distinguir mayúsculas), o {@code null} si el texto está vacío o no es un estado. */
    private static EstadoSolicitudCotizacion aEstado(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return EstadoSolicitudCotizacion.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
