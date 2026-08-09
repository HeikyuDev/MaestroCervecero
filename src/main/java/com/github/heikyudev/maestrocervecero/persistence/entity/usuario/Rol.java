package com.github.heikyudev.maestrocervecero.persistence.entity.usuario;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Roles funcionales del ERP. Cada uno trae "hardcodeado" su propio conjunto de
 * {@link Permiso} granulares: no es un rol vacío que se arma a mano desde una UI de
 * administración, es una decisión de código que refleja los puestos reales de la cervecería
 * (producción, compras, depósito, administración).
 * <p>
 * {@code UserDetailServiceImpl} vuelca el nombre del Rol como authority {@code ROLE_<nombre>}
 * y cada Permiso del set como authority propia sin prefijo (ver esa clase para el detalle).
 */
@Getter
@RequiredArgsConstructor
public enum Rol {
    GERENTE_DE_PRODUCCION(Set.of(
            Permiso.INSUMO_CREAR,
            Permiso.INSUMO_MODIFICAR,
            Permiso.INSUMO_ELIMINAR,
            Permiso.INSUMO_CONSULTAR,
            Permiso.EQUIPAMIENTO_CREAR,
            Permiso.EQUIPAMIENTO_MODIFICAR,
            Permiso.EQUIPAMIENTO_ELIMINAR,
            Permiso.EQUIPAMIENTO_CONSULTAR,
            Permiso.RECETA_CREAR,
            Permiso.RECETA_MODIFICAR,
            Permiso.RECETA_ELIMINAR,
            Permiso.RECETA_CONSULTAR,
            Permiso.ETAPA_CONTROL_CREAR,
            Permiso.ETAPA_CONTROL_MODIFICAR,
            Permiso.ETAPA_CONTROL_ELIMINAR,
            Permiso.ETAPA_CONTROL_CONSULTAR,
            Permiso.PARAMETRO_CONTROL_CREAR,
            Permiso.PARAMETRO_CONTROL_MODIFICAR,
            Permiso.PARAMETRO_CONTROL_ELIMINAR,
            Permiso.PARAMETRO_CONTROL_CONSULTAR,
            Permiso.ORDEN_PRODUCCION_REGISTRAR,
            Permiso.ORDEN_PRODUCCION_FINALIZAR,
            Permiso.ORDEN_PRODUCCION_ANULAR,
            Permiso.ORDEN_PRODUCCION_CONSULTAR,
            Permiso.LOTE_REGISTRAR,
            Permiso.LOTE_ANULAR,
            Permiso.LOTE_CONSULTAR,
            Permiso.BARRIL_CREAR,
            Permiso.BARRIL_MODIFICAR,
            Permiso.BARRIL_ELIMINAR,
            Permiso.BARRIL_CONSULTAR,
            Permiso.COSTO_DIRECTO_ADICIONAL_CREAR,
            Permiso.COSTO_DIRECTO_ADICIONAL_MODIFICAR,
            Permiso.COSTO_DIRECTO_ADICIONAL_ELIMINAR,
            Permiso.COSTO_DIRECTO_ADICIONAL_CONSULTAR,
            Permiso.COSTO_FIJO_CREAR,
            Permiso.COSTO_FIJO_MODIFICAR,
            Permiso.COSTO_FIJO_ELIMINAR,
            Permiso.COSTO_FIJO_CONSULTAR,
            Permiso.MERMA_INSUMO_REGISTRAR,
            Permiso.MERMA_INSUMO_ANULAR,
            Permiso.MERMA_INSUMO_CONSULTAR,
            Permiso.CLIENTE_CREAR,
            Permiso.CLIENTE_MODIFICAR,
            Permiso.CLIENTE_ELIMINAR,
            Permiso.CLIENTE_CONSULTAR
    )),

    OPERARIO_DE_PRODUCCION(Set.of(
            Permiso.EQUIPAMIENTO_CONSULTAR,
            Permiso.BARRIL_CONSULTAR,
            Permiso.LOTE_INICIAR,
            Permiso.LOTE_CONSULTAR,
            Permiso.LOTE_FINALIZAR_MOLIENDA,
            Permiso.LOTE_FINALIZAR_MACERACION,
            Permiso.LOTE_FINALIZAR_HERVIDO,
            Permiso.LOTE_FINALIZAR_FERMENTACION,
            Permiso.LOTE_FINALIZAR_MADURACION,
            Permiso.LOTE_FINALIZAR_ENVASADO,
            Permiso.MEDICION_REGISTRAR,
            Permiso.MEDICION_ANULAR,
            Permiso.MEDICION_CONSULTAR,
            Permiso.CONSUMO_INSUMO_REGISTRAR,
            Permiso.CONSUMO_INSUMO_ANULAR,
            Permiso.CONSUMO_INSUMO_CONSULTAR,
            Permiso.ENVASADO_REGISTRAR,
            Permiso.ENVASADO_ANULAR,
            Permiso.ENVASADO_CONSULTAR,
            Permiso.LIMPIEZA_EQUIPAMIENTO_REGISTRAR,
            Permiso.LIMPIEZA_EQUIPAMIENTO_CONSULTAR,
            Permiso.LIMPIEZA_EQUIPAMIENTO_ANULAR,
            Permiso.DESPACHO_BARRIL_REGISTRAR,
            Permiso.DESPACHO_BARRIL_ANULAR,
            Permiso.DESPACHO_BARRIL_CONSULTAR,
            Permiso.DEVOLUCION_BARRIL_REGISTRAR,
            Permiso.DEVOLUCION_BARRIL_ANULAR,
            Permiso.DEVOLUCION_BARRIL_CONSULTAR,
            Permiso.FRACCIONAMIENTO_BARRIL_REGISTRAR,
            Permiso.FRACCIONAMIENTO_BARRIL_CONSULTAR,
            Permiso.FRACCIONAMIENTO_BARRIL_ANULAR
    )),

    GERENTE_DE_COMPRAS(Set.of(
            Permiso.PRESENTACION_COMERCIAL_CREAR,
            Permiso.PRESENTACION_COMERCIAL_MODIFICAR,
            Permiso.PRESENTACION_COMERCIAL_ELIMINAR,
            Permiso.PRESENTACION_COMERCIAL_CONSULTAR,
            Permiso.LOCALIDAD_CONSULTAR,
            Permiso.PROVEEDOR_CREAR,
            Permiso.PROVEEDOR_MODIFICAR,
            Permiso.PROVEEDOR_ELIMINAR,
            Permiso.PROVEEDOR_CONSULTAR,
            Permiso.ORDEN_COMPRA_REGISTRAR,
            Permiso.ORDEN_COMPRA_ANULAR,
            Permiso.ORDEN_COMPRA_FINALIZAR,
            Permiso.ORDEN_COMPRA_CONSULTAR,
            Permiso.ORDEN_PRODUCCION_CONSULTAR
    )),

    ENCARGADO_DE_DEPOSITO(Set.of(
            Permiso.INGRESO_INSUMO_REGISTRAR,
            Permiso.INGRESO_INSUMO_ANULAR,
            Permiso.INGRESO_INSUMO_CONSULTAR,
            Permiso.CONSUMO_INSUMO_CONSULTAR,
            Permiso.ORDEN_COMPRA_CONSULTAR
    )),

    ADMINISTRADOR(Set.of(
            Permiso.PAIS_CREAR,
            Permiso.PAIS_MODIFICAR,
            Permiso.PAIS_ELIMINAR,
            Permiso.PAIS_CONSULTAR,
            Permiso.PROVINCIA_CREAR,
            Permiso.PROVINCIA_MODIFICAR,
            Permiso.PROVINCIA_ELIMINAR,
            Permiso.PROVINCIA_CONSULTAR,
            Permiso.LOCALIDAD_CREAR,
            Permiso.LOCALIDAD_MODIFICAR,
            Permiso.LOCALIDAD_ELIMINAR,
            Permiso.LOCALIDAD_CONSULTAR,
            Permiso.USUARIO_CREAR,
            Permiso.USUARIO_MODIFICAR,
            Permiso.USUARIO_ELIMINAR,
            Permiso.USUARIO_CONSULTAR
    ));

    private final Set<Permiso> permisos;
}