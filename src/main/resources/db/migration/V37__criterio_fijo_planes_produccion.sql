-- =============================================================================
-- V37 - Criterio fijo de los planes de produccion
-- =============================================================================
-- La generacion automatica de planes de produccion usa un unico criterio por
-- modalidad: el plan secuencial elige el fermentador de mayor capacidad
-- efectiva y el plan concurrente, la combinacion de menor cantidad de lotes.
-- La configuracion de produccion no guarda criterios de seleccion: se quitan
-- sus columnas y sus restricciones de valores. IF EXISTS permite aplicarla
-- sobre una base de desarrollo armada con un esquema sin esas columnas.
-- =============================================================================

ALTER TABLE configuracion_produccion
    DROP CONSTRAINT IF EXISTS ck_configuracion_produccion_criterio_seleccion_plan_secuencial;

ALTER TABLE configuracion_produccion
    DROP CONSTRAINT IF EXISTS ck_configuracion_produccion_criterio_seleccion_plan_concurrente;

ALTER TABLE configuracion_produccion
    DROP COLUMN IF EXISTS criterio_seleccion_plan_secuencial;

ALTER TABLE configuracion_produccion
    DROP COLUMN IF EXISTS criterio_seleccion_plan_concurrente;
