### 1. `filtrarOrdenesCompra(Long idPlanificacionProduccion, Long idProveedor, EstadoSolicitud estado, LocalDate fechaEntregaEstimada, Pageable pageable)`

`idProveedor` no vive directamente en `OrdenCompraEntity`: se resuelve a través de la relación `versionProveedor.proveedor`, y considera cualquier versión (histórica o activa) del proveedor. Todos los criterios son coincidencia exacta y opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FOC-01**|Filtra por los 4 criterios informados|`idPlanificacionProduccion: 1L`, `idProveedor: 2L`, `estado: PENDIENTE`, `fechaEntregaEstimada: 2026-01-15`, `pageable: PageRequest.of(0, 10)`, BD con 1 orden que cumple los cuatro criterios|`filtrarOrdenesCompra(1L, 2L, PENDIENTE, 2026-01-15, pageable)` contiene elementos|Retorna `Page<OrdenCompraResponseDTO>` con 1 elemento mapeado.|
|**CP-FOC-02**|Los 4 parámetros nulos no restringen la búsqueda|`idPlanificacionProduccion: null`, `idProveedor: null`, `estado: null`, `fechaEntregaEstimada: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 órdenes en distintos estados|El service propaga los 4 parámetros nulos tal cual al repositorio|Retorna `Page<OrdenCompraResponseDTO>` con las 3 órdenes (equivalente a no filtrar).|
|**CP-FOC-03**|Consulta sin coincidencias|`idPlanificacionProduccion: 99L`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarOrdenesCompra(99L, null, null, null, pageable)` está vacío|Retorna `Page<OrdenCompraResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Orden de compra encontrada|`id: 1L` (Existe en BD)|`findById(1L)` → **Presente**|Retorna `OrdenCompraResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Orden de compra inexistente|`id: 99L` (No existe en BD)|`findById(99L)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró la orden de compra con ID: 99".|

### 3. `registrarOrdenCompra(OrdenCompraFormDTO ordenCompraFormDTO)`

La orden siempre está impulsada por una planificación de producción `PENDIENTE`: solo se pueden solicitar ítems del catálogo del proveedor seleccionado (última versión activa) cuyo insumo forme parte de la versión de receta asociada a esa planificación. El costo unitario se define recién en esta transacción, no lo fija el catálogo.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-ROC-01**|Fecha de entrega estimada nula|`fechaEntregaEstimada: null`|`fechaEntregaEstimada == null` → **TRUE**|Lanza `ReglaNegocioException` ("La fecha de entrega estimada no puede ser anterior a la fecha actual"). No consulta ningún repositorio (`verifyNoInteractions`).|
|**CP-ROC-02**|Fecha de entrega estimada anterior a la fecha actual|`fechaEntregaEstimada: LocalDate.now().minusDays(1)`|`fechaEntregaEstimada.isBefore(now())` → **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-ROC-03**|Cantidad de un ítem nula|`detallesCompra: [{cantidad: null, costoUnitario: 10, idCatalogoProveedor: 5L}]`|`cantidad == null` → **TRUE**|Lanza `ReglaNegocioException` ("La cantidad de cada ítem debe ser mayor a cero"). No consulta ningún repositorio.|
|**CP-ROC-04**|Cantidad de un ítem en cero _(Límite)_|`detallesCompra: [{cantidad: 0, costoUnitario: 10, idCatalogoProveedor: 5L}]`|`cantidad <= 0` → **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-ROC-05**|Costo unitario de un ítem nulo|`detallesCompra: [{cantidad: 10, costoUnitario: null, idCatalogoProveedor: 5L}]`|`costoUnitario == null` → **TRUE**|Lanza `ReglaNegocioException` ("El costo unitario de cada ítem debe ser mayor a cero"). No consulta ningún repositorio.|
|**CP-ROC-06**|Costo unitario de un ítem en cero _(Límite)_|`detallesCompra: [{cantidad: 10, costoUnitario: 0, idCatalogoProveedor: 5L}]`|`costoUnitario.signum() <= 0` → **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-ROC-07**|Planificación de producción no encontrada|`idPlanificacionProduccion: 99L` (No existe)|`planificacionProduccionRepository.findById(99L)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la planificación de producción con ID: 99"). No consulta proveedor ni catálogo, ni ejecuta `save()`.|
|**CP-ROC-08**|Planificación de producción no está en estado PENDIENTE|`idPlanificacionProduccion: 1L` (Existe, `estado: FINALIZADA`)|`estado != PENDIENTE` → **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden asociar órdenes de compra a planificaciones de producción en estado PENDIENTE"). No consulta proveedor ni catálogo, ni ejecuta `save()`.|
|**CP-ROC-09**|Proveedor no encontrado|`idProveedor: 99L` (No existe)|`proveedorRepository.findById(99L)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el proveedor con ID: 99"). No consulta catálogo, ni ejecuta `save()`.|
|**CP-ROC-10**|Proveedor sin versión activa|`idProveedor: 2L` (Existe, ninguna versión con `esUltimaVersion: true`)|`buscarVersionActiva(...)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró una versión activa para el proveedor con ID: 2"). No consulta catálogo, ni ejecuta `save()`.|
|**CP-ROC-11**|Ítem de catálogo no encontrado|`idCatalogoProveedor: 99L` (No existe)|`catalogoProveedorRepository.findById(99L)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró el ítem de catálogo con ID: 99"). No ejecuta `save()`.|
|**CP-ROC-12**|Ítem de catálogo no pertenece al proveedor seleccionado|`idCatalogoProveedor: 5L`, cuya `version.id` (999L) no coincide con la versión activa del proveedor seleccionado (3L)|`catalogo.version.id != versionActiva.id` → **TRUE**|Lanza `ReglaNegocioException` ("El ítem de catálogo con ID 5 no pertenece al proveedor seleccionado"). No ejecuta `save()`.|
|**CP-ROC-13**|Insumo del ítem no forma parte de la versión de receta|Catálogo ofrece la malta 8L; la receta de la planificación solo planifica la malta 7L|`idsInsumosDeReceta.contains(8L)` → **FALSE**|Lanza `ReglaNegocioException` ("El insumo del ítem de catálogo con ID 5 no forma parte de la versión de receta de la planificación de producción seleccionada"). No ejecuta `save()`.|
|**CP-ROC-14**|Registro exitoso _(Camino feliz)_|Fecha válida, 1 ítem con `cantidad: 10`, `costoUnitario: 10`, catálogo perteneciente al proveedor y cuyo insumo (malta 7L) forma parte de la receta de la planificación|Todas las validaciones → **FALSE**|La orden se persiste en `estado = PENDIENTE`, con el detalle de compra construido (`cantidad`, `costoUnitario` y referencia a la orden) y retorna `OrdenCompraResponseDTO`.|

### 4. `anularOrdenCompra(Long id, AnulacionOrdenCompraFormDTO anulacionFormDTO)`

Solo procede sobre órdenes en estado `PENDIENTE`, y solo si no tiene ningún ingreso de insumo en estado `REGISTRADO` asociado a alguno de sus ítems de detalle de compra: la anulación corresponde a un error de carga (ítem olvidado, cantidad mal solicitada, etc.), no a una compra que ya empezó a recibirse.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AOC-01**|Motivo de anulación nulo|`id: 1L`, `motivoAnulacion: null`|`motivoAnulacion == null` → **TRUE**|Lanza `ReglaNegocioException` ("El motivo de anulación es obligatorio"). No consulta ningún repositorio (`verifyNoInteractions`).|
|**CP-AOC-02**|Motivo de anulación en blanco|`id: 1L`, `motivoAnulacion: "   "`|`motivoAnulacion.isBlank()` → **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-AOC-03**|Orden de compra no encontrada|`id: 99L` (No existe), `motivoAnulacion: "Error de carga"`|`ordenCompraRepository.findById(99L)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la orden de compra con ID: 99"). No ejecuta `save()`.|
|**CP-AOC-04**|Orden que no está en estado PENDIENTE|`id: 1L` (Existe, `estado: FINALIZADA`), `motivoAnulacion: "Error de carga"`|`estado != PENDIENTE` → **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden anular órdenes de compra en estado PENDIENTE"). No consulta `ingresoInsumoRepository` (`verifyNoInteractions`) ni ejecuta `save()`.|
|**CP-AOC-05**|Orden con un ingreso de insumo REGISTRADO asociado|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoAnulacion: "Error de carga"`, `ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, REGISTRADO)` → **true**|Existe al menos un ingreso REGISTRADO asociado|Lanza `ReglaNegocioException` ("No se puede anular la orden de compra porque tiene al menos un ingreso de insumo en estado REGISTRADO asociado"). No ejecuta `save()`.|
|**CP-AOC-06**|Anulación exitosa sin ingresos de insumo REGISTRADO asociados _(Camino feliz)_|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoAnulacion: "Error de carga"`, `ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, REGISTRADO)` → **false**|Todas las validaciones → **FALSE**|La orden pasa a `estado = ANULADA`, con `fechaAnulacion` y `motivoAnulacion` seteados, se persiste (`save()`) y retorna `OrdenCompraResponseDTO`.|

### 5. `finalizarOrdenCompra(Long id, FinalizacionForzadaOrdenCompraFormDTO finalizacionFormDTO)`

Solo procede sobre órdenes en estado `PENDIENTE`, y solo si tiene **al menos un** ingreso de insumo en estado `REGISTRADO` asociado a alguno de sus ítems de detalle de compra (condición inversa a `anularOrdenCompra`): la finalización forzada cierra una orden que sí llegó a recibir algo pero no se puede completar (cantidad ingresada menor a la solicitada, proveedor discontinuado, etc.); si no recibió nada, corresponde anularla en lugar de finalizarla.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FZOC-01**|Motivo de finalización nulo|`id: 1L`, `motivoFinalizacion: null`|`motivoFinalizacion == null` → **TRUE**|Lanza `ReglaNegocioException` ("El motivo de finalización es obligatorio"). No consulta ningún repositorio (`verifyNoInteractions`).|
|**CP-FZOC-02**|Motivo de finalización en blanco|`id: 1L`, `motivoFinalizacion: "   "`|`motivoFinalizacion.isBlank()` → **TRUE**|Lanza `ReglaNegocioException`. No consulta ningún repositorio.|
|**CP-FZOC-03**|Orden de compra no encontrada|`id: 99L` (No existe), `motivoFinalizacion: "Proveedor discontinuado"`|`ordenCompraRepository.findById(99L)` → **Optional.empty()**|Lanza `RecursoNoEncontradoException` ("No se encontró la orden de compra con ID: 99"). No ejecuta `save()`.|
|**CP-FZOC-04**|Orden que no está en estado PENDIENTE|`id: 1L` (Existe, `estado: ANULADA`), `motivoFinalizacion: "Proveedor discontinuado"`|`estado != PENDIENTE` → **TRUE**|Lanza `ReglaNegocioException` ("Solo se pueden finalizar órdenes de compra en estado PENDIENTE"). No consulta `ingresoInsumoRepository` (`verifyNoInteractions`) ni ejecuta `save()`.|
|**CP-FZOC-05**|Orden sin ningún ingreso de insumo REGISTRADO asociado|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoFinalizacion: "Proveedor discontinuado"`, `ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, REGISTRADO)` → **false**|No existe ningún ingreso REGISTRADO asociado|Lanza `ReglaNegocioException` ("No se puede finalizar la orden de compra porque no tiene ningún ingreso de insumo en estado REGISTRADO asociado; corresponde anularla en su lugar"). No ejecuta `save()`.|
|**CP-FZOC-06**|Finalización forzada exitosa con al menos un ingreso de insumo REGISTRADO asociado _(Camino feliz)_|`id: 1L` (Existe, `estado: PENDIENTE`), `motivoFinalizacion: "Cantidad ingresada menor a la solicitada"`, `ingresoInsumoRepository.existsByDetalleCompra_OrdenCompra_IdAndEstado(1L, REGISTRADO)` → **true**|Todas las validaciones → **FALSE**|La orden pasa a `estado = FINALIZADA`, con `fechaFinalizacion` y `motivoFinalizacion` seteados, se persiste (`save()`) y retorna `OrdenCompraResponseDTO`.|
