### 1. `filtrarMotivosAjuste(String nombre, TipoAjuste tipoAjuste, Pageable pageable)`

Solo devuelve motivos de ajuste activos (`estado = 'ACTIVO'`). `nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas; `tipoAjuste` (INGRESO/EGRESO) es coincidencia exacta. Ambos opcionales.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FMA-01**|Filtra por nombre y tipo de ajuste informados|`nombre: "Rotura"`, `tipoAjuste: EGRESO`, `pageable: PageRequest.of(0, 10)`, BD con 2 motivos activos que cumplen ambos criterios|`filtrarMotivosAjuste("Rotura", EGRESO, pageable)` contiene elementos|Retorna `Page<MotivoAjusteResponseDTO>` con 2 elementos mapeados.|
|**CP-FMA-02**|Nombre y tipo de ajuste nulos no restringen la búsqueda|`nombre: null`, `tipoAjuste: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<MotivoAjusteResponseDTO>` con todos los motivos activos (equivalente a no filtrar).|
|**CP-FMA-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `tipoAjuste: null`, `pageable: PageRequest.of(0, 10)`|`filtrarMotivosAjuste("Inexistente", null, pageable)` está vacío|Retorna `Page<MotivoAjusteResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Motivo de ajuste encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MotivoAjusteResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Motivo de ajuste inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el motivo de ajuste con ID: 99".|

### 3. `altaMotivoAjuste(MotivoAjusteFormDTO motivoAjusteFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AMA-01**|Nombre duplicado|`nombre: "Rotura de lote"` (Existe en BD), `tipoAjuste: EGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AMA-02**|Nombre duplicado Case-Insensitive|`nombre: "rotura de lote"` (Existe `"Rotura de lote"`), `tipoAjuste: EGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AMA-03**|Alta exitosa con tipo INGRESO _(Camino feliz)_|`nombre: "Corrección de conteo"` (Único), `tipoAjuste: INGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `tipoAjuste = INGRESO` y `estado = ACTIVO` asignados, y retorna DTO.|
|**CP-AMA-04**|Alta exitosa con tipo EGRESO|`nombre: "Rotura de lote"` (Único), `tipoAjuste: EGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**|Persiste la entidad con `tipoAjuste = EGRESO` y `estado = ACTIVO`, y retorna DTO.|

### 4. `modificarMotivoAjuste(Long id, MotivoAjusteFormDTO motivoAjusteFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MMA-01**|Motivo de ajuste no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación, ni ejecuta `save()` (`verifyNoInteractions`).|
|**CP-MMA-02**|Nombre en uso por otro motivo de ajuste|`id: 1L` (Existe), `nombre: "Rotura de lote"` (Pertenece al `id: 2L`), `tipoAjuste: INGRESO`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MMA-03**|Conservar el nombre propio actual|`id: 1L`, `nombre: "corrección de conteo"` (Mismo motivo de ajuste, distinto case), `tipoAjuste: INGRESO`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MMA-04**|Modificación exitosa cambiando el tipo de ajuste _(Camino feliz)_|`id: 1L` (Existe, `tipoAjuste: INGRESO`), `nombre: "Corrección de conteo (egreso)"` (Libre), `tipoAjuste: EGRESO`|Validaciones OK|Actualiza `nombre` y `tipoAjuste`, ejecuta `save()` y retorna DTO con `tipoAjuste = EGRESO`.|

### 5. `bajaMotivoAjuste(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BMA-01**|Baja de motivo de ajuste inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BMA-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO del motivo de ajuste dado de baja.|
