## Casos de prueba de `MapperDashboardGerente`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del gerente de producción. Clase `MapperDashboardGerenteTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDG-01** | Lote en producción | Lleva los datos del lote del listado (código, receta, etapa, fermentador, prioridad, fechas estimadas) y el atraso recibido. |
| **CP-MDG-02** | Lote en producción de un lote pendiente | Conserva la etapa y el fermentador nulos, con atraso `0` y la prioridad del lote. |
| **CP-MDG-03** | Entrada nula | El lote, el fermentador y la planificación nulos se mapean a nulo. |
| **CP-MDG-04** | Cola de un fermentador | Lleva el ID, el identificador y el estado operativo del fermentador, el lote en ejecución, el próximo lote y la cantidad de pendientes recibidos. |
| **CP-MDG-05** | Planificación pendiente | Lleva su ID, su etiqueta, su prioridad y sus fechas estimadas de inicio y de finalización. |
