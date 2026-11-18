package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.CotizacionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.OfertaCotizacionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceCotizacionInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ICotizacionProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CotizacionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.LineaCotizacionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller de la pantalla pública con la que un proveedor responde una solicitud de cotización desde el enlace del
 * correo, sin iniciar sesión (ver {@code SecurityConfig#cotizacionProveedorFilterChain}). Es una pantalla aparte, sin
 * el menú de la aplicación, pensada para alguien que la usa de vez en cuando.
 * <p>
 * El token del enlace viaja en la dirección, nunca dentro del formulario. El formulario tiene una fila por insumo
 * solicitado que el proveedor ofrece, en el mismo orden que la pantalla. Si el enlace no sirve (venció, la solicitud
 * cerró o fue cancelada, o el proveedor se dio de baja) se muestra una pantalla que explica el motivo.
 * </p>
 */
@Controller
@RequestMapping("/cotizacion")
@RequiredArgsConstructor
public class CotizacionProveedorController {

    private static final String VISTA_FORMULARIO = "cotizacion/cotizacion-proveedor";
    private static final String VISTA_ENLACE_INVALIDO = "cotizacion/enlace-invalido";
    private static final String ATRIBUTO_FORMULARIO = "cotizacionForm";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ICotizacionProveedorServicio cotizacionProveedorServicio;

    /**
     * Muestra el formulario con los insumos solicitados que el proveedor ofrece, completado con su respuesta actual si
     * ya respondió; si el enlace no sirve, la pantalla que explica el motivo.
     */
    @GetMapping("/{token}")
    public String mostrarCotizacion(@PathVariable String token, Model model) {
        CotizacionProveedorResponseDTO cotizacion;
        try {
            cotizacion = cotizacionProveedorServicio.buscarCotizacion(token);
        } catch (EnlaceCotizacionInvalidoException e) {
            return mostrarEnlaceInvalido(model, e);
        }
        model.addAttribute(ATRIBUTO_FORMULARIO, formularioDe(cotizacion));
        cargarModelo(model, token, cotizacion);
        return VISTA_FORMULARIO;
    }

    /**
     * Registra la respuesta (Post-Redirect-Get) y vuelve al formulario con el aviso de que se puede modificar hasta el
     * cierre. Si una fila no cumple una regla, vuelve a mostrar el formulario con el error junto al campo; si el enlace
     * dejó de servir, muestra la pantalla que explica el motivo.
     */
    @PostMapping("/{token}")
    public String registrarCotizacion(@PathVariable String token,
                                      @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) CotizacionProveedorFormDTO cotizacionForm,
                                      BindingResult bindingResult,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                CotizacionProveedorResponseDTO registrada = cotizacionProveedorServicio.registrarCotizacion(token, cotizacionForm);
                agregarMensaje(redirectAttributes, "Cotización enviada correctamente: puede modificarla hasta el "
                        + registrada.getFechaLimiteRespuesta().format(FORMATO_FECHA_HORA), TipoAlerta.SUCCESS);
                return "redirect:/cotizacion/" + token;
            } catch (EnlaceCotizacionInvalidoException e) {
                return mostrarEnlaceInvalido(model, e);
            } catch (ReglaNegocioException e) {
                if (e.getCampo() != null) {
                    bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
                } else {
                    bindingResult.reject("reglaNegocio", e.getMessage());
                }
            }
        }

        CotizacionProveedorResponseDTO cotizacion;
        try {
            cotizacion = cotizacionProveedorServicio.buscarCotizacion(token);
        } catch (EnlaceCotizacionInvalidoException e) {
            return mostrarEnlaceInvalido(model, e);
        }
        cotizacionForm.setOfertas(alinearConLineas(cotizacionForm.getOfertas(), cotizacion.getLineas()));
        cargarModelo(model, token, cotizacion);
        return VISTA_FORMULARIO;
    }

    /** El formulario con una fila por insumo, completada con la respuesta actual del proveedor. */
    private static CotizacionProveedorFormDTO formularioDe(CotizacionProveedorResponseDTO cotizacion) {
        List<OfertaCotizacionFormDTO> filas = cotizacion.getLineas().stream()
                .map(linea -> OfertaCotizacionFormDTO.builder()
                        .idLinea(linea.getIdLinea())
                        .idCatalogoProveedor(linea.getIdCatalogoProveedorOfertado())
                        .precioUnitario(linea.getPrecioUnitarioOfertado())
                        .cantidadDisponible(linea.getCantidadDisponibleOfertada())
                        .build())
                .toList();
        return CotizacionProveedorFormDTO.builder().ofertas(new ArrayList<>(filas)).build();
    }

    /**
     * Deja una fila por insumo de la pantalla, en su orden, con lo que el proveedor había cargado: si las filas enviadas
     * ya coinciden con los insumos (lo habitual), no cambia nada y los errores quedan junto a su campo.
     */
    private static List<OfertaCotizacionFormDTO> alinearConLineas(List<OfertaCotizacionFormDTO> enviadas, List<LineaCotizacionProveedorResponseDTO> lineas) {
        List<OfertaCotizacionFormDTO> filas = enviadas == null ? List.of() : enviadas;
        boolean alineadas = filas.size() == lineas.size();
        for (int i = 0; alineadas && i < lineas.size(); i++) {
            alineadas = filas.get(i) != null && Objects.equals(filas.get(i).getIdLinea(), lineas.get(i).getIdLinea());
        }
        if (alineadas) {
            return filas;
        }
        List<OfertaCotizacionFormDTO> resultado = new ArrayList<>();
        for (LineaCotizacionProveedorResponseDTO linea : lineas) {
            resultado.add(filas.stream()
                    .filter(fila -> fila != null && Objects.equals(fila.getIdLinea(), linea.getIdLinea()))
                    .findFirst()
                    .orElse(OfertaCotizacionFormDTO.builder().idLinea(linea.getIdLinea()).build()));
        }
        return resultado;
    }

    private static void cargarModelo(Model model, String token, CotizacionProveedorResponseDTO cotizacion) {
        model.addAttribute("token", token);
        model.addAttribute("cotizacion", cotizacion);
    }

    private static String mostrarEnlaceInvalido(Model model, EnlaceCotizacionInvalidoException e) {
        model.addAttribute("mensaje", e.getMessage());
        model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        return VISTA_ENLACE_INVALIDO;
    }
}
