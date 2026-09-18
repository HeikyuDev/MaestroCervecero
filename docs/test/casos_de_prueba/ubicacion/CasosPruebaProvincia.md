### 1. `filtrarProvincias(String nombre, Long idPais, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas; `idPais` es coincidencia exacta. Ambos son opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FPr-01**|Filtra por nombre e idPais informados|`nombre: "Mis"`, `idPais: 1L`, `pageable: PageRequest.of(0, 10)`, BD con 1 provincia activa que cumple ambos criterios|`filtrarProvincias("Mis", 1L, pageable)` contiene elementos|Retorna `Page<ProvinciaResponseDTO>` con 1 elemento mapeado.|
|**CP-FPr-02**|Los 2 parámetros nulos no restringen la búsqueda|`nombre: null`, `idPais: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 provincias activas|El service propaga los 2 parámetros nulos tal cual al repositorio|Retorna `Page<ProvinciaResponseDTO>` con las 3 provincias activas (equivalente a no filtrar).|
|**CP-FPr-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `idPais: null`, `pageable: PageRequest.of(0, 10)`|`filtrarProvincias("Inexistente", null, pageable)` está vacío|Retorna `Page<ProvinciaResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Provincia encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `ProvinciaResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Provincia inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La provincia no existe".|

### 3. `altaProvincia(ProvinciaFormDTO provinciaFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-APR-01**|País asociado inexistente|`idPais: 99L` (No existe en BD), `nombre: "Misiones"`|`paisRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El país no existe". No valida duplicación ni ejecuta `save()`.|
|**CP-APR-02**|Nombre duplicado en el mismo país|`idPais: 1L` (Existe), `nombre: "Misiones"` (Ya registrada en ese país)|`existsByNombreIgnoreCaseAndPaisId` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-APR-03**|Nombre duplicado Case-Insensitive|`idPais: 1L`, `nombre: "misiones"` (Existe `"Misiones"` en ese país)|`existsByNombreIgnoreCaseAndPaisId` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-APR-04**|Mismo nombre en distinto país _(Válido)_|`idPais: 2L` (Existe), `nombre: "Misiones"` (Registrada solo en el `idPais: 1L`)|`existsByNombreIgnoreCaseAndPaisId` $\rightarrow$ **FALSE**|Persiste la entidad asociada al país 2, con `estado = ACTIVO`, y retorna DTO.|
|**CP-APR-05**|Alta exitosa _(Camino feliz)_|`idPais: 1L` (Existe), `nombre: "Corrientes"` (Único en ese país)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `pais` y `estado = ACTIVO` asignados, y retorna DTO.|

### 4. `modificarProvincia(Long id, ProvinciaFormDTO provinciaFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MPR-01**|Provincia no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`provinciaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La provincia no existe". No consulta `paisRepository` ni persiste.|
|**CP-MPR-02**|País asociado inexistente|`id: 1L` (Existe), `idPais: 99L` (No existe en BD)|`paisRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El país no existe". No valida duplicación ni ejecuta `save()`.|
|**CP-MPR-03**|Nombre en uso por otra provincia del mismo país|`id: 1L`, `idPais: 1L`, `nombre: "Corrientes"` (Pertenece a la `id: 2L` del mismo país)|`existsByNombreIgnoreCaseAndPaisIdAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MPR-04**|Conservar el nombre propio actual|`id: 1L`, `idPais: 1L`, `nombre: "misiones"` (Misma provincia)|`existsByNombreIgnoreCaseAndPaisIdAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MPR-05**|Reasignación de país|`id: 1L` (Actualmente del `idPais: 1L`), `idPais: 2L` (Existe), `nombre` libre en el país destino|Validaciones OK contra el nuevo `idPais`|Actualiza `nombre` y `pais`, ejecuta `save()` y retorna DTO con el país 2.|
|**CP-MPR-06**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `idPais: 1L` (Existe), `nombre: "Misiones Actualizada"` (Libre)|Provincia y país encontrados, nombre libre|Actualiza los campos `nombre` y `pais`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaProvincia(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BPR-01**|Baja de provincia inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `localidadRepository` ni llama a `save()`.|
|**CP-BPR-02**|Baja de provincia con localidades activas|`id: 1L` (Existe con 2 localidades activas)|`existsByProvinciaId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BPR-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe y sin localidades asociadas)|`existsByProvinciaId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la provincia dada de baja.|