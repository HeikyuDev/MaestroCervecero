### 1. `filtrarBarriles(String identificador, Double capacidad, EstadoOperativoBarril estadoOperativo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FB-01**|Filtra por identificador, capacidad y estado operativo informados|`identificador: "BAR"`, `capacidad: 50.0`, `estadoOperativo: DISPONIBLE`, `pageable: PageRequest.of(0, 10)`, BD con 2 barriles activos que cumplen los tres criterios|`filtrarBarriles("BAR", 50.0, DISPONIBLE, pageable)` contiene elementos|Retorna `Page<BarrilResponseDTO>` con 2 elementos mapeados.|
|**CP-FB-02**|Los tres parámetros nulos no restringen la búsqueda|`identificador: null`, `capacidad: null`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga los tres parámetros nulos tal cual al repositorio|Retorna `Page<BarrilResponseDTO>` con todos los barriles activos (equivalente a no filtrar).|
|**CP-FB-03**|Consulta sin coincidencias|`identificador: "Inexistente"`, `capacidad: null`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarBarriles("Inexistente", null, null, pageable)` está vacío|Retorna `Page<BarrilResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Barril encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `BarrilResponseDTO` con los datos de la entidad, incluyendo el `fabricante` mapeado.|
|**CP-BI-02**|Barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el barril con ID: 99".|

### 3. `altaBarril(BarrilFormDTO barrilFormDTO)`

Se valida en este orden: capacidad, existencia del fabricante y, recién con el fabricante confirmado, el identificador duplicado para ese fabricante. En todos los casos de falla, el resto de los datos del DTO son válidos.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AB-01**|Capacidad nula|`capacidad: null`|`capacidad == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad es obligatoria."). No consulta `fabricanteBarrilRepository` ni `barrilRepository` (`verifyNoInteractions`).|
|**CP-AB-02**|Capacidad negativa|`capacidad: -1.0`|`capacidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad debe ser mayor a 0."). No consulta la BD.|
|**CP-AB-03**|Capacidad igual a cero _(Límite)_|`capacidad: 0.0`|`capacidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad debe ser mayor a 0."). No consulta la BD.|
|**CP-AB-04**|Fabricante de barril inexistente|`capacidad: 50.0`, `idFabricanteBarril: 99L` (No existe o dado de baja)|`fabricanteBarrilRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el fabricante de barril con ID: 99". No consulta `existsByIdentificadorIgnoreCaseAndFabricanteId`, ni ejecuta `save()`.|
|**CP-AB-05**|Identificador duplicado para el mismo fabricante (activo)|`identificador: "BAR-01"`, `idFabricanteBarril: 1L` (Ya existe un barril activo con ese identificador para ese fabricante)|`existsByIdentificadorIgnoreCaseAndFabricanteId("BAR-01", 1L)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un barril activo con el identificador 'BAR-01' para el fabricante seleccionado". No ejecuta `save()`.|
|**CP-AB-06**|Mismo identificador permitido para un fabricante distinto|`identificador: "BAR-01"`, `idFabricanteBarril: 2L` (El identificador "BAR-01" existe, pero para el fabricante `1L`, no para el `2L`)|`existsByIdentificadorIgnoreCaseAndFabricanteId("BAR-01", 2L)` $\rightarrow$ **FALSE**|La unicidad es relativa al fabricante: persiste el barril con `fabricante.id = 2L` y retorna DTO.|
|**CP-AB-07**|Alta exitosa _(Camino feliz)_|DTO válido: capacidad y fabricante válidos, identificador único para ese fabricante|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `estado = ACTIVO`, `estadoOperativo = DISPONIBLE`, `contenidoActual = 0.0` y el `fabricante` resuelto, y retorna DTO.|

### 4. `modificarBarril(Long id, BarrilFormDTO barrilFormDTO)`

Localiza el barril primero; recién si existe se valida la capacidad, la existencia del fabricante (que puede reasignarse) y la duplicidad del identificador para ese fabricante, excluyendo al propio barril.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MB-01**|Barril no encontrado por ID|`id: 99L` (No existe o dado de baja), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida capacidad, ni consulta `fabricanteBarrilRepository`, ni persiste.|
|**CP-MB-02**|Capacidad nula|`id: 1L` (Existe), `capacidad: null`|`findById(1L)` $\rightarrow$ **Presente**; `capacidad == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad es obligatoria."). No consulta `fabricanteBarrilRepository` ni `existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot`, ni persiste.|
|**CP-MB-03**|Capacidad igual a cero _(Límite)_|`id: 1L` (Existe), `capacidad: 0.0`|`capacidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad debe ser mayor a 0."). No persiste.|
|**CP-MB-04**|Fabricante de barril inexistente (reasignación inválida)|`id: 1L` (Existe), `capacidad: 50.0`, `idFabricanteBarril: 99L` (No existe o dado de baja)|`fabricanteBarrilRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el fabricante de barril con ID: 99". No ejecuta `save()`.|
|**CP-MB-05**|Identificador en uso por otro barril activo del mismo fabricante|`id: 1L` (Existe), `identificador: "BAR-02"` (Pertenece a otro barril activo del mismo fabricante)|`existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot("BAR-02", idFabricanteBarril, 1L)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe otro barril activo con el identificador 'BAR-02' para el fabricante seleccionado". No persiste.|
|**CP-MB-06**|Conservar el identificador y el fabricante propios _(Camino feliz)_|`id: 1L`, `identificador`/`idFabricanteBarril` iguales a los actuales del barril|`...AndIdNot(...)` $\rightarrow$ **FALSE** (se excluye el propio ID)|Permite continuar con la modificación, persiste y retorna DTO.|
|**CP-MB-07**|Reasignación exitosa a otro fabricante _(Camino feliz)_|`id: 1L` (Pertenecía al fabricante `1L`), `idFabricanteBarril: 2L` (Existe, activo), identificador único para el fabricante `2L`|Todas las validaciones $\rightarrow$ **FALSE**|Actualiza `identificador`, `capacidad`, `usosMaximosAntesMantenimiento` y reasigna `fabricante` al `2L`; guarda y retorna DTO con el nuevo fabricante. No modifica `estadoOperativo` ni `contenidoActual`.|

### 5. `bajaBarril(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BB-01**|Baja de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BB-02**|Barril en estado operativo CON_CERVEZA|`id: 1L` (Existe), `estadoOperativo: CON_CERVEZA`|`estadoOperativo == CON_CERVEZA` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("No se puede dar de baja el barril porque se encuentra en estado operativo CON_CERVEZA o DESPACHADO."). No llama a `save()`.|
|**CP-BB-03**|Barril en estado operativo DESPACHADO|`id: 1L` (Existe), `estadoOperativo: DESPACHADO`|`estadoOperativo == DESPACHADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` (mismo mensaje que CP-BB-02). No llama a `save()`.|
|**CP-BB-04**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe), `estadoOperativo: DISPONIBLE` (ni CON_CERVEZA ni DESPACHADO)|Ambas comparaciones $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del barril dado de baja.|
