### 1. `filtrarMaltas(String nombre, TipoMalta tipo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FM-01**|Filtra por nombre y tipo informados|`nombre: "Pilsen"`, `tipo: BASE`, `pageable: PageRequest.of(0, 10)`, BD con 2 maltas activas que cumplen ambos criterios|`filtrarMaltas("Pilsen", BASE, pageable)` contiene elementos|Retorna `Page<MaltaResponseDTO>` con 2 elementos mapeados.|
|**CP-FM-02**|Nombre y tipo nulos no restringen la búsqueda|`nombre: null`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<MaltaResponseDTO>` con todas las maltas activas (equivalente a no filtrar).|
|**CP-FM-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarMaltas("Inexistente", null, pageable)` está vacío|Retorna `Page<MaltaResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Malta encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MaltaResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Malta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La malta no existe".|

### 3. `altaMalta(MaltaFormDTO maltaFormDTO)`

El extracto potencial debe ser mayor a 0% y menor o igual a 90%. Mensaje de error: "El extracto potencial debe ser mayor a 0% y menor o igual a 90%".

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AM-01**|Extracto potencial nulo|`potencialExtracto: null`, `nombre: "Pilsen"`|`potencialExtracto == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-02**|Extracto potencial negativo|`potencialExtracto: -1`, `nombre: "Pilsen"`|`potencialExtracto <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-03**|Extracto potencial mayor a 90 _(Límite sup.)_|`potencialExtracto: 91`, `nombre: "Pilsen"`|`potencialExtracto > 90` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-04**|Nombre duplicado|`potencialExtracto: 80`, `nombre: "Caramelo 60"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-05**|Nombre duplicado Case-Insensitive|`potencialExtracto: 80`, `nombre: "caramelo 60"` (Existe `"Caramelo 60"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-06**|Alta exitosa y asignación de `KILOGRAMO` _(Camino feliz)_|`potencialExtracto: 80`, `nombre: "Chocolate"` (Único), `tipo: BASE`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad asignando fijamente `unidadDeMedida = KILOGRAMO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AM-07**|Extracto potencial en límite inferior entero válido|`potencialExtracto: 1`, `nombre: "Malta Roasted"`|`potencialExtracto <= 0 \| potencialExtracto > 90` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `potencialExtracto = 1`.|
|**CP-AM-08**|Extracto potencial en límite superior válido|`potencialExtracto: 90`, `nombre: "Malta Pale Ale"`|`potencialExtracto <= 0 \| potencialExtracto > 90` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `potencialExtracto = 90`.|
|**CP-AM-09**|Extracto potencial igual a cero _(Límite inf.)_|`potencialExtracto: 0`, `nombre: "Pilsen"`|`potencialExtracto <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|

### 4. `modificarMalta(Long id, MaltaFormDTO maltaFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MM-01**|Falla por extracto potencial inválido|`id: 1L`, `potencialExtracto: -5` (o `null`, `0`, `95`)|`potencialExtracto <= 0 \| potencialExtracto > 90` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MM-02**|Malta no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La malta no existe". No persiste.|
|**CP-MM-03**|Nombre en uso por otra malta|`id: 1L`, `nombre: "Pilsen"` (En uso por ID 2L)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No persiste.|
|**CP-MM-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Pilsen Nacional"`, DTO válido|Nombre libre y malta encontrada|Actualiza campos (`nombre`, `tipo`, `potencialExtracto`), mantiene `KILOGRAMO`, guarda y retorna DTO.|
|**CP-MM-05**|Conservar nombre propio actual|`id: 1L`, `nombre: "pilsen"` (Misma malta)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|

### 5. `bajaMalta(Long id)`

No se puede dar de baja una malta que forma parte de al menos una receta activa (`versión con esUltimaVersion = true` perteneciente a una receta con `estado = ACTIVO`), ni una que tenga stock (al menos un lote de insumo con `cantidadActual > 0`).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BM-01**|Baja de malta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la malta con ID: 99". No llama a `save()`.|
|**CP-BM-02**|Malta forma parte de una receta activa|`id: 1L` (Existe), `versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)` $\rightarrow$ **true**|Existe al menos una receta activa que la usa|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque forma parte de al menos una receta activa"). No consulta stock (`verifyNoInteractions(loteInsumoRepository)`) ni ejecuta `save()`.|
|**CP-BM-03**|Malta tiene stock|`id: 1L` (Existe), `versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)` $\rightarrow$ **false**, `loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)` $\rightarrow$ **true**|No está en ninguna receta activa, pero tiene al menos un lote con cantidad actual mayor a cero|Lanza `ReglaNegocioException` ("No se puede dar de baja la malta porque tiene stock"). No ejecuta `save()`.|
|**CP-BM-04**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD), sin receta activa ni stock asociados|`findById(1L)` $\rightarrow$ **Presente**, ambas validaciones $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la malta dada de baja.|