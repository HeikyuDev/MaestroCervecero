### 1. `filtrarLotesInsumoDisponibles(Long idInsumo)`

`idInsumo` lo elige el usuario entre los devueltos por `filtrarInsumosRequeridos`. Se usa para armar la lista de lotes de insumo físico entre los que el operario puede elegir al registrar un consumo directo (`registrarConsumoInsumoDirecto`). Filtra por `(cantidadActual - cantidadReservada) > 0` (cantidad disponible calculada, no persistida) y ordena por `fechaVencimiento` ascendente (FEFO), el mismo criterio que la reserva automática al iniciar un lote. No bloquea: es una consulta de solo lectura para mostrar la lista; el lote que el operario finalmente elija se vuelve a buscar bloqueado recién al confirmar el consumo.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLD-01**|Consulta con lotes disponibles|`idInsumo: 1L`, 2 lotes de insumo con cantidad disponible|`filtrarLotesInsumoDisponibles(1L)` contiene elementos|Retorna `List<LoteInsumoResponseDTO>` con los 2 elementos mapeados, en el mismo orden.|
|**CP-FLD-02**|Consulta sin lotes con cantidad disponible|`idInsumo: 1L`, sin lotes disponibles|`filtrarLotesInsumoDisponibles(1L)` está vacío|Retorna una lista vacía (no lanza excepción).|

### 2. `filtrarLotesInsumoDisponibles(Collection<Long> idsInsumo, String nombreInsumo, String identificacion, Pageable pageable)`

Ofrece, paginados, los lotes de insumo entre los que el operario elige el lote de un consumo directo. `idsInsumo` son los insumos de la etapa (los resuelve el Controller con `filtrarInsumosRequeridos`); si es nulo o vacío no hay nada que ofrecer y devuelve una página vacía sin consultar. Solo aparecen lotes con saldo disponible mayor a 0 (cantidad actual menos reservada, a 4 decimales). `nombreInsumo` (`NormalizacionTexto.nombrePropio`) e `identificacion` (`NormalizacionTexto.codigo`) se buscan por coincidencia parcial sin distinguir mayúsculas, y un texto en blanco equivale a no filtrar. Cada lote informa `vencido` (su fecha de vencimiento es anterior a hoy; si vence hoy todavía es válido), `fechaVencimiento` y `cantidadDisponible`.

**Orden FEFO**, definido por la consulta para que la paginación sea estable (el orden del `Pageable` se ignora): primero los lotes vigentes por fecha de vencimiento ascendente, luego los que no tienen fecha de vencimiento y al final los vencidos, también por fecha de vencimiento ascendente; ante igual fecha, por `id`.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLD-03**|Conserva el orden de la consulta y marca los vencidos|`idsInsumo: [1]`; la consulta devuelve tres lotes: vence en 5 días, vence hoy y vencido ayer|`fechaVencimiento < hoy` solo para el último|Retorna la página con los lotes en el mismo orden; `vencido` es `false`, `false` y `true`; informa `fechaVencimiento` y `cantidadDisponible` (`10 - 4 = 6`).|
|**CP-FLD-04**|Sin insumos de la etapa|`idsInsumo: null` y `idsInsumo: []`|No hay insumos para ofrecer|Retorna una página vacía en ambos casos y no consulta el repositorio.|
|**CP-FLD-05**|Normalización de los textos buscados|`nombreInsumo: "   Malta    Pilsen "`, `identificacion: "  lote-a  "`; y por separado ambos en blanco|El service normaliza los textos; un texto en blanco se reemplaza por `null`|Consulta con `"Malta Pilsen"` y `"LOTE-A"`; y con `null` y `null`.|
|**CP-FLD-06**|El orden del `Pageable` se ignora y se pasa la fecha de hoy|`PageRequest.of(1, 8, Sort.by(DESC, "id"))`|El FEFO lo define la consulta|El repositorio recibe un `Pageable` sin orden, página `1`, tamaño `8` y `hoy = LocalDate.now()`.|
|**CP-FLD-07**|Sin coincidencias|`nombreInsumo: "Inexistente"`|Ningún lote cumple|Retorna una página vacía con `totalElements: 0`.|

**Consulta `filtrarLotesInsumoDisponiblesParaConsumo` (verificación manual contra la base de datos).** No puede ejercitarse con el repositorio mockeado; se verifica ejecutándola contra PostgreSQL con datos de prueba (hoy = 14/10/2026). El contexto de Spring la valida al arrancar.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLD-08**|Orden FEFO: vigentes, luego sin fecha y al final los vencidos|Lotes disponibles del insumo 1: `A` vence 20/10/2026, `B` vence 15/10/2026, `C` vence 14/10/2026 (hoy), `D` venció 01/10/2026, `E` venció 10/09/2026, `F` y `G` vencen ambos 15/10/2026 (`G` con `id` mayor que `F`)|`ORDER BY CASE ... END, fechaVencimiento, id`|Salen en el orden `C`, `B`, `F`, `G`, `A`, `E`, `D`: vigentes por fecha (el que vence hoy es vigente y va primero), con desempate por `id`, y al final los vencidos por fecha ascendente. Ese orden se conserva al paginar de a 2.|
|**CP-FLD-09**|Solo lotes con saldo y de los insumos pedidos|Lotes del insumo 1: uno con `cantidadActual: 10` y `cantidadReservada: 10` (saldo 0), uno con saldo `0.00001` (residuo de punto flotante), uno con saldo `5`; un lote con saldo del insumo 2; `idsInsumo: [1]`|`(cantidadActual - cantidadReservada) > 0.00005` e `i.id IN :idsInsumo`|Devuelve únicamente el lote con saldo `5` del insumo 1. El total de la página es el de la consulta de conteo.|
|**CP-FLD-10**|Criterios de texto|Insumos "Malta Pilsen" y "Malta Caramelo" con lotes "L-123" y "L-124"; consultas con `nombreInsumo: "pils"`, con `identificacion: "L-12"`, con ambos y con ambos `null`|`UPPER(...) LIKE` parcial; un criterio nulo no restringe|Con `"pils"` devuelve los lotes de Malta Pilsen; con `"L-12"` devuelve los lotes cuya identificación contiene el texto; combinados se aplican con `AND`; con `null` devuelve todos los de `idsInsumo`.|

### 3. `filtrarLotesInsumoPorInsumo(Long idInsumo, String identificacion, Pageable pageable)`

Ofrece, paginados, todos los lotes de un insumo, también los agotados, para elegir el lote existente al registrar un ingreso. Si `idInsumo` es nulo devuelve una página vacía sin consultar. `identificacion` (`NormalizacionTexto.codigo`) se busca por coincidencia parcial sin distinguir mayúsculas, y un texto en blanco equivale a no filtrar. Cada lote informa `vencido` (su fecha de vencimiento es anterior a hoy), `fechaVencimiento` y `cantidadActual`. El orden lo define la consulta (fecha de vencimiento ascendente y, ante igual fecha, `id`) y el del `Pageable` se ignora.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLI-01**|Incluye los agotados y marca los vencidos|`idInsumo: 1L`; la consulta devuelve un lote vencido ayer, uno agotado (`cantidadActual: 0`) y uno vigente|`fechaVencimiento < hoy` solo para el primero|Retorna la página con los tres lotes en el mismo orden; `vencido` es `true`, `false` y `false`; el lote agotado se informa con `cantidadActual: 0`.|
|**CP-FLI-02**|Sin insumo|`idInsumo: null`|No hay insumo del que ofrecer lotes|Retorna una página vacía y no consulta el repositorio.|
|**CP-FLI-03**|Normalización del texto buscado|`identificacion: "  lote-a  "`; y por separado `"   "`|El service normaliza el texto; un texto en blanco se reemplaza por `null`|Consulta con `"LOTE-A"`; y con `null`.|
|**CP-FLI-04**|El orden del `Pageable` se ignora|`PageRequest.of(1, 8, Sort.by(DESC, "id"))`|El orden lo define la consulta|El repositorio recibe un `Pageable` sin orden, página `1` y tamaño `8`.|

**Consulta `filtrarLotesInsumoPorInsumo` (verificación manual contra la base de datos).** No puede ejercitarse con el repositorio mockeado; se verifica ejecutándola contra PostgreSQL con datos de prueba. El contexto de Spring la valida al arrancar.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FLI-05**|Todos los lotes del insumo, en orden de vencimiento|Insumo 1 con tres lotes (uno agotado) y el insumo 2 con un lote; consulta con `idInsumo: 1` y `identificacion: null`|`i.id = :idInsumo`, sin condición de saldo, `ORDER BY fechaVencimiento, id`|Devuelve los tres lotes del insumo 1, el agotado incluido, ordenados por vencimiento ascendente con desempate por `id`; no devuelve el lote del insumo 2.|
|**CP-FLI-06**|Criterio de identificación|Lotes "L-123" y "L-124" del insumo 1; consultas con `identificacion: "l-12"`, `"L-124"` y `null`|`UPPER(...) LIKE` parcial; un criterio nulo no restringe|Con `"l-12"` devuelve ambos; con `"L-124"` solo el segundo; con `null` devuelve todos los del insumo.|

### 4. `buscarPorId(Long id)`

Sirve para volver a mostrar el lote de insumo elegido cuando el registro del consumo falla.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BLI-01**|Lote de insumo encontrado|`id: 10L` existente, vencido hace 3 días, con `cantidadActual: 10` y `cantidadReservada: 4`|`findById(10L)` $\rightarrow$ **Presente**|Retorna el `LoteInsumoResponseDTO` con la identificación del proveedor, `cantidadDisponible = 6.0` y `vencido = true`.|
|**CP-BLI-02**|Lote de insumo inexistente|`id: 99L`|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el lote de insumo con ID: 99").|
