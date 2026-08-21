### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 2 fermentadores activos|`findAll(pageable)` contiene elementos|Retorna `Page<FermentadorResponseDTO>` con 2 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<FermentadorResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Fermentador encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `FermentadorResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Fermentador inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El fermentador no existe".|

### 3. `altaFermentador(FermentadorFormDTO fermentadorFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AF-01**|Capacidad útil mayor a total|`capTotal: 100.0`, `capUtil: 120.0`, `idInterno: "FERM-01"`|`capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AF-02**|Capacidad útil igual a total _(Límite)_|`capTotal: 100.0`, `capUtil: 100.0`, `idInterno: "FERM-01"`|`capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AF-03**|Identificador interno duplicado|`capTotal: 100.0`, `capUtil: 80.0`, `idInterno: "FERM-01"` (Existe en BD)|`existsByIdentificador...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AF-04**|Identificador duplicado Case-Insensitive|`capTotal: 100.0`, `capUtil: 80.0`, `idInterno: "ferm-01"` (Existe `"FERM-01"`)|`existsByIdentificador...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AF-05**|Alta exitosa _(Camino feliz)_|`capTotal: 100.0`, `capUtil: 80.0`, `idInterno: "FERM-02"` (Único)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `estadoOperativo = DISPONIBLE` y `estado = ACTIVO`, y retorna DTO.|

### 4. `modificarFermentador(Long id, FermentadorFormDTO fermentadorFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MF-01**|Falla por capacidad útil mayor o igual a total|`id: 1L`, `capTotal: 100.0`, `capUtil: 110.0`|`capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MF-02**|Identificador en uso por otro equipamiento|`id: 1L`, `idInterno: "FERM-EXISTENTE"`|`existsByIdentificador...AndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No busca por ID ni persiste.|
|**CP-MF-03**|Fermentador no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No persiste.|
|**CP-MF-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `idInterno: "FERM-NUEVO"`, DTO válido|Identificador libre y fermentador encontrado|Actualiza campos (`capTotal`, `capUtil`, `descripcion`, `idInterno`), guarda y retorna DTO.|
|**CP-MF-05**|Conservar identificador propio actual|`id: 1L`, `idInterno: "ferm-01"` (Mismo equipamiento)|`existsByIdentificador...AndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|

### 5. `bajaFermentador(Long id)`

| **ID**       | **Nombre del Caso**             | **Datos de Entrada (Escenario)** | **Condición Evaluada**                             | **Resultado Esperado**                                              |
| ------------ | ------------------------------- | -------------------------------- | -------------------------------------------------- | ------------------------------------------------------------------- |
| **CP-BF-01** | Baja de fermentador inexistente | `id: 99L` (No existe en BD)      | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException`. No llama a `save()`.        |
| **CP-BF-02** | Baja exitosa _(Baja lógica vía Estado)_    | `id: 1L` (Existe en BD)          | `findById(1L)` $\rightarrow$ **Presente**          | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del fermentador dado de baja. |