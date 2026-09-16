### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 3 proveedores activos|`findAll(pageable)` contiene elementos|Retorna `Page<ProveedorResponseDTO>` con 3 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<ProveedorResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Proveedor encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `ProveedorResponseDTO` con los datos de la última versión del proveedor.|
|**CP-BI-02**|Proveedor inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el proveedor con ID: 99".|

### 3. `altaProveedor(ProveedorFormDTO proveedorFormDTO)`

Se concentran las reglas de negocio de la versión inicial. En todos los casos de falla, el resto de los datos del DTO son válidos.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AP-01**|Razón social duplicada (activa)|`razonSocial: "Maltería del Sur S.A."` (pertenece a un proveedor activo existente)|`existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` mencionando solo la razón social. No consulta `localidadRepository`, `insumoRepository` ni `presentacionComercialRepository`, ni ejecuta `save()`.|
|**CP-AP-02**|CUIT duplicado (activo)|`cuit: "30-11111111-1"` (pertenece a un proveedor activo existente); razón social única (`existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrue` $\rightarrow$ **FALSE**)|`existsByCuitAndEsUltimaVersionTrue(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` mencionando solo el CUIT. No ejecuta `save()`.|
|**CP-AP-03**|Localidad inexistente|`idLocalidad: 99L` (No existe en BD)|`localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la localidad con ID: 99". No ejecuta `save()`.|
|**CP-AP-04**|Insumo inexistente en un ítem del catálogo|Catálogo con 1 ítem, `idInsumo: 99L` (No existe en BD)|`insumoRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el insumo con ID: 99". No ejecuta `save()`.|
|**CP-AP-05**|Presentación comercial inexistente en un ítem del catálogo|Catálogo con 1 ítem, `idPresentacionComercial: 99L` (No existe en BD)|`presentacionComercialRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la presentación comercial con ID: 99". No ejecuta `save()`.|
|**CP-AP-06**|Alta con catálogo vacío _(Opcional)_|DTO válido con `catalogoProveedor: List.of()`|`stream().map(...).toList()` sobre lista vacía|Persiste el proveedor con catálogo vacío. No consulta `insumoRepository` ni `presentacionComercialRepository`.|
|**CP-AP-07**|Alta exitosa _(Camino feliz)_|DTO válido: razón social y CUIT únicos, localidad existente, catálogo con 2 ítems válidos|Todas las validaciones $\rightarrow$ **FALSE**|Persiste el proveedor con `estado = ACTIVO` y una única versión con `esUltimaVersion = true` y su catálogo de 2 ítems, y retorna DTO.|

### 4. `modificarProveedor(Long id, ProveedorFormDTO proveedorFormDTO)`

Valida el versionado, la duplicidad excluyendo al propio proveedor y la existencia del registro. Las validaciones de localidad, insumo y presentación comercial son las mismas del alta (**CP-AP-03** a **CP-AP-05**).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MP-01**|Razón social en uso por otro proveedor activo|`id: 1L`, `razonSocial: "Maltería del Norte S.A."` (pertenece a otro proveedor activo)|`existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` mencionando solo la razón social. No busca por ID ni persiste.|
|**CP-MP-02**|CUIT en uso por otro proveedor activo|`id: 1L`, `cuit: "30-99999999-9"` (pertenece a otro proveedor activo); razón social única (`existsByRazonSocialIgnoreCaseAndEsUltimaVersionTrueAndProveedorIdNot` $\rightarrow$ **FALSE**)|`existsByCuitAndEsUltimaVersionTrueAndProveedorIdNot(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` mencionando solo el CUIT. No busca por ID ni persiste.|
|**CP-MP-03**|Conservar la razón social y el CUIT propios _(Camino feliz)_|`id: 1L`, `razonSocial`/`cuit` iguales a los de la versión activa actual|`...AndProveedorIdNot(...)` $\rightarrow$ **FALSE** (se excluye el propio ID)|Permite continuar con la modificación, persiste y retorna DTO.|
|**CP-MP-04**|Proveedor no encontrado por ID|`id: 99L` (No existe o dado de baja), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No persiste.|
|**CP-MP-05**|Localidad inexistente|`id: 1L` (Existe con 1 versión activa), `idLocalidad: 99L` (No existe)|`localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-MP-06**|Insumo referenciado inexistente|`id: 1L` (Existe), catálogo con `idInsumo: 99L` (No existe)|`insumoRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-MP-07**|Presentación comercial referenciada inexistente|`id: 1L` (Existe), catálogo con `idPresentacionComercial: 99L` (No existe)|`presentacionComercialRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-MP-08**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe con 1 versión activa), DTO válido|Validaciones OK y proveedor encontrado|Desactiva la versión previa (`esUltimaVersion = false`), agrega la nueva en `true`, guarda y retorna DTO.|
|**CP-MP-09**|Versionado sobre historial existente|`id: 1L` con 3 versiones (2 históricas + 1 activa), DTO válido|`filter(isEsUltimaVersion)` $\rightarrow$ **1 coincidencia**|El proveedor queda con 4 versiones y exactamente 1 con `esUltimaVersion = true`.|

### 5. `bajaProveedor(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BP-01**|Baja de proveedor inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BP-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD, sin órdenes de compra pendientes asociadas)|`existsByVersionProveedor_Proveedor_IdAndEstado(1L, PENDIENTE)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del proveedor dado de baja.|
|**CP-BP-03**|Baja rechazada por orden de compra pendiente asociada|`id: 1L` (Existe en BD); existe una orden de compra en estado `PENDIENTE` asociada a alguna versión (histórica o activa) del proveedor|`existsByVersionProveedor_Proveedor_IdAndEstado(1L, PENDIENTE)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BP-04**|Baja permitida con órdenes asociadas en otro estado|`id: 1L` (Existe en BD); tiene órdenes `FINALIZADA`/`ANULADA` asociadas, pero ninguna `PENDIENTE`|`existsByVersionProveedor_Proveedor_IdAndEstado(1L, PENDIENTE)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del proveedor dado de baja.|
