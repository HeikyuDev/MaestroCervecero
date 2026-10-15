## Casos de prueba de `MapperConsumoInsumo`

**Tipo de prueba:** unitaria (clase `MapperConsumoInsumoTest`), sin Spring ni mocks. Verifica cómo se informa la anulación de un consumo en el `ConsumoInsumoResponseDTO`.

**Regla de conversión:** `fechaAnulacion` y `motivoAnulacion` se copian de la entidad. `vencidoAlRegistrar` indica si el vencimiento del lote de insumo es anterior a la fecha de registro del consumo. `usuarioAnulacion` es el usuario de la última modificación de la auditoría (`lastModifiedBy`) y solo se informa si el consumo está `ANULADO`: la anulación es la última modificación de un consumo, que después no se vuelve a modificar. Un consumo `REGISTRADO` no informa usuario de anulación.

---

### `toDTO(ConsumoInsumoEntity)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCI-01** | Consumo anulado | Consumo `ANULADO` con `fechaAnulacion: 02/07/2027 09:30`, `motivoAnulacion: "Error de carga"` y `lastModifiedBy: "operario"` | Se informa quién, cuándo y por qué se anuló | El DTO tiene la misma fecha y motivo y `usuarioAnulacion: "operario"`. |
| **CP-MCI-02** | Consumo registrado | Consumo `REGISTRADO` con `lastModifiedBy: "operario"` | El usuario de la última modificación no es el de una anulación | `usuarioAnulacion` y `fechaAnulacion` son `null`. |
| **CP-MCI-04** | Lote vencido al registrar | Consumo con `createdDate: 01/07/2027 10:15` y lote de insumo con `fechaVencimiento: 30/06/2027` | El vencimiento es anterior a la fecha de registro | `vencidoAlRegistrar` es `true`. |
| **CP-MCI-05** | Lote vigente al registrar | Consumo con `createdDate: 01/07/2027 10:15` y lote que vence el mismo día (01/07/2027), otro que vence después y otro sin fecha de vencimiento | Un lote que vence el día del registro todavía es válido; sin fecha no se marca | `vencidoAlRegistrar` es `false` en los tres. |
| **CP-MCI-03** | Consumo nulo | `consumoInsumoEntity: null` | La entrada no existe | Retorna `null`. |
