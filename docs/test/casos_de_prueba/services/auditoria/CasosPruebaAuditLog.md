### 1. `filtrarAuditLogs(LocalDateTime fechaDesde, LocalDateTime fechaHasta, AccionAuditoria accion, ConceptoAuditoria conceptoAuditoria, String username, Pageable pageable)`

La bitácora es de solo lectura y no tiene baja lógica: no hay condición de estado. `username` es coincidencia parcial, sin distinguir mayúsculas/minúsculas. Los límites del rango de fechas son inclusivos y cualquiera de los dos puede omitirse. Todos los criterios son opcionales, `null` = no filtra.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FA-01**|Filtra por los cinco criterios informados|`fechaDesde: 2026-09-01T00:00`, `fechaHasta: 2026-09-30T23:59:59`, `accion: CREAR`, `conceptoAuditoria: MALTA`, `username: "adm"`, `pageable: PageRequest.of(0, 20)`, BD con 2 entradas que cumplen los cinco criterios|`filtrarAuditLogs(...)` contiene elementos|Retorna `Page<AuditLogResponseDTO>` con 2 elementos mapeados (id, username, acción, concepto, entidadId, fechaHora e IP).|
|**CP-FA-02**|Los cinco parámetros nulos no restringen la búsqueda|Todos los criterios `null`, `pageable: PageRequest.of(0, 20)`|El service propaga los criterios nulos tal cual al repositorio|Retorna `Page<AuditLogResponseDTO>` con todas las entradas (equivalente a no filtrar).|
|**CP-FA-03**|Consulta sin coincidencias|`username: "inexistente"`, resto de criterios `null`, `pageable: PageRequest.of(0, 20)`|`filtrarAuditLogs(...)` está vacío|Retorna `Page<AuditLogResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FA-04**|Rango de fechas invertido|`fechaDesde: 2026-09-30T00:00`, `fechaHasta: 2026-09-01T00:00`|`fechaDesde` es posterior a `fechaHasta` $\rightarrow$ **Rechaza**|Lanza `ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta.")`. No consulta el repositorio.|
|**CP-FA-05**|Solo fecha desde informada|`fechaDesde: 2026-09-01T00:00`, `fechaHasta: null`, resto `null`|Un rango con un solo extremo no se valida y se propaga tal cual|Retorna la página del repositorio; no lanza excepción.|
|**CP-FA-06**|Solo fecha hasta informada|`fechaDesde: null`, `fechaHasta: 2026-09-30T23:59:59`, resto `null`|Un rango con un solo extremo no se valida y se propaga tal cual|Retorna la página del repositorio; no lanza excepción.|
|**CP-FA-07**|Fecha desde igual a fecha hasta|`fechaDesde` = `fechaHasta` = `2026-09-15T10:30`|El rango de un único instante es válido (límites inclusivos)|No lanza excepción; consulta el repositorio con ambos valores iguales.|
|**CP-FA-08**|Username en blanco equivale a no filtrar|`username: "   "`, resto `null`|Un username vacío o en blanco se normaliza a `null`|Consulta el repositorio con `username: null`.|
|**CP-FA-09**|Username con espacios sobrantes|`username: "  admin  "`, resto `null`|El username se recorta antes de consultar|Consulta el repositorio con `username: "admin"`.|

### 2. `guardar(AuditLogEntity auditLogEntity)`

Corre de forma asíncrona (`@Async`); en los tests se invoca directamente. Un problema al auditar nunca debe hacer fallar a la operación de negocio que lo disparó.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-GA-01**|Guarda una entrada que ya trae fecha y hora|Entrada con `fechaHora: 2026-09-15T10:30`|La fecha y hora ya está informada|Llama a `save` con la entrada y conserva su `fechaHora`.|
|**CP-GA-02**|Completa la fecha y hora si falta|Entrada con `fechaHora: null`|`fechaHora == null`|Asigna la fecha y hora actual antes de llamar a `save`.|
|**CP-GA-03**|Una falla al persistir no se propaga|El repositorio lanza una `RuntimeException` en `save`|Se atrapa cualquier excepción al auditar|No lanza excepción; el error solo se registra en el log.|
