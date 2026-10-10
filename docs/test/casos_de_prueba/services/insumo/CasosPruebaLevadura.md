### 1. `filtrarLevaduras(String nombre, TipoLevadura tipo, Pageable pageable)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-FLv-01** | Filtra por nombre y tipo informados | `nombre: "SafAle"`, `tipo: ALE`, `pageable: PageRequest.of(0, 10)`, BD con 2 levaduras activas que cumplen ambos criterios | `filtrarLevaduras("SafAle", ALE, pageable)` contiene elementos | Retorna `Page<LevaduraResponseDTO>` con 2 elementos mapeados. |
| **CP-FLv-02** | Nombre y tipo nulos no restringen la búsqueda | `nombre: null`, `tipo: null`, `pageable: PageRequest.of(0, 10)` | El service propaga ambos parámetros nulos tal cual al repositorio | Retorna `Page<LevaduraResponseDTO>` con todas las levaduras activas (equivalente a no filtrar). |
| **CP-FLv-03** | Consulta sin coincidencias | `nombre: "Inexistente"`, `tipo: null`, `pageable: PageRequest.of(0, 10)` | `filtrarLevaduras("Inexistente", null, pageable)` está vacío | Retorna `Page<LevaduraResponseDTO>` vacía (`getContent().isEmpty() == true`). |

---

### 2. `buscarPorId(Long id)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BI-01** | Levadura encontrada | `id: 1L` (Existe en BD) | `findById(1L)` $\rightarrow$ **Presente** | Retorna `LevaduraResponseDTO` con los datos de la entidad. |
| **CP-BI-02** | Levadura inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "La levadura no existe". |

---

### 3. `altaLevadura(LevaduraFormDTO formDTO)`

La cantidad de células viables por gramo debe ser un número real: NaN o infinito se rechazan con "La cantidad de células viables por gramo debe ser un número mayor a 0". Además debe estar entre 6.000.000.000 y 20.000.000.000 (mensaje, que también se usa para nulo, cero y negativos: "La cantidad de células viables por gramo debe ser mayor o igual a 6.000.000.000 y menor o igual a 20.000.000.000"). El service trabaja en células (no en millones). El nombre no puede superar 100 caracteres (mensaje: "El nombre no puede superar 100 caracteres").

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-AL-01** | Células por gramo nula | `celulas: null`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo == null` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-02** | Células por gramo igual a cero | `celulas: 0.0`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo < 6.000.000.000` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-03** | Células por gramo negativa | `celulas: -1.0`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo < 6.000.000.000` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-04** | Nombre duplicado | `celulas: 1.0E10`, `nombre: "SafAle US-05"` (Existe en BD) | `existsByNombreIgnoreCase` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No ejecuta `save()`. |
| **CP-AL-05** | Nombre duplicado Case-Insensitive | `celulas: 1.0E10`, `nombre: "safale us-05"` (Existe `"SafAle US-05"`) | `existsByNombreIgnoreCase` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No ejecuta `save()`. |
| **CP-AL-06** | Alta exitosa y asignación de `GRAMO` *(Camino feliz)* | `celulas: 1.0E10`, `nombre: "SafLager W-34/70"` (Único), `tipo: LAGER` | Todas las validaciones $\rightarrow$ **FALSE** | Persiste la entidad asignando fijamente `unidadDeMedida = GRAMO` y `estado = ACTIVO`, y retorna DTO. |
| **CP-AL-07** | Células por gramo en límite inferior válido | `celulas: 6.000.000.000`, `nombre: "Belle Saison"` | `cantidadCelulasPorGramo < 6.000.000.000 \| cantidadCelulasPorGramo > 20.000.000.000` $\rightarrow$ **FALSE** | Persiste con éxito y retorna DTO con `cantidadCelulasPorGramo = 6.000.000.000`. |
| **CP-AL-08** | Células por gramo menor al mínimo *(Límite inf.)* | `celulas: 5.999.999.999`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo < 6.000.000.000` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-09** | Células por gramo mayor al máximo *(Límite sup.)* | `celulas: 20.000.000.001`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo > 20.000.000.000` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-10** | Células por gramo en límite superior válido | `celulas: 20.000.000.000`, `nombre: "Kveik Voss"` | `cantidadCelulasPorGramo < 6.000.000.000 \| cantidadCelulasPorGramo > 20.000.000.000` $\rightarrow$ **FALSE** | Persiste con éxito y retorna DTO con `cantidadCelulasPorGramo = 20.000.000.000`. |
|**CP-AL-11**|La cantidad de células por gramo es NaN|`cantidadCelulasPorGramo: Double.NaN` (resto válido)|La cantidad de células por gramo es NaN $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad de células viables por gramo debe ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-12**|La cantidad de células por gramo es infinita positiva|`cantidadCelulasPorGramo: Double.POSITIVE_INFINITY` (resto válido)|La cantidad de células por gramo es infinita positiva $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad de células viables por gramo debe ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-13**|La cantidad de células por gramo es infinita negativa|`cantidadCelulasPorGramo: Double.NEGATIVE_INFINITY` (resto válido)|La cantidad de células por gramo es infinita negativa $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad de células viables por gramo debe ser un número mayor a 0"). No consulta la BD.|
|**CP-AL-14**|La cantidad de células por gramo es exactamente 6.000.000.000 (valor límite válido)|`cantidadCelulasPorGramo: 6000000000.0` (resto válido)|La cantidad de células por gramo es exactamente 6.000.000.000 (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-AL-15**|La cantidad de células por gramo es exactamente 20.000.000.000 (valor límite válido)|`cantidadCelulasPorGramo: 20000000000.0` (resto válido)|La cantidad de células por gramo es exactamente 20.000.000.000 (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|
|**CP-AL-16**|El nombre tiene 101 caracteres|`nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-AL-17**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|

---

### 4. `modificarLevadura(Long id, LevaduraFormDTO formDTO)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ML-01** | Falla por células por gramo inválida | `id: 1L`, `celulas: 0.0` (o `null`, `5.999.999.999`, `20.000.000.001`) | `cantidadCelulasPorGramo < 6.000.000.000 \| cantidadCelulasPorGramo > 20.000.000.000` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD. |
| **CP-ML-02** | Levadura no encontrada por ID | `id: 99L` (No existe en BD), DTO válido | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "La levadura no existe". No persiste. |
| **CP-ML-03** | Nombre en uso por otra levadura | `id: 1L`, `nombre: "SafAle US-05"` (En uso por ID 2L) | `existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No persiste. |
| **CP-ML-04** | Modificación exitosa *(Camino Feliz)* | `id: 1L` (Existe), `nombre: "SafAle S-04 Modificada"`, DTO válido | Nombre libre y levadura encontrada | Actualiza campos (`nombre`, `tipo`, `cantidadCelulasPorGramo`), mantiene `GRAMO`, guarda y retorna DTO. |
| **CP-ML-05** | Conservar nombre propio actual | `id: 1L`, `nombre: "safale s-04"` (Misma levadura) | `existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE** | Permite actualizar datos, persiste y retorna DTO. |
|**CP-ML-06**|La cantidad de células por gramo es NaN|`id: 1L` (existe), `cantidadCelulasPorGramo: Double.NaN` (resto válido)|La cantidad de células por gramo es NaN $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad de células viables por gramo debe ser un número mayor a 0"). No consulta la BD.|
|**CP-ML-07**|El nombre tiene 101 caracteres|`id: 1L` (existe), `nombre: "A" x 101 caracteres` (resto válido)|El nombre tiene 101 caracteres $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres"). No consulta la BD.|
|**CP-ML-08**|El nombre tiene exactamente 100 caracteres (valor límite válido)|`id: 1L` (existe), `nombre: "A" x 100 caracteres` (resto válido)|El nombre tiene exactamente 100 caracteres (valor límite válido) $\rightarrow$ **FALSE** (se acepta)|Persiste con éxito y no lanza excepción.|

---

### 5. `bajaLevadura(Long id)`

No se puede dar de baja una levadura que forma parte de al menos una receta activa (`versión con esUltimaVersion = true` perteneciente a una receta con `estado = ACTIVO`), ni una que tenga stock (al menos un lote de insumo con `cantidadActual > 0`).

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BL-01** | Baja de levadura inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la levadura con ID: 99". No llama a `save()`. |
| **CP-BL-02** | Levadura forma parte de una receta activa | `id: 1L` (Existe), `versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)` $\rightarrow$ **true** | Existe al menos una receta activa que la usa | Lanza `ReglaNegocioException` ("No se puede dar de baja la levadura porque forma parte de al menos una receta activa"). No consulta stock (`verifyNoInteractions(loteInsumoRepository)`) ni ejecuta `save()`. |
| **CP-BL-03** | Levadura tiene stock | `id: 1L` (Existe), `versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)` $\rightarrow$ **false**, `loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)` $\rightarrow$ **true** | No está en ninguna receta activa, pero tiene al menos un lote con cantidad actual mayor a cero | Lanza `ReglaNegocioException` ("No se puede dar de baja la levadura porque tiene stock"). No ejecuta `save()`. |
| **CP-BL-04** | Baja exitosa *(Baja lógica vía Estado)* | `id: 1L` (Existe en BD), sin receta activa ni stock asociados | `findById(1L)` $\rightarrow$ **Presente**, ambas validaciones $\rightarrow$ **FALSE** | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO de la levadura dada de baja. |