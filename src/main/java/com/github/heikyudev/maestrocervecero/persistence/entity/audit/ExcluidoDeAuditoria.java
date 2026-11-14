package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un campo persistente que nunca debe copiarse al detalle de auditoría (los estados anterior y posterior
 * en JSON de la bitácora): contraseñas, hashes, tokens y cualquier otro dato secreto.
 * <p>
 * Es la forma explícita de excluir un campo. Como defensa adicional, el serializador también omite todo campo cuyo
 * nombre contenga alguno de los fragmentos de {@code SerializadorEstadoEntidad#FRAGMENTOS_SENSIBLES}, aunque
 * no lleve esta anotación.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcluidoDeAuditoria {
}
