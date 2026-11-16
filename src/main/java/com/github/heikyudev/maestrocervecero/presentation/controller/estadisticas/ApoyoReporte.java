package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.EncabezadoReporte;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import com.github.heikyudev.maestrocervecero.util.MensajeConLista;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Apoyo del Controller de estadísticas: los mensajes de la pantalla, el emisor y la respuesta de un reporte impreso, la
 * vuelta a la pantalla cuando el reporte impreso no se puede generar y la lectura de un valor de enum.
 */
public final class ApoyoReporte {

    static final String TITULO_ERRORES = "Revise los criterios:";

    private ApoyoReporte() {
    }

    /**
     * Muestra los errores de los criterios (o, si no hay, los avisos) como alerta amarilla del layout.
     */
    public static void mostrarMensajes(Model model, List<String> errores, List<String> avisos) {
        if (!errores.isEmpty()) {
            model.addAttribute("mensaje", errores.size() == 1 ? errores.get(0) : MensajeConLista.construir(TITULO_ERRORES, errores));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        } else if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
    }

    /**
     * Quien emite un reporte impreso: su nombre de usuario y, si la cuenta se encuentra, su nombre y apellido.
     */
    public static String emisor(Authentication authentication, IUsuarioServicio usuarioServicio) {
        String username = authentication.getName();
        try {
            UsuarioResponseDTO usuario = usuarioServicio.buscarPorUsername(username);
            return usuario == null ? username : EncabezadoReporte.emisor(username, usuario.getNombre(), usuario.getApellido());
        } catch (RecursoNoEncontradoException e) {
            return username;
        }
    }

    /**
     * La respuesta de un reporte impreso: PDF en línea con el nombre {@code <reporte>_<desde>_<hasta>.pdf}.
     */
    public static ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String reporte, LocalDate desde, LocalDate hasta) {
        String archivo = reporte + "_" + desde + "_" + hasta + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(archivo).build().toString())
                .body(pdf);
    }

    /**
     * Redirección a la pantalla del reporte con los mismos parámetros recibidos (los vacíos se omiten): la pantalla vuelve
     * a validar los criterios y muestra el motivo por el que el reporte impreso no se generó.
     *
     * @param url Ruta de la pantalla del reporte.
     * @param parametros Parámetros recibidos, en el orden en que se escriben en la dirección.
     * @return La vista de redirección.
     */
    public static String volverALaPantalla(String url, Map<String, String> parametros) {
        UriComponentsBuilder destino = UriComponentsBuilder.fromPath(url);
        parametros.forEach((nombre, valor) -> {
            if (vacioANulo(valor) != null) {
                destino.queryParam(nombre, valor.strip());
            }
        });
        return "redirect:" + destino.encode().build().toUriString();
    }

    /** El valor del enum con ese nombre (sin distinguir mayúsculas), o {@code null} si el texto está vacío o no existe. */
    public static <E extends Enum<E>> E aEnum(Class<E> tipo, String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        return Arrays.stream(tipo.getEnumConstants())
                .filter(valor -> valor.name().equals(limpio.toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }
}
