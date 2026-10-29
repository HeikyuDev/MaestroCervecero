### 1. `filtrarPaises(String nombre, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas. Es opcional, `null` = no filtra.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FP-01**|Filtra por nombre informado|`nombre: "Arg"`, `pageable: PageRequest.of(0, 10)`, BD con 1 país activo cuyo nombre contiene "Arg"|`filtrarPaises("Arg", pageable)` contiene elementos|Retorna `Page<PaisResponseDTO>` con 1 elemento mapeado.|
|**CP-FP-02**|Nombre nulo no restringe la búsqueda|`nombre: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 países activos|El service propaga `nombre: null` tal cual al repositorio|Retorna `Page<PaisResponseDTO>` con los 3 países activos (equivalente a no filtrar).|
|**CP-FP-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `pageable: PageRequest.of(0, 10)`|`filtrarPaises("Inexistente", pageable)` está vacío|Retorna `Page<PaisResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FP-04**|El nombre buscado se normaliza|`nombre: "   Estados   Unidos  "`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `nombre: "Estados Unidos"`.|
|**CP-FP-05**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|País encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `PaisResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|País inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el país con ID: 99".|

### 3. `altaPais(PaisFormDTO paisFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AP-01**|Nombre de país duplicado|`nombre: "Argentina"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AP-02**|Nombre duplicado Case-Insensitive|`nombre: "argentina"` (Existe `"Argentina"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AP-03**|Alta exitosa _(Camino feliz)_|`nombre: "Uruguay"` (Único)|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**|Persiste la entidad con `nombre = "Uruguay"` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AP-04**|Nombre de más de 100 caracteres _(Límite)_|`nombre`: 101 caracteres|Longitud del nombre `> 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD ni ejecuta `save()`.|
|**CP-AP-05**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-AP-06**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AP-07**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un país con el nombre 'pilsen'"). No ejecuta `save()`.|
|**CP-AP-08**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_pais_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un país con el nombre 'Pilsen Nacional'").|
|**CP-AP-09**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-AP-10**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|

### 4. `modificarPais(Long id, PaisFormDTO paisFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MP-01**|País no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni persiste.|
|**CP-MP-02**|Nombre en uso por otro país|`id: 1L` (Existe), `nombre: "Brasil"` (Pertenece al `id: 2L`)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MP-03**|Conservar el nombre propio actual|`id: 1L`, `nombre: "argentina"` (Mismo país)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MP-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Argentina Actualizado"` (Libre)|País encontrado y nombre libre|Actualiza el campo `nombre`, ejecuta `save()` y retorna DTO actualizado.|
|**CP-MP-05**|Nombre de más de 100 caracteres _(Límite)_|`id: 1L`, `nombre`: 101 caracteres|Longitud del nombre `> 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD ni ejecuta `save()`.|
|**CP-MP-06**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-MP-07**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-MP-08**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un país con el nombre 'pilsen'"). No persiste.|
|**CP-MP-09**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_pais_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un país con el nombre 'Pilsen Nacional'").|
|**CP-MP-10**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|

### 5. `bajaPais(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BP-01**|Baja de país inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `provinciaRepository` ni llama a `save()`.|
|**CP-BP-02**|Baja de país con provincias activas|`id: 1L` (Existe con 2 provincias activas)|`existsByPaisId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BP-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe y sin provincias asociadas)|`existsByPaisId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del país dado de baja.|

### 6. `listarPaisesActivos()`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LPa-01**|Países activos mapeados a DTO|El repositorio devuelve "Argentina" (`id: 1`) y "Brasil" (`id: 2`)|`listarActivos()` entrega dos países|Retorna dos DTO con esos ids y nombres, en el mismo orden (el orden alfabético lo define la consulta del repositorio).|
|**CP-LPa-02**|Sin países activos|El repositorio devuelve una lista vacía|`listarActivos()` entrega una lista vacía|Retorna una lista vacía.|
