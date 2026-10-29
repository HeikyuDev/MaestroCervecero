-- =============================================================================
-- V13 - CUIT de proveedor con formato y lote de insumo sin distinguir mayusculas
-- =============================================================================
-- CUIT de proveedor: pasa a guardarse con el formato XX-XXXXXXXX-X. Los CUIT de
-- 11 digitos existentes (guardados solo con digitos) se formatean. El indice
-- unico udx_version_proveedor_cuit sigue valiendo: dos CUIT con los mismos
-- digitos dan el mismo valor formateado, asi que no aparecen duplicados nuevos.
--
-- Lote de insumo: la identificacion del lote del proveedor se guarda tal como
-- figura en el envase (conserva las mayusculas). La identidad del lote
-- (insumo + identificacion) se compara ignorando mayusculas y espacios en los
-- extremos (lower(btrim(...))), igual que el numero de serie del barril. La
-- restriccion unica sensible a mayusculas se reemplaza por un indice unico
-- funcional. Como hasta ahora las identificaciones se guardaban en mayusculas,
-- los lotes existentes no tienen duplicados al ignorar el caso.
-- =============================================================================

UPDATE version_proveedor
SET cuit = substring(cuit from 1 for 2) || '-' || substring(cuit from 3 for 8) || '-' || substring(cuit from 11 for 1)
WHERE cuit ~ '^[0-9]{11}$';

-- Si algun insumo tuviera lotes cuyas identificaciones difieren solo en mayusculas o en espacios de los extremos,
-- la migracion se detiene con un mensaje que lo indica (hay que unificar esos lotes antes de migrar).
DO $$
DECLARE
    repetidos text;
BEGIN
    SELECT string_agg('insumo ' || insumo_id || ': ' || identificacion, '; ') INTO repetidos
    FROM (SELECT insumo_id, lower(btrim(identificacion_lote_proveedor)) AS identificacion
          FROM lote_insumo
          GROUP BY insumo_id, lower(btrim(identificacion_lote_proveedor))
          HAVING count(*) > 1) duplicados;
    IF repetidos IS NOT NULL THEN
        RAISE EXCEPTION 'No se puede migrar: hay lotes de insumo con la misma identificacion ignorando mayusculas (%)', repetidos;
    END IF;
END $$;

ALTER TABLE lote_insumo DROP CONSTRAINT uk_lote_insumo_insumo_id_identificacion_lote_proveedor;
CREATE UNIQUE INDEX udx_lote_insumo_insumo_identificacion ON lote_insumo (insumo_id, lower(btrim(identificacion_lote_proveedor)));
