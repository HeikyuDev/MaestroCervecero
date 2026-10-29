Casos de prueba de los métodos de dominio de `ReservaInsumoEntity` (no de un service: es lógica propia de la entidad, sin dependencias externas). Una reserva nunca se borra y conserva su historial.

**Invariante.** `cantidadReservada` (original, inmutable) = `cantidadConsumida` + `cantidadLiberada` + `cantidadReducidaPorAjuste` + `cantidadPendiente`.

**Estados.** `ACTIVA` con saldo pendiente (mayor a 0,00005); sin saldo, `CONSUMIDA` si se consumió todo lo reservado y `LIBERADA` si no (se devolvió el saldo o una merma la agotó). Todas las cantidades se redondean a 4 decimales.

### 1. `nueva`, `consumir`, `revertirConsumo`, `liberar` y `reducirPorAjuste`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RIE-01**|Reserva nueva|`nueva(null, null, 10.0)`|Alta de una reserva|`cantidadReservada = 10.0`, `cantidadPendiente = 10.0`, consumida, liberada y reducida en `0.0`, estado `ACTIVA`.|
|**CP-RIE-02**|Consumir una parte|Reserva de `10.0`; `consumir(4.0)`|Queda saldo|`cantidadPendiente = 6.0`, `cantidadConsumida = 4.0`, `cantidadReservada = 10.0`, estado `ACTIVA`.|
|**CP-RIE-03**|Consumir todo|Reserva de `10.0`; `consumir(4.0)` y `consumir(6.0)`|El pendiente llega a 0 y consumida igual a la original|`cantidadPendiente = 0.0`, `cantidadConsumida = 10.0`, estado `CONSUMIDA`.|
|**CP-RIE-04**|Consumo con decimales _(Redondeo)_|Reserva de `0.3`; `consumir(0.1)` y `consumir(0.2)`|`0.3 - 0.1 - 0.2` no es `0.0` en coma flotante|`cantidadPendiente = 0.0`, `cantidadConsumida = 0.3`, estado `CONSUMIDA`.|
|**CP-RIE-05**|Revertir el consumo de una reserva Consumida|Reserva de `10.0` consumida por completo; `revertirConsumo(10.0)`|Se anula el consumo|`cantidadPendiente = 10.0`, `cantidadConsumida = 0.0`, estado `ACTIVA`.|
|**CP-RIE-06**|Liberar una reserva con saldo|Reserva de `10.0` con `6.0` consumidos; `liberar()`|Hay `4.0` pendientes|Retorna `4.0`; `cantidadLiberada = 4.0`, `cantidadPendiente = 0.0`, `cantidadConsumida = 6.0`, `cantidadReservada = 10.0`, estado `LIBERADA`.|
|**CP-RIE-07**|Liberar una reserva sin consumir|Reserva de `10.0`; `liberar()`|Todo está pendiente|Retorna `10.0`; `cantidadLiberada = 10.0`, estado `LIBERADA`.|
|**CP-RIE-08**|Reducción parcial por ajuste|Reserva de `10.0`; `reducirPorAjuste(3.0)`|Queda saldo|`cantidadPendiente = 7.0`, `cantidadReducidaPorAjuste = 3.0`, `cantidadLiberada = 0.0`, estado `ACTIVA`.|
|**CP-RIE-09**|Reducción por ajuste que agota el pendiente|Reserva de `10.0` con `6.0` consumidos; `reducirPorAjuste(4.0)`|El pendiente llega a 0 sin haberse consumido todo|`cantidadPendiente = 0.0`, `cantidadReducidaPorAjuste = 4.0`, `cantidadConsumida = 6.0`, estado `LIBERADA`.|
|**CP-RIE-10**|Reducción mayor al pendiente|Reserva de `5.0`; `reducirPorAjuste(8.0)`|La reducción supera el saldo|`cantidadPendiente = 0.0` (nunca negativo), `cantidadReducidaPorAjuste = 5.0` (solo lo que realmente se quitó), estado `LIBERADA`.|
|**CP-RIE-11**|El invariante se mantiene|Reserva de `10.0`; `consumir(3.0)`, `reducirPorAjuste(2.0)`, `revertirConsumo(1.0)` y `liberar()`|Secuencia de operaciones|consumida + liberada + reducida + pendiente es igual a `cantidadReservada`; estado `LIBERADA`.|
