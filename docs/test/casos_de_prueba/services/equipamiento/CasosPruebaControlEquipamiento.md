### 1. `filtrarEquipamientosParaControl(String identificador, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable)`

Devuelve los equipamientos activos con los datos de control: tipo, estado operativo, usos desde el último mantenimiento (limpiezas registradas que empezaron después de la fecha del último mantenimiento registrado) y máximo. El orden es natural por identificador interno (lo resuelve el repositorio). Los usos y las últimas operaciones de toda la página se obtienen con una consulta cada uno, por lista de IDs, sin importar cuántos equipamientos tenga la página. El identificador buscado se normaliza igual que el guardado.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-01**|Arma los datos de control con una consulta de usos|`identificador: "MOL"`, `tipo: MOLINO`, `estadoOperativo: DISPONIBLE`, página con un molino (7 usos) y un macerador sin usos|`contarUsosDesdeUltimoMantenimiento([1, 2])` se invoca una vez|El molino informa tipo `MOLINO`, 7 usos y máximo 100. El macerador informa tipo `MACERADOR` y 0 usos.|
|**CP-CE-02**|Página vacía|El repositorio devuelve una página sin equipamientos|La página no tiene IDs|Retorna una página vacía. No consulta usos.|
|**CP-CE-03**|El identificador buscado se normaliza|`identificador: "  mol   01  "` y `identificador: "    "`|Tras normalizar queda `"MOL 01"` en el primer caso y vacío en el segundo|Consulta el repositorio con `"MOL 01"` y con `null` respectivamente.|

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

Devuelve las limpiezas, fallas y mantenimientos del equipamiento por separado, con todas las operaciones (anuladas incluidas), de la de inicio más reciente a la más antigua, con estado, inicio, fin, observaciones, responsable (usuario que la registró) y datos de anulación. El indicador `anulable` es verdadero solo en la última operación registrada del equipamiento, de cualquier tipo: la de mayor fecha y, a igual fecha (posible en datos migrados), la de mayor ID, siempre que su fecha sea estrictamente posterior al fin de la última participación del equipamiento en un lote (si nunca participó en uno, no hay restricción) y que el equipamiento no esté en uso por un lote (el estado operativo se lee del equipamiento que valida la existencia, sin otra consulta). Es la misma regla que usa la anulación y el listado de control, y la última participación se consulta una sola vez por llamada.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-11**|Agrupa por tipo y marca solo la última registrada como anulable|`idEquipamiento: 1L` con 2 limpiezas (una `REGISTRADO` de responsable "operario1" y una `ANULADO` con el mismo fin), 1 falla y 1 mantenimiento; `buscarIdUltimaOperacionRegistrada(1L)` devuelve el ID de la limpieza `REGISTRADO`|`operacion.estado == REGISTRADO` y `operacion.id == ID de la última` $\rightarrow$ **solo para una**|Cada lista contiene sus operaciones con todos los datos. Solo la limpieza `REGISTRADO` es anulable; la anulada conserva `fechaAnulacion` y `motivoAnulacion` y no es anulable; la falla y el mantenimiento no son anulables. Solo la limpieza informa `estadoOperativoResultante`.|
|**CP-CE-12**|Equipamiento sin operaciones|`idEquipamiento: 1L` (existe), sin operaciones|Los tres listados están vacíos|Retorna las tres listas vacías.|
|**CP-CE-13**|Equipamiento inexistente|`idEquipamiento: 99L`|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 99"). No consulta las operaciones.|
|**CP-CE-24**|Empate de fecha: solo la operación de mayor ID es anulable|Una limpieza (ID 30) y una falla (ID 31) registradas con la misma fecha; `buscarIdUltimaOperacionRegistrada(1L)` $\rightarrow$ **31**|Solo una operación tiene el ID de la última|La falla es anulable y la limpieza no: nunca hay más de una operación anulable.|
|**CP-CE-26**|La última operación no es anulable si hubo un lote posterior|Una limpieza `REGISTRADO` que es la última operación; `buscarFechaUltimaParticipacionRegistrada(1L)` $\rightarrow$ **fecha posterior a la de la limpieza**|`esPosteriorAlUltimoLote` $\rightarrow$ **FALSE**|La limpieza no es anulable.|
|**CP-CE-27**|La última operación es anulable si es posterior al último lote|Una limpieza `REGISTRADO` que es la última operación; `buscarFechaUltimaParticipacionRegistrada(1L)` $\rightarrow$ **fecha anterior a la de la limpieza**|`esPosteriorAlUltimoLote` $\rightarrow$ **TRUE**|La limpieza es anulable.|
|**CP-CE-28**|La última participación se consulta una sola vez|Una limpieza, una falla y un mantenimiento; `buscarFechaUltimaParticipacionRegistrada(1L)` $\rightarrow$ **Optional.empty()**|Una consulta por llamada, sin importar la cantidad de operaciones|`buscarFechaUltimaParticipacionRegistrada` se invoca exactamente una vez y la última operación es anulable.|
|**CP-CE-29**|Ninguna operación es anulable con el equipamiento en uso|Equipamiento con `estadoOperativo: EN_USO`; una limpieza `REGISTRADO` que es la última operación, una falla y un mantenimiento; `buscarFechaUltimaParticipacionRegistrada(1L)` $\rightarrow$ **fecha anterior a la de la limpieza**|`estaEnUso` $\rightarrow$ **TRUE**|Las tres operaciones informan `anulable = false`, aunque la limpieza cumpla el resto de las condiciones.|
|**CP-CE-30**|El equipamiento fuera de uso no restringe el indicador y se lee una sola vez|Equipamiento con `estadoOperativo: EN_LIMPIEZA`; una limpieza `REGISTRADO` que es la última operación y una falla anterior; sin participaciones en lotes|`estaEnUso` $\rightarrow$ **FALSE**|La limpieza es anulable y la falla no. `findById(1L)` se invoca exactamente una vez.|

### 4. `listarHistorial(Long idEquipamiento, TipoActividadEquipamiento tipo, LocalDateTime desde, LocalDateTime hasta, Pageable pageable)`

Une las etapas de lote en las que se usó el equipamiento y las limpiezas, fallas y mantenimientos registrados (las operaciones anuladas no aparecen), de la actividad más reciente a la más antigua, y devuelve una página. Cada fila informa el estado operativo que dejó en el equipamiento. El rango de fechas se aplica a la fecha de la operación y al inicio de la etapa.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-14**|Une etapas y operaciones por fecha descendente|Una etapa `FINALIZADA` de Molienda, una limpieza y una falla registradas|Las dos fuentes se unen y se ordenan por fecha descendente|Retorna las filas de la más reciente a la más antigua, cada una con el estado operativo que dejó.|
|**CP-CE-15**|Etapa en curso|Una etapa `EN_CURSO` sin fecha de finalización|`estado == EN_CURSO` $ightarrow$ **TRUE**|La fila no tiene fecha de fin, figura en curso y deja el equipamiento En uso.|
|**CP-CE-16**|Estado que deja cada etapa|Etapas de Molienda, Fermentación, Maduración y una etapa `CANCELADA` iniciada|Tipo de etapa y estado de la etapa|Fermentación y Maduración no cambian el estado (el fermentador pasa a En limpieza al finalizar el Envasado) y la etapa cancelada deja En limpieza.|
|**CP-CE-17**|Filtro por tipo|Tipo `LOTE` y tipo `FALLA`|El tipo decide qué fuentes se consultan|Por `LOTE` no consulta las operaciones; por `FALLA` no consulta las etapas y deja solo las fallas.|
|**CP-CE-18**|Rango de fechas|`desde` posterior a `hasta`; un rango válido|`hasta.isBefore(desde)`|El rango válido se pasa a las dos consultas; el invertido lanza `ReglaNegocioException` ("La fecha hasta no puede ser anterior a la fecha desde", campo `hasta`) y no consulta.|
|**CP-CE-19**|Equipamiento inexistente|`idEquipamiento: 99L`|`existsById(99L)` $ightarrow$ **FALSE**|Lanza `RecursoNoEncontradoException` ("No se encontró el equipamiento con ID: 99"). No consulta etapas ni operaciones.|
|**CP-CE-25**|Paginación|3 fallas, páginas de 2|`subList` de la página pedida|Informa el total (3), devuelve la fila que corresponde y una página fuera de rango vacía.|

### 5. Consultas del repositorio _(verificación manual contra la base de datos)_

Las consultas JPQL nuevas se verifican contra una base descartable con datos sembrados, porque los tests unitarios las reemplazan por mocks.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-20**|`buscarFechaUltimaOperacionRegistrada`|Equipamiento con limpiezas, fallas y mantenimientos `REGISTRADO` y una operación `ANULADO` con fecha posterior|`MAX(fecha)` de las operaciones `REGISTRADO` de las tres tablas|Devuelve la mayor fecha entre las registradas; la anulada no cuenta. Sin operaciones devuelve vacío.|
|**CP-CE-21**|`contarUsosDesdeUltimoMantenimiento`|Dos equipamientos: uno con limpiezas antes y después de un mantenimiento y otro sin operaciones|Una consulta por lista de IDs|Los usos cuentan solo las limpiezas que empezaron después de la fecha del último mantenimiento. El equipamiento sin operaciones no aparece.|
|**CP-CE-22**|`buscarEtapasConUsoRealPorInicio`|Etapas `PENDIENTE`, `EN_CURSO`, `FINALIZADA`, `CANCELADA` con y sin fecha de inicio, dentro y fuera del rango de inicio|Solo etapas con fecha de inicio y estado distinto de `PENDIENTE` cuyo inicio cae en el rango (extremos nulos sin acotar)|Devuelve las etapas con uso real de la más reciente a la más antigua, cada una con su lote.|
|**CP-CE-23**|`buscarRegistradasEnPeriodo`|Operaciones `REGISTRADO` y `ANULADO` dentro y fuera del rango|Solo operaciones `REGISTRADO` cuya fecha cae en el rango|Devuelve las registradas del rango, de la más reciente a la más antigua; las anuladas no aparecen.|
