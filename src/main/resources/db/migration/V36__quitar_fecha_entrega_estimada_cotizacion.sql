-- =============================================================================
-- V36 - Fecha estimada de entrega fuera de la cotizacion del proveedor
-- =============================================================================
-- El proveedor cotiza solo precio y cantidad por presentacion. La fecha
-- estimada de entrega la indica el gerente de compras en la orden de compra
-- que confirma a partir de la orden propuesta por el plan.
-- =============================================================================

ALTER TABLE invitacion_cotizacion DROP COLUMN fecha_entrega_estimada;
