### 1. `filtrarEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FE-01**|Filtra por los 3 criterios informados, traduciendo el tipo a su clase concreta|`identificadorInterno: "MOL"`, `tipo: MOLINO`, `estadoOperativo: DISPONIBLE`, `pageable: PageRequest.of(0, 10)`, BD con 2 equipamientos activos que cumplen los 3 criterios|`filtrarEquipamientos("MOL", MolinoEntity.class, DISPONIBLE, pageable)` contiene elementos|Retorna `Page<EquipamientoResponseDTO>` con 2 elementos mapeados. El service traduce `TipoEquipamiento.MOLINO` a `MolinoEntity.class` antes de invocar al repositorio.|
|**CP-FE-02**|Tipo nulo se propaga como clase nula|`identificadorInterno: "MOL"`, `tipo: null`, `estadoOperativo: DISPONIBLE`, `pageable: PageRequest.of(0, 10)`|El service invoca al repositorio con `tipoClase: null`|Retorna `Page<EquipamientoResponseDTO>` con los equipamientos de cualquier tipo que cumplen los demás criterios.|
|**CP-FE-03**|Identificador interno y estado operativo nulos no restringen la búsqueda|`identificadorInterno: null`, `tipo: MOLINO`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga ambos parámetros nulos tal cual al repositorio|Retorna `Page<EquipamientoResponseDTO>` con todos los equipamientos del tipo indicado, sin importar su identificador o estado operativo.|
|**CP-FE-04**|Consulta sin coincidencias|`identificadorInterno: "Inexistente"`, `tipo: null`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarEquipamientos("Inexistente", null, null, pageable)` está vacío|Retorna `Page<EquipamientoResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `obtenerResumen()`

El resumen cuenta solo equipamientos activos y siempre trae las 16 combinaciones de tipo (molino, macerador, olla de hervor, fermentador) y estado operativo (disponible, en uso, en limpieza, en mantenimiento), con 0 donde no hay ninguno.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-OR-01**|Cuenta los equipamientos por tipo y estado|`listarActivos()` devuelve 2 molinos `DISPONIBLE`, 1 molino `EN_MANTENIMIENTO`, 1 fermentador `EN_USO` y 1 macerador `EN_LIMPIEZA`|Cada equipamiento se suma a la combinación de su tipo concreto y su estado operativo|Molino: `DISPONIBLE = 2`, `EN_MANTENIMIENTO = 1`. Fermentador: `EN_USO = 1`. Macerador: `EN_LIMPIEZA = 1`.|
|**CP-OR-02**|Las combinaciones sin equipos quedan en cero|`listarActivos()` devuelve solo 1 molino `DISPONIBLE`|El resumen no omite las combinaciones sin equipos|El mapa tiene los 4 tipos y cada uno los 4 estados; todas las combinaciones valen 0 salvo molino `DISPONIBLE = 1`.|
|**CP-OR-03**|Sin equipamientos|`listarActivos()` devuelve una lista vacía|No hay nada que contar|Retorna el mapa completo con las 16 combinaciones en 0. No lanza excepción.|

### 3. `filtrarResumenEquipamientos(String identificadorInterno, TipoEquipamiento tipo, EstadoOperativo estadoOperativo, Pageable pageable)`

Para cada equipamiento de la página informa la etapa que tiene en curso (`EN_CURSO`) y las etapas que tiene reservadas a futuro (`PENDIENTE` de lotes `PENDIENTE` o `EN_EJECUCION`). Las etapas de toda la página se consultan con una sola query.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FR-01**|Filtra por los 3 criterios, traduciendo el tipo a su clase concreta|`identificadorInterno: "MOL"`, `tipo: MOLINO`, `estadoOperativo: DISPONIBLE`, `pageable: PageRequest.of(0, 10)`, BD con 1 molino que los cumple|El service traduce `TipoEquipamiento.MOLINO` a `MolinoEntity.class` antes de invocar al repositorio|Retorna `Page<EquipamientoResumenResponseDTO>` con 1 fila: id, identificador, tipo `MOLINO` y estado operativo mapeados.|
|**CP-FR-02**|Criterios nulos no restringen la búsqueda|`identificadorInterno: null`, `tipo: null`, `estadoOperativo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga los tres criterios nulos tal cual al repositorio|Retorna las filas de los equipamientos de cualquier tipo y estado.|
|**CP-FR-03**|Equipamiento en uso con su etapa en curso|Un fermentador `EN_USO` con una etapa `EN_CURSO` (lote "L-014", etapa `FERMENTACION`)|La etapa `EN_CURSO` se informa como etapa en curso|La fila tiene `etapaEnCurso` con `idLote`, `identificadorInternoLote = "L-014"` y `etapa = FERMENTACION`, y `etapasPendientes` vacía.|
|**CP-FR-04**|Equipamiento libre con etapas pendientes|Un molino `DISPONIBLE` con 2 etapas `PENDIENTE` (lotes "L-020" y "L-021")|Las etapas `PENDIENTE` se informan como reservas a futuro|La fila tiene `etapaEnCurso = null` y `etapasPendientes` con las 2 participaciones, cada una con su lote y etapa.|
|**CP-FR-05**|Equipamiento sin participación|Un macerador `DISPONIBLE` sin ninguna etapa en curso ni pendiente|No hay etapas para ese equipamiento|La fila tiene `etapaEnCurso = null` y `etapasPendientes` vacía.|
|**CP-FR-06**|Etapas repartidas entre varios equipamientos|Página con un molino y un fermentador; la query de etapas devuelve etapas de ambos|Cada etapa se asigna al equipamiento al que pertenece|Cada fila muestra solo sus propias etapas. La query de etapas se invoca una sola vez, con los IDs de ambos equipamientos.|
|**CP-FR-07**|Una etapa en curso y otras pendientes del mismo equipamiento|Un macerador con una etapa `EN_CURSO` y una `PENDIENTE`|Cada etapa va a su lugar según su estado|`etapaEnCurso` tiene la etapa en curso y `etapasPendientes` solo la pendiente.|
|**CP-FR-08**|Consulta sin coincidencias|`identificadorInterno: "Inexistente"`, resto `null`|La página de equipamientos está vacía|Retorna una página vacía. No consulta las etapas (`verifyNoInteractions(etapaLoteRepository)`).|
