-- =============================================================================
-- V29 - Excedente maximo de un plan de produccion
-- =============================================================================
-- La configuracion de produccion guarda cuantos litros puede superar un plan de
-- produccion a la cantidad a producir de su planificacion al confirmarlo
-- (excedente_maximo_litros). La fila existente toma el valor por defecto de
-- 300 litros. El valor nunca es negativo.
-- =============================================================================

ALTER TABLE configuracion_produccion
    ADD COLUMN excedente_maximo_litros double precision NOT NULL DEFAULT 300;

ALTER TABLE configuracion_produccion
    ADD CONSTRAINT ck_configuracion_produccion_excedente_maximo_litros CHECK (excedente_maximo_litros >= 0);
