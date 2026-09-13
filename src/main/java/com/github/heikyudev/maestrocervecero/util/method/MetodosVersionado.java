package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.entity.versionado.VersionadoEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Clase de utilidades para el patrón de "historial versionado" ({@link VersionadoEntity}),
 * compartido por módulos sin ninguna otra relación entre sí (proveedor, receta): evita
 * reimplementar en cada uno la lógica de encontrar la versión activa o desactivar la anterior.
 */
public class MetodosVersionado {

    /**
     * Busca, entre una lista de versiones, la que está marcada como activa
     * ({@code esUltimaVersion = true}).
     *
     * @param versiones Las versiones sobre las que buscar.
     * @return La versión activa, o {@link Optional#empty()} si ninguna lo está.
     */
    public static <V extends VersionadoEntity> Optional<V> buscarVersionActiva(List<V> versiones) {
        return versiones.stream()
                .filter(VersionadoEntity::isEsUltimaVersion)
                .findFirst();
    }

    /**
     * Busca la versión activa de una lista de versiones, o lanza la excepción indicada si
     * ninguna lo está.
     *
     * @param versiones Las versiones sobre las que buscar.
     * @param exceptionSupplier Provee la excepción a lanzar si no hay ninguna versión activa.
     * @return La versión activa.
     */
    public static <V extends VersionadoEntity> V obtenerVersionActiva(List<V> versiones, Supplier<? extends RuntimeException> exceptionSupplier) {
        return buscarVersionActiva(versiones).orElseThrow(exceptionSupplier);
    }

    /**
     * Desactiva (pasa a {@code esUltimaVersion = false}) la versión actualmente activa de una
     * lista de versiones, si existe. Se usa antes de agregar una versión nueva al historial.
     *
     * @param versiones Las versiones sobre las que desactivar la activa.
     */
    public static void desactivarVersionAnterior(List<? extends VersionadoEntity> versiones) {
        versiones.stream()
                .filter(VersionadoEntity::isEsUltimaVersion)
                .forEach(version -> version.setEsUltimaVersion(false));
    }
}
