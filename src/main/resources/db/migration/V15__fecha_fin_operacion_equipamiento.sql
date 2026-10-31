-- =============================================================================
-- V15 - Fecha de fin en las operaciones de ciclo de vida del equipamiento
-- =============================================================================
-- Cada limpieza, falla o mantenimiento de un equipamiento ocupa un intervalo:
--
--   fecha     pasa a significar la fecha y hora de INICIO de la operacion.
--   fecha_fin fecha y hora de FIN de la operacion (nunca anterior al inicio).
--
-- Las operaciones existentes se registraron con un unico instante, asi que su fin
-- se completa con ese mismo instante (fecha_fin = fecha) y la columna queda
-- obligatoria. La tabla se comparte solo con las operaciones de equipamiento; las
-- operaciones de barril tienen su propia tabla y no se modifican.
--
-- El indice acelera las consultas por equipamiento (ultima operacion registrada,
-- historial y linea de tiempo).
-- =============================================================================

ALTER TABLE operacion_ciclo_vida_equipamiento ADD COLUMN fecha_fin timestamp(6);

UPDATE operacion_ciclo_vida_equipamiento SET fecha_fin = fecha;

ALTER TABLE operacion_ciclo_vida_equipamiento ALTER COLUMN fecha_fin SET NOT NULL;

ALTER TABLE operacion_ciclo_vida_equipamiento
    ADD CONSTRAINT ck_operacion_ciclo_vida_equipamiento_fechas CHECK (fecha_fin >= fecha);

CREATE INDEX idx_operacion_ciclo_vida_equipamiento_equipamiento
    ON operacion_ciclo_vida_equipamiento (equipamiento_id, estado, fecha_fin);

CREATE INDEX idx_etapa_lote_equipamiento ON etapa_lote (equipamiento_id);
