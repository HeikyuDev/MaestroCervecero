-- =============================================================================
-- V32 - Ofertas por presentacion y fecha estimada de entrega en la cotizacion
-- =============================================================================
-- Un proveedor puede ofrecer un mismo insumo en varias presentaciones de su
-- catalogo a la vez (por ejemplo, 2 bolsas de 25 kg y 3 bolsas de 10 kg): la
-- oferta pasa a ser unica por invitacion, linea y presentacion (item del
-- catalogo) en lugar de por invitacion y linea.
--
-- El proveedor indica en su respuesta la fecha en que estima entregar lo que
-- ofrece: una sola por invitacion, porque cada proveedor da lugar a una sola
-- orden de compra con una sola fecha. Es nula mientras no respondio o si
-- respondio sin ofertas.
-- =============================================================================

ALTER TABLE oferta_cotizacion DROP CONSTRAINT uk_oferta_cotizacion_invitacion_linea;

ALTER TABLE oferta_cotizacion ADD CONSTRAINT uk_oferta_cotizacion_invitacion_linea_catalogo
    UNIQUE (invitacion_cotizacion_id, linea_solicitud_cotizacion_id, catalogo_proveedor_id);

ALTER TABLE invitacion_cotizacion ADD COLUMN fecha_entrega_estimada date;
