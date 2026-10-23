package com.github.heikyudev.maestrocervecero.configuration.web;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.templatemode.TemplateMode;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Dialecto que muestra todo {@code <input type="number">} con su valor sin ceros sobrantes (50.0 se muestra como 50 y
 * 4.5 como 4.5, con punto decimal porque así lo exige el input numérico de HTML).
 * <p>
 * Se ejecuta después de {@code th:field} y {@code th:value}, por lo que no cambia el enlace del campo (name, id,
 * errores) ni el valor rechazado: un texto que no es un número (por ejemplo "abc" o "1,5") queda tal cual se escribió.
 */
@Component
public class NumeroSinCerosDialect extends AbstractProcessorDialect {

    private static final String NOMBRE = "Numero sin ceros";
    private static final String PREFIJO = "numerosinceros";

    public NumeroSinCerosDialect() {
        super(NOMBRE, PREFIJO, StandardDialect.PROCESSOR_PRECEDENCE + 1000);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new InputNumericoProcessor(dialectPrefix));
    }

    /**
     * Quita los ceros sobrantes de un número escrito; un texto que no es un número finito se devuelve sin cambios.
     */
    static String sinCerosSobrantes(String texto) {
        try {
            double numero = Double.parseDouble(texto.trim());
            if (Double.isNaN(numero) || Double.isInfinite(numero)) {
                return texto;
            }
            return BigDecimal.valueOf(numero).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return texto;
        }
    }

    private static final class InputNumericoProcessor extends AbstractElementTagProcessor {

        InputNumericoProcessor(String dialectPrefix) {
            super(TemplateMode.HTML, dialectPrefix, "input", false, "type", false, 0);
        }

        @Override
        protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
            if (!"number".equalsIgnoreCase(tag.getAttributeValue("type"))) {
                return;
            }
            String valor = tag.getAttributeValue("value");
            if (valor == null || valor.isBlank()) {
                return;
            }
            String limpio = sinCerosSobrantes(valor);
            if (!limpio.equals(valor)) {
                structureHandler.setAttribute("value", limpio);
            }
        }
    }
}
