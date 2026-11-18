-- =============================================================================
-- V28 - Eliminar la busqueda de barril
-- =============================================================================
-- El sistema no registra busquedas de barril. Se elimina la tabla
-- busqueda_barril (con su indice unico parcial y su clave foranea a
-- solicitud_busqueda) y el concepto BUSQUEDA_BARRIL de la bitacora de
-- auditoria. La solicitud de busqueda se mantiene.
-- =============================================================================

DROP TABLE busqueda_barril;

DELETE FROM audit_log WHERE concepto_auditoria = 'BUSQUEDA_BARRIL';

ALTER TABLE audit_log DROP CONSTRAINT ck_audit_log_concepto_auditoria;

ALTER TABLE audit_log ADD CONSTRAINT ck_audit_log_concepto_auditoria CHECK (concepto_auditoria IN ('SESION','MALTA','LUPULO','LEVADURA','MACERADOR','OLLA_DE_HERVOR','MOLINO','FERMENTADOR','RECETA','PLANIFICACION_PRODUCCION','LOTE','CONSUMO_INSUMO','ENVASADO_LOTE','BARRIL','FABRICANTE_BARRIL','FALLA_BARRIL','MANTENIMIENTO_BARRIL','LIMPIEZA_BARRIL','LIMPIEZA_EQUIPAMIENTO','FALLA_EQUIPAMIENTO','MANTENIMIENTO_EQUIPAMIENTO','PRESENTACION_COMERCIAL','PAIS','PROVINCIA','LOCALIDAD','PROVEEDOR','ORDEN_COMPRA','INGRESO_INSUMO','COSTO_DIRECTO_ADICIONAL','COSTO_FIJO','CLIENTE','DESPACHO_BARRIL','DEVOLUCION_BARRIL','FRACCIONAMIENTO_BARRIL','SOLICITUD_BUSQUEDA','CONFIGURACION_PRODUCCION','MOTIVO_AJUSTE','AJUSTE_INSUMO','USUARIO'));
