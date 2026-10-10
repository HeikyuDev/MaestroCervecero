### 1. `filtrarLupulos(String nombre, FormatoLupulo formato, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FL-01**|Filtra por nombre y formato informados|`nombre: "Cascade"`, `formato: PELLET`, `pageable: PageRequest.of(0, 10)`, BD con 2 lúpulos activos que cumplen ambos criterios|`filtrarLupulos("Cascade", PELLET, pageable)` contiene elementos|Retorna `Page<LupuloResponseDTO>` con 2 elementos mapeados.|
|**CP-FL-02**|Nombre y formato nulos no restringen la búsqueda|`nombre: null`, `formato: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<LupuloResponseDTO>` con todos los lúpulos activos (equivalente a no filtrar).|
|**CP-FL-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `formato: null`, `pageable: PageRequest.of(0, 10)`|`filtrarLupulos("Inexistente", null, pageable)` está vacío|Retorna `Page<LupuloResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Lúpulo encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `LupuloResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Lúpulo inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El lúpulo no existe".|

### 3. `altaLupulo(LupuloFormDTO formDTO)`

Los ácidos alfa deben ser un número real: NaN o infinito se rechazan con "El porcentaje de ácidos alfa debe ser un número mayor a 0". Además deben ser mayores a 0% y menores o iguales a 25% (mensaje: "Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%", que también se usa para nulo, cero y negativos) y el nombre no puede superar 100 caracteres (mensaje: "El nombre no puede superar 100 caracteres").

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AL-01**|Ácidos alfa nulo|`aa: null`, `nombre: "Cascade"`|`aa == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-02**|Ácidos alfa igual a cero _(Límite inf.)_|`aa: 0`, `nombre: "Cascade"`|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-03**|Ácidos alfa negativo|`aa: -1`, `nombre: "Cascade"`|`aa <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-04**|Nombre duplicado|`aa: 6`, `nombre: "Citra"` (Existe en BD)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-05**|Nombre duplicado Case-Insensitive|`aa: 6`, `nombre: "citra"` (Existe `"Citra"`)|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AL-06**|Alta exitosa y asignación de `GRAMO` _(Camino feliz)_|`aa: 12`, `nombre: "Mosaic"` (Único), `formato: PELLET`|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad asignando fijamente `unidadDeMedida = GRAMO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AL-07**|Ácidos alfa en límite inferior entero válido|`aa: 1`, `nombre: "Saaz"`|`aa <= 0 \| aa > 25` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `aa = 1`.|
|**CP-AL-08**|Ácidos alfa mayores a 25 _(Límite sup.)_|`aa: 25.01`, `nombre: "Cascade"`|`aa > 25` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AL-09**|Ácidos alfa en límite superior válido|`aa: 25`, `nombre: "Super Alpha"`|`aa <= 0 \| aa > 25` $\rightarrow$ **FALSE**|Persiste con éxito y retorna DTO con `aa = 25`.|
|**CP-AL-10**|El porcentaje de ácidos alfa es NaN|`aa: Double.NaN` (resto válido)|El porcentaje de ácidos alfa es NaN $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje de ácidos alfa debe ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-11**|El porcentaje de ácidos alfa es infinito positivo|`aa: Double.POSITIVE_INFINITY` (resto válido)|El porcentaje de ácidos alfa es infinito positivo $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje de ácidos alfa debe ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-12**|El porcentaje de ácidos alfa es infinito negativo|`aa: Double.NEGATIVE_INFINITY` (resto válido)|El porcentaje de ácidos alfa es infinito negativo $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje de ácidos alfa debe ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-13**|El porcentaje de ácidos alfa es exactamente 25 (valor límite válido)|`aa: 25.0` (resto válido)|El porcentaje de ácidos alfa es exactamente 25 (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-AL-14**|El nombre tiene 101 caracteres|`nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-AL-15**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|

### 4. `modificarLupulo(Long id, LupuloFormDTO formDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ML-01**|Falla por ácidos alfa inválidos|`id: 1L`, `aa: 0` (o `null`, `26`)|`aa <= 0 \| aa > 25` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-ML-02**|Lúpulo no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "El lúpulo no existe". No persiste.|
|**CP-ML-03**|Nombre en uso por otro lúpulo|`id: 1L`, `nombre: "Citra"` (En uso por ID 2L)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No persiste.|
|**CP-ML-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Cascade Modificado"`, DTO válido|Nombre libre y lúpulo encontrado|Actualiza campos (`nombre`, `formato`, `aa`), mantiene `GRAMO`, guarda y retorna DTO.|
|**CP-ML-05**|Conservar nombre propio actual|`id: 1L`, `nombre: "cascade"` (Mismo lúpulo)|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|
|**CP-ML-06**|El porcentaje de ácidos alfa es NaN|`id: 1L` (existe), `aa: Double.NaN` (resto válido)|El porcentaje de ácidos alfa es NaN $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje de ácidos alfa debe ser un número mayor a 0"). No consulta la BD.|
|**CP-ML-07**|El nombre tiene 101 caracteres|`id: 1L` (existe), `nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-ML-08**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`id: 1L` (existe), `nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|

### 5. `bajaLupulo(Long id)`

No se puede dar de baja un lúpulo que forma parte de al menos una receta activa (`versión con esUltimaVersion = true` perteneciente a una receta con `estado = ACTIVO`), ni uno que tenga stock (al menos un lote de insumo con `cantidadActual > 0`).

| **ID**       | **Nombre del Caso**          | **Datos de Entrada (Escenario)** | **Condición Evaluada**                             | **Resultado Esperado**                                                                                         |
| ------------ | ---------------------------- | -------------------------------- | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **CP-BL-01** | Baja de lúpulo inexistente   | `id: 99L` (No existe en BD)      | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el lúpulo con ID: 99". No llama a `save()`. |
| **CP-BL-02** | Lúpulo forma parte de una receta activa | `id: 1L` (Existe), `versionRecetaRepository.existsByDetalleLupuloEnRecetaActiva(1L)` $\rightarrow$ **true** | Existe al menos una receta activa que lo usa | Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque forma parte de al menos una receta activa"). No consulta stock (`verifyNoInteractions(loteInsumoRepository)`) ni ejecuta `save()`. |
| **CP-BL-03** | Lúpulo tiene stock | `id: 1L` (Existe), `versionRecetaRepository.existsByDetalleLupuloEnRecetaActiva(1L)` $\rightarrow$ **false**, `loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)` $\rightarrow$ **true** | No está en ninguna receta activa, pero tiene al menos un lote con cantidad actual mayor a cero | Lanza `ReglaNegocioException` ("No se puede dar de baja el lúpulo porque tiene stock"). No ejecuta `save()`. |
| **CP-BL-04** | Baja exitosa _(Baja lógica vía Estado)_ | `id: 1L` (Existe en BD), sin receta activa ni stock asociados | `findById(1L)` $\rightarrow$ **Presente**, ambas validaciones $\rightarrow$ **FALSE** | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del lúpulo dado de baja. |