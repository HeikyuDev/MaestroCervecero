-- =============================================================================
-- V38 - Finalizacion de la solicitud de cotizacion y de su plan de compra
-- =============================================================================
-- plan_compra.estado:          PROPUESTO, FINALIZADO y CANCELADO. El plan queda
--                              FINALIZADO cuando ya no tiene ordenes propuestas
--                              pendientes (todas confirmadas o descartadas).
-- solicitud_cotizacion.estado: se agrega FINALIZADA, que la solicitud con el
--                              plan generado toma al no quedar ordenes
--                              propuestas pendientes.
--
-- Datos existentes: un plan ELEGIDO pasa a FINALIZADO si ya no tiene ordenes
-- propuestas pendientes y a PROPUESTO si todavia tiene alguna. Una solicitud con
-- el plan generado cuyo plan no tiene ordenes propuestas pendientes pasa a
-- FINALIZADA (su plan queda FINALIZADO).
-- =============================================================================

ALTER TABLE plan_compra DROP CONSTRAINT ck_plan_compra_estado;

ALTER TABLE solicitud_cotizacion DROP CONSTRAINT ck_solicitud_cotizacion_estado;

UPDATE plan_compra p
SET estado = CASE
        WHEN EXISTS (SELECT 1 FROM orden_compra_propuesta o
                     WHERE o.plan_compra_id = p.id AND o.estado = 'PROPUESTA') THEN 'PROPUESTO'
        ELSE 'FINALIZADO'
    END
WHERE p.estado = 'ELEGIDO';

UPDATE solicitud_cotizacion s
SET estado = 'FINALIZADA'
WHERE s.estado = 'PLAN_GENERADO'
  AND NOT EXISTS (SELECT 1 FROM plan_compra p
                  JOIN orden_compra_propuesta o ON o.plan_compra_id = p.id
                  WHERE p.solicitud_cotizacion_id = s.id AND o.estado = 'PROPUESTA');

UPDATE plan_compra p
SET estado = 'FINALIZADO'
WHERE p.estado = 'PROPUESTO'
  AND EXISTS (SELECT 1 FROM solicitud_cotizacion s
              WHERE s.id = p.solicitud_cotizacion_id AND s.estado = 'FINALIZADA');

ALTER TABLE plan_compra ADD CONSTRAINT ck_plan_compra_estado
    CHECK (estado IN ('PROPUESTO','FINALIZADO','CANCELADO'));

ALTER TABLE solicitud_cotizacion ADD CONSTRAINT ck_solicitud_cotizacion_estado
    CHECK (estado IN ('PENDIENTE_DE_CONFIRMACION','ABIERTA','CERRADA','PLAN_GENERADO','FINALIZADA','CANCELADA'));
