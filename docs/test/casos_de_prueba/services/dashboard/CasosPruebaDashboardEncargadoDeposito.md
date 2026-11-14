## Casos de prueba de `DashboardEncargadoDepositoServicioImpl`

**Tipo de prueba:** unitaria con Mockito (`@ExtendWith(MockitoExtension.class)`, clase `DashboardEncargadoDepositoServicioImplTest`). Se simulan `IOrdenCompraRepository`, `ILoteInsumoRepository` e `IStockInsumoServicio`.

### 1. `obtenerDashboardEncargadoDeposito()`

Alimenta el panel del encargado de depósito. Es de solo lectura (`@Transactional(readOnly = true)`), no recibe parámetros y calcula todo con la fecha de hoy y un tope de 5 filas por bloque (`LimitesTecnicos.PANEL_ENCARGADO_DEPOSITO_FILAS_POR_BLOQUE`). Devuelve `DashboardEncargadoDepositoResponseDTO` con:

- **Órdenes pendientes:** las órdenes `PENDIENTE` (`buscarPendientesParaPanel`), por fecha de entrega estimada ascendente y, a igualdad, por ID, con total real y hasta 5 filas (ID, razón social del proveedor y fecha de entrega estimada).
- **Entregas vencidas:** `contarPendientesConEntregaVencida(hoy)`.
- **Lotes vencidos:** los lotes de insumos activos con vencimiento anterior a hoy y cantidad actual mayor a 0 (`buscarVencidosConCantidadParaPanel(hoy, tope)`), por vencimiento ascendente y, a igualdad, por ID.
- **Insumos sin stock:** `filtrarInsumos(null, null, SIN_STOCK_DISPONIBLE, tope)`.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-DED-01** | Sistema vacío | Sin órdenes, lotes ni insumos que cumplan ningún criterio | Todas las consultas devuelven páginas vacías | Los tres bloques tienen `total: 0` y `filas` vacías y `ordenesConEntregaVencida: 0`. |
| **CP-DED-02** | Órdenes pendientes | 9 órdenes Pendiente, dos visibles (una con entrega vencida) | Se piden con el tope de 5 filas y se conserva el orden de la página | `total: 9`; las filas en el orden recibido con el proveedor y la fecha de entrega estimada de cada una. |
| **CP-DED-03** | Entregas vencidas | 4 órdenes pendientes con entrega anterior a hoy | La consulta recibe la fecha de hoy | `ordenesConEntregaVencida: 4`. |
| **CP-DED-04** | Lotes vencidos | 12 lotes vencidos con cantidad, uno visible: "Malta Pilsen", lote "L-125", vencido hace 30 días, `7.5` kg | La consulta recibe la fecha de hoy y el tope de 5 filas | `total: 12`; la fila lleva el ID, el nombre y la unidad del insumo, la identificación del lote, el vencimiento y la cantidad actual. |
| **CP-DED-05** | Insumos sin stock | 6 insumos sin stock disponible, uno visible: "Lúpulo Cascade" | Se pide la situación sin stock disponible con el tope de 5 filas | `total: 6`; la fila lleva el ID, el nombre y el tipo. |
