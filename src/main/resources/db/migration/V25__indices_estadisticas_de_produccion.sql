-- =============================================================================
-- V25 - Indices de las estadisticas de produccion (costos, tiempos por etapa y
-- consumo de insumos)
-- =============================================================================
-- Etapas de lote: los reportes "Costos de produccion" (etapa de envasado
-- finalizada) y "Tiempos por etapa" (etapas finalizadas) acotan las etapas por
-- estado y por fecha de finalizacion. Los indices existentes empiezan por el lote
-- o por el equipamiento y no sirven para ese rango.
--
-- Consumos de insumo: "Consumo de insumos" acota los consumos registrados por su
-- fecha de alta, y "Costos de produccion" suma los consumos registrados de las
-- etapas de cada lote. La tabla no tenia indices propios.
--
-- Detalles de costo directo: "Costos de produccion" suma los costos adicionales
-- de cada lote.
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_etapa_lote_estado_fecha_finalizacion ON etapa_lote (estado, fecha_finalizacion);

CREATE INDEX IF NOT EXISTS idx_consumo_insumo_estado_created_date ON consumo_insumo (estado, created_date);

CREATE INDEX IF NOT EXISTS idx_consumo_insumo_etapa_lote_estado ON consumo_insumo (etapa_lote_id, estado);

CREATE INDEX IF NOT EXISTS idx_detalle_costo_directo_lote ON detalle_costo_directo (lote_id);
