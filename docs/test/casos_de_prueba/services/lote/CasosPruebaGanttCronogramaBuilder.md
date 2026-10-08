## Casos de prueba de `CronogramaProduccionBuilder`

**Tipo de prueba:** unitaria (clase `CronogramaProduccionBuilderTest`), sin Spring ni mocks. Verifica que `CronogramaProduccionBuilder.construir(lotes, identificadoresFermentadores, idPlanificacionDestacada, hoy)` convierta los `LoteCronogramaResponseDTO` de todas las planificaciones en el `CronogramaProduccionVista` que dibuja la plantilla `cronograma/cronograma.html`: el diagrama de Gantt (un `GanttPlanificacionVista`) y la cola de lotes pendientes de cada fermentador.

**Geometría compartida:** las reglas de cada barra según el estado del lote, el eje de tiempo con sus marcas, la marca de hoy, la alineación de las etiquetas y el agrupado en carriles son las de `GanttGeometria`, la misma clase que usa `GanttPlanificacionBuilder` (ver `CasosPruebaGanttPlanificacionBuilder.md`, que las cubre con exactitud). Estos casos verifican solo lo propio del cronograma global.

**Lo propio del cronograma global:**
- Todos los fermentadores recibidos tienen su carril, aunque no tengan lotes (carril sin barras). Un fermentador que aparece solo en los lotes también tiene carril. Los carriles van ordenados por identificador sin distinguir mayúsculas; los lotes sin fermentador van al final, en "Sin fermentador", que no tiene cola.
- En un carril conviven, intercaladas y ordenadas por inicio, las barras de lotes de planificaciones distintas. La etiqueta de cada barra es `<receta> - Plan #<id> - <lote>` (las partes que faltan se omiten) y el tooltip suma las líneas "Receta" y "Planificación" a los datos del lote.
- Si se destaca una planificación, las barras (y las filas de cola, y el lote en ejecución) de los lotes de las demás se marcan como atenuadas (`atenuada`); la vista indica `planificacionResaltada`. Sin planificación destacada no se atenúa nada.
- El eje abarca todas las barras y también el día de hoy, con un día de margen a cada lado. No hay marcas de inicio ni de fin de planificación (`null`).
- La cola de un fermentador lista sus lotes `PENDIENTE` por fecha de inicio estimada y, ante igual fecha, por id (sin fecha, al final), con su posición desde 1; `primero` y `ultimo` indican los extremos (con un solo pendiente es ambos). El lote `EN_EJECUCION` se informa aparte y los finalizados y cancelados no entran en la cola.

Los ejemplos usan fechas de julio de 2027 (`fecha(d)` = `d/07/2027`).

---

### `construir(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-GCB-01** | Carriles vacíos | Sin lotes, fermentadores "F-02" y "F-01", hoy 10/07 | Todo fermentador tiene carril aunque no tenga lotes | `vacio: true`; carriles "F-01" y "F-02" sin barras; eje del 09/07 al 11/07 (`totalDias: 3`), `hoyPct: 50.0`; marcas de planificación `null`; `planificacionResaltada: false`. Dos colas ("F-01", "F-02") sin lote en ejecución y sin pendientes. |
| **CP-GCB-02** | Listas nulas | Lotes y fermentadores `null` | Los datos faltantes no rompen | Modelo vacío, sin carriles ni colas; eje alrededor de hoy (`totalDias: 3`). |
| **CP-GCB-03** | Dos planificaciones en un carril | "STOUT-1" (plan 8, del 07/07 al 10/07) y "PALE_ALE-1" (plan 7, del 01/07 al 05/07), ambos en "F-01", pasados en desorden | Conviven en el carril, ordenados por inicio | Eje del 30/06 al 11/07 (`totalDias: 12`). Un carril con las barras de los lotes 1 y 2 en ese orden: `izquierdaPct` 8.33 y 58.33, `anchoPct` 41.67 y 33.33. |
| **CP-GCB-04** | Etiqueta y tooltip | Lote "PALE_ALE-1" de la receta "Pale Ale", plan 7, 300 L, del 01/07 al 05/07, molino "M-01", macerador "MA-01", olla "O-01" | La barra se identifica entre lotes intercalados | Etiqueta "Pale Ale - Plan #7 - PALE_ALE-1", estado `pendiente`. El tooltip empieza con "PALE_ALE-1 - Pendiente", "Receta: Pale Ale", "Planificación: #7" y sigue con las fechas, "Volumen: 300 L" y los tres equipos. |
| **CP-GCB-05** | Sin receta ni planificación | Lote sin receta ni planificación | Las partes que faltan se omiten | Etiqueta "PALE_ALE-1"; el tooltip muestra "Receta: -" y "Planificación: -". |
| **CP-GCB-06** | Planificación destacada | Lotes de los planes 7 y 8 en "F-01"; se destaca el 7, y luego ninguno | Solo se atenúan las barras de las demás | Con el 7: `planificacionResaltada: true` y `atenuada` = `false`, `true`. Sin destacar: `false` y `false, false`. |
| **CP-GCB-07** | Atenuado en las colas | Lote en ejecución y pendiente del plan 7 y pendiente del plan 8 en "F-01"; se destaca el plan 7 y luego el 9 (sin lotes) | Las filas de cola y el lote en ejecución siguen el mismo criterio | Con el 7: en ejecución sin atenuar; pendientes `false`, `true`. Con el 9: en ejecución atenuado, pendientes `true`, `true` y las tres barras atenuadas. |
| **CP-GCB-08** | Hoy después de las barras | Lote pendiente del 05/07 al 08/07, hoy 20/07 | El eje incluye hoy | Eje del 04/07 al 21/07 (`totalDias: 18`), `hoyPct: 91.67`, alineación `CENTRO`. |
| **CP-GCB-09** | Hoy antes de las barras | Lote pendiente del 05/07 al 08/07, hoy 01/07 | El eje incluye hoy | Eje del 30/06 al 09/07 (`totalDias: 10`), `hoyPct: 15.0`; la barra: `izquierdaPct: 50.0`, `anchoPct: 40.0`. |
| **CP-GCB-10** | Reglas de cada estado | Lote en ejecución (inicio real 02/07, fin estimado 11/07) en "F-01" y lote finalizado (inicio real 02/07, fin real 08/07) en "F-02", hoy 06/07 | Se conservan las reglas por estado | Barra `en-ejecucion` del 02/07 al 11/07 con `progresoPct: 50.0`; barra `finalizado` del 02/07 al 08/07. |
| **CP-GCB-11** | Carriles extra y sin fermentador | Lotes en "F-03", sin fermentador y en "f-01"; fermentador recibido "F-02" | Un fermentador solo de los lotes tiene carril; el orden no distingue mayúsculas | Carriles "f-01", "F-02", "F-03" y "Sin fermentador" (al final). Colas de "f-01", "F-02" y "F-03", sin cola para "Sin fermentador". |
| **CP-GCB-12** | Lote sin fechas | Dos pendientes en "F-01": el 1 sin fechas y el 2 del 05/07 al 08/07 | Sin fechas no se dibuja, pero sigue en la cola | Solo se dibuja la barra del lote 2; la cola es 2, 1 (el lote sin fecha al final). |
| **CP-GCB-13** | Orden y posiciones de la cola | Pendientes en "F-01", pasados en desorden: lote 3 (inicio 15/07), lote 2 (05/07) y lote 1 (05/07) | Orden por inicio estimado y, ante igual fecha, por id | Cola 1, 2, 3 con posiciones 1, 2, 3; `primero`: `true`, `false`, `false`; `ultimo`: `false`, `false`, `true`. |
| **CP-GCB-14** | Un solo pendiente | Un único pendiente en "F-01" | Es a la vez el primero y el último | Posición 1, `primero: true` y `ultimo: true`. |
| **CP-GCB-15** | Solo pendientes en la cola | "F-01" con un finalizado, un en ejecución, un pendiente y un cancelado; "F-02" con un pendiente | El lote en ejecución va aparte y las colas no se mezclan | Cola de "F-01": en ejecución = lote 2, pendientes = lote 3. Cola de "F-02": sin en ejecución, pendientes = lote 5. El carril de "F-01" dibuja las 4 barras. |
