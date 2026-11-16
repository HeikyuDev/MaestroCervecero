package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Permiso;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Pestañas de la pantalla "Estadísticas" ({@code /estadisticas?pestana=<clave>}), en el orden en que se muestran. Cada
 * pestaña es un reporte: la ve quien tiene su permiso, y su contenido lo arma el reporte que la
 * declara. Vive junto a los servicios porque su descripción (la pregunta que responde el reporte) es la que cada Service
 * devuelve con su reporte. Las de producción exigen {@code ESTADISTICA_PRODUCCION_CONSULTAR} y las de compras
 * {@code ESTADISTICA_COMPRAS_CONSULTAR}.
 */
@Getter
@RequiredArgsConstructor
public enum PestanaEstadistica {

    LOTES("lotes", "Lotes", "bi-collection", "Cuántos lotes se planificaron en el período y cómo terminaron",
            Permiso.ESTADISTICA_PRODUCCION_CONSULTAR),
    COSTOS("costos", "Costos", "bi-cash-coin", "Cuánto costó producir cada litro en el período",
            Permiso.ESTADISTICA_PRODUCCION_CONSULTAR),
    TIEMPOS("tiempos", "Tiempos", "bi-hourglass-split", "Cuánto duran las etapas frente a lo estimado",
            Permiso.ESTADISTICA_PRODUCCION_CONSULTAR),
    INSUMOS("insumos", "Insumos", "bi-basket", "Qué insumos se consumieron y cuánto se gastó en ellos",
            Permiso.ESTADISTICA_PRODUCCION_CONSULTAR),
    EQUIPAMIENTO("equipamiento", "Equipamiento", "bi-tools", "Cuánto se usó cada equipo y cuántas intervenciones tuvo",
            Permiso.ESTADISTICA_PRODUCCION_CONSULTAR),
    COMPRAS("compras", "Compras", "bi-cart3", "Cuánto se compró y a qué proveedores",
            Permiso.ESTADISTICA_COMPRAS_CONSULTAR),
    PROVEEDORES("proveedores", "Proveedores", "bi-truck", "Qué proveedores entregan lo pedido y en fecha",
            Permiso.ESTADISTICA_COMPRAS_CONSULTAR),
    PRECIOS("precios", "Precios", "bi-graph-up-arrow", "Cómo variaron los precios de los insumos comprados",
            Permiso.ESTADISTICA_COMPRAS_CONSULTAR),
    RECEPCION("recepcion", "Recepción", "bi-box-arrow-in-down", "Qué se recibió en el período y qué falta recibir",
            Permiso.ESTADISTICA_COMPRAS_CONSULTAR);

    /** Clave de la pestaña en la URL ({@code ?pestana=}) y en el nombre del reporte impreso. */
    private final String clave;
    /** Texto de la pestaña. */
    private final String etiqueta;
    /** Ícono de Bootstrap Icons de la pestaña. */
    private final String icono;
    /**
     * La pregunta que responde el reporte, en palabras simples y sin punto final: se muestra debajo de las pestañas
     * aunque los criterios no sean válidos. Es la misma descripción que devuelve el Service con el reporte.
     */
    private final String descripcion;
    /** Permiso que habilita la pestaña y su reporte impreso. */
    private final Permiso permiso;

    /**
     * Resuelve la pestaña a partir de su clave en la URL.
     *
     * @param clave La clave pedida, sin distinguir mayúsculas.
     * @return La pestaña, o vacío si la clave es nula o no existe.
     */
    public static Optional<PestanaEstadistica> deClave(String clave) {
        if (clave == null) {
            return Optional.empty();
        }
        String buscada = clave.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(p -> p.clave.equals(buscada)).findFirst();
    }
}
