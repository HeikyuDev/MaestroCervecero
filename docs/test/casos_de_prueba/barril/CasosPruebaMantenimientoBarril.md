### 1. `filtrarMantenimientosBarril(Long idBarril, EstadoTransaccion estado, LocalDateTime fechaMantenimientoDesde, LocalDateTime fechaMantenimientoHasta, Pageable pageable)`

`estado` no asume `REGISTRADO` por defecto: un mantenimiento anulado sigue siendo un registro histórico consultable, así que `null` muestra ambos estados. El rango de fecha de mantenimiento es independiente por extremo. Todos los criterios son opcionales.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FMB-01**|Filtra por los 4 criterios informados|`idBarril: 1L`, `estado: REGISTRADO`, `fechaMantenimientoDesde: 2026-01-01T00:00`, `fechaMantenimientoHasta: 2026-01-31T23:59`, `pageable: PageRequest.of(0, 10)`, BD con 1 mantenimiento que cumple los cuatro criterios|`filtrarMantenimientosBarril(1L, REGISTRADO, ..., pageable)` contiene elementos|Retorna `Page<MantenimientoBarrilResponseDTO>` con 1 elemento mapeado.|
|**CP-FMB-02**|Los 4 parámetros nulos no restringen la búsqueda, incluyendo mezcla de estados|`idBarril: null`, `estado: null`, `fechaMantenimientoDesde: null`, `fechaMantenimientoHasta: null`, `pageable: PageRequest.of(0, 10)`, BD con 1 mantenimiento REGISTRADO y 1 ANULADO|El service propaga los 4 parámetros nulos tal cual al repositorio|Retorna `Page<MantenimientoBarrilResponseDTO>` con los 2 mantenimientos, sin excluir el ANULADO.|
|**CP-FMB-03**|El usuario puede acotar explícitamente a un solo estado|`estado: ANULADO`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`, BD con 1 mantenimiento REGISTRADO y 1 ANULADO|`filtrarMantenimientosBarril(null, ANULADO, null, null, pageable)` contiene elementos|Retorna `Page<MantenimientoBarrilResponseDTO>` con 1 elemento (solo el ANULADO).|
|**CP-FMB-04**|Consulta sin coincidencias|`idBarril: 99L`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarMantenimientosBarril(99L, null, null, null, pageable)` está vacío|Retorna `Page<MantenimientoBarrilResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FMB-05**|Filtra por un barril específico|`idBarril: 1L`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`, BD con 2 mantenimientos del barril 1L y 1 de otro barril|`filtrarMantenimientosBarril(1L, null, null, null, pageable)` contiene elementos|Retorna `Page<MantenimientoBarrilResponseDTO>` con los 2 mantenimientos del barril 1L, sin incluir el del otro barril.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Mantenimiento de barril encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MantenimientoBarrilResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Mantenimiento de barril inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el mantenimiento de barril con ID: 99".|

### 3. `registrarMantenimientoBarril(MantenimientoBarrilFormDTO mantenimientoBarrilFormDTO)`

Solo se puede registrar sobre un barril en estado operativo `EN_MANTENIMIENTO`; el barril pasa a `DISPONIBLE` como parte del registro.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RMB-01**|Fecha de mantenimiento nula|`fechaMantenimiento: null`, `observaciones: "Se reemplazó la válvula de presión"`, `idBarril: 1L`|`fechaMantenimiento == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de mantenimiento es obligatoria"). No consulta el barril.|
|**CP-RMB-02**|Observaciones nulas|`fechaMantenimiento: 2026-01-20T09:00`, `observaciones: null`, `idBarril: 1L`|`observaciones == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Las observaciones son obligatorias"). No consulta el barril.|
|**CP-RMB-03**|Observaciones en blanco|`fechaMantenimiento: 2026-01-20T09:00`, `observaciones: "   "`, `idBarril: 1L`|`observaciones.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta el barril.|
|**CP-RMB-04**|Barril no encontrado|`idBarril: 99L` (No existe), resto de los datos válidos|`barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el barril con ID: 99"). No ejecuta `save()`.|
|**CP-RMB-05**|Barril no está en estado operativo EN_MANTENIMIENTO|`idBarril: 1L` (Existe, `estadoOperativo: DISPONIBLE`), resto de los datos válidos|`estadoOperativo != EN_MANTENIMIENTO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se puede registrar un mantenimiento sobre un barril en estado operativo EN_MANTENIMIENTO"). No ejecuta `save()` del mantenimiento.|
|**CP-RMB-06**|Registro exitoso _(Camino feliz)_|`idBarril: 1L` (Existe, `estadoOperativo: EN_MANTENIMIENTO`), `fechaMantenimiento: 2026-01-20T09:00`, `observaciones: "Se reemplazó la válvula de presión y se verificó el sellado"`|Todas las validaciones $\rightarrow$ **FALSE**|El barril pasa a `estadoOperativo = DISPONIBLE` y se persiste. El mantenimiento se persiste con `estado = REGISTRADO`, `fechaMantenimiento` y `observaciones` asignados, y retorna DTO.|

### 4. `anularMantenimientoBarril(Long id, AnulacionMantenimientoBarrilFormDTO anulacionFormDTO)`

Solo procede sobre mantenimientos en estado `REGISTRADO` cuyo barril asociado se encuentre en `DISPONIBLE`; el barril vuelve a `EN_MANTENIMIENTO` como parte de la anulación. Además, solo procede si es la operación más reciente registrada sobre el barril entre fallas, mantenimientos y limpiezas.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AMB-01**|Motivo de anulación nulo|`id: 1L`, `motivoAnulacion: null`|`motivoAnulacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio"). No consulta ningún repositorio.|
|**CP-AMB-02**|Motivo de anulación en blanco|`id: 1L`, `motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-AMB-03**|Mantenimiento de barril no encontrado|`id: 99L` (No existe), `motivoAnulacion: "Mantenimiento cargado por error"`|`mantenimientoBarrilRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el mantenimiento de barril con ID: 99"). No ejecuta `save()`.|
|**CP-AMB-04**|Mantenimiento que no está en estado REGISTRADO|`id: 1L` (Existe, `estado: ANULADO`), `motivoAnulacion: "Mantenimiento cargado por error"`|`estado != REGISTRADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden anular mantenimientos de barril en estado REGISTRADO"). No consulta el barril ni ejecuta `save()`.|
|**CP-AMB-05**|Barril asociado no encontrado|`id: 1L` (Existe, `estado: REGISTRADO`, referencia al barril `2L`), `motivoAnulacion: "Mantenimiento cargado por error"`, `barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)` $\rightarrow$ **Optional.empty()**|El barril no existe o no está activo|Lanza `RecursoNoEncontradoException` ("No se encontró el barril con ID: 2"). No ejecuta `save()` del mantenimiento.|
|**CP-AMB-06**|Barril asociado no está en estado operativo DISPONIBLE|`id: 1L` (Existe, `estado: REGISTRADO`), barril asociado existente con `estadoOperativo: CON_CERVEZA`, `motivoAnulacion: "Mantenimiento cargado por error"`|`estadoOperativo != DISPONIBLE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se puede anular un mantenimiento cuyo barril asociado se encuentre en estado operativo DISPONIBLE"). No ejecuta `save()` del mantenimiento.|
|**CP-AMB-07**|Anulación exitosa _(Camino feliz)_|`id: 1L` (Existe, `estado: REGISTRADO`), barril asociado con `estadoOperativo: DISPONIBLE`, `motivoAnulacion: "Mantenimiento cargado por error"`|Todas las validaciones $\rightarrow$ **FALSE**|El barril vuelve a `estadoOperativo = EN_MANTENIMIENTO` y se persiste. El mantenimiento pasa a `estado = ANULADO`, con `fechaAnulacion` y `motivoAnulacion` seteados, se persiste y retorna DTO.|
|**CP-AMB-08**|Existe un mantenimiento posterior del mismo tipo sobre el barril|`id: 1L` (Existe, `estado: REGISTRADO`, `fechaMantenimiento: 2026-01-20T09:00`, barril `2L`), `buscarFechaUltimoMantenimientoRegistrado(2L)` $\rightarrow$ **2026-01-25T09:00**|La fecha del mantenimiento más reciente del barril es posterior a la de este mantenimiento|Lanza `ReglaNegocioException` ("Solo se puede anular la operación más reciente registrada sobre este barril"). No consulta el barril ni ejecuta `save()`.|
|**CP-AMB-09**|Existe una operación posterior de otro tipo sobre el barril|`id: 1L` (Existe, `estado: REGISTRADO`, `fechaMantenimiento: 2026-01-20T09:00`, barril `2L`), `buscarFechaUltimoMantenimientoRegistrado(2L)` $\rightarrow$ **Optional.empty()**, `buscarFechaUltimaLimpiezaRegistrada(2L)` $\rightarrow$ **2026-01-22T09:00**|Existe una limpieza posterior a la fecha de este mantenimiento|Lanza `ReglaNegocioException` ("Solo se puede anular la operación más reciente registrada sobre este barril"). No consulta el barril ni ejecuta `save()`.|
