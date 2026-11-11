package com.github.heikyudev.maestrocervecero.configuration.web;

import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Set;

/**
 * Dialecto que expone {@code #cantidades} en las plantillas: el formato de {@link FormatoCantidad} disponible en
 * cualquier expresión, también en los atributos {@code th:data-*}, donde Thymeleaf no permite acceder a clases estáticas.
 * <p>
 * Uso: {@code #cantidades.formatear(valor)} ("1.847,3685") y {@code #cantidades.conUnidad(valor, 'kg')} ("4,5 kg").
 */
@Component
public class CantidadesDialect extends AbstractDialect implements IExpressionObjectDialect {

    private static final String NOMBRE = "Cantidades";
    private static final String OBJETO = "cantidades";

    private static final IExpressionObjectFactory FABRICA = new IExpressionObjectFactory() {
        @Override
        public Set<String> getAllExpressionObjectNames() {
            return Set.of(OBJETO);
        }

        @Override
        public Object buildObject(IExpressionContext context, String expressionObjectName) {
            return new Cantidades();
        }

        @Override
        public boolean isCacheable(String expressionObjectName) {
            return true;
        }
    };

    public CantidadesDialect() {
        super(NOMBRE);
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return FABRICA;
    }

    /** Objeto que las plantillas usan como {@code #cantidades}. */
    public static final class Cantidades {

        public String formatear(Number numero) {
            return FormatoCantidad.formatear(numero);
        }

        public String conUnidad(Number numero, String unidad) {
            return FormatoCantidad.conUnidad(numero, unidad);
        }
    }
}
