## Casos de prueba de `MetodosEstimacionEtapa`

**Tipo de prueba:** unitaria (clase `MetodosEstimacionEtapaTest`), sin Spring ni mocks. Verifica los cálculos puros de la duración estimada de una etapa de lote, de su fin previsto, de su duración real y la redacción de una duración.

**Reglas:** todas las duraciones son minutos enteros. Las que salen de un rendimiento (Molienda y Envasado) se redondean hacia arriba al minuto, con un mínimo de 1, y toleran el error de coma flotante. Un dato nulo, no positivo o no finito da `null`: nunca se inventa una duración.

### `minutosDeMolienda(Double masaMaltaKg, Double rendimientoKgPorHora)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MEE-01**|Divide la malta por el rendimiento|`45 kg` a `30 kg/h`; `100 kg` a `40 kg/h`|`masa / rendimiento × 60`|`90` y `150` minutos.|
|**CP-MEE-02**|Redondeo hacia arriba _(Redondeo)_|`4.3952 kg` a `5 kg/h`; `1.1 kg` a `0.1 kg/h`|`52.7424 → 53`; `660.0000000000001 → 660` (tolerancia de coma flotante)|`53` y `660` minutos.|
|**CP-MEE-03**|Mínimo de un minuto _(Límite)_|`0.001 kg` a `100 kg/h`|El resultado es menor que un minuto|`1` minuto.|
|**CP-MEE-04**|Datos inválidos|Malta o rendimiento nulos, `0`, negativos, `NaN` o infinitos|El dato no es positivo y finito|Devuelve `null` en cada caso.|

### `minutosDeEnvasado(Double capacidadUtilLitros, Double velocidadLitrosPorHora)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MEE-05**|Divide la capacidad útil por la velocidad|`300 L` a `120 L/h`; `20 L` a `10 L/h`; `100 L` a `30 L/h`; velocidad `0`; capacidad o velocidad nulas|`capacidad útil / velocidad × 60` y velocidad positiva|`150`, `120` y `200` minutos; `null` con velocidad `0` o con un dato nulo.|

### `minutosDeReceta(Integer minutos)` y `minutosDeDias(Integer dias)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MEE-06**|Duraciones de la receta|Minutos `60`, `90`, `0`, `null`; días `1`, `10`, `-1`, `null`|El dato es positivo|`60` y `90` minutos; `null` con `0` y `null`. `1` día son `1440` minutos y `10` días `14400`; `null` con `-1` y `null`.|

### `calcularFinPrevisto(LocalDateTime inicio, Long minutosEstimados)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MEE-07**|Fin previsto|Inicio `01/07/2027 08:00` con `90` y con `14400` minutos; inicio nulo; duración nula|Hay inicio y duración|`01/07/2027 09:30` y `11/07/2027 08:00`; `null` si falta alguno de los dos datos.|

### `minutosReales(LocalDateTime inicio, LocalDateTime fin)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MEE-08**|Duración real|Inicio `01/07/2027 08:00` y fin `09:40`; fin `09:40:59`; fin igual al inicio; fin anterior; fechas nulas|Hay ambas fechas y el fin no es anterior|`100` minutos (los segundos sobrantes se descartan), `0` con fin igual al inicio; `null` con el fin anterior o con una fecha nula.|

### `formatearDuracion(Long minutos)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MEE-09**|Redacción de la duración|`0`, `45`, `60`, `90`, `150`, `1440`, `14400`, `1500`, `1530` minutos|Días completos en días; menos de una hora en minutos; el resto en horas y minutos|"0 min", "45 min", "1 h", "1 h 30 min", "2 h 30 min", "1 día", "10 días", "25 h", "25 h 30 min".|
|**CP-MEE-10**|Duración nula o negativa|`null`; `-5`|No hay duración que redactar|Devuelve `null`.|
