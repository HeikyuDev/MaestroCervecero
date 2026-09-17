### 1. `filtrarEnvasadosLote(Long idEtapaLote, Long idBarril, EstadoTransaccion estado, Pageable pageable)`

`idEtapaLote` es **obligatorio**: no lo tipea el usuario, lo resuelve el Controller a partir del contexto de la pantalla de gestión de envasados — no tiene sentido mostrar envasados de otros lotes o de otras etapas mezclados.

`idBarril` es opcional, nulo = no filtra. `estado` sigue la misma regla que en `filtrarConsumosInsumo`/`filtrarMedicionesLote`: si no se especifica, el service asume `REGISTRADO` por defecto — nunca deja pasar `null` sin filtrar.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FEL-01**|Filtra por barril y estado informados|`idEtapaLote: 1L`, `idBarril: 3L`, `estado: REGISTRADO`, `pageable: PageRequest.of(0, 10)`, BD con 2 envasados que cumplen ambos criterios|`filtrarEnvasadosLote(1L, 3L, REGISTRADO, pageable)` contiene elementos|Retorna `Page<EnvasadoLoteResponseDTO>` con 2 elementos mapeados.|
|**CP-FEL-02**|Estado nulo asume REGISTRADO por defecto; idBarril nulo se propaga tal cual (no filtra)|`idEtapaLote: 1L`, `idBarril: null`, `estado: null`, `pageable: PageRequest.of(0, 10)`|El service reemplaza `estado: null` por `REGISTRADO` antes de llamar al repositorio|Se verifica que el repositorio se invoque con `REGISTRADO`, no con `null`, y con `idBarril: null`. Retorna `Page<EnvasadoLoteResponseDTO>` con los envasados registrados de esa etapa.|
|**CP-FEL-03**|El usuario puede elegir explícitamente ver los envasados anulados|`idEtapaLote: 1L`, `idBarril: null`, `estado: ANULADO`, `pageable: PageRequest.of(0, 10)`, 1 envasado ANULADO en BD|`filtrarEnvasadosLote(1L, null, ANULADO, pageable)` contiene elementos|Retorna `Page<EnvasadoLoteResponseDTO>` con 1 elemento.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Envasado de lote encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `EnvasadoLoteResponseDTO` con los datos de la entidad, incluyendo la etapa de lote y el barril mapeados.|
|**CP-BI-02**|Envasado de lote inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el envasado de lote con ID: 99".|

### 3. `registrarEnvasadoLote(Long idEtapaLote, RegistrarEnvasadoLoteFormDTO registrarEnvasadoLoteFormDTO)`

El barril se busca bloqueado para escritura (`buscarPorIdParaCambiarEstadoOperativo`), para que dos envasados registrados en simultáneo no puedan tomar el mismo barril. Ese bloqueo no es observable en un test unitario con Mockito (no simula locks de base de datos); lo que sí se puede y debe verificar es que el service invoque ese método del repositorio, y no el `findById` sin lock.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-REL-01**|Etapa de lote no encontrada|`idEtapaLote: 99L` (No existe)|`etapaLoteRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la etapa de lote con ID: 99". `verifyNoInteractions(barrilRepository, envasadoLoteRepository)`.|
|**CP-REL-02**|Lote no está EN_EJECUCION|Etapa de lote cuyo `lote.estado: PENDIENTE`|`lote.getEstado() != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El lote debe encontrarse en estado EN_EJECUCION para poder registrar un envasado"). `verifyNoInteractions(barrilRepository, envasadoLoteRepository)`.|
|**CP-REL-03**|Etapa no está EN_CURSO|Lote `EN_EJECUCION`, etapa (de tipo Envasado) con `estado: PENDIENTE`|`etapaLote.getEstado() != EN_CURSO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden registrar envasados sobre la etapa actualmente en curso del lote"). `verifyNoInteractions(barrilRepository, envasadoLoteRepository)`.|
|**CP-REL-04**|Tipo de etapa no permite registrar envasado|Lote `EN_EJECUCION`, etapa `EN_CURSO` de tipo `MADURACION`|`tipoEtapa.isPermiteRegistrarEnvasado()` $\rightarrow$ **FALSE**|Lanza `ReglaNegocioException` ("La etapa MADURACION no admite el registro de envasados"). `verifyNoInteractions(barrilRepository, envasadoLoteRepository)`.|
|**CP-REL-05**|Barril no encontrado|Etapa válida (Envasado, EN_CURSO), `idBarril: 99L` (No existe)|`barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el barril con ID: 99". No ejecuta `save()` en ningún repositorio.|
|**CP-REL-06**|Barril no está en estado operativo DISPONIBLE|Barril existente con `estadoOperativo: EN_LIMPIEZA`|`barril.getEstadoOperativo() != DISPONIBLE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El barril debe encontrarse en estado operativo DISPONIBLE para poder envasar en él"). No ejecuta `save()`.|
|**CP-REL-07**|Cantidad a envasar nula|Barril `DISPONIBLE`, `cantidad: null`|`cantidad == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad a envasar debe ser mayor a cero"). No ejecuta `save()`.|
|**CP-REL-08**|Cantidad a envasar en cero _(Límite)_|Barril `DISPONIBLE`, `cantidad: 0.0`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` (mismo mensaje que CP-REL-07: "La cantidad a envasar debe ser mayor a cero"). No ejecuta `save()`.|
|**CP-REL-09**|Cantidad a envasar negativa|Barril `DISPONIBLE`, `cantidad: -5.0`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` (mismo mensaje que CP-REL-07). No ejecuta `save()`.|
|**CP-REL-10**|Cantidad a envasar supera la capacidad del barril|Barril `DISPONIBLE` con `capacidad: 50.0`, `cantidad: 60.0`|`cantidad > capacidad` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad a envasar no puede superar la capacidad del barril seleccionado"). No ejecuta `save()`.|
|**CP-REL-11**|Cantidad a envasar igual a la capacidad del barril _(Límite, permitido)_|Barril `DISPONIBLE` con `capacidad: 50.0`, `cantidad: 50.0`|`cantidad > capacidad` $\rightarrow$ **FALSE**|Persiste el envasado con `cantidadEnvasada = 50.0`, `estado = REGISTRADO`. El barril queda con `estadoOperativo = CON_CERVEZA` y `contenidoActual = 50.0`.|
|**CP-REL-12**|Registro exitoso _(Camino feliz)_|Etapa de Envasado `EN_CURSO` de un lote `EN_EJECUCION`; barril `DISPONIBLE` con `capacidad: 50.0`; `cantidad: 30.0`|Todas las validaciones $\rightarrow$ **FALSE**|El barril se persiste con `estadoOperativo = CON_CERVEZA` y `contenidoActual = 30.0`. Se persiste un nuevo `EnvasadoLoteEntity` con `cantidadEnvasada = 30.0`, `estado = REGISTRADO`, la etapa y el barril asociados, y retorna DTO.|

### 4. `anularEnvasadoLote(Long id, AnularEnvasadoLoteFormDTO anularEnvasadoLoteFormDTO)`

Revierte el traspaso: el barril vuelve a `DISPONIBLE` con `contenidoActual = 0.0`. Antes de revertir, valida que el barril siga reflejando exactamente lo que este envasado registró (`estadoOperativo == CON_CERVEZA` y `contenidoActual == cantidadEnvasada`) — si no coincide, alguna otra operación ya modificó el barril desde entonces y ya no es seguro deshacer este envasado puntual. El barril también se recupera bloqueado (`buscarPorIdParaCambiarEstadoOperativo`), igual que al registrar.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AEL-01**|Motivo de anulación nulo|`motivoAnulacion: null`|`motivoAnulacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio"). `verifyNoInteractions(envasadoLoteRepository, barrilRepository)`.|
|**CP-AEL-02**|Motivo de anulación vacío (solo espacios)|`motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` (mismo mensaje que CP-AEL-01: "El motivo de anulación es obligatorio"). Sin interacciones con los repositorios.|
|**CP-AEL-03**|Envasado de lote no encontrado|`id: 99L` (No existe)|`envasadoLoteRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el envasado de lote con ID: 99"). No consulta `barrilRepository`, ni ejecuta `save()`.|
|**CP-AEL-04**|Envasado ya ANULADO|Envasado existente con `estado: ANULADO`|`estado != REGISTRADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden anular envasados en estado REGISTRADO"). No consulta `barrilRepository`, ni ejecuta `save()`.|
|**CP-AEL-05**|Lote asociado no está EN_EJECUCION|Envasado `REGISTRADO`, lote asociado con `estado: FINALIZADO`|`lote.getEstado() != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El lote debe encontrarse en estado EN_EJECUCION para poder anular un envasado"). No consulta `barrilRepository`, ni ejecuta `save()`.|
|**CP-AEL-06**|El barril asociado ya no existe|Envasado `REGISTRADO`, `barril.id: 5L`, lote `EN_EJECUCION`; `barrilRepository.buscarPorIdParaCambiarEstadoOperativo(5L)` $\rightarrow$ **Optional.empty()**|El barril referenciado por el envasado ya no está activo|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el barril con ID: 5". No ejecuta `save()`.|
|**CP-AEL-07**|El barril ya no está en estado operativo CON_CERVEZA|Envasado `REGISTRADO` con `cantidadEnvasada: 30.0`; barril con `estadoOperativo: DESPACHADO`, `contenidoActual: 30.0`|`barril.getEstadoOperativo() != CON_CERVEZA` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("No se puede anular el envasado porque el barril asociado ya no mantiene su contenido intacto"). No ejecuta `save()`.|
|**CP-AEL-08**|El contenido actual del barril ya no coincide con la cantidad envasada|Envasado `REGISTRADO` con `cantidadEnvasada: 30.0`; barril con `estadoOperativo: CON_CERVEZA`, `contenidoActual: 30.0` pero modificado a `contenidoActual: 45.0` por otra operación|`!barril.getContenidoActual().equals(cantidadEnvasada)` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` (mismo mensaje que CP-AEL-07). No ejecuta `save()`.|
|**CP-AEL-09**|Anulación exitosa _(Camino feliz)_|Envasado `REGISTRADO` con `cantidadEnvasada: 30.0`; barril con `estadoOperativo: CON_CERVEZA`, `contenidoActual: 30.0`|Todas las validaciones $\rightarrow$ **FALSE**|El barril se persiste con `estadoOperativo = DISPONIBLE` y `contenidoActual = 0.0`. El envasado queda `ANULADO`, con `fechaAnulacion` seteada (justo ahora) y `motivoAnulacion` guardado.|
