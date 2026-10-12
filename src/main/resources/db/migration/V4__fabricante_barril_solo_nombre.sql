-- =============================================================================
-- V4 - Fabricante de barril: solo nombre
-- =============================================================================
-- El fabricante solo indica el modelo de fabricacion del barril, por lo que
-- conserva unicamente el nombre. Se eliminan el nombre comercial, el CUIT, el
-- telefono y el correo.
--
-- El nombre es unico entre fabricantes activos, ignorando mayusculas y espacios
-- en los extremos. Si la tabla ya tuviera nombres repetidos entre fabricantes
-- activos, la creacion del indice fallaria: hay que resolver los duplicados
-- antes de migrar.
-- =============================================================================

ALTER TABLE fabricante_barril RENAME COLUMN razon_social TO nombre;

ALTER TABLE fabricante_barril
    DROP COLUMN nombre_comercial,
    DROP COLUMN cuit,
    DROP COLUMN telefono,
    DROP COLUMN email;

CREATE UNIQUE INDEX udx_fabricante_barril_nombre ON fabricante_barril (lower(btrim(nombre))) WHERE estado = 'ACTIVO';
