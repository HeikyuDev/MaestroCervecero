-- =============================================================================
-- V14 - Reserva de insumo con historial
-- =============================================================================
-- Una reserva de insumo nunca se borra: conserva la cantidad reservada original
-- y lo que paso con ella.
--
--   cantidad_reservada           cantidad reservada ORIGINAL (no cambia).
--   cantidad_consumida           consumida de la reserva (acumulada; baja si se anula un consumo).
--   cantidad_liberada            saldo devuelto al stock al liberar la reserva sin consumirlo.
--   cantidad_reducida_por_ajuste cantidad que una merma (ajuste de insumo) le quito a la reserva.
--   cantidad_pendiente           saldo que todavia esta reservado.
--   estado                       ACTIVA (tiene saldo pendiente), CONSUMIDA (se uso todo) o
--                                LIBERADA (se cerro sin consumirse por completo).
--
-- Invariante: cantidad_reservada = cantidad_consumida + cantidad_liberada
--                                + cantidad_reducida_por_ajuste + cantidad_pendiente
--
-- Hasta ahora cantidad_reservada era el saldo, que bajaba al consumir. Las filas
-- existentes se reconstruyen: el saldo actual pasa a cantidad_pendiente y la cantidad
-- original es el saldo mas lo consumido sin anular (consumos reservados REGISTRADOS
-- de la misma etapa y del mismo lote de insumo).
-- =============================================================================

ALTER TABLE reserva_insumo ADD COLUMN cantidad_pendiente double precision;
ALTER TABLE reserva_insumo ADD COLUMN cantidad_consumida double precision;
ALTER TABLE reserva_insumo ADD COLUMN cantidad_liberada double precision;
ALTER TABLE reserva_insumo ADD COLUMN cantidad_reducida_por_ajuste double precision;
ALTER TABLE reserva_insumo ADD COLUMN estado varchar(255);

UPDATE reserva_insumo r
SET cantidad_pendiente = r.cantidad_reservada,
    cantidad_consumida = COALESCE((SELECT SUM(c.cantidad_consumida)
                                   FROM consumo_insumo c
                                   WHERE c.etapa_lote_id = r.etapa_lote_id
                                     AND c.lote_insumo_id = r.lote_insumo_id
                                     AND c.tipo_consumo = 'RESERVADO'
                                     AND c.estado = 'REGISTRADO'), 0),
    cantidad_liberada = 0,
    cantidad_reducida_por_ajuste = 0;

UPDATE reserva_insumo
SET cantidad_reservada = cantidad_pendiente + cantidad_consumida,
    estado = CASE WHEN cantidad_pendiente > 0.00005 THEN 'ACTIVA' ELSE 'CONSUMIDA' END;

ALTER TABLE reserva_insumo ALTER COLUMN cantidad_pendiente SET NOT NULL;
ALTER TABLE reserva_insumo ALTER COLUMN cantidad_consumida SET NOT NULL;
ALTER TABLE reserva_insumo ALTER COLUMN cantidad_liberada SET NOT NULL;
ALTER TABLE reserva_insumo ALTER COLUMN cantidad_reducida_por_ajuste SET NOT NULL;
ALTER TABLE reserva_insumo ALTER COLUMN estado SET NOT NULL;

ALTER TABLE reserva_insumo ADD CONSTRAINT ck_reserva_insumo_estado CHECK (estado IN ('ACTIVA','CONSUMIDA','LIBERADA'));
ALTER TABLE reserva_insumo ADD CONSTRAINT ck_reserva_insumo_cantidades CHECK (
    cantidad_reservada >= 0 AND cantidad_pendiente >= 0 AND cantidad_consumida >= 0
    AND cantidad_liberada >= 0 AND cantidad_reducida_por_ajuste >= 0);

-- Las reservas se consultan por etapa (trazabilidad de la pantalla del lote) y por lote de insumo (mermas)
CREATE INDEX idx_reserva_insumo_etapa_lote_id ON reserva_insumo (etapa_lote_id, estado);
CREATE INDEX idx_reserva_insumo_lote_insumo_id ON reserva_insumo (lote_insumo_id, estado);
