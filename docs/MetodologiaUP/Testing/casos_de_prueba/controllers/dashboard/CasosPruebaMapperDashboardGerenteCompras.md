## Casos de prueba de `MapperDashboardGerenteCompras`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del gerente de compras. Clase `MapperDashboardGerenteComprasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDC-01** | Planificación sin orden | Lleva su ID, su etiqueta con el nombre de la receta, su fecha de inicio estimada y su prioridad. |
| **CP-MDC-02** | Orden en curso | Lleva su ID, la razón social de su proveedor, su fecha de entrega estimada y el avance recibido. |
| **CP-MDC-03** | Orden finalizada | Lleva su ID, su proveedor y su fecha de finalización; el cierre es manual solo si la orden guardó un motivo de finalización. |
| **CP-MDC-04** | Entrada nula | La planificación y las órdenes nulas se mapean a nulo. |
