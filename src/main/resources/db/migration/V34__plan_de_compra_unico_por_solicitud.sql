-- =============================================================================
-- V34 - Un solo plan de ordenes de compra por solicitud de cotizacion
-- =============================================================================
-- Cada solicitud de cotizacion genera un unico plan de ordenes de compra, el
-- mejor segun el criterio de la configuracion de compras:
--
--   configuracion_compras   sin cantidad_planes: quedan el plazo de respuesta,
--                           el criterio y el peso del precio
--   plan_compra             a lo sumo un plan por solicitud (unico por
--                           solicitud_cotizacion_id), sin numero; estados
--                           PROPUESTO, ELEGIDO y CANCELADO
--   solicitud_cotizacion    el estado PLANES_GENERADOS pasa a PLAN_GENERADO y
--                           fecha_generacion_planes a fecha_generacion_plan
--
-- Datos existentes: de cada solicitud se conserva un solo plan, el ELEGIDO si
-- lo hay; si no, el que tiene alguna orden propuesta vinculada a una orden de
-- compra; y si no, el de menor numero (el mejor segun el criterio). Los demas
-- planes (los DESCARTADO y los alternativos que nunca se eligieron) se borran
-- con sus ordenes propuestas e items: ninguno tiene ordenes de compra
-- confirmadas, porque confirmar una orden elige su plan.
-- =============================================================================

CREATE TEMPORARY TABLE plan_compra_conservado AS
SELECT DISTINCT ON (p.solicitud_cotizacion_id) p.id
FROM plan_compra p
ORDER BY p.solicitud_cotizacion_id,
         (p.estado = 'ELEGIDO') DESC,
         EXISTS (SELECT 1 FROM orden_compra_propuesta o WHERE o.plan_compra_id = p.id AND o.orden_compra_id IS NOT NULL) DESC,
         p.numero ASC;

DELETE FROM item_orden_compra_propuesta i
USING orden_compra_propuesta o
WHERE i.orden_compra_propuesta_id = o.id
  AND o.plan_compra_id NOT IN (SELECT id FROM plan_compra_conservado);

DELETE FROM orden_compra_propuesta
WHERE plan_compra_id NOT IN (SELECT id FROM plan_compra_conservado);

DELETE FROM plan_compra
WHERE id NOT IN (SELECT id FROM plan_compra_conservado);

DROP TABLE plan_compra_conservado;

-- plan_compra: un plan por solicitud, sin numero y sin el estado DESCARTADO
ALTER TABLE plan_compra DROP CONSTRAINT uk_plan_compra_solicitud_numero;

ALTER TABLE plan_compra DROP COLUMN numero;

ALTER TABLE plan_compra ADD CONSTRAINT uk_plan_compra_solicitud_cotizacion_id UNIQUE (solicitud_cotizacion_id);

ALTER TABLE plan_compra DROP CONSTRAINT ck_plan_compra_estado;

ALTER TABLE plan_compra ADD CONSTRAINT ck_plan_compra_estado
    CHECK (estado IN ('PROPUESTO','ELEGIDO','CANCELADO'));

-- solicitud_cotizacion: estado y fecha del plan generado
ALTER TABLE solicitud_cotizacion DROP CONSTRAINT ck_solicitud_cotizacion_estado;

UPDATE solicitud_cotizacion SET estado = 'PLAN_GENERADO' WHERE estado = 'PLANES_GENERADOS';

ALTER TABLE solicitud_cotizacion ADD CONSTRAINT ck_solicitud_cotizacion_estado
    CHECK (estado IN ('PENDIENTE_DE_CONFIRMACION','ABIERTA','CERRADA','PLAN_GENERADO','CANCELADA'));

ALTER TABLE solicitud_cotizacion RENAME COLUMN fecha_generacion_planes TO fecha_generacion_plan;

-- configuracion_compras: sin cantidad de planes
ALTER TABLE configuracion_compras DROP CONSTRAINT ck_configuracion_compras_cantidad_planes;

ALTER TABLE configuracion_compras DROP COLUMN cantidad_planes;
