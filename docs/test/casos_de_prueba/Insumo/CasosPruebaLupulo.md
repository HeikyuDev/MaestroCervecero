### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 2 lúpulos activos|`findAll(pageable)` contiene elementos|Retorna `Page<LupuloResponseDTO>` con 2 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<LupuloResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Lúpulo encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `LupuloResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Lúpulo inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El lúpulo no existe".|

### 3. `altaLupulo(LupuloFormDTO formDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AL-01**|Alfa ácidos nulo|`aa: null`, `nombre: "Cascade"`|`aa == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-02**|Alfa ácidos igual a cero _(Límite)_|`aa: 0`, `nombre: "Cascade"`|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-03**|Alfa ácidos negativo _(Límite inf.)_|`aa: -1`, `nombre: "Cascade"`|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-04**|Nombre duplicado|`aa: 6`, `nombre: "Citra"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-05**|Nombre duplicado Case-Insensitive|`aa: 6`, `nombre: "citra"` (Existe `"Citra"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-06**|Alta exitosa y asignación de `GRAMO` _(Camino feliz)_|`aa: 12`, `nombre: "Mosaic"` (Único), `formato: PELLET`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad asignando fijamente `unidadDeMedida = GRAMO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AL-07**|Alfa ácidos en límite inferior entero válido|`aa: 1`, `nombre: "Saaz"`|`aa <= 0` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `aa = 1`.|

### 4. `modificarLupulo(Long id, LupuloFormDTO formDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ML-01**|Falla por alfa ácidos inválido|`id: 1L`, `aa: 0` (o `null`)|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-ML-02**|Lúpulo no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El lúpulo no existe". No persiste.|
|**CP-ML-03**|Nombre en uso por otro lúpulo|`id: 1L`, `nombre: "Citra"` (En uso por ID 2L)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No persiste.|
|**CP-ML-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Cascade Modificado"`, DTO válido|Nombre libre y lúpulo encontrado|Actualiza campos (`nombre`, `formato`, `aa`), mantiene `GRAMO`, guarda y retorna DTO.|
|**CP-ML-05**|Conservar nombre propio actual|`id: 1L`, `nombre: "cascade"` (Mismo lúpulo)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|

### 5. `bajaLupulo(Long id)`

| **ID**       | **Nombre del Caso**          | **Datos de Entrada (Escenario)** | **Condición Evaluada**                             | **Resultado Esperado**                                                                                         |
| ------------ | ---------------------------- | -------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **CP-BL-01** | Baja de lúpulo inexistente   | `id: 99L` (No existe en BD)      | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el lúpulo con ID: 99". No llama a `save()`. |
| **CP-BL-02** | Baja exitosa _(Baja lógica vía Estado)_ | `id: 1L` (Existe en BD)          | `findById(1L)` $\rightarrow$ **Presente**          | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del lúpulo dado de baja.                                                 |