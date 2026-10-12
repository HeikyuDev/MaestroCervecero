-- =============================================================================
-- V5 - Indices unicos de usuario y cliente
-- =============================================================================
-- La unicidad rige solo entre registros activos, por eso los indices son
-- parciales (WHERE estado = 'ACTIVO'): el nombre de usuario y el correo de un
-- registro dado de baja pueden volver a usarse.
--
-- Usuarios: el nombre de usuario y el correo se guardan en minusculas y sin
-- espacios en los extremos. La restriccion global uk_usuarios_username se
-- reemplaza por un indice parcial que solo considera usuarios activos.
-- Clientes: el correo es unico entre clientes activos; el telefono no tiene
-- unicidad.
-- El telefono es opcional en ambas tablas.
--
-- Si las tablas ya tuvieran nombres de usuario o correos repetidos entre
-- registros activos (ignorando mayusculas y espacios en los extremos), la
-- creacion de un indice fallaria: hay que resolver los duplicados antes de
-- migrar.
-- =============================================================================

-- Se normalizan los datos existentes para que coincidan con lo que guarda el
-- Service y con el nombre de usuario que se normaliza en el inicio de sesion.
UPDATE usuarios SET username = lower(btrim(username)), correo = lower(btrim(correo));
UPDATE cliente SET email = lower(btrim(email));

-- Telefono opcional
ALTER TABLE usuarios ALTER COLUMN telefono DROP NOT NULL;
ALTER TABLE cliente ALTER COLUMN telefono DROP NOT NULL;

-- Usuarios
ALTER TABLE usuarios DROP CONSTRAINT uk_usuarios_username;
CREATE UNIQUE INDEX udx_usuarios_username ON usuarios (lower(btrim(username))) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX udx_usuarios_correo ON usuarios (lower(btrim(correo))) WHERE estado = 'ACTIVO';

-- Clientes
CREATE UNIQUE INDEX udx_cliente_email ON cliente (lower(btrim(email))) WHERE estado = 'ACTIVO';
