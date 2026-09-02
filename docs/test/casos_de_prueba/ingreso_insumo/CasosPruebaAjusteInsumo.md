### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes|`pageable: PageRequest.of(0, 10)`, BD con 3 ajustes de insumo|`findAll(pageable)` contiene elementos|Retorna `Page<AjusteInsumoResponseDTO>` con 3 elementos mapeados.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<AjusteInsumoResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Ajuste de insumo encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `AjusteInsumoResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Ajuste de insumo inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el ajuste de insumo con ID: 99".|

### 3. `registrarAjusteInsumo(AjusteInsumoFormDTO ajusteInsumoFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RA-01**|Motivo de ajuste no encontrado|`idMotivoAjuste: 99L` (No existe), `idLoteInsumo: 1L`, `cantidad: 10.0`|`motivoAjusteRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta el lote de insumo ni ejecuta `save()`.|
|**CP-RA-02**|Lote de insumo no encontrado|`idMotivoAjuste: 1L` (Existe), `idLoteInsumo: 99L` (No existe), `cantidad: 10.0`|`loteInsumoRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-RA-03**|Cantidad nula|`idMotivoAjuste: 1L`, `idLoteInsumo: 1L`, `cantidad: null`|`cantidad == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad debe ser mayor a cero"). No ejecuta `save()`.|
|**CP-RA-04**|Cantidad en cero _(Límite)_|`idMotivoAjuste: 1L`, `idLoteInsumo: 1L`, `cantidad: 0.0`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-RA-05**|Cantidad negativa|`idMotivoAjuste: 1L`, `idLoteInsumo: 1L`, `cantidad: -5.0`|`cantidad <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-RA-06**|Motivo EGRESO con cantidad mayor a la disponible del lote|Motivo `tipoAjuste: EGRESO`, lote con `cantidadActual: 10.0`, `cantidadReservada: 0.0` (disponible 10.0), `cantidad: 15.0`|`cantidad > cantidadDisponible` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La cantidad a descontar no puede superar la cantidad disponible del lote de insumo"). No guarda el lote ni el ajuste.|
|**CP-RA-07**|Motivo EGRESO con cantidad igual a la disponible _(Límite, no debe quedar negativo)_|Motivo `tipoAjuste: EGRESO`, lote con `cantidadActual: 10.0`, `cantidadReservada: 0.0`, `cantidad: 10.0`|`cantidad > cantidadDisponible` $\rightarrow$ **FALSE**|El lote queda con `cantidadActual = 0.0`. Persiste el ajuste en estado `REGISTRADO` y retorna DTO.|
|**CP-RA-08**|Motivo INGRESO no valida contra la cantidad disponible|Motivo `tipoAjuste: INGRESO`, lote con `cantidadActual: 5.0`, `cantidadReservada: 3.0` (disponible 2.0), `cantidad: 100.0`|El ajuste es de tipo INGRESO: no se evalúa `cantidad > cantidadDisponible`|El lote queda con `cantidadActual = 105.0`. Persiste el ajuste en estado `REGISTRADO` y retorna DTO (una suma nunca deja saldo negativo).|
|**CP-RA-09**|Registro exitoso con motivo INGRESO _(Camino feliz)_|Motivo `tipoAjuste: INGRESO`, lote con `cantidadActual: 20.0`, `cantidad: 5.0`, `observacion: "Corrección de conteo físico"`|Todas las validaciones $\rightarrow$ **FALSE**|El lote queda con `cantidadActual = 25.0`. Persiste el ajuste con `cantidad`, `observacion`, `motivoAjuste`, `loteInsumo` y `estado = REGISTRADO` asignados, y retorna DTO.|
|**CP-RA-10**|Registro exitoso con motivo EGRESO _(Camino feliz)_|Motivo `tipoAjuste: EGRESO`, lote con `cantidadActual: 20.0`, `cantidad: 5.0`, `observacion: "Rotura de lote"`|Todas las validaciones $\rightarrow$ **FALSE**|El lote queda con `cantidadActual = 15.0`. Persiste el ajuste en estado `REGISTRADO` y retorna DTO.|

### 4. `anularAjusteInsumo(Long id, AnularAjusteInsumoFormDTO anularAjusteInsumoFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AA-01**|Motivo de anulación nulo|`id: 1L`, `motivoAnulacion: null`|`motivoAnulacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio"). No consulta el repositorio (`verifyNoInteractions`).|
|**CP-AA-02**|Motivo de anulación en blanco|`id: 1L`, `motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No consulta el repositorio.|
|**CP-AA-03**|Ajuste de insumo no encontrado|`id: 99L` (No existe), `motivoAnulacion: "Error de carga"`|`ajusteInsumoRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AA-04**|Ajuste ya anulado|`id: 1L` (Existe, `estado: ANULADO`), `motivoAnulacion: "Error de carga"`|`estado != REGISTRADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden anular ajustes de insumo en estado REGISTRADO"). No ejecuta `save()`.|
|**CP-AA-05**|Anular un ajuste INGRESO cuya cantidad supera la disponible del lote|Ajuste `REGISTRADO`, motivo `tipoAjuste: INGRESO`, `cantidad: 20.0`, lote con `cantidadActual: 15.0`, `cantidadReservada: 0.0` (disponible 15.0)|`cantidad > cantidadDisponible` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("No se puede anular el ajuste: su cantidad supera la cantidad disponible del lote de insumo"). No guarda el lote ni el ajuste.|
|**CP-AA-06**|Anular un ajuste INGRESO exitosamente|Ajuste `REGISTRADO`, motivo `tipoAjuste: INGRESO`, `cantidad: 10.0`, lote con `cantidadActual: 15.0`, `cantidadReservada: 0.0` (disponible 15.0)|`cantidad > cantidadDisponible` $\rightarrow$ **FALSE**|El lote queda con `cantidadActual = 5.0`. El ajuste pasa a `estado = ANULADO`, con `fechaAnulacion` y `motivoAnulacion` seteados, y retorna DTO.|
|**CP-AA-07**|Anular un ajuste EGRESO exitosamente _(Nunca queda negativo)_|Ajuste `REGISTRADO`, motivo `tipoAjuste: EGRESO`, `cantidad: 10.0`, lote con `cantidadActual: 5.0`|El ajuste anulado era de tipo EGRESO: no se evalúa `cantidad > cantidadDisponible`|El lote queda con `cantidadActual = 15.0`. El ajuste pasa a `estado = ANULADO` y retorna DTO.|
