## Casos de prueba de `DashboardEncargadoDistribucionServicioImpl`

**Tipo de prueba:** unitaria con Mockito (`@ExtendWith(MockitoExtension.class)`, clase `DashboardEncargadoDistribucionServicioImplTest`). Se simulan `IDespachoBarrilRepository`, `ISolicitudBusquedaRepository` e `IControlBarrilServicio`.

### 1. `obtenerDashboardEncargadoDistribucion()`

Alimenta el panel del encargado de distribución. Es de solo lectura (`@Transactional(readOnly = true)`), no recibe parámetros y calcula todo con la fecha de hoy y un tope de 5 filas por bloque (`LimitesTecnicos.PANEL_ENCARGADO_DISTRIBUCION_FILAS_POR_BLOQUE`). Devuelve `DashboardEncargadoDistribucionResponseDTO` con:

- **Barriles en poder de clientes:** el total de `filtrarBarrilesParaControl` con estado `DESPACHADO` (página de un registro).
- **Devoluciones vencidas y que vencen hoy:** `contarVigentesConDevolucionAnteriorA(hoy)` y `contarVigentesConDevolucionEn(hoy)`.
- **Devoluciones pendientes:** los despachos vigentes con devolución estimada de hoy o anterior (`buscarVigentesConDevolucionHastaParaPanel`), más atrasado primero, con el total real, hasta 5 filas y los días de atraso de cada una (0 si vence hoy).
- **Retiros pendientes:** las solicitudes de búsqueda pendientes de despachos vigentes (`buscarPendientesParaPanel`), la de fecha propuesta más antigua primero, con el total real, hasta 5 filas y si la fecha propuesta ya pasó; y la cantidad de vencidas (`contarPendientesVencidas(ahora)`).
- **Clientes con barriles:** una fila por cliente con la cantidad de barriles y su próxima fecha de devolución (`buscarClientesConBarrilesEnPoderParaPanel`).
- **Barriles listos:** `filtrarBarrilesParaControl` con estado `CON_CERVEZA`, con la receta y el lote de la cerveza cuando el barril los informa.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-DDI-01** | Sistema vacío | Sin barriles despachados ni con cerveza | Todas las consultas devuelven páginas vacías o 0 | Los contadores en `0` (también los retiros vencidos) y los cuatro bloques con `total: 0` y sin filas. |
| **CP-DDI-02** | Contadores | 8 barriles Despachados, 3 devoluciones anteriores a hoy y 1 de hoy | Cada contador sale de su consulta con la fecha de hoy | `barrilesEnPoderDeClientes: 8`, `devolucionesVencidas: 3` y `devolucionesQueVencenHoy: 1`. |
| **CP-DDI-03** | Devoluciones pendientes | 9 devoluciones de hoy o vencidas, dos visibles: una de hace 3 días y otra de hoy | Se piden con la fecha de hoy y el tope de 5 filas | `total: 9`; las filas en el orden recibido con el ID y el identificador del barril, el cliente y la fecha de devolución; `diasDeAtraso: 3` y `0`. |
| **CP-DDI-04** | Clientes con barriles | 9 clientes con barriles, dos visibles: uno con 2 barriles y otro con 1 | Cada cliente aparece una vez con su cantidad y su próxima devolución | `total: 9`; cada fila lleva el ID, el nombre, la cantidad de barriles y la próxima fecha de devolución. |
| **CP-DDI-05** | Barriles listos | 7 barriles Con cerveza, dos visibles: uno con su cerveza y otro sin contenido informado | Se piden con estado Con cerveza y el tope de 5 filas | `total: 7`; el primero lleva la receta, el lote y el contenido; el segundo tiene la receta y el lote nulos. |
| **CP-DDI-06** | Retiros pendientes | 6 solicitudes de búsqueda pendientes de despachos vigentes, dos visibles: una con la fecha propuesta 2 horas atrás y otra para dentro de 2 días; 2 vencidas en total | Se piden con el tope de 5 filas y los vencidos se cuentan con el momento actual | `retirosVencidos: 2`, `total: 6`; la primera fila lleva la solicitud, el barril (ID y número de serie), el cliente y `vencida: true`; la segunda `vencida: false`. |
