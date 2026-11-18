-- =============================================================================
-- V33 - Ofertas sin stock en la cotizacion
-- =============================================================================
-- El proveedor puede indicar cantidad disponible 0 en una presentacion para
-- decir explicitamente que no tiene stock. Esa fila se guarda como oferta sin
-- precio: la cantidad pasa a admitir 0 y el precio es obligatorio (mayor a 0)
-- solo cuando la cantidad es mayor a 0; con cantidad 0 el precio es nulo. Los
-- planes de ordenes de compra no usan las ofertas sin stock.
-- =============================================================================

ALTER TABLE oferta_cotizacion DROP CONSTRAINT ck_oferta_cotizacion_cantidad_disponible;

ALTER TABLE oferta_cotizacion DROP CONSTRAINT ck_oferta_cotizacion_precio_unitario;

ALTER TABLE oferta_cotizacion ALTER COLUMN precio_unitario DROP NOT NULL;

ALTER TABLE oferta_cotizacion ADD CONSTRAINT ck_oferta_cotizacion_cantidad_disponible
    CHECK (cantidad_disponible >= 0);

ALTER TABLE oferta_cotizacion ADD CONSTRAINT ck_oferta_cotizacion_precio_unitario
    CHECK ((cantidad_disponible > 0 AND precio_unitario IS NOT NULL AND precio_unitario > 0)
        OR (cantidad_disponible = 0 AND precio_unitario IS NULL));
