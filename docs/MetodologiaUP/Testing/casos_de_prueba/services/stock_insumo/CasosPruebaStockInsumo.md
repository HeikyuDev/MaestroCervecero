## Casos de prueba de `StockInsumoServicioImpl`

**Tipo de prueba:** unitaria con Mockito (clase `StockInsumoServicioImplTest`), con los repositorios simulados. El servicio es de solo lectura, toma la fecha de hoy una sola vez por consulta, y delega el orden en las consultas (el orden del `Pageable` se ignora). Un insumo dado de baja se trata como inexistente: la búsqueda del insumo por ID y la del lote con su insumo devuelven vacío.

**Consultas de repositorio (verificación manual contra la base de datos).** Las consultas JPQL no pueden ejercitarse con los repositorios simulados: `filtrarInsumosParaStock`, `sumarStockPorInsumo`, `filtrarLotesDeInsumo`, `buscarConInsumoActivoPorId`, `IReservaInsumoRepository.filtrarPorLoteInsumo` e `IConsumoInsumoRepository.filtrarPorLoteInsumo` se verifican ejecutándolas contra PostgreSQL con datos de prueba (las pantallas de stock en el navegador, con la base de pruebas). Lo que debe comprobarse: el orden alfabético y estable de los insumos, los tres filtros de situación, el conteo de la paginación, las sumas por insumo con la fecha de hoy, el orden FEFO de los lotes (también con stock en 0 y vencidos) y el orden de las reservas y los consumos con el último registrado primero.

---

### 1. `filtrarInsumos(String nombre, TipoInsumo tipo, SituacionStockInsumo situacionStock, Pageable pageable)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-01** | Criterios pasados al repositorio | `nombre: "   Pil_sen   100% "`, `tipo: MALTA`, `situacionStock: null` | Normaliza el nombre, escapa los comodines de `LIKE`, traduce el tipo a su clase y pasa hoy | Consulta con `"Pil!_sen 100!%"`, `MaltaEntity.class`, las tres banderas en `false` y `hoy = LocalDate.now()`. |
| **CP-SSI-02** | Sin criterios | Nombre `null` y nombre en blanco, tipo y situación `null` | Un criterio nulo no restringe | Consulta con nombre `null`, tipo `null` y las tres banderas en `false`. |
| **CP-SSI-03** | Situación de stock | `CON_STOCK_DISPONIBLE`, `SIN_STOCK_DISPONIBLE` y `CON_VENCIDOS` | Cada situación enciende solo su bandera | `(true, false, false)`, `(false, true, false)` y `(false, false, true)`. |
| **CP-SSI-04** | Cantidades de la página y insumos sin lotes | Página con una malta con lotes (6, 4, 7, 17) y un lúpulo sin lotes | Una sola consulta de sumas para los IDs de la página | La malta informa sus cuatro cantidades; el lúpulo, todas en 0; se consulta `sumarStockPorInsumo([1, 2], hoy)` una vez. |
| **CP-SSI-05** | Página vacía | Ningún insumo coincide | No hay insumos para sumar | Página vacía y no se consulta el repositorio de lotes. |
| **CP-SSI-06** | El orden del `Pageable` se ignora | `PageRequest.of(1, 8, Sort.by(DESC, "id"))` | El orden lo define la consulta | El repositorio recibe página `1`, tamaño `8` y sin orden. |
| **CP-SSI-07** | Página fuera de rango | Se pide la página 5 y solo hay 2 páginas | `Paginacion` repite la consulta con la última | Retorna la página `1` con su contenido. |

### 2. `obtenerStockInsumo(Long idInsumo)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-08** | Resumen de un insumo | `findById(1L)` devuelve una malta; las sumas son (6, 4, 0, 10) | El resumen suma los lotes del insumo | Retorna el insumo con `disponible: 6` y `total: 10`. |
| **CP-SSI-09** | Resumen de un insumo sin lotes | Insumo sin filas de suma | Sin lotes | Las cuatro cantidades son 0. |
| **CP-SSI-10** | Insumo inexistente o dado de baja | `findById(99L)` $\rightarrow$ **Optional.empty()** (la consulta del insumo solo devuelve activos) | No existe un insumo activo | Lanza `RecursoNoEncontradoException` ("No se encontró el insumo con ID: 99") y no consulta los lotes. |

### 3. `filtrarLotesDeInsumo(Long idInsumo, Pageable pageable)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-11** | Lotes de un insumo | Lotes vencido, vigente que vence hoy y sin stock, en ese orden | Conserva el orden de la consulta, calcula las cantidades y no pasa orden | Retorna los tres en el mismo orden; el vencido con `vencido: true` y `vencida: 7`; el vigente con `disponible: 6`; el vacío con `total: 0`; el `Pageable` no tiene orden. |
| **CP-SSI-12** | Lotes de un insumo inexistente o dado de baja | `findById(99L)` $\rightarrow$ **Optional.empty()** | No existe un insumo activo | Lanza `RecursoNoEncontradoException` ("No se encontró el insumo con ID: 99") y no consulta los lotes. |
| **CP-SSI-13** | Lotes: página fuera de rango | Se pide la página 4 y solo hay 2 páginas | `Paginacion` repite la consulta con la última | Retorna la página `1`. |

### 4. `obtenerDetalleLoteInsumo(Long idLoteInsumo)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-14** | Cabecera de un lote | Lote 10 de la malta 1 con `10`, `4` y vencimiento futuro | Mapea el lote con su insumo y cantidades | Informa `idInsumo: 1`, `nombreInsumo`, `disponible: 6`, `reservada: 4` y `vencido: false`. |
| **CP-SSI-15** | Cabecera de un lote inexistente o de un insumo dado de baja | `buscarConInsumoActivoPorId(99L)` $\rightarrow$ **Optional.empty()** | El lote no existe o su insumo está dado de baja | Lanza `RecursoNoEncontradoException` ("No se encontró el lote de insumo con ID: 99"). |

### 5. `filtrarReservasDeLoteInsumo(Long idLoteInsumo, Pageable pageable)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-16** | Reservas de un lote | Dos reservas del lote 10 en el orden de la consulta | Conserva el orden y no pasa orden | Retorna las reservas 21 y 20, mapeadas, con `Pageable` sin orden. |
| **CP-SSI-17** | Reservas de un lote inexistente o de un insumo dado de baja | `buscarConInsumoActivoPorId(99L)` $\rightarrow$ **Optional.empty()** | El lote no existe o su insumo está dado de baja | Lanza `RecursoNoEncontradoException` y no consulta las reservas. |

### 6. `filtrarConsumosDeLoteInsumo(Long idLoteInsumo, Pageable pageable)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-18** | Consumos de un lote | Un consumo directo registrado y uno reservado anulado, en el orden de la consulta | Conserva el orden y no pasa orden | Retorna los consumos 31 y 30, mapeados, con `Pageable` sin orden. |
| **CP-SSI-19** | Consumos de un lote inexistente o de un insumo dado de baja | `buscarConInsumoActivoPorId(99L)` $\rightarrow$ **Optional.empty()** | El lote no existe o su insumo está dado de baja | Lanza `RecursoNoEncontradoException` y no consulta los consumos. |

### 7. Reservas y consumos con la página fuera de rango

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SSI-20** | Reservas y consumos: página fuera de rango | Se pide la página 9 y solo hay 2 páginas | `Paginacion` repite la consulta con la última | Ambas listas retornan la página `1`. |
