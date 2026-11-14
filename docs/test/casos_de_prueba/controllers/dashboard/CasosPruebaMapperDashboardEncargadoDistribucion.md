## Casos de prueba de `MapperDashboardEncargadoDistribucion`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del encargado de distribución. Clase `MapperDashboardEncargadoDistribucionTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDI-01** | Devolución pendiente | Lleva el ID y el identificador del barril, el nombre del cliente, la fecha de devolución estimada y los días de atraso hasta hoy (`0` si vence hoy). |
| **CP-MDI-02** | Cliente con barriles | Lleva el ID, el nombre, la cantidad de barriles y la próxima fecha de devolución de la fila de la consulta. |
| **CP-MDI-03** | Barril listo | Lleva el ID, el identificador, la receta y el lote de su cerveza y su contenido; sin contenido informado deja la receta y el lote nulos. |
| **CP-MDI-04** | Entrada nula | El despacho, la fila y el barril nulos se mapean a nulo. |
