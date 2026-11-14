## Casos de prueba de `MapperDashboardOperario`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del operario. Clase `MapperDashboardOperarioTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDO-01** | Lote del día | Lleva los datos del lote del listado (código, receta, etapa, fermentador, fechas estimadas) y el atraso y la falta de consumo recibidos. |
| **CP-MDO-02** | Lote del día de un lote pendiente | Conserva la etapa y el fermentador nulos, con atraso `0` y falta de consumo `false`. |
| **CP-MDO-03** | Lote, equipo o barril nulo | Cada mapeo devuelve nulo. |
| **CP-MDO-04** | Equipo pendiente | Lleva su ID, su identificador, su tipo y la causa recibida (nula si no se informa). |
| **CP-MDO-05** | Barril pendiente | Lleva su ID, su identificador, su fabricante y la causa recibida (nula si no se informa). |
| **CP-MDO-06** | Causa de un equipo: falla | Una falla da `FALLA`. |
| **CP-MDO-07** | Causa de un equipo: limpieza que lo dejó en mantenimiento | Una limpieza con `estadoOperativoResultante: EN_MANTENIMIENTO` da `PREVENTIVO`. |
| **CP-MDO-08** | Causa de un equipo no derivable | Una limpieza que lo dejó disponible, un mantenimiento o ninguna operación dan nulo. |
| **CP-MDO-09** | Causa de un barril: falla | Una falla da `FALLA`. |
| **CP-MDO-10** | Causa de un barril: limpieza que lo dejó en mantenimiento | Una limpieza con `estadoOperativoResultante: EN_MANTENIMIENTO` da `PREVENTIVO`. |
| **CP-MDO-11** | Causa de un barril no derivable | Una limpieza que lo dejó disponible, un mantenimiento o ninguna operación dan nulo. |
