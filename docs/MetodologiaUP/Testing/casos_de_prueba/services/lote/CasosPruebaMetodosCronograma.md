## Casos de prueba de `MetodosCronograma`

**Tipo de prueba:** unitaria (clase `MetodosCronogramaTest`), sin Spring ni mocks. Verifica el lugar que ocupa un lote nuevo en la cola de lotes pendientes de un fermentador según su planificación.

**Orden de la cola (`compararParaCola`):** mayor prioridad primero (`ALTA`, `MEDIA`, `BAJA`); a igual prioridad, menor margen (`fechaFinalizacionEstimada - fechaInicioEstimada` de la planificación, en días) primero; a igual margen, fecha de finalización estimada más temprana primero. Una prioridad nula se trata como `MEDIA`.

**Regla de posición:** el lote nuevo se ubica inmediatamente después del último lote de la cola que le gana o empata por completo (el primero que llega va primero). En una cola reordenada a mano se mira ese último lote, aunque haya lotes que le pierden delante de él.

### `calcularPosicionEnCola(List<CriterioDeCola> criteriosDeLaCola, CriterioDeCola criterioDelLoteNuevo)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCR-01**|`ALTA` va delante de los de menor prioridad|Cola `[BAJA, BAJA]` y lote `ALTA`; cola `[ALTA, MEDIA, BAJA]` y lote `ALTA`|Hay lotes de prioridad estrictamente menor|Posición `0` en la primera cola y `1` en la segunda (detrás de la `ALTA` existente).|
|**CP-MCR-02**|Empate total va al final|Cola `[MEDIA, MEDIA]` y lote `MEDIA`; cola `[ALTA, MEDIA, MEDIA]` y lote `MEDIA`, todos con el mismo margen y fin|Ningún lote le pierde|Posición `2` y `3`: el que llegó primero va primero.|
|**CP-MCR-03**|`MEDIA` va entre `ALTA` y `BAJA`|Cola `[ALTA, ALTA, BAJA, BAJA]` y lote `MEDIA`|Los `BAJA` son de prioridad menor y los `ALTA` de mayor|Posición `2`.|
|**CP-MCR-04**|`BAJA` va al final|Cola `[ALTA, MEDIA, BAJA]` y lote `BAJA`|Ningún lote le pierde|Posición `3`.|
|**CP-MCR-05**|Cola vacía|Cola `[]` y lote `ALTA`|No hay lotes pendientes|Posición `0`.|
|**CP-MCR-06**|Cola reordenada a mano (prioridad)|Cola `[BAJA, MEDIA, BAJA]` y lote `MEDIA`; cola `[MEDIA, BAJA, ALTA]` y lote `MEDIA`|Hay un lote que le gana o empata por detrás de uno que le pierde|Posición `2` y `3`: nunca pasa delante de un lote que le gana o empata.|
|**CP-MCR-07**|Prioridad nula|Cola `[null, BAJA]` y lote `null`; cola `[MEDIA, BAJA]` y lote `null`|`null` se trata como `MEDIA`|Posición `1` en ambos casos.|
|**CP-MCR-08**|A igual prioridad, el menor margen va primero|Cola `MEDIA` con márgenes `10` y `20`; lote `MEDIA` con margen `15`, `5` y `25`|Se compara el margen en días|Posición `1`, `0` y `2`.|
|**CP-MCR-09**|Mismo margen: gana el fin más temprano|Dos planificaciones `MEDIA` de 20 días: de `01/07/2027` a `21/07/2027` y de `05/07/2027` a `25/07/2027`|Se compara la fecha de finalización|La del fin `21/07/2027` va primero: posición `0` contra la otra y `1` en el caso inverso.|
|**CP-MCR-10**|Empate total|Cola de dos lotes `ALTA` con la misma planificación y lote nuevo con esa misma planificación|Nada desempata|Posición `2`.|
|**CP-MCR-11**|La prioridad manda sobre el margen|Cola `[MEDIA con margen 400]` y lote `BAJA` con margen `1`; y al revés|La prioridad se compara antes que el margen|Posición `1` en el primer caso y `0` en el segundo.|
|**CP-MCR-12**|Cola reordenada a mano (desempate)|Cola `MEDIA` con márgenes `[10, 30, 10]`, `[30, 30, 10]` y `[30, 10, 30]`; lote con margen `20`|Se toma el último lote que le gana|Posición `3`, `3` y `2`.|
|**CP-MCR-13**|Planificación sin fechas|`CriterioDeCola` sin fecha de inicio, o sin ambas fechas|El margen de una planificación sin fechas es el mayor posible|`margenEnDias()` es `Long.MAX_VALUE` (y `12` con fechas del `01/07/2027` al `13/07/2027`); una cola con una planificación sin fechas queda detrás de un lote con fechas: posición `0`.|
