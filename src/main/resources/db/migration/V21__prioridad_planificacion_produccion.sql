-- =============================================================================
-- V21 - Prioridad de la planificacion de produccion
--
-- Cada planificacion tiene una prioridad (BAJA, MEDIA o ALTA) que define el lugar de
-- sus lotes nuevos en la cola de pendientes de un fermentador. Las planificaciones
-- existentes quedan con prioridad MEDIA.
-- =============================================================================

ALTER TABLE planificacion_produccion
    ADD COLUMN prioridad varchar(255) NOT NULL DEFAULT 'MEDIA';

ALTER TABLE planificacion_produccion
    ADD CONSTRAINT ck_planificacion_produccion_prioridad CHECK (prioridad in ('BAJA','MEDIA','ALTA'));
