### 1. `filtrarMotivosAjuste(String nombre, TipoAjuste tipoAjuste, Pageable pageable)`

Solo devuelve motivos de ajuste activos (`estado = 'ACTIVO'`). `nombre` es coincidencia parcial, sin distinguir mayúsculas/minúsculas; `tipoAjuste` (INGRESO/EGRESO) es coincidencia exacta. Ambos opcionales.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FMA-01**|Filtra por nombre y tipo de ajuste informados|`nombre: "Rotura"`, `tipoAjuste: EGRESO`, `pageable: PageRequest.of(0, 10)`, BD con 2 motivos activos que cumplen ambos criterios|`filtrarMotivosAjuste("Rotura", EGRESO, pageable)` contiene elementos|Retorna `Page<MotivoAjusteResponseDTO>` con 2 elementos mapeados.|
|**CP-FMA-02**|Nombre y tipo de ajuste nulos no restringen la búsqueda|`nombre: null`, `tipoAjuste: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<MotivoAjusteResponseDTO>` con todos los motivos activos (equivalente a no filtrar).|
|**CP-FMA-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `tipoAjuste: null`, `pageable: PageRequest.of(0, 10)`|`filtrarMotivosAjuste("Inexistente", null, pageable)` está vacío|Retorna `Page<MotivoAjusteResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FMA-04**|El nombre buscado se normaliza|`nombre: "   Rotura   de   envase  "`, `tipoAjuste: null`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `nombre: "Rotura de envase"`.|
|**CP-FMA-05**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `tipoAjuste: null`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|
|**CP-FMA-06**|Los comodines de LIKE del nombre se escapan|`nombre: " 100% de_merma "`, `tipoAjuste: null`|El service escapa `%` y `_` con `!` (la consulta usa `ESCAPE '!'`)|Consulta el repositorio con `"100!% de!_merma"`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Motivo de ajuste encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MotivoAjusteResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Motivo de ajuste inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el motivo de ajuste con ID: 99".|
|**CP-BI-03**|Motivo con ajustes asociados|`id: 1L` (Existe) con ajustes de insumo asociados|`existsByMotivoAjusteId(1L)` $\rightarrow$ **TRUE**|El DTO informa `tieneAjustes = true` (con `false` cuando no tiene, como en CP-BI-01).|

### 3. `altaMotivoAjuste(MotivoAjusteFormDTO motivoAjusteFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AMA-01**|Nombre duplicado|`nombre: "Rotura de lote"` (Existe en BD), `tipoAjuste: EGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AMA-02**|Nombre duplicado Case-Insensitive|`nombre: "rotura de lote"` (Existe `"Rotura de lote"`), `tipoAjuste: EGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AMA-03**|Alta exitosa con tipo INGRESO _(Camino feliz)_|`nombre: "Corrección de conteo"` (Único), `tipoAjuste: INGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `tipoAjuste = INGRESO` y `estado = ACTIVO` asignados, y retorna DTO.|
|**CP-AMA-04**|Alta exitosa con tipo EGRESO|`nombre: "Rotura de lote"` (Único), `tipoAjuste: EGRESO`|`existsByNombreIgnoreCase` $\rightarrow$ **FALSE**|Persiste la entidad con `tipoAjuste = EGRESO` y `estado = ACTIVO`, y retorna DTO.|
|**CP-AMA-05**|Normalización del nombre antes de validar y guardar|`nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad con `"Pilsen Nacional"`, persiste la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-AMA-06**|El nombre vacío tras normalizar|`nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-AMA-07**|Duplicado detectado sobre el valor normalizado|`nombre: "  pilsen "` (existe un registro activo equivalente)|`existsByNombreIgnoreCase` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un motivo de ajuste con el nombre 'pilsen'"). No ejecuta `save()`.|
|**CP-AMA-08**|Violación del índice único al guardar|`nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_motivo_ajuste_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un motivo de ajuste con el nombre 'Pilsen Nacional'").|
|**CP-AMA-09**|Reutilización del nombre de un registro dado de baja|`nombre: "Pilsen Nacional"` (solo existe un registro con `estado = BAJA` con ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-AMA-10**|Las tildes distinguen nombres|`nombre: "Pílsen"` (existe un registro activo con `"Pilsen"`)|La consulta de unicidad con `"Pílsen"` → **FALSE** (`Pilsen` y `Pílsen` son nombres distintos)|Persiste con éxito: los valores se comparan sin ignorar las tildes.|
|**CP-AMA-11**|Tipo de ajuste nulo|`nombre: "Rotura de lote"`, `tipoAjuste: null`|El tipo es obligatorio|Lanza `ReglaNegocioException` ("El tipo de ajuste es obligatorio") con el campo `tipoAjuste`. No consulta el repositorio.|
|**CP-AMA-12**|Los errores del nombre informan su campo|`nombre: "   "` y un nombre de 101 caracteres|Nombre vacío o demasiado largo|Ambas `ReglaNegocioException` informan el campo `nombre`; la segunda dice "El nombre no puede superar 100 caracteres".|

### 4. `modificarMotivoAjuste(Long id, MotivoAjusteFormDTO motivoAjusteFormDTO)`

No existe un caso en el que un mismo motivo a veces sume y a veces reste: si el motivo tiene al menos un ajuste de insumo asociado, registrado o anulado, su `tipoAjuste` queda fijo y cambiarlo lanza `ReglaNegocioException` en el campo `tipoAjuste`. El nombre se puede modificar siempre (la unicidad entre activos sigue vigente) y enviar el mismo tipo no es un cambio. Un `tipoAjuste` nulo se rechaza con "El tipo de ajuste es obligatorio" (campo `tipoAjuste`) y los errores del nombre informan el campo `nombre`.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MMA-01**|Motivo de ajuste no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación, ni ejecuta `save()` (`verifyNoInteractions`).|
|**CP-MMA-02**|Nombre en uso por otro motivo de ajuste|`id: 1L` (Existe), `nombre: "Rotura de lote"` (Pertenece al `id: 2L`), `tipoAjuste: INGRESO`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-MMA-03**|Conservar el nombre propio actual|`id: 1L`, `nombre: "corrección de conteo"` (Mismo motivo de ajuste, distinto case), `tipoAjuste: INGRESO`|`existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MMA-04**|Modificación exitosa cambiando el tipo de ajuste _(Camino feliz)_|`id: 1L` (Existe, `tipoAjuste: INGRESO`), `nombre: "Corrección de conteo (egreso)"` (Libre), `tipoAjuste: EGRESO`, sin ajustes de insumo registrados|Validaciones OK|Actualiza `nombre` y `tipoAjuste`, ejecuta `save()` y retorna DTO con `tipoAjuste = EGRESO`.|
|**CP-MMA-05**|Cambio de tipo en un motivo con ajustes de insumo asociados|`id: 1L` (Existe, `tipoAjuste: INGRESO`), `tipoAjuste` del formulario `EGRESO`, el motivo tiene ajustes asociados (registrados o anulados: la consulta no distingue el estado)|`existsByMotivoAjusteId(1L)` $\rightarrow$ **TRUE** y el tipo cambia|Lanza `ReglaNegocioException` ("No se puede cambiar el tipo de ajuste porque el motivo ya tiene ajustes de insumo asociados") con el campo `tipoAjuste`. No consulta la unicidad del nombre ni ejecuta `save()`.|
|**CP-MMA-06**|Normalización del nombre antes de validar y guardar|`id: 1L` (existe), `nombre: "  Pilsen   Nacional  "` (resto válido)|El Service normaliza el nombre antes de validar, consultar la unicidad y guardar|Consulta la unicidad (excluyendo el propio ID) con `"Pilsen Nacional"`, actualiza la entidad con ese valor (`save` y `flush`) y no modifica el DTO.|
|**CP-MMA-07**|El nombre vacío tras normalizar|`id: 1L`, `nombre: "   "` (solo espacios)|El nombre normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio"). No consulta la BD (`verifyNoInteractions`).|
|**CP-MMA-08**|Duplicado detectado sobre el valor normalizado|`id: 1L` (existe), `nombre: "  pilsen "` (en uso por otro registro activo)|`existsByNombreIgnoreCaseAndIdNot` con el valor normalizado → **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un motivo de ajuste con el nombre 'pilsen'"). No persiste.|
|**CP-MMA-09**|Violación del índice único al guardar|`id: 1L` (existe), `nombre: "Pilsen Nacional"`, la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_motivo_ajuste_nombre` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un motivo de ajuste con el nombre 'Pilsen Nacional'").|
|**CP-MMA-10**|Reutilización del nombre de un registro dado de baja|`id: 1L` (existe), `nombre: "Pilsen Nacional"` (solo otro registro con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo registros `ACTIVO` → **FALSE**|Persiste con éxito: el valor de un registro dado de baja puede volver a usarse.|
|**CP-MMA-11**|Cambio de tipo en un motivo sin ajustes|`id: 1L` (`tipoAjuste: INGRESO`), formulario con `EGRESO`, sin ajustes asociados|`existsByMotivoAjusteId(1L)` $\rightarrow$ **FALSE**|Persiste el nuevo tipo y retorna el DTO con `tipoAjuste = EGRESO`.|
|**CP-MMA-12**|Cambio de nombre en un motivo con ajustes|`id: 1L` (`INGRESO`) con ajustes asociados, nombre nuevo único, mismo tipo|El tipo no cambia, por lo que no se consulta si tiene ajustes|Persiste el nuevo nombre, conserva el tipo y no consulta `existsByMotivoAjusteId`.|
|**CP-MMA-13**|Mismo tipo en un motivo con ajustes|`id: 1L` (`EGRESO`) con ajustes asociados, mismo nombre y mismo tipo|Enviar el tipo sin cambios no es un cambio de tipo|Persiste la modificación sin lanzar excepción.|
|**CP-MMA-14**|Tipo de ajuste nulo|`tipoAjuste: null`, nombre válido|El tipo es obligatorio|Lanza `ReglaNegocioException` ("El tipo de ajuste es obligatorio") con el campo `tipoAjuste`. No consulta el repositorio.|
|**CP-MMA-15**|El error del nombre informa su campo|`nombre: "   "`|El nombre normalizado queda vacío|La `ReglaNegocioException` informa el campo `nombre`.|

### 5. `bajaMotivoAjuste(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BMA-01**|Baja de motivo de ajuste inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BMA-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO del motivo de ajuste dado de baja.|
|**CP-BMA-03**|Baja de un motivo con ajustes asociados|`id: 1L` (Existe) con ajustes de insumo asociados, registrados o anulados|`existsByMotivoAjusteId(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("No se puede dar de baja el motivo de ajuste porque tiene ajustes de insumo asociados"). El estado sigue `ACTIVO` y no llama a `save()`.|
