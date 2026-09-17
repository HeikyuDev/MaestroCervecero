### 1. `filtrarMedicionesLote(Long idEtapaLote, Long idDetalleParametroControl, EstadoTransaccion estado, LocalDateTime fechaMedicionDesde, LocalDateTime fechaMedicionHasta, Pageable pageable)`

`idEtapaLote` e `idDetalleParametroControl` son **obligatorios**: definen el contexto fijo de la pantalla de gestión de mediciones (una etapa de un lote puntual y un detalle de parámetro de control puntual — nunca tiene sentido mezclar mediciones de distintos parámetros o distintos lotes en la misma vista), y son las mismas relaciones directas de `MedicionLoteEntity`.

`estado` es un criterio de negocio legítimo para el usuario (a diferencia de una baja lógica, acá "ver lo anulado" tiene valor real): si no lo especifica, el service asume `REGISTRADO` por defecto — nunca deja pasar `null` sin filtrar, a diferencia del resto de los criterios opcionales de otros módulos. El rango de fechas (`fechaMedicionDesde`/`fechaMedicionHasta`) sí sigue la regla habitual: nulo no acota.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FML-01**|Filtra por los 2 criterios obligatorios más estado y rango de fechas informados|`idEtapaLote: 1L`, `idDetalleParametroControl: 5L`, `estado: REGISTRADO`, `fechaMedicionDesde/Hasta` acotando la fecha de medición, `pageable: PageRequest.of(0, 10)`, BD con 2 mediciones que cumplen todos los criterios|`filtrarMedicionesLote(...)` contiene elementos|Retorna `Page<MedicionLoteResponseDTO>` con 2 elementos mapeados.|
|**CP-FML-02**|Estado nulo asume REGISTRADO por defecto; el rango de fechas nulo sí queda sin acotar|`idEtapaLote: 1L`, `idDetalleParametroControl: 5L`, `estado: null`, `fechaMedicionDesde/Hasta: null`, `pageable: PageRequest.of(0, 10)`|El service reemplaza `estado: null` por `REGISTRADO` antes de llamar al repositorio, y propaga el rango de fechas nulo tal cual|Retorna `Page<MedicionLoteResponseDTO>` con las mediciones registradas de esa etapa de lote y detalle. Se verifica que el repositorio se invoque con `REGISTRADO`, no con `null`.|
|**CP-FML-03**|El usuario puede elegir explícitamente ver las mediciones anuladas|`idEtapaLote: 1L`, `idDetalleParametroControl: 5L`, `estado: ANULADO`, fechas `null`, `pageable: PageRequest.of(0, 10)`|`filtrarMedicionesLote(1L, 5L, ANULADO, null, null, pageable)` está vacío (sin anuladas en este escenario)|Retorna `Page<MedicionLoteResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Medición encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `MedicionLoteResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Medición inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la medición con ID: 99".|

### 3. `registrarMedicion(Long idEtapaLote, MedicionLoteFormDTO medicionLoteFormDTO)`

Escenario base salvo indicación contraria: lote en `EN_EJECUCION`, etapa actual Maceración (`EN_CURSO`), con un plan de monitoreo configurado en la receta para esa etapa, un detalle de parámetro de control (rango ideal `[5.0, 6.0]`, ideal `5.5`) cuyo `ParametroControlEntity` tiene un rango real más amplio `[0.0, 14.0]`, y `fechaMedicion` en el pasado.

El rango ideal (`DetalleParametroControlEntity.valorMinimo/valorMaximo`) es el objetivo deseado para esta receta — una medición fuera de él es normal y solo dispara `hayAlerta`. El rango real (`ParametroControlEntity.valorMinimo/valorMaximo`) es el límite físico del parámetro — una medición fuera de él se rechaza directamente.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RM-01**|Etapa de lote no encontrada|`idEtapaLote: 99L` (no existe)|`etapaLoteRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions` sobre `detalleParametroControlRepository` y `medicionLoteRepository`.|
|**CP-RM-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con los otros repos.|
|**CP-RM-03**|La etapa no admite mediciones|Etapa actual `MOLIENDA` (`permiteRegistrarMediciones = false`)|`!etapa.isPermiteRegistrarMediciones()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con los otros repos.|
|**CP-RM-04**|La receta no tiene plan de monitoreo para esa etapa|Único plan de monitoreo configurado para `HERVIDO`; etapa actual del lote es `MACERACION`|`planesMonitoreo.stream().anyMatch(...)` $\rightarrow$ **FALSE**|Lanza `ReglaNegocioException`. Sin interacciones con los otros repos.|
|**CP-RM-05**|Detalle de parámetro de control no encontrado|`idDetalleParametroControl: 99L` (no existe)|`detalleParametroControlRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(medicionLoteRepository)`.|
|**CP-RM-06**|El detalle de parámetro de control pertenece a otra etapa|Detalle configurado bajo un plan de monitoreo de `HERVIDO`; etapa actual del lote es `MACERACION`|`planMonitoreoEtapa.etapaControl.etapaAControlar != etapaActual` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. `verifyNoInteractions(medicionLoteRepository)`.|
|**CP-RM-07**|Fecha de medición posterior a la fecha actual|`fechaMedicion: ahora + 1 hora`|`fechaMedicion.isAfter(now())` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. `verifyNoInteractions(medicionLoteRepository)`.|
|**CP-RM-08**|Medición duplicada (mismo parámetro, misma fecha)|Ya existe una medición `REGISTRADO` para el mismo `idDetalleParametroControl` y la misma `fechaMedicion` exacta|`existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(...)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No se llama a `save()`.|
|**CP-RM-09**|Dos parámetros distintos en la misma fecha y hora exacta|Parámetro B, misma fecha que una medición previa del parámetro A (distinto ID)|`existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(idParametroB, ...)` $\rightarrow$ **FALSE**|No lanza — se registra normalmente. El chequeo de duplicado es por parámetro, no por etapa.|
|**CP-RM-10**|Registro exitoso con valor dentro del rango _(Camino feliz)_|`valorMedido: 5.5` (dentro de `[5.0, 6.0]`)|`valorMedido < min \| valorMedido > max` $\rightarrow$ **FALSE**|Persiste la medición con `estado = REGISTRADO`, `hayAlerta = false`, y los datos informados (`valorMedido`, `fechaMedicion`, detalle de parámetro de control, etapa de lote) correctamente asociados.|
|**CP-RM-11**|Valor por debajo del mínimo ideal (pero dentro del real) genera alerta|`valorMedido: 4.5` (por debajo del ideal `5.0`, dentro del real `[0.0, 14.0]`)|`valorMedido < detalle.valorMinimo` $\rightarrow$ **TRUE**; `valorMedido < parametroControl.valorMinimo` $\rightarrow$ **FALSE**|Persiste la medición con `hayAlerta = true`.|
|**CP-RM-12**|Valor por encima del máximo ideal (pero dentro del real) genera alerta|`valorMedido: 6.5` (por encima del ideal `6.0`, dentro del real `[0.0, 14.0]`)|`valorMedido > detalle.valorMaximo` $\rightarrow$ **TRUE**; `valorMedido > parametroControl.valorMaximo` $\rightarrow$ **FALSE**|Persiste la medición con `hayAlerta = true`.|
|**CP-RM-13**|Valor por debajo del mínimo real se rechaza|`valorMedido: -1.0` (por debajo del real `0.0`)|`valorMedido < parametroControl.valorMinimo` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No se llama a `save()`.|
|**CP-RM-14**|Valor por encima del máximo real se rechaza|`valorMedido: 15.0` (por encima del real `14.0`)|`valorMedido > parametroControl.valorMaximo` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No se llama a `save()`.|
|**CP-RM-15**|La etapa no es la etapa actual del lote (no está EN_CURSO)|Etapa Maceración existente pero en estado `PENDIENTE` (el lote ya avanzó a otra etapa)|`etapaLote.estado != EN_CURSO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con los otros repos.|

### 4. `anularMedicion(Long id, AnularMedicionLoteFormDTO anularMedicionLoteFormDTO)`

Escenario base salvo indicación contraria: medición existente en estado `REGISTRADO`, cuyo lote asociado está en `EN_EJECUCION`. Las reglas de "motivo obligatorio" y "la medición debe estar en REGISTRADO" no las enumeró el usuario explícitamente, pero se agregaron por ser el mismo patrón que siguen todas las demás anulaciones de entidades con `EstadoTransaccion` en el proyecto (`anularIngresoInsumo`, `anularAjusteInsumo`).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AM-01**|Motivo de anulación nulo|`motivoAnulacion: null`|`motivoAnulacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. `verifyNoInteractions(medicionLoteRepository)`.|
|**CP-AM-02**|Motivo de anulación vacío (solo espacios)|`motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. `verifyNoInteractions(medicionLoteRepository)`.|
|**CP-AM-03**|Medición no encontrada|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No se llama a `save()`.|
|**CP-AM-04**|Medición ya ANULADA|Medición existente con `estado: ANULADO`|`estado != REGISTRADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No se llama a `save()`.|
|**CP-AM-05**|Lote asociado no está EN_EJECUCION|Lote existente con `estado: FINALIZADO`|`lote.estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No se llama a `save()`.|
|**CP-AM-06**|Anulación exitosa _(Camino feliz)_|Escenario base completo|Todas las validaciones $\rightarrow$ **FALSE**|`estado = ANULADO`, `fechaAnulacion` seteada (justo ahora), `motivoAnulacion` guardado con el valor informado.|
