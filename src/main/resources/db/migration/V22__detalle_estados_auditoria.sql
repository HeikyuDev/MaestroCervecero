-- =============================================================================
-- V22 - Detalle de estados de la bitacora de auditoria
--
-- Cada entrada de la bitacora puede guardar, en JSON, el estado del registro afectado antes y
-- despues de la operacion (estado_anterior y estado_posterior). Son texto y admiten NULL: las
-- entradas existentes, las de inicio y cierre de sesion y las operaciones que no modifican el
-- registro quedan sin detalle.
-- =============================================================================

ALTER TABLE audit_log
    ADD COLUMN estado_anterior text,
    ADD COLUMN estado_posterior text;
