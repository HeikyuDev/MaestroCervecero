### 1. `filtrarMaltas(String nombre, TipoMalta tipo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FM-01**|Filtra por nombre y tipo informados|`nombre: "Pilsen"`, `tipo: BASE`, `pageable: PageRequest.of(0, 10)`, BD con 2 maltas activas que cumplen ambos criterios|`filtrarMaltas("Pilsen", BASE, pageable)` contiene elementos|Retorna `Page<MaltaResponseDTO>` con 2 elementos mapeados.|
|**CP-FM-02**|Nombre y tipo nulos no restringen la búsqueda|`nombre: null`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<MaltaResponseDTO>` con todas las maltas activas (equivalente a no filtrar).|
|**CP-FM-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarMaltas("Inexistente", null, pageable)` está vacío|Retorna `Page<MaltaResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FM-04**|El nombre buscado se normaliza|`nombre: "   Pilsen   Nacional  "`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `nombre: "Pilsen Nacional"`.|
|**CP-FM-05**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Malta encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MaltaResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Malta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la malta con ID: 99".|

### 3. `altaMalta(MaltaFormDTO maltaFormDTO)`

El extracto potencial debe ser mayor a 0% y menor o igual a 90%. Si falta, el mensaje es "El extracto potencial es obligatorio"; si es cero o negativo, "El extracto potencial debe ser mayor a 0"; si supera el máximo, "El extracto potencial debe ser mayor a 0% y menor o igual a 90%". El nombre no puede superar 100 caracteres (mensaje: "El nombre no puede superar 100 caracteres").

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AM-01**|Extracto potencial nulo|`potencialExtracto: null`, `nombre: "Pilsen"`|`potencialExtracto == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El extracto potencial es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-02**|Extracto potencial negativo|`potencialExtracto: -1`, `nombre: "Pilsen"`|`potencialExtracto <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El extracto potencial debe ser mayor a 0"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-03**|Extracto potencial mayor a 90 _(Límite sup.)_|`potencialExtracto: 91`, `nombre: "Pilsen"`|`potencialExtracto > 90` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El extracto potencial debe ser mayor a 0% y menor o igual a 90%"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-04**|Nombre duplicado|`potencialExtracto: 80`, `nombre: "Caramelo 60"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-05**|Nombre duplicado Case-Insensitive|`potencialExtracto: 80`, `nombre: "caramelo 60"` (Existe `"Caramelo 60"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-06**|Alta exitosa y asignación de `KILOGRAMO` _(Camino feliz)_|`potencialExtracto: 80`, `nombre: "Chocolate"` (Único), `tipo: BASE`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad asignando fijamente `unidadDeMedida = KILOGRAMO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AM-07**|Extracto potencial en límite inferior entero válido|`potencialExtracto: 1`, `nombre: "Malta Roasted"`|`potencialExtracto <= 0 \| potencialExtracto > 90` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `potencialExtracto = 1`.|
|**CP-AM-08**|Extracto potencial en límite superior válido|`potencialExtracto: 90`, `nombre: "Malta Pale Ale"`|`potencialExtracto <= 0 \| potencialExtracto > 90` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `potencialExtracto = 90`.|
|**CP-AM-09**|Extracto potencial igual a cero _(Límite inf.)_|`potencialExtracto: 0`, `nombre: "Pilsen"`|`potencialExtracto <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El extracto potencial debe ser mayor a 0"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-10**|El nombre tiene 101 caracteres|`nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-AM-11**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-AM-12**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-AM-13**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-14**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe una malta con el nombre 'pilsen'"). No ejecuta `save()`.|
|**CP-AM-15**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_insumo_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe una malta con el nombre 'Pilsen Nacional'").|
|**CP-AM-16**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-AM-17**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|

### 4. `modificarMalta(Long id, MaltaFormDTO maltaFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MM-01**|Falla por extracto potencial inválido|`id: 1L`, `potencialExtracto: -5` (o `null`, `0`, `95`)|`potencialExtracto <= 0 \| potencialExtracto > 90` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MM-02**|Malta no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la malta con ID: 99". No persiste.|
|**CP-MM-03**|Nombre en uso por otra malta|`id: 1L`, `nombre: "Pilsen"` (En uso por ID 2L)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No persiste.|
|**CP-MM-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Pilsen Nacional"`, DTO válido|Nombre libre y malta encontrada|Actualiza campos (`nombre`, `tipo`, `potencialExtracto`), mantiene `KILOGRAMO`, guarda y retorna DTO.|
|**CP-MM-05**|Conservar nombre propio actual|`id: 1L`, `nombre: "pilsen"` (Misma malta)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|
|**CP-MM-06**|El nombre tiene 101 caracteres|`id: 1L` (existe), `nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-MM-07**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`id: 1L` (existe), `nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-MM-08**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-MM-09**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-MM-10**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("El nombre 'pilsen' ya está en uso por otra malta"). No persiste.|
|**CP-MM-11**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_insumo_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("El nombre 'Pilsen Nacional' ya está en uso por otra malta").|
|**CP-MM-12**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|

### 5. `bajaMalta(Long id)`

No se puede dar de baja una malta que forma parte de al menos una receta activa (`versión con esUltimaVersion = true` perteneciente a una receta con `estado = ACTIVO`), ni una que tenga stock (al menos un lote de insumo con `cantidadActual > 0`).

Orden de los chequeos: receta activa, planificación de producción en estado Pendiente (cualquier versión de la receta), orden de compra en estado Pendiente (cualquier versión del proveedor), catálogo de proveedor activo (última versión de un proveedor activo) y stock.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BM-01**|Baja de malta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la malta con ID: 99". No llama a `save()`.|
|**CP-BM-02**|Malta forma parte de una receta activa|`id: 1L` (Existe), `versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)` $\rightarrow$ **true**|Existe al menos una receta activa que la usa|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque forma parte de al menos una receta activa"). No consulta stock (`verifyNoInteractions(loteInsumoRepository)`) ni ejecuta `save()`.|
|**CP-BM-03**|Malta tiene stock|`id: 1L` (Existe), `versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)` $\rightarrow$ **false**, `loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)` $\rightarrow$ **true**|No está en ninguna receta activa, pero tiene al menos un lote con cantidad actual mayor a cero|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque tiene stock"). No ejecuta `save()`.|
|**CP-BM-04**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD), sin receta activa, planificación pendiente, orden de compra en estado Pendiente, catálogo de proveedor activo ni stock asociados|`findById(1L)` $\rightarrow$ **Presente**, ambas validaciones $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la malta dada de baja.|
|**CP-BM-05**|La malta forma parte de una planificación de producción en estado Pendiente|`id: 1L` (Existe), receta activa $\rightarrow$ **false**, `planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(1L)` $\rightarrow$ **true**|Existe una planificación `PENDIENTE` cuya versión de receta (cualquiera, no solo la última) contiene el insumo|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque forma parte de una planificación de producción en estado Pendiente"). No consulta órdenes de compra, catálogos ni stock. No llama a `save()`.|
|**CP-BM-06**|La malta se encuentra en una orden de compra en estado Pendiente|`id: 1L` (Existe), receta activa $\rightarrow$ **false**, planificación pendiente $\rightarrow$ **false**, `ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)` $\rightarrow$ **true**|Existe una orden de compra `PENDIENTE` (de cualquier versión del proveedor) con un ítem de ese insumo|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque se encuentra en al menos una orden de compra en estado Pendiente"). No consulta catálogos ni stock. No llama a `save()`.|
|**CP-BM-07**|La malta se encuentra en el catálogo de un proveedor activo|`id: 1L` (Existe), receta activa $\rightarrow$ **false**, planificación pendiente y orden de compra en estado Pendiente $\rightarrow$ **false**, `catalogoProveedorRepository.existsByInsumoId(1L)` $\rightarrow$ **true**|Existe un ítem de catálogo de la última versión de un proveedor activo con ese insumo|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque se encuentra asociada al catálogo de al menos un proveedor activo"). No consulta el stock. No llama a `save()`.|

#### Casos manuales contra la base de datos: consultas JPQL nuevas del insumo

Los tests de servicio usan mocks, por lo que las consultas se verifican manualmente contra PostgreSQL.

| ID | Consulta | Escenario en BD | Resultado Esperado |
| --- | --- | --- | --- |
| **CP-BM-M01** | `IPlanificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente` | Planificación `PENDIENTE` cuya versión de receta contiene el insumo | `TRUE` |
| **CP-BM-M02** | `IPlanificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente` | Planificación `PENDIENTE` atada a una versión anterior (`esUltimaVersion = false`) de la receta que contiene el insumo | `TRUE` |
| **CP-BM-M03** | `IPlanificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente` | La planificación que contiene el insumo está `FINALIZADA` o `ANULADA` | `FALSE` |
| **CP-BM-M04** | `IOrdenCompraRepository.existsByInsumoEnOrdenCompraPendiente` | Orden `PENDIENTE` con un ítem cuyo catálogo de proveedor referencia el insumo | `TRUE` |
| **CP-BM-M05** | `IOrdenCompraRepository.existsByInsumoEnOrdenCompraPendiente` | Orden `PENDIENTE` registrada contra una versión anterior del proveedor, con el insumo en el ítem | `TRUE` |
| **CP-BM-M06** | `IOrdenCompraRepository.existsByInsumoEnOrdenCompraPendiente` | La orden con el insumo está `FINALIZADA` o `ANULADA` | `FALSE` |
| **CP-BM-M07** | `ICatalogoProveedorRepository.existsByInsumoId` | Ítem de catálogo con el insumo en la última versión de un proveedor `ACTIVO` | `TRUE` |
| **CP-BM-M08** | `ICatalogoProveedorRepository.existsByInsumoId` | El insumo solo figura en una versión anterior del proveedor, o el proveedor tiene `estado = BAJA` | `FALSE` |
