package com.github.heikyudev.maestrocervecero.service.aspect;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de un @Service como "auditable": {@link AuditAspect} lo intercepta
 * y, cuando el método termina bien, deja una entrada en la bitácora global.
 * <p>
 * Uso típico:
 * <pre>
     *   {@code @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = "INSUMO")}
 *   public UsuarioEntity crear(UsuarioEntity usuario) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableAction {

    // Propiedades que estas obligado a pasarle a la anotacion cuando la utilices

    // Qué tipo de operación de negocio representa el método (REGISTRAR, MODIFICAR, ELIMINAR, ANULAR, LOGIN, LOGOUT).
    AccionAuditoria accion();

    // Nombre del concepto Afectado, para dejarlo asentado en la bitácora.
    ConceptoAuditoria conceptoAuditoria();
}
