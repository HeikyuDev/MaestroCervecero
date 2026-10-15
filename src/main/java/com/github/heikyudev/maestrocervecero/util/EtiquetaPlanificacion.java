package com.github.heikyudev.maestrocervecero.util;

/**
 * Single place that builds the label of a production plan ("Plan #7 - Pale Ale"). The response DTOs expose it as {@code getEtiqueta()}, so controllers and templates show the same text on every screen.
 */
public final class EtiquetaPlanificacion {

    private EtiquetaPlanificacion() {
    }

    /**
     * @param id           The plan id (may be {@code null}).
     * @param nombreReceta The recipe name (may be {@code null} or blank).
     * @return "Plan #id - nombreReceta", "Plan #id" without a recipe name, or {@code null} without an id.
     */
    public static String de(Long id, String nombreReceta) {
        if (id == null) {
            return null;
        }
        return "Plan #" + id + (nombreReceta != null && !nombreReceta.isBlank() ? " - " + nombreReceta : "");
    }
}
