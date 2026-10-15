## Casos de prueba de `MapperPlanificacionProduccion`

**Tipo de prueba:** unitaria (clase `MapperPlanificacionProduccionTest`), sin Spring ni mocks. Verifica cómo se informa la anulación de una planificación en el `PlanificacionProduccionResponseDTO`.

**Regla de conversión:** `fechaAnulacion` y `motivoAnulacion` se copian de la entidad. `usuarioAnulacion` es el usuario de la última modificación de la auditoría (`lastModifiedBy`) y solo se informa si la planificación está `ANULADA`.

---

### `toDTO(PlanificacionProduccionEntity, double)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MPP-01** | Planificación anulada | Planificación `ANULADA` con `fechaAnulacion: 02/07/2027 09:30`, `motivoAnulacion: "Se canceló el pedido del cliente"` y `lastModifiedBy: "gerente"` | Se informa quién, cuándo y por qué se anuló | El DTO tiene la misma fecha y motivo y `usuarioAnulacion: "gerente"`. |
| **CP-MPP-02** | Planificación no anulada | Planificación `PENDIENTE` con `lastModifiedBy: "gerente"` | El usuario de la última modificación no es el de una anulación | `usuarioAnulacion` es `null`. |
