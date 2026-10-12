-- =============================================================================
-- V7 - Indices unicos de catalogo, orden de compra, busqueda de barril y receta
-- =============================================================================
-- Cada indice es la garantia final de una regla que el Service ya valida antes
-- de guardar:
--
-- - Un catalogo de proveedor (los items de una version de proveedor) no repite
--   el mismo insumo con la misma presentacion comercial.
-- - Una orden de compra no repite el mismo item del catalogo.
-- - Una solicitud de busqueda tiene una sola busqueda registrada. El indice es
--   parcial (WHERE estado = 'REGISTRADO'): una busqueda anulada no cuenta y se
--   puede registrar otra sobre la misma solicitud.
-- - Una version de receta no repite la misma malta ni la misma levadura, y no
--   repite un mismo lupulo con el mismo uso y el mismo tiempo de hervor (el
--   tiempo de hervor se guarda en 0 cuando el uso no es HERVOR). El mismo lupulo
--   puede figurar con distinto uso o distinto tiempo de hervor.
--
-- Si las tablas ya tuvieran filas repetidas, la creacion de un indice fallaria:
-- hay que resolver los duplicados antes de migrar.
-- =============================================================================

CREATE UNIQUE INDEX udx_catalogo_proveedor_item ON catalogo_proveedor (version_proveedor_id, insumo_id, presentacion_comercial_id);

CREATE UNIQUE INDEX udx_detalle_compra_item ON detalle_compra (orden_compra_id, catalogo_proveedor_id);

CREATE UNIQUE INDEX udx_busqueda_barril_solicitud ON busqueda_barril (solicitud_busqueda_id) WHERE estado = 'REGISTRADO';

CREATE UNIQUE INDEX udx_detalle_malta_version_malta ON detalle_malta (version_receta_id, malta_id);
CREATE UNIQUE INDEX udx_detalle_lupulo_version_lupulo_uso_tiempo ON detalle_lupulo (version_receta_id, lupulo_id, uso, tiempo_de_hervor);
CREATE UNIQUE INDEX udx_detalle_levadura_version_levadura ON detalle_levadura (version_receta_id, levadura_id);
