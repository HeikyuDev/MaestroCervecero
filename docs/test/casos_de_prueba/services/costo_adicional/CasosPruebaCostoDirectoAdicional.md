### 1. `filtrarCostosDirectosAdicionales(String nombre, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas. Es opcional, `null` = no filtra.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FCDA-01**|Filtra por nombre informado|`nombre: "Gas"`, `pageable: PageRequest.of(0, 10)`, BD con 1 costo directo adicional activo cuyo nombre contiene "Gas"|`filtrarCostosDirectosAdicionales("Gas", pageable)` contiene elementos|Retorna `Page<CostoDirectoAdicionalResponseDTO>` con 1 elemento mapeado.|
|**CP-FCDA-02**|Nombre nulo no restringe la búsqueda|`nombre: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 costos directos adicionales activos|El service propaga `nombre: null` tal cual al repositorio|Retorna `Page<CostoDirectoAdicionalResponseDTO>` con los 3 costos activos (equivalente a no filtrar).|
|**CP-FCDA-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `pageable: PageRequest.of(0, 10)`|`filtrarCostosDirectosAdicionales("Inexistente", pageable)` está vacío|Retorna `Page<CostoDirectoAdicionalResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FCDA-04**|El nombre buscado se normaliza|`nombre: "   Alquiler   de   planta  "`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `nombre: "Alquiler de planta"`.|
|**CP-FCDA-05**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|

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
|**CP-ACD-09**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-ACD-10**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-ACD-11**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un costo directo adicional con el nombre 'pilsen'"). No ejecuta `save()`.|
|**CP-ACD-12**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_costo_directo_adicional_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un costo directo adicional con el nombre 'Pilsen Nacional'").|
|**CP-ACD-13**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-ACD-14**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|

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
|**CP-MCD-08**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-MCD-09**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-MCD-10**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("El nombre 'pilsen' ya está en uso por otro costo directo adicional"). No persiste.|
|**CP-MCD-11**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_costo_directo_adicional_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("El nombre 'Pilsen Nacional' ya está en uso por otro costo directo adicional").|
|**CP-MCD-12**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|

### 5. `bajaCostoDirectoAdicional(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BCD-01**|Baja de costo directo adicional inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BCD-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO del costo directo adicional dado de baja.|
