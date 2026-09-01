### 1. `buscarTodos(Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BT-01**|Consulta con registros existentes (mezcla de estados)|`pageable: PageRequest.of(0, 10)`, BD con 2 ingresos `REGISTRADO` y 1 `ANULADO`|`findAll(pageable)` contiene elementos|Retorna `Page<IngresoInsumoResponseDTO>` con 3 elementos mapeados. A diferencia de la baja lógica del resto de los módulos, los ingresos `ANULADO` no se excluyen de la búsqueda.|
|**CP-BT-02**|Consulta sin registros existentes|`pageable: PageRequest.of(0, 10)`, BD vacía|`findAll(pageable)` está vacío|Retorna `Page<IngresoInsumoResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Ingreso de insumo encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `IngresoInsumoResponseDTO` con los datos del ingreso.|
|**CP-BI-02**|Ingreso de insumo inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el ingreso de insumo con ID: 99".|

### 3. `registrarIngresoInsumoPorCompra(IngresoInsumoPorCompraFormDTO ingresoInsumoPorCompraFormDTO)`

Se concentran las reglas de negocio del ingreso originado en una orden de compra. En todos los casos de falla, el resto de los datos del DTO son válidos.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RC-01**|Ítem de detalle de compra inexistente|`idDetalleCompra: 99L` (No existe en BD)|`detalleCompraRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el ítem de detalle de compra con ID: 99". No consulta nada más.|
|**CP-RC-02**|Orden de compra del ítem no está en estado PENDIENTE|Detalle de compra existente cuya orden de compra está en estado `FINALIZADA`|`detalleCompraEntity.getOrdenCompra().getEstado() != PENDIENTE`|Lanza `ReglaNegocioException` con mensaje "Solo se pueden registrar ingresos de ítems de órdenes de compra en estado PENDIENTE". No persiste.|
|**CP-RC-03**|Cantidad recibida nula|`cantidadRecibida: null`|`cantidadRecibida == null`|Lanza `ReglaNegocioException` con mensaje "La cantidad recibida debe ser mayor a cero". No persiste.|
|**CP-RC-04**|Cantidad recibida menor o igual a cero|`cantidadRecibida: 0.0`|`cantidadRecibida <= 0`|Lanza `ReglaNegocioException` con mensaje "La cantidad recibida debe ser mayor a cero". No persiste.|
|**CP-RC-05**|Cantidad recibida supera la cantidad pendiente de entrega del ítem|Ítem con `cantidad: 100`, sin ingresos previos, `cantidadRecibida: 150.0`|`cantidadRecibida > cantidadPendiente (100)`|Lanza `ReglaNegocioException` con mensaje "La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (100.0)". No persiste.|
|**CP-RC-06**|Cálculo de pendiente descuenta ingresos `REGISTRADO` previos pero ignora los `ANULADO`|Ítem con `cantidad: 100`; un ingreso previo `REGISTRADO` de 40 y uno `ANULADO` de 30; `cantidadRecibida: 65.0`|Pendiente = 100 − 40 (el `ANULADO` no descuenta) = 60; `65 > 60`|Lanza `ReglaNegocioException` con mensaje "La cantidad recibida no puede superar la cantidad pendiente de entrega del ítem (60.0)".|
|**CP-RC-07**|Fecha de vencimiento nula|`fechaVencimiento: null`|`fechaVencimiento == null`|Lanza `ReglaNegocioException` con mensaje "La fecha de vencimiento no puede ser anterior a la fecha actual". No persiste.|
|**CP-RC-08**|Fecha de vencimiento anterior a la fecha actual|`fechaVencimiento: ayer`|`fechaVencimiento.isBefore(hoy)`|Lanza `ReglaNegocioException` con mensaje "La fecha de vencimiento no puede ser anterior a la fecha actual". No persiste.|
|**CP-RC-09**|Lote existente con la misma identificación de lote de proveedor pero fecha de vencimiento distinta|Ya existe un `LoteInsumoEntity` del insumo con `identificacionLoteProveedor: "L-2025-001"` y `fechaVencimiento: 2026-01-01`; el ingreso informa la misma identificación con `fechaVencimiento: 2026-06-01`|`loteExistente.getFechaVencimiento() != fechaVencimientoInformada`|Lanza `ReglaNegocioException` con mensaje "Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta". No suma al lote ni persiste.|
|**CP-RC-10**|Alta exitosa creando un lote nuevo _(Camino feliz)_|No existe lote previo con esa identificación para el insumo; DTO válido|Todas las validaciones $\rightarrow$ **OK**; `findByInsumoIdAndIdentificacionLoteProveedor` $\rightarrow$ **Optional.empty()**|Crea y persiste un `LoteInsumoEntity` nuevo con `cantidadActual` igual a la cantidad recibida. Persiste el ingreso con `tipoIngreso = COMPRA`, `estado = REGISTRADO`, `costoUnitario` igual al del detalle de compra (no se pide en el formulario), y retorna el DTO.|
|**CP-RC-11**|Alta exitosa reutilizando un lote existente con la misma fecha de vencimiento _(Camino feliz)_|Ya existe un `LoteInsumoEntity` del insumo con la misma identificación y `fechaVencimiento` coincidente, `cantidadActual: 50.0`; `cantidadRecibida: 20.0`|`loteExistente.getFechaVencimiento() == fechaVencimientoInformada`|No crea un lote nuevo: suma la cantidad recibida al lote existente (`cantidadActual` pasa de 50.0 a 70.0), persiste ambas entidades y retorna el DTO.|

### 4. `registrarIngresoInsumoDirecto(IngresoInsumoDirectoFormDTO ingresoInsumoDirectoFormDTO)`

Comparte con el alta por compra la validación de lote (**CP-RC-09**), pero valida el insumo y el costo unitario directamente, ya que no hay un detalle de compra que los determine.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RD-01**|Insumo inexistente (o dado de baja)|`idInsumo: 99L` (No existe en BD)|`insumoRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el insumo con ID: 99". No consulta nada más.|
|**CP-RD-02**|Cantidad recibida nula|`cantidadRecibida: null`|`cantidadRecibida == null`|Lanza `ReglaNegocioException` con mensaje "La cantidad recibida debe ser mayor a cero". No persiste.|
|**CP-RD-03**|Cantidad recibida menor o igual a cero|`cantidadRecibida: 0.0`|`cantidadRecibida <= 0`|Lanza `ReglaNegocioException` con mensaje "La cantidad recibida debe ser mayor a cero". No persiste.|
|**CP-RD-04**|Costo unitario nulo|`costoUnitario: null`|`costoUnitario == null`|Lanza `ReglaNegocioException` con mensaje "El costo unitario debe ser mayor a cero". No persiste.|
|**CP-RD-05**|Costo unitario menor o igual a cero|`costoUnitario: 0`|`costoUnitario.signum() <= 0`|Lanza `ReglaNegocioException` con mensaje "El costo unitario debe ser mayor a cero". No persiste.|
|**CP-RD-06**|Fecha de vencimiento nula|`fechaVencimiento: null`|`fechaVencimiento == null`|Lanza `ReglaNegocioException` con mensaje "La fecha de vencimiento no puede ser anterior a la fecha actual". No persiste.|
|**CP-RD-07**|Fecha de vencimiento anterior a la fecha actual|`fechaVencimiento: ayer`|`fechaVencimiento.isBefore(hoy)`|Lanza `ReglaNegocioException` con mensaje "La fecha de vencimiento no puede ser anterior a la fecha actual". No persiste.|
|**CP-RD-08**|Lote existente con la misma identificación de lote de proveedor pero fecha de vencimiento distinta|Ya existe un `LoteInsumoEntity` del insumo con `identificacionLoteProveedor: "L-2025-001"` y `fechaVencimiento: 2026-01-01`; el ingreso informa la misma identificación con `fechaVencimiento: 2026-06-01`|`loteExistente.getFechaVencimiento() != fechaVencimientoInformada`|Lanza `ReglaNegocioException` con mensaje "Ya existe un lote del insumo con la identificación de lote de proveedor 'L-2025-001' pero con una fecha de vencimiento distinta". No persiste.|
|**CP-RD-09**|Alta exitosa creando un lote nuevo _(Camino feliz)_|No existe lote previo con esa identificación para el insumo; DTO válido|Todas las validaciones $\rightarrow$ **OK**; `findByInsumoIdAndIdentificacionLoteProveedor` $\rightarrow$ **Optional.empty()**|Crea y persiste un `LoteInsumoEntity` nuevo. Persiste el ingreso con `tipoIngreso = DIRECTO`, `estado = REGISTRADO`, `detalleCompra = null` y `costoUnitario` igual al informado en el formulario, y retorna el DTO.|
|**CP-RD-10**|Alta exitosa reutilizando un lote existente con la misma fecha de vencimiento _(Camino feliz)_|Ya existe un `LoteInsumoEntity` del insumo con la misma identificación y `fechaVencimiento` coincidente, `cantidadActual: 50.0`; `cantidadRecibida: 20.0`|`loteExistente.getFechaVencimiento() == fechaVencimientoInformada`|No crea un lote nuevo: suma la cantidad recibida al lote existente (`cantidadActual` pasa de 50.0 a 70.0), persiste ambas entidades y retorna el DTO.|

### 5. `anularIngresoInsumo(Long id, AnularIngresoInsumoFormDTO anularIngresoInsumoFormDTO)`

No existe la baja lógica para este registro: solo puede pasar de `REGISTRADO` a `ANULADO`.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AI-01**|Motivo de anulación nulo|`motivoAnulacion: null`|`motivoAnulacion == null`|Lanza `ReglaNegocioException` con mensaje "El motivo de anulación es obligatorio". No busca el ingreso ni persiste.|
|**CP-AI-02**|Motivo de anulación en blanco|`motivoAnulacion: "   "`|`motivoAnulacion.isBlank()`|Lanza `ReglaNegocioException` con mensaje "El motivo de anulación es obligatorio". No busca el ingreso ni persiste.|
|**CP-AI-03**|Ingreso de insumo inexistente|`id: 99L` (No existe en BD), motivo válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el ingreso de insumo con ID: 99". No persiste.|
|**CP-AI-04**|Ingreso ya anulado|`id: 1L` con `estado: ANULADO`, motivo válido|`ingresoInsumoEntity.getEstado() != REGISTRADO`|Lanza `ReglaNegocioException` con mensaje "Solo se pueden anular ingresos de insumo en estado REGISTRADO". No persiste.|
|**CP-AI-05**|Cantidad del ingreso supera la cantidad disponible del lote|Ingreso `REGISTRADO` con `cantidadRecibida: 50.0`; lote asociado con `cantidadActual: 50.0` y `cantidadReservada: 20.0` (disponible: 30.0)|`cantidadRecibida (50.0) > cantidadDisponible (30.0)`|Lanza `ReglaNegocioException` con mensaje "No se puede anular el ingreso: su cantidad supera la cantidad disponible del lote de insumo". No descuenta del lote ni persiste el ingreso.|
|**CP-AI-06**|Anulación exitosa _(Camino feliz)_|Ingreso `REGISTRADO` con `cantidadRecibida: 20.0`; lote asociado con `cantidadActual: 50.0`, `cantidadReservada: 0.0` (disponible: 50.0), motivo válido|`cantidadRecibida (20.0) <= cantidadDisponible (50.0)`|El ingreso pasa a `estado = ANULADO`, registra `fechaAnulacion` y `motivoAnulacion`. El lote pasa a `cantidadActual = 30.0`. Persiste ambas entidades y retorna el DTO del ingreso anulado.|
