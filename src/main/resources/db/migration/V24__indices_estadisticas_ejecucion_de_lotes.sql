-- =============================================================================
-- V24 - Indices de las estadisticas de produccion (reporte "Ejecucion de lotes")
-- =============================================================================
-- Lotes: el reporte acota los lotes por fecha de inicio estimada, sin fijar el
-- estado, y los ordena por esa fecha y por id. El indice de V20 empieza por el
-- estado y no sirve para ese rango.
--
-- Envasados de lote: los litros envasados se suman por etapa de lote y estado
-- del envasado (el indice de V20 empieza por el barril).
-- =============================================================================

CREATE INDEX IF NOT EXISTS idx_lote_fecha_inicio_estimada_id ON lote (fecha_inicio_estimada, id);

CREATE INDEX IF NOT EXISTS idx_envasado_lote_etapa_lote_estado ON envasado_lote (etapa_lote_id, estado);
