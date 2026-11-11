-- =============================================================================
-- V19 - Indice sobre ajuste_insumo.motivo_ajuste_id
-- =============================================================================
-- Gestion de motivos de ajuste: antes de modificar el tipo de un motivo o de
-- darlo de baja se consulta si tiene algun ajuste de insumo asociado (registrado
-- o anulado). El indice evita recorrer toda la tabla ajuste_insumo en esa
-- consulta y al resolver la clave foranea fk_ajuste_insumo_motivo_ajuste_id.
-- =============================================================================

CREATE INDEX idx_ajuste_insumo_motivo_ajuste_id ON ajuste_insumo (motivo_ajuste_id);
