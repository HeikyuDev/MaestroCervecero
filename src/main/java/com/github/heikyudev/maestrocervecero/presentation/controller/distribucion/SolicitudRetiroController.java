package com.github.heikyudev.maestrocervecero.presentation.controller.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoSolicitudBusqueda;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ISolicitudBusquedaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.MensajeConLista;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de las solicitudes de retiro ("Solicitudes de Retiro"): las solicitudes de búsqueda de barril que registran
 * los clientes al coordinar el retiro desde el enlace del recordatorio. Es del ENCARGADO_DE_DISTRIBUCION y es de solo
 * lectura: una solicitud queda buscada al registrar la devolución del barril y cancelada al anular el despacho.
 * <p>
 * La lista muestra la última registrada arriba, filtrable por cliente (con el buscador de clientes en modal), rango de
 * fecha propuesta del retiro y estado (pendiente, buscada o cancelada). El detalle muestra todos los datos, con las observaciones
 * del cliente.
 * </p>
 */
@Controller
@RequestMapping("/solicitudes-retiro")
@PreAuthorize("hasAuthority('SOLICITUD_BUSQUEDA_CONSULTAR')")
@RequiredArgsConstructor
public class SolicitudRetiroController {

    private static final String VISTA_LISTA = "retiro_barriles/solicitudes-lista";
    private static final String VISTA_DETALLE = "retiro_barriles/solicitud-detalle";
    private static final String URL_LISTA = "/solicitudes-retiro";
    private static final String URL_BUSCADOR_CLIENTES = "/clientes/buscador";

    private final ISolicitudBusquedaServicio solicitudBusquedaServicio;
    private final IClienteServicio clienteServicio;

    /**
     * Lista las solicitudes de a {@code TAMANIO_PAGINA} y con la última registrada arriba. Los criterios (cliente, rango
     * de fecha propuesta del retiro, inclusive en ambos extremos, y estado) son opcionales: sin estado, o con un valor
     * inválido (se avisa), se muestran todas. Un cliente que ya no existe no restringe el listado. Un rango de fechas
     * invertido muestra el aviso del service y una lista vacía, para no perder los criterios elegidos.
     */
    @GetMapping({"", "/"})
    public String listarSolicitudes(@RequestParam(required = false) Long idCliente,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                    @RequestParam(required = false) String estado,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        List<String> avisos = new ArrayList<>();
        EstadoSolicitudBusqueda filtroEstado = aEstadoSolicitud(estado);
        if (vacioANulo(estado) != null && filtroEstado == null) {
            avisos.add("El estado indicado no es válido, se muestran todas las solicitudes");
        }

        ClienteResponseDTO cliente = resolverUno(idCliente, clienteServicio::buscarPorId);
        Pageable pageable = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO);
        Page<SolicitudBusquedaResponseDTO> solicitudes;
        try {
            solicitudes = solicitudBusquedaServicio.filtrarSolicitudesBusqueda(cliente == null ? null : cliente.getId(),
                    fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                    fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null,
                    filtroEstado, pageable);
        } catch (ReglaNegocioException e) {
            solicitudes = new PageImpl<>(List.of(), pageable, 0);
            avisos.add(e.getMessage());
        }

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("estados", EstadoSolicitudBusqueda.values());
        model.addAttribute("urlPantalla", URL_LISTA);
        model.addAttribute("urlBuscadorClientes", URL_BUSCADOR_CLIENTES);
        model.addAttribute("filtroIdCliente", cliente == null ? null : cliente.getId());
        model.addAttribute("filtroClienteTexto", cliente == null ? null : cliente.getNombre());
        model.addAttribute("filtroFechaDesde", fechaDesde);
        model.addAttribute("filtroFechaHasta", fechaHasta);
        model.addAttribute("filtroEstado", filtroEstado);
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_LISTA;
    }

    /** El estado de solicitud indicado, o {@code null} si falta o no es válido. */
    private static EstadoSolicitudBusqueda aEstadoSolicitud(String texto) {
        String valor = vacioANulo(texto);
        if (valor == null) {
            return null;
        }
        try {
            return EstadoSolicitudBusqueda.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Muestra el detalle de una solicitud, de solo lectura. Si no existe, el {@code ControllerAdvices} muestra el aviso.
     */
    @GetMapping("/{id}")
    public String verSolicitud(@PathVariable Long id, Model model) {
        model.addAttribute("solicitud", solicitudBusquedaServicio.buscarPorId(id));
        model.addAttribute("urlPantalla", URL_LISTA);
        return VISTA_DETALLE;
    }
}
