package com.github.heikyudev.maestrocervecero.persistence.entity.versionado;

/**
 * Contrato común de las entidades de "versión" que forman parte de un historial versionado
 * (por ejemplo {@code VersionProveedorEntity}, {@code VersionRecetaEntity}): cada vez que el
 * usuario modifica el agregado dueño de la versión, el service crea una instancia nueva en vez
 * de actualizar la existente, y marca {@code esUltimaVersion = true} en la nueva, dejando
 * {@code false} en la anterior — así los registros que ya referencian una versión vieja no se
 * ven afectados por cambios posteriores.
 * <p>
 * Existe para poder compartir, entre módulos que no tienen ninguna otra relación entre sí, la
 * lógica de "encontrar la versión activa" y "desactivar la versión anterior" sin reimplementarla
 * en cada uno (ver {@code MetodosVersionado}).
 * </p>
 * <p>
 * El estado (activo o de baja) vive en el agregado dueño de las versiones, por lo que un índice único de la tabla de
 * versión no puede consultarlo. Cada versión guarda entonces {@code padreActivo}: {@code true} mientras el dueño está
 * activo y {@code false} en todas sus versiones cuando se lo da de baja, de modo que los índices únicos de negocio
 * (que solo rigen para {@code esUltimaVersion = true AND padre_activo = true}) liberan los datos de un dueño dado de baja.
 * </p>
 */
public interface VersionadoEntity {

    boolean isEsUltimaVersion();

    void setEsUltimaVersion(boolean esUltimaVersion);

    boolean isPadreActivo();

    void setPadreActivo(boolean padreActivo);
}
