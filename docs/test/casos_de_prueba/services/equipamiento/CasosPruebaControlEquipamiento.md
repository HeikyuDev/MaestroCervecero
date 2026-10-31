### 1. `filtrarEquipamientosParaControl(String identificador, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable)`

Devuelve los equipamientos activos con los datos de control: tipo, estado operativo, usos desde el último mantenimiento (limpiezas registradas que empezaron después de la fecha del último mantenimiento registrado) y máximo, y última operación registrada (tipo y fecha). El orden es natural por identificador interno (lo resuelve el repositorio). Los usos y las últimas operaciones de toda la página se obtienen con una consulta cada uno, por lista de IDs, sin importar cuántos equipamientos tenga la página. El identificador buscado se normaliza igual que el guardado.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-01**|Arma los datos de control con una consulta de usos y una de últimas operaciones|`identificador: "MOL"`, `tipo: MOLINO`, `estadoOperativo: DISPONIBLE`, página con un molino (7 usos, última operación una limpieza) y un macerador sin usos ni operaciones|`contarUsosDesdeUltimoMantenimiento([1, 2])` y `buscarUltimasOperacionesRegistradas([1, 2])` se invocan una vez cada una|El molino informa tipo `MOLINO`, 7 usos, máximo 100, tipo de última operación `LIMPIEZA` y su fecha. El macerador informa tipo `MACERADOR`, 0 usos y última operación nula.|
|**CP-CE-02**|Página vacía|El repositorio devuelve una página sin equipamientos|La página no tiene IDs|Retorna una página vacía. No consulta usos ni operaciones.|
|**CP-CE-03**|El identificador buscado se normaliza|`identificador: "  mol   01  "` y `identificador: "    "`|Tras normalizar queda `"MOL 01"` en el primer caso y vacío en el segundo|Consulta el repositorio con `"MOL 01"` y con `null` respectivamente.|
|**CP-CE-04**|Empate de fecha en la última operación|Dos operaciones registradas del mismo equipamiento con igual fecha (una falla de ID 20 y una limpieza de ID 10)|`buscarUltimasOperacionesRegistradas` devuelve ambas|Informa como última operación la de mayor ID: `FALLA`.|

### 2. `buscarResumen(Long idEquipamiento)`

Devuelve los datos del equipamiento activo, los parámetros propios de su tipo (los que no le corresponden quedan nulos), sus usos desde el último mantenimiento y las fechas de su última limpieza y su último mantenimiento registrados.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-05**|Resumen de un molino|`idEquipamiento: 1L` (molino con rendimiento 120), último mantenimiento terminó 10 días antes del inicio, `contarLimpiezasRegistradasDesde(1L, finMantenimiento)` $\rightarrow$ **4**|El equipamiento existe y está activo|Informa tipo `MOLINO`, estado operativo, máximo de usos 100, 4 usos, las dos fechas de fin y `rendimientoMolienda: 120`. Capacidades y demás parámetros nulos.|
|**CP-CE-06**|Resumen de un macerador|`idEquipamiento: 2L` (macerador)|El equipamiento existe y está activo|Informa tipo `MACERADOR`, capacidad total 500, capacidad útil 450, espacio muerto 10 y eficiencia de maceración 0,75. El rendimiento de molienda es nulo.|
|**CP-CE-07**|Resumen de una olla de hervor|`idEquipamiento: 3L` (olla de hervor)|El equipamiento existe y está activo|Informa tipo `OLLA_HERVOR`, capacidades, evaporación 8 y pérdida por trub 3. El espacio muerto es nulo.|
|**CP-CE-08**|Resumen de un fermentador sin limpiezas ni mantenimientos|`idEquipamiento: 4L` (fermentador), sin limpiezas ni mantenimientos registrados|Las consultas de fechas devuelven vacío|Informa tipo `FERMENTADOR`, capacidades, 0 usos y ambas fechas nulas. Cuenta las limpiezas sin fecha de corte (`contarLimpiezasRegistradasDesde(4L, null)`).|
|**CP-CE-09**|Equipamiento inexistente|`idEquipamiento: 99L`|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 99").|
|**CP-CE-10**|Equipamiento dado de baja|`idEquipamiento: 1L` (`estado: BAJA`)|`estado != ACTIVO` $\rightarrow$ **TRUE**|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 1").|

### 3. `listarOperaciones(Long idEquipamiento)`

Devuelve las limpiezas, fallas y mantenimientos del equipamiento por separado, con todas las operaciones (anuladas incluidas), de la de inicio más reciente a la más antigua, con estado, inicio, fin, observaciones, responsable (usuario que la registró) y datos de anulación. El indicador `anulable` es verdadero solo en la última operación registrada del equipamiento, de cualquier tipo: la de mayor fecha y, a igual fecha (posible en datos migrados), la de mayor ID. Es la misma regla de desempate que usa la anulación y el listado de control.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-11**|Agrupa por tipo y marca solo la última registrada como anulable|`idEquipamiento: 1L` con 2 limpiezas (una `REGISTRADO` de responsable "operario1" y una `ANULADO` con el mismo fin), 1 falla y 1 mantenimiento; `buscarIdUltimaOperacionRegistrada(1L)` devuelve el ID de la limpieza `REGISTRADO`|`operacion.estado == REGISTRADO` y `operacion.id == ID de la última` $\rightarrow$ **solo para una**|Cada lista contiene sus operaciones con todos los datos. Solo la limpieza `REGISTRADO` es anulable; la anulada conserva `fechaAnulacion` y `motivoAnulacion` y no es anulable; la falla y el mantenimiento no son anulables. Solo la limpieza informa `estadoOperativoResultante`.|
|**CP-CE-12**|Equipamiento sin operaciones|`idEquipamiento: 1L` (existe), sin operaciones|Los tres listados están vacíos|Retorna las tres listas vacías.|
|**CP-CE-13**|Equipamiento inexistente|`idEquipamiento: 99L`|`existsById(99L)` $\rightarrow$ **FALSE**|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 99"). No consulta las operaciones.|
|**CP-CE-24**|Empate de fecha: solo la operación de mayor ID es anulable|Una limpieza (ID 30) y una falla (ID 31) registradas con la misma fecha; `buscarIdUltimaOperacionRegistrada(1L)` $\rightarrow$ **31**|Solo una operación tiene el ID de la última|La falla es anulable y la limpieza no: nunca hay más de una operación anulable.|

### 4. `listarLineaDeTiempo(Long idEquipamiento, LocalDateTime desde, LocalDateTime hasta)`

Devuelve un bloque por cada etapa de lote con uso real del equipamiento (etapa `EN_CURSO`, `FINALIZADA` o `CANCELADA` con fecha de inicio) y por cada operación registrada (nunca las anuladas) cuya fecha cae dentro del período, ordenados por inicio. Un extremo nulo no acota el período por ese lado. Los bloques conservan sus fechas reales, sin recortarlas al período. La etapa en curso usa el momento de la consulta como fin provisorio (`finProvisorio: true`). La consulta de etapas trae el lote junto con la etapa (una sola consulta).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-14**|Combina bloques de lote y de operaciones, ordenados por inicio|Una etapa `FINALIZADA` de la molienda del lote "LOT-001", una falla y una limpieza registradas|Se unen las dos consultas y se ordenan por fecha de inicio|Retorna los bloques en el orden falla, lote, limpieza. El bloque de lote informa código del lote, etapa, estado de la etapa, inicio y fin reales; el de falla, sus observaciones y sus fechas.|
|**CP-CE-15**|Etapa en curso con fin provisorio|Una etapa `EN_CURSO` sin fecha de finalización|`estado == EN_CURSO` $\rightarrow$ **TRUE**|El bloque tiene `finProvisorio: true` y como fin el momento de la consulta.|
|**CP-CE-16**|Etapa cancelada sin fecha de finalización|Una etapa `CANCELADA` con fecha de inicio y sin fecha de finalización|La etapa no está en curso y no tiene fin|El bloque tiene el fin igual al inicio (sin duración) y `finProvisorio: false`.|
|**CP-CE-17**|Etapa en curso fuera de un período futuro|Período que empieza 10 días después de ahora; la consulta devuelve una etapa `EN_CURSO`|El fin provisorio (ahora) es anterior al inicio del período|Se descarta el bloque: retorna una lista vacía.|
|**CP-CE-18**|Período invertido|`desde: 2026-01-15T10:00`, `hasta: 2026-01-14T10:00`|`hasta.isBefore(desde)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha hasta no puede ser anterior a la fecha desde", campo `hasta`). No consulta etapas.|
|**CP-CE-19**|Equipamiento inexistente|`idEquipamiento: 99L`|`existsById(99L)` $\rightarrow$ **FALSE**|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 99"). No consulta etapas ni operaciones.|

### 5. Consultas del repositorio _(verificación manual contra la base de datos)_

Las consultas JPQL nuevas se verifican contra una base descartable con datos sembrados, porque los tests unitarios las reemplazan por mocks.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-20**|`buscarFechaUltimaOperacionRegistrada`|Equipamiento con limpiezas, fallas y mantenimientos `REGISTRADO` y una operación `ANULADO` con fecha posterior|`MAX(fecha)` de las operaciones `REGISTRADO` de las tres tablas|Devuelve la mayor fecha entre las registradas; la anulada no cuenta. Sin operaciones devuelve vacío.|
|**CP-CE-21**|`buscarUltimasOperacionesRegistradas` y `contarUsosDesdeUltimoMantenimiento`|Dos equipamientos: uno con limpiezas antes y después de un mantenimiento y otro sin operaciones|Una consulta por lista de IDs|La última operación de cada equipamiento con operaciones; los usos cuentan solo las limpiezas que empezaron después de la fecha del último mantenimiento. El equipamiento sin operaciones no aparece.|
|**CP-CE-22**|`buscarEtapasConUsoRealEnPeriodo`|Etapas `PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA` con y sin fecha de inicio, dentro y fuera del período|Solo etapas con fecha de inicio y estado distinto de `PENDIENTE` que se superponen con el período|Devuelve las etapas con uso real superpuestas con el período, con su lote cargado, ordenadas por inicio.|
|**CP-CE-23**|`buscarRegistradasEnPeriodo`|Operaciones `REGISTRADO` y `ANULADO` dentro y fuera del período|Solo operaciones `REGISTRADO` que se superponen con el período|Devuelve las registradas superpuestas con el período, ordenadas por inicio; las anuladas no aparecen.|
