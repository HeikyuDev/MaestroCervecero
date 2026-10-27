# Mejoras Futuras

Este documento registra funcionalidades identificadas durante el desarrollo que **no se van a implementar todavía**, pero que aportan valor real al sistema y quedan documentadas para retomarlas más adelante.

---

## 1. Re-reserva automática por FEFO tras una merma que afecta cantidad reservada

### Contexto

Al registrar un `AjusteInsumo` de tipo `EGRESO` (o al anular uno de tipo `INGRESO`) que supera la `cantidadDisponible` de un `LoteInsumoEntity`, el sistema actual (`AjusteInsumoServicioImpl.descontarDelLote`) reduce **proporcionalmente** la `cantidadReservada` de todas las `ReservaInsumoEntity` activas sobre ese lote de insumo. Esto evita que el usuario tenga que cancelar un lote de producción cuando en la realidad operativa lo que ocurrió fue la rotura/pérdida de una bolsa física de insumo.

El problema que deja abierto ese comportamiento: la(s) etapa(s) de producción que tenían esa cantidad reservada quedan con **menos insumo reservado del que en verdad necesitan** para completar su receta. Hoy el sistema no hace nada para compensar esa merma — el faltante queda silenciosamente reflejado como una reserva más chica.

### Propuesta

En lugar de solo reducir la reserva afectada, el sistema debería intentar **re-reservar automáticamente la cantidad faltante usando el mismo algoritmo FEFO** (First Expired, First Out) que ya se usa en `iniciarLote`, buscando otro(s) lote(s) de insumo disponibles para cubrir el déficit.

Ejemplo: se pierden 100 Kg de un lote físico de Malta A que tenía reservados 60 Kg para la etapa de Molienda del Lote de producción #12. Hoy, esa reserva simplemente queda reducida (o en 0, si el excedente la consume entera). Con la mejora, el sistema debería buscar automáticamente otro lote de Malta A en stock (el siguiente más próximo a vencer) y generar una nueva `ReservaInsumoEntity` para cubrir esos 60 Kg faltantes de esa misma etapa.

### Por qué no es trivial

1. **Cruce de módulos**: hoy `AjusteInsumoServicioImpl` (paquete `ingreso_insumo`) no depende en absoluto de la lógica de `Lote`/`ReservaInsumo`. La lógica FEFO de reserva vive hoy embebida dentro de `LoteServicioImpl` (`obtenerYValidarStockDisponible`, `reservarInsumosFEFO`). Para reutilizarla desde Ajuste, hay que extraerla a un componente compartido (p. ej. un `IReservaInsumoServicio` o similar) del que dependan ambos servicios, en lugar de que uno le pegue directamente a la implementación del otro.

2. **Qué pasa si no alcanza el stock**: si no queda otro lote de ese insumo en stock (o no alcanza para cubrir todo el déficit), no se puede bloquear el registro del ajuste — el operario igual necesita dejar constancia de la merma real. El comportamiento actual (reducir la reserva proporcionalmente) pasaría a ser el **fallback**: se re-reserva todo lo que se pueda vía FEFO, y lo que no se logre cubrir cae en la reducción de reserva existente.

3. **Un solo ajuste puede afectar múltiples etapas/lotes de producción a la vez**: un mismo `LoteInsumoEntity` puede tener reservas activas de varias `EtapaLoteEntity` distintas (de distintos lotes de producción en curso). Una sola merma podría necesitar disparar **N re-reservas FEFO** en la misma transacción — mismo patrón que ya maneja `iniciarLote` para múltiples requerimientos, pero ahora encadenado dentro del flujo de un ajuste.

4. **Cobertura de tests**: hace falta cubrir como mínimo tres escenarios — cobertura total (hay stock suficiente en otro lote para cubrir todo el faltante), cobertura parcial (se cubre una parte y el resto cae en la reducción de reserva actual) y cobertura nula (no hay stock alternativo, se preserva el comportamiento de hoy).

### Cuándo retomarla

"Registrar Consumo de Insumo" (`ConsumoInsumoServicioImpl`) ya trabaja de lleno sobre `ReservaInsumoEntity`, por lo que la forma de manipular reservas está definida. La re-reserva automática puede abordarse tomando ese servicio como referencia para extraer la lógica FEFO a un componente compartido.

---

## 2. Aviso de edición concurrente de un proveedor

### Contexto

Modificar un proveedor (`ProveedorServicioImpl.modificarProveedor`) bloquea la fila del proveedor (`PESSIMISTIC_WRITE`) y registra una nueva versión con los datos enviados. El bloqueo garantiza una sola versión vigente, pero no detecta que el proveedor cambió desde que el usuario abrió el formulario: si dos usuarios abren la misma pantalla de edición y guardan uno después del otro, la segunda modificación se aplica sobre la versión que dejó la primera sin avisar, y los cambios de la primera se pierden en silencio.

### Propuesta

Agregar al formulario de edición un token de versión (por ejemplo, el id de la versión vigente que se precargó) como campo oculto. Al guardar, el Service compara ese token con la versión vigente: si no coincide, rechaza la modificación con un mensaje claro ("El proveedor fue modificado por otra persona desde que abrió el formulario, revise los datos e intente nuevamente") y el formulario se vuelve a mostrar con los datos vigentes.

### Por qué no es trivial

1. **Contrato del formulario**: el token viaja en `ProveedorFormDTO`, pero es un dato de control, no un dato del proveedor; hay que decidir si va en el DTO o como parámetro separado del método del Controller.
2. **Experiencia de uso**: ante el rechazo, el usuario necesita ver qué cambió; mostrar solo el mensaje obliga a rehacer la carga.
3. **Uniformidad**: el mismo problema existe en los demás módulos versionados (por ejemplo, Recetas); conviene resolverlo con un mecanismo común.

---

## 3. Una consulta por lote para los nombres del catálogo del proveedor

### Contexto

Al abrir el formulario de edición de un proveedor o al volver a mostrarlo tras un error, `ProveedorController` resuelve el nombre de cada insumo y de cada presentación comercial del catálogo con una consulta por id (`buscarPorId`). Con el máximo de 100 ítems, eso son hasta unas 200 consultas por pedido.

### Propuesta

Agregar a los Services de insumos y de presentaciones comerciales una búsqueda por un conjunto de ids (una consulta `IN` por tipo de dato), y que el Controller arme los mapas id → datos con dos consultas en total. Un id que ya no existe sigue sin entrada en el mapa, y la vista lo indica como "no disponible".

### Por qué no es trivial

1. **Contrato de los Services**: hace falta un método nuevo en `IInsumoServicio` y en `IPresentacionComercialServicio`, con su validación, sus tests y su documentación de casos de prueba.
2. **Insumos activos y dados de baja**: la consulta por lote debe respetar el mismo criterio de visibilidad que `buscarPorId` para que la pantalla muestre lo mismo.

---

## 4. Tiempo máximo de espera de los bloqueos de fila

### Contexto

El registro de órdenes de compra y de ingresos de insumo, su anulación y la baja del proveedor toman bloqueos de fila (`PESSIMISTIC_WRITE`) sobre la orden de compra, el proveedor, la planificación, el insumo y el lote. No hay configurado `jakarta.persistence.lock.timeout` (ni el equivalente del motor): una transacción que espera un bloqueo lo hace hasta el tiempo de espera por defecto de la base de datos, y varias personas operando sobre la misma orden o el mismo proveedor forman una cola larga.

### Propuesta

Configurar un tiempo de espera corto para los bloqueos (por ejemplo, unos segundos) y traducir la falla por tiempo vencido a un mensaje claro ("El registro está siendo modificado por otra persona, intente nuevamente"), con el mismo tratamiento que ya reciben los índices únicos.

### Por qué no es trivial

1. **Alcance**: el valor se aplica a todos los módulos que bloquean filas, no solo a estos dos.
2. **Traducción del error**: hay que decidir en qué capa se convierte la excepción del motor y cómo se muestra en cada pantalla.

---

## 5. Consultas por ítem al decidir si una orden de compra quedó completamente recibida

### Contexto

Al registrar un ingreso por compra, y al anular el ingreso de una orden finalizada automáticamente, `ordenCompraCompletamenteRecibida` calcula la cantidad pendiente de cada ítem de la orden con una consulta por ítem (`findByDetalleCompraIdAndEstado`). Con el máximo de 100 ítems son hasta 100 consultas por operación, dentro de una transacción que mantiene bloqueada la orden.

### Propuesta

Reemplazarlas por una sola consulta que devuelva, por ítem de la orden, la suma de la cantidad recibida de sus ingresos `REGISTRADO`, y calcular el pendiente en memoria.

### Por qué no es trivial

1. **Contrato del repositorio**: la consulta agrupada es nueva y su JPQL solo se verifica contra la base de datos.
2. **Un solo criterio**: el cálculo del pendiente (conversión de la presentación comercial a la unidad del insumo y redondeo) debe seguir en un único lugar, también para la validación del ingreso individual.

---

## 6. Bitácora propia para el cierre y la reapertura automáticos de una orden de compra

### Contexto

Cuando un ingreso completa la recepción, la orden de compra pasa a `FINALIZADA` de forma automática; cuando se anula ese ingreso, puede volver a `PENDIENTE`. Ambos cambios ocurren dentro de la operación de ingreso, que registra una sola entrada de bitácora (concepto ingreso de insumo): el cambio de estado de la orden solo queda en sus campos de auditoría de la entidad (`LastModifiedBy` y `LastModifiedDate`), sin una entrada propia de finalización o de reapertura del concepto orden de compra.

### Propuesta

Registrar una segunda entrada de bitácora (acción finalizar, concepto orden de compra) cuando la operación cierra la orden, y una de reapertura cuando la vuelve a abrir, reutilizando el mismo registro diferido hasta la confirmación de la transacción.

### Por qué no es trivial

1. **Mecanismo**: el aspecto de auditoría solo conoce los métodos anotados; hay que exponer una forma de registrar una entrada adicional desde el Service sin duplicar la lógica de usuario e IP.
2. **Vocabulario**: la reapertura no tiene hoy una acción de auditoría propia.
