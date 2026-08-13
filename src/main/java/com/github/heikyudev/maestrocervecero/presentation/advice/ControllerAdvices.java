package com.github.heikyudev.maestrocervecero.presentation.advice;

import com.github.heikyudev.maestrocervecero.service.exception.BaseException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Manejo global de excepciones para controladores MVC ({@link Controller}).
 * <p>
 * Cada handler traduce la excepción en un par {@code mensaje}/{@code tipo} vía Flash
 * Attributes -el mismo contrato que ya consume {@code login.html}- y redirige de vuelta
 * al origen de la petición (header {@code Referer}), con un fallback a la raíz del sitio.
 * </p>
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class ControllerAdvices {

    private static final String MENSAJE_ERROR_INESPERADO = "Ocurrió un error inesperado. Intentá nuevamente.";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String handleRecursoNoEncontrado(RecursoNoEncontradoException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.DANGER);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public String handleRecursoDuplicado(RecursoDuplicadoException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        log.warn("Recurso duplicado: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.DANGER);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public String handleReglaNegocio(ReglaNegocioException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.warn("Regla de negocio incumplida: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.WARNING);
    }

    /**
     * Red de seguridad para subclases de {@link BaseException} que no tengan un handler
     * específico propio.
     */
    @ExceptionHandler(BaseException.class)
    public String handleBaseException(BaseException ex,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        log.warn("Excepción de dominio sin handler específico: {}", ex.getMessage());
        return redirigirConMensaje(request, redirectAttributes, ex.getMessage(), TipoAlerta.DANGER);
    }


    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        log.error("Error inesperado procesando la petición a {}", request.getRequestURI(), ex);
        return redirigirConMensaje(request, redirectAttributes, MENSAJE_ERROR_INESPERADO, TipoAlerta.DANGER);
    }

    private String redirigirConMensaje(HttpServletRequest request,
                                        RedirectAttributes redirectAttributes,
                                        String mensaje,
                                        TipoAlerta tipo) {
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", tipo.getCodigo());
        return "redirect:" + resolveRedirectPath(request);
    }

    private String resolveRedirectPath(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? referer : "/";
    }
}
