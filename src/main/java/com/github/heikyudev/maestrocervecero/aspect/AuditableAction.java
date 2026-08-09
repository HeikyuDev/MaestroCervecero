package com.github.heikyudev.maestrocervecero.aspect;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;

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
 *   {@code @AuditableAction(accion = AccionAuditoria.REGISTRAR, entidadAfectada = "UsuarioEntity")}
 *   public UsuarioEntity crear(UsuarioEntity usuario) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditableAction {

    // Qué tipo de operación de negocio representa el método (REGISTRAR, MODIFICAR, ELIMINAR, ANULAR).
    AccionAuditoria accion();

    // Nombre de la entidad/tabla afectada, para dejarlo asentado en la bitácora.
    String entidadAfectada();
}
