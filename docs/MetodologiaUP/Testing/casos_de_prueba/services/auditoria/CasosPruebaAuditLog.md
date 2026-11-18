### 1. `filtrarAuditLogs(LocalDateTime fechaDesde, LocalDateTime fechaHasta, AccionAuditoria accion, ConceptoAuditoria conceptoAuditoria, String username, Pageable pageable)`

La bitácora es de solo lectura y no tiene baja lógica: no hay condición de estado. `username` es coincidencia parcial, sin distinguir mayúsculas/minúsculas. Los límites del rango de fechas son inclusivos y cualquiera de los dos puede omitirse. Todos los criterios son opcionales, `null` = no filtra.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FA-01**|Filtra por los cinco criterios informados|`fechaDesde: 2026-09-01T00:00`, `fechaHasta: 2026-09-30T23:59:59`, `accion: CREAR`, `conceptoAuditoria: MALTA`, `username: "adm"`, `pageable: PageRequest.of(0, 20)`, BD con 2 entradas que cumplen los cinco criterios|`filtrarAuditLogs(...)` contiene elementos|Retorna `Page<AuditLogResponseDTO>` con 2 elementos mapeados (id, username, acción, concepto, entidadId, fechaHora e IP).|
|**CP-FA-02**|Los cinco parámetros nulos no restringen la búsqueda|Todos los criterios `null`, `pageable: PageRequest.of(0, 20)`|El service propaga los criterios nulos tal cual al repositorio|Retorna `Page<AuditLogResponseDTO>` con todas las entradas (equivalente a no filtrar).|
|**CP-FA-03**|Consulta sin coincidencias|`username: "inexistente"`, resto de criterios `null`, `pageable: PageRequest.of(0, 20)`|`filtrarAuditLogs(...)` está vacío|Retorna `Page<AuditLogResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FA-04**|Rango de fechas invertido|`fechaDesde: 2026-09-30T00:00`, `fechaHasta: 2026-09-01T00:00`|`fechaDesde` es posterior a `fechaHasta` $\rightarrow$ **Rechaza**|Lanza `ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde")`. No consulta el repositorio.|
|**CP-FA-05**|Solo fecha desde informada|`fechaDesde: 2026-09-01T00:00`, `fechaHasta: null`, resto `null`|Un rango con un solo extremo no se valida y se propaga tal cual|Retorna la página del repositorio; no lanza excepción.|
|**CP-FA-06**|Solo fecha hasta informada|`fechaDesde: null`, `fechaHasta: 2026-09-30T23:59:59`, resto `null`|Un rango con un solo extremo no se valida y se propaga tal cual|Retorna la página del repositorio; no lanza excepción.|
|**CP-FA-07**|Fecha desde igual a fecha hasta|`fechaDesde` = `fechaHasta` = `2026-09-15T10:30`|El rango de un único instante es válido (límites inclusivos)|No lanza excepción; consulta el repositorio con ambos valores iguales.|
|**CP-FA-08**|Username en blanco equivale a no filtrar|`username: "   "`, resto `null`|Un username vacío o en blanco se normaliza a `null`|Consulta el repositorio con `username: null`.|
|**CP-FA-09**|Username con espacios sobrantes|`username: "  admin  "`, resto `null`|El username se recorta antes de consultar|Consulta el repositorio con `username: "admin"`.|
|**CP-FA-10**|Username con espacios internos repetidos|`username: "  juan   perez "`, resto `null`|El username se normaliza (recorta y colapsa espacios) antes de consultar|Consulta el repositorio con `username: "juan perez"`.|
|**CP-FA-11**|Marca de entrada con detalle|4 entradas: con solo `estadoPosterior`, con solo `estadoAnterior`, con `estadoAnterior` en blanco y sin estados|El DTO de la lista informa si la entrada guardó algún estado, sin leer ni interpretar el JSON|`tieneDetalle` es `true`, `true`, `false` y `false`. No se compara ningún JSON.|

### 2. `guardar(AuditLogEntity auditLogEntity)`

Corre de forma asíncrona (`@Async`); en los tests se invoca directamente. Un problema al auditar nunca debe hacer fallar a la operación de negocio que lo disparó.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-GA-01**|Guarda una entrada que ya trae fecha y hora|Entrada con `fechaHora: 2026-09-15T10:30`|La fecha y hora ya está informada|Llama a `save` con la entrada y conserva su `fechaHora`.|
|**CP-GA-02**|Completa la fecha y hora si falta|Entrada con `fechaHora: null`|`fechaHora == null`|Asigna la fecha y hora actual antes de llamar a `save`.|
|**CP-GA-03**|Una falla al persistir no se propaga|El repositorio lanza una `RuntimeException` en `save`|Se atrapa cualquier excepción al auditar|No lanza excepción; el error solo se registra en el log (solo el tipo de la falla, nunca su mensaje).|
|**CP-GA-04**|Reintento sin estados|Entrada con `estadoAnterior` y `estadoPosterior`; el primer `save` falla y el segundo se completa|El detalle de estados es lo más probable de rechazar|Se reintenta una vez con los dos estados en `null`; la entrada queda guardada sin detalle.|
|**CP-GA-05**|El reintento también falla|Entrada con `estadoPosterior`; todos los `save` fallan|El reintento es único|`save` se llama dos veces, no lanza excepción y el error solo se registra en el log.|

### 3. `buscarPorId(Long id)`

Devuelve la cabecera de la entrada y la comparación campo a campo de sus estados anterior y posterior (guardados como JSON). Cada campo queda como `SIN_CAMBIOS`, `MODIFICADO`, `AGREGADO` (solo existe en el posterior) o `ELIMINADO` (solo existe en el anterior). Un JSON ausente, vacío, inválido o que no es un objeto se trata como un estado inexistente. La comparación valor por valor se prueba en `CasosPruebaCapturaEstados.md` (`ComparadorEstadosAuditoria`).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BA-01**|Modificación con campos cambiados y sin cambios|`id: 7`, acción `MODIFICAR`, anterior `{"id":12,"nombre":"Pilsen","potencialExtracto":80}`, posterior `{"id":12,"nombre":"Pilsen Premium","potencialExtracto":80}`|Los dos estados son legibles|Retorna la cabecera (id, usuario, acción, concepto, entidad, fecha e IP), `tieneDetalle: true` y los campos "ID" y "Potencial extracto" `SIN_CAMBIOS` y "Nombre" `MODIFICADO` (de "Pilsen" a "Pilsen Premium"), más ambos JSON con sangría.|
|**CP-BA-02**|Alta (sin estado anterior)|`id: 8`, acción `CREAR`, solo posterior `{"id":13,"nombre":"Cascade"}`|El estado anterior no existe|`tieneDetalle: true` y todos los campos `AGREGADO` con valor anterior nulo.|
|**CP-BA-03**|Baja lógica|`id: 9`, acción `ELIMINAR`, anterior `{"id":12,"estado":"ACTIVO"}`, posterior `{"id":12,"estado":"BAJA"}`|La baja lógica es una modificación del estado|"ID" `SIN_CAMBIOS` y "Estado" `MODIFICADO` de "ACTIVO" a "BAJA".|
|**CP-BA-04**|Campos agregados y eliminados|anterior `{"id":12,"viejo":"x"}`, posterior `{"id":12,"nuevo":"y"}`|Un campo existe solo en uno de los estados|"ID" `SIN_CAMBIOS`, "Viejo" `ELIMINADO` y "Nuevo" `AGREGADO`, en ese orden.|
|**CP-BA-05**|Entrada sin estados|`id: 11`, acción `LOGIN_EXITOSO`, estados nulos|No hay detalle que mostrar|`tieneDetalle: false`, sin campos y JSON nulos. Conserva la cabecera. No falla.|
|**CP-BA-06**|JSON inválido|anterior `{esto no es json` y posterior `[1, 2`|El JSON guardado está dañado|`tieneDetalle: false` y sin campos. No lanza excepción.|
|**CP-BA-07**|ID inexistente|`id: 99`, el repositorio no encuentra la entrada|La entrada no existe|Lanza `RecursoNoEncontradoException("No se encontró el registro de auditoría con ID: 99")`.|
