package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Columna de la tabla de un reporte de estadísticas: su título, su tipo de dato (que define la alineación) y, en una
 * columna de tipo {@link TipoColumna#ENLACE}, la ruta del botón ("Abrir" o "Ver") y el permiso que lo habilita. Una celda de
 * enlace sin ID no muestra el botón.
 */
@Getter
@AllArgsConstructor
public final class ColumnaReporteResponseDTO {

    private final String titulo;

    private final TipoColumna tipo;

    /**
     * Ruta del destino del botón "Abrir", con el marcador {@code {id}} que se reemplaza por el ID de la celda (por
     * ejemplo, {@code /lotes/{id}}); {@code null} si la columna no es de tipo {@link TipoColumna#ENLACE}.
     */
    private final String ruta;

    /** Permiso que habilita el botón, o {@code null} si no exige ninguno. */
    private final String permiso;

    /**
     * {@code true} si el botón es "Ver" (ícono de ojo, para un detalle de solo lectura); {@code false} si es "Abrir" (la
     * entrada a una pantalla de trabajo).
     */
    private final boolean ver;

    /** Definición de la columna que se muestra con el tooltip del encabezado, o {@code null} si no lleva. */
    private final String ayuda;

    /** Una columna de datos del tipo indicado. */
    public static ColumnaReporteResponseDTO de(String titulo, TipoColumna tipo) {
        return new ColumnaReporteResponseDTO(titulo, tipo, null, null, false, null);
    }

    /** Una columna de datos del tipo indicado, con su definición en el tooltip del encabezado. */
    public static ColumnaReporteResponseDTO de(String titulo, TipoColumna tipo, String ayuda) {
        return new ColumnaReporteResponseDTO(titulo, tipo, null, null, false, ayuda);
    }

    /** La columna "Acciones" con el botón "Abrir" de cada fila hacia la ruta indicada, habilitado por el permiso. */
    public static ColumnaReporteResponseDTO enlace(String titulo, String ruta, String permiso) {
        return new ColumnaReporteResponseDTO(titulo, TipoColumna.ENLACE, ruta, permiso, false, null);
    }

    /** La columna "Acciones" con el botón "Ver" (detalle de solo lectura) de cada fila hacia la ruta indicada, habilitado por el permiso. */
    public static ColumnaReporteResponseDTO ver(String titulo, String ruta, String permiso) {
        return new ColumnaReporteResponseDTO(titulo, TipoColumna.ENLACE, ruta, permiso, true, null);
    }

    /** {@code true} si la columna se imprime: todas menos la de acciones. */
    public boolean isImprimible() {
        return tipo != TipoColumna.ENLACE;
    }
}
