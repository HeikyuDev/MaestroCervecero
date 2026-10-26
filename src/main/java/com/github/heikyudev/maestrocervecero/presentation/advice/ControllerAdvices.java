package com.github.heikyudev.maestrocervecero.presentation.advice;

import com.github.heikyudev.maestrocervecero.service.exception.BaseException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manejo global de excepciones para controladores MVC ({@link Controller}).
 * <p>
 * Cada handler traduce la excepción en un par {@code mensaje}/{@code tipo} vía Flash
 * Attributes -el mismo contrato que ya consume {@code login.html}- y redirige de vuelta
 * al origen de la petición (header {@code Referer}) solo si es del mismo sitio, con un fallback a la raíz. Si el origen es
 * la misma página que falló al pedirse, vuelve a su ruta padre para no repetir el rechazo sin fin. Un buscador en modal no
 * puede redirigir: responde con el estado HTTP del rechazo y el motivo como texto.
 * </p>
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class ControllerAdvices {

    private static final String MENSAJE_ERROR_INESPERADO = "Ocurrió un error inesperado. Intente nuevamente";
    private static final String MENSAJE_VALOR_INVALIDO = "El valor indicado no es válido";
    private static final String MENSAJE_CONFLICTO_DE_DATOS =
            "No se pudo guardar porque los datos entran en conflicto con otro registro. Revise la información e intente nuevamente";
    private static final Pattern SUFIJO_DE_REGISTRO = Pattern.compile("^(.*)/\\d+(?:/editar)?/?$");

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public Object handleRecursoNoEncontrado(RecursoNoEncontradoException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.DANGER, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public Object handleRecursoDuplicado(RecursoDuplicadoException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.DANGER, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public Object handleReglaNegocio(ReglaNegocioException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.warn("Regla de negocio incumplida: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.WARNING, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Red de seguridad para subclases de {@link BaseException} que no tengan un handler
     * específico propio.
     */
    @ExceptionHandler(BaseException.class)
    public Object handleBaseException(BaseException ex,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        log.warn("Excepción de dominio sin handler específico: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.DANGER, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Red de seguridad para una violación de integridad de la base de datos que ningún Service tradujo a una
     * excepción de negocio (por ejemplo, un índice único sin mensaje asociado o una clave foránea). Sin este
     * handler, el catch-all de abajo la mostraría como un error inesperado.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                               HttpServletRequest request,
                                               RedirectAttributes redirectAttributes) {
        log.warn("Violación de integridad de datos en {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return redirigirConMensaje(request, redirectAttributes, MENSAJE_CONFLICTO_DE_DATOS, TipoAlerta.DANGER, HttpStatus.CONFLICT);
    }

    /**
     * Sin este handler, el catch-all de abajo se tragaría el rechazo de {@code @PreAuthorize}
     * y le mostraría al usuario un "error inesperado" en vez de un mensaje de permisos.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.warn("Acceso denegado a {}", request.getRequestURI());
        // Un rechazo de permisos siempre vuelve al origen con la alerta, también desde un buscador en modal
        redirectAttributes.addFlashAttribute("mensaje", "No tiene permisos para realizar esta acción");
        redirectAttributes.addFlashAttribute("tipo", TipoAlerta.DANGER.getCodigo());
        return volverAlOrigen(request);
    }

    /**
     * Un valor de la URL que no se puede convertir al tipo del parámetro (por ejemplo, {@code /lotes/abc}) es un
     * error del pedido, no del sistema: no se registra como error. En una página se vuelve al origen con una alerta
     * amarilla. Un buscador en modal ({@code /buscador} o {@code /filtros} en la ruta) no puede redirigir, porque el
     * navegador recibiría una página completa: responde 400 con el mensaje como texto, que el modal muestra dentro de sí.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTipoDeArgumentoInvalido(MethodArgumentTypeMismatchException ex,
                                                HttpServletRequest request,
                                                RedirectAttributes redirectAttributes) {
        log.warn("Valor inválido para el parámetro {} en {}", ex.getName(), request.getRequestURI());
        return redirigirConMensaje(request, redirectAttributes, MENSAJE_VALOR_INVALIDO, TipoAlerta.WARNING, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.error("Error inesperado procesando la petición a {}", request.getRequestURI(), ex);
        return redirigirConMensaje(request, redirectAttributes, MENSAJE_ERROR_INESPERADO, TipoAlerta.DANGER, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Traduce el rechazo según quién lo pidió. Una página vuelve al origen con la alerta (Post-Redirect-Get). Un buscador
     * en modal ({@code /buscador} o {@code /filtros} en la ruta) no puede redirigir: el navegador recibiría una página
     * completa de otro lugar y el modal solo podría decir que no se pudo cargar. Por eso responde con el estado HTTP del
     * rechazo y el motivo como texto, que el modal muestra dentro de sí.
     */
    private Object redirigirConMensaje(HttpServletRequest request,
                                       RedirectAttributes redirectAttributes,
                                       String mensaje,
                                       TipoAlerta tipo,
                                       HttpStatus estado) {
        if (esBuscadorEnModal(request)) {
            return ResponseEntity.status(estado)
                    .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
                    .body(mensaje);
        }
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", tipo.getCodigo());
        return volverAlOrigen(request);
    }

    private static boolean esBuscadorEnModal(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.contains("/buscador") || ruta.contains("/filtros");
    }

    /**
     * Vuelve al origen del pedido. Si el origen es la misma página que se pidió con GET (por ejemplo, la pantalla de
     * edición de un registro que otro usuario dio de baja), redirigir a él repetiría el mismo rechazo sin fin: en ese
     * caso se vuelve a la ruta padre, la que resulta de quitar el sufijo {@code /{id}/editar} o {@code /{id}}, y a la
     * raíz si la ruta no termina en un identificador. La vista no expande llaves como variables de plantilla: una ruta
     * con llaves en la cabecera {@code Referer} se redirige tal cual, sin evaluarse.
     */
    private static RedirectView volverAlOrigen(HttpServletRequest request) {
        String ruta = resolverRutaRelativaDelReferer(request);
        if (ruta != null && esLaMismaPaginaPedida(request, ruta)) {
            ruta = rutaPadre(request.getRequestURI());
        }
        RedirectView vista = new RedirectView(ruta == null ? "/" : ruta, ruta == null);
        vista.setExpandUriTemplateVariables(false);
        return vista;
    }

    /**
     * Un GET cuyo destino de vuelta es él mismo (ruta y consulta) volvería a fallar igual al seguir la redirección.
     * Un POST no: la redirección lo convierte en un GET, que es otra petición.
     */
    private static boolean esLaMismaPaginaPedida(HttpServletRequest request, String ruta) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String consulta = request.getQueryString();
        String rutaPedida = consulta == null ? request.getRequestURI() : request.getRequestURI() + "?" + consulta;
        return ruta.equals(rutaPedida);
    }

    /**
     * Quita de la ruta el sufijo {@code /{id}/editar} o {@code /{id}} (con un identificador numérico). Devuelve
     * {@code null} si la ruta no termina en un identificador o si no queda una ruta padre.
     */
    static String rutaPadre(String ruta) {
        Matcher coincidencia = SUFIJO_DE_REGISTRO.matcher(ruta);
        if (!coincidencia.matches() || coincidencia.group(1).isEmpty()) {
            return null;
        }
        return coincidencia.group(1);
    }

    /**
     * Convierte la cabecera {@code Referer} en una ruta relativa (ruta y consulta, sin fragmento), solo si apunta al
     * mismo origen (esquema, servidor y puerto) que el pedido actual. Cualquier otro valor -ausente, mal formado, de
     * otro sitio o con credenciales- devuelve {@code null}, y se vuelve a la raíz: la cabecera la controla el cliente
     * y no puede usarse para redirigir a otro sitio.
     */
    static String resolverRutaRelativaDelReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(referer.trim());
            if (uri.getScheme() == null && uri.getHost() == null && uri.getAuthority() == null) {
                // Una ruta relativa ("/lotes?pagina=1") ya es del mismo sitio
                return rutaConConsultaSiEsSegura(uri);
            }
            if (uri.getScheme() == null || uri.getHost() == null || uri.getUserInfo() != null
                    || !uri.getScheme().equalsIgnoreCase(request.getScheme())
                    || !uri.getHost().equalsIgnoreCase(request.getServerName())
                    || puertoEfectivo(uri.getPort(), uri.getScheme()) != puertoEfectivo(request.getServerPort(), request.getScheme())) {
                return null;
            }
            return rutaConConsultaSiEsSegura(uri);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private static String rutaConConsultaSiEsSegura(URI uri) {
        String ruta = uri.getRawPath();
        if (ruta == null || !ruta.startsWith("/") || ruta.startsWith("//") || ruta.contains("\\")) {
            return null;
        }
        return uri.getRawQuery() == null ? ruta : ruta + "?" + uri.getRawQuery();
    }

    private static int puertoEfectivo(int puerto, String esquema) {
        if (puerto != -1) {
            return puerto;
        }
        return "https".equalsIgnoreCase(esquema) ? 443 : 80;
    }
}
