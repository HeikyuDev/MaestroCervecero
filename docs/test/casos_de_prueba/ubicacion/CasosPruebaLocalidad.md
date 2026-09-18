### 1. `filtrarLocalidades(String nombre, String codigoPostal, Long idProvincia, Long idPais, Pageable pageable)`

`nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas; `codigoPostal`, `idProvincia` e `idPais` son coincidencia exacta. `idPais` filtra a través de la provincia de cada localidad (dos saltos: localidad → provincia → país), no es un campo propio de la entidad. Todos son opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FL-01**|Filtra por los 4 criterios informados|`nombre: "Palermo"`, `codigoPostal: "1414"`, `idProvincia: 1L`, `idPais: 1L`, `pageable: PageRequest.of(0, 10)`, BD con 1 localidad activa que cumple los cuatro criterios|`filtrarLocalidades("Palermo", "1414", 1L, 1L, pageable)` contiene elementos|Retorna `Page<LocalidadResponseDTO>` con 1 elemento mapeado.|
|**CP-FL-02**|Los 4 parámetros nulos no restringen la búsqueda|`nombre: null`, `codigoPostal: null`, `idProvincia: null`, `idPais: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 localidades activas|El service propaga los 4 parámetros nulos tal cual al repositorio|Retorna `Page<LocalidadResponseDTO>` con las 3 localidades activas (equivalente a no filtrar).|
|**CP-FL-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarLocalidades("Inexistente", null, null, null, pageable)` está vacío|Retorna `Page<LocalidadResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Localidad encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `LocalidadResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Localidad inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La localidad no existe".|

### 3. `altaLocalidad(LocalidadFormDTO localidadFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AL-01**|Provincia inexistente|`nombre: "Palermo"`, `codigoPostal: "1414"`, `idProvincia: 99L` (No existe)|`provinciaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida nombre ni código postal, ni ejecuta `save()`.|
|**CP-AL-02**|Nombre duplicado en la misma provincia|`nombre: "Palermo"` (Existe en la provincia 1), `codigoPostal: "1414"`, `idProvincia: 1L` (Existe)|`existsByNombreIgnoreCaseAndProvinciaId` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No valida código postal ni ejecuta `save()`.|
|**CP-AL-03**|Nombre duplicado Case-Insensitive|`nombre: "palermo"` (Existe `"Palermo"` en la provincia 1), `codigoPostal: "1414"`, `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaId` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-04**|Código postal duplicado|`nombre: "Belgrano"` (Único en la provincia 1), `codigoPostal: "1414"` (Existe en otra localidad), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaId` $\rightarrow$ **FALSE**, `existsByCodigoPostal` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-05**|Precedencia de la validación de provincia|`nombre: "Palermo"` (Duplicado si la provincia existiera), `codigoPostal: "1414"`, `idProvincia: 99L` (No existe)|`provinciaRepository.findById` se evalúa antes que `existsByNombreIgnoreCaseAndProvinciaId`|Lanza `RecursoNoEncontradoException` (no `RecursoDuplicadoException`). No consulta `localidadRepository`.|
|**CP-AL-06**|Alta exitosa _(Camino feliz)_|`nombre: "Belgrano"` (Único), `codigoPostal: "1428"` (Único), `idProvincia: 1L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `codigoPostal`, `provincia` y `estado = ACTIVO` asignados, y retorna DTO.|

### 4. `modificarLocalidad(Long id, LocalidadFormDTO localidadFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ML-01**|Localidad no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida provincia, nombre ni código postal, ni ejecuta `save()`.|
|**CP-ML-02**|Provincia inexistente|`id: 1L` (Existe), `idProvincia: 99L` (No existe)|`provinciaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida nombre ni código postal, ni ejecuta `save()`.|
|**CP-ML-03**|Nombre en uso por otra localidad de la misma provincia|`id: 1L` (Existe), `nombre: "Belgrano"` (Pertenece al `id: 2L` en la misma provincia), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaIdAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No valida código postal ni ejecuta `save()`.|
|**CP-ML-04**|Código postal en uso por otra localidad|`id: 1L` (Existe), `nombre: "Palermo"` (Propio), `codigoPostal: "1428"` (Pertenece a otra localidad), `idProvincia: 1L`|`existsByCodigoPostalAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-ML-05**|Conservar el propio nombre y código postal actuales|`id: 1L`, `nombre: "palermo"` (Mismo, distinto case), `codigoPostal: "1414"` (Propio), `idProvincia: 1L`|`existsByNombreIgnoreCaseAndProvinciaIdAndIdNot` $\rightarrow$ **FALSE**, `existsByCodigoPostalAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-ML-06**|Modificación exitosa _(Camino feliz)_|`id: 1L` (Existe), `nombre: "Palermo Chico"` (Libre), `codigoPostal: "1425"` (Libre), `idProvincia: 2L` (Existe, distinta a la actual)|Todas las validaciones $\rightarrow$ **FALSE**|Actualiza `nombre`, `codigoPostal` y `provincia`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaLocalidad(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BL-01**|Baja de localidad inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta `versionProveedorRepository` ni `clienteRepository`, ni llama a `save()`.|
|**CP-BL-02**|Baja de localidad asociada a un proveedor activo|`id: 1L` (Existe y asociada a la última versión de 1 proveedor activo)|`existsByLocalidadIdAndEsUltimaVersionTrue(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("...asociada a al menos un proveedor activo"). No consulta `clienteRepository` ni llama a `save()`.|
|**CP-BL-03**|Baja de localidad asociada a un cliente activo|`id: 1L` (Existe, sin proveedores asociados, y asociada a 1 cliente activo)|`existsByLocalidadIdAndEsUltimaVersionTrue(1L)` $\rightarrow$ **FALSE**, `existsByLocalidadId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("...asociada a al menos un cliente activo"). No llama a `save()`.|
|**CP-BL-04**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe, sin proveedores ni clientes asociados)|`existsByLocalidadIdAndEsUltimaVersionTrue(1L)` $\rightarrow$ **FALSE**, `existsByLocalidadId(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO de la localidad dada de baja.|
