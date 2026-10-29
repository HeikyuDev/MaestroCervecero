### 1. `filtrarLotesInsumoReservados(Long idEtapaLote, Long idInsumo)`

`idEtapaLote` no lo tipea el usuario: lo determina el contexto de la pantalla (igual que en `filtrarInsumosRequeridos`). `idInsumo` es opcional: con un insumo devuelve sus reservas en la etapa y con `null` devuelve las de todos los insumos de la etapa, en una sola consulta.

Devuelve TODAS las reservas pedidas, incluidas las que ya se agotaron (cantidad reservada en 0, por un consumo o por una merma de `AjusteInsumo`). Vienen ordenadas con las que tienen saldo primero y las agotadas al final, y dentro de cada grupo por nombre de insumo, fecha de vencimiento del lote de insumo e `id`. Cada reserva trae su lote de insumo con la marca `vencido` y su insumo cargados en la misma consulta.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLR-01**|Consulta con reservas existentes|`idEtapaLote: 1L`, `idInsumo: 1L`, 2 reservas de esa etapa para ese insumo (`8.0` y `4.0`)|`filtrarLotesInsumoReservados(1L, 1L)` contiene elementos|Retorna `List<ReservaInsumoResponseDTO>` con los 2 elementos mapeados, en el mismo orden.|
|**CP-FLR-02**|Consulta sin reservas para ese insumo en esa etapa|`idEtapaLote: 1L`, `idInsumo: 1L`, sin reservas|`filtrarLotesInsumoReservados(1L, 1L)` está vacío|Retorna una lista vacía (no lanza excepción).|
|**CP-FLR-03**|Insumo nulo: todas las reservas de la etapa, con el lote de insumo vencido marcado|`idEtapaLote: 1L`, `idInsumo: null`, 2 reservas de insumos distintos (`8.0` de un lote vencido ayer y `0.0` de un lote vigente)|`filtrarLotesInsumoReservados(1L, null)` propaga el nulo|Retorna las 2 reservas en el orden del repositorio; el lote de insumo de la primera tiene `vencido = true` y el de la segunda `false`.|

### 2. `filtrarReservasConSaldo(Long idEtapaLote, String nombreInsumo, String identificacion, Pageable pageable)`

Ofrece los lotes de insumo para elegir en un consumo reservado: solo las reservas de la etapa que todavía tienen saldo reservado (las agotadas no se ofrecen). `idEtapaLote` lo resuelve el Controller. Los criterios de texto son opcionales: el service los normaliza y un texto vacío o `null` no restringe. El orden es FEFO y lo define la consulta (vigentes por fecha de vencimiento, luego sin fecha y al final los vencidos), por eso el orden del `Pageable` recibido se ignora. Cada reserva trae su lote de insumo con la marca `vencido`.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FRS-01**|Insumo elegido, identificación normalizada, orden ignorado y vencido marcado|`idEtapaLote: 1L`, `idInsumo: 7L`, `identificacion: " l-125 "`, página 2 de 8 con orden por `id` descendente, 1 reserva de `8.0` de un lote vencido ayer|El service pasa el insumo tal cual, normaliza la identificación y consulta con un `Pageable` sin orden|Retorna la página con 1 reserva mapeada (`8.0`, lote de insumo con `vencido = true`); el repositorio recibe `7L`, `"l-125"` y `PageRequest.of(2, 8)`.|
|**CP-FRS-02**|Insumo e identificación nulos o vacíos|`idEtapaLote: 1L`, `idInsumo: null`, `identificacion: "   "`, página 0 de 8|Sin insumo no se restringe y un texto vacío no restringe|Retorna una página vacía; el repositorio recibe `null` en ambos criterios.|
