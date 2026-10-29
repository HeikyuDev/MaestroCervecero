### 1. `filtrarLocalidades(String nombre, String codigoPostal, Long idProvincia, Long idPais, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas; `codigoPostal`, `idProvincia` e `idPais` son coincidencia exacta. `idPais` filtra a través de la provincia de cada localidad (dos saltos: localidad → provincia → país), no es un campo propio de la entidad. Todos son opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FL-01**|Filtra por los 4 criterios informados|`nombre: "Palermo"`, `codigoPostal: "1414"`, `idProvincia: 1L`, `idPais: 1L`, `pageable: PageRequest.of(0, 10)`, BD con 1 localidad activa que cumple los cuatro criterios|`filtrarLocalidades("Palermo", "1414", 1L, 1L, pageable)` contiene elementos|Retorna `Page<LocalidadResponseDTO>` con 1 elemento mapeado.|
|**CP-FL-02**|Los 4 parámetros nulos no restringen la búsqueda|`nombre: null`, `codigoPostal: null`, `idProvincia: null`, `idPais: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 localidades activas|El service propaga los 4 parámetros nulos tal cual al repositorio|Retorna `Page<LocalidadResponseDTO>` con las 3 localidades activas (equivalente a no filtrar).|
|**CP-FL-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarLocalidades("Inexistente", null, null, null, pageable)` está vacío|Retorna `Page<LocalidadResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FL-04**|El nombre y el código postal buscados se normalizan|`nombre: "   Villa   Carlos Paz  "`, `codigoPostal: "  b1636  "`, `idProvincia: null`, `idPais: null`, `pageable: PageRequest.of(0, 10)`|Los textos buscados reciben el mismo tratamiento que los valores guardados: sin espacios en los extremos ni repetidos, y el código postal en mayúsculas|Consulta el repositorio con `nombre: "Villa Carlos Paz"` y `codigoPostal: "B1636"`.|
|**CP-FL-05**|Un nombre y un código postal formados solo por espacios no filtran|`nombre: "    "`, `codigoPostal: "   "`, `idProvincia: null`, `idPais: null`, `pageable: PageRequest.of(0, 10)`|Tras normalizar quedan vacíos y se interpretan como ausencia de filtro|Consulta el repositorio con `nombre: null` y `codigoPostal: null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Localidad encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `LocalidadResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Localidad inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la localidad con ID: 99".|

### 3. `altaLocalidad(LocalidadFormDTO localidadFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AL-01**|Provincia inexistente|`nombre: "Palermo"`, `codigoPostal: "1414"`, `idProvincia: 99L` (No existe)|`provinciaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida nombre ni código postal, ni ejecuta `save()`.|
|**CP-AL-02**|Nombre duplicado en la misma provincia|`nombre: "Palermo"` (Existe en la provincia 1), `codigoPostal: "1414"`, `idProvincia: 1L` (Existe)|`existsByNombreIgnoreCaseAndProvinciaId` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-03**|Nombre duplicado Case-Insensitive|`nombre: "palermo"` (Existe `"Palermo"` en la provincia 1), `codigoPostal: "1414"`, `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaId` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-04**|Código postal repetido _(sin unicidad)_|`nombre: "Belgrano"` (Único en la provincia 1), `codigoPostal: "1414"` (Lo usa otra localidad), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaId` $\rightarrow$ **FALSE**; el código postal es un dato informativo y no se consulta|Persiste la entidad y retorna DTO con `codigoPostal = "1414"`.|
|**CP-AL-05**|Precedencia de la validación de provincia|`nombre: "Palermo"` (Duplicado si la provincia existiera), `codigoPostal: "1414"`, `idProvincia: 99L` (No existe)|`provinciaRepository.findById` se evalúa antes que `existsByNombreIgnoreCaseAndProvinciaId`|Lanza `RecursoNoEncontradoException` (no `RecursoDuplicadoException`). No consulta `localidadRepository`.|
|**CP-AL-06**|Alta exitosa _(Camino feliz)_|`nombre: "Belgrano"` (Único), `codigoPostal: "1428"`, `idProvincia: 1L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `codigoPostal`, `provincia` y `estado = ACTIVO` asignados, y retorna DTO.|
|**CP-AL-07**|Nombre de más de 100 caracteres _(Límite)_|`nombre`: 101 caracteres, `codigoPostal: "1414"`, `idProvincia: 1L`|Longitud del nombre `> 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD ni ejecuta `save()`.|
|**CP-AL-08**|Código postal de más de 10 caracteres _(Límite)_|`nombre: "Palermo"`, `codigoPostal`: 11 caracteres, `idProvincia: 1L`|Longitud del código postal `> 10` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El código postal no puede superar 10 caracteres"). No consulta la BD ni ejecuta `save()`.|
|**CP-AL-09**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-AL-10**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-11**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCaseAndProvinciaId` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe una localidad con el nombre 'pilsen' para la provincia seleccionada"). No ejecuta `save()`.|
|**CP-AL-12**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_localidad_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe una localidad con el nombre 'Pilsen Nacional' para la provincia seleccionada").|
|**CP-AL-13**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-AL-14**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|
|**CP-AL-15**|Código postal guardado en mayúsculas|`codigoPostal: "  c1428  "` (resto válido)|El Service normaliza el código postal como código (recorte y mayúsculas)|Persiste la localidad con `codigoPostal = "C1428"` y no modifica el DTO.|
|**CP-AL-16**|Código postal vacío tras normalizar|`codigoPostal: "   "` (solo espacios)|El código postal normalizado queda vacío|Lanza `ReglaNegocioException` ("El código postal es obligatorio"). No consulta la BD (`verifyNoInteractions`).|

### 4. `modificarLocalidad(Long id, LocalidadFormDTO localidadFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ML-01**|Localidad no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida provincia, nombre ni código postal, ni ejecuta `save()`.|
|**CP-ML-02**|Provincia inexistente|`id: 1L` (Existe), `idProvincia: 99L` (No existe)|`provinciaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida nombre ni código postal, ni ejecuta `save()`.|
|**CP-ML-03**|Nombre en uso por otra localidad de la misma provincia|`id: 1L` (Existe), `nombre: "Belgrano"` (Pertenece al `id: 2L` en la misma provincia), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaIdAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-ML-04**|Código postal repetido _(sin unicidad)_|`id: 1L` (Existe), `nombre: "Palermo"` (Propio), `codigoPostal: "1428"` (Lo usa otra localidad), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaIdAndIdNot` $\rightarrow$ **FALSE**; el código postal no se consulta|Persiste la entidad y retorna DTO con `codigoPostal = "1428"`.|
|**CP-ML-05**|Conservar el propio nombre y código postal actuales|`id: 1L`, `nombre: "palermo"` (Mismo, distinto case), `codigoPostal: "1414"` (Propio), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaIdAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-ML-06**|Modificación exitosa _(Camino feliz)_|`id: 1L` (Existe), `nombre: "Palermo Chico"` (Libre), `codigoPostal: "1425"`, `idProvincia: 2L` (Existe, distinta a la actual)|Todas las validaciones $\rightarrow$ **FALSE**|Actualiza `nombre`, `codigoPostal` y `provincia`, ejecuta `save()` y retorna DTO actualizado.|
|**CP-ML-07**|Nombre de más de 100 caracteres _(Límite)_|`id: 1L`, `nombre`: 101 caracteres, `codigoPostal: "1414"`, `idProvincia: 1L`|Longitud del nombre `> 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD ni ejecuta `save()`.|
|**CP-ML-08**|Código postal de más de 10 caracteres _(Límite)_|`id: 1L`, `nombre: "Palermo"`, `codigoPostal`: 11 caracteres, `idProvincia: 1L`|Longitud del código postal `> 10` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El código postal no puede superar 10 caracteres"). No consulta la BD ni ejecuta `save()`.|
|**CP-ML-09**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-ML-10**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-ML-11**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndProvinciaIdAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe una localidad con el nombre 'pilsen' para la provincia seleccionada"). No persiste.|
|**CP-ML-12**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_localidad_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe una localidad con el nombre 'Pilsen Nacional' para la provincia seleccionada").|
|**CP-ML-13**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-ML-14**|Código postal guardado en mayúsculas|`id: 1L` (existe), `codigoPostal: "  c1428  "` (resto válido)|El Service normaliza el código postal como código (recorte y mayúsculas)|Actualiza la localidad con `codigoPostal = "C1428"` y no modifica el DTO.|

### 5. `bajaLocalidad(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BL-01**|Baja de localidad inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `versionProveedorRepository` ni `clienteRepository`, ni llama a `save()`.|
|**CP-BL-02**|Baja de localidad asociada a un proveedor activo|`id: 1L` (Existe y asociada a la última versión de 1 proveedor activo)|`existsByLocalidadIdAndEsUltimaVersionTrue(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("...asociada a al menos un proveedor activo"). No consulta `clienteRepository` ni llama a `save()`.|
|**CP-BL-03**|Baja de localidad asociada a un cliente activo|`id: 1L` (Existe, sin proveedores asociados, y asociada a 1 cliente activo)|`existsByLocalidadIdAndEsUltimaVersionTrue(1L)` $\rightarrow$ **FALSE**, `existsByLocalidadId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("...asociada a al menos un cliente activo"). No llama a `save()`.|
|**CP-BL-04**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe, sin proveedores ni clientes asociados)|`existsByLocalidadIdAndEsUltimaVersionTrue(1L)` $\rightarrow$ **FALSE**, `existsByLocalidadId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO de la localidad dada de baja.|

### 6. `listarLocalidadesActivasDeProvincia(Long idProvincia)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LLo-01**|Localidades activas de la provincia mapeadas a DTO|`idProvincia: 5L`; el repositorio devuelve "Río Cuarto" (`id: 9`) y "Villa María" (`id: 10`)|`listarActivasPorProvincia(5L)` entrega dos localidades|Retorna dos DTO con esos ids y nombres, en el mismo orden (el orden alfabético lo define la consulta del repositorio).|
|**CP-LLo-02**|Provincia nula|`idProvincia: null`|No hay provincia por la que filtrar|Retorna una lista vacía sin consultar el repositorio.|
