## Casos de prueba de `MapperOrdenCompra`

**Tipo de prueba:** unitaria (clase `MapperOrdenCompraTest`), sin Spring ni mocks. Verifica el subtotal de cada ítem, el total de la orden y las cantidades recibida y pendiente en el `OrdenCompraResponseDTO`.

**Regla de conversión:** `subtotal` es `cantidad` por `costoUnitario` y `total` es la suma de los subtotales, ambos sin redondear. `cantidadRecibida` y `cantidadPendiente` son unidades (la recibida es la cantidad de insumo recibida dividida por lo que trae cada presentación; la pendiente, las pedidas menos las recibidas), y `cantidadRecibidaEnUnidadDelInsumo` y `cantidadPendienteEnUnidadDelInsumo` son su equivalente en la unidad del insumo. Los cuatro solo se informan con `toDTOConCantidades`, redondeados a 4 decimales; el cálculo es el del ingreso de insumos (`CantidadesDetalleCompra`).

---

### `toDTO(OrdenCompraEntity)` y `toDTOConCantidades(OrdenCompraEntity, ToDoubleFunction<DetalleCompraEntity>)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-01** | Subtotales y total sin cantidades | Orden con ítems `3 x 12.3456` y `2 x 0.5000` | `toDTO` calcula subtotales y total | Subtotales `37.0368` y `1.0000`, `total: 38.0368`; las cuatro cantidades recibida y pendiente son `null`. |
| **CP-MOC-02** | Orden sin ítems | Orden con la lista de ítems vacía | La suma de una lista vacía | `detallesCompra` vacío y `total: 0`. |
| **CP-MOC-03** | Decimales del costo unitario | Orden con un ítem `3 x 0.0001` | El subtotal y el total no se redondean | `subtotal: 0.0003` y `total: 0.0003` (4 decimales). |
| **CP-MOC-04** | Cantidades redondeadas | Ítem `2 x 10.0000` con presentación de 1 `KILOGRAMO` para un insumo en `KILOGRAMO`, cantidad recibida 0.33333 | Recibida y pendiente se redondean a 4 decimales | `cantidadRecibida: 0.3333`, `cantidadPendiente: 1.6667` (en unidades), las mismas cantidades en la unidad del insumo, `subtotal` y `total` `20.0000`. |
| **CP-MOC-05** | Unidades y conversión de unidad de la presentación | Ítem `5 x 10.0000` con presentación de 25000 `GRAMO` para un insumo en `KILOGRAMO`, cantidad recibida 50.5 | La presentación se convierte a la unidad del insumo (125 kg solicitados) | `cantidadRecibida: 2.02` y `cantidadPendiente: 2.98` unidades; `cantidadRecibidaEnUnidadDelInsumo: 50.5` y `cantidadPendienteEnUnidadDelInsumo: 74.5`. |
| **CP-MOC-06** | Orden nula | `ordenCompraEntity: null` | La entrada no existe | Ambos métodos retornan `null`. |
| **CP-MOC-07** | Usuario de cierre según el estado | Tres órdenes con `lastModifiedBy: "gerente.compras"`, una `ANULADA`, una `FINALIZADA` y una `PENDIENTE` | El usuario de anulación o de finalización solo se informa en el estado que corresponde | La anulada informa `usuarioAnulacion: "gerente.compras"` y `usuarioFinalizacion: null`; la finalizada, `usuarioFinalizacion: "gerente.compras"` y `usuarioAnulacion: null`; la pendiente informa ambos en `null`. En una finalización automática, `usuarioFinalizacion` es el usuario que registró el ingreso que completó la orden (es quien hizo la última modificación). |
| **CP-MOC-08** | Completitud de la orden | Ítems `5 x 10.0000` y `3 x 10.0000`, ambos con presentación de 25000 `GRAMO` para un insumo en `KILOGRAMO`; recibidos 50 kg y 75 kg | `toDTOConCantidades` suma las unidades de los ítems | `totalItems: 2`, `totalPedidas: 8`, `totalRecibidas: 5.0`, `totalPendientes: 3.0` y `porcentajeRecibido: 63`. |
| **CP-MOC-09** | Porcentaje acotado y totales sin cantidades | Un ítem `4 x 10.0000` con presentación de 25000 `GRAMO` y 150 kg recibidos; y la misma orden con `toDTO` | Lo recibido de más no pasa de 100 %; sin cantidades recibidas no hay totales recibidos | Con `toDTOConCantidades`: `porcentajeRecibido: 100` y `totalPendientes: 0.0`. Con `toDTO`: `totalItems: 1`, `totalPedidas: 4` y `totalRecibidas`, `totalPendientes` y `porcentajeRecibido` en `null`. |
