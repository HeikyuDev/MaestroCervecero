## Casos de prueba de `GuardadoUnico`

**Tipo de prueba:** unitaria (clase `GuardadoUnicoTest`), con el repositorio simulado (mock) y sin base de datos. Verifica que el guardado confirme los datos en la base dentro del método del Service y que la violación de un índice único se traduzca a la excepción de negocio asociada al nombre del índice.

**Regla:** `guardar(repositorio, entidad, excepcionesPorIndice)` ejecuta `repositorio.save(entidad)` y luego `repositorio.flush()`. Si falla con `DataIntegrityViolationException`, busca en la cadena de causas el SQLSTATE y el nombre de la restricción:

- Una violación de unicidad es la que informa el SQLSTATE `23505`.
- El nombre sale de `ConstraintViolationException.getConstraintName()` y, cuando es nulo (caso de PostgreSQL con Hibernate 7.4), del campo `constraint` de `PSQLException.getServerErrorMessage()`. Se compara sin distinguir mayúsculas.
- Si el nombre figura en el mapa, lanza su excepción. Si no, lanza `RecursoDuplicadoException("Ya existe un registro con los mismos datos")`.
- Una violación que no es de unicidad se propaga sin traducir.

---

### `guardar(JpaRepository, T, Map)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-GU-01** | Guardado exitoso | `save(entidad)` devuelve la entidad guardada | No hay violación | Devuelve la entidad que devolvió `save`. Llama a `save` y después a `flush`, en ese orden. |
| **CP-GU-02** | Índice mapeado violado en el `save` | `save` lanza `DataIntegrityViolationException` con SQLSTATE `23505` y `constraint = udx_usuarios_username`; el mapa asocia ese índice a "El nombre de usuario ya está en uso" | El nombre del índice figura en el mapa | Lanza `RecursoDuplicadoException`: "El nombre de usuario ya está en uso". |
| **CP-GU-03** | Índice mapeado violado en el `flush` | `save` responde bien y `flush` lanza la misma violación | La violación también se traduce si aparece al confirmar | Lanza `RecursoDuplicadoException`: "El nombre de usuario ya está en uso". |
| **CP-GU-04** | Índice no mapeado | Violación `23505` con `constraint = udx_otra_tabla_campo`, ausente del mapa | El índice no tiene excepción asociada | Lanza `RecursoDuplicadoException`: "Ya existe un registro con los mismos datos". |
| **CP-GU-05** | Nombre en mayúsculas | Violación `23505` con `constraint = UDX_USUARIOS_USERNAME` | La comparación del nombre no distingue mayúsculas | Lanza `RecursoDuplicadoException`: "El nombre de usuario ya está en uso". |
| **CP-GU-06** | Nombre informado por Hibernate | `ConstraintViolationException` con `getConstraintName() = udx_usuarios_username` y SQLSTATE `23505` | Se usa el nombre que informa Hibernate cuando está disponible | Lanza `RecursoDuplicadoException`: "El nombre de usuario ya está en uso". |
| **CP-GU-07** | Violación sin nombre identificable | `DataIntegrityViolationException` con una causa `SQLException` de SQLSTATE `23505` sin nombre de restricción y mapa vacío | Es una violación de unicidad pero no se conoce el índice | Lanza `RecursoDuplicadoException`: "Ya existe un registro con los mismos datos". |
| **CP-GU-08** | Violación que no es de unicidad | `DataIntegrityViolationException` con SQLSTATE `23502` (columna obligatoria nula) | No es una violación de unicidad | Propaga la misma `DataIntegrityViolationException`, sin traducir. |
| **CP-GU-09** | Excepción ajena | `save` lanza `ReglaNegocioException` | No es una violación de integridad | Propaga la misma excepción. |

### `localizarRestriccion(Throwable)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-GU-10** | Nombre desde el mensaje del servidor | Cadena `DataIntegrityViolationException` → `ConstraintViolationException` (sin nombre) → `PSQLException` con `constraint = UDX_Usuarios_Username` y SQLSTATE `23505` | El nombre se lee del campo `constraint` del servidor, no del texto del mensaje | Devuelve el nombre en minúsculas `udx_usuarios_username`, SQLSTATE `23505` y `esUnicidad() = true`. |
| **CP-GU-11** | Sin causas reconocibles | `DataIntegrityViolationException` sin causa | No hay SQLSTATE ni nombre | Devuelve nombre `null`, SQLSTATE `null` y `esUnicidad() = false`. |
