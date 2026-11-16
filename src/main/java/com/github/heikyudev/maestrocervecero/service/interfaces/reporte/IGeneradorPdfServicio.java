package com.github.heikyudev.maestrocervecero.service.interfaces.reporte;

import java.util.Map;

/**
 * Genera documentos PDF a partir de plantillas Thymeleaf. Lo usan todos los reportes impresos del sistema: cada uno
 * aporta su plantilla y su modelo (los reportes de estadísticas comparten la plantilla {@code estadisticas/pdf/reporte}).
 */
public interface IGeneradorPdfServicio {

    /** Familia tipográfica que deben declarar las plantillas de los reportes impresos (se incrusta en cada documento). */
    String FUENTE = "Liberation Sans";

    /**
     * Procesa una plantilla con su modelo y convierte el resultado a PDF.
     * <p>
     * Precondiciones: la plantilla existe y produce XHTML bien formado (toda etiqueta cerrada); no usa expresiones que
     * dependan de la petición web (enlaces {@code @{...}} ni atributos {@code sec:}).
     * </p>
     * <p>
     * Postcondiciones: devuelve el contenido de un documento PDF con la fuente del sistema de reportes incrustada; no
     * guarda nada en disco ni modifica datos.
     * </p>
     *
     * @param plantilla Nombre de la plantilla Thymeleaf (por ejemplo, {@code estadisticas/pdf/reporte}).
     * @param modelo Variables que usa la plantilla.
     * @return El contenido del PDF.
     * @throws IllegalStateException Si la plantilla no se puede convertir a PDF.
     */
    byte[] generar(String plantilla, Map<String, Object> modelo);
}
