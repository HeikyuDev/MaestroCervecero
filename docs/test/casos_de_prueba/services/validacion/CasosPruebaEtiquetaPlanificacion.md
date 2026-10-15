## Casos de prueba de `EtiquetaPlanificacion`

**Tipo de prueba:** unitaria (clase `EtiquetaPlanificacionTest`), sin Spring ni mocks. Verifica el único armado del nombre con que se muestra una planificación en todas las pantallas.

**Regla:** la planificación se nombra siempre "Plan #N - Nombre", con el nombre de la receta de su versión. Los DTO de respuesta de la planificación y del lote la exponen con el mismo texto.

---

### `de(Long id, String nombreReceta)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-EPL-01** | Etiqueta completa | `id: 7`, `nombreReceta: "Pale Ale"` | Con id y nombre | Devuelve "Plan #7 - Pale Ale". |
| **CP-EPL-02** | Sin nombre o sin id | `id: 7` con nombre nulo o en blanco; `id` nulo con nombre "Pale Ale" | Datos ausentes | Devuelve "Plan #7" sin nombre y `null` sin id. |
| **CP-EPL-03** | Los DTO exponen la misma etiqueta | Una planificación con `id: 7` y receta "Pale Ale"; un lote de la planificación 7 con receta "Pale Ale" | Un solo armado para planificación y lote | `getEtiqueta()` de la planificación y `getEtiquetaPlanificacion()` del lote devuelven "Plan #7 - Pale Ale". |
