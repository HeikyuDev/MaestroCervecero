## Casos de prueba de `ControlBarrilServicioImpl`

**Tipo de prueba:** unitaria con repositorios simulados. Clase `ControlBarrilServicioImplTest`. Es un servicio de solo lectura, sin locks: cada consulta usa un número fijo de consultas sin importar cuántos barriles u operaciones haya.

### 1. `filtrarBarrilesParaControl(identificador, idFabricante, estadoOperativo, pageable)`

Devuelve los barriles activos con su fabricante (cargado junto con el barril), estado y usos desde el último mantenimiento, en orden natural por identificador (lo resuelve el repositorio). Los usos de toda la página se obtienen con una consulta por lista de IDs. Los barriles Con cerveza informan además su contenido: el lote de su último envasado registrado (el de mayor fecha de envasado) y el nombre de la receta de ese lote, resueltos para todos los de la página con una consulta por lista de IDs; los demás barriles informan contenido nulo.

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-CBS-01**|Arma los datos con una consulta de usos|Página con dos barriles, uno con 7 usos|El primero informa 7 usos, máximo 10 y fabricante; el segundo 0 usos; `contarUsosDesdeUltimoMantenimiento` se invoca una vez.|
|**CP-CBS-02**|Página vacía|El repositorio devuelve una página sin barriles|Retorna una página vacía y no consulta usos.|
|**CP-CBS-03**|Normaliza el identificador|`"  brl   01  "` y `"    "`|Consulta con `"BRL 01"` y con `null`.|
|**CP-CBS-22**|Escapa los comodines del identificador|`"_"`|Consulta con `"!_"` para compararlo de forma literal.|
|**CP-CBS-23**|Contenido de los barriles Con cerveza|Página con tres barriles Con cerveza y uno Disponible|Los Con cerveza informan el lote (ID y código) y la receta de su último envasado registrado; el Disponible informa contenido nulo. `buscarLoteDelUltimoEnvasadoRegistrado` se invoca una vez, con los IDs de los tres barriles Con cerveza.|
|**CP-CBS-24**|Sin barriles Con cerveza|Página con un barril En uso y uno Disponible|Todos informan contenido nulo y no se consulta el contenido.|
|**CP-CBS-27**|Envasados con igual fecha|La consulta devuelve dos filas del mismo barril (mayor ID primero)|Informa el contenido de la primera fila, la del envasado de mayor ID.|

### 2. `buscarResumen(idBarril)`

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-CBS-04**|Resumen completo|Barril con mantenimiento previo, 4 usos y última limpieza|Informa datos, 4 usos, máximo 10 y las dos fechas.|
|**CP-CBS-05**|Sin operaciones|Sin limpiezas ni mantenimientos|0 usos, fechas nulas y conteo sin fecha de corte.|
|**CP-CBS-25**|Contenido de un barril Con cerveza|Barril Con cerveza cuyo último envasado registrado es de un lote de la receta Pale Ale|Informa el ID y el código del lote y el nombre de la receta.|
|**CP-CBS-26**|Barril que no está Con cerveza|Barril Disponible|Informa contenido nulo y no consulta el contenido.|
|**CP-CBS-06**|Barril inexistente|`buscarPorIdIncluyendoDadosDeBaja` vacío|Lanza `RecursoNoEncontradoException` ("No se encontró el barril con ID: 99").|

### 3. `listarOperaciones(idBarril, tipo)`

Lista las operaciones del tipo pedido, anuladas incluidas, de la más reciente a la más antigua. `anulable` es verdadero solo en la última operación registrada del barril (cualquier tipo), si el barril no está En uso y la operación es posterior al último envasado (`MetodosCicloVida.estaEnUso` y `esPosteriorAlUltimoEnvasado`, la misma definición de la anulación).

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-CBS-07**|Solo la última es anulable|Tres limpiezas (última registrada, una anulada, una anterior)|`[true, false, false]`; informa quién registró y el estado resultante.|
|**CP-CBS-08**|Barril En uso|La última operación existe|Ninguna es anulable.|
|**CP-CBS-09**|No posterior al envasado|Última operación con la fecha del último envasado|No es anulable.|
|**CP-CBS-10**|Datos propios de cada tipo|Fraccionamiento, despacho y devolución|Cantidad extraída; cliente y fecha estimada; litros descartados. Solo consulta el repositorio de cada tipo.|
|**CP-CBS-11**|Fallas y mantenimientos|Un registro de cada uno|Usa sus repositorios y el tipo correcto.|
|**CP-CBS-12**|Barril inexistente|`buscarPorIdIncluyendoDadosDeBaja` vacío|Lanza `RecursoNoEncontradoException`.|

### 4. `listarHistorial(idBarril, tipo, desde, hasta, pageable)`

Una fila por envasado registrado (con su lote, sin una consulta por fila) y por operación registrada (nunca anuladas), de la más reciente a la más antigua; ante igual fecha, por tipo y por ID descendente. Las dos fuentes se unen en memoria. Estado resultante: limpieza y fraccionamiento el registrado; falla En mantenimiento; mantenimiento Disponible; despacho Despachado; devolución En limpieza; envasado En uso si el lote está en ejecución y Con cerveza si está finalizado.

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-CBS-13**|Unión ordenada con estado resultante|Dos envasados y seis operaciones|Orden por fecha descendente y el estado resultante de cada fila; una consulta por fuente.|
|**CP-CBS-14**|Filtro por tipo|Envasado y Falla|Envasado no consulta las operaciones; otro tipo no consulta los envasados.|
|**CP-CBS-15**|Paginación|25 operaciones, página 2|Total 25 y 5 filas.|
|**CP-CBS-16**|Validaciones|`hasta` anterior a `desde`; barril inexistente|`ReglaNegocioException` ("La fecha hasta no puede ser anterior a la fecha desde") y `RecursoNoEncontradoException`.|

### 5. Barril dado de baja

Un barril dado de baja se trata como inexistente en las tres consultas por barril, igual que en los servicios de anulación, para que no se pueda abrir por URL.

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-CBS-17**|Resumen de un barril dado de baja|`buscarPorIdIncluyendoDadosDeBaja` devuelve un barril con `estado: BAJA`|Devuelve el resumen con sus usos y fechas, `estado: BAJA` e `isDadoDeBaja() = true`, para sus pantallas de solo lectura.|
|**CP-CBS-18**|Operaciones de un barril dado de baja|Ídem, `listarOperaciones` con una limpieza `REGISTRADO` que es la última operación|Lista la operación y ninguna es anulable: un barril dado de baja no admite anulaciones.|
|**CP-CBS-19**|Historial de un barril dado de baja|Ídem, `listarHistorial`|Lista el historial igual que el de un barril activo.|

### 6. Estado operativo del barril y `anulable`

Además de ser la última operación registrada, con el barril fuera de uso y posterior al último envasado, una operación es anulable solo si el estado operativo actual del barril es el que ella dejó, el mismo criterio que exigen los servicios de anulación: limpieza y fraccionamiento, el estado resultante que registraron; falla, En mantenimiento; mantenimiento, Disponible; despacho, Despachado; devolución, En limpieza (`MapperControlBarril.estadoOperativoResultante`).

|**ID**|**Nombre del Caso**|**Escenario**|**Resultado Esperado**|
|---|---|---|---|
|**CP-CBS-20**|Estado del barril distinto del resultante|Última limpieza (resultante Disponible) con el barril En limpieza|No es anulable.|
|**CP-CBS-21**|Estado resultante de cada tipo|Falla, mantenimiento, despacho, devolución y fraccionamiento, cada uno con el barril en su estado resultante y con Con cerveza|Anulable solo con su estado resultante.|
