Casos de prueba de `MetodosCicloVidaEquipamiento` (utilidad sin dependencias externas): validaciones compartidas por la limpieza, la falla y el mantenimiento de un equipamiento. Cada regla tiene una única redacción, usada por las tres operaciones.

### 1. `validarObservaciones(String observaciones)` y `validarMotivoAnulacion(String motivoAnulacion)`

Ambos textos son obligatorios, se normalizan (sin espacios en los extremos ni repetidos) y no pueden superar 255 caracteres.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-01**|Observaciones normalizadas|`"  Ruido   anormal  "`|El texto se normaliza|Devuelve `"Ruido anormal"`.|
|**CP-MCV-02**|Observaciones nulas, vacías o en blanco|`null`, `""` y `"   "`|`observaciones` sin contenido $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Las observaciones son obligatorias", campo `observaciones`).|
|**CP-MCV-03**|Observaciones en el límite de 255 caracteres|255 y 256 caracteres|`length() > 255`|Con 255 no lanza excepción. Con 256 lanza `ReglaNegocioException` ("El texto de las observaciones no puede superar 255 caracteres", campo `observaciones`).|
|**CP-MCV-04**|Motivo de anulación nulo, vacío o en blanco|`null`, `""` y `"   "`|`motivoAnulacion` sin contenido $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio", campo `motivoAnulacion`).|
|**CP-MCV-05**|Motivo de anulación en el límite de 255 caracteres y normalización|`"  Carga   por error "`, 255 y 256 caracteres|`length() > 255`|Devuelve `"Carga por error"`. Con 255 no lanza excepción. Con 256 lanza `ReglaNegocioException` ("El motivo de anulación no puede superar 255 caracteres", campo `motivoAnulacion`).|

### 2. `validarFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-06**|Intervalo válido y fin igual al inicio _(Borde)_|Inicio 10:00 y fin 12:00; inicio y fin iguales|El fin no es anterior al inicio ni futuro|No lanza excepción.|
|**CP-MCV-07**|Fecha de inicio nula|`fechaInicio: null`|`fechaInicio == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de inicio es obligatoria", campo `fechaInicio`).|
|**CP-MCV-08**|Fecha de fin nula|`fechaFin: null`|`fechaFin == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de fin es obligatoria", campo `fechaFin`).|
|**CP-MCV-09**|Fin anterior al inicio|Inicio 12:00 y fin 10:00|`fechaFin.isBefore(fechaInicio)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de fin no puede ser anterior a la fecha de inicio", campo `fechaFin`).|
|**CP-MCV-10**|Fin posterior a la fecha y hora actual|`fechaFin`: ahora + 5 minutos|`fechaFin.isAfter(LocalDateTime.now())` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de fin no puede ser posterior a la fecha y hora actual", campo `fechaFin`).|

### 3. `validarInicioPosteriorALaActividadAnterior(LocalDateTime fechaInicio, LocalDateTime... finesAnteriores)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-11**|Inicio posterior a toda la actividad anterior, o sin actividad|Fines anteriores: 1 segundo y 3 días antes del inicio; o ambos nulos|Todos los fines son anteriores al inicio|No lanza excepción.|
|**CP-MCV-12**|Inicio igual al fin de una actividad anterior|Un fin anterior igual al inicio|`!finAnterior.isBefore(fechaInicio)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de inicio debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento", campo `fechaInicio`).|
|**CP-MCV-13**|Inicio anterior al fin de cualquiera de las actividades|Fines anteriores: 1 día antes del inicio y 1 hora después|El segundo fin es posterior al inicio|Lanza `ReglaNegocioException` (mismo mensaje que CP-MCV-12).|

### 4. `validarEsLaUltimaOperacion(Long idEstaOperacion, Long idUltimaOperacion)`

La "última" operación registrada es la de mayor fecha de fin y, ante igual fecha de fin (posible en datos migrados), la de mayor ID. La consulta que la resuelve devuelve su ID; esta validación compara contra el ID de la operación a anular.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-14**|Es la última operación|ID de la operación igual al ID de la última; o ID de la última nulo (sin operaciones)|Los IDs coinciden o no hay última|No lanza excepción.|
|**CP-MCV-15**|La última es otra operación (incluye el empate de fecha de fin)|ID de la operación 5 e ID de la última 6|`idUltimaOperacion != idEstaOperacion` $ightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores").|

### 5. `resolverEquipamientoActivo(Optional<EquipamientoEntity> equipamiento, Long idEquipamiento)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-16**|Equipamiento inexistente|`Optional.empty()`, `idEquipamiento: 99L`|El equipamiento no existe|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 99", campo `idEquipamiento`).|
|**CP-MCV-17**|Equipamiento dado de baja|Equipamiento con `estado: BAJA`|`estado == BAJA` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El equipamiento está dado de baja", campo `idEquipamiento`), distinto del mensaje de equipamiento inexistente.|
|**CP-MCV-18**|Equipamiento activo|Equipamiento con `estado: ACTIVO`|El equipamiento existe y está activo|Devuelve el mismo equipamiento.|

### 6. `validarEstadoOperativoParaRegistrar(EstadoOperativo estadoActual, EstadoOperativo estadoRequerido, String operacion)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-19**|Estado operativo requerido|Estado actual y requerido `EN_LIMPIEZA`|`estadoActual == estadoRequerido` $\rightarrow$ **TRUE**|No lanza excepción.|
|**CP-MCV-20**|Equipamiento en uso|Estado actual `EN_USO`, operación "una falla"|El equipamiento en uso no admite operaciones manuales|Lanza `ReglaNegocioException` ("No se puede registrar una falla porque el equipamiento está en uso", campo `idEquipamiento`).|
|**CP-MCV-21**|Equipamiento en limpieza|Estado actual `EN_LIMPIEZA`, requerido `DISPONIBLE`, operación "una falla"|Hay una limpieza pendiente|Lanza `ReglaNegocioException` ("Para registrar una falla, primero registre la limpieza del equipamiento").|
|**CP-MCV-22**|Equipamiento en mantenimiento|Estado actual `EN_MANTENIMIENTO`, requerido `EN_LIMPIEZA`, operación "una limpieza"|Hay un mantenimiento pendiente|Lanza `ReglaNegocioException` ("Para registrar una limpieza, primero registre el mantenimiento del equipamiento").|
|**CP-MCV-23**|Equipamiento disponible|Estado actual `DISPONIBLE`, requerido `EN_MANTENIMIENTO`, operación "un mantenimiento"|El equipamiento no necesita la operación|Lanza `ReglaNegocioException` ("No se puede registrar un mantenimiento porque el equipamiento está disponible").|
