### 1. Pruebas para `altaMacerador(MaceradorFormDTO)`

Aquí se concentran las principales reglas de validación del negocio



|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-01**|Capacidad total nula|`capacidadTotal: null`, `capacidadUtil: 80.0`, `eficiencia: 75.0`, `id: "MAC-01"`|`capacidadTotal == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad total y la capacidad util son obligatorias."). No consulta la BD.|
|**CP-02**|Capacidad útil mayor a total|`capacidadTotal: 100.0`, `capacidadUtil: 120.0`, `eficiencia: 75.0`, `id: "MAC-01"`|`capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-03**|Capacidad útil igual a total _(Límite)_|`capacidadTotal: 100.0`, `capacidadUtil: 100.0`, `eficiencia: 75.0`, `id: "MAC-01"`|`capacidadUtil >= capacidadTotal` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-04**|Eficiencia de maceración nula|`capacidadTotal: 100.0`, `capacidadUtil: 80.0`, `eficiencia: null`, `id: "MAC-01"`|`eficienciaMaceracion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La eficiencia de maceración es obligatoria."). No consulta la BD.|
|**CP-05**|Eficiencia bajo el mínimo _(Límite inf.)_|`capacidadTotal: 100.0`, `capacidadUtil: 80.0`, `eficiencia: 39.9`, `id: "MAC-01"`|`eficiencia < 40` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-06**|Eficiencia sobre el máximo _(Límite sup.)_|`capacidadTotal: 100.0`, `capacidadUtil: 80.0`, `eficiencia: 100.1`, `id: "MAC-01"`|`eficiencia > 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta la BD.|
|**CP-07**|Identificador interno duplicado|`capTotal: 100.0`, `capUtil: 80.0`, `eficiencia: 75.0`, `id: "MAC-01"` (Existe en BD)|`existsByIdentificador...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-08**|Identificador duplicado Case-Insensitive|`capTotal: 100.0`, `capUtil: 80.0`, `eficiencia: 75.0`, `id: "mac-01"` (Existe `"MAC-01"`)|`existsByIdentificador...` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-09**|Alta exitosa _(Camino feliz)_|`capTotal: 100.0`, `capUtil: 80.0`, `eficiencia: 75.0`, `id: "MAC-02"` (Único)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `estadoOperativo = DISPONIBLE` y `estado = ACTIVO`, y retorna DTO.|
|**CP-10**|Eficiencia en límite inferior válido|`capTotal: 100.0`, `capUtil: 80.0`, `eficiencia: 40.0`, `id: "MAC-03"`|`eficiencia < 40 \| eficiencia > 100` $ $\rightarr$ **FALSE**|Persiste la entidad y retorna DTO con `eficiencia = 40.0`.|
|**CP-11**|Eficiencia en límite superior válido|`capTotal: 100.0`, `capUtil: 80.0`, `eficiencia: 100.0`, `id: "MAC-04"`|`eficiencia < 40 \| eficiencia > 100` $ $\rightarr$ **FALSE**|Persiste la entidad y retorna DTO con `eficiencia = 100.0`.|
|**CP-12**|Usos máximos antes de mantenimiento nulo|`usosMaximosAntesMantenimiento: null` (resto válido)|`usosMaximosAntesMantenimiento == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los usos máximos antes de mantenimiento son obligatorios."). No consulta la BD.|
|**CP-13**|Usos máximos antes de mantenimiento igual a cero _(Límite)_|`usosMaximosAntesMantenimiento: 0` (resto válido)|`usosMaximosAntesMantenimiento <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los usos máximos antes de mantenimiento deben ser mayores a 0."). No consulta la BD.|

### 2. Pruebas para `modificarMacerador(Long id, MaceradorFormDTO)`

Valida la modificación, fallos por reglas de negocio y existencia del registro.

| **ID**       | **Nombre del Caso**                               | **Datos de Entrada (Escenario)**           | **Condición Evaluada**                                | **Resultado Esperado**                             |
| ------------ | ------------------------------------------------- | ------------------------------------------ | ----------------------------------------------------- | -------------------------------------------------- |
| **CP-MM-01** | Falla por capacidad útil                          | `id: 1L`, `capUtil >= capTotal`            | `validarCapacidadUtil` $\rightarrow$ **Falla**        | Lanza `ReglaNegocioException`                      |
| **CP-MM-02** | Falla por eficiencia inválida                     | `id: 1L`, `eficiencia: 30.0`               | `validarEficienciaMaceracion` $\rightarrow$ **Falla** | Lanza `ReglaNegocioException`                      |
| **CP-MM-03** | Falla por identificador en uso por otro macerador | `id: 1L`, `identificador: "MAC-EXISTENTE"` | `validarIdentificadorInterno` $\rightarrow$ **Falla** | Lanza `RecursoDuplicadoException`                  |
| **CP-MM-04** | Macerador no encontrado por ID                    | `id: 99L` (no existe en BD), DTO válido    | `findById(id)` $\rightarrow$ **Optional.empty()**     | Lanza `RecursoNoEncontradoException`               |
| **CP-MM-05** | Modificación exitosa _(Camino Feliz)_             | `id: 1L` (existe), DTO válido              | Validaciones OK y macerador encontrado                | Actualiza campos, guarda y retorna DTO actualizado |
| **CP-MM-06** | Falla por usos máximos antes de mantenimiento inválidos | `id: 1L`, `usosMaximosAntesMantenimiento: 0` | `usosMaximosAntesMantenimiento <= 0` $\rightarrow$ **Falla** | Lanza `ReglaNegocioException` |
| **CP-MM-07** | Asociado a lote pendiente | `id: 1L` (existe), DTO válido | `existsLotePendienteAsociado(1L)` $\rightarrow$ **TRUE** | El sistema verifica que el macerador seleccionado no se encuentre asociado a un lote de producción en estado "Pendiente". Si se encuentra asociado, informa que no puede ser modificado y se termina el caso de uso. Lanza `ReglaNegocioException`. No persiste. |
| **CP-MM-08** | Se encuentra en uso | `id: 1L` (existe, sin lote pendiente), `estadoOperativo: EN_USO`, DTO válido | `maceradorEntity.getEstadoOperativo() == EN_USO` $\rightarrow$ **TRUE** | El sistema verifica que el macerador seleccionado no se encuentre en estado operativo "En Uso". Si se encuentra en uso, informa que el equipamiento se encuentra en uso y no puede ser modificado, finalizando el caso de uso. Lanza `ReglaNegocioException`. No persiste. |
### 3. Pruebas para `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Escenario**|**Condición**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Macerador encontrado|`id: 1L` (existe en BD)|`findById(1L)` presente|Retorna `MaceradorResponseDTO`|
|**CP-BI-02**|Macerador inexistente|`id: 99L` (no existe en BD)|`findById(99L)` vacío|Lanza `RecursoNoEncontradoException`|

### 4. Pruebas para `bajaMacerador(Long id)`

|**ID**|**Nombre del Caso**|**Escenario**|**Condición**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BM-01**|Baja de macerador inexistente|`id: 99L` (no existe en BD)|`findById(99L)` vacío|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BM-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (existe en BD, sin lote pendiente y no está en uso)|`findById(1L)` presente|Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO|
|**CP-BM-03**|Asociado a lote pendiente|`id: 1L` (existe en BD)|`existsLotePendienteAsociado(1L)` $\rightarrow$ **TRUE**|El sistema verifica que el macerador seleccionado no se encuentre asociado a un lote de producción en estado "Pendiente". Si se encuentra asociado, informa que no puede ser dado de baja y se termina el caso de uso. Lanza `ReglaNegocioException`. No llama a `save()`.|
|**CP-BM-04**|Se encuentra en uso|`id: 1L` (existe en BD, sin lote pendiente), `estadoOperativo: EN_USO`|`maceradorEntity.getEstadoOperativo() == EN_USO` $\rightarrow$ **TRUE**|El sistema verifica que el macerador seleccionado no se encuentre en estado operativo "En Uso". Si se encuentra en uso, informa que el equipamiento se encuentra en uso y no puede ser dado de baja, finalizando el caso de uso. Lanza `ReglaNegocioException`. No llama a `save()`.|

### 5. Pruebas para `filtrarMaceradores(String identificadorInterno, EstadoOperativo estadoOperativo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-FMa-01**|Filtra por identificador interno y estado operativo informados|`identificadorInterno: "MAC"`, `estadoOperativo: DISPONIBLE`, BD con 1 macerador activo que cumple ambos criterios|Retorna `Page<MaceradorResponseDTO>` con 1 elemento mapeado|
|**CP-FMa-02**|Identificador interno y estado operativo nulos no restringen la búsqueda|`identificadorInterno: null`, `estadoOperativo: null`|Retorna `Page<MaceradorResponseDTO>` con todos los maceradores activos (equivalente a no filtrar)|
|**CP-FMa-03**|Consulta sin coincidencias|`identificadorInterno: "Inexistente"`, `estadoOperativo: null`|Retorna `Page<MaceradorResponseDTO>` vacía (`getContent().isEmpty() == true`)|