## Casos de prueba de integración contra PostgreSQL: esquema y unicidad

**Tipo de prueba:** integración (clase `EsquemaYUnicidadIntegracionTest`, etiqueta `integracion`). Levanta el contexto completo de Spring con el perfil `test` contra la base `maestrocervecero_test`, con el esquema creado por Flyway y validado por Hibernate (`ddl-auto=validate`). Cómo prepararla y ejecutarla: `docs/BaseDeDatos.md`, sección "Tests de integración".

**Regla:** antes de cada test `IntegracionBaseTest` vacía las tablas de negocio y elimina los usuarios distintos de `admin`; conserva `flyway_schema_history`, `configuracion_produccion` y el administrador.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-IB-01** | Flyway migró el esquema | Contexto arrancado contra la base de tests | Todas las migraciones están aplicadas | Hay al menos dos migraciones (`V1`, `V2`), todas en estado `SUCCESS`, ninguna pendiente, y la cantidad de filas exitosas de `flyway_schema_history` coincide con la de migraciones. |
| **CP-IB-02** | El esquema valida y existen los datos iniciales | Contexto arrancado con `ddl-auto=validate` | Las entidades coinciden con las tablas; `V2` y `UserDataLoader` cargaron sus datos | La propiedad `ddl-auto` es `validate`; existe la fila `id = 1` de `configuracion_produccion` y el usuario `admin`. |
| **CP-IB-03** | Username nuevo | `GuardadoUnico.guardar` de un usuario con username `nuevo` | No hay otro usuario con ese username | Se guarda con un `id` asignado y hay una fila con ese username. |
| **CP-IB-04** | Username duplicado | Se guarda un usuario `duplicado` y luego otro con el mismo username | La restricción `uk_usuarios_username` rechaza el segundo | Lanza `RecursoDuplicadoException` con el mensaje asociado al índice ("El nombre de usuario 'duplicado' ya está en uso"); queda una sola fila. |
| **CP-IB-05** | Índice sin excepción asociada | Dos guardados con el mismo username y mapa de excepciones vacío | El nombre del índice no figura en el mapa | Lanza `RecursoDuplicadoException`: "Ya existe un registro con los mismos datos". |
| **CP-IB-06** | Violación que no es de unicidad | Usuario con `nombre = null` | La columna obligatoria rechaza el valor | Lanza `DataIntegrityViolationException` sin traducir. |
| **CP-IB-07** | Qué informa Hibernate con PostgreSQL | Dos `saveAndFlush` con el mismo username | Cadena de causas de la violación | `ConstraintViolationException` con SQLSTATE `23505` y tipo `UNIQUE`, pero `getConstraintName()` nulo; el nombre `uk_usuarios_username` está en `PSQLException.getServerErrorMessage().getConstraint()`. |
| **CP-IB-08** | Índice único parcial por expresión | Tabla temporal de prueba con `CREATE UNIQUE INDEX udx_tmp_indice_parcial_nombre ON ... (lower(nombre)) WHERE estado = 'ACTIVO'`; filas `Pale Ale` activa y `Pale Ale` en baja; se inserta `PALE ALE` activa | El índice parcial ignora los registros en baja y detecta el duplicado entre activos sin distinguir mayúsculas | La segunda inserción (`BAJA`) se acepta; la tercera lanza `DataIntegrityViolationException` cuyo nombre de restricción es `udx_tmp_indice_parcial_nombre` y es una violación de unicidad. La tabla se elimina al terminar. |
