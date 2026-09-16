### 1. `filtrarFabricantesBarril(String razonSocial, String nombreComercial, String cuit, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FFB-01**|Filtra por razón social, nombre comercial y CUIT informados|`razonSocial: "Tonelería"`, `nombreComercial: "Sur"`, `cuit: "30-1111"`, `pageable: PageRequest.of(0, 10)`, BD con 2 fabricantes activos que cumplen los tres criterios|`filtrarFabricantesBarril("Tonelería", "Sur", "30-1111", pageable)` contiene elementos|Retorna `Page<FabricanteBarrilResponseDTO>` con 2 elementos mapeados.|
|**CP-FFB-02**|Los tres parámetros nulos no restringen la búsqueda|`razonSocial: null`, `nombreComercial: null`, `cuit: null`, `pageable: PageRequest.of(0, 10)`|El service propaga los tres parámetros nulos tal cual al repositorio|Retorna `Page<FabricanteBarrilResponseDTO>` con todos los fabricantes activos (equivalente a no filtrar).|
|**CP-FFB-03**|Consulta sin coincidencias|`razonSocial: "Inexistente"`, `nombreComercial: null`, `cuit: null`, `pageable: PageRequest.of(0, 10)`|`filtrarFabricantesBarril("Inexistente", null, null, pageable)` está vacío|Retorna `Page<FabricanteBarrilResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Fabricante de barril encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `FabricanteBarrilResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Fabricante de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el fabricante de barril con ID: 99".|

### 3. `altaFabricanteBarril(FabricanteBarrilFormDTO fabricanteBarrilFormDTO)`

Se valida la razón social y el CUIT de forma independiente: cada uno dispara su propia excepción, mencionando solo el campo que falló. En todos los casos de duplicado, el resto de los datos del DTO son válidos.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AFB-01**|Razón social duplicada (activa)|`razonSocial: "Tonelería del Sur S.A."` (pertenece a un fabricante activo existente)|`existsByRazonSocialIgnoreCase(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un fabricante de barril activo con la razón social 'Tonelería del Sur S.A.'". No consulta `existsByCuit`, ni ejecuta `save()`.|
|**CP-AFB-02**|CUIT duplicado (activo)|`razonSocial` única, `cuit: "30-11111111-1"` (pertenece a un fabricante activo existente)|`existsByRazonSocialIgnoreCase(...)` $\rightarrow$ **FALSE**; `existsByCuit(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un fabricante de barril activo con el CUIT '30-11111111-1'". No ejecuta `save()`.|
|**CP-AFB-03**|Alta exitosa _(Camino feliz)_|DTO válido: razón social y CUIT únicos|Ambas validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `estado = ACTIVO` y todos los campos del DTO, y retorna DTO.|

### 4. `modificarFabricanteBarril(Long id, FabricanteBarrilFormDTO fabricanteBarrilFormDTO)`

Localiza el fabricante primero; recién si existe se valida la duplicidad de razón social y CUIT, cada una excluyendo al propio fabricante que se está modificando.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MFB-01**|Fabricante de barril no encontrado por ID|`id: 99L` (No existe o dado de baja), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `existsByRazonSocialIgnoreCaseAndIdNot` ni `existsByCuitAndIdNot`, ni persiste.|
|**CP-MFB-02**|Razón social en uso por otro fabricante activo|`id: 1L` (Existe), `razonSocial: "Tonelería del Norte S.A."` (pertenece a otro fabricante activo)|`findById(1L)` $\rightarrow$ **Presente**; `existsByRazonSocialIgnoreCaseAndIdNot(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe otro fabricante de barril activo con la razón social 'Tonelería del Norte S.A.'". No consulta `existsByCuitAndIdNot`, ni persiste.|
|**CP-MFB-03**|CUIT en uso por otro fabricante activo|`id: 1L` (Existe), `razonSocial` libre, `cuit: "30-99999999-9"` (pertenece a otro fabricante activo)|`existsByRazonSocialIgnoreCaseAndIdNot(...)` $\rightarrow$ **FALSE**; `existsByCuitAndIdNot(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe otro fabricante de barril activo con el CUIT '30-99999999-9'". No persiste.|
|**CP-MFB-04**|Conservar la razón social y el CUIT propios _(Camino feliz)_|`id: 1L`, `razonSocial`/`cuit` iguales a los actuales del fabricante|`...AndIdNot(...)` $\rightarrow$ **FALSE** para ambos (se excluye el propio ID)|Permite continuar con la modificación, persiste y retorna DTO.|
|**CP-MFB-05**|Modificación exitosa con datos nuevos _(Camino feliz)_|`id: 1L` (Existe), DTO válido con razón social, nombre comercial, CUIT, teléfono y email nuevos y únicos|Ambas validaciones $\rightarrow$ **FALSE**|Actualiza los 5 campos sobre la entidad administrada, guarda y retorna DTO con los nuevos valores.|

### 5. `bajaFabricanteBarril(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BFB-01**|Baja de fabricante de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `barrilRepository.existsByFabricanteId`, ni llama a `save()`.|
|**CP-BFB-02**|Baja rechazada por barriles activos asociados|`id: 1L` (Existe en BD); tiene al menos un barril con `estado: ACTIVO` asociado|`existsByFabricanteId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` con mensaje "No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados". No llama a `save()`.|
|**CP-BFB-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD, sin barriles activos asociados)|`existsByFabricanteId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del fabricante dado de baja.|
