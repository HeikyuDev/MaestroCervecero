### 1. `filtrarBusquedasBarril(Long idSolicitudBusqueda, EstadoTransaccion estado, Pageable pageable)`

`estado` no asume `REGISTRADO` por defecto: una búsqueda anulada sigue siendo un registro histórico consultable, así que `null` muestra ambos estados. Todos los criterios son opcionales.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FBB-01**|Filtra por los 2 criterios informados|`idSolicitudBusqueda: 1L`, `estado: REGISTRADO`, `pageable: PageRequest.of(0, 10)`, BD con 1 búsqueda que cumple los dos criterios|`filtrarBusquedasBarril(1L, REGISTRADO, pageable)` contiene elementos|Retorna `Page<BusquedaBarrilResponseDTO>` con 1 elemento mapeado.|
|**CP-FBB-02**|Los 2 parámetros nulos no restringen la búsqueda, incluyendo mezcla de estados|`idSolicitudBusqueda: null`, `estado: null`, `pageable: PageRequest.of(0, 10)`, BD con 1 búsqueda REGISTRADO y 1 ANULADO|El service propaga los 2 parámetros nulos tal cual al repositorio|Retorna `Page<BusquedaBarrilResponseDTO>` con las 2 búsquedas, sin excluir la ANULADO.|
|**CP-FBB-03**|El usuario puede acotar explícitamente a un solo estado|`idSolicitudBusqueda: null`, `estado: ANULADO`, `pageable: PageRequest.of(0, 10)`, BD con 1 búsqueda REGISTRADO y 1 ANULADO|`filtrarBusquedasBarril(null, ANULADO, pageable)` contiene elementos|Retorna `Page<BusquedaBarrilResponseDTO>` con 1 elemento (solo la ANULADO).|
|**CP-FBB-04**|Consulta sin coincidencias|`idSolicitudBusqueda: 99L`, `estado: null`, `pageable: PageRequest.of(0, 10)`|`filtrarBusquedasBarril(99L, null, pageable)` está vacío|Retorna `Page<BusquedaBarrilResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FBB-05**|Filtra por una solicitud de búsqueda específica|`idSolicitudBusqueda: 1L`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`, BD con 1 búsqueda de la solicitud 1L y 1 de otra solicitud|`filtrarBusquedasBarril(1L, null, pageable)` contiene elementos|Retorna `Page<BusquedaBarrilResponseDTO>` con la búsqueda de la solicitud 1L, sin incluir la de la otra solicitud.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Búsqueda de barril encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `BusquedaBarrilResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Búsqueda de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la búsqueda de barril con ID: 99".|

### 3. `registrarBusquedaBarril(BusquedaBarrilFormDTO busquedaBarrilFormDTO)`

Solo se puede registrar a partir de una solicitud de búsqueda con `buscado = false`. Al registrar, esa solicitud pasa a `buscado = true` — mientras la búsqueda siga registrada, no se puede volver a registrar otra sobre la misma solicitud.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RBB-01**|Solicitud de búsqueda no encontrada|`idSolicitudBusqueda: 99L` (No existe)|`solicitudBusquedaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la solicitud de búsqueda con ID: 99"). No ejecuta `save()`.|
|**CP-RBB-02**|La solicitud de búsqueda ya fue buscada|`idSolicitudBusqueda: 1L` (Existe, `buscado: true`)|`solicitudBusquedaEntity.isBuscado()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se puede registrar una búsqueda sobre una solicitud de búsqueda que todavía no fue buscada"). No ejecuta `save()` de la búsqueda.|
|**CP-RBB-03**|Registro exitoso _(Camino feliz)_|`idSolicitudBusqueda: 1L` (Existe, `buscado: false`)|Todas las validaciones $\rightarrow$ **FALSE**|La solicitud de búsqueda pasa a `buscado = true` y se persiste. La búsqueda se persiste con `estado = REGISTRADO` y `solicitudBusqueda` asignada, y retorna DTO.|

### 4. `anularBusquedaBarril(Long id, AnulacionBusquedaBarrilFormDTO anulacionFormDTO)`

Solo procede sobre búsquedas en estado `REGISTRADO`. Al anular, la solicitud de búsqueda asociada vuelve a `buscado = false`, permitiendo registrar una nueva búsqueda sobre ella.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ABB-01**|Motivo de anulación nulo|`id: 1L`, `motivoAnulacion: null`|`motivoAnulacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio"). No consulta ningún repositorio.|
|**CP-ABB-02**|Motivo de anulación en blanco|`id: 1L`, `motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-ABB-03**|Búsqueda de barril no encontrada|`id: 99L` (No existe), `motivoAnulacion: "Búsqueda cargada por error"`|`busquedaBarrilRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la búsqueda de barril con ID: 99"). No ejecuta `save()`.|
|**CP-ABB-04**|Búsqueda que no está en estado REGISTRADO|`id: 1L` (Existe, `estado: ANULADO`), `motivoAnulacion: "Búsqueda cargada por error"`|`estado != REGISTRADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden anular búsquedas de barril en estado REGISTRADO"). No ejecuta `save()`.|
|**CP-ABB-05**|Anulación exitosa _(Camino feliz)_|`id: 1L` (Existe, `estado: REGISTRADO`, solicitud asociada con `buscado: true`), `motivoAnulacion: "Búsqueda cargada por error"`|Todas las validaciones $\rightarrow$ **FALSE**|La solicitud de búsqueda asociada vuelve a `buscado = false` y se persiste. La búsqueda pasa a `estado = ANULADO`, con `fechaAnulacion` y `motivoAnulacion` seteados, se persiste y retorna DTO.|
