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
 */
public interface VersionadoEntity {

    boolean isEsUltimaVersion();

    void setEsUltimaVersion(boolean esUltimaVersion);
}
