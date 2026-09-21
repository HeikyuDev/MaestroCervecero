### 1. `filtrarPlanificacionesProduccion(Long idReceta, EstadoSolicitud estado, LocalDate fechaInicio, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FPP-01**|Filtra por los 3 criterios informados|`idReceta: 1L`, `estado: PENDIENTE`, `fechaInicio: 2026-03-01`, `pageable: PageRequest.of(0, 10)`, BD con 1 planificación que cumple los 3 criterios|`filtrarPlanificacionesProduccion(1L, PENDIENTE, 2026-03-01, pageable)` contiene elementos|Retorna `Page<PlanificacionProduccionResponseDTO>` con 1 elemento mapeado.|
|**CP-FPP-02**|Los 3 criterios nulos no restringen la búsqueda|`idReceta: null`, `estado: null`, `fechaInicio: null`, `pageable: PageRequest.of(0, 10)`|El service propaga los 3 parámetros nulos tal cual al repositorio|Retorna `Page<PlanificacionProduccionResponseDTO>` con todas las planificaciones (equivalente a no filtrar).|
|**CP-FPP-03**|Consulta sin coincidencias|`idReceta: 99L`, `estado: null`, `fechaInicio: null`, `pageable: PageRequest.of(0, 10)`|`filtrarPlanificacionesProduccion(99L, null, null, pageable)` está vacío|Retorna `Page<PlanificacionProduccionResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Planificación de producción encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `PlanificacionProduccionResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Planificación de producción inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la planificación de producción con ID: 99".|

### 3. `registrarPlanificacionProduccion(PlanificacionProduccionFormDTO planificacionProduccionFormDTO)`

El usuario elige una receta, no una versión: el service resuelve la última versión activa (`esUltimaVersion = true`) de la receta indicada y la asocia a la planificación.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RPP-01**|Cantidad a producir nula|`cantidadAProducir: null`|`cantidadAProducir == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad a producir debe ser mayor a 0"). No consulta ningún repositorio.|
|**CP-RPP-02**|Cantidad a producir en cero _(Límite)_|`cantidadAProducir: 0.0`|`cantidadAProducir <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-RPP-03**|Fecha de inicio estimada nula|`fechaInicioEstimada: null`, `fechaFinalizacionEstimada: 2026-04-01`|`fechaInicioEstimada == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada"). No consulta ningún repositorio.|
|**CP-RPP-04**|Fecha de finalización estimada nula|`fechaInicioEstimada: 2026-03-01`, `fechaFinalizacionEstimada: null`|`fechaFinalizacionEstimada == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-RPP-05**|Fecha de finalización estimada anterior a la de inicio|`fechaInicioEstimada: 2026-04-01`, `fechaFinalizacionEstimada: 2026-03-01`|`fechaFinalizacionEstimada.isBefore(fechaInicioEstimada)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-RPP-06**|Receta no encontrada|`idReceta: 99L` (No existe)|`recetaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la receta con ID: 99"). No ejecuta `save()`.|
|**CP-RPP-07**|Receta sin versión activa|`idReceta: 1L` (Existe, ninguna versión con `esUltimaVersion: true`)|`buscarVersionActiva(...)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró una versión activa para la receta con ID: 1"). No ejecuta `save()`.|
|**CP-RPP-08**|Registro exitoso _(Camino feliz)_|`cantidadAProducir: 100.0`, `fechaInicioEstimada: 2026-03-01`, `fechaFinalizacionEstimada: 2026-04-01`, `idReceta: 1L` con versión activa|Todas las validaciones $\rightarrow$ **FALSE**|La planificación se persiste en `estado = PENDIENTE`, con la versión de receta activa asociada, y retorna `PlanificacionProduccionResponseDTO`.|

### 4. `anularPlanificacionProduccion(Long id, AnulacionPlanificacionProduccionFormDTO anulacionFormDTO)`

Solo procede sobre planificaciones en estado `PENDIENTE`, y solo si no tiene lotes en estado `PENDIENTE` o `EN_EJECUCION` (primero hay que cancelarlos) ni ningún lote en estado `FINALIZADO` asociado (la anulación es para errores de carga, no para producción que ya ocurrió).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-APP-01**|Motivo de anulación nulo|`id: 1L`, `motivoAnulacion: null`|`motivoAnulacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio"). No consulta ningún repositorio.|
|**CP-APP-02**|Motivo de anulación en blanco|`id: 1L`, `motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-APP-03**|Planificación de producción no encontrada|`id: 99L` (No existe), `motivoAnulacion: "Error de carga"`|`planificacionProduccionRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la planificación de producción con ID: 99"). No ejecuta `save()`.|
|**CP-APP-04**|Planificación que no está en estado PENDIENTE|`id: 1L` (Existe, `estado: FINALIZADA`), `motivoAnulacion: "Error de carga"`|`estado != PENDIENTE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden anular planificaciones de producción en estado PENDIENTE"). No consulta `loteRepository` ni ejecuta `save()`.|
|**CP-APP-05**|Planificación con un lote PENDIENTE o EN_EJECUCION asociado|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoAnulacion: "Error de carga"`, `loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, [PENDIENTE, EN_EJECUCION])` $\rightarrow$ **true**|Existe al menos un lote pendiente o en ejecución|Lanza `ReglaNegocioException` ("No se puede anular la planificación de producción porque tiene al menos un lote en estado PENDIENTE o EN_EJECUCION asociado"). No ejecuta `save()`.|
|**CP-APP-06**|Planificación con un lote FINALIZADO asociado|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoAnulacion: "Error de carga"`, sin lotes pendientes/en ejecución, `loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, FINALIZADO)` $\rightarrow$ **true**|No tiene lotes en curso, pero sí uno finalizado|Lanza `ReglaNegocioException` ("No se puede anular la planificación de producción porque tiene al menos un lote en estado FINALIZADO asociado"). No ejecuta `save()`.|
|**CP-APP-07**|Anulación exitosa sin lotes en curso ni finalizados _(Camino feliz)_|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoAnulacion: "Error de carga"`, sin lotes pendientes/en ejecución ni finalizados|Todas las validaciones $\rightarrow$ **FALSE**|La planificación pasa a `estado = ANULADA`, con `fechaAnulacion` y `motivoAnulacion` seteados, se persiste (`save()`) y retorna `PlanificacionProduccionResponseDTO`.|

### 5. `finalizarPlanificacionProduccion(Long id, FinalizacionForzadaPlanificacionProduccionFormDTO finalizacionFormDTO)`

Solo procede sobre planificaciones en estado `PENDIENTE`, y solo si no tiene lotes en estado `PENDIENTE` o `EN_EJECUCION` (primero hay que cancelarlos) y tiene **al menos un** lote en estado `FINALIZADO` asociado (condición inversa a `anularPlanificacionProduccion`): la finalización forzada cierra una planificación que sí llegó a producir algo pero no completó la cantidad solicitada; si no produjo nada, corresponde anularla en lugar de finalizarla.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FZPP-01**|Motivo de finalización nulo|`id: 1L`, `motivoFinalizacion: null`|`motivoFinalizacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de finalización es obligatorio"). No consulta ningún repositorio.|
|**CP-FZPP-02**|Motivo de finalización en blanco|`id: 1L`, `motivoFinalizacion: "   "`|`motivoFinalizacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-FZPP-03**|Planificación de producción no encontrada|`id: 99L` (No existe), `motivoFinalizacion: "Cantidad producida insuficiente"`|`planificacionProduccionRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la planificación de producción con ID: 99"). No ejecuta `save()`.|
|**CP-FZPP-04**|Planificación que no está en estado PENDIENTE|`id: 1L` (Existe, `estado: ANULADA`), `motivoFinalizacion: "Cantidad producida insuficiente"`|`estado != PENDIENTE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden finalizar planificaciones de producción en estado PENDIENTE"). No consulta `loteRepository` ni ejecuta `save()`.|
|**CP-FZPP-05**|Planificación con un lote PENDIENTE o EN_EJECUCION asociado|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoFinalizacion: "Cantidad producida insuficiente"`, `loteRepository.existsByPlanificacionProduccion_IdAndEstadoIn(1L, [PENDIENTE, EN_EJECUCION])` $\rightarrow$ **true**|Existe al menos un lote pendiente o en ejecución|Lanza `ReglaNegocioException` ("No se puede finalizar la planificación de producción porque tiene al menos un lote en estado PENDIENTE o EN_EJECUCION asociado"). No ejecuta `save()`.|
|**CP-FZPP-06**|Planificación sin ningún lote FINALIZADO asociado|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoFinalizacion: "Cantidad producida insuficiente"`, sin lotes pendientes/en ejecución, `loteRepository.existsByPlanificacionProduccion_IdAndEstado(1L, FINALIZADO)` $\rightarrow$ **false**|No existe ningún lote finalizado asociado|Lanza `ReglaNegocioException` ("No se puede finalizar la planificación de producción porque no tiene ningún lote en estado FINALIZADO asociado; corresponde anularla en su lugar"). No ejecuta `save()`.|
|**CP-FZPP-07**|Finalización forzada exitosa con al menos un lote FINALIZADO asociado _(Camino feliz)_|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoFinalizacion: "Cantidad producida insuficiente"`, sin lotes pendientes/en ejecución, con al menos un lote finalizado|Todas las validaciones $\rightarrow$ **FALSE**|La planificación pasa a `estado = FINALIZADA`, con `fechaFinalizacion` y `motivoFinalizacion` seteados, se persiste (`save()`) y retorna `PlanificacionProduccionResponseDTO`.|
