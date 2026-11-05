-- =============================================================================
-- V18 - Estado En uso del barril, fecha del envasado y litros descartados
-- =============================================================================
-- Control de Barriles, modelo y envasado del lote:
--
--   barril.estado_operativo       admite EN_USO: el barril esta siendo llenado por
--                                 un lote en ejecucion y no admite operaciones
--                                 hasta que el lote se finalice.
--   envasado_lote.fecha_envasado  fecha y hora del envasado, obligatoria; es la
--                                 fecha de participacion del barril en el lote.
--   devolucion_barril.litros_descartados
--                                 litros que tenia el barril al volver y que se
--                                 descartaron al registrar la devolucion.
--
-- Datos existentes:
--   - fecha_envasado se completa con la fecha de registro del envasado
--     (created_date) y, si falta, con el inicio de su etapa o la ultima
--     modificacion del registro.
--   - litros_descartados queda en 0 en las devoluciones existentes.
--   - Los barriles con un envasado registrado de un lote que sigue en ejecucion,
--     que conservan intacto el contenido de ese envasado y no tuvieron ninguna
--     operacion posterior, pasan de CON_CERVEZA a EN_USO.
-- =============================================================================

ALTER TABLE barril DROP CONSTRAINT ck_barril_estado_operativo;

ALTER TABLE barril ADD CONSTRAINT ck_barril_estado_operativo
    CHECK (estado_operativo IN ('CON_CERVEZA','DESPACHADO','DISPONIBLE','EN_LIMPIEZA','EN_MANTENIMIENTO','EN_USO'));

ALTER TABLE envasado_lote ADD COLUMN fecha_envasado timestamp(6);

UPDATE envasado_lote e
   SET fecha_envasado = COALESCE(
           e.created_date,
           (SELECT et.fecha_inicio FROM etapa_lote et WHERE et.id = e.etapa_lote_id),
           e.last_modified_date,
           CURRENT_TIMESTAMP);

ALTER TABLE envasado_lote ALTER COLUMN fecha_envasado SET NOT NULL;

ALTER TABLE devolucion_barril ADD COLUMN litros_descartados double precision NOT NULL DEFAULT 0;

UPDATE barril b
   SET estado_operativo = 'EN_USO'
 WHERE b.estado_operativo = 'CON_CERVEZA'
   AND EXISTS (SELECT 1
                 FROM envasado_lote e
                 JOIN etapa_lote et ON et.id = e.etapa_lote_id
                 JOIN lote l ON l.id = et.lote_id
                WHERE e.barril_id = b.id
                  AND e.estado = 'REGISTRADO'
                  AND l.estado = 'EN_EJECUCION'
                  AND e.cantidad_envasada = b.contenido_actual
                  AND NOT EXISTS (SELECT 1
                                    FROM operacion_ciclo_vida_barril o
                                   WHERE o.barril_id = b.id
                                     AND o.estado = 'REGISTRADO'
                                     AND o.fecha > e.fecha_envasado));
