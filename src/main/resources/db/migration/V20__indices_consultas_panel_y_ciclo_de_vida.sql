-- =============================================================================
-- V20 - Indices de las consultas del panel del operario y del ciclo de vida
-- =============================================================================
-- Panel del operario: los lotes en ejecucion se ordenan por fecha de
-- finalizacion estimada y los lotes a iniciar se filtran y ordenan por fecha de
-- inicio estimada, ambos acotados por estado.
--
-- Ciclo de vida: la ultima operacion registrada de cada barril se busca por
-- barril, estado y fecha, y el ultimo envasado de un barril, por barril, estado y
-- fecha de envasado. La de cada equipo usa el indice
-- idx_operacion_ciclo_vida_equipamiento_equipamiento (V16).
--
-- Ajustes de insumo: el listado se filtra por estado y se ordena por fecha de
-- registro.
-- =============================================================================

CREATE INDEX idx_lote_estado_fecha_inicio_estimada ON lote (estado, fecha_inicio_estimada);

CREATE INDEX idx_lote_estado_fecha_finalizacion_estimada ON lote (estado, fecha_finalizacion_estimada);

CREATE INDEX idx_envasado_lote_barril_estado_fecha_envasado ON envasado_lote (barril_id, estado, fecha_envasado);

CREATE INDEX idx_operacion_ciclo_vida_barril_barril_estado_fecha ON operacion_ciclo_vida_barril (barril_id, estado, fecha);

CREATE INDEX idx_ajuste_insumo_estado_created_date ON ajuste_insumo (estado, created_date);
