### 1. `filtrarOllasHervor(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FOH-01**|Filtra por identificador interno y estado operativo informados|`identificadorInterno: "OLLA"`, `estadoOperativo: DISPONIBLE`, `pageable: PageRequest.of(0, 10)`, BD con 2 ollas activas que cumplen ambos criterios|`filtrarOllasHervor("OLLA", DISPONIBLE, pageable)` contiene elementos|Retorna `Page<OllaHervorResponseDTO>` con 2 elementos mapeados.|
|**CP-FOH-02**|Identificador interno y estado operativo nulos no restringen la búsqueda|`identificadorInterno: null`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<OllaHervorResponseDTO>` con todas las ollas de hervor activas (equivalente a no filtrar).|
|**CP-FOH-03**|Consulta sin coincidencias|`identificadorInterno: "Inexistente"`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarOllasHervor("Inexistente", null, pageable)` está vacío|Retorna `Page<OllaHervorResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Olla de hervor encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `OllaHervorResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Olla de hervor inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "La olla de hervor no existe".|

### 3. `altaOllaHervor(OllaHervorFormDTO formDTO)`

| **ID**       | **Nombre del Caso**                        | **Datos de Entrada (Escenario)**                                                                     | **Condición Evaluada**                                           | **Resultado Esperado**                                                     |
| ------------ | ------------------------------------------ | ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------- | -------------------------------------------------------------------------- |
| **CP-AO-01** | Capacidad útil mayor a total               | `capTotal: 100.0`, `capUtil: 120.0` (resto válido)                                                   | `capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**         | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AO-02** | Capacidad útil igual a total _(Límite)_    | `capTotal: 100.0`, `capUtil: 100.0` (resto válido)                                                   | `capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**         | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AO-03** | Evaporación negativa _(Límite inf.)_       | `evaporacion: -0.1` (resto válido)                                                                   | `porcentajeEvaporacion < 0` $\rightarrow$ **TRUE**               | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AO-04** | Evaporación mayor a 100 _(Límite sup.)_    | `evaporacion: 100.1` (resto válido)                                                                  | `porcentajeEvaporacion > 100` $\rightarrow$ **TRUE**             | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AO-05** | Pérdida por trub negativa _(Límite inf.)_  | `trub: -0.1` (resto válido)                                                                          | `perdidaPorTrub < 0` $\rightarrow$ **TRUE**                      | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AO-06** | Identificador interno duplicado            | `idInterno: "OLLA-01"` (Existe en BD)                                                                | `existsByIdentificador...` $\rightarrow$ **TRUE**                | Lanza `RecursoDuplicadoException`. No ejecuta `save()`.                    |
| **CP-AO-07** | Identificador duplicado Case-Insensitive   | `idInterno: "olla-01"` (Existe `"OLLA-01"`)                                                          | `existsByIdentificador...` $\rightarrow$ **TRUE**                | Lanza `RecursoDuplicadoException`. No ejecuta `save()`.                    |
| **CP-AO-08** | Alta exitosa _(Camino feliz)_              | `capTotal: 100.0`, `capUtil: 80.0`, `evaporacion: 10.0`, `trub: 3.0`, `idInterno: "OLLA-02"` (Único) | Todas las validaciones $\rightarrow$ **FALSE**                   | Persiste la entidad con `estadoOperativo = DISPONIBLE` y `estado = ACTIVO`, y retorna DTO.      |
| **CP-AO-09** | Evaporación en límite inferior válido      | `evaporacion: 0.0` (resto válido)                                                                    | `evaporacion < 0 \| evaporacion > 100` $ $\rightarrow$ **FALSE** | Persiste con éxito y retorna DTO con `porcentajeEvaporacion = 0.0`.        |
| **CP-AO-10** | Evaporación en límite superior válido      | `evaporacion: 100.0` (resto válido)                                                                  | `evaporacion < 0 \| evaporacion > 100` $ $\rightarrow$ **FALSE** | Persiste con éxito y retorna DTO con `porcentajeEvaporacion = 100.0`.      |
| **CP-AO-11** | Pérdida por trub en límite inferior válido | `trub: 0.0` (resto válido)                                                                           | `perdidaPorTrub < 0` $\rightarrow$ **FALSE**                     | Persiste con éxito y retorna DTO con `perdidaPorTrub = 0.0`.               |

### 4. `modificarOllaHervor(Long id, OllaHervorFormDTO formDTO)`

| **ID**       | **Nombre del Caso**                        | **Datos de Entrada (Escenario)**                         | **Condición Evaluada**                                          | **Resultado Esperado**                                                                 |
| ------------ | ------------------------------------------ | -------------------------------------------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| **CP-MO-01** | Falla por capacidad útil inválida          | `id: 1L`, `capTotal: 100.0`, `capUtil: 110.0`            | `capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**        | Lanza `ReglaNegocioException`. No consulta la BD.                                      |
| **CP-MO-02** | Falla por evaporación inválida             | `id: 1L`, `evaporacion: 105.0`                           | `evaporacion < 0 \| evaporacion > 100` $ $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD.                                      |
| **CP-MO-03** | Falla por trub negativo                    | `id: 1L`, `trub: -1.0`                                   | `perdidaPorTrub < 0` $\rightarrow$ **TRUE**                     | Lanza `ReglaNegocioException`. No consulta la BD.                                      |
| **CP-MO-04** | Identificador en uso por otro equipamiento | `id: 1L`, `idInterno: "OLLA-EXISTENTE"`                  | `existsByIdentificador...AndIdNot` $\rightarrow$ **TRUE**       | Lanza `RecursoDuplicadoException`. No busca por ID ni persiste.                        |
| **CP-MO-05** | Olla de hervor no encontrada por ID        | `id: 99L` (No existe en BD), DTO válido                  | `findById(99L)` $\rightarrow$ **Optional.empty()**              | Lanza `RecursoNoEncontradoException`. No persiste.                                     |
| **CP-MO-06** | Modificación exitosa _(Camino Feliz)_      | `id: 1L` (Existe), `idInterno: "OLLA-NUEVA"`, DTO válido | Identificador libre y olla encontrada                           | Actualiza campos (`capTotal`, `capUtil`, `evaporacion`, `trub`), guarda y retorna DTO. |
| **CP-MO-07** | Conservar identificador propio actual      | `id: 1L`, `idInterno: "olla-01"` (Mismo equipamiento)    | `existsByIdentificador...AndIdNot` $\rightarrow$ **FALSE**      | Permite actualizar datos, persiste y retorna DTO.                                      |

### 5. `bajaOllaHervor(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BO-01**|Baja de olla inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BO-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la olla dada de baja.|