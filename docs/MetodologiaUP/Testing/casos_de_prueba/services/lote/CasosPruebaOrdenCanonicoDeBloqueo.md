## Casos de prueba de `OrdenCanonicoDeBloqueo`

**Tipo de prueba:** unitaria (clase `OrdenCanonicoDeBloqueoTest`) con repositorios simulados.

**Regla:** todas las operaciones del módulo de lotes que bloquean varias filas lo hacen en un único orden global: planificación de producción, fermentador (exclusión mutua de su cola), cola de lotes pendientes del fermentador, lote, equipos del lote y lotes de insumo por ID ascendente. `OrdenCanonicoDeBloqueo.bloquearLote` cubre los cuatro primeros niveles. Los IDs se resuelven con consultas escalares que no cargan entidades, para que cada fila se lea recién cuando ya está bloqueada. El bloqueo en sí no es observable con Mockito: los tests verifican el orden de las llamadas.

---

### `bloquearLote(Long idLote, boolean bloquearPlanificacion)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OCB-01** | Bloqueo completo con planificación | Lote `7L`, fermentador `5L`, planificación `3L`; `bloquearPlanificacion = true` | Orden de las llamadas | Devuelve el lote. Orden (`InOrder`): `buscarPorIdParaActualizar` de la planificación, `buscarPorIdParaCambiarEstadoOperativo` del fermentador, `buscarPendientesDeFermentadorParaReordenar(5L)` y `buscarPorIdParaActualizar(7L)` del lote. |
| **CP-OCB-02** | Bloqueo sin planificación | Lote `7L`, fermentador `5L`; `bloquearPlanificacion = false` | No se bloquea la planificación | Devuelve el lote. No interactúa con el repositorio de planificaciones; orden: fermentador, cola pendiente y lote. |
| **CP-OCB-03** | Lote inexistente | `idLote: 99L`; `buscarIdFermentadorDelLote(99L)` $\rightarrow$ **Optional.empty()** | El lote no existe | Lanza `RecursoNoEncontradoException` ("No se encontró el lote con ID: 99"). No bloquea planificaciones ni fermentadores. |
| **CP-OCB-04** | Planificación o fermentador inexistente | (a) planificación `3L` sin fila al bloquearla; (b) fermentador `5L` sin fila al bloquearlo | Una fila del orden no existe | (a) Lanza `RecursoNoEncontradoException` ("No se encontró la planificación de producción con ID: 3") sin tocar el fermentador; (b) lanza `RecursoNoEncontradoException` ("No se encontró el fermentador con ID: 5"). |
