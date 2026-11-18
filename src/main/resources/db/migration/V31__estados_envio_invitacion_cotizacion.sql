-- =============================================================================
-- V31 - Estados del envio de las invitaciones a cotizar
-- =============================================================================
-- El correo de una invitacion se envia fuera de la transaccion que bloquea la
-- solicitud: primero se guarda el token del intento (EN_ENVIO), despues se
-- envia y por ultimo se registra el resultado (ENVIADA o FALLIDA). Una
-- invitacion de un proveedor dado de baja o sin version vigente queda OMITIDA
-- (estado final: el proceso programado deja de reintentarla).
-- =============================================================================

ALTER TABLE invitacion_cotizacion DROP CONSTRAINT ck_invitacion_cotizacion_estado_envio;

ALTER TABLE invitacion_cotizacion ADD CONSTRAINT ck_invitacion_cotizacion_estado_envio
    CHECK (estado_envio IN ('PENDIENTE','EN_ENVIO','ENVIADA','FALLIDA','OMITIDA'));
