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
|**CP-APC-11**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-APC-12**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-APC-13**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe una presentación comercial con el nombre 'pilsen'"). No ejecuta `save()`.|
|**CP-APC-14**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_presentacion_comercial_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe una presentación comercial con el nombre 'Pilsen Nacional'").|
|**CP-APC-15**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-APC-16**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|

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
|**CP-MPC-09**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-MPC-10**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-MPC-11**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("El nombre 'pilsen' ya está en uso por otra presentación comercial"). No persiste.|
|**CP-MPC-12**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_presentacion_comercial_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("El nombre 'Pilsen Nacional' ya está en uso por otra presentación comercial").|
|**CP-MPC-13**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|

### 5. `bajaPresentacionComercial(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BPC-01**|Baja de presentación comercial inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `ordenCompraRepository` ni `catalogoProveedorRepository` ni llama a `save()`.|
|**CP-BPC-02**|Baja de presentación asociada a un catálogo de proveedor|`id: 1L` (Existe y asociada al catálogo de 1 proveedor activo), sin orden de compra en estado Pendiente|`existsByPresentacionComercialEnOrdenCompraPendiente(1L)` $\rightarrow$ **FALSE**, `existsByPresentacionComercialId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("No se puede dar de baja la presentación comercial porque se encuentra asociada al catálogo de al menos un proveedor activo"). No llama a `save()`.|
|**CP-BPC-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe, sin órdenes de compra en estado Pendiente y sin catálogos asociados)|Ambas validaciones $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la presentación comercial dada de baja.|
|**CP-BPC-04**|Baja de presentación incluida en una orden de compra en estado Pendiente|`id: 1L` (Existe y con un ítem en una orden de compra `PENDIENTE`)|`existsByPresentacionComercialEnOrdenCompraPendiente(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("No se puede dar de baja la presentación comercial porque se encuentra en al menos una orden de compra en estado Pendiente"). Se informa antes que el catálogo: no consulta `catalogoProveedorRepository`. No llama a `save()`.|

#### Casos manuales contra la base de datos: `IOrdenCompraRepository.existsByPresentacionComercialEnOrdenCompraPendiente`

Los tests de servicio usan mocks, por lo que la consulta JPQL se verifica manualmente contra PostgreSQL.

|**ID**|**Escenario en BD**|**Resultado Esperado**|
|---|---|---|
|**CP-BPC-M01**|Orden `PENDIENTE` con un ítem cuyo catálogo de proveedor referencia la presentación comercial|`TRUE`|
|**CP-BPC-M02**|Orden `PENDIENTE` registrada contra una versión anterior del proveedor, con la presentación en el ítem|`TRUE`|
|**CP-BPC-M03**|La orden con la presentación está `FINALIZADA` o `ANULADA`|`FALSE`|
|**CP-BPC-M04**|La presentación no figura en ningún ítem de orden de compra|`FALSE`|
