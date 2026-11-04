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

### 2. `validarFecha(LocalDateTime fecha)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-06**|Fecha pasada o igual al momento actual _(Borde)_|Una fecha pasada y la fecha y hora actual menos 1 segundo|La fecha no es nula ni futura|No lanza excepción.|
|**CP-MCV-07**|Fecha nula|`fecha: null`|`fecha == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de la operación es obligatoria", campo `fecha`).|

### 3. `validarFechaPosteriorALaActividadAnterior(LocalDateTime fecha, LocalDateTime... actividadesAnteriores)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-11**|Fecha posterior a toda la actividad anterior, o sin actividad|Actividades anteriores: 1 segundo y 3 días antes de la fecha; o ambas nulas|Todas son anteriores a la fecha|No lanza excepción.|
|**CP-MCV-12**|Fecha igual a la de una actividad anterior|Una actividad anterior igual a la fecha|`!anterior.isBefore(fecha)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento", campo `fecha`).|
|**CP-MCV-13**|Fecha anterior a la de cualquiera de las actividades|Actividades anteriores: 1 día antes de la fecha y 1 hora después|La segunda es posterior a la fecha|Lanza `ReglaNegocioException` (mismo mensaje que CP-MCV-12).|

### 4. `validarEsLaUltimaOperacion(Long idEstaOperacion, Long idUltimaOperacion)`

La "última" operación registrada es la de mayor fecha y, ante igual fecha (posible en datos migrados), la de mayor ID. La consulta que la resuelve devuelve su ID; esta validación compara contra el ID de la operación a anular.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-14**|Es la última operación|ID de la operación igual al ID de la última; o ID de la última nulo (sin operaciones)|Los IDs coinciden o no hay última|No lanza excepción.|
|**CP-MCV-15**|La última es otra operación (incluye el empate de fecha)|ID de la operación 5 e ID de la última 6|`idUltimaOperacion != idEstaOperacion` $ightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores").|

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

### 7. `esPosteriorAlUltimoLote(LocalDateTime fechaOperacion, LocalDateTime fechaFinUltimaParticipacion)` y `validarPosteriorAlUltimoLote(LocalDateTime fechaOperacion, LocalDateTime fechaFinUltimaParticipacion)`

La participación en un lote es una actividad que no se anula desde este módulo, así que una operación solo puede anularse si su fecha es estrictamente posterior al fin de la última participación del equipamiento en un lote. Si el equipamiento nunca participó en uno (fecha nula), no hay restricción. `esPosteriorAlUltimoLote` expresa la regla y `validarPosteriorAlUltimoLote` la aplica lanzando la excepción.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-24**|`esPosteriorAlUltimoLote` sin participación en lotes|Fin de la última participación nulo|No hubo participación|Devuelve `true`.|
|**CP-MCV-25**|`esPosteriorAlUltimoLote` con la operación posterior|Operación 2 horas después del fin de la participación; y operación 1 nanosegundo después|`fechaOperacion > fechaFin`|Devuelve `true` en ambos casos.|
|**CP-MCV-26**|`esPosteriorAlUltimoLote` con fechas iguales|Operación y fin de la participación en el mismo instante|La operación debe ser estrictamente posterior|Devuelve `false`.|
|**CP-MCV-27**|`esPosteriorAlUltimoLote` con el lote posterior a la operación|Fin de la participación 2 horas después de la operación|`fechaOperacion < fechaFin`|Devuelve `false`.|
|**CP-MCV-28**|`validarPosteriorAlUltimoLote` acepta|Fin de la participación nulo; y operación posterior al fin de la participación|`esPosteriorAlUltimoLote` $\rightarrow$ **TRUE**|No lanza excepción.|
|**CP-MCV-29**|`validarPosteriorAlUltimoLote` rechaza|Operación igual al fin de la participación; y operación anterior a él|`esPosteriorAlUltimoLote` $\rightarrow$ **FALSE**|Lanza `ReglaNegocioException` ("No se puede anular la operación porque el equipamiento participó en un lote posterior").|
