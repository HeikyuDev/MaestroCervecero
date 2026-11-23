## Casos de prueba de `MetodosCronograma`

**Tipo de prueba:** unitaria (clase `MetodosCronogramaTest`), sin Spring ni mocks. Verifica el lugar que ocupa un lote nuevo en la cola de lotes pendientes de un fermentador según su planificación.

**Orden de la cola (`compararParaCola`):** mayor prioridad primero (`ALTA`, `MEDIA`, `BAJA`); a igual prioridad, fecha de finalización estimada de la planificación más temprana primero (una fecha nula va última). Una prioridad nula se trata como `MEDIA`. `CriterioDeCola` guarda solo la prioridad y la fecha de finalización estimada.

**Regla de posición:** el lote nuevo se ubica inmediatamente después del último lote de la cola que le gana o empata por completo (el primero que llega va primero). En una cola reordenada a mano se mira ese último lote, aunque haya lotes que le pierden delante de él.

### `calcularPosicionEnCola(List<CriterioDeCola> criteriosDeLaCola, CriterioDeCola criterioDelLoteNuevo)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCR-01**|`ALTA` va delante de los de menor prioridad|Cola `[BAJA, BAJA]` y lote `ALTA`; cola `[ALTA, MEDIA, BAJA]` y lote `ALTA`|Hay lotes de prioridad estrictamente menor|Posición `0` en la primera cola y `1` en la segunda (detrás de la `ALTA` existente).|
|**CP-MCR-02**|Empate total va al final|Cola `[MEDIA, MEDIA]` y lote `MEDIA`; cola `[ALTA, MEDIA, MEDIA]` y lote `MEDIA`, todos con el mismo fin|Ningún lote le pierde|Posición `2` y `3`: el que llegó primero va primero.|
|**CP-MCR-03**|`MEDIA` va entre `ALTA` y `BAJA`|Cola `[ALTA, ALTA, BAJA, BAJA]` y lote `MEDIA`|Los `BAJA` son de prioridad menor y los `ALTA` de mayor|Posición `2`.|
|**CP-MCR-04**|`BAJA` va al final|Cola `[ALTA, MEDIA, BAJA]` y lote `BAJA`|Ningún lote le pierde|Posición `3`.|
|**CP-MCR-05**|Cola vacía|Cola `[]` y lote `ALTA`|No hay lotes pendientes|Posición `0`.|
|**CP-MCR-06**|Cola reordenada a mano (prioridad)|Cola `[BAJA, MEDIA, BAJA]` y lote `MEDIA`; cola `[MEDIA, BAJA, ALTA]` y lote `MEDIA`|Hay un lote que le gana o empata por detrás de uno que le pierde|Posición `2` y `3`: nunca pasa delante de un lote que le gana o empata.|
|**CP-MCR-07**|Prioridad nula|Cola `[null, BAJA]` y lote `null`; cola `[MEDIA, BAJA]` y lote `null`|`null` se trata como `MEDIA`|Posición `1` en ambos casos.|
|**CP-MCR-08**|A igual prioridad, el fin más temprano va primero|Cola `MEDIA` que termina el `11/07/2027` y el `21/07/2027`; lote `MEDIA` que termina el `16/07/2027`, el `06/07/2027` y el `26/07/2027`|Se compara la fecha de finalización estimada|Posición `1`, `0` y `2`.|
|**CP-MCR-09**|La duración de la planificación no interviene (caso real)|Planificaciones `MEDIA`: #8 del `15/10/2026` al `31/01/2027` (108 días) con dos lotes en la cola y #10 del `07/12/2026` al `11/03/2027` (94 días), registrada después|Solo cuenta el fin: la #8 termina antes|Un lote de la #10 va detrás de los dos de la #8 (posición `2`); un lote de la #8 contra una cola `[#10]` va primero (posición `0`).|
|**CP-MCR-10**|Empate total|Cola de dos lotes `ALTA` con el mismo fin y lote nuevo con ese mismo fin|Nada desempata|Posición `2`.|
|**CP-MCR-11**|La prioridad manda sobre la fecha de finalización|Cola `[MEDIA que termina a +400 días]` y lote `BAJA` que termina a +1; y al revés|La prioridad se compara antes que el fin|Posición `1` en el primer caso y `0` en el segundo.|
|**CP-MCR-12**|Cola reordenada a mano (desempate)|Cola `MEDIA` con fines a `[+10, +30, +10]`, `[+30, +30, +10]` y `[+30, +10, +30]` días; lote que termina a `+20`|Se toma el último lote que le gana|Posición `3`, `3` y `2`.|
|**CP-MCR-13**|Planificación sin fecha de finalización|`CriterioDeCola` `MEDIA` sin fecha de finalización contra uno `MEDIA` que termina a `+10`, en ambos sentidos, y dos sin fecha|Una fecha nula va última|Contra una cola `[sin fecha]` el lote con fecha va primero (posición `0`); un lote sin fecha contra `[+10]` va detrás (posición `1`); dos sin fecha empatan por completo (posición `1`).|
