## Casos de prueba de `DashboardGerenteComprasServicioImpl`

**Tipo de prueba:** unitaria con Mockito (`@ExtendWith(MockitoExtension.class)`, clase `DashboardGerenteComprasServicioImplTest`). Se simulan `IPlanificacionProduccionRepository`, `IOrdenCompraRepository`, `IDetalleCompraRepository` e `IIngresoInsumoRepository`.

### 1. `obtenerDashboardGerenteCompras()`

Alimenta el panel del gerente de compras. Es de solo lectura (`@Transactional(readOnly = true)`), no recibe parámetros y calcula todo con la fecha de hoy y un tope de 5 filas por bloque (`LimitesTecnicos.PANEL_GERENTE_COMPRAS_FILAS_POR_BLOQUE`). Devuelve `DashboardGerenteComprasResponseDTO` con:

- **Planificaciones sin orden:** las planificaciones `PENDIENTE` sin ninguna orden de compra no anulada, por fecha de inicio estimada y ID ascendentes, con el total real y hasta 5 filas.
- **Órdenes pendientes:** las órdenes `PENDIENTE` por fecha de entrega estimada y ID ascendentes, con el total real, hasta 5 filas y el avance de recepción de cada una: las presentaciones recibidas (ingresos `REGISTRADO` convertidos a presentaciones, sin pasar de lo pedido en cada ítem) sobre las pedidas, redondeado hacia abajo. Los ítems y los ingresos se consultan una sola vez para todas las filas, y no se consultan si no hay órdenes.
- **Entregas vencidas:** `contarPendientesConEntregaVencida(hoy)`.
- **Órdenes finalizadas:** las órdenes `FINALIZADA` desde el inicio del día de hace 30 días, la más recientemente finalizada primero, con el total real, hasta 5 filas y el cierre manual si guardaron un motivo de finalización. `diasDeOrdenesFinalizadas` informa los 30 días.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-DGC-01** | Sistema vacío | Sin planificaciones ni órdenes | Todas las consultas devuelven páginas vacías | Los tres bloques tienen `total: 0` y `filas` vacías, `ordenesConEntregaVencida: 0` y `diasDeOrdenesFinalizadas: 30`. No consulta ítems ni ingresos. |
| **CP-DGC-02** | Planificaciones sin orden | 6 planificaciones sin orden, dos visibles | Se piden con el tope de 5 filas | `total: 6`; las filas en el orden recibido con el ID, la etiqueta con la receta, la fecha de inicio estimada y la prioridad. |
| **CP-DGC-03** | Órdenes pendientes y avance | 9 órdenes pendientes, dos visibles. La primera tiene un ítem de 4 presentaciones de 25 kg con 50 kg recibidos y otro de 2 presentaciones con 200 kg recibidos; la segunda no tiene ingresos | El avance suma las presentaciones recibidas sin pasar de lo pedido en cada ítem | `total: 9`; la primera fila lleva el proveedor y la fecha de entrega estimada y `avanceRecepcion: 66` ((2 + 2) de 6 presentaciones); la segunda, `0`. |
| **CP-DGC-04** | Entregas vencidas | 3 órdenes pendientes con entrega anterior a hoy | La consulta recibe la fecha de hoy | `ordenesConEntregaVencida: 3`. |
| **CP-DGC-05** | Órdenes finalizadas | 14 órdenes finalizadas en el período, dos visibles: una sin motivo de finalización y otra con motivo | La consulta recibe el inicio del día de hace 30 días y el tope de 5 filas | `total: 14`; la primera fila tiene `cierreManual: false` y la segunda `cierreManual: true`, ambas con su fecha de finalización y su proveedor. |
