-- =============================================================================
-- V17 - Renombre del rol GERENTE_COMERCIAL a ENCARGADO_DE_DISTRIBUCION
-- =============================================================================
-- El sistema no maneja ventas: el rol se ocupa de los clientes y de los
-- despachos y las devoluciones de barriles. Solo cambia el valor de la columna
-- usuarios.rol y la lista permitida por su restriccion; no cambian tablas ni
-- columnas. Los registros historicos de auditoria no se modifican.
-- =============================================================================

ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_rol;

UPDATE usuarios SET rol = 'ENCARGADO_DE_DISTRIBUCION' WHERE rol = 'GERENTE_COMERCIAL';

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_rol CHECK (rol IN ('GERENTE_DE_PRODUCCION','OPERARIO_DE_PRODUCCION','GERENTE_DE_COMPRAS','ENCARGADO_DE_DEPOSITO','ENCARGADO_DE_DISTRIBUCION','ADMINISTRADOR'));
