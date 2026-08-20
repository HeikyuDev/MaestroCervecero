### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 3 recetas activas|`findAll(pageable)` contiene elementos|Retorna `Page<RecetaResponseDTO>` con 3 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<RecetaResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Receta encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `RecetaResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Receta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la receta con ID: 99".|

### 3. `altaReceta(RecetaFormDTO recetaFormDTO)`

Se concentran las reglas de negocio de la versión inicial. En todos los casos de falla, el resto de los datos del DTO son válidos.

|**ID**|**Nombre del Caso**| **Datos de Entrada (Escenario)**                                                                       |**Condición Evaluada**|**Resultado Esperado**|
|---|---|--------------------------------------------------------------------------------------------------------|---|---|
|**CP-AR-01**|Volumen base en cero _(Límite)_| `volumenBase: 0.0`                                                                                     |`volumenBase == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`).|
|**CP-AR-02**|Volumen base nulo| `volumenBase: null`                                                                                    |`volumenBase == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-03**|Relación de empaste en cero _(Límite)_| `relacionDeEmpaste: 0.0`                                                                               |`relacionDeEmpaste == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-04**|OG objetivo igual a FG objetivo _(Límite)_| `ogObjetivo: 1.010`, `fgObjetivo: 1.010`                                                               |`ogObjetivo <= fgObjetivo` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-05**|OG objetivo menor a FG objetivo| `ogObjetivo: 1.005`, `fgObjetivo: 1.010`                                                               |`ogObjetivo <= fgObjetivo` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-06**|IBU objetivo negativo _(Límite inf.)_| `ibuObjetivo: -1`                                                                                      |`ibuObjetivo == null \| < 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-07**|IBU objetivo en cero _(Límite válido)_| `ibuObjetivo: 0.0`, resto del DTO válido                                                               |`ibuObjetivo < 0` $\rightarrow$ **FALSE**|Persiste la receta y retorna DTO con `ibuObjetivo = 0.0`.|
|**CP-AR-08**|Duración de maceración en cero _(Límite)_| `duracionMaceracion: 0`                                                                                |`duracionMaceracion == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-09**|Duración de hervido en cero _(Límite)_| `duracionHervido: 0`                                                                                   |`duracionHervido == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-10**|Duración de fermentación en cero _(Límite)_| `duracionFermentacion: 0`                                                                              |`duracionFermentacion == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-11**|Duración de maduración en cero _(Límite)_| `duracionMaduracion: 0`                                                                                |`duracionMaduracion == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-AR-12**|Receta sin maltas| `detallesMalta: List.of()`                                                                             |`detallesMalta == null \| isEmpty()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `maltaRepository`.|
|**CP-AR-13**|Cantidad de malta en cero _(Límite)_| 1 malta con `cantidad: 0.0`                                                                            |`cantidad == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `maltaRepository`.|
|**CP-AR-14**|Receta sin lúpulos| `detallesLupulo: List.of()`                                                                            |`detallesLupulo == null \| isEmpty()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `lupuloRepository`.|
|**CP-AR-15**|Cantidad de lúpulo en cero _(Límite)_| 1 lúpulo HERVOR con `cantidad: 0.0`                                                                    |`cantidad == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `lupuloRepository`.|
|**CP-AR-16**|Receta sin lúpulo de uso HERVOR| 2 lúpulos con `uso: WHIRPOOL` y `uso: DRY_HOP`                                                         |`lupulosHervor.isEmpty()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `lupuloRepository`.|
|**CP-AR-17**|Tiempo de hervor en cero _(Límite inf.)_| Lúpulo HERVOR con `tiempoDeHervor: 0.0`, `duracionHervido: 60`                                         |`tiempoDeHervor == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `lupuloRepository`.|
|**CP-AR-18**|Tiempo de hervor igual a duración del hervido _(Límite sup.)_| Lúpulo HERVOR con `tiempoDeHervor: 60.0`, `duracionHervido: 60`                                        |`tiempoDeHervor >= duracionHervido` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `lupuloRepository`.|
|**CP-AR-19**|Tiempo de hervor en límite superior válido| Lúpulo HERVOR con `tiempoDeHervor: 59.0`, `duracionHervido: 60`                                        |`tiempoDeHervor >= duracionHervido` $\rightarrow$ **FALSE**|Persiste la receta y retorna DTO.|
|**CP-AR-20**|Normalización de tiempo en lúpulo no HERVOR| 1 lúpulo HERVOR válido + 1 `AROMA` con `tiempoDeHervor: 30.0`                                          |`uso == HERVOR` $\rightarrow$ **FALSE** (ternario)|Persiste el detalle AROMA con `tiempoDeHervor = 0.0`, descartando el valor recibido.|
|**CP-AR-21**|Receta sin levaduras| `detallesLevadura: List.of()`                                                                          |`detallesLevadura == null \| isEmpty()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `levaduraRepository`.|
|**CP-AR-22**|Cantidad de levadura en cero _(Límite)_| 1 levadura con `cantidad: 0.0`                                                                         |`cantidad == null \| <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta `levaduraRepository`.|
|**CP-AR-23**|Nombre de receta duplicado| `nombre: "IPA Clásica"` (Existe en BD)                                                                 |`existsByNombreIgnoreCase...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AR-24**|Nombre duplicado Case-Insensitive| `nombre: "ipa clásica"` (Existe `"IPA Clásica"`)                                                       |`existsByNombreIgnoreCase...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AR-25**|Malta inexistente| `idMalta: 99L` (No existe en BD)                                                                       |`maltaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AR-26**|Lúpulo inexistente| `idLupulo: 99L` (No existe en BD)                                                                      |`lupuloRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AR-27**|Levadura inexistente| `idLevadura: 99L` (No existe en BD)                                                                    |`levaduraRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AR-28**|Etapa de control inexistente| `idEtapaControl: 99L` (No existe en BD)                                                                |`etapaControlRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AR-29**|Parámetro de control inexistente| `idParametroControl: 99L` (No existe en BD)                                                            |`parametroControlRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AR-30**|Valor mínimo mayor al valor máximo| `valorMinimo: 25.0`, `valorMaximo: 18.0`                                                               |`valorMinimo == null \| valorMaximo == null \| min > max` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AR-31**|Valor ideal fuera del rango planificado| `valorMinimo: 18.0`, `valorMaximo: 22.0`, `valorIdeal: 25.0`                                           |`valorIdeal < min \| valorIdeal > max` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AR-32**|Mínimo planificado inferior al límite teórico| `valorMinimo: -5.0`; parámetro con `valorMinimo: 0.0`                                                  |`valorMinimo < parametroControl.getValorMinimo()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AR-33**|Máximo planificado superior al límite teórico| `valorMaximo: 110.0`; parámetro con `valorMaximo: 100.0`                                               |`valorMaximo > parametroControl.getValorMaximo()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AR-34**|Rango planificado en los límites teóricos _(Límite válido)_| `valorMinimo: 0.0`, `valorMaximo: 100.0`, `valorIdeal: 0.0`; parámetro con límites `0.0` y `100.0`     |Todas las validaciones de rango $\rightarrow$ **FALSE**|Persiste el detalle de parámetro de control y retorna DTO.|
|**CP-AR-35**|Alta sin plan de monitoreo _(Opcional)_| DTO válido con `planesMonitoreo: null`                                                                 |`Optional.ofNullable(planesMonitoreo)` $\rightarrow$ **Vacío**|Persiste la receta sin planes de monitoreo. No consulta `etapaControlRepository`.|
|**CP-AR-36**|Alta exitosa _(Camino feliz)_| DTO válido: 2 maltas, 3 lúpulos (1 HERVOR), 1 levadura, 1 plan de monitoreo, `nombre: "IPA Nueva"` (Único) |Todas las validaciones $\rightarrow$ **FALSE**|Persiste la receta con 1 versión en `esUltimaVersion = true` y retorna DTO.|

### 4. `modificarReceta(Long id, RecetaFormDTO recetaFormDTO)`

Valida el versionado, los fallos por reglas de negocio y la existencia del registro. Las validaciones de datos generales, detalles y rangos son las mismas del alta (**CP-AR-01** a **CP-AR-22** y **CP-AR-30** a **CP-AR-34**), por lo que se agrupan por validador.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MR-01**|Falla por datos generales inválidos|`id: 1L`, `volumenBase: 0.0`|`validarDatosGenerales` $\rightarrow$ **Falla**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-MR-02**|Falla por detalles de malta|`id: 1L`, `detallesMalta: List.of()`|`validarDetallesMalta` $\rightarrow$ **Falla**|Lanza `ReglaNegocioException`. No busca por ID ni persiste.|
|**CP-MR-03**|Falla por detalles de lúpulo|`id: 1L`, lúpulos sin uso HERVOR|`validarDetallesLupulo` $\rightarrow$ **Falla**|Lanza `ReglaNegocioException`. No busca por ID ni persiste.|
|**CP-MR-04**|Falla por detalles de levadura|`id: 1L`, `detallesLevadura: List.of()`|`validarDetallesLevadura` $\rightarrow$ **Falla**|Lanza `ReglaNegocioException`. No busca por ID ni persiste.|
|**CP-MR-05**|Falla por rango de parámetro de control|`id: 1L`, `valorMinimo: 25.0`, `valorMaximo: 18.0`|`validarRangoDetalleParametroControl` $\rightarrow$ **Falla**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-MR-06**|Nombre en uso por otra receta|`id: 1L`, `nombre: "RECETA-EXISTENTE"`|`existsByNombre...AndRecetaIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No busca por ID ni persiste.|
|**CP-MR-07**|Conservar el nombre propio actual|`id: 1L`, `nombre: "IPA Clásica"` (Misma receta)|`existsByNombre...AndRecetaIdNot` $\rightarrow$ **FALSE**|Permite registrar la nueva versión, persiste y retorna DTO.|
|**CP-MR-08**|Receta no encontrada por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No persiste.|
|**CP-MR-09**|Insumo referenciado inexistente|`id: 1L` (Existe), `idMalta: 99L` (No existe)|`maltaRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()` y la versión anterior sigue activa.|
|**CP-MR-10**|Modificación exitosa _(Camino Feliz)_|`id: 1L` (Existe con 1 versión activa), DTO válido|Validaciones OK y receta encontrada|Desactiva la versión previa (`esUltimaVersion = false`), agrega la nueva en `true`, guarda y retorna DTO.|
|**CP-MR-11**|Versionado sobre historial existente|`id: 1L` con 3 versiones (2 históricas + 1 activa), DTO válido|`filter(isEsUltimaVersion)` $\rightarrow$ **1 coincidencia**|La receta queda con 4 versiones y exactamente 1 con `esUltimaVersion = true`.|

### 5. `bajaReceta(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BR-01**|Baja de receta inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `delete()`.|
|**CP-BR-02**|Baja exitosa _(Soft Delete)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Invoca `delete(entity)` y retorna DTO de la receta dada de baja.|