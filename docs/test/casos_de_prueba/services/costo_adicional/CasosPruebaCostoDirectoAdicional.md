### 1. `filtrarCostosDirectosAdicionales(String nombre, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas. Es opcional, `null` = no filtra.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FCDA-01**|Filtra por nombre informado|`nombre: "Gas"`, `pageable: PageRequest.of(0, 10)`, BD con 1 costo directo adicional activo cuyo nombre contiene "Gas"|`filtrarCostosDirectosAdicionales("Gas", pageable)` contiene elementos|Retorna `Page<CostoDirectoAdicionalResponseDTO>` con 1 elemento mapeado.|
|**CP-FCDA-02**|Nombre nulo no restringe la búsqueda|`nombre: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 costos directos adicionales activos|El service propaga `nombre: null` tal cual al repositorio|Retorna `Page<CostoDirectoAdicionalResponseDTO>` con los 3 costos activos (equivalente a no filtrar).|
|**CP-FCDA-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `pageable: PageRequest.of(0, 10)`|`filtrarCostosDirectosAdicionales("Inexistente", pageable)` está vacío|Retorna `Page<CostoDirectoAdicionalResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Costo directo adicional encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `CostoDirectoAdicionalResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Costo directo adicional inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el costo directo adicional con ID: 99".|

### 3. `altaCostoDirectoAdicional(CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ACD-01**|Nombre duplicado|`nombre: "Energía eléctrica"` (Existe en BD), `costoPorLitro: 5.00`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-ACD-02**|Nombre duplicado Case-Insensitive|`nombre: "energía eléctrica"` (Existe `"Energía eléctrica"`), `costoPorLitro: 5.00`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-ACD-03**|Costo por litro nulo|`nombre: "Gas natural"` (Único), `costoPorLitro: null`|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**, `costoPorLitro == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-ACD-04**|Costo por litro en cero _(Límite)_|`nombre: "Gas natural"` (Único), `costoPorLitro: 0.00`|`costoPorLitro.signum() <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-ACD-05**|Costo por litro negativo|`nombre: "Gas natural"` (Único), `costoPorLitro: -0.01`|`costoPorLitro.signum() <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-ACD-06**|Precedencia de la validación de duplicado|`nombre: "Energía eléctrica"` (Existe en BD), `costoPorLitro: 0.00` (también inválido)|`existsByNombreIgnoreCase` se evalúa antes que `validarCostoPorLitro`|Lanza `RecursoDuplicadoException` (no `ReglaNegocioException`). No ejecuta `save()`.|
|**CP-ACD-07**|Costo por litro en el mínimo válido _(Límite)_|`nombre: "Mantenimiento de equipos"` (Único), `costoPorLitro: 0.0001`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad y retorna DTO con `costoPorLitro = 0.0001`.|
|**CP-ACD-08**|Alta exitosa _(Camino feliz)_|`nombre: "Energía eléctrica"` (Único), `costoPorLitro: 5.00`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `costoPorLitro` y `estado = ACTIVO` asignados, y retorna DTO.|

### 4. `modificarCostoDirectoAdicional(Long id, CostoDirectoAdicionalFormDTO costoDirectoAdicionalFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCD-01**|Costo directo adicional no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni costo, ni ejecuta `save()` (`verifyNoInteractions`).|
|**CP-MCD-02**|Nombre en uso por otro costo directo adicional|`id: 1L` (Existe), `nombre: "Gas natural"` (Pertenece al `id: 2L`), `costoPorLitro: 5.00`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MCD-03**|Conservar el nombre propio actual|`id: 1L`, `nombre: "energía eléctrica"` (Mismo costo directo adicional, distinto case), `costoPorLitro: 5.00`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MCD-04**|Costo por litro nulo|`id: 1L` (Existe), nombre libre, `costoPorLitro: null`|`findById` y `existsByNombreIgnoreCaseAndIdNot` OK, `costoPorLitro == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-MCD-05**|Costo por litro en cero _(Límite)_|`id: 1L` (Existe), nombre libre, `costoPorLitro: 0.00`|`costoPorLitro.signum() <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-MCD-06**|Costo por litro negativo|`id: 1L` (Existe), nombre libre, `costoPorLitro: -0.01`|`costoPorLitro.signum() <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-MCD-07**|Modificación exitosa _(Camino feliz)_|`id: 1L` (Existe), `nombre: "Energía eléctrica (tarifa 2026)"` (Libre), `costoPorLitro: 6.50`|Validaciones OK, costo directo adicional encontrado y nombre libre|Actualiza `nombre` y `costoPorLitro`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaCostoDirectoAdicional(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BCD-01**|Baja de costo directo adicional inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BCD-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO del costo directo adicional dado de baja.|
