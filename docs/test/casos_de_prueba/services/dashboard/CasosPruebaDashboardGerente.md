## Casos de prueba de `DashboardGerenteServicioImpl`

**Tipo de prueba:** unitaria con Mockito (`@ExtendWith(MockitoExtension.class)`, clase `DashboardGerenteServicioImplTest`). Se simulan `ILoteRepository`, `IControlEquipamientoServicio`, `IControlBarrilServicio` e `IPlanificacionProduccionServicio`.

### 1. `obtenerDashboardGerente()`

Alimenta el panel del gerente de producción. Es de solo lectura (`@Transactional(readOnly = true)`), no recibe parámetros y calcula todo con la fecha de hoy y un tope de 5 filas por bloque (`LimitesTecnicos.PANEL_GERENTE_FILAS_POR_BLOQUE`). Devuelve `DashboardGerenteResponseDTO` con:

- **Contadores:** lotes en ejecución (total de la página de un registro), lotes atrasados (`contarLotesEnEjecucionAtrasados(hoy)`), fermentadores En uso sobre el total de fermentadores activos y equipos y barriles En limpieza o En mantenimiento (suma de los conteos por estado).
- **Producción en curso:** una entrada por cada `TipoEtapa` con lotes En ejecución, en el orden de las etapas, cada una con su total real y hasta 5 filas, con el atraso (`MetodosCronograma.calcularDiasDeAtraso`) y la prioridad.
- **Cola de fermentadores:** los fermentadores activos (primera página de `filtrarEquipamientosParaControl` con tipo Fermentador) con su lote En ejecución, su próximo lote Pendiente y la cantidad de pendientes, a partir de una sola llamada a `buscarVivosDeFermentadores`.
- **Conteos por estado:** equipos (4 estados) y barriles (6 estados), siempre todos, con una página de un registro por estado.
- **Planificaciones pendientes:** con total real y hasta 5 filas.

Los lotes de cada página se cargan con `buscarParaListadoPorIds` y se devuelven en el orden de los IDs de la página; un bloque sin IDs no consulta lotes.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-DGE-01** | Sistema vacío | Sin lotes, equipos, barriles ni planificaciones | Todas las consultas devuelven páginas vacías | Contadores en `0`, `produccionEnCurso` vacía, bloques con `total: 0` y sin filas, 4 conteos de equipos y 6 de barriles, todos en `0`. No carga lotes ni consulta lotes vivos de fermentadores. |
| **CP-DGE-02** | Contadores | 7 lotes en ejecución, 2 atrasados, 5 fermentadores de los cuales 3 En uso, 2 equipos En limpieza y 1 En mantenimiento, 4 barriles En limpieza y 3 En mantenimiento | Cada contador sale de su consulta | `lotesEnEjecucion: 7`, `lotesAtrasados: 2`, `fermentadoresOcupados: 3`, `fermentadoresTotal: 5`, `fueraDeServicio: 10` (2 + 1 + 4 + 3). |
| **CP-DGE-03** | Producción en curso por etapa | Lotes En ejecución en Hervido (1) y en Fermentación (8, con 2 visibles: uno atrasado 4 días con prioridad Alta y uno en plazo con prioridad Baja) | Se agrupa por etapa, en el orden de las etapas, y se omiten las etapas sin lotes | Dos etapas: Hervido y Fermentación. Fermentación informa `total: 8`, las filas en el orden de la página, el atraso `4` y `0`, y la prioridad de cada planificación. |
| **CP-DGE-04** | Cola de un fermentador | Dos fermentadores visibles de 9 en total; el primero con un lote En ejecución atrasado 2 días y dos lotes Pendiente | Se arma la cola con una sola consulta de lotes vivos | `total: 9`. La fila del primero trae el lote en ejecución con atraso `2`, el primer pendiente como próximo lote (atraso `0`) y `lotesPendientes: 2`. Consulta una sola vez `buscarVivosDeFermentadores` con los IDs de los dos fermentadores. |
| **CP-DGE-05** | Fermentador sin lotes | Un fermentador sin lotes vivos | No tiene lote en ejecución ni pendientes | `loteEnEjecucion` y `proximoLote` nulos y `lotesPendientes: 0`. |
| **CP-DGE-06** | Equipos por estado | Equipos: 6 Disponible y 3 En uso, ninguno En limpieza ni En mantenimiento | Siempre se informan los cuatro estados, en orden | Códigos `DISPONIBLE`, `EN_USO`, `EN_LIMPIEZA`, `EN_MANTENIMIENTO`, etiquetas "Disponible", "En uso", "En limpieza" y "En mantenimiento" y totales `6`, `3`, `0`, `0`. |
| **CP-DGE-07** | Barriles por estado | Barriles: 8 Con cerveza y 2 Despachado, el resto en 0 | Siempre se informan los seis estados, en orden | Códigos `DISPONIBLE`, `EN_USO`, `CON_CERVEZA`, `DESPACHADO`, `EN_LIMPIEZA`, `EN_MANTENIMIENTO`, totales `0`, `0`, `8`, `2`, `0`, `0` y la etiqueta "Con cerveza" en el tercero. |
| **CP-DGE-08** | Planificaciones pendientes | 12 planificaciones Pendiente, una visible | Se pide estado Pendiente con el tope de 5 filas | `total: 12` y una fila con ID, prioridad y fechas estimadas de la planificación. |
| **CP-DGE-11** | Lote que la carga no devuelve | La página de lotes en ejecución en Hervido trae los IDs 1 y 2 y la carga devuelve solo el 2 | Se omite el lote faltante | La etapa Hervido informa `total: 2` y una sola fila, la del ID 2. El service deja una advertencia en el log. |
