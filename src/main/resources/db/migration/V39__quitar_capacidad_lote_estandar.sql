-- =============================================================================
-- V39 - Configuracion de produccion sin capacidad de lote estandar
-- =============================================================================
-- La configuracion de produccion no guarda una capacidad de lote estandar: la
-- planificacion de produccion no estima su fecha de finalizacion y ningun otro
-- proceso usa ese valor. Se elimina la columna capacidad_lote_estandar.
-- =============================================================================

ALTER TABLE configuracion_produccion DROP COLUMN IF EXISTS capacidad_lote_estandar;
