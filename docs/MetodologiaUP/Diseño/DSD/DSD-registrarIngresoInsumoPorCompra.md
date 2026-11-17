# Diagrama de secuencia de diseño: registrar ingreso de insumo por compra

El diagrama muestra cómo `IngresoInsumoController.registrarIngresoPorCompra` y `IngresoInsumoServicioImpl.registrarIngresoInsumoPorCompra` reciben un ítem de una orden de compra, actualizan el stock y su costo promedio y, cuando corresponde, finalizan la orden. Parte del formulario ya abierto, con un usuario autorizado mediante `INGRESO_INSUMO_REGISTRAR`. El ingreso directo queda fuera de este caso de uso.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as IngresoInsumoController
    participant S as IngresoInsumoServicioImpl
    participant D as IDetalleCompraRepository
    participant O as IOrdenCompraRepository
    participant I as IInsumoRepository
    participant L as ILoteInsumoRepository
    participant Stock as LoteInsumoEntity
    participant G as GuardadoUnico
    participant R as IIngresoInsumoRepository

    Usuario->>Vista: Elegir orden e ítem e informar unidades, lote y fechas
    Vista->>C: POST /ingresos-insumo/compra
    Note over C: Spring MVC enlaza y valida IngresoInsumoPorCompraFormDTO<br/>itemPerteneceALaOrden verifica la selección cuando puede resolver la orden indicada
    alt Errores de validación MVC o ítem ajeno a la orden elegida
        C->>C: cargarModeloCompra(model, ingresoForm, ordenSeleccionada)
        C-->>Vista: Vista ingreso_insumo/ingreso-compra-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: registrarIngresoInsumoPorCompra(ingresoForm)
        Note over S: Validar ítem informado, unidades positivas<br/>fecha de ingreso no futura y vencimiento no anterior a hoy
        alt Registro permitido (búsquedas y validaciones satisfactorias)
            S->>D: buscarPorIdParaIngresar(idDetalleCompra)
            D-->>S: DetalleCompraEntity bajo bloqueo de escritura
            S->>O: buscarPorIdParaCambiarEstado(idOrdenCompra)
            O-->>S: OrdenCompraEntity bajo bloqueo de escritura
            Note over S: Exigir orden PENDIENTE e insumo no dado de baja
            S->>R: findByDetalleCompraIdAndEstado(idDetalleCompra, REGISTRADO)
            R-->>S: Ingresos registrados del ítem
            Note over S: calcularPresentacionesPendientes limita las unidades a recibir<br/>CantidadesDetalleCompra convierte unidades comerciales y costo<br/>a cantidad y costo por unidad del insumo
            S->>I: buscarPorIdParaIngresar(idInsumo)
            I-->>S: InsumoEntity bajo bloqueo de escritura
            Note over S: Revalidar insumo activo y normalizar identificación del lote
            S->>L: buscarPorInsumoIdYIdentificacionLoteProveedorParaIngresar(idInsumo, identificacion)
            L-->>S: Lote de insumo existente, si lo hay
            alt Existe lote con esa identificación para el insumo
                Note over S: Exigir coincidencia de la fecha de vencimiento<br/>y reutilizar el lote encontrado
            else No existe lote con esa identificación
                Note over S: Construir LoteInsumoEntity con insumo, identificación y vencimiento
            end
            S->>Stock: sumarIngreso(cantidadRecibida, costoPorUnidadDelInsumo)
            Note over Stock: Aumentar cantidad actual y recalcular costo unitario PPP
            S->>G: guardar(loteInsumoRepository, loteInsumoEntity, excepcionesPorIndice)
            Note over G,L: Guardar y hacer flush del lote mediante ILoteInsumoRepository<br/>Traducir conflicto concurrente de unicidad a regla de negocio
            G-->>S: LoteInsumoEntity guardado
            Note over S: Crear IngresoInsumoEntity REGISTRADO de tipo COMPRA<br/>con cantidad y costo por unidad de insumo, detalle y lote de insumo
            S->>R: save(ingresoInsumoEntity)
            R-->>S: IngresoInsumoEntity registrado
            Note over S: ordenCompraCompletamenteRecibida consulta los ingresos<br/>REGISTRADO de cada ítem para evaluar su pendiente
            opt Todos los ítems de la orden quedaron completamente recibidos
                Note over S: Orden FINALIZADA con fecha y sin motivo de cierre forzado
                S->>O: save(ordenCompraEntity)
            end
            S-->>C: IngresoInsumoResponseDTO
            Note over C: Mensaje flash: Ingreso de insumo registrado correctamente
            C-->>Vista: redirect:/ingresos-insumo?tipo=compra
            Vista->>C: GET /ingresos-insumo?tipo=compra
            C-->>Vista: Vista ingreso_insumo/ingreso-lista
            Vista-->>Usuario: Pestaña Por compra actualizada y mensaje de éxito
        else Rechazo en el primer paso que falla
            S-->>C: ReglaNegocioException, RecursoNoEncontradoException o RecursoDuplicadoException
            Note over S: Interrumpir el ingreso y revertir la transacción
            Note over C: Asociar error al campo admitido indicado por la excepción<br/>o al resumen global en otro caso
            C->>C: cargarModeloCompra(model, ingresoForm, ordenSeleccionada)
            C-->>Vista: Vista ingreso_insumo/ingreso-compra-form con datos y errores
            Vista-->>Usuario: Formulario conservado para corregir
        end
    end
```

## Participantes y abstracción

- **Interfaz web e IngresoInsumoController:** representan `ingreso_insumo/ingreso-compra-form.html`, la petición HTTP y el listado `ingreso_insumo/ingreso-lista.html`. El ID de la orden seleccionada viaja como parámetro auxiliar de pantalla, mientras el detalle de compra identifica la recepción en el servicio. El éxito usa Post-Redirect-Get hacia la pestaña Por compra y los errores vuelven a renderizar el formulario en la misma petición.
- **IngresoInsumoServicioImpl, IDetalleCompraRepository e IOrdenCompraRepository:** coordinan la recepción transaccional y bloquean detalle y orden para serializar ingresos y cierres concurrentes. La orden debe estar `PENDIENTE` y las unidades recibidas no pueden superar las pendientes del ítem.
- **IIngresoInsumoRepository:** aporta las recepciones registradas para calcular pendientes y guarda el ingreso `COMPRA`. Tras guardarlo, se revisan todos los ítems y se finaliza automáticamente la orden si ya no queda cantidad pendiente. Ese cierre conserva fecha y no tiene motivo de finalización forzada.
- **IInsumoRepository e ILoteInsumoRepository:** resuelven el stock por insumo e identificación de lote de proveedor, comparada sin distinguir mayúsculas. Se bloquea primero el insumo para serializar la creación o reutilización de su lote. Si el lote existe, su vencimiento debe coincidir con el informado.
- **LoteInsumoEntity:** tiene línea de vida porque su método real `sumarIngreso` aumenta existencias y recalcula el precio promedio ponderado (PPP), combinando valor y cantidad anteriores con el nuevo aporte.
- **GuardadoUnico:** encapsula la persistencia y el `flush` del lote de insumo. La restricción única conserva la garantía final y un conflicto concurrente se traduce a una regla de negocio que permite informar el rechazo.

La cantidad de compra se expresa en unidades comerciales, por ejemplo bolsas. Se convierte al contenido real en la unidad del insumo antes de sumar stock. El costo de la orden está definido por presentación y también se convierte al costo por unidad de insumo que conserva el ingreso. Esta transformación se mantiene como nota por su significado de negocio, sin detallar fórmulas.

El fragmento permitido resume el recorrido satisfactorio. Cualquier validación fallida, referencia inexistente, exceso de recepción o conflicto de vencimiento interrumpe el primer paso que falle, sin ejecutar los siguientes. La transacción revierte las escrituras previas, incluido el cambio de stock y PPP. Las fechas de ingreso y vencimiento son obligatorias y se comparan con hoy según sus reglas respectivas.

El controlador captura los rechazos del servicio, incluidos recursos no encontrados y duplicados. Los asocia al campo admitido indicado por la excepción, o al resumen global en otro caso. Las consultas de `itemPerteneceALaOrden`, `cargarModeloCompra` y del listado se encapsulan para mantener el nivel de diseño. El servicio deriva la orden real del detalle recibido y no confía en el parámetro auxiliar de pantalla.

Se omiten getters, setters, construcción de entidades, conversiones a DTO, redondeos, buscadores y consultas de presentación. No se registra un ingreso directo, consumo o reserva de insumos en este flujo. La infraestructura transversal de seguridad y auditoría y los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [IngresoInsumoController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/ingreso_insumo/IngresoInsumoController.java): `registrarIngresoPorCompra`, `itemPerteneceALaOrden`, reconstrucción del formulario y manejo de errores.
- [IngresoInsumoServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/ingreso_insumo/IngresoInsumoServicioImpl.java): `registrarIngresoInsumoPorCompra`, cálculo de pendientes, resolución del lote y cierre automático de la orden.
- [LoteInsumoEntity.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/entity/ingreso_insumo/LoteInsumoEntity.java): `sumarIngreso` y actualización del PPP.
- [IDetalleCompraRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/orden_compra/IDetalleCompraRepository.java), [IOrdenCompraRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/orden_compra/IOrdenCompraRepository.java), [IInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/insumo/IInsumoRepository.java), [ILoteInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/ingreso_insumo/ILoteInsumoRepository.java) e [IIngresoInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/ingreso_insumo/IIngresoInsumoRepository.java): búsquedas, bloqueos y persistencia.
- [GuardadoUnico.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/persistencia/GuardadoUnico.java): guardado y traducción de conflictos de unicidad.
