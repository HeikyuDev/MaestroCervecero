## Casos de prueba de `MapperDashboardEncargadoDistribucion`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del encargado de distribución. Clase `MapperDashboardEncargadoDistribucionTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDI-01** | Devolución pendiente | Lleva el ID y el identificador del barril, el nombre del cliente, la fecha de devolución estimada y los días de atraso hasta hoy (`0` si vence hoy). |
| **CP-MDI-02** | Cliente con barriles | Lleva el ID, el nombre, la cantidad de barriles y la próxima fecha de devolución de la fila de la consulta. |
| **CP-MDI-03** | Barril listo | Lleva el ID, el identificador, la receta y el lote de su cerveza y su contenido; sin contenido informado deja la receta y el lote nulos. |
| **CP-MDI-04** | Entrada nula | El despacho, la fila, el barril y la solicitud nulos se mapean a nulo. |
| **CP-MDI-05** | Retiro pendiente | La solicitud lleva su ID, el ID, el identificador interno y el número de serie del barril, el cliente y la fecha propuesta; está vencida solo si la fecha propuesta es anterior al momento de referencia (una fecha igual no lo está). |
