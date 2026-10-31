-- =============================================================================
-- V16 - Una sola fecha y hora por operacion de equipamiento
--
-- Cada limpieza, falla y mantenimiento tiene una unica fecha y hora (la columna
-- fecha, el momento en que se realizo la operacion). Se elimina fecha_fin y lo que
-- dependia de ella: su CHECK y el indice por (equipamiento, estado, fecha_fin),
-- que se vuelve a crear sobre fecha.
-- =============================================================================

DROP INDEX IF EXISTS idx_operacion_ciclo_vida_equipamiento_equipamiento;

ALTER TABLE operacion_ciclo_vida_equipamiento DROP CONSTRAINT IF EXISTS ck_operacion_ciclo_vida_equipamiento_fechas;

ALTER TABLE operacion_ciclo_vida_equipamiento DROP COLUMN IF EXISTS fecha_fin;

CREATE INDEX idx_operacion_ciclo_vida_equipamiento_equipamiento
    ON operacion_ciclo_vida_equipamiento (equipamiento_id, estado, fecha);
