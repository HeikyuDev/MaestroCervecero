## Casos de prueba de `MapperIngresoInsumo`

**Tipo de prueba:** unitaria (clase `MapperIngresoInsumoTest`), sin Spring ni mocks. Verifica cómo se informan la anulación y la orden de compra de un ingreso en el `IngresoInsumoResponseDTO`.

**Regla de conversión:** `usuarioAnulacion` es el usuario de la última modificación de la auditoría (`lastModifiedBy`) y solo se informa si el ingreso está `ANULADO`: la anulación es la última modificación de un ingreso, que después no se vuelve a modificar. `idOrdenCompra` es el ID de la orden de compra del ítem recibido (`detalleCompra.ordenCompra.id`) y es `null` en un ingreso `DIRECTO`, que no tiene ítem de la orden.

---

### `toDTO(IngresoInsumoEntity)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MII-01** | Ingreso anulado | Ingreso `ANULADO` con `fechaAnulacion: 02/07/2027 09:30`, `motivoAnulacion: "Error de carga"` y `lastModifiedBy: "operario"` | Se informa quién, cuándo y por qué se anuló | El DTO tiene la misma fecha y motivo y `usuarioAnulacion: "operario"`. |
| **CP-MII-02** | Ingreso registrado | Ingreso `REGISTRADO` con `lastModifiedBy: "operario"` | El usuario de la última modificación no es el de una anulación | `usuarioAnulacion` es `null`. |
| **CP-MII-03** | Ingreso por compra | Ingreso `COMPRA` cuyo ítem pertenece a la orden de compra con ID 7 | El ingreso tiene ítem de la orden | `idOrdenCompra: 7`. |
| **CP-MII-04** | Ingreso directo | Ingreso `DIRECTO` sin ítem de la orden | El ingreso no tiene ítem de la orden | `idOrdenCompra` y `detalleCompra` son `null`. |
