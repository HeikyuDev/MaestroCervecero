### 1. `filtrarSolicitudesBusqueda(Long idDespachoBarril, LocalDateTime fechaBusquedaDesde, LocalDateTime fechaBusquedaHasta, Boolean buscado, Pageable pageable)`

El rango de fecha de búsqueda es independiente por extremo (se puede acotar solo el "desde", solo el "hasta", o ninguno). Todos los criterios son opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FSB-01**|Filtra por los 4 criterios informados|`idDespachoBarril: 1L`, `fechaBusquedaDesde: 2026-02-01T00:00`, `fechaBusquedaHasta: 2026-02-28T23:59`, `buscado: false`, `pageable: PageRequest.of(0, 10)`, BD con 1 solicitud que cumple los cuatro criterios|`filtrarSolicitudesBusqueda(1L, ..., false, pageable)` contiene elementos|Retorna `Page<SolicitudBusquedaResponseDTO>` con 1 elemento mapeado.|
|**CP-FSB-02**|Los 4 parámetros nulos no restringen la búsqueda, incluyendo mezcla de `buscado`|`idDespachoBarril: null`, `fechaBusquedaDesde: null`, `fechaBusquedaHasta: null`, `buscado: null`, `pageable: PageRequest.of(0, 10)`, BD con 1 solicitud `buscado: true` y 1 `buscado: false`|El service propaga los 4 parámetros nulos tal cual al repositorio|Retorna `Page<SolicitudBusquedaResponseDTO>` con las 2 solicitudes, sin excluir ninguna por su valor de `buscado`.|
|**CP-FSB-03**|El usuario puede acotar explícitamente por `buscado`|`idDespachoBarril: null`, `fechaBusquedaDesde: null`, `fechaBusquedaHasta: null`, `buscado: true`, `pageable: PageRequest.of(0, 10)`, BD con 1 solicitud `buscado: true` y 1 `buscado: false`|`filtrarSolicitudesBusqueda(null, null, null, true, pageable)` contiene elementos|Retorna `Page<SolicitudBusquedaResponseDTO>` con 1 elemento (solo la `buscado: true`).|
|**CP-FSB-04**|Consulta sin coincidencias|`idDespachoBarril: null`, `fechaBusquedaDesde: 2030-01-01T00:00`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarSolicitudesBusqueda(null, 2030-01-01T00:00, null, null, pageable)` está vacío|Retorna `Page<SolicitudBusquedaResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FSB-05**|Filtra por un despacho de barril específico|`idDespachoBarril: 1L`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`, BD con 1 solicitud del despacho 1L|`filtrarSolicitudesBusqueda(1L, null, null, null, pageable)` contiene elementos|Retorna `Page<SolicitudBusquedaResponseDTO>` con la solicitud del despacho 1L.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Solicitud de búsqueda encontrada|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `SolicitudBusquedaResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Solicitud de búsqueda inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la solicitud de búsqueda con ID: 99".|

### 3. `registrarSolicitudBusqueda(SolicitudBusquedaFormDTO solicitudBusquedaFormDTO)`

La solicitud queda con `buscado = false` hasta que efectivamente se realice la búsqueda. `observaciones` es opcional (puede ser `null`).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RSB-01**|Fecha de búsqueda nula|`fechaBusqueda: null`, `observaciones: "Cliente prefiere horario de la tarde"`, `idDespachoBarril: 1L`|`fechaBusqueda == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de búsqueda es obligatoria"). No consulta el despacho.|
|**CP-RSB-02**|Fecha de búsqueda anterior a la fecha y hora actual|`fechaBusqueda: ahora - 1 día`, resto de los datos válidos|`fechaBusqueda.isBefore(LocalDateTime.now())` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La fecha de búsqueda no puede ser anterior a la fecha y hora actual"). No consulta el despacho.|
|**CP-RSB-03**|Despacho de barril no encontrado|`idDespachoBarril: 99L` (No existe), resto de los datos válidos|`despachoBarrilRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el despacho de barril con ID: 99"). No ejecuta `save()`.|
|**CP-RSB-04**|Registro exitoso _(Camino feliz)_|`idDespachoBarril: 1L` (Existe), `fechaBusqueda: ahora + 1 día`, `observaciones: "Cliente prefiere horario de la tarde"`|Todas las validaciones $\rightarrow$ **FALSE**|La solicitud se persiste con `fechaBusqueda`, `observaciones` y `despachoBarril` asignados, y `buscado = false`, y retorna DTO.|
|**CP-RSB-05**|Registro exitoso con observaciones nulas _(Camino feliz)_|`idDespachoBarril: 1L` (Existe), `fechaBusqueda: ahora + 1 día`, `observaciones: null`|Todas las validaciones $\rightarrow$ **FALSE**|La solicitud se persiste con `observaciones = null` sin lanzar ninguna excepción, y retorna DTO con `observaciones: null`.|
