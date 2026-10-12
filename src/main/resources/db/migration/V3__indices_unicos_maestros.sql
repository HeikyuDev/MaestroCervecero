-- =============================================================================
-- V3 - Indices unicos de los maestros simples
-- =============================================================================
-- La unicidad rige solo entre registros activos, por eso los indices son
-- parciales (WHERE estado = 'ACTIVO'): el nombre de un registro dado de baja
-- puede volver a usarse.
--
-- Los nombres, la razon social y el correo se comparan ignorando mayusculas
-- (lower(btrim(columna))). Los codigos e identificadores se guardan en
-- mayusculas y normalizados por el Service, por lo que el indice va sobre la
-- columna. El CUIT se guarda con sus 11 digitos, sin guiones.
--
-- Si las tablas ya tuvieran duplicados entre registros activos, la creacion de
-- un indice fallaria: hay que resolver los duplicados antes de migrar.
-- =============================================================================

-- Insumos: el nombre es unico por tipo (una malta y un lupulo pueden llamarse
-- igual). El nombre, el estado y el discriminador viven en la tabla base.
CREATE UNIQUE INDEX udx_insumo_nombre ON insumo (tipo_insumo, lower(btrim(nombre))) WHERE estado = 'ACTIVO';

-- Equipamiento: el identificador interno es unico entre los cuatro tipos
-- (fermentador, molino, macerador y olla de hervor).
CREATE UNIQUE INDEX udx_equipamiento_identificador_interno ON equipamiento (identificador_interno) WHERE estado = 'ACTIVO';

-- Ubicaciones
CREATE UNIQUE INDEX udx_pais_nombre ON pais (lower(btrim(nombre))) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX udx_provincia_nombre ON provincia (pais_id, lower(btrim(nombre))) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX udx_localidad_nombre ON localidad (provincia_id, lower(btrim(nombre))) WHERE estado = 'ACTIVO';

-- Otros maestros de nombre unico
CREATE UNIQUE INDEX udx_presentacion_comercial_nombre ON presentacion_comercial (lower(btrim(nombre))) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX udx_motivo_ajuste_nombre ON motivo_ajuste (lower(btrim(nombre))) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX udx_costo_directo_adicional_nombre ON costo_directo_adicional (lower(btrim(nombre))) WHERE estado = 'ACTIVO';

-- Barril: el identificador es unico dentro de su fabricante.
CREATE UNIQUE INDEX udx_barril_identificador ON barril (fabricante_id, identificador) WHERE estado = 'ACTIVO';
