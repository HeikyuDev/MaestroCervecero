-- =============================================================================
-- V6 - Indices unicos de proveedor y receta (modulos versionados)
-- =============================================================================
-- Proveedor y receta guardan sus datos en una tabla de versiones
-- (version_proveedor y version_receta): cada modificacion inserta una version
-- nueva con es_ultima_version = true y deja la anterior en false. La unicidad
-- rige solo entre registros activos y solo sobre la version vigente.
--
-- El estado (ACTIVO o BAJA) vive en la tabla padre (proveedor y receta), asi que
-- un indice parcial de la tabla de version no puede consultarlo. Por eso cada
-- version guarda padre_activo: true mientras el padre esta activo, y false en
-- todas sus versiones cuando se lo da de baja. El Service mantiene la columna
-- (true al crear una version, false al dar de baja el padre). Los indices de
-- negocio son parciales: WHERE es_ultima_version = true AND padre_activo = true.
-- Asi la razon social, el CUIT, el correo electronico y el nombre de un padre
-- dado de baja pueden volver a usarse.
--
-- Ademas, un indice unico por padre (WHERE es_ultima_version = true) garantiza
-- una sola version vigente por proveedor y por receta.
--
-- Proveedores: la razon social y el correo electronico se comparan ignorando
-- mayusculas y espacios en los extremos (lower(btrim(columna))); el CUIT se
-- guarda con sus 11 digitos, sin guiones. El telefono es opcional y no tiene
-- unicidad. Recetas: el nombre se compara ignorando mayusculas y espacios en los
-- extremos; los nombres de versiones historicas no cuentan.
--
-- Si las tablas ya tuvieran, entre proveedores o recetas activos y sobre su
-- version vigente, razones sociales, CUIT, correos electronicos o nombres
-- repetidos (ignorando mayusculas y espacios), o un padre con mas de una version
-- vigente, la creacion de un indice fallaria: hay que resolver los duplicados
-- antes de migrar.
-- =============================================================================

-- padre_activo: las versiones existentes toman el estado de su padre.
ALTER TABLE version_proveedor ADD COLUMN padre_activo boolean NOT NULL DEFAULT true;
ALTER TABLE version_receta ADD COLUMN padre_activo boolean NOT NULL DEFAULT true;

UPDATE version_proveedor v SET padre_activo = false FROM proveedor p WHERE p.id = v.proveedor_id AND p.estado <> 'ACTIVO';
UPDATE version_receta v SET padre_activo = false FROM receta r WHERE r.id = v.receta_id AND r.estado <> 'ACTIVO';

-- Se normalizan los datos existentes para que coincidan con lo que guarda el
-- Service: espacios repetidos y de los extremos colapsados, CUIT sin separadores
-- y correo en minusculas.
UPDATE version_proveedor SET
    razon_social = regexp_replace(btrim(razon_social), '[[:space:]]+', ' ', 'g'),
    nombre_comercial = regexp_replace(btrim(nombre_comercial), '[[:space:]]+', ' ', 'g'),
    direccion = regexp_replace(btrim(direccion), '[[:space:]]+', ' ', 'g'),
    cuit = regexp_replace(cuit, '[[:space:]./-]', '', 'g'),
    email = lower(btrim(email));
UPDATE version_receta SET nombre = regexp_replace(btrim(nombre), '[[:space:]]+', ' ', 'g');

-- Telefono opcional
ALTER TABLE version_proveedor ALTER COLUMN telefono DROP NOT NULL;

-- Una sola version vigente por padre
CREATE UNIQUE INDEX udx_version_proveedor_ultima_version ON version_proveedor (proveedor_id) WHERE es_ultima_version = true;
CREATE UNIQUE INDEX udx_version_receta_ultima_version ON version_receta (receta_id) WHERE es_ultima_version = true;

-- Proveedores
CREATE UNIQUE INDEX udx_version_proveedor_razon_social ON version_proveedor (lower(btrim(razon_social))) WHERE es_ultima_version = true AND padre_activo = true;
CREATE UNIQUE INDEX udx_version_proveedor_cuit ON version_proveedor (cuit) WHERE es_ultima_version = true AND padre_activo = true;
CREATE UNIQUE INDEX udx_version_proveedor_email ON version_proveedor (lower(btrim(email))) WHERE es_ultima_version = true AND padre_activo = true AND email IS NOT NULL;

-- Recetas
CREATE UNIQUE INDEX udx_version_receta_nombre ON version_receta (lower(btrim(nombre))) WHERE es_ultima_version = true AND padre_activo = true;
