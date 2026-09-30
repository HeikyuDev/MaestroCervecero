### 1. `filtrarPresentacionesComerciales(String nombre, Double cantidad, UnidadDeMedida unidadDeMedida, Pageable pageable)`

Solo devuelve presentaciones comerciales activas (`estado = 'ACTIVO'`). `nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas; `cantidad` y `unidadDeMedida` son coincidencia exacta. Todos opcionales.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FPC-01**|Filtra por los 3 criterios informados|`nombre: "Bolsa"`, `cantidad: 25.0`, `unidadDeMedida: KILOGRAMO`, `pageable: PageRequest.of(0, 10)`, BD con 1 presentación activa que cumple los tres criterios|`filtrarPresentacionesComerciales("Bolsa", 25.0, KILOGRAMO, pageable)` contiene elementos|Retorna `Page<PresentacionComercialResponseDTO>` con 1 elemento mapeado.|
|**CP-FPC-02**|Los 3 parámetros nulos no restringen la búsqueda|`nombre: null`, `cantidad: null`, `unidadDeMedida: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 presentaciones activas|El service propaga los 3 parámetros nulos tal cual al repositorio|Retorna `Page<PresentacionComercialResponseDTO>` con las 3 presentaciones activas (equivalente a no filtrar).|
|**CP-FPC-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `cantidad: null`, `unidadDeMedida: null`, `pageable: PageRequest.of(0, 10)`|`filtrarPresentacionesComerciales("Inexistente", null, null, pageable)` está vacío|Retorna `Page<PresentacionComercialResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Presentación comercial encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `PresentacionComercialResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Presentación comercial inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La presentación comercial no existe".|

### 3. `altaPresentacionComercial(PresentacionComercialFormDTO presentacionComercialFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-APC-01**|Cantidad nula|`nombre: "Bolsa de 25 Kg"`, `cantidad: null`, `unidadDeMedida: KILOGRAMO`|`cantidad == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-APC-02**|Cantidad en cero _(Límite)_|`nombre: "Bolsa de 25 Kg"`, `cantidad: 0.0`, `unidadDeMedida: KILOGRAMO`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-APC-03**|Cantidad negativa|`nombre: "Bolsa de 25 Kg"`, `cantidad: -0.1`, `unidadDeMedida: KILOGRAMO`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-APC-04**|Cantidad en el mínimo válido _(Límite)_|`nombre: "Sobre de 0,1 gm"`, `cantidad: 0.1`, `unidadDeMedida: GRAMO`|`cantidad == null \|\| <= 0` $\rightarrow$ **FALSE**|Persiste la entidad y retorna DTO con `cantidad = 0.1`.|
|**CP-APC-05**|Nombre duplicado|`nombre: "Bolsa de 25 Kg"` (Existe en BD), `cantidad: 25.0`, `unidadDeMedida: KILOGRAMO`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-APC-06**|Nombre duplicado Case-Insensitive|`nombre: "bolsa de 25 kg"` (Existe `"Bolsa de 25 Kg"`), `cantidad: 25.0`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-APC-07**|Precedencia de la validación de cantidad|`nombre: "Bolsa de 25 Kg"` (Existe en BD), `cantidad: 0.0`|`validarCantidad` se ejecuta antes que `existsByNombreIgnoreCase`|Lanza `ReglaNegocioException` (no `RecursoDuplicadoException`). No invoca `existsByNombreIgnoreCase`.|
|**CP-APC-08**|Alta exitosa _(Camino feliz)_|`nombre: "Paquete de 100 gm"` (Único), `cantidad: 100.0`, `unidadDeMedida: GRAMO`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `cantidad`, `unidadDeMedida` y `estado = ACTIVO` asignados, y retorna DTO.|
|**CP-APC-09**|Alta con unidad de medida TONELADA|`nombre: "Pallet de 1 Tn"` (Único), `cantidad: 1.0`, `unidadDeMedida: TONELADA`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad y retorna DTO con `unidadDeMedida = TONELADA`.|
|**CP-APC-10**|Mismo nombre con distinta unidad de medida|`nombre: "Bolsa de 25 Kg"` (Existe en BD), `cantidad: 25000.0`, `unidadDeMedida: GRAMO`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. La unicidad es solo por nombre, sin considerar la unidad de medida.|

### 4. `modificarPresentacionComercial(Long id, PresentacionComercialFormDTO presentacionComercialFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MPC-01**|Cantidad nula|`id: 1L`, `cantidad: null`|`cantidad == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No busca por ID ni persiste (`verifyNoInteractions`).|
|**CP-MPC-02**|Cantidad en cero _(Límite)_|`id: 1L`, `cantidad: 0.0`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No busca por ID ni persiste (`verifyNoInteractions`).|
|**CP-MPC-03**|Cantidad negativa|`id: 1L`, `cantidad: -0.1`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No busca por ID ni persiste.|
|**CP-MPC-04**|Presentación comercial no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni ejecuta `save()`.|
|**CP-MPC-05**|Nombre en uso por otra presentación comercial|`id: 1L` (Existe), `nombre: "Paquete de 100 gm"` (Pertenece al `id: 2L`), `cantidad: 25.0`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MPC-06**|Conservar el nombre propio actual|`id: 1L`, `nombre: "bolsa de 25 kg"` (Misma presentación comercial), `cantidad: 25.0`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MPC-07**|Modificación de cantidad y unidad de medida|`id: 1L` (Existe), `nombre: "Bolsa de 25 Kg"` (Propio), `cantidad: 25000.0`, `unidadDeMedida: GRAMO`|Validaciones OK y presentación encontrada|Actualiza `cantidad` y `unidadDeMedida`, ejecuta `save()` y retorna DTO con los nuevos valores.|
|**CP-MPC-08**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Bolsa de 50 Kg"` (Libre), `cantidad: 50.0`, `unidadDeMedida: KILOGRAMO`|Validaciones OK, presentación encontrada y nombre libre|Actualiza `nombre`, `cantidad` y `unidadDeMedida`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaPresentacionComercial(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BPC-01**|Baja de presentación comercial inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `catalogoProveedorRepository` ni llama a `save()`.|
|**CP-BPC-02**|Baja de presentación asociada a un catálogo de proveedor|`id: 1L` (Existe y asociada al catálogo de 1 proveedor activo)|`existsByPresentacionComercialId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BPC-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe y sin catálogos asociados)|`existsByPresentacionComercialId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la presentación comercial dada de baja.|