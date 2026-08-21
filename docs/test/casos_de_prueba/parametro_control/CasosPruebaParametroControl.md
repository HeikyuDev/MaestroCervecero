### 1. `buscarTodos(Pageable pageable)`

| **ID**       | **Nombre del Caso**               | **Datos de Entrada (Escenario)**                               | **Condición Evaluada**                 | **Resultado Esperado**                                                                |
| ------------ | --------------------------------- | -------------------------------------------------------------- | -------------------------------------- | ------------------------------------------------------------------------------------- |
| **CP-BT-01** | Consulta con registros existentes | `pageable: PageRequest.of(0, 10)`, BD con 2 parámetros activos | `findAll(pageable)` contiene elementos | Retorna `Page<ParametroControlResponseDTO>` con 2 elementos mapeados.                 |
| **CP-BT-02** | Consulta sin registros existentes | `pageable: PageRequest.of(0, 10)`, BD vacía                    | `findAll(pageable)` está vacío         | Retorna `Page<ParametroControlResponseDTO>` vacía (`getContent().isEmpty() == true`). |

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Parámetro de control encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `ParametroControlResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Parámetro de control inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el parámetro de control con ID: 99".|

### 3. `altaParametroControl(ParametroControlFormDTO parametroControlFormDTO)`

| **ID**       | **Nombre del Caso**                                  | **Datos de Entrada (Escenario)**                                            | **Condición Evaluada**                              | **Resultado Esperado**                                                                                |
| ------------ | ---------------------------------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| **CP-AP-01** | Valor mínimo nulo                                    | `min: null`, `max: 10.0`, `nombre: "Temperatura"`                           | `valorMinimo == null` $\rightarrow$ **TRUE**        | Lanza `ReglaNegocioException` ("El valor mínimo debe ser mayor a 0"). No consulta BD.                 |
| **CP-AP-02** | Valor mínimo igual a cero _(Límite)_                 | `min: 0.0`, `max: 10.0`, `nombre: "Temperatura"`                            | `valorMinimo <= 0` $\rightarrow$ **TRUE**           | Lanza `ReglaNegocioException` ("El valor mínimo debe ser mayor a 0"). No consulta BD.                 |
| **CP-AP-03** | Valor mínimo negativo _(Límite inf.)_                | `min: -0.1`, `max: 10.0`, `nombre: "Temperatura"`                           | `valorMinimo <= 0` $\rightarrow$ **TRUE**           | Lanza `ReglaNegocioException` ("El valor mínimo debe ser mayor a 0"). No consulta BD.                 |
| **CP-AP-04** | Valor máximo nulo                                    | `min: 5.0`, `max: null`, `nombre: "Temperatura"`                            | `valorMaximo == null` $\rightarrow$ **TRUE**        | Lanza `ReglaNegocioException` ("El valor mínimo no puede ser mayor al valor máximo"). No consulta BD. |
| **CP-AP-05** | Valor mínimo mayor al valor máximo                   | `min: 15.0`, `max: 10.0`, `nombre: "Temperatura"`                           | `valorMinimo > valorMaximo` $\rightarrow$ **TRUE**  | Lanza `ReglaNegocioException` ("El valor mínimo no puede ser mayor al valor máximo"). No consulta BD. |
| **CP-AP-06** | Nombre duplicado                                     | `min: 5.0`, `max: 10.0`, `nombre: "Densidad"` (Existe en BD)                | `existsByNombreIgnoreCase` $\rightarrow$ **TRUE**   | Lanza `RecursoDuplicadoException`. No ejecuta `save()`.                                               |
| **CP-AP-07** | Nombre duplicado Case-Insensitive                    | `min: 5.0`, `max: 10.0`, `nombre: "densidad inicial"` (Existe `"densidad"`) | `existsByNombreIgnoreCase` $\rightarrow$ **TRUE**   | Lanza `RecursoDuplicadoException`. No ejecuta `save()`.                                               |
| **CP-AP-08** | Alta exitosa _(Camino feliz)_                        | `min: 65.0`, `max: 68.0`, `nombre: "Temperatura"` (Único)                   | Todas las validaciones $\rightarrow$ **FALSE**      | Persiste la entidad con `estado = ACTIVO` y retorna `ParametroControlResponseDTO`.                                          |
| **CP-AP-09** | Valor mínimo igual al valor máximo _(Límite válido)_ | `min: 5.2`, `max: 5.2`, `nombre: "pH"`                                      | `valorMinimo > valorMaximo` $\rightarrow$ **FALSE** | Persiste con éxito y retorna DTO con `valorMinimo = 5.2` y `valorMaximo = 5.2`.                       |
| **CP-AP-10** | Valor mínimo en límite positivo inferior válido      | `min: 0.1`, `max: 1.0`, `nombre: "Oxígeno Disuelto"`                        | `valorMinimo <= 0` $\rightarrow$ **FALSE**          | Persiste con éxito y retorna DTO con `valorMinimo = 0.1`.                                             |

### 4. `modificarParametroControl(Long id, ParametroControlFormDTO parametroControlFormDTO)`

| **ID**       | **Nombre del Caso**                                | **Datos de Entrada (Escenario)**                                          | **Condición Evaluada**                                     | **Resultado Esperado**                                                                                             |
| ------------ | -------------------------------------------------- | ------------------------------------------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------ |
| **CP-MP-01** | Falla por valor mínimo inválido ($\le 0$ o `null`) | `id: 1L`, `min: 0.0`, `max: 10.0`                                         | `valorMinimo <= 0` $\rightarrow$ **TRUE**                  | Lanza `ReglaNegocioException`. No consulta la BD.                                                                  |
| **CP-MP-02** | Falla por mínimo mayor a máximo (o max `null`)     | `id: 1L`, `min: 20.0`, `max: 10.0`                                        | `valorMinimo > valorMaximo` $\rightarrow$ **TRUE**         | Lanza `ReglaNegocioException`. No consulta la BD.                                                                  |
| **CP-MP-03** | Nombre en uso por otro parámetro de control        | `id: 1L`, `nombre: "Densidad"`, `min: 1.0`, `max: 2.0` (En uso por ID 2L) | `existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE**  | Lanza `RecursoDuplicadoException`. No busca por ID ni persiste.                                                    |
| **CP-MP-04** | Parámetro de control no encontrado por ID          | `id: 99L` (No existe en BD), DTO válido                                   | `findById(99L)` $\rightarrow$ **Optional.empty()**         | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el parámetro de control con ID: 99". No persiste. |
| **CP-MP-05** | Modificación exitosa _(Camino Feliz)_              | `id: 1L` (Existe), `nombre: "pH"`, `min: 5.0`, `max: 5.5`, DTO válido     | Nombre libre y parámetro encontrado                        | Actualiza campos (`nombre`, `descripcion`, `valorMinimo`, `valorMaximo`), guarda y retorna DTO.                    |
| **CP-MP-06** | Conservar propio nombre actual                     | `id: 1L`, `nombre: "ph"`, `min: 5.0`, `max: 5.5` (Mismo registro)         | `existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE** | Permite actualizar datos, persiste y retorna DTO.                                                                  |

### 5. `bajaParametroControl(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BP-01**|Baja de parámetro inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el parámetro de control con ID: 99". No llama a `save()`.|
|**CP-BP-02**|Baja de parámetro asociado a un plan de monitoreo de una receta activa|`id: 1L` (Existe, referenciado por un `DetalleParametroControlEntity` cuyo plan pertenece a una receta ACTIVA)|`existsPlanMonitoreoActivoAsociado(1L)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BP-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD, sin planes de monitoreo activos asociados)|`existsPlanMonitoreoActivoAsociado(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del parámetro dado de baja.|
