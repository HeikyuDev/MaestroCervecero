-- =============================================================================
-- V2 - Datos iniciales
-- =============================================================================
-- Datos minimos que todo ambiente necesita para funcionar.
--
-- configuracion_produccion es una entidad singleton: existe una unica fila con
-- id fijo (ConfiguracionProduccionEntity.SINGLETON_ID = 1) que el gerente de
-- produccion solo modifica, nunca inserta ni elimina.
--
-- El usuario administrador no se crea aqui: lo crea UserDataLoader al arrancar
-- la aplicacion, porque la contrasena se almacena con el PasswordEncoder.
-- =============================================================================

insert into configuracion_produccion (
    id,
    capacidad_lote_estandar,
    velocidad_estandar_envasado,
    porcentaje_minimo_consumo_avanzar_etapa,
    criterio_seleccion_plan_secuencial,
    criterio_seleccion_plan_concurrente,
    created_by,
    created_date
) values (
    1,
    300.0,
    100.0,
    80.0,
    'FERMENTADOR_LIBERACION_MAS_TEMPRANA',
    'EQUIPOS_LIBERACION_MAS_TEMPRANA',
    'sistema',
    current_timestamp
);
