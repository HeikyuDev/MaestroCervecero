-- =============================================================================
-- V9 - Razon social de proveedor sin unicidad
-- =============================================================================
-- El CUIT es el identificador unico de un proveedor: dos proveedores activos
-- pueden tener la misma razon social siempre que su CUIT sea distinto. Se
-- elimina el indice unico de la razon social de version_proveedor.
--
-- El CUIT (udx_version_proveedor_cuit) y el correo electronico
-- (udx_version_proveedor_email) siguen siendo unicos entre las versiones
-- vigentes de proveedores activos.
-- =============================================================================

DROP INDEX IF EXISTS udx_version_proveedor_razon_social;
