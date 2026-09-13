### 1. `filtrarLotesInsumoReservados(Long idEtapaLote, Long idInsumo)`

`idEtapaLote` no lo tipea el usuario: lo determina el contexto de la pantalla (igual que en `filtrarInsumosRequeridos`/`filtrarMedicionesLote`). `idInsumo` sí lo elige el usuario, al seleccionar uno de los insumos devueltos por `filtrarInsumosRequeridos`.

Devuelve TODAS las reservas de ese insumo en esa etapa, incluidas las que ya se agotaron (cantidad reservada en 0, por un consumo o por una merma de `AjusteInsumo`): mostrar solo las que todavía tienen cantidad, u ordenarlas primero, es una decisión de presentación del frontend, no un filtro de este método.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLR-01**|Consulta con reservas existentes|`idEtapaLote: 1L`, `idInsumo: 1L`, 2 reservas de esa etapa para ese insumo (`8.0` y `4.0`)|`filtrarLotesInsumoReservados(1L, 1L)` contiene elementos|Retorna `List<ReservaInsumoResponseDTO>` con los 2 elementos mapeados, en el mismo orden.|
|**CP-FLR-02**|Consulta sin reservas para ese insumo en esa etapa|`idEtapaLote: 1L`, `idInsumo: 1L`, sin reservas|`filtrarLotesInsumoReservados(1L, 1L)` está vacío|Retorna una lista vacía (no lanza excepción).|
