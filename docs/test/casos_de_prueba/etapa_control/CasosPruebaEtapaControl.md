### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 2 etapas activas|`findAll(pageable)` contiene elementos|Retorna `Page<EtapaControlResponseDTO>` con 2 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<EtapaControlResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Etapa de control encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `EtapaControlResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Etapa de control inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la etapa de control con ID: 99".|

### 3. `altaEtapaControl(EtapaControlFormDTO etapaControlFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AC-01**|Etapa a controlar nula|`nombre: "Control pH"`, `etapaAControlar: null`|`etapaAControlar == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AC-02**|Etapa no controlable (`MOLIENDA`)|`nombre: "Control Molienda"`, `etapaAControlar: TipoEtapa.MOLIENDA`|`!ETAPAS_CONTROLABLES.contains(...)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AC-03**|Etapa no controlable (`ENVASADO`)|`nombre: "Control Oxígeno"`, `etapaAControlar: TipoEtapa.ENVASADO`|`!ETAPAS_CONTROLABLES.contains(...)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AC-04**|Nombre duplicado para la misma etapa|`nombre: "Control Densidad"`, `etapaAControlar: MACERACION` (Ya existe)|`existsByNombre...AndEtapa...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AC-05**|Nombre duplicado Case-Insensitive para la misma etapa|`nombre: "control densidad"`, `etapaAControlar: MACERACION` (Existe `"Control Densidad"`)|`existsByNombre...AndEtapa...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AC-06**|Mismo nombre pero para diferente etapa controlable|`nombre: "Control Densidad"`, `etapaAControlar: FERMENTACION` (Existe `"Control Densidad"` pero en `MACERACION`)|`existsByNombre...AndEtapa...` $\rightarrow$ **FALSE**|Permite el alta, persiste la entidad y retorna DTO.|
|**CP-AC-07**|Alta exitosa _(Camino feliz)_|`nombre: "Control Temperatura Fermentación"`, `etapaAControlar: TipoEtapa.FERMENTACION` (Único)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `estado = ACTIVO` y retorna `EtapaControlResponseDTO`.|

### 4. `modificarEtapaControl(Long id, EtapaControlFormDTO etapaControlFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MC-01**|Falla por etapa a controlar no admitida|`id: 1L`, `etapaAControlar: TipoEtapa.MOLIENDA` (o `null`)|`!ETAPAS_CONTROLABLES.contains(...)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MC-02**|Nombre en uso por otra etapa de control para la misma etapa|`id: 1L`, `nombre: "Control pH"`, `etapaAControlar: HERVIDO` (En uso por ID 2L)|`existsByNombre...AndEtapa...AndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No busca por ID ni persiste.|
|**CP-MC-03**|Etapa de control no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la etapa de control con ID: 99". No persiste.|
|**CP-MC-04**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe), `nombre: "Control pH Final"`, `etapaAControlar: HERVIDO`, DTO válido|Nombre libre y etapa encontrada|Actualiza campos (`nombre`, `descripcion`, `etapaAControlar`), guarda y retorna DTO.|
|**CP-MC-05**|Conservar propio nombre y etapa actual|`id: 1L`, `nombre: "control ph"`, `etapaAControlar: HERVIDO` (Mismo registro)|`existsByNombre...AndEtapa...AndIdNot` $\rightarrow$ **FALSE**|Permite actualizar datos, persiste y retorna DTO.|

### 5. `bajaEtapaControl(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BC-01**|Baja de etapa de control inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la etapa de control con ID: 99". No llama a `save()`.|
|**CP-BC-02**|Baja de etapa asociada a un plan de monitoreo de una receta activa|`id: 1L` (Existe, referenciada por un `PlanMonitoreoEtapaEntity` cuya receta está ACTIVA)|`existsPlanMonitoreoActivoAsociado(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BC-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD, sin planes de monitoreo activos asociados)|`existsPlanMonitoreoActivoAsociado(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la etapa de control dada de baja.|