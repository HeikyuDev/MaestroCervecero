### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 2 maltas activas|`findAll(pageable)` contiene elementos|Retorna `Page<MaltaResponseDTO>` con 2 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<MaltaResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Malta encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MaltaResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Malta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La malta no existe".|

### 3. `altaMalta(MaltaFormDTO maltaFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AM-01**|Rendimiento nulo|`rendimiento: null`, `nombre: "Pilsen"`|`rendimiento == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-02**|Rendimiento negativo _(Límite inf.)_|`rendimiento: -1`, `nombre: "Pilsen"`|`rendimiento < 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-03**|Rendimiento mayor a 100 _(Límite sup.)_|`rendimiento: 101`, `nombre: "Pilsen"`|`rendimiento > 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-04**|Nombre duplicado|`rendimiento: 80`, `nombre: "Caramelo 60"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-05**|Nombre duplicado Case-Insensitive|`rendimiento: 80`, `nombre: "caramelo 60"` (Existe `"Caramelo 60"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-06**|Alta exitosa y asignación de `KILOGRAMO` _(Camino feliz)_|`rendimiento: 80`, `nombre: "Chocolate"` (Único), `tipo: BASE`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad asignando fijamente `unidadDeMedida = KILOGRAMO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AM-07**|Rendimiento en límite inferior entero válido|`rendimiento: 0`, `nombre: "Malta Roasted"`|`rendimiento < 0 \| rendimiento > 100` $ $\rightarr$ **FALSE**|Persiste con éxito y retorna DTO con `rendimiento = 0`.|
|**CP-AM-08**|Rendimiento en límite superior entero válido|`rendimiento: 100`, `nombre: "Malta Pale Ale"`|`rendimiento < 0 \| rendimiento > 100` $ $\rightarr$ **FALSE**|Persiste con éxito y retorna DTO con `rendimiento = 100`.|

### 4. `modificarMalta(Long id, MaltaFormDTO maltaFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MM-01**|Falla por rendimiento inválido|`id: 1L`, `rendimiento: -5` (o `null`, `105`)|`rendimiento < 0 \| rendimiento > 100` $ $\rightarr$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MM-02**|Malta no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La malta no existe". No persiste.|
|**CP-MM-03**|Nombre en uso por otra malta|`id: 1L`, `nombre: "Pilsen"` (En uso por ID 2L)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No persiste.|
|**CP-MM-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Pilsen Nacional"`, DTO válido|Nombre libre y malta encontrada|Actualiza campos (`nombre`, `tipo`, `rendimiento`), mantiene `KILOGRAMO`, guarda y retorna DTO.|
|**CP-MM-05**|Conservar nombre propio actual|`id: 1L`, `nombre: "pilsen"` (Misma malta)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|

### 5. `bajaMalta(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BM-01**|Baja de malta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la malta con ID: 99". No llama a `save()`.|
|**CP-BM-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la malta dada de baja.|