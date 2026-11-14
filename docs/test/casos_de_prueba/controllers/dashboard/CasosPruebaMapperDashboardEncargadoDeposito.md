## Casos de prueba de `MapperDashboardEncargadoDeposito`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del encargado de depósito. Clase `MapperDashboardEncargadoDepositoTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDD-01** | Orden pendiente | Lleva su ID, la razón social de su proveedor y su fecha de entrega estimada. |
| **CP-MDD-02** | Lote de insumo vencido | Lleva su ID, el nombre y la unidad de su insumo, su identificación, su vencimiento y su cantidad actual. |
| **CP-MDD-03** | Insumo sin stock | Lleva su ID, su nombre y su tipo. |
| **CP-MDD-04** | Entrada nula | La orden, el lote y el insumo nulos se mapean a nulo. |
