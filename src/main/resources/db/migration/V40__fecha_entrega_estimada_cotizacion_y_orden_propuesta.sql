-- =============================================================================
-- V40 - Fecha estimada de entrega del proveedor en la cotizacion
-- =============================================================================
-- El proveedor indica, junto con su cotizacion, la fecha en que estima
-- entregar lo que ofrece: una por respuesta, porque cada proveedor da lugar a
-- una sola orden de compra. Se exige si ofrece alguna presentacion con stock.
--
-- - invitacion_cotizacion.fecha_entrega_estimada: la fecha de la respuesta
--   actual del proveedor; nula si no respondio o no ofrece nada con stock.
-- - orden_compra_propuesta.fecha_entrega_estimada: la fecha del proveedor
--   copiada al generar el plan; prearma la orden de compra que se confirma.
-- =============================================================================

ALTER TABLE invitacion_cotizacion ADD COLUMN fecha_entrega_estimada date;
ALTER TABLE orden_compra_propuesta ADD COLUMN fecha_entrega_estimada date;
