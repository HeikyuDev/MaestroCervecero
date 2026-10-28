
-- =============================================================================
-- V11 - Identificador interno generado por el sistema y numero de serie del barril
-- =============================================================================
-- El identificador interno de los equipos (molino, macerador, olla de hervor y
-- fermentador) y de los barriles deja de ser tipeado por el usuario: lo genera
-- el sistema al dar de alta, de forma incremental por tipo y sin reutilizar
-- nunca un numero, ni siquiera tras una baja (MOL-01, MAC-01, OLL-01, FER-01 y
-- BRL-01, con dos digitos como minimo).
--
-- Se crea una secuencia por prefijo. Las secuencias no son transaccionales: un
-- alta revertida puede dejar un hueco en la numeracion, lo cual es aceptado.
--
-- Equipamiento: cada secuencia arranca despues del mayor sufijo numerico que
-- tenga un identificador existente con ese prefijo (sin distinguir mayusculas,
-- incluidos los dados de baja), o en 1 si no hay ninguno. Los equipos
-- existentes conservan su identificador sin cambios.
--
-- Barril: el identificador que tipeaba el usuario pasa a ser el numero de serie
-- (obligatorio, unico entre los barriles activos del mismo fabricante) y la
-- columna identificador pasa a ser identificador_interno, generado. Los barriles
-- existentes reciben BRL-01, BRL-02... segun su id y la secuencia arranca en la
-- cantidad de barriles mas uno.
-- =============================================================================

-- Secuencias de equipamiento
CREATE SEQUENCE seq_identificador_mol;
CREATE SEQUENCE seq_identificador_mac;
CREATE SEQUENCE seq_identificador_oll;
CREATE SEQUENCE seq_identificador_fer;

SELECT setval('seq_identificador_mol', COALESCE((SELECT max(substring(identificador_interno from '[0-9]+$')::bigint) FROM equipamiento WHERE identificador_interno ~* '^MOL-[0-9]{1,15}$'), 0) + 1, false);
SELECT setval('seq_identificador_mac', COALESCE((SELECT max(substring(identificador_interno from '[0-9]+$')::bigint) FROM equipamiento WHERE identificador_interno ~* '^MAC-[0-9]{1,15}$'), 0) + 1, false);
SELECT setval('seq_identificador_oll', COALESCE((SELECT max(substring(identificador_interno from '[0-9]+$')::bigint) FROM equipamiento WHERE identificador_interno ~* '^OLL-[0-9]{1,15}$'), 0) + 1, false);
SELECT setval('seq_identificador_fer', COALESCE((SELECT max(substring(identificador_interno from '[0-9]+$')::bigint) FROM equipamiento WHERE identificador_interno ~* '^FER-[0-9]{1,15}$'), 0) + 1, false);

-- Barril: el identificador tipeado pasa a numero de serie
DROP INDEX IF EXISTS udx_barril_identificador;

ALTER TABLE barril ADD COLUMN numero_serie varchar(255);
UPDATE barril SET numero_serie = identificador;
ALTER TABLE barril ALTER COLUMN numero_serie SET NOT NULL;

-- Barril: la columna identificador pasa a ser el identificador interno generado
ALTER TABLE barril RENAME COLUMN identificador TO identificador_interno;

UPDATE barril
SET identificador_interno = 'BRL-' || CASE WHEN numerado.numero < 10 THEN '0' || numerado.numero ELSE numerado.numero::text END
FROM (SELECT id, row_number() OVER (ORDER BY id) AS numero FROM barril) AS numerado
WHERE barril.id = numerado.id;

CREATE SEQUENCE seq_identificador_brl;
SELECT setval('seq_identificador_brl', (SELECT count(*) FROM barril) + 1, false);

-- El numero de serie es unico dentro de su fabricante entre los barriles activos
-- (sin distinguir mayusculas); el identificador interno es unico entre todos los
-- barriles porque nunca se reutiliza.
CREATE UNIQUE INDEX udx_barril_numero_serie ON barril (fabricante_id, lower(btrim(numero_serie))) WHERE estado = 'ACTIVO';
CREATE UNIQUE INDEX udx_barril_identificador_interno ON barril (identificador_interno);
