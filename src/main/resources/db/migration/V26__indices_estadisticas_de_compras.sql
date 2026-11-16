-- =============================================================================
-- V26 - Indices de las estadisticas de compras
-- =============================================================================
-- Ordenes de compra: los reportes "Compras del periodo", "Desempeno de
-- proveedores" y "Evolucion de precios" acotan las ordenes por su fecha de
-- emision (created_date), en cualquier estado. Los indices de V23 empiezan por
-- el estado o por la planificacion y no sirven para ese rango.
--
-- Ingresos de insumo: "Recepcion de insumos" acota los ingresos registrados por
-- su fecha de ingreso. El indice de V23 empieza por el item de compra.
--
-- Los items de cada orden ya tienen indice: el unico de V7 empieza por
-- orden_compra_id.
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_orden_compra_created_date ON orden_compra (created_date);

CREATE INDEX IF NOT EXISTS idx_ingreso_insumo_estado_fecha_ingreso ON ingreso_insumo (estado, fecha_ingreso);
