### 1. `buscarTodos(Pageable pageable)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BT-01** | Consulta con registros existentes | `pageable: PageRequest.of(0, 10)`, BD con 2 levaduras activas | `findAll(pageable)` contiene elementos | Retorna `Page<LevaduraResponseDTO>` con 2 elementos mapeados. |
| **CP-BT-02** | Consulta sin registros existentes | `pageable: PageRequest.of(0, 10)`, BD vacía | `findAll(pageable)` está vacío | Retorna `Page<LevaduraResponseDTO>` vacía (`getContent().isEmpty() == true`). |

---

### 2. `buscarPorId(Long id)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BI-01** | Levadura encontrada | `id: 1L` (Existe en BD) | `findById(1L)` $\rightarrow$ **Presente** | Retorna `LevaduraResponseDTO` con los datos de la entidad. |
| **CP-BI-02** | Levadura inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "La levadura no existe". |

---

### 3. `altaLevadura(LevaduraFormDTO formDTO)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-AL-01** | Células por gramo nula | `celulas: null`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo == null` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-02** | Células por gramo igual a cero *(Límite)* | `celulas: 0.0`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo <= 0` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-03** | Células por gramo negativa *(Límite inf.)* | `celulas: -1.0`, `nombre: "SafAle S-04"` | `cantidadCelulasPorGramo <= 0` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD (`verifyNoInteractions`). |
| **CP-AL-04** | Nombre duplicado | `celulas: 1.0E10`, `nombre: "SafAle US-05"` (Existe en BD) | `existsByNombreIgnoreCase` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No ejecuta `save()`. |
| **CP-AL-05** | Nombre duplicado Case-Insensitive | `celulas: 1.0E10`, `nombre: "safale us-05"` (Existe `"SafAle US-05"`) | `existsByNombreIgnoreCase` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No ejecuta `save()`. |
| **CP-AL-06** | Alta exitosa y asignación de `GRAMO` *(Camino feliz)* | `celulas: 1.0E10`, `nombre: "SafLager W-34/70"` (Único), `tipo: LAGER` | Todas las validaciones $\rightarrow$ **FALSE** | Persiste la entidad asignando fijamente `unidadDeMedida = GRAMO` y retorna DTO. |
| **CP-AL-07** | Células por gramo en límite inferior válido | `celulas: 0.1` (Mayor a cero), `nombre: "Belle Saison"` | `cantidadCelulasPorGramo <= 0` $\rightarrow$ **FALSE** | Persiste con éxito y retorna DTO con `cantidadCelulasPorGramo = 0.1`. |

---

### 4. `modificarLevadura(Long id, LevaduraFormDTO formDTO)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ML-01** | Falla por células por gramo inválida | `id: 1L`, `celulas: 0.0` (o `null`) | `cantidadCelulasPorGramo <= 0` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException`. No consulta la BD. |
| **CP-ML-02** | Levadura no encontrada por ID | `id: 99L` (No existe en BD), DTO válido | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "La levadura no existe". No persiste. |
| **CP-ML-03** | Nombre en uso por otra levadura | `id: 1L`, `nombre: "SafAle US-05"` (En uso por ID 2L) | `existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No persiste. |
| **CP-ML-04** | Modificación exitosa *(Camino Feliz)* | `id: 1L` (Existe), `nombre: "SafAle S-04 Modificada"`, DTO válido | Nombre libre y levadura encontrada | Actualiza campos (`nombre`, `tipo`, `cantidadCelulasPorGramo`), mantiene `GRAMO`, guarda y retorna DTO. |
| **CP-ML-05** | Conservar nombre propio actual | `id: 1L`, `nombre: "safale s-04"` (Misma levadura) | `existsByNombreIgnoreCaseAndIdNot` $\rightarrow$ **FALSE** | Permite actualizar datos, persiste y retorna DTO. |

---

### 5. `bajaLevadura(Long id)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BL-01** | Baja de levadura inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la levadura con ID: 99". No llama a `delete()`. |
| **CP-BL-02** | Baja exitosa *(Soft Delete)* | `id: 1L` (Existe en BD) | `findById(1L)` $\rightarrow$ **Presente** | Invoca `delete(entity)` y retorna DTO de la levadura dada de baja. |