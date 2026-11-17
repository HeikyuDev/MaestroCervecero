package com.github.heikyudev.maestrocervecero.configuration.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Verifica al arrancar en producción que la dirección pública con la que se arman los enlaces de los correos
 * ({@code app.url-base}, variable de entorno {@code APP_URL_BASE}) use HTTPS: un enlace con token por HTTP viajaría sin
 * cifrar. Si no la usa, la aplicación no arranca.
 */
@Component
@Profile("prod")
public class ValidacionUrlBaseProduccion {

    static final String PREFIJO_REQUERIDO = "https://";

    public ValidacionUrlBaseProduccion(@Value("${app.url-base}") String urlBase) {
        validar(urlBase);
    }

    /**
     * Valida la dirección pública de producción.
     *
     * @param urlBase Valor de {@code app.url-base}.
     * @throws IllegalStateException Si es nula, está en blanco o no empieza con {@code https://}.
     */
    static void validar(String urlBase) {
        if (urlBase == null || urlBase.isBlank() || !urlBase.trim().regionMatches(true, 0, PREFIJO_REQUERIDO, 0, PREFIJO_REQUERIDO.length())) {
            throw new IllegalStateException("La propiedad app.url-base (variable de entorno APP_URL_BASE) debe empezar con "
                    + PREFIJO_REQUERIDO + " en producción. Valor actual: '" + urlBase + "'");
        }
    }
}
