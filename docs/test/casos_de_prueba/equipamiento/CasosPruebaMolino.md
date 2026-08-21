### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 2 molinos activos|`findAll(pageable)` contiene elementos|Retorna `Page<MolinoResponseDTO>` con 2 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<MolinoResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Molino encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MolinoResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Molino inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El molino no existe".|

### 3. `altaMolino(MolinoFormDTO molinoFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AM-01**|Rendimiento de molienda negativo _(Límite inf.)_|`rendimiento: -1.0`, `idInterno: "MOL-01"`|`rendimientoMolienda <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-02**|Rendimiento de molienda igual a cero _(Límite)_|`rendimiento: 0.0`, `idInterno: "MOL-01"`|`rendimientoMolienda <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AM-03**|Identificador interno duplicado|`rendimiento: 50.0`, `idInterno: "MOL-01"` (Existe en BD)|`existsByIdentificador...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-04**|Identificador duplicado Case-Insensitive|`rendimiento: 50.0`, `idInterno: "mol-01"` (Existe `"MOL-01"`)|`existsByIdentificador...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AM-05**|Alta exitosa _(Camino feliz)_|`rendimiento: 50.0`, `idInterno: "MOL-02"` (Único)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `estadoOperativo = DISPONIBLE` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AM-06**|Rendimiento en límite inferior válido|`rendimiento: 0.1` (Mayor a 0), `idInterno: "MOL-03"`|`rendimientoMolienda <= 0` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `rendimientoMolienda = 0.1`.|

### 4. `modificarMolino(Long id, MolinoFormDTO molinoFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MM-01**|Falla por rendimiento de molienda menor o igual a 0|`id: 1L`, `rendimiento: 0.0`|`rendimientoMolienda <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MM-02**|Identificador en uso por otro equipamiento|`id: 1L`, `idInterno: "MOL-EXISTENTE"`, `rendimiento: 50.0`|`existsByIdentificador...AndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No busca por ID ni persiste.|
|**CP-MM-03**|Molino no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No persiste.|
|**CP-MM-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `idInterno: "MOL-NUEVO"`, DTO válido|Identificador libre y molino encontrado|Actualiza campos (`idInterno`, `descripcion`, `rendimientoMolienda`), guarda y retorna DTO.|
|**CP-MM-05**|Conservar identificador propio actual|`id: 1L`, `idInterno: "mol-01"` (Mismo equipamiento)|`existsByIdentificador...AndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|

### 5. `bajaMolino(Long id)`

| **ID**       | **Nombre del Caso**          | **Datos de Entrada (Escenario)** | **Condición Evaluada**                             | **Resultado Esperado**                                         |
| ------------ | ---------------------------- | -------------------------------- | -------------------------------------------------- | -------------------------------------------------------------- |
| **CP-BM-01** | Baja de molino inexistente   | `id: 99L` (No existe en BD)      | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException`. No llama a `save()`.   |
| **CP-BM-02** | Baja exitosa _(Baja lógica vía Estado)_ | `id: 1L` (Existe en BD)          | `findById(1L)` $\rightarrow$ **Presente**          | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del molino dado de baja. |