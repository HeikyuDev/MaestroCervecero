## Casos de prueba de `MapperOrdenCompra`

**Tipo de prueba:** unitaria (clase `MapperOrdenCompraTest`), sin Spring ni mocks. Verifica el subtotal de cada ítem, el total de la orden y las cantidades recibida y pendiente en el `OrdenCompraResponseDTO`.

**Regla de conversión:** `subtotal` es `cantidad` por `costoUnitario` y `total` es la suma de los subtotales, ambos sin redondear. `cantidadRecibida` y `cantidadPendiente` solo se informan con `toDTOConCantidades`, redondeadas a 4 decimales; el pendiente usa la misma regla que el ingreso de insumos (`CantidadesDetalleCompra`).

---

### `toDTO(OrdenCompraEntity)` y `toDTOConCantidades(OrdenCompraEntity, ToDoubleFunction<DetalleCompraEntity>)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-01** | Subtotales y total sin cantidades | Orden con ítems `3 x 12.3456` y `2 x 0.5000` | `toDTO` calcula subtotales y total | Subtotales `37.0368` y `1.0000`, `total: 38.0368`; `cantidadRecibida` y `cantidadPendiente` son `null`. |
| **CP-MOC-02** | Orden sin ítems | Orden con la lista de ítems vacía | La suma de una lista vacía | `detallesCompra` vacío y `total: 0`. |
| **CP-MOC-03** | Decimales del costo unitario | Orden con un ítem `3 x 0.0001` | El subtotal y el total no se redondean | `subtotal: 0.0003` y `total: 0.0003` (4 decimales). |
| **CP-MOC-04** | Cantidades redondeadas | Ítem `2 x 10.0000` con presentación de 1 `KILOGRAMO` para un insumo en `KILOGRAMO`, cantidad recibida 0.33333 | Recibida y pendiente se redondean a 4 decimales | `cantidadRecibida: 0.3333`, `cantidadPendiente: 1.6667`, `subtotal` y `total` `20.0000`. |
| **CP-MOC-05** | Conversión de unidad de la presentación | Ítem `5 x 10.0000` con presentación de 25000 `GRAMO` para un insumo en `KILOGRAMO`, cantidad recibida 50.5 | La presentación se convierte a la unidad del insumo (125 kg solicitados) | `cantidadRecibida: 50.5` y `cantidadPendiente: 74.5`. |
| **CP-MOC-06** | Orden nula | `ordenCompraEntity: null` | La entrada no existe | Ambos métodos retornan `null`. |
