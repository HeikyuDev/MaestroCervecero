-- =============================================================================
-- V23 - Indices de las consultas de los paneles de control
-- =============================================================================
-- Ordenes de compra: las pendientes se ordenan y se cuentan por fecha de entrega
-- estimada, las finalizadas se acotan por fecha de finalizacion, ambas por
-- estado, y las planificaciones sin orden buscan las ordenes de cada
-- planificacion por su estado.
--
-- Lotes de insumo: los vencidos se filtran por fecha de vencimiento.
--
-- Despachos de barril: los vigentes se acotan y se ordenan por fecha de
-- devolucion estimada (el estado del despacho vive en operacion_ciclo_vida_barril,
-- que usa el indice idx_operacion_ciclo_vida_barril_barril_estado_fecha de V20).
--
-- Ingresos de insumo: el avance de recepcion de cada orden busca los ingresos de
-- sus items.
--
-- Etapas de lote: los lotes por etapa en curso buscan las etapas de cada lote por
-- estado y tipo.
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_orden_compra_estado_fecha_entrega_estimada ON orden_compra (estado, fecha_entrega_estimada);

CREATE INDEX IF NOT EXISTS idx_orden_compra_estado_fecha_finalizacion ON orden_compra (estado, fecha_finalizacion);

CREATE INDEX IF NOT EXISTS idx_orden_compra_planificacion_produccion_estado ON orden_compra (planificacion_produccion_id, estado);

CREATE INDEX IF NOT EXISTS idx_lote_insumo_fecha_vencimiento ON lote_insumo (fecha_vencimiento);

CREATE INDEX IF NOT EXISTS idx_despacho_barril_fecha_devolucion_estimada ON despacho_barril (fecha_devolucion_estimada);

CREATE INDEX IF NOT EXISTS idx_ingreso_insumo_detalle_compra_estado ON ingreso_insumo (detalle_compra_id, estado);

CREATE INDEX IF NOT EXISTS idx_etapa_lote_lote_estado_etapa ON etapa_lote (lote_id, estado, etapa);
