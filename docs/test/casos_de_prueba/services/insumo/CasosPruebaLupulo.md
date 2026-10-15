### 1. `filtrarLupulos(String nombre, FormatoLupulo formato, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FL-01**|Filtra por nombre y formato informados|`nombre: "Cascade"`, `formato: PELLET`, `pageable: PageRequest.of(0, 10)`, BD con 2 lúpulos activos que cumplen ambos criterios|`filtrarLupulos("Cascade", PELLET, pageable)` contiene elementos|Retorna `Page<LupuloResponseDTO>` con 2 elementos mapeados.|
|**CP-FL-02**|Nombre y formato nulos no restringen la búsqueda|`nombre: null`, `formato: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<LupuloResponseDTO>` con todos los lúpulos activos (equivalente a no filtrar).|
|**CP-FL-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `formato: null`, `pageable: PageRequest.of(0, 10)`|`filtrarLupulos("Inexistente", null, pageable)` está vacío|Retorna `Page<LupuloResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FL-04**|El nombre buscado se normaliza|`nombre: "   Cascade   Americano  "`, `formato: null`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `nombre: "Cascade Americano"`.|
|**CP-FL-05**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `formato: null`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Lúpulo encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `LupuloResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Lúpulo inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el lúpulo con ID: 99".|

### 3. `altaLupulo(LupuloFormDTO formDTO)`

Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25% (el límite inferior es exclusivo, por eso el mensaje no usa la fórmula "debe estar entre"; mensaje para un valor mayor a 25 o infinito positivo: "Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%"). Si falta, el mensaje es "Los ácidos alfa son obligatorios"; si es cero, negativo o infinito negativo, "Los ácidos alfa deben ser mayores a 0". Un valor NaN se rechaza con "Los ácidos alfa deben ser un número mayor a 0" y el nombre no puede superar 100 caracteres (mensaje: "El nombre no puede superar 100 caracteres").

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AL-01**|Ácidos alfa nulo|`aa: null`, `nombre: "Cascade"`|`aa == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa son obligatorios"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-02**|Ácidos alfa igual a cero _(Límite inf.)_|`aa: 0`, `nombre: "Cascade"`|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser mayores a 0"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-03**|Ácidos alfa negativo|`aa: -1`, `nombre: "Cascade"`|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser mayores a 0"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-04**|Nombre duplicado|`aa: 6`, `nombre: "Citra"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-05**|Nombre duplicado Case-Insensitive|`aa: 6`, `nombre: "citra"` (Existe `"Citra"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-06**|Alta exitosa y asignación de `GRAMO` _(Camino feliz)_|`aa: 12`, `nombre: "Mosaic"` (Único), `formato: PELLET`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad asignando fijamente `unidadDeMedida = GRAMO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AL-07**|Ácidos alfa en límite inferior entero válido|`aa: 1`, `nombre: "Saaz"`|`aa <= 0 \| aa > 25` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `aa = 1`.|
|**CP-AL-08**|Ácidos alfa mayores a 25 _(Límite sup.)_|`aa: 25.01`, `nombre: "Cascade"`|`aa > 25` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-09**|Ácidos alfa en límite superior válido|`aa: 25`, `nombre: "Super Alpha"`|`aa <= 0 \| aa > 25` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `aa = 25`.|
|**CP-AL-10**|El porcentaje de ácidos alfa es NaN|`aa: Double.NaN` (resto válido)|El porcentaje de ácidos alfa es NaN $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-11**|El porcentaje de ácidos alfa es infinito positivo|`aa: Double.POSITIVE_INFINITY` (resto válido)|El porcentaje de ácidos alfa es infinito positivo $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%"). No consulta la BD.|
|**CP-AL-12**|El porcentaje de ácidos alfa es infinito negativo|`aa: Double.NEGATIVE_INFINITY` (resto válido)|El porcentaje de ácidos alfa es infinito negativo $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser mayores a 0"). No consulta la BD.|
|**CP-AL-13**|El porcentaje de ácidos alfa es exactamente 25 (valor límite válido)|`aa: 25.0` (resto válido)|El porcentaje de ácidos alfa es exactamente 25 (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-AL-14**|El nombre tiene 101 caracteres|`nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-AL-15**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-AL-16**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-AL-17**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-18**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un lúpulo con el nombre 'pilsen'"). No ejecuta `save()`.|
|**CP-AL-19**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_insumo_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un lúpulo con el nombre 'Pilsen Nacional'").|
|**CP-AL-20**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-AL-21**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|

### 4. `modificarLupulo(Long id, LupuloFormDTO formDTO)`

Orden de los chequeos: nombre obligatorio y largo, ácidos alfa, existencia del lúpulo, nombre duplicado entre los lúpulos activos y persistencia. El nombre, los ácidos alfa y el formato son siempre editables: el escalado de los lotes ya iniciados usa la instantánea guardada al registrarlos (ver `CasosPruebaLote.md`, método `registrarLote`) y no cambia.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ML-01**|Falla por ácidos alfa inválidos|`id: 1L`, `aa: 0` (o `null`, `26`)|`aa <= 0 \| aa > 25` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-ML-02**|Lúpulo no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el lúpulo con ID: 99". No persiste.|
|**CP-ML-03**|Nombre en uso por otro lúpulo|`id: 1L`, `nombre: "Citra"` (En uso por ID 2L)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No persiste.|
|**CP-ML-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Cascade Modificado"`, DTO válido|Nombre libre y lúpulo encontrado|Actualiza campos (`nombre`, `formato`, `aa`), mantiene `GRAMO`, guarda y retorna DTO.|
|**CP-ML-05**|Conservar nombre propio actual|`id: 1L`, `nombre: "cascade"` (Mismo lúpulo)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|
|**CP-ML-06**|El porcentaje de ácidos alfa es NaN|`id: 1L` (existe), `aa: Double.NaN` (resto válido)|El porcentaje de ácidos alfa es NaN $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los ácidos alfa deben ser un número mayor a 0"). No consulta la BD.|
|**CP-ML-07**|El nombre tiene 101 caracteres|`id: 1L` (existe), `nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-ML-08**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`id: 1L` (existe), `nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-ML-09**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-ML-10**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-ML-11**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("El nombre 'pilsen' ya está en uso por otro lúpulo"). No persiste.|
|**CP-ML-12**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_insumo_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("El nombre 'Pilsen Nacional' ya está en uso por otro lúpulo").|
|**CP-ML-13**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-ML-18**|Cambia solo el nombre|`id: 1L` (existe, `aa: 5.5`, `PELLET`), DTO con otro `nombre` y los mismos `aa` y `formato`|El nombre no colisiona con otro registro activo|Persiste con el nuevo nombre.|
|**CP-ML-19**|Cambian los ácidos alfa aunque haya lotes que usan el lúpulo|`id: 1L` (existe, `aa: 5.5`), DTO con `aa: 6.0`|El cambio no se bloquea por lotes Pendientes o En ejecución|Persiste con el nuevo valor.|

### 5. `bajaLupulo(Long id)`

No se puede dar de baja un lúpulo que forma parte de al menos una receta activa (`versión con esUltimaVersion = true` perteneciente a una receta con `estado = ACTIVO`), ni uno que tenga stock (al menos un lote de insumo con `cantidadActual > 0`).

Orden de los chequeos: receta activa, planificación de producción en estado Pendiente (cualquier versión de la receta), orden de compra en estado Pendiente (cualquier versión del proveedor), catálogo de proveedor activo (última versión de un proveedor activo) y stock.

| **ID**       | **Nombre del Caso**          | **Datos de Entrada (Escenario)** | **Condición Evaluada**                             | **Resultado Esperado**                                                                                         |
| ------------ | ---------------------------- | -------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **CP-BL-01** | Baja de lúpulo inexistente   | `id: 99L` (No existe en BD)      | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el lúpulo con ID: 99". No llama a `save()`. |
| **CP-BL-02** | Lúpulo forma parte de una receta activa | `id: 1L` (Existe), `versionRecetaRepository.existsByDetalleLupuloEnRecetaActiva(1L)` $\rightarrow$ **true** | Existe al menos una receta activa que lo usa | Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque forma parte de al menos una receta activa"). No consulta stock (`verifyNoInteractions(loteInsumoRepository)`) ni ejecuta `save()`. |
| **CP-BL-03** | Lúpulo tiene stock | `id: 1L` (Existe), `versionRecetaRepository.existsByDetalleLupuloEnRecetaActiva(1L)` $\rightarrow$ **false**, `loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)` $\rightarrow$ **true** | No está en ninguna receta activa, pero tiene al menos un lote con cantidad actual mayor a cero | Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque tiene stock"). No ejecuta `save()`. |
| **CP-BL-04** | Baja exitosa _(Baja lógica vía Estado)_ | `id: 1L` (Existe en BD), sin receta activa, planificación pendiente, orden de compra en estado Pendiente, catálogo de proveedor activo ni stock asociados | `findById(1L)` $\rightarrow$ **Presente**, ambas validaciones $\rightarrow$ **FALSE** | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del lúpulo dado de baja. |
|**CP-BL-05**|El lúpulo forma parte de una planificación de producción en estado Pendiente|`id: 1L` (Existe), receta activa $\rightarrow$ **false**, `planificacionProduccionRepository.existsByDetalleLupuloEnPlanificacionPendiente(1L)` $\rightarrow$ **true**|Existe una planificación `PENDIENTE` cuya versión de receta (cualquiera, no solo la última) contiene el insumo|Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque forma parte de una planificación de producción en estado Pendiente"). No consulta órdenes de compra, catálogos ni stock. No llama a `save()`.|
|**CP-BL-06**|El lúpulo se encuentra en una orden de compra en estado Pendiente|`id: 1L` (Existe), receta activa $\rightarrow$ **false**, planificación pendiente $\rightarrow$ **false**, `ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)` $\rightarrow$ **true**|Existe una orden de compra `PENDIENTE` (de cualquier versión del proveedor) con un ítem de ese insumo|Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque se encuentra en al menos una orden de compra en estado Pendiente"). No consulta catálogos ni stock. No llama a `save()`.|
|**CP-BL-07**|El lúpulo se encuentra en el catálogo de un proveedor activo|`id: 1L` (Existe), receta activa $\rightarrow$ **false**, planificación pendiente y orden de compra en estado Pendiente $\rightarrow$ **false**, `catalogoProveedorRepository.existsByInsumoId(1L)` $\rightarrow$ **true**|Existe un ítem de catálogo de la última versión de un proveedor activo con ese insumo|Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque se encuentra asociado al catálogo de al menos un proveedor activo"). No consulta el stock. No llama a `save()`.|

#### Casos manuales contra la base de datos: consultas JPQL nuevas del insumo

Los tests de servicio usan mocks, por lo que las consultas se verifican manualmente contra PostgreSQL.

| ID | Consulta | Escenario en BD | Resultado Esperado |
| --- | --- | --- | --- |
| **CP-BL-M01** | `IPlanificacionProduccionRepository.existsByDetalleLupuloEnPlanificacionPendiente` | Planificación `PENDIENTE` cuya versión de receta contiene el insumo | `TRUE` |
| **CP-BL-M02** | `IPlanificacionProduccionRepository.existsByDetalleLupuloEnPlanificacionPendiente` | Planificación `PENDIENTE` atada a una versión anterior (`esUltimaVersion = false`) de la receta que contiene el insumo | `TRUE` |
| **CP-BL-M03** | `IPlanificacionProduccionRepository.existsByDetalleLupuloEnPlanificacionPendiente` | La planificación que contiene el insumo está `FINALIZADA` o `ANULADA` | `FALSE` |
| **CP-BL-M04** | `IOrdenCompraRepository.existsByInsumoEnOrdenCompraPendiente` | Orden `PENDIENTE` con un ítem cuyo catálogo de proveedor referencia el insumo | `TRUE` |
| **CP-BL-M05** | `IOrdenCompraRepository.existsByInsumoEnOrdenCompraPendiente` | Orden `PENDIENTE` registrada contra una versión anterior del proveedor, con el insumo en el ítem | `TRUE` |
| **CP-BL-M06** | `IOrdenCompraRepository.existsByInsumoEnOrdenCompraPendiente` | La orden con el insumo está `FINALIZADA` o `ANULADA` | `FALSE` |
| **CP-BL-M07** | `ICatalogoProveedorRepository.existsByInsumoId` | Ítem de catálogo con el insumo en la última versión de un proveedor `ACTIVO` | `TRUE` |
| **CP-BL-M08** | `ICatalogoProveedorRepository.existsByInsumoId` | El insumo solo figura en una versión anterior del proveedor, o el proveedor tiene `estado = BAJA` | `FALSE` |
