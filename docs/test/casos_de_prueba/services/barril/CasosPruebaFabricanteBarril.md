### 1. `filtrarFabricantesBarril(String nombre, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FFB-01**|Filtra por nombre informado|`nombre: "Tonelería"`, `pageable: PageRequest.of(0, 10)`, BD con 2 fabricantes activos que cumplen el criterio|`filtrarFabricantesBarril("Tonelería", pageable)` contiene elementos|Retorna `Page<FabricanteBarrilResponseDTO>` con 2 elementos mapeados.|
|**CP-FFB-02**|El nombre nulo no restringe la búsqueda|`nombre: null`, `pageable: PageRequest.of(0, 10)`|El service propaga el parámetro nulo tal cual al repositorio|Retorna `Page<FabricanteBarrilResponseDTO>` con todos los fabricantes activos (equivalente a no filtrar).|
|**CP-FFB-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `pageable: PageRequest.of(0, 10)`|`filtrarFabricantesBarril("Inexistente", pageable)` está vacío|Retorna `Page<FabricanteBarrilResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FFB-04**|El nombre buscado se normaliza|`nombre: "   Tonelería   del Sur  "`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `"Tonelería del Sur"` y retorna la página mapeada a DTO.|
|**CP-FFB-05**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Fabricante de barril encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `FabricanteBarrilResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Fabricante de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el fabricante de barril con ID: 99".|

### 3. `altaFabricanteBarril(FabricanteBarrilFormDTO fabricanteBarrilFormDTO)`

El fabricante solo indica el modelo de fabricación del barril, por lo que su único dato es el nombre. El Service normaliza el nombre (recorte de los extremos y colapso de espacios, conservando mayúsculas y tildes), valida que sea obligatorio y de hasta 100 caracteres, y verifica que no lo tenga otro fabricante activo. El índice único `udx_fabricante_barril_nombre` es la garantía final ante pedidos simultáneos.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AFB-01**|Nombre duplicado (activo)|`nombre: "Tonelería del Sur"` (pertenece a un fabricante activo existente)|`existsByNombreIgnoreCase(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un fabricante de barril con el nombre 'Tonelería del Sur'". No ejecuta `save()`.|
|**CP-AFB-02**|Nombre duplicado Case-Insensitive|`nombre: "tonelería del sur"` (existe `"Tonelería del Sur"`)|`existsByNombreIgnoreCase(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un fabricante de barril con el nombre 'tonelería del sur'". No ejecuta `save()`.|
|**CP-AFB-03**|Alta exitosa _(Camino feliz)_|DTO válido: nombre único|`existsByNombreIgnoreCase(...)` $\rightarrow$ **FALSE**|Persiste la entidad con el `nombre` del DTO y `estado = ACTIVO`, y retorna DTO.|
|**CP-AFB-04**|Normalización del nombre antes de validar y guardar|`nombre: "  Tonelería   del  Sur  "`|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Tonelería del Sur"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-AFB-05**|Nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AFB-06**|Duplicado detectado sobre el valor normalizado|`nombre: "  tonelería  del sur "` (existe un fabricante activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un fabricante de barril con el nombre 'tonelería del sur'"). No ejecuta `save()`.|
|**CP-AFB-07**|Violación del índice único al guardar|`nombre: "Tonelería del Sur"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_fabricante_barril_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un fabricante de barril con el nombre 'Tonelería del Sur'").|
|**CP-AFB-08**|Reutilización del nombre de un fabricante dado de baja|`nombre: "Tonelería del Sur"` (solo existe un fabricante con `estado = BAJA` con ese nombre)|La consulta de unicidad considera solo fabricantes `ACTIVO` $\rightarrow$ **FALSE**|Persiste con éxito: el nombre de un fabricante dado de baja puede volver a usarse.|
|**CP-AFB-09**|Nombre que supera el tope de 100 caracteres|`nombre`: 101 caracteres|El nombre normalizado tiene más de 100 caracteres|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AFB-10**|Nombre en el límite de 100 caracteres|`nombre`: exactamente 100 caracteres (único)|El nombre normalizado tiene 100 caracteres $\rightarrow$ dentro del tope|Persiste con éxito.|

### 4. `modificarFabricanteBarril(Long id, FabricanteBarrilFormDTO fabricanteBarrilFormDTO)`

Normaliza y valida el nombre (obligatorio y de hasta 100 caracteres) antes de consultar la BD; luego localiza el fabricante y, recién si existe, valida la duplicidad del nombre excluyendo al propio fabricante que se está modificando.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MFB-01**|Fabricante de barril no encontrado por ID|`id: 99L` (No existe o dado de baja), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `existsByNombreIgnoreCaseAndIdNot`, ni persiste.|
|**CP-MFB-02**|Nombre en uso por otro fabricante activo|`id: 1L` (Existe), `nombre: "Tonelería del Norte"` (pertenece a otro fabricante activo)|`findById(1L)` $\rightarrow$ **Presente**; `existsByNombreIgnoreCaseAndIdNot(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un fabricante de barril con el nombre 'Tonelería del Norte'". No persiste.|
|**CP-MFB-03**|Nombre en uso por otro fabricante activo con distinto case (Case-Insensitive)|`id: 1L` (Existe), `nombre: "tonelería del norte"` (existe `"Tonelería del Norte"` en otro fabricante activo)|`existsByNombreIgnoreCaseAndIdNot(...)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` con mensaje "Ya existe un fabricante de barril con el nombre 'tonelería del norte'". No persiste.|
|**CP-MFB-04**|Conservar el nombre propio _(Camino feliz)_|`id: 1L`, `nombre` igual al actual del fabricante|`existsByNombreIgnoreCaseAndIdNot(...)` $\rightarrow$ **FALSE** (se excluye el propio ID)|Permite continuar con la modificación, persiste y retorna DTO.|
|**CP-MFB-05**|Modificación exitosa con nombre nuevo _(Camino feliz)_|`id: 1L` (Existe), DTO válido con nombre nuevo y único|`existsByNombreIgnoreCaseAndIdNot(...)` $\rightarrow$ **FALSE**|Actualiza el `nombre` sobre la entidad administrada, guarda y retorna DTO con el nuevo valor. El estado permanece `ACTIVO`.|
|**CP-MFB-06**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Tonelería   Nueva  "`|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Tonelería Nueva"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-MFB-07**|Nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-MFB-08**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  tonelería  del norte "` (en uso por otro fabricante activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un fabricante de barril con el nombre 'tonelería del norte'"). No persiste.|
|**CP-MFB-09**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Tonelería del Norte"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_fabricante_barril_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un fabricante de barril con el nombre 'Tonelería del Norte'").|
|**CP-MFB-10**|Reutilización del nombre de un fabricante dado de baja|`id: 1L` (existe), `nombre: "Tonelería del Norte"` (solo otro fabricante con `estado = BAJA` tiene ese nombre)|La consulta de unicidad considera solo fabricantes `ACTIVO` $\rightarrow$ **FALSE**|Persiste con éxito: el nombre de un fabricante dado de baja puede volver a usarse.|
|**CP-MFB-11**|Nombre que supera el tope de 100 caracteres|`id: 1L`, `nombre`: 101 caracteres|El nombre normalizado tiene más de 100 caracteres|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD (`verifyNoInteractions`).|

### 5. `bajaFabricanteBarril(Long id)`

La baja es lógica (`estado = BAJA`) y libera el nombre: a partir de ese momento otro fabricante activo puede usarlo (ver CP-AFB-08 y CP-MFB-10).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BFB-01**|Baja de fabricante de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `barrilRepository.existsByFabricanteId`, ni llama a `save()`.|
|**CP-BFB-02**|Baja rechazada por barriles activos asociados|`id: 1L` (Existe en BD); tiene al menos un barril con `estado: ACTIVO` asociado|`existsByFabricanteId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` con mensaje "No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados". No llama a `save()`.|
|**CP-BFB-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD, sin barriles activos asociados)|`existsByFabricanteId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del fabricante dado de baja.|
