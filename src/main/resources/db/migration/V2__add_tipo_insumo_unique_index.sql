-- =============================================================================
-- V2 — Gestión de Insumos: columna discriminadora e índice único por nombre/tipo
-- =============================================================================
-- Contexto: el proyecto no utiliza herramienta de migraciones (Flyway/Liquibase);
-- Hibernate genera el schema con spring.jpa.hibernate.ddl-auto=create-drop.
-- Este script sigue la convención de nombres de migraciones versionadas y debe
-- ejecutarse MANUALMENTE (una sola vez) sobre los entornos con datos existentes
-- antes de desplegar la gestión de insumos (Malta, Lúpulo, Levadura).
--
-- 1) La columna discriminadora tipo_insumo la agrega Hibernate automáticamente
--    desde @DiscriminatorColumn(name = "tipo_insumo") en InsumoEntity; el ALTER
--    siguiente cubre las bases preexistentes creadas antes de este cambio.
-- 2) El índice único parcial garantiza a nivel base de datos que el nombre sea
--    único por tipo de insumo, case-insensitive, solo entre registros activos:
--    permite recrear un insumo cuyo nombre pertenecía a uno dado de baja lógica.
-- =============================================================================

ALTER TABLE insumo ADD COLUMN tipo_insumo VARCHAR(31);
CREATE UNIQUE INDEX udx_insumo_nombre_tipo ON insumo (LOWER(nombre), tipo_insumo) WHERE deleted = false;
