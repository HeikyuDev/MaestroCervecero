Casos de prueba de los métodos de dominio de `LoteInsumoEntity` (no de un service: es lógica propia de la entidad, sin dependencias externas). Verifica el stock disponible, la reserva y el consumo de un lote de insumo.

**Redondeo.** Cada resultado de una suma o una resta sobre `cantidadActual` o `cantidadReservada` se redondea a 4 decimales (`Redondeo.redondear`, `LimitesTecnicos.ESCALA_CANTIDADES`), y las comparaciones usan cantidades redondeadas. Así el error de representación de los decimales no se acumula (por ejemplo, tres consumos de `0.1` sobre un stock de `0.3` lo dejan exactamente en `0.0`). Las operaciones de ingreso (`sumarIngreso`, `anularIngreso`) no forman parte de estos casos.

### 1. `getCantidadDisponible()`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LIE-01**|Un residuo de punto flotante no cuenta como stock|`cantidadActual: 0.1 + 0.2` (`0.30000000000000004`), `cantidadReservada: 0.3`|`cantidadActual - cantidadReservada` $\rightarrow$ `5.5e-17`|Retorna `0.0`.|
|**CP-LIE-02**|La diferencia se redondea a 4 decimales|`cantidadActual: 0.7`, `cantidadReservada: 0.4` (`0.7 - 0.4 = 0.29999999999999993`)|`redondear(0.29999999999999993)`|Retorna `0.3`.|
|**CP-LIE-03**|Cantidades nulas|`cantidadActual: null`, `cantidadReservada: null`|Un valor nulo se trata como cero|Retorna `0.0`.|

### 2. `reservar(double cantidad)` y `liberarReserva(double cantidad)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LIE-04**|Reservar todo el stock con una cantidad con residuo decimal|`cantidadActual: 0.3`, `cantidadReservada: 0.0`; `reservar(0.1 + 0.2)`|`redondear(0.30000000000000004) > cantidadDisponible (0.3)` $\rightarrow$ **FALSE**|`cantidadReservada = 0.3` y `getCantidadDisponible() = 0.0`.|
|**CP-LIE-05**|Reservar más que lo disponible|`cantidadActual: 1.0`, `cantidadReservada: 0.4`; `reservar(0.7)`|`0.7 > 0.6` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Stock disponible insuficiente para reservar"). `cantidadReservada` queda en `0.4`.|
|**CP-LIE-06**|Tres reservas y tres liberaciones de 0,1|`cantidadActual: 0.3`, `cantidadReservada: 0.0`; tres `reservar(0.1)` y luego tres `liberarReserva(0.1)`|Cada resultado se redondea|Tras reservar, `cantidadReservada = 0.3`; tras liberar, `cantidadReservada = 0.0`.|

### 3. `ejecutarConsumo(double cantidad)` y `consumirDirecto(double cantidad)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LIE-07**|Tres consumos reservados de 0,1|`cantidadActual: 0.3`, `cantidadReservada: 0.3`; tres `ejecutarConsumo(0.1)`|Cada resultado se redondea|`cantidadActual = 0.0` y `cantidadReservada = 0.0`.|
|**CP-LIE-08**|Tres consumos directos de 0,1|`cantidadActual: 0.3`, `cantidadReservada: 0.0`; tres `consumirDirecto(0.1)`|Cada resultado se redondea|`cantidadActual = 0.0`; `cantidadReservada` sin cambios (`0.0`).|

### 4. `revertirConsumo(double cantidad)` y `revertirConsumoDirecto(double cantidad)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LIE-09**|Revertir tres consumos reservados de 0,1|`cantidadActual: 0.0`, `cantidadReservada: 0.0`; tres `revertirConsumo(0.1)`|Cada resultado se redondea|`cantidadActual = 0.3` y `cantidadReservada = 0.3`.|
|**CP-LIE-10**|Revertir tres consumos directos de 0,1|`cantidadActual: 0.0`, `cantidadReservada: 0.0`; tres `revertirConsumoDirecto(0.1)`|Cada resultado se redondea|`cantidadActual = 0.3`; `cantidadReservada` sin cambios (`0.0`).|
|**CP-LIE-11**|Sumar 0,1 y 0,2 y anular ambos ingresos|`cantidadActual: 0.0`, PPP `0`; `sumarIngreso(0.1, 7)`, `sumarIngreso(0.2, 7)`, `anularIngreso(0.2, 7)`, `anularIngreso(0.1, 7)`|Cada resultado se redondea y el PPP no se recalcula si el lote queda sin stock|`cantidadActual = 0.3` tras los ingresos y `0.0` tras las anulaciones; PPP = `7`.|
