package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Módulos del sistema sobre los que la bitácora global de auditoría registra acciones.
 */
@Getter
@RequiredArgsConstructor
public enum ConceptoAuditoria {
    SESION("Sesión"),
    MALTA("Malta"),
    LUPULO("Lúpulo"),
    LEVADURA("Levadura"),
    MACERADOR("Macerador"),
    OLLA_DE_HERVOR("Olla de hervor"),
    MOLINO("Molino"),
    FERMENTADOR("Fermentador"),
    RECETA("Receta"),
    PLANIFICACION_PRODUCCION("Planificación de producción"),
    LOTE("Lote"),
    CONSUMO_INSUMO("Consumo de insumo"),
    ENVASADO_LOTE("Envasado de lote"),
    BARRIL("Barril"),
    FABRICANTE_BARRIL("Fabricante de barril"),
    FALLA_BARRIL("Falla de barril"),
    MANTENIMIENTO_BARRIL("Mantenimiento de barril"),
    LIMPIEZA_BARRIL("Limpieza de barril"),
    LIMPIEZA_EQUIPAMIENTO("Limpieza de equipamiento"),
    FALLA_EQUIPAMIENTO("Falla de equipamiento"),
    MANTENIMIENTO_EQUIPAMIENTO("Mantenimiento de equipamiento"),
    PRESENTACION_COMERCIAL("Presentación comercial"),
    PAIS("País"),
    PROVINCIA("Provincia"),
    LOCALIDAD("Localidad"),
    PROVEEDOR("Proveedor"),
    ORDEN_COMPRA("Orden de compra"),
    INGRESO_INSUMO("Ingreso de insumo"),
    COSTO_DIRECTO_ADICIONAL("Costo directo adicional"),
    COSTO_FIJO("Costo fijo"),
    CLIENTE("Cliente"),
    DESPACHO_BARRIL("Despacho de barril"),
    DEVOLUCION_BARRIL("Devolución de barril"),
    FRACCIONAMIENTO_BARRIL("Fraccionamiento de barril"),
    SOLICITUD_BUSQUEDA("Solicitud de búsqueda"),
    BUSQUEDA_BARRIL("Búsqueda de barril"),
    CONFIGURACION_PRODUCCION("Configuración de producción"),
    MOTIVO_AJUSTE("Motivo de ajuste"),
    AJUSTE_INSUMO("Ajuste de insumo"),
    USUARIO("Usuario");

    /** Nombre legible del módulo, el que se muestra al usuario. */
    private final String etiqueta;
}
