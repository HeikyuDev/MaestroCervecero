## Casos de prueba de `BloqueoPorLotesVivos`

**Tipo de prueba:** unitaria (clase `BloqueoPorLotesVivosTest`), sin Spring ni mocks. Verifica el mensaje con el que se rechaza la modificación de un dato de un insumo que cambia el escalado cuando lotes Pendientes o En ejecución lo usan.

**Regla:** si la lista de códigos de lote no está vacía, lanza `ReglaNegocioException` con "No se puede modificar {datos} porque {lo|la} usa el lote {código}" (un lote) o "... usan los lotes {códigos}" (varios), sin punto final. Se listan como máximo 5 códigos, en el orden recibido, separados por ", " y con " y " antes del último; si hay más de 5 se listan los primeros 5 y se agrega "y N más".

---

### `validar(List<String>, String, String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BLV-01** | Sin lotes | Lista vacía y lista `null` | No hay lotes que usen el insumo | No lanza excepción. |
| **CP-BLV-02** | Un solo lote | `["PILSEN_CLASICA-R7-3"]`, pronombre `la` | Un código | Lanza `ReglaNegocioException`: "No se puede modificar el extracto potencial de la malta porque la usa el lote PILSEN_CLASICA-R7-3". |
| **CP-BLV-03** | Dos lotes | `["A-R1-1", "A-R1-2"]`, pronombre `lo` | Dos códigos | Lanza `ReglaNegocioException`: "... porque lo usan los lotes A-R1-1 y A-R1-2". |
| **CP-BLV-04** | Tres lotes | `["A-R1-1", "A-R1-2", "A-R1-3"]` | Tres códigos | Lanza `ReglaNegocioException`: "... los lotes A-R1-1, A-R1-2 y A-R1-3". |
| **CP-BLV-05** | Exactamente 5 lotes _(Límite)_ | `["L1", "L2", "L3", "L4", "L5"]` | Cantidad igual al máximo listado | Lanza `ReglaNegocioException` con los 5 códigos completos: "... los lotes L1, L2, L3, L4 y L5". |
| **CP-BLV-06** | 6 lotes _(Límite)_ | `["L1", ..., "L6"]` | Cantidad mayor al máximo listado en uno | Lanza `ReglaNegocioException`: "... los lotes L1, L2, L3, L4, L5 y 1 más". |
| **CP-BLV-07** | Más de 6 lotes | `["L1", ..., "L8"]` | Cantidad mayor al máximo listado | Lanza `ReglaNegocioException`: "... los lotes L1, L2, L3, L4, L5 y 3 más". |
