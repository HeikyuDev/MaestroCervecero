### 1. `filtrarPaises(String nombre, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas. Es opcional, `null` = no filtra.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FP-01**|Filtra por nombre informado|`nombre: "Arg"`, `pageable: PageRequest.of(0, 10)`, BD con 1 país activo cuyo nombre contiene "Arg"|`filtrarPaises("Arg", pageable)` contiene elementos|Retorna `Page<PaisResponseDTO>` con 1 elemento mapeado.|
|**CP-FP-02**|Nombre nulo no restringe la búsqueda|`nombre: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 países activos|El service propaga `nombre: null` tal cual al repositorio|Retorna `Page<PaisResponseDTO>` con los 3 países activos (equivalente a no filtrar).|
|**CP-FP-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `pageable: PageRequest.of(0, 10)`|`filtrarPaises("Inexistente", pageable)` está vacío|Retorna `Page<PaisResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|País encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `PaisResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|País inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El país no existe".|

### 3. `altaPais(PaisFormDTO paisFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AP-01**|Nombre de país duplicado|`nombre: "Argentina"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AP-02**|Nombre duplicado Case-Insensitive|`nombre: "argentina"` (Existe `"Argentina"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AP-03**|Alta exitosa _(Camino feliz)_|`nombre: "Uruguay"` (Único)|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**|Persiste la entidad con `nombre = "Uruguay"` y `estado = ACTIVO`, y retorna DTO.|

### 4. `modificarPais(Long id, PaisFormDTO paisFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MP-01**|País no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni persiste.|
|**CP-MP-02**|Nombre en uso por otro país|`id: 1L` (Existe), `nombre: "Brasil"` (Pertenece al `id: 2L`)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MP-03**|Conservar el nombre propio actual|`id: 1L`, `nombre: "argentina"` (Mismo país)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MP-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Argentina Actualizado"` (Libre)|País encontrado y nombre libre|Actualiza el campo `nombre`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaPais(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BP-01**|Baja de país inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `provinciaRepository` ni llama a `save()`.|
|**CP-BP-02**|Baja de país con provincias activas|`id: 1L` (Existe con 2 provincias activas)|`existsByPaisId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BP-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe y sin provincias asociadas)|`existsByPaisId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del país dado de baja.|