### 1. `filtrarLotesInsumoDisponibles(Long idInsumo)`

`idInsumo` lo elige el usuario entre los devueltos por `filtrarInsumosRequeridos`. Se usa para armar la lista de lotes de insumo físico entre los que el operario puede elegir al registrar un consumo directo (`registrarConsumoInsumoDirecto`). Filtra por `(cantidadActual - cantidadReservada) > 0` (cantidad disponible calculada, no persistida) y ordena por `fechaVencimiento` ascendente (FEFO), el mismo criterio que la reserva automática al iniciar un lote. No bloquea: es una consulta de solo lectura para mostrar la lista; el lote que el operario finalmente elija se vuelve a buscar bloqueado recién al confirmar el consumo.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLD-01**|Consulta con lotes disponibles|`idInsumo: 1L`, 2 lotes de insumo con cantidad disponible|`filtrarLotesInsumoDisponibles(1L)` contiene elementos|Retorna `List<LoteInsumoResponseDTO>` con los 2 elementos mapeados, en el mismo orden.|
|**CP-FLD-02**|Consulta sin lotes con cantidad disponible|`idInsumo: 1L`, sin lotes disponibles|`filtrarLotesInsumoDisponibles(1L)` está vacío|Retorna una lista vacía (no lanza excepción).|
